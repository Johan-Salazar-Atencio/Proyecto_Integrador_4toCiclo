package com.plantia.ui.garden.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.plantia.data.model.Apariencia
import com.plantia.ui.escena.Escena
import com.plantia.ui.escena.EscenaPixel
import com.plantia.ui.escena.PlantaPixel
import com.plantia.ui.escena.faseNumero
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
    marchita: Boolean = false,
    /** Si se da, aparece arriba a la derecha el icono que abre los fondos de esta planta. */
    onFondos: (() -> Unit)? = null,
    /** false cuando la planta ya esta protegida: la animacion del desastre se desvanece de la escena. */
    desastreActivo: Boolean = true,
    /** Sube cada vez que se previene el evento: dispara el escudo que lo aleja (0 = ninguno). */
    efectoProteccion: Int = 0,
    /** true mientras se muestra, una sola vez, como el evento arraso con la planta. */
    arrasando: Boolean = false,
    /** Tipo de evento que la golpeo (para el texto del golpe). */
    tipoGolpe: String? = null,
    /** Maceta puesta a la planta (null = el monton de tierra de siempre). */
    macetaCodigo: String? = null
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
                    modifier = Modifier.fillMaxSize(),
                    macetaCodigo = macetaCodigo
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
        if (escena != null) {
            // Al proteger la planta el desastre se desvanece (1,2 s) y queda el fondo de siempre
            Crossfade(targetState = desastreActivo, animationSpec = tween(1_200), label = "desastre") { activo ->
                EscenaPixel(if (activo) escena else escena.copy(desastreFrames = emptyList()), Modifier.fillMaxSize(), planta)
            }
        } else {
            planta()
        }
        EfectoProteccion(efectoProteccion, Modifier.fillMaxSize())

        // Chip compacto: animo, dia de crecimiento y fase; el nombre va fuera del marco para no tapar la escena
        ChipCrecimiento(
            animo = animo,
            diasCuidados = diasCuidados,
            diasTotales = diasTotales,
            marchita = marchita,
            modifier = Modifier.align(Alignment.TopStart).padding(10.dp)
        )
        EfectoArraso(arrasando, tipoGolpe, Modifier.fillMaxSize())
        if (onFondos != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SurfaceWhite.copy(alpha = 0.92f))
                    .clickable(onClickLabel = "Fondos de $nombre", onClick = onFondos),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Image, contentDescription = "Fondos de $nombre", tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
        }
    }
}

/** Icono de animo, "Dia 3/7" y 3 puntos de fase en un solo chip pequeno. */
@Composable
private fun ChipCrecimiento(
    animo: String,
    diasCuidados: Int,
    diasTotales: Int,
    marchita: Boolean,
    modifier: Modifier = Modifier
) {
    val fase = faseNumero(diasCuidados, diasTotales)
    val texto = when {
        marchita -> "Marchita"
        diasTotales <= 0 -> null
        diasCuidados >= diasTotales -> "Madura"
        else -> "Día $diasCuidados/$diasTotales"
    }
    val descripcion = buildString {
        append("Ánimo $animo")
        if (diasTotales > 0) append(", día $diasCuidados de $diasTotales, fase $fase de 3")
        if (marchita) append(", marchita")
    }
    Row(
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = descripcion }
            .clip(CircleShape)
            .background(SurfaceWhite.copy(alpha = 0.92f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(text = emojiAnimo(animo), fontSize = 16.sp)
        if (texto != null) {
            Text(text = texto, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            if (!marchita) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(3) { i ->
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (i < fase) GreenPrimary else TextSecondary.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}

private fun emojiAnimo(animo: String): String = when (animo) {
    "Feliz" -> "😊"
    "Regular" -> "😐"
    else -> "😟"
}

@Composable
private fun PlantaDeRespaldo() {
    Text(text = "🪴", fontSize = 84.sp, color = GreenDark)
}
