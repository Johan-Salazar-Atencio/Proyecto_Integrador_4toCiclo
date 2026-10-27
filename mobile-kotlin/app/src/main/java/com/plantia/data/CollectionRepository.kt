package com.plantia.data

import com.plantia.data.remote.ColeccionItemDto
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.ScanResultDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface ColeccionResultado {
    data class Con(val plantas: List<ColeccionItemDto>) : ColeccionResultado
    data class Fallo(val mensaje: String) : ColeccionResultado
}

sealed interface DetalleColeccionResultado {
    data class Con(val planta: ScanResultDto) : DetalleColeccionResultado
    data class Fallo(val mensaje: String) : DetalleColeccionResultado
}

/**
 * Convierte URLs que el backend arma con `localhost` (p. ej. fotos de escaneos) al host que
 * realmente responde, para que las imagenes tambien carguen por WiFi. Llamar fuera del hilo
 * principal: la primera vez `baseUrlEfectiva` sondea la red.
 */
fun normalizarUrlLocal(url: String?): String? {
    if (url.isNullOrBlank()) return null
    val prefijo = listOf("http://localhost:8080", "http://127.0.0.1:8080").firstOrNull { url.startsWith(it) }
        ?: return url
    return RetrofitClient.baseUrlEfectiva.trimEnd('/') + url.removePrefix(prefijo)
}

/** Coleccion de plantas escaneadas (datos reales de Spring). */
object CollectionRepository {

    private const val USER_ID = 1

    suspend fun listar(): ColeccionResultado = withContext(Dispatchers.IO) {
        try {
            val plantas = RetrofitClient.api.coleccion(USER_ID)
                // Se descartan registros sin nombre o resultados fallidos de la IA.
                .filter { !it.nombreComun.isNullOrBlank() && !it.nombreComun.startsWith("No identificado", ignoreCase = true) }
                .map { it.copy(fotoUrl = normalizarUrlLocal(it.fotoUrl)) }
            ColeccionResultado.Con(plantas)
        } catch (e: Exception) {
            ColeccionResultado.Fallo(e.message ?: "No se pudo conectar con el servidor")
        }
    }

    /** Detalle de una planta guardada, con la misma forma que un escaneo (sin porcentaje de IA). */
    suspend fun detalle(id: Int): DetalleColeccionResultado = withContext(Dispatchers.IO) {
        try {
            val d = RetrofitClient.api.coleccionDetalle(id)
            val item = d.item ?: return@withContext DetalleColeccionResultado.Fallo("Planta no encontrada")
            DetalleColeccionResultado.Con(
                ScanResultDto(
                    idEscaneo = 0,
                    idCatalogo = item.idCatalogo,
                    nombreComun = item.nombreComun,
                    nombreCientifico = item.nombreCientifico,
                    confianza = null,
                    imagenUrl = normalizarUrlLocal(item.fotoUrl),
                    nuevoEnColeccion = false,
                    cuidados = d.cuidados.orEmpty()
                )
            )
        } catch (e: Exception) {
            DetalleColeccionResultado.Fallo(e.message ?: "No se pudo conectar con el servidor")
        }
    }
}
