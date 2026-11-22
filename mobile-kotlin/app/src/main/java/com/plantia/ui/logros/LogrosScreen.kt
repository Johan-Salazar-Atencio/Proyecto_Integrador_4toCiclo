package com.plantia.ui.logros

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.data.model.Logro
import com.plantia.data.model.LogrosResumen
import com.plantia.ui.components.BarraProgreso
import com.plantia.ui.logros.viewmodel.FiltroLogros
import com.plantia.ui.logros.viewmodel.LogrosUiState
import com.plantia.ui.logros.viewmodel.LogrosViewModel
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.FondoDegradado
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.Primario
import com.plantia.ui.theme.SobrePrimario
import com.plantia.ui.theme.SuperficieElevada
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.util.IconosOscurosEnBarraDeEstado

/**
 * Logros del usuario como en un videojuego: los desbloqueados a color con borde segun su rareza y los bloqueados
 * en gris, con candado y su avance. Tocar uno abre su detalle. La grilla se adapta al ancho (2 columnas en un
 * telefono, mas en horizontal o tableta).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogrosScreen(
    onBack: () -> Unit = {},
    viewModel: LogrosViewModel = viewModel(factory = LogrosViewModel.Factory)
) {
    IconosOscurosEnBarraDeEstado()
    val estado = viewModel.uiState

    // Al volver a la pantalla el avance se actualiza solo
    val ciclo = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(ciclo) {
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.cargar() }
    }

    // Se guarda el id (no el logro) para que sobreviva a girar la pantalla y se refresque con la recarga
    var abiertoId by rememberSaveable { mutableStateOf<Int?>(null) }
    val abierto = estado.resumen?.logros?.firstOrNull { it.idLogro == abiertoId }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(FondoDegradado),
        topBar = {
            TopAppBar(
                title = { Text("Logros", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        val resumen = estado.resumen
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                resumen == null && estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primario)
                }
                resumen == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    MensajeCentrado(
                        texto = estado.error ?: "No se pudieron cargar los logros",
                        accion = "Reintentar",
                        onAccion = viewModel::cargar
                    )
                }
                else -> GrillaLogros(resumen, estado, onFiltro = viewModel::elegirFiltro, onAbrir = { abiertoId = it.idLogro })
            }
        }
    }

    if (abierto != null) {
        ModalBottomSheet(
            onDismissRequest = { abiertoId = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SurfaceWhite
        ) {
            DetalleLogro(abierto)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GrillaLogros(
    resumen: LogrosResumen,
    estado: LogrosUiState,
    onFiltro: (FiltroLogros) -> Unit,
    onAbrir: (Logro) -> Unit
) {
    val visibles = estado.visibles
    LazyVerticalGrid(
        // 150dp de minimo: 2 columnas en un telefono de 360dp y mas cuando hay ancho
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "encabezado", span = { GridItemSpan(maxLineSpan) }) { Encabezado(resumen) }

        item(key = "filtros", span = { GridItemSpan(maxLineSpan) }) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FiltroLogros.entries.forEach { filtro ->
                    FilterChip(
                        selected = estado.filtro == filtro,
                        onClick = { onFiltro(filtro) },
                        label = { Text("${filtro.etiqueta} (${estado.conteo(filtro)})", fontSize = 13.sp) },
                        shape = RoundedCornerShape(50),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SurfaceWhite,
                            labelColor = TextPrimary,
                            selectedContainerColor = Primario,
                            selectedLabelColor = SobrePrimario
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = estado.filtro == filtro,
                            borderColor = DividerColor,
                            selectedBorderColor = Primario
                        )
                    )
                }
            }
        }

        if (visibles.isEmpty()) {
            item(key = "vacio", span = { GridItemSpan(maxLineSpan) }) {
                MensajeCentrado(
                    texto = when {
                        resumen.logros.isEmpty() -> "Todavía no hay logros disponibles."
                        estado.filtro == FiltroLogros.DESBLOQUEADOS -> "Aún no has desbloqueado ningún logro. ¡Cuida tus plantas para conseguir el primero!"
                        else -> "¡Ya desbloqueaste todos los logros!"
                    }
                )
            }
        }

        items(visibles, key = { it.idLogro }) { logro ->
            TarjetaLogro(logro, onClick = { onAbrir(logro) })
        }
    }
}

@Composable
private fun Encabezado(resumen: LogrosResumen) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Tu colección de logros", fontSize = 13.sp, color = TextSecondary)
        Text(
            "${resumen.desbloqueados} de ${resumen.total} desbloqueados",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        BarraProgreso(progreso = resumen.fraccion, color = Primario, alto = 10.dp, pista = SuperficieElevada)
        Text("${(resumen.fraccion * 100).toInt()} % completado", fontSize = 12.sp, color = TextSecondary)
    }
}

@Composable
private fun TarjetaLogro(logro: Logro, onClick: () -> Unit) {
    val forma = RoundedCornerShape(24.dp)
    val desbloqueado = logro.desbloqueado
    val colorRareza = logro.rareza.color()
    val estadoTexto = if (desbloqueado) "desbloqueado, ${logro.rareza.etiqueta}" else "bloqueado"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 176.dp)
            .clip(forma)
            .background(SurfaceWhite)
            .border(if (desbloqueado) 2.dp else 1.dp, if (desbloqueado) colorRareza else DividerColor, forma)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "${logro.titulo}, $estadoTexto" }
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InsigniaLogro(logro, tamano = 56.dp)
        Text(
            logro.titulo,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (desbloqueado) TextPrimary else TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        when {
            desbloqueado -> EtiquetaRareza(logro)
            logro.oculto -> Text("Logro secreto", fontSize = 12.sp, color = TextSecondary)
            else -> Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                BarraProgreso(progreso = logro.fraccion, color = TextSecondary, pista = SuperficieElevada)
                Text("${logro.progreso}/${logro.meta}", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}

/** Circulo con el icono del logro: a color con su rareza si esta desbloqueado, gris con candado si no. */
@Composable
private fun InsigniaLogro(logro: Logro, tamano: Dp) {
    val desbloqueado = logro.desbloqueado
    Box(contentAlignment = Alignment.BottomEnd) {
        Box(
            modifier = Modifier
                .size(tamano)
                .clip(CircleShape)
                .background(if (desbloqueado) logro.rareza.color() else SuperficieElevada),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconoDeLogro(logro.icono),
                contentDescription = null,
                tint = if (desbloqueado) SobrePrimario else TextSecondary.copy(alpha = 0.7f),
                modifier = Modifier.size(tamano * 0.55f)
            )
        }
        if (!desbloqueado) {
            Box(
                modifier = Modifier
                    .size(tamano * 0.38f)
                    .clip(CircleShape)
                    .background(SurfaceWhite)
                    .border(1.dp, DividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Lock, contentDescription = "Bloqueado", tint = TextSecondary, modifier = Modifier.size(tamano * 0.22f))
            }
        }
    }
}

