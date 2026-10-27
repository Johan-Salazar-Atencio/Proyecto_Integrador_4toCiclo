package com.plantia.data

import com.plantia.data.remote.AuthLoginRequestDto
import com.plantia.data.remote.AuthRegistroRequestDto
import com.plantia.data.remote.RetrofitClient
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
                AuthResultado.Ok(UsuarioAuth(nombre = response.nombre, email = response.email))
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
                AuthResultado.Ok(UsuarioAuth(nombre = response.nombre, email = response.email))
            } catch (e: HttpException) {
                val mensaje = when (e.code()) {
                    401 -> "Correo o contraseña incorrectos."
                    400 -> "Correo o contraseña incorrectos."
                    else -> extraerMensaje(e) ?: "Correo o contraseña incorrectos."
                }
                AuthResultado.Error(mensaje)
            } catch (e: IOException) {
                AuthResultado.Error("No se pudo conectar con el servidor.")
            } catch (e: Exception) {
                AuthResultado.Error("Ocurrió un error inesperado.")
            }
        }
    }

    private fun extraerMensaje(e: HttpException): String? {
        return try {
            val errorBody = e.response()?.errorBody()?.string()
            if (errorBody.isNullOrBlank()) return null
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
