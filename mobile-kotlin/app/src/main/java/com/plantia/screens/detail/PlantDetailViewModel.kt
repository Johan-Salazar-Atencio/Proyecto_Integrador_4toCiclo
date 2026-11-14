package com.plantia.screens.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.CollectionRepository
import com.plantia.data.DetalleColeccionResultado
import com.plantia.data.GardenRepository
import com.plantia.data.PlantaResultado
import com.plantia.data.ScanRepository
import com.plantia.data.remote.ScanResultDto
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

sealed interface DetalleUiState {
    data object Cargando : DetalleUiState
    data class Listo(val resultado: ScanResultDto) : DetalleUiState
    /** No hay un escaneo en memoria (por ejemplo, la app se reabrio directamente en la ficha). */
    data object SinResultado : DetalleUiState
    data class Fallo(val mensaje: String) : DetalleUiState
}

/**
 * Ficha de una planta, venga del ultimo escaneo o de la coleccion. Desde aqui se guarda
 * en la coleccion y se digitaliza en el jardin.
 */
class PlantDetailViewModel(
    private val scan: ScanRepository,
    private val collection: CollectionRepository,
    private val garden: GardenRepository
) : ViewModel() {

    var uiState: DetalleUiState by mutableStateOf(DetalleUiState.Cargando)
        private set

    /** Mensaje de un fallo puntual; la pantalla lo muestra y lo limpia. */
    var aviso: String? by mutableStateOf(null)
        private set

    fun mostrarUltimoEscaneo() {
        uiState = scan.ultimoResultado.value?.let { DetalleUiState.Listo(it) } ?: DetalleUiState.SinResultado
    }

    fun cargarDeColeccion(id: Int) {
        uiState = DetalleUiState.Cargando
        viewModelScope.launch {
            uiState = when (val r = collection.detalle(id)) {
                is DetalleColeccionResultado.Con -> DetalleUiState.Listo(r.planta)
                is DetalleColeccionResultado.Fallo -> DetalleUiState.Fallo(r.mensaje)
            }
        }
    }

    /** Guarda el escaneo en la coleccion; el backend ya lo registro, por eso se continua aunque falle. */
    fun guardar(alTerminar: () -> Unit) {
        val resultado = (uiState as? DetalleUiState.Listo)?.resultado ?: return
        viewModelScope.launch {
            collection.agregar(resultado.idEscaneo)
            alTerminar()
        }
    }

    /** Crea la planta en el jardin y, si el servidor lo confirma, avisa para abrirlo. */
    fun digitalizar(alTerminar: () -> Unit) {
        val resultado = (uiState as? DetalleUiState.Listo)?.resultado ?: return
        viewModelScope.launch {
            when (val r = garden.digitalizar(resultado.idEscaneo, resultado.idCatalogo)) {
                is PlantaResultado.Con -> alTerminar()
                is PlantaResultado.Fallo -> aviso = "No se pudo digitalizar: ${r.mensaje}"
            }
        }
    }

    fun avisoMostrado() {
        aviso = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                PlantDetailViewModel(AppContainer.scan, AppContainer.collection, AppContainer.garden)
            }
        }
    }
}
