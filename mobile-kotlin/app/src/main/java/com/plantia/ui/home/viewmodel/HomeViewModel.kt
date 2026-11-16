package com.plantia.ui.home.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.local.Sesion
import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.DailyMission
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.QuickAccessItem
import com.plantia.data.model.UserSummary
import com.plantia.data.model.WeatherAlert
import com.plantia.data.repository.AlertaResultado
import com.plantia.data.repository.ColeccionResultado
import com.plantia.data.repository.CollectionRepository
import com.plantia.data.repository.GardenRepository
import com.plantia.data.repository.HomeRepository
import com.plantia.data.repository.PlantaActivaResultado
import com.plantia.data.repository.PlantaResultado
import com.plantia.data.repository.PlantasResultado
import com.plantia.di.AppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Estado de la pantalla Home. Cada card se dibuja solo si tiene datos. */
data class HomeUiState(
    val cargando: Boolean = true,
    val usuario: UserSummary? = null,
    val alerta: WeatherAlert? = null,
    val planta: PlantSummary? = null,
    /** Plantas activas (fuera del baul) entre las que se pasa con las flechas del Inicio. */
    val plantas: List<PlantSummary> = emptyList(),
    val accesosServidor: List<QuickAccessItem> = emptyList(),
    val totalColeccion: Int? = null,
    val misiones: List<DailyMission> = emptyList(),
    val error: String? = null
) {
    /** Accesos rapidos sin el Jardin (se entra desde la tarjeta de la planta) y con el total de la coleccion. */
    val accesos: List<QuickAccessItem>
        get() = accesosServidor
            .filterNot { it.id == "garden" || it.id == "jardin" }
            .map { item ->
                if (item.id != "collection" || totalColeccion == null) {
                    item
                } else {
                    item.copy(subtitle = subtituloColeccion(totalColeccion))
                }
            }

    private fun subtituloColeccion(total: Int): String = when (total) {
        0 -> "Aun sin plantas"
        1 -> "1 planta descubierta"
        else -> "$total plantas descubiertas"
    }
}

/** Cada cuanto se consulta el servidor mientras el Home esta visible. */
const val INTERVALO_SONDEO_MS = 20_000L

