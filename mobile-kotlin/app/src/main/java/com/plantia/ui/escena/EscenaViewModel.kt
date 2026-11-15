package com.plantia.ui.escena

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.plantia.data.model.Arte
import com.plantia.data.repository.AlertaResultado
import com.plantia.data.repository.ArteRepository
import com.plantia.data.repository.HomeRepository
import com.plantia.di.AppContainer
import java.util.Calendar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Mantiene la escena del jardin al dia: el fondo cambia con la hora y la animacion del desastre
 * aparece y desaparece con la alerta. Consulta cada [intervaloMs] mientras la pantalla esta abierta.
 */
class EscenaViewModel(
    private val arteRepo: ArteRepository,
    private val home: HomeRepository,
    private val hora: () -> Int = { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) },
    private val intervaloMs: Long = 20_000
) : ViewModel() {

    /** null mientras no hay arte: el marco se queda con su fondo de color. */
    var escena: Escena? by mutableStateOf(null)
        private set

    private var arte: Arte? = null
    private var tipo: String? = null
    private var fase: String? = null

    init {
        // intervaloMs = 0 desactiva el sondeo (las pruebas llaman a actualizar() a mano)
        if (intervaloMs > 0) {
            viewModelScope.launch {
                while (isActive) {
                    actualizar()
                    delay(intervaloMs)
                }
            }
        }
    }

    suspend fun actualizar() {
        if (arte == null) arte = arteRepo.cargar()
        // Si la consulta de la alerta falla se conserva el desastre que ya se veia
        when (val alerta = home.alerta()) {
            is AlertaResultado.Con -> {
                tipo = alerta.alerta.disasterType
                fase = alerta.alerta.phase
            }
            AlertaResultado.Sin -> {
                tipo = null
                fase = null
            }
            AlertaResultado.Fallo -> Unit
        }
        escena = elegirEscena(arte, hora(), tipo, fase)
    }

    companion object {
        val Factory = viewModelFactory { initializer { EscenaViewModel(AppContainer.arte, AppContainer.home) } }
    }
}
