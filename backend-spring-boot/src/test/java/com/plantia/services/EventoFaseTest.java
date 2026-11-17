package com.plantia.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/** Que animacion de desastre toca segun el avance del evento. */
class EventoFaseTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 15, 10, 0);
    private static final LocalDateTime FIN = INICIO.plusHours(10);

    @Test
    void alComenzarElEventoEsAviso() {
        assertThat(EventoService.fase(INICIO, FIN, INICIO.plusMinutes(5))).isEqualTo("AVISO");
    }

    @Test
    void pasadoElPrimerTramoEsPico() {
        assertThat(EventoService.fase(INICIO, FIN, INICIO.plusHours(5))).isEqualTo("PICO");
    }

    @Test
    void justoEnElLimiteDelTramoYaEsPico() {
        assertThat(EventoService.fase(INICIO, FIN, INICIO.plusHours(4))).isEqualTo("PICO");
    }

    @Test
    void sinFechasFiablesEsPico() {
        assertThat(EventoService.fase(null, FIN, INICIO)).isEqualTo("PICO");
        assertThat(EventoService.fase(INICIO, INICIO, INICIO)).isEqualTo("PICO");
    }
}
