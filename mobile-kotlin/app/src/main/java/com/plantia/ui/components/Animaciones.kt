package com.plantia.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Entrada escalonada: el elemento aparece con fundido y sube un poco. Al ponerle un
 * [retardoMs] distinto a cada elemento se logra el efecto de cascada.
 */
@Composable
fun Modifier.entradaEscalonada(
    visible: Boolean,
    retardoMs: Int,
    desplazamiento: Dp = 28.dp,
    escalaInicial: Float = 1f
): Modifier {
    val progreso by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 550, delayMillis = retardoMs, easing = FastOutSlowInEasing),
        label = "entrada"
    )
    val px = with(LocalDensity.current) { desplazamiento.toPx() }
    return this.graphicsLayer {
        alpha = progreso
        translationY = (1f - progreso) * px
        val escala = escalaInicial + (1f - escalaInicial) * progreso
        scaleX = escala
        scaleY = escala
    }
}

/** Micro-animacion de botones: se encogen un poco al presionar y rebotan al soltar. */
@Composable
fun Modifier.escalaAlPresionar(
    interaccion: InteractionSource,
    escalaPresionado: Float = 0.96f
): Modifier {
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) escalaPresionado else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "escala_boton"
    )
    return this.scale(escala)
}
