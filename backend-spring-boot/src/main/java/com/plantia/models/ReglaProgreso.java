package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reglas_progreso")
public class ReglaProgreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Integer idRegla;

    /** ESCANEO, ESPECIE_NUEVA, DIGITALIZAR, CUIDAR, MISION, REFUGIO o RACHA. */
    @Column(name = "motivo", nullable = false, unique = true)
    private String motivo;

    @Column(name = "xp", nullable = false)
    private Integer xp = 0;

    @Column(name = "monedas", nullable = false)
    private Integer monedas = 0;

    /** Veces por dia que cuenta la accion; null = sin tope. */
    @Column(name = "tope_diario")
    private Integer topeDiario;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    public Integer getIdRegla() { return idRegla; }
    public void setIdRegla(Integer idRegla) { this.idRegla = idRegla; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public Integer getXp() { return xp; }
    public void setXp(Integer xp) { this.xp = xp; }
    public Integer getMonedas() { return monedas; }
    public void setMonedas(Integer monedas) { this.monedas = monedas; }
    public Integer getTopeDiario() { return topeDiario; }
    public void setTopeDiario(Integer topeDiario) { this.topeDiario = topeDiario; }
    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
}
