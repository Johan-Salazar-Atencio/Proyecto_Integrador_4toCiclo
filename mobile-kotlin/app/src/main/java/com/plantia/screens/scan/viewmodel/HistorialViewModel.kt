package com.plantia.screens.scan.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.toDomain
import com.plantia.model.RecentScan
import kotlinx.coroutines.launch

/** Historial de escaneos del usuario (pantalla de historial). */
class HistorialViewModel : ViewModel() {

    var escaneos: List<RecentScan> by mutableStateOf(emptyList())
        private set

    var cargando: Boolean by mutableStateOf(true)
        private set

    var error: String? by mutableStateOf(null)
        private set

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            error = null
            runCatching {
                RetrofitClient.api.escaneosRecientes().body().orEmpty()
            }.onSuccess { lista ->
                escaneos = lista.map { it.toDomain() }
            }.onFailure {
                error = "No se pudo cargar el historial"
            }
            cargando = false
        }
    }

}
