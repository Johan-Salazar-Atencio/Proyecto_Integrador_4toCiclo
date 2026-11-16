package com.plantia.data.model

data class DailyMission(
    val id: String,
    val title: String,
    val reward: String,
    val progress: Float,
    val currentCount: Int,
    val targetCount: Int,
    val isCompleted: Boolean,
    /** "EVENT" en la mision del evento activo (tarjeta roja); null en las misiones diarias. */
    val kind: String? = null,
    val eventType: String? = null,
    /** Segundos que faltan para que termine el evento; null en las misiones diarias. */
    val endsInSeconds: Long? = null,
    /** Tipo de una mision diaria (RIEGO, ESCANEO, SOL, ABONO, CUIDADO, DIGITALIZAR, COLECCION, REFUGIO); null en la del evento. */
    val type: String? = null
) {
    val esEvento: Boolean get() = kind == "EVENT"
}
