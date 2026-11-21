package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "logros")
public class Logro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_logro")
    private Integer idLogro;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    @Column(name = "titulo", nullable = false)
    private String titulo;

    @Column(name = "descripcion", nullable = false)
    private String descripcion;

    @Column(name = "icono", nullable = false)
    private String icono;

    @Column(name = "rareza", nullable = false)
    private String rareza;

    @Column(name = "condicion", nullable = false)
    private String condicion;

    @Column(name = "meta", nullable = false)
    private Integer meta;

    @Column(name = "parametro")
    private String parametro;

    @Column(name = "recompensa_xp", nullable = false)
    private Integer recompensaXp;

    @Column(name = "recompensa_monedas", nullable = false)
    private Integer recompensaMonedas;

    @Column(name = "secreto", nullable = false)
    private Boolean secreto;

    @Column(name = "activo", nullable = false)
    private Boolean activo;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    public Integer getIdLogro() { return idLogro; }
    public void setIdLogro(Integer idLogro) { this.idLogro = idLogro; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getIcono() { return icono; }
    public void setIcono(String icono) { this.icono = icono; }
    public String getRareza() { return rareza; }
    public void setRareza(String rareza) { this.rareza = rareza; }
    public String getCondicion() { return condicion; }
    public void setCondicion(String condicion) { this.condicion = condicion; }
    public Integer getMeta() { return meta; }
    public void setMeta(Integer meta) { this.meta = meta; }
    public String getParametro() { return parametro; }
    public void setParametro(String parametro) { this.parametro = parametro; }
    public Integer getRecompensaXp() { return recompensaXp; }
    public void setRecompensaXp(Integer recompensaXp) { this.recompensaXp = recompensaXp; }
    public Integer getRecompensaMonedas() { return recompensaMonedas; }
    public void setRecompensaMonedas(Integer recompensaMonedas) { this.recompensaMonedas = recompensaMonedas; }
    public Boolean getSecreto() { return secreto; }
    public void setSecreto(Boolean secreto) { this.secreto = secreto; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
}
