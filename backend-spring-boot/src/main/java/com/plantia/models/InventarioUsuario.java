package com.plantia.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Lo que el usuario compro (los fondos); esta_equipado marca el que esta en uso. */
@Entity
@Table(name = "inventario_usuario")
public class InventarioUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario")
    private Integer idInventario;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_item")
    private Integer idItem;

    @Column(name = "esta_equipado")
    private Boolean estaEquipado = false;

    @Column(name = "fecha_compra")
    private LocalDateTime fechaCompra = LocalDateTime.now();

    public Integer getIdInventario() { return idInventario; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdItem() { return idItem; }
    public void setIdItem(Integer idItem) { this.idItem = idItem; }
    public Boolean getEstaEquipado() { return estaEquipado; }
    public void setEstaEquipado(Boolean estaEquipado) { this.estaEquipado = estaEquipado; }
    public LocalDateTime getFechaCompra() { return fechaCompra; }
}
