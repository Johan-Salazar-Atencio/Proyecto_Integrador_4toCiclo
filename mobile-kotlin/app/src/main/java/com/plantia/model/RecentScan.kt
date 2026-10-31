package com.plantia.model

import androidx.compose.ui.graphics.Color

/** Resumen de un escaneo para la tira de escaneos recientes. */
data class RecentScan(
    val id: String,
    val plantName: String,
    val confidence: String,
    val statusColor: Color = Color(0xFF10B981)
)
