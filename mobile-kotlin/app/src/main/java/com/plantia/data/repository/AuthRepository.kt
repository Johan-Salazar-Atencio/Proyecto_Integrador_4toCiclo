package com.plantia.data.repository

/** Usuario autenticado tal como lo necesita la app. */
data class UsuarioAuth(val id: Int, val nombre: String, val email: String)

sealed interface AuthResultado {
    data class Ok(val usuario: UsuarioAuth) : AuthResultado
    /** `mensaje` ya viene listo para mostrarse al usuario. */
    data class Error(val mensaje: String) : AuthResultado
}

/**
 * Contrato de acceso (registro e inicio de sesion).
 *
 * Lo implementa [ApiAuthRepository] contra /api/v1/auth (Spring). La pantalla solo
 * conoce esta interfaz, asi que se puede cambiar la implementacion en `AuthViewModel`.
 */
interface AuthRepository {
    suspend fun registrar(nombre: String, email: String, password: String): AuthResultado
    suspend fun iniciarSesion(email: String, password: String): AuthResultado
}
