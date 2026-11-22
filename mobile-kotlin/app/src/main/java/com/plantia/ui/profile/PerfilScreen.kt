package com.plantia.ui.profile

import androidx.compose.ui.graphics.Color
import com.plantia.ui.theme.FondoDegradado

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import com.plantia.data.local.Recordatorios
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.BuildConfig
import com.plantia.data.local.ModoTema
import com.plantia.data.model.Region
import com.plantia.data.model.UserSummary
import com.plantia.ui.components.BarraProgreso
import com.plantia.ui.components.tarjeta
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.Peligro
import com.plantia.ui.theme.Primario
import com.plantia.ui.theme.SobrePrimario
import com.plantia.ui.theme.SuperficieElevada
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.Tinte
import com.plantia.ui.theme.RojoClaro
import com.plantia.ui.theme.RojoTexto
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.util.IconosOscurosEnBarraDeEstado

/**
 * Perfil del usuario (propuesta E): foto con boton de camara, nombre y rango, tres estadisticas (plantas, dias y
 * hojas), apariencia y una lista con editar datos, recordatorios, historial, enciclopedia, terminos y cierre de sesion.
 * Usa solo colores del tema, asi que se ve bien en claro y en oscuro.
 */
@Composable
fun PerfilScreen(
    usuario: UserSummary?,
    cargando: Boolean,
    error: String?,
    digitales: Int?,
    modoTema: ModoTema,
    regiones: List<Region>,
    cambiandoRegion: Boolean,
    errorRegion: String?,
    baseUrlActual: String?,
    hostManualActual: String,
    onCambiarRegion: (String) -> Unit,
    onCambiarTema: (ModoTema) -> Unit,
    onGuardarHost: (String?) -> Unit,
    onReintentar: () -> Unit,
    onVerHistorial: () -> Unit,
    onVerLogros: () -> Unit = {},
    onVerTerminos: () -> Unit,
    onEditarDatos: () -> Unit,
    onCerrarSesion: () -> Unit,
    modifier: Modifier = Modifier,
    subiendoFoto: Boolean = false,
    errorFoto: String? = null,
    onCambiarFoto: (java.io.File) -> Unit = {}
) {
    IconosOscurosEnBarraDeEstado()
    var confirmarSalida by remember { mutableStateOf(false) }

    // Recordatorio diario: pide el permiso de notificaciones (Android 13+) antes de activarlo
    val contexto = androidx.compose.ui.platform.LocalContext.current
    var recordar by remember { mutableStateOf(Recordatorios.activos(contexto)) }
    val pedirPermiso = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            Recordatorios.activar(contexto)
            recordar = true
        }
    }
    val alCambiarRecordatorio: (Boolean) -> Unit = { quiere ->
        if (!quiere) {
            Recordatorios.desactivar(contexto)
            recordar = false
        } else if (android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                contexto, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            pedirPermiso.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            Recordatorios.activar(contexto)
            recordar = true
        }
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Volverás a la pantalla de bienvenida.") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; onCerrarSesion() }) {
                    Text("Cerrar sesión", color = RojoTexto, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(modifier = modifier.background(FondoDegradado), containerColor = Color.Transparent) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                cargando -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primario)
                }
                usuario == null -> AvisoError(error ?: "No se pudo cargar el perfil", onReintentar)
                else -> TarjetaUsuario(usuario, subiendoFoto, errorFoto, onCambiarFoto)
            }

            SeccionTema(modoTema, onCambiarTema)

            if (usuario != null && regiones.isNotEmpty()) {
                SeccionRegion(regiones, usuario.regionCode, cambiandoRegion, errorRegion, onCambiarRegion)
            }

            Column(Modifier.fillMaxWidth().tarjeta().padding(horizontal = 14.dp, vertical = 4.dp)) {
                FilaRecordatorios(recordar, alCambiarRecordatorio)
                FilaAcceso(Icons.Filled.Edit, "Editar datos", onEditarDatos)
                FilaAcceso(Icons.Filled.EmojiEvents, "Logros", onVerLogros)
                FilaAcceso(Icons.Filled.History, "Historial de escaneos", onVerHistorial)
                FilaAcceso(Icons.Filled.Description, "Términos y privacidad", onVerTerminos)
            }

            BotonCerrarSesion { confirmarSalida = true }

            // Solo en versiones de depuracion: el equipo cambia a mano el servidor al que se conecta la app.
            if (BuildConfig.DEBUG) ConexionAvanzada(baseUrlActual, hostManualActual, onGuardarHost)
        }
    }
}

