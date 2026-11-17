package com.plantia.util

import com.plantia.data.remote.RetrofitClient

/**
 * El backend arma algunas URLs con `localhost` (fotos de escaneos). Se cambia el host por el que
 * realmente responde para que las imagenes tambien carguen por WiFi.
 */
fun normalizarUrlLocal(url: String?): String? {
    if (url.isNullOrBlank()) return null
    return RetrofitClient.urlAbsoluta(url) ?: url
}
