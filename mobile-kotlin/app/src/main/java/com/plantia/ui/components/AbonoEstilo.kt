package com.plantia.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Colores de cada abono según su `codigo`: suave (lima), orgánico (tierra) y reforzado (violeta). */
object AbonoEstilo {
    const val MEDIO = "ABONO_MEDIO"
    const val UNO = "ABONO_1"
    const val TRES = "ABONO_3"

    private val Suave = Color(0xFF7CB518)
    private val Organico = Color(0xFF9A6B3F)
    private val Reforzado = Color(0xFF9333EA)

    /** Color principal del abono; un código desconocido usa el del suave. */
    fun color(codigo: String?): Color = when (codigo) {
        UNO -> Organico
        TRES -> Reforzado
        else -> Suave
    }

    /** Hojas que dibujan el abono: más hojas cuanto más rinde. */
    fun hojas(codigo: String?): String = when (codigo) {
        UNO -> "🌿🌿"
        TRES -> "🌿🌿🌿"
        else -> "🌿"
    }
}

/** Disco con el color del abono y sus hojas: la ilustración de la tarjeta. */
@Composable
fun IconoAbono(codigo: String?, tamano: Dp = 52.dp, modifier: Modifier = Modifier) {
    val color = AbonoEstilo.color(codigo)
    Box(
        modifier.size(tamano).clip(CircleShape).background(color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        Text(AbonoEstilo.hojas(codigo), fontSize = (tamano.value * 0.36f).sp)
    }
}

/**
 * Pulso al tocar: la tarjeta se encoge un poco mientras su propio botón está pulsado. Cada tarjeta crea su fuente de
 * interacción, así que solo se anima la que se toca (antes reaccionaban todas a la vez).
 */
@Composable
fun Modifier.pulsoAlTocar(fuente: MutableInteractionSource): Modifier {
    val pulsada by fuente.collectIsPressedAsState()
    val escala by animateFloatAsState(if (pulsada) 0.96f else 1f, label = "pulsoAbono")
    return this.scale(escala)
}
