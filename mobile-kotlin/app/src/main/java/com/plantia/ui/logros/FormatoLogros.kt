package com.plantia.ui.logros

import com.plantia.data.model.LogroNuevo

private val MESES = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val FECHA_ISO = Regex("""^(\d{4})-(\d{2})-(\d{2})""")

/** "2026-11-20T10:15:30" -> "20 nov 2026"; null si la fecha no llega o no se entiende. */
fun fechaLogro(iso: String?): String? {
    val partes = FECHA_ISO.find(iso.orEmpty().trim())?.groupValues ?: return null
    val mes = partes[2].toInt()
    if (mes !in 1..12) return null
    return "${partes[3].toInt()} ${MESES[mes - 1]} ${partes[1]}"
}

/** "+50 XP · +10 hojas"; vacio si el logro no da recompensa. */
fun textoRecompensa(xp: Int, monedas: Int): String =
    listOfNotNull(
        xp.takeIf { it > 0 }?.let { "+$it XP" },
        monedas.takeIf { it > 0 }?.let { "+$it ${if (it == 1) "hoja" else "hojas"}" }
    ).joinToString(" · ")

fun LogroNuevo.textoRecompensa(): String = textoRecompensa(recompensaXp, recompensaMonedas)
