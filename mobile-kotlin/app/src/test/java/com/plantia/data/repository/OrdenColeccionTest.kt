package com.plantia.data.repository

import com.plantia.data.remote.dto.ColeccionItemDto
import org.junit.Assert.assertEquals
import org.junit.Test

/** La coleccion se ordena por nombre comun, sin distinguir mayusculas ni tildes, y de forma estable. */
class OrdenColeccionTest {

    private fun planta(id: Int, comun: String, cientifico: String = "") =
        ColeccionItemDto(id = id, nombreComun = comun, nombreCientifico = cientifico)

    @Test
    fun ordena_por_nombre_comun_y_no_por_el_cientifico() {
        val lista = listOf(
            planta(1, "Rosa", "Aaa rosa"),
            planta(2, "Cactus", "Zzz cactus"),
            planta(3, "Menta", "Mmm menta")
        )
        assertEquals(listOf("Cactus", "Menta", "Rosa"), lista.ordenadasPorNombreComun().map { it.nombreComun })
    }

    @Test
    fun ignora_mayusculas_y_tildes() {
        val lista = listOf(planta(1, "zábila"), planta(2, "Álamo"), planta(3, "aloe"), planta(4, "Árnica"), planta(5, "Begonia"))
        assertEquals(
            listOf("Álamo", "aloe", "Árnica", "Begonia", "zábila"),
            lista.ordenadasPorNombreComun().map { it.nombreComun }
        )
    }

    @Test
    fun con_el_mismo_nombre_conserva_el_orden_de_llegada() {
        val lista = listOf(planta(10, "Helecho"), planta(11, "helecho"), planta(12, "HELECHO"), planta(9, "Abeto"))
        assertEquals(listOf(9, 10, 11, 12), lista.ordenadasPorNombreComun().map { it.id })
    }
}
