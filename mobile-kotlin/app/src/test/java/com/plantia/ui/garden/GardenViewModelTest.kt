package com.plantia.ui.garden

import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.TemaFondo
import com.plantia.data.repository.FondosResultado
import com.plantia.ui.tienda.FakeTiendaRepository
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.FakeHomeRepository
import com.plantia.fakes.alerta
import com.plantia.data.repository.AlertaResultado
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
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

    // --- Prevenir evento ---

    private val home = FakeHomeRepository()

    private fun vmConEvento(hayEvento: Boolean = true): GardenViewModel {
        home.alertaResultado = if (hayEvento) AlertaResultado.Con(alerta().copy(tip = "Cubre el suelo")) else AlertaResultado.Sin
        val vm = GardenViewModel(garden, home = home)
        kotlinx.coroutines.test.runTest { vm.actualizarEvento() }
        return vm
    }

    @Test
    fun sin_evento_no_se_ofrece_prevenir() {
        assertFalse(vmConEvento(hayEvento = false).puedePrevenir)
    }

    @Test
    fun con_evento_y_una_planta_viva_se_ofrece_prevenir() {
        assertTrue(vmConEvento().puedePrevenir)
    }

    @Test
    fun una_planta_ya_protegida_no_se_ofrece_prevenir() {
        garden.activa = PlantaActivaResultado.ConPlanta(planta().copy(refugiada = true))
        assertFalse(vmConEvento().puedePrevenir)
    }

    @Test
    fun una_planta_muerta_o_en_recuperacion_no_se_ofrece_prevenir() {
        garden.activa = PlantaActivaResultado.ConPlanta(planta().copy(fase = "MUERTA"))
        assertFalse(vmConEvento().puedePrevenir)
        garden.activa = PlantaActivaResultado.ConPlanta(planta().copy(enRecuperacion = true))
        assertFalse(vmConEvento().puedePrevenir)
    }

    @Test
    fun prevenir_protege_la_planta_y_avisa_con_el_consejo() {
        garden.alPrevenir = PlantaResultado.Con(planta().copy(refugiada = true))
        val vm = vmConEvento()
        var mensaje = ""

        vm.prevenir { mensaje = it }

        assertEquals(listOf(1), garden.prevenidos)
        assertTrue((vm.uiState as GardenUiState.Success).planta.refugiada)
        assertEquals("Monstera esta protegida del evento. Cubre el suelo", mensaje)
        assertFalse(vm.puedePrevenir)
        assertFalse(vm.previniendo)
    }

    @Test
    fun sin_evento_prevenir_no_llama_al_servidor() {
        val vm = vmConEvento(hayEvento = false)

        vm.prevenir {}

        assertTrue(garden.prevenidos.isEmpty())
    }

    @Test
    fun si_el_evento_ya_termino_avisa_y_deja_de_ofrecer_prevenir() {
        garden.alPrevenir = PlantaResultado.Fallo("Ya no hay un evento activo para prevenir")
        val vm = vmConEvento()
        home.alertaResultado = AlertaResultado.Sin
        var mensaje = ""

        vm.prevenir { mensaje = it }

        assertEquals("Ya no hay un evento activo para prevenir", mensaje)
        assertNull(vm.evento)
        assertFalse(vm.puedePrevenir)
    }

    @Test
    fun un_fallo_de_red_al_consultar_conserva_el_evento_que_ya_se_veia() = runTest {
        val vm = vmConEvento()
        home.alertaResultado = AlertaResultado.Fallo

        vm.actualizarEvento()

        assertTrue(vm.evento != null)
    }

    // --- Revivir ---

    private fun muerta() = planta(salud = 0).copy(fase = "MUERTA", costoRevivir = 250)

    @Test
    fun revivir_a_una_planta_muerta_la_devuelve_a_la_vida_y_avisa() {
        garden.activa = PlantaActivaResultado.ConPlanta(muerta())
        garden.alRevivir = PlantaResultado.Con(planta(salud = 50).copy(fase = "BROTE"))
        val vm = GardenViewModel(garden)
        var mensaje = ""

        vm.revivir { mensaje = it }

        assertEquals(listOf(1), garden.revividos)
        assertFalse((vm.uiState as GardenUiState.Success).planta.muerta)
        assertEquals("Monstera volvio a la vida. Cuidala bien", mensaje)
        assertFalse(vm.reviviendo)
    }

    @Test
    fun si_no_alcanzan_las_hojas_muestra_el_mensaje_y_la_planta_sigue_muerta() {
        garden.activa = PlantaActivaResultado.ConPlanta(muerta())
        garden.alRevivir = PlantaResultado.Fallo("No te alcanzan las hojas para revivirla")
        val vm = GardenViewModel(garden)
        var mensaje = ""

        vm.revivir { mensaje = it }

        assertEquals("No te alcanzan las hojas para revivirla", mensaje)
        assertTrue((vm.uiState as GardenUiState.Success).planta.muerta)
        assertFalse(vm.reviviendo)
    }

    @Test
    fun una_planta_viva_no_se_manda_a_revivir() {
        val vm = GardenViewModel(garden)

        vm.revivir {}

        assertTrue(garden.revividos.isEmpty())
    }

    // --- Efectos del evento ---

    @Test
    fun prevenir_dispara_el_efecto_del_escudo() {
        val vm = vmConEvento()
        assertEquals(0, vm.proteccionesNuevas)

        vm.prevenir {}

        assertEquals(1, vm.proteccionesNuevas)
    }

    @Test
    fun un_prevenir_que_falla_no_dispara_el_efecto() {
        garden.alPrevenir = PlantaResultado.Fallo("Ya no hay un evento activo para prevenir")
        val vm = vmConEvento()

        vm.prevenir {}

        assertEquals(0, vm.proteccionesNuevas)
    }

    @Test
    fun marcar_el_golpe_como_visto_lo_apaga() {
        garden.activa = PlantaActivaResultado.ConPlanta(planta().copy(golpeTipo = "OLA_DE_CALOR", enRecuperacion = true))
        garden.alGolpeVisto = PlantaResultado.Con(planta().copy(enRecuperacion = true))
        val vm = GardenViewModel(garden)

        vm.golpeVisto()

        assertEquals(listOf(1), garden.golpesVistos)
        assertNull((vm.uiState as GardenUiState.Success).planta.golpeTipo)
    }

    @Test
    fun si_el_servidor_no_responde_el_golpe_se_apaga_igual_en_pantalla() {
        garden.activa = PlantaActivaResultado.ConPlanta(planta().copy(golpeTipo = "HELADA"))
        garden.alGolpeVisto = PlantaResultado.Fallo("sin conexion")
        val vm = GardenViewModel(garden)

        vm.golpeVisto()

        assertNull((vm.uiState as GardenUiState.Success).planta.golpeTipo)
    }

    @Test
    fun sin_golpe_pendiente_no_se_llama_al_servidor() {
        val vm = GardenViewModel(garden)

        vm.golpeVisto()

        assertTrue(garden.golpesVistos.isEmpty())
    }
}
