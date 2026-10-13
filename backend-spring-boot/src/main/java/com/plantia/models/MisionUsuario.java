package com.plantia.models;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "misiones_usuario")
public class MisionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mision_usuario")
    private Integer idMisionUsuario;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "id_mision", nullable = false)
    private Integer idMision;

    @Column(name = "progreso")
    private Integer progreso;

    @Column(name = "completada")
    private Boolean completada;

    @Column(name = "fecha")
    private LocalDate fecha;

    public Integer getIdMisionUsuario() { return idMisionUsuario; }
    public void setIdMisionUsuario(Integer idMisionUsuario) { this.idMisionUsuario = idMisionUsuario; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdMision() { return idMision; }
    public void setIdMision(Integer idMision) { this.idMision = idMision; }
    public Integer getProgreso() { return progreso; }
    public void setProgreso(Integer progreso) { this.progreso = progreso; }
    public Boolean getCompletada() { return completada; }
    public void setCompletada(Boolean completada) { this.completada = completada; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
}
