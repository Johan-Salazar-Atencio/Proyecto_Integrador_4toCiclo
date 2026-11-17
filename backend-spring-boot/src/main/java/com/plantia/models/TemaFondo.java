package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Tema de fondo del jardin: trae una escena animada para el dia, otra para la tarde y otra para la noche. */
@Entity
@Table(name = "temas_fondo")
public class TemaFondo {

    @Id
    @Column(name = "id_tema")
    private Integer idTema;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    /** El tema de siempre ("Campo"): lo usa toda planta que no eligio otro. */
    @Column(name = "es_predeterminado")
    private Boolean esPredeterminado = false;

    @Column(name = "activo")
    private Boolean activo = true;

    public Integer getIdTema() { return idTema; }
    public void setIdTema(Integer idTema) { this.idTema = idTema; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getEsPredeterminado() { return esPredeterminado; }
    public void setEsPredeterminado(Boolean esPredeterminado) { this.esPredeterminado = esPredeterminado; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
