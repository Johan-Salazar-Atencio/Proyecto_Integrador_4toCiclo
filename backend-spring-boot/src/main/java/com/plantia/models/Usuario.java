package com.plantia.models;

import java.time.LocalDate;
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

    /** Region del usuario (tabla regiones). Si va vacia, la base lo deja en GLOBAL (V25). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_region")
    private Region regionEntidad;

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

    /** Cuidados reales hechos por el usuario (V30): base de los logros RIEGOS/ABONOS/LUZ_TOTAL. */
    @Column(name = "riegos_total", nullable = false)
    private Integer riegosTotal = 0;

    @Column(name = "abonos_total", nullable = false)
    private Integer abonosTotal = 0;

    @Column(name = "luz_total", nullable = false)
    private Integer luzTotal = 0;

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
    public Region getRegionEntidad() { return regionEntidad; }
    public void setRegionEntidad(Region regionEntidad) { this.regionEntidad = regionEntidad; }
    /** Codigo de su region (GLOBAL si no tiene). */
    public String getRegion() { return regionEntidad == null || regionEntidad.getCodigo() == null ? "GLOBAL" : regionEntidad.getCodigo(); }
    public Integer getIdRegion() { return regionEntidad == null ? null : regionEntidad.getIdRegion(); }
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
    public Integer getRiegosTotal() { return riegosTotal; }
    public void setRiegosTotal(Integer riegosTotal) { this.riegosTotal = riegosTotal; }
    public Integer getAbonosTotal() { return abonosTotal; }
    public void setAbonosTotal(Integer abonosTotal) { this.abonosTotal = abonosTotal; }
    public Integer getLuzTotal() { return luzTotal; }
    public void setLuzTotal(Integer luzTotal) { this.luzTotal = luzTotal; }
}
