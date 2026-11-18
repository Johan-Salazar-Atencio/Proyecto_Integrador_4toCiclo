package com.plantia.ui.profile

import com.plantia.data.local.Recordatorios
import com.plantia.fakes.FakeUsuarioRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.ui.profile.viewmodel.EditarDatosViewModel
import java.util.Calendar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Editar datos: el nombre del usuario se guarda en el servidor y el correo solo se muestra. */
@OptIn(ExperimentalCoroutinesApi::class)
class EditarDatosViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val usuarios = FakeUsuarioRepository()

    private fun viewModel() = EditarDatosViewModel(usuarios)

    @Test
    fun al_abrir_carga_el_nombre_y_el_correo() {
        val vm = viewModel()

        assertEquals(usuarios.perfil?.name, vm.nombreGuardado)
        assertEquals(usuarios.perfil?.email.orEmpty(), vm.correo)
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
