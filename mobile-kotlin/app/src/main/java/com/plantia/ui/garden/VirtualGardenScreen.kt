package com.plantia.ui.garden

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.data.model.AccionCuidado
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.TiendaItem
import com.plantia.data.model.WeatherAlert
import com.plantia.ui.components.rememberCuentaRegresiva
import com.plantia.ui.garden.components.AnimatedStatBar
import com.plantia.ui.garden.components.CareActionButton
import com.plantia.ui.escena.Escena
import com.plantia.ui.escena.EscenaViewModel
import com.plantia.ui.garden.components.FondosPlantaSheet
import com.plantia.ui.garden.components.PlantFrame
import com.plantia.ui.garden.viewmodel.GardenUiState
import com.plantia.ui.garden.viewmodel.GardenViewModel
import com.plantia.ui.home.viewmodel.INTERVALO_SONDEO_MS
import com.plantia.ui.theme.RojoClaro
import com.plantia.ui.theme.RojoTexto
import com.plantia.ui.preview.DatosEjemplo
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
import com.plantia.util.formatoCuentaRegresiva
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// El backend no manda animo ni consejos: se derivan de los porcentajes.
internal fun estadoAnimo(planta: PlantSummary): String = when {
    planta.muerta || planta.enRecuperacion -> "Triste"
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VirtualGardenScreen(
    onBack: () -> Unit = {},
    onScan: () -> Unit = {},
    onTienda: () -> Unit = {},
    viewModel: GardenViewModel = viewModel(factory = GardenViewModel.Factory),
    escenaViewModel: EscenaViewModel = viewModel(factory = EscenaViewModel.Factory)
) {
    IconosOscurosEnBarraDeEstado()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Si el evento golpeo la planta, al entrar se muestra una sola vez como arraso con ella
    var arrasando by remember { mutableStateOf(false) }
    val golpe = (viewModel.uiState as? GardenUiState.Success)?.planta?.golpeTipo
    LaunchedEffect(golpe) {
        if (golpe != null) {
            arrasando = true
            delay(3_500)
            arrasando = false
            viewModel.golpeVisto()
        }
    }

    // Un evento que lanza el admin aparece solo (y el boton de prevenir con el): se consulta mientras el jardin esta a la vista
    val ciclo = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(ciclo) {
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.actualizarEvento()
                delay(INTERVALO_SONDEO_MS)
            }
        }
    }

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
            val temas = viewModel.fondos
            val plantaActual = (viewModel.uiState as? GardenUiState.Success)?.planta
            if (temas != null && plantaActual != null) {
                FondosPlantaSheet(
                    nombrePlanta = plantaActual.name,
                    temas = temas,
                    cargando = viewModel.cargandoFondos,
                    onElegir = { tema ->
                        viewModel.elegirFondo(
                            tema,
                            alTerminar = { mensaje ->
                                scope.launch {
                                    snackbar.currentSnackbarData?.dismiss()
                                    snackbar.showSnackbar(mensaje)
                                }
                            },
                            alCambiar = escenaViewModel::recargar
                        )
                    },
                    onTienda = {
                        viewModel.cerrarFondos()
                        onTienda()
                    },
                    onCerrar = viewModel::cerrarFondos
                )
            }
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
                    escena = escenaViewModel.escena,
                    planta = estado.planta,
                    abono = viewModel.abono,
                    evento = if (viewModel.puedePrevenir || estado.planta.refugiada) viewModel.evento else null,
                    previniendo = viewModel.previniendo,
                    efectoProteccion = viewModel.proteccionesNuevas,
                    arrasando = arrasando,
                    reviviendo = viewModel.reviviendo,
                    onRevivir = {
                        viewModel.revivir { mensaje ->
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(mensaje)
                            }
                        }
                    },
                    onPrevenir = {
                        viewModel.prevenir { mensaje ->
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(mensaje)
                            }
                        }
                    },
                    onTienda = onTienda,
                    onFondos = {
                        viewModel.abrirFondos { mensaje ->
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(mensaje)
                            }
                        }
                    },
                    onAbono = {
                        viewModel.usarAbono { mensaje ->
                            scope.launch {
                                snackbar.currentSnackbarData?.dismiss()
                                snackbar.showSnackbar(mensaje)
                            }
                        }
                    },
                    onCuidar = { accion ->
                        viewModel.cuidar(accion) { mensaje ->
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
internal fun JardinContenido(
    planta: PlantSummary,
    escena: Escena? = null,
    abono: TiendaItem? = null,
    /** Evento activo (o null): con el aparece el boton "Prevenir evento" junto a los cuidados. */
    evento: WeatherAlert? = null,
    previniendo: Boolean = false,
    onPrevenir: () -> Unit = {},
    /** La planta murio: se ofrece revivirla a cambio de hojas. */
    reviviendo: Boolean = false,
    onRevivir: () -> Unit = {},
    /** Efectos: escudo al prevenir el evento y golpe del evento sobre la planta (ver PlantFrame). */
    efectoProteccion: Int = 0,
    arrasando: Boolean = false,
    onAbono: () -> Unit = {},
    onTienda: () -> Unit = {},
    onFondos: (() -> Unit)? = null,
    onCuidar: (AccionCuidado) -> Unit
) {
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
                    animo = estadoAnimo(planta),
                    imageUrl = planta.imageUrl,
                    escena = escena,
                    apariencia = planta.apariencia,
                    diasCuidados = planta.diasCuidados,
                    diasTotales = planta.diasTotales,
                    marchita = planta.healthPercent < 30 || planta.muerta,
                    onFondos = onFondos,
                    desastreActivo = !planta.refugiada,
                    efectoProteccion = efectoProteccion,
                    arrasando = arrasando,
                    tipoGolpe = planta.golpeTipo,
                macetaCodigo = planta.macetaCodigo,
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
                EncabezadoPlanta(planta)
                CuerpoJardin(planta, evento, previniendo, onPrevenir, abono, onAbono, onTienda, reviviendo, onRevivir, onCuidar)
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
            EncabezadoPlanta(planta)
            PlantFrame(
                nombre = planta.name,
                animo = estadoAnimo(planta),
                imageUrl = planta.imageUrl,
                escena = escena,
                apariencia = planta.apariencia,
                diasCuidados = planta.diasCuidados,
                diasTotales = planta.diasTotales,
                marchita = planta.healthPercent < 30 || planta.muerta,
                onFondos = onFondos,
                    desastreActivo = !planta.refugiada,
                    efectoProteccion = efectoProteccion,
                    arrasando = arrasando,
                    tipoGolpe = planta.golpeTipo,
                macetaCodigo = planta.macetaCodigo,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            )
            CuerpoJardin(planta, evento, previniendo, onPrevenir, abono, onAbono, onTienda, reviviendo, onRevivir, onCuidar)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Lo que va debajo del marco segun el estado de la planta: muerta (solo se ofrece revivirla), golpeada por
 * un evento (aviso con la cuenta regresiva y los cuidados) o normal.
 */
@Composable
private fun ColumnScope.CuerpoJardin(
    planta: PlantSummary,
    evento: WeatherAlert?,
    previniendo: Boolean,
    onPrevenir: () -> Unit,
    abono: TiendaItem?,
    onAbono: () -> Unit,
    onTienda: () -> Unit,
    reviviendo: Boolean,
    onRevivir: () -> Unit,
    onCuidar: (AccionCuidado) -> Unit
) {
    if (planta.muerta) {
        TarjetaMuerta(planta, reviviendo, onRevivir)
        TextButton(onClick = onTienda, modifier = Modifier.fillMaxWidth()) { Text("🛒 Ir a la tienda", color = GreenDark, fontWeight = FontWeight.Bold) }
        return
    }
    if (planta.enRecuperacion) TarjetaRecuperacion(planta)
    TarjetaVital(planta)
    TarjetaAcciones(planta, evento, previniendo, onPrevenir, onCuidar)
    TarjetaAbono(abono, onAbono, onTienda)
    TarjetaConsejo(consejo(planta))
}

/** La planta fue golpeada por un evento: solo el abono la salva y el tiempo corre. */
@Composable
private fun TarjetaRecuperacion(planta: PlantSummary) {
    val restante = rememberCuentaRegresiva(planta.recuperacionSegundos)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RojoClaro)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${planta.name} esta muy débil", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = RojoTexto)
            Text(
                "Un evento la golpeó. Solo el abono la recupera: abónala antes de que se acabe el tiempo o morirá.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextPrimary
            )
            if (restante != null) {
                Text(
                    "Tiempo restante  ${formatoCuentaRegresiva(restante)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = RojoTexto
                )
            }
        }
    }
}

