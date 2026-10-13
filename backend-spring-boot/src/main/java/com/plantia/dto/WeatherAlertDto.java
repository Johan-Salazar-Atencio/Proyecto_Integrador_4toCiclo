package com.plantia.dto;

/** Coincide con WeatherAlert del movil. severity: LOW | MEDIUM | HIGH. */
public record WeatherAlertDto(
        String title,
        String description,
        String severity,
        String timeRemaining,
        String actionText
) {
}
