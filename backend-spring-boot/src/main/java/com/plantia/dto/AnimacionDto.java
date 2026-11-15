package com.plantia.dto;

import java.util.List;

/**
 * Animacion de un desastre en una fase (AVISO o PICO): una lista de frames con transparencia
 * que se dibujan encima de la escena. disasterType es el codigo del tipo (HUAYCO, OLA_DE_CALOR...).
 */
public record AnimacionDto(
        String disasterType,
        String phase,
        int msPerFrame,
        boolean repeat,
        List<String> frames
) {
}
