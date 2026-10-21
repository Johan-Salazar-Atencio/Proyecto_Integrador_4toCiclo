package com.plantia.data

import androidx.compose.ui.graphics.Color
import com.plantia.data.remote.DailyMissionDto
import com.plantia.data.remote.PlantSummaryDto
import com.plantia.data.remote.QuickAccessDto
import com.plantia.data.remote.RecentScanDto
import com.plantia.data.remote.UserSummaryDto
import com.plantia.data.remote.WeatherAlertDto
import com.plantia.model.AlertSeverity
import com.plantia.model.DailyMission
import com.plantia.model.PlantSummary
import com.plantia.model.QuickAccessItem
import com.plantia.model.RecentScan
import com.plantia.model.UserSummary
import com.plantia.model.WeatherAlert

/**
 * Traduce los DTOs de Spring a los modelos de dominio que consume la UI.
 *
 * Los modelos de `com.plantia.model` son los que ya usan los componentes, asi
 * que la app no cambia: solo cambia de donde vienen los datos.
 */

private const val COLOR_POR_DEFECTO = 0xFFE8F8F0.toInt()

/** "#E8F8F0" -> Color. Tolera que venga sin "#" o con formato invalido. */
fun colorDesdeHex(hex: String?): Color {
    val limpio = hex?.trim()?.removePrefix("#").orEmpty()
    if (limpio.length != 6 && limpio.length != 8) return Color(COLOR_POR_DEFECTO)
    return try {
        val valor = limpio.toLong(16)
        // 6 digitos = RGB (alpha FF); 8 digitos = ARGB.
        Color(if (limpio.length == 6) (0xFF000000L or valor).toInt() else valor.toInt())
    } catch (e: NumberFormatException) {
        Color(COLOR_POR_DEFECTO)
    }
}

fun QuickAccessDto.toDomain(): QuickAccessItem = QuickAccessItem(
    id = id,
    title = title,
    subtitle = subtitle,
    icon = icon,
    backgroundColor = colorDesdeHex(backgroundColor),
    iconColor = colorDesdeHex(iconColor)
)

fun UserSummaryDto.toDomain(): UserSummary = UserSummary(
    name = name,
    level = level,
    badge = badge,
    leaves = leaves,
    experience = experience,
    experienceToNextLevel = experienceToNextLevel,
    profileImageUrl = profileImageUrl
)

fun WeatherAlertDto.toDomain(): WeatherAlert = WeatherAlert(
    title = title,
    description = description,
    severity = when (severity.uppercase()) {
        "LOW" -> AlertSeverity.LOW
        "HIGH" -> AlertSeverity.HIGH
        else -> AlertSeverity.MEDIUM
    },
    timeRemaining = timeRemaining,
    actionText = actionText
)

fun PlantSummaryDto.toDomain(): PlantSummary = PlantSummary(
    name = name,
    species = species,
    healthPercent = healthPercent.coerceIn(0, 100),
    humidityPercent = humidityPercent.coerceIn(0, 100),
    lightPercent = lightPercent.coerceIn(0, 100),
    lastWatered = lastWatered,
    imageUrl = imageUrl
)

fun DailyMissionDto.toDomain(): DailyMission = DailyMission(
    id = id,
    title = title,
    reward = reward,
    progress = progress.coerceIn(0f, 1f),
    currentCount = currentCount,
    targetCount = targetCount,
    isCompleted = isCompleted
)

fun RecentScanDto.toDomain(): RecentScan = RecentScan(
    id = id,
    plantName = plantName,
    confidence = confidence,
    statusColor = colorDesdeHex(statusColor)
)
