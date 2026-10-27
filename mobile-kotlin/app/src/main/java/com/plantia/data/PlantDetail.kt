package com.plantia.data

/** Nivel de cuidado de una necesidad (riego, luz, etc.) con su valor 0f..1f. */
data class CareLevel(
    val label: String,
    val level: Float,
    val recommendation: String
)

/** Resultado de IA mostrado en la pantalla de detalle de una planta. */
data class PlantDetail(
    val id: String,
    val photoUrl: String? = null,
    val commonName: String,
    val species: String,
    val confidence: Float,
    val diagnosis: String,
    val isHealthy: Boolean,
    val careLevels: List<CareLevel>
)
