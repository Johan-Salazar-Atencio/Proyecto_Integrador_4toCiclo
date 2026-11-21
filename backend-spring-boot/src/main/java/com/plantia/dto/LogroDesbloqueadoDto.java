package com.plantia.dto;

public record LogroDesbloqueadoDto(
        Integer idLogro,
        String codigo,
        String titulo,
        String descripcion,
        String icono,
        String rareza,
        int recompensaXp,
        int recompensaMonedas
) {
}
