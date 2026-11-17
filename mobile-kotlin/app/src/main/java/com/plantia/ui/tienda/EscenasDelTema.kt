package com.plantia.ui.tienda

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        variantes.forEach { v ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                AsyncImage(
                    model = v.vistaUrl,
                    contentDescription = "Escena de ${nombreMomento(v.momento)}",
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.None,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(SuperficieElevada)
                )
                Text(nombreMomento(v.momento), fontSize = 11.sp, color = TextSecondary)
            }
        }
    }
}
