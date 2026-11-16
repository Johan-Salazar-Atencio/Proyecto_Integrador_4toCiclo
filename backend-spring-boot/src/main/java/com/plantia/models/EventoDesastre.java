package com.plantia.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "eventos_desastre")
public class EventoDesastre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_desastre")
    private Integer idDesastre;

    @Column(name = "id_creador")
    private Integer idCreador;

    /** Tipo de evento (tabla tipos_desastre) y zona a la que afecta (tabla regiones; GLOBAL si va vacia, V25). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_desastre")
    private TipoDesastre tipoDesastre;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_region")
    private Region regionEntidad;

    /** true cuando el scheduler ya registro el resultado en las plantas. */
    @Column(name = "procesado", nullable = false)
    private Boolean procesado = false;

    @Column(name = "mensaje_notificacion", nullable = false)
    private String mensajeNotificacion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDateTime fechaFin;

    @Column(name = "estado")
    private String estado;

    @Column(name = "severidad")
    private String severidad;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getIdDesastre() { return idDesastre; }
    public void setIdDesastre(Integer idDesastre) { this.idDesastre = idDesastre; }
    public Integer getIdCreador() { return idCreador; }
    public void setIdCreador(Integer idCreador) { this.idCreador = idCreador; }
    public TipoDesastre getTipoDesastre() { return tipoDesastre; }
    public void setTipoDesastre(TipoDesastre tipoDesastre) { this.tipoDesastre = tipoDesastre; }
    public Region getRegionEntidad() { return regionEntidad; }
    public void setRegionEntidad(Region regionEntidad) { this.regionEntidad = regionEntidad; }

    /** Codigo del tipo (HUAYCO, HELADA...) o null si el evento no tiene tipo. */
    public String getTipoEvento() { return tipoDesastre == null ? null : tipoDesastre.getCodigo(); }
    public Integer getIdTipoDesastre() { return tipoDesastre == null ? null : tipoDesastre.getIdTipoDesastre(); }
    /** Codigo de la region a la que afecta (GLOBAL si no tiene). */
    public String getRegion() { return regionEntidad == null || regionEntidad.getCodigo() == null ? "GLOBAL" : regionEntidad.getCodigo(); }
    public Integer getIdRegion() { return regionEntidad == null ? null : regionEntidad.getIdRegion(); }

    // Atajos solo para armar eventos en las pruebas: el codigo corriente nunca crea eventos (los crea el admin en Django).
    public void setTipoEvento(String codigo) { tipoTransitorio().setCodigo(codigo); }
    public void setIdTipoDesastre(Integer id) {
        if (id == null) { this.tipoDesastre = null; } else { tipoTransitorio().setIdTipoDesastre(id); }
    }
    public void setRegion(String codigo) {
        if (this.regionEntidad == null) { this.regionEntidad = new Region(); }
        this.regionEntidad.setCodigo(codigo);
    }

    private TipoDesastre tipoTransitorio() {
        if (this.tipoDesastre == null) { this.tipoDesastre = new TipoDesastre(); }
        return this.tipoDesastre;
    }
    public Boolean getProcesado() { return procesado; }
    public void setProcesado(Boolean procesado) { this.procesado = procesado; }
    public String getMensajeNotificacion() { return mensajeNotificacion; }
    public void setMensajeNotificacion(String mensajeNotificacion) { this.mensajeNotificacion = mensajeNotificacion; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getSeveridad() { return severidad; }
    public void setSeveridad(String severidad) { this.severidad = severidad; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
