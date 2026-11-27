package com.plantia.ui.escena

import com.plantia.data.model.Apariencia
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * Plantas en pixel art (64x64): port exacto del motor de la web
 * (`frontend-react/src/components/plantaPixelSprites.js`), para que el movil dibuje pixel a pixel lo mismo.
 *
 * Cada planta se compone con piezas parametricas (hoja, tallo, cilindro, esfera) sobre una maceta, con rampas de
 * 6 tonos, luz desde arriba a la izquierda y contorno oscuro. El balanceo redibuja cada fotograma con las hojas un
 * poco desplazadas, y la planta marchita (o muerta) dobla de verdad sus hojas.
 *
 * Para que salga identico a JavaScript se imitan sus reglas: `Math.round` (empates hacia +infinito), el azar con
 * semilla en enteros de 32 bits, `Math.hypot` de V8 y seno, coseno y potencia de fdlibm (`StrictMath`).
 * `PlantaPixel64Test` compara las huellas de cientos de fotogramas con las que da la web: si se cambia el motor
 * de la web, regenerar `planta64_huellas.txt` (ver el test) y portar aqui el mismo cambio.
 */
object MotorPlanta64 {
    const val LADO = 64
    const val FOTOGRAMAS = 12
    const val ETAPA_MARCHITA = 6
    const val MARCHITA = "marchita"
    const val MUERTA = "muerta"

    /** La maceta la dibuja otro (el estante del invernadero): solo la planta. */
    const val SIN_MACETA = "NINGUNA"

    private const val SUELO = 47
    private const val CX = 32
    private val LUZ = doubleArrayOf(-0.55, -0.83)
    private val CONTORNO = intArrayOf(44, 32, 40)
    private val ESPINA = intArrayOf(244, 236, 208)

    // ---------------------------------------------------------------------------------------------- apariencia

    data class Ap(
        val forma: String,
        val colorHoja: String,
        val tieneFlor: Boolean,
        val colorFlor: String?,
        val tieneFruto: Boolean,
        val colorFruto: String?
    )

    val APARIENCIA_DEFECTO = Ap("arbusto", "#4f9a52", false, null, true, "#d9473b")

    private val HEX = Regex("^#?([0-9a-f]{6}|[0-9a-f]{3})$", RegexOption.IGNORE_CASE)

    private fun hex(valor: String?, defecto: String?): String? {
        val m = HEX.find((valor ?: "").trim()) ?: return defecto
        val g = m.groupValues[1]
        val h = if (g.length == 3) g.map { "$it$it" }.joinToString("") else g
        return "#${h.lowercase()}"
    }

    fun aparienciaDe(a: Apariencia?): Ap {
        if (a == null) return APARIENCIA_DEFECTO
        return Ap(
            forma = if (a.forma != null && a.forma in FORMAS) a.forma else "arbusto",
            colorHoja = hex(a.colorHoja, APARIENCIA_DEFECTO.colorHoja)!!,
            tieneFlor = a.tieneFlor == true,
            colorFlor = hex(a.colorFlor, null),
            tieneFruto = a.tieneFruto == true,
            colorFruto = hex(a.colorFruto, null)
        )
    }

    private val SOLO_FOLLAJE = setOf("monstera", "helecho", "palmera", "trepadora")
    private val SOLO_FLOR = setOf("suculenta", "roseta", "cactus")

    /** Como madura la planta en sus dos ultimas etapas: "fruto", "flor" o "follaje". */
    fun cicloDe(ap: Ap): String {
        if (ap.forma in SOLO_FOLLAJE) return "follaje"
        if (ap.forma in SOLO_FLOR) return if (ap.tieneFlor || ap.tieneFruto) "flor" else "follaje"
        if (ap.tieneFruto) return "fruto"
        return if (ap.tieneFlor) "flor" else "follaje"
    }

    // ---------------------------------------------------------------------------------------------- aritmetica de JS

    /** `Math.round` de JavaScript: al entero mas cercano y los empates hacia +infinito. */
    internal fun redondear(x: Double): Double {
        val f = floor(x)
        return if (x - f >= 0.5) f + 1 else f
    }

    private fun ri(x: Double): Int = redondear(x).toInt()
    private fun sin(x: Double) = StrictMath.sin(x)
    private fun cos(x: Double) = StrictMath.cos(x)
    private fun pow(a: Double, b: Double) = StrictMath.pow(a, b)
    private fun sqrt(x: Double) = StrictMath.sqrt(x)

    /** `Math.hypot` de V8 (escala por el mayor y suma de Kahan): Java calcula distinto en el ultimo bit. */
    private fun hypot(a: Double, b: Double): Double {
        val x = abs(a)
        val y = abs(b)
        val mayor = max(x, y)
        if (mayor == 0.0) return 0.0
        var suma = 0.0
        var compensacion = 0.0
        for (v in doubleArrayOf(x, y)) {
            val n = v / mayor
            val sumando = n * n - compensacion
            val previa = suma + sumando
            compensacion = (previa - suma) - sumando
            suma = previa
        }
        return sqrt(suma) * mayor
    }

    // ---------------------------------------------------------------------------------------------- color

    private fun aRgb(h: String): IntArray {
        val n = h.substring(1).toInt(16)
        return intArrayOf((n shr 16) and 255, (n shr 8) and 255, n and 255)
    }

    private fun aHsl(c: IntArray): DoubleArray {
        val r = c[0] / 255.0
        val g = c[1] / 255.0
        val b = c[2] / 255.0
        val mx = max(r, max(g, b))
        val mn = min(r, min(g, b))
        val l = (mx + mn) / 2
        if (mx == mn) return doubleArrayOf(0.0, 0.0, l)
        val d = mx - mn
        val s = if (l > 0.5) d / (2 - mx - mn) else d / (mx + mn)
        val h = when (mx) {
            r -> (g - b) / d + (if (g < b) 6 else 0)
            g -> (b - r) / d + 2
            else -> (r - g) / d + 4
        }
        return doubleArrayOf(h * 60, s, l)
    }

    private fun deHsl(h: Double, s0: Double, l0: Double): IntArray {
        val s = max(0.0, min(1.0, s0))
        val l = max(0.0, min(1.0, l0))
        val c = (1 - abs(2 * l - 1)) * s
        val x = c * (1 - abs(((h / 60) % 2) - 1))
        val m = l - c / 2
        val (r, g, b) = when {
            h < 60 -> Triple(c, x, 0.0)
            h < 120 -> Triple(x, c, 0.0)
            h < 180 -> Triple(0.0, c, x)
            h < 240 -> Triple(0.0, x, c)
            h < 300 -> Triple(x, 0.0, c)
            else -> Triple(c, 0.0, x)
        }
        return intArrayOf(ri((r + m) * 255), ri((g + m) * 255), ri((b + m) * 255))
    }

    /** Acerca el tono [h] al [objetivo] por el camino corto del circulo cromatico. */
    private fun virar(h: Double, objetivo: Double, t: Double) = (h + ((((objetivo - h + 540) % 360) - 180) * t) + 360) % 360

    private fun mezclar(a: String, b: String, t: Double): String {
        val x = aRgb(a)
        val y = aRgb(b)
        fun c(i: Int) = ri(x[i] * (1 - t) + y[i] * t).toString(16).padStart(2, '0')
        return "#${c(0)}${c(1)}${c(2)}"
    }

    private class Rampa(val tonos: Array<IntArray>) {
        operator fun get(i: Int) = tonos[i]
    }

