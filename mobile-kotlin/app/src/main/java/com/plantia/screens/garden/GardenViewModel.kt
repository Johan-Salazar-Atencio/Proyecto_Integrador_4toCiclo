package com.plantia.screens.garden

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.AccionCuidado
import com.plantia.data.GardenRepository
import com.plantia.data.MockGardenData
import com.plantia.data.PlantaActivaResultado
import com.plantia.model.PlantSummary
import kotlinx.coroutines.launch

sealed interface GardenUiState {
    data object Loading : GardenUiState
    data class Success(val planta: PlantSummary) : GardenUiState
    data object SinPlanta : GardenUiState
    data class Error(val mensaje: String) : GardenUiState
}

class GardenViewModel : ViewModel() {

    var uiState: GardenUiState by mutableStateOf(GardenUiState.Loading)
        private set

    init {
        cargar()
    }

    fun cargar() {
        uiState = GardenUiState.Loading
        viewModelScope.launch {
            uiState = when (val r = GardenRepository.plantaActiva()) {
                is PlantaActivaResultado.ConPlanta -> GardenUiState.Success(r.planta)
                PlantaActivaResultado.SinPlanta -> GardenUiState.SinPlanta
                is PlantaActivaResultado.Fallo -> GardenUiState.Error(r.mensaje)
            }
        }
    }

    /** Aplica un cuidado (simulado por ahora) y devuelve el mensaje para el usuario. */
    fun cuidar(accion: AccionCuidado): String {
        val actual = (uiState as? GardenUiState.Success)?.planta ?: return ""
        val resultado = MockGardenData.aplicarCuidado(actual, accion)
        uiState = GardenUiState.Success(resultado.planta)
        return resultado.mensaje
    }
}
