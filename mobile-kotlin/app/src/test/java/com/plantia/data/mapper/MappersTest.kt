package com.plantia.data.mapper

import com.plantia.data.remote.dto.DailyMissionDto
import com.plantia.data.remote.dto.RegionDto
import com.plantia.data.remote.dto.UserSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersTest {

    @Test
    fun el_resumen_del_usuario_lleva_su_region() {
        assertEquals("AREQUIPA", UserSummaryDto(name = "Johan", regionCode = "AREQUIPA").toDomain().regionCode)
    }

    @Test
    fun un_servidor_antiguo_sin_region_da_codigo_vacio() {
        assertEquals("", UserSummaryDto(name = "Johan").toDomain().regionCode)
    }

    @Test
    fun la_region_conserva_codigo_y_nombre() {
        val region = RegionDto(code = "LIMA", name = "Lima Metropolitana").toDomain()

        assertEquals("LIMA", region.code)
        assertEquals("Lima Metropolitana", region.name)
    }

    @Test
    fun el_avance_de_una_mision_nunca_pasa_de_100() {
        val mision = DailyMissionDto(id = "m", progress = 1.7f, currentCount = 3, targetCount = 2, isCompleted = true).toDomain()

        assertEquals(1f, mision.progress, 0f)
        assertTrue(mision.isCompleted)
    }
}
