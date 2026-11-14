package com.plantia.screens.garden

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.AccionCuidado
import com.plantia.data.GardenRepository
import com.plantia.data.PlantaActivaResultado
import com.plantia.data.PlantaResultado
import com.plantia.di.AppContainer
import com.plantia.model.PlantSummary
import kotlinx.coroutines.launch

sealed interface GardenUiState {
    data object Loading : GardenUiState
    data class Success(val planta: PlantSummary) : GardenUiState
    data object SinPlanta : GardenUiState
    data class Error(val mensaje: String) : GardenUiState
}

class GardenViewModel(private val garden: GardenRepository) : ViewModel() {

    var uiState: GardenUiState by mutableStateOf(GardenUiState.Loading)
        private set

    private var cuidando = false

    init {
        cargar()
    }

    fun cargar() {
        uiState = GardenUiState.Loading
        viewModelScope.launch {
            uiState = when (val r = garden.plantaActiva()) {
                is PlantaActivaResultado.ConPlanta -> GardenUiState.Success(r.planta)
                PlantaActivaResultado.SinPlanta -> GardenUiState.SinPlanta
                is PlantaActivaResultado.Fallo -> GardenUiState.Error(r.mensaje)
            }
        }
    }

    /** Aplica un cuidado en el backend y entrega el mensaje para el usuario. */
    fun cuidar(accion: AccionCuidado, alTerminar: (String) -> Unit) {
        val actual = (uiState as? GardenUiState.Success)?.planta ?: return
        val id = actual.id ?: return
        if (cuidando) return

        valorCompleto(actual, accion)?.let { return alTerminar(it) }

        cuidando = true
        viewModelScope.launch {
            when (val r = garden.cuidar(id, accion)) {
                is PlantaResultado.Con -> {
                    uiState = GardenUiState.Success(r.planta)
                    alTerminar(mensajeDeExito(actual, r.planta, accion))
                }
                is PlantaResultado.Fallo -> alTerminar("No se pudo aplicar el cuidado: ${r.mensaje}")
            }
            cuidando = false
        }
    }

    /** Aviso si esa necesidad ya esta al 100 % (no hace falta llamar al servidor). */
    private fun valorCompleto(p: PlantSummary, accion: AccionCuidado): String? = when (accion) {
        AccionCuidado.REGAR -> "${p.name} ya tiene toda el agua que necesita".takeIf { p.humidityPercent >= 100 }
        AccionCuidado.SOL -> "${p.name} ya recibio toda la luz que necesita".takeIf { p.lightPercent >= 100 }
        AccionCuidado.ABONAR -> "${p.name} ya esta en plena salud".takeIf { p.healthPercent >= 100 }
    }

    private fun mensajeDeExito(antes: PlantSummary, despues: PlantSummary, accion: AccionCuidado): String =
        when (accion) {
            AccionCuidado.REGAR -> "Regaste a ${antes.name}: +${despues.humidityPercent - antes.humidityPercent}% de agua"
            AccionCuidado.SOL -> "${antes.name} tomo sol: +${despues.lightPercent - antes.lightPercent}% de luz"
            AccionCuidado.ABONAR -> "Abonaste a ${antes.name}: +${despues.healthPercent - antes.healthPercent}% de salud"
        }

    companion object {
        val Factory = viewModelFactory { initializer { GardenViewModel(AppContainer.garden) } }
    }
}
