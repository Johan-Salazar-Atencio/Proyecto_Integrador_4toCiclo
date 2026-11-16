package com.plantia.ui.misiones.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.DailyMission
import com.plantia.data.repository.HomeRepository
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

data class MisionesUiState(
    val cargando: Boolean = true,
    val misiones: List<DailyMission> = emptyList(),
    val error: String? = null
) {
    /** La mision del evento activo (tarjeta roja), si hay uno. */
    val evento: DailyMission? get() = misiones.firstOrNull { it.esEvento }

    val diarias: List<DailyMission> get() = misiones.filterNot { it.esEvento }

    val completadas: Int get() = diarias.count { it.isCompleted }
}

/** Todas las misiones de hoy; la del evento va aparte, en rojo. Cada una lleva a donde se cumple. */
class MisionesViewModel(private val home: HomeRepository) : ViewModel() {

    var uiState by mutableStateOf(MisionesUiState())
        private set

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, error = null)
            val misiones = home.misiones()
            // Un fallo de red conserva lo que ya se veia
            uiState = MisionesUiState(
                cargando = false,
                misiones = misiones ?: uiState.misiones,
                error = if (misiones == null) "No se pudieron cargar las misiones" else null
            )
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { MisionesViewModel(AppContainer.home) } }
    }
}