/** Fila con interruptor: recibir un recordatorio diario para cuidar las plantas. */
@Composable
private fun FilaRecordatorios(activo: Boolean, onCambiar: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCambiar(!activo) }.padding(horizontal = 2.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        IconoDeFila(Icons.Filled.Notifications)
        Column(Modifier.weight(1f)) {
            Text("Recordatorios", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Text("Un aviso diario a las ${Recordatorios.HORA}:00 para cuidar tus plantas", fontSize = 11.sp, color = TextSecondary)
        }
        androidx.compose.material3.Switch(
            checked = activo,
            onCheckedChange = onCambiar,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedTrackColor = Primario,
                checkedThumbColor = SobrePrimario,
                checkedBorderColor = Primario,
                uncheckedTrackColor = SuperficieElevada,
                uncheckedThumbColor = TextSecondary,
                uncheckedBorderColor = DividerColor
            )
        )
    }
}

@Composable
private fun IconoDeFila(icono: ImageVector, peligro: Boolean = false) {
    Box(Modifier.size(32.dp).clip(CircleShape).background(SuperficieElevada), contentAlignment = Alignment.Center) {
        Icon(icono, contentDescription = null, tint = if (peligro) Peligro else Primario, modifier = Modifier.size(17.dp))
    }
}

/** Tarjeta de arriba: foto con anillo de XP y boton de camara, nombre, insignia y nivel, barra de XP y hojas. */
@Composable
private fun TarjetaUsuario(
    usuario: UserSummary,
    subiendoFoto: Boolean,
    errorFoto: String?,
    onCambiarFoto: (java.io.File) -> Unit
) {
    val contexto = androidx.compose.ui.platform.LocalContext.current
    val alcance = androidx.compose.runtime.rememberCoroutineScope()
    val selector = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) alcance.launch {
            // Copiar la foto elegida a la cache es lectura de disco: fuera del hilo principal
            val imagen = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.plantia.util.ImagenLocal.desdeUri(contexto, uri)
            }
            if (imagen != null) onCambiarFoto(imagen.archivo)
        }
    }
    val progreso by animateFloatAsState(usuario.progresoNivel, tween(600), label = "xp-perfil")
    val colorAnillo = Primario
    val colorPista = DividerColor
    Column(
        modifier = Modifier.fillMaxWidth().tarjeta().padding(start = 14.dp, end = 14.dp, top = 18.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier.size(84.dp).clickable(onClickLabel = "Cambiar foto de perfil") {
                if (!subiendoFoto) {
                    selector.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                }
            },
            contentAlignment = Alignment.Center
        ) {
            // Anillo de XP alrededor de la foto
            Canvas(Modifier.fillMaxSize()) {
                val grosor = 5.dp.toPx()
                val tope = androidx.compose.ui.geometry.Size(size.width - grosor, size.height - grosor)
                val origen = androidx.compose.ui.geometry.Offset(grosor / 2, grosor / 2)
                drawArc(colorPista, 0f, 360f, false, origen, tope, style = Stroke(grosor))
                drawArc(colorAnillo, -90f, 360f * progreso, false, origen, tope, style = Stroke(grosor, cap = StrokeCap.Round))
            }
            Box(
                modifier = Modifier.size(70.dp).clip(CircleShape).background(SuperficieElevada),
                contentAlignment = Alignment.Center
            ) {
                val inicial: @Composable () -> Unit = {
                    Text(
                        text = usuario.name.trim().split(" ").filter { it.isNotBlank() }.take(2)
                            .joinToString("") { it.first().uppercase() }.ifEmpty { "?" },
                        fontSize = 26.sp,
                        fontFamily = com.plantia.ui.theme.FuentePixel,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
                if (usuario.profileImageUrl.isNullOrBlank()) inicial()
                else com.plantia.ui.components.FotoPerfil(usuario.profileImageUrl, "Foto de ${usuario.name}", respaldo = inicial)
                if (subiendoFoto) CircularProgressIndicator(color = Primario, modifier = Modifier.size(32.dp))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(SurfaceWhite)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Primario),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = SobrePrimario, modifier = Modifier.size(14.dp))
            }
        }
        if (errorFoto != null) Text(errorFoto, fontSize = 12.sp, color = Peligro)

        Text(usuario.name, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
        if (usuario.email.isNotBlank()) Text(usuario.email, fontSize = 12.sp, color = TextSecondary)
        Text(
            "${usuario.badge} · Nivel ${usuario.level}",
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Primario,
            modifier = Modifier.clip(RoundedCornerShape(50)).background(Tinte).padding(horizontal = 10.dp, vertical = 3.dp)
        )
        Spacer(Modifier.height(2.dp))
        BarraProgreso(progreso, Primario, pista = SuperficieElevada)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (usuario.esNivelMaximo) "Nivel máximo · ${usuario.experience} XP" else "${usuario.experience} / ${usuario.experienceToNextLevel} XP",
                fontSize = 10.5.sp, color = TextSecondary
            )
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(SuperficieElevada).border(1.dp, DividerColor, RoundedCornerShape(50))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Filled.Eco, contentDescription = "Hojas", tint = Primario, modifier = Modifier.size(14.dp))
                Text("${usuario.leaves}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            }
        }
    }
}

