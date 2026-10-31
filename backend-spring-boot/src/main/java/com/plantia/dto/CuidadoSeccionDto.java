package com.plantia.dto;

/** Seccion de la base de conocimiento de cuidados. */
public record CuidadoSeccionDto(
        Integer id,
        String tipo,
        String titulo,
        String contenido,
        int orden,
        boolean generadoPorIa
) {
}
