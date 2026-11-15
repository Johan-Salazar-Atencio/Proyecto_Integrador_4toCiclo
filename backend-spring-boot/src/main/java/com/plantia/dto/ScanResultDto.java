package com.plantia.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Respuesta del POST /api/v1/escaneos. */
public record ScanResultDto(
        Integer idEscaneo,
        Integer idCatalogo,
        String nombreComun,
        String nombreCientifico,
        Double confianza,
        String imagenUrl,
        boolean nuevoEnColeccion,
        List<CuidadoSeccionDto> cuidados,
        boolean identificado,
        /** Rasgos visuales de la IA para el jardin virtual; null si no los hay. */
        Map<String, Object> apariencia,
        /** XP y nivel ganados con el escaneo; no se envia si no hubo progreso. */
        @JsonInclude(JsonInclude.Include.NON_NULL) ProgresoDto progreso
) {
    /** Compatibilidad: respuesta sin apariencia. */
    public ScanResultDto(Integer idEscaneo, Integer idCatalogo, String nombreComun,
                         String nombreCientifico, Double confianza, String imagenUrl,
                         boolean nuevoEnColeccion, List<CuidadoSeccionDto> cuidados,
                         boolean identificado) {
        this(idEscaneo, idCatalogo, nombreComun, nombreCientifico, confianza, imagenUrl,
                nuevoEnColeccion, cuidados, identificado, null, null);
    }

    /** Compatibilidad: respuesta sin progreso. */
    public ScanResultDto(Integer idEscaneo, Integer idCatalogo, String nombreComun,
                         String nombreCientifico, Double confianza, String imagenUrl,
                         boolean nuevoEnColeccion, List<CuidadoSeccionDto> cuidados,
                         boolean identificado, Map<String, Object> apariencia) {
        this(idEscaneo, idCatalogo, nombreComun, nombreCientifico, confianza, imagenUrl,
                nuevoEnColeccion, cuidados, identificado, apariencia, null);
    }
}
