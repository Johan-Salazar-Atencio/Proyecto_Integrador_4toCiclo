package com.plantia.ui.garden.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.model.AbonoInventario
import com.plantia.ui.components.AbonoEstilo
import com.plantia.ui.components.IconoAbono
import com.plantia.ui.components.pulsoAlTocar
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/**
 * "Abonos para [planta]": solo el contador de cada abono (suave, orgánico, reforzado) y el acceso a la tienda.
 * Usar uno descuenta 1 del contador; comprar se hace en la tienda. El servidor valida el tope de un abono al día.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaAbonos(
    nombrePlanta: String,
    abonos: List<AbonoInventario>,
    /** Id del abono que se está usando ahora; solo esa tarjeta muestra el progreso. */
    usandoId: Int? = null,
    /** Por qué no se pudo usar el último abono elegido; null si no hubo problema (QA-57). */
    aviso: String? = null,
    onUsar: (AbonoInventario) -> Unit,
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
                "Hacen crecer a tu planta más rápido. Solo puedes usar un abono al día por planta.",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(2.dp))
            if (aviso != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8EC)),
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Text(aviso, modifier = Modifier.padding(14.dp), fontSize = 14.sp, color = TextPrimary)
                }
            }
            if (abonos.isEmpty()) {
                Text("No hay abonos disponibles por ahora.", fontSize = 14.sp, color = TextSecondary)
            }
            abonos.forEach { abono -> TarjetaAbonoInventario(abono, usando = usandoId == abono.idItem, onUsar = { onUsar(abono) }) }
            TextButton(onClick = onTienda, modifier = Modifier.fillMaxWidth()) {
                Text("🛒 Ir a la tienda", color = GreenDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Una tarjeta por abono, con su color; tiene su propia fuente de interacción, así que solo se anima la que se toca. */
@Composable
private fun TarjetaAbonoInventario(abono: AbonoInventario, usando: Boolean, onUsar: () -> Unit) {
    val color = AbonoEstilo.color(abono.codigo)
    val fuente = remember { MutableInteractionSource() }
    val hay = abono.cantidad > 0
    Card(
        modifier = Modifier.fillMaxWidth().pulsoAlTocar(fuente),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = if (hay) color.copy(alpha = 0.12f) else SurfaceWhite),
        border = BorderStroke(1.5.dp, if (hay) color else DividerColor)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconoAbono(abono.codigo, 48.dp)
            Column(Modifier.weight(1f)) {
                Text(abono.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(abono.descripcion, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
                Text(
                    "Tienes ${abono.cantidad}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (hay) color else TextSecondary,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(color.copy(alpha = if (hay) 0.18f else 0.08f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            Button(
                onClick = onUsar,
                enabled = hay && !usando,
                interactionSource = fuente,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
            ) {
                if (usando) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                else Text("Usar", fontWeight = FontWeight.Bold)
            }
        }
    }
}
