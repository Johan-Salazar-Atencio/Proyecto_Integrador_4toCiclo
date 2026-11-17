package com.plantia.ui.escena

import com.google.gson.Gson
import com.plantia.data.model.Apariencia
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantaPixelLogicaTest {

    private val datos: PlantaSpritesJson =
        Gson().fromJson(File("src/main/assets/planta_sprites.json").readText(), PlantaSpritesJson::class.java)

    @Test
    fun elSpriteAvanzaConLosDiasDeCadaDuracion() {
        // 5 dias: un sprite distinto cada dia
        assertEquals(listOf(0, 1, 2, 3, 4, 5), (0..5).map { indiceSprite(it, 5, false) })
        // 7 dias
        assertEquals(listOf(0, 1, 1, 2, 2, 3, 4, 5), (0..7).map { indiceSprite(it, 7, false) })
        // 12 dias: llega al ultimo sprite solo en el dia 12
        assertEquals(4, indiceSprite(10, 12, false))
        assertEquals(5, indiceSprite(12, 12, false))
    }

    @Test
    fun laFaseCoincideConLaDelServidor() {
        // 5 dias: fase 2 desde el dia 2; 7: desde el 3; 12: desde el 5; la 3 al completar
        assertEquals(listOf(1, 1, 2, 2, 2, 3), (0..5).map { faseNumero(it, 5) })
        assertEquals(listOf(1, 1, 1, 2, 2, 2, 2, 3), (0..7).map { faseNumero(it, 7) })
        assertEquals(1, faseNumero(4, 12))
        assertEquals(2, faseNumero(5, 12))
        assertEquals(3, faseNumero(12, 12))
        assertEquals(1, faseNumero(0, 0))
    }

    @Test
    fun marchitaYSinDatosDelServidor() {
        assertEquals(SPRITE_MARCHITA, indiceSprite(3, 7, true))
        assertEquals(3, indiceSprite(0, 0, false))
    }

    @Test
    fun cadaFormaTieneSusSieteSpritesDe32x32() {
        for (forma in datos.formas.keys) {
            for (flor in listOf(true, false)) for (fruto in listOf(true, false)) {
                val sprites = spritesDe(datos, Apariencia(forma = forma, tieneFlor = flor, tieneFruto = fruto))
                assertEquals("$forma $flor $fruto", 7, sprites.size)
                assertTrue(sprites.all { s -> s.size == 32 && s.all { it.length == 32 } })
            }
        }
        assertEquals(7, spritesDe(datos, null).size)
        assertEquals(7, spritesDe(datos, Apariencia(forma = "desconocida")).size)
    }

    @Test
    fun laPaletaUsaElColorDeLaHojaYOscureceElContorno() {
        val paleta = paletaDe(datos, Apariencia(colorHoja = "#4f9a52"))
        assertEquals(colorDe("#4f9a52"), paleta['G'])
        val contorno = paleta['o']!!
        assertTrue(((contorno shr 8) and 255) < 0x9a)
        // Sin apariencia: el color por defecto de la web
        assertEquals(colorDe("#4f9a52"), paletaDe(datos, null)['G'])
    }

    @Test
    fun tramosUneLosPixelesContiguosDelMismoColor() {
        val paleta = mapOf('G' to 1, 'o' to 2)
        assertEquals(
            listOf(Tramo(1, 0, 2, 1), Tramo(3, 0, 1, 2)),
            tramos(listOf(".GGo."), paleta)
        )
    }
}