class HomeViewModel(
    private val home: HomeRepository,
    private val collection: CollectionRepository,
    private val garden: GardenRepository
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    /** Mensaje breve para el usuario (por ejemplo, la subida de nivel); la pantalla lo muestra y llama a [avisoMostrado]. */
    var aviso by mutableStateOf<String?>(null)
        private set

    fun avisoMostrado() {
        aviso = null
    }

    init {
        // Si cambia el usuario de la sesion, se recarga todo el Home.
        viewModelScope.launch {
            Sesion.userId.collect { cargar() }
        }
    }

    fun recargar() = cargar()

    /** Actualiza solo el total de la coleccion (al volver al Home tras escanear o guardar). */
    fun refrescarColeccion() {
        viewModelScope.launch {
            uiState = uiState.copy(totalColeccion = totalColeccion())
        }
    }

    /**
     * Mantiene al dia la alerta, el perfil y la planta mientras el Home esta visible: un desastre que
     * lanza el admin aparece solo, sin volver a abrir la app. Se cancela con la pantalla.
     */
    suspend fun sondear(intervaloMs: Long = INTERVALO_SONDEO_MS) {
        while (true) {
            actualizarEnVivo()
            delay(intervaloMs)
        }
    }

    /**
     * Refresca solo lo que cambia con el tiempo. Un fallo de red no borra lo que ya se ve:
     * se reintenta en el siguiente ciclo.
     */
    suspend fun actualizarEnVivo() {
        if (uiState.cargando) return
        val alerta = home.alerta()
        val usuario = home.usuario()
        val planta = garden.plantaActiva()
        val misiones = home.misiones()

        var nuevo = uiState
        nuevo = when (alerta) {
            is AlertaResultado.Con -> nuevo.copy(alerta = alerta.alerta)
            AlertaResultado.Sin -> nuevo.copy(alerta = null)
            AlertaResultado.Fallo -> nuevo
        }
        if (misiones != null) nuevo = nuevo.copy(misiones = misiones)
        if (usuario != null) {
            // Un nivel mas que antes: el XP llego de un escaneo, un cuidado o un desastre superado
            val nivelAntes = nuevo.usuario?.level
            if (nivelAntes != null && usuario.level > nivelAntes) {
                aviso = "Subiste al nivel ${usuario.level}"
            }
            nuevo = nuevo.copy(usuario = usuario)
        }
        if (planta is PlantaActivaResultado.ConPlanta) {
            nuevo = nuevo.copy(planta = fusionar(nuevo.planta, planta.planta))
        }
        (garden.listar() as? PlantasResultado.Con)?.let { nuevo = nuevo.copy(plantas = activas(it.plantas)) }
        uiState = nuevo
    }

    /**
     * Misma planta: solo se copia lo que cambia (salud, agua, luz, proteccion y recuperacion) y se conserva su imagen,
     * que el Home resolvio con otra ruta. Otra planta: se reemplaza completa.
     */
    private fun fusionar(actual: PlantSummary?, remota: PlantSummary): PlantSummary =
        if (actual != null && actual.id == remota.id) {
            actual.copy(
                healthPercent = remota.healthPercent,
                humidityPercent = remota.humidityPercent,
                lightPercent = remota.lightPercent,
                lastWatered = remota.lastWatered,
                refugiada = remota.refugiada,
                fase = remota.fase,
                enRecuperacion = remota.enRecuperacion,
                recuperacionSegundos = remota.recuperacionSegundos,
                golpeTipo = remota.golpeTipo,
                diasCuidados = remota.diasCuidados,
                diasTotales = remota.diasTotales,
                apariencia = remota.apariencia ?: actual.apariencia,
                macetaCodigo = remota.macetaCodigo
            )
        } else {
            remota
        }

    private fun cargar() {
        viewModelScope.launch {
            uiState = uiState.copy(cargando = true, error = null)
            val datos = home.cargar()
            // La planta del jardin llega completa (dias, apariencia, maceta) por el repositorio del jardin
            // y conserva la imagen que el Home resolvio con otra ruta
            val completa = (garden.plantaActiva() as? PlantaActivaResultado.ConPlanta)?.planta
                ?.let { it.copy(imageUrl = datos.planta?.imageUrl ?: it.imageUrl) } ?: datos.planta
            val lista = (garden.listar() as? PlantasResultado.Con)?.plantas?.let(::activas).orEmpty()
            uiState = HomeUiState(
                cargando = false,
                usuario = datos.usuario,
                alerta = datos.alerta,
                planta = completa,
                plantas = lista,
                accesosServidor = datos.accesos,
                totalColeccion = totalColeccion(),
                misiones = datos.misiones,
                error = when {
                    datos.sinConexion -> "No se pudo conectar con el servidor"
                    datos.fallidas > 0 -> "Algunas secciones no se pudieron cargar"
                    else -> null
                }
            )
        }
    }

    private fun activas(todas: List<PlantSummary>) = todas.filterNot { it.enBaul }

    /** Pasa a la planta anterior o siguiente de la lista y la deja como la del jardin. */
    fun anterior() = mover(-1)
    fun siguiente() = mover(+1)

    private fun mover(paso: Int) {
        val lista = uiState.plantas
        if (lista.size < 2) return
        val actual = lista.indexOfFirst { it.id == uiState.planta?.id }.coerceAtLeast(0)
        val id = lista[(actual + paso + lista.size) % lista.size].id ?: return
        viewModelScope.launch {
            when (val r = garden.seleccionar(id)) {
                is PlantaResultado.Con -> uiState = uiState.copy(planta = r.planta)
                is PlantaResultado.Fallo -> aviso = r.mensaje
            }
        }
    }

    private var cuidando = false

    /** Riega, da luz o abona a la planta que se ve en el Inicio y avisa el resultado. */
    fun cuidar(accion: AccionCuidado) {
        val actual = uiState.planta ?: return
        val id = actual.id ?: return
        if (cuidando) return
        val lleno = when (accion) {
            AccionCuidado.REGAR -> actual.humidityPercent >= 100
            AccionCuidado.SOL -> actual.lightPercent >= 100
            AccionCuidado.ABONAR -> actual.healthPercent >= 100
        }
        if (lleno) {
            aviso = "${actual.name} ya tiene todo lo que necesita"
            return
        }
        cuidando = true
        viewModelScope.launch {
            when (val r = garden.cuidar(id, accion)) {
                is PlantaResultado.Con -> {
                    uiState = uiState.copy(planta = r.planta)
                    aviso = "${accion.etiqueta}: ${actual.name} mejora"
                }
                is PlantaResultado.Fallo -> aviso = "No se pudo aplicar el cuidado: ${r.mensaje}"
            }
            cuidando = false
        }
    }

    private suspend fun totalColeccion(): Int? =
        (collection.listar() as? ColeccionResultado.Con)?.plantas?.size

    companion object {
        val Factory = viewModelFactory {
            initializer { HomeViewModel(AppContainer.home, AppContainer.collection, AppContainer.garden) }
        }
    }
}
