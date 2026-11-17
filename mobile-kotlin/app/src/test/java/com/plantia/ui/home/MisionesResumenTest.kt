package com.plantia.ui.home

import com.plantia.data.model.DailyMission
import com.plantia.ui.home.components.resumenDeMisiones
import org.junit.Assert.assertEquals
import org.junit.Test

class MisionesResumenTest {

    private fun diaria(completa: Boolean) = DailyMission("m", "Riego", "+5", 0f, 0, 2, completa)

    private val evento = DailyMission("e", "Prevenir", "+80", 0.5f, 1, 2, false, kind = "EVENT")

    @Test
    fun cuenta_las_diarias_completadas() {
        assertEquals("1 de 3 completadas", resumenDeMisiones(listOf(diaria(true), diaria(false), diaria(false))))
    }

    @Test
    fun la_mision_del_evento_no_cuenta_en_el_resumen() {
        assertEquals("0 de 1 completadas", resumenDeMisiones(listOf(evento, diaria(false))))
    }

    @Test
    fun sin_diarias_lo_dice() {
        assertEquals("Sin misiones diarias hoy", resumenDeMisiones(listOf(evento)))
        assertEquals("Sin misiones diarias hoy", resumenDeMisiones(emptyList()))
    }
}
