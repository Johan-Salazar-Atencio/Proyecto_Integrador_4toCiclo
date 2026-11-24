package com.plantia.ui.navigation

import com.plantia.data.local.PermisoRedLocal
import com.plantia.ui.components.TextosPermisos
import com.plantia.ui.components.TextosRedLocal
import com.plantia.ui.scan.components.TextosPermisoCamara
import com.plantia.ui.tienda.TextosCompra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** QA-56, QA-55, QA-59, QA-62 y QA-64 del QA en emulador. */
class InicioYPermisosTest {

    @Test
    fun con_sesion_guardada_abre_en_el_inicio_y_sin_ella_en_la_bienvenida() {
        assertEquals(Routes.HOME, Routes.inicial("abc123"))
        assertEquals(Routes.WELCOME, Routes.inicial(""))
        assertEquals(Routes.WELCOME, Routes.inicial("   "))
    }

    @Test
    fun el_aviso_de_red_local_solo_aparece_si_se_nego_en_android_17_o_superior() {
        PermisoRedLocal.actualizar(37, concedido = false)
        assertTrue(PermisoRedLocal.negado.value)
        PermisoRedLocal.actualizar(37, concedido = true)
        assertFalse(PermisoRedLocal.negado.value)
        PermisoRedLocal.actualizar(34, concedido = false) // en versiones viejas no existe ese permiso
        assertFalse(PermisoRedLocal.negado.value)
        assertTrue(TextosRedLocal.EXPLICACION.contains("Ajustes"))
    }

    @Test
    fun la_primera_vez_se_explica_la_red_local_antes_de_pedirla() {
        val paso = PermisoRedLocal.PasoAlIniciar.EXPLICAR
        assertEquals(paso, PermisoRedLocal.pasoAlIniciar(37, concedido = false, yaExplicado = false))
        assertEquals(paso, PermisoRedLocal.pasoAlIniciar(38, concedido = false, yaExplicado = false))
    }

    @Test
    fun si_ya_se_explico_y_sigue_sin_permiso_se_pide_directamente() {
        assertEquals(
            PermisoRedLocal.PasoAlIniciar.PEDIR,
            PermisoRedLocal.pasoAlIniciar(37, concedido = false, yaExplicado = true)
        )
    }

    @Test
    fun con_permiso_o_en_versiones_sin_el_no_se_explica_ni_se_pide() {
        val nada = PermisoRedLocal.PasoAlIniciar.NINGUNO
        assertEquals(nada, PermisoRedLocal.pasoAlIniciar(37, concedido = true, yaExplicado = false))
        assertEquals(nada, PermisoRedLocal.pasoAlIniciar(37, concedido = true, yaExplicado = true))
        assertEquals(nada, PermisoRedLocal.pasoAlIniciar(36, concedido = false, yaExplicado = false))
        assertEquals(nada, PermisoRedLocal.pasoAlIniciar(24, concedido = false, yaExplicado = true))
    }

    @Test
    fun el_mdns_solo_arranca_con_permiso_de_red_local_o_sin_ese_permiso_en_el_sistema() {
        assertFalse(PermisoRedLocal.puedeBuscarEnRed(37, concedido = false))
        assertTrue(PermisoRedLocal.puedeBuscarEnRed(37, concedido = true))
        assertTrue(PermisoRedLocal.puedeBuscarEnRed(36, concedido = false))
    }

    @Test
    fun las_explicaciones_dicen_para_que_es_cada_permiso() {
        assertTrue(TextosPermisos.RED_EXPLICACION.startsWith("Plantia se conecta al servidor de tu red (el PC del laboratorio o de casa)"))
        assertTrue(TextosPermisos.RED_EXPLICACION.contains("identificar tus plantas"))
        assertTrue(TextosPermisos.NOTIF_EXPLICACION.contains("avisarte de logros y de tus plantas"))
        assertEquals("Continuar", TextosPermisos.CONTINUAR)
    }

    @Test
    fun sin_camara_se_ofrece_pedirla_o_ir_a_ajustes_y_la_galeria() {
        assertEquals("Permitir cámara", TextosPermisoCamara.boton(negadoParaSiempre = false))
        assertEquals("Abrir ajustes", TextosPermisoCamara.boton(negadoParaSiempre = true))
        assertTrue(TextosPermisoCamara.EXPLICACION.contains("galería"))
    }

    @Test
    fun la_compra_se_confirma_con_nombre_y_precio() {
        assertEquals("¿Comprar Maceta rosa?", TextosCompra.titulo("Maceta rosa"))
        assertEquals("Se descuentan 80 hojas de tu saldo.", TextosCompra.detalle(80))
    }
}
