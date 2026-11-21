package com.plantia.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

public record LogroDto(
        Integer idLogro,
        String codigo,
        String titulo,
        String descripcion,
        String icono,
        String rareza,
        int meta,
        int progreso,
        boolean desbloqueado,
        @JsonInclude(JsonInclude.Include.NON_NULL) LocalDateTime desbloqueadoEn,
        boolean secreto,
        int recompensaXp,
        int recompensaMonedas
) {
}