    /** Rampa de 6 tonos (0 = contorno ... 5 = brillo). La sombra vira a azul y la luz a amarillo. */
    private fun rampa(color: String, hoja: Boolean = false): Rampa {
        val hsl = aHsl(aRgb(color))
        val h = hsl[0]
        var s = hsl[1]
        var l = hsl[2]
        if (hoja) {
            l = max(0.3, min(0.5, l))
            s = max(0.22, min(0.6, s))
        }
        val fria = max(s, 0.16)
        return Rampa(
            arrayOf(
                deHsl(virar(h, 245.0, 0.34), fria * 1.05, l * 0.3),
                deHsl(virar(h, 230.0, 0.22), fria, l * 0.52),
                deHsl(virar(h, 215.0, 0.1), s, l * 0.76),
                deHsl(h, s, l),
                deHsl(virar(h, 62.0, 0.16), s * 0.95, l + (1 - l) * 0.24),
                deHsl(virar(h, 58.0, 0.28), s * 0.85, l + (1 - l) * 0.5),
            )
        )
    }

    // ---------------------------------------------------------------------------------------------- lienzo

    /** d = RGBA; m = mascara de "solido" (lo que recibe contorno). */
    private class Lienzo {
        val d = IntArray(LADO * LADO * 4)
        val m = IntArray(LADO * LADO)
    }

    private fun pon(l: Lienzo, x: Int, y: Int, c: IntArray, solido: Int = 1, alfa: Int = 255) {
        if (x < 0 || x >= LADO || y < 0 || y >= LADO) return
        val i = (y * LADO + x) * 4
        l.d[i] = c[0]; l.d[i + 1] = c[1]; l.d[i + 2] = c[2]; l.d[i + 3] = alfa
        l.m[y * LADO + x] = solido
    }

    private fun hay(l: Lienzo, x: Int, y: Int) = x >= 0 && x < LADO && y >= 0 && y < LADO && l.d[(y * LADO + x) * 4 + 3] > 0

    /** Contorno oscuro alrededor de todo lo solido (solo sobre pixeles vacios). */
    private fun contornear(l: Lienzo) {
        val marca = ArrayList<Int>()
        fun s(xx: Int, yy: Int) = xx >= 0 && xx < LADO && yy >= 0 && yy < LADO && l.m[yy * LADO + xx] == 1
        for (y in 0 until LADO) {
            for (x in 0 until LADO) {
                if (hay(l, x, y)) continue
                if (s(x - 1, y) || s(x + 1, y) || s(x, y - 1) || s(x, y + 1)) marca.add(y * LADO + x)
            }
        }
        for (p in marca) pon(l, p % LADO, p / LADO, CONTORNO, 2)
    }

    /** Azar con semilla (mulberry32): cada fotograma repite exactamente las mismas decisiones. */
    private class Azar(semilla: Int) {
        private var a = semilla
        fun siguiente(): Double {
            a += 0x6d2b79f5
            var t = (a xor (a ushr 15)) * (1 or a)
            t = (t + (t xor (t ushr 7)) * (61 or t)) xor t
            return ((t xor (t ushr 14)).toLong() and 0xffffffffL).toDouble() / 4294967296.0
        }
    }

    // ---------------------------------------------------------------------------------------------- piezas

    private class Rampas(
        val hoja: Rampa, val amarilla: Rampa, val brote: Rampa, val tallo: Rampa, val talloSeco: Rampa,
        val punta: Rampa, val flor: Rampa, val centro: Rampa, val fruto: Rampa, val verde: Rampa,
        val tronco: Rampa, val tutor: Rampa, val semilla: Rampa
    )

    private class Pincel(
        val l: Lienzo,
        val k: Int,
        val marchita: Boolean,
        val azar: Azar,
        var amp: Double,
        var aleteo: Double,
        val caida: Double,
        val R: Rampas
    ) {
        fun rng() = azar.siguiente()
    }

    private fun fase(P: Pincel) = (2 * Math.PI * P.k) / FOTOGRAMAS

    /** Viento: desplaza un punto segun su altura sobre la maceta (la base no se mueve). */
    private fun mec(P: Pincel, x: Double, y: Double): DoubleArray {
        val h = max(0.0, SUELO - y) / 44
        val f = fase(P)
        var dx = P.amp * (pow(h, 1.4) * sin(f) + 0.25 * h * sin(2 * f + 1.3))
        if (P.marchita) dx = dx * 0.3 + P.caida * h * h
        return doubleArrayOf(x + dx, y + abs(dx) * 0.12)
    }

    private fun interface Curva {
        fun en(t: Double): DoubleArray
    }

    private fun bez(x0: Double, y0: Double, cx: Double, cy: Double, x1: Double, y1: Double) = Curva { t ->
        doubleArrayOf(
            pow(1 - t, 2.0) * x0 + 2 * (1 - t) * t * cx + t * t * x1,
            pow(1 - t, 2.0) * y0 + 2 * (1 - t) * t * cy + t * t * y1
        )
    }

    private fun muestrear(curva: Curva, n: Int): List<DoubleArray> = (0..n).map { curva.en(it.toDouble() / n) }

    private class Cercano(
        var d: Double = 1e9, var t: Double = 0.0, var lado: Int = 1, var dx: Double = 0.0, var dy: Double = -1.0,
        var atras: Boolean = false, var tNeg: Double = 0.0, var dNeg: Double = 0.0
    )

    /** Punto de la polilinea mas cercano a (x, y): distancia, parametro 0..1, lado y direccion. */
    private fun cercano(pts: List<DoubleArray>, x: Double, y: Double): Cercano {
        val n = pts.size - 1
        val r = Cercano()
        for (i in 0 until n) {
            val ax = pts[i][0]
            val ay = pts[i][1]
            val vx = pts[i + 1][0] - ax
            val vy = pts[i + 1][1] - ay
            val l2 = (vx * vx + vy * vy).let { if (it == 0.0) 1e-6 else it }
            val u = ((x - ax) * vx + (y - ay) * vy) / l2
            val uc = max(0.0, min(1.0, u))
            val d2 = pow(x - ax - vx * uc, 2.0) + pow(y - ay - vy * uc, 2.0)
            if (d2 < r.d) {
                val cruz = vx * (y - ay) - vy * (x - ax)
                val largo = sqrt(l2)
                r.d = d2; r.t = (i + uc) / n; r.lado = if (cruz >= 0) 1 else -1
                r.dx = vx / largo; r.dy = vy / largo
                r.atras = i == 0 && u < 0
                r.tNeg = u / n; r.dNeg = abs(cruz) / largo
            }
        }
        r.d = sqrt(r.d)
        return r
    }

    /** Ancho relativo (0..1) de la hoja a lo largo del nervio (t: 0 = base, 1 = punta). */
    private fun perfil(nombre: String, t: Double): Double = when (nombre) {
        "lanza" -> if (t <= 0 || t >= 1) 0.0 else pow(sin(Math.PI * pow(t, 0.9)), 1.15)
        "gorda" -> if (t <= 0 || t >= 1) 0.0 else pow(sin(Math.PI * pow(t, 0.62)), 0.5)
        "corazon" -> when {
            t >= 1 || t <= -0.14 -> 0.0
            t > 0.2 -> pow(cos(((t - 0.2) / 0.8) * (Math.PI / 2)), 0.85)
            else -> sqrt(max(0.0, 1 - pow((0.2 - t) / 0.34, 2.0)))
        }
        else -> if (t <= 0 || t >= 1) 0.0 else pow(sin(Math.PI * pow(t, 0.75)), 0.8)
    }

    private data class OpHoja(
        val perfil: String = "ovada",
        val capa: Int = 0,
        val cortes: Int = 0,
        val agujeros: Boolean = false,
        val punta: Rampa? = null,
        val curva: Double = 0.14,
        val dientes: Int = 0,
        val rampa: Rampa? = null,
        val nervio: Boolean? = null,
        val nervioTono: Int? = null,
        val marcado: Boolean = false
    )

