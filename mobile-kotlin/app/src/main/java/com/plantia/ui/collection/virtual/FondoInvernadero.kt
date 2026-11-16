package com.plantia.ui.collection.virtual

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.plantia.ui.escena.momentoDelDia
import java.time.LocalTime
import kotlin.math.roundToInt
import kotlin.math.sin

/** Madera del estante donde se apoyan las macetas, segun el invernadero. */
data class PaletaEstante(val tabla: Color, val canto: Color)

fun paletaDeEstante(codigo: String): PaletaEstante = when (codigo) {
    "VICTORIANO" -> PaletaEstante(Color(0xFFF4F1E8), Color(0xFFC9C3B2))
    "LUCES" -> PaletaEstante(Color(0xFF5A4030), Color(0xFF33241A))
    else -> PaletaEstante(Color(0xFF8A6238), Color(0xFF5E4125))
}

/** Grosor del marco como parte del ancho: el contenido se acomoda dentro. */
const val GROSOR_MARCO = 0.034f

// ---------- Cielo y paisaje de la selva (un solo paisaje que cambia con la hora) ----------

private fun cielo(momento: String): List<Color> = when (momento) {
    "TARDE" -> listOf(0xFF6C4E9E, 0xFFB6567F, 0xFFE07A5F, 0xFFF2A65A, 0xFFF7C873)
    "NOCHE" -> listOf(0xFF070B24, 0xFF0C1438, 0xFF121C4A, 0xFF1A2A5E, 0xFF22366E)
    else -> listOf(0xFF7FCBE6, 0xFF9AD8EA, 0xFFB4E4E6, 0xFFCCEEDE, 0xFFE0F5D0)
}.map { Color(it) }

/** Tres planos de selva, del lejano al cercano. */
private fun planos(momento: String): List<Color> = when (momento) {
    "TARDE" -> listOf(0xFF6B5B4A, 0xFF4A4A33, 0xFF2F3A26)
    "NOCHE" -> listOf(0xFF1B3550, 0xFF11273D, 0xFF0A1A2C)
    else -> listOf(0xFF4F9A57, 0xFF357A3C, 0xFF21562A)
}.map { Color(it) }

// Alturas (como parte del alto) de las copas de cada plano; fijas para que el paisaje no cambie entre aperturas.
private val COPAS = arrayOf(
    floatArrayOf(.46f, .42f, .47f, .40f, .44f, .38f, .45f, .41f, .47f, .39f, .43f, .46f, .40f, .44f),
    floatArrayOf(.56f, .60f, .54f, .58f, .62f, .55f, .59f, .53f, .61f, .57f, .54f, .60f, .56f, .62f),
    floatArrayOf(.72f, .68f, .75f, .70f, .66f, .74f, .71f, .67f, .76f, .69f, .73f, .68f, .75f, .70f)
)

private fun DrawScope.paisaje(momento: String) {
    val w = size.width
    val h = size.height
    val bandas = cielo(momento)
    val alto = h / bandas.size
    bandas.forEachIndexed { i, c -> drawRect(c, Offset(0f, i * alto), Size(w, alto + 1f)) }

    // Un solo astro: sol de dia, sol bajo en la tarde, luna de noche
    val lado = w / 90f
    when (momento) {
        "NOCHE" -> {
            var semilla = 11
            fun r(): Float { semilla = (semilla * 1103515245 + 12345) and 0x7fffffff; return (semilla % 1000) / 1000f }
            repeat(30) { drawRect(Color(0xCCFFFFFF), Offset((r() * w).roundToInt().toFloat(), (r() * h * 0.5f).roundToInt().toFloat()), Size(lado, lado)) }
            drawCircle(Color(0x22F4F1DE), w * 0.12f, Offset(w * 0.20f, h * 0.15f))
            drawCircle(Color(0xFFF4F1DE), w * 0.065f, Offset(w * 0.20f, h * 0.15f))
            drawCircle(Color(0xFF121C4A), w * 0.055f, Offset(w * 0.225f, h * 0.14f))
        }
        "TARDE" -> {
            drawCircle(Color(0x33FFD27A), w * 0.20f, Offset(w * 0.26f, h * 0.34f))
            drawCircle(Color(0xFFFFB04F), w * 0.085f, Offset(w * 0.26f, h * 0.34f))
        }
        else -> {
            drawCircle(Color(0x33FFF3A6), w * 0.16f, Offset(w * 0.22f, h * 0.15f))
            drawCircle(Color(0xFFFFE27A), w * 0.065f, Offset(w * 0.22f, h * 0.15f))
        }
    }

    // Haces de luz entre las copas (no de noche)
    if (momento != "NOCHE") {
        val haz = Color(if (momento == "TARDE") 0x1FFFD9A0 else 0x24FFFFFF)
        listOf(0.2f, 0.5f, 0.78f).forEach { x ->
            val camino = Path().apply {
                moveTo(w * x, 0f); lineTo(w * (x + 0.06f), 0f); lineTo(w * (x + 0.17f), h); lineTo(w * (x + 0.05f), h); close()
            }
            drawPath(camino, haz)
        }
    }

    // Planos de selva en columnas de pixel: lejano, medio y cercano
    val verdes = planos(momento)
    COPAS.forEachIndexed { plano, alturas ->
        val ancho = w / alturas.size
        alturas.forEachIndexed { i, a ->
            val x0 = (i * ancho).roundToInt().toFloat()
            val x1 = ((i + 1) * ancho).roundToInt().toFloat()
            drawRect(verdes[plano], Offset(x0, h * a), Size(x1 - x0, h - h * a))
        }
    }

    if (momento == "NOCHE") {
        // Luciernagas
        val c = Color(0xFFD6FF7A)
        listOf(.18f to .52f, .42f to .66f, .66f to .58f, .86f to .72f, .30f to .80f).forEach { (x, y) ->
            drawCircle(c.copy(alpha = 0.25f), lado * 3f, Offset(w * x, h * y))
            drawRect(c, Offset(w * x - lado / 2, h * y - lado / 2), Size(lado, lado))
        }
    }
}

