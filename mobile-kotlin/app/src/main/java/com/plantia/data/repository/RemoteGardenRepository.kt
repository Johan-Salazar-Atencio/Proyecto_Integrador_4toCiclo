package com.plantia.data.repository

import com.plantia.BuildConfig
import com.plantia.data.mapper.toDomain
import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.PlantSummary
import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.dto.ApodoRequest
import com.plantia.data.remote.dto.PlantSummaryDto
import com.plantia.data.remote.dto.PonerMacetaRequest
import com.plantia.util.normalizarUrlLocal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RemoteGardenRepository(
    private val api: () -> PlantiaApi = { RetrofitClient.api }
) : GardenRepository {

    override suspend fun plantaActiva(): PlantaActivaResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = api().plantaActiva()
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

    override suspend fun listar(): PlantasResultado = withContext(Dispatchers.IO) {
        try {
            val respuesta = api().plantas()
            if (respuesta.isSuccessful) {
                PlantasResultado.Con(respuesta.body().orEmpty().map { it.aPlanta() })
            } else {
                PlantasResultado.Fallo("El servidor respondio ${respuesta.code()}")
            }
        } catch (e: Exception) {
            PlantasResultado.Fallo(e.message ?: SIN_CONEXION)
        }
    }

    override suspend fun digitalizar(idEscaneo: Int, idCatalogo: Int?): PlantaResultado {
        val body = when {
            idEscaneo > 0 -> mapOf("idEscaneo" to idEscaneo)
            idCatalogo != null -> mapOf("idCatalogo" to idCatalogo)
            else -> return PlantaResultado.Fallo("Esta planta no se pudo identificar")
        }
        return planta { api().digitalizar(body) }
    }

    override suspend fun seleccionar(idPlanta: Int): PlantaResultado =
        planta { api().seleccionarPlanta(idPlanta) }

    override suspend fun cuidar(idPlanta: Int, accion: AccionCuidado): PlantaResultado =
        planta { api().cuidarPlanta(idPlanta, mapOf("accion" to accion.name)) }

    override suspend fun prevenir(idPlanta: Int): PlantaResultado =
        planta(errores = ERRORES_PREVENIR) { api().prevenirPlanta(idPlanta) }

    override suspend fun revivir(idPlanta: Int): PlantaResultado =
        planta(errores = ERRORES_REVIVIR) { api().revivirPlanta(idPlanta) }

    override suspend fun golpeVisto(idPlanta: Int): PlantaResultado =
        planta { api().golpeVisto(idPlanta) }

    override suspend fun guardarEnBaul(idPlanta: Int): PlantaResultado =
        planta(delServidor = true) { api().guardarEnBaul(idPlanta) }

    override suspend fun sacarDelBaul(idPlanta: Int): PlantaResultado =
        planta(delServidor = true) { api().sacarDelBaul(idPlanta) }

    override suspend fun renombrar(idPlanta: Int, apodo: String): PlantaResultado =
        planta(delServidor = true) { api().renombrarPlanta(idPlanta, ApodoRequest(apodo)) }

    override suspend fun ponerMaceta(idItem: Int, idPlantas: List<Int>): PlantasResultado =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = api().ponerMacetas(PonerMacetaRequest(idItem, idPlantas))
                val cuerpo = respuesta.body()
                if (respuesta.isSuccessful && cuerpo != null) PlantasResultado.Con(cuerpo.map { it.aPlanta() })
                else PlantasResultado.Fallo(respuesta.mensajeDelServidor())
            } catch (e: Exception) {
                PlantasResultado.Fallo(e.message ?: SIN_CONEXION)
            }
        }

    /**
     * [errores] traduce un codigo HTTP a un mensaje para el usuario; los demas dicen lo que respondio el servidor. */
    private suspend fun planta(
        errores: Map<Int, String> = emptyMap(),
        /** true: si falla se muestra el texto que manda Spring (apodo largo, jardin lleno...). */
        delServidor: Boolean = false,
        llamada: suspend () -> Response<PlantSummaryDto>
    ): PlantaResultado =
        withContext(Dispatchers.IO) {
            try {
                val respuesta = llamada()
                val cuerpo = respuesta.body()
                if (respuesta.isSuccessful && cuerpo != null) {
                    PlantaResultado.Con(cuerpo.aPlanta())
                } else {
                    PlantaResultado.Fallo(
                        errores[respuesta.code()]
                            ?: if (delServidor) respuesta.mensajeDelServidor() else "El servidor respondio ${respuesta.code()}"
                    )
                }
            } catch (e: Exception) {
                PlantaResultado.Fallo(e.message ?: SIN_CONEXION)
            }
        }

    /** Sin sprite de la etapa se muestra la foto del escaneo; las URLs se ajustan al host activo. */
    private fun PlantSummaryDto.aPlanta(): PlantSummary =
        toDomain().copy(imageUrl = urlAbsoluta(imageUrl) ?: urlAbsoluta(fotoUrl))

    /** El backend manda rutas relativas ("/sprites/..."); se completan con el host que responde. */
    private fun urlAbsoluta(ruta: String?): String? {
        if (ruta.isNullOrBlank()) return null
        if (ruta.startsWith("http")) return normalizarUrlLocal(ruta)
        val base = (RetrofitClient.baseUrl.value ?: BuildConfig.API_BASE_URL).trimEnd('/')
        return base + "/" + ruta.trimStart('/')
    }

    private companion object {
        const val SIN_CONEXION = "No se pudo conectar con el servidor"
        val ERRORES_PREVENIR = mapOf(
            409 to "Ya no hay un evento activo para prevenir",
            404 to "No se encontro la planta"
        )
        val ERRORES_REVIVIR = mapOf(
            402 to "No te alcanzan las hojas para revivirla",
            409 to "La planta no esta muerta",
            404 to "No se encontro la planta"
        )
    }
}
