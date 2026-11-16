package com.plantia.ui.tienda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.TiendaItem
import com.plantia.ui.collection.virtual.FondoInvernadero
import com.plantia.ui.collection.virtual.MacetaPixel
import com.plantia.ui.collection.virtual.PROPORCION_MACETA
import com.plantia.ui.collection.virtual.filtrarPlantas
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/** Maceta de la tienda: su dibujo, el precio y, si ya la tiene, el boton para ponerla a sus plantas. */
@Composable
fun TarjetaMaceta(
    maceta: TiendaItem,
    monedas: Int,
    ocupado: Boolean,
    onComprar: (TiendaItem) -> Unit,
    onAplicar: (TiendaItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)).background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                MacetaPixel(maceta.codigo.orEmpty(), Modifier.size(width = 56.dp, height = (56 / PROPORCION_MACETA).dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(maceta.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (maceta.descripcion.isNotBlank()) Text(maceta.descripcion, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
                when {
                    maceta.comprado ->
                        Button(
                            onClick = { onAplicar(maceta) },
                            enabled = !ocupado,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Poner a mis plantas", fontSize = 13.sp) }
                    maceta.bloqueado ->
                        OutlinedButton(onClick = {}, enabled = false) { Text("🔒 Nivel ${maceta.nivelRequerido}", fontSize = 13.sp) }
                    else ->
                        Button(
                            onClick = { onComprar(maceta) },
                            enabled = !ocupado && monedas >= maceta.precio,
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Comprar · ${maceta.precio} 🍃", fontSize = 13.sp) }
                }
            }
        }
    }
}

/** Invernadero de la tienda: vista previa en pixel art; se elige despues desde el chip de la coleccion. */
@Composable
fun TarjetaInvernadero(
    invernadero: TiendaItem,
    monedas: Int,
    ocupado: Boolean,
    onComprar: (TiendaItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FondoInvernadero(
                codigo = invernadero.codigo.orEmpty(),
                modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp))
            )
            Text(invernadero.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (invernadero.descripcion.isNotBlank()) Text(invernadero.descripcion, fontSize = 12.sp, lineHeight = 16.sp, color = TextSecondary)
            when {
                invernadero.comprado ->
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                        Text("✓ Lo tienes · elígelo desde el chip de tu colección")
                    }
                invernadero.bloqueado ->
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) { Text("🔒 Nivel ${invernadero.nivelRequerido}") }
                else ->
                    Button(
                        onClick = { onComprar(invernadero) },
                        enabled = !ocupado && monedas >= invernadero.precio,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                    ) { Text("Comprar · ${invernadero.precio} 🍃") }
            }
        }
    }
}

/** Hoja para elegir a cuales plantas virtuales se les pone la maceta (una, varias o todas, con buscador). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaAplicarMaceta(
    maceta: TiendaItem,
    plantas: List<PlantSummary>,
    cargando: Boolean,
    onAplicar: (List<Int>) -> Unit,
    onCerrar: () -> Unit
) {
    var consulta by remember { mutableStateOf("") }
    var elegidas by remember { mutableStateOf(setOf<Int>()) }
    val vistas = filtrarPlantas(plantas, consulta)
    val idsVistas = vistas.mapNotNull { it.id }
    val todasMarcadas = idsVistas.isNotEmpty() && elegidas.containsAll(idsVistas)

    // Abierta completa: el boton de abajo siempre se ve, aunque haya muchas plantas
    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Poner «${maceta.nombre}» a…", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            OutlinedTextField(
                value = consulta,
                onValueChange = { consulta = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Buscar planta", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${elegidas.size} elegida${if (elegidas.size == 1) "" else "s"}", fontSize = 13.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                TextButton(onClick = { elegidas = if (todasMarcadas) elegidas - idsVistas.toSet() else elegidas + idsVistas }) {
                    Text(if (todasMarcadas) "Quitar todas" else "Todas")
                }
            }
            when {
                cargando -> Text("Cargando tus plantas…", color = TextSecondary, fontSize = 14.sp)
                plantas.isEmpty() -> Text("Aún no tienes plantas virtuales en el invernadero.", color = TextSecondary, fontSize = 14.sp)
                else -> LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(vistas, key = { it.id ?: it.name }) { planta ->
                        val id = planta.id ?: return@items
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { elegidas = if (id in elegidas) elegidas - id else elegidas + id }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(checked = id in elegidas, onCheckedChange = { elegidas = if (it) elegidas + id else elegidas - id })
                            MacetaPixel(planta.macetaCodigo, Modifier.size(width = 32.dp, height = (32 / PROPORCION_MACETA).dp))
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(planta.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(planta.species, fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = { onAplicar(elegidas.toList()) },
                enabled = elegidas.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Poner maceta (${elegidas.size})") }
        }
    }
}
