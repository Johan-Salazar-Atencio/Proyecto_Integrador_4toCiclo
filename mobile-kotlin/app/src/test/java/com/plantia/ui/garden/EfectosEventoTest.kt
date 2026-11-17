package com.plantia.ui.garden

import com.plantia.ui.garden.components.textoDelGolpe
import org.junit.Assert.assertEquals
import org.junit.Test

class EfectosEventoTest {

    @Test
    fun el_texto_del_golpe_nombra_el_evento() {
        assertEquals("¡El huayco arrasó con tu planta!", textoDelGolpe("HUAYCO"))
        assertEquals("¡La helada arrasó con tu planta!", textoDelGolpe("HELADA"))
        assertEquals("¡La ola de calor arrasó con tu planta!", textoDelGolpe("OLA_DE_CALOR"))
        assertEquals("¡La plaga arrasó con tu planta!", textoDelGolpe("PLAGA"))
    }

    @Test
    fun un_tipo_desconocido_dice_el_evento() {
        assertEquals("¡El evento arrasó con tu planta!", textoDelGolpe(null))
        assertEquals("¡El evento arrasó con tu planta!", textoDelGolpe("TERREMOTO"))
    }
}
