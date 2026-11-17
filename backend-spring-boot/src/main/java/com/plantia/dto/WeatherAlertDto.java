package com.plantia.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Coincide con WeatherAlert del movil. severity: LOW | MEDIUM | HIGH.
 * disasterType es el codigo del tipo (HUAYCO, HELADA, OLA_DE_CALOR, PLAGA) y phase dice que
 * animacion mostrar: AVISO al comenzar el evento y PICO despues.
 * Los cinco ultimos campos alimentan la tarjeta del evento en el Inicio y se omiten si no hay dato:
 * nombre del tipo, imagen, texto informativo, consejo de campo y segundos que faltan.
 */
public record WeatherAlertDto(
        String title,
        String description,
        String severity,
        String timeRemaining,
        String actionText,
        String disasterType,
        String phase,
        @JsonInclude(JsonInclude.Include.NON_NULL) String eventName,
        @JsonInclude(JsonInclude.Include.NON_NULL) String imageUrl,
        @JsonInclude(JsonInclude.Include.NON_NULL) String info,
        @JsonInclude(JsonInclude.Include.NON_NULL) String tip,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long endsInSeconds
) {
    /** Solo los campos del contrato original con el movil. */
    public WeatherAlertDto(String title, String description, String severity, String timeRemaining,
                           String actionText, String disasterType, String phase) {
        this(title, description, severity, timeRemaining, actionText, disasterType, phase,
                null, null, null, null, null);
    }
}
