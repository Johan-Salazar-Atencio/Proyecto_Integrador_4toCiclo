package com.plantia.model

data class WeatherAlert(
    val title: String,
    val description: String,
    val severity: AlertSeverity,
    val timeRemaining: String,
    val actionText: String
)

enum class AlertSeverity {
    LOW, MEDIUM, HIGH
}
