package com.plantia.data

import com.plantia.BuildConfig
import com.plantia.data.remote.PlantSummaryDto
import com.plantia.data.remote.RetrofitClient
import com.plantia.model.PlantSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

sealed interface PlantaActivaResultado {
    data class ConPlanta(val planta: PlantSummary) : PlantaActivaResultado
    /** El usuario aun no tiene ninguna planta en crianza (HTTP 204). */
    data object SinPlanta : PlantaActivaResultado
    data class Fallo(val mensaje: String) : PlantaActivaResultado
}

sealed interface PlantaResultado {
    data class Con(val planta: PlantSummary) : PlantaResultado
    data class Fallo(val mensaje: String) : PlantaResultado
}

sealed interface PlantasResultado {
    data class Con(val plantas: List<PlantSummary>) : PlantasResultado
    data class Fallo(val mensaje: String) : PlantasResultado
}

/** Plantas digitales del usuario (Mi jardin): lectura, digitalizar, elegir y cuidar. */
object GardenRepository {

    private const val SIN_CONEXION = "No se pudo conectar con el servidor"

    suspend fun plantaActiva(): PlantaActivaResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = RetrofitClient.api.plantaActiva()
            val cuerpo = respuesta.body()
            when {
                respuesta.code() == 204 -> PlantaActivaResultado.SinPlanta
                respuesta.isSuccessful && cuerpo != null -> PlantaActivaResultado.ConPlanta(cuerpo.aPlanta())
                else -> PlantaActivaResultado.Fallo("El servidor respondio ${respuesta.code()}")
            }
        } catch (e: Exception) {
            PlantaActivaResultado.Fallo(e.message ?: SIN_CONEXION)
        }
    }

    suspend fun listar(): PlantasResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = RetrofitClient.api.plantas()
            if (respuesta.isSuccessful) {
                PlantasResultado.Con(respuesta.body().orEmpty().map { it.aPlanta() })
            } else {
                PlantasResultado.Fallo("El servidor respondio ${respuesta.code()}")
            }
        } catch (e: Exception) {
            PlantasResultado.Fallo(e.message ?: SIN_CONEXION)
        }
    }

    /** Digitaliza desde un escaneo propio (`idEscaneo` > 0) o desde una especie del catalogo. */
    suspend fun digitalizar(idEscaneo: Int, idCatalogo: Int?): PlantaResultado {
        val body = when {
            idEscaneo > 0 -> mapOf("idEscaneo" to idEscaneo)
            idCatalogo != null -> mapOf("idCatalogo" to idCatalogo)
            else -> return PlantaResultado.Fallo("Esta planta no se pudo identificar")
        }
        return planta { RetrofitClient.api.digitalizar(body) }
    }

    /** Elige cual de las plantas digitales se ve en el jardin. */
    suspend fun seleccionar(idPlanta: Int): PlantaResultado =
        planta { RetrofitClient.api.seleccionarPlanta(idPlanta) }

    suspend fun cuidar(idPlanta: Int, accion: AccionCuidado): PlantaResultado =
        planta { RetrofitClient.api.cuidarPlanta(idPlanta, mapOf("accion" to accion.name)) }

    private suspend fun planta(llamada: suspend () -> Response<PlantSummaryDto>): PlantaResultado =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = llamada()
                val cuerpo = respuesta.body()
                if (respuesta.isSuccessful && cuerpo != null) {
                    PlantaResultado.Con(cuerpo.aPlanta())
                } else {
                    PlantaResultado.Fallo("El servidor respondio ${respuesta.code()}")
                }
            } catch (e: Exception) {
                PlantaResultado.Fallo(e.message ?: SIN_CONEXION)
            }
        }

    /** Sin sprite de la etapa se muestra la foto del escaneo; las URLs se ajustan al host activo. */
    private fun PlantSummaryDto.aPlanta(): PlantSummary {
        val base = toDomain()
        return base.copy(imageUrl = urlAbsoluta(imageUrl) ?: urlAbsoluta(fotoUrl))
    }

    /** El backend manda rutas relativas ("/sprites/..."); se completan con el host que responde. */
    private fun urlAbsoluta(ruta: String?): String? {
        if (ruta.isNullOrBlank()) return null
        if (ruta.startsWith("http")) return normalizarUrlLocal(ruta)
        val base = (RetrofitClient.baseUrl.value ?: BuildConfig.API_BASE_URL).trimEnd('/')
        return base + "/" + ruta.trimStart('/')
    }
}
