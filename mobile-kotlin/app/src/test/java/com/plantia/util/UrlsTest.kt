package com.plantia.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Las imagenes que manda el servidor cargan desde el telefono: una ruta se completa con la direccion del servidor. */
class UrlsTest {

    @Test
    fun una_ruta_del_servidor_se_completa_con_su_direccion() {
        val url = normalizarUrlLocal("/files/eventos/ola_de_calor_ab12.jpg")!!

        assertTrue(url.startsWith("http"))
        assertTrue(url.endsWith("/files/eventos/ola_de_calor_ab12.jpg"))
    }

    @Test
    fun una_direccion_con_localhost_cambia_de_host_y_conserva_la_ruta() {
        val url = normalizarUrlLocal("http://localhost:8080/files/foto.jpg")!!

        assertTrue(url.endsWith("/files/foto.jpg"))
    }

    @Test
    fun sin_direccion_no_hay_imagen() {
        assertNull(normalizarUrlLocal(null))
        assertEquals(null, normalizarUrlLocal("  "))
    }
}