    private class InfoHoja(
        val t: Double, val s: Double, val d: Double, val lado: Int, val dx: Double, val dy: Double, val hueco: Boolean
    )

    /**
     * Hoja: nace en (bx, by), apunta a [ang] grados (0 = arriba, negativo = izquierda) y mide [len] de largo y
     * [ancho] de medio ancho. Con `cortes` se abren ranuras de monstera.
     */
    private fun hoja(P: Pincel, bx: Double, by: Double, ang: Double, len: Double, ancho: Double, o: OpHoja = OpHoja()): DoubleArray {
        var R = o.rampa ?: P.R.hoja
        val desfase = P.rng() * 6.283
        var a = ang
        var L = len
        var cv = o.curva
        if (P.marchita) {
            val sentido = if (a == 0.0) (if (P.rng() < 0.5) -1.0 else 1.0) else sign(a)
            a = max(-168.0, min(168.0, a + sentido * (36 + P.rng() * 24)))
            cv = o.curva + 0.14
            L = len * 0.94
            if (R === P.R.hoja && P.rng() < 0.3) R = P.R.amarilla
        } else {
            a += P.aleteo * sin(fase(P) + desfase)
        }
        val base = mec(P, bx, by)
        val x0 = base[0]
        val y0 = base[1]
        val rad = (a * Math.PI) / 180
        val dx = sin(rad)
        val dy = -cos(rad)
        val x1 = x0 + dx * L
        val y1 = y0 + dy * L
        var nx = -dy
        var ny = dx
        if (ny > 0) { nx = -nx; ny = -ny }
        val k = cv * L * min(1.0, abs(dx) * 1.5)
        val pts = muestrear(bez(x0, y0, (x0 + x1) / 2 + nx * k, (y0 + y1) / 2 + ny * k, x1, y1), 14)
        val ext = if (o.perfil == "corazon") 0.14 else 0.0
        val fino = ancho < 2.3
        val nervio = o.nervio ?: (ancho >= 5)
        val memo = HashMap<Int, InfoHoja?>()

        fun info(x: Int, y: Int): InfoHoja? {
            val clave = y * 256 + x
            if (memo.containsKey(clave)) return memo[clave]
            val q = cercano(pts, x.toDouble(), y.toDouble())
            var t = q.t
            var d = q.d
            var p: InfoHoja? = null
            if (ext != 0.0 && q.atras) { t = q.tNeg; d = q.dNeg }
            // Borde dentado (frondas): el ancho alterna cada `dientes` pixeles a lo largo del nervio
            val diente = if (o.dientes != 0 && floor((t * L) / o.dientes) % 2 == 0.0) 0.6 else 1.0
            val hw = perfil(o.perfil, t) * ancho * diente
            val dentro = (!q.atras || ext != 0.0) && (d <= hw || (d <= 0.5 && t > 0.03 && t < 0.97))
            if (dentro && !(ext != 0.0 && t < 0.05 && d < 0.75)) {
                val s = if (hw > 0) min(1.0, d / hw) else 0.0
                var hueco = false
                if (o.cortes != 0 && t > 0.2 && t < 0.88) {
                    var i = 0
                    while (i < o.cortes && !hueco) {
                        val ti = 0.27 + ((i + 0.5) * 0.56) / o.cortes
                        if (s > 0.36 && abs(t - ti - 0.1 * (s - 0.36)) < 0.55 / L) hueco = true
                        if (o.agujeros && s > 0.13 && s < 0.34 && abs(t - ti + 0.07) < 0.8 / L) hueco = true
                        i++
                    }
                }
                p = InfoHoja(t, s, d, q.lado, q.dx, q.dy, hueco)
            }
            memo[clave] = p
            return p
        }

        val m = ceil(ancho).toInt() + 2
        val minX = pts.minOf { it[0] }
        val maxX = pts.maxOf { it[0] }
        val minY = pts.minOf { it[1] }
        val maxY = pts.maxOf { it[1] }
        for (y in floor(minY).toInt() - m..ceil(maxY).toInt() + m) {
            for (x in floor(minX).toInt() - m..ceil(maxX).toInt() + m) {
                val p = info(x, y) ?: continue
                val borde = info(x - 1, y) == null || info(x + 1, y) == null || info(x, y - 1) == null || info(x, y + 1) == null
                val luz = (-p.dy * LUZ[0] + p.dx * LUZ[1]) * p.lado > 0
                var tono = when {
                    p.hueco -> 0
                    fino -> if (p.t > 0.8) 4 else if (luz) 4 else 3
                    borde -> if (o.marcado) (if (luz) 1 else 0) else if (luz) 2 else 1
                    nervio && p.d < 0.6 && p.t > 0.06 && p.t < 0.82 -> o.nervioTono ?: 5
                    luz -> if (p.s > 0.3 && p.s < 0.6 && p.t > 0.26 && p.t < 0.46 && ancho >= 4) 5 else 4
                    else -> if (p.s > 0.6) 2 else 3
                }
                tono = max(if (p.hueco) 0 else 1, tono - o.capa)
                pon(P.l, x, y, if (o.punta != null && p.t > 0.88) o.punta[tono] else R[tono])
            }
        }
        return doubleArrayOf(x1, y1)
    }

    private data class OpCurva(val grosor: Int = 1, val capa: Int = 0, val rampa: Rampa? = null, val tope: Int? = null)

    /** Tallo o rama: curva de 1 o 2 pixeles de grosor. */
    private fun curva(P: Pincel, x0: Double, y0: Double, cx: Double, cy: Double, x1: Double, y1: Double, o: OpCurva = OpCurva()) {
        val R = if (P.marchita && o.rampa == null) P.R.talloSeco else o.rampa ?: P.R.tallo
        val a = mec(P, x0, y0)
        val c = mec(P, cx, cy)
        val b = mec(P, x1, y1)
        val n = ceil((abs(x1 - x0) + abs(y1 - y0) + abs(cx - x0)) * 1.5).toInt() + 2
        val horizontal = abs(x1 - x0) > abs(y1 - y0)
        for (pt in muestrear(bez(a[0], a[1], c[0], c[1], b[0], b[1]), n)) {
            val px = ri(pt[0])
            val py = ri(pt[1])
            if (o.tope != null && py > o.tope) continue
            if (o.grosor == 1) {
                pon(P.l, px, py, R[max(1, 2 - o.capa)])
            } else if (horizontal) {
                pon(P.l, px, py, R[3])
                pon(P.l, px, py + 1, R[1])
            } else {
                pon(P.l, px, py, R[3])
                pon(P.l, px + 1, py, R[1])
            }
        }
    }

    private data class OpCilindro(
        val costillas: Boolean = false, val anillos: Boolean = false, val tope: Int = LADO,
        val rampa: Rampa? = null, val espinas: IntArray? = null
    )

    private class DatoCilindro(val u: Double, val d: Double, val r: Double)

