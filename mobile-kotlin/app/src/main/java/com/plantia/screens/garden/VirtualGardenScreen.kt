package com.plantia.screens.garden

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.data.AccionCuidado
import com.plantia.data.MockGardenData
import com.plantia.model.PlantSummary
import com.plantia.screens.garden.components.AnimatedStatBar
import com.plantia.screens.garden.components.CareActionButton
import com.plantia.screens.garden.components.PlantFrame
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.BlueInfo
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.MobilekotlinTheme
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.util.IconosOscurosEnBarraDeEstado
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Logica de presentacion (derivada de los porcentajes; el backend no manda animo ni consejos)
// ---------------------------------------------------------------------------

internal fun estadoAnimo(planta: PlantSummary): String = when {
    planta.healthPercent >= 70 -> "Feliz"
    planta.healthPercent >= 40 -> "Regular"
    else -> "Triste"
}

/** Consejo segun la necesidad mas baja; si todo va bien, un mensaje positivo. */
internal fun consejo(planta: PlantSummary): String {
    val (minimo, mensaje) = listOf(
        planta.humidityPercent to "A ${planta.name} le falta agua: ¡riégala!",
        planta.lightPercent to "${planta.name} necesita más luz: sácala al sol.",
        planta.healthPercent to "${planta.name} está débil: un poco de abono le ayudará."
    ).minBy { it.first }
    return if (minimo >= 70) "${planta.name} está en excelente estado. ¡Sigue así!" else mensaje
}

// ---------------------------------------------------------------------------
// Pantalla
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualGardenScreen(
    onBack: () -> Unit = {},
    onScan: () -> Unit = {},
    viewModel: GardenViewModel = viewModel()
) {
    IconosOscurosEnBarraDeEstado()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BackgroundLight,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Jardín virtual", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
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
                GardenUiState.Loading -> CircularProgressIndicator(
                    color = GreenPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
                GardenUiState.SinPlanta -> EstadoMensaje(
                    emoji = "🌱",
                    titulo = "Aún no tienes una planta",
                    detalle = "Escanea una planta para empezar tu jardín.",
                    boton = "Escanear",
                    onBoton = onScan
                )
                is GardenUiState.Error -> EstadoMensaje(
                    emoji = "📡",
                    titulo = "No se pudo cargar el jardín",
                    detalle = estado.mensaje,
                    boton = "Reintentar",
                    onBoton = viewModel::cargar
                )
                is GardenUiState.Success -> JardinContenido(
                    planta = estado.planta,
                    onCuidar = { accion ->
                        val mensaje = viewModel.cuidar(accion)
                        if (mensaje.isNotEmpty()) {
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(mensaje)
                            }
                        }
                    }
                )
            }
        }
    }
}

/** Contenido con planta. Vertical: apilado. Horizontal: marco a la izquierda, datos a la derecha. */
@Composable
internal fun JardinContenido(planta: PlantSummary, onCuidar: (AccionCuidado) -> Unit) {
    val config = LocalConfiguration.current
    val apaisado = config.screenWidthDp > config.screenHeightDp

    if (apaisado) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.weight(0.42f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                PlantFrame(
                    nombre = planta.name,
                    especie = planta.species,
                    animo = estadoAnimo(planta),
                    imageUrl = planta.imageUrl,
                    modifier = Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true)
                )
            }
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TarjetaVital(planta)
                TarjetaAcciones(onCuidar)
                TarjetaConsejo(consejo(planta))
                Spacer(Modifier.height(4.dp))
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            PlantFrame(
                nombre = planta.name,
                especie = planta.species,
                animo = estadoAnimo(planta),
                imageUrl = planta.imageUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
            TarjetaVital(planta)
            TarjetaAcciones(onCuidar)
            TarjetaConsejo(consejo(planta))
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Tarjetas
// ---------------------------------------------------------------------------

@Composable
private fun TarjetaBlanca(modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { contenido() }
    }
}

@Composable
private fun TarjetaVital(planta: PlantSummary) {
    TarjetaBlanca {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Estado vital",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text("Último riego: ${planta.lastWatered}", fontSize = 12.sp, color = TextSecondary)
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AnimatedStatBar("Salud", Icons.Filled.Favorite, planta.healthPercent)
            AnimatedStatBar("Agua", Icons.Filled.WaterDrop, planta.humidityPercent)
            AnimatedStatBar("Luz", Icons.Filled.WbSunny, planta.lightPercent)
        }
    }
}

@Composable
private fun TarjetaAcciones(onCuidar: (AccionCuidado) -> Unit) {
    TarjetaBlanca {
        Text("Acciones de cuidado", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Mantén el balance de tu planta", fontSize = 13.sp, color = TextSecondary)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccionCuidado.entries.forEach { accion ->
                val (icono, color) = when (accion) {
                    AccionCuidado.REGAR -> Icons.Filled.WaterDrop to BlueInfo
                    AccionCuidado.SOL -> Icons.Filled.WbSunny to AmberAlert
                    AccionCuidado.ABONAR -> Icons.Filled.Science to GreenPrimary
                }
                CareActionButton(
                    etiqueta = accion.etiqueta,
                    pista = accion.pista,
                    icono = icono,
                    colorIcono = Color.White,
                    colorFondo = color,
                    onClick = { onCuidar(accion) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TarjetaConsejo(texto: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GreenLight)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = GreenDark, modifier = Modifier.size(24.dp))
            Text(texto, fontSize = 14.sp, lineHeight = 20.sp, color = TextPrimary)
        }
    }
}

// ---------------------------------------------------------------------------
// Estados sin planta / error
// ---------------------------------------------------------------------------

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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
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
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Portrait", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewPortrait() {
    MobilekotlinTheme(dynamicColor = false) {
        Box(Modifier.fillMaxSize()) { JardinContenido(MockGardenData.plantaSimulada) {} }
    }
}

@Preview(name = "Planta sedienta", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewSedienta() {
    MobilekotlinTheme(dynamicColor = false) {
        Box(Modifier.fillMaxSize()) { JardinContenido(MockGardenData.plantaSedienta) {} }
    }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 851, heightDp = 393)
@Composable
private fun PreviewLandscape() {
    MobilekotlinTheme(dynamicColor = false) {
        Box(Modifier.fillMaxSize()) { JardinContenido(MockGardenData.plantaSimulada) {} }
    }
}
