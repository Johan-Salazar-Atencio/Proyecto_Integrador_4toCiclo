package com.plantia.ui.profile

import com.plantia.data.local.Recordatorios
import com.plantia.data.repository.PlantaResultado
import com.plantia.data.repository.PlantasResultado
import com.plantia.fakes.FakeGardenRepository
import com.plantia.fakes.FakeUsuarioRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.planta
import com.plantia.ui.profile.viewmodel.EditarDatosViewModel
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Editar datos: el nombre del usuario y el de cada planta se guardan en el servidor. */
@OptIn(ExperimentalCoroutinesApi::class)
class EditarDatosViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val usuarios = FakeUsuarioRepository()
    private val garden = FakeGardenRepository()

    private fun viewModel() = EditarDatosViewModel(usuarios, garden)

    @Test
    fun al_abrir_carga_el_nombre_y_las_plantas() {
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1), planta(id = 2)))

        val vm = viewModel()

        assertEquals(usuarios.perfil?.name, vm.nombreGuardado)
        assertEquals(2, vm.plantas.size)
    }

    @Test
    fun guardar_el_nombre_lo_envia_sin_espacios_y_lo_deja_guardado() {
        val vm = viewModel()

        vm.guardarNombre("  Ana Torres ")

        assertEquals(listOf("Ana Torres"), usuarios.nombres)
        assertEquals("Ana Torres", vm.nombreGuardado)
        assertEquals("Nombre guardado", vm.aviso)
    }

    @Test
    fun un_nombre_de_una_letra_no_se_envia() {
        val vm = viewModel()

        vm.guardarNombre("A")

        assertTrue(usuarios.nombres.isEmpty())
        assertNotNull(vm.aviso)
    }

    @Test
    fun guardar_el_nombre_de_una_planta_la_actualiza_en_la_lista() {
        garden.lista = PlantasResultado.Con(listOf(planta(id = 1)))
        garden.alRenombrar = PlantaResultado.Con(planta(id = 1).copy(name = "Rosita"))
        val vm = viewModel()

        vm.guardarPlanta(vm.plantas.single(), "Rosita")

        assertEquals("Rosita", vm.plantas.single().name)
    }

    @Test
    fun el_recordatorio_sale_a_las_nueve_de_hoy_si_aun_no_pasaron_o_de_manana() {
        val antes = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 16, 7, 30, 0) }.timeInMillis
        val despues = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 16, 10, 0, 0) }.timeInMillis

        val hoy = Calendar.getInstance().apply { timeInMillis = Recordatorios.proximaHora(antes) }
        val manana = Calendar.getInstance().apply { timeInMillis = Recordatorios.proximaHora(despues) }

        assertEquals(16, hoy.get(Calendar.DAY_OF_MONTH))
        assertEquals(Recordatorios.HORA, hoy.get(Calendar.HOUR_OF_DAY))
        assertEquals(17, manana.get(Calendar.DAY_OF_MONTH))
    }
}
