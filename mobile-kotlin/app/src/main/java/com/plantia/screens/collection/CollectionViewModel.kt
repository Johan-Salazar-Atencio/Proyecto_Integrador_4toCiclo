package com.plantia.screens.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.CollectionRepository
import com.plantia.data.ColeccionResultado
import com.plantia.data.GardenRepository
import com.plantia.data.PlantaResultado
import com.plantia.data.PlantasResultado
import com.plantia.data.remote.ColeccionItemDto
import com.plantia.model.PlantSummary
import kotlinx.coroutines.launch

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    /** Las listas pueden venir vacias: es el estado inicial de un usuario nuevo. */
    data class Success(
        val escaneadas: List<ColeccionItemDto>,
        val digitales: List<PlantSummary>
    ) : CollectionUiState
    data class Error(val mensaje: String) : CollectionUiState
}

class CollectionViewModel : ViewModel() {

    var uiState: CollectionUiState by mutableStateOf(CollectionUiState.Loading)
        private set

    /** Mensaje de un fallo puntual (por ejemplo al elegir una planta); la pantalla lo muestra y lo limpia. */
    var aviso: String? by mutableStateOf(null)
        private set

    init {
        cargar()
    }

    fun cargar() {
        uiState = CollectionUiState.Loading
        viewModelScope.launch {
            uiState = when (val r = CollectionRepository.listar()) {
                is ColeccionResultado.Con -> CollectionUiState.Success(
                    escaneadas = r.plantas,
                    // Si fallan las digitales se muestra la pestana vacia, no se tumba la coleccion.
                    digitales = (GardenRepository.listar() as? PlantasResultado.Con)?.plantas.orEmpty()
                )
                is ColeccionResultado.Fallo -> CollectionUiState.Error(r.mensaje)
            }
        }
    }

    /** Elige la planta del jardin y, si el servidor lo confirma, avisa para abrirlo. */
    fun seleccionar(planta: PlantSummary, alTerminar: () -> Unit) {
        val id = planta.id ?: return
        viewModelScope.launch {
            when (val r = GardenRepository.seleccionar(id)) {
                is PlantaResultado.Con -> alTerminar()
                is PlantaResultado.Fallo -> aviso = "No se pudo elegir la planta: ${r.mensaje}"
            }
        }
    }

    fun avisoMostrado() {
        aviso = null
    }
}
