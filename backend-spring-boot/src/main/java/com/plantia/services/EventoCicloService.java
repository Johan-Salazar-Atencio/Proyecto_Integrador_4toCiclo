package com.plantia.services;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plantia.models.EventoDesastre;
import com.plantia.repositories.EventoDesastreRepository;

/**
 * Ciclo de vida de los eventos: PROGRAMADO -> ACTIVO al llegar fecha_inicio y
 * ACTIVO -> FINALIZADO al llegar fecha_fin. Lo invoca EventoScheduler cada pocos segundos.
 */
@Service
public class EventoCicloService {

    private static final Logger log = LoggerFactory.getLogger(EventoCicloService.class);

    private final EventoDesastreRepository eventoRepo;
    private final ResultadoDesastreService resultados;

    public EventoCicloService(EventoDesastreRepository eventoRepo, ResultadoDesastreService resultados) {
        this.eventoRepo = eventoRepo;
        this.resultados = resultados;
    }

    /**
     * Avanza los estados segun la hora indicada, registra el resultado de los eventos terminados
     * y devuelve los que cambiaron a FINALIZADO en esta pasada.
     */
    @Transactional
    public List<EventoDesastre> avanzar(LocalDateTime ahora) {
        activar(ahora);
        List<EventoDesastre> finalizados = finalizar(ahora);
        resultados.procesarPendientes(ahora);
        return finalizados;
    }

    private void activar(LocalDateTime ahora) {
        for (EventoDesastre e : eventoRepo.findByEstadoAndFechaInicioLessThanEqual("PROGRAMADO", ahora)) {
            // Si el servidor estuvo apagado y la ventana ya paso, pasa directo a FINALIZADO
            cambiar(e, e.getFechaFin().isAfter(ahora) ? "ACTIVO" : "FINALIZADO", ahora);
        }
    }

    private List<EventoDesastre> finalizar(LocalDateTime ahora) {
        List<EventoDesastre> vencidos = eventoRepo.findByEstadoAndFechaFinLessThanEqual("ACTIVO", ahora);
        vencidos.forEach(e -> cambiar(e, "FINALIZADO", ahora));
        return vencidos;
    }

    private void cambiar(EventoDesastre e, String estado, LocalDateTime ahora) {
        log.info("Evento {} ({}): {} -> {}", e.getIdDesastre(), e.getTipoEvento(), e.getEstado(), estado);
        e.setEstado(estado);
        e.setUpdatedAt(ahora);
        eventoRepo.save(e);
    }
}
