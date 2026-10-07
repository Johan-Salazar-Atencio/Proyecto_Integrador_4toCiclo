package com.plantia.model

data class PlantSummary(
    val name: String,
    val species: String,
    val healthPercent: Int,
    val humidityPercent: Int,
    val lightPercent: Int,
    val lastWatered: String,
    val imageUrl: String? = null
)
