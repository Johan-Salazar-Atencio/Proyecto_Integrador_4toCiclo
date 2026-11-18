package com.plantia.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.plantia.data.model.DailyMission
import com.plantia.ui.components.BarraProgreso
import com.plantia.ui.components.tarjeta
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.Evento
import com.plantia.ui.theme.Primario
import com.plantia.ui.theme.SuperficieElevada
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.ui.theme.Tinte

/** Texto de avance de las misiones diarias ("1 de 3 completadas"); la del evento no cuenta aqui. */
internal fun resumenDeMisiones(misiones: List<DailyMission>): String {
    val diarias = misiones.filterNot { it.esEvento }
    return when {
        diarias.isEmpty() -> "Sin misiones diarias hoy"
        else -> "${diarias.count { it.isCompleted }} de ${diarias.size} completadas"
    }
}

/**
 * Tarjeta de misiones del Inicio: solo las tres primeras misiones diarias; "Ver todas" abre una ventana
 * flotante con todas las de hoy (la del evento va primero). Tocar una fila de la ventana lleva a donde se
 * cumple ([onIr] recibe la mision).
 */
@Composable
fun MisionesResumenCard(
    misiones: List<DailyMission>,
    onIr: (DailyMission) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    val diarias = misiones.filterNot { it.esEvento }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .tarjeta()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f)) {
                Text("Misión del día", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                Text(resumenDeMisiones(misiones), fontSize = 11.sp, color = TextSecondary)
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.5.dp, Primario, CircleShape)
                    .clickable { abierto = true }
                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ver todas", fontSize = 11.5.sp, fontWeight = FontWeight.ExtraBold, color = Primario)
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Primario, modifier = Modifier.size(16.dp))
            }
        }
        diarias.take(3).forEach { FilaMision(it) }
    }

    if (abierto) {
        VentanaMisiones(
            misiones = misiones,
            onCerrar = { abierto = false },
            onIr = { abierto = false; onIr(it) }
        )
    }
}

/** Ventana flotante y centrada con todas las misiones de hoy. */
@Composable
private fun VentanaMisiones(misiones: List<DailyMission>, onCerrar: () -> Unit, onIr: (DailyMission) -> Unit) {
    val ordenadas = misiones.sortedByDescending { it.esEvento }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .heightIn(max = 560.dp)
                .tarjeta(RoundedCornerShape(28.dp))
                .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Misiones de hoy", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, modifier = Modifier.weight(1f))
                Surface(
                    onClick = onCerrar,
                    modifier = Modifier.size(34.dp),
                    shape = CircleShape,
                    color = SurfaceWhite,
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = TextPrimary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ordenadas.forEach { mision ->
                    FilaMision(mision, onClick = if (mision.isCompleted) null else ({ onIr(mision) }))
                }
            }
        }
    }
}

/**
 * Una fila de mision: circulo con icono (check si esta completa), titulo (tachado si esta completa), barra de
 * avance o la recompensa, y el contador. La del evento lleva la etiqueta "EVENTO" y el color del evento.
 */
@Composable
fun FilaMision(mision: DailyMission, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val hecha = mision.isCompleted
    val acento = if (mision.esEvento) Evento else Primario
    val fondoIcono = if (mision.esEvento) Evento.copy(alpha = 0.18f) else Tinte
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SuperficieElevada)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(fondoIcono), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when {
                    hecha -> Icons.Filled.Check
                    mision.esEvento -> Icons.Outlined.WarningAmber
                    else -> Icons.Outlined.TrackChanges
                },
                contentDescription = null,
                tint = acento,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (mision.esEvento) {
                Text("EVENTO", fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.4.sp, color = Evento)
            }
            Text(
                mision.title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (hecha) TextPrimary.copy(alpha = 0.6f) else TextPrimary,
                textDecoration = if (hecha) TextDecoration.LineThrough else null
            )
            if (hecha || mision.progress <= 0f) {
                if (mision.reward.isNotBlank()) Text(mision.reward, fontSize = 10.5.sp, color = TextSecondary)
            } else {
                BarraProgreso(progreso = mision.progress, color = acento, alto = 8.dp, pista = SurfaceWhite)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "${mision.currentCount}/${mision.targetCount}",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (hecha) acento else TextSecondary
        )
    }
}
