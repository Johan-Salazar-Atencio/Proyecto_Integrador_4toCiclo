package com.plantia.ui.logros

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.plantia.ui.logros.MotorMedalla.Companion.CONTORNO
import com.plantia.ui.logros.MotorMedalla.Companion.DESTELLO
import com.plantia.ui.logros.MotorMedalla.Companion.FILL
import com.plantia.ui.logros.MotorMedalla.Companion.RING
import com.plantia.ui.logros.MotorMedalla.Companion.circulo
import com.plantia.ui.logros.MotorMedalla.Companion.redondear
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

// Colores del dibujo (los mismos del prototipo y de las plantas de Jose)
private const val BLANCO = 0xFFFFFFFF.toInt()
private const val TERRACOTA = 0xFFD9825B.toInt()
private const val TERRACOTA_CLARA = 0xFFE79A6E.toInt()
private const val TERRACOTA_SOMBRA = 0xFFB86A45.toInt()
private const val TIERRA = 0xFF5B4636.toInt()
private const val TIERRA_CLARA = 0xFF6E5441.toInt()
private const val TALLO = 0xFF7FC241.toInt()
private const val TALLO_SOMBRA = 0xFF4E8C34.toInt()
private const val HOJA_LUZ = 0xFFA4D65E.toInt()
private const val BULBO = 0xFFBFE27A.toInt()
private const val ORO = 0xFFFBBF24.toInt()
private const val ORO_CLARO = 0xFFFEF3C7.toInt()
private const val RUBI = 0xFFE11D48.toInt()

/** Un rectangulo de la rejilla en celdas enteras; cada celda mide [p] px. */
private fun DrawScope.celda(x: Int, y: Int, w: Int, h: Int, c: Int, p: Float, alfa: Float = 1f) {
    drawRect(Color(c), Offset(x * p, y * p), Size(w * p, h * p), alpha = alfa)
}

/**
 * Pinta el cuadro actual del [motor] (el del ultimo tic): maceta, tallo con hojas, medalla, particulas e icono.
 * [p] = px por celda. Se llama solo dentro del `Canvas` (fase de dibujo), asi el tic no recompone nada.
 */
fun DrawScope.dibujarMedalla(motor: MotorMedalla, p: Float, icono: ImageBitmap?) {
    val t = motor.t - 1
    if (t < 0) return
    dibujarMaceta(motor, t, p)
    if (t >= motor.inicioMedalla) dibujarMarco(motor, t - motor.inicioMedalla, p)
    dibujarParticulas(motor, p)
    if (motor.iconoVisible && icono != null) {
        // El icono (18x18) va centrado en la medalla y cae como un sello (escala 1,9 -> 1)
        val lado = 18 * p * motor.escalaIcono
        val centroX = motor.cx * p
        val centroY = motor.my * p
        drawImage(
            image = icono,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(icono.width, icono.height),
            dstOffset = IntOffset(redondear(centroX - lado / 2), redondear(centroY - lado / 2)),
            dstSize = IntSize(redondear(lado), redondear(lado)),
            alpha = motor.alfaIcono,
            filterQuality = FilterQuality.None
        )
    }
}

