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
    val phase: String = "PICO",
    /** Nombre del tipo de evento ("Ola de calor"); null si el servidor no lo manda. */
    val eventName: String? = null,
    /** Imagen promocional del evento (URL completa); null si no tiene. */
    val imageUrl: String? = null,
    /** Texto informativo del evento. */
    val info: String? = null,
    /** Consejo de campo que se muestra al prevenir el evento. */
    val tip: String? = null,
    /** Segundos que faltan para que termine; null si no se sabe. */
    val endsInSeconds: Long? = null
)

enum class AlertSeverity {
    LOW, MEDIUM, HIGH
}
