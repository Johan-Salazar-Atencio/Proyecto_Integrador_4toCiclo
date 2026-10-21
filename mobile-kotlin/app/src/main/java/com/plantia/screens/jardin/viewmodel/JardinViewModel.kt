package com.plantia.screens.jardin.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.toDomain
import com.plantia.model.PlantSummary
import kotlinx.coroutines.launch

/** Planta en crianza del usuario (GET /api/v1/plantas/activa). */
class JardinViewModel : ViewModel() {

    var planta: PlantSummary? by mutableStateOf(null)
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
                RetrofitClient.api.plantaActiva().body()
            }.onSuccess { dto ->
                if (dto == null) {
                    error = "Todavia no tienes plantas en el jardin"
                } else {
                    planta = dto.toDomain()
                }
            }.onFailure {
                error = "No se pudo cargar tu jardin"
            }
            cargando = false
        }
    }
}
