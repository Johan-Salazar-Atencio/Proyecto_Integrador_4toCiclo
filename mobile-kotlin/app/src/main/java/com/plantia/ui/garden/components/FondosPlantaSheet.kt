package com.plantia.ui.garden.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.model.TemaFondo
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.ui.tienda.EscenasDelTema

/**
 * "Fondos de [planta]": el tema de siempre y los que compro el usuario, cada uno con sus escenas de dia, tarde
 * y noche. El que se elige se aplica solo a esta planta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FondosPlantaSheet(
    nombrePlanta: String,
    temas: List<TemaFondo>,
    cargando: Boolean,
    onElegir: (TemaFondo) -> Unit,
    onTienda: () -> Unit,
    onCerrar: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BackgroundLight
    ) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Fondos de $nombrePlanta", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Se aplican solo a esta planta y cambian con la hora del día.", fontSize = 13.sp, color = TextSecondary)
        }
        Spacer(Modifier.height(10.dp))
        if (cargando && temas.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(temas, key = { it.id }) { tema -> TarjetaTema(tema, onElegir) }
            item {
                TextButton(onClick = onTienda, modifier = Modifier.fillMaxWidth()) {
                    Text("🛒 Conseguir más fondos en la tienda", color = GreenDark, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TarjetaTema(tema: TemaFondo, onElegir: (TemaFondo) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EscenasDelTema(tema.variantes)
            Text(
                text = if (tema.predeterminado) "${tema.nombre} · el de siempre" else tema.nombre,
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
            if (tema.descripcion.isNotBlank()) Text(tema.descripcion, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
            if (tema.enUso) {
                Text(
                    "✓ En uso", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SurfaceWhite,
                    modifier = Modifier.clip(CircleShape).background(GreenPrimary).padding(horizontal = 12.dp, vertical = 4.dp)
                )
            } else {
                Button(
                    onClick = { onElegir(tema) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                ) { Text(if (tema.predeterminado) "Volver a este" else "Usar en esta planta") }
            }
        }
    }
}
