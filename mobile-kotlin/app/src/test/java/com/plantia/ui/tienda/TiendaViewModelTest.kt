package com.plantia.ui.tienda

import com.plantia.data.model.Tienda
import com.plantia.data.model.TiendaItem
import com.plantia.data.repository.AbonoResultado
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.TiendaRepository
import com.plantia.data.repository.TiendaResultado
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.planta
import com.plantia.ui.tienda.viewmodel.TiendaUiState
import com.plantia.ui.tienda.viewmodel.TiendaViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private fun item(id: Int, tipo: String, precio: Int, comprado: Boolean = false, equipado: Boolean = false) = TiendaItem(
    id = id, nombre = "Item $id", tipo = tipo, descripcion = "", precio = precio, nivelRequerido = 1,
    previewUrl = null, momento = if (tipo == TiendaItem.FONDO) "DIA" else null,
    comprado = comprado, equipado = equipado, bloqueado = false
)

private class FakeTiendaRepository : TiendaRepository {
    var tienda = Tienda(300, 2, listOf(item(1, TiendaItem.ABONO, 30), item(2, TiendaItem.FONDO, 100)))
    var respuesta: TiendaResultado = TiendaResultado.Con(tienda)
    var abono: AbonoResultado = AbonoResultado.Con(planta())
    val compras = mutableListOf<Int>()
    val usosDeAbono = mutableListOf<Int>()

    override suspend fun cargar() = respuesta
    override suspend fun comprar(idItem: Int): TiendaResultado {
        compras += idItem
        return respuesta
    }
    override suspend fun equipar(idItem: Int) = respuesta
    override suspend fun desequipar(idItem: Int) = respuesta
    override suspend fun usarAbono(idPlanta: Int): AbonoResultado {
        usosDeAbono += idPlanta
        return abono
    }
}

class TiendaViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repo = FakeTiendaRepository()
    private val garden = FakeGardenRepository()

    @Test
    fun carga_la_tienda_al_iniciar() {
        val vm = TiendaViewModel(repo, garden)
        val estado = vm.uiState as TiendaUiState.Listo
        assertEquals(300, estado.tienda.monedas)
        assertEquals(1, estado.tienda.fondos.size)
        assertEquals(1, estado.tienda.abono?.id)
    }

    @Test
    fun si_falla_al_cargar_muestra_el_error() {
        repo.respuesta = TiendaResultado.Fallo("sin conexion")
        assertEquals(TiendaUiState.Error("sin conexion"), TiendaViewModel(repo, garden).uiState)
    }

    @Test
    fun comprar_actualiza_la_tienda_y_avisa() {
        val vm = TiendaViewModel(repo, garden)
        repo.respuesta = TiendaResultado.Con(Tienda(200, 2, listOf(item(2, TiendaItem.FONDO, 100, comprado = true))))

        vm.comprar(item(2, TiendaItem.FONDO, 100))

        assertEquals(listOf(2), repo.compras)
        assertEquals(200, (vm.uiState as TiendaUiState.Listo).tienda.monedas)
        assertEquals("Compraste «Item 2»", vm.aviso)
    }

    @Test
    fun un_error_de_compra_solo_se_avisa_y_la_tienda_sigue_a_la_vista() {
        val vm = TiendaViewModel(repo, garden)
        repo.respuesta = TiendaResultado.Fallo("No te alcanzan las monedas")

        vm.comprar(item(2, TiendaItem.FONDO, 100))

        assertTrue(vm.uiState is TiendaUiState.Listo)
        assertEquals("No te alcanzan las monedas", vm.aviso)
    }

    @Test
    fun el_abono_se_usa_en_la_planta_del_jardin() {
        val vm = TiendaViewModel(repo, garden)

        vm.usarAbono()

        assertEquals(listOf(1), repo.usosDeAbono)
        assertTrue(vm.aviso!!.startsWith("¡Abono aplicado!"))
    }

    @Test
    fun sin_planta_no_se_cobra_el_abono() {
        garden.activa = PlantaActivaResultado.SinPlanta
        val vm = TiendaViewModel(repo, garden)

        vm.usarAbono()

        assertTrue(repo.usosDeAbono.isEmpty())
        assertEquals("Primero digitaliza una planta para abonarla", vm.aviso)
    }

    @Test
    fun el_error_del_abono_es_el_del_servidor() {
        repo.abono = AbonoResultado.Fallo("Ya usaste un abono con esta planta hoy")
        val vm = TiendaViewModel(repo, garden)

        vm.usarAbono()

        assertEquals("Ya usaste un abono con esta planta hoy", vm.aviso)
    }

    @Test
    fun el_aviso_se_borra_al_verlo() {
        val vm = TiendaViewModel(repo, garden)
        vm.comprar(item(2, TiendaItem.FONDO, 100))
        vm.avisoVisto()
        assertNull(vm.aviso)
    }
}
