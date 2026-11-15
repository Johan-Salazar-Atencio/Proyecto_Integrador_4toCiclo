package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Articulo de la tienda (solo lectura por ahora: se usa el ABONO). */
@Entity
@Table(name = "items_tienda")
public class ItemTienda {

    @Id
    @Column(name = "id_item")
    private Integer idItem;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "precio_monedas")
    private Integer precioMonedas;

    public Integer getIdItem() { return idItem; }
    public String getNombre() { return nombre; }
    public String getTipo() { return tipo; }
    public Integer getPrecioMonedas() { return precioMonedas; }
}
