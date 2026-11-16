package com.plantia.ui.home

import com.plantia.data.repository.AlertaResultado
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.fakes.FakeCollectionRepository
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.FakeHomeRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.alerta
import com.plantia.fakes.mision
import com.plantia.fakes.planta
import com.plantia.fakes.usuarioEjemplo
import com.plantia.ui.home.viewmodel.HomeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

/** El Home se mantiene al dia solo: alerta, perfil y planta se refrescan sin reabrir la app. */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val home = FakeHomeRepository()
    private val garden = FakeGardenRepository()

    private fun viewModel() = HomeViewModel(home, FakeCollectionRepository(), garden)

    @Test
    fun una_alerta_nueva_aparece_en_el_siguiente_refresco() = runTest {
        val vm = viewModel()
        assertNull(vm.uiState.alerta)

        home.alertaResultado = AlertaResultado.Con(alerta())
        vm.actualizarEnVivo()

        assertEquals("Helada Inminente", vm.uiState.alerta?.title)
    }

    @Test
    fun cuando_el_desastre_termina_la_alerta_desaparece() = runTest {
        home.datos = home.datos.copy(alerta = alerta())
        val vm = viewModel()
        assertNotNull(vm.uiState.alerta)

        home.alertaResultado = AlertaResultado.Sin
        vm.actualizarEnVivo()

        assertNull(vm.uiState.alerta)
    }

    @Test
    fun si_falla_la_consulta_se_conserva_la_alerta_que_ya_se_veia() = runTest {
        home.datos = home.datos.copy(alerta = alerta())
        val vm = viewModel()

        home.alertaResultado = AlertaResultado.Fallo
        vm.actualizarEnVivo()

        assertNotNull(vm.uiState.alerta)
    }

    @Test
    fun el_refresco_actualiza_el_perfil() = runTest {
        val vm = viewModel()

        home.usuarioResultado = usuarioEjemplo(nivel = 2, xp = 520)
        vm.actualizarEnVivo()

        assertEquals(2, vm.uiState.usuario?.level)
        assertEquals(520, vm.uiState.usuario?.experience)
    }

    @Test
    fun el_refresco_copia_salud_y_refugio_pero_conserva_la_imagen_del_home() = runTest {
        home.datos = home.datos.copy(planta = planta(salud = 100).copy(imageUrl = "/sprites/monstera.png"))
        val vm = viewModel()

        garden.activa = PlantaActivaResultado.ConPlanta(planta(salud = 50).copy(refugiada = true, imageUrl = "http://otra"))
        vm.actualizarEnVivo()

        val p = vm.uiState.planta!!
        assertEquals(50, p.healthPercent)
        assertEquals(true, p.refugiada)
        assertEquals("/sprites/monstera.png", p.imageUrl)
    }

    @Test
    fun si_ahora_la_planta_activa_es_otra_se_reemplaza() = runTest {
        val vm = viewModel()

        garden.activa = PlantaActivaResultado.ConPlanta(planta(id = 99, salud = 70))
        vm.actualizarEnVivo()

        assertEquals(99, vm.uiState.planta?.id)
    }

    @Test
    fun el_sondeo_consulta_al_iniciar_y_luego_cada_intervalo() = runTest {
        val vm = viewModel()
        val tarea = launch { vm.sondear(intervaloMs = 1_000) }

        runCurrent()
        assertEquals(1, home.consultasAlerta)
        advanceTimeBy(2_500)
        runCurrent()
        assertEquals(3, home.consultasAlerta)

        tarea.cancel()
    }

    // --- Subida de nivel ---

    @Test
    fun si_el_refresco_trae_un_nivel_mayor_avisa_la_subida() = runTest {
        val vm = viewModel()
        assertNull(vm.aviso)

        home.usuarioResultado = usuarioEjemplo(nivel = 2, xp = 520)
        vm.actualizarEnVivo()

        assertEquals("Subiste al nivel 2", vm.aviso)
    }

    @Test
    fun si_solo_sube_el_xp_no_hay_aviso_de_nivel() = runTest {
        val vm = viewModel()

        home.usuarioResultado = usuarioEjemplo(nivel = 1, xp = 300)
        vm.actualizarEnVivo()

        assertNull(vm.aviso)
        assertEquals(300, vm.uiState.usuario?.experience)
    }

    @Test
    fun el_home_muestra_todas_las_misiones_del_dia() = runTest {
        home.datos = home.datos.copy(misiones = listOf(mision("mission_1"), mision("mission_4")))

        val vm = viewModel()

        assertEquals(listOf("mission_1", "mission_4"), vm.uiState.misiones.map { it.id })
    }

    @Test
    fun el_refresco_en_vivo_actualiza_el_avance_de_las_misiones() = runTest {
        home.datos = home.datos.copy(misiones = listOf(mision(progreso = 0)))
        val vm = viewModel()

        home.misionesResultado = listOf(mision(progreso = 2))
        vm.actualizarEnVivo()

        assertEquals(true, vm.uiState.misiones.single().isCompleted)
    }

    @Test
    fun si_no_se_pueden_consultar_las_misiones_se_conservan_las_que_ya_se_ven() = runTest {
        home.datos = home.datos.copy(misiones = listOf(mision(progreso = 1)))
        val vm = viewModel()

        home.misionesResultado = null
        vm.actualizarEnVivo()

        assertEquals(1, vm.uiState.misiones.single().currentCount)
    }
}
