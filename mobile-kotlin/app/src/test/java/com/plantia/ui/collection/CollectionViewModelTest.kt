package com.plantia.ui.collection

import com.plantia.data.repository.ColeccionResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.data.repository.PlantasResultado
import com.plantia.fakes.FakeCollectionRepository
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.planta
import com.plantia.ui.collection.viewmodel.CollectionUiState
import com.plantia.ui.collection.viewmodel.CollectionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CollectionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val collection = FakeCollectionRepository()
    private val garden = FakeGardenRepository()

    @Test
    fun une_las_plantas_escaneadas_y_las_digitales() {
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1), planta(id = 2, activa = false)))
        val vm = CollectionViewModel(collection, garden)
        val estado = vm.uiState as CollectionUiState.Success
        assertEquals(2, estado.digitales.size)
    }

    @Test
    fun si_fallan_las_digitales_la_coleccion_se_muestra_igual() {
        garden.lista = PlantasResultado.Fallo("sin red")
        val estado = CollectionViewModel(collection, garden).uiState as CollectionUiState.Success
        assertTrue(estado.digitales.isEmpty())
    }

    @Test
    fun si_falla_la_coleccion_muestra_error() {
        collection.lista = ColeccionResultado.Fallo("HTTP 500")
        val estado = CollectionViewModel(collection, garden).uiState as CollectionUiState.Error
        assertEquals("HTTP 500", estado.mensaje)
    }

    @Test
    fun seleccionar_navega_solo_si_el_servidor_confirma() {
        val vm = CollectionViewModel(collection, garden)
        var navego = false
        vm.seleccionar(planta(id = 4)) { navego = true }
        assertTrue(navego)

        garden.alSeleccionar = PlantaResultado.Fallo("HTTP 404")
        navego = false
        vm.seleccionar(planta(id = 4)) { navego = true }
        assertEquals(false, navego)
        assertEquals("No se pudo elegir la planta: HTTP 404", vm.aviso)
    }
}
