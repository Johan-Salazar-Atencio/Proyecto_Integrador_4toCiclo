package com.plantia.ui.escena

import android.content.Context
import androidx.compose.foundation.Canvas
import com.plantia.ui.collection.virtual.dibujarMaceta
import com.plantia.ui.collection.virtual.paletaDeMaceta
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.google.gson.Gson
import com.plantia.data.model.Apariencia
import kotlin.math.roundToInt

// La escena es una cuadricula de 96x96 pixeles. La planta (32x32) se dibuja al doble de tamano
// y su primera fila de tierra queda sobre la tierra del fondo (fila 75), asi no flota.
private const val LADO_ESCENA = 96f
private const val ESCALA = 2
private const val FILA_TIERRA_ESCENA = 75
private const val FILA_TIERRA_SPRITE = 27
private const val ANCHO_MACETA_ESCENA = 26f

private var cache: PlantaSpritesJson? = null

/** Lee los sprites compartidos con la web (assets/planta_sprites.json); null si no se pueden leer. */
fun cargarSprites(contexto: Context): PlantaSpritesJson? {
    cache?.let { return it }
    return runCatching {
        contexto.assets.open("planta_sprites.json").bufferedReader().use {
            Gson().fromJson(it, PlantaSpritesJson::class.java)
        }
    }.getOrNull()?.takeIf { it.conjuntos.isNotEmpty() }?.also { cache = it }
}

@Composable
fun rememberSprites(): PlantaSpritesJson? {
    val contexto = LocalContext.current
    return remember { cargarSprites(contexto) }
}

/**
 * Planta en pixel art sobre la tierra de la escena, centrada. Ocupa todo el espacio del padre
 * (la escena) y se escala con el: el tamano de cada pixel sale del ancho, nunca es fijo.
 */
@Composable
fun PlantaPixel(
    sprites: PlantaSpritesJson,
    apariencia: Apariencia?,
    diasCuidados: Int,
    diasTotales: Int,
    marchita: Boolean,
    descripcion: String,
    modifier: Modifier = Modifier,
    /** Codigo de la maceta puesta a la planta; con ella se dibuja la maceta en lugar del monton de tierra. */
    macetaCodigo: String? = null
) {
    val indice = indiceSprite(diasCuidados, diasTotales, marchita)
    val paleta = remember(sprites, apariencia) { paletaDe(sprites, apariencia) }
    val planta = remember(sprites, apariencia, indice) {
        tramos(spritesDe(sprites, apariencia).getOrNull(indice).orEmpty(), paleta)
    }
    val suelo = remember(sprites, paleta) { tramos(sprites.tierra, paleta) }

    Canvas(modifier.semantics { contentDescription = descripcion }) {
        val lado = minOf(size.width, size.height)
        val unidad = lado / LADO_ESCENA
        val origenX = (size.width - lado) / 2f + (LADO_ESCENA - sprites.lado * ESCALA) / 2f * unidad
        val origenY = (size.height - lado) / 2f + (FILA_TIERRA_ESCENA - FILA_TIERRA_SPRITE * ESCALA) * unidad
        // Los bordes se redondean para que no queden costuras entre pixeles
        fun dibujar(lista: List<Tramo>) = lista.forEach { t ->
            val x0 = (origenX + t.x * ESCALA * unidad).roundToInt()
            val x1 = (origenX + (t.x + t.ancho) * ESCALA * unidad).roundToInt()
            val y0 = (origenY + t.y * ESCALA * unidad).roundToInt()
            val y1 = (origenY + (t.y + 1) * ESCALA * unidad).roundToInt()
            drawRect(Color(t.color), Offset(x0.toFloat(), y0.toFloat()), Size((x1 - x0).toFloat(), (y1 - y0).toFloat()))
        }
        if (macetaCodigo != null) {
            // La maceta se apoya en el suelo de la escena y la planta sale de ella
            val ancho = ANCHO_MACETA_ESCENA * unidad
            val izquierda = (size.width - lado) / 2f + (LADO_ESCENA - ANCHO_MACETA_ESCENA) / 2f * unidad
            val arriba = (size.height - lado) / 2f + FILA_TIERRA_ESCENA * unidad
            dibujarMaceta(paletaDeMaceta(macetaCodigo), Offset(izquierda, arriba), ancho / 16f)
            dibujar(planta)
        } else {
            dibujar(planta)
            dibujar(suelo)
        }
    }
}
