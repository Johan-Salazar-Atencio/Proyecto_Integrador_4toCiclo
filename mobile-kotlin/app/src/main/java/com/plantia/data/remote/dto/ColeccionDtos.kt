package com.plantia.data.remote.dto

/** GET /api/v1/coleccion */
data class ColeccionItemDto(
    val id: Int = 0,
    val idCatalogo: Int? = null,
    val nombreComun: String = "",
    val nombreCientifico: String = "",
    val fotoUrl: String? = null,
    val vecesVista: Int = 0,
    val primeraVez: String? = null,
    val ultimaVez: String? = null,
    val esToxicaMascotas: Boolean = false
)

/** GET /api/v1/coleccion/{id} */
data class ColeccionDetalleDto(
    val item: ColeccionItemDto = ColeccionItemDto(),
    val cuidados: List<CuidadoSeccionDto> = emptyList()
)
