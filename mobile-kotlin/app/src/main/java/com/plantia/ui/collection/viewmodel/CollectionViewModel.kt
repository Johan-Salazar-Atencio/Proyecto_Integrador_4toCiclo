package com.plantia.ui.collection.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.TiendaItem
import com.plantia.data.remote.dto.ColeccionItemDto
import com.plantia.data.repository.ColeccionResultado
import com.plantia.data.repository.CollectionRepository
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.PlantaResultado
import com.plantia.data.repository.PlantasResultado
import com.plantia.data.repository.TiendaRepository
import com.plantia.data.repository.TiendaResultado
import com.plantia.data.repository.UsuarioRepository
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

sealed interface CollectionUiState {
    data object Loading : CollectionUiState
    /** Las listas pueden venir vacias: es el estado inicial de un usuario nuevo. */
    data class Success(
        val escaneadas: List<ColeccionItemDto>,
        val digitales: List<PlantSummary>
    ) : CollectionUiState
    data class Error(val mensaje: String) : CollectionUiState
}

class CollectionViewModel(
    private val collection: CollectionRepository,
    private val garden: GardenRepository,
    /** Para saber que invernaderos tiene el usuario; sin ella el menu de temas queda vacio. */
    private val tienda: TiendaRepository? = null,
    /** Para leer y cambiar el invernadero elegido. */
    private val usuarios: UsuarioRepository? = null
) : ViewModel() {

    /** Codigo del invernadero de la coleccion virtual (SELVA por defecto). */
    var invernadero: String by mutableStateOf("SELVA")
        private set

    /** Invernaderos que el usuario tiene (el predeterminado y los que compro), para elegir entre ellos. */
    var invernaderosPropios: List<TiendaItem> by mutableStateOf(emptyList())
        private set

    var uiState: CollectionUiState by mutableStateOf(CollectionUiState.Loading)
        private set

    /** Mensaje de un fallo puntual (por ejemplo al elegir una planta); la pantalla lo muestra y lo limpia. */
    var aviso: String? by mutableStateOf(null)
        private set

    init {
        cargar()
    }

    fun cargar() {
        uiState = CollectionUiState.Loading
        viewModelScope.launch {
            uiState = when (val r = collection.listar()) {
                is ColeccionResultado.Con -> CollectionUiState.Success(
                    escaneadas = r.plantas,
                    // Si fallan las digitales se muestra la pestana vacia, no se tumba la coleccion.
                    digitales = (garden.listar() as? PlantasResultado.Con)?.plantas.orEmpty()
                )
                is ColeccionResultado.Fallo -> CollectionUiState.Error(r.mensaje)
            }
            cargarInvernadero()
        }
    }

    /** Un fallo aqui no tumba la coleccion: se queda con la Selva tropical y sin otros temas. */
    private suspend fun cargarInvernadero() {
        usuarios?.let { repo ->
            runCatching { repo.perfil() }.getOrNull()?.let { invernadero = it.greenhouseCode }
        }
        tienda?.let { repo ->
            (repo.cargar() as? TiendaResultado.Con)?.let { r ->
                invernaderosPropios = r.tienda.invernaderos.filter { it.comprado }
            }
        }
    }

    /** Pone otro invernadero (de los que tiene) a la coleccion. */
    fun elegirInvernadero(item: TiendaItem) {
        val repo = usuarios ?: return
        viewModelScope.launch {
            val perfil = runCatching { repo.cambiarInvernadero(item.id) }.getOrNull()
            if (perfil != null) {
                invernadero = perfil.greenhouseCode
                aviso = "Tu colección ahora es «${item.nombre}»"
            } else {
                aviso = "No se pudo cambiar el invernadero"
            }
        }
    }

    /** Cambia el apodo; vacio vuelve al nombre de la especie. */
    fun renombrar(planta: PlantSummary, apodo: String) {
        val id = planta.id ?: return
        actualizar(planta, { if (apodo.isBlank()) "Vuelve a llamarse ${it.name}" else "Ahora se llama ${it.name}" }) {
            garden.renombrar(id, apodo)
        }
    }

    /** Guarda la planta en el baul: queda congelada y libera un lugar. */
    fun guardarEnBaul(planta: PlantSummary) {
        val id = planta.id ?: return
        actualizar(planta, { "${it.name} descansa en el baúl" }) { garden.guardarEnBaul(id) }
    }

    /** Saca la planta del baul; el servidor la rechaza si el invernadero ya esta lleno. */
    fun sacarDelBaul(planta: PlantSummary) {
        val id = planta.id ?: return
        actualizar(planta, { "${it.name} volvió al invernadero" }) { garden.sacarDelBaul(id) }
    }

    /** Aplica el cambio y deja la planta nueva en su lugar, sin recargar toda la coleccion. */
    private fun actualizar(
        planta: PlantSummary,
        exito: (PlantSummary) -> String,
        llamada: suspend () -> PlantaResultado
    ) {
        viewModelScope.launch {
            when (val r = llamada()) {
                is PlantaResultado.Con -> {
                    (uiState as? CollectionUiState.Success)?.let { actual ->
                        uiState = actual.copy(digitales = actual.digitales.map { if (it.id == r.planta.id) r.planta else it })
                    }
                    aviso = exito(r.planta)
                }
                is PlantaResultado.Fallo -> aviso = r.mensaje
            }
        }
    }

    /** Elige la planta del jardin y, si el servidor lo confirma, avisa para abrirlo. */
    fun seleccionar(planta: PlantSummary, alTerminar: () -> Unit) {
        val id = planta.id ?: return
        viewModelScope.launch {
            when (val r = garden.seleccionar(id)) {
                is PlantaResultado.Con -> alTerminar()
                is PlantaResultado.Fallo -> aviso = "No se pudo elegir la planta: ${r.mensaje}"
            }
        }
    }

    fun avisoMostrado() {
        aviso = null
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CollectionViewModel(AppContainer.collection, AppContainer.garden, AppContainer.tienda, AppContainer.usuario)
            }
        }
    }
}
