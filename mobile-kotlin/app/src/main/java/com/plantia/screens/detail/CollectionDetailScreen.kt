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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.ui.components.AvisoToast
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/**
 * Ficha de una planta ya guardada en la coleccion: mismos cuidados reales que devuelve el
 * backend (`GET /coleccion/{id}`), sin porcentaje de IA y solo con la opcion de digitalizar.
 */
@Composable
fun CollectionDetailScreen(
    coleccionId: Int,
    onBack: () -> Unit = {},
    onDigitalize: () -> Unit = {},
    viewModel: PlantDetailViewModel = viewModel(factory = PlantDetailViewModel.Factory)
) {
    LaunchedEffect(coleccionId) { viewModel.cargarDeColeccion(coleccionId) }
    AvisoToast(viewModel.aviso, viewModel::avisoMostrado)

    when (val estado = viewModel.uiState) {
        DetalleUiState.Cargando, DetalleUiState.SinResultado ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        is DetalleUiState.Fallo -> Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No se pudo cargar la planta", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Text(estado.mensaje, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.cargarDeColeccion(coleccionId) },
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
        is DetalleUiState.Listo -> PlantDetailContent(
            resultado = estado.resultado,
            onBack = onBack,
            onDigitalize = { viewModel.digitalizar(onDigitalize) },
            onSave = {},
            mostrarGuardar = false
        )
    }
}
