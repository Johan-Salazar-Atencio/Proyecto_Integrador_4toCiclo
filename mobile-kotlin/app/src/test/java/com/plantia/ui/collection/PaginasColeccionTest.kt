package com.plantia.ui.collection

import com.plantia.fakes.planta
import com.plantia.ui.collection.virtual.filtrarPlantas
import com.plantia.ui.collection.virtual.nombreDelInvernadero
import com.plantia.ui.collection.virtual.paginasDelInvernadero
import com.plantia.ui.collection.virtual.totalDePantallas
import org.junit.Assert.assertEquals
import org.junit.Test

class PaginasColeccionTest {

    private fun plantas(n: Int) = (1..n).map { planta(id = it, activa = false) }

    @Test
    fun seis_plantas_caben_en_una_pantalla_y_la_septima_abre_otra() {
        assertEquals(listOf(6), paginasDelInvernadero(plantas(6)).map { it.size })
        assertEquals(listOf(6, 1), paginasDelInvernadero(plantas(7)).map { it.size })
        assertEquals(listOf(6, 6), paginasDelInvernadero(plantas(12)).map { it.size })
    }

    @Test
    fun sin_plantas_hay_una_pantalla_vacia_y_siempre_se_suma_el_baul() {
        assertEquals(1, paginasDelInvernadero(emptyList()).size)
        assertEquals(2, totalDePantallas(emptyList()))
        assertEquals(3, totalDePantallas(plantas(12)))
    }

    @Test
    fun la_busqueda_mira_el_apodo_y_la_especie_sin_importar_mayusculas() {
        val lista = listOf(
            planta(id = 1).copy(name = "Fifi", species = "Monstera deliciosa"),
            planta(id = 2).copy(name = "Rosita", species = "Rosa chinensis")
        )

        assertEquals(listOf(1), filtrarPlantas(lista, "FIF").map { it.id })
        assertEquals(listOf(2), filtrarPlantas(lista, " chinensis ").map { it.id })
        assertEquals(2, filtrarPlantas(lista, "").size)
        assertEquals(0, filtrarPlantas(lista, "cactus").size)
    }

    @Test
    fun el_nombre_del_invernadero_sin_tienda_usa_el_conocido() {
        assertEquals("Selva tropical", nombreDelInvernadero("SELVA"))
        assertEquals("Invernadero victoriano", nombreDelInvernadero("VICTORIANO"))
        assertEquals("Selva tropical", nombreDelInvernadero("RARO"))
    }
}
