package com.plantia.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Lo que ganó el usuario con una acción. Las respuestas de escaneo, digitalizar y cuidar lo
 * incluyen para que las apps muestren XP y la subida de nivel sin otra consulta.
 */
public record ProgresoDto(
        int xpGanado,
        int monedasGanadas,
        int nivel,
        boolean subioDeNivel,
        int experienciaXp,
        int rachaDias,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<LogroDesbloqueadoDto> logrosNuevos
) {
    public ProgresoDto(int xpGanado, int monedasGanadas, int nivel, boolean subioDeNivel,
                       int experienciaXp, int rachaDias) {
        this(xpGanado, monedasGanadas, nivel, subioDeNivel, experienciaXp, rachaDias, List.of());
    }

    public ProgresoDto conLogrosNuevos(List<LogroDesbloqueadoDto> logrosNuevos) {
        return new ProgresoDto(xpGanado, monedasGanadas, nivel, subioDeNivel, experienciaXp, rachaDias,
                logrosNuevos == null ? List.of() : logrosNuevos);
    }
}
