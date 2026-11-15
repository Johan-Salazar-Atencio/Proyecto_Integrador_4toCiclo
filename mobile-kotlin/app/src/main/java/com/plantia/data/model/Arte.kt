package com.plantia.data.model

/** Fondo animado de la escena: una tira horizontal de [frames] cuadros. Las URLs ya son absolutas. */
data class FondoEscena(
    val momento: String,
    val imageUrl: String,
    val frames: Int,
    val frameWidthPx: Int,
    val frameHeightPx: Int,
    val msPerFrame: Int,
    val esPredeterminado: Boolean,
    /** Comprado en la tienda y puesto por el usuario. */
    val equipado: Boolean = false
)

/** Animacion de un desastre en una fase: frames con transparencia, en orden. */
data class AnimacionDesastre(
    val disasterType: String,
    val phase: String,
    val msPerFrame: Int,
    val frames: List<String>
)

/** Todo el arte que dibuja la escena. */
data class Arte(
    val fondos: List<FondoEscena>,
    val animaciones: List<AnimacionDesastre>
)
