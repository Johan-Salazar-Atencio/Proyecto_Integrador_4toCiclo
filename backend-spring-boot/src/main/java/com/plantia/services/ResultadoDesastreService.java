package com.plantia.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager em;

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
        if (e.getFechaInicio() != null && (e.getFechaInicio().isAfter(ahora) || !e.getFechaInicio().isBefore(e.getFechaFin()))) {
            // QA-07: FINALIZADO sin haber empezado = cancelado antes de tiempo. Antes danaba a todas las plantas no prevenidas.
            log.info("Evento {} termino sin haber empezado (cancelado): se marca procesado sin danos", e.getIdDesastre());
        } else if (e.getFechaFin().isBefore(ahora.minus(gracia))) {
            log.info("Evento {} termino hace mas de {} h: se marca procesado sin danos", e.getIdDesastre(), gracia.toHours());
        } else {
            aplicar(e, ahora);
        }
        e.setProcesado(true);
        e.setUpdatedAt(ahora);
        eventoRepo.save(e);
    }

    private void aplicar(EventoDesastre e, LocalDateTime ahora) {
        int saludFinal = saludFinalSegunSeveridad(e);
        LocalDateTime limite = ahora.plusHours(parametros.horasRecuperacion());
        int salvadas = 0;
        int danadas = 0;
        // Por usuario: plantas afectadas y cuantas se previnieron (para el premio de evento completo)
        Map<Integer, int[]> porUsuario = new LinkedHashMap<>();
        // Mismo orden de candados que las acciones del usuario (QA-03): primero el usuario y luego sus plantas por id.
        // Antes se recorrian las plantas en cualquier orden y se bloqueaba al usuario despues: con cuidados o
        // "seleccionar" a la vez habia deadlock (500) durante el cierre del evento.
        java.util.TreeMap<Integer, List<PlantaUsuario>> porDueno = new java.util.TreeMap<>();
        for (PlantaUsuario p : plantasAfectadas(e)) {
            if (p.getIdUsuario() != null) {
                porDueno.computeIfAbsent(p.getIdUsuario(), k -> new java.util.ArrayList<>()).add(p);
            }
        }
        List<PlantaUsuario> enOrden = new java.util.ArrayList<>();
        java.util.Set<Integer> bloqueados = new java.util.HashSet<>();
        porDueno.values().forEach(lista -> {
            lista.sort(java.util.Comparator.comparing(PlantaUsuario::getIdPlantaUsuario, java.util.Comparator.nullsFirst(Integer::compare)));
            enOrden.addAll(lista);
        });
        for (PlantaUsuario p : enOrden) {
            if (bloqueados.add(p.getIdUsuario())) {
                progreso.bloquearUsuario(p.getIdUsuario());
                plantaRepo.bloquearDeUsuario(p.getIdUsuario());
            }
            if (em != null && em.contains(p)) {
                em.refresh(p); // valores vigentes tras el candado (un cuidado pudo cambiarla)
            }
            if ("MUERTA".equals(p.getEtapaCrecimiento()) || Boolean.TRUE.equals(p.getEnBaul())
                    || registroRepo.existsByIdDesastreAndIdPlantaUsuario(e.getIdDesastre(), p.getIdPlantaUsuario())) {
                continue;
            }
            RegistroDesastreUsuario r = new RegistroDesastreUsuario();
            r.setIdDesastre(e.getIdDesastre());
            r.setIdPlantaUsuario(p.getIdPlantaUsuario());
            r.setFechaProcesado(ahora);

            int[] cuenta = porUsuario.computeIfAbsent(p.getIdUsuario(), k -> new int[2]);
            cuenta[0]++;
            boolean prevenida = Boolean.TRUE.equals(p.getEstaRefugiada());
            r.setEstabaRefugiada(prevenida);
            if (prevenida) {
                r.setEstadoFinal("SALVADA");
                p.setEstaRefugiada(false);
                p.setRefugiadaDesde(null);
                // El premio por haber prevenido a tiempo se da al terminar el evento
                r.setXpOtorgado(progreso.otorgarSinRacha(p.getIdUsuario(), "REFUGIO", e.getIdDesastre(), ahora).xpGanado());
                cuenta[1]++;
                salvadas++;
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
        porUsuario.forEach((idUsuario, cuenta) -> {
            if (cuenta[0] > 0 && cuenta[0] == cuenta[1]) {
                progreso.otorgarSinRacha(idUsuario, "EVENTO_COMPLETO", e.getIdDesastre(), ahora);
            }
            evaluarLogrosSeguro(idUsuario, ahora);
        });
        log.info("Evento {} procesado: {} plantas salvadas, {} golpeadas", e.getIdDesastre(), salvadas, danadas);
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
