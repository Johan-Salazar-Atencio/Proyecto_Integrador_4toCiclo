package com.plantia.screens.collection.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.toDomain
import com.plantia.model.ColeccionItem
import com.plantia.model.Cuidado
import kotlinx.coroutines.launch

/** "Mi coleccion": plantas que el usuario ha visto. */
class ColeccionViewModel : ViewModel() {

    var items: List<ColeccionItem> by mutableStateOf(emptyList())
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
                RetrofitClient.api.coleccion().body().orEmpty()
            }.onSuccess { lista ->
                items = lista.map { it.toDomain() }
            }.onFailure {
                error = "No se pudo cargar tu coleccion"
            }
            cargando = false
        }
    }
}

/** Detalle de un item de la coleccion: la planta y sus cuidados. */
class ColeccionDetalleViewModel : ViewModel() {

    var item: ColeccionItem? by mutableStateOf(null)
        private set

    var cuidados: List<Cuidado> by mutableStateOf(emptyList())
        private set

    var cargando: Boolean by mutableStateOf(true)
        private set

    var error: String? by mutableStateOf(null)
        private set

    fun cargar(idColeccion: Int) {
        viewModelScope.launch {
            cargando = true
            error = null
            runCatching {
                RetrofitClient.api.coleccionDetalle(idColeccion).body()
            }.onSuccess { detalle ->
                if (detalle == null) {
                    error = "No se encontro la planta"
                } else {
                    item = detalle.item.toDomain()
                    cuidados = detalle.cuidados.map { it.toDomain() }
                }
            }.onFailure {
                error = "No se pudo cargar el detalle"
            }
            cargando = false
        }
    }
}
