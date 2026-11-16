package com.plantia.ui.garden.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.TemaFondo
import com.plantia.data.model.TiendaItem
import com.plantia.data.model.WeatherAlert
import com.plantia.data.repository.AlertaResultado
import com.plantia.data.repository.HomeRepository
import com.plantia.data.repository.AbonoResultado
import com.plantia.data.repository.FondosResultado
import com.plantia.data.repository.TiendaRepository
import com.plantia.data.repository.TiendaResultado
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.di.AppContainer
import kotlinx.coroutines.launch

sealed interface GardenUiState {
    data object Loading : GardenUiState
    data class Success(val planta: PlantSummary) : GardenUiState
    data object SinPlanta : GardenUiState
    data class Error(val mensaje: String) : GardenUiState
}

class GardenViewModel(
    private val garden: GardenRepository,
    /** Tienda para el abono (+1 dia de crecimiento); sin ella el jardin no muestra ese boton. */
    private val tienda: TiendaRepository? = null,
    /** Para saber si hay un evento activo: el boton "Prevenir evento" solo aparece entonces. */
    private val home: HomeRepository? = null
) : ViewModel() {

    /** Evento activo para el usuario, o null si no hay (o si todavia no se consulto). */
    var evento: WeatherAlert? by mutableStateOf(null)
        private set

    /** Hay una peticion de prevenir en curso: evita el doble toque. */
    var previniendo: Boolean by mutableStateOf(false)
        private set

    /** El boton "Prevenir evento" se ofrece con un evento activo y una planta viva, sin proteger y sin golpe previo. */
    val puedePrevenir: Boolean
        get() {
            val planta = (uiState as? GardenUiState.Success)?.planta ?: return false
            return evento != null && !planta.refugiada && !planta.muerta && !planta.enRecuperacion
        }

    /** Sube cada vez que se previene el evento en la planta: la pantalla dispara el efecto del escudo. */
    var proteccionesNuevas: Int by mutableStateOf(0)
        private set

    /**
     * La animacion de como el evento arraso con la planta ya se mostro: se avisa al servidor para que no vuelva a
     * salir. Si no responde se apaga de todos modos aqui, para que no se repita mientras esta pantalla siga abierta.
     */
    fun golpeVisto() {
        val actual = (uiState as? GardenUiState.Success)?.planta ?: return
        val id = actual.id ?: return
        if (actual.golpeTipo == null) return
        viewModelScope.launch {
            uiState = when (val r = garden.golpeVisto(id)) {
                is PlantaResultado.Con -> GardenUiState.Success(r.planta)
                is PlantaResultado.Fallo -> GardenUiState.Success(actual.copy(golpeTipo = null))
            }
        }
    }

    /** Hay una peticion de revivir en curso: evita el doble toque (y el doble cobro). */
    var reviviendo: Boolean by mutableStateOf(false)
        private set

    /** Revive la planta muerta del jardin a cambio de hojas; entrega el mensaje para el usuario. */
    fun revivir(alTerminar: (String) -> Unit) {
        val actual = (uiState as? GardenUiState.Success)?.planta ?: return
        val id = actual.id ?: return
        if (reviviendo || !actual.muerta) return
        reviviendo = true
        viewModelScope.launch {
            when (val r = garden.revivir(id)) {
                is PlantaResultado.Con -> {
                    uiState = GardenUiState.Success(r.planta)
                    alTerminar("${actual.name} volvio a la vida. Cuidala bien")
                }
                is PlantaResultado.Fallo -> alTerminar(r.mensaje)
            }
            reviviendo = false
        }
    }

    /** Consulta si hay un evento activo. Un fallo de red conserva lo que ya se sabia. */
    suspend fun actualizarEvento() {
        val repo = home ?: return
        when (val alerta = repo.alerta()) {
            is AlertaResultado.Con -> evento = alerta.alerta
            AlertaResultado.Sin -> evento = null
            AlertaResultado.Fallo -> Unit
        }
    }

    /** Previene el evento activo en la planta del jardin y entrega el mensaje (con el consejo de campo). */
    fun prevenir(alTerminar: (String) -> Unit) {
        val actual = (uiState as? GardenUiState.Success)?.planta ?: return
        val id = actual.id ?: return
        if (previniendo || !puedePrevenir) return
        val consejo = evento?.tip
        previniendo = true
        viewModelScope.launch {
            when (val r = garden.prevenir(id)) {
                is PlantaResultado.Con -> {
                    uiState = GardenUiState.Success(r.planta)
                    proteccionesNuevas++
                    alTerminar(buildString {
                        append("${actual.name} esta protegida del evento")
                        if (!consejo.isNullOrBlank()) append(". $consejo")
                    })
                }
                is PlantaResultado.Fallo -> {
                    alTerminar(r.mensaje)
                    // Lo mas probable es que el evento ya termino
                    actualizarEvento()
                }
            }
            previniendo = false
        }
    }

    /** Abono de la tienda a la venta (con su precio), o null si no esta disponible. */
    var abono: TiendaItem? by mutableStateOf(null)
        private set

    var uiState: GardenUiState by mutableStateOf(GardenUiState.Loading)
        private set

    /** Temas de fondo que se le pueden poner a la planta del jardin; null mientras el panel esta cerrado. */
    var fondos: List<TemaFondo>? by mutableStateOf(null)
        private set

    var cargandoFondos: Boolean by mutableStateOf(false)
        private set

    private var cuidando = false

    init {
        cargar()
        cargarAbono()
    }

    private fun cargarAbono() {
        val repo = tienda ?: return
        viewModelScope.launch {
            abono = (repo.cargar() as? TiendaResultado.Con)?.tienda?.abono
        }
    }

    /** Abre el panel de fondos de la planta: el de siempre y los que compro el usuario. */
    fun abrirFondos(alTerminar: (String) -> Unit) {
        val repo = tienda ?: return
        val id = (uiState as? GardenUiState.Success)?.planta?.id ?: return
        fondos = emptyList()
        cargandoFondos = true
        viewModelScope.launch {
            when (val r = repo.fondosDePlanta(id)) {
                is FondosResultado.Con -> fondos = r.temas
                is FondosResultado.Fallo -> {
                    fondos = null
                    alTerminar(r.mensaje)
                }
            }
            cargandoFondos = false
        }
    }

    fun cerrarFondos() {
        fondos = null
    }

    /** Pone un tema a esta planta (las demas no cambian); `alCambiar` avisa para volver a armar la escena. */
    fun elegirFondo(tema: TemaFondo, alTerminar: (String) -> Unit, alCambiar: () -> Unit) {
        val repo = tienda ?: return
        val planta = (uiState as? GardenUiState.Success)?.planta ?: return
        val id = planta.id ?: return
        if (tema.enUso) return
        viewModelScope.launch {
            when (val r = repo.ponerFondo(id, if (tema.predeterminado) null else tema.id)) {
                is FondosResultado.Con -> {
                    fondos = r.temas
                    alTerminar("${planta.name} ahora usa «${tema.nombre}»")
                    alCambiar()
                }
                is FondosResultado.Fallo -> alTerminar(r.mensaje)
            }
        }
    }

    /** Compra un abono y lo usa en la planta del jardin; el servidor valida monedas, nivel y el tope diario. */
    fun usarAbono(alTerminar: (String) -> Unit) {
        val repo = tienda ?: return
        val actual = (uiState as? GardenUiState.Success)?.planta ?: return
        val id = actual.id ?: return
        if (cuidando) return
        cuidando = true
        viewModelScope.launch {
            when (val r = repo.usarAbono(id)) {
                is AbonoResultado.Con -> {
                    uiState = GardenUiState.Success(r.planta)
                    alTerminar("Abono aplicado: ${r.planta.name} ganó un día 🌿")
                }
                is AbonoResultado.Fallo -> alTerminar(r.mensaje)
            }
            cuidando = false
        }
    }

    fun cargar() {
        uiState = GardenUiState.Loading
        viewModelScope.launch {
            uiState = when (val r = garden.plantaActiva()) {
                is PlantaActivaResultado.ConPlanta -> GardenUiState.Success(r.planta)
                PlantaActivaResultado.SinPlanta -> GardenUiState.SinPlanta
                is PlantaActivaResultado.Fallo -> GardenUiState.Error(r.mensaje)
            }
        }
    }

    /** Aplica un cuidado en el backend y entrega el mensaje para el usuario. */
    fun cuidar(accion: AccionCuidado, alTerminar: (String) -> Unit) {
        val actual = (uiState as? GardenUiState.Success)?.planta ?: return
        val id = actual.id ?: return
        if (cuidando) return

        valorCompleto(actual, accion)?.let { return alTerminar(it) }

        cuidando = true
        viewModelScope.launch {
            when (val r = garden.cuidar(id, accion)) {
                is PlantaResultado.Con -> {
                    uiState = GardenUiState.Success(r.planta)
                    alTerminar(mensajeDeExito(actual, r.planta, accion))
                }
                is PlantaResultado.Fallo -> alTerminar("No se pudo aplicar el cuidado: ${r.mensaje}")
            }
            cuidando = false
        }
    }

    /** Aviso si esa necesidad ya esta al 100 % (no hace falta llamar al servidor). */
    private fun valorCompleto(p: PlantSummary, accion: AccionCuidado): String? = when (accion) {
        AccionCuidado.REGAR -> "${p.name} ya tiene toda el agua que necesita".takeIf { p.humidityPercent >= 100 }
        AccionCuidado.SOL -> "${p.name} ya recibio toda la luz que necesita".takeIf { p.lightPercent >= 100 }
        AccionCuidado.ABONAR -> "${p.name} ya esta en plena salud".takeIf { p.healthPercent >= 100 }
    }

    private fun mensajeDeExito(antes: PlantSummary, despues: PlantSummary, accion: AccionCuidado): String =
        when (accion) {
            AccionCuidado.REGAR -> "Regaste a ${antes.name}: +${despues.humidityPercent - antes.humidityPercent}% de agua"
            AccionCuidado.SOL -> "${antes.name} tomo sol: +${despues.lightPercent - antes.lightPercent}% de luz"
            AccionCuidado.ABONAR -> "Abonaste a ${antes.name}: +${despues.healthPercent - antes.healthPercent}% de salud"
        }

    companion object {
        val Factory = viewModelFactory { initializer { GardenViewModel(AppContainer.garden, AppContainer.tienda, AppContainer.home) } }
    }
}
