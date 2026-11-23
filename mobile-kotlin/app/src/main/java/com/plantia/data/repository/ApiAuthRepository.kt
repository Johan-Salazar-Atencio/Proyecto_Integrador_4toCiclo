package com.plantia.data.repository

import com.plantia.data.local.Sesion
import com.plantia.data.remote.RetrofitClient
import com.plantia.data.remote.dto.AuthLoginRequestDto
import com.plantia.data.remote.dto.AuthRegistroRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class ApiAuthRepository : AuthRepository {

    override suspend fun registrar(nombre: String, email: String, password: String): AuthResultado {
        return withContext(Dispatchers.IO) {
            // La primera vez RetrofitClient sondea la red: nunca en el hilo principal.
            val api = RetrofitClient.api
            try {
                val response = api.registrar(
                    AuthRegistroRequestDto(
                        nombre = nombre.trim(),
                        email = email.trim().lowercase(),
                        password = password
                    )
                )
                AuthResultado.Ok(iniciarSesionLocal(response.id, response.nombre, response.email, response.sessionToken))
            } catch (e: HttpException) {
                val mensaje = when (e.code()) {
                    409 -> "Ese correo ya está registrado. Prueba iniciando sesión."
                    400 -> extraerMensaje(e) ?: "Datos inválidos. Revisa los campos."
                    else -> extraerMensaje(e) ?: "No se pudo completar el registro."
                }
                AuthResultado.Error(mensaje)
            } catch (e: IOException) {
                AuthResultado.Error("No se pudo conectar con el servidor.")
            } catch (e: Exception) {
                AuthResultado.Error("Ocurrió un error inesperado.")
            }
        }
    }

    override suspend fun iniciarSesion(email: String, password: String): AuthResultado {
        return withContext(Dispatchers.IO) {
            val api = RetrofitClient.api
            try {
                val response = api.login(
                    AuthLoginRequestDto(
                        email = email.trim().lowercase(),
                        password = password
                    )
                )
                AuthResultado.Ok(iniciarSesionLocal(response.id, response.nombre, response.email, response.sessionToken))
            } catch (e: HttpException) {
                val mensaje = mensajeDeLogin(e.code(), runCatching { e.response()?.errorBody()?.string() }.getOrNull())
                AuthResultado.Error(mensaje)
            } catch (e: IOException) {
                AuthResultado.Error("No se pudo conectar con el servidor.")
            } catch (e: Exception) {
                AuthResultado.Error("Ocurrió un error inesperado.")
            }
        }
    }

    /** Guarda el id y el token del usuario en la sesion. */
    private fun iniciarSesionLocal(id: Int, nombre: String, email: String, token: String?): UsuarioAuth {
        Sesion.setSesion(id, token)
        return UsuarioAuth(id = id, nombre = nombre, email = email)
    }

    private fun extraerMensaje(e: HttpException): String? {
        return try {
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody.isNullOrBlank()) return null
            // Spring responde {"error": "..."} (QA-46)
            mensajeDeError(errorBody)?.let { return it }
            // Intentar extraer "mensaje": "..." o "message": "..."
            val regexMensaje = Regex(""""mensaje"\s*:\s*"([^"]+)"""")
            val match = regexMensaje.find(errorBody)
            if (match != null) {
                return match.groupValues[1]
            }
            val regexMessage = Regex(""""message"\s*:\s*"([^"]+)"""")
            val match2 = regexMessage.find(errorBody)
            match2?.groupValues?.get(1)
        } catch (ex: Exception) {
            null
        }
    }
}

/**
 * Mensaje del login segun la respuesta. QA-46: tras 5 claves malas Spring responde 429 con
 * {"error": "Demasiados intentos..."}, pero la app mostraba "Correo o contraseña incorrectos."
 */
internal fun mensajeDeLogin(codigo: Int, cuerpo: String?): String = when (codigo) {
    400, 401 -> "Correo o contraseña incorrectos."
    429 -> mensajeDeError(cuerpo) ?: "Demasiados intentos. Espera unos minutos y vuelve a intentarlo."
    else -> mensajeDeError(cuerpo) ?: "Correo o contraseña incorrectos."
}
