package com.plantia.screens.collection.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.plantia.data.remote.ColeccionItemDto
import com.plantia.model.PlantSummary
import com.plantia.screens.garden.estadoAnimo
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/** Foto cuadrada de una planta; si no hay foto o falla la carga, un macetero de respaldo. */
@Composable
private fun FotoPlanta(url: String?, descripcion: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(GreenLight),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Text("🪴", fontSize = 44.sp)
        } else {
            SubcomposeAsyncImage(
                model = url,
                contentDescription = descripcion,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary, modifier = Modifier.size(24.dp))
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("🪴", fontSize = 44.sp)
                    }
                }
            )
        }
    }
}

@Composable
private fun Etiqueta(texto: String, fondo: Color, color: Color) {
    Text(
        text = texto,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(CircleShape)
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun TarjetaBase(onClick: () -> Unit, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            contenido()
        }
    }
}

/** Planta escaneada: foto real, nombre, especie y, si aplica, "xN" veces vista y aviso de toxicidad. */
@Composable
fun TarjetaEscaneada(item: ColeccionItemDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TarjetaBase(onClick, modifier) {
        FotoPlanta(item.fotoUrl, item.nombreComun.orEmpty(), Modifier.fillMaxWidth())
        Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.nombreComun.orEmpty(),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.nombreCientifico.orEmpty(),
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (item.vecesVista > 1 || item.esToxicaMascotas) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
                if (item.vecesVista > 1) Etiqueta("×${item.vecesVista}", GreenLight, GreenDark)
                if (item.esToxicaMascotas) Etiqueta("Tóxica", AmberLight, AmberAlert)
            }
        }
    }
}

/** Planta digital: la activa lleva la insignia "Activo". */
@Composable
fun TarjetaDigital(planta: PlantSummary, activa: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TarjetaBase(onClick, modifier) {
        Box {
            FotoPlanta(planta.imageUrl, planta.name, Modifier.fillMaxWidth())
            if (activa) {
                Text(
                    text = "Activo",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = planta.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${estadoAnimo(planta)} · Salud ${planta.healthPercent}%",
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Invitacion al final de la lista (reemplaza a las tarjetas "misterio" de la referencia). */
@Composable
fun TarjetaDescubrirMas(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GreenLight),
        border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SurfaceWhite),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = GreenDark)
            }
            Column {
                Text("¡Descubre más plantas!", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(2.dp))
                Text("Escanea una nueva para sumarla a tu colección.", fontSize = 13.sp, color = TextSecondary)
            }
        }
    }
}