// ---------- Marco, lianas y guacamayo ----------

private fun DrawScope.marcoDeMadera(claro: Color, oscuro: Color) {
    val w = size.width
    val h = size.height
    val t = w * GROSOR_MARCO
    // Viga de arriba un poco mas gruesa
    drawRect(oscuro, Offset(0f, 0f), Size(w, t * 1.7f))
    drawRect(claro, Offset(0f, 0f), Size(w, t * 1.1f))
    drawRect(oscuro, Offset(0f, h - t), Size(w, t))
    drawRect(claro, Offset(0f, h - t), Size(w, t * 0.6f))
    drawRect(oscuro, Offset(0f, 0f), Size(t, h))
    drawRect(claro, Offset(0f, 0f), Size(t * 0.6f, h))
    drawRect(oscuro, Offset(w - t, 0f), Size(t, h))
    drawRect(claro, Offset(w - t * 0.6f, 0f), Size(t * 0.6f, h))
    // Vetas de la viga
    var x = t * 2f
    while (x < w - t * 2f) {
        drawRect(oscuro, Offset(x, t * 0.35f), Size(t * 1.4f, t * 0.12f))
        x += t * 4.3f
    }
}

private fun DrawScope.lianas() {
    val w = size.width
    val h = size.height
    val t = w * GROSOR_MARCO
    val px = w / 90f
    val tallo = Color(0xFF2F7A3A)
    val hoja = Color(0xFF55B85B)
    listOf(0.07f to 0.20f, 0.40f to 0.12f, 0.55f to 0.26f, 0.66f to 0.10f).forEach { (fx, largo) ->
        val x0 = w * fx
        val y0 = t * 1.7f
        val pasos = ((h * largo) / (px * 2f)).roundToInt()
        for (i in 0 until pasos) {
            val x = x0 + sin(i * 0.8f + fx * 9f) * px * 1.6f
            val y = y0 + i * px * 2f
            drawRect(tallo, Offset(x.roundToInt().toFloat(), y.roundToInt().toFloat()), Size(px, px * 2f))
            if (i % 3 == 1) {
                val lado = if (i % 2 == 0) -1f else 1f
                drawRect(hoja, Offset(x + (if (lado < 0) -px * 2f else px), y), Size(px * 2f, px))
                drawRect(hoja, Offset(x + (if (lado < 0) -px * 3f else px * 2f), y + px), Size(px * 2f, px))
            }
        }
    }
}

private val GUACAMAYO = listOf(
    ".....rrr....",
    "....rrrrr...",
    "...rrwwwr...",
    "..orwkwwr...",
    "..oowwwrr...",
    "...orrrrrr..",
    "....rrrrrr..",
    "....rryyyr..",
    "....rryyyb..",
    "....rryybb..",
    "....rrybbb..",
    ".....rybbb..",
    ".....rbbbb..",
    "......bbb...",
    "......bbb...",
    "......bb...."
)

private val PALETA_GUACAMAYO = mapOf(
    'r' to Color(0xFFD7263D), 'y' to Color(0xFFF9C80E), 'b' to Color(0xFF1F6FD1),
    'w' to Color(0xFFF4F1E8), 'k' to Color(0xFF111111), 'o' to Color(0xFFCFC8B8)
)

private fun DrawScope.guacamayo() {
    val w = size.width
    val t = w * GROSOR_MARCO
    val px = w / 64f
    val ancho = 12 * px
    val x0 = w - t - ancho - px * 3f
    val y0 = t * 1.7f + px * 4f
    // Rama donde se posa
    drawRect(Color(0xFF6B4A2B), Offset(x0 - px * 3f, y0 + 16 * px), Size(ancho + px * 6f, px * 1.6f))
    GUACAMAYO.forEachIndexed { fila, texto ->
        texto.forEachIndexed { col, c ->
            val color = PALETA_GUACAMAYO[c] ?: return@forEachIndexed
            drawRect(color, Offset(x0 + col * px, y0 + fila * px), Size(px + 0.5f, px + 0.5f))
        }
    }
}

