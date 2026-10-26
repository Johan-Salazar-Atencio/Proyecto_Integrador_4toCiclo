package com.plantia.screens.detail

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.plantia.data.MockGardenData
import com.plantia.data.MockPlantData
import com.plantia.data.ScanResultStore
import com.plantia.data.remote.CuidadoSeccionDto
import com.plantia.data.remote.ScanResultDto
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.BlueInfo
import com.plantia.ui.theme.BlueLight
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.MobilekotlinTheme
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import kotlin.math.roundToInt

private const val NOMBRE_DESCONOCIDA = "Planta desconocida"

/** Tipos de `CuidadoSeccionDto.tipo` que alimentan el diagnostico en vez de las tarjetas. */
private val TIPOS_DIAGNOSTICO = setOf("PLAGAS", "TOXICIDAD", "OTRO")

/** Orden fijo de las tarjetas: lo basico primero, sin importar el orden que devuelva la IA. */
private val PRIORIDAD_TIPO = mapOf("RIEGO" to 0, "LUZ" to 1, "TEMPERATURA" to 2, "SUSTRATO" to 3, "PODA" to 4, "ABONO" to 5)

private val RojoCalor = Color(0xFFEF4444)
private val RojoCalorLight = Color(0xFFFEE2E2)

// ---------------------------------------------------------------------------
// Mapeo DTO -> vista (funciones puras)
// ---------------------------------------------------------------------------

/**
 * Spring responde "Planta desconocida" cuando el motor IA no responde; Gemini, por su parte,
 * puede contestar "No identificado (imagen sin planta)" con confianza 0.
 */
internal fun ScanResultDto.identificacionFallida(): Boolean =
    nombreComun.isNullOrBlank() ||
        nombreComun.trim().equals(NOMBRE_DESCONOCIDA, ignoreCase = true) ||
        nombreComun.trim().startsWith("No identificado", ignoreCase = true)

/** Tarjetas de cuidado: todo lo que no es advertencia, en el orden del servidor. */
internal fun ScanResultDto.tarjetasCuidado(): List<CuidadoSeccionDto> =
    cuidados.orEmpty()
        .filter { it.tipo?.uppercase() !in TIPOS_DIAGNOSTICO && !it.contenido.isNullOrBlank() }
        .sortedWith(compareBy({ PRIORIDAD_TIPO[it.tipo?.uppercase()] ?: Int.MAX_VALUE }, { it.orden }))

/** Texto del diagnostico: contenido de PLAGAS/TOXICIDAD/OTRO concatenado; null si no hay. */
internal fun ScanResultDto.textoDiagnostico(): String? =
    cuidados.orEmpty()
        .filter { it.tipo?.uppercase() in TIPOS_DIAGNOSTICO }
        .sortedBy { it.orden }
        .mapNotNull { it.contenido?.trim()?.takeIf(String::isNotEmpty) }
        .takeIf { it.isNotEmpty() }
        ?.joinToString(separator = "\n\n")

/** `confianza` llega en escala 0..100. */
internal fun ScanResultDto.etiquetaPrecision(): String? =
    confianza?.takeIf { it > 0.0 }?.let {
        "IA Identificado ${it.coerceIn(0.0, 100.0).roundToInt()}% Precisión"
    }

private data class EstiloCuidado(val icono: ImageVector, val color: Color, val fondo: Color)

private fun estiloDe(tipo: String?): EstiloCuidado = when (tipo?.uppercase()) {
    "RIEGO" -> EstiloCuidado(Icons.Filled.WaterDrop, BlueInfo, BlueLight)
    "LUZ" -> EstiloCuidado(Icons.Filled.WbSunny, AmberAlert, AmberLight)
    "TEMPERATURA" -> EstiloCuidado(Icons.Filled.Thermostat, RojoCalor, RojoCalorLight)
    "SUSTRATO" -> EstiloCuidado(Icons.Filled.Grass, GreenDark, GreenLight)
    "PODA" -> EstiloCuidado(Icons.Filled.ContentCut, GreenDark, GreenLight)
    "ABONO" -> EstiloCuidado(Icons.Filled.Eco, GreenDark, GreenLight)
    else -> EstiloCuidado(Icons.Filled.Info, TextSecondary, BackgroundLight)
}

