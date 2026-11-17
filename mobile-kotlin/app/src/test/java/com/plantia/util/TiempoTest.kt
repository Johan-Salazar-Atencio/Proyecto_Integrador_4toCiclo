package com.plantia.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TiempoTest {

    @Test
    fun da_horas_minutos_y_segundos_con_dos_digitos() {
        assertEquals("02:14:35", formatoCuentaRegresiva(2 * 3600 + 14 * 60 + 35))
        assertEquals("00:00:07", formatoCuentaRegresiva(7))
    }

    @Test
    fun pasa_de_24_horas_sin_reiniciarse() {
        assertEquals("26:00:00", formatoCuentaRegresiva(26 * 3600))
    }

    @Test
    fun un_valor_negativo_queda_en_cero() {
        assertEquals("00:00:00", formatoCuentaRegresiva(-5))
    }
}
