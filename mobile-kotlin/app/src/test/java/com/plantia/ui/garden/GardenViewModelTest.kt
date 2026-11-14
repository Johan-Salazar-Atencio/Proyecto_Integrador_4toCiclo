package com.plantia.ui.garden

import com.plantia.data.model.AccionCuidado
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.planta
import com.plantia.ui.garden.viewmodel.GardenUiState
import com.plantia.ui.garden.viewmodel.GardenViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GardenViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val garden = FakeGardenRepository()

    @Test
    fun carga_la_planta_activa_al_iniciar() {
        val vm = GardenViewModel(garden)
        val estado = vm.uiState as GardenUiState.Success
        assertEquals("Monstera", estado.planta.name)
    }

    @Test
    fun sin_planta_muestra_estado_vacio() {
        garden.activa = PlantaActivaResultado.SinPlanta
        assertEquals(GardenUiState.SinPlanta, GardenViewModel(garden).uiState)
    }

    @Test
    fun regar_actualiza_la_planta_y_calcula_el_aumento() {
        garden.alCuidar = PlantaResultado.Con(planta(agua = 70))
        val vm = GardenViewModel(garden)
        var mensaje = ""

        vm.cuidar(AccionCuidado.REGAR) { mensaje = it }

        assertEquals(listOf(1 to AccionCuidado.REGAR), garden.cuidados)
        assertEquals("Regaste a Monstera: +20% de agua", mensaje)
        assertEquals(70, (vm.uiState as GardenUiState.Success).planta.humidityPercent)
    }

    @Test
    fun con_el_agua_al_maximo_avisa_sin_llamar_al_servidor() {
        garden.activa = PlantaActivaResultado.ConPlanta(planta(agua = 100))
        val vm = GardenViewModel(garden)
        var mensaje = ""

        vm.cuidar(AccionCuidado.REGAR) { mensaje = it }

        assertTrue(garden.cuidados.isEmpty())
        assertEquals("Monstera ya tiene toda el agua que necesita", mensaje)
    }

    @Test
    fun si_el_servidor_falla_avisa_y_conserva_la_planta() {
        garden.alCuidar = PlantaResultado.Fallo("HTTP 500")
        val vm = GardenViewModel(garden)
        var mensaje = ""

        vm.cuidar(AccionCuidado.SOL) { mensaje = it }

        assertEquals("No se pudo aplicar el cuidado: HTTP 500", mensaje)
        assertEquals(50, (vm.uiState as GardenUiState.Success).planta.lightPercent)
    }
}
