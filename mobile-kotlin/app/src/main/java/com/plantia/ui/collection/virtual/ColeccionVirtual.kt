package com.plantia.ui.collection.virtual

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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

    val alcance = rememberCoroutineScope()
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BarraSuperior(consulta, { consulta = it }, invernadero, invernaderosPropios, onElegirInvernadero)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { pagina ->
                if (pagina < paginas.size) {
                    PaginaInvernadero(
                        codigo = invernadero,
                        indicador = { Indicador(pagina, paginas.size + 1, enBaul.size, esBaul = false, modifier = Modifier.align(Alignment.BottomCenter)) },
                        plantas = paginas[pagina],
                        vacioPorBusqueda = consulta.isNotBlank() && activas.isEmpty(),
                        onAbrir = onAbrir,
                        onMenu = { conAcciones = it },
                        onScan = onScan
                    )
                } else {
                    PaginaBaul(enBaul, onSacarDelBaul, indicador = { Indicador(pagina, paginas.size + 1, enBaul.size, esBaul = true, modifier = Modifier.align(Alignment.BottomCenter)) })
                }
            }
            val total = paginas.size + 1
            if (total > 1) {
                FlechaPagina(Icons.Filled.ChevronLeft, "Página anterior", Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                    alcance.launch { pager.animateScrollToPage((pager.currentPage - 1 + total) % total) }
                }
                FlechaPagina(Icons.Filled.ChevronRight, "Página siguiente", Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                    alcance.launch { pager.animateScrollToPage((pager.currentPage + 1) % total) }
                }
            }
        }
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
    var buscando by rememberSaveable { mutableStateOf(consulta.isNotEmpty()) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MenuInvernadero(invernadero, propios, onElegir)
        if (buscando) {
            OutlinedTextField(
                value = consulta,
                onValueChange = onConsulta,
                modifier = Modifier.weight(1f).height(52.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Buscar planta", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { onConsulta(""); buscando = false }) { Icon(Icons.Filled.Close, contentDescription = "Cerrar búsqueda") }
                }
            )
        } else {
            Spacer(Modifier.weight(1f))
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(SurfaceWhite).clickable { buscando = true }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = GreenDark, modifier = Modifier.size(16.dp))
                Text("Buscar", fontSize = 12.sp, color = GreenDark)
            }
        }
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
    indicador: @Composable BoxScope.() -> Unit,
    plantas: List<PlantSummary>,
    vacioPorBusqueda: Boolean,
    onAbrir: (PlantSummary) -> Unit,
    onMenu: (PlantSummary) -> Unit,
    onScan: () -> Unit
) {
    // La escena ocupa todo el ancho (68 x 102 pixeles de cuadricula); si la pantalla es mas baja se recorta el zocalo
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val ancho = maxWidth
        val u = ancho / ANCHO_PAGINA
        val libre = if (plantas.size < PLANTAS_POR_PAGINA) plantas.size else null
        val alto = if (maxHeight > u * ALTO_PAGINA) maxHeight else u * ALTO_PAGINA
        Box(Modifier.align(Alignment.TopStart).width(ancho).height(alto)) {
            FondoInvernadero(codigo, Modifier.fillMaxSize(), huecoLibre = libre)
            for (fila in 0 until 2) {
                for (columna in 0 until 3) {
                    val indice = fila * 3 + columna
                    val lado = u * 22f
                    val x = u * (HuecoInvernadero.izquierda(columna) + HuecoInvernadero.ANCHO / 2f - 11f)
                    val y = u * (HuecoInvernadero.repisa(fila) - 22f)
                    val planta = plantas.getOrNull(indice)
                    Box(Modifier.offset(x, y).size(lado)) {
                        if (planta != null) PlantaEnEstante(planta, { onAbrir(planta) }, { onMenu(planta) })
                        else if (indice == plantas.size) Box(Modifier.fillMaxSize().clickable(onClickLabel = "Escanear una planta", onClick = onScan))
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize()) { indicador() }
        if (vacioPorBusqueda) {
            Text(
                "Ninguna planta coincide con tu búsqueda",
                modifier = Modifier.align(Alignment.Center).background(SurfaceWhite.copy(alpha = 0.9f), RoundedCornerShape(12.dp)).padding(12.dp),
                color = TextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center
            )
        }
    }
}

/** La planta en su maceta, apoyada en la repisa. Tocarla la abre; mantenerla pulsada abre sus acciones (apodo y baul). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlantaEnEstante(planta: PlantSummary, onAbrir: () -> Unit, onMenu: () -> Unit) {
    val sprites = rememberSprites()
    Box(
        Modifier
            .fillMaxSize()
            .combinedClickable(
                onClickLabel = "Abrir ${planta.name}",
                onLongClickLabel = "Acciones de ${planta.name}",
                onClick = onAbrir,
                onLongClick = onMenu
            )
            .semantics { contentDescription = planta.name }
    ) {
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
                error = { Text("🪴", fontSize = 28.sp, modifier = Modifier.align(Alignment.Center)) }
            )
        }
        MacetaPixel(
            planta.macetaCodigo,
            Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.64f).aspectRatio(PROPORCION_MACETA)
        )
        val marca = when {
            planta.muerta -> "💀"
            planta.enRecuperacion -> "⚠️"
            planta.refugiada -> "🛡️"
            else -> null
        }
        if (marca != null) Text(marca, fontSize = 14.sp, modifier = Modifier.align(Alignment.TopStart))
    }
}

@Composable
private fun PaginaBaul(plantas: List<PlantSummary>, onSacar: (PlantSummary) -> Unit, indicador: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
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
    indicador()
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
private fun Indicador(paginaActual: Int, total: Int, enBaul: Int, esBaul: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        if (esBaul) {
            Text(
                "Baúl · $enBaul guardada${if (enBaul == 1) "" else "s"}",
                fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(total) { i ->
                val activa = i == paginaActual
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (activa) 16.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (esBaul) (if (activa) GreenPrimary else TextSecondary.copy(alpha = 0.35f)) else androidx.compose.ui.graphics.Color.White.copy(alpha = if (activa) 1f else 0.55f))
                )
            }
        }
    }
}

@Composable
private fun FlechaPagina(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, modifier: Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = descripcion, tint = TextPrimary, modifier = Modifier.size(22.dp))
        }
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
