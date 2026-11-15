package com.plantia.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

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
        boolean activa,
        /** XP y nivel ganados con la accion (digitalizar, cuidar); no se envia en las consultas. */
        @JsonInclude(JsonInclude.Include.NON_NULL) ProgresoDto progreso
) {
    /** Sin progreso: consultas del jardin. */
    public PlantSummaryDto(String name, String species, int healthPercent, int humidityPercent,
                           int lightPercent, String lastWatered, String imageUrl, Integer id,
                           int crecimiento, Map<String, Object> apariencia, String fotoUrl, boolean activa) {
        this(name, species, healthPercent, humidityPercent, lightPercent, lastWatered, imageUrl,
                id, crecimiento, apariencia, fotoUrl, activa, null);
    }

    /** Solo los campos del contrato con el movil. */
    public PlantSummaryDto(String name, String species, int healthPercent, int humidityPercent,
                           int lightPercent, String lastWatered, String imageUrl) {
        this(name, species, healthPercent, humidityPercent, lightPercent, lastWatered, imageUrl,
                null, 0, null, null, false, null);
    }

    public PlantSummaryDto conProgreso(ProgresoDto progreso) {
        return new PlantSummaryDto(name, species, healthPercent, humidityPercent, lightPercent, lastWatered,
                imageUrl, id, crecimiento, apariencia, fotoUrl, activa, progreso);
    }
}
