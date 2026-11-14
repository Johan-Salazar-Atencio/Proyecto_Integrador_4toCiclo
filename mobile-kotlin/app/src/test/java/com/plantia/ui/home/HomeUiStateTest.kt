package com.plantia.ui.home

import androidx.compose.ui.graphics.Color
import com.plantia.data.model.QuickAccessItem
import com.plantia.ui.home.viewmodel.HomeUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeUiStateTest {

    private fun acceso(id: String, subtitulo: String = "") =
        QuickAccessItem(id = id, title = id, subtitle = subtitulo, icon = "", backgroundColor = Color.White, iconColor = Color.Black)

    @Test
    fun los_accesos_no_incluyen_el_jardin() {
        val estado = HomeUiState(accesosServidor = listOf(acceso("scan"), acceso("garden"), acceso("jardin")))
        assertEquals(listOf("scan"), estado.accesos.map { it.id })
    }

    @Test
    fun el_subtitulo_de_la_coleccion_usa_el_total() {
        val base = HomeUiState(accesosServidor = listOf(acceso("collection", "Tus plantas")))
        assertEquals("Tus plantas", base.accesos.single().subtitle)
        assertEquals("Aun sin plantas", base.copy(totalColeccion = 0).accesos.single().subtitle)
        assertEquals("1 planta descubierta", base.copy(totalColeccion = 1).accesos.single().subtitle)
        assertEquals("5 plantas descubiertas", base.copy(totalColeccion = 5).accesos.single().subtitle)
    }
}
