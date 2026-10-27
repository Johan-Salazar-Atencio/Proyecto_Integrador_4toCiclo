package com.plantia.data.remote

/** Item de GET /api/v1/coleccion (plantas escaneadas del usuario). Fechas en ISO-8601. */
data class ColeccionItemDto(
    val id: Int = 0,
    val idCatalogo: Int? = null,
    val nombreComun: String? = null,
    val nombreCientifico: String? = null,
    val fotoUrl: String? = null,
    val vecesVista: Int = 1,
    val primeraVez: String? = null,
    val ultimaVez: String? = null,
    val esToxicaMascotas: Boolean = false
)

/** Respuesta de GET /api/v1/coleccion/{id}: la planta mas sus cuidados. */
data class ColeccionDetalleDto(
    val item: ColeccionItemDto? = null,
    val cuidados: List<CuidadoSeccionDto>? = null
)
