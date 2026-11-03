package com.plantia.data

import kotlinx.coroutines.delay

/**
 * SIMULACION del acceso: las cuentas viven en memoria y se pierden al cerrar la app.
 *
 * El backend todavia no tiene login ni registro (solo existe la tabla `usuarios` con
 * nombre, email y password_hash).
 *
 * TODO(backend): reemplazar por una implementacion real de [AuthRepository]:
 *   POST /api/v1/auth/registro {nombre, email, password} -> 201 (409 si el correo existe)
 *   POST /api/v1/auth/login    {email, password}         -> usuario / token
 */
object MockAuthRepository : AuthRepository {

    /** Cuenta de prueba precargada, para poder iniciar sesion tras reabrir la app. */
    const val CORREO_DEMO = "demo@plantia.test"
    const val CLAVE_DEMO = "plantia123"

    private data class Cuenta(val nombre: String, val password: String)

    private val cuentas = mutableMapOf(
        CORREO_DEMO to Cuenta("Jardinero Demo", CLAVE_DEMO)
    )

    /** Quien tiene la sesion abierta en esta ejecucion (aun no se persiste). */
    var usuarioActual: UsuarioAuth? = null
        private set

    private fun normalizar(email: String) = email.trim().lowercase()

    override suspend fun registrar(nombre: String, email: String, password: String): AuthResultado {
        delay(900) // simula la latencia de red
        val clave = normalizar(email)
        if (cuentas.containsKey(clave)) {
            return AuthResultado.Error("Ese correo ya está registrado. Prueba iniciando sesión.")
        }
        cuentas[clave] = Cuenta(nombre.trim(), password)
        return entrar(UsuarioAuth(0, nombre.trim(), clave))
    }

    override suspend fun iniciarSesion(email: String, password: String): AuthResultado {
        delay(900)
        val clave = normalizar(email)
        val cuenta = cuentas[clave]
        if (cuenta == null || cuenta.password != password) {
            return AuthResultado.Error("Correo o contraseña incorrectos.")
        }
        return entrar(UsuarioAuth(0, cuenta.nombre, clave))
    }

    private fun entrar(usuario: UsuarioAuth): AuthResultado {
        usuarioActual = usuario
        return AuthResultado.Ok(usuario)
    }
}