    /** Cilindro con extremos redondos y sombreado de volumen (cactus, troncos, tutor). */
    private fun cilindro(P: Pincel, x0: Double, y0: Double, x1: Double, y1: Double, r0: Double, r1: Double, o: OpCilindro = OpCilindro()) {
        val R = o.rampa ?: P.R.hoja
        val a = mec(P, x0, y0)
        val b = mec(P, x1, y1)
        val vx = b[0] - a[0]
        val vy = b[1] - a[1]
        val l2 = (vx * vx + vy * vy).let { if (it == 0.0) 1e-6 else it }
        val largo = sqrt(l2)
        val rm = max(r0, r1) + 1
        fun dato(x: Int, y: Int): DatoCilindro? {
            val u = max(0.0, min(1.0, ((x - a[0]) * vx + (y - a[1]) * vy) / l2))
            val d = hypot(x - a[0] - vx * u, y - a[1] - vy * u)
            val r = r0 + (r1 - r0) * u
            return if (y <= o.tope && d <= r + 0.15) DatoCilindro(u, d, r) else null
        }
        for (y in floor(min(a[1], b[1]) - rm).toInt()..ceil(max(a[1], b[1]) + rm).toInt()) {
            for (x in floor(min(a[0], b[0]) - rm).toInt()..ceil(max(a[0], b[0]) + rm).toInt()) {
                val p = dato(x, y) ?: continue
                val lado = if (vx * (y - a[1]) - vy * (x - a[0]) >= 0) 1 else -1
                val luz = ((-vy / largo) * LUZ[0] + (vx / largo) * LUZ[1]) * lado > 0
                val s = min(1.0, p.d / p.r)
                val mm = if (luz) s else -s
                val borde = dato(x - 1, y) == null || dato(x + 1, y) == null || dato(x, y - 1) == null || (y < o.tope && dato(x, y + 1) == null)
                val tono = if (borde) {
                    if (luz) 1 else 0
                } else when {
                    mm > 0.82 -> 3
                    mm > 0.56 -> 4
                    mm > 0.38 -> 5
                    mm > 0.12 -> 4
                    mm > -0.3 -> 3
                    mm > -0.7 -> 2
                    else -> 1
                }
                val lateral = ri(s * p.r * lado)
                val eje = ri(p.u * largo)
                var color = R[tono]
                if (!borde) {
                    if (o.costillas && ((lateral % 3) + 3) % 3 == 1) {
                        color = if (eje % 4 == 1 && o.espinas != null) o.espinas else R[max(1, tono - 1)]
                    }
                    if (o.anillos && eje % 3 == 0) color = R[max(1, tono - 1)]
                }
                pon(P.l, x, y, color)
            }
        }
    }

    private data class OpDisco(val fijo: Boolean = false, val tope: Int? = null, val borde: Boolean = true, val capa: Int = 0)

    /** Esfera sombreada (frutos, botones, copa de arbol). */
    private fun disco(P: Pincel, cx: Double, cy: Double, r: Double, R: Rampa, o: OpDisco = OpDisco()) {
        val c = if (o.fijo) doubleArrayOf(cx, cy) else mec(P, cx, cy)
        val x0 = c[0]
        val y0 = c[1]
        fun dentro(x: Int, y: Int) = pow(x - x0, 2.0) + pow(y - y0, 2.0) <= r * r
        for (y in floor(y0 - r).toInt()..ceil(y0 + r).toInt()) {
            for (x in floor(x0 - r).toInt()..ceil(x0 + r).toInt()) {
                if (!dentro(x, y) || (o.tope != null && y > o.tope)) continue
                val luz = -((x - x0) * 0.6 + (y - y0) * 0.75) / r
                val borde = !dentro(x - 1, y) || !dentro(x + 1, y) || !dentro(x, y - 1) || !dentro(x, y + 1)
                var tono = if (luz > 0.5) 5 else if (luz > 0.15) 4 else if (luz > -0.3) 3 else if (luz > -0.62) 2 else 1
                if (borde && o.borde) tono = if (luz > 0) min(tono, 2) else 0
                pon(P.l, x, y, R[max(0, tono - o.capa)])
            }
        }
    }

    private fun flor(P: Pincel, x: Double, y: Double) {
        val c = mec(P, x, y)
        val cx = c[0]
        val cy = c[1]
        val R = P.R.flor
        for (i in 0 until 5) {
            val a = ((-90 + i * 72) * Math.PI) / 180
            val px = cx + cos(a) * 2.1
            val py = cy + sin(a) * 2.1
            for (yy in floor(py - 1.6).toInt()..ceil(py + 1.6).toInt()) {
                for (xx in floor(px - 1.6).toInt()..ceil(px + 1.6).toInt()) {
                    if (pow(xx - px, 2.0) + pow(yy - py, 2.0) > 2.3) continue
                    val luz = -((xx - cx) * 0.6 + (yy - cy) * 0.75)
                    pon(P.l, xx, yy, R[if (luz > 1.4) 5 else if (luz > -0.4) 4 else if (luz > -1.8) 3 else 2])
                }
            }
        }
        val ox = ri(cx)
        val oy = ri(cy)
        pon(P.l, ox, oy, P.R.centro[4]); pon(P.l, ox - 1, oy, P.R.centro[5])
        pon(P.l, ox, oy + 1, P.R.centro[2]); pon(P.l, ox - 1, oy + 1, P.R.centro[3])
    }

    private fun boton(P: Pincel, x: Double, y: Double) {
        disco(P, x, y, 1.5, P.R.flor)
        val c = mec(P, x, y)
        pon(P.l, ri(c[0]), ri(c[1] + 2), P.R.hoja[2])
        pon(P.l, ri(c[0] - 1), ri(c[1] + 1), P.R.hoja[1])
    }

    private fun fruto(P: Pincel, x: Double, y: Double, maduro: Boolean) {
        disco(P, x, y, if (maduro) 2.7 else 2.3, if (maduro) P.R.fruto else P.R.verde)
        val c = mec(P, x, y)
        pon(P.l, ri(c[0]), ri(c[1] - (if (maduro) 3 else 2)), P.R.hoja[1])
    }

    // ---------------------------------------------------------------------------------------------- maceta

    private data class EstiloMaceta(val color: String, val banda: Boolean = false, val acento: String? = null)

    private fun tonoMaceta(u: Double) =
        if (u < 0.08) 2 else if (u < 0.2) 4 else if (u < 0.3) 5 else if (u < 0.44) 4 else if (u < 0.64) 3 else if (u < 0.84) 2 else 1

    private fun maceta(l: Lienzo, estilo: EstiloMaceta) {
        val R = rampa(estilo.color)
        val A = rampa(estilo.acento ?: "#efe3c8")
        val T = rampa("#5b3a2a")
        // Tono segun la posicion horizontal, con tramado en los cambios de banda
        fun tono(x: Int, y: Int, x0: Int, x1: Int, extra: Int): Int {
            val u = (x - x0).toDouble() / (x1 - x0)
            val sig = tonoMaceta(min(1.0, u + 0.6 / (x1 - x0)))
            val base = if ((x + y) % 2 == 0) sig else tonoMaceta(u)
            return max(1, min(5, base + extra))
        }
        fun fila(y: Int, x0: Int, x1: Int, extra: Int, rampaFila: Rampa = R) {
            for (x in x0..x1) {
                val borde = if (x == x0) 1 else if (x == x1) 0 else null
                pon(l, x, y, rampaFila[borde ?: tono(x, y, x0, x1, extra)])
            }
        }
        // Borde trasero y tierra
        for (x in 21..42) pon(l, x, 45, R[1])
        for ((y, x0, x1) in listOf(Triple(46, 19, 44), Triple(47, 18, 45))) {
            for (x in x0..x1) {
                val dentro = x >= x0 + 2 && x <= x1 - 2
                val mota = (x * 7 + y * 13) % 5 == 0
                pon(l, x, y, if (dentro) T[if (mota) 3 else if (y == 46) 1 else 2] else R[if (x < CX) 4 else 2])
            }
        }
        // Borde delantero, sombra bajo el borde y cuerpo
        fila(48, 18, 45, 1)
        fila(49, 18, 45, 1)
        fila(50, 18, 45, 0)
        fila(51, 20, 43, -2)
        for (y in 52..61) {
            val dentro = floor((y - 52) / 3.4).toInt()
            val banda = estilo.banda && (y == 55 || y == 56)
            fila(y, 20 + dentro, 43 - dentro, if (y >= 60) -1 else 0, if (banda) A else R)
        }
        for (x in 23..40) pon(l, x, 61, R[if (x > 36) 0 else 1])
    }

