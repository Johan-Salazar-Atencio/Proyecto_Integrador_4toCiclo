package com.plantia.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.models.EventoDesastre;
import com.plantia.models.PlantaUsuario;
import com.plantia.models.RegistroDesastreUsuario;
import com.plantia.models.TipoDesastre;
import com.plantia.repositories.EventoDesastreRepository;
import com.plantia.repositories.PlantaUsuarioRepository;
import com.plantia.repositories.RegistroDesastreUsuarioRepository;
import com.plantia.repositories.TipoDesastreRepository;

/**
 * Cuando un evento termina, cada planta afectada queda SALVADA (se previno el evento) o DANADA: la salud
 * baja casi a cero segun el tipo y la severidad y empieza una ventana para abonarla; si no se abona a tiempo
 * muere (ver PlantasService.revisarRecuperacion). Si el usuario previno el evento en todas sus plantas,
 * ademas completa el evento y recibe un premio extra.
 */
@Service
public class ResultadoDesastreService {

    private static final Logger log = LoggerFactory.getLogger(ResultadoDesastreService.class);

    /** Salud con la que queda una planta que no se previno, si el tipo de evento no define otra. */
    static final int SALUD_FINAL_BAJO = 40;
    static final int SALUD_FINAL_MODERADO = 20;
    static final int SALUD_FINAL_CRITICO = 10;

    private final EventoDesastreRepository eventoRepo;
    private final PlantaUsuarioRepository plantaRepo;
    private final RegistroDesastreUsuarioRepository registroRepo;
    private final TipoDesastreRepository tipoRepo;
    private final ProgresoService progreso;
    private final ParametrosJuego parametros;
    private final ObjectProvider<LogroService> logroService;
    private final Duration gracia;

    /** Para una transaccion corta por usuario (QA-45); null en pruebas sin Spring. */
    @Autowired(required = false)
    private org.springframework.transaction.PlatformTransactionManager transacciones;

    /** Constructor que usa Spring (hay otro, de compatibilidad para pruebas: sin esta marca la app no arranca). */
    @Autowired
    public ResultadoDesastreService(EventoDesastreRepository eventoRepo,
                                    PlantaUsuarioRepository plantaRepo,
                                    RegistroDesastreUsuarioRepository registroRepo,
                                    TipoDesastreRepository tipoRepo,
                                    ProgresoService progreso,
                                    ParametrosJuego parametros,
                                    ObjectProvider<LogroService> logroService,
                                    @Value("${app.eventos.gracia-horas:6}") long graciaHoras) {
        this.eventoRepo = eventoRepo;
        this.plantaRepo = plantaRepo;
        this.registroRepo = registroRepo;
        this.tipoRepo = tipoRepo;
        this.progreso = progreso;
        this.parametros = parametros;
        this.logroService = logroService;
        this.gracia = Duration.ofHours(graciaHoras);
    }

    public ResultadoDesastreService(EventoDesastreRepository eventoRepo,
                                    PlantaUsuarioRepository plantaRepo,
                                    RegistroDesastreUsuarioRepository registroRepo,
                                    TipoDesastreRepository tipoRepo,
                                    ProgresoService progreso,
                                    ParametrosJuego parametros,
                                    @Value("${app.eventos.gracia-horas:6}") long graciaHoras) {
        this(eventoRepo, plantaRepo, registroRepo, tipoRepo, progreso, parametros, new ObjectProvider<LogroService>() {
            @Override public LogroService getObject(Object... args) { return null; }
            @Override public LogroService getObject() { return null; }
            @Override public LogroService getIfAvailable() { return null; }
            @Override public LogroService getIfUnique() { return null; }
            @Override public java.util.stream.Stream<LogroService> stream() { return java.util.stream.Stream.empty(); }
            @Override public java.util.stream.Stream<LogroService> orderedStream() { return java.util.stream.Stream.empty(); }
        }, graciaHoras);
    }

    /** Procesa los eventos FINALIZADOS que aun no tienen resultado. */
    @Transactional
    public void procesarPendientes(LocalDateTime ahora) {
        for (EventoDesastre e : eventoRepo.findByEstadoAndProcesado("FINALIZADO", false)) {
            procesar(e, ahora);
        }
    }

