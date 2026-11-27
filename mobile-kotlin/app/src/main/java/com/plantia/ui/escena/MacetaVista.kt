package com.plantia.ui.escena

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

// Recorte del lienzo de 64x64 donde cae la maceta (con su contorno y su sombra), igual que MacetaVista.jsx
const val MACETA_X = 17
const val MACETA_Y = 44
const val MACETA_ANCHO = 30
const val MACETA_ALTO = 20

/**
 * Tamano de un dibujo de pixel art de [ancho] x [alto] ampliado por el mayor factor ENTERO de pixeles de pantalla
 * que cabe en [maxAncho] x [maxAlto]: asi todos sus pixeles miden lo mismo (con un factor no entero unos salen mas
 * anchos que otros). Nunca baja de 1.
 */
@Composable
fun tamanoPixelArt(ancho: Int, alto: Int, maxAncho: Dp, maxAlto: Dp = Dp.Infinity): DpSize {
    val densidad = LocalDensity.current
    return with(densidad) {
        val porAncho = floor(maxAncho.toPx() / ancho).toInt()
        val porAlto = if (maxAlto == Dp.Infinity) porAncho else floor(maxAlto.toPx() / alto).toInt()
        val factor = max(1, min(porAncho, porAlto))
        DpSize((ancho * factor).toDp(), (alto * factor).toDp())
    }
}

/**
 * Maceta de la tienda con el mismo motor y sombreado que las plantas (la que se ve en el jardin), sin planta,
 * ampliada a escala entera dentro de [maxAncho] x [maxAlto].
 */
@Composable
fun MacetaVista(codigo: String?, maxAncho: Dp, maxAlto: Dp, modifier: Modifier = Modifier, descripcion: String? = null) {
    val imagen = remember(codigo) {
        val todo = MotorPlanta64.aArgb(MotorPlanta64.macetaVaciaRgba(null, codigo))
        val recorte = IntArray(MACETA_ANCHO * MACETA_ALTO) { i ->
            todo[(MACETA_Y + i / MACETA_ANCHO) * MotorPlanta64.LADO + MACETA_X + i % MACETA_ANCHO]
        }
        Bitmap.createBitmap(recorte, MACETA_ANCHO, MACETA_ALTO, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
    val tamano = tamanoPixelArt(MACETA_ANCHO, MACETA_ALTO, maxAncho, maxAlto)
    val semantica = if (descripcion != null) Modifier.semantics { contentDescription = descripcion } else Modifier
    Canvas(modifier.size(tamano).then(semantica)) {
        drawImage(
            imagen,
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = FilterQuality.None
        )
    }
}
