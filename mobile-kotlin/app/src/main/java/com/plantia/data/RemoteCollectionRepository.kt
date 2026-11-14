package com.plantia.data

import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.ScanResultDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * El backend arma algunas URLs con `localhost` (fotos de escaneos). Se cambia el host por el que
 * realmente responde para que las imagenes tambien carguen por WiFi.
 */
fun normalizarUrlLocal(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return RetrofitClient.urlAbsoluta(url) ?: url
}

class RemoteCollectionRepository(
    private val api: () -> PlantiaApi = { RetrofitClient.api }
) : CollectionRepository {

    override suspend fun listar(): ColeccionResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = api().coleccion()
            if (!respuesta.isSuccessful) {
                return@withContext ColeccionResultado.Fallo("El servidor respondio ${respuesta.code()}")
            }
            val plantas = respuesta.body().orEmpty()
                // Se descartan registros sin nombre o resultados fallidos de la IA.
                .filter { it.nombreComun.isNotBlank() && !it.nombreComun.startsWith("No identificado", ignoreCase = true) }
                .map { it.copy(fotoUrl = normalizarUrlLocal(it.fotoUrl)) }
            ColeccionResultado.Con(plantas)
        } catch (e: Exception) {
            ColeccionResultado.Fallo(e.message ?: SIN_CONEXION)
        }
    }

    override suspend fun agregar(idEscaneo: Int): ColeccionResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = api().agregarColeccion(mapOf("idEscaneo" to idEscaneo))
            if (!respuesta.isSuccessful) {
                return@withContext ColeccionResultado.Fallo("El servidor respondio ${respuesta.code()}")
            }
            ColeccionResultado.Con(respuesta.body()?.let { listOf(it) } ?: emptyList())
        } catch (e: Exception) {
            ColeccionResultado.Fallo(e.message ?: SIN_CONEXION)
        }
    }

    override suspend fun detalle(id: Int): DetalleColeccionResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = api().coleccionDetalle(id)
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
            DetalleColeccionResultado.Fallo(e.message ?: SIN_CONEXION)
        }
    }

    private companion object {
        const val SIN_CONEXION = "No se pudo conectar con el servidor"
    }
}
