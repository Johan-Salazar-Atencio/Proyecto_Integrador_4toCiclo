package com.plantia.ui.detail.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.PlantSummary
import com.plantia.data.remote.dto.ScanResultDto
import com.plantia.data.repository.CollectionRepository
import com.plantia.data.repository.DetalleColeccionResultado
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.PlantaResultado
import com.plantia.data.repository.PlantasResultado
import com.plantia.data.repository.ScanRepository
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

    /** Planta de la misma especie que ya esta en el jardin (o el baul); null si no hay (QA-04). */
    var enJardin: PlantSummary? by mutableStateOf(null)
        private set

    fun mostrarUltimoEscaneo() {
        uiState = scan.ultimoResultado.value?.let { DetalleUiState.Listo(it) } ?: DetalleUiState.SinResultado
        (uiState as? DetalleUiState.Listo)?.let { revisarJardin(it.resultado) }
    }

    /** Antes de digitalizar se mira si la especie ya esta en el jardin, para avisarlo con claridad. */
    private fun revisarJardin(resultado: ScanResultDto) {
        enJardin = null
        if (resultado.nombreCientifico.isNullOrBlank()) return
        viewModelScope.launch {
            val r = garden.listar()
            if (r is PlantasResultado.Con) enJardin = plantaDeLaEspecie(r.plantas, resultado.nombreCientifico)
        }
    }

    fun cargarDeColeccion(id: Int) {
        uiState = DetalleUiState.Cargando
        viewModelScope.launch {
            uiState = when (val r = collection.detalle(id)) {
                is DetalleColeccionResultado.Con -> DetalleUiState.Listo(r.planta)
                is DetalleColeccionResultado.Fallo -> DetalleUiState.Fallo(r.mensaje)
            }
            (uiState as? DetalleUiState.Listo)?.let { revisarJardin(it.resultado) }
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
        /** La planta de la misma especie (nombre cientifico, sin distinguir mayusculas); null si no hay. */
        fun plantaDeLaEspecie(plantas: List<PlantSummary>, cientifico: String?): PlantSummary? {
            val especie = cientifico?.trim()?.lowercase().orEmpty()
            if (especie.isEmpty()) return null
            return plantas.firstOrNull { it.species.trim().lowercase() == especie }
        }

        /** Texto que la ficha muestra sobre el boton cuando la especie ya esta en el jardin. */
        fun avisoYaEnJardin(planta: PlantSummary?): String? = when {
            planta == null -> null
            planta.enBaul -> "${planta.name} está en tu baúl: sácala primero para verla en el jardín."
            planta.muerta -> "${planta.name} ya está en tu jardín y está muerta: digitalizarla otra vez no la revive."
            else -> "${planta.name} ya está en tu jardín. Digitalizarla otra vez no la reinicia: conserva su progreso y solo se actualiza la foto."
        }

        val Factory = viewModelFactory {
            initializer {
                PlantDetailViewModel(AppContainer.scan, AppContainer.collection, AppContainer.garden)
            }
        }
    }
}
