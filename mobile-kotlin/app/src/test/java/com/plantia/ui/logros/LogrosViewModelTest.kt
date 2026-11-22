package com.plantia.ui.logros

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import com.plantia.data.logros.AvisosLogros
import com.plantia.data.model.LogrosResumen
import com.plantia.data.model.Rareza
import com.plantia.data.repository.LogrosResultado
import com.plantia.fakes.FakeLogrosRepository
import com.plantia.fakes.MainDispatcherRule
import com.plantia.fakes.logro
import com.plantia.fakes.logroNuevo
import com.plantia.ui.logros.viewmodel.FiltroLogros
import com.plantia.ui.logros.viewmodel.LogrosViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class LogrosViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private val repo = FakeLogrosRepository()

    private val lista = listOf(
        logro(1, desbloqueado = true), logro(2, progreso = 3), logro(3, desbloqueado = true), logro(4, secreto = true)
    )

    private fun viewModel(): LogrosViewModel {
        repo.listaResultado = LogrosResultado.Con(LogrosResumen(4, 2, lista))
        return LogrosViewModel(repo)
    }

    @Test
    fun carga_los_logros_y_cuenta_cada_filtro() {
        val estado = viewModel().uiState

        assertFalse(estado.cargando)
        assertEquals(4, estado.conteo(FiltroLogros.TODOS))
        assertEquals(2, estado.conteo(FiltroLogros.DESBLOQUEADOS))
        assertEquals(2, estado.conteo(FiltroLogros.BLOQUEADOS))
    }

    @Test
    fun el_filtro_deja_solo_los_que_corresponden() {
        val vm = viewModel()

        vm.elegirFiltro(FiltroLogros.DESBLOQUEADOS)
        assertEquals(listOf(1, 3), vm.uiState.visibles.map { it.idLogro })

        vm.elegirFiltro(FiltroLogros.BLOQUEADOS)
        assertEquals(listOf(2, 4), vm.uiState.visibles.map { it.idLogro })

        vm.elegirFiltro(FiltroLogros.TODOS)
        assertEquals(4, vm.uiState.visibles.size)
    }

    @Test
    fun un_fallo_sin_datos_muestra_el_error_y_reintentar_lo_arregla() {
        repo.listaResultado = LogrosResultado.Fallo("sin red")
        val vm = LogrosViewModel(repo)
        assertNull(vm.uiState.resumen)
        assertEquals("sin red", vm.uiState.error)

        repo.listaResultado = LogrosResultado.Con(LogrosResumen(4, 2, lista))
        vm.cargar()

        assertNotNull(vm.uiState.resumen)
        assertNull(vm.uiState.error)
    }

    @Test
    fun un_fallo_al_recargar_conserva_lo_que_ya_se_veia() {
        val vm = viewModel()

        repo.listaResultado = LogrosResultado.Fallo("sin red")
        vm.cargar()

        assertEquals(4, vm.uiState.visibles.size)
        assertEquals("sin red", vm.uiState.error)
    }

    @Test
    fun el_avance_de_un_logro_se_calcula_con_su_meta() {
        assertEquals(0.3f, logro(1, progreso = 3, meta = 10).fraccion, 0.001f)
        assertEquals(1f, logro(1, progreso = 99, meta = 10).fraccion, 0.001f)
        assertEquals(0f, logro(1, progreso = 5, meta = 0).fraccion, 0.001f)
        assertEquals(1f, logro(1, desbloqueado = true, progreso = 0).fraccion, 0.001f)
    }

    @Test
    fun una_clave_de_icono_desconocida_usa_el_icono_por_defecto() {
        assertSame(Icons.Filled.EmojiEvents, iconoDeLogro("clave-nueva-del-admin"))
        assertSame(Icons.Filled.EmojiEvents, iconoDeLogro(null))
        assertSame(Icons.Filled.EmojiEvents, iconoDeLogro(""))
        assertSame(iconoDeLogro("hoja"), iconoDeLogro(" HOJA "))
    }

    @Test
    fun la_rareza_desconocida_cuenta_como_comun() {
        assertEquals(Rareza.COMUN, Rareza.desde("MITICA"))
        assertEquals(Rareza.COMUN, Rareza.desde(null))
        assertEquals(Rareza.LEGENDARIA, Rareza.desde("legendaria"))
    }

    @Test
    fun la_fecha_del_servidor_se_muestra_corta() {
        assertEquals("20 nov 2026", fechaLogro("2026-11-20T10:15:30"))
        assertEquals("5 ene 2027", fechaLogro("2027-01-05"))
        assertNull(fechaLogro(null))
        assertNull(fechaLogro("ayer"))
        assertNull(fechaLogro("2026-13-01"))
    }

    @Test
    fun la_recompensa_solo_muestra_lo_que_da() {
        assertEquals("+50 XP · +10 hojas", textoRecompensa(50, 10))
        assertEquals("+1 hoja", textoRecompensa(0, 1))
        assertEquals("", textoRecompensa(0, 0))
    }

    @Test
    fun el_aviso_fuera_de_la_app_no_repite_los_ya_avisados() {
        val pendientes = listOf(logroNuevo(1), logroNuevo(2), logroNuevo(2), logroNuevo(3))

        val nuevos = AvisosLogros.porAvisar(pendientes, avisados = setOf(1))

        assertEquals(listOf(2, 3), nuevos.map { it.idLogro })
    }

    @Test
    fun el_aviso_resume_cuando_son_varios() {
        val (titulo1, texto1) = AvisosLogros.textoAviso(listOf(logroNuevo(1, titulo = "Primera hoja")))
        assertEquals("¡Logro desbloqueado!", titulo1)
        assertEquals("Primera hoja: Descripcion 1", texto1)

        val (titulo2, texto2) = AvisosLogros.textoAviso(listOf(logroNuevo(1, titulo = "A"), logroNuevo(2, titulo = "B")))
        assertEquals("¡2 logros desbloqueados!", titulo2)
        assertEquals("A · B", texto2)
    }
}
