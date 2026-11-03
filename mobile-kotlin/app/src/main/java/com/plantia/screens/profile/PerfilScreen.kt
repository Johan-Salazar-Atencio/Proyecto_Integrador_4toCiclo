package com.plantia.screens.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.plantia.data.ModoTema
import com.plantia.model.UserSummary
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
 * Perfil del usuario: nivel y experiencia, resumen de plantas, tema de la app, accesos y cierre de sesion.
 * Usa solo colores del tema, asi que se ve bien en claro y en oscuro.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(
    usuario: UserSummary?,
    cargando: Boolean,
    error: String?,
    escaneadas: Int?,
    digitales: Int,
    modoTema: ModoTema,
    baseUrlActual: String?,
    hostManualActual: String,
    onCambiarTema: (ModoTema) -> Unit,
    onGuardarHost: (String?) -> Unit,
    onReintentar: () -> Unit,
    onVerHistorial: () -> Unit,
    onVerTerminos: () -> Unit,
    onCerrarSesion: () -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconosOscurosEnBarraDeEstado()
    var confirmarSalida by remember { mutableStateOf(false) }

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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when {
                cargando -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
                usuario == null -> AvisoError(error ?: "No se pudo cargar el perfil", onReintentar)
                else -> {
                    TarjetaUsuario(usuario)
                    FilaEstadisticas(escaneadas, digitales)
                }
            }

            SeccionTema(modoTema, onCambiarTema)

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                FilaAcceso(Icons.Filled.History, "Historial de escaneos", onVerHistorial)
                FilaAcceso(Icons.Filled.Description, "Términos y privacidad", onVerTerminos)
                FilaAcceso(Icons.AutoMirrored.Filled.ExitToApp, "Cerrar sesión", { confirmarSalida = true }, peligro = true)
            }

            // Solo en versiones de depuracion: el equipo cambia a mano el servidor al que se conecta la app.
            if (BuildConfig.DEBUG) ConexionAvanzada(baseUrlActual, hostManualActual, onGuardarHost)
        }
    }
}

@Composable
private fun TarjetaUsuario(usuario: UserSummary) {
    val falta = usuario.experienceToNextLevel.coerceAtLeast(1)
    val objetivo = (usuario.experience.toFloat() / falta).coerceIn(0f, 1f)
    var arranque by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { arranque = true }
    val progreso by animateFloatAsState(if (arranque) objetivo else 0f, tween(900), label = "xp")

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
            Box {
                Box(
                    modifier = Modifier.size(92.dp).clip(CircleShape).background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = usuario.name.trim().firstOrNull()?.uppercase() ?: "?",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(GreenDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.surface, modifier = Modifier.size(16.dp))
                }
            }

            Text(usuario.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Etiqueta("Nivel ${usuario.level} · ${usuario.badge}", GreenLight, GreenDark)
                Etiqueta("🍃 ${usuario.leaves} hojas", AmberLight, AmberAlert)
            }

            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Progreso de nivel", fontSize = 12.sp, color = TextSecondary)
                    Text("${usuario.experience} / ${usuario.experienceToNextLevel} XP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                }
                Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))) {
                    Box(Modifier.fillMaxWidth(progreso).fillMaxSize().clip(RoundedCornerShape(50)).background(GreenPrimary))
                }
            }
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
private fun FilaEstadisticas(escaneadas: Int?, digitales: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Estadistica(Icons.Filled.Collections, escaneadas?.toString() ?: "–", "Escaneadas", Modifier.weight(1f))
        Estadistica(Icons.Filled.Yard, digitales.toString(), "Digitales", Modifier.weight(1f))
    }
}

@Composable
private fun Estadistica(icono: ImageVector, valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(GreenLight), contentAlignment = Alignment.Center) {
                Icon(icono, contentDescription = null, tint = GreenDark, modifier = Modifier.size(22.dp))
            }
            Text(valor, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(etiqueta, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
private fun SeccionTema(actual: ModoTema, onCambiar: (ModoTema) -> Unit) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tema de la app", fontWeight = FontWeight.Bold, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ModoTema.entries.forEach { modo ->
                    FilterChip(
                        selected = modo == actual,
                        onClick = { onCambiar(modo) },
                        label = { Text(modo.etiqueta) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GreenPrimary,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = modo == actual,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = GreenPrimary
                        )
                    )
                }
            }
            Text(
                if (actual == ModoTema.SISTEMA) "Sigue el modo claro u oscuro de tu teléfono." else "Se queda así aunque cambies el modo del teléfono.",
                fontSize = 12.sp,
                color = TextSecondary
            )
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
