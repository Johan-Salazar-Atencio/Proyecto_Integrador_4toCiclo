package com.plantia.ui.logros

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Tiempos y particulas de la medalla que brota, iguales al prototipo aprobado. */
class MotorMedallaTest {

    private fun motor(rareza: Int, estatico: Boolean = false, alTerminar: () -> Unit = {}) =
        MotorMedalla(rareza, estatico, Random(42), alTerminar).apply { colocar(60, 140, 120, 260) }

    @Test
    fun la_duracion_crece_con_la_rareza_como_en_el_prototipo() {
        // fin = 2 + ceil(H / 3) + 9 + 10, con H = 10, 12, 14, 16  ->  ~2,0 a 2,2 s a 80 ms por tic
        assertEquals(listOf(25, 25, 26, 27), (0..3).map { motor(it).fin })
        assertEquals(listOf(11, 12, 13, 14), (0..3).map { motor(it).radio })
    }

    @Test
    fun avisa_que_termino_una_sola_vez_y_justo_al_final() {
        var veces = 0
        val m = motor(0) { veces++ }

        repeat(m.fin - 1) { m.tic() }
        assertFalse(m.terminada)
        assertEquals(0, veces)

        m.tic()
        assertTrue(m.terminada)
        repeat(50) { m.tic() }
        assertEquals(1, veces)
    }

    @Test
    fun adelantar_salta_al_final_y_avisa() {
        var veces = 0
        val m = motor(3) { veces++ }
        m.tic()

        m.adelantar()

        assertTrue(m.terminada)
        assertTrue(m.tarjetaVisible)
        assertEquals(1, veces)
        // Adelantar otra vez no repite el aviso
        m.adelantar()
        assertEquals(1, veces)
    }

    @Test
    fun con_animaciones_quitadas_queda_el_cuadro_final_sin_particulas() {
        var veces = 0
        val m = motor(2, estatico = true) { veces++ }
        assertTrue(m.tarjetaVisible)

        m.completarEstatico()

        assertTrue(m.terminada)
        assertEquals(0, m.nParticulas)
        assertTrue(m.iconoVisible)
        assertEquals(1f, m.escalaIcono)
        assertTrue(m.debeParar())
        assertEquals(1, veces)
    }

    @Test
    fun el_sello_cae_grande_y_tiembla_al_caer() {
        val m = motor(2)
        val sello = m.inicioMedalla + m.sello
        repeat(sello) { m.tic() }
        assertFalse(m.iconoVisible)

        m.tic() // tic del sello
        assertTrue(m.iconoVisible)
        assertEquals(1.9f, m.escalaIcono)
        assertEquals(0.7f, m.alfaIcono)
        assertEquals(2, kotlin.math.abs(m.shx)) // epica tiembla con fuerza 2

        repeat(3) { m.tic() }
        assertEquals(1f, m.escalaIcono)
        repeat(5) { m.tic() }
        assertEquals(0, m.shx)
        assertEquals(0, m.shy)
    }

    @Test
    fun la_tarjeta_sube_dos_tics_despues_del_sello() {
        val m = motor(1)
        repeat(m.inicioMedalla + m.sello + 2) { m.tic() }
        assertFalse(m.tarjetaVisible)
        m.tic()
        assertTrue(m.tarjetaVisible)
    }

    @Test
    fun las_particulas_al_sellar_son_las_del_prototipo() {
        // anillo de destellos + hojas: 8+10, 10+16, 14+24, 18+36
        val esperado = listOf(18, 26, 38, 54)
        for (r in 0..3) {
            val m = motor(r)
            repeat(m.fin + 40) { m.tic() }
            assertEquals("rareza $r", esperado[r], m.pico)
            assertTrue(m.pico <= MotorMedalla.MAX_PARTICULAS)
        }
    }

    @Test
    fun el_bucle_se_apaga_solo() {
        val comun = motor(0)
        var n = 0
        while (!comun.debeParar() && n++ < 1000) comun.tic()
        assertTrue(comun.t in (comun.fin + 70)..(comun.fin + 131))

        // Epica y legendaria siguen con sus adornos hasta ~10 s y luego paran
        val legendaria = motor(3)
        n = 0
        while (!legendaria.debeParar() && n++ < 1000) legendaria.tic()
        assertEquals(legendaria.fin + 131, legendaria.t)
    }

    @Test
    fun el_tallo_crece_de_3_en_3_hasta_su_alto() {
        val m = motor(3)
        val altos = (0 until 10).map { m.tic(); m.h }
        assertEquals(listOf(0, 0, 0, 3, 6, 9, 12, 15, 16, 16), altos)
    }

    @Test
    fun el_circulo_no_repite_puntos_y_empieza_arriba() {
        val c = MotorMedalla.circulo(11)
        val pares = c.toList().chunked(2)
        assertEquals(pares.size, pares.toSet().size)
        assertEquals(listOf(0, -11), pares.first())
    }

    @Test
    fun redondea_como_javascript() {
        assertEquals(3, MotorMedalla.redondear(2.5))
        assertEquals(-2, MotorMedalla.redondear(-2.5))
        assertEquals(-3, MotorMedalla.redondear(-2.6))
    }
}