    private fun sombra(l: Lienzo) {
        for ((fila, alfa) in listOf(intArrayOf(62, 19, 44) to 70, intArrayOf(63, 23, 40) to 38)) {
            val (y, x0, x1) = Triple(fila[0], fila[1], fila[2])
            for (x in x0..x1) if (!hay(l, x, y)) pon(l, x, y, intArrayOf(30, 22, 34), 0, alfa)
        }
    }

    // ---------------------------------------------------------------------------------------------- plantas

    private val S = SUELO.toDouble()
    private val C = CX.toDouble()

    private fun semilla(P: Pincel) {
        disco(P, C - 1.5, S - 1.4, 2.4, P.R.semilla, OpDisco(fijo = true))
        pon(P.l, CX - 1, SUELO - 4, P.R.semilla[1])
        curva(P, C, S - 3, C + 1, S - 5, C + 1.5, S - 7, OpCurva(rampa = P.R.brote))
        hoja(P, C + 1.5, S - 7, 42.0, 3.4, 1.6, OpHoja(rampa = P.R.brote))
    }

    /** Rama lateral del arbusto: hojas alternas y un penacho en la punta. */
    private fun rama(P: Pincel, x0: Double, y0: Double, ang: Double, len: Double, largo: Double, ancho: Double, puntos: MutableList<DoubleArray>) {
        val rad = (ang * Math.PI) / 180
        val x1 = x0 + sin(rad) * len
        val y1 = y0 - cos(rad) * len
        val lado = sign(ang)
        fun ex(t: Double) = x0 + (x1 - x0) * t
        fun ey(t: Double) = y0 + (y1 - y0) * t
        hoja(P, ex(0.6), ey(0.6), ang + lado * 34, largo, ancho, OpHoja(capa = 1))
        hoja(P, ex(0.9), ey(0.9), ang - lado * 40, largo, ancho, OpHoja(capa = 1))
        curva(P, x0, y0, (x0 + x1) / 2, (y0 + y1) / 2 - len * 0.1, x1, y1)
        hoja(P, ex(0.45), ey(0.45), ang + lado * 60, largo * 0.9, ancho)
        hoja(P, ex(0.7), ey(0.7), ang - lado * 52, largo * 0.85, ancho)
        hoja(P, x1, y1, ang + lado * 30, largo, ancho)
        hoja(P, x1, y1, ang - lado * 26, largo * 0.9, ancho)
        hoja(P, x1, y1, ang, largo * 0.7, ancho * 0.8, OpHoja(rampa = P.R.brote))
        puntos.add(doubleArrayOf(x1 + lado * 2, y1 + 4))
        puntos.add(doubleArrayOf((x0 + x1) / 2 + lado, (y0 + y1) / 2 + 4))
    }

    private fun arbusto(P: Pincel, e: Int): List<DoubleArray> {
        val alto = doubleArrayOf(0.0, 7.0, 14.0, 20.0, 24.0, 26.0)[e]
        val largo = doubleArrayOf(0.0, 7.0, 9.0, 9.5, 10.0, 10.5)[e]
        val ancho = doubleArrayOf(0.0, 2.6, 3.2, 3.5, 3.7, 3.9)[e]
        val eje = bez(C, S, C - 2, S - alto * 0.5, C + 0.5, S - alto)
        val tope = eje.en(1.0)
        val tx = tope[0]
        val ty = tope[1]
        val puntos = ArrayList<DoubleArray>()
        val ramas = when {
            e <= 2 -> emptyList()
            e == 3 -> listOf(doubleArrayOf(0.3, -52.0, 12.0), doubleArrayOf(0.44, 50.0, 12.0))
            else -> listOf(doubleArrayOf(0.22, -58.0, 15.0), doubleArrayOf(0.32, 56.0, 16.0), doubleArrayOf(0.54, -38.0, 12.0), doubleArrayOf(0.64, 40.0, 11.0))
        }
        if (e >= 2) {
            for (f in doubleArrayOf(0.55, 0.85)) {
                val p = eje.en(f)
                hoja(P, p[0], p[1], -(34 + P.rng() * 16), largo, ancho, OpHoja(capa = 1))
                hoja(P, p[0], p[1], 36 + P.rng() * 16, largo, ancho, OpHoja(capa = 1))
            }
        }
        curva(P, C, S, C - 2, S - alto * 0.5, C + 0.5, S - alto, OpCurva(grosor = if (e >= 3) 2 else 1))
        for (i in ramas.indices.reversed()) {
            val (f, ang, len) = Triple(ramas[i][0], ramas[i][1], ramas[i][2])
            val p = eje.en(f)
            rama(P, p[0], p[1], ang, len + (if (e == 5) 1 else 0), largo, ancho, puntos)
        }
        if (e == 2) {
            for ((f, ang) in listOf(0.45 to 78.0, 0.75 to 62.0)) {
                val p = eje.en(f)
                hoja(P, p[0], p[1], -ang, largo, ancho)
                hoja(P, p[0] + 1, p[1] - 1, ang, largo, ancho)
            }
        }
        if (e >= 2) {
            hoja(P, tx, ty + 2, -56.0, largo * 0.95, ancho)
            hoja(P, tx, ty + 2, 54.0, largo * 0.95, ancho)
        }
        hoja(P, tx, ty, if (e == 1) -62.0 else -22.0, largo * 0.8, ancho * 0.85, if (e == 1) OpHoja() else OpHoja(rampa = P.R.brote))
        hoja(P, tx, ty, if (e == 1) 60.0 else 20.0, largo * 0.8, ancho * 0.85, if (e == 1) OpHoja() else OpHoja(rampa = P.R.brote))
        puntos.add(doubleArrayOf(tx + 3, ty + 5))
        return puntos.take(7)
    }

    /** dx, dy, angulo, largo, ancho, capa, cortes, agujeros (1 = si). */
    private val MONSTERA: List<List<DoubleArray>> = run {
        fun h(vararg v: Double) = if (v.size == 7) v + 0.0 else v
        val m4 = listOf(
            h(0.0, -31.0, 4.0, 14.0, 7.0, 1.0, 3.0), h(-11.0, -22.0, -78.0, 12.0, 6.0, 1.0, 2.0), h(11.0, -23.0, 76.0, 12.0, 6.0, 1.0, 2.0),
            h(-6.0, -13.0, -114.0, 15.0, 7.6, 0.0, 3.0, 1.0), h(6.0, -15.0, 110.0, 15.5, 7.8, 0.0, 3.0, 1.0),
            h(-4.0, -26.0, -50.0, 15.0, 7.5, 0.0, 3.0, 1.0), h(5.0, -28.0, 44.0, 14.5, 7.3, 0.0, 3.0, 1.0), h(-1.0, -9.0, -152.0, 8.0, 4.4, 0.0, 0.0)
        )
        listOf(
            emptyList(),
            listOf(h(-3.0, -7.0, -50.0, 7.5, 3.8, 0.0, 0.0), h(3.0, -10.0, 40.0, 8.0, 4.0, 0.0, 0.0)),
            listOf(h(0.0, -22.0, 6.0, 10.0, 5.0, 1.0, 0.0), h(-5.0, -12.0, -82.0, 11.0, 5.4, 0.0, 0.0), h(5.0, -15.0, 70.0, 11.5, 5.6, 0.0, 0.0)),
            listOf(
                h(0.0, -29.0, 4.0, 13.0, 6.6, 1.0, 2.0), h(-6.0, -13.0, -112.0, 14.0, 7.0, 0.0, 2.0), h(6.0, -15.0, 108.0, 14.5, 7.2, 0.0, 2.0),
                h(-4.0, -25.0, -52.0, 14.0, 7.0, 0.0, 3.0), h(5.0, -27.0, 46.0, 13.5, 6.8, 0.0, 3.0)
            ),
            m4,
            m4 + listOf(h(2.0, -11.0, 146.0, 8.5, 4.6, 0.0, 0.0)),
        )
    }

