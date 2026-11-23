package com.plantia.ui.navigation

import com.plantia.data.local.PermisoRedLocal
import com.plantia.ui.components.TextosRedLocal
import com.plantia.ui.scan.components.TextosPermisoCamara
import com.plantia.ui.tienda.TextosCompra
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** QA-56, QA-55, QA-62 y QA-64 del QA en emulador. */
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
