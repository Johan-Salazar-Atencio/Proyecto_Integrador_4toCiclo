package com.plantia.data.logros

import com.plantia.data.model.LogroNuevo
import com.plantia.data.repository.LogrosRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Cuenta con la que esta abierta la app (id y token de [com.plantia.data.local.Sesion]). */
data class SesionActual(val userId: Int, val token: String) {
    val iniciada: Boolean get() = token.isNotBlank()
}

/**
 * Cola de logros por celebrar. Junta los `logrosNuevos` de las acciones y los `pendientes` del servidor,
 * sin repetir ninguno (por `idLogro`), y muestra uno a la vez: el mas raro primero.
 *
 * - Un logro se marca visto (`POST /logros/vistos`) solo cuando [terminar] avisa que su celebracion acabo; si la
 *   app se cierra a la mitad, sigue pendiente en el servidor y se vuelve a celebrar. Si el POST falla se reintenta
 *   en el siguiente [revisar] o [terminar].
 * - Al cerrar sesion o cambiar de cuenta la cola se vacia: nunca se muestran logros de otra cuenta.
 *
 * Todo el estado se toca en el hilo del [scope] (el principal en la app); las llamadas de red las hace el repositorio.
 */
class GestorLogros(
    private val repo: LogrosRepository,
    private val scope: CoroutineScope,
    sesion: Flow<SesionActual>
) {
    private var cola: List<LogroNuevo> = emptyList()

    /** Ids ya celebrados con esta cuenta: aunque su POST aun no llegue, no se vuelven a encolar. */
    private val celebrados = mutableSetOf<Int>()

    /** Celebrados cuyo POST de "vistos" falta o fallo. */
    private val porMarcar = linkedSetOf<Int>()

    /** Sube cada vez que se vacia todo: descarta lo que traian las llamadas de la cuenta anterior. */
    private var generacion = 0
    private var sesionIniciada = false
    private var trabajoRevision: Job? = null

    private val _actual = MutableStateFlow<LogroNuevo?>(null)

    /** El logro que se celebra ahora (null = nada que mostrar). Sigue siendo el mismo hasta llamar a [terminar]. */
    val actual: StateFlow<LogroNuevo?> = _actual

    /** Cuantos faltan por celebrar, contando el actual. */
    val enCola: Int get() = cola.size

    init {
        scope.launch {
            var anterior: SesionActual? = null
            sesion.collect { s ->
                val previa = anterior
                anterior = s
                sesionIniciada = s.iniciada
                if (previa != null && (previa.userId != s.userId || !s.iniciada)) limpiar()
                if (s.iniciada && s != previa) revisarAhora(forzar = true)
            }
        }
    }

    /** Agrega logros de una accion (escaneo, digitalizar, cuidados). Ignora los que ya estan o ya se celebraron. */
    fun encolar(nuevos: List<LogroNuevo>) {
        // Los repositorios llaman desde hilos de red: el estado solo se toca en el hilo del scope
        scope.launch { encolarAhora(nuevos) }
    }

    private fun encolarAhora(nuevos: List<LogroNuevo>) {
        val conocidos = cola.mapTo(mutableSetOf()) { it.idLogro } + celebrados
        val frescos = nuevos.filter { it.idLogro > 0 }.distinctBy { it.idLogro }.filter { it.idLogro !in conocidos }
        if (frescos.isEmpty()) return
        // El que ya se esta mostrando no cambia de lugar
        val cabeza = cola.firstOrNull()
        val resto = (cola.drop(1) + frescos).sortedWith(MAS_RARO_PRIMERO)
        cola = listOfNotNull(cabeza) + resto
        _actual.value = cola.firstOrNull()
    }

    /**
     * La celebracion de [idLogro] termino: sale de la cola, pasa al siguiente y se avisa al servidor.
     * Si [idLogro] no es el que se esta mostrando (doble toque, cierre repetido) no hace nada.
     */
    fun terminar(idLogro: Int) {
        val cabeza = cola.firstOrNull() ?: return
        if (cabeza.idLogro != idLogro) return
        cola = cola.drop(1)
        celebrados += idLogro
        porMarcar += idLogro
        _actual.value = cola.firstOrNull()
        scope.launch { enviarVistos() }
    }

    /**
     * Pide al servidor los logros pendientes y los encola (abrir la app, volver a primer plano, iniciar sesion,
     * comprar...). Sin sesion no hace nada. Antes reintenta los "vistos" que fallaron.
     */
    fun revisar(forzar: Boolean = false) {
        scope.launch { revisarAhora(forzar) }
    }

    private fun revisarAhora(forzar: Boolean) {
        if (!sesionIniciada) return
        if (!forzar && trabajoRevision?.isActive == true) return
        trabajoRevision?.cancel()
        val gen = generacion
        trabajoRevision = scope.launch {
            enviarVistos()
            val pendientes = repo.pendientes() ?: return@launch
            if (gen == generacion) encolarAhora(pendientes)
        }
    }

    /** Vacia la cola y todo lo que sabe de la cuenta anterior. */
    fun limpiar() {
        generacion++
        trabajoRevision?.cancel()
        cola = emptyList()
        celebrados.clear()
        porMarcar.clear()
        enviando = false
        _actual.value = null
    }

    private var enviando = false

    private suspend fun enviarVistos() {
        if (enviando) return
        enviando = true
        val gen = generacion
        try {
            while (gen == generacion) {
                val ids = porMarcar.toList()
                if (ids.isEmpty()) return
                if (!repo.marcarVistos(ids) || gen != generacion) return
                porMarcar.removeAll(ids.toSet())
            }
        } finally {
            if (gen == generacion) enviando = false
        }
    }

    private companion object {
        /** Legendaria antes que epica, rara y comun; a igual rareza, por id. */
        val MAS_RARO_PRIMERO = compareByDescending<LogroNuevo> { it.rareza.ordinal }.thenBy { it.idLogro }
    }
}