    /**
     * Un evento que termino hace mas que la ventana de gracia (servidor apagado, datos viejos)
     * se marca como procesado sin danar plantas: el dano solo aplica a eventos recien terminados.
     */
    void procesar(EventoDesastre e, LocalDateTime ahora) {
        boolean completo = true;
        if (e.getFechaInicio() != null && (e.getFechaInicio().isAfter(ahora) || !e.getFechaInicio().isBefore(e.getFechaFin()))) {
            // QA-07: FINALIZADO sin haber empezado = cancelado (el panel deja fecha_inicio = fecha_fin). Sin danos ni premios;
            // solo se quita la prevencion (QA-34: antes la quitaba Django sin el orden de candados y habia deadlock).
            log.info("Evento {} cancelado o sin haber empezado: se marca procesado sin danos", e.getIdDesastre());
            completo = porUsuario(e, ahora, true);
        } else if (e.getFechaFin().isBefore(ahora.minus(gracia))) {
            log.info("Evento {} termino hace mas de {} h: se marca procesado sin danos", e.getIdDesastre(), gracia.toHours());
        } else {
            completo = porUsuario(e, ahora, false);
        }
        if (!completo) {
            // Un usuario fallo: el evento queda sin procesar y se reintenta en la siguiente pasada. Los ya procesados se
            // saltan (registros_desastre_usuario marca cada planta), asi nadie cobra ni recibe el golpe dos veces.
            log.warn("Evento {}: quedaron usuarios sin procesar, se reintenta", e.getIdDesastre());
            return;
        }
        e.setProcesado(true);
        e.setUpdatedAt(ahora);
        eventoRepo.save(e);
    }

    /**
     * Recorre a los usuarios afectados en orden de id y procesa a cada uno en su PROPIA transaccion corta (QA-45): antes
     * todo el evento GLOBAL iba en una sola y retenia a todos los usuarios hasta terminar. Orden de candados igual que
     * las acciones (QA-03): usuario y luego sus plantas por id. Devuelve false si algun usuario fallo.
     */
    private boolean porUsuario(EventoDesastre e, LocalDateTime ahora, boolean cancelado) {
        java.util.TreeMap<Integer, List<PlantaUsuario>> porDueno = new java.util.TreeMap<>();
        for (PlantaUsuario p : plantasAfectadas(e)) {
            if (p.getIdUsuario() != null && (!cancelado || Boolean.TRUE.equals(p.getEstaRefugiada()))) {
                porDueno.computeIfAbsent(p.getIdUsuario(), k -> new java.util.ArrayList<>()).add(p);
            }
        }
        int saludFinal = cancelado ? 0 : saludFinalSegunSeveridad(e);
        LocalDateTime limite = ahora.plusHours(parametros.horasRecuperacion());
        int[] total = new int[2];
        boolean completo = true;
        for (Map.Entry<Integer, List<PlantaUsuario>> dueno : porDueno.entrySet()) {
            Integer idUsuario = dueno.getKey();
            try {
                int[] r = enTransaccion(() -> cancelado
                        ? quitarPrevencion(idUsuario, dueno.getValue())
                        : aplicarAUsuario(e, idUsuario, dueno.getValue(), saludFinal, limite, ahora));
                total[0] += r[0];
                total[1] += r[1];
                if (!cancelado) {
                    evaluarLogrosSeguro(idUsuario, ahora);
                }
            } catch (RuntimeException ex) {
                completo = false;
                log.error("Evento {}: no se pudo procesar al usuario {}", e.getIdDesastre(), idUsuario, ex);
            }
        }
        log.info("Evento {} procesado: {} plantas salvadas, {} golpeadas{}", e.getIdDesastre(), total[0], total[1],
                cancelado ? " (cancelado: solo se quito la prevencion)" : "");
        return completo;
    }

    private int[] enTransaccion(java.util.function.Supplier<int[]> trabajo) {
        if (transacciones == null) {
            return trabajo.get(); // pruebas sin Spring
        }
        org.springframework.transaction.support.TransactionTemplate t =
                new org.springframework.transaction.support.TransactionTemplate(transacciones);
        t.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return t.execute(estado -> trabajo.get());
    }

    /** Bloquea al usuario y sus plantas y devuelve sus plantas afectadas leidas de nuevo (valores vigentes). */
    private List<PlantaUsuario> bloquearYLeer(Integer idUsuario, List<PlantaUsuario> afectadas) {
        progreso.bloquearUsuario(idUsuario);
        plantaRepo.bloquearDeUsuario(idUsuario);
        List<PlantaUsuario> plantas = new java.util.ArrayList<>(transacciones == null ? afectadas
                : plantaRepo.findAllById(afectadas.stream().map(PlantaUsuario::getIdPlantaUsuario).toList()));
        plantas.sort(java.util.Comparator.comparing(PlantaUsuario::getIdPlantaUsuario, java.util.Comparator.nullsFirst(Integer::compare)));
        return plantas;
    }

