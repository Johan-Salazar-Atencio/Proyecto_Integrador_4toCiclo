package com.plantia.data.remote.dto

/** GET /api/v1/regiones */
data class RegionDto(
    val code: String = "",
    val name: String = ""
)

/** Cuerpo de PUT /api/v1/usuarios/me */
data class CambiarRegionRequest(val region: String)

/** Cuerpo de PUT /api/v1/usuarios/me/nombre */
data class CambiarNombreRequest(val nombre: String)
