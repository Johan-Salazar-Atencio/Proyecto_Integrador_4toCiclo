package com.plantia.data

import com.plantia.data.remote.RetrofitClient
import com.plantia.model.PlantSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface PlantaActivaResultado {
    data class ConPlanta(val planta: PlantSummary) : PlantaActivaResultado
    /** El usuario aun no tiene ninguna planta en crianza (HTTP 204). */
    data object SinPlanta : PlantaActivaResultado
    data class Fallo(val mensaje: String) : PlantaActivaResultado
}

/** Acceso a los datos del jardin: lectura real (Spring) + cuidados simulados. */
object GardenRepository {

    private const val USER_ID = 1

    suspend fun plantaActiva(): PlantaActivaResultado = withContext(Dispatchers.IO) {
        // Si el usuario digitalizo una planta (simulado), esa es la que se muestra.
        MockGardenData.plantaDigital?.let {
            return@withContext PlantaActivaResultado.ConPlanta(MockGardenData.aplicarAjustes(it))
        }
        try {
            // `RetrofitClient.api` puede sondear la red la primera vez: siempre fuera del hilo principal.
            val respuesta = RetrofitClient.api.plantaActiva(USER_ID)
            val cuerpo = respuesta.body()
            when {
                respuesta.code() == 204 -> PlantaActivaResultado.SinPlanta
                respuesta.isSuccessful && cuerpo != null -> {
                    val conUrl = cuerpo.copy(imageUrl = urlAbsoluta(cuerpo.imageUrl))
                    PlantaActivaResultado.ConPlanta(MockGardenData.aplicarAjustes(conUrl))
                }
                else -> PlantaActivaResultado.Fallo("El servidor respondio ${respuesta.code()}")
            }
        } catch (e: Exception) {
            PlantaActivaResultado.Fallo(e.message ?: "No se pudo conectar con el servidor")
        }
    }

    /** El backend manda rutas relativas ("/sprites/..."); se completan con el host que responde. */
    private fun urlAbsoluta(ruta: String?): String? {
        if (ruta.isNullOrBlank()) return null
        if (ruta.startsWith("http")) return ruta
        return RetrofitClient.baseUrlEfectiva.trimEnd('/') + "/" + ruta.trimStart('/')
    }
}
