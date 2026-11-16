package com.plantia.ui.tienda

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import com.plantia.ui.collection.virtual.ANCHO_PAGINA
import com.plantia.ui.collection.virtual.ALTO_PAGINA
import com.plantia.ui.collection.virtual.paletaDeMaceta
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

/** Maceta de la tienda (dos por fila): su dibujo, el nombre, el precio o "Tuya" y el boton para ponerla a sus plantas. */
@Composable
fun TarjetaMaceta(
    maceta: TiendaItem,
    monedas: Int,
    ocupado: Boolean,
    onComprar: (TiendaItem) -> Unit,
    onAplicar: (TiendaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val color = paletaDeMaceta(maceta.codigo.orEmpty()).cuerpo
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.7f))
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(11.dp)).background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                MacetaPixel(maceta.codigo.orEmpty(), Modifier.size(width = 66.dp, height = (66 / PROPORCION_MACETA).dp))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(maceta.nombre, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                if (maceta.comprado) Etiqueta("Tuya", GreenLight, com.plantia.ui.theme.GreenDark)
                else if (!maceta.bloqueado) Etiqueta("🍃 ${maceta.precio}", com.plantia.ui.theme.AmberLight, com.plantia.ui.theme.AmberDark)
            }
            when {
                maceta.comprado ->
                    OutlinedButton(
                        onClick = { onAplicar(maceta) },
                        enabled = !ocupado,
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) { Text("Aplicar a plantas", fontSize = 12.sp, color = com.plantia.ui.theme.GreenDark) }
                maceta.bloqueado ->
                    OutlinedButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(34.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                        Text("🔒 Nivel ${maceta.nivelRequerido}", fontSize = 12.sp)
                    }
                else ->
                    Button(
                        onClick = { onComprar(maceta) },
                        enabled = !ocupado && monedas >= maceta.precio,
                        modifier = Modifier.fillMaxWidth().height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) { Text("Comprar", fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String, fondo: androidx.compose.ui.graphics.Color, color: androidx.compose.ui.graphics.Color) {
    Text(
        texto,
        fontSize = 11.sp,
        color = color,
        modifier = Modifier.clip(CircleShape).background(fondo).padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** Invernadero de la tienda: vista previa vertical en pixel art a la izquierda y, a la derecha, nombre, descripcion y boton. */
@Composable
fun TarjetaInvernadero(
    invernadero: TiendaItem,
    monedas: Int,
    ocupado: Boolean,
    onComprar: (TiendaItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = if (invernadero.comprado) androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.6f)) else null
    ) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FondoInvernadero(
                codigo = invernadero.codigo.orEmpty(),
                modifier = Modifier.width(62.dp).aspectRatio(ANCHO_PAGINA / ALTO_PAGINA).clip(RoundedCornerShape(9.dp))
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(invernadero.nombre, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                if (invernadero.descripcion.isNotBlank()) Text(invernadero.descripcion, fontSize = 11.sp, lineHeight = 15.sp, color = TextSecondary)
                Spacer(Modifier.height(2.dp))
                when {
                    invernadero.comprado -> Etiqueta("✓ Lo tienes · elígelo desde el chip de tu colección", GreenLight, com.plantia.ui.theme.GreenDark)
                    invernadero.bloqueado -> Etiqueta("🔒 Nivel ${invernadero.nivelRequerido}", GreenLight, TextSecondary)
                    else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Etiqueta("🍃 ${invernadero.precio}", com.plantia.ui.theme.AmberLight, com.plantia.ui.theme.AmberDark)
                        Button(
                            onClick = { onComprar(invernadero) },
                            enabled = !ocupado && monedas >= invernadero.precio,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp)
                        ) { Text("Comprar", fontSize = 12.sp) }
                    }
                }
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
