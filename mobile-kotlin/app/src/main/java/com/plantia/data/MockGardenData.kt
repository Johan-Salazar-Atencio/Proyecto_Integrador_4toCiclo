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
 * SIMULACION de los cuidados del jardin.
 *
 * El backend todavia no tiene un endpoint para regar / dar sol / abonar (solo existe
 * GET /api/v1/plantas/activa) ni un proceso que cambie los porcentajes con el tiempo.
 * Mientras tanto los cuidados se aplican aqui, en memoria: se suman sobre lo que devuelve el
 * servidor y duran hasta que se cierre la app.
 *
 * TODO(backend): cuando exista el endpoint de cuidados, reemplazar `aplicarCuidado` por la
 * llamada real y borrar `aplicarAjustes`.
 */
object MockGardenData {

    private var ajusteSalud = 0
    private var ajusteAgua = 0
    private var ajusteLuz = 0
    private var regadaEnEstaSesion = false

    /** Planta digital creada con "Digitalizar" (simulada); si existe, el jardin la muestra. */
    var plantaDigital: PlantSummary? = null
        private set

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
     * SIMULA "Digitalizar": la planta escaneada pasa a ser la mascota del jardin.
     * TODO(backend): reemplazar por la llamada real cuando exista el endpoint que crea
     * la planta en crianza a partir de un escaneo.
     */
    fun digitalizar(escaneo: ScanResultDto) {
        ajusteSalud = 0
        ajusteAgua = 0
        ajusteLuz = 0
        regadaEnEstaSesion = false
        plantaDigital = PlantSummary(
            name = escaneo.nombreComun?.takeIf { it.isNotBlank() } ?: "Mi planta",
            species = escaneo.nombreCientifico.orEmpty(),
            healthPercent = 90,
            humidityPercent = 60,
            lightPercent = 70,
            lastWatered = "Recien digitalizada",
            // Mientras no haya sprites, se muestra la foto del escaneo.
            imageUrl = escaneo.imagenUrl
        )
    }

    /** Suma los cuidados simulados a lo que devolvio el servidor. */
    fun aplicarAjustes(base: PlantSummary): PlantSummary = base.copy(
        healthPercent = (base.healthPercent + ajusteSalud).coerceIn(0, 100),
        humidityPercent = (base.humidityPercent + ajusteAgua).coerceIn(0, 100),
        lightPercent = (base.lightPercent + ajusteLuz).coerceIn(0, 100),
        lastWatered = if (regadaEnEstaSesion) "Hace unos minutos" else base.lastWatered
    )

    /** Aplica un cuidado sobre la planta que se esta mostrando. */
    fun aplicarCuidado(actual: PlantSummary, accion: AccionCuidado): ResultadoCuidado {
        val nombre = actual.name
        return when (accion) {
            AccionCuidado.REGAR -> {
                if (actual.humidityPercent >= 100) {
                    return ResultadoCuidado(actual, "$nombre ya tiene toda el agua que necesita")
                }
                ajusteAgua += accion.delta
                regadaEnEstaSesion = true
                ResultadoCuidado(
                    actual.copy(
                        humidityPercent = (actual.humidityPercent + accion.delta).coerceAtMost(100),
                        lastWatered = "Hace unos minutos"
                    ),
                    "Regaste a $nombre: +${accion.delta}% de agua"
                )
            }
            AccionCuidado.SOL -> {
                if (actual.lightPercent >= 100) {
                    return ResultadoCuidado(actual, "$nombre ya recibio toda la luz que necesita")
                }
                ajusteLuz += accion.delta
                ResultadoCuidado(
                    actual.copy(lightPercent = (actual.lightPercent + accion.delta).coerceAtMost(100)),
                    "$nombre tomo sol: +${accion.delta}% de luz"
                )
            }
            AccionCuidado.ABONAR -> {
                if (actual.healthPercent >= 100) {
                    return ResultadoCuidado(actual, "$nombre ya esta en plena salud")
                }
                ajusteSalud += accion.delta
                ResultadoCuidado(
                    actual.copy(healthPercent = (actual.healthPercent + accion.delta).coerceAtMost(100)),
                    "Abonaste a $nombre: +${accion.delta}% de salud"
                )
            }
        }
    }
}
