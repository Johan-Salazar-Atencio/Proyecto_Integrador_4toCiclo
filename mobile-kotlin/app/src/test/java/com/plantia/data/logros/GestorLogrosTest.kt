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

    // ---- Regla del resumen y cierre de varios ----

    private fun GestorLogros.una() = celebracion.value as Celebracion.Una

    @Test
    fun con_tres_logros_se_celebran_todos_el_mas_raro_primero() {
        val g = gestor()
        g.encolar(listOf(logroNuevo(1, Rareza.COMUN), logroNuevo(2, Rareza.EPICA), logroNuevo(3, Rareza.RARA)))

        val orden = mutableListOf<Int>()
        while (g.celebracion.value != null) {
            val c = g.una()
            assertEquals(3, c.total)
            assertEquals(orden.size + 1, c.numero)
            orden += c.logro.idLogro
            g.terminar(c.logro.idLogro)
        }

        assertEquals(listOf(2, 3, 1), orden)
        assertEquals(listOf(2, 3, 1), repo.vistosGuardados)
    }

    @Test
    fun desde_el_cuarto_se_celebran_los_dos_mas_raros_y_luego_el_resumen() {
        val g = gestor()
        g.encolar(
            listOf(
                logroNuevo(1, Rareza.COMUN), logroNuevo(2, Rareza.RARA), logroNuevo(3, Rareza.EPICA),
                logroNuevo(4, Rareza.RARA), logroNuevo(5, Rareza.COMUN)
            )
        )

        assertEquals(3, g.una().logro.idLogro)
        assertEquals(4, g.una().quedanDespues)
        g.terminar(3)
        assertEquals(2, g.una().logro.idLogro)
        g.terminar(2)

        val resumen = g.celebracion.value as Celebracion.Resumen
        assertEquals(listOf(4, 1, 5), resumen.logros.map { it.idLogro })
        // Aun no se marcaron los del resumen
        assertEquals(listOf(3, 2), repo.vistosGuardados)

        g.terminarVarios(resumen.logros.map { it.idLogro })

        assertNull(g.celebracion.value)
        assertEquals(listOf(3, 2, 4, 1, 5), repo.vistosGuardados)
    }

    @Test
    fun con_cuatro_justos_el_resumen_lleva_dos() {
        val g = gestor()
        g.encolar((1..4).map { logroNuevo(it) })
        g.terminar(1)
        g.terminar(2)

        assertEquals(listOf(3, 4), (g.celebracion.value as Celebracion.Resumen).logros.map { it.idLogro })
    }

    @Test
    fun la_medalla_que_se_muestra_no_se_cambia_por_el_resumen_si_llegan_mas() {
        val g = gestor()
        g.encolar((1..3).map { logroNuevo(it) })
        g.terminar(1)
        g.terminar(2)
        assertEquals(3, g.una().logro.idLogro)

        // Mientras se ve la 3.ª llegan dos mas: la 3.ª sigue, y al cerrarla sale el resumen
        g.encolar(listOf(logroNuevo(4), logroNuevo(5)))
        assertEquals(3, g.una().logro.idLogro)
        assertEquals(5, g.una().total)

        g.terminar(3)
        assertEquals(listOf(4, 5), (g.celebracion.value as Celebracion.Resumen).logros.map { it.idLogro })
    }

    @Test
    fun una_tanda_nueva_empieza_de_cero() {
        val g = gestor()
        g.encolar((1..2).map { logroNuevo(it) })
        g.terminar(1)
        g.terminar(2)
        assertNull(g.celebracion.value)

        // La cola se vacio: los siguientes 3 se celebran todos, sin resumen
        g.encolar((3..5).map { logroNuevo(it) })
        assertEquals(1, g.una().numero)
        g.terminar(3)
        g.terminar(4)
        assertEquals(5, g.una().logro.idLogro)
    }

    @Test
    fun saltar_todos_cierra_la_cola_entera_y_marca_vistos() {
        val g = gestor()
        g.encolar((1..3).map { logroNuevo(it) })

        g.terminarVarios(g.idsEnCola())

        assertNull(g.celebracion.value)
        assertEquals(0, g.enCola)
        assertEquals(listOf(1, 2, 3), repo.vistosGuardados)
    }

    @Test
    fun terminar_varios_ignora_ids_que_no_estan_y_deja_el_resto() {
        val g = gestor()
        g.encolar((1..3).map { logroNuevo(it) })

        g.terminarVarios(listOf(2, 99))

        assertEquals(listOf(1, 3), g.idsEnCola())
        assertEquals(listOf(2), repo.vistosGuardados)
        g.terminarVarios(listOf(98))
        assertEquals(listOf(2), repo.vistosGuardados)
    }

    @Test
    fun los_vistos_se_envian_en_lotes_de_cien_como_maximo() {
        val g = gestor()
        g.encolar((1..250).map { logroNuevo(it) })

        g.terminarVarios(g.idsEnCola())

        assertEquals(listOf(100, 100, 50), repo.intentosVistos.map { it.size })
        assertTrue(repo.intentosVistos.all { it.size <= GestorLogros.LOTE_VISTOS })
        assertEquals((1..250).toList(), repo.vistosGuardados)
    }

    @Test
    fun si_falla_un_lote_reintenta_solo_lo_que_falta() {
        val g = gestor()
        g.encolar((1..150).map { logroNuevo(it) })
        repo.vistosFunciona = false
        g.terminarVarios(g.idsEnCola())
        assertTrue(repo.vistosGuardados.isEmpty())
        assertEquals(100, repo.intentosVistos.single().size)

        repo.vistosFunciona = true
        g.revisar()

        assertEquals((1..150).toList(), repo.vistosGuardados)
        assertTrue(repo.intentosVistos.all { it.size <= GestorLogros.LOTE_VISTOS })
    }
}
