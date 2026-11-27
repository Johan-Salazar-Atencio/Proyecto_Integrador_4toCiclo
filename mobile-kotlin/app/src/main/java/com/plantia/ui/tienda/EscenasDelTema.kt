package com.plantia.ui.tienda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import com.plantia.ui.escena.tamanoPixelArt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.plantia.data.model.VarianteFondo
import com.plantia.ui.theme.SuperficieElevada
import com.plantia.ui.theme.TextSecondary

internal fun nombreMomento(momento: String): String = when (momento) {
    "DIA" -> "☀️ Día"
    "TARDE" -> "🌇 Tarde"
    "NOCHE" -> "🌙 Noche"
    else -> momento
}

/** Las escenas de un tema de fondo (dia, tarde y noche) en miniatura, cada una con su etiqueta. */
@Composable
fun EscenasDelTema(variantes: List<VarianteFondo>, modifier: Modifier = Modifier) {
    if (variantes.isEmpty()) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Cada escena (96x96) se amplia por un factor ENTERO de pixeles de pantalla: todos los pixeles iguales
        val lado = tamanoPixelArt(96, 96, maxAncho = (maxWidth - 16.dp) / variantes.size)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            variantes.forEach { v ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = v.vistaUrl,
                    contentDescription = "Escena de ${nombreMomento(v.momento)}",
                    contentScale = ContentScale.FillBounds,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.size(lado).clip(RoundedCornerShape(12.dp)).background(SuperficieElevada)
                )
                Text(nombreMomento(v.momento), fontSize = 11.sp, color = TextSecondary)
            }
            }
        }
    }
}
