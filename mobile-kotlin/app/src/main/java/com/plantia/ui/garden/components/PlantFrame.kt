package com.plantia.ui.garden.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import com.plantia.data.model.Apariencia
import com.plantia.ui.escena.Escena
import com.plantia.ui.escena.EscenaPixel
import com.plantia.ui.escena.PlantaPixel
import com.plantia.ui.escena.rememberSprites
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.LocalTemaOscuro
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/**
 * Fondos del marco. Hoy solo hay uno neutro; para fondos intercambiables basta con agregar
 * mas `Brush` aqui (o pasar uno propio) sin tocar el resto de la pantalla.
 */
object GardenBackgrounds {
    val Neutro: Brush
        @Composable get() = Brush.verticalGradient(
            if (LocalTemaOscuro.current) listOf(Color(0xFF173A28), Color(0xFF0F2E1C)) else listOf(Color(0xFFF1FAF5), Color(0xFFDDF3E6))
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
    fondo: Brush = GardenBackgrounds.Neutro,
    /** Escena en pixel art (fondo animado y desastre); sin ella se usa el fondo de color. */
    escena: Escena? = null,
    /** Datos para dibujar la planta en pixel art sobre la tierra; sin sprites se usa la imagen. */
    apariencia: Apariencia? = null,
    diasCuidados: Int = 0,
    diasTotales: Int = 0,
    marchita: Boolean = false
) {
    val sprites = rememberSprites()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(fondo)
    ) {
        val planta: @Composable BoxScope.() -> Unit = if (sprites != null) {
            {
                PlantaPixel(
                    sprites = sprites,
                    apariencia = apariencia,
                    diasCuidados = diasCuidados,
                    diasTotales = diasTotales,
                    marchita = marchita,
                    descripcion = nombre,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {{
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
                        // Si la imagen no carga se dibuja una planta de respaldo.
                        error = { PlantaDeRespaldo() }
                    )
                }
            }
        }}
        if (escena != null) EscenaPixel(escena, Modifier.fillMaxSize(), planta) else planta()

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
    Text(text = "🪴", fontSize = 84.sp, color = GreenDark)
}
