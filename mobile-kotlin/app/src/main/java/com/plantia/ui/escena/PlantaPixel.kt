package com.plantia.ui.escena

import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.plantia.data.model.Apariencia
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// La escena es una cuadricula de 96x96. El lienzo de la planta (64x64, con su maceta) va en (16, 28): asi su tierra
// (fila 47 del lienzo) cae en la fila 75 de la escena, sobre el suelo del fondo. Igual que .jardin-planta en la web.
private const val LADO_ESCENA = 96f
private const val X_PLANTA = 16f
private const val Y_PLANTA = 28f

// Balanceo: un fotograma cada 160 ms (12 fotogramas = 1,9 s por vaiven), como PlantaPixel.jsx
private const val MS_FOTOGRAMA = 160L

/** Etapa de dibujo (0..5) y estado (null, marchita o muerta) de una planta. */
data class EstadoPlanta64(val etapa: Int, val estado: String?)

/**
 * Lo mismo que `etapaDe` de la web (jardin/etapas.js): la etapa sigue el avance real aunque este marchita, y el
 * estado seca el dibujo (marchita) o lo apaga del todo (muerta).
 */
fun estadoPlanta64(diasCuidados: Int, diasTotales: Int, marchita: Boolean, muerta: Boolean) = EstadoPlanta64(
    etapa = indiceSprite(diasCuidados, diasTotales, marchita = false),
    estado = when {
        muerta -> MotorPlanta64.MUERTA
        marchita -> MotorPlanta64.MARCHITA
        else -> null
    }
)

/**
 * Planta en pixel art de 64x64 (el mismo motor que la web, ver [MotorPlanta64]) ampliada sin suavizar.
 *
 * - En la escena (por defecto) ocupa el cuadro del padre como una escena de 96x96 y se dibuja con su maceta: la
 *   comprada ([macetaCodigo]) o la propia de su forma.
 * - Con [enEstante] llena el padre (el hueco del estante del invernadero), tambien con su maceta.
 *
 * Se balancea si [animar] (salvo marchita, muerta o con las animaciones del sistema apagadas).
 */
@Composable
fun PlantaPixel(
    apariencia: Apariencia?,
    diasCuidados: Int,
    diasTotales: Int,
    marchita: Boolean,
    descripcion: String,
    modifier: Modifier = Modifier,
    /** Codigo de la maceta comprada (AZUL, ROSA...); null = la maceta propia de la forma. */
    macetaCodigo: String? = null,
    muerta: Boolean = false,
    enEstante: Boolean = false,
    animar: Boolean = true
) {
    val contexto = LocalContext.current
    val (etapa, estado) = estadoPlanta64(diasCuidados, diasTotales, marchita, muerta)
    val codigo = macetaCodigo
    val sinAnimaciones = remember(contexto) {
        runCatching { Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
    val quieto = !animar || estado != null || sinAnimaciones

    // Un ImageBitmap por fotograma, creado la primera vez que se pinta
    val cuadros = remember(apariencia, etapa, estado, codigo) { arrayOfNulls<ImageBitmap>(MotorPlanta64.FOTOGRAMAS) }
    var k by remember { mutableIntStateOf(0) }
    LaunchedEffect(quieto) {
        if (quieto) {
            k = 0
            return@LaunchedEffect
        }
        while (true) {
            delay(MS_FOTOGRAMA)
            k = (k + 1) % MotorPlanta64.FOTOGRAMAS
        }
    }

    Canvas(modifier.semantics { contentDescription = descripcion }) {
        val i = if (quieto) 0 else k
        val imagen = cuadros[i] ?: Bitmap.createBitmap(
            MotorPlanta64.fotograma(apariencia, etapa, i, codigo, estado), MotorPlanta64.LADO, MotorPlanta64.LADO, Bitmap.Config.ARGB_8888
        ).asImageBitmap().also { cuadros[i] = it }

        val (x, y, lado) = if (enEstante) {
            val lado = minOf(size.width, size.height)
            Triple(0f, size.height - lado, lado)
        } else {
            val escena = minOf(size.width, size.height)
            val unidad = escena / LADO_ESCENA
            Triple((size.width - escena) / 2f + X_PLANTA * unidad, (size.height - escena) / 2f + Y_PLANTA * unidad, MotorPlanta64.LADO * unidad)
        }
        val izquierda = if (enEstante) (size.width - lado) / 2f else x
        drawImage(
            imagen,
            dstOffset = IntOffset(izquierda.roundToInt(), y.roundToInt()),
            dstSize = IntSize(lado.roundToInt(), lado.roundToInt()),
            filterQuality = FilterQuality.None
        )
    }
}
