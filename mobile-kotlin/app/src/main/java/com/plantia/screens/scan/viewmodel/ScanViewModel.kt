package com.plantia.screens.scan.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.ScanResultDto
import java.io.File
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data object Loading : ScanUiState
    data class Success(val result: ScanResultDto) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScanViewModel : ViewModel() {

    var uiState: ScanUiState by mutableStateOf(ScanUiState.Idle)
        private set

    /** Captura tomada -> se sube al backend y se espera la informacion de la planta. */
    fun escanear(foto: File) {
        if (!foto.exists() || foto.length() == 0L) {
            uiState = ScanUiState.Error("La foto no se guardo correctamente")
            return
        }
        uiState = ScanUiState.Loading
        viewModelScope.launch {
            try {
                val part = MultipartBody.Part.createFormData(
                    "foto",
                    foto.name,
                    foto.asRequestBody("image/jpeg".toMediaType())
                )
                val result = RetrofitClient.api.escanear(USER_ID, part)
                uiState = ScanUiState.Success(result)
            } catch (e: Exception) {
                uiState = ScanUiState.Error(e.message ?: "No se pudo conectar con el servidor")
            }
        }
    }

    /** Error de captura: cierra la pantalla de "Analizando..." con un mensaje. */
    fun fallar(mensaje: String) {
        if (uiState is ScanUiState.Loading) {
            uiState = ScanUiState.Error(mensaje)
        }
    }

    fun reiniciar() {
        uiState = ScanUiState.Idle
    }

    companion object {
        /** Usuario temporal (sin auth) hasta integrar login. */
        const val USER_ID = 1
    }
}
