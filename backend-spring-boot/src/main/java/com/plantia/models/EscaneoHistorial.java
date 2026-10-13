package com.plantia.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "escaneos_historial")
public class EscaneoHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_escaneo")
    private Integer idEscaneo;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_catalogo")
    private Integer idCatalogo;

    @Column(name = "imagen_foto_url", nullable = false)
    private String imagenFotoUrl;

    @Column(name = "confianza_ia", precision = 5, scale = 2)
    private BigDecimal confianzaIa;

    @Column(name = "respuesta_ia_raw", columnDefinition = "jsonb", nullable = false)
    private String respuestaIaRaw;

    @Column(name = "fecha_escaneo")
    private LocalDateTime fechaEscaneo;

    public Integer getIdEscaneo() { return idEscaneo; }
    public void setIdEscaneo(Integer idEscaneo) { this.idEscaneo = idEscaneo; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(Integer idCatalogo) { this.idCatalogo = idCatalogo; }
    public String getImagenFotoUrl() { return imagenFotoUrl; }
    public void setImagenFotoUrl(String imagenFotoUrl) { this.imagenFotoUrl = imagenFotoUrl; }
    public BigDecimal getConfianzaIa() { return confianzaIa; }
    public void setConfianzaIa(BigDecimal confianzaIa) { this.confianzaIa = confianzaIa; }
    public String getRespuestaIaRaw() { return respuestaIaRaw; }
    public void setRespuestaIaRaw(String respuestaIaRaw) { this.respuestaIaRaw = respuestaIaRaw; }
    public LocalDateTime getFechaEscaneo() { return fechaEscaneo; }
    public void setFechaEscaneo(LocalDateTime fechaEscaneo) { this.fechaEscaneo = fechaEscaneo; }
}
