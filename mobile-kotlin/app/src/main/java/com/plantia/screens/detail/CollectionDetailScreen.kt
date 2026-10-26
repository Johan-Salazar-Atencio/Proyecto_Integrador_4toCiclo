package com.plantia.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.DetalleColeccionResultado
import com.plantia.data.CollectionRepository
import com.plantia.data.MockGardenData
import com.plantia.data.remote.ScanResultDto
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

private sealed interface EstadoDetalle {
    data object Cargando : EstadoDetalle
    data class Listo(val planta: ScanResultDto) : EstadoDetalle
    data class Fallo(val mensaje: String) : EstadoDetalle
}

/**
 * Ficha de una planta ya guardada en la coleccion: mismos cuidados reales que devuelve el
 * backend (`GET /coleccion/{id}`), sin porcentaje de IA y solo con la opcion de digitalizar.
 */
@Composable
fun CollectionDetailScreen(
    coleccionId: Int,
    onBack: () -> Unit = {},
    onDigitalize: () -> Unit = {}
) {
    var intento by remember { mutableStateOf(0) }
    var estado by remember { mutableStateOf<EstadoDetalle>(EstadoDetalle.Cargando) }

    LaunchedEffect(coleccionId, intento) {
        estado = EstadoDetalle.Cargando
        estado = when (val r = CollectionRepository.detalle(coleccionId)) {
            is DetalleColeccionResultado.Con -> EstadoDetalle.Listo(r.planta)
            is DetalleColeccionResultado.Fallo -> EstadoDetalle.Fallo(r.mensaje)
        }
    }

    when (val e = estado) {
        EstadoDetalle.Cargando -> Box(Modifier.fillMaxSize().padding(0.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenPrimary)
        }
        is EstadoDetalle.Fallo -> Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No se pudo cargar la planta", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(e.mensaje, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { intento++ },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.White)
            ) { Text("Reintentar") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BackgroundLight, contentColor = TextPrimary)
            ) { Text("Volver") }
        }
        is EstadoDetalle.Listo -> PlantDetailContent(
            resultado = e.planta,
            onBack = onBack,
            onDigitalize = {
                // Simulado: la planta guardada pasa a ser una planta digital activa.
                MockGardenData.digitalizar(e.planta)
                onDigitalize()
            },
            onSave = {},
            mostrarGuardar = false
        )
    }
}