/** La planta murio por no abonarla a tiempo: se puede revivir a cambio de hojas. */
@Composable
private fun TarjetaMuerta(planta: PlantSummary, reviviendo: Boolean, onRevivir: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RojoClaro)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🥀 ${planta.name} murió", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = RojoTexto)
            Text(
                "No se abonó a tiempo después del evento. Puedes revivirla y sigue creciendo desde donde iba.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = TextPrimary
            )
            Button(
                onClick = onRevivir,
                enabled = !reviviendo,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.White)
            ) {
                Text(
                    text = when {
                        reviviendo -> "Reviviendo..."
                        planta.costoRevivir != null -> "Revivir · ${planta.costoRevivir} hojas"
                        else -> "Revivir"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Nombre y especie fuera del marco, para que nada tape la escena. */
@Composable
private fun EncabezadoPlanta(planta: PlantSummary) {
    Column(Modifier.fillMaxWidth()) {
        Text(planta.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        if (planta.species.isNotBlank()) {
            Text(planta.species, fontSize = 13.sp, fontStyle = FontStyle.Italic, color = TextSecondary)
        }
    }
}

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
private fun TarjetaAcciones(
    planta: PlantSummary,
    evento: WeatherAlert?,
    previniendo: Boolean,
    onPrevenir: () -> Unit,
    onCuidar: (AccionCuidado) -> Unit
) {
    TarjetaBlanca {
        Text("Cuidados", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AccionCuidado.entries.forEach { accion ->
                val (icono, color) = when (accion) {
                    AccionCuidado.REGAR -> Icons.Filled.WaterDrop to BlueInfo
                    AccionCuidado.SOL -> Icons.Filled.WbSunny to AmberAlert
                    AccionCuidado.ABONAR -> Icons.Filled.Science to GreenPrimary
                }
                CareActionButton(
                    etiqueta = accion.etiqueta,
                    pista = if (accion == AccionCuidado.ABONAR && planta.enRecuperacion) "Recupera" else accion.pista,
                    icono = icono,
                    colorIcono = Color.White,
                    colorFondo = color,
                    onClick = { onCuidar(accion) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        // Solo con un evento activo (o ya prevenido) y una planta viva
        if (evento != null && !planta.muerta && !planta.enRecuperacion) {
            Spacer(Modifier.height(10.dp))
            BotonPrevenir(protegida = planta.refugiada, previniendo = previniendo, onPrevenir = onPrevenir)
        }
    }
}

/** "Prevenir evento": en rojo mientras falta; cuando la planta ya esta protegida pasa a ser una confirmacion en verde. */
@Composable
private fun BotonPrevenir(protegida: Boolean, previniendo: Boolean, onPrevenir: () -> Unit) {
    Button(
        onClick = onPrevenir,
        enabled = !protegida && !previniendo,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RojoTexto,
            contentColor = Color.White,
            disabledContainerColor = if (protegida) GreenLight else RojoClaro,
            disabledContentColor = if (protegida) GreenDark else RojoTexto
        )
    ) {
        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = when {
                protegida -> "Protegida del evento"
                previniendo -> "Previniendo..."
                else -> "Prevenir evento"
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Abono de la tienda: +1 dia de crecimiento a cambio de monedas (maximo uno al dia por planta). */
@Composable
private fun TarjetaAbono(abono: TiendaItem?, onAbono: () -> Unit, onTienda: () -> Unit) {
    if (abono != null) {
        Card(
            onClick = onAbono,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = GreenLight)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🌿 ${abono.nombre}: +1 día de crecimiento", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${abono.precio} 🍃 · máximo 1 al día", fontSize = 12.sp, color = GreenDark, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    TextButton(onClick = onTienda, modifier = Modifier.fillMaxWidth()) { Text("🛒 Ir a la tienda", color = GreenDark, fontWeight = FontWeight.Bold) }
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

@Preview(name = "Portrait", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewPortrait() {
    MobilekotlinTheme(dynamicColor = false) {
        Box(Modifier.fillMaxSize()) { JardinContenido(DatosEjemplo.plantaSimulada) {} }
    }
}

@Preview(name = "Planta sedienta", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewSedienta() {
    MobilekotlinTheme(dynamicColor = false) {
        Box(Modifier.fillMaxSize()) { JardinContenido(DatosEjemplo.plantaSedienta) {} }
    }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 851, heightDp = 393)
@Composable
private fun PreviewLandscape() {
    MobilekotlinTheme(dynamicColor = false) {
        Box(Modifier.fillMaxSize()) { JardinContenido(DatosEjemplo.plantaSimulada) {} }
    }
}
