package com.plantia.data

import com.plantia.model.PlantSummary

sealed interface PlantaActivaResultado {
    data class ConPlanta(val planta: PlantSummary) : PlantaActivaResultado
    /** El usuario aun no tiene ninguna planta en crianza (HTTP 204). */
    data object SinPlanta : PlantaActivaResultado
    data class Fallo(val mensaje: String) : PlantaActivaResultado
}

sealed interface PlantaResultado {
    data class Con(val planta: PlantSummary) : PlantaResultado
    data class Fallo(val mensaje: String) : PlantaResultado
}

sealed interface PlantasResultado {
    data class Con(val plantas: List<PlantSummary>) : PlantasResultado
    data class Fallo(val mensaje: String) : PlantasResultado
}

/** Plantas digitales del usuario (Mi jardin): lectura, digitalizar, elegir y cuidar. */
interface GardenRepository {
    suspend fun plantaActiva(): PlantaActivaResultado

    suspend fun listar(): PlantasResultado

    /** Digitaliza desde un escaneo propio (`idEscaneo` > 0) o desde una especie del catalogo. */
    suspend fun digitalizar(idEscaneo: Int, idCatalogo: Int?): PlantaResultado

    /** Elige cual de las plantas digitales se ve en el jardin. */
    suspend fun seleccionar(idPlanta: Int): PlantaResultado

    suspend fun cuidar(idPlanta: Int, accion: AccionCuidado): PlantaResultado
}
