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
    val cuidados: List<CuidadoSeccionDto>? = null,
    /** false si la IA no reconocio una especie (QA-14); null en servidores viejos. */
    val identificado: Boolean? = null,
    /** XP y logros ganados con el escaneo; el servidor viejo no lo manda. */
    val progreso: ProgresoDto? = null
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
    val statusColor: String = "#10B981",
    /** La foto que se tomo o subio en ese escaneo (ruta del servidor). */
    val fotoUrl: String? = null
)
