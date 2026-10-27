package com.plantia.dto;

import java.util.List;

/** Respuesta del POST /api/v1/escaneos. */
public record ScanResultDto(
        Integer idEscaneo,
        Integer idCatalogo,
        String nombreComun,
        String nombreCientifico,
        Double confianza,
        String imagenUrl,
        boolean nuevoEnColeccion,
        List<CuidadoSeccionDto> cuidados
) {
}
