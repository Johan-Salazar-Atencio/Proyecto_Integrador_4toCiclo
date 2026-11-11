package com.plantia.dto;

import java.util.List;
import java.util.Map;

/**
 * Contrato de salida normalizado del motor IA (Django).
 * Proveedores: GPT-4o / Gemini / local.
 *
 * {@code apariencia}: rasgos visuales para dibujar la planta en el jardin virtual
 * (forma, colorHoja, tieneFlor, colorFlor, tieneFruto, colorFruto). Puede ser null.
 */
public record IaResultDto(
        String nombreComun,
        String nombreCientifico,
        Double confianza,
        List<IaCuidadoDto> cuidados,
        String datosRaw,
        Map<String, Object> apariencia
) {
    /** Compatibilidad: resultado sin apariencia (respaldo cuando la IA no responde). */
    public IaResultDto(String nombreComun, String nombreCientifico, Double confianza,
                       List<IaCuidadoDto> cuidados, String datosRaw) {
        this(nombreComun, nombreCientifico, confianza, cuidados, datosRaw, null);
    }

    public record IaCuidadoDto(String tipo, String titulo, String contenido) {
    }
}
