package com.plantia.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.model.PlantSummary
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

@Composable
fun PlantSummaryCard(
    plant: PlantSummary,
    onEnterGarden: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 4.dp,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        GreenLight.copy(alpha = 0.2f)
                    )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(GreenLight)
                        .border(1.dp, GreenPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Yard,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plant.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = plant.species,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "...",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextSecondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(GreenLight.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                // Sin libreria de imagenes todavia: se muestra el nombre real
                // de la planta en vez de un texto fijo.
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }

            if (plant.lastWatered.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ultimo riego: ${plant.lastWatered}",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            StatProgressBar(
                label = "Salud Vital",
                percent = plant.healthPercent,
                color = GreenPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            StatProgressBar(
                label = "Humedad",
                percent = plant.humidityPercent,
                color = Color(0xFF3B82F6)
            )
            Spacer(modifier = Modifier.height(12.dp))
            StatProgressBar(
                label = "Luz Solar",
                percent = plant.lightPercent,
                color = Color(0xFFF59E0B)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onEnterGarden,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary
                )
            ) {
                Text(
                    text = "Entrar al JardÃ­n Virtual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}
