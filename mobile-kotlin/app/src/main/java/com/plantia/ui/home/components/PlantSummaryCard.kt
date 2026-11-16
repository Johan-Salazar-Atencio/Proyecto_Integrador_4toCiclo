package com.plantia.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.PlantSummary
import com.plantia.ui.escena.Escena
import com.plantia.ui.garden.components.PlantFrame
import com.plantia.ui.garden.estadoAnimo
import com.plantia.ui.theme.AmberDark
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.BlueInfo
import com.plantia.ui.theme.BlueLight
import com.plantia.ui.theme.FuentePixel
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/**
 * Tarjeta de la planta del Inicio (propuesta E): la escena en pixel art con la planta, las flechas para pasar
 * a otra, el contador "1 / 8", el nombre con su especie y los tres cuidados (Regar, Luz, Abono).
 * Tocar la escena o el nombre entra al jardin virtual.
 */
@Composable
fun PlantSummaryCard(
    plant: PlantSummary,
    posicion: Int,
    total: Int,
    escena: Escena?,
    onEnterGarden: () -> Unit,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onCuidar: (AccionCuidado) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite,
        shadowElevation = 4.dp,
        border = BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.6f))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                PlantFrame(
                    nombre = plant.name,
                    animo = estadoAnimo(plant),
                    imageUrl = plant.imageUrl,
                    escena = escena,
                    apariencia = plant.apariencia,
                    diasCuidados = plant.diasCuidados,
                    diasTotales = plant.diasTotales,
                    marchita = plant.healthPercent < 30 || plant.muerta,
                    desastreActivo = !plant.refugiada,
                    tipoGolpe = plant.golpeTipo,
                    macetaCodigo = plant.macetaCodigo,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1.35f).clickable(onClick = onEnterGarden)
                )
                if (total > 1) {
                    FlechaPlanta(Icons.Default.ChevronLeft, "Planta anterior", onAnterior, Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
                    FlechaPlanta(Icons.Default.ChevronRight, "Planta siguiente", onSiguiente, Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
                }
                if (total > 0) {
                    Text(
                        "$posicion / $total",
                        fontSize = 11.sp,
                        color = GreenDark,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .clip(CircleShape)
                            .background(SurfaceWhite.copy(alpha = 0.92f))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onEnterGarden),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        plant.name,
                        fontSize = 22.sp,
                        fontFamily = FuentePixel,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        plant.species,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        color = TextSecondary,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotonCuidado("Regar", Icons.Default.Opacity, BlueLight, BlueInfo) { onCuidar(AccionCuidado.REGAR) }
                    BotonCuidado("Luz", Icons.Default.WbSunny, AmberLight, AmberDark) { onCuidar(AccionCuidado.SOL) }
                    BotonCuidado("Abono", Icons.Default.Grass, GreenLight, GreenDark) { onCuidar(AccionCuidado.ABONAR) }
                }
            }
        }
    }
}

@Composable
private fun FlechaPlanta(icono: ImageVector, descripcion: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.size(28.dp), shape = CircleShape, color = SurfaceWhite.copy(alpha = 0.9f), shadowElevation = 2.dp) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = descripcion, tint = TextPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun RowScope.BotonCuidado(texto: String, icono: ImageVector, fondo: Color, color: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), color = fondo) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(texto, fontSize = 12.sp, color = color, fontWeight = FontWeight.Medium)
        }
    }
}
