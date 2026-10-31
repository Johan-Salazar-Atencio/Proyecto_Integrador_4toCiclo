package com.plantia.screens.scan.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.remote.ScanResultDto
import com.plantia.screens.scan.viewmodel.ScanUiState
import java.io.File

private val Verde = Color(0xFF10B981)
private val FondoTarjeta = Color(0xFF111827)

@Composable
fun PlantResultCard(
    state: ScanUiState,
    foto: File?,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = FondoTarjeta),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            when (state) {
                is ScanUiState.Loading -> LoadingContent()
                is ScanUiState.Success -> SuccessContent(state.result, foto)
                is ScanUiState.Error -> ErrorContent(state.message)
                ScanUiState.Idle -> Unit
            }
            if (state !is ScanUiState.Loading) {
                IconButton(
                    onClick = onCerrar,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(color = Verde, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Analizando...", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Identificando la planta con IA",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun SuccessContent(result: ScanResultDto, foto: File?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val bitmap = remember(foto) {
            foto?.let { BitmapFactory.decodeFile(it.absolutePath)?.asImageBitmap() }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.nombreComun ?: "Planta desconocida",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = result.nombreCientifico ?: "Especie no identificada",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Badge("Confianza ${formatConfianza(result.confianza)}")
            if (result.nuevoEnColeccion) {
                Spacer(Modifier.width(8.dp))
                Badge("Nueva en tu coleccion")
            }
        }

        val cuidados = result.cuidados.orEmpty()
        if (cuidados.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Cuidados", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            cuidados.forEach { c ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Text(
                        text = c.titulo ?: c.tipo ?: "Cuidado",
                        color = Verde,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = c.contenido ?: "",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Text(
            "No se pudo escanear",
            color = Color(0xFFF87171),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        Text(message, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(Verde.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

private fun formatConfianza(confianza: Double?): String {
    if (confianza == null) return "0%"
    val valor = if (confianza <= 1.0) confianza * 100 else confianza
    return "${valor.toInt()}%"
}
