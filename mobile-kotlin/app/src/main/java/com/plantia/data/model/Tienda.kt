package com.plantia.data.model

/** Lo que ve el usuario en la tienda. Las URLs de vista previa ya son absolutas. */
data class Tienda(
    val monedas: Int,
    val nivel: Int,
    val items: List<TiendaItem>
) {
    val fondos: List<TiendaItem> get() = items.filter { it.tipo == TiendaItem.FONDO }
    /** Abonos a la venta, del mas barato al mas caro (medio dia, 1 dia, 3 dias...). */
    val abonos: List<TiendaItem> get() = items.filter { it.tipo == TiendaItem.ABONO }.sortedBy { it.precio }
    val abono: TiendaItem? get() = abonos.firstOrNull()
    val macetas: List<TiendaItem> get() = items.filter { it.tipo == TiendaItem.MACETA }
    val invernaderos: List<TiendaItem> get() = items.filter { it.tipo == TiendaItem.INVERNADERO }
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
    val bloqueado: Boolean,
    /** Macetas e invernaderos: el identificador con el que la app los dibuja (AZUL, VICTORIANO...). */
    val codigo: String? = null,
    /** Solo abonos: cuántos tiene el usuario guardados (inventario). */
    val cantidad: Int = 0
) {
    companion object {
        const val FONDO = "FONDO"
        const val ABONO = "ABONO"
        const val MACETA = "MACETA"
        const val INVERNADERO = "INVERNADERO"
    }
}

/** Un abono del inventario del usuario (`GET /api/v1/abonos`): solo importa cuántos tiene de cada uno. */
data class AbonoInventario(
    val idItem: Int,
    /** ABONO_MEDIO, ABONO_1 o ABONO_3: decide el color y el dibujo. */
    val codigo: String,
    val nombre: String,
    val descripcion: String,
    val precio: Int,
    val cantidad: Int
)

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
