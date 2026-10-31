package com.plantia.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.plantia.ui.theme.SurfaceWhite

private val TerracotaOscura = Color(0xFFD9825B)
private val TerracotaClara = Color(0xFFE79A6E)
private val Tierra = Color(0xFF5B4636)
private val TierraClara = Color(0xFF6E5441)
private val VerdeBulbo = Color(0xFFBFE27A)
private val VerdeHoja = Color(0xFF7FC241)
private val VerdeHojaClara = Color(0xFFA4D65E)
private val Tinta = Color(0xFF2B2B2B)

/**
 * Brote simpatico dentro de una maceta, dibujado con vectores (sin imagenes).
 * Se usa en Welcome y en el acceso para dar identidad a la app.
 */
@Composable
fun BroteEnMaceta(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.aspectRatio(1f)) {
        val w = size.width
        val h = size.height
        fun x(v: Float) = v * w
        fun y(v: Float) = v * h

        // Sombra en el suelo
        drawOval(Color(0x22000000), topLeft = Offset(x(.22f), y(.905f)), size = Size(x(.56f), y(.05f)))

        // Cuerpo de la maceta (trapecio con base redondeada)
        val cuerpo = Path().apply {
            moveTo(x(.27f), y(.68f))
            lineTo(x(.73f), y(.68f))
            lineTo(x(.675f), y(.90f))
            quadraticBezierTo(x(.665f), y(.935f), x(.63f), y(.935f))
            lineTo(x(.37f), y(.935f))
            quadraticBezierTo(x(.335f), y(.935f), x(.325f), y(.90f))
            close()
        }
        drawPath(cuerpo, TerracotaOscura)

        // Borde superior
        drawRoundRect(
            color = TerracotaClara,
            topLeft = Offset(x(.22f), y(.60f)),
            size = Size(x(.56f), y(.11f)),
            cornerRadius = CornerRadius(x(.035f))
        )

        // Tierra
        drawOval(Tierra, topLeft = Offset(x(.25f), y(.60f)), size = Size(x(.50f), y(.065f)))

        // Bulbo con carita
        drawCircle(VerdeBulbo, radius = x(.135f), center = Offset(x(.5f), y(.50f)))

        // Monticulo de tierra que tapa la base del bulbo
        drawOval(TierraClara, topLeft = Offset(x(.36f), y(.608f)), size = Size(x(.28f), y(.05f)))

        // Hojas (dos ovalos inclinados con una vena mas clara)
        rotate(-30f, pivot = Offset(x(.46f), y(.37f))) {
            drawOval(VerdeHoja, topLeft = Offset(x(.26f), y(.31f)), size = Size(x(.22f), y(.12f)))
            drawOval(VerdeHojaClara, topLeft = Offset(x(.30f), y(.345f)), size = Size(x(.13f), y(.04f)))
        }
        rotate(30f, pivot = Offset(x(.54f), y(.37f))) {
            drawOval(VerdeHoja, topLeft = Offset(x(.52f), y(.31f)), size = Size(x(.22f), y(.12f)))
            drawOval(VerdeHojaClara, topLeft = Offset(x(.57f), y(.345f)), size = Size(x(.13f), y(.04f)))
        }

        // Cara: ojos con brillo, sonrisa y mejillas
        drawCircle(Tinta, radius = x(.017f), center = Offset(x(.455f), y(.495f)))
        drawCircle(Tinta, radius = x(.017f), center = Offset(x(.545f), y(.495f)))
        drawCircle(Color.White, radius = x(.006f), center = Offset(x(.461f), y(.489f)))
        drawCircle(Color.White, radius = x(.006f), center = Offset(x(.551f), y(.489f)))
        drawArc(
            color = Tinta,
            startAngle = 25f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(x(.467f), y(.505f)),
            size = Size(x(.066f), y(.045f)),
            style = Stroke(width = x(.011f), cap = StrokeCap.Round)
        )
        drawCircle(Color(0x66FF8A8A), radius = x(.022f), center = Offset(x(.41f), y(.535f)))
        drawCircle(Color(0x66FF8A8A), radius = x(.022f), center = Offset(x(.59f), y(.535f)))
    }
}

/**
 * Escena circular con anillo blanco: el brote sobre un fondo calido-menta. Con [respira] el
 * brote se mece suavemente en bucle.
 */
@Composable
fun EscenaBrote(modifier: Modifier = Modifier, respira: Boolean = true) {
    val transicion = rememberInfiniteTransition(label = "respiracion")
    val vaiven by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "vaiven"
    )
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(Color(0xFFFFF4E8), Color(0xFFE2F5EA)))
                ),
            contentAlignment = Alignment.Center
        ) {
            BroteEnMaceta(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
                    .graphicsLayer {
                        // Sube y baja ~6 dp con un leve cambio de escala, como si respirara.
                        if (respira) {
                            translationY = -6.dp.toPx() * vaiven
                            val escala = 1f + 0.015f * vaiven
                            scaleX = escala
                            scaleY = escala
                        }
                    }
            )
        }
    }
}
