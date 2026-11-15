package com.plantia.ui.tienda.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.Tienda
import com.plantia.data.model.TiendaItem
import com.plantia.data.repository.AbonoResultado
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.TiendaRepository
import com.plantia.data.repository.TiendaResultado
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

sealed interface TiendaUiState {
    data object Loading : TiendaUiState
    data class Listo(val tienda: Tienda) : TiendaUiState
    data class Error(val mensaje: String) : TiendaUiState
}

/** Tienda: lista lo que el admin tiene a la venta; comprar, equipar y usar el abono pasan por el servidor. */
class TiendaViewModel(
    private val tienda: TiendaRepository,
    private val garden: GardenRepository
) : ViewModel() {

    var uiState: TiendaUiState by mutableStateOf(TiendaUiState.Loading)
        private set

    /** Mensaje de la ultima accion (exito o error); la pantalla lo muestra y llama a [avisoVisto]. */
    var aviso: String? by mutableStateOf(null)
        private set

    var ocupado: Boolean by mutableStateOf(false)
        private set

    init {
        cargar()
    }

    fun cargar() {
        uiState = TiendaUiState.Loading
        viewModelScope.launch { aplicar(tienda.cargar(), exito = null) }
    }

    fun avisoVisto() {
        aviso = null
    }

    fun comprar(item: TiendaItem) = accion { aplicar(tienda.comprar(item.id), "Compraste «${item.nombre}»") }

    fun equipar(item: TiendaItem) = accion { aplicar(tienda.equipar(item.id), "Ahora usas «${item.nombre}»") }

    fun desequipar(item: TiendaItem) = accion { aplicar(tienda.desequipar(item.id), "Volviste al fondo de siempre") }

    /** Compra un abono y lo usa en la planta del jardin. */
    fun usarAbono() = accion {
        when (val activa = garden.plantaActiva()) {
            is PlantaActivaResultado.ConPlanta -> {
                val id = activa.planta.id
                if (id == null) {
                    aviso = "No se encontro tu planta"
                } else when (val r = tienda.usarAbono(id)) {
                    is AbonoResultado.Con -> {
                        aplicar(tienda.cargar(), "¡Abono aplicado! ${r.planta.name} ganó un día de crecimiento 🌱")
                    }
                    is AbonoResultado.Fallo -> aviso = r.mensaje
                }
            }
            PlantaActivaResultado.SinPlanta -> aviso = "Primero digitaliza una planta para abonarla"
            is PlantaActivaResultado.Fallo -> aviso = activa.mensaje
        }
    }

    private fun accion(bloque: suspend () -> Unit) {
        if (ocupado) return
        ocupado = true
        viewModelScope.launch {
            try {
                bloque()
            } finally {
                ocupado = false
            }
        }
    }

    private fun aplicar(resultado: TiendaResultado, exito: String?) {
        when (resultado) {
            is TiendaResultado.Con -> {
                uiState = TiendaUiState.Listo(resultado.tienda)
                if (exito != null) aviso = exito
            }
            is TiendaResultado.Fallo ->
                // Con la tienda ya a la vista un error de compra solo se avisa; si no habia nada, pantalla de error
                if (uiState is TiendaUiState.Listo) aviso = resultado.mensaje else uiState = TiendaUiState.Error(resultado.mensaje)
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { TiendaViewModel(AppContainer.tienda, AppContainer.garden) } }
    }
}
