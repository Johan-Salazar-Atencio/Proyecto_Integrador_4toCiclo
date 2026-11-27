package com.plantia.ui.escena

import com.plantia.data.model.Apariencia
import java.util.zip.CRC32
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * El motor de plantas del movil (MotorPlanta64) debe dibujar pixel a pixel lo mismo que el de la web.
 * `planta64_huellas.txt` lo genera la web (`node scripts/arte/huellas_planta64.mjs`) con el CRC32 de los pixeles
 * RGBA de cada fotograma: forma, colores, etapa, fotograma, maceta y estado (sana, marchita, muerta).
 */
class PlantaPixel64Test {

    private fun huella(rgba: IntArray): String {
        val crc = CRC32()
        crc.update(ByteArray(rgba.size) { rgba[it].toByte() })
        return crc.value.toString(16).padStart(8, '0')
    }

    private fun texto(v: String) = v.ifEmpty { null }

    @Test
    fun dibujaLoMismoQueLaWeb() {
        val lineas = javaClass.classLoader!!.getResourceAsStream("planta64_huellas.txt")!!.bufferedReader().readLines().filter { it.isNotBlank() }
        assertTrue("faltan huellas", lineas.size > 2000)
        val distintos = ArrayList<String>()
        for (linea in lineas) {
            val (clave, esperada) = linea.split("=")
            val c = clave.split("|")
            val obtenida = if (c[0] == "vacia") {
                huella(MotorPlanta64.macetaVaciaRgba(Apariencia(forma = c[1]), texto(c[2])))
            } else {
                val ap = if (c[0] == "-") null else Apariencia(
                    forma = c[0], colorHoja = texto(c[1]), tieneFlor = c[2] == "true", colorFlor = texto(c[3]),
                    tieneFruto = c[4] == "true", colorFruto = texto(c[5])
                )
                huella(MotorPlanta64.fotogramaRgba(ap, c[6].toInt(), c[7].toInt(), texto(c[8]), texto(c[9])))
            }
            if (obtenida != esperada) distintos.add(clave)
        }
        assertEquals("Fotogramas distintos a la web (${distintos.size}): ${distintos.take(12)}", 0, distintos.size)
    }

    @Test
    fun laCacheDevuelveElMismoDibujoEnArgb() {
        val ap = Apariencia(forma = "monstera", colorHoja = "#2e7d32")
        val rgba = MotorPlanta64.fotogramaRgba(ap, 5, 3)
        val argb = MotorPlanta64.fotograma(ap, 5, 3)
        assertEquals(MotorPlanta64.aArgb(rgba).toList(), argb.toList())
        // Los fotogramas se repiten cada 12 (tambien con k negativo)
        assertEquals(argb.toList(), MotorPlanta64.fotograma(ap, 5, 15).toList())
        assertEquals(argb.toList(), MotorPlanta64.fotograma(ap, 5, -9).toList())
    }

    @Test
    fun etapaDeLaPlantaComoEnLaWeb() {
        assertEquals(EstadoPlanta64(0, null), estadoPlanta64(0, 7, marchita = false, muerta = false))
        assertEquals(EstadoPlanta64(3, null), estadoPlanta64(0, 0, marchita = false, muerta = false))
        assertEquals(EstadoPlanta64(5, MotorPlanta64.MARCHITA), estadoPlanta64(7, 7, marchita = true, muerta = false))
        assertEquals(EstadoPlanta64(2, MotorPlanta64.MARCHITA), estadoPlanta64(3, 7, marchita = true, muerta = false))
        assertEquals(EstadoPlanta64(4, MotorPlanta64.MUERTA), estadoPlanta64(6, 7, marchita = true, muerta = true))
    }
}
