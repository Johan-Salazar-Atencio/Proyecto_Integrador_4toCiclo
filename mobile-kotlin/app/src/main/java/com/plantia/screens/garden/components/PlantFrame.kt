package com.plantia.screens.garden.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/**
 * Fondos del marco. Hoy solo hay uno neutro; para fondos intercambiables basta con agregar
 * mas `Brush` aqui (o pasar uno propio) sin tocar el resto de la pantalla.
 */
object GardenBackgrounds {
    val Neutro: Brush = Brush.verticalGradient(
        listOf(Color(0xFFF1FAF5), Color(0xFFDDF3E6))
    )
}

/**
 * Marco central de la planta: fondo intercambiable, imagen (o dibujo de respaldo) y el
 * nombre con el estado de animo arriba.
 */
@Composable
fun PlantFrame(
    nombre: String,
    especie: String,
    animo: String,
    imageUrl: String?,
    modifier: Modifier = Modifier,
    fondo: Brush = GardenBackgrounds.Neutro
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(fondo)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (imageUrl.isNullOrBlank()) {
                PlantaDeRespaldo()
            } else {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = nombre,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(32.dp))
                        }
                    },
                    // El backend aun no sirve los sprites: se muestra un dibujo en su lugar.
                    error = { PlantaDeRespaldo() }
                )
            }
        }

        // Nombre + estado de animo
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp)
                .clip(CircleShape)
                .background(SurfaceWhite.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                text = "$nombre · $animo",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        if (especie.isNotBlank()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 14.dp)
            ) {
                Text(
                    text = especie,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PlantaDeRespaldo() {
    Box(
        modifier = Modifier
            .size(150.dp)
            .clip(CircleShape)
            .background(GreenLight),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "🪴", fontSize = 84.sp, color = GreenDark)
    }
}
