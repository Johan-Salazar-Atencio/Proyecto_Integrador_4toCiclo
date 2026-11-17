package com.plantia.data.repository

import com.google.gson.JsonParser
import retrofit2.Response

/** Spring responde los errores como {"error": "..."}; este es el texto que se le muestra al usuario. */
internal fun Response<*>.mensajeDelServidor(): String {
    val texto = runCatching { errorBody()?.string() }.getOrNull()
    val error = runCatching { JsonParser.parseString(texto).asJsonObject.get("error").asString }.getOrNull()
    return error ?: "El servidor respondio ${code()}"
}
