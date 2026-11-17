package com.plantia.ui.misiones

import com.plantia.data.model.DailyMission

/** A donde lleva una mision: el escaner o las plantas virtuales (donde se riega, se da luz, se abona y se previene). */
enum class DestinoMision { ESCANEAR, PLANTAS_VIRTUALES }

/**
 * Pantalla donde se cumple una mision. La del evento y las de cuidado van a las plantas virtuales: ahi se entra a
 * cada planta, una por una, y se toca "Prevenir evento" o el cuidado. Las de escanear, digitalizar y ampliar la
 * coleccion empiezan en el escaner. Un tipo que la app no conoce no lleva a ningun lado (null).
 */
fun destinoDeMision(mision: DailyMission): DestinoMision? = when {
    mision.esEvento -> DestinoMision.PLANTAS_VIRTUALES
    else -> when (mision.type) {
        "ESCANEO", "DIGITALIZAR", "COLECCION" -> DestinoMision.ESCANEAR
        "RIEGO", "SOL", "ABONO", "CUIDADO", "REFUGIO" -> DestinoMision.PLANTAS_VIRTUALES
        else -> null
    }
}
