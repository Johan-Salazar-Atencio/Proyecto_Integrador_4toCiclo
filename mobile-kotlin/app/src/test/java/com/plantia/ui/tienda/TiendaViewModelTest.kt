package com.plantia.ui.tienda

import com.plantia.data.model.Tienda
import com.plantia.data.model.TiendaItem
import com.plantia.data.repository.AbonoResultado
import com.plantia.data.repository.FondosResultado
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.PlantasResultado
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

private fun item(id: Int, tipo: String, precio: Int, comprado: Boolean = false) = TiendaItem(
    id = id, nombre = "Item $id", tipo = tipo, descripcion = "", precio = precio, nivelRequerido = 1,
    previewUrl = null, variantes = emptyList(), comprado = comprado, bloqueado = false
)

internal class FakeTiendaRepository : TiendaRepository {
    var tienda = Tienda(300, 2, listOf(item(1, TiendaItem.ABONO, 30), item(2, TiendaItem.FONDO, 100)))
    var respuesta: TiendaResultado = TiendaResultado.Con(tienda)
    var abono: AbonoResultado = AbonoResultado.Con(planta())
    val compras = mutableListOf<Int>()
    val usosDeAbono = mutableListOf<Int>()
    var fondos: FondosResultado = FondosResultado.Con(emptyList())
    val fondosPuestos = mutableListOf<Pair<Int, Int?>>()

    override suspend fun cargar() = respuesta
    override suspend fun comprar(idItem: Int): TiendaResultado {
        compras += idItem
        return respuesta
    }
    override suspend fun usarAbono(idPlanta: Int): AbonoResultado {
        usosDeAbono += idPlanta
        return abono
    }
    override suspend fun fondosDePlanta(idPlanta: Int) = fondos
    override suspend fun ponerFondo(idPlanta: Int, idTema: Int?): FondosResultado {
        fondosPuestos += idPlanta to idTema
        return fondos
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
        assertTrue(vm.aviso!!.startsWith("Compraste «Item 2»"))
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

    private fun maceta(id: Int, comprado: Boolean = true) = TiendaItem(
        id = id, nombre = "Azul", tipo = TiendaItem.MACETA, descripcion = "", precio = 0, nivelRequerido = 1,
        previewUrl = null, variantes = emptyList(), comprado = comprado, bloqueado = false, codigo = "AZUL"
    )

    @Test
    fun al_abrir_la_hoja_de_macetas_solo_se_ofrecen_las_plantas_fuera_del_baul() {
        garden.lista = PlantasResultado.Con(
            listOf(planta(id = 1), planta(id = 2, activa = false).copy(enBaul = true), planta(id = 3, activa = false))
        )
        val vm = TiendaViewModel(repo, garden)

        vm.abrirAplicar(maceta(9))

        assertEquals(9, vm.aplicando?.id)
        assertEquals(listOf(1, 3), vm.plantasParaMaceta.map { it.id })
    }

    @Test
    fun poner_la_maceta_manda_las_plantas_elegidas_y_cierra_la_hoja() {
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1), planta(id = 3, activa = false)))
        val vm = TiendaViewModel(repo, garden)
        vm.abrirAplicar(maceta(9))

        vm.aplicarMaceta(listOf(1, 3))

        assertEquals(listOf(9 to listOf(1, 3)), garden.macetas)
        assertNull(vm.aplicando)
        assertEquals("«Azul» puesta a 2 plantas", vm.aviso)
    }

    @Test
    fun si_el_servidor_rechaza_la_maceta_la_hoja_sigue_abierta_y_se_avisa() {
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1)))
        garden.alPonerMaceta = PlantasResultado.Fallo("Primero compra esta maceta")
        val vm = TiendaViewModel(repo, garden)
        vm.abrirAplicar(maceta(9))

        vm.aplicarMaceta(listOf(1))

        assertEquals("Primero compra esta maceta", vm.aviso)
        assertEquals(9, vm.aplicando?.id)
    }

    @Test
    fun comprar_una_maceta_o_un_invernadero_dice_donde_usarlos() {
        val vm = TiendaViewModel(repo, garden)

        vm.comprar(maceta(9, comprado = false))
        assertTrue(vm.aviso!!.contains("Poner a mis plantas"))

        vm.avisoVisto()
        vm.comprar(maceta(10, comprado = false).copy(tipo = TiendaItem.INVERNADERO, nombre = "Luces"))
        assertTrue(vm.aviso!!.contains("chip de tu colección"))
    }
}
