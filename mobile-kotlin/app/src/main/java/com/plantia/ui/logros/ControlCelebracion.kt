package com.plantia.ui.logros

import com.plantia.data.logros.GestorLogros
import com.plantia.data.model.LogroNuevo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** El aviso de arriba que baja cuando termina la animacion de un logro. [serie] distingue un aviso de otro. */
data class AvisoVisible(val logro: LogroNuevo, val serie: Int, val entro: Boolean = false)

/**
 * Lo que decide la celebracion fuera del dibujo ("D + aviso al terminar"):
 *
 * - Al TERMINAR la animacion de la medalla baja el aviso arriba ([alTerminarAnimacion]); se quita solo a los
 *   [DURACION_AVISO_MS] (o [DURACION_AVISO_REDUCIDO_MS] con animaciones quitadas) o con un toque ([quitarAviso]).
 * - Si se cierra la medalla el aviso sigue sobre la app hasta su tiempo.
 * - Si empieza otra celebracion ([alEmpezar]) el aviso anterior se quita, para no amontonarlos.
 * - El logro se marca visto solo al CERRAR la medalla ([cerrar]), nunca al terminar la animacion.
 */
class ControlCelebracion(
    private val gestor: GestorLogros,
    private val scope: CoroutineScope
) {
    private val _aviso = MutableStateFlow<AvisoVisible?>(null)
    val aviso: StateFlow<AvisoVisible?> = _aviso

    private var serie = 0
    private var mostrando: Any? = null
    private var temporizador: Job? = null

    /** Empieza a pintarse la celebracion [clave] (medalla o resumen). Si es otra distinta, se quita el aviso. */
    fun alEmpezar(clave: Any?) {
        if (clave == null) {
            // Ya no se muestra nada: el aviso se queda sobre la app hasta su tiempo
            mostrando = null
            return
        }
        if (clave == mostrando) return
        mostrando = clave
        ocultar()
    }

    /** La animacion de la medalla de [logro] termino: baja el aviso. */
    fun alTerminarAnimacion(logro: LogroNuevo, reducido: Boolean) {
        val nuevo = AvisoVisible(logro, ++serie)
        _aviso.value = nuevo
        temporizador?.cancel()
        temporizador = scope.launch {
            delay(duracionAvisoMs(reducido))
            quitarAviso(nuevo.serie)
        }
    }

    /** La animacion de entrada del aviso ya se vio: si cambia de capa (al cerrar la medalla) no vuelve a bajar. */
    fun marcarEntrada(serie: Int) {
        val a = _aviso.value ?: return
        if (a.serie == serie && !a.entro) _aviso.value = a.copy(entro = true)
    }

    /** Toque en el aviso o fin de su tiempo. */
    fun quitarAviso(serie: Int) {
        if (_aviso.value?.serie == serie) ocultar()
    }

    private fun ocultar() {
        temporizador?.cancel()
        temporizador = null
        _aviso.value = null
    }

    /** El usuario cerro la medalla de [logro]: ahora si se marca visto. */
    fun cerrar(logro: LogroNuevo) = gestor.terminar(logro.idLogro)

    /** Cerro la tarjeta resumen: se marcan vistos todos los que mostraba. */
    fun cerrarResumen(logros: List<LogroNuevo>) = gestor.terminarVarios(logros.map { it.idLogro })

    /** "Saltar todos": se cierra la cola entera. */
    fun saltarTodos() = gestor.terminarVarios(gestor.idsEnCola())

    companion object {
        const val DURACION_AVISO_MS = 4_000L
        const val DURACION_AVISO_REDUCIDO_MS = 6_000L

        fun duracionAvisoMs(reducido: Boolean) = if (reducido) DURACION_AVISO_REDUCIDO_MS else DURACION_AVISO_MS
    }
}
