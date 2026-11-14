package com.plantia.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp

/**
 * Prototipo de animacion vectorial (Canvas) de una Monstera deliciosa.
 * Parecido >= 70% a la planta original (plant-monstera.png).
 * Usa paths vectoriales + animacion de balanceo. Sin imagenes raster.
 *
 * Notas:
 * - Virtualizacion: usar dentro de LazyColumn/LazyGrid con key estable.
 * - Respetar reduced motion: pasar [animar] = false si el sistema lo indica.
 */
@Composable
fun PlantaVectorAnimada(
    modifier: Modifier = Modifier,
    tamaño: Int = 220,
    salud: Float = 1f,
    animar: Boolean = true
) {
    val swayMax = 1.2f + (1f - salud) * 1.5f
    val transition = rememberInfiniteTransition(label = "plantaSway")
    val sway by transition.animateFloat(
        initialValue = -swayMax,
        targetValue = swayMax,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )
    val efectivo = if (animar) sway else 0f

    Canvas(modifier = modifier.size(tamaño.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val baseY = h * 0.82f

        // Maceta
        drawMaceta(cx, baseY, w)
        // Tallo
        rotate(degrees = efectivo * 0.5f, pivot = Offset(cx, baseY)) {
            drawTallo(cx, baseY, h)
        }
        // Hojas
        rotate(degrees = efectivo, pivot = Offset(cx, baseY * 0.55f)) {
            drawHojaIzquierda(cx, baseY, w, h)
            drawHojaDerecha(cx, baseY, w, h)
            drawHojaSuperior(cx, baseY, w, h)
            drawHojaIzqBaja(cx, baseY, w, h)
            drawHojaDerBaja(cx, baseY, w, h)
        }
    }
}

private fun DrawScope.drawMaceta(cx: Float, baseY: Float, w: Float) {
    val path = Path().apply {
        moveTo(cx - w * 0.18f, baseY)
        lineTo(cx - w * 0.22f, baseY + 40f)
        quadraticBezierTo(cx, baseY + 50f, cx + w * 0.22f, baseY + 40f)
        lineTo(cx + w * 0.18f, baseY)
        close()
    }
    drawPath(path, color = Color(0xFF8D6E63))
    drawOval(
        color = Color(0xFF5D4037),
        topLeft = Offset(cx - w * 0.18f, baseY - 6f),
        size = androidx.compose.ui.geometry.Size(w * 0.36f, 12f)
    )
}

private fun DrawScope.drawTallo(cx: Float, baseY: Float, h: Float) {
    val path = Path().apply {
        moveTo(cx, baseY)
        quadraticBezierTo(cx - 2f, baseY - 60f, cx, baseY - 120f)
    }
    drawPath(path, color = Color(0xFF33691E), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f))
}

private fun DrawScope.drawHojaIzquierda(cx: Float, baseY: Float, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(cx, baseY - 90f)
        cubicTo(cx - 40f, baseY - 100f, cx - 70f, baseY - 120f, cx - 80f, baseY - 150f)
        cubicTo(cx - 82f, baseY - 170f, cx - 65f, baseY - 185f, cx - 40f, baseY - 180f)
        cubicTo(cx - 25f, baseY - 177f, cx - 15f, baseY - 165f, cx - 10f, baseY - 150f)
        cubicTo(cx - 5f, baseY - 135f, cx - 2f, baseY - 115f, cx, baseY - 90f)
        close()
    }
    drawPath(path, color = Color(0xFF4CAF50))
    // fenestraciones
    drawOval(color = Color(0xFFF5F5F5), topLeft = Offset(cx - 60f, baseY - 145f), size = androidx.compose.ui.geometry.Size(12f, 18f))
    drawOval(color = Color(0xFFF5F5F5), topLeft = Offset(cx - 40f, baseY - 160f), size = androidx.compose.ui.geometry.Size(10f, 14f))
}

