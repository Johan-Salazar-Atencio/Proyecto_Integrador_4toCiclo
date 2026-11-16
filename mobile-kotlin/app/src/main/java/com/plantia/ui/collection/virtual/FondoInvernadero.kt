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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import com.plantia.ui.escena.momentoDelDia
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Invernadero en pixel art de la propuesta E. Cada pagina es una cuadricula de 68 x 102 "pixeles": el techo de
 * vidrio (0 a 26), dos modulos con su repisa de 33 cada uno (26 a 92) y el zocalo de ladrillo (92 a 102).
 * Las plantas y macetas no se dibujan aqui: se colocan encima en cada hueco (ver [HuecoInvernadero]).
 */

/** Ancho y alto de la cuadricula de una pagina. */
const val ANCHO_PAGINA = 68f
const val ALTO_PAGINA = 102f

/** Donde va cada planta dentro de la cuadricula: columna 0..2 y fila 0..1. */
object HuecoInvernadero {
    /** Esquina izquierda de la columna y ancho que ocupa. */
    fun izquierda(columna: Int) = 5f + columna * 22f
    const val ANCHO = 14f

    /** Arriba de la planta (la planta mide 10 y la maceta 8) y la tabla de la repisa donde se apoyan. */
    fun arriba(fila: Int) = 26f + 33f * fila + 9f
    fun repisa(fila: Int) = 26f + 33f * fila + 27f
}

private fun c(hex: String): Color = Color(hex.removePrefix("#").toLong(16) or 0xFF000000)

private class Cielo(val s: List<Color>, val lejos: Color, val cerca: Color, val arbol: Color, val astro: Color, val tinte: Color, val ta: Float)

private fun cieloDe(momento: String) = when (momento) {
    "TARDE" -> Cielo(listOf("#FF8E72", "#FFB878", "#FFE0A0").map(::c), c("#C48A9A"), c("#6FA05F"), c("#3F7F4A"), c("#FFF3B8"), c("#FFD9B0"), .18f)
    "NOCHE" -> Cielo(listOf("#0A1238", "#16245C", "#2A3A86").map(::c), c("#1B3A5E"), c("#12402F"), c("#0C2D22"), c("#F6F1D2"), c("#9CB8FF"), .1f)
    else -> Cielo(listOf("#5BC8FF", "#93DCFF", "#CBF0FF").map(::c), c("#9BDDAA"), c("#52C17A"), c("#2F9E4F"), c("#FFE066"), c("#E9FFF8"), .14f)
}

/** Marco (de dia, de noche) y madera de la repisa de cada tema. */
private class Marco(val dia: List<Color>, val noche: List<Color>, val repisa: List<Color>)

private fun marcoDe(tema: String) = when (tema) {
    "VICTORIANO" -> Marco(listOf("#F4F7F5", "#B9C7BF").map(::c), listOf("#A9B9CC", "#6D8099").map(::c), listOf("#E0A767", "#A8703A").map(::c))
    "LUCES" -> Marco(listOf("#3A4747", "#1F2A2A").map(::c), listOf("#2B3636", "#151D1D").map(::c), listOf("#C8883F", "#94612A").map(::c))
    else -> Marco(listOf("#8A5A34", "#5E3A1E").map(::c), listOf("#6E4A2E", "#47301D").map(::c), listOf("#A8744C", "#7E5233").map(::c))
}

/** Pinta una cuadricula de 68 x 102 escalada al ancho; cada rectangulo cae en pixeles enteros (sin costuras). */
private class Lienzo(val ds: DrawScope, val u: Float) {
    fun r(x: Float, y: Float, w: Float, h: Float, color: Color, alpha: Float = 1f) {
        val izq = floor(x * u)
        val arr = floor(y * u)
        val der = ceil((x + w) * u)
        val aba = ceil((y + h) * u)
        ds.drawRect(color.copy(alpha = color.alpha * alpha), Offset(izq, arr), Size(der - izq, aba - arr))
    }

    fun r(x: Int, y: Int, w: Int, h: Int, color: Color, alpha: Float = 1f) =
        r(x.toFloat(), y.toFloat(), w.toFloat(), h.toFloat(), color, alpha)
}

