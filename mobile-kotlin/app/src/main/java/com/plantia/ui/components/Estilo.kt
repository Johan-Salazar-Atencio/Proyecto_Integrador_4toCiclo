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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Borde de color con un brillo difuminado alrededor (propuesta E). El color dice que significa la tarjeta:
 * verde lo normal, ambar un evento, rojo un peligro, azul informacion. Va antes del clip y del fondo.
 */
fun Modifier.brilloTarjeta(
    color: Color,
    forma: Shape = RoundedCornerShape(20.dp),
    elevacion: Dp = 10.dp
): Modifier = this
    .shadow(elevacion, forma, clip = false, ambientColor = color.copy(alpha = 0.35f), spotColor = color.copy(alpha = 0.5f))
    .border(1.5.dp, color.copy(alpha = 0.6f), forma)

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
