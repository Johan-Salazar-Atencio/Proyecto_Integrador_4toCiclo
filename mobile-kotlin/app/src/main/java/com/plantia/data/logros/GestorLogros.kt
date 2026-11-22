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

/** Lo que toca mostrar ahora en la celebracion. */
sealed interface Celebracion {
    /** Un logro con su medalla. [numero] de [total] cuenta los de esta tanda (para el "2 de 5"). */
    data class Una(val logro: LogroNuevo, val numero: Int, val total: Int) : Celebracion {
        /** Cuantos quedan despues de este. */
        val quedanDespues: Int get() = total - numero
    }

    /** Tarjeta resumen con el resto de la tanda (desde el 4.º logro, tras celebrar los 2 mas raros). */
    data class Resumen(val logros: List<LogroNuevo>) : Celebracion
}

/**
 * Cola de logros por celebrar. Junta los `logrosNuevos` de las acciones y los `pendientes` del servidor,
 * sin repetir ninguno (por `idLogro`), y muestra uno a la vez: el mas raro primero.
 *
 * - Un logro se marca visto (`POST /logros/vistos`) solo cuando [terminar] (o [terminarVarios]) avisa que su
 *   celebracion se cerro; si la app se cierra a la mitad, sigue pendiente en el servidor y se vuelve a celebrar.
 *   Si el POST falla se reintenta en el siguiente [revisar] o [terminar]. Se envian en lotes de [LOTE_VISTOS] ids
 *   como maximo (el limite del servidor).
 * - Tanda: los logros que se juntan sin que la cola llegue a vaciarse. Con 1–3 se celebran todos; con 4 o mas se
 *   celebran los 2 mas raros y el resto sale en una tarjeta resumen ([celebracion]).
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

    /** Celebrados (cerrados) desde que la cola dejo de estar vacia por ultima vez. */
    private var celebradosEnTanda = 0

    private val _actual = MutableStateFlow<LogroNuevo?>(null)

    /** El logro que se celebra ahora (null = nada que mostrar). Sigue siendo el mismo hasta llamar a [terminar]. */
    val actual: StateFlow<LogroNuevo?> = _actual

    private val _celebracion = MutableStateFlow<Celebracion?>(null)

    /** Que pintar: la medalla del logro actual o la tarjeta resumen (null = nada). */
    val celebracion: StateFlow<Celebracion?> = _celebracion

    /** Cuantos faltan por celebrar, contando el actual. */
    val enCola: Int get() = cola.size

    private fun publicar() {
        if (cola.isEmpty()) celebradosEnTanda = 0
        val cabeza = cola.firstOrNull()
        val previa = _celebracion.value
        val plan = planCelebracion(cola, celebradosEnTanda)
        // La medalla que ya se esta mostrando no se cambia por el resumen aunque lleguen mas logros
        _celebracion.value =
            if (plan is Celebracion.Resumen && previa is Celebracion.Una && previa.logro.idLogro == cabeza?.idLogro) {
                previa.copy(total = celebradosEnTanda + cola.size)
            } else plan
        _actual.value = cabeza
    }

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
        publicar()
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
        celebradosEnTanda++
        publicar()
        scope.launch { enviarVistos() }
    }

    /**
     * Cierra de golpe varios logros de la cola (tarjeta resumen, "Saltar todos"): salen todos, se marcan vistos
     * y la cola sigue con lo que quede. Los ids que no estan en la cola se ignoran.
     */
    fun terminarVarios(ids: Collection<Int>) {
        val quitar = ids.toSet()
        val salen = cola.filter { it.idLogro in quitar }
        if (salen.isEmpty()) return
        cola = cola.filter { it.idLogro !in quitar }
        salen.forEach {
            celebrados += it.idLogro
            porMarcar += it.idLogro
        }
        celebradosEnTanda += salen.size
        publicar()
        scope.launch { enviarVistos() }
    }

    /** Ids de toda la cola, en el orden en que se celebrarian (para "Saltar todos"). */
    fun idsEnCola(): List<Int> = cola.map { it.idLogro }

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
        celebradosEnTanda = 0
        _actual.value = null
        _celebracion.value = null
    }

    private var enviando = false

    private suspend fun enviarVistos() {
        if (enviando) return
        enviando = true
        val gen = generacion
        try {
            while (gen == generacion) {
                // El servidor acepta como maximo LOTE_VISTOS ids por llamada
                val ids = porMarcar.take(LOTE_VISTOS)
                if (ids.isEmpty()) return
                if (!repo.marcarVistos(ids) || gen != generacion) return
                porMarcar.removeAll(ids.toSet())
            }
        } finally {
            if (gen == generacion) enviando = false
        }
    }

    companion object {
        /** Maximo de ids por `POST /logros/vistos` (lo valida el servidor). */
        const val LOTE_VISTOS = 100

        /** Con cuantos logros celebrados en la tanda se pasa al resumen (si quedan [MIN_RESUMEN] o mas). */
        const val CELEBRADOS_ANTES_DEL_RESUMEN = 2

        /** El resumen solo sale si quedan al menos estos: con 1–3 logros en total se celebran todos. */
        const val MIN_RESUMEN = 2

        /** Legendaria antes que epica, rara y comun; a igual rareza, por id. */
        private val MAS_RARO_PRIMERO = compareByDescending<LogroNuevo> { it.rareza.ordinal }.thenBy { it.idLogro }

        /**
         * Regla del resumen: ya se celebraron 2 en esta tanda y quedan 2 o mas -> tarjeta resumen con el resto.
         * Asi 1–3 logros se celebran todos y desde el 4.º se celebran los 2 mas raros y luego el resumen.
         */
        fun planCelebracion(cola: List<LogroNuevo>, celebradosEnTanda: Int): Celebracion? {
            val cabeza = cola.firstOrNull() ?: return null
            return if (celebradosEnTanda >= CELEBRADOS_ANTES_DEL_RESUMEN && cola.size >= MIN_RESUMEN) {
                Celebracion.Resumen(cola)
            } else {
                Celebracion.Una(cabeza, numero = celebradosEnTanda + 1, total = celebradosEnTanda + cola.size)
            }
        }
    }
}
