package com.plantia.ui.misiones

import com.plantia.ui.theme.DividerColor

import com.plantia.ui.theme.SuperficieElevada

import com.plantia.ui.theme.SobrePrimario

import com.plantia.ui.theme.Peligro

import androidx.compose.foundation.background
import com.plantia.ui.theme.FondoDegradado
import com.plantia.ui.components.BarraProgreso

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.data.model.DailyMission
import com.plantia.ui.components.rememberCuentaRegresiva
import com.plantia.ui.home.components.DailyMissionCard
import com.plantia.ui.misiones.viewmodel.MisionesViewModel
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.RojoClaro
import com.plantia.ui.theme.RojoTexto
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.util.IconosOscurosEnBarraDeEstado
import com.plantia.util.formatoCuentaRegresiva

/**
 * Todas las misiones de hoy. Con un evento activo, su tarjeta roja va primero. Tocar una mision lleva a donde se
 * cumple: el escaner o las plantas virtuales (ahi se previene el evento planta por planta).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisionesScreen(
    onBack: () -> Unit = {},
    onEscanear: () -> Unit = {},
    onPlantasVirtuales: () -> Unit = {},
    viewModel: MisionesViewModel = viewModel(factory = MisionesViewModel.Factory)
) {
    IconosOscurosEnBarraDeEstado()
    val estado = viewModel.uiState

    // Al volver de una planta (donde se previno el evento o se cuido) el avance se actualiza solo
    val ciclo = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(ciclo) {
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.cargar() }
    }

    val ir: (DestinoMision?) -> Unit = { destino ->
        when (destino) {
            DestinoMision.ESCANEAR -> onEscanear()
            DestinoMision.PLANTAS_VIRTUALES -> onPlantasVirtuales()
            null -> Unit
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(FondoDegradado),
        topBar = {
            TopAppBar(
                title = { Text("Misiones", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (estado.cargando && estado.misiones.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
            } else if (estado.error != null && estado.misiones.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(estado.error, color = TextSecondary, textAlign = TextAlign.Center)
                    TextButton(onClick = viewModel::cargar) { Text("Reintentar", color = GreenDark, fontWeight = FontWeight.Bold) }
                }
            } else {
                estado.evento?.let { evento ->
                    TarjetaEvento(evento, onIr = { ir(destinoDeMision(evento)) })
                }
                Text(
                    text = "Misiones diarias · ${estado.completadas}/${estado.diarias.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                if (estado.diarias.isEmpty()) {
                    Text("Hoy no hay misiones diarias.", color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp))
                }
                estado.diarias.forEach { mision ->
                    val destino = destinoDeMision(mision)
                    DailyMissionCard(
                        mission = mision,
                        onClick = if (destino != null && !mision.isCompleted) ({ ir(destino) }) else null
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** La mision del evento activo: roja, con el avance, lo que paga y cuanto falta para que termine. */
@Composable
private fun TarjetaEvento(mision: DailyMission, onIr: () -> Unit) {
    val restante = rememberCuentaRegresiva(mision.endsInSeconds)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = RojoClaro),
        border = BorderStroke(1.5.dp, RojoTexto.copy(alpha = 0.6f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = RojoTexto, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("EVENTO ACTIVO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RojoTexto, modifier = Modifier.weight(1f))
                if (restante != null) {
                    Text(
                        "Termina en ${formatoCuentaRegresiva(restante)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = RojoTexto
                    )
                }
            }
            Text(mision.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            BarraProgreso(progreso = mision.progress, color = Peligro, pista = SuperficieElevada)
            Text(
                "${mision.currentCount} de ${mision.targetCount} plantas protegidas · ${mision.reward}",
                fontSize = 13.sp,
                color = TextPrimary
            )
            if (!mision.isCompleted) {
                Text(
                    "Entra a cada planta virtual y toca \"Prevenir evento\".",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Button(
                onClick = onIr,
                enabled = !mision.isCompleted,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Peligro,
                    contentColor = SobrePrimario,
                    disabledContainerColor = SuperficieElevada,
                    disabledContentColor = GreenDark
                )
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (mision.isCompleted) "Evento prevenido" else "Ir a mis plantas",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