/** Maceta de terracota (rama de Jose), tallo que crece y hojas que se abren con un vaiven. */
private fun DrawScope.dibujarMaceta(m: MotorMedalla, t: Int, p: Float) {
    val cx = m.cx
    val bot = m.bot
    val sueloY = m.sueloY
    val h = m.h
    fun fila(y: Int, ancho: Int, c: Int) = celda(cx - ancho / 2, y, ancho, 1, c, p)
    for (i in 0 until 3) fila(bot - 12 + i + 1, 24, CONTORNO)
    for (i in 0 until 7) fila(bot - 9 + i + 1, 22 - i, CONTORNO)
    for (i in 0 until 3) fila(bot - 12 + i + 1, 22, TERRACOTA_CLARA)
    for (i in 0 until 7) {
        val a = 20 - i
        fila(bot - 9 + i + 1, a, TERRACOTA)
        celda(cx - a / 2, bot - 9 + i + 1, 2, 1, TERRACOTA_CLARA, p)
        celda(cx + a / 2 - 2, bot - 9 + i + 1, 2, 1, TERRACOTA_SOMBRA, p)
    }
    fila(sueloY, 18, TIERRA)
    fila(sueloY + 1, 18, TIERRA_CLARA)
    if (h > 0) {
        celda(cx - 1, sueloY - h, 2, h, TALLO, p)
        celda(cx, sueloY - h, 1, h, TALLO_SOMBRA, p)
    }
    val vaiven = if (m.estatico) 0 else intArrayOf(0, 0, 1, 1)[(t shr 1) and 3]
    for (k in 0 until 4) {
        val alt = 5 + k * 5
        if (h < alt || alt > m.altoTallo) continue
        val y = sueloY - alt
        val dy = if (k % 2 != 0) vaiven else -vaiven
        for (s in intArrayOf(-1, 1)) {
            val x0 = cx + s
            fun hoja(dx: Int, w: Int, yy: Int, c: Int) =
                celda(if (s < 0) x0 - dx - w + 1 else x0 + dx, y + yy + dy, w, 1, c, p)
            hoja(1, 5, 0, CONTORNO); hoja(2, 6, 1, CONTORNO)
            hoja(1, 4, 1, HOJA_LUZ); hoja(2, 5, 2, TALLO); hoja(4, 3, 3, TALLO_SOMBRA)
        }
    }
    // Brote en la punta mientras la medalla no aparece
    if (!m.iconoVisible && h > 0) celda(cx - 1, sueloY - h - 2, 2, 2, BULBO, p)
}

