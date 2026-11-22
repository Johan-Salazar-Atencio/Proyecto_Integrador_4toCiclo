package com.plantia.data.logros

import com.plantia.data.model.Rareza
import com.plantia.fakes.FakeLogrosRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.logroNuevo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GestorLogrosTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repo = FakeLogrosRepository()
    private val sesion = MutableStateFlow(SesionActual(userId = 7, token = "tok"))

    private fun gestor() = GestorLogros(repo, CoroutineScope(SupervisorJob() + Dispatchers.Main), sesion)

    @Test
    fun al_iniciar_con_sesion_trae_los_pendientes_y_los_celebra() {
        repo.pendientesResultado = listOf(logroNuevo(1))

        val g = gestor()

        assertEquals(1, g.actual.value?.idLogro)
        assertEquals(1, repo.consultasPendientes)
    }

    @Test
    fun sin_sesion_no_consulta_nada() {
        sesion.value = SesionActual(1, "")
        repo.pendientesResultado = listOf(logroNuevo(1))

        val g = gestor()
        g.revisar()

        assertEquals(0, repo.consultasPendientes)
        assertNull(g.actual.value)
    }

    @Test
    fun no_repite_un_logro_que_llega_por_dos_caminos() {
        repo.pendientesResultado = listOf(logroNuevo(1), logroNuevo(2))
        val g = gestor()

        // La misma accion trae el logro 2 y uno nuevo (3); el servidor tambien lo manda por pendientes
        g.encolar(listOf(logroNuevo(2), logroNuevo(3), logroNuevo(3)))
        g.revisar()

        assertEquals(3, g.enCola)
    }

    @Test
    fun ordena_por_rareza_y_no_cambia_el_que_ya_se_muestra() {
        val g = gestor()
        g.encolar(listOf(logroNuevo(1, Rareza.COMUN)))
        assertEquals(1, g.actual.value?.idLogro)

        g.encolar(listOf(logroNuevo(2, Rareza.RARA), logroNuevo(3, Rareza.LEGENDARIA), logroNuevo(4, Rareza.EPICA)))

        assertEquals(1, g.actual.value?.idLogro)
        val orden = mutableListOf<Int>()
        while (g.actual.value != null) {
            orden += g.actual.value!!.idLogro
            g.terminar(g.actual.value!!.idLogro)
        }
        assertEquals(listOf(1, 3, 4, 2), orden)
    }

    @Test
    fun no_marca_visto_hasta_que_la_celebracion_termina() {
        val g = gestor()
        g.encolar(listOf(logroNuevo(5)))

        assertTrue(repo.intentosVistos.isEmpty())

        g.terminar(5)

        assertEquals(listOf(5), repo.vistosGuardados)
        assertNull(g.actual.value)
    }

    @Test
    fun terminar_con_otro_id_o_dos_veces_no_hace_nada_extra() {
        val g = gestor()
        g.encolar(listOf(logroNuevo(5), logroNuevo(6)))
        val primero = g.actual.value!!.idLogro

        g.terminar(99)
        assertEquals(primero, g.actual.value?.idLogro)

        g.terminar(primero)
        g.terminar(primero)

        assertEquals(listOf(primero), repo.vistosGuardados)
        assertEquals(1, g.enCola)
    }

    @Test
    fun varios_a_la_vez_se_muestran_uno_tras_otro() {
        val g = gestor()
        g.encolar((1..4).map { logroNuevo(it) })

        val vistos = mutableListOf<Int>()
        while (g.actual.value != null) {
            vistos += g.actual.value!!.idLogro
            g.terminar(g.actual.value!!.idLogro)
        }

        assertEquals(listOf(1, 2, 3, 4), vistos)
        assertEquals(listOf(1, 2, 3, 4), repo.vistosGuardados)
    }

    @Test
    fun si_el_post_falla_reintenta_despues_y_no_vuelve_a_celebrarlo() {
        repo.vistosFunciona = false
        val g = gestor()
        g.encolar(listOf(logroNuevo(8)))
        g.terminar(8)
        assertTrue(repo.vistosGuardados.isEmpty())

        // El servidor sigue creyendo que esta pendiente: lo devuelve, pero ya se celebro
        repo.pendientesResultado = listOf(logroNuevo(8))
        repo.vistosFunciona = true
        g.revisar()

        assertNull(g.actual.value)
        assertEquals(listOf(8), repo.vistosGuardados)
    }

    @Test
    fun un_fallo_de_red_al_revisar_no_rompe_nada() {
        repo.pendientesResultado = null
        val g = gestor()

        g.revisar()

        assertNull(g.actual.value)
    }

    @Test
    fun al_cerrar_sesion_se_vacia_la_cola() {
        repo.pendientesResultado = listOf(logroNuevo(1), logroNuevo(2))
        val g = gestor()
        assertEquals(2, g.enCola)

        sesion.value = SesionActual(1, "")

        assertNull(g.actual.value)
        assertEquals(0, g.enCola)
    }

    @Test
    fun al_cambiar_de_cuenta_no_queda_nada_de_la_anterior() {
        repo.pendientesResultado = listOf(logroNuevo(1))
        val g = gestor()
        g.terminar(1)
        repo.intentosVistos.clear()

        // Otra cuenta: el servidor no tiene nada pendiente y el logro 1 ya no se considera celebrado
        repo.pendientesResultado = emptyList()
        sesion.value = SesionActual(9, "otro")
        assertNull(g.actual.value)

        // Si la nueva cuenta tuviera un logro con el mismo id, si debe celebrarse
        repo.pendientesResultado = listOf(logroNuevo(1))
        g.revisar()
        assertEquals(1, g.actual.value?.idLogro)
    }

    @Test
    fun ignora_logros_sin_id_valido() {
        val g = gestor()

        g.encolar(listOf(logroNuevo(0), logroNuevo(-3)))

        assertNull(g.actual.value)
    }
}
