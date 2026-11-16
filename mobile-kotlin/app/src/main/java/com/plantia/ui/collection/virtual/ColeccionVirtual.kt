package com.plantia.ui.collection.virtual

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.TiendaItem
import com.plantia.ui.escena.PlantaPixel
import com.plantia.ui.escena.rememberSprites
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/**
 * Coleccion virtual: un invernadero en pixel art con las plantas en macetas, de a 6 por pantalla (desliza al
 * costado para ver las siguientes) y, al final, el baul. Tocar una planta la abre en el jardin; el "⋮" abre
 * sus acciones (apodo y baul).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColeccionVirtual(
    plantas: List<PlantSummary>,
    invernadero: String,
    invernaderosPropios: List<TiendaItem>,
    onElegirInvernadero: (TiendaItem) -> Unit,
    onAbrir: (PlantSummary) -> Unit,
    onRenombrar: (PlantSummary, String) -> Unit,
    onGuardarEnBaul: (PlantSummary) -> Unit,
    onSacarDelBaul: (PlantSummary) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    var consulta by rememberSaveable { mutableStateOf("") }
    var conAcciones by remember { mutableStateOf<PlantSummary?>(null) }
    var renombrando by remember { mutableStateOf<PlantSummary?>(null) }

    val vistas = filtrarPlantas(plantas, consulta)
    val activas = vistas.filter { !it.enBaul }
    val enBaul = vistas.filter { it.enBaul }
    val paginas = paginasDelInvernadero(activas)
    val pager = rememberPagerState(pageCount = { paginas.size + 1 })

    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BarraSuperior(consulta, { consulta = it }, invernadero, invernaderosPropios, onElegirInvernadero)

        HorizontalPager(state = pager, modifier = Modifier.weight(1f).fillMaxWidth()) { pagina ->
            if (pagina < paginas.size) {
                PaginaInvernadero(
                    codigo = invernadero,
                    plantas = paginas[pagina],
                    vacioPorBusqueda = consulta.isNotBlank() && activas.isEmpty(),
                    onAbrir = onAbrir,
                    onMenu = { conAcciones = it },
                    onScan = onScan
                )
            } else {
                PaginaBaul(enBaul, onSacarDelBaul)
            }
        }

        Indicador(
            paginaActual = pager.currentPage,
            totalInvernadero = paginas.size,
            enBaul = enBaul.size
        )
    }

    conAcciones?.let { planta ->
        ModalBottomSheet(onDismissRequest = { conAcciones = null }) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp).padding(bottom = 24.dp)) {
                Text(planta.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(planta.species, fontSize = 13.sp, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Accion("🌱  Abrir en el jardín") { conAcciones = null; onAbrir(planta) }
                Accion("✏️  Cambiar apodo") { conAcciones = null; renombrando = planta }
                if (planta.enRecuperacion) {
                    Text(
                        "Está en recuperación: abónala antes de guardarla en el baúl",
                        fontSize = 13.sp, color = TextSecondary, modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    Accion("🧳  Guardar en el baúl") { conAcciones = null; onGuardarEnBaul(planta) }
                }
            }
        }
    }

    renombrando?.let { planta ->
        DialogoApodo(
            planta = planta,
            onGuardar = { apodo -> renombrando = null; onRenombrar(planta, apodo) },
            onCancelar = { renombrando = null }
        )
    }
}

@Composable
private fun BarraSuperior(
    consulta: String,
    onConsulta: (String) -> Unit,
    invernadero: String,
    propios: List<TiendaItem>,
    onElegir: (TiendaItem) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = consulta,
            onValueChange = onConsulta,
            modifier = Modifier.weight(1f),
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Buscar planta", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (consulta.isNotEmpty()) {
                    IconButton(onClick = { onConsulta("") }) { Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda") }
                }
            }
        )
        MenuInvernadero(invernadero, propios, onElegir)
    }
}

/** Chip con el invernadero puesto; al tocarlo lista solo los que el usuario tiene. */
@Composable
private fun MenuInvernadero(actual: String, propios: List<TiendaItem>, onElegir: (TiendaItem) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val nombre = propios.firstOrNull { it.codigo == actual }?.nombre ?: nombreDelInvernadero(actual)
    Box {
        FilterChip(
            selected = true,
            onClick = { abierto = true },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GreenLight,
                selectedLabelColor = GreenDark
            ),
            label = { Text("🌿 ${nombre.take(14)} ▾", fontSize = 12.sp, maxLines = 1) }
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            if (propios.isEmpty()) {
                DropdownMenuItem(text = { Text("Aún no hay otros temas") }, onClick = { abierto = false })
            }
            propios.forEach { item ->
                DropdownMenuItem(
                    text = { Text(if (item.codigo == actual) "✓ ${item.nombre}" else item.nombre) },
                    onClick = {
                        abierto = false
                        if (item.codigo != actual) onElegir(item)
                    }
                )
            }
        }
    }
}

