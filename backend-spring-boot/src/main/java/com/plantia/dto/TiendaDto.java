package com.plantia.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/** Lo que ve el usuario en la tienda: sus monedas y nivel, y los articulos activos con su estado. */
public record TiendaDto(int monedas, int nivel, List<Item> items) {

    /**
     * @param variantes  solo fondos: las escenas de dia, tarde y noche del tema, con su vista previa
     * @param comprado   siempre false para el abono (se compra muchas veces: ver cantidad)
     * @param cantidad   solo abonos: cuantos tiene el usuario en su inventario (0 en los demas articulos)
     * @param bloqueado  true si falta nivel
     * @param codigo     macetas e invernaderos: el identificador con el que la app dibuja el articulo
     */
    public record Item(int id, String nombre, String tipo, String descripcion, int precio, int nivelRequerido,
                       String previewUrl, List<TemaFondoDto.Variante> variantes, boolean comprado, boolean bloqueado,
                       @JsonInclude(JsonInclude.Include.NON_NULL) String codigo, int cantidad) {
    }
}
