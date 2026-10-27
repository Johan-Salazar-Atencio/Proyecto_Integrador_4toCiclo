package com.plantia.screens.auth

/** Reglas de validacion del formulario de acceso (funciones puras, faciles de probar). */

internal const val LARGO_MINIMO_PASSWORD = 8

private val PATRON_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

internal fun esCorreoValido(email: String): Boolean = PATRON_CORREO.matches(email.trim())

internal fun esNombreValido(nombre: String): Boolean = nombre.trim().length >= 2

internal fun esPasswordValida(password: String): Boolean = password.length >= LARGO_MINIMO_PASSWORD

internal fun coincidenPasswords(password: String, confirmar: String): Boolean = password == confirmar
