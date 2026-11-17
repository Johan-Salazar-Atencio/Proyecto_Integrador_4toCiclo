package com.plantia.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Resultado de un evento para una planta: una sola fila por evento y planta. */
@Entity
@Table(name = "registros_desastre_usuario")
public class RegistroDesastreUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Integer idRegistro;

    @Column(name = "id_desastre", nullable = false)
    private Integer idDesastre;

    @Column(name = "id_planta_usuario", nullable = false)
    private Integer idPlantaUsuario;

    /** SALVADA, DANADA o DESTRUIDA. */
    @Column(name = "estado_final", nullable = false)
    private String estadoFinal;

    @Column(name = "estaba_refugiada", nullable = false)
    private Boolean estabaRefugiada = false;

    @Column(name = "dano_salud_aplicado")
    private Integer danoSaludAplicado = 0;

    @Column(name = "xp_otorgado", nullable = false)
    private Integer xpOtorgado = 0;

    @Column(name = "fecha_procesado")
    private LocalDateTime fechaProcesado = LocalDateTime.now();

    public Integer getIdRegistro() { return idRegistro; }
    public void setIdRegistro(Integer idRegistro) { this.idRegistro = idRegistro; }
    public Integer getIdDesastre() { return idDesastre; }
    public void setIdDesastre(Integer idDesastre) { this.idDesastre = idDesastre; }
    public Integer getIdPlantaUsuario() { return idPlantaUsuario; }
    public void setIdPlantaUsuario(Integer idPlantaUsuario) { this.idPlantaUsuario = idPlantaUsuario; }
    public String getEstadoFinal() { return estadoFinal; }
    public void setEstadoFinal(String estadoFinal) { this.estadoFinal = estadoFinal; }
    public Boolean getEstabaRefugiada() { return estabaRefugiada; }
    public void setEstabaRefugiada(Boolean estabaRefugiada) { this.estabaRefugiada = estabaRefugiada; }
    public Integer getDanoSaludAplicado() { return danoSaludAplicado; }
    public void setDanoSaludAplicado(Integer danoSaludAplicado) { this.danoSaludAplicado = danoSaludAplicado; }
    public Integer getXpOtorgado() { return xpOtorgado; }
    public void setXpOtorgado(Integer xpOtorgado) { this.xpOtorgado = xpOtorgado; }
    public LocalDateTime getFechaProcesado() { return fechaProcesado; }
    public void setFechaProcesado(LocalDateTime fechaProcesado) { this.fechaProcesado = fechaProcesado; }
}
