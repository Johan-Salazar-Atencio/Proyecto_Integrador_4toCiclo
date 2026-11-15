package com.plantia.ui.garden

import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.TemaFondo
import com.plantia.data.repository.FondosResultado
import com.plantia.ui.tienda.FakeTiendaRepository
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.planta
import com.plantia.ui.garden.viewmodel.GardenUiState
import com.plantia.ui.garden.viewmodel.GardenViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    // --- Fondos de la planta ---

    private fun tema(id: Int, nombre: String, predeterminado: Boolean = false, enUso: Boolean = false) =
        TemaFondo(id, nombre, "", predeterminado, enUso, emptyList())

    @Test
    fun abrir_los_fondos_carga_los_temas_de_la_planta_del_jardin() {
        val tienda = FakeTiendaRepository()
        tienda.fondos = FondosResultado.Con(listOf(tema(1, "Campo", predeterminado = true, enUso = true), tema(20, "Nevado")))
        val vm = GardenViewModel(garden, tienda)

        assertNull(vm.fondos)
        vm.abrirFondos {}

        assertEquals(listOf("Campo", "Nevado"), vm.fondos?.map { it.nombre })
    }

    @Test
    fun elegir_un_tema_lo_pone_a_esta_planta_y_avisa_para_rearmar_la_escena() {
        val tienda = FakeTiendaRepository()
        val nevado = tema(20, "Nevado")
        tienda.fondos = FondosResultado.Con(listOf(tema(1, "Campo", predeterminado = true), nevado.copy(enUso = true)))
        val vm = GardenViewModel(garden, tienda)
        var mensaje = ""
        var cambios = 0

        vm.elegirFondo(nevado, { mensaje = it }, { cambios++ })

        assertEquals(listOf(1 to 20), tienda.fondosPuestos)
        assertEquals("Monstera ahora usa «Nevado»", mensaje)
        assertEquals(1, cambios)
    }

    @Test
    fun volver_al_predeterminado_manda_sin_tema() {
        val tienda = FakeTiendaRepository()
        val vm = GardenViewModel(garden, tienda)

        vm.elegirFondo(tema(1, "Campo", predeterminado = true), {}, {})

        assertEquals(listOf(1 to null), tienda.fondosPuestos)
    }

    @Test
    fun el_tema_que_ya_esta_en_uso_no_llama_al_servidor() {
        val tienda = FakeTiendaRepository()
        GardenViewModel(garden, tienda).elegirFondo(tema(20, "Nevado", enUso = true), {}, {})
        assertTrue(tienda.fondosPuestos.isEmpty())
    }

    @Test
    fun si_el_servidor_rechaza_el_fondo_se_muestra_su_mensaje() {
        val tienda = FakeTiendaRepository()
        tienda.fondos = FondosResultado.Fallo("No tienes ese fondo")
        var mensaje = ""

        GardenViewModel(garden, tienda).elegirFondo(tema(22, "Cyberpunk"), { mensaje = it }, {})

        assertEquals("No tienes ese fondo", mensaje)
    }

    @Test
    fun cerrar_el_panel_borra_la_lista() {
        val tienda = FakeTiendaRepository()
        tienda.fondos = FondosResultado.Con(listOf(tema(1, "Campo", predeterminado = true, enUso = true)))
        val vm = GardenViewModel(garden, tienda)
        vm.abrirFondos {}
        vm.cerrarFondos()
        assertNull(vm.fondos)
    }
}