/** Medalla: marco punto a punto, relleno desde abajo, sello con onda, remaches, alas, corona y brillo. */
private fun DrawScope.dibujarMarco(m: MotorMedalla, t: Int, p: Float) {
    val cx = m.cx
    val cy = m.my
    val radio = m.radio
    val r = m.rareza.coerceIn(0, 3)
    val hex = MotorMedalla.HEX[r]
    val luz = MotorMedalla.LUZ[r]
    val pts = circulo(radio)
    val n = pts.size / 2
    val revelados = floor(n * (t + 1).toDouble() / RING).toInt().coerceIn(0, n)

    // Relleno que sube desde abajo
    val filas = floor((t - RING + 1) * (2.0 * radio) / 3).toInt().coerceIn(0, 2 * radio)
    for (i in 0 until filas) {
        val y = radio - i - 1
        val hh = floor(sqrt(max(0.0, ((radio - 2) * (radio - 2) - y * y).toDouble()) + radio * 0.6)).toInt()
        celda(cx - hh, cy + y, hh * 2 + 1, 1, MotorMedalla.OSC[r], p)
    }
    // Marco dibujandose punto a punto (con la punta en blanco mientras avanza)
    for (i in 0 until revelados) celda(cx + pts[2 * i] - 1, cy + pts[2 * i + 1] - 1, 4, 4, CONTORNO, p)
    for (i in 0 until revelados) celda(cx + pts[2 * i], cy + pts[2 * i + 1], 2, 2, hex, p)
    if (revelados < n) {
        for (i in max(0, revelados - 3) until revelados) celda(cx + pts[2 * i], cy + pts[2 * i + 1], 2, 2, BLANCO, p)
    }
    if (r >= 2 && revelados > 0) {
        // Segundo marco que se dibuja al reves
        val p2 = circulo(radio + 3)
        val n2 = p2.size / 2
        val hasta = floor(n2 * ((t + 1).toDouble() / RING).coerceIn(0.0, 1.0)).toInt()
        var i = 0
        while (i < hasta) {
            val k = n2 - 1 - i
            celda(cx + p2[2 * k], cy + p2[2 * k + 1], 1, 1, luz, p)
            i += 2
        }
    }
    val st = FILL + 1
    if (t < st) return

    if (r >= 1) {
        for ((dx, dy) in arrayOf(0 to -radio, radio to 0, 0 to radio, -radio to 0)) {
            celda(cx + dx - 1, cy + dy - 1, 4, 4, CONTORNO, p)
            celda(cx + dx, cy + dy, 2, 2, BLANCO, p)
        }
    }
    // Onda de choque del sello (doble en epica y legendaria)
    val e = t - st
    for ((d, c) in arrayOf(0 to hex, 2 to luz)) {
        if (d != 0 && r < 2) continue
        val k = e - d
        if (k < 0 || k > 7) continue
        val rr = radio + 4 + k * 4
        val alfa = 1f - k / 8f
        val onda = circulo(rr)
        for (j in 0 until onda.size / 2) celda(cx + onda[2 * j], cy + onda[2 * j + 1], 1, 1, c, p, alfa)
    }
    if (r >= 2) {
        // Alas con aleteo
        val aleteo = if (m.estatico) 0 else ((t shr 2) and 1)
        for (s in intArrayOf(-1, 1)) for (j in 0 until 4) {
            val largo = 11 - j * 2
            val x = if (s < 0) cx - radio - 2 - largo else cx + radio + 3
            val y = cy - 9 + j * 4 - aleteo
            celda(x - 1, y - 1, largo + 2, 5, CONTORNO, p)
            celda(x, y, largo, 3, if (j % 2 != 0) luz else BLANCO, p)
        }
    }
    if (r == 3) {
        // Corona que cae y estrellas orbitando
        if (e >= 3) {
            val dc = intArrayOf(-10, -5, -2, 0)[(e - 3).coerceIn(0, 3)]
            val y0 = cy - radio - 5 + dc
            celda(cx - 7, y0 - 1, 15, 6, CONTORNO, p)
            for (dx in intArrayOf(-6, 0, 5)) celda(cx + dx - 1, y0 - 5, 4, 5, CONTORNO, p)
            celda(cx - 6, y0, 13, 3, ORO, p)
            for (dx in intArrayOf(-5, 0, 5)) celda(cx + dx, y0 - 4, 2, 4, ORO, p)
            celda(cx - 1, y0 + 1, 2, 1, RUBI, p)
            celda(cx - 5, y0, 1, 1, BLANCO, p)
        }
        if (!m.estatico) for (i in 0 until 6) {
            val a = t * 0.15 + i * PI / 3
            destelloFijo(cx + cos(a) * (radio + 9), cy + sin(a) * (radio + 8), if (i % 2 != 0) ORO_CLARO else ORO, p)
        }
    }
    if (r >= 2 && !m.estatico) {
        // Brillo que cruza la medalla cada ~2,2 s
        val k = (t - st) % 28
        if (k < 8) {
            val off = -radio + k * (2.0 * radio / 7)
            for (y in (-radio + 3) until (radio - 3)) {
                val x = redondear(off - y * 0.6 + radio * 0.3)
                val lim = floor(sqrt(((radio - 3) * (radio - 3) - y * y).toDouble())).toInt()
                if (abs(x) < lim) celda(cx + x, cy + y, 2, 1, BLANCO, p, 0.55f)
            }
        }
    }
}

private fun DrawScope.destelloFijo(x: Double, y: Double, c: Int, p: Float) {
    val xi = redondear(x)
    val yi = redondear(y)
    celda(xi, yi, 1, 1, c, p)
    celda(xi - 1, yi, 3, 1, c, p)
    celda(xi, yi - 1, 1, 3, c, p)
}

/** Destellos (punto que se vuelve cruz a media vida) y hojas que vuelan con gravedad. */
private fun DrawScope.dibujarParticulas(m: MotorMedalla, p: Float) {
    for (i in 0 until m.nParticulas) {
        val x = redondear(m.px[i])
        val y = redondear(m.py[i])
        val c = m.color[i]
        if (m.tipo[i] == DESTELLO) {
            celda(x, y, 1, 1, c, p)
            if (abs(m.vida[i] - m.vidaTotal[i] / 2f) < m.vidaTotal[i] / 4f) {
                celda(x - 1, y, 3, 1, c, p)
                celda(x, y - 1, 1, 3, c, p)
            }
        } else {
            celda(x, y, m.ancho[i], m.alto[i], c, p)
        }
    }
}
