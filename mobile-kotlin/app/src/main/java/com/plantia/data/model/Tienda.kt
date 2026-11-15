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
    /** Solo fondos: la escena de dia, tarde y noche del tema. */
    val variantes: List<VarianteFondo>,
    val comprado: Boolean,
    val bloqueado: Boolean
) {
    companion object {
        const val FONDO = "FONDO"
        const val ABONO = "ABONO"
    }
}

/** Escena de un momento del dia (DIA, TARDE o NOCHE) con su vista previa (URL absoluta). */
data class VarianteFondo(val momento: String, val vistaUrl: String)

/** Tema de fondo que se le puede poner a una planta: el de siempre o uno que compro el usuario. */
data class TemaFondo(
    val id: Int,
    val nombre: String,
    val descripcion: String,
    val predeterminado: Boolean,
    val enUso: Boolean,
    val variantes: List<VarianteFondo>
)
