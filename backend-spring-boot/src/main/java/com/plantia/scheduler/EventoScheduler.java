package com.plantia.scheduler;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.plantia.services.EventoCicloService;

/** Tarea periodica que activa y finaliza los eventos de desastre. */
@Component
public class EventoScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventoScheduler.class);

    private final EventoCicloService ciclo;

    public EventoScheduler(EventoCicloService ciclo) {
        this.ciclo = ciclo;
    }

    @Scheduled(fixedDelayString = "${app.eventos.intervalo-ms:30000}", initialDelayString = "${app.eventos.intervalo-ms:30000}")
    public void avanzarEventos() {
        try {
            ciclo.avanzar(LocalDateTime.now());
        } catch (RuntimeException e) {
            // Un fallo no debe cancelar las siguientes ejecuciones
            log.error("No se pudo avanzar el ciclo de eventos", e);
        }
    }
}
