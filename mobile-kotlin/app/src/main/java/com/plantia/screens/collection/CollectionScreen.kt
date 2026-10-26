package com.plantia.screens.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.data.MockGardenData
import com.plantia.data.remote.ColeccionItemDto
import com.plantia.model.PlantSummary
import com.plantia.screens.collection.components.TarjetaDescubrirMas
import com.plantia.screens.collection.components.TarjetaDigital
import com.plantia.screens.collection.components.TarjetaEscaneada
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.MobilekotlinTheme
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.util.IconosOscurosEnBarraDeEstado

private const val PESTANA_ESCANEADAS = 0
private const val PESTANA_DIGITALES = 1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    onBack: () -> Unit = {},
    onPlantaEscaneada: (ColeccionItemDto) -> Unit = {},
    onPlantaDigital: (PlantSummary) -> Unit = {},
    onScan: () -> Unit = {},
    viewModel: CollectionViewModel = viewModel()
) {
    IconosOscurosEnBarraDeEstado()

    var pestana by rememberSaveable { mutableIntStateOf(PESTANA_ESCANEADAS) }
    // Plantas digitales (simuladas en memoria) y cual esta activa en el jardin.
    val digitales = remember { MockGardenData.plantasDigitales }
    val nombreActiva = remember { MockGardenData.nombreDeLaActiva }

    Scaffold(
        containerColor = BackgroundLight,
        // Los margenes del sistema se aplican a mano: la barra superior maneja la barra de estado
        // y la lista deja espacio abajo para la barra de navegacion sin cortar las tarjetas.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Mi colección", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val estado = viewModel.uiState) {
                CollectionUiState.Loading -> CircularProgressIndicator(
                    color = GreenPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
                is CollectionUiState.Error -> EstadoMensaje(
                    emoji = "📡",
                    titulo = "No se pudo cargar tu colección",
                    detalle = estado.mensaje,
                    boton = "Reintentar",
                    onBoton = viewModel::cargar
                )
                is CollectionUiState.Success -> ContenidoColeccion(
                    escaneadas = estado.escaneadas,
                    digitales = digitales,
                    nombreActiva = nombreActiva,
                    pestana = pestana,
                    onPestana = { pestana = it },
                    onPlantaEscaneada = onPlantaEscaneada,
                    onPlantaDigital = onPlantaDigital,
                    onScan = onScan
                )
            }
        }
    }
}

/** Cabecera + cuadricula. Vacio => mensaje; con plantas => cuadricula adaptativa. */
@Composable
internal fun ContenidoColeccion(
    escaneadas: List<ColeccionItemDto>,
    digitales: List<PlantSummary>,
    nombreActiva: String?,
    pestana: Int,
    onPestana: (Int) -> Unit,
    onPlantaEscaneada: (ColeccionItemDto) -> Unit,
    onPlantaDigital: (PlantSummary) -> Unit,
    onScan: () -> Unit
) {
    val vacia = if (pestana == PESTANA_ESCANEADAS) escaneadas.isEmpty() else digitales.isEmpty()
    val bordeInferior = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val soloLados = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))

    if (vacia) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(soloLados)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + bordeInferior),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Cabecera(escaneadas.size, digitales.size, pestana, onPestana)
            if (pestana == PESTANA_ESCANEADAS) {
                EstadoMensaje(
                    emoji = "🌱",
                    titulo = "Aún no tienes plantas",
                    detalle = "Escanea tu primera planta para empezar tu colección.",
                    boton = "Escanear",
                    onBoton = onScan
                )
            } else {
                EstadoMensaje(
                    emoji = "🪴",
                    titulo = "Aún no tienes plantas digitales",
                    detalle = "Digitaliza una planta escaneada para cuidarla en tu jardín.",
                    boton = "Escanear",
                    onBoton = onScan
                )
            }
        }
        return
    }

    // Columnas por ancho minimo: 2 en vertical y mas en horizontal, sin numeros fijos.
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        modifier = Modifier.fillMaxSize().then(soloLados),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + bordeInferior),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Cabecera(escaneadas.size, digitales.size, pestana, onPestana)
        }
        if (pestana == PESTANA_ESCANEADAS) {
            items(escaneadas, key = { "e${it.id}" }) { planta ->
                TarjetaEscaneada(planta, onClick = { onPlantaEscaneada(planta) })
            }
            item(span = { GridItemSpan(maxLineSpan) }) { TarjetaDescubrirMas(onClick = onScan) }
        } else {
            items(digitales, key = { "d${it.name}" }) { planta ->
                TarjetaDigital(planta, activa = planta.name == nombreActiva, onClick = { onPlantaDigital(planta) })
            }
        }
    }
}

@Composable
private fun Cabecera(nEscaneadas: Int, nDigitales: Int, pestana: Int, onPestana: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Contador abierto: la base de datos es dinamica, no hay un tope de especies.
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Plantas descubiertas",
                    fontSize = 15.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
                Text("$nEscaneadas / ∞", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GreenDark)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pestana("Escaneadas ($nEscaneadas)", pestana == PESTANA_ESCANEADAS, { onPestana(PESTANA_ESCANEADAS) }, Modifier.weight(1f))
            Pestana("Digitales ($nDigitales)", pestana == PESTANA_DIGITALES, { onPestana(PESTANA_DIGITALES) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun Pestana(texto: String, seleccionada: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (seleccionada) GreenPrimary else SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (seleccionada) Color.White else TextPrimary
        )
    }
}

@Composable
private fun EstadoMensaje(
    emoji: String,
    titulo: String,
    detalle: String,
    boton: String,
    onBoton: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(detalle, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onBoton,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.White)
        ) {
            Text(boton)
        }
    }
}

// ---------------------------------------------------------------------------
// Previews (datos de ejemplo SOLO para Previews; la pantalla real arranca vacia)
// ---------------------------------------------------------------------------

private val ejemploEscaneadas = listOf(
    ColeccionItemDto(1, 1, "Monstera", "Monstera deliciosa", null, 4, null, null, true),
    ColeccionItemDto(2, 2, "Fresa", "Fragaria ananassa", null, 1, null, null, false),
    ColeccionItemDto(3, 3, "Saguaro", "Carnegiea gigantea", null, 2, null, null, false)
)

@Preview(name = "Con plantas", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewConPlantas() {
    MobilekotlinTheme(dynamicColor = false) {
        ContenidoColeccion(ejemploEscaneadas, emptyList(), null, PESTANA_ESCANEADAS, {}, {}, {}, {})
    }
}

@Preview(name = "Vacia", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewVacia() {
    MobilekotlinTheme(dynamicColor = false) {
        ContenidoColeccion(emptyList(), emptyList(), null, PESTANA_ESCANEADAS, {}, {}, {}, {})
    }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 851, heightDp = 393)
@Composable
private fun PreviewLandscape() {
    MobilekotlinTheme(dynamicColor = false) {
        ContenidoColeccion(ejemploEscaneadas, emptyList(), null, PESTANA_ESCANEADAS, {}, {}, {}, {})
    }
}
