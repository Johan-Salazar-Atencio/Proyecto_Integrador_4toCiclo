package com.plantia.ui.detail

import com.plantia.data.remote.dto.CuidadoSeccionDto
import com.plantia.data.remote.dto.ScanResultDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantDetailMappingTest {

    private fun cuidado(tipo: String, contenido: String?, orden: Int = 0) =
        CuidadoSeccionDto(id = orden, tipo = tipo, titulo = tipo, contenido = contenido, orden = orden)

    @Test
    fun detecta_identificaciones_fallidas() {
        assertTrue(ScanResultDto(nombreComun = null).identificacionFallida())
        assertTrue(ScanResultDto(nombreComun = " Planta desconocida ").identificacionFallida())
        assertTrue(ScanResultDto(nombreComun = "No identificado (imagen sin planta)").identificacionFallida())
        assertFalse(ScanResultDto(nombreComun = "Monstera").identificacionFallida())
    }

    @Test
    fun las_tarjetas_van_en_orden_fijo_y_sin_advertencias() {
        val escaneo = ScanResultDto(
            cuidados = listOf(
                cuidado("ABONO", "Cada mes", 0),
                cuidado("TOXICIDAD", "Toxica para mascotas", 1),
                cuidado("LUZ", "Luz indirecta", 2),
                cuidado("RIEGO", "Moderado", 3),
                cuidado("PODA", null, 4)
            )
        )
        assertEquals(listOf("RIEGO", "LUZ", "ABONO"), escaneo.tarjetasCuidado().map { it.tipo })
    }

    @Test
    fun el_diagnostico_une_plagas_toxicidad_y_otros() {
        val escaneo = ScanResultDto(
            cuidados = listOf(
                cuidado("TOXICIDAD", " Toxica ", 1),
                cuidado("PLAGAS", "Revisar cochinilla", 0),
                cuidado("RIEGO", "Moderado", 2)
            )
        )
        assertEquals("Revisar cochinilla\n\nToxica", escaneo.textoDiagnostico())
        assertNull(ScanResultDto(cuidados = listOf(cuidado("RIEGO", "Moderado"))).textoDiagnostico())
    }

    @Test
    fun la_precision_se_redondea_y_se_oculta_si_es_cero() {
        assertEquals("IA Identificado 94% Precisión", ScanResultDto(confianza = 93.6).etiquetaPrecision())
        assertNull(ScanResultDto(confianza = 0.0).etiquetaPrecision())
        assertNull(ScanResultDto(confianza = null).etiquetaPrecision())
    }
}
