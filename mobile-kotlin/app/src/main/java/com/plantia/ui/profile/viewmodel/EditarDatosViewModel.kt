package com.plantia.ui.profile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.repository.UsuarioRepository
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

/** Editar datos: el usuario cambia su nombre; el correo solo se muestra. El apodo de las plantas se cambia en cada planta. */
class EditarDatosViewModel(
    private val usuarios: UsuarioRepository
) : ViewModel() {

    /** Nombre guardado del usuario (null mientras carga). */
    var nombreGuardado: String? by mutableStateOf(null)
        private set

    /** Correo de la cuenta: solo lectura. */
    var correo: String by mutableStateOf("")
        private set

    var cargando: Boolean by mutableStateOf(true)
        private set

    /** Mensaje corto del ultimo guardado (exito o error); la pantalla lo muestra y llama a [avisoMostrado]. */
    var aviso: String? by mutableStateOf(null)
        private set

    init {
        cargar()
    }

    fun avisoMostrado() {
        aviso = null
    }

    private fun cargar() {
        viewModelScope.launch {
            cargando = true
            val perfil = runCatching { usuarios.perfil() }.getOrNull()
            nombreGuardado = perfil?.name
            correo = perfil?.email.orEmpty()
            cargando = false
        }
    }

    fun guardarNombre(nuevo: String) {
        val limpio = nuevo.trim()
        if (limpio.length < 2) {
            aviso = "El nombre debe tener al menos 2 letras"
            return
        }
        viewModelScope.launch {
            val actualizado = runCatching { usuarios.cambiarNombre(limpio) }.getOrNull()
            if (actualizado != null) {
                nombreGuardado = actualizado.name
                aviso = "Nombre guardado"
            } else {
                aviso = "No se pudo guardar el nombre"
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { EditarDatosViewModel(AppContainer.usuario) }
        }
    }
}
