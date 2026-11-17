package com.plantia.ui.garden.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.TextPrimary
import kotlin.math.roundToInt

private val RojoCritico = Color(0xFFEF4444)

/** Cada necesidad tiene su color fijo: la vida es roja como el corazon, el agua azul y la luz amarilla. */
internal val ColorVida = Color(0xFFE53935)
internal val ColorAgua = Color(0xFF2196F3)
internal val ColorLuz = Color(0xFFFBC02D)

/** Verde si va bien, ambar si baja, rojo si es critico. */
internal fun colorPorNivel(porcentaje: Int): Color = when {
    porcentaje >= 60 -> GreenPrimary
    porcentaje >= 30 -> AmberAlert
    else -> RojoCritico
}

/**
 * Barra horizontal que se llena desde 0 hasta [porcentaje] al aparecer y que sigue
 * animandose cada vez que el valor cambia (por ejemplo al regar).
 */
@Composable
fun AnimatedStatBar(
    etiqueta: String,
    icono: ImageVector,
    porcentaje: Int,
    modifier: Modifier = Modifier,
    /** Color fijo de la barra; null = verde, ambar o rojo segun el nivel. */
    colorFijo: Color? = null,
    alto: Dp = 10.dp,
    tamanoTexto: TextUnit = 14.sp
) {
    // Arranca en 0 y `LaunchedEffect` lo sube al valor real: de ahi la animacion inicial.
    var objetivo by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(porcentaje) { objetivo = porcentaje.coerceIn(0, 100) / 100f }

    val progreso by animateFloatAsState(
        targetValue = objetivo,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "progreso_$etiqueta"
    )
    val color by animateColorAsState(
        targetValue = colorFijo ?: colorPorNivel(porcentaje),
        animationSpec = tween(500),
        label = "color_$etiqueta"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(if (alto > 12.dp) 22.dp else 18.dp))
            Spacer(Modifier.size(8.dp))
            Text(
                text = etiqueta,
                fontSize = tamanoTexto,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(progreso * 100).roundToInt()}%",
                fontSize = tamanoTexto,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(alto)
                .clip(RoundedCornerShape(50))
                .background(DividerColor)
        ) {
            // El minimo de ancho evita que un valor bajo se vea como una punta deformada
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .then(if (progreso > 0f) Modifier.widthIn(min = alto) else Modifier)
                    .fillMaxWidth(progreso)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
