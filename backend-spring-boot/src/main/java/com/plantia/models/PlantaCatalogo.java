package com.plantia.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plantas_catalogo")
public class PlantaCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_catalogo")
    private Integer idCatalogo;

    @Column(name = "nombre_comun", nullable = false)
    private String nombreComun;

    @Column(name = "nombre_cientifico", nullable = false, unique = true)
    private String nombreCientifico;

    @Column(name = "frecuencia_riego_dias", nullable = false)
    private Integer frecuenciaRiegoDias;

    @Column(name = "frecuencia_sol_horas", nullable = false)
    private Integer frecuenciaSolHoras;

    @Column(name = "temperatura_min_c")
    private Integer temperaturaMinC;

    @Column(name = "temperatura_max_c")
    private Integer temperaturaMaxC;

    @Column(name = "es_toxica_mascotas")
    private Boolean esToxicaMascotas;

    @Column(name = "sprite_base_url", nullable = false)
    private String spriteBaseUrl;

    @Column(name = "etapa_semilla_url")
    private String etapaSemillaUrl;

    @Column(name = "etapa_brote_url")
    private String etapaBroteUrl;

    @Column(name = "etapa_adulta_url")
    private String etapaAdultaUrl;

    @Column(name = "datos_ia_raw", columnDefinition = "jsonb")
    private String datosIaRaw;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(Integer idCatalogo) { this.idCatalogo = idCatalogo; }
    public String getNombreComun() { return nombreComun; }
    public void setNombreComun(String nombreComun) { this.nombreComun = nombreComun; }
    public String getNombreCientifico() { return nombreCientifico; }
    public void setNombreCientifico(String nombreCientifico) { this.nombreCientifico = nombreCientifico; }
    public Integer getFrecuenciaRiegoDias() { return frecuenciaRiegoDias; }
    public void setFrecuenciaRiegoDias(Integer frecuenciaRiegoDias) { this.frecuenciaRiegoDias = frecuenciaRiegoDias; }
    public Integer getFrecuenciaSolHoras() { return frecuenciaSolHoras; }
    public void setFrecuenciaSolHoras(Integer frecuenciaSolHoras) { this.frecuenciaSolHoras = frecuenciaSolHoras; }
    public Integer getTemperaturaMinC() { return temperaturaMinC; }
    public void setTemperaturaMinC(Integer temperaturaMinC) { this.temperaturaMinC = temperaturaMinC; }
    public Integer getTemperaturaMaxC() { return temperaturaMaxC; }
    public void setTemperaturaMaxC(Integer temperaturaMaxC) { this.temperaturaMaxC = temperaturaMaxC; }
    public Boolean getEsToxicaMascotas() { return esToxicaMascotas; }
    public void setEsToxicaMascotas(Boolean esToxicaMascotas) { this.esToxicaMascotas = esToxicaMascotas; }
    public String getSpriteBaseUrl() { return spriteBaseUrl; }
    public void setSpriteBaseUrl(String spriteBaseUrl) { this.spriteBaseUrl = spriteBaseUrl; }
    public String getEtapaSemillaUrl() { return etapaSemillaUrl; }
    public void setEtapaSemillaUrl(String etapaSemillaUrl) { this.etapaSemillaUrl = etapaSemillaUrl; }
    public String getEtapaBroteUrl() { return etapaBroteUrl; }
    public void setEtapaBroteUrl(String etapaBroteUrl) { this.etapaBroteUrl = etapaBroteUrl; }
    public String getEtapaAdultaUrl() { return etapaAdultaUrl; }
    public void setEtapaAdultaUrl(String etapaAdultaUrl) { this.etapaAdultaUrl = etapaAdultaUrl; }
    public String getDatosIaRaw() { return datosIaRaw; }
    public void setDatosIaRaw(String datosIaRaw) { this.datosIaRaw = datosIaRaw; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
