package com.plantia.screens.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun correosValidos() {
        assertTrue(esCorreoValido("tu@correo.com"))
        assertTrue(esCorreoValido("  Juan.Perez+plantia@tecsup.edu.pe  "))
    }

    @Test
    fun correosInvalidos() {
        assertFalse(esCorreoValido(""))
        assertFalse(esCorreoValido("sin-arroba.com"))
        assertFalse(esCorreoValido("a@b"))
        assertFalse(esCorreoValido("a@b.c"))
        assertFalse(esCorreoValido("a b@correo.com"))
    }

    @Test
    fun nombreNecesitaAlMenosDosLetras() {
        assertFalse(esNombreValido(" "))
        assertFalse(esNombreValido("A"))
        assertTrue(esNombreValido("Al"))
        assertTrue(esNombreValido("  Ana  "))
    }

    @Test
    fun passwordMinimoOchoCaracteres() {
        assertFalse(esPasswordValida("1234567"))
        assertTrue(esPasswordValida("12345678"))
    }

    @Test
    fun confirmacionDebeCoincidir() {
        assertTrue(coincidenPasswords("plantia123", "plantia123"))
        assertFalse(coincidenPasswords("plantia123", "plantia124"))
    }
}
