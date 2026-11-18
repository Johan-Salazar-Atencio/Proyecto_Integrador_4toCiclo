package com.plantia.ui.garden.components

import com.plantia.ui.theme.SobrePrimario

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.RojoTexto

/** Frase que se muestra cuando un evento golpeo la planta; el sujeto depende del tipo de evento. */
internal fun textoDelGolpe(tipo: String?): String {
    val sujeto = when (tipo) {
        "HUAYCO" -> "El huayco"
        "HELADA" -> "La helada"
        "OLA_DE_CALOR" -> "La ola de calor"
        "PLAGA" -> "La plaga"
        else -> "El evento"
    }
    return "¡$sujeto arrasó con tu planta!"
}

/**
 * Escudo que se expande desde el centro cuando se previene el evento: cada vez que [disparador] sube
 * (0 = nada que mostrar) se repite el efecto una sola vez, en poco mas de un segundo.
 */
@Composable
fun EfectoProteccion(disparador: Int, modifier: Modifier = Modifier) {
    val progreso = remember { Animatable(1f) } // 1f = terminado: no se dibuja nada
    LaunchedEffect(disparador) {
        if (disparador > 0) {
            progreso.snapTo(0f)
            progreso.animateTo(1f, tween(1_100, easing = FastOutSlowInEasing))
        }
    }
    val p = progreso.value
    if (p >= 1f) return
    val colorEscudo = GreenPrimary
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val radio = size.maxDimension * 0.75f * p
            val alfa = 1f - p
            drawCircle(colorEscudo.copy(alpha = 0.14f * alfa), radius = radio)
            drawCircle(colorEscudo.copy(alpha = 0.6f * alfa), radius = radio, style = Stroke(width = 6.dp.toPx()))
        }
        Box(
            modifier = Modifier
                .size(84.dp)
                .graphicsLayer {
                    val escala = 0.5f + p * 0.9f
                    scaleX = escala
                    scaleY = escala
                    alpha = 1f - p * 0.6f
                }
                .clip(CircleShape)
                .background(GreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Shield, contentDescription = "Evento prevenido", tint = SobrePrimario, modifier = Modifier.size(44.dp))
        }
    }
}

/** Golpe del evento: un tinte rojo que late y el mensaje de que arraso con la planta. Aparece y se va solo. */
@Composable
fun EfectoArraso(activo: Boolean, tipo: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = activo,
        enter = fadeIn(tween(300)),
        exit = fadeOut(tween(700)),
        modifier = modifier
    ) {
        val pulso by rememberInfiniteTransition(label = "arraso").animateFloat(
            initialValue = 0.18f,
            targetValue = 0.45f,
            animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse),
            label = "pulso"
        )
        Box(
            modifier = Modifier.fillMaxSize().background(RojoTexto.copy(alpha = pulso)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = textoDelGolpe(tipo),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(24.dp)
                    .background(Color(0xCC7F1D1D), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}
