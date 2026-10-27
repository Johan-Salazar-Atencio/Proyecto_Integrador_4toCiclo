package com.plantia.models;

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
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
