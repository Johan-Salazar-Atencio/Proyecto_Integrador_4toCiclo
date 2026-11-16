package com.plantia.ui.collection

import com.plantia.data.repository.ColeccionResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.data.repository.PlantasResultado
import com.plantia.fakes.FakeCollectionRepository
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.FakeUsuarioRepository
import com.plantia.fakes.usuarioEjemplo
import com.plantia.data.model.TiendaItem
import com.plantia.data.model.Tienda
import com.plantia.data.repository.TiendaResultado
import com.plantia.ui.tienda.FakeTiendaRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.planta
import com.plantia.ui.collection.viewmodel.CollectionUiState
import com.plantia.ui.collection.viewmodel.CollectionViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CollectionViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val collection = FakeCollectionRepository()
    private val garden = FakeGardenRepository()

    @Test
    fun une_las_plantas_escaneadas_y_las_digitales() {
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1), planta(id = 2, activa = false)))
        val vm = CollectionViewModel(collection, garden)
        val estado = vm.uiState as CollectionUiState.Success
        assertEquals(2, estado.digitales.size)
    }

    @Test
    fun si_fallan_las_digitales_la_coleccion_se_muestra_igual() {
        garden.lista = PlantasResultado.Fallo("sin red")
        val estado = CollectionViewModel(collection, garden).uiState as CollectionUiState.Success
        assertTrue(estado.digitales.isEmpty())
    }

    @Test
    fun si_falla_la_coleccion_muestra_error() {
        collection.lista = ColeccionResultado.Fallo("HTTP 500")
        val estado = CollectionViewModel(collection, garden).uiState as CollectionUiState.Error
        assertEquals("HTTP 500", estado.mensaje)
    }

    @Test
    fun seleccionar_navega_solo_si_el_servidor_confirma() {
        val vm = CollectionViewModel(collection, garden)
        var navego = false
        vm.seleccionar(planta(id = 4)) { navego = true }
        assertTrue(navego)

        garden.alSeleccionar = PlantaResultado.Fallo("HTTP 404")
        navego = false
        vm.seleccionar(planta(id = 4)) { navego = true }
        assertEquals(false, navego)
        assertEquals("No se pudo elegir la planta: HTTP 404", vm.aviso)
    }

    private fun invernadero(id: Int, codigo: String, comprado: Boolean) = TiendaItem(
        id = id, nombre = "Tema $codigo", tipo = TiendaItem.INVERNADERO, descripcion = "", precio = 0,
        nivelRequerido = 1, previewUrl = null, variantes = emptyList(), comprado = comprado, bloqueado = false,
        codigo = codigo
    )

    @Test
    fun guardar_en_el_baul_cambia_solo_esa_planta_y_avisa() {
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1), planta(id = 2, activa = false)))
        val vm = CollectionViewModel(collection, garden)

        vm.guardarEnBaul(planta(id = 2))

        assertEquals(listOf(2), garden.guardados)
        assertEquals("Monstera descansa en el baúl", vm.aviso)
    }

    @Test
    fun el_baul_lleno_o_cualquier_fallo_se_avisa_sin_tocar_la_lista() {
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1).copy(enBaul = true)))
        garden.alSacar = PlantaResultado.Fallo("Tu invernadero está lleno")
        val vm = CollectionViewModel(collection, garden)

        vm.sacarDelBaul(planta(id = 1))

        assertEquals("Tu invernadero está lleno", vm.aviso)
        val estado = vm.uiState as CollectionUiState.Success
        assertTrue(estado.digitales.single().enBaul)
    }

    @Test
    fun renombrar_manda_el_apodo_y_deja_la_planta_con_el_nombre_nuevo() {
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1)))
        garden.alRenombrar = PlantaResultado.Con(planta(id = 1).copy(name = "Fifi"))
        val vm = CollectionViewModel(collection, garden)

        vm.renombrar(planta(id = 1), "Fifi")

        assertEquals(listOf(1 to "Fifi"), garden.apodos)
        assertEquals("Fifi", (vm.uiState as CollectionUiState.Success).digitales.single().name)
        assertEquals("Ahora se llama Fifi", vm.aviso)
    }

    @Test
    fun carga_el_invernadero_elegido_y_solo_los_que_el_usuario_tiene() {
        val usuarios = FakeUsuarioRepository().also { it.perfil = usuarioEjemplo().copy(greenhouseCode = "LUCES") }
        val tienda = FakeTiendaRepository().also {
            it.respuesta = TiendaResultado.Con(
                Tienda(10, 1, listOf(invernadero(1, "SELVA", true), invernadero(2, "VICTORIANO", false), invernadero(3, "LUCES", true)))
            )
        }

        val vm = CollectionViewModel(collection, garden, tienda, usuarios)

        assertEquals("LUCES", vm.invernadero)
        assertEquals(listOf("SELVA", "LUCES"), vm.invernaderosPropios.map { it.codigo })
    }

    @Test
    fun elegir_invernadero_lo_cambia_si_el_servidor_confirma_y_si_no_avisa() {
        val usuarios = FakeUsuarioRepository()
        val vm = CollectionViewModel(collection, garden, FakeTiendaRepository(), usuarios)
        val luces = invernadero(3, "LUCES", true)

        vm.elegirInvernadero(luces)
        assertEquals("No se pudo cambiar el invernadero", vm.aviso)
        assertEquals("SELVA", vm.invernadero)

        usuarios.alCambiarInvernadero = usuarioEjemplo().copy(greenhouseCode = "LUCES")
        vm.elegirInvernadero(luces)
        assertEquals("LUCES", vm.invernadero)
        assertEquals(listOf(3, 3), usuarios.invernaderos)
    }
}
