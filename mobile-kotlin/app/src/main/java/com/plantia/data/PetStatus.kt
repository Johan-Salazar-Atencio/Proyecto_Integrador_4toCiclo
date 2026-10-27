package com.plantia.data

/** Estado de la mascota/planta virtual; porcentajes 0..100 para las barras. */
data class PetStatus(
    val plantId: String,
    val nickname: String,
    val healthPercent: Int,
    val waterPercent: Int,
    val lightPercent: Int
)
