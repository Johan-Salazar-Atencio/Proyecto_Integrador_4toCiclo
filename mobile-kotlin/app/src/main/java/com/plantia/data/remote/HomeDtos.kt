package com.plantia.data.remote

import retrofit2.Response

/**
 * Contrato de respuesta de Spring Boot para la pantalla Home.
 *
 * Los nombres de campo replican exactamente los `record` de Java para que
 * Gson los deserialice sin anotaciones. Los endpoints que pueden devolver
 * 204 (sin contenido) se invocan con `Response<...>` para poder distinguir
 * "vacio" de "error".
 */

/** GET /api/v1/accesos */
data class QuickAccessDto(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val icon: String = "",
    val backgroundColor: String = "",
    val iconColor: String = ""
)

/** GET /api/v1/usuarios/me */
data class UserSummaryDto(
    val name: String = "",
    val level: Int = 0,
    val badge: String = "",
    val leaves: Int = 0,
    val experience: Int = 0,
    val experienceToNextLevel: Int = 0,
    val profileImageUrl: String? = null
)

/** GET /api/v1/eventos/activo (204 si no hay desastre activo) */
data class WeatherAlertDto(
    val title: String = "",
    val description: String = "",
    val severity: String = "MEDIUM",
    val timeRemaining: String = "",
    val actionText: String = ""
)

/** GET /api/v1/plantas/activa (204 si el usuario no tiene plantas) */
data class PlantSummaryDto(
    val name: String = "",
    val species: String = "",
    val healthPercent: Int = 0,
    val humidityPercent: Int = 0,
    val lightPercent: Int = 0,
    val lastWatered: String = "",
    val imageUrl: String? = null
)

/** GET /api/v1/misiones/diaria (204 si no hay misiones activas) */
data class DailyMissionDto(
    val id: String = "",
    val title: String = "",
    val reward: String = "",
    val progress: Float = 0f,
    val currentCount: Int = 0,
    val targetCount: Int = 0,
    val isCompleted: Boolean = false
)
