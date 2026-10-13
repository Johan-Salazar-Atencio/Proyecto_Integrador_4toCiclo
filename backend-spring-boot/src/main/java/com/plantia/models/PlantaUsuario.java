package com.plantia.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "plantas_usuario")
public class PlantaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_planta_usuario")
    private Integer idPlantaUsuario;

    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_catalogo")
    private Integer idCatalogo;

    @Column(name = "apodo")
    private String apodo;

    @Column(name = "salud_porcentaje")
    private Integer saludPorcentaje;

    @Column(name = "hidratacion_porcentaje")
    private Integer hidratacionPorcentaje;

    @Column(name = "sol_porcentaje")
    private Integer solPorcentaje;

    @Column(name = "etapa_crecimiento")
    private String etapaCrecimiento;

    @Column(name = "modo_tiempo")
    private String modoTiempo;

    @Column(name = "esta_refugiada")
    private Boolean estaRefugiada;

    @Column(name = "ultimo_riego")
    private LocalDateTime ultimoRiego;

    @Column(name = "ultimo_sol")
    private LocalDateTime ultimoSol;

    @Column(name = "ultimo_abono")
    private LocalDateTime ultimoAbono;

    @Column(name = "fecha_plantado")
    private LocalDateTime fechaPlantado;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Integer getIdPlantaUsuario() { return idPlantaUsuario; }
    public void setIdPlantaUsuario(Integer idPlantaUsuario) { this.idPlantaUsuario = idPlantaUsuario; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdCatalogo() { return idCatalogo; }
    public void setIdCatalogo(Integer idCatalogo) { this.idCatalogo = idCatalogo; }
    public String getApodo() { return apodo; }
    public void setApodo(String apodo) { this.apodo = apodo; }
    public Integer getSaludPorcentaje() { return saludPorcentaje; }
    public void setSaludPorcentaje(Integer saludPorcentaje) { this.saludPorcentaje = saludPorcentaje; }
    public Integer getHidratacionPorcentaje() { return hidratacionPorcentaje; }
    public void setHidratacionPorcentaje(Integer hidratacionPorcentaje) { this.hidratacionPorcentaje = hidratacionPorcentaje; }
    public Integer getSolPorcentaje() { return solPorcentaje; }
    public void setSolPorcentaje(Integer solPorcentaje) { this.solPorcentaje = solPorcentaje; }
    public String getEtapaCrecimiento() { return etapaCrecimiento; }
    public void setEtapaCrecimiento(String etapaCrecimiento) { this.etapaCrecimiento = etapaCrecimiento; }
    public String getModoTiempo() { return modoTiempo; }
    public void setModoTiempo(String modoTiempo) { this.modoTiempo = modoTiempo; }
    public Boolean getEstaRefugiada() { return estaRefugiada; }
    public void setEstaRefugiada(Boolean estaRefugiada) { this.estaRefugiada = estaRefugiada; }
    public LocalDateTime getUltimoRiego() { return ultimoRiego; }
    public void setUltimoRiego(LocalDateTime ultimoRiego) { this.ultimoRiego = ultimoRiego; }
    public LocalDateTime getUltimoSol() { return ultimoSol; }
    public void setUltimoSol(LocalDateTime ultimoSol) { this.ultimoSol = ultimoSol; }
    public LocalDateTime getUltimoAbono() { return ultimoAbono; }
    public void setUltimoAbono(LocalDateTime ultimoAbono) { this.ultimoAbono = ultimoAbono; }
    public LocalDateTime getFechaPlantado() { return fechaPlantado; }
    public void setFechaPlantado(LocalDateTime fechaPlantado) { this.fechaPlantado = fechaPlantado; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
