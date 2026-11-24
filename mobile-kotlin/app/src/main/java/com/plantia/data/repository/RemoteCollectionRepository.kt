package com.plantia.data.repository

import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.dto.ColeccionItemDto
import com.plantia.data.remote.dto.ScanResultDto
import com.plantia.util.normalizarUrlLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer

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
                .ordenadasPorNombreComun()
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

private val MARCAS_DE_TILDE = Regex("\\p{Mn}+")

/** Clave para ordenar nombres: sin tildes y sin distinguir mayusculas ("Álamo" va junto a "alamo"). */
internal fun claveDeNombre(nombre: String): String =
    MARCAS_DE_TILDE.replace(Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD), "").lowercase()

/** La coleccion se lee por nombre comun (A-Z, sin tildes ni mayusculas); a igual nombre queda el orden que traia (estable). */
internal fun List<ColeccionItemDto>.ordenadasPorNombreComun(): List<ColeccionItemDto> =
    sortedWith(compareBy { claveDeNombre(it.nombreComun) })
