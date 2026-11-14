package com.plantia.ui.home.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.local.Sesion
import com.plantia.data.model.DailyMission
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.QuickAccessItem
import com.plantia.data.model.UserSummary
import com.plantia.data.model.WeatherAlert
import com.plantia.data.repository.ColeccionResultado
import com.plantia.data.repository.CollectionRepository
import com.plantia.data.repository.HomeRepository
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

/** Estado de la pantalla Home. Cada card se dibuja solo si tiene datos. */
data class HomeUiState(
    val cargando: Boolean = true,
    val usuario: UserSummary? = null,
    val alerta: WeatherAlert? = null,
    val planta: PlantSummary? = null,
    val accesosServidor: List<QuickAccessItem> = emptyList(),
    val totalColeccion: Int? = null,
    val mision: DailyMission? = null,
    val error: String? = null
) {
    /** Accesos rapidos sin el Jardin (se entra desde la tarjeta de la planta) y con el total de la coleccion. */
    val accesos: List<QuickAccessItem>
        get() = accesosServidor
            .filterNot { it.id == "garden" || it.id == "jardin" }
            .map { item ->
                if (item.id != "collection" || totalColeccion == null) {
                    item
                } else {
                    item.copy(subtitle = subtituloColeccion(totalColeccion))
                }
            }

    private fun subtituloColeccion(total: Int): String = when (total) {
        0 -> "Aun sin plantas"
        1 -> "1 planta descubierta"
        else -> "$total plantas descubiertas"
    }
}

class HomeViewModel(
    private val home: HomeRepository,
    private val collection: CollectionRepository
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        // Si cambia el usuario de la sesion, se recarga todo el Home.
        viewModelScope.launch {
            Sesion.userId.collect { cargar() }
        }
    }

    fun recargar() = cargar()

    /** Actualiza solo el total de la coleccion (al volver al Home tras escanear o guardar). */
    fun refrescarColeccion() {
        viewModelScope.launch {
            uiState = uiState.copy(totalColeccion = totalColeccion())
        }
    }

    private fun cargar() {
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, error = null)
            val datos = home.cargar()
            uiState = HomeUiState(
                cargando = false,
                usuario = datos.usuario,
                alerta = datos.alerta,
                planta = datos.planta,
                accesosServidor = datos.accesos,
                totalColeccion = totalColeccion(),
                mision = datos.mision,
                error = when {
                    datos.sinConexion -> "No se pudo conectar con el servidor"
                    datos.fallidas > 0 -> "Algunas secciones no se pudieron cargar"
                    else -> null
                }
            )
        }
    }

    private suspend fun totalColeccion(): Int? =
        (collection.listar() as? ColeccionResultado.Con)?.plantas?.size

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(AppContainer.home, AppContainer.collection) }
        }
    }
}
