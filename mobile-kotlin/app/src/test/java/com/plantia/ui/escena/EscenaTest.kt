package com.plantia.ui.escena

import com.plantia.data.model.AnimacionDesastre
import com.plantia.data.model.Arte
import com.plantia.data.model.FondoEscena
import com.plantia.data.repository.AlertaResultado
import com.plantia.fakes.FakeArteRepository
import com.plantia.fakes.FakeHomeRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.alerta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

private fun fondo(momento: String, predeterminado: Boolean = true, url: String = "http://x/$momento.png") = FondoEscena(
    momento = momento, imageUrl = url, frames = 8, frameWidthPx = 96, frameHeightPx = 96,
    msPerFrame = 250, esPredeterminado = predeterminado
)

private fun anim(tipo: String, fase: String, frames: Int) = AnimacionDesastre(
    disasterType = tipo, phase = fase, msPerFrame = 150, frames = List(frames) { "http://x/${tipo}_${fase}_$it.png" }
)

private val ARTE = Arte(
    fondos = listOf(fondo("DIA"), fondo("TARDE"), fondo("NOCHE")),
    animaciones = listOf(anim("HUAYCO", "AVISO", 4), anim("HUAYCO", "PICO", 8), anim("HELADA", "PICO", 8))
)

class MomentoDelDiaTest {
    @Test
    fun el_dia_va_de_las_6_a_las_12() {
        assertEquals("DIA", momentoDelDia(6))
        assertEquals("DIA", momentoDelDia(11))
    }

    @Test
    fun la_tarde_va_de_las_12_a_las_18() {
        assertEquals("TARDE", momentoDelDia(12))
        assertEquals("TARDE", momentoDelDia(17))
    }

    @Test
    fun la_noche_va_de_las_18_a_las_6_incluida_la_madrugada() {
        assertEquals("NOCHE", momentoDelDia(18))
        assertEquals("NOCHE", momentoDelDia(23))
        assertEquals("NOCHE", momentoDelDia(0))
        assertEquals("NOCHE", momentoDelDia(5))
    }
}

class ElegirEscenaTest {
    @Test
    fun sin_arte_no_hay_escena() {
        assertNull(elegirEscena(null, 12, null, null))
    }

    @Test
    fun el_fondo_es_el_del_momento_del_dia() {
        assertEquals("http://x/NOCHE.png", elegirEscena(ARTE, 22, null, null)?.fondoUrl)
        assertEquals("http://x/DIA.png", elegirEscena(ARTE, 10, null, null)?.fondoUrl)
    }

    @Test
    fun sin_desastre_no_hay_capa() {
        assertEquals(emptyList<String>(), elegirEscena(ARTE, 10, null, null)?.desastreFrames)
    }

    @Test
    fun el_desastre_usa_el_tipo_y_la_fase_pedidos() {
        val escena = elegirEscena(ARTE, 10, "HUAYCO", "AVISO")
        assertEquals(4, escena?.desastreFrames?.size)
        assertEquals(150, escena?.desastreMs)
    }

    @Test
    fun si_falta_la_fase_pedida_usa_otra_del_mismo_tipo() {
        // HELADA solo tiene PICO
        assertEquals(8, elegirEscena(ARTE, 10, "HELADA", "AVISO")?.desastreFrames?.size)
    }

    @Test
    fun un_tipo_desconocido_no_dibuja_capa() {
        assertEquals(emptyList<String>(), elegirEscena(ARTE, 10, "TERREMOTO", "PICO")?.desastreFrames)
    }

    @Test
    fun si_falta_el_fondo_del_momento_usa_cualquier_otro() {
        val soloDia = Arte(fondos = listOf(fondo("DIA")), animaciones = emptyList())
        assertEquals("http://x/DIA.png", elegirEscena(soloDia, 23, null, null)?.fondoUrl)
    }

    @Test
    fun entre_dos_fondos_del_mismo_momento_gana_el_predeterminado() {
        val arte = Arte(
            fondos = listOf(fondo("DIA", predeterminado = false, url = "http://x/otro.png"), fondo("DIA", true, "http://x/base.png")),
            animaciones = emptyList()
        )
        assertEquals("http://x/base.png", elegirEscena(arte, 10, null, null)?.fondoUrl)
    }

    @Test
    fun el_fondo_equipado_de_la_tienda_gana_al_predeterminado() {
        val comprado = FondoEscena(
            momento = "NOCHE", imageUrl = "http://x/cyberpunk.png", frames = 96, frameWidthPx = 96, frameHeightPx = 96,
            msPerFrame = 120, esPredeterminado = false, equipado = true
        )
        val arte = Arte(fondos = listOf(fondo("DIA"), fondo("NOCHE"), comprado), animaciones = emptyList())
        assertEquals("http://x/cyberpunk.png", elegirEscena(arte, 22, null, null)?.fondoUrl)
        // La madrugada tambien es NOCHE, y en otro momento del dia sigue el predeterminado
        assertEquals("http://x/cyberpunk.png", elegirEscena(arte, 3, null, null)?.fondoUrl)
        assertEquals("http://x/DIA.png", elegirEscena(arte, 10, null, null)?.fondoUrl)
    }

    @Test
    fun sin_fondos_no_hay_escena() {
        assertNull(elegirEscena(Arte(emptyList(), emptyList()), 10, null, null))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EscenaViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val home = FakeHomeRepository()
    private val arte = FakeArteRepository(ARTE)

    private fun viewModel(hora: Int = 10) = EscenaViewModel(arte, home, hora = { hora }, intervaloMs = 0)

    @Test
    fun carga_el_fondo_del_momento() = runTest {
        val vm = viewModel(hora = 22)
        vm.actualizar()
        assertEquals("http://x/NOCHE.png", vm.escena?.fondoUrl)
    }

    @Test
    fun una_alerta_activa_agrega_la_animacion_del_desastre() = runTest {
        home.alertaResultado = AlertaResultado.Con(alerta().copy(disasterType = "HUAYCO", phase = "PICO"))
        val vm = viewModel()
        vm.actualizar()
        assertEquals(8, vm.escena?.desastreFrames?.size)
    }

    @Test
    fun al_terminar_el_desastre_la_animacion_desaparece() = runTest {
        home.alertaResultado = AlertaResultado.Con(alerta().copy(disasterType = "HUAYCO", phase = "PICO"))
        val vm = viewModel()
        vm.actualizar()
        home.alertaResultado = AlertaResultado.Sin
        vm.actualizar()
        assertEquals(0, vm.escena?.desastreFrames?.size)
    }

    @Test
    fun si_la_consulta_de_la_alerta_falla_se_conserva_el_desastre() = runTest {
        home.alertaResultado = AlertaResultado.Con(alerta().copy(disasterType = "HELADA", phase = "PICO"))
        val vm = viewModel()
        vm.actualizar()
        home.alertaResultado = AlertaResultado.Fallo
        vm.actualizar()
        assertEquals(8, vm.escena?.desastreFrames?.size)
    }

    @Test
    fun si_el_arte_no_estaba_se_reintenta_en_la_siguiente_consulta() = runTest {
        arte.arte = null
        val vm = viewModel()
        vm.actualizar()
        assertNull(vm.escena)

        arte.arte = ARTE
        vm.actualizar()
        assertNotNull(vm.escena)
    }
}
