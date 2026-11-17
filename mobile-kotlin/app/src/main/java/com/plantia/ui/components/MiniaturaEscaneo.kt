package com.plantia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage

/**
 * Miniatura cuadrada de un escaneo: la foto que se tomo o subio. Mientras carga, si falla o si el escaneo
 * no tiene foto, se dibuja el icono de planta del color del estado, asi nunca queda un hueco.
 */
@Composable
fun MiniaturaEscaneo(
    fotoUrl: String?,
    color: Color,
    tamano: Dp,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .size(tamano)
            .clip(forma)
            .background(color.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        val respaldo: @Composable () -> Unit = {
            Icon(
                imageVector = Icons.Default.LocalFlorist,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(tamano / 2)
            )
        }
        if (fotoUrl.isNullOrBlank()) {
            respaldo()
        } else {
            SubcomposeAsyncImage(
                model = fotoUrl,
                contentDescription = "Foto del escaneo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { respaldo() },
                error = { respaldo() }
            )
        }
    }
}
