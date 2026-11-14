package com.plantia.data

import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.RetrofitClient
import com.plantia.model.UserSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class RemoteHomeRepository(
    private val api: () -> PlantiaApi = { RetrofitClient.api }
) : HomeRepository {

    override suspend fun cargar(): HomeDatos = withContext(Dispatchers.IO) {
        val servicio = api()
        // Cada peticion es independiente: una que falle no debe tumbar el resto.
        val usuario = intentar { servicio.usuarioMe() }
        val accesos = intentar { servicio.accesos() }
        val alerta = intentar { servicio.eventoActivo() }
        val planta = intentar { servicio.plantaActiva() }
        val mision = intentar { servicio.misionDiaria() }

        val respuestas = listOf(usuario, accesos, alerta, planta, mision)
        HomeDatos(
            usuario = usuario.cuerpo()?.toDomain(),
            alerta = alerta.cuerpo()?.toDomain(),
            planta = planta.cuerpo()?.toDomain(),
            accesos = accesos.cuerpo()?.map { it.toDomain() }.orEmpty(),
            mision = mision.cuerpo()?.toDomain(),
            fallidas = respuestas.count { it == null || !it.isSuccessful },
            sinConexion = respuestas.all { it == null }
        )
    }

    /** Ejecuta la llamada y captura cualquier excepcion (red caida, timeout). */
    private suspend fun <T> intentar(llamada: suspend () -> Response<T>): Response<T>? = try {
        llamada()
    } catch (e: Exception) {
        null
    }

    /** 204 (sin contenido) es un exito con cuerpo nulo. */
    private fun <T> Response<T>?.cuerpo(): T? = if (this != null && isSuccessful) body() else null
}

class RemoteUsuarioRepository(
    private val api: () -> PlantiaApi = { RetrofitClient.api }
) : UsuarioRepository {

    override suspend fun perfil(): UserSummary? = withContext(Dispatchers.IO) {
        api().usuarioMe().body()?.toDomain()
    }
}
