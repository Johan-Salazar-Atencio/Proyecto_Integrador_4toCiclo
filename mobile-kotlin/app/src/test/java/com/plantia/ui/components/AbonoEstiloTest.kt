package com.plantia.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AbonoEstiloTest {

    @Test
    fun cada_abono_tiene_su_propio_color() {
        val colores = listOf(AbonoEstilo.MEDIO, AbonoEstilo.UNO, AbonoEstilo.TRES).map { AbonoEstilo.color(it) }
        assertEquals(3, colores.toSet().size)
    }

    @Test
    fun un_codigo_desconocido_usa_el_color_del_suave() {
        assertEquals(AbonoEstilo.color(AbonoEstilo.MEDIO), AbonoEstilo.color(null))
        assertNotEquals(AbonoEstilo.color(AbonoEstilo.MEDIO), AbonoEstilo.color(AbonoEstilo.TRES))
    }

    @Test
    fun cuanto_mas_rinde_mas_hojas_se_dibujan() {
        assertEquals(1, AbonoEstilo.hojas(AbonoEstilo.MEDIO).codePointCount(0, AbonoEstilo.hojas(AbonoEstilo.MEDIO).length))
        assertEquals(3, AbonoEstilo.hojas(AbonoEstilo.TRES).codePointCount(0, AbonoEstilo.hojas(AbonoEstilo.TRES).length))
    }
}
