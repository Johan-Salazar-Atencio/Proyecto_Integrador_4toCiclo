package com.plantia.ui.collection.virtual

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.plantia.ui.components.tarjeta
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.Primario
import com.plantia.ui.theme.SobrePrimario
import com.plantia.ui.theme.SuperficieElevada
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
    val total = paginas.size + 1
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BarraSuperior(
            consulta, { consulta = it }, invernadero, invernaderosPropios, onElegirInvernadero,
            paginaActual = pager.currentPage, totalInvernaderos = paginas.size,
            guardadas = enBaul.size,
            onBaul = { alcance.launch { pager.animateScrollToPage(paginas.size) } },
            onPagina = { alcance.launch { pager.animateScrollToPage(it) } }
        )

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .tarjeta()
        ) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { pagina ->
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
            // Sin vuelta al principio: el baúl es la última pantalla y el invernadero la primera.
            if (pager.currentPage > 0) {
                FlechaPagina(Icons.Filled.ChevronLeft, "Página anterior", Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) {
                    alcance.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                }
            }
            if (pager.currentPage < total - 1) {
                FlechaPagina(Icons.Filled.ChevronRight, "Página siguiente", Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                    alcance.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
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
    onElegir: (TiendaItem) -> Unit,
    paginaActual: Int,
    totalInvernaderos: Int,
    guardadas: Int,
    onBaul: () -> Unit,
    onPagina: (Int) -> Unit
) {
    var buscando by rememberSaveable { mutableStateOf(consulta.isNotEmpty()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MenuInvernadero(invernadero, propios, onElegir)
            Spacer(Modifier.weight(1f))
            val enBaul = paginaActual >= totalInvernaderos
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    if (enBaul) "Baúl" else "Página ${paginaActual + 1} de $totalInvernaderos",
                    fontSize = 11.sp, color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    repeat(totalInvernaderos) { i ->
                        val activa = i == paginaActual
                        Box(
                            Modifier
                                .height(7.dp)
                                .width(if (activa) 18.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (activa) Primario else DividerColor)
                                .clickable(onClickLabel = "Ir a la página ${i + 1}") { onPagina(i) }
                        )
                    }
                }
            }
            // El baúl va después de las páginas del invernadero: su propio botón, con las plantas que guarda.
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (enBaul) Primario else SuperficieElevada)
                    .border(1.dp, if (enBaul) Primario else DividerColor, RoundedCornerShape(50))
                    .clickable(onClickLabel = if (enBaul) "Volver al invernadero" else "Abrir el baúl") {
                        if (enBaul) onPagina(0) else onBaul()
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    Icons.Filled.Inventory2, contentDescription = null,
                    tint = if (enBaul) SobrePrimario else TextSecondary, modifier = Modifier.size(16.dp)
                )
                Text(
                    "$guardadas", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (enBaul) SobrePrimario else TextPrimary
                )
            }
            Box(
                Modifier
                    .size(40.dp)
                    .tarjeta(CircleShape)
                    .clickable(onClickLabel = "Buscar planta") { buscando = !buscando; if (!buscando) onConsulta("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Search, contentDescription = "Buscar", tint = TextPrimary, modifier = Modifier.size(20.dp))
            }
        }
        if (buscando) {
            OutlinedTextField(
                value = consulta,
                onValueChange = onConsulta,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text("Buscar planta", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { onConsulta(""); buscando = false }) { Icon(Icons.Filled.Close, contentDescription = "Cerrar búsqueda") }
                }
            )
        }
    }
}

/** Pildora con el tema de invernadero puesto; al tocarla lista solo los que el usuario tiene. */
@Composable
private fun MenuInvernadero(actual: String, propios: List<TiendaItem>, onElegir: (TiendaItem) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    val nombre = propios.firstOrNull { it.codigo == actual }?.nombre ?: nombreDelInvernadero(actual)
    Box {
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(SuperficieElevada)
                .border(1.dp, DividerColor, RoundedCornerShape(50))
                .clickable { abierto = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Eco, contentDescription = null, tint = Primario, modifier = Modifier.size(16.dp))
            Text(nombre.take(16), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary, maxLines = 1)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
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
    // La escena (68 x 102 pixeles de cuadricula) se ve ENTERA, sin recortes: en vertical se ajusta al ancho y al
    // alto disponibles (la que mas limite) y queda centrada; en horizontal falta alto, asi que queda angosta,
    // centrada, y la pagina se desplaza hacia abajo para verla completa.
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val horizontal = maxWidth > maxHeight
        val ancho = if (horizontal) minOf(maxWidth, 420.dp) else minOf(maxWidth, maxHeight * (ANCHO_PAGINA / ALTO_PAGINA))
        val u = ancho / ANCHO_PAGINA
        val libre = if (plantas.size < PLANTAS_POR_PAGINA) plantas.size else null
        val alto = u * ALTO_PAGINA
        Box(
            Modifier.fillMaxSize().then(if (horizontal) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            contentAlignment = if (horizontal) Alignment.TopCenter else Alignment.Center
        ) {
            Box(Modifier.width(ancho).height(alto)) {
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
                if (vacioPorBusqueda) {
                    Text(
                        "Ninguna planta coincide con tu búsqueda",
                        modifier = Modifier.align(Alignment.Center).background(SurfaceWhite.copy(alpha = 0.92f), RoundedCornerShape(16.dp)).padding(12.dp),
                        color = TextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** La planta en su maceta, apoyada en la repisa. Tocarla la abre; mantenerla pulsada abre sus acciones (apodo y baul). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlantaEnEstante(planta: PlantSummary, onAbrir: () -> Unit, onMenu: () -> Unit) {
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
        // La planta con SU maceta, dibujada por el mismo motor que en el jardin
        PlantaPixel(
            apariencia = planta.apariencia,
            diasCuidados = planta.diasCuidados,
            diasTotales = planta.diasTotales,
            marchita = planta.healthPercent < 30 || planta.enRecuperacion || planta.muerta,
            muerta = planta.muerta,
            descripcion = planta.name,
            modifier = Modifier.fillMaxSize(),
            macetaCodigo = planta.macetaCodigo,
            enEstante = true,
            animar = false
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
private fun PaginaBaul(plantas: List<PlantSummary>, onSacar: (PlantSummary) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Baúl", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(SuperficieElevada).padding(8.dp),
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
            colors = ButtonDefaults.buttonColors(containerColor = Primario, contentColor = SobrePrimario),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Sacar", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold) }
    }
}

@Composable
private fun FlechaPagina(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, modifier: Modifier, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        modifier = modifier.size(32.dp),
        shape = CircleShape,
        color = SurfaceWhite.copy(alpha = 0.9f),
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
internal fun DialogoApodo(planta: PlantSummary, onGuardar: (String) -> Unit, onCancelar: () -> Unit) {
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
