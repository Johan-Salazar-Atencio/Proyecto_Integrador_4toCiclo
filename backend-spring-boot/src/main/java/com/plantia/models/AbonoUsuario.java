package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Cuantos abonos de un tipo tiene el usuario (se suman al comprar y se descuentan al usarlos). */
@Entity
@Table(name = "abonos_usuario")
public class AbonoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_abono_usuario")
    private Integer idAbonoUsuario;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_item")
    private Integer idItem;

    @Column(name = "cantidad")
    private Integer cantidad = 0;

    public Integer getIdAbonoUsuario() { return idAbonoUsuario; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdItem() { return idItem; }
    public void setIdItem(Integer idItem) { this.idItem = idItem; }
    public Integer getCantidad() { return cantidad; }
    public void setCantidad(Integer cantidad) { this.cantidad = cantidad; }
}
