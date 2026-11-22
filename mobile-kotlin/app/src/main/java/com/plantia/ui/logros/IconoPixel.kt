package com.plantia.ui.logros

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density

/** Lado del icono sin contorno y con contorno (como los sprites 16x16 + borde de Jose). */
const val LADO_ICONO = 16
const val LADO_SPRITE = LADO_ICONO + 2

/** Contorno oscuro de Jose `[44, 32, 40]`. */
private const val CONTORNO_JOSE = 0xFF2C2028.toInt()

private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

/** Luz, medio y sombra de cada familia de iconos (los mismos tonos del prototipo). */
private val AMBAR = intArrayOf(rgb(254, 240, 138), rgb(251, 191, 36), rgb(200, 110, 8))
private val VERDE = intArrayOf(rgb(196, 235, 120), rgb(127, 194, 65), rgb(70, 130, 48))
private val AZUL = intArrayOf(rgb(186, 230, 253), rgb(56, 189, 248), rgb(2, 120, 190))
private val NARANJA = intArrayOf(rgb(253, 186, 116), rgb(249, 115, 22), rgb(185, 60, 10))
private val ACERO = intArrayOf(rgb(199, 210, 254), rgb(99, 140, 240), rgb(40, 80, 200))
private val MORADO = intArrayOf(rgb(233, 213, 255), rgb(192, 132, 252), rgb(126, 34, 206))
private val ROJO = intArrayOf(rgb(254, 205, 211), rgb(251, 113, 133), rgb(190, 18, 60))

/** Clave del icono -> sus tres tonos. Una clave desconocida usa el ambar del trofeo por defecto. */
fun paletaDeIcono(clave: String?): IntArray = when (clave?.trim()?.lowercase()) {
    "semilla", "hoja", "maceta", "invernadero", "fondo", "bicho" -> VERDE
    "gota", "huayco", "copo" -> AZUL
    "racha" -> NARANJA
    "escudo", "refugio", "luna" -> ACERO
    "abono", "secreto" -> MORADO
    "corazon", "mision" -> ROJO
    else -> AMBAR // sol, luz, nivel, amanecer, tienda y el trofeo por defecto
}

/**
 * Convierte la silueta 16x16 de un icono ([lleno], fila por fila) en un sprite 18x18 ARGB con sombreado en
 * diagonal (luz arriba a la izquierda) y contorno oscuro de 1 px, igual que los sprites del prototipo.
 */
fun construirSpritePixel(lleno: BooleanArray, paleta: IntArray): IntArray {
    require(lleno.size == LADO_ICONO * LADO_ICONO)
    val n = LADO_SPRITE
    val col = IntArray(n * n)
    for (y in 0 until LADO_ICONO) for (x in 0 until LADO_ICONO) {
        if (!lleno[y * LADO_ICONO + x]) continue
        val u = (x + 0.5f - 8) / 7.5f
        val v = (y + 0.5f - 8) / 7.5f
        val s = u * 0.55f + v * 0.83f
        col[(y + 1) * n + (x + 1)] = paleta[if (s < -0.25f) 0 else if (s < 0.35f) 1 else 2]
    }
    val salida = col.copyOf()
    for (y in 0 until n) for (x in 0 until n) {
        if (col[y * n + x] != 0) continue
        val vecino = (y > 0 && col[(y - 1) * n + x] != 0) || (y < n - 1 && col[(y + 1) * n + x] != 0) ||
            (x > 0 && col[y * n + x - 1] != 0) || (x < n - 1 && col[y * n + x + 1] != 0)
        if (vecino) salida[y * n + x] = CONTORNO_JOSE
    }
    return salida
}

/**
 * El icono Material del logro, pixelado: se pinta una vez en 16x16, se toma su silueta y se arma el sprite 18x18.
 * Se dibuja ampliado con `FilterQuality.None` para que se vean los pixeles.
 */
@Composable
fun rememberIconoPixel(clave: String?): ImageBitmap {
    val vector = iconoDeLogro(clave)
    val pintor = rememberVectorPainter(vector)
    val direccion = LocalLayoutDirection.current
    return remember(vector, clave) {
        val mascara = ImageBitmap(LADO_ICONO, LADO_ICONO)
        CanvasDrawScope().draw(Density(1f), direccion, Canvas(mascara), Size(LADO_ICONO.toFloat(), LADO_ICONO.toFloat())) {
            with(pintor) { draw(size) }
        }
        val mapa = mascara.toPixelMap()
        val lleno = BooleanArray(LADO_ICONO * LADO_ICONO) { mapa[it % LADO_ICONO, it / LADO_ICONO].alpha >= 0.45f }
        val pixeles = construirSpritePixel(lleno, paletaDeIcono(clave))
        Bitmap.createBitmap(pixeles, LADO_SPRITE, LADO_SPRITE, Bitmap.Config.ARGB_8888).asImageBitmap()
    }
}
