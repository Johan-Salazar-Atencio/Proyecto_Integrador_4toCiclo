package com.plantia.ui.home

import com.plantia.data.repository.AlertaResultado
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.fakes.FakeCollectionRepository
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.FakeHomeRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.alerta
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

    // --- Boton Refugiar Planta ---

    @Test
    fun refugiar_marca_la_planta_y_avisa() = runTest {
        home.datos = home.datos.copy(alerta = alerta())
        val vm = viewModel()

        vm.refugiar()

        assertEquals(listOf(1), garden.refugiados)
        assertEquals(true, vm.uiState.planta?.refugiada)
        assertEquals("Monstera esta a resguardo", vm.aviso)
        assertEquals(false, vm.refugiando)
    }

    @Test
    fun si_el_desastre_ya_termino_avisa_y_refresca_la_alerta() = runTest {
        home.datos = home.datos.copy(alerta = alerta())
        garden.alRefugiar = PlantaResultado.Fallo("Ya no hay un desastre activo para refugiar la planta")
        val vm = viewModel()
        home.alertaResultado = AlertaResultado.Sin

        vm.refugiar()

        assertEquals("Ya no hay un desastre activo para refugiar la planta", vm.aviso)
        assertEquals(false, vm.uiState.planta?.refugiada)
        assertNull(vm.uiState.alerta)
    }

    @Test
    fun sin_planta_no_llama_al_servidor_y_avisa() = runTest {
        home.datos = home.datos.copy(planta = null)
        val vm = viewModel()

        vm.refugiar()

        assertEquals(emptyList<Int>(), garden.refugiados)
        assertEquals("Aun no tienes una planta para refugiar", vm.aviso)
    }

    @Test
    fun una_planta_ya_refugiada_no_se_vuelve_a_pedir() = runTest {
        home.datos = home.datos.copy(planta = planta().copy(refugiada = true))
        val vm = viewModel()

        vm.refugiar()

        assertEquals(emptyList<Int>(), garden.refugiados)
    }

    @Test
    fun el_aviso_se_limpia_al_mostrarse() = runTest {
        val vm = viewModel()
        vm.refugiar()
        assertNotNull(vm.aviso)

        vm.avisoMostrado()

        assertNull(vm.aviso)
    }
}