// ---------------------------------------------------------------------------
// Pantalla
// ---------------------------------------------------------------------------

@Composable
fun PlantDetailScreen(
    plantId: String?,
    onBack: () -> Unit = {},
    onDigitalize: () -> Unit = {},
    onSave: () -> Unit = {}
) {
    // Ultimo escaneo real si existe; si no, datos simulados (Fase 2 sin backend).
    val resultado = remember(plantId) { ScanResultStore.ultimo ?: MockPlantData.scanResult }
    PlantDetailContent(
        resultado = resultado,
        onBack = onBack,
        onDigitalize = {
            // Simulado: la planta escaneada pasa a ser la mascota del jardin.
            MockGardenData.digitalizar(resultado)
            onDigitalize()
        },
        onSave = onSave
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailContent(
    resultado: ScanResultDto,
    onBack: () -> Unit,
    onDigitalize: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fallida = resultado.identificacionFallida()

    // Esta pantalla es clara: iconos oscuros en la barra de estado (se restauran al salir).
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val ventana = (view.context as? Activity)?.window
            val control = ventana?.let { WindowCompat.getInsetsController(it, view) }
            val anterior = control?.isAppearanceLightStatusBars
            control?.isAppearanceLightStatusBars = true
            onDispose { anterior?.let { control.isAppearanceLightStatusBars = it } }
        }
    }
    val config = LocalConfiguration.current
    val apaisado = config.screenWidthDp > config.screenHeightDp

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Detalle de planta", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
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
        },
        bottomBar = {
            if (fallida) {
                BarraReintento(onBack)
            } else {
                BarraAcciones(
                    puedeGuardar = true,
                    onDigitalize = onDigitalize,
                    onSave = onSave
                )
            }
        }
    ) { padding ->
        if (apaisado) {
            // Landscape: foto/cabecera a la izquierda, informacion a la derecha.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    Cabecera(resultado, fallida)
                    Spacer(Modifier.height(16.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Informacion(resultado, fallida)
                    Spacer(Modifier.height(4.dp))
                }
            }
        } else {
            // Portrait: todo apilado.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Cabecera(resultado, fallida)
                Informacion(resultado, fallida)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Secciones
// ---------------------------------------------------------------------------

private fun etiquetaTipo(tipo: String?): String = when (tipo?.uppercase()) {
    "RIEGO" -> "Riego"
    "LUZ" -> "Luz solar"
    "TEMPERATURA" -> "Clima"
    "SUSTRATO" -> "Sustrato"
    "PODA" -> "Poda"
    "ABONO" -> "Abono"
    else -> "Cuidado"
}

/** Tarjeta superior: foto con la precision de la IA y, debajo, nombre y especie. */
@Composable
private fun Cabecera(resultado: ScanResultDto, fallida: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(GreenLight),
            contentAlignment = Alignment.Center
        ) {
            if (resultado.imagenUrl.isNullOrBlank()) {
                Icon(
                    Icons.Filled.Eco,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(64.dp)
                )
            } else {
                AsyncImage(
                    model = resultado.imagenUrl,
                    contentDescription = resultado.nombreComun,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (!fallida) {
                resultado.etiquetaPrecision()?.let { etiqueta ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(listOf(Color.Transparent, Color(0x99000000)))
                            )
                            .padding(12.dp)
                    ) {
                        EtiquetaPrecision(etiqueta)
                    }
                }
            }
        }

        if (!fallida) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = resultado.nombreComun.orEmpty(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                resultado.nombreCientifico?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        fontSize = 15.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (resultado.nuevoEnColeccion) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Nueva en tu colección",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GreenDark,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(GreenLight)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EtiquetaPrecision(texto: String) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(GreenPrimary)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(texto, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Informacion(resultado: ScanResultDto, fallida: Boolean) {
    if (fallida) {
        FalloIdentificacion()
        return
    }
    val tarjetas = resultado.tarjetasCuidado()
    if (tarjetas.isNotEmpty()) {
        ResumenRapido(tarjetas.take(3))
        Text("Guía de cuidados", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        ListaCuidados(tarjetas)
    }
    DiagnosticoBotanico(resultado.textoDiagnostico())
}

/** Vista de un vistazo: el `titulo` corto de los 3 cuidados principales. */
@Composable
private fun ResumenRapido(items: List<CuidadoSeccionDto>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { cuidado ->
            val estilo = estiloDe(cuidado.tipo)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(estilo.fondo)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(estilo.icono, contentDescription = null, tint = estilo.color, modifier = Modifier.size(22.dp))
                Text(etiquetaTipo(cuidado.tipo), fontSize = 11.sp, color = TextSecondary)
                Text(
                    text = cuidado.titulo.orEmpty().ifBlank { etiquetaTipo(cuidado.tipo) },
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Lista vertical de cuidados; cada fila se expande al tocarla para leer el detalle completo. */
@Composable
private fun ListaCuidados(items: List<CuidadoSeccionDto>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { ItemCuidado(it) }
    }
}

@Composable
private fun ItemCuidado(cuidado: CuidadoSeccionDto) {
    val estilo = estiloDe(cuidado.tipo)
    var expandido by remember(cuidado.id) { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(estilo.fondo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(estilo.icono, contentDescription = cuidado.tipo, tint = estilo.color, modifier = Modifier.size(22.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(etiquetaTipo(cuidado.tipo), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    cuidado.titulo?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 12.sp, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Icon(
                    if (expandido) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expandido) "Contraer" else "Expandir",
                    tint = TextSecondary
                )
            }
            Text(
                text = cuidado.contenido.orEmpty(),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSecondary,
                maxLines = if (expandido) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiagnosticoBotanico(texto: String?) {
    val hayProblemas = texto != null
    var expandido by remember(texto) { mutableStateOf(false) }
    val largo = (texto?.length ?: 0) > 160
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (hayProblemas) AmberLight else GreenLight)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (hayProblemas) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (hayProblemas) AmberAlert else GreenPrimary
                )
                Text("Diagnóstico Botánico IA", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
            Text(
                text = texto ?: "No se detectaron problemas.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextPrimary,
                maxLines = if (expandido || !largo) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )
            if (largo) {
                Text(
                    text = if (expandido) "Leer menos" else "Leer más…",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GreenDark,
                    modifier = Modifier.clickable { expandido = !expandido }
                )
            }
        }
    }
}

@Composable
private fun FalloIdentificacion() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RojoCalorLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = RojoCalor, modifier = Modifier.size(40.dp))
            Text("Fallo en la identificación", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
                "No pudimos reconocer la planta. Intenta de nuevo con una foto más clara y bien iluminada.",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

/** Con la identificacion fallida no hay nada que guardar ni digitalizar: solo reintentar. */
@Composable
private fun BarraReintento(onReintentar: () -> Unit) {
    Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onReintentar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Volver a escanear")
            }
        }
    }
}

@Composable
private fun BarraAcciones(puedeGuardar: Boolean, onDigitalize: () -> Unit, onSave: () -> Unit) {
    Surface(color = SurfaceWhite, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // Respeta barra de navegacion y recortes (notch) en los lados al rotar.
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDigitalize,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenDark)
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Digitalizar")
            }
            Button(
                onClick = onSave,
                enabled = puedeGuardar,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Guardar")
            }
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
        PlantDetailContent(MockPlantData.scanResult, {}, {}, {})
    }
}

@Preview(name = "Sin advertencias", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewSinAdvertencias() {
    MobilekotlinTheme(dynamicColor = false) {
        PlantDetailContent(
            MockPlantData.scanResult.copy(
                cuidados = MockPlantData.scanResult.cuidados.orEmpty().filter { it.tipo != "TOXICIDAD" }
            ),
            {}, {}, {}
        )
    }
}

@Preview(name = "Fallo", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewFallo() {
    MobilekotlinTheme(dynamicColor = false) {
        PlantDetailContent(MockPlantData.scanResultFallo, {}, {}, {})
    }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 851, heightDp = 393)
@Composable
private fun PreviewLandscape() {
    MobilekotlinTheme(dynamicColor = false) {
        PlantDetailContent(MockPlantData.scanResult, {}, {}, {})
    }
}
