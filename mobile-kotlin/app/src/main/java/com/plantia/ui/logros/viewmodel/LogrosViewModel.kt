package com.plantia.ui.logros.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.Logro
import com.plantia.data.model.LogrosResumen
import com.plantia.data.repository.LogrosRepository
import com.plantia.data.repository.LogrosResultado
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

enum class FiltroLogros(val etiqueta: String) {
    TODOS("Todos"),
    DESBLOQUEADOS("Desbloqueados"),
    BLOQUEADOS("Bloqueados")
}

data class LogrosUiState(
    val cargando: Boolean = true,
    val resumen: LogrosResumen? = null,
    val error: String? = null,
    val filtro: FiltroLogros = FiltroLogros.TODOS
) {
    private val todos: List<Logro> get() = resumen?.logros.orEmpty()

    val bloqueados: Int get() = todos.count { !it.desbloqueado }

    /** Los logros que pasan el filtro elegido, en el orden del servidor. */
    val visibles: List<Logro>
        get() = when (filtro) {
            FiltroLogros.TODOS -> todos
            FiltroLogros.DESBLOQUEADOS -> todos.filter { it.desbloqueado }
            FiltroLogros.BLOQUEADOS -> todos.filter { !it.desbloqueado }
        }

    fun conteo(filtro: FiltroLogros): Int = when (filtro) {
        FiltroLogros.TODOS -> todos.size
        FiltroLogros.DESBLOQUEADOS -> todos.size - bloqueados
        FiltroLogros.BLOQUEADOS -> bloqueados
    }
}

/** Pantalla de Logros: lista con avance, filtros y estados cargando / error. */
class LogrosViewModel(private val logros: LogrosRepository) : ViewModel() {

    var uiState by mutableStateOf(LogrosUiState())
        private set

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, error = null)
            when (val resultado = logros.listar()) {
                is LogrosResultado.Con -> uiState = uiState.copy(cargando = false, resumen = resultado.resumen, error = null)
                // Un fallo de red conserva lo que ya se veia
                is LogrosResultado.Fallo -> uiState = uiState.copy(cargando = false, error = resultado.mensaje)
            }
        }
    }

    fun elegirFiltro(filtro: FiltroLogros) {
        uiState = uiState.copy(filtro = filtro)
    }

    companion object {
        val Factory = viewModelFactory { initializer { LogrosViewModel(AppContainer.logros) } }
    }
}
