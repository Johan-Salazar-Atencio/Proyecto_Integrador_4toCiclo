package com.plantia.dto;

import java.util.List;

/** Lo que ve el usuario en la tienda: sus monedas y nivel, y los articulos activos con su estado. */
public record TiendaDto(int monedas, int nivel, List<Item> items) {

    /**
     * @param variantes  solo fondos: las escenas de dia, tarde y noche del tema, con su vista previa
     * @param comprado   siempre false para el abono, que se usa en el momento
     * @param bloqueado  true si falta nivel
     */
    public record Item(int id, String nombre, String tipo, String descripcion, int precio, int nivelRequerido,
                       String previewUrl, List<TemaFondoDto.Variante> variantes, boolean comprado, boolean bloqueado) {
    }
}
