package com.plantia.data.repository

import com.plantia.data.model.RecentScan
import com.plantia.data.remote.dto.ScanResultDto
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/** Escaneo de plantas con la camara y su historial. */
interface ScanRepository {

    /** Ultimo resultado de escaneo, para mostrarlo en la ficha sin pasarlo por la ruta. */
    val ultimoResultado: StateFlow<ScanResultDto?>

    /** Sube la foto, espera la identificacion de la IA y guarda el resultado en [ultimoResultado]. */
    suspend fun escanear(foto: File): ScanResultDto

    suspend fun recientes(): List<RecentScan>

    fun limpiarUltimo()
}
