package com.plantia.dto;

import java.util.List;

/** Detalle de un item de la coleccion con sus cuidados. */
public record ColeccionDetalleDto(
        ColeccionItemDto item,
        List<CuidadoSeccionDto> cuidados
) {
}
