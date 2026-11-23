package com.plantia.data.repository

import com.google.gson.JsonParser
import retrofit2.Response

/** Spring responde los errores como {"error": "..."}; este es el texto que se le muestra al usuario. */
internal fun Response<*>.mensajeDelServidor(): String {
    val texto = runCatching { errorBody()?.string() }.getOrNull()
    return mensajeDeError(texto) ?: "El servidor respondio ${code()}"
}

/** El texto de {"error": "..."} de un cuerpo de error; null si no lo trae. */
internal fun mensajeDeError(cuerpo: String?): String? =
    runCatching { JsonParser.parseString(cuerpo).asJsonObject.get("error").asString }.getOrNull()?.takeIf { it.isNotBlank() }

/** Error con el mensaje que mando el servidor (QA-36: antes la app mostraba "HTTP 429"). */
class ErrorDelServidor(mensaje: String, val codigo: Int) : Exception(mensaje)

/** Pasa un HttpException de Retrofit a un error con el mensaje del servidor, o uno claro segun el codigo. */
internal fun retrofit2.HttpException.conMensajeDelServidor(): ErrorDelServidor {
    val texto = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val mensaje = mensajeDeError(texto) ?: when (code()) {
        429 -> "Hay muchos escaneos en este momento. Espera un minuto y vuelve a intentarlo."
        413 -> "La foto es demasiado grande."
        415 -> "La foto no es una imagen válida."
        else -> "El servidor respondió ${code()}"
    }
    return ErrorDelServidor(mensaje, code())
}
