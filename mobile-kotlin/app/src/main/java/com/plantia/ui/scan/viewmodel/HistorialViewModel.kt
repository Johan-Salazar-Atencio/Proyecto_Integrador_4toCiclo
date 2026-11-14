package com.plantia.ui.scan.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.RecentScan
import com.plantia.data.repository.ScanRepository
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

/** Historial de escaneos del usuario (pantalla de historial). */
class HistorialViewModel(private val scan: ScanRepository) : ViewModel() {

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
            runCatching { scan.recientes() }
                .onSuccess { escaneos = it }
                .onFailure { error = "No se pudo cargar el historial" }
            cargando = false
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { HistorialViewModel(AppContainer.scan) } }
    }
}
