package com.plantia.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "coleccion_usuario",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_usuario", "id_catalogo"}))
public class ColeccionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_coleccion")
    private Integer idColeccion;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "id_catalogo")
    private Integer idCatalogo;

    @Column(name = "id_escaneo")
    private Integer idEscaneo;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "veces_vista")
    private Integer vecesVista;

    @Column(name = "primera_vez")
    private LocalDateTime primeraVez;

    @Column(name = "ultima_vez")
    private LocalDateTime ultimaVez;

    public Integer getIdColeccion() { return idColeccion; }
    public void setIdColeccion(Integer idColeccion) { this.idColeccion = idColeccion; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(Integer idCatalogo) { this.idCatalogo = idCatalogo; }
    public Integer getIdEscaneo() { return idEscaneo; }
    public void setIdEscaneo(Integer idEscaneo) { this.idEscaneo = idEscaneo; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public Integer getVecesVista() { return vecesVista; }
    public void setVecesVista(Integer vecesVista) { this.vecesVista = vecesVista; }
    public LocalDateTime getPrimeraVez() { return primeraVez; }
    public void setPrimeraVez(LocalDateTime primeraVez) { this.primeraVez = primeraVez; }
    public LocalDateTime getUltimaVez() { return ultimaVez; }
    public void setUltimaVez(LocalDateTime ultimaVez) { this.ultimaVez = ultimaVez; }
}
