package com.plantia.data.remote.dto

/** GET /api/v1/catalogo */
data class CatalogoItemDto(
    val id: Int = 0,
    val nombreComun: String = "",
    val nombreCientifico: String = "",
    val frecuenciaRiegoDias: Int = 0,
    val frecuenciaSolHoras: Int = 0,
    val temperaturaMinC: Int? = null,
    val temperaturaMaxC: Int? = null,
    val esToxicaMascotas: Boolean = false,
    val spriteBaseUrl: String = ""
)