// ---------- Los otros dos temas ----------

private fun DrawScope.victoriano(momento: String) {
    val w = size.width
    val h = size.height
    val bandas = when (momento) {
        "TARDE" -> listOf(0xFFF6D7B0, 0xFFF3CFAE, 0xFFEAD0BC, 0xFFDDCFC8, 0xFFD2CCCB, 0xFFC8C8CD)
        "NOCHE" -> listOf(0xFF1D2A4B, 0xFF24335A, 0xFF2C3C68, 0xFF354773, 0xFF3E517D, 0xFF485B88)
        else -> listOf(0xFFF6EFD9, 0xFFEFE9D2, 0xFFE6E8D2, 0xFFDCE6D6, 0xFFD2E3DA, 0xFFC8DFDD)
    }.map { Color(it) }
    val alto = h / bandas.size
    bandas.forEachIndexed { i, c -> drawRect(c, Offset(0f, i * alto), Size(w, alto + 1f)) }
    val hierro = Color(0xFFFFFFFF)
    val grosor = (w / 160f).coerceAtLeast(2f)
    for (i in 1..5) drawRect(hierro, Offset(w * i / 6f - grosor / 2, 0f), Size(grosor, h))
    listOf(0.22f, 0.5f).forEach { y -> drawRect(hierro, Offset(0f, h * y - grosor / 2), Size(w, grosor)) }
    drawPath(
        Path().apply { moveTo(w * 0.10f, 0f); lineTo(w * 0.16f, 0f); lineTo(w * 0.04f, h * 0.22f); lineTo(0f, h * 0.22f); close() },
        Color(0x40FFFFFF)
    )
    drawArc(hierro, 180f, 180f, false, Offset(w * 0.12f, -h * 0.10f), Size(w * 0.76f, h * 0.26f), style = Stroke(width = grosor))
}

private fun DrawScope.luces() {
    val w = size.width
    val h = size.height
    val bandas = listOf(0xFF0A1030, 0xFF0E1740, 0xFF131F52, 0xFF192862, 0xFF203270, 0xFF283B7C).map { Color(it) }
    val alto = h / bandas.size
    bandas.forEachIndexed { i, c -> drawRect(c, Offset(0f, i * alto), Size(w, alto + 1f)) }
    var semilla = 7
    fun r(): Float { semilla = (semilla * 1103515245 + 12345) and 0x7fffffff; return (semilla % 1000) / 1000f }
    val lado = (w / 90f).coerceAtLeast(2f)
    repeat(34) { drawRect(Color(0xCCFFFFFF), Offset((r() * w).roundToInt().toFloat(), (r() * h * 0.7f).roundToInt().toFloat()), Size(lado, lado)) }
    val colores = listOf(Color(0xFFFFD166), Color(0xFFFF8FA3), Color(0xFF9BE7FF), Color(0xFFB8F2A6))
    val hilo = Color(0xFF1B1B2E)
    for (tramo in 0..1) {
        val x0 = w * tramo / 2f
        val ancho = w / 2f
        val puntos = 9
        var anterior: Offset? = null
        for (i in 0..puntos) {
            val t = i / puntos.toFloat()
            val x = x0 + ancho * t
            val y = h * 0.07f + (4f * t * (1f - t)) * h * 0.05f
            val actual = Offset(x, y)
            anterior?.let { drawLine(hilo, it, actual, strokeWidth = lado * 0.5f) }
            anterior = actual
            if (i in 1 until puntos) {
                val color = colores[(i + tramo) % colores.size]
                drawCircle(color.copy(alpha = 0.25f), radius = lado * 3.2f, center = Offset(x, y + lado * 2))
                drawRect(color, Offset(x - lado, y + lado), Size(lado * 2, lado * 2.4f))
            }
        }
    }
}

/**
 * Fondo en pixel art del invernadero elegido, con su marco: Selva tropical (predeterminado: madera, lianas y un
 * guacamayo sobre un solo paisaje que cambia entre dia, tarde y noche), Victoriano (hierro y vidrio) o
 * Luces de noche. [hora] es la hora del telefono (0 a 23).
 */
@Composable
fun FondoInvernadero(
    codigo: String,
    modifier: Modifier = Modifier,
    hora: Int = remember { LocalTime.now().hour }
) {
    val momento = momentoDelDia(hora)
    Canvas(modifier) {
        when (codigo) {
            "VICTORIANO" -> {
                victoriano(momento)
                marcoDeMadera(Color(0xFFE9E6DA), Color(0xFFA9A493))
            }
            "LUCES" -> {
                luces()
                marcoDeMadera(Color(0xFF5A4030), Color(0xFF33241A))
            }
            else -> {
                paisaje(momento)
                marcoDeMadera(Color(0xFF9A6B3C), Color(0xFF5E4125))
                lianas()
                guacamayo()
            }
        }
    }
}
