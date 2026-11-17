package com.plantia.data.repository

import com.plantia.data.remote.dto.ColeccionItemDto
import com.plantia.data.remote.dto.ScanResultDto

sealed interface ColeccionResultado {
    data class Con(val plantas: List<ColeccionItemDto>) : ColeccionResultado
    data class Fallo(val mensaje: String) : ColeccionResultado
}

sealed interface DetalleColeccionResultado {
    data class Con(val planta: ScanResultDto) : DetalleColeccionResultado
    data class Fallo(val mensaje: String) : DetalleColeccionResultado
}

/** Coleccion de plantas escaneadas por el usuario. */
interface CollectionRepository {
    suspend fun listar(): ColeccionResultado

    /** Agrega una planta a la coleccion (idempotente). */
    suspend fun agregar(idEscaneo: Int): ColeccionResultado

    /** Detalle de una planta guardada, con la misma forma que un escaneo (sin porcentaje de IA). */
    suspend fun detalle(id: Int): DetalleColeccionResultado
}
