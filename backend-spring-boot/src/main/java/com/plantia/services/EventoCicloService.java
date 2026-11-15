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

    public EventoCicloService(EventoDesastreRepository eventoRepo) {
        this.eventoRepo = eventoRepo;
    }

    /** Avanza los estados segun la hora indicada y devuelve los eventos que cambiaron a FINALIZADO. */
    @Transactional
    public List<EventoDesastre> avanzar(LocalDateTime ahora) {
        activar(ahora);
        return finalizar(ahora);
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
