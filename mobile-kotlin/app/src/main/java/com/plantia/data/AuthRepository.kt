package com.plantia.data

/** Usuario autenticado tal como lo necesita la app. */
data class UsuarioAuth(val nombre: String, val email: String)

sealed interface AuthResultado {
    data class Ok(val usuario: UsuarioAuth) : AuthResultado
    /** `mensaje` ya viene listo para mostrarse al usuario. */
    data class Error(val mensaje: String) : AuthResultado
}

/**
 * Contrato de acceso (registro e inicio de sesion).
 *
 * Hoy lo implementa [MockAuthRepository] (simulado). Cuando el backend tenga los endpoints,
 * basta crear otra implementacion que llame a la API y cambiarla en `AuthViewModel`:
 * la pantalla no necesita tocarse.
 */
interface AuthRepository {
    suspend fun registrar(nombre: String, email: String, password: String): AuthResultado
    suspend fun iniciarSesion(email: String, password: String): AuthResultado
}
