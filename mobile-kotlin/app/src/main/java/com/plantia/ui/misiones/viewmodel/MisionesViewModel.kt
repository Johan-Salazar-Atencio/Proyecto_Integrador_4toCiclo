package com.plantia.ui.misiones.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.DailyMission
import com.plantia.data.model.PlantSummary
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.HomeRepository
import com.plantia.data.repository.PlantaResultado
import com.plantia.data.repository.PlantasResultado
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

data class MisionesUiState(
    val cargando: Boolean = true,
    val misiones: List<DailyMission> = emptyList(),
    val plantas: List<PlantSummary> = emptyList(),
    val error: String? = null
) {
    /** La mision del evento activo (tarjeta roja), si hay uno. */
    val evento: DailyMission? get() = misiones.firstOrNull { it.esEvento }

    val diarias: List<DailyMission> get() = misiones.filterNot { it.esEvento }

    val completadas: Int get() = diarias.count { it.isCompleted }

    /** Plantas en las que todavia se puede prevenir el evento: vivas, sin proteger y sin golpe previo. */
    val prevenibles: List<PlantSummary>
        get() = plantas.filter { it.id != null && !it.muerta && !it.refugiada && !it.enRecuperacion }
}

/** Todas las misiones de hoy; la del evento va aparte y permite prevenirlo en varias plantas a la vez. */
class MisionesViewModel(
    private val home: HomeRepository,
    private val garden: GardenRepository
) : ViewModel() {

    var uiState by mutableStateOf(MisionesUiState())
        private set

    /** Hoja con las plantas para prevenir el evento. */
    var hojaAbierta by mutableStateOf(false)
        private set

    /** Ids de las plantas marcadas en la hoja. */
    var seleccion by mutableStateOf<Set<Int>>(emptySet())
        private set

    var previniendo by mutableStateOf(false)
        private set

    var aviso by mutableStateOf<String?>(null)
        private set

    init {
        cargar()
    }

    fun avisoMostrado() {
        aviso = null
    }

    fun cargar() {
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, error = null)
            val misiones = home.misiones()
            val plantas = (garden.listar() as? PlantasResultado.Con)?.plantas
            // Un fallo de red conserva lo que ya se veia
            uiState = MisionesUiState(
                cargando = false,
                misiones = misiones ?: uiState.misiones,
                plantas = plantas ?: uiState.plantas,
                error = if (misiones == null) "No se pudieron cargar las misiones" else null
            )
        }
    }

    /** Abre la hoja con todas las plantas prevenibles marcadas; no se abre si no hay ninguna. */
    fun abrirHoja() {
        val ids = uiState.prevenibles.mapNotNull { it.id }
        if (ids.isEmpty()) return
        seleccion = ids.toSet()
        hojaAbierta = true
    }

    fun cerrarHoja() {
        hojaAbierta = false
    }

    fun alternar(id: Int) {
        seleccion = if (id in seleccion) seleccion - id else seleccion + id
    }

    fun alternarTodas() {
        val todas = uiState.prevenibles.mapNotNull { it.id }.toSet()
        seleccion = if (seleccion == todas) emptySet() else todas
    }

    /** Previene el evento en las plantas marcadas. */
    fun prevenir() {
        val ids = seleccion.toList()
        if (ids.isEmpty() || previniendo) return
        previniendo = true
        viewModelScope.launch {
            when (val r = garden.prevenirVarias(ids)) {
                is PlantasResultado.Con -> {
                    hojaAbierta = false
                    seleccion = emptySet()
                    aviso = if (ids.size == 1) "Evento prevenido en 1 planta" else "Evento prevenido en ${ids.size} plantas"
                }
                is PlantasResultado.Fallo -> aviso = r.mensaje
            }
            previniendo = false
            // Con exito se ve el avance; con un fallo lo mas probable es que el evento ya termino
            cargar()
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { MisionesViewModel(AppContainer.home, AppContainer.garden) } }
    }
}
