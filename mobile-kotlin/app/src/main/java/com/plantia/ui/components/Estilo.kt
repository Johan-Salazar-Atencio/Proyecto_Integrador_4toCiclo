package com.plantia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.SurfaceWhite

/**
 * Borde de las tarjetas del nuevo diseno: 1dp del color de linea del tema y sin sombra fuerte. Se conservan
 * los parametros [color] y [elevacion] por compatibilidad con las pantallas que ya lo usan: ya no pintan brillo.
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.brilloTarjeta(
    color: Color,
    forma: Shape = RoundedCornerShape(24.dp),
    elevacion: Dp = 0.dp
): Modifier = composed { this.border(1.dp, DividerColor, forma) }

/** Tarjeta del nuevo diseno: fondo de tarjeta, borde de 1dp y esquinas de 24dp. */
fun Modifier.tarjeta(forma: Shape = RoundedCornerShape(24.dp)): Modifier = composed {
    this.clip(forma).background(SurfaceWhite, forma).border(1.dp, DividerColor, forma)
}

/**
 * La barra de progreso de toda la app (XP, salud, agua, luz, misiones). A diferencia de la de Material 3,
 * no dibuja el punto suelto al final; la pista es del mismo color, mas tenue, y un valor bajo nunca se ve
 * como una punta deformada.
 */
@Composable
fun BarraProgreso(
    progreso: Float,
    color: Color,
    modifier: Modifier = Modifier,
    alto: Dp = 8.dp,
    pista: Color = color.copy(alpha = 0.18f)
) {
    val valor = progreso.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(50))
            .background(pista)
    ) {
        if (valor > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .widthIn(min = alto)
                    .fillMaxWidth(valor)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}
