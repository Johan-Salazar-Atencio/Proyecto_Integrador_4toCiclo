package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Valor del juego que el admin puede ajustar (costo de revivir, horas de recuperacion, etc.). */
@Entity
@Table(name = "parametros_juego")
public class ParametroJuego {

    @Id
    @Column(name = "clave")
    private String clave;

    @Column(name = "valor", nullable = false)
    private Integer valor;

    @Column(name = "descripcion")
    private String descripcion;

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public Integer getValor() { return valor; }
    public void setValor(Integer valor) { this.valor = valor; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
