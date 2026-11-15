package com.plantia.ui.escena

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.delay

/**
 * Escena en pixel art: el fondo animado va detras, [contenido] (la planta) en medio y la animacion
 * del desastre, con transparencia, encima. Se dibuja ampliada sin suavizar para que los pixeles se
 * vean nitidos. Solo anima un cuadro cada cierto tiempo, asi que gasta muy poco.
 */
@Composable
fun EscenaPixel(
    escena: Escena,
    modifier: Modifier = Modifier,
    contenido: @Composable BoxScope.() -> Unit
) {
    val contexto = LocalContext.current
    val tira by produceState<ImageBitmap?>(null, escena.fondoUrl) { value = cargarBitmap(contexto, escena.fondoUrl) }
    val capas by produceState<List<ImageBitmap>>(emptyList(), escena.desastreFrames) {
        value = escena.desastreFrames.mapNotNull { cargarBitmap(contexto, it) }
    }

    var cuadroFondo by remember { mutableIntStateOf(0) }
    LaunchedEffect(tira, escena.fondoFrames, escena.fondoMs) {
        cuadroFondo = 0
        if (tira == null || escena.fondoFrames <= 1) return@LaunchedEffect
        while (true) {
            delay(escena.fondoMs.toLong())
            cuadroFondo = (cuadroFondo + 1) % escena.fondoFrames
        }
    }
    var cuadroCapa by remember { mutableIntStateOf(0) }
    LaunchedEffect(capas, escena.desastreMs) {
        cuadroCapa = 0
        if (capas.size <= 1 || escena.desastreMs <= 0) return@LaunchedEffect
        while (true) {
            delay(escena.desastreMs.toLong())
            cuadroCapa = (cuadroCapa + 1) % capas.size
        }
    }

    Box(modifier = modifier) {
        tira?.let { hoja ->
            Canvas(Modifier.fillMaxSize()) {
                val ancho = if (escena.fondoAncho > 0) escena.fondoAncho else hoja.width / escena.fondoFrames
                val alto = if (escena.fondoAlto > 0) escena.fondoAlto else hoja.height
                dibujar(hoja, IntOffset(cuadroFondo.coerceIn(0, escena.fondoFrames - 1) * ancho, 0), IntSize(ancho, alto))
            }
        }
        contenido()
        capas.getOrNull(cuadroCapa)?.let { capa ->
            Canvas(Modifier.fillMaxSize()) { dibujar(capa, IntOffset.Zero, IntSize(capa.width, capa.height)) }
        }
    }
}

/** Dibuja el recorte [origen]/[tam] de la imagen ocupando todo el espacio, sin suavizar. */
private fun DrawScope.dibujar(imagen: ImageBitmap, origen: IntOffset, tam: IntSize) {
    val destino: Size = size
    drawImage(
        image = imagen,
        srcOffset = origen,
        srcSize = tam,
        dstSize = IntSize(destino.width.toInt(), destino.height.toInt()),
        filterQuality = FilterQuality.None
    )
}

/** Descarga una imagen (Coil la deja en cache); null si falla, y la escena sigue sin ella. */
private suspend fun cargarBitmap(contexto: android.content.Context, url: String): ImageBitmap? {
    val peticion = ImageRequest.Builder(contexto).data(url).allowHardware(false).build()
    val resultado = contexto.imageLoader.execute(peticion) as? SuccessResult ?: return null
    return (resultado.drawable as? BitmapDrawable)?.bitmap?.asImageBitmap()
}
