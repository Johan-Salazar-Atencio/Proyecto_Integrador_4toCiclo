package com.plantia.screens.home.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.toDomain
import com.plantia.model.DailyMission
import com.plantia.model.PlantSummary
import com.plantia.model.QuickAccessItem
import com.plantia.model.UserSummary
import com.plantia.model.WeatherAlert
import kotlinx.coroutines.launch
import retrofit2.Response

/** Estado de la pantalla Home. Cada card se dibuja solo si tiene datos. */
data class HomeUiState(
    val cargando: Boolean = true,
    val usuario: UserSummary? = null,
    val alerta: WeatherAlert? = null,
    val planta: PlantSummary? = null,
    val accesos: List<QuickAccessItem> = emptyList(),
    val mision: DailyMission? = null,
    val error: String? = null
)

class HomeViewModel : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        cargar()
    }

    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, error = null)

            val api = RetrofitClient.api
            // Cada peticion es independiente: una que falle no debe tumbar el resto.
            val usuario = lanzar { api.usuarioMe(USER_ID) }
            val accesos = lanzar { api.accesos() }
            val alerta = lanzar { api.eventoActivo(USER_ID) }
            val planta = lanzar { api.plantaActiva(USER_ID) }
            val mision = lanzar { api.misionDiaria(USER_ID) }

            val estado = HomeUiState(
                cargando = false,
                usuario = usuario?.bodyOrNull()?.toDomain(),
                alerta = alerta?.bodyOrNull()?.toDomain(),
                planta = planta?.bodyOrNull()?.toDomain(),
                accesos = accesos?.bodyOrNull()?.map { it.toDomain() }.orEmpty(),
                mision = mision?.bodyOrNull()?.toDomain()
            )

            val fallos = listOf(usuario, accesos, alerta, planta, mision)
                .count { it == null || !it.isSuccessful }
            val todoCaido = listOf(usuario, accesos, alerta, planta, mision)
                .all { it == null }

            uiState = estado.copy(
                error = if (todoCaido) {
                    "No se pudo conectar con el servidor"
                } else if (fallos > 0) {
                    "Algunas secciones no se pudieron cargar"
                } else {
                    null
                }
            )
        }
    }

    /** Ejecuta la llamada y captura cualquier excepcion (red caida, timeout). */
    private suspend fun <T> lanzar(block: suspend () -> Response<T>): Response<T>? = try {
        block()
    } catch (e: Exception) {
        null
    }

    /** 204 (sin contenido) es un exito con cuerpo nulo. */
    private fun <T> Response<T>?.bodyOrNull(): T? =
        if (this != null && isSuccessful) body() else null

    companion object {
        /** Usuario temporal (sin auth) hasta integrar login. */
        const val USER_ID = 1
    }
}
