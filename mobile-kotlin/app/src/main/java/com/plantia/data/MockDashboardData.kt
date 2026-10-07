package com.plantia.data

import androidx.compose.ui.graphics.Color
import com.plantia.model.AlertSeverity
import com.plantia.model.DailyMission
import com.plantia.model.PlantSummary
import com.plantia.model.QuickAccessItem
import com.plantia.model.UserSummary
import com.plantia.model.WeatherAlert

object MockDashboardData {

    val userSummary = UserSummary(
        name = "Johan",
        level = 5,
        badge = "Cuidador",
        leaves = 320,
        experience = 2450,
        experienceToNextLevel = 3000
    )

    val weatherAlert = WeatherAlert(
        title = "Se aproxima un Huayco Inminente",
        description = "Resguarda las macetas de terraza para evitar exceso de lluvia y danos.",
        severity = AlertSeverity.HIGH,
        timeRemaining = "04h 12m",
        actionText = "Refugiar Planta"
    )

    val plantSummary = PlantSummary(
        name = "Monstera",
        species = "Monstera deliciosa",
        healthPercent = 80,
        humidityPercent = 60,
        lightPercent = 90,
        lastWatered = "Hace 2 dias"
    )

    val quickAccessItems = listOf(
        QuickAccessItem(
            id = "scan",
            title = "Escanear",
            subtitle = "Identificar y cuidar",
            icon = "camera",
            backgroundColor = Color(0xFFE8F8F0),
            iconColor = Color(0xFF22C55E)
        ),
        QuickAccessItem(
            id = "collection",
            title = "Mi Coleccion",
            subtitle = "8 companeras vivas",
            icon = "leaf",
            backgroundColor = Color(0xFFE8F8F0),
            iconColor = Color(0xFF22C55E)
        ),
        QuickAccessItem(
            id = "store",
            title = "Tienda Botanica",
            subtitle = "Macetas & abonos",
            icon = "store",
            backgroundColor = Color(0xFFFFF3E0),
            iconColor = Color(0xFFF59E0B)
        ),
        QuickAccessItem(
            id = "encyclopedia",
            title = "Enciclopedia",
            subtitle = "Guas botanicas",
            icon = "book",
            backgroundColor = Color(0xFFE8F4FD),
            iconColor = Color(0xFF3B82F6)
        )
    )

    val dailyMission = DailyMission(
        id = "mission_001",
        title = "Riego Consciente",
        reward = "+50 gotas de rocio",
        progress = 0.5f,
        currentCount = 1,
        targetCount = 2,
        isCompleted = false
    )
}