private fun Lienzo.liana(x: Int, largo: Int, c1: Color, c2: Color) {
    for (k in 0 until largo) {
        r(x + if (k % 3 == 1) 1 else 0, 2 + k, 1, 1, c1)
        if (k % 3 == 2) {
            r(x - 1, 2 + k, 1, 1, c2)
            r(x + 2, 2 + k, 1, 1, c2)
        }
    }
    r(x - 1, 2 + largo, 3, 1, c2)
}

private fun Lienzo.helecho(x: Int) {
    r(x + 1, 2, 1, 3, c("#999999"))
    r(x, 5, 3, 2, c("#E8743B"))
    r(x - 2, 7, 7, 1, c("#2F9E4F"))
    r(x - 3, 8, 2, 2, c("#3FBF63"))
    r(x + 4, 8, 2, 2, c("#3FBF63"))
    r(x - 1, 8, 6, 1, c("#2F9E4F"))
    r(x + 1, 9, 1, 2, c("#2F9E4F"))
}

private fun Lienzo.guirnalda(noche: Boolean) {
    val colores = listOf("#FFD43B", "#FF7EB6", "#6FE3FF", "#FFB878").map(::c)
    var x = 3
    var j = 0
    while (x < 63) {
        val y = 3 + (2 * sin((x - 3) / 60f * PI)).roundToInt()
        r(x, y, 5, 1, c("#111111"))
        r(x + 1, y, 4, 4, colores[j % 4], if (noche) .4f else .15f)
        r(x + 2, y + 1, 2, 2, colores[j % 4])
        x += 5
        j++
    }
}

/** Paisaje que se ve por la ventana (cielo, astro, nubes o estrellas, montes y arboles). */
private fun Lienzo.paisaje(momento: String) {
    val p = cieloDe(momento)
    r(2, 0, 64, 22, p.s[0]); r(2, 22, 64, 18, p.s[1]); r(2, 40, 64, 14, p.s[2])
    r(46, 8, 6, 6, p.astro); r(45, 9, 8, 4, p.astro); r(47, 7, 4, 8, p.astro)
    if (momento == "NOCHE") {
        listOf(8 to 5, 22 to 12, 36 to 6, 60 to 10, 14 to 24, 30 to 28, 56 to 26, 40 to 20, 6 to 34, 40 to 36, 62 to 32, 26 to 4)
            .forEach { (x, y) -> r(x, y, 1, 1, Color.White) }
    } else {
        val n = Color.White
        r(8, 12, 10, 2, n, .9f); r(10, 10, 6, 2, n, .9f); r(26, 24, 9, 2, n, .85f)
        r(28, 22, 5, 2, n, .85f); r(52, 30, 8, 2, n, .8f); r(54, 28, 4, 2, n, .8f)
    }
    r(2, 46, 64, 20, p.lejos); r(4, 42, 16, 4, p.lejos); r(6, 40, 8, 2, p.lejos); r(36, 40, 22, 6, p.lejos); r(42, 38, 10, 2, p.lejos)
    r(2, 54, 64, 12, p.cerca); r(2, 52, 26, 2, p.cerca); r(44, 52, 22, 2, p.cerca)
    listOf(6 to 46, 22 to 49, 40 to 45, 56 to 48).forEach { (x, y) ->
        r(x, y, 6, 8, p.arbol); r(x + 1, y - 2, 4, 2, p.arbol); r(x + 2, y + 8, 1, 5, c("#5E3A1E"))
    }
    r(2, 0, 64, 66, p.tinte, p.ta)
}

