package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "animaciones_desastre")
public class AnimacionDesastre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_animacion")
    private Integer idAnimacion;

    @Column(name = "id_tipo_desastre", nullable = false)
    private Integer idTipoDesastre;

    /** AVISO, PICO, FINAL, REFUGIO o DANO. */
    @Column(name = "fase", nullable = false)
    private String fase;

    @Column(name = "ms_por_frame", nullable = false)
    private Integer msPorFrame = 150;

    @Column(name = "repetir", nullable = false)
    private Boolean repetir = true;

    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    public Integer getIdAnimacion() { return idAnimacion; }
    public void setIdAnimacion(Integer idAnimacion) { this.idAnimacion = idAnimacion; }
    public Integer getIdTipoDesastre() { return idTipoDesastre; }
    public void setIdTipoDesastre(Integer idTipoDesastre) { this.idTipoDesastre = idTipoDesastre; }
    public String getFase() { return fase; }
    public void setFase(String fase) { this.fase = fase; }
    public Integer getMsPorFrame() { return msPorFrame; }
    public void setMsPorFrame(Integer msPorFrame) { this.msPorFrame = msPorFrame; }
    public Boolean getRepetir() { return repetir; }
    public void setRepetir(Boolean repetir) { this.repetir = repetir; }
    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
}
