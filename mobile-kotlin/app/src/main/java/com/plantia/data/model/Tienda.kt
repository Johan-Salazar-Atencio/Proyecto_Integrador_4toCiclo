package com.plantia.data.model

/** Lo que ve el usuario en la tienda. Las URLs de vista previa ya son absolutas. */
data class Tienda(
    val monedas: Int,
    val nivel: Int,
    val items: List<TiendaItem>
) {
    val fondos: List<TiendaItem> get() = items.filter { it.tipo == TiendaItem.FONDO }
    val abono: TiendaItem? get() = items.firstOrNull { it.tipo == TiendaItem.ABONO }
}

data class TiendaItem(
    val id: Int,
    val nombre: String,
    val tipo: String,
    val descripcion: String,
    val precio: Int,
    val nivelRequerido: Int,
    val previewUrl: String?,
    /** DIA, TARDE o NOCHE: el fondo del jardin al que reemplaza. */
    val momento: String?,
    val comprado: Boolean,
    val equipado: Boolean,
    val bloqueado: Boolean
) {
    companion object {
        const val FONDO = "FONDO"
        const val ABONO = "ABONO"
    }
}
