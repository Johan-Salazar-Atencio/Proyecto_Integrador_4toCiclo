package com.plantia.ui.logros

import com.plantia.data.logros.GestorLogros
import com.plantia.data.logros.SesionActual
import com.plantia.fakes.FakeLogrosRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.logroNuevo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** "D + aviso al terminar": cuando sale el aviso, cuando se va y cuando se marca visto. */
@OptIn(ExperimentalCoroutinesApi::class)
class ControlCelebracionTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repo = FakeLogrosRepository()
    // Se crea despues de que la regla sustituye el hilo principal
    private val gestor by lazy {
        GestorLogros(repo, CoroutineScope(SupervisorJob() + Dispatchers.Main), MutableStateFlow(SesionActual(7, "tok")))
    }

    private fun TestScope.control() = ControlCelebracion(gestor, backgroundScope)

    @Test
    fun el_aviso_sale_al_terminar_la_animacion_y_no_marca_visto() = runTest {
        val c = control()
        val logro = logroNuevo(1)
        gestor.encolar(listOf(logro))
        c.alEmpezar("logro-1")
        assertNull(c.aviso.value)

        c.alTerminarAnimacion(logro, reducido = false)

        assertEquals(1, c.aviso.value?.logro?.idLogro)
        // Marcar visto es al CERRAR la medalla, no al terminar la animacion
        assertTrue(repo.intentosVistos.isEmpty())
        assertEquals(1, gestor.actual.value?.idLogro)
    }

    @Test
    fun al_cerrar_la_medalla_se_marca_visto_y_el_aviso_sigue_sobre_la_app() = runTest {
        val c = control()
        val logro = logroNuevo(1)
        gestor.encolar(listOf(logro))
        c.alEmpezar("logro-1")
        c.alTerminarAnimacion(logro, reducido = false)

        c.cerrar(logro)
        c.alEmpezar(null)

        assertEquals(listOf(1), repo.vistosGuardados)
        assertNotNull(c.aviso.value)
    }

    @Test
    fun el_aviso_se_va_solo_a_los_4_segundos() = runTest {
        val c = control()
        c.alTerminarAnimacion(logroNuevo(1), reducido = false)

        advanceTimeBy(3_999)
        runCurrent()
        assertNotNull(c.aviso.value)

        advanceTimeBy(2)
        runCurrent()
        assertNull(c.aviso.value)
    }

    @Test
    fun con_animaciones_quitadas_dura_6_segundos() = runTest {
        val c = control()
        c.alTerminarAnimacion(logroNuevo(1), reducido = true)

        advanceTimeBy(5_999)
        runCurrent()
        assertNotNull(c.aviso.value)

        advanceTimeBy(2)
        runCurrent()
        assertNull(c.aviso.value)
    }

    @Test
    fun un_toque_quita_el_aviso() = runTest {
        val c = control()
        c.alTerminarAnimacion(logroNuevo(1), reducido = false)

        c.quitarAviso(c.aviso.value!!.serie)

        assertNull(c.aviso.value)
    }

    @Test
    fun si_empieza_otra_celebracion_el_aviso_anterior_se_quita() = runTest {
        val c = control()
        val a = logroNuevo(1)
        val b = logroNuevo(2)
        gestor.encolar(listOf(a, b))
        c.alEmpezar("logro-1")
        c.alTerminarAnimacion(a, reducido = false)

        c.cerrar(a)
        c.alEmpezar("logro-2")

        assertNull(c.aviso.value)
    }

    @Test
    fun el_mismo_logro_recompuesto_no_quita_su_aviso() = runTest {
        val c = control()
        val a = logroNuevo(1)
        c.alEmpezar("logro-1")
        c.alTerminarAnimacion(a, reducido = false)

        c.alEmpezar("logro-1")

        assertNotNull(c.aviso.value)
    }

    @Test
    fun el_temporizador_de_un_aviso_viejo_no_quita_el_nuevo() = runTest {
        val c = control()
        c.alTerminarAnimacion(logroNuevo(1), reducido = false)
        advanceTimeBy(3_000)
        c.alTerminarAnimacion(logroNuevo(2), reducido = false)

        advanceTimeBy(1_500)
        runCurrent()

        assertEquals(2, c.aviso.value?.logro?.idLogro)
        advanceTimeBy(3_000)
        runCurrent()
        assertNull(c.aviso.value)
    }

    @Test
    fun la_entrada_se_marca_una_vez_para_no_volver_a_bajar_al_cambiar_de_capa() = runTest {
        val c = control()
        c.alTerminarAnimacion(logroNuevo(1), reducido = false)
        val serie = c.aviso.value!!.serie

        c.marcarEntrada(serie + 1)
        assertEquals(false, c.aviso.value!!.entro)
        c.marcarEntrada(serie)
        assertEquals(true, c.aviso.value!!.entro)
    }

    @Test
    fun cerrar_el_resumen_y_saltar_todos_marcan_vistos_juntos() = runTest {
        val c = control()
        gestor.encolar((1..3).map { logroNuevo(it) })

        c.cerrarResumen(listOf(logroNuevo(2), logroNuevo(3)))
        assertEquals(listOf(2, 3), repo.vistosGuardados)

        c.saltarTodos()
        assertEquals(listOf(2, 3, 1), repo.vistosGuardados)
        assertNull(gestor.celebracion.value)
    }
}