    private int[] quitarPrevencion(Integer idUsuario, List<PlantaUsuario> afectadas) {
        for (PlantaUsuario p : bloquearYLeer(idUsuario, afectadas)) {
            if (Boolean.TRUE.equals(p.getEstaRefugiada())) {
                p.setEstaRefugiada(false);
                p.setRefugiadaDesde(null);
                plantaRepo.save(p);
            }
        }
        return new int[2];
    }

    private int[] aplicarAUsuario(EventoDesastre e, Integer idUsuario, List<PlantaUsuario> afectadas, int saludFinal,
                                  LocalDateTime limite, LocalDateTime ahora) {
        int afectadasAhora = 0;
        int prevenidas = 0;
        int danadas = 0;
        for (PlantaUsuario p : bloquearYLeer(idUsuario, afectadas)) {
            if ("MUERTA".equals(p.getEtapaCrecimiento()) || Boolean.TRUE.equals(p.getEnBaul())
                    || registroRepo.existsByIdDesastreAndIdPlantaUsuario(e.getIdDesastre(), p.getIdPlantaUsuario())) {
                continue;
            }
            RegistroDesastreUsuario r = new RegistroDesastreUsuario();
            r.setIdDesastre(e.getIdDesastre());
            r.setIdPlantaUsuario(p.getIdPlantaUsuario());
            r.setFechaProcesado(ahora);
            afectadasAhora++;
            boolean prevenida = Boolean.TRUE.equals(p.getEstaRefugiada());
            r.setEstabaRefugiada(prevenida);
            if (prevenida) {
                r.setEstadoFinal("SALVADA");
                p.setEstaRefugiada(false);
                p.setRefugiadaDesde(null);
                // El premio por haber prevenido a tiempo se da al terminar el evento
                r.setXpOtorgado(progreso.otorgarSinRacha(p.getIdUsuario(), "REFUGIO", e.getIdDesastre(), ahora).xpGanado());
                prevenidas++;
            } else {
                int salud = p.getSaludPorcentaje() == null ? 100 : p.getSaludPorcentaje();
                int nueva = Math.min(salud, saludFinal);
                r.setDanoSaludAplicado(salud - nueva);
                r.setEstadoFinal("DANADA");
                p.setSaludPorcentaje(nueva);
                // Casi muerta: solo el abono la recupera, y tiene un plazo antes de morir
                p.setRecuperacionHasta(limite);
                p.setGolpeTipo(e.getTipoEvento());
                danadas++;
            }
            // updated_at no se toca: PlantasService.envejecer lo usa para contar las horas de desgaste
            plantaRepo.save(p);
            registroRepo.save(r);
        }
        if (afectadasAhora > 0 && afectadasAhora == prevenidas) {
            progreso.otorgarSinRacha(idUsuario, "EVENTO_COMPLETO", e.getIdDesastre(), ahora);
        }
        return new int[] {prevenidas, danadas};
    }

    private void evaluarLogrosSeguro(Integer userId, LocalDateTime ahora) {
        try {
            LogroService servicio = logroService.getIfAvailable();
            if (servicio != null) {
                servicio.evaluarSeguro(userId, ahora); // tras el commit del cierre del evento
            }
        } catch (RuntimeException e) {
            log.error("No se pudieron evaluar logros tras desastre para el usuario {}", userId, e);
        }
    }

    private List<PlantaUsuario> plantasAfectadas(EventoDesastre e) {
        String region = e.getRegion();
        return region == null || region.isBlank() || "GLOBAL".equals(region)
                ? plantaRepo.findAll()
                : plantaRepo.findDeUsuariosDeRegion(region);
    }

    /** Salud con la que queda una planta que no se previno, segun la severidad del evento. */
    int saludFinalSegunSeveridad(EventoDesastre e) {
        TipoDesastre tipo = e.getIdTipoDesastre() == null ? null : tipoRepo.findById(e.getIdTipoDesastre()).orElse(null);
        String severidad = e.getSeveridad() == null ? "MODERADO" : e.getSeveridad();
        return switch (severidad) {
            case "BAJO" -> tipo != null ? tipo.getSaludFinalBajo() : SALUD_FINAL_BAJO;
            case "CRITICO" -> tipo != null ? tipo.getSaludFinalCritico() : SALUD_FINAL_CRITICO;
            default -> tipo != null ? tipo.getSaludFinalModerado() : SALUD_FINAL_MODERADO;
        };
    }
}
