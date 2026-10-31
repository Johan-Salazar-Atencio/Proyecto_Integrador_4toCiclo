package com.plantia.data

import com.plantia.data.remote.ScanResultDto

/**
 * Ultimo resultado de escaneo en memoria, para pasarlo a la pantalla de detalle
 * sin serializarlo en la ruta. Se reemplazara por un ViewModel/repositorio.
 */
object ScanResultStore {
    var ultimo: ScanResultDto? = null
}
