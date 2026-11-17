package com.plantia.dto;

import java.util.List;

/**
 * Un tema de fondo que el usuario puede poner a su planta (el predeterminado o uno que compro).
 *
 * @param enUso true si es el que tiene la planta
 */
public record TemaFondoDto(int id, String nombre, String descripcion, boolean predeterminado, boolean enUso,
                           List<Variante> variantes) {

    /** Escena de un momento del dia (DIA, TARDE o NOCHE) con su vista previa. */
    public record Variante(String momento, String vistaUrl) {
    }
}
