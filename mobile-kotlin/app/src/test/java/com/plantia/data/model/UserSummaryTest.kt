package com.plantia.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** La barra de XP va de lo que marca el nivel actual a lo que marca el siguiente. */
class UserSummaryTest {

    private fun usuario(xp: Int, desde: Int, hasta: Int) = UserSummary(
        name = "Johan", level = 2, badge = "Aprendiz", leaves = 0,
        experience = xp, experienceToNextLevel = hasta, experienceFromLevel = desde
    )

    @Test
    fun a_mitad_del_nivel_la_barra_va_por_la_mitad() {
        assertEquals(0.5f, usuario(xp = 750, desde = 500, hasta = 1000).progresoNivel, 0.001f)
    }

    @Test
    fun recien_subido_de_nivel_la_barra_arranca_casi_vacia() {
        assertEquals(0.04f, usuario(xp = 520, desde = 500, hasta = 1000).progresoNivel, 0.001f)
    }

    @Test
    fun en_el_ultimo_nivel_la_barra_esta_completa_y_se_marca_nivel_maximo() {
        val u = usuario(xp = 10_500, desde = 10_000, hasta = 10_500)
        assertEquals(1f, u.progresoNivel, 0.001f)
        assertTrue(u.esNivelMaximo)
    }

    @Test
    fun a_mitad_de_nivel_no_es_nivel_maximo() {
        assertFalse(usuario(xp = 750, desde = 500, hasta = 1000).esNivelMaximo)
    }

    @Test
    fun con_un_servidor_viejo_sin_el_dato_base_se_usa_el_xp_total() {
        // experienceFromLevel = 0 por defecto: la barra es xp / siguiente, como antes
        assertEquals(0.5f, usuario(xp = 500, desde = 0, hasta = 1000).progresoNivel, 0.001f)
    }

    @Test
    fun el_progreso_nunca_sale_de_0_a_1() {
        assertEquals(0f, usuario(xp = 100, desde = 500, hasta = 1000).progresoNivel, 0.001f)
        assertEquals(1f, usuario(xp = 5_000, desde = 500, hasta = 1000).progresoNivel, 0.001f)
    }
}
