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
import com.plantia.ui.components.tarjeta
import com.plantia.ui.theme.Agua
import com.plantia.ui.theme.Primario
import com.plantia.ui.theme.SobrePrimario
import com.plantia.ui.theme.Tinte
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/** Maceta de la tienda (dos por fila): vista previa, nombre, "Tuya" y el boton para ponerla a sus plantas, o precio / nivel. */
@Composable
fun TarjetaMaceta(
    maceta: TiendaItem,
    monedas: Int,
    ocupado: Boolean,
    onComprar: (TiendaItem) -> Unit,
    onAplicar: (TiendaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.tarjeta().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(92.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.verticalGradient(listOf(Agua.copy(alpha = 0.22f), Primario.copy(alpha = 0.16f)))),
            contentAlignment = Alignment.BottomCenter
        ) {
            MacetaPixel(maceta.codigo.orEmpty(), Modifier.padding(bottom = 10.dp).size(width = 66.dp, height = (66 / PROPORCION_MACETA).dp))
        }
        Text(
            maceta.nombre, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, maxLines = 1,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        when {
            maceta.comprado -> {
                Text(
                    "Tuya", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Primario,
                    modifier = Modifier.padding(start = 2.dp).clip(CircleShape).background(Tinte).padding(horizontal = 8.dp, vertical = 2.dp)
                )
                OutlinedButton(
                    onClick = { onAplicar(maceta) },
                    enabled = !ocupado,
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, Primario),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) { Text("Aplicar a plantas", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Primario, maxLines = 1) }
            }
            maceta.bloqueado -> BotonBloqueado(maceta.nivelRequerido)
            else -> BotonComprar(maceta.precio, enabled = !ocupado && monedas >= maceta.precio, modifier = Modifier.fillMaxWidth()) { onComprar(maceta) }
        }
    }
}

/** Invernadero de la tienda: vista previa vertical en pixel art a la izquierda y, a la derecha, nombre, descripcion y boton. */
@Composable
fun TarjetaInvernadero(
    invernadero: TiendaItem,
    monedas: Int,
    ocupado: Boolean,
    onComprar: (TiendaItem) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().tarjeta().padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FondoInvernadero(
            codigo = invernadero.codigo.orEmpty(),
            modifier = Modifier.width(66.dp).aspectRatio(ANCHO_PAGINA / ALTO_PAGINA).clip(RoundedCornerShape(14.dp))
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(invernadero.nombre, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            if (invernadero.descripcion.isNotBlank()) Text(invernadero.descripcion, fontSize = 11.sp, lineHeight = 15.sp, color = TextSecondary)
            Spacer(Modifier.height(2.dp))
            when {
                invernadero.comprado -> Text(
                    "Tuyo · elígelo desde la píldora de tu colección", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Primario
                )
                invernadero.bloqueado -> BotonBloqueado(invernadero.nivelRequerido)
                else -> BotonComprar(invernadero.precio, enabled = !ocupado && monedas >= invernadero.precio, modifier = Modifier.fillMaxWidth()) { onComprar(invernadero) }
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
                colors = ButtonDefaults.buttonColors(containerColor = Primario, contentColor = SobrePrimario),
                shape = RoundedCornerShape(14.dp)
            ) { Text("Poner maceta (${elegidas.size})") }
        }
    }
}
