package com.plantia.ui.detail

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.plantia.data.model.Cuidado
import com.plantia.data.remote.dto.CuidadoSeccionDto
import com.plantia.data.remote.dto.ScanResultDto
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.BlueInfo
import com.plantia.ui.theme.BlueLight
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.RojoClaro
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

private val RojoCalor = Color(0xFFEF4444)
private val RojoCalorLight: Color
    @Composable get() = RojoClaro

private data class EstiloCuidado(val icono: ImageVector, val color: Color, val fondo: Color)

@Composable
private fun estiloDe(tipo: String?): EstiloCuidado = when (tipo?.uppercase()) {
    "RIEGO" -> EstiloCuidado(Icons.Filled.WaterDrop, BlueInfo, BlueLight)
    "LUZ" -> EstiloCuidado(Icons.Filled.WbSunny, AmberAlert, AmberLight)
    "TEMPERATURA" -> EstiloCuidado(Icons.Filled.Thermostat, RojoCalor, RojoCalorLight)
    "SUSTRATO" -> EstiloCuidado(Icons.Filled.Grass, GreenDark, GreenLight)
    "PODA" -> EstiloCuidado(Icons.Filled.ContentCut, GreenDark, GreenLight)
    "ABONO" -> EstiloCuidado(Icons.Filled.Eco, GreenDark, GreenLight)
    else -> EstiloCuidado(Icons.Filled.Info, TextSecondary, BackgroundLight)
}

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
internal fun Cabecera(resultado: ScanResultDto, fallida: Boolean) {
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
internal fun Informacion(resultado: ScanResultDto, fallida: Boolean) {
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
internal fun BarraReintento(onReintentar: () -> Unit) {
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
internal fun BarraAcciones(
    puedeGuardar: Boolean,
    mostrarGuardar: Boolean,
    onDigitalize: () -> Unit,
    onSave: () -> Unit
) {
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
            // Desde la coleccion la planta ya esta guardada: solo se ofrece Digitalizar, en verde.
            if (!mostrarGuardar) {
                Button(
                    onClick = onDigitalize,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Digitalizar")
                }
                return@Row
            }
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