/** Un modulo (ventanal con su repisa): marco, rayos de luz, adornos del tema y la tabla donde se apoyan las macetas. */
private fun Lienzo.modulo(tema: String, momento: String, indice: Int, hueco: Int?) {
    val noche = momento == "NOCHE"
    val m = marcoDe(tema)
    val f = if (noche) m.noche else m.dia
    val madera = m.repisa
    val blanco = Color.White
    // Rayos de luz sobre el vidrio
    listOf(
        listOf(24f to 0f, 29f to 0f, 17f to 20f, 17f to 15f) to .2f,
        listOf(56f to 0f, 60f to 0f, 51f to 12f, 51f to 8f) to .18f,
        listOf(7f to 0f, 11f to 0f, 3f to 16f, 3f to 11f) to .18f
    ).forEach { (puntos, alfa) ->
        val camino = Path().apply {
            moveTo(puntos[0].first * u, puntos[0].second * u)
            puntos.drop(1).forEach { lineTo(it.first * u, it.second * u) }
            close()
        }
        ds.drawPath(camino, blanco.copy(alpha = alfa))
    }
    r(16, 0, 1, 27, f[0]); r(17, 0, 1, 27, f[1], .6f); r(50, 0, 1, 27, f[0]); r(51, 0, 1, 27, f[1], .6f)
    r(2, 13, 64, 1, f[0]); r(2, 14, 64, 1, f[1], .5f)
    r(0, 0, 2, 33, f[0]); r(2, 0, 1, 33, f[1]); r(66, 0, 2, 33, f[0]); r(65, 0, 1, 33, f[1])
    r(0, 0, 68, 2, f[0]); r(0, 2, 68, 1, f[1])

    when (tema) {
        "VICTORIANO" -> {
            helecho(if (indice % 2 == 1) 56 else 8)
            val rosa = c("#FF7EB6")
            r(63, 8, 2, 2, rosa); r(64, 13, 2, 2, rosa); r(63, 19, 2, 2, rosa)
            r(1, 6, 1, 18, c("#3FBF63"), .8f)
        }
        "LUCES" -> {
            guirnalda(noche)
            val g = if (noche) .35f else .12f
            r(33, 2, 1, 5, c("#111111")); r(28, 5, 12, 12, c("#FFD43B"), g)
            r(31, 7, 5, 6, c("#2A2A2A")); r(32, 8, 3, 4, c("#FFD43B"))
            if (momento != "DIA") {
                listOf(10 to 18, 56 to 10, 24 to 22, 44 to 19).forEach { (x, y) ->
                    val dx = (indice * 3) % 7
                    r(x + dx, y, 1, 1, c("#FFF59A")); r(x - 1 + dx, y - 1, 3, 3, c("#FFF59A"), .25f)
                }
            }
        }
        else -> {
            liana(4, 9 + (indice % 3) * 2, c("#2F9E4F"), c("#3FBF63"))
            liana(11, 6, c("#1F7A3A"), c("#2F9E4F"))
            liana(55, 8, c("#1F7A3A"), c("#3FBF63"))
            liana(62, 11, c("#2F9E4F"), c("#3FBF63"))
            r(5, 13, 2, 2, c("#FF7EB6")); r(60, 15, 2, 2, c("#FFD43B")); r(12, 10, 2, 2, c("#FF7EB6"))
            if (indice == 0) {
                // Guacamayo posado
                r(40, 9, 4, 4, c("#E5484D")); r(43, 8, 3, 3, c("#E5484D")); r(46, 9, 1, 1, c("#FFB020"))
                r(40, 10, 3, 2, c("#3B82F6")); r(37, 11, 3, 1, c("#3B82F6")); r(44, 9, 1, 1, blanco)
            }
        }
    }

    // El "+" del primer hueco libre
    if (hueco != null) {
        val x = 5 + hueco * 22
        val cx = x + 7
        val t = f[0]
        r(cx - 3, 19, 7, 1, t, .8f); r(cx, 16, 1, 7, t, .8f)
        r(x, 12, 2, 1, t, .6f); r(x + 12, 12, 2, 1, t, .6f); r(x, 24, 2, 1, t, .6f); r(x + 12, 24, 2, 1, t, .6f)
    }

    // Repisa
    r(0, 27, 68, 4, madera[0]); r(0, 31, 68, 2, madera[1]); r(0, 27, 68, 1, blanco, .25f)
    var gx = 4
    while (gx < 66) {
        r(gx, 29, 3, 1, madera[1], .5f)
        gx += 9
    }
}

