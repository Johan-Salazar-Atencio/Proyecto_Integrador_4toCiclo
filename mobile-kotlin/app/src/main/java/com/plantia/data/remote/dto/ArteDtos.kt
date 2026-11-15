package com.plantia.data.remote.dto

/** GET /api/v1/fondos: tira horizontal de `frames` cuadros de widthPx x heightPx. */
data class FondoDto(
    val id: String = "",
    val momento: String = "",
    val name: String = "",
    val imageUrl: String = "",
    val frames: Int = 1,
    val widthPx: Int = 0,
    val heightPx: Int = 0,
    val msPerFrame: Int = 200,
    val isDefault: Boolean = false,
    /** El fondo de la tienda que el usuario tiene puesto: reemplaza al predeterminado de su momento. */
    val equipado: Boolean = false
)

/** GET /api/v1/animaciones: frames con transparencia para dibujar encima de la escena. */
data class AnimacionDto(
    val disasterType: String = "",
    val phase: String = "",
    val msPerFrame: Int = 150,
    val repeat: Boolean = true,
    val frames: List<String> = emptyList()
)
