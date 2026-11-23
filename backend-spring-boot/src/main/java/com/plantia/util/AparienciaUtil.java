package com.plantia.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Apariencia de la planta (rasgos para dibujarla): solo se guardan los campos conocidos, con textos cortos y
 * booleanos (QA-12). La decide la IA al escanear; nunca el cliente.
 */
public final class AparienciaUtil {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> TEXTOS = Set.of("forma", "colorHoja", "colorFlor", "colorFruto");
    private static final Set<String> BOOLEANOS = Set.of("tieneFlor", "tieneFruto");
    static final int MAX_TEXTO = 40;

    private AparienciaUtil() { }

    /** Copia solo los campos validos; null si no queda ninguno. */
    public static Map<String, Object> limpiar(Map<String, Object> apariencia) {
        if (apariencia == null) {
            return null;
        }
        Map<String, Object> limpia = new LinkedHashMap<>();
        apariencia.forEach((k, v) -> {
            if (TEXTOS.contains(k) && v instanceof String s && !s.isBlank() && s.length() <= MAX_TEXTO) {
                limpia.put(k, s.trim());
            } else if (BOOLEANOS.contains(k) && v instanceof Boolean b) {
                limpia.put(k, b);
            }
        });
        return limpia.isEmpty() ? null : limpia;
    }

    /** La apariencia limpia como JSON para guardar; null si no hay. */
    public static String aJson(Map<String, Object> apariencia) {
        Map<String, Object> limpia = limpiar(apariencia);
        if (limpia == null) {
            return null;
        }
        try {
            return JSON.writeValueAsString(limpia);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
