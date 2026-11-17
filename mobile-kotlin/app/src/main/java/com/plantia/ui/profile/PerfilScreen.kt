package com.plantia.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import com.plantia.data.local.Recordatorios
import com.plantia.ui.theme.BlueInfo
import androidx.compose.foundation.BorderStroke
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
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
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
    onVerTerminos: () -> Unit,
    onEditarDatos: () -> Unit,
    onVerEnciclopedia: () -> Unit,
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

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when {
                cargando -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
                usuario == null -> AvisoError(error ?: "No se pudo cargar el perfil", onReintentar)
                else -> {
                    TarjetaUsuario(usuario, subiendoFoto, errorFoto, onCambiarFoto)
                    FilaEstadisticas(digitales, usuario.streakDays, usuario.leaves)
                }
            }

            SeccionTema(modoTema, onCambiarTema)

            if (usuario != null && regiones.isNotEmpty()) {
                SeccionRegion(regiones, usuario.regionCode, cambiandoRegion, errorRegion, onCambiarRegion)
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                FilaAcceso(Icons.Filled.Edit, "Editar datos", onEditarDatos)
                FilaRecordatorios(recordar, alCambiarRecordatorio)
                FilaAcceso(Icons.Filled.History, "Historial de escaneos", onVerHistorial)
                FilaAcceso(Icons.Filled.MenuBook, "Enciclopedia de plantas", onVerEnciclopedia)
                FilaAcceso(Icons.Filled.Description, "Términos y privacidad", onVerTerminos)
                FilaAcceso(Icons.AutoMirrored.Filled.ExitToApp, "Cerrar sesión", { confirmarSalida = true }, peligro = true)
            }

            // Solo en versiones de depuracion: el equipo cambia a mano el servidor al que se conecta la app.
            if (BuildConfig.DEBUG) ConexionAvanzada(baseUrlActual, hostManualActual, onGuardarHost)
        }
    }
}

/** Fila con interruptor: recibir un recordatorio diario para cuidar las plantas. */
@Composable
private fun FilaRecordatorios(activo: Boolean, onCambiar: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onCambiar(!activo) }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(GreenLight), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Notifications, contentDescription = null, tint = GreenDark, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Recordatorios", fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("Un aviso diario a las ${Recordatorios.HORA}:00 para cuidar tus plantas", fontSize = 12.sp, color = TextSecondary)
        }
        androidx.compose.material3.Switch(
            checked = activo,
            onCheckedChange = onCambiar,
            colors = androidx.compose.material3.SwitchDefaults.colors(checkedTrackColor = GreenPrimary)
        )
    }
}

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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.clickable(onClickLabel = "Cambiar foto de perfil") {
                    if (!subiendoFoto) {
                        selector.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                }
            ) {
                Box(
                    modifier = Modifier.size(92.dp).clip(CircleShape).background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    val inicial: @Composable () -> Unit = {
                        Text(
                            text = usuario.name.trim().split(" ").filter { it.isNotBlank() }.take(2)
                                .joinToString("") { it.first().uppercase() }.ifEmpty { "?" },
                            fontSize = 40.sp,
                            fontFamily = com.plantia.ui.theme.FuentePixel,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.surface
                        )
                    }
                    if (usuario.profileImageUrl.isNullOrBlank()) inicial()
                    else com.plantia.ui.components.FotoPerfil(usuario.profileImageUrl, "Foto de ${usuario.name}", respaldo = inicial)
                    if (subiendoFoto) androidx.compose.material3.CircularProgressIndicator(color = MaterialTheme.colorScheme.surface, modifier = Modifier.size(36.dp))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(AmberAlert),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(16.dp))
                }
            }
            if (errorFoto != null) Text(errorFoto, fontSize = 12.sp, color = RojoTexto)

            Text(usuario.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (usuario.email.isNotBlank()) Text(usuario.email, fontSize = 13.sp, color = TextSecondary)

            Etiqueta("${usuario.badge} · Nivel ${usuario.level}", GreenLight, GreenDark)
        }
    }
}

@Composable
private fun Etiqueta(texto: String, fondo: androidx.compose.ui.graphics.Color, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(50), color = fondo) {
        Text(texto, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun FilaEstadisticas(plantas: Int?, dias: Int, hojas: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Estadistica(plantas?.toString() ?: "–", "Plantas", GreenPrimary, Modifier.weight(1f))
        Estadistica(dias.toString(), "Días de racha", BlueInfo, Modifier.weight(1f))
        Estadistica(hojas.toString(), "Hojas", AmberAlert, Modifier.weight(1f))
    }
}

@Composable
private fun Estadistica(valor: String, etiqueta: String, borde: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, borde.copy(alpha = 0.55f))
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(valor, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(etiqueta, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

/** Region del usuario: decide que desastres le llegan. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionRegion(
    regiones: List<Region>,
    actual: String,
    cambiando: Boolean,
    error: String?,
    onCambiar: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Mi región", fontWeight = FontWeight.Bold, color = TextPrimary)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                regiones.forEach { region ->
                    val seleccionada = region.code == actual.ifBlank { "GLOBAL" }
                    FilterChip(
                        selected = seleccionada,
                        onClick = { onCambiar(region.code) },
                        enabled = !cambiando,
                        label = { Text(region.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = !cambiando,
                            selected = seleccionada,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = GreenPrimary
                        )
                    )
                }
            }
            Text(
                error ?: "Recibes las alertas de desastre de tu región.",
                fontSize = 12.sp,
                color = if (error != null) RojoTexto else TextSecondary
            )
        }
    }
}

@Composable
private fun SeccionTema(actual: ModoTema, onCambiar: (ModoTema) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Apariencia", fontSize = 12.sp, color = TextSecondary, modifier = Modifier.padding(start = 4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ModoTema.entries.forEach { modo ->
                val elegido = modo == actual
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(11.dp))
                        .background(if (elegido) GreenPrimary else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onCambiar(modo) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        modo.etiqueta,
                        fontSize = 13.sp,
                        fontWeight = if (elegido) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (elegido) MaterialTheme.colorScheme.surface else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaAcceso(icono: ImageVector, texto: String, onClick: () -> Unit, peligro: Boolean = false) {
    val color = if (peligro) RojoTexto else TextPrimary
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(if (peligro) RojoClaro else GreenLight), contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = null, tint = if (peligro) RojoTexto else GreenDark, modifier = Modifier.size(20.dp))
        }
        Text(texto, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = color)
        if (!peligro) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = TextSecondary)
    }
}

@Composable
private fun AvisoError(mensaje: String, onReintentar: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = RojoClaro) {
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
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth().clickable { abierto = !abierto }, verticalAlignment = Alignment.CenterVertically) {
                Text("Desarrollo: conexión al servidor", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(if (abierto) "Ocultar" else "Mostrar", color = GreenDark, fontSize = 13.sp)
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.weight(1f)
                    ) { Text("Guardar") }
                    OutlinedButton(onClick = { onGuardarHost(null); hostManual = "" }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
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
