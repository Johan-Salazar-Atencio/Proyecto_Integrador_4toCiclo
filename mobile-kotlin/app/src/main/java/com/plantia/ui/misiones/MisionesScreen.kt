package com.plantia.ui.misiones

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.data.model.DailyMission
import com.plantia.data.model.PlantSummary
import com.plantia.ui.components.AvisoToast
import com.plantia.ui.components.rememberCuentaRegresiva
import com.plantia.ui.home.components.DailyMissionCard
import com.plantia.ui.misiones.viewmodel.MisionesViewModel
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.RojoClaro
import com.plantia.ui.theme.RojoTexto
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.util.IconosOscurosEnBarraDeEstado
import com.plantia.util.formatoCuentaRegresiva

/** Todas las misiones de hoy. Con un evento activo, su tarjeta roja va primero y permite prevenirlo. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisionesScreen(
    onBack: () -> Unit = {},
    viewModel: MisionesViewModel = viewModel(factory = MisionesViewModel.Factory)
) {
    IconosOscurosEnBarraDeEstado()
    val estado = viewModel.uiState
    AvisoToast(viewModel.aviso, viewModel::avisoMostrado)

    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Misiones", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
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
                    TarjetaEvento(evento, prevenibles = estado.prevenibles.size, onPrevenir = viewModel::abrirHoja)
                }
                Text(
                    text = "Misiones diarias · ${estado.completadas}/${estado.diarias.size}",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                if (estado.diarias.isEmpty()) {
                    Text("Hoy no hay misiones diarias.", color = TextSecondary, modifier = Modifier.padding(horizontal = 16.dp))
                }
                estado.diarias.forEach { DailyMissionCard(mission = it) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    val eventoActivo = estado.evento
    if (viewModel.hojaAbierta && eventoActivo != null) {
        HojaPrevenir(
            titulo = eventoActivo.title,
            plantas = estado.prevenibles,
            seleccion = viewModel.seleccion,
            previniendo = viewModel.previniendo,
            onAlternar = viewModel::alternar,
            onTodas = viewModel::alternarTodas,
            onPrevenir = viewModel::prevenir,
            onCerrar = viewModel::cerrarHoja
        )
    }
}

/** La mision del evento activo: roja, con el avance, lo que paga y cuanto falta para que termine. */
@Composable
private fun TarjetaEvento(mision: DailyMission, prevenibles: Int, onPrevenir: () -> Unit) {
    val restante = rememberCuentaRegresiva(mision.endsInSeconds)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
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
            LinearProgressIndicator(
                progress = { mision.progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = RojoTexto,
                trackColor = Color.White
            )
            Text(
                "${mision.currentCount} de ${mision.targetCount} plantas protegidas · ${mision.reward}",
                fontSize = 13.sp,
                color = TextPrimary
            )
            Button(
                onClick = onPrevenir,
                enabled = !mision.isCompleted && prevenibles > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RojoTexto,
                    contentColor = Color.White,
                    disabledContainerColor = Color.White,
                    disabledContentColor = if (mision.isCompleted) GreenDark else TextSecondary
                )
            ) {
                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        mision.isCompleted -> "Evento prevenido"
                        prevenibles == 0 -> "Sin plantas por proteger"
                        else -> "Prevenir evento"
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/** Plantas en las que se puede prevenir el evento: se marcan una a una o todas a la vez. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaPrevenir(
    titulo: String,
    plantas: List<PlantSummary>,
    seleccion: Set<Int>,
    previniendo: Boolean,
    onAlternar: (Int) -> Unit,
    onTodas: () -> Unit,
    onPrevenir: () -> Unit,
    onCerrar: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = BackgroundLight
    ) {
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(titulo, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Elige las plantas que quieres proteger", fontSize = 13.sp, color = TextSecondary)
                }
                TextButton(onClick = onTodas) {
                    Text(
                        if (seleccion.size == plantas.size) "Quitar todas" else "Todas",
                        color = GreenDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(plantas, key = { it.id ?: it.name.hashCode() }) { planta ->
                    val id = planta.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = id != null) { id?.let(onAlternar) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
                    ) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = id != null && id in seleccion,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = GreenPrimary),
                                modifier = Modifier.padding(8.dp)
                            )
                            Column {
                                Text(planta.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                if (planta.species.isNotBlank()) {
                                    Text(planta.species, fontSize = 12.sp, fontStyle = FontStyle.Italic, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onPrevenir,
                enabled = seleccion.isNotEmpty() && !previniendo,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RojoTexto, contentColor = Color.White)
            ) {
                Text(
                    text = when {
                        previniendo -> "Previniendo..."
                        seleccion.isEmpty() -> "Elige al menos una planta"
                        seleccion.size == 1 -> "Prevenir en 1 planta"
                        else -> "Prevenir en ${seleccion.size} plantas"
                    },
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
