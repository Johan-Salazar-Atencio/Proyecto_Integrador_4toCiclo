package com.plantia.ui.escena

import com.plantia.data.model.Arte

/** Lo que se dibuja en la escena: el fondo del momento del dia y, si hay un desastre, su animacion. */
data class Escena(
    val fondoUrl: String,
    val fondoFrames: Int,
    val fondoAncho: Int,
    val fondoAlto: Int,
    val fondoMs: Int,
    val desastreFrames: List<String> = emptyList(),
    val desastreMs: Int = 0
)

/** Momento del dia segun la hora: DIA de 6 a 16, TARDE de 17 a 18 y NOCHE el resto. */
fun momentoDelDia(hora: Int): String = when (hora) {
    in 6..16 -> "DIA"
    in 17..18 -> "TARDE"
    else -> "NOCHE"
}

/**
 * Elige que dibujar. Sin fondo del momento usa cualquier otro fondo (mejor que nada); sin arte
 * devuelve null y el marco conserva su fondo de color. La animacion del desastre es la de su tipo
 * y fase; si falta la fase pedida, prueba con la otra.
 */
fun elegirEscena(arte: Arte?, hora: Int, tipoDesastre: String?, fase: String?): Escena? {
    if (arte == null) return null
    val momento = momentoDelDia(hora)
    val fondo = arte.fondos.filter { it.momento == momento }.sortedByDescending { it.esPredeterminado }.firstOrNull()
        ?: arte.fondos.firstOrNull()
        ?: return null
    val animacion = tipoDesastre?.takeIf { it.isNotBlank() }?.let { tipo ->
        val delTipo = arte.animaciones.filter { it.disasterType == tipo }
        delTipo.firstOrNull { it.phase == fase } ?: delTipo.firstOrNull()
    }
    val ancho = if (fondo.frameWidthPx > 0) fondo.frameWidthPx else 0
    return Escena(
        fondoUrl = fondo.imageUrl,
        fondoFrames = fondo.frames,
        fondoAncho = ancho,
        fondoAlto = fondo.frameHeightPx,
        fondoMs = fondo.msPerFrame,
        desastreFrames = animacion?.frames.orEmpty(),
        desastreMs = animacion?.msPerFrame ?: 0
    )
}
