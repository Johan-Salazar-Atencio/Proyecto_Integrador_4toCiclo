package com.plantia.screens.catalog.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.CatalogoRepository
import com.plantia.di.AppContainer
import com.plantia.model.CatalogoItem
import com.plantia.model.Cuidado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Catalogo general de plantas, con busqueda y filtro de toxicidad. */
class CatalogoViewModel(private val catalogo: CatalogoRepository) : ViewModel() {

    var items: List<CatalogoItem> by mutableStateOf(emptyList())
        private set

    var consulta: String by mutableStateOf("")
        private set

    var soloToxicas: Boolean by mutableStateOf(false)
        private set

    var cargando: Boolean by mutableStateOf(true)
        private set

    var error: String? by mutableStateOf(null)
        private set

    private var busquedaJob: Job? = null

    init {
        cargar()
    }

    fun onConsultaCambia(valor: String) {
        consulta = valor
        // Se espera a que el usuario termine de escribir.
        busquedaJob?.cancel()
        busquedaJob = viewModelScope.launch {
            delay(350)
            cargar()
        }
    }

    fun onToxicasCambia(valor: Boolean) {
        soloToxicas = valor
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            cargando = true
            error = null
            runCatching { catalogo.buscar(consulta, soloToxicas) }
                .onSuccess { items = it }
                .onFailure { error = "No se pudo cargar el catalogo" }
            cargando = false
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { CatalogoViewModel(AppContainer.catalogo) } }
    }
}

/** Detalle de una planta del catalogo: sus cuidados. */
class CuidadosCatalogoViewModel(private val catalogo: CatalogoRepository) : ViewModel() {

    var planta: CatalogoItem? by mutableStateOf(null)
        private set

    var cuidados: List<Cuidado> by mutableStateOf(emptyList())
        private set

    var cargando: Boolean by mutableStateOf(true)
        private set

    var error: String? by mutableStateOf(null)
        private set

    fun cargar(idCatalogo: Int) {
        viewModelScope.launch {
            cargando = true
            error = null
            runCatching { catalogo.ficha(idCatalogo) }
                .onSuccess { ficha ->
                    planta = ficha.planta
                    cuidados = ficha.cuidados
                    if (ficha.planta == null) error = "No se encontro la planta"
                }
                .onFailure { error = "No se pudieron cargar los cuidados" }
            cargando = false
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { CuidadosCatalogoViewModel(AppContainer.catalogo) } }
    }
}
