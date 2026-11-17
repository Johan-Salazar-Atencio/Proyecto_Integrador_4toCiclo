package com.plantia.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.model.UserSummary
import com.plantia.ui.components.BarraProgreso
import com.plantia.ui.components.FotoPerfil
import com.plantia.ui.components.tarjeta
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.Primario
import com.plantia.ui.theme.SuperficieElevada
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.ui.theme.Tinte

/** Iniciales para el avatar cuando no hay foto: "Johan Demo" -> "JD", "Lucia" -> "L". */
internal fun iniciales(nombre: String): String =
    nombre.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/**
 * Barra superior del Inicio, como tarjeta: avatar con anillo del avance de XP, saludo, insignia y nivel, la
 * pildora de hojas (abre la tienda) y, debajo, la barra de XP hacia el siguiente nivel.
 */
@Composable
fun UserHeaderCard(
    user: UserSummary,
    onHojas: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val progreso by animateFloatAsState(user.progresoNivel, tween(600), label = "xp-home")
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .tarjeta()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarConAnillo(user, progreso)
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "¡Hola, ${user.name}!",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${user.badge} · Nivel ${user.level}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Primario,
                    maxLines = 1,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(CircleShape)
                        .background(Tinte)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
            Surface(
                onClick = onHojas,
                shape = CircleShape,
                color = SuperficieElevada,
                border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Outlined.Eco, contentDescription = "Hojas", tint = Primario, modifier = Modifier.size(18.dp))
                    Text("${user.leaves}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                }
            }
        }
        BarraProgreso(progreso = progreso, color = Primario, alto = 8.dp, pista = SuperficieElevada)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = if (user.esNivelMaximo) "Nivel máximo · ${user.experience} XP"
                else "${user.experience} / ${user.experienceToNextLevel} XP para el nivel ${user.level + 1}",
                fontSize = 10.5.sp,
                color = TextSecondary
            )
            Text("${(progreso * 100).toInt()} %", fontSize = 10.5.sp, color = TextSecondary)
        }
    }
}

/** Avatar de 46dp con un anillo que se llena segun el avance del nivel. */
@Composable
private fun AvatarConAnillo(user: UserSummary, progreso: Float) {
    val colorAnillo = Primario
    val colorPista = DividerColor
    Box(modifier = Modifier.size(46.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(46.dp)) {
            val trazo = 4.dp.toPx()
            val lado = size.minDimension - trazo
            val origen = androidx.compose.ui.geometry.Offset(trazo / 2, trazo / 2)
            drawArc(colorPista, 0f, 360f, false, origen, Size(lado, lado), style = Stroke(trazo))
            drawArc(colorAnillo, -90f, 360f * progreso, false, origen, Size(lado, lado), style = Stroke(trazo, cap = StrokeCap.Round))
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SuperficieElevada),
            contentAlignment = Alignment.Center
        ) {
            val textoIniciales: @Composable () -> Unit = {
                Text(iniciales(user.name), color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
            }
            if (user.profileImageUrl.isNullOrBlank()) textoIniciales()
            else FotoPerfil(user.profileImageUrl, "Foto de ${user.name}", respaldo = textoIniciales)
        }
    }
}
