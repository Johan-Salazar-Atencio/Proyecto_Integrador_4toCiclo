package com.plantia.models;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "rol")
    private String rol;

    /** SHA-256 del token de sesion (nunca el token); ver util/TokenSesion. */
    @Column(name = "session_token")
    private String sessionToken;

    @Column(name = "session_expira")
    private LocalDateTime sessionExpira;

    @Column(name = "nivel")
    private Integer nivel;

    @Column(name = "experiencia_xp")
    private Integer experienciaXp;

    @Column(name = "monedas")
    private Integer monedas;

    @Column(name = "racha_dias")
    private Integer rachaDias;

    @Column(name = "region")
    private String region;

    /** Llave a regiones; si va vacia, la base la completa desde region (V13). */
    @Column(name = "id_region")
    private Integer idRegion;

    /** Ultimo dia con actividad; base de la racha diaria. */
    @Column(name = "ultima_actividad")
    private LocalDate ultimaActividad;

    @Column(name = "foto_url")
    private String fotoUrl;

    /** Invernadero (articulo de la tienda tipo INVERNADERO) que eligio para su coleccion; null = Selva tropical. */
    @Column(name = "id_invernadero")
    private Integer idInvernadero;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public LocalDateTime getSessionExpira() { return sessionExpira; }
    public void setSessionExpira(LocalDateTime sessionExpira) { this.sessionExpira = sessionExpira; }
    public Integer getNivel() { return nivel; }
    public void setNivel(Integer nivel) { this.nivel = nivel; }
    public Integer getExperienciaXp() { return experienciaXp; }
    public void setExperienciaXp(Integer experienciaXp) { this.experienciaXp = experienciaXp; }
    public Integer getMonedas() { return monedas; }
    public void setMonedas(Integer monedas) { this.monedas = monedas; }
    public Integer getRachaDias() { return rachaDias; }
    public void setRachaDias(Integer rachaDias) { this.rachaDias = rachaDias; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public Integer getIdRegion() { return idRegion; }
    public void setIdRegion(Integer idRegion) { this.idRegion = idRegion; }
    public LocalDate getUltimaActividad() { return ultimaActividad; }
    public void setUltimaActividad(LocalDate ultimaActividad) { this.ultimaActividad = ultimaActividad; }
    public Integer getIdInvernadero() { return idInvernadero; }
    public void setIdInvernadero(Integer idInvernadero) { this.idInvernadero = idInvernadero; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
