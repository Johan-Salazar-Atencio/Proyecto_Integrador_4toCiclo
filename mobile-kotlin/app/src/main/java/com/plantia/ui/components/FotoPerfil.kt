package com.plantia.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import com.plantia.util.normalizarUrlLocal

/**
 * Foto de perfil del usuario, recortada por quien la use (por ejemplo dentro de un circulo). Mientras carga o si
 * falla se dibuja [respaldo], asi nunca queda un hueco.
 */
@Composable
fun FotoPerfil(
    url: String,
    descripcion: String,
    modifier: Modifier = Modifier.fillMaxSize(),
    respaldo: @Composable () -> Unit
) {
    SubcomposeAsyncImage(
        // La URL llega con el host del servidor (localhost): se ajusta al que realmente responde
        model = normalizarUrlLocal(url) ?: url,
        contentDescription = descripcion,
        contentScale = ContentScale.Crop,
        modifier = modifier,
        loading = { respaldo() },
        error = { respaldo() }
    )
}