    private fun monstera(P: Pincel, e: Int): List<DoubleArray> {
        val hojas = MONSTERA[e]
        fun peciolo(h: DoubleArray) =
            curva(P, C + h[0] * 0.15, S, C + h[0] * 0.3, S + h[1] * 0.6, C + h[0], S + h[1], OpCurva(capa = h[5].toInt()))
        fun pintar(h: DoubleArray) = hoja(
            P, C + h[0], S + h[1], h[2], h[3], h[4],
            OpHoja(perfil = "corazon", capa = h[5].toInt(), cortes = h[6].toInt(), agujeros = h[7] != 0.0, curva = 0.08, nervio = h[4] >= 5)
        )
        hojas.filter { it[5] == 1.0 }.forEach { peciolo(it); pintar(it) }
        hojas.filter { it[5] == 0.0 }.forEach { peciolo(it) }
        if (e == 5) {
            // Hoja nueva aun enrollada
            curva(P, C + 1, S, C + 3, S - 12, C + 2, S - 22)
            hoja(P, C + 2, S - 22, 10.0, 11.0, 1.8, OpHoja(perfil = "lanza", rampa = P.R.brote, nervio = false))
        }
        hojas.filter { it[5] == 0.0 }.forEach { pintar(it) }
        return emptyList()
    }

    private fun roseta(P: Pincel, x: Double, y: Double, s: Double) {
        val o = OpHoja(perfil = "gorda", curva = 0.03, punta = if (P.marchita) null else P.R.punta, marcado = true, nervio = false)
        for (a in doubleArrayOf(-24.0, 24.0)) hoja(P, x, y - 3 * s, a, 15.5 * s, 4.2 * s, o.copy(capa = 1))
        hoja(P, x, y - 3 * s, 0.0, 16.5 * s, 4.4 * s, o.copy(capa = 1))
        for (lado in intArrayOf(-1, 1)) hoja(P, x + lado * s, y - 1.5 * s, lado * 52.0, 15 * s, 4.8 * s, o)
        for (lado in intArrayOf(-1, 1)) hoja(P, x + lado * 2 * s, y, lado * 82.0, 13.5 * s, 4.8 * s, o)
        for (lado in intArrayOf(-1, 1)) hoja(P, x + lado * 1.5, y, lado * 30.0, 10.5 * s, 4.6 * s, o)
        hoja(P, x, y, 0.0, 7 * s, 4 * s, o)
    }

    private fun suculenta(P: Pincel, e: Int, ciclo: String): List<DoubleArray> {
        val florece = ciclo != "follaje" && e >= 4
        val s = if (florece) 1.02 else doubleArrayOf(0.0, 0.42, 0.7, 1.0, 1.08, 1.12)[e]
        if (florece) {
            curva(P, C, S - 8, C + 3, S - 22, C + 7, S - 30, OpCurva(rampa = P.R.punta))
            curva(P, C + 3.5, S - 22, C + 8, S - 24, C + 10, S - 26, OpCurva(rampa = P.R.punta))
            curva(P, C + 2.5, S - 18, C - 1, S - 22, C - 3, S - 24, OpCurva(rampa = P.R.punta))
        } else if (e >= 4) {
            roseta(P, 23.0, S, 0.36)
            if (e >= 5) roseta(P, 41.5, S, 0.38)
        }
        roseta(P, C, S, s)
        return if (florece) listOf(doubleArrayOf(C + 7, S - 32), doubleArrayOf(C + 11, S - 27), doubleArrayOf(C - 4, S - 25)) else emptyList()
    }

    private fun cactus(P: Pincel, e: Int, ciclo: String): List<DoubleArray> {
        val h = doubleArrayOf(0.0, 5.0, 12.0, 20.0, 23.0, 25.0)[e]
        val r = doubleArrayOf(0.0, 3.4, 4.6, 5.8, 6.2, 6.4)[e]
        val o = OpCilindro(costillas = true, espinas = if (P.marchita) null else ESPINA, tope = SUELO)
        val puntos = arrayListOf(doubleArrayOf(C, S - h - r - 1))
        if (e >= 3) {
            val y = S - h * 0.42
            val alto = if (e >= 4) 9.0 else 6.0
            cilindro(P, C - r + 1, y, C - r - 4.5, y, 2.4, 2.4, o)
            cilindro(P, C - r - 4.5, y, C - r - 4.5, y - alto, 2.6, 2.4, o)
            puntos.add(doubleArrayOf(C - r - 4.5, y - alto - 4))
        }
        if (e >= 4) {
            val y = S - h * 0.62
            val alto = if (e >= 5) 8.0 else 6.0
            cilindro(P, C + r - 1, y, C + r + 4, y, 2.2, 2.2, o)
            cilindro(P, C + r + 4, y, C + r + 4, y - alto, 2.4, 2.2, o)
            puntos.add(doubleArrayOf(C + r + 4, y - alto - 4))
        }
        cilindro(P, C, S, C, S - h, r, r * 0.94, o)
        if (ciclo == "follaje" && e >= 5) cilindro(P, C + 8, S, C + 8, S - 3, 2.6, 2.6, o)
        return puntos
    }

    private class Fronda(val a: Double, val largo: Double, val capa: Int)

    private fun helecho(P: Pincel, e: Int): List<DoubleArray> {
        val n = intArrayOf(0, 3, 5, 7, 9, 11)[e]
        val len = doubleArrayOf(0.0, 9.0, 15.0, 21.0, 24.0, 26.0)[e]
        val ancho = doubleArrayOf(0.0, 2.0, 2.6, 3.3, 3.6, 3.8)[e]
        val frondas = ArrayList<Fronda>()
        for (i in 0 until n) {
            val a = -88 + (176 * (i + 0.5)) / n + (P.rng() - 0.5) * 10
            val largo = len * (0.82 + 0.3 * cos((a * Math.PI) / 180)) + P.rng() * 2
            frondas.add(Fronda(a, largo, i % 2))
        }
        for (capa in intArrayOf(1, 0)) {
            for (f in frondas.filter { it.capa == capa }) {
                hoja(P, C + f.a / 30, S, f.a, f.largo, ancho, OpHoja(perfil = "lanza", capa = capa, dientes = 2, curva = 0.3, nervio = true, nervioTono = 2))
            }
        }
        if (e >= 5 && !P.marchita) {
            // Fronda nueva enrollada
            curva(P, C + 1, S, C + 3, S - 5, C + 2, S - 11, OpCurva(rampa = P.R.brote))
            val p = mec(P, C + 2, S - 11)
            for ((dx, dy) in listOf(1 to -1, 2 to -2, 1 to -3, 0 to -2)) pon(P.l, ri(p[0] + dx), ri(p[1] + dy), P.R.brote[4])
        }
        return emptyList()
    }

