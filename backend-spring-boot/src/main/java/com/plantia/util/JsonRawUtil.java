package com.plantia.util;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilidad para persistir respuestas crudas de la IA en columnas JSONB.
 *
 * Los modelos no siempre devuelven JSON puro: pueden anteponer una frase
 * ("Aqui tienes el analisis: {...}"). PostgreSQL rechaza eso con
 * `invalid input syntax for type json`, por lo que se valida antes de
 * insertar y, si no es JSON valido, se envuelve el texto en un objeto.
 */
public final class JsonRawUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonRawUtil() {
    }

    public static String seguro(String raw) {
        if (raw == null || raw.isBlank()) {
            return "{}";
        }
        try {
            MAPPER.readTree(raw);
            return raw;
        } catch (Exception e) {
            try {
                return MAPPER.writeValueAsString(Map.of("respuesta_cruda", raw));
            } catch (Exception ex) {
                return "{}";
            }
        }
    }
}
