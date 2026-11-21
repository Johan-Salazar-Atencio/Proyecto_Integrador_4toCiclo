package com.plantia.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "logros_usuario")
public class LogroUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_logro_usuario")
    private Integer idLogroUsuario;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "id_logro", nullable = false)
    private Integer idLogro;

    @Column(name = "progreso", nullable = false)
    private Integer progreso;

    @Column(name = "desbloqueado_en")
    private LocalDateTime desbloqueadoEn;

    @Column(name = "notificado", nullable = false)
    private Boolean notificado;

    public Integer getIdLogroUsuario() { return idLogroUsuario; }
    public void setIdLogroUsuario(Integer idLogroUsuario) { this.idLogroUsuario = idLogroUsuario; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public Integer getIdLogro() { return idLogro; }
    public void setIdLogro(Integer idLogro) { this.idLogro = idLogro; }
    public Integer getProgreso() { return progreso; }
    public void setProgreso(Integer progreso) { this.progreso = progreso; }
    public LocalDateTime getDesbloqueadoEn() { return desbloqueadoEn; }
    public void setDesbloqueadoEn(LocalDateTime desbloqueadoEn) { this.desbloqueadoEn = desbloqueadoEn; }
    public Boolean getNotificado() { return notificado; }
    public void setNotificado(Boolean notificado) { this.notificado = notificado; }
}
