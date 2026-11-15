package com.plantia.ui.escena

import com.plantia.data.model.Apariencia
import kotlin.math.roundToInt

/**
 * Sprites de la planta (32x32) compartidos con la web: el JSON sale de
 * `scripts/arte/exportar_sprites.mjs` a partir de plantaPixelSprites.js. Cada letra de un sprite es
 * un color; los de hoja, fruto y flor se calculan del color de la especie con `reglasColor`.
 */
data class PlantaSpritesJson(
    val lado: Int = 32,
    val tierra: List<String> = emptyList(),
    val paletaFija: Map<String, String> = emptyMap(),
    /** grupo (hoja, fruto, flor) -> letra -> [operacion ("base", "oscuro" o "claro"), cantidad]. */
    val reglasColor: Map<String, Map<String, List<Any>>> = emptyMap(),
    val defecto: Defecto = Defecto(),
    /** forma -> "<flor><fruto>" (1 o 0) -> id del conjunto de 7 sprites. */
    val formas: Map<String, Map<String, String>> = emptyMap(),
    val conjuntos: Map<String, List<List<String>>> = emptyMap()
) {
    data class Defecto(
        val forma: String = "arbusto",
        val colorHoja: String = "#4f9a52",
        val tieneFlor: Boolean = false,
        val tieneFruto: Boolean = true,
        val colorFruto: String = "#d9473b",
        val colorFlorSinColor: String = "#f472b6"
    )
}

/** Pixeles contiguos del mismo color en una fila, para dibujar menos rectangulos. */
data class Tramo(val x: Int, val y: Int, val ancho: Int, val color: Int)

/** Indice del sprite marchito dentro de cada conjunto. */
const val SPRITE_MARCHITA = 6

/**
 * Sprite que toca segun el avance (semilla, brote, joven, adulta, con botones o frutos verdes, y la
 * ultima, completa). Es un avance fino dentro de las 3 fases del servidor: cada dia importante se
 * nota sin cambiar la BD. Sin datos del servidor (total 0) se muestra la planta adulta.
 */
fun indiceSprite(dias: Int, total: Int, marchita: Boolean): Int {
    if (marchita) return SPRITE_MARCHITA
    if (total <= 0) return 3
    if (dias <= 0) return 0
    val avance = dias.toDouble() / total
    return when {
        avance >= 1.0 -> 5
        avance >= 0.8 -> 4
        avance >= 0.6 -> 3
        avance >= 0.34 -> 2
        else -> 1
    }
}

/** Fase 1 a 3 como la calcula el servidor: la segunda empieza al 35 % de los dias y la tercera al completarlos. */
fun faseNumero(dias: Int, total: Int): Int = when {
    total <= 0 -> 1
    dias >= total -> 3
    dias >= kotlin.math.ceil(total * 0.35).toInt() -> 2
    else -> 1
}

/** Los 7 sprites (solo la planta) que corresponden a la apariencia. */
fun spritesDe(datos: PlantaSpritesJson, ap: Apariencia?): List<List<String>> {
    val d = datos.defecto
    val flor = ap?.tieneFlor ?: d.tieneFlor
    val fruto = ap?.tieneFruto ?: d.tieneFruto
    val porForma = datos.formas[ap?.forma ?: d.forma] ?: datos.formas[d.forma].orEmpty()
    val id = porForma["${if (flor) 1 else 0}${if (fruto) 1 else 0}"]
    return datos.conjuntos[id].orEmpty()
}

/** Letra -> color ARGB, con los colores de hoja, fruto y flor sacados de la apariencia. */
fun paletaDe(datos: PlantaSpritesJson, ap: Apariencia?): Map<Char, Int> {
    val d = datos.defecto
    val hoja = ap?.colorHoja?.takeIf { it.isNotBlank() } ?: d.colorHoja
    val fruto = ap?.colorFruto?.takeIf { it.isNotBlank() } ?: ap?.colorFlor?.takeIf { it.isNotBlank() } ?: d.colorFruto
    val flor = ap?.colorFlor?.takeIf { it.isNotBlank() } ?: ap?.colorFruto?.takeIf { it.isNotBlank() } ?: d.colorFlorSinColor
    val paleta = HashMap<Char, Int>()
    datos.paletaFija.forEach { (letra, hex) -> paleta[letra[0]] = colorDe(hex) }
    mapOf("hoja" to hoja, "fruto" to fruto, "flor" to flor).forEach { (grupo, base) ->
        datos.reglasColor[grupo]?.forEach { (letra, regla) ->
            paleta[letra[0]] = aplicarRegla(colorDe(base), regla[0] as String, (regla[1] as Number).toDouble())
        }
    }
    return paleta
}

fun tramos(filas: List<String>, paleta: Map<Char, Int>): List<Tramo> {
    val res = ArrayList<Tramo>()
    filas.forEachIndexed { y, fila ->
        var x = 0
        while (x < fila.length) {
            val c = fila[x]
            var fin = x + 1
            while (fin < fila.length && fila[fin] == c) fin++
            val color = paleta[c]
            if (c != '.' && color != null) res.add(Tramo(x, y, fin - x, color))
            x = fin
        }
    }
    return res
}

/** "#rrggbb" -> ARGB opaco. */
fun colorDe(hex: String): Int = 0xFF000000.toInt() or (hex.trim().removePrefix("#").toInt(16) and 0xFFFFFF)

private fun aplicarRegla(base: Int, operacion: String, t: Double): Int = when (operacion) {
    "oscuro" -> mezclar(base, 0x000000, t)
    "claro" -> mezclar(base, 0xFFFFFF, t)
    else -> base
}

private fun mezclar(a: Int, b: Int, t: Double): Int {
    fun canal(n: Int) = (((a shr n) and 255) * (1 - t) + ((b shr n) and 255) * t).roundToInt()
    return 0xFF000000.toInt() or (canal(16) shl 16) or (canal(8) shl 8) or canal(0)
}
