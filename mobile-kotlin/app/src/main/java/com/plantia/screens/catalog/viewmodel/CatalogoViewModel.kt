package com.plantia.screens.catalog.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.toDomain
import com.plantia.model.CatalogoItem
import com.plantia.model.Cuidado
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Catalogo general de plantas, con busqueda y filtro de toxicidad. */
class CatalogoViewModel : ViewModel() {

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
        busquedaJob = viewModelScope.launch(Dispatchers.IO) {
            delay(350)
            cargar()
        }
    }

    fun onToxicasCambia(valor: Boolean) {
        soloToxicas = valor
        cargar()
    }

    fun cargar() {
        viewModelScope.launch(Dispatchers.IO) {
            cargando = true
            error = null
            runCatching {
                RetrofitClient.api.catalogo(
                    q = consulta.trim().ifBlank { null },
                    toxica = if (soloToxicas) true else null
                ).body().orEmpty()
            }.onSuccess { lista ->
                items = lista.map { it.toDomain() }
            }.onFailure {
                error = "No se pudo cargar el catalogo"
            }
            cargando = false
        }
    }
}

/** Detalle de una planta del catalogo: sus cuidados. */
class CuidadosCatalogoViewModel : ViewModel() {

    var planta: CatalogoItem? by mutableStateOf(null)
        private set

    var cuidados: List<Cuidado> by mutableStateOf(emptyList())
        private set

    var cargando: Boolean by mutableStateOf(true)
        private set

    var error: String? by mutableStateOf(null)
        private set

    fun cargar(idCatalogo: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            cargando = true
            error = null
            runCatching {
                val catalogo = RetrofitClient.api.catalogo().body().orEmpty()
                val planta = catalogo.firstOrNull { it.id == idCatalogo }
                val cuidados = RetrofitClient.api.cuidadosCatalogo(idCatalogo).body().orEmpty()
                planta to cuidados
            }.onSuccess { (plantaDto, cuidadosDto) ->
                // Ojo: dentro de `launch` el `this` es el CoroutineScope, por eso
                // se usan los nombres de la clase sin prefijar.
                planta = plantaDto?.toDomain()
                cuidados = cuidadosDto.map { it.toDomain() }
                if (plantaDto == null) error = "No se encontro la planta"
            }.onFailure {
                error = "No se pudieron cargar los cuidados"
            }
            cargando = false
        }
    }
}
