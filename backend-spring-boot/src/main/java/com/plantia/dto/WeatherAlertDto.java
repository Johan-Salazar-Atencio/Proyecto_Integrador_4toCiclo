package com.plantia.dto;

/**
 * Coincide con WeatherAlert del movil. severity: LOW | MEDIUM | HIGH.
 * disasterType es el codigo del tipo (HUAYCO, HELADA, OLA_DE_CALOR, PLAGA) y phase dice que
 * animacion mostrar: AVISO al comenzar el evento y PICO despues.
 */
public record WeatherAlertDto(
        String title,
        String description,
        String severity,
        String timeRemaining,
        String actionText,
        String disasterType,
        String phase
) {
}
