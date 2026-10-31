package com.plantia.dto;

import java.util.List;

/**
 * Contrato de salida normalizado del motor IA (Django).
 * Proveedores: GPT-4o / Gemini / local.
 */
public record IaResultDto(
        String nombreComun,
        String nombreCientifico,
        Double confianza,
        List<IaCuidadoDto> cuidados,
        String datosRaw
) {
    public record IaCuidadoDto(String tipo, String titulo, String contenido) {
    }
}
