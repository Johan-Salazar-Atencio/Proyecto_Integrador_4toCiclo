package com.plantia.dto;

/** Item del catalogo para el listado / busqueda. */
public record CatalogoItemDto(
        Integer id,
        String nombreComun,
        String nombreCientifico,
        int frecuenciaRiegoDias,
        int frecuenciaSolHoras,
        Integer temperaturaMinC,
        Integer temperaturaMaxC,
        boolean esToxicaMascotas,
        String spriteBaseUrl
) {
}
