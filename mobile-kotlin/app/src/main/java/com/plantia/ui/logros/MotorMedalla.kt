package com.plantia.ui.logros

import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Motor de la "medalla que brota" (propuesta D), portado del prototipo aprobado por Johan.
 *
 * Todo va en una rejilla de pixeles logicos (1 celda = P dp en pantalla) y avanza a tics de [TIC_MS] ms, como las
 * plantas de Jose: crece un tallo corto en la maceta, en su punta se dibuja el marco de la medalla, se llena, cae el
 * sello (temblor + anillo de destellos + hojas volando) y sube la tarjeta. Por rareza cambian el alto del tallo, el
 * radio, los remaches, las alas, la corona y la cantidad de partículas.
 *
 * No sabe nada de Compose: la pantalla le da la geometria con [colocar], llama a [tic] y lo pinta leyendo su estado.
 * [alTerminar] se llama UNA sola vez, cuando la animacion llega a su fin (o se adelanta con un toque).
 */
class MotorMedalla(
    /** 0 = comun, 1 = rara, 2 = epica, 3 = legendaria. */
    val rareza: Int,
    /** Animaciones quitadas: sin bucle, se calcula el cuadro final de una vez. */
    val estatico: Boolean,
    private val azar: Random = Random.Default,
    private val alTerminar: () -> Unit = {}
) {
    private val r = rareza.coerceIn(0, 3)

    // ---- Tiempos (en tics) ----
    /** Alto del tallo. */
    val altoTallo = intArrayOf(10, 12, 14, 16)[r]
    /** Tic en que el tallo termina de crecer y empieza la medalla. */
    val inicioMedalla = 2 + ceil(altoTallo / 3.0).toInt()
    val radio = intArrayOf(11, 12, 13, 14)[r]
    /** Tic (contado desde [inicioMedalla]) en que cae el sello. */
    val sello = FILL + 1
    /** Al llegar aqui la animacion termino: sale el aviso de arriba. */
    val fin = inicioMedalla + sello + 10

    // ---- Geometria (celdas) ----
    var cx = 0; private set
    var bot = 0; private set
    var anchoRejilla = 0; private set
    var altoRejilla = 0; private set
    val sueloY: Int get() = bot - 12
    /** Centro de la medalla. */
    val my: Int get() = sueloY - altoTallo - radio - 1

    /** La pantalla lo llama al medir (y cuando cambia de tamaño). */
    fun colocar(cx: Int, bot: Int, anchoRejilla: Int, altoRejilla: Int) {
        this.cx = cx; this.bot = bot; this.anchoRejilla = anchoRejilla; this.altoRejilla = altoRejilla
    }

    // ---- Estado ----
    /** Tics ya ejecutados. Lo que se pinta corresponde al tic [t] - 1. */
    var t = 0; private set
    var terminada = false; private set
    /** La tarjeta del logro ya debe verse (2 tics despues del sello). */
    var tarjetaVisible = estatico; private set
    /** Alto actual del tallo. */
    var h = 0; private set
    private var temblor = 0
    private var fuerzaTemblor = 1
    /** Desplazamiento del temblor, en celdas. */
    var shx = 0; private set
    var shy = 0; private set
    /** Mayor cantidad de particulas vivas a la vez (para medir). */
    var pico = 0; private set

    /** Tics desde que cae el sello (negativo = aun no cae). */
    val desdeSello: Int get() = (t - 1) - inicioMedalla - sello
    val iconoVisible: Boolean get() = desdeSello >= 0
    /** El sello cae grande y se asienta: 1,9 → 1,5 → 1,2 → 1. */
    val escalaIcono: Float get() = if (desdeSello < 0) 1f else ESCALA_SELLO[min(3, desdeSello)]
    val alfaIcono: Float get() = if (desdeSello == 0) 0.7f else 1f

    // ---- Particulas en arreglos preasignados (no se crean objetos por tic) ----
    var nParticulas = 0; private set
    val px = FloatArray(MAX_PARTICULAS)
    val py = FloatArray(MAX_PARTICULAS)
    private val vx = FloatArray(MAX_PARTICULAS)
    private val vy = FloatArray(MAX_PARTICULAS)
    private val gr = FloatArray(MAX_PARTICULAS)
    val vida = IntArray(MAX_PARTICULAS)
    val vidaTotal = IntArray(MAX_PARTICULAS)
    val tipo = IntArray(MAX_PARTICULAS)
    val ancho = IntArray(MAX_PARTICULAS)
    val alto = IntArray(MAX_PARTICULAS)
    val color = IntArray(MAX_PARTICULAS)

    /** Un tic de logica: crecer, soltar particulas, moverlas y ver si ya termino. */
    fun tic() {
        val tt = t++
        logica(tt)
        var j = 0
        for (i in 0 until nParticulas) {
            px[i] += vx[i]; py[i] += vy[i]; vy[i] += gr[i]; vida[i]--
            if (vida[i] > 0 && py[i] < altoRejilla + 6 && py[i] > -14) {
                if (j != i) copiar(i, j)
                j++
            }
        }
        nParticulas = j
        if (j > pico) pico = j
        if (t >= fin && !terminada) {
            terminada = true
            alTerminar()
        }
    }

    /** Toque antes de que acabe: salta al final (las particulas ya soltadas siguen su camino). */
    fun adelantar() {
        var n = 0
        while (!terminada && n++ < 200) tic()
    }

    /** Animaciones quitadas: se calcula el cuadro final sin particulas. */
    fun completarEstatico() {
        var n = 0
        while (!terminada && n++ < 300) tic()
        nParticulas = 0
    }

    /** Ya no hay nada que mover: se apaga el bucle. Los adornos de epica/legendaria siguen como maximo ~10 s. */
    fun debeParar(): Boolean =
        estatico || (t > fin + 70 && nParticulas == 0 && r < 2) || t > fin + 130

    private fun logica(tt: Int) {
        h = (((tt - 2) * 3).coerceIn(0, altoTallo))
        val e = tt - inicioMedalla - sello
        if (e == 0) {
            temblor = 5
            fuerzaTemblor = if (r >= 2) 2 else 1
            anillo(cx.toFloat(), my.toFloat(), intArrayOf(8, 10, 14, 18)[r], COLORES[r], 2.2f)
            val n = intArrayOf(10, 16, 24, 36)[r]
            val hojas = HOJAS[r]
            repeat(n) {
                val a = rnd(-2.9f, -0.25f)
                nueva(
                    HOJA, cx.toFloat(), my.toFloat(),
                    cos(a) * rnd(0.8f, 2.4f), sin(a) * rnd(1.6f, 3.4f), 0.22f, 3, 2, 34, elegir(hojas)
                )
            }
        }
        if (e == 2) tarjetaVisible = true
        sacudir()
    }

    private fun sacudir() {
        if (temblor > 0) {
            val m = fuerzaTemblor
            shx = if (temblor % 2 != 0) m else -m
            shy = if (temblor % 3 == 0) -m else m
            temblor--
        } else {
            shx = 0; shy = 0
        }
    }

    private fun anillo(x: Float, y: Float, n: Int, cols: IntArray, vel: Float) {
        for (i in 0 until n) {
            val a = (i.toFloat() / n * 6.283f)
            nueva(DESTELLO, x + cos(a) * 8, y + sin(a) * 8, cos(a) * vel, sin(a) * vel * 0.9f, 0f, 1, 1, 11, elegir(cols))
        }
    }

    private fun nueva(k: Int, x: Float, y: Float, vx0: Float, vy0: Float, g: Float, w: Int, hh: Int, v: Int, c: Int) {
        if (nParticulas >= MAX_PARTICULAS) return
        val i = nParticulas++
        tipo[i] = k; px[i] = x; py[i] = y; vx[i] = vx0; vy[i] = vy0; gr[i] = g
        ancho[i] = w; alto[i] = hh; vida[i] = v; vidaTotal[i] = v; color[i] = c
    }

    private fun copiar(de: Int, a: Int) {
        tipo[a] = tipo[de]; px[a] = px[de]; py[a] = py[de]; vx[a] = vx[de]; vy[a] = vy[de]; gr[a] = gr[de]
        ancho[a] = ancho[de]; alto[a] = alto[de]; vida[a] = vida[de]; vidaTotal[a] = vidaTotal[de]; color[a] = color[de]
    }

    private fun rnd(a: Float, b: Float) = a + azar.nextFloat() * (b - a)
    private fun elegir(l: IntArray) = l[azar.nextInt(l.size)]

    companion object {
        /** Igual que PlantaPixel de Jose: un tic cada 80 ms. */
        const val TIC_MS = 80L
        /** Un toque antes de este tiempo salta al final; despues, cierra. */
        const val SALTO_MS = 1000L
        const val MAX_PARTICULAS = 160

        /** Tics que tarda el marco en dibujarse y en llenarse (contados desde [inicioMedalla]). */
        const val RING = 6
        const val FILL = RING + 2

        const val DESTELLO = 0
        const val HOJA = 1

        private val ESCALA_SELLO = floatArrayOf(1.9f, 1.5f, 1.2f, 1f)

        /** Contorno oscuro de Jose. */
        const val CONTORNO = 0xFF2C2028.toInt()

        /** Color, luz y sombra de cada rareza (fijos: son el "pixel art", no cambian con el tema). */
        val HEX = intArrayOf(0xFFA3E635.toInt(), 0xFF38BDF8.toInt(), 0xFFC084FC.toInt(), 0xFFFBBF24.toInt())
        val LUZ = intArrayOf(0xFFECFCCB.toInt(), 0xFFE0F2FE.toInt(), 0xFFF3E8FF.toInt(), 0xFFFEF3C7.toInt())
        val OSC = intArrayOf(0xFF365314.toInt(), 0xFF0C4A6E.toInt(), 0xFF4C1D95.toInt(), 0xFF78350F.toInt())
        private const val BLANCO = 0xFFFFFFFF.toInt()
        val COLORES = arrayOf(
            intArrayOf(HEX[0], LUZ[0], BLANCO),
            intArrayOf(HEX[1], LUZ[1], BLANCO),
            intArrayOf(HEX[2], LUZ[2], 0xFFFBCFE8.toInt()),
            intArrayOf(HEX[3], LUZ[3], BLANCO, 0xFFFB923C.toInt())
        )
        private val HOJAS = arrayOf(
            intArrayOf(0xFFA4D65E.toInt(), 0xFF7FC241.toInt()),
            intArrayOf(0xFFA4D65E.toInt(), 0xFF7FC241.toInt(), 0xFFD8F1FF.toInt(), 0xFF58B4F0.toInt()),
            intArrayOf(0xFFA4D65E.toInt(), 0xFF7FC241.toInt(), 0xFFF3E8FF.toInt(), 0xFFC084FC.toInt()),
            intArrayOf(0xFFA4D65E.toInt(), 0xFF7FC241.toInt(), 0xFFFEF3C7.toInt(), 0xFFFBBF24.toInt())
        )

        private val circulos = HashMap<Int, IntArray>()

        /** Puntos (x0, y0, x1, y1...) de un circulo de radio [radio], desde arriba en sentido horario y sin repetir. */
        fun circulo(radio: Int): IntArray = synchronized(circulos) {
            circulos.getOrPut(radio) {
                val n = ceil(radio * 14.0).toInt()
                val vistos = HashSet<Int>()
                val salida = ArrayList<Int>()
                for (i in 0 until n) {
                    val a = -PI / 2 + i.toDouble() / n * PI * 2
                    val x = redondear(cos(a) * radio)
                    val y = redondear(sin(a) * radio)
                    if (vistos.add(x * 1000 + y)) { salida += x; salida += y }
                }
                salida.toIntArray()
            }
        }

        /** Redondeo como `Math.round` de JavaScript (la mitad sube). */
        fun redondear(v: Double): Int = kotlin.math.floor(v + 0.5).toInt()
        fun redondear(v: Float): Int = redondear(v.toDouble())
    }
}
