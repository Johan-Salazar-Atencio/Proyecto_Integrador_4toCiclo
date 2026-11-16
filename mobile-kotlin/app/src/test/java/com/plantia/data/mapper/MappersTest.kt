package com.plantia.data.mapper

import com.plantia.data.remote.dto.DailyMissionDto
import com.plantia.data.remote.dto.PlantSummaryDto
import com.plantia.data.remote.dto.WeatherAlertDto
import com.plantia.data.remote.dto.RegionDto
import com.plantia.data.remote.dto.UserSummaryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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

    @Test
    fun la_mision_de_evento_trae_su_tipo_y_su_cuenta_regresiva() {
        val mision = DailyMissionDto(
            id = "event_9", title = "Prevenir ola de calor", kind = "EVENT", eventType = "OLA_DE_CALOR", endsInSeconds = 7323
        ).toDomain()

        assertTrue(mision.esEvento)
        assertEquals("OLA_DE_CALOR", mision.eventType)
        assertEquals(7323L, mision.endsInSeconds)
    }

    @Test
    fun una_mision_diaria_normal_no_es_de_evento() {
        val mision = DailyMissionDto(id = "mission_1", title = "Riego").toDomain()

        assertFalse(mision.esEvento)
        assertNull(mision.endsInSeconds)
    }

    @Test
    fun la_alerta_lleva_imagen_texto_consejo_y_segundos() {
        val alerta = WeatherAlertDto(
            title = "Ola de Calor Inminente", disasterType = "OLA_DE_CALOR", eventName = "Ola de calor",
            imageUrl = "/files/eventos/ola.jpg", info = "Texto", tip = "Consejo", endsInSeconds = 3600
        ).toDomain()

        assertEquals("Ola de calor", alerta.eventName)
        assertEquals("/files/eventos/ola.jpg", alerta.imageUrl)
        assertEquals("Texto", alerta.info)
        assertEquals("Consejo", alerta.tip)
        assertEquals(3600L, alerta.endsInSeconds)
    }

    @Test
    fun una_alerta_de_un_servidor_antiguo_no_trae_datos_extra() {
        val alerta = WeatherAlertDto(title = "Helada").toDomain()

        assertNull(alerta.imageUrl)
        assertNull(alerta.info)
        assertNull(alerta.endsInSeconds)
    }

    @Test
    fun una_planta_muerta_se_reconoce_por_su_fase() {
        assertTrue(PlantSummaryDto(name = "Monstera", fase = "MUERTA").toDomain().muerta)
        assertFalse(PlantSummaryDto(name = "Monstera", fase = "BROTE").toDomain().muerta)
        assertFalse(PlantSummaryDto(name = "Monstera").toDomain().muerta)
    }

    @Test
    fun la_recuperacion_de_una_planta_golpeada_se_conserva() {
        val planta = PlantSummaryDto(
            name = "Monstera", fase = "BROTE", enRecuperacion = true, recuperacionSegundos = 86000, golpeTipo = "OLA_DE_CALOR"
        ).toDomain()

        assertTrue(planta.enRecuperacion)
        assertEquals(86000L, planta.recuperacionSegundos)
        assertEquals("OLA_DE_CALOR", planta.golpeTipo)
    }

    @Test
    fun la_mision_diaria_trae_su_tipo() {
        assertEquals("ESCANEO", DailyMissionDto(id = "mission_2", title = "Explorador", type = "ESCANEO").toDomain().type)
    }
}
