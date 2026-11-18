package com.plantia.ui.scan.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.plantia.ui.theme.PrimarioOscuro

/**
 * Marco del visor: cuatro esquinas lima con puntas redondeadas y una linea lima que sube y baja.
 * Va sobre la camara, por eso el lima es el mismo en claro y en oscuro.
 */
@Composable
fun ScanOverlay(
    modifier: Modifier = Modifier,
    lado: androidx.compose.ui.unit.Dp = 224.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserPosition"
    )

    Box(modifier = modifier.size(lado)) {
        Canvas(Modifier.size(lado)) {
            val grosor = 5.dp.toPx()
            val largo = 44.dp.toPx()
            val radio = 22.dp.toPx()
            val m = grosor / 2f
            val ancho = size.width
            val alto = size.height
            val trazo = Stroke(width = grosor, cap = StrokeCap.Round)

            fun esquina(x: Float, y: Float, dx: Float, dy: Float) {
                // Dos tramos rectos y un arco en la esquina
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x + dx * largo, y)
                    lineTo(x + dx * radio, y)
                    quadraticTo(x, y, x, y + dy * radio)
                    lineTo(x, y + dy * largo)
                }
                drawPath(path, PrimarioOscuro, style = trazo)
            }
            esquina(m, m, 1f, 1f)
            esquina(ancho - m, m, -1f, 1f)
            esquina(m, alto - m, 1f, -1f)
            esquina(ancho - m, alto - m, -1f, -1f)

            // Linea de escaneo
            val y = alto * laserPosition
            drawRoundRect(
                color = PrimarioOscuro.copy(alpha = 0.55f),
                topLeft = Offset(largo, y - 1.dp.toPx()),
                size = Size(ancho - largo * 2, 2.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
    }
}