@Composable
private fun PaginaInvernadero(
    codigo: String,
    plantas: List<PlantSummary>,
    vacioPorBusqueda: Boolean,
    onAbrir: (PlantSummary) -> Unit,
    onMenu: (PlantSummary) -> Unit,
    onScan: () -> Unit
) {
    val estante = paletaDeEstante(codigo)
    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        FondoInvernadero(codigo, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 16.dp)) {
            for (fila in 0 until 2) {
                Column(Modifier.weight(1f).fillMaxWidth()) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (columna in 0 until 3) {
                            val planta = plantas.getOrNull(fila * 3 + columna)
                            Box(Modifier.weight(1f).fillMaxHeight()) {
                                if (planta != null) {
                                    PlantaEnEstante(planta, { onAbrir(planta) }, { onMenu(planta) })
                                } else {
                                    HuecoVacio(esElPrimero = plantas.isEmpty() && fila == 0 && columna == 1, onScan = onScan)
                                }
                            }
                        }
                    }
                    // Tabla del estante
                    Box(Modifier.fillMaxWidth().height(10.dp).background(estante.tabla))
                    Box(Modifier.fillMaxWidth().height(4.dp).background(estante.canto))
                }
            }
        }
        if (vacioPorBusqueda) {
            Text(
                "Ninguna planta coincide con tu búsqueda",
                modifier = Modifier.align(Alignment.Center).background(SurfaceWhite.copy(alpha = 0.9f), RoundedCornerShape(12.dp)).padding(12.dp),
                color = TextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PlantaEnEstante(planta: PlantSummary, onAbrir: () -> Unit, onMenu: () -> Unit) {
    val sprites = rememberSprites()
    // Las plantas se apoyan en el estante y su contenido no se estira con la pantalla: el "⋮" queda junto a ellas
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
    Box(Modifier.fillMaxWidth().aspectRatio(0.85f).clickable(onClick = onAbrir)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                Box(Modifier.aspectRatio(1f).fillMaxHeight()) {
                    if (sprites != null) {
                        PlantaPixel(
                            sprites = sprites,
                            apariencia = planta.apariencia,
                            diasCuidados = planta.diasCuidados,
                            diasTotales = planta.diasTotales,
                            marchita = planta.enRecuperacion || planta.muerta,
                            descripcion = planta.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SubcomposeAsyncImage(
                            model = planta.imageUrl,
                            contentDescription = planta.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                            error = { Text("🪴", fontSize = 36.sp, modifier = Modifier.align(Alignment.Center)) }
                        )
                    }
                    MacetaPixel(
                        planta.macetaCodigo,
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.5f).aspectRatio(PROPORCION_MACETA)
                    )
                }
            }
            Text(
                planta.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.ui.graphics.Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .background(androidx.compose.ui.graphics.Color(0x66000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            )
        }
        val marca = when {
            planta.muerta -> "💀"
            planta.enRecuperacion -> "⚠️"
            planta.refugiada -> "🛡️"
            else -> null
        }
        if (marca != null) Text(marca, fontSize = 16.sp, modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
        IconButton(onClick = onMenu, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Acciones de ${planta.name}", tint = androidx.compose.ui.graphics.Color.White)
        }
    }
    }
}

@Composable
private fun HuecoVacio(esElPrimero: Boolean, onScan: () -> Unit) {
    Column(
        Modifier.fillMaxSize().clickable(onClick = onScan),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (esElPrimero) {
            Text(
                "Escanea y digitaliza tu primera planta",
                fontSize = 11.sp, color = androidx.compose.ui.graphics.Color.White, textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        MacetaPixel("TERRACOTA", Modifier.fillMaxWidth(0.5f).aspectRatio(PROPORCION_MACETA).alpha(0.28f))
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun PaginaBaul(plantas: List<PlantSummary>, onSacar: (PlantSummary) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🧳 Baúl", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(
            "Las plantas guardadas aquí quedan congeladas: no crecen, no se desgastan y no les pasa nada. Solo liberan espacio.",
            fontSize = 12.sp, color = TextSecondary
        )
        if (plantas.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("El baúl está vacío", color = TextSecondary, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(plantas, key = { it.id ?: it.name }) { planta ->
                    FilaDelBaul(planta, onSacar = { onSacar(planta) })
                }
            }
        }
    }
}

@Composable
private fun FilaDelBaul(planta: PlantSummary, onSacar: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GreenLight).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(SurfaceWhite), contentAlignment = Alignment.Center) {
            SubcomposeAsyncImage(
                model = planta.imageUrl,
                contentDescription = planta.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                error = { Text("🪴", fontSize = 24.sp) }
            )
        }
        Column(Modifier.weight(1f)) {
            Text(planta.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(planta.species, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Button(
            onClick = onSacar,
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Sacar", fontSize = 13.sp) }
    }
}

@Composable
private fun Indicador(paginaActual: Int, totalInvernadero: Int, enBaul: Int) {
    val total = totalInvernadero + 1
    Column(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .size(if (i == paginaActual) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (i == paginaActual) GreenPrimary else TextSecondary.copy(alpha = 0.35f))
                )
            }
        }
        Text(
            if (paginaActual < totalInvernadero) "Invernadero ${paginaActual + 1} de $totalInvernadero · desliza para ver más"
            else "Baúl · $enBaul guardada${if (enBaul == 1) "" else "s"}",
            fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun Accion(texto: String, onClick: () -> Unit) {
    Text(
        texto,
        fontSize = 16.sp,
        color = TextPrimary,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp)
    )
}

@Composable
private fun DialogoApodo(planta: PlantSummary, onGuardar: (String) -> Unit, onCancelar: () -> Unit) {
    var texto by remember { mutableStateOf(planta.name) }
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Apodo de tu planta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { if (it.length <= MAX_APODO) texto = it },
                    singleLine = true,
                    label = { Text(planta.species) }
                )
                Text("Déjalo vacío para volver al nombre de la especie · ${texto.length}/$MAX_APODO", fontSize = 12.sp, color = TextSecondary)
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(texto.trim()) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

/** El servidor rechaza apodos de mas de 50 caracteres. */
internal const val MAX_APODO = 50
