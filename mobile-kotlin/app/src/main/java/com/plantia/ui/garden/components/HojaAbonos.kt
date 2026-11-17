package com.plantia.ui.garden.components

import com.plantia.ui.theme.SobrePrimario

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.model.TiendaItem
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/**
 * "Abonos para [planta]": cada abono rinde distinto (medio dia, un dia, tres dias) y cuesta distinto.
 * Se elige uno y se usa en el momento; el servidor valida las hojas y el tope de un abono al dia.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaAbonos(
    nombrePlanta: String,
    abonos: List<TiendaItem>,
    hojas: Int?,
    onUsar: (TiendaItem) -> Unit,
    onTienda: () -> Unit,
    onCerrar: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BackgroundLight
    ) {
        Column(
            Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Abonos para $nombrePlanta", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "Hacen crecer a tu planta más rápido. Solo puedes usar un abono al día por planta." +
                    (hojas?.let { " Tienes $it 🍃." } ?: ""),
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(2.dp))
            if (abonos.isEmpty()) {
                Text("No hay abonos a la venta por ahora.", fontSize = 14.sp, color = TextSecondary)
            }
            abonos.forEach { abono ->
                val alcanza = hojas == null || hojas >= abono.precio
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                ) {
                    Row(
                        Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).background(GreenLight),
                            contentAlignment = Alignment.Center
                        ) { Text("🌿", fontSize = 24.sp) }
                        Column(Modifier.weight(1f)) {
                            Text(abono.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(abono.descripcion, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
                        }
                        Button(
                            onClick = { onUsar(abono) },
                            enabled = alcanza && !abono.bloqueado,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = SobrePrimario)
                        ) { Text(if (abono.bloqueado) "🔒" else "${abono.precio} 🍃", fontWeight = FontWeight.Bold) }
                    }
                }
            }
            TextButton(onClick = onTienda, modifier = Modifier.fillMaxWidth()) {
                Text("🛒 Ir a la tienda", color = GreenDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}
