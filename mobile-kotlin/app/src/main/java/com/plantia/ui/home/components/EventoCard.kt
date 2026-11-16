package com.plantia.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.plantia.data.model.WeatherAlert
import com.plantia.ui.components.rememberCuentaRegresiva
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.util.formatoCuentaRegresiva

/**
 * El evento activo en el Inicio: imagen (recortada a 16:9, nunca estirada), un recuadro con el nombre, el
 * mensaje y la cuenta regresiva, y el texto informativo. Tocarla abre las misiones, donde se previene.
 */
@Composable
fun EventoCard(
    alerta: WeatherAlert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val restante = rememberCuentaRegresiva(alerta.endsInSeconds)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = BorderStroke(1.5.dp, AmberAlert.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (!alerta.imageUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = alerta.imageUrl,
                    contentDescription = alerta.eventName ?: alerta.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    // Sin imagen (o mientras carga) queda un bloque del color del evento, sin saltos de tamano
                    loading = { Box(Modifier.fillMaxSize().background(AmberLight)) },
                    error = { Box(Modifier.fillMaxSize().background(AmberLight)) }
                )
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(AmberLight)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = AmberAlert, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(alerta.eventName ?: alerta.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        if (alerta.description.isNotBlank()) {
                            Text(alerta.description, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Termina en", fontSize = 10.sp, color = TextSecondary)
                        Text(
                            text = if (restante != null) formatoCuentaRegresiva(restante) else alerta.timeRemaining,
                            fontSize = 16.sp,
                            fontFamily = com.plantia.ui.theme.FuentePixel,
                            fontWeight = FontWeight.Medium,
                            color = AmberAlert
                        )
                    }
                }
                if (!alerta.info.isNullOrBlank()) {
                    Text(alerta.info, fontSize = 13.sp, lineHeight = 19.sp, color = TextSecondary)
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
}