    private fun palmera(P: Pincel, e: Int): List<DoubleArray> {
        val fr = OpHoja(perfil = "lanza", dientes = 2, curva = 0.34, nervio = true, nervioTono = 2)
        if (e <= 2) {
            val hojas = if (e == 1) listOf(-16.0 to 11.0, 18.0 to 9.0) else listOf(-46.0 to 14.0, -15.0 to 17.0, 17.0 to 16.0, 48.0 to 13.0)
            for ((a, len) in hojas) hoja(P, C, S, a, len, if (e == 1) 2.4 else 3.0, if (e == 1) OpHoja(perfil = "lanza", curva = 0.2) else fr)
            return emptyList()
        }
        val alto = doubleArrayOf(0.0, 0.0, 0.0, 10.0, 14.0, 17.0)[e]
        val n = intArrayOf(0, 0, 0, 6, 7, 9)[e]
        val len = doubleArrayOf(0.0, 0.0, 0.0, 18.0, 20.0, 21.0)[e]
        val cy = S - alto - 1
        val frondas = ArrayList<Fronda>()
        for (i in 0 until n) {
            val a = -116 + (232 * (i + 0.5)) / n + (P.rng() - 0.5) * 8
            val largo = len + P.rng() * 2
            frondas.add(Fronda(a, largo, i % 2))
        }
        for (f in frondas.filter { it.capa == 1 }) hoja(P, C + 1, cy, f.a, f.largo, 4.0, fr.copy(capa = 1))
        cilindro(P, C, S, C + 1, S - alto, 2.4, 1.9, OpCilindro(rampa = P.R.tronco, anillos = true, tope = SUELO))
        for (f in frondas.filter { it.capa == 0 }) hoja(P, C + 1, cy, f.a, f.largo, 4.0, fr.copy(capa = 0))
        disco(P, C + 1, cy, 2.0, P.R.tronco)
        return emptyList()
    }

    private fun copa(P: Pincel, cx: Double, cy: Double, r: Double, capa: Int) {
        val n = ri(r * (if (capa != 0) 1.3 else 1.7))
        if (capa == 0) disco(P, cx, cy, r * 0.72, P.R.hoja, OpDisco(borde = false, capa = 1))
        for (i in 0 until n) {
            val a = (i * 360.0) / n + P.rng() * 30
            val rad = (a * Math.PI) / 180
            val d = r * (if (capa != 0) 0.3 else 0.15)
            val ang = if (a > 180) a - 360 else a
            hoja(P, cx + sin(rad) * d, cy - cos(rad) * d, ang, r * 0.98, 2.7, OpHoja(capa = capa, nervio = false))
        }
    }

    private fun arbol(P: Pincel, e: Int): List<DoubleArray> {
        val alto = doubleArrayOf(0.0, 7.0, 13.0, 17.0, 19.0, 20.0)[e]
        val cima = S - alto
        if (e == 1) {
            curva(P, C, S, C - 1, S - 4, C + 1, cima, OpCurva(rampa = P.R.tronco))
            hoja(P, C + 1, cima, -44.0, 5.5, 2.5)
            hoja(P, C + 1, cima, 40.0, 6.0, 2.6)
            return emptyList()
        }
        if (e == 2) {
            copa(P, C + 1, cima - 2, 5.5, 1)
            curva(P, C, S, C - 1, S - 6, C + 1, cima, OpCurva(rampa = P.R.tronco, grosor = 2))
            copa(P, C + 1, cima - 2, 5.5, 0)
            return emptyList()
        }
        val mas = if (e >= 4) 1 else 0
        val copas = listOf(
            doubleArrayOf(C - 8, cima - 6, 6.5 + mas), doubleArrayOf(C + 9, cima - 7, 6.5 + mas), doubleArrayOf(C + 1, cima - 12, 7.5 + mas)
        )
        for (c in copas) copa(P, c[0], c[1], c[2], 1)
        curva(P, C, cima + 4, C - 5, cima + 1, C - 8, cima - 5, OpCurva(rampa = P.R.tronco, grosor = 2))
        curva(P, C + 1, cima + 5, C + 6, cima + 1, C + 9, cima - 6, OpCurva(rampa = P.R.tronco, grosor = 2))
        cilindro(P, C, S, C + 1, cima - 2, 2.3, 1.5, OpCilindro(rampa = P.R.tronco, tope = SUELO))
        for (c in copas) copa(P, c[0], c[1], c[2], 0)
        if (e >= 4) copa(P, C + 1, cima - 4, 5.5, 0)
        return listOf(
            doubleArrayOf(C - 11, cima - 4), doubleArrayOf(C + 12, cima - 5), doubleArrayOf(C - 2, cima - 15),
            doubleArrayOf(C + 5, cima - 9), doubleArrayOf(C - 5, cima - 8), doubleArrayOf(C + 9, cima - 11)
        )
    }

    private class Nudo(val x: Double, val y: Double, val lado: Int, val len: Double)

    private fun trepadora(P: Pincel, e: Int): List<DoubleArray> {
        val cor = OpHoja(perfil = "corazon", curva = 0.06)
        if (e == 1) {
            curva(P, C, S, C - 1, S - 3, C + 1, S - 6)
            hoja(P, C, S - 4, 62.0, 5.5, 3.0, cor)
            hoja(P, C + 1, S - 6, -52.0, 6.0, 3.2, cor)
            return emptyList()
        }
        val alto = doubleArrayOf(0.0, 0.0, 15.0, 26.0, 32.0, 36.0)[e]
        val n = intArrayOf(0, 0, 4, 7, 9, 11)[e]
        cilindro(P, C, S, C, S - alto, 1.7, 1.6, OpCilindro(rampa = P.R.tutor, anillos = true, tope = SUELO))
        val nudos = ArrayList<Nudo>()
        for (i in 0 until n) {
            val f = (i + 0.5) / n
            val lado = if (i % 2 != 0) 1 else -1
            nudos.add(Nudo(C + lado * 1.5, S - 2 - f * (alto - 1), lado, 7.8 - 2.2 * f + P.rng()))
        }
        var px = C
        var py = S
        for (nudo in nudos) {
            curva(P, px, py, (px + nudo.x) / 2 + nudo.lado * 2, (py + nudo.y) / 2, nudo.x, nudo.y)
            px = nudo.x
            py = nudo.y
        }
        // Ramas que cuelgan por fuera de la maceta
        fun cuelga(lado: Int, caida: Double) {
            val finX = C + lado * 15
            val finY = S + caida
            curva(P, C + lado * 4, S, C + lado * 14, S - 6, finX, finY)
            val camino = bez(C + lado * 4, S, C + lado * 14, S - 6, finX, finY)
            for (t in doubleArrayOf(0.5, 0.78, 1.0)) {
                val p = camino.en(t)
                hoja(P, p[0], p[1], lado * (150 + P.rng() * 20), 5.6, 3.0, cor)
            }
        }
        if (e >= 3) cuelga(-1, if (e >= 4) 9.0 else 5.0)
        if (e >= 4) cuelga(1, if (e >= 5) 10.0 else 6.0)
        for (i in nudos.indices.reversed()) {
            val nudo = nudos[i]
            hoja(P, nudo.x, nudo.y, nudo.lado * (74 + P.rng() * 30), nudo.len, nudo.len * 0.5, cor)
        }
        return emptyList()
    }

    /** amp = balanceo del conjunto (px en la punta); aleteo = giro propio de cada hoja (grados). */
    private class Forma(val amp: Double, val aleteo: Double, val maceta: EstiloMaceta, val dibujo: (Pincel, Int, String) -> List<DoubleArray>)

