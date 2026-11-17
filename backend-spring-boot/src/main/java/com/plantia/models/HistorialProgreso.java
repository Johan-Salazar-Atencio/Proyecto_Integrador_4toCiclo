package com.plantia.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "historial_progreso")
public class HistorialProgreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_movimiento")
    private Integer idMovimiento;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "id_regla", nullable = false)
    private Integer idRegla;

    @Column(name = "xp_ganado", nullable = false)
    private Integer xpGanado = 0;

    @Column(name = "monedas_ganadas", nullable = false)
    private Integer monedasGanadas = 0;

    /** Nivel del usuario justo despues de este movimiento. */
    @Column(name = "nivel_resultante", nullable = false)
    private Integer nivelResultante;

    /** Id de lo que origino el movimiento (planta, escaneo, mision...); opcional. */
    @Column(name = "id_referencia")
    private Integer idReferencia;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha = LocalDateTime.now();

    public Integer getIdMovimiento() { return idMovimiento; }
    public void setIdMovimiento(Integer idMovimiento) { this.idMovimiento = idMovimiento; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdRegla() { return idRegla; }
    public void setIdRegla(Integer idRegla) { this.idRegla = idRegla; }
    public Integer getXpGanado() { return xpGanado; }
    public void setXpGanado(Integer xpGanado) { this.xpGanado = xpGanado; }
    public Integer getMonedasGanadas() { return monedasGanadas; }
    public void setMonedasGanadas(Integer monedasGanadas) { this.monedasGanadas = monedasGanadas; }
    public Integer getNivelResultante() { return nivelResultante; }
    public void setNivelResultante(Integer nivelResultante) { this.nivelResultante = nivelResultante; }
    public Integer getIdReferencia() { return idReferencia; }
    public void setIdReferencia(Integer idReferencia) { this.idReferencia = idReferencia; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
}