@Composable
private fun EtiquetaRareza(logro: Logro) {
    Text(
        logro.rareza.etiqueta,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(logro.rareza.color().copy(alpha = 0.2f))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/** Hoja inferior con todo el logro: descripcion, avance o fecha, y recompensa. */
@Composable
private fun DetalleLogro(logro: Logro) {
    val recompensa = textoRecompensa(logro.recompensaXp, logro.recompensaMonedas)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        InsigniaLogro(logro, tamano = 88.dp)
        Text(logro.titulo, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
        if (!logro.oculto) EtiquetaRareza(logro)
        if (logro.descripcion.isNotBlank()) {
            Text(logro.descripcion, fontSize = 15.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }

        if (logro.desbloqueado) {
            val fecha = fechaLogro(logro.desbloqueadoEn)
            Text(
                if (fecha != null) "Desbloqueado el $fecha" else "Desbloqueado",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GreenDark
            )
        } else if (!logro.oculto) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                BarraProgreso(progreso = logro.fraccion, color = TextSecondary, alto = 10.dp, pista = SuperficieElevada)
                Text("Progreso: ${logro.progreso}/${logro.meta}", fontSize = 13.sp, color = TextSecondary)
            }
        }

        if (recompensa.isNotEmpty() && !logro.oculto) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SuperficieElevada)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recompensa", fontSize = 12.sp, color = TextSecondary)
                Text(recompensa, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun MensajeCentrado(texto: String, accion: String? = null, onAccion: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(texto, color = TextSecondary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        if (accion != null) {
            TextButton(onClick = onAccion) { Text(accion, color = GreenDark, fontWeight = FontWeight.Bold) }
        }
    }
}
