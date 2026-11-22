package com.plantia.ui.logros

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconoPixelTest {

    private val contorno = 0xFF2C2028.toInt()

    @Test
    fun un_pixel_lleno_queda_rodeado_de_contorno() {
        val lleno = BooleanArray(LADO_ICONO * LADO_ICONO)
        lleno[5 * LADO_ICONO + 5] = true

        val s = construirSpritePixel(lleno, paletaDeIcono("gota"))
        val n = LADO_SPRITE

        assertNotEquals(0, s[6 * n + 6])
        assertNotEquals(contorno, s[6 * n + 6])
        for ((x, y) in listOf(5 to 6, 7 to 6, 6 to 5, 6 to 7)) assertEquals(contorno, s[y * n + x])
        // Las diagonales no llevan contorno (vecinos en cruz, como el prototipo)
        assertEquals(0, s[5 * n + 5])
    }

    @Test
    fun silueta_vacia_da_sprite_transparente() {
        val s = construirSpritePixel(BooleanArray(LADO_ICONO * LADO_ICONO), paletaDeIcono(null))
        assertTrue(s.all { it == 0 })
        assertEquals(LADO_SPRITE * LADO_SPRITE, s.size)
    }

    @Test
    fun sombrea_luz_arriba_a_la_izquierda_y_sombra_abajo_a_la_derecha() {
        val lleno = BooleanArray(LADO_ICONO * LADO_ICONO) { true }
        val paleta = paletaDeIcono("semilla")
        val s = construirSpritePixel(lleno, paleta)
        val n = LADO_SPRITE
        assertEquals(paleta[0], s[1 * n + 1])
        assertEquals(paleta[2], s[16 * n + 16])
    }

    @Test
    fun clave_desconocida_usa_el_ambar_del_trofeo() {
        assertTrue(paletaDeIcono("clave_nueva_del_admin").contentEquals(paletaDeIcono("nivel")))
        assertTrue(paletaDeIcono(" GOTA ").contentEquals(paletaDeIcono("gota")))
    }
}