private fun DrawScope.drawHojaDerecha(cx: Float, baseY: Float, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(cx, baseY - 100f)
        cubicTo(cx + 40f, baseY - 110f, cx + 70f, baseY - 130f, cx + 80f, baseY - 160f)
        cubicTo(cx + 82f, baseY - 180f, cx + 65f, baseY - 195f, cx + 40f, baseY - 190f)
        cubicTo(cx + 25f, baseY - 187f, cx + 15f, baseY - 175f, cx + 10f, baseY - 160f)
        cubicTo(cx + 5f, baseY - 145f, cx + 2f, baseY - 125f, cx, baseY - 100f)
        close()
    }
    drawPath(path, color = Color(0xFF4CAF50))
    drawOval(color = Color(0xFFF5F5F5), topLeft = Offset(cx + 48f, baseY - 155f), size = androidx.compose.ui.geometry.Size(12f, 18f))
    drawOval(color = Color(0xFFF5F5F5), topLeft = Offset(cx + 30f, baseY - 170f), size = androidx.compose.ui.geometry.Size(10f, 14f))
}

private fun DrawScope.drawHojaSuperior(cx: Float, baseY: Float, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(cx, baseY - 115f)
        cubicTo(cx - 15f, baseY - 125f, cx - 20f, baseY - 150f, cx - 10f, baseY - 170f)
        cubicTo(cx - 5f, baseY - 185f, cx + 10f, baseY - 190f, cx + 20f, baseY - 175f)
        cubicTo(cx + 28f, baseY - 162f, cx + 22f, baseY - 140f, cx + 15f, baseY - 125f)
        cubicTo(cx + 10f, baseY - 118f, cx + 5f, baseY - 115f, cx, baseY - 115f)
        close()
    }
    drawPath(path, color = Color(0xFF66BB6A))
    drawOval(color = Color(0xFFF5F5F5), topLeft = Offset(cx - 2f, baseY - 160f), size = androidx.compose.ui.geometry.Size(8f, 12f))
}

private fun DrawScope.drawHojaIzqBaja(cx: Float, baseY: Float, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(cx - 5f, baseY - 50f)
        cubicTo(cx - 30f, baseY - 55f, cx - 55f, baseY - 65f, cx - 65f, baseY - 85f)
        cubicTo(cx - 68f, baseY - 98f, cx - 58f, baseY - 108f, cx - 40f, baseY - 105f)
        cubicTo(cx - 28f, baseY - 103f, cx - 18f, baseY - 95f, cx - 12f, baseY - 82f)
        cubicTo(cx - 8f, baseY - 70f, cx - 6f, baseY - 60f, cx - 5f, baseY - 50f)
        close()
    }
    drawPath(path, color = Color(0xFF4CAF50))
    drawOval(color = Color(0xFFF5F5F5), topLeft = Offset(cx - 52f, baseY - 88f), size = androidx.compose.ui.geometry.Size(8f, 12f))
}

private fun DrawScope.drawHojaDerBaja(cx: Float, baseY: Float, w: Float, h: Float) {
    val path = Path().apply {
        moveTo(cx + 5f, baseY - 45f)
        cubicTo(cx + 30f, baseY - 50f, cx + 55f, baseY - 60f, cx + 65f, baseY - 80f)
        cubicTo(cx + 68f, baseY - 93f, cx + 58f, baseY - 103f, cx + 40f, baseY - 100f)
        cubicTo(cx + 28f, baseY - 98f, cx + 18f, baseY - 90f, cx + 12f, baseY - 77f)
        cubicTo(cx + 8f, baseY - 65f, cx + 6f, baseY - 55f, cx + 5f, baseY - 45f)
        close()
    }
    drawPath(path, color = Color(0xFF4CAF50))
    drawOval(color = Color(0xFFF5F5F5), topLeft = Offset(cx + 44f, baseY - 83f), size = androidx.compose.ui.geometry.Size(8f, 12f))
}