package com.plantia.data.repository

import com.plantia.data.mapper.toDomain
import com.plantia.data.model.LogroNuevo
import com.plantia.data.model.LogrosResumen
import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.dto.LogrosVistosRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface LogrosResultado {
    data class Con(val resumen: LogrosResumen) : LogrosResultado
    data class Fallo(val mensaje: String) : LogrosResultado
}

/** Logros del usuario: la lista con su avance, los que faltan por celebrar y el aviso de "ya los vio". */
interface LogrosRepository {

    suspend fun listar(): LogrosResultado

    /** Desbloqueados que todavia no se celebraron; `null` si no se pudo consultar (sin red, sin sesion...). */
    suspend fun pendientes(): List<LogroNuevo>?

    /** Marca como vistos los logros ya celebrados; `false` si fallo y hay que reintentar despues. */
    suspend fun marcarVistos(ids: List<Int>): Boolean
}

class RemoteLogrosRepository(
    private val api: () -> PlantiaApi = { RetrofitClient.api }
) : LogrosRepository {

    override suspend fun listar(): LogrosResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = api().logros()
            val cuerpo = respuesta.body()
            if (respuesta.isSuccessful && cuerpo != null) LogrosResultado.Con(cuerpo.toDomain())
            else LogrosResultado.Fallo("El servidor respondio ${respuesta.code()}")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogrosResultado.Fallo(e.message ?: SIN_CONEXION)
        }
    }

    override suspend fun pendientes(): List<LogroNuevo>? = withContext(Dispatchers.IO) {
        try {
            val respuesta = api().logrosPendientes()
            if (respuesta.isSuccessful) {
                respuesta.body().orEmpty().filter { it.idLogro > 0 }.map { it.toDomain() }
            } else null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun marcarVistos(ids: List<Int>): Boolean = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext true
        try {
            api().logrosVistos(LogrosVistosRequest(ids)).isSuccessful
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            false
        }
    }

    private companion object {
        const val SIN_CONEXION = "No se pudo conectar con el servidor"
    }
}
