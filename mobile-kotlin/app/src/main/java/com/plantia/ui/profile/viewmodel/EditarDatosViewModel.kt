package com.plantia.ui.profile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.PlantSummary
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.PlantaResultado
import com.plantia.data.repository.PlantasResultado
import com.plantia.data.repository.UsuarioRepository
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

/** Editar datos: el nombre del usuario y el nombre (apodo) de cada una de sus plantas. */
class EditarDatosViewModel(
    private val usuarios: UsuarioRepository,
    private val garden: GardenRepository
) : ViewModel() {

    /** Nombre guardado del usuario (null mientras carga). */
    var nombreGuardado: String? by mutableStateOf(null)
        private set

    var plantas: List<PlantSummary> by mutableStateOf(emptyList())
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
            nombreGuardado = runCatching { usuarios.perfil() }.getOrNull()?.name
            plantas = (garden.listar() as? PlantasResultado.Con)?.plantas.orEmpty()
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

    /** Cambia el apodo de una planta; vacio vuelve a su nombre comun. */
    fun guardarPlanta(planta: PlantSummary, nuevo: String) {
        val id = planta.id ?: return
        viewModelScope.launch {
            when (val r = garden.renombrar(id, nuevo.trim())) {
                is PlantaResultado.Con -> {
                    plantas = plantas.map { if (it.id == id) r.planta else it }
                    aviso = "Nombre de ${r.planta.name} guardado"
                }
                is PlantaResultado.Fallo -> aviso = "No se pudo guardar: ${r.mensaje}"
            }
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { EditarDatosViewModel(AppContainer.usuario, AppContainer.garden) }
        }
    }
}
