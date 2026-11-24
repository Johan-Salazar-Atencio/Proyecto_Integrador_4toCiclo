package com.plantia.ui.tienda.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.Tienda
import com.plantia.data.model.TiendaItem
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.PlantasResultado
import com.plantia.data.repository.TiendaRepository
import com.plantia.data.repository.TiendaResultado
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

sealed interface TiendaUiState {
    data object Loading : TiendaUiState
    data class Listo(val tienda: Tienda) : TiendaUiState
    data class Error(val mensaje: String) : TiendaUiState
}

/** Tienda: lista lo que el admin tiene a la venta; comprar y equipar pasan por el servidor. El abono se compra aquí y se usa en el jardín. */
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

    /** Maceta a la que se le estan eligiendo plantas; null con la hoja cerrada. */
    var aplicando: TiendaItem? by mutableStateOf(null)
        private set

    /** Plantas del invernadero entre las que se elige (las del baul no, estan congeladas). */
    var plantasParaMaceta: List<PlantSummary> by mutableStateOf(emptyList())
        private set

    var cargandoPlantas: Boolean by mutableStateOf(false)
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

    /** Artículo que se está comprando ahora: solo su tarjeta muestra el progreso, las demás no reaccionan. */
    var comprandoId: Int? by mutableStateOf(null)
        private set

    /** Compra un articulo; cada tipo se usa en un lugar distinto y el aviso lo dice. El abono suma al contador. */
    fun comprar(item: TiendaItem) = accion {
        comprandoId = item.id
        try {
            if (item.tipo == TiendaItem.ABONO) {
                val resultado = tienda.comprarAbono(item.id)
                val ahora = ((resultado as? TiendaResultado.Con)?.tienda?.items?.firstOrNull { it.id == item.id })?.cantidad
                aplicar(resultado, "Compraste «${item.nombre}»" + (ahora?.let { ". Ahora tienes $it" } ?: "") + ". Úsalo desde tu jardín")
            } else {
                val donde = when (item.tipo) {
                    TiendaItem.MACETA -> "Ponla a tus plantas con «Poner a mis plantas»"
                    TiendaItem.INVERNADERO -> "Elígelo desde el chip de tu colección"
                    else -> "Ponlo desde el icono de fondos de tu jardín"
                }
                aplicar(tienda.comprar(item.id), "Compraste «${item.nombre}». $donde")
            }
        } finally {
            comprandoId = null
        }
    }

    /** Abre la hoja para elegir a cuales plantas se les pone la maceta. */
    fun abrirAplicar(maceta: TiendaItem) {
        aplicando = maceta
        plantasParaMaceta = emptyList()
        cargandoPlantas = true
        viewModelScope.launch {
            when (val r = garden.listar()) {
                is PlantasResultado.Con -> plantasParaMaceta = r.plantas.filter { !it.enBaul }
                is PlantasResultado.Fallo -> {
                    aviso = r.mensaje
                    aplicando = null
                }
            }
            cargandoPlantas = false
        }
    }

    fun cerrarAplicar() {
        aplicando = null
    }

    /** Pone la maceta a las plantas elegidas; el servidor las cambia todas o ninguna. */
    fun aplicarMaceta(idPlantas: List<Int>) {
        val maceta = aplicando ?: return
        accion {
            when (val r = garden.ponerMaceta(maceta.id, idPlantas)) {
                is PlantasResultado.Con -> {
                    aviso = "«${maceta.nombre}» puesta a ${idPlantas.size} planta${if (idPlantas.size == 1) "" else "s"}"
                    aplicando = null
                }
                is PlantasResultado.Fallo -> aviso = r.mensaje
            }
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
