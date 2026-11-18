package com.plantia.ui.home.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.PlantSummary
import com.plantia.ui.components.tarjeta
import com.plantia.ui.escena.Escena
import com.plantia.ui.garden.components.PlantFrame
import com.plantia.ui.garden.estadoAnimo
import com.plantia.ui.theme.Abono
import com.plantia.ui.theme.Agua
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.Luz
import com.plantia.ui.theme.RojoClaro
import com.plantia.ui.theme.RojoTexto
import com.plantia.ui.theme.SuperficieElevada
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/**
 * Tarjeta de la planta del Inicio: la escena en pixel art con el chip de animo ("Dia x/y"), las flechas para
 * pasar a otra, el nombre con su especie y los tres cuidados (Regar, Luz, Abono).
 * Tocar la escena o el nombre entra al jardin virtual.
 */
@Composable
fun PlantSummaryCard(
    plant: PlantSummary,
    total: Int,
    escena: Escena?,
    onEnterGarden: () -> Unit,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onCuidar: (AccionCuidado) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .tarjeta()
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))) {
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
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).clickable(onClick = onEnterGarden),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                plant.name,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Text(
                plant.species,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        if (plant.muerta) {
            // Murio por no abonarla a tiempo: no se cuida, se revive (en el jardin, a cambio de hojas)
            Surface(onClick = onEnterGarden, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = RojoClaro) {
                Text(
                    text = plant.costoRevivir?.let { "Murió · Revivir en el jardín ($it hojas)" } ?: "Murió · Revivir en el jardín",
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 12.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RojoTexto
                )
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonCuidado("Regar", Icons.Outlined.Opacity, Agua) { onCuidar(AccionCuidado.REGAR) }
                BotonCuidado("Luz", Icons.Outlined.WbSunny, Luz) { onCuidar(AccionCuidado.SOL) }
                // El abono se compra y se elige en el jardin; solo una planta golpeada se recupera aqui
                BotonCuidado("Abono", Icons.Outlined.Grass, Abono) {
                    if (plant.enRecuperacion) onCuidar(AccionCuidado.ABONAR) else onEnterGarden()
                }
            }
        }
    }
}

@Composable
private fun FlechaPlanta(icono: ImageVector, descripcion: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.size(30.dp), shape = CircleShape, color = Color(0x8C08100C)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = descripcion, tint = Color.White, modifier = Modifier.size(18.dp))
        }
    }
}

/** Boton de cuidado: icono dentro de un circulo del color del cuidado (18 %) sobre una superficie elevada. */
@Composable
private fun RowScope.BotonCuidado(texto: String, icono: ImageVector, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(18.dp),
        color = SuperficieElevada,
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(texto, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.ExtraBold)
        }
    }
}
