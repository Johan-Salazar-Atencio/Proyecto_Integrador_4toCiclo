package com.plantia.dto;

import java.util.List;

/**
 * Inventario de abonos del usuario: sus monedas y un renglon por tipo de abono (los activos en la tienda y los
 * que aun tiene aunque ya no se vendan), con cuantos le quedan.
 *
 * @param codigo     ABONO_MEDIO (suave), ABONO_1 (organico) o ABONO_3 (reforzado): con el se elige el color/dibujo
 * @param mediosDias medios dias de crecimiento que suma (1, 2 o 6)
 * @param cantidad   existencias del usuario (0 si no tiene)
 */
public record AbonosDto(int monedas, List<Abono> abonos) {

    public record Abono(int idItem, String codigo, String nombre, String descripcion, int precio, int mediosDias,
                        String previewUrl, int cantidad) {
    }
}
