package com.plantia.util

import java.util.Locale

/** "02:14:35": horas, minutos y segundos para una cuenta regresiva (nunca negativa). */
fun formatoCuentaRegresiva(segundos: Long): String {
    val total = segundos.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
}
