package com.plantia.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Articulo de la tienda: ABONO (se usa al comprarlo), FONDO (se equipa) o MACETA (aun sin usar). */
@Entity
@Table(name = "items_tienda")
public class ItemTienda {

    @Id
    @Column(name = "id_item")
    private Integer idItem;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "precio_monedas")
    private Integer precioMonedas;

    @Column(name = "nivel_requerido")
    private Integer nivelRequerido = 1;

    /** Vista previa (ruta servida por /files/**). */
    @Column(name = "asset_url")
    private String assetUrl;

    /** Solo para FONDO: el fondo de escena que desbloquea. */
    @Column(name = "id_fondo")
    private Integer idFondo;

    @Column(name = "descripcion")
    private String descripcion;

    /** El admin lo desactiva para quitarlo de la tienda sin perder lo que ya compraron. */
    @Column(name = "activo")
    private Boolean activo = true;

    public Integer getIdItem() { return idItem; }
    public void setIdItem(Integer idItem) { this.idItem = idItem; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public Integer getPrecioMonedas() { return precioMonedas; }
    public void setPrecioMonedas(Integer precioMonedas) { this.precioMonedas = precioMonedas; }
    public Integer getNivelRequerido() { return nivelRequerido; }
    public void setNivelRequerido(Integer nivelRequerido) { this.nivelRequerido = nivelRequerido; }
    public String getAssetUrl() { return assetUrl; }
    public void setAssetUrl(String assetUrl) { this.assetUrl = assetUrl; }
    public Integer getIdFondo() { return idFondo; }
    public void setIdFondo(Integer idFondo) { this.idFondo = idFondo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
