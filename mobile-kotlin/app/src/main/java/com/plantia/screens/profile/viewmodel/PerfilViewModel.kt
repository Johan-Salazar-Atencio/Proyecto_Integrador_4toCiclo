package com.plantia.screens.profile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.toDomain
import com.plantia.model.UserSummary
import kotlinx.coroutines.launch

/** Perfil del usuario (GET /api/v1/usuarios/me). */
class PerfilViewModel : ViewModel() {

    var usuario: UserSummary? by mutableStateOf(null)
        private set

    var cargando: Boolean by mutableStateOf(true)
        private set

    var error: String? by mutableStateOf(null)
        private set

    init {
        cargar()
    }

    fun recargar() = cargar()

    private fun cargar() {
        viewModelScope.launch {
            cargando = true
            error = null
            runCatching {
                RetrofitClient.api.usuarioMe().body()
            }.onSuccess { dto ->
                if (dto == null) {
                    // El backend devuelve 404 si el id no existe: se limpia el
                    // usuario para que se vea el error en vez de un dato viejo.
                    usuario = null
                    error = "No existe un usuario con ese id"
                } else {
                    usuario = dto.toDomain()
                }
            }.onFailure {
                error = "No se pudo cargar el perfil"
            }
            cargando = false
        }
    }
}
