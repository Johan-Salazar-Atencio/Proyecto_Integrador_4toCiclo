package com.plantia.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fondos_escena")
public class FondoEscena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fondo")
    private Integer idFondo;

    /** DIA, TARDE o NOCHE. */
    @Column(name = "momento", nullable = false)
    private String momento;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    /** Ruta del archivo servido por /files/** (la imagen no se guarda en la base). */
    @Column(name = "imagen_url", nullable = false)
    private String imagenUrl;

    @Column(name = "total_frames", nullable = false)
    private Integer totalFrames = 1;

    @Column(name = "ancho_px")
    private Integer anchoPx;

    @Column(name = "alto_px")
    private Integer altoPx;

    @Column(name = "ms_por_frame", nullable = false)
    private Integer msPorFrame = 200;

    @Column(name = "es_predeterminado", nullable = false)
    private Boolean esPredeterminado = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /** Tema al que pertenece esta escena (cada tema tiene una por momento del dia). */
    @Column(name = "id_tema")
    private Integer idTema;

    /** Vista previa pequena (primer cuadro). */
    @Column(name = "vista_url")
    private String vistaUrl;

    @Column(name = "id_creador")
    private Integer idCreador;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    public Integer getIdFondo() { return idFondo; }
    public void setIdFondo(Integer idFondo) { this.idFondo = idFondo; }
    public String getMomento() { return momento; }
    public void setMomento(String momento) { this.momento = momento; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public Integer getTotalFrames() { return totalFrames; }
    public void setTotalFrames(Integer totalFrames) { this.totalFrames = totalFrames; }
    public Integer getAnchoPx() { return anchoPx; }
    public void setAnchoPx(Integer anchoPx) { this.anchoPx = anchoPx; }
    public Integer getAltoPx() { return altoPx; }
    public void setAltoPx(Integer altoPx) { this.altoPx = altoPx; }
    public Integer getMsPorFrame() { return msPorFrame; }
    public void setMsPorFrame(Integer msPorFrame) { this.msPorFrame = msPorFrame; }
    public Boolean getEsPredeterminado() { return esPredeterminado; }
    public void setEsPredeterminado(Boolean esPredeterminado) { this.esPredeterminado = esPredeterminado; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public Integer getIdTema() { return idTema; }
    public void setIdTema(Integer idTema) { this.idTema = idTema; }
    public String getVistaUrl() { return vistaUrl; }
    public void setVistaUrl(String vistaUrl) { this.vistaUrl = vistaUrl; }
    public Integer getIdCreador() { return idCreador; }
    public void setIdCreador(Integer idCreador) { this.idCreador = idCreador; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
