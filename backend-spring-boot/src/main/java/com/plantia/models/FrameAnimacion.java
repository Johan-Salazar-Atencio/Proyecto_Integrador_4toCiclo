package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "frames_animacion")
public class FrameAnimacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_frame")
    private Integer idFrame;

    @Column(name = "id_animacion", nullable = false)
    private Integer idAnimacion;

    /** Posicion del frame dentro de la animacion, desde 1. */
    @Column(name = "orden", nullable = false)
    private Integer orden;

    /** Ruta del archivo servido por /files/**. */
    @Column(name = "imagen_url", nullable = false)
    private String imagenUrl;

    public Integer getIdFrame() { return idFrame; }
    public void setIdFrame(Integer idFrame) { this.idFrame = idFrame; }
    public Integer getIdAnimacion() { return idAnimacion; }
    public void setIdAnimacion(Integer idAnimacion) { this.idAnimacion = idAnimacion; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
}