/** Techo de vidrio a dos aguas con su cielo, vigas y adornos. */
private fun Lienzo.techo(tema: String, momento: String) {
    val p = cieloDe(momento)
    val noche = momento == "NOCHE"
    val m = marcoDe(tema)
    val f = if (noche) m.noche else m.dia
    r(0, 0, 68, 26, f[1])
    val triangulo = Path().apply {
        moveTo(4 * u, 26 * u); lineTo(34 * u, 4 * u); lineTo(64 * u, 26 * u); close()
    }
    ds.clipPath(triangulo) {
        r(0, 0, 68, 26, p.s[0])
        if (noche) {
            listOf(20 to 12, 44 to 16, 30 to 18, 50 to 12, 26 to 10).forEach { (x, y) -> r(x, y, 1, 1, Color.White) }
        } else {
            r(38, 14, 10, 2, Color.White, .85f); r(40, 12, 6, 2, Color.White, .85f); r(18, 18, 8, 2, Color.White, .8f)
        }
        r(0, 0, 68, 26, p.tinte, p.ta)
    }
    val viga = Stroke(width = u)
    ds.drawLine(f[0], Offset(34 * u, 4 * u), Offset(19 * u, 26 * u), strokeWidth = viga.width)
    ds.drawLine(f[0], Offset(34 * u, 4 * u), Offset(49 * u, 26 * u), strokeWidth = viga.width)
    ds.drawLine(f[0], Offset(34 * u, 4 * u), Offset(34 * u, 26 * u), strokeWidth = viga.width)
    ds.drawPath(triangulo, f[0], style = Stroke(width = 2 * u))
    r(33, 0, 2, 4, f[0]); r(33, 0, 2, 1, c("#FFD43B")); r(0, 24, 68, 2, f[0])
    when (tema) {
        "VICTORIANO" -> {
            r(33, 6, 1, 3, c("#999999")); r(31, 9, 5, 4, c("#FFD43B"), if (noche) .9f else .5f); r(32, 10, 3, 2, c("#FFF3B8"))
        }
        "LUCES" -> {
            var t = 0f
            while (t <= 1f) {
                val x = 4 + t * 30
                val y = 26 - t * 22
                r(x - 1, y, 4f, 4f, c("#FFD43B"), if (noche) .4f else .15f); r(x, y + 1, 2f, 2f, c("#FFD43B"))
                r(68 - x - 3, y, 4f, 4f, c("#FF7EB6"), if (noche) .4f else .15f); r(68 - x - 2, y + 1, 2f, 2f, c("#FF7EB6"))
                t += .17f
            }
        }
        else -> {
            liana(3, 13, c("#2F9E4F"), c("#3FBF63")); liana(7, 8, c("#1F7A3A"), c("#2F9E4F"))
            liana(64, 11, c("#2F9E4F"), c("#3FBF63")); liana(60, 7, c("#1F7A3A"), c("#3FBF63"))
        }
    }
}

/** Zocalo de ladrillos del pie del invernadero. */
private fun Lienzo.zocalo(tema: String, momento: String) {
    val w = marcoDe(tema).repisa
    r(0, 0, 68, 10, w[1])
    var x = -4
    while (x < 68) {
        r(x, 0, 7, 4, w[0]); r(x + 4, 5, 7, 4, w[0])
        x += 8
    }
    if (momento == "NOCHE") r(0, 0, 68, 10, c("#000011"), .4f)
}

/** La pagina completa en la cuadricula de 68 x 102, escalada al ancho de [ds]. */
private fun DrawScope.paginaCompleta(tema: String, momento: String, hueco: Int?) {
    val u = size.width / ANCHO_PAGINA
    val l = Lienzo(this, u)
    l.techo(tema, momento)
    translate(0f, 26 * u) {
        l.paisaje(momento)
        l.modulo(tema, momento, 0, if (hueco != null && hueco < 3) hueco else null)
        translate(0f, 33 * u) {
            l.modulo(tema, momento, 1, if (hueco != null && hueco >= 3) hueco - 3 else null)
        }
    }
    // Si la pantalla es mas alta que la escena, el zocalo de ladrillo sigue hacia abajo
    var y = 92f
    while (y * u < size.height) {
        translate(0f, y * u) { l.zocalo(tema, momento) }
        y += 10f
    }
}

/**
 * Pagina del invernadero elegido en pixel art (propuesta E): Selva tropical (predeterminado), Victoriano o Luces.
 * [hora] es la hora del telefono (0 a 23) y decide dia, tarde o noche. [huecoLibre] marca con un "+" el primer lugar
 * vacio (0 a 5) para invitar a escanear otra planta. Su proporcion es [ANCHO_PAGINA] : [ALTO_PAGINA].
 */
@Composable
fun FondoInvernadero(
    codigo: String,
    modifier: Modifier = Modifier,
    hora: Int = remember { LocalTime.now().hour },
    huecoLibre: Int? = null
) {
    val momento = momentoDelDia(hora)
    Canvas(modifier) { paginaCompleta(codigo, momento, huecoLibre) }
}
