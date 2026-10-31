package com.plantia.dto;

/** Coincide con PlantSummary del movil. */
public record PlantSummaryDto(
        String name,
        String species,
        int healthPercent,
        int humidityPercent,
        int lightPercent,
        String lastWatered,
        String imageUrl
) {
}
