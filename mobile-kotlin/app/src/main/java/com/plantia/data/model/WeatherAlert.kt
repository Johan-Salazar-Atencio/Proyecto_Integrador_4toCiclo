package com.plantia.data.model

data class WeatherAlert(
    val title: String,
    val description: String,
    val severity: AlertSeverity,
    val timeRemaining: String,
    val actionText: String,
    /** Codigo del tipo de desastre (HUAYCO, HELADA, OLA_DE_CALOR, PLAGA); vacio si el servidor no lo envia. */
    val disasterType: String = "",
    /** AVISO al comenzar el evento y PICO despues. */
    val phase: String = "PICO"
)

enum class AlertSeverity {
    LOW, MEDIUM, HIGH
}
