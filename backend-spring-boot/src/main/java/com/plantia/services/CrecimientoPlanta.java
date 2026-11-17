package com.plantia.services;

import java.util.Map;

/**
 * Reglas de crecimiento por dias con cuidado. La duracion total sale de la forma que
 * da la IA al escanear: plantas pequenas 5 dias, medianas 7 y grandes 12.
 * Las fases usan los nombres de etapa que ya exige la BD: SEMILLA, BROTE y ADULTA.
 */
final class CrecimientoPlanta {

    static final int DIAS_PEQUENA = 5;
    static final int DIAS_MEDIANA = 7;
    static final int DIAS_GRANDE = 12;
    /** La segunda fase empieza al 35 % de los dias totales (redondeado hacia arriba). */
    private static final double INICIO_CRECIMIENTO = 0.35;

    private CrecimientoPlanta() { }

    /** Dias totales segun la forma; sin forma conocida se toma como mediana. */
    static int diasTotales(String forma) {
        if (forma == null) {
            return DIAS_MEDIANA;
        }
        return switch (forma.toLowerCase()) {
            case "suculenta", "cactus", "roseta", "helecho" -> DIAS_PEQUENA;
            case "palmera", "arbol" -> DIAS_GRANDE;
            default -> DIAS_MEDIANA;
        };
    }

    static int diasTotales(Map<String, Object> apariencia) {
        Object forma = apariencia != null ? apariencia.get("forma") : null;
        return diasTotales(forma instanceof String s ? s : null);
    }

    static String faseSegun(int dias, int total) {
        if (dias >= total) {
            return "ADULTA";
        }
        return dias >= (int) Math.ceil(total * INICIO_CRECIMIENTO) ? "BROTE" : "SEMILLA";
    }

    /** Orden de las fases, para que una planta nunca retroceda de fase. */
    static int rango(String fase) {
        return switch (fase == null ? "" : fase) {
            case "ADULTA" -> 2;
            case "BROTE" -> 1;
            default -> 0;
        };
    }
}
