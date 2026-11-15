package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "misiones")
public class Mision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mision")
    private Integer idMision;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "tipo", nullable = false)
    private String tipo;

    @Column(name = "meta", nullable = false)
    private Integer meta;

    @Column(name = "recompensa_monedas")
    private Integer recompensaMonedas;

    @Column(name = "recompensa_xp", nullable = false)
    private Integer recompensaXp = 0;

    @Column(name = "activa")
    private Boolean activa;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    public Integer getIdMision() { return idMision; }
    public void setIdMision(Integer idMision) { this.idMision = idMision; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getMeta() { return meta; }
    public void setMeta(Integer meta) { this.meta = meta; }
    public Integer getRecompensaMonedas() { return recompensaMonedas; }
    public void setRecompensaMonedas(Integer recompensaMonedas) { this.recompensaMonedas = recompensaMonedas; }
    public Integer getRecompensaXp() { return recompensaXp; }
    public void setRecompensaXp(Integer recompensaXp) { this.recompensaXp = recompensaXp; }
    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
