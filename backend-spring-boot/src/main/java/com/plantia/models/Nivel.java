package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "niveles")
public class Nivel {

    @Id
    @Column(name = "nivel")
    private Integer nivel;

    @Column(name = "xp_requerido", nullable = false)
    private Integer xpRequerido;

    @Column(name = "recompensa_monedas")
    private Integer recompensaMonedas;

    public Integer getNivel() { return nivel; }
    public void setNivel(Integer nivel) { this.nivel = nivel; }
    public Integer getXpRequerido() { return xpRequerido; }
    public void setXpRequerido(Integer xpRequerido) { this.xpRequerido = xpRequerido; }
    public Integer getRecompensaMonedas() { return recompensaMonedas; }
    public void setRecompensaMonedas(Integer recompensaMonedas) { this.recompensaMonedas = recompensaMonedas; }
}
