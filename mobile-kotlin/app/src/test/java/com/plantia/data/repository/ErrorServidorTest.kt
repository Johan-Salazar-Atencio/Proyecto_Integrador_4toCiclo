package com.plantia.data.repository

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/** QA-36: un escaneo rechazado muestra el mensaje del servidor, no "HTTP 429". */
class ErrorServidorTest {

    private fun http(codigo: Int, cuerpo: String) =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

    @Test
    fun usa_el_mensaje_error_del_servidor() {
        val e = http(429, """{"error":"Escaneaste muchas plantas seguidas. Espera un minuto y vuelve a intentarlo."}""")
            .conMensajeDelServidor()
        assertEquals("Escaneaste muchas plantas seguidas. Espera un minuto y vuelve a intentarlo.", e.message)
        assertEquals(429, e.codigo)
    }

    @Test
    fun sin_cuerpo_json_da_un_mensaje_claro_segun_el_codigo() {
        assertEquals("Hay muchos escaneos en este momento. Espera un minuto y vuelve a intentarlo.",
            http(429, "").conMensajeDelServidor().message)
        assertEquals("La foto no es una imagen válida.", http(415, "<html>").conMensajeDelServidor().message)
        assertEquals("El servidor respondió 503", http(503, "x").conMensajeDelServidor().message)
    }

    @Test
    fun mensaje_de_error_ignora_cuerpos_raros() {
        assertNull(mensajeDeError(null))
        assertNull(mensajeDeError("[]"))
        assertNull(mensajeDeError("""{"error":""}"""))
        assertEquals("x", mensajeDeError("""{"error":"x"}"""))
    }

    @Test
    fun el_login_bloqueado_muestra_el_aviso_del_servidor() {
        assertEquals("Demasiados intentos fallidos. Espera unos minutos y vuelve a intentarlo.",
            mensajeDeLogin(429, """{"error":"Demasiados intentos fallidos. Espera unos minutos y vuelve a intentarlo."}"""))
        assertEquals("Demasiados intentos. Espera unos minutos y vuelve a intentarlo.", mensajeDeLogin(429, ""))
        assertEquals("Correo o contraseña incorrectos.", mensajeDeLogin(401, """{"error":"Correo o contraseña incorrectos"}"""))
    }
}
