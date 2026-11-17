package com.plantia.ui.scan.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.RecentScan
import com.plantia.data.remote.dto.ScanResultDto
import com.plantia.data.repository.ScanRepository
import com.plantia.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Loading : ScanUiState
    data class Success(val result: ScanResultDto) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScanViewModel(private val scan: ScanRepository) : ViewModel() {

    var uiState: ScanUiState by mutableStateOf(ScanUiState.Idle)
        private set

    private var trabajo: Job? = null

    /** Escaneos recientes del usuario. */
    var recientes: List<RecentScan> by mutableStateOf(emptyList())
        private set

    init {
        cargarRecientes()
    }

    /** Captura tomada -> se sube al backend y se espera la informacion de la planta. */
    fun escanear(foto: File) {
        if (!foto.exists() || foto.length() == 0L) {
            uiState = ScanUiState.Error("La foto no se guardo correctamente")
            return
        }
        uiState = ScanUiState.Loading
        trabajo?.cancel()
        trabajo = viewModelScope.launch {
            try {
                uiState = ScanUiState.Success(scan.escanear(foto))
                // El historial ya incluye este escaneo: se refresca.
                cargarRecientes()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState = ScanUiState.Error(e.message ?: "No se pudo conectar con el servidor")
            }
        }
    }

    /** Cancela la busqueda en curso y vuelve a la camara. */
    fun cancelar() {
        trabajo?.cancel()
        trabajo = null
        uiState = ScanUiState.Idle
    }

    /** Error de captura: cierra la pantalla de "Analizando..." con un mensaje. */
    fun fallar(mensaje: String) {
        if (uiState is ScanUiState.Loading) {
            uiState = ScanUiState.Error(mensaje)
        }
    }

    fun cargarRecientes() {
        viewModelScope.launch {
            recientes = runCatching { scan.recientes() }.getOrDefault(emptyList())
        }
    }

    fun reiniciar() {
        uiState = ScanUiState.Idle
    }

    companion object {
        val Factory = viewModelFactory { initializer { ScanViewModel(AppContainer.scan) } }
    }
}
