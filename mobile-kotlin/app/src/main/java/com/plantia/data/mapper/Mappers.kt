package com.plantia.data.mapper

import androidx.compose.ui.graphics.Color
import com.plantia.data.model.AlertSeverity
import com.plantia.data.model.CatalogoItem
import com.plantia.data.model.ColeccionItem
import com.plantia.data.model.Cuidado
import com.plantia.data.model.DailyMission
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.QuickAccessItem
import com.plantia.data.model.RecentScan
import com.plantia.data.model.UserSummary
import com.plantia.data.model.WeatherAlert
import com.plantia.data.remote.dto.CatalogoItemDto
import com.plantia.data.remote.dto.ColeccionItemDto
import com.plantia.data.remote.dto.CuidadoSeccionDto
import com.plantia.data.remote.dto.DailyMissionDto
import com.plantia.data.remote.dto.PlantSummaryDto
import com.plantia.data.remote.dto.QuickAccessDto
import com.plantia.data.remote.dto.RecentScanDto
import com.plantia.data.remote.dto.UserSummaryDto
import com.plantia.data.remote.dto.WeatherAlertDto

/**
 * Traduce los DTOs de Spring a los modelos de dominio que consume la UI.
 *
 * Los modelos de `data.model` son los que ya usan los componentes, asi
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
    imageUrl = imageUrl,
    id = id,
    activa = activa
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

fun ColeccionItemDto.toDomain(): ColeccionItem = ColeccionItem(
    id = id,
    idCatalogo = idCatalogo,
    nombreComun = nombreComun ?: "",
    nombreCientifico = nombreCientifico ?: "",
    fotoUrl = fotoUrl,
    vecesVista = vecesVista,
    primeraVez = primeraVez,
    ultimaVez = ultimaVez,
    esToxicaMascotas = esToxicaMascotas
)

fun CatalogoItemDto.toDomain(): CatalogoItem = CatalogoItem(
    id = id,
    nombreComun = nombreComun,
    nombreCientifico = nombreCientifico,
    frecuenciaRiegoDias = frecuenciaRiegoDias,
    frecuenciaSolHoras = frecuenciaSolHoras,
    temperaturaMinC = temperaturaMinC,
    temperaturaMaxC = temperaturaMaxC,
    esToxicaMascotas = esToxicaMascotas,
    spriteBaseUrl = spriteBaseUrl
)

fun CuidadoSeccionDto.toDomain(): Cuidado = Cuidado(
    id = id,
    tipo = tipo.orEmpty(),
    titulo = titulo.orEmpty(),
    contenido = contenido.orEmpty(),
    orden = orden,
    generadoPorIa = generadoPorIa
)
