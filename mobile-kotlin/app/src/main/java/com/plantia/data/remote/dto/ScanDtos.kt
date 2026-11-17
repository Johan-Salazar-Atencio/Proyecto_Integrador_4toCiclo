package com.plantia.data.remote.dto

/** Contrato de respuesta de Spring Boot: POST /api/v1/escaneos */
data class ScanResultDto(
    val idEscaneo: Int = 0,
    val idCatalogo: Int? = null,
    val nombreComun: String? = null,
    val nombreCientifico: String? = null,
    val confianza: Double? = null,
    val imagenUrl: String? = null,
    val nuevoEnColeccion: Boolean = false,
    val cuidados: List<CuidadoSeccionDto>? = null
)

data class CuidadoSeccionDto(
    val id: Int = 0,
    val tipo: String? = null,
    val titulo: String? = null,
    val contenido: String? = null,
    val orden: Int = 0,
    val generadoPorIa: Boolean = false
)

/** GET /api/v1/escaneos/recientes */
data class RecentScanDto(
    val id: String = "",
    val plantName: String = "",
    val confidence: String = "",
    val statusColor: String = "#10B981"
)
