package com.plantia.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.plantia.data.model.WeatherAlert
import com.plantia.ui.components.rememberCuentaRegresiva
import com.plantia.ui.components.tarjeta
import com.plantia.ui.theme.Evento
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.util.formatoCuentaRegresiva

/**
 * El evento activo en el Inicio: imagen con la etiqueta "Desastre · Evento activo", el nombre, el texto y un
 * recuadro "Termina en" con la cuenta regresiva. Tocarla abre las misiones, donde se previene.
 */
@Composable
fun EventoCard(
    alerta: WeatherAlert,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val restante = rememberCuentaRegresiva(alerta.endsInSeconds)
    val colorEvento = Evento
    val tarjeta = SurfaceWhite
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .tarjeta()
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.fillMaxWidth().height(116.dp)) {
            if (!alerta.imageUrl.isNullOrBlank()) {
                SubcomposeAsyncImage(
                    model = alerta.imageUrl,
                    contentDescription = alerta.eventName ?: alerta.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    // Sin imagen (o mientras carga) queda un bloque del color del evento, sin saltos de tamano
                    loading = { Box(Modifier.fillMaxSize().background(colorEvento.copy(alpha = 0.18f))) },
                    error = { Box(Modifier.fillMaxSize().background(colorEvento.copy(alpha = 0.18f))) }
                )
            } else {
                Box(Modifier.fillMaxSize().background(colorEvento.copy(alpha = 0.18f)))
            }
            // Degradado hacia el color de la tarjeta, para que la imagen se funda con el texto
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(0.4f to Color.Transparent, 1f to tarjeta)
                )
            )
            Text(
                "DESASTRE · EVENTO ACTIVO",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.3.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(colorEvento)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Column(
            Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp, top = 2.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(alerta.eventName ?: alerta.title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            if (alerta.description.isNotBlank()) {
                Text(alerta.description, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
            }
            if (!alerta.info.isNullOrBlank()) {
                Text(alerta.info, fontSize = 12.sp, lineHeight = 17.sp, color = TextSecondary)
            }
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorEvento.copy(alpha = 0.14f))
                    .border(1.dp, colorEvento.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Termina en", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colorEvento)
                Text(
                    text = if (restante != null) formatoCuentaRegresiva(restante) else alerta.timeRemaining,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorEvento
                )
            }
        }
    }
}
