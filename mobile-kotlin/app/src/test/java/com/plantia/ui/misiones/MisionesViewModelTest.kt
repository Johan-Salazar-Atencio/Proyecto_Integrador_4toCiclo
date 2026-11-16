package com.plantia.ui.misiones

import com.plantia.data.model.DailyMission
import com.plantia.fakes.FakeHomeRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.ui.misiones.viewmodel.MisionesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class MisionesViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val home = FakeHomeRepository()

    private fun diaria(id: String, completa: Boolean = false) = DailyMission(
        id = id, title = "Riego", reward = "+15 XP", progress = if (completa) 1f else 0f,
        currentCount = if (completa) 2 else 0, targetCount = 2, isCompleted = completa, type = "RIEGO"
    )

    private val evento = DailyMission(
        id = "event_9", title = "Prevenir ola de calor", reward = "+80 XP", progress = 0.5f,
        currentCount = 1, targetCount = 2, isCompleted = false, kind = "EVENT", eventType = "OLA_DE_CALOR", endsInSeconds = 7200
    )

    private fun viewModel() = MisionesViewModel(home)

    @Test
    fun separa_la_mision_del_evento_de_las_diarias() {
        home.misionesResultado = listOf(evento, diaria("m1", completa = true), diaria("m2"))

        val estado = viewModel().uiState

        assertEquals("event_9", estado.evento?.id)
        assertEquals(listOf("m1", "m2"), estado.diarias.map { it.id })
        assertEquals(1, estado.completadas)
        assertFalse(estado.cargando)
    }

    @Test
    fun sin_evento_no_hay_tarjeta_roja() {
        home.misionesResultado = listOf(diaria("m1"))

        assertNull(viewModel().uiState.evento)
    }

    @Test
    fun volver_a_cargar_trae_el_avance_nuevo() {
        home.misionesResultado = listOf(evento)
        val vm = viewModel()
        home.misionesResultado = listOf(evento.copy(currentCount = 2, progress = 1f, isCompleted = true))

        vm.cargar()

        assertEquals(2, vm.uiState.evento?.currentCount)
    }

    @Test
    fun un_fallo_de_red_conserva_las_misiones_que_ya_se_veian() {
        home.misionesResultado = listOf(evento, diaria("m1"), diaria("m2"))
        val vm = viewModel()
        home.misionesResultado = null

        vm.cargar()

        assertEquals("No se pudieron cargar las misiones", vm.uiState.error)
        assertEquals(3, vm.uiState.misiones.size)
    }
}
