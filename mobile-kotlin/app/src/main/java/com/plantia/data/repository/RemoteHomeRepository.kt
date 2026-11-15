package com.plantia.data.repository

import com.plantia.data.mapper.toDomain
import com.plantia.data.model.DailyMission
import com.plantia.data.model.Region
import com.plantia.data.model.UserSummary
import com.plantia.data.remote.PlantiaApi
import com.plantia.data.remote.dto.CambiarRegionRequest
import com.plantia.data.remote.RetrofitClient
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
        val misiones = intentar { servicio.misiones() }

        val respuestas = listOf(usuario, accesos, alerta, planta, misiones)
        HomeDatos(
            usuario = usuario.cuerpo()?.toDomain(),
            alerta = alerta.cuerpo()?.toDomain(),
            planta = planta.cuerpo()?.toDomain(),
            accesos = accesos.cuerpo()?.map { it.toDomain() }.orEmpty(),
            misiones = misiones.cuerpo()?.map { it.toDomain() }.orEmpty(),
            fallidas = respuestas.count { it == null || !it.isSuccessful },
            sinConexion = respuestas.all { it == null }
        )
    }

    override suspend fun alerta(): AlertaResultado = withContext(Dispatchers.IO) {
        val respuesta = intentar { api().eventoActivo() }
        val cuerpo = respuesta.cuerpo()
        when {
            respuesta == null || !respuesta.isSuccessful -> AlertaResultado.Fallo
            cuerpo == null -> AlertaResultado.Sin
            else -> AlertaResultado.Con(cuerpo.toDomain())
        }
    }

    override suspend fun misiones(): List<DailyMission>? = withContext(Dispatchers.IO) {
        intentar { api().misiones() }.cuerpo()?.map { it.toDomain() }
    }

    override suspend fun usuario(): UserSummary? = withContext(Dispatchers.IO) {
        intentar { api().usuarioMe() }.cuerpo()?.toDomain()
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

    override suspend fun regiones(): List<Region>? = withContext(Dispatchers.IO) {
        val respuesta = runCatching { api().regiones() }.getOrNull()
        if (respuesta != null && respuesta.isSuccessful) respuesta.body()?.map { it.toDomain() } else null
    }

    override suspend fun cambiarRegion(codigo: String): UserSummary? = withContext(Dispatchers.IO) {
        val respuesta = runCatching { api().cambiarRegion(CambiarRegionRequest(codigo)) }.getOrNull()
        if (respuesta != null && respuesta.isSuccessful) respuesta.body()?.toDomain() else null
    }
}
