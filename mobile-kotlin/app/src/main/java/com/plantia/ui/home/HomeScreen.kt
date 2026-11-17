package com.plantia.ui.home

import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import com.plantia.ui.theme.FondoDegradado

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.ui.components.AvisoToast
import com.plantia.ui.escena.EscenaViewModel
import com.plantia.ui.home.components.EventoCard
import com.plantia.ui.home.components.MisionesResumenCard
import com.plantia.ui.home.components.PlantSummaryCard
import com.plantia.ui.home.components.SinEventoCard
import com.plantia.ui.home.components.SinPlantasCard
import com.plantia.ui.home.components.UserHeaderCard
import com.plantia.ui.home.viewmodel.HomeViewModel
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.RojoClaro
import com.plantia.ui.theme.RojoTexto
import com.plantia.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToGarden: () -> Unit = {},
    onNavigateToTienda: () -> Unit = {},
    onNavigateToMisiones: () -> Unit = {},
    onScan: () -> Unit = {}
) {
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
    val escenaViewModel: EscenaViewModel = viewModel(factory = EscenaViewModel.Factory)
    val state = homeViewModel.uiState
    // Al pasar a otra planta la escena (fondo y arte) se vuelve a pedir
    LaunchedEffect(state.planta?.id) { escenaViewModel.recargar() }

    // El total de la coleccion cambia al escanear o guardar: se actualiza cada vez que se vuelve al inicio.
    LaunchedEffect(Unit) { homeViewModel.refrescarColeccion() }

    // Alerta, perfil y planta se refrescan solos mientras el Home esta a la vista (se pausa en segundo plano).
    val ciclo = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(ciclo) {
        ciclo.repeatOnLifecycle(Lifecycle.State.STARTED) { homeViewModel.sondear() }
    }
    AvisoToast(homeViewModel.aviso, homeViewModel::avisoMostrado)

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(FondoDegradado)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (state.cargando) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
            } else {
                state.error?.let { mensaje ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = RojoClaro
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mensaje,
                                fontSize = 13.sp,
                                color = RojoTexto,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Reintentar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RojoTexto,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { homeViewModel.recargar() }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                state.usuario?.let { usuario ->
                    UserHeaderCard(user = usuario, onHojas = onNavigateToTienda)
                }

                // Sin plantas el Inicio no queda vacio: invita a escanear la primera
                if (state.planta == null && state.error == null) {
                    SinPlantasCard(onScan = onScan)
                }

                state.planta?.let { planta ->
                    PlantSummaryCard(
                        plant = planta,
                        total = state.plantas.size,
                        escena = escenaViewModel.escena,
                        onEnterGarden = onNavigateToGarden,
                        onAnterior = homeViewModel::anterior,
                        onSiguiente = homeViewModel::siguiente,
                        onCuidar = homeViewModel::cuidar
                    )
                }

                // El evento activo con su imagen y cuenta regresiva; tocarlo lleva a Misiones, donde se previene
                // Sin evento se ve un aviso tranquilo con consejos que van pasando
                if (state.alerta != null) {
                    EventoCard(alerta = state.alerta, onClick = onNavigateToMisiones)
                } else if (state.error == null) {
                    SinEventoCard()
                }

                if (state.misiones.isNotEmpty()) {
                    MisionesResumenCard(misiones = state.misiones, onClick = onNavigateToMisiones)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
