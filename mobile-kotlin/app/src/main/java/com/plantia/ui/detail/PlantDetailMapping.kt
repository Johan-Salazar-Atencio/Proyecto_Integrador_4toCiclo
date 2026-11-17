package com.plantia.ui.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.plantia.data.remote.dto.CuidadoSeccionDto
import com.plantia.data.remote.dto.ScanResultDto
import kotlin.math.roundToInt

private const val NOMBRE_DESCONOCIDA = "Planta desconocida"

/** Tipos de `CuidadoSeccionDto.tipo` que alimentan el diagnostico en vez de las tarjetas. */
private val TIPOS_DIAGNOSTICO = setOf("PLAGAS", "TOXICIDAD", "OTRO")

/** Orden fijo de las tarjetas: lo basico primero, sin importar el orden que devuelva la IA. */
private val PRIORIDAD_TIPO = mapOf("RIEGO" to 0, "LUZ" to 1, "TEMPERATURA" to 2, "SUSTRATO" to 3, "PODA" to 4, "ABONO" to 5)

/**
 * Spring responde "Planta desconocida" cuando el motor IA no responde; Gemini, por su parte,
 * puede contestar "No identificado (imagen sin planta)" con confianza 0.
 */
internal fun ScanResultDto.identificacionFallida(): Boolean =
    nombreComun.isNullOrBlank() ||
        nombreComun.trim().equals(NOMBRE_DESCONOCIDA, ignoreCase = true) ||
        nombreComun.trim().startsWith("No identificado", ignoreCase = true)

/** Tarjetas de cuidado: todo lo que no es advertencia, en el orden del servidor. */
internal fun ScanResultDto.tarjetasCuidado(): List<CuidadoSeccionDto> =
    cuidados.orEmpty()
        .filter { it.tipo?.uppercase() !in TIPOS_DIAGNOSTICO && !it.contenido.isNullOrBlank() }
        .sortedWith(compareBy({ PRIORIDAD_TIPO[it.tipo?.uppercase()] ?: Int.MAX_VALUE }, { it.orden }))

/** Texto del diagnostico: contenido de PLAGAS/TOXICIDAD/OTRO concatenado; null si no hay. */
internal fun ScanResultDto.textoDiagnostico(): String? =
    cuidados.orEmpty()
        .filter { it.tipo?.uppercase() in TIPOS_DIAGNOSTICO }
        .sortedBy { it.orden }
        .mapNotNull { it.contenido?.trim()?.takeIf(String::isNotEmpty) }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(separator = "\n\n")

/** `confianza` llega en escala 0..100. */
internal fun ScanResultDto.etiquetaPrecision(): String? =
    confianza?.takeIf { it > 0.0 }?.let {
        "IA Identificado ${it.coerceIn(0.0, 100.0).roundToInt()}% Precisión"
    }
