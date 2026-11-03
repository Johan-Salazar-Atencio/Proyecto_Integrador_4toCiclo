package com.plantia.data

import com.plantia.data.remote.ScanResultDto
import com.plantia.model.PlantSummary

/** Acciones de cuidado del jardin. `delta` es el aporte simulado al estado de la planta. */
enum class AccionCuidado(val etiqueta: String, val pista: String, val delta: Int) {
    REGAR("Regar", "+20%", 20),
    SOL("Sol", "+15%", 15),
    ABONAR("Abonar", "+10%", 10)
}

/** Resultado de aplicar un cuidado: planta actualizada y mensaje para el usuario. */
data class ResultadoCuidado(val planta: PlantSummary, val mensaje: String)

/**
 * SIMULACION de las plantas digitales y sus cuidados.
 *
 * El backend todavia no tiene: crear una planta digital a partir de un escaneo, un endpoint
 * para regar / dar sol / abonar, ni un proceso que cambie los porcentajes con el tiempo
 * (solo existe GET /api/v1/plantas/activa). Mientras tanto todo esto vive aqui, en memoria,
 * hasta que se cierre la app.
 *
 * TODO(backend): reemplazar `digitalizar`, `activar` y `aplicarCuidado` por llamadas reales
 * y borrar `aplicarAjustes`.
 */
object MockGardenData {

    // Ajustes sobre la planta que devuelve el servidor (la semilla), si no hay digitales.
    private var ajusteSalud = 0
    private var ajusteAgua = 0
    private var ajusteLuz = 0
    private var regadaEnEstaSesion = false

    // Coleccion de plantas digitales (las que pasaron por "Digitalizar"), la mas nueva primero.
    private val digitales = mutableListOf<PlantSummary>()
    private var nombreActiva: String? = null

    val plantasDigitales: List<PlantSummary> get() = digitales.toList()

    /** Planta digital que se muestra en el jardin, si hay alguna. */
    val plantaDigital: PlantSummary? get() = digitales.firstOrNull { it.name == nombreActiva }

    val nombreDeLaActiva: String? get() = nombreActiva

    /** Planta de ejemplo para Previews. */
    val plantaSimulada = PlantSummary(
        name = "Monstera",
        species = "Monstera deliciosa",
        healthPercent = 80,
        humidityPercent = 60,
        lightPercent = 90,
        lastWatered = "Hace 2 dias"
    )

    /** Planta con valores bajos para probar los colores de alerta. */
    val plantaSedienta = plantaSimulada.copy(healthPercent = 35, humidityPercent = 15, lightPercent = 55)

    /**
     * SIMULA "Digitalizar": la planta escaneada se suma a las digitales y pasa a ser la activa.
     * Si ya existia una con el mismo nombre, se reemplaza por una nueva.
     */
    fun digitalizar(escaneo: ScanResultDto) {
        val nueva = PlantSummary(
            name = escaneo.nombreComun?.takeIf { it.isNotBlank() } ?: "Mi planta",
            species = escaneo.nombreCientifico.orEmpty(),
            healthPercent = 90,
            humidityPercent = 60,
            lightPercent = 70,
            lastWatered = "Recien digitalizada",
            // Mientras no haya sprites, se muestra la foto del escaneo.
            imageUrl = escaneo.imagenUrl
        )
        digitales.removeAll { it.name == nueva.name }
        digitales.add(0, nueva)
        nombreActiva = nueva.name
    }

    /** Borra todo lo simulado en memoria (al cerrar sesion). */
    fun reiniciar() {
        digitales.clear()
        nombreActiva = null
        ajusteSalud = 0
        ajusteAgua = 0
        ajusteLuz = 0
        regadaEnEstaSesion = false
    }

    /** Elige cual de las plantas digitales se ve en el jardin. */
    fun activar(nombre: String) {
        if (digitales.any { it.name == nombre }) nombreActiva = nombre
    }

    /** Suma los cuidados simulados a lo que devolvio el servidor (solo la planta semilla). */
    fun aplicarAjustes(base: PlantSummary): PlantSummary = base.copy(
        healthPercent = (base.healthPercent + ajusteSalud).coerceIn(0, 100),
        humidityPercent = (base.humidityPercent + ajusteAgua).coerceIn(0, 100),
        lightPercent = (base.lightPercent + ajusteLuz).coerceIn(0, 100),
        lastWatered = if (regadaEnEstaSesion) "Hace unos minutos" else base.lastWatered
    )

    /** Aplica un cuidado sobre la planta que se esta mostrando. */
    fun aplicarCuidado(actual: PlantSummary, accion: AccionCuidado): ResultadoCuidado {
        val nombre = actual.name
        val nueva: PlantSummary = when (accion) {
            AccionCuidado.REGAR -> {
                if (actual.humidityPercent >= 100) {
                    return ResultadoCuidado(actual, "$nombre ya tiene toda el agua que necesita")
                }
                actual.copy(
                    humidityPercent = (actual.humidityPercent + accion.delta).coerceAtMost(100),
                    lastWatered = "Hace unos minutos"
                )
            }
            AccionCuidado.SOL -> {
                if (actual.lightPercent >= 100) {
                    return ResultadoCuidado(actual, "$nombre ya recibio toda la luz que necesita")
                }
                actual.copy(lightPercent = (actual.lightPercent + accion.delta).coerceAtMost(100))
            }
            AccionCuidado.ABONAR -> {
                if (actual.healthPercent >= 100) {
                    return ResultadoCuidado(actual, "$nombre ya esta en plena salud")
                }
                actual.copy(healthPercent = (actual.healthPercent + accion.delta).coerceAtMost(100))
            }
        }

        val indice = digitales.indexOfFirst { it.name == nombre }
        if (indice >= 0) {
            digitales[indice] = nueva
        } else {
            // Planta del servidor: se recuerda el aporte para reaplicarlo al recargar.
            when (accion) {
                AccionCuidado.REGAR -> { ajusteAgua += accion.delta; regadaEnEstaSesion = true }
                AccionCuidado.SOL -> ajusteLuz += accion.delta
                AccionCuidado.ABONAR -> ajusteSalud += accion.delta
            }
        }

        val mensaje = when (accion) {
            AccionCuidado.REGAR -> "Regaste a $nombre: +${accion.delta}% de agua"
            AccionCuidado.SOL -> "$nombre tomo sol: +${accion.delta}% de luz"
            AccionCuidado.ABONAR -> "Abonaste a $nombre: +${accion.delta}% de salud"
        }
        return ResultadoCuidado(nueva, mensaje)
    }
}
