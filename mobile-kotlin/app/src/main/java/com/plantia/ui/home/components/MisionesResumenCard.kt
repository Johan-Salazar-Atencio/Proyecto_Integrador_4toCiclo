package com.plantia.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.model.DailyMission
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.RojoTexto
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/** Texto de avance de las misiones diarias ("1 de 3 completadas"); la del evento no cuenta aqui. */
internal fun resumenDeMisiones(misiones: List<DailyMission>): String {
    val diarias = misiones.filterNot { it.esEvento }
    return when {
        diarias.isEmpty() -> "Sin misiones diarias hoy"
        else -> "${diarias.count { it.isCompleted }} de ${diarias.size} completadas"
    }
}

/**
 * Una sola tarjeta que lleva a todas las misiones. Con un evento activo muestra la etiqueta roja
 * "Evento", porque ahi esta la mision de prevenirlo.
 */
@Composable
fun MisionesResumenCard(
    misiones: List<DailyMission>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hayEvento = misiones.any { it.esEvento }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.TrackChanges, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Misiones", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(resumenDeMisiones(misiones), fontSize = 13.sp, color = TextSecondary)
            }
            if (hayEvento) {
                Text(
                    "Evento",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(RojoTexto)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
        }
    }
    Spacer(Modifier.height(12.dp))
}
