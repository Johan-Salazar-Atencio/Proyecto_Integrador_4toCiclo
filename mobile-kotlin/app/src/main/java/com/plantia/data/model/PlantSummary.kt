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
    val activa: Boolean = false,
    /** true mientras la planta esta a resguardo de un desastre. */
    val refugiada: Boolean = false,
    /** Rasgos de la planta adulta que dio la IA (forma y colores); null = dibujo por defecto. */
    val apariencia: Apariencia? = null,
    /** Dias con cuidado contados y dias necesarios para crecer del todo (0 si el servidor no los manda). */
    val diasCuidados: Int = 0,
    val diasTotales: Int = 0
)

/** Apariencia que da la IA al escanear; cada campo ausente toma el valor por defecto del sprite. */
data class Apariencia(
    val forma: String? = null,
    val colorHoja: String? = null,
    val tieneFlor: Boolean? = null,
    val colorFlor: String? = null,
    val tieneFruto: Boolean? = null,
    val colorFruto: String? = null
)
