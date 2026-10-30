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
 * El backend arma algunas URLs con `localhost` (fotos de escaneos). Se cambia el host por el que
 * realmente responde para que las imagenes tambien carguen por WiFi.
 */
fun normalizarUrlLocal(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return RetrofitClient.urlAbsoluta(url) ?: url
}

/** Coleccion de plantas escaneadas (datos reales de Spring). */
object CollectionRepository {

    suspend fun listar(): ColeccionResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = RetrofitClient.api.coleccion()
            if (!respuesta.isSuccessful) {
                return@withContext ColeccionResultado.Fallo("El servidor respondio ${respuesta.code()}")
            }
            val plantas = respuesta.body().orEmpty()
                // Se descartan registros sin nombre o resultados fallidos de la IA.
                .filter { it.nombreComun.isNotBlank() && !it.nombreComun.startsWith("No identificado", ignoreCase = true) }
                .map { it.copy(fotoUrl = normalizarUrlLocal(it.fotoUrl)) }
            ColeccionResultado.Con(plantas)
        } catch (e: Exception) {
            ColeccionResultado.Fallo(e.message ?: "No se pudo conectar con el servidor")
        }
    }

    /** Detalle de una planta guardada, con la misma forma que un escaneo (sin porcentaje de IA). */
    suspend fun detalle(id: Int): DetalleColeccionResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = RetrofitClient.api.coleccionDetalle(id)
            val detalle = respuesta.body()
            if (!respuesta.isSuccessful || detalle == null) {
                return@withContext DetalleColeccionResultado.Fallo("Planta no encontrada")
            }
            val item = detalle.item
            DetalleColeccionResultado.Con(
                ScanResultDto(
                    idEscaneo = 0,
                    idCatalogo = item.idCatalogo,
                    nombreComun = item.nombreComun,
                    nombreCientifico = item.nombreCientifico,
                    confianza = null,
                    imagenUrl = normalizarUrlLocal(item.fotoUrl),
                    nuevoEnColeccion = false,
                    cuidados = detalle.cuidados
                )
            )
        } catch (e: Exception) {
            DetalleColeccionResultado.Fallo(e.message ?: "No se pudo conectar con el servidor")
        }
    }
}
