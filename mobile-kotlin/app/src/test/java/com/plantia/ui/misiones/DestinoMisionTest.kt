package com.plantia.ui.misiones

import com.plantia.data.model.DailyMission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DestinoMisionTest {

    private fun mision(tipo: String?, kind: String? = null) = DailyMission("m", "x", "+1", 0f, 0, 1, false, kind = kind, type = tipo)

    @Test
    fun la_mision_del_evento_lleva_a_las_plantas_virtuales() {
        assertEquals(DestinoMision.PLANTAS_VIRTUALES, destinoDeMision(mision(null, kind = "EVENT")))
    }

    @Test
    fun los_cuidados_y_prevenir_llevan_a_las_plantas_virtuales() {
        listOf("RIEGO", "SOL", "ABONO", "CUIDADO", "REFUGIO").forEach {
            assertEquals(it, DestinoMision.PLANTAS_VIRTUALES, destinoDeMision(mision(it)))
        }
    }

    @Test
    fun escanear_digitalizar_y_ampliar_la_coleccion_empiezan_en_el_escaner() {
        listOf("ESCANEO", "DIGITALIZAR", "COLECCION").forEach {
            assertEquals(it, DestinoMision.ESCANEAR, destinoDeMision(mision(it)))
        }
    }

    @Test
    fun un_tipo_desconocido_o_ausente_no_lleva_a_ningun_lado() {
        assertNull(destinoDeMision(mision("VOLAR")))
        assertNull(destinoDeMision(mision(null)))
    }
}
