package com.plantia.data.repository

import com.plantia.BuildConfig
import com.plantia.data.mapper.toDomain
import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.PlantSummary
import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.dto.PlantSummaryDto
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
    }
}
