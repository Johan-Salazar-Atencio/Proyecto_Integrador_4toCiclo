package com.plantia.data.local

/** Datos de la app que otros procesos de fondo (el chequeo de logros) necesitan saber. */
object EstadoApp {
    /** true mientras hay una pantalla de Plantia visible: ahi los logros se celebran dentro de la app. */
    @Volatile
    var enPrimerPlano: Boolean = false
}
