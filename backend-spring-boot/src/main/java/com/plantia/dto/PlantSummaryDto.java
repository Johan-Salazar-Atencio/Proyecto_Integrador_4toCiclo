package com.plantia.dto;

import java.util.Map;

/**
 * Coincide con PlantSummary del movil (los 7 primeros campos). Los demas los usa
 * el jardin de la web; el movil los ignora.
 */
public record PlantSummaryDto(
        String name,
        String species,
        int healthPercent,
        int humidityPercent,
        int lightPercent,
        String lastWatered,
        String imageUrl,
        Integer id,
        int crecimiento,
        Map<String, Object> apariencia,
        String fotoUrl,
        boolean activa
) {
    /** Solo los campos del contrato con el movil. */
    public PlantSummaryDto(String name, String species, int healthPercent, int humidityPercent,
                           int lightPercent, String lastWatered, String imageUrl) {
        this(name, species, healthPercent, humidityPercent, lightPercent, lastWatered, imageUrl,
                null, 0, null, null, false);
    }
}
