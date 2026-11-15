package com.plantia.services;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Cuando un evento termina, cada planta afectada queda SALVADA (estaba refugiada),
 * DANADA (pierde salud segun el tipo y la severidad) o DESTRUIDA (la salud llega a 0).
 */
@Service
public class ResultadoDesastreService {

    private static final Logger log = LoggerFactory.getLogger(ResultadoDesastreService.class);

    static final int DANO_BAJO = 10;
    static final int DANO_MODERADO = 25;
    static final int DANO_CRITICO = 50;

    private final EventoDesastreRepository eventoRepo;
    private final PlantaUsuarioRepository plantaRepo;
    private final RegistroDesastreUsuarioRepository registroRepo;
    private final TipoDesastreRepository tipoRepo;
    private final Duration gracia;

    public ResultadoDesastreService(EventoDesastreRepository eventoRepo,
                                    PlantaUsuarioRepository plantaRepo,
                                    RegistroDesastreUsuarioRepository registroRepo,
                                    TipoDesastreRepository tipoRepo,
                                    @Value("${app.eventos.gracia-horas:6}") long graciaHoras) {
        this.eventoRepo = eventoRepo;
        this.plantaRepo = plantaRepo;
        this.registroRepo = registroRepo;
        this.tipoRepo = tipoRepo;
        this.gracia = Duration.ofHours(graciaHoras);
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
        if (e.getFechaFin().isBefore(ahora.minus(gracia))) {
            log.info("Evento {} termino hace mas de {} h: se marca procesado sin danos", e.getIdDesastre(), gracia.toHours());
        } else {
            aplicar(e, ahora);
        }
        e.setProcesado(true);
        e.setUpdatedAt(ahora);
        eventoRepo.save(e);
    }

    private void aplicar(EventoDesastre e, LocalDateTime ahora) {
        int dano = danoSegunSeveridad(e);
        int salvadas = 0;
        int danadas = 0;
        for (PlantaUsuario p : plantasAfectadas(e)) {
            if ("MUERTA".equals(p.getEtapaCrecimiento())
                    || registroRepo.existsByIdDesastreAndIdPlantaUsuario(e.getIdDesastre(), p.getIdPlantaUsuario())) {
                continue;
            }
            RegistroDesastreUsuario r = new RegistroDesastreUsuario();
            r.setIdDesastre(e.getIdDesastre());
            r.setIdPlantaUsuario(p.getIdPlantaUsuario());
            r.setFechaProcesado(ahora);

            boolean refugiada = Boolean.TRUE.equals(p.getEstaRefugiada());
            r.setEstabaRefugiada(refugiada);
            if (refugiada) {
                r.setEstadoFinal("SALVADA");
                p.setEstaRefugiada(false);
                p.setRefugiadaDesde(null);
                salvadas++;
            } else {
                int salud = p.getSaludPorcentaje() == null ? 100 : p.getSaludPorcentaje();
                int nueva = Math.max(0, salud - dano);
                r.setDanoSaludAplicado(salud - nueva);
                r.setEstadoFinal(nueva == 0 ? "DESTRUIDA" : "DANADA");
                p.setSaludPorcentaje(nueva);
                danadas++;
            }
            p.setUpdatedAt(ahora);
            plantaRepo.save(p);
            registroRepo.save(r);
        }
        log.info("Evento {} procesado: {} plantas salvadas, {} afectadas", e.getIdDesastre(), salvadas, danadas);
    }

    private List<PlantaUsuario> plantasAfectadas(EventoDesastre e) {
        String region = e.getRegion();
        return region == null || region.isBlank() || "GLOBAL".equals(region)
                ? plantaRepo.findAll()
                : plantaRepo.findDeUsuariosDeRegion(region);
    }

    int danoSegunSeveridad(EventoDesastre e) {
        TipoDesastre tipo = e.getIdTipoDesastre() == null ? null : tipoRepo.findById(e.getIdTipoDesastre()).orElse(null);
        String severidad = e.getSeveridad() == null ? "MODERADO" : e.getSeveridad();
        return switch (severidad) {
            case "BAJO" -> tipo != null ? tipo.getDanoBajo() : DANO_BAJO;
            case "CRITICO" -> tipo != null ? tipo.getDanoCritico() : DANO_CRITICO;
            default -> tipo != null ? tipo.getDanoModerado() : DANO_MODERADO;
        };
    }
}
