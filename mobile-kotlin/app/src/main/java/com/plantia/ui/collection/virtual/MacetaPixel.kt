package com.plantia.ui.collection.virtual

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.roundToInt

/** Colores de una maceta: el cuerpo, la luz del borde y la sombra del costado. */
data class PaletaMaceta(val cuerpo: Color, val luz: Color, val sombra: Color)

/** Cada maceta de la tienda se dibuja con su codigo; uno desconocido cae en la terracota de siempre. */
fun paletaDeMaceta(codigo: String): PaletaMaceta = when (codigo) {
    "AZUL" -> PaletaMaceta(Color(0xFF4A7FC1), Color(0xFF7BA6DB), Color(0xFF2F5A92))
    "CREMA" -> PaletaMaceta(Color(0xFFEBD9B0), Color(0xFFF7EBCB), Color(0xFFC9B27E))
    "ROSA" -> PaletaMaceta(Color(0xFFE88FB0), Color(0xFFF4B6CE), Color(0xFFC1668A))
    "DORADA" -> PaletaMaceta(Color(0xFFD9A441), Color(0xFFF0C873), Color(0xFFA97A22))
    else -> PaletaMaceta(Color(0xFFC46A3C), Color(0xFFDE8A5C), Color(0xFF9A4C26))
}

// Rejilla de 16 x 8 "pixeles": 2 filas de borde (16 de ancho) y 6 de cuerpo que se estrechan hacia el fondo.
private const val COLUMNAS = 16
private const val FILAS = 8
private val ANCHO_POR_FILA = intArrayOf(16, 16, 14, 14, 13, 13, 12, 12)

/** Relacion ancho/alto de la maceta, para quien la acomoda. */
const val PROPORCION_MACETA = COLUMNAS / FILAS.toFloat()

/** Maceta en pixel art; el tamano de cada pixel sale del ancho disponible, nunca es fijo. */
@Composable
fun MacetaPixel(codigo: String, modifier: Modifier = Modifier) {
    val paleta = paletaDeMaceta(codigo)
    Canvas(modifier) { dibujarMaceta(paleta, Offset.Zero, size.width / COLUMNAS) }
}

/** Dibuja la maceta con su esquina superior izquierda en [origen]; cada pixel mide [px]. */
fun DrawScope.dibujarMaceta(paleta: PaletaMaceta, origen: Offset, px: Float) {
    for (fila in 0 until FILAS) {
        val ancho = ANCHO_POR_FILA[fila]
        val izquierda = (COLUMNAS - ancho) / 2
        for (col in 0 until ancho) {
            val color = when {
                fila == 0 -> paleta.luz
                col == 0 && fila > 1 -> paleta.luz
                col >= ancho - 2 && fila > 1 -> paleta.sombra
                fila == FILAS - 1 -> paleta.sombra
                else -> paleta.cuerpo
            }
            celda(origen, izquierda + col, fila, px, color)
        }
    }
}

private fun DrawScope.celda(origen: Offset, columna: Int, fila: Int, px: Float, color: Color) {
    // Los bordes se redondean para que no queden costuras entre pixeles
    val x0 = (origen.x + columna * px).roundToInt()
    val x1 = (origen.x + (columna + 1) * px).roundToInt()
    val y0 = (origen.y + fila * px).roundToInt()
    val y1 = (origen.y + (fila + 1) * px).roundToInt()
    drawRect(color, Offset(x0.toFloat(), y0.toFloat()), Size((x1 - x0).toFloat(), (y1 - y0).toFloat()))
}
