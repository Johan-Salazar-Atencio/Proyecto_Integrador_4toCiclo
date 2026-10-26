package com.plantia.screens.collection

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.CollectionRepository
import com.plantia.data.ColeccionResultado
import com.plantia.data.remote.ColeccionItemDto
import kotlinx.coroutines.launch

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    /** `escaneadas` puede venir vacia: es el estado inicial de un usuario nuevo. */
    data class Success(val escaneadas: List<ColeccionItemDto>) : CollectionUiState
    data class Error(val mensaje: String) : CollectionUiState
}

class CollectionViewModel : ViewModel() {

    var uiState: CollectionUiState by mutableStateOf(CollectionUiState.Loading)
        private set

    init {
        cargar()
    }

    fun cargar() {
        uiState = CollectionUiState.Loading
        viewModelScope.launch {
            uiState = when (val r = CollectionRepository.listar()) {
                is ColeccionResultado.Con -> CollectionUiState.Success(r.plantas)
                is ColeccionResultado.Fallo -> CollectionUiState.Error(r.mensaje)
            }
        }
    }
}
