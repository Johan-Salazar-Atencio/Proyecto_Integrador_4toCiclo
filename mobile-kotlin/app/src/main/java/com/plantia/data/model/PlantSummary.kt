package com.plantia.data.model

data class PlantSummary(
    val name: String,
    val species: String,
    val healthPercent: Int,
    val humidityPercent: Int,
    val lightPercent: Int,
    val lastWatered: String,
    val imageUrl: String? = null,
    /** Id de la planta digital en el backend; null en datos de ejemplo. */
    val id: Int? = null,
    /** Planta que se muestra en el jardin. */
    val activa: Boolean = false
)