    private val FORMAS: Map<String, Forma> = mapOf(
        "arbusto" to Forma(1.5, 5.0, EstiloMaceta("#c7683f")) { p, e, _ -> arbusto(p, e) },
        "arbol" to Forma(0.9, 6.0, EstiloMaceta("#4c6c9c", banda = true)) { p, e, _ -> arbol(p, e) },
        "helecho" to Forma(1.2, 7.0, EstiloMaceta("#a67d58")) { p, e, _ -> helecho(p, e) },
        "suculenta" to Forma(0.25, 1.2, EstiloMaceta("#e4d6b8", banda = true, acento = "#c7683f")) { p, e, c -> suculenta(p, e, c) },
        "roseta" to Forma(0.25, 1.2, EstiloMaceta("#8fa3ad")) { p, e, c -> suculenta(p, e, c) },
        "cactus" to Forma(0.0, 0.0, EstiloMaceta("#c7683f", banda = true)) { p, e, c -> cactus(p, e, c) },
        "monstera" to Forma(1.1, 3.5, EstiloMaceta("#e9e5dc")) { p, e, _ -> monstera(p, e) },
        "trepadora" to Forma(0.6, 5.0, EstiloMaceta("#d7a23f")) { p, e, _ -> trepadora(p, e) },
        "palmera" to Forma(1.5, 8.0, EstiloMaceta("#5d6672")) { p, e, _ -> palmera(p, e) },
    )

    // ---------------------------------------------------------------------------------------------- fotogramas

    private fun rampas(ap: Ap, estado: String?): Rampas {
        val verde = ap.colorHoja
        val muerta = estado == MUERTA
        val seco = if (muerta) mezclar(verde, "#6e6258", 0.95) else mezclar(verde, "#b0803a", 0.74)
        val marchita = !estado.isNullOrEmpty()
        val esHoja = !muerta
        return Rampas(
            hoja = rampa(if (marchita) seco else verde, esHoja),
            amarilla = rampa(if (muerta) seco else mezclar(verde, "#c2a83e", 0.55), esHoja),
            brote = rampa(if (marchita) seco else mezclar(verde, "#cfe86e", 0.45), esHoja),
            tallo = rampa(if (muerta) "#5b5149" else mezclar(verde, "#5d6b24", 0.4), esHoja),
            talloSeco = rampa(if (muerta) "#5b5149" else "#7a5a34"),
            punta = rampa(mezclar(verde, ap.colorFlor ?: "#e2637f", 0.5)),
            flor = rampa(ap.colorFlor ?: ap.colorFruto ?: "#f08ab0"),
            centro = rampa("#f2c230"),
            fruto = rampa(ap.colorFruto ?: ap.colorFlor ?: "#d9473b"),
            // Fruto sin madurar: verde lima claro, para que se distinga de las hojas
            verde = rampa(mezclar("#dbe96a", ap.colorFruto ?: "#d9473b", 0.12)),
            tronco = rampa(if (muerta) "#5e554d" else if (marchita) "#7d6650" else "#8a5a3c"),
            tutor = rampa("#a47a52"),
            semilla = rampa("#c79a5e"),
        )
    }

    /** Color del cuerpo de cada maceta de la tienda: los mismos que macetas.js y MacetaPixel.kt. */
    private val CUERPO_MACETA = mapOf(
        "AZUL" to "#4A7FC1", "CREMA" to "#EBD9B0", "ROSA" to "#E88FB0", "DORADA" to "#D9A441", "TERRACOTA" to "#C46A3C"
    )

    private fun estiloMaceta(forma: Forma, codigo: String?): EstiloMaceta {
        if (codigo.isNullOrEmpty()) return forma.maceta
        return forma.maceta.copy(color = CUERPO_MACETA[codigo] ?: CUERPO_MACETA.getValue("TERRACOTA"))
    }

    private fun dibujar(ap: Ap, etapaPedida: Int, k: Int, codigoMaceta: String?, estadoPedido: String?): IntArray {
        val forma = FORMAS.getValue(ap.forma)
        val ciclo = cicloDe(ap)
        var etapa = etapaPedida
        var estado = estadoPedido?.takeIf { it.isNotEmpty() }
        // ETAPA_MARCHITA (6) es el formato antiguo: marchita con el tamano de la adulta
        if (etapa == ETAPA_MARCHITA) {
            etapa = 3
            estado = estado ?: MARCHITA
        }
        val marchita = estado != null
        val l = Lienzo()
        if (codigoMaceta != SIN_MACETA) maceta(l, estiloMaceta(forma, codigoMaceta))
        val P = Pincel(
            l, k, marchita, Azar(ap.forma.length * 7919 + etapa * 104729),
            forma.amp, forma.aleteo, if (estado == MUERTA) 5.5 else 3.5, rampas(ap, estado)
        )
        if (etapa == 0) {
            P.amp = 0.8
            P.aleteo = 9.0
            semilla(P)
        } else {
            val puntos = forma.dibujo(P, etapa, ciclo)
            if (!marchita && etapa >= 4 && ciclo != "follaje") {
                for (p in puntos) {
                    when {
                        ciclo == "fruto" -> fruto(P, p[0], p[1], etapa == 5)
                        etapa == 5 -> flor(P, p[0], p[1])
                        else -> boton(P, p[0], p[1])
                    }
                }
            }
        }
        contornear(l)
        sombra(l)
        return l.d
    }

    // Cache acotada: cada fotograma son 4096 pixeles y un jardin tiene pocas plantas a la vista
    private val cache = object : LinkedHashMap<String, IntArray>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, IntArray>?) = size > 400
    }

    /** Pixeles RGBA (4 valores 0..255 por pixel, 64x64) del fotograma [k] de la planta en esa etapa (0..6). */
    fun fotogramaRgba(apariencia: Apariencia?, etapa: Int, k: Int = 0, codigoMaceta: String? = null, estado: String? = null): IntArray {
        val ap = aparienciaDe(apariencia)
        val e = max(0, min(ETAPA_MARCHITA, etapa))
        val i = ((k % FOTOGRAMAS) + FOTOGRAMAS) % FOTOGRAMAS
        return dibujar(ap, e, i, codigoMaceta, estado)
    }

    /** Mismo fotograma en ARGB (un Int por pixel), listo para un Bitmap; se guarda en cache. */
    fun fotograma(apariencia: Apariencia?, etapa: Int, k: Int = 0, codigoMaceta: String? = null, estado: String? = null): IntArray {
        val ap = aparienciaDe(apariencia)
        val e = max(0, min(ETAPA_MARCHITA, etapa))
        val i = ((k % FOTOGRAMAS) + FOTOGRAMAS) % FOTOGRAMAS
        val clave = "${ap.forma}|${ap.colorHoja}|${ap.colorFlor}|${ap.colorFruto}|${cicloDe(ap)}|$e|$i|${codigoMaceta.orEmpty()}|${estado.orEmpty()}"
        synchronized(cache) { cache[clave]?.let { return it } }
        val argb = aArgb(dibujar(ap, e, i, codigoMaceta, estado))
        synchronized(cache) { cache[clave] = argb }
        return argb
    }

    /** La maceta de esa forma sin nada plantado (RGBA). */
    fun macetaVaciaRgba(apariencia: Apariencia?, codigoMaceta: String? = null): IntArray {
        val ap = aparienciaDe(apariencia)
        val l = Lienzo()
        maceta(l, estiloMaceta(FORMAS.getValue(ap.forma), codigoMaceta))
        contornear(l)
        sombra(l)
        return l.d
    }

    fun aArgb(rgba: IntArray): IntArray = IntArray(LADO * LADO) { p ->
        val i = p * 4
        (rgba[i + 3] shl 24) or (rgba[i] shl 16) or (rgba[i + 1] shl 8) or rgba[i + 2]
    }
}
