package com.plantia.ui.misiones

import com.plantia.data.model.DailyMission
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.PlantasResultado
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.FakeHomeRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.planta
import com.plantia.ui.misiones.viewmodel.MisionesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MisionesViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val home = FakeHomeRepository()
    private val garden = FakeGardenRepository()

    private fun diaria(id: String, completa: Boolean = false) = DailyMission(
        id = id, title = "Riego", reward = "+15 XP", progress = if (completa) 1f else 0f,
        currentCount = if (completa) 2 else 0, targetCount = 2, isCompleted = completa
    )

    private fun evento(actual: Int = 1, total: Int = 3) = DailyMission(
        id = "event_9", title = "Prevenir ola de calor", reward = "+80 XP", progress = actual / total.toFloat(),
        currentCount = actual, targetCount = total, isCompleted = actual >= total, kind = "EVENT",
        eventType = "OLA_DE_CALOR", endsInSeconds = 7200
    )

    private fun hayEventoYPlantas() {
        home.misionesResultado = listOf(evento(), diaria("m1", completa = true), diaria("m2"))
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1), planta(id = 2), planta(id = 3)))
    }

    private fun viewModel() = MisionesViewModel(home, garden)

    @Test
    fun separa_la_mision_del_evento_de_las_diarias() {
        hayEventoYPlantas()

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
    fun solo_se_pueden_proteger_las_plantas_vivas_sin_proteger_ni_golpear() {
        home.misionesResultado = listOf(evento())
        garden.lista = PlantasResultado.Con(listOf(
            planta(id = 1),
            planta(id = 2).copy(refugiada = true),
            planta(id = 3).copy(fase = "MUERTA"),
            planta(id = 4).copy(enRecuperacion = true)
        ))

        assertEquals(listOf(1), viewModel().uiState.prevenibles.map { it.id })
    }

    @Test
    fun abrir_la_hoja_marca_todas_las_plantas_prevenibles() {
        hayEventoYPlantas()
        val vm = viewModel()

        vm.abrirHoja()

        assertTrue(vm.hojaAbierta)
        assertEquals(setOf(1, 2, 3), vm.seleccion)
    }

    @Test
    fun sin_plantas_prevenibles_la_hoja_no_se_abre() {
        home.misionesResultado = listOf(evento())
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1).copy(refugiada = true)))
        val vm = viewModel()

        vm.abrirHoja()

        assertFalse(vm.hojaAbierta)
    }

    @Test
    fun se_marca_y_desmarca_una_planta_o_todas() {
        hayEventoYPlantas()
        val vm = viewModel()
        vm.abrirHoja()

        vm.alternar(2)
        assertEquals(setOf(1, 3), vm.seleccion)
        vm.alternar(2)
        assertEquals(setOf(1, 2, 3), vm.seleccion)
        vm.alternarTodas()
        assertEquals(emptySet<Int>(), vm.seleccion)
        vm.alternarTodas()
        assertEquals(setOf(1, 2, 3), vm.seleccion)
    }

    @Test
    fun prevenir_manda_las_plantas_marcadas_cierra_la_hoja_y_avisa() {
        hayEventoYPlantas()
        val vm = viewModel()
        vm.abrirHoja()
        vm.alternar(3)

        vm.prevenir()

        assertEquals(listOf(listOf(1, 2)), garden.prevenidosVarios)
        assertFalse(vm.hojaAbierta)
        assertEquals("Evento prevenido en 2 plantas", vm.aviso)
        assertFalse(vm.previniendo)
    }

    @Test
    fun el_aviso_de_una_sola_planta_va_en_singular() {
        hayEventoYPlantas()
        val vm = viewModel()
        vm.abrirHoja()
        vm.alternarTodas()
        vm.alternar(1)

        vm.prevenir()

        assertEquals("Evento prevenido en 1 planta", vm.aviso)
    }

    @Test
    fun sin_plantas_marcadas_no_llama_al_servidor() {
        hayEventoYPlantas()
        val vm = viewModel()
        vm.abrirHoja()
        vm.alternarTodas()

        vm.prevenir()

        assertTrue(garden.prevenidosVarios.isEmpty())
    }

    @Test
    fun si_el_servidor_rechaza_se_muestra_su_mensaje_y_se_recarga() {
        hayEventoYPlantas()
        garden.alPrevenirVarias = PlantasResultado.Fallo("Ya no hay un evento activo para prevenir")
        val vm = viewModel()
        vm.abrirHoja()
        home.misionesResultado = listOf(diaria("m1"))

        vm.prevenir()

        assertEquals("Ya no hay un evento activo para prevenir", vm.aviso)
        assertNull(vm.uiState.evento)
    }

    @Test
    fun un_fallo_de_red_conserva_las_misiones_que_ya_se_veian() {
        hayEventoYPlantas()
        val vm = viewModel()
        home.misionesResultado = null

        vm.cargar()

        assertEquals("No se pudieron cargar las misiones", vm.uiState.error)
        assertEquals(3, vm.uiState.misiones.size)
    }
}