/** Region del usuario: decide que desastres le llegan. Se elige en una lista desplegable. */
@Composable
private fun SeccionRegion(
    regiones: List<Region>,
    actual: String,
    cambiando: Boolean,
    error: String?,
    onCambiar: (String) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    val codigo = actual.ifBlank { "GLOBAL" }
    val nombre = regiones.firstOrNull { it.code == codigo }?.name ?: codigo
    Column(Modifier.fillMaxWidth().tarjeta().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TituloGrupo("Mi región")
        Box {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SuperficieElevada)
                    .clickable(enabled = !cambiando) { abierto = true }
                    .padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(nombre, modifier = Modifier.weight(1f), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = TextPrimary)
                if (cambiando) CircularProgressIndicator(color = Primario, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                else Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                regiones.forEach { region ->
                    DropdownMenuItem(
                        text = { Text(if (region.code == codigo) "✓ ${region.name}" else region.name) },
                        onClick = { abierto = false; if (region.code != codigo) onCambiar(region.code) }
                    )
                }
            }
        }
        Text(
            error ?: "Recibes las alertas de desastre de tu región.",
            fontSize = 11.sp,
            color = if (error != null) Peligro else TextSecondary
        )
    }
}

@Composable
private fun TituloGrupo(texto: String) {
    Text(texto.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = TextSecondary, letterSpacing = 0.5.sp)
}

@Composable
private fun SeccionTema(actual: ModoTema, onCambiar: (ModoTema) -> Unit) {
    Column(Modifier.fillMaxWidth().tarjeta().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TituloGrupo("Apariencia")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SuperficieElevada)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ModoTema.entries.forEach { modo ->
                val elegido = modo == actual
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (elegido) Primario else Color.Transparent)
                        .clickable { onCambiar(modo) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modo.etiqueta,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (elegido) SobrePrimario else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaAcceso(icono: ImageVector, texto: String, onClick: () -> Unit) {
    Column {
        HorizontalDivider(color = DividerColor)
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 2.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            IconoDeFila(icono)
            Text(texto, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextPrimary)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

/** Cerrar sesion: boton de contorno en color de peligro. */
@Composable
private fun BotonCerrarSesion(onClick: () -> Unit) {
    val forma = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(forma)
            .border(1.5.dp, Peligro.copy(alpha = 0.55f), forma)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, tint = Peligro, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Cerrar sesión", fontWeight = FontWeight.ExtraBold, color = Peligro)
    }
}

@Composable
private fun AvisoError(mensaje: String, onReintentar: () -> Unit) {
    Surface(shape = RoundedCornerShape(18.dp), color = RojoClaro) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(mensaje, modifier = Modifier.weight(1f), color = RojoTexto, fontSize = 14.sp)
            TextButton(onClick = onReintentar) { Text("Reintentar", color = RojoTexto, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Conexion al servidor: solo se ve en versiones de depuracion. */
@Composable
private fun ConexionAvanzada(baseUrlActual: String?, hostManualActual: String, onGuardarHost: (String?) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    var hostManual by remember(hostManualActual) { mutableStateOf(hostManualActual) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().clickable { abierto = !abierto }, verticalAlignment = Alignment.CenterVertically) {
                Text("Desarrollo: conexión al servidor", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(if (abierto) "Ocultar" else "Mostrar", color = Primario, fontSize = 13.sp)
            }
            if (abierto) {
                Text("En uso: ${baseUrlActual ?: "sin conexión"}", fontSize = 13.sp, color = TextSecondary)
                OutlinedTextField(
                    value = hostManual,
                    onValueChange = { hostManual = it },
                    label = { Text("Dirección del servidor") },
                    placeholder = { Text("http://192.168.1.70:8080/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onGuardarHost(hostManual) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primario, contentColor = SobrePrimario),
                        modifier = Modifier.weight(1f)
                    ) { Text("Guardar") }
                    OutlinedButton(onClick = { onGuardarHost(null); hostManual = "" }, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                        Text("Automático")
                    }
                }
                Text(
                    "Automático usa el cable USB y el descubrimiento en la red local; escribe la dirección solo si hace falta.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
