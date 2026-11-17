package com.plantia.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tipos_desastre")
public class TipoDesastre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_desastre")
    private Integer idTipoDesastre;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "icono_url")
    private String iconoUrl;

    @Column(name = "color_hex", nullable = false)
    private String colorHex = "#888888";

    /** Salud que pierde una planta sin refugio, segun la severidad del evento. */
    @Column(name = "dano_bajo", nullable = false)
    private Integer danoBajo = 10;

    @Column(name = "dano_moderado", nullable = false)
    private Integer danoModerado = 25;

    @Column(name = "dano_critico", nullable = false)
    private Integer danoCritico = 50;

    @Column(name = "imagen_url")
    private String imagenUrl;

    /** Consejo corto de campo que se muestra al prevenir el evento. */
    @Column(name = "consejo")
    private String consejo;

    /** Salud con la que queda una planta que no se previno, segun la severidad del evento. */
    @Column(name = "salud_final_bajo", nullable = false)
    private Integer saludFinalBajo = 40;

    @Column(name = "salud_final_moderado", nullable = false)
    private Integer saludFinalModerado = 20;

    @Column(name = "salud_final_critico", nullable = false)
    private Integer saludFinalCritico = 10;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "id_creador")
    private Integer idCreador;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Integer getIdTipoDesastre() { return idTipoDesastre; }
    public void setIdTipoDesastre(Integer idTipoDesastre) { this.idTipoDesastre = idTipoDesastre; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getIconoUrl() { return iconoUrl; }
    public void setIconoUrl(String iconoUrl) { this.iconoUrl = iconoUrl; }
    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }
    public Integer getDanoBajo() { return danoBajo; }
    public void setDanoBajo(Integer danoBajo) { this.danoBajo = danoBajo; }
    public Integer getDanoModerado() { return danoModerado; }
    public void setDanoModerado(Integer danoModerado) { this.danoModerado = danoModerado; }
    public Integer getDanoCritico() { return danoCritico; }
    public void setDanoCritico(Integer danoCritico) { this.danoCritico = danoCritico; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public String getConsejo() { return consejo; }
    public void setConsejo(String consejo) { this.consejo = consejo; }
    public Integer getSaludFinalBajo() { return saludFinalBajo; }
    public void setSaludFinalBajo(Integer v) { this.saludFinalBajo = v; }
    public Integer getSaludFinalModerado() { return saludFinalModerado; }
    public void setSaludFinalModerado(Integer v) { this.saludFinalModerado = v; }
    public Integer getSaludFinalCritico() { return saludFinalCritico; }
    public void setSaludFinalCritico(Integer v) { this.saludFinalCritico = v; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public Integer getIdCreador() { return idCreador; }
    public void setIdCreador(Integer idCreador) { this.idCreador = idCreador; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
