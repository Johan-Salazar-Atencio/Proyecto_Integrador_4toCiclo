package com.plantia.dto;

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
        int rachaDias
) {
}
