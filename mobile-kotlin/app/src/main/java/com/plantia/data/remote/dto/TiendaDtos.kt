package com.plantia.data.remote.dto

/** GET /api/v1/tienda: las monedas y el nivel del usuario, y los articulos activos con su estado. */
data class TiendaDto(
    val monedas: Int = 0,
    val nivel: Int = 1,
    val items: List<TiendaItemDto> = emptyList()
)

data class TiendaItemDto(
    val id: Int = 0,
    val nombre: String = "",
    /** ABONO (se usa al comprarlo) o FONDO (se equipa). */
    val tipo: String = "",
    val descripcion: String = "",
    val precio: Int = 0,
    val nivelRequerido: Int = 1,
    val previewUrl: String? = null,
    /** Solo fondos: DIA, TARDE o NOCHE. */
    val momento: String? = null,
    val comprado: Boolean = false,
    val equipado: Boolean = false,
    val bloqueado: Boolean = false
)
