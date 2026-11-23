package com.plantia.ui.detail

import com.plantia.data.mapper.toDomain
import com.plantia.data.remote.dto.ScanResultDto
import com.plantia.data.repository.DetalleColeccionResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.fakes.FakeCollectionRepository
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.FakeScanRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.ui.detail.viewmodel.DetalleUiState
import com.plantia.ui.detail.viewmodel.PlantDetailViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlantDetailViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val scan = FakeScanRepository()
    private val collection = FakeCollectionRepository()
    private val garden = FakeGardenRepository()
    private fun vm() = PlantDetailViewModel(scan, collection, garden)

    @Test
    fun sin_escaneo_en_memoria_pide_volver() {
        val vm = vm()
        vm.mostrarUltimoEscaneo()
        assertEquals(DetalleUiState.SinResultado, vm.uiState)
    }

    @Test
    fun muestra_el_ultimo_escaneo() {
        scan.fijarUltimo(ScanResultDto(idEscaneo = 3, nombreComun = "Ficus"))
        val vm = vm()
        vm.mostrarUltimoEscaneo()
        assertEquals("Ficus", (vm.uiState as DetalleUiState.Listo).resultado.nombreComun)
    }

    @Test
    fun carga_el_detalle_de_la_coleccion() {
        collection.detalle = DetalleColeccionResultado.Con(ScanResultDto(nombreComun = "Cactus"))
        val vm = vm()
        vm.cargarDeColeccion(5)
        assertEquals("Cactus", (vm.uiState as DetalleUiState.Listo).resultado.nombreComun)
    }

    @Test
    fun digitalizar_usa_el_id_del_escaneo_y_navega_al_confirmar() {
        scan.fijarUltimo(ScanResultDto(idEscaneo = 3, idCatalogo = 9))
        val vm = vm()
        vm.mostrarUltimoEscaneo()
        var navego = false

        vm.digitalizar { navego = true }

        assertEquals(listOf(3 to 9), garden.digitalizados)
        assertTrue(navego)
        assertNull(vm.aviso)
    }

    @Test
    fun digitalizar_con_error_avisa_y_no_navega() {
        garden.alDigitalizar = PlantaResultado.Fallo("HTTP 404")
        scan.fijarUltimo(ScanResultDto(idEscaneo = 3))
        val vm = vm()
        vm.mostrarUltimoEscaneo()
        var navego = false

        vm.digitalizar { navego = true }

        assertEquals(false, navego)
        assertEquals("No se pudo digitalizar: HTTP 404", vm.aviso)
        vm.avisoMostrado()
        assertNull(vm.aviso)
    }

    @Test
    fun guardar_agrega_a_la_coleccion_y_continua() {
        scan.fijarUltimo(ScanResultDto(idEscaneo = 3))
        val vm = vm()
        vm.mostrarUltimoEscaneo()
        var termino = false

        vm.guardar { termino = true }

        assertEquals(listOf(3), collection.agregados)
        assertTrue(termino)
    }

    // QA-04: si la especie ya esta en el jardin, la ficha lo avisa antes de digitalizar
    private fun plantaDe(especie: String, fase: String = "BROTE", enBaul: Boolean = false) =
        com.plantia.data.model.PlantSummary(name = "Mi Rosita", species = especie, healthPercent = 50, humidityPercent = 50,
            lightPercent = 50, lastWatered = "", id = 4, fase = fase, enBaul = enBaul)

    @Test
    fun avisa_si_la_especie_ya_esta_en_el_jardin() {
        garden.lista = com.plantia.data.repository.PlantasResultado.Con(listOf(plantaDe("Rosa qaensis")))
        scan.fijarUltimo(ScanResultDto(idEscaneo = 3, idCatalogo = 9, nombreCientifico = " rosa QAENSIS"))
        val vm = vm()
        vm.mostrarUltimoEscaneo()

        assertEquals(4, vm.enJardin?.id)
        val aviso = PlantDetailViewModel.avisoYaEnJardin(vm.enJardin)!!
        assertTrue(aviso.contains("no la reinicia"))
    }

    @Test
    fun sin_la_especie_en_el_jardin_no_hay_aviso() {
        garden.lista = com.plantia.data.repository.PlantasResultado.Con(listOf(plantaDe("Ficus qaensis")))
        scan.fijarUltimo(ScanResultDto(idEscaneo = 3, idCatalogo = 9, nombreCientifico = "Rosa qaensis"))
        val vm = vm()
        vm.mostrarUltimoEscaneo()

        assertNull(vm.enJardin)
        assertNull(PlantDetailViewModel.avisoYaEnJardin(null))
    }

    @Test
    fun el_aviso_distingue_muerta_y_baul() {
        assertTrue(PlantDetailViewModel.avisoYaEnJardin(plantaDe("x", fase = "MUERTA"))!!.contains("no la revive"))
        assertTrue(PlantDetailViewModel.avisoYaEnJardin(plantaDe("x", enBaul = true))!!.contains("baúl"))
    }

    @Test
    fun la_respuesta_de_digitalizar_trae_ya_en_jardin() {
        val dto = com.plantia.data.remote.dto.PlantSummaryDto(name = "Mi Rosita", species = "Rosa qaensis", yaEnJardin = true)
        assertTrue(dto.toDomain().yaEnJardin)
    }
}
