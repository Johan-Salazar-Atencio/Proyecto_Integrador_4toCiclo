package com.plantia.screens.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.data.CollectionRepository
import com.plantia.data.ColeccionResultado
import com.plantia.screens.home.components.DailyMissionCard
import com.plantia.screens.home.components.PlantSummaryCard
import com.plantia.screens.home.components.QuickAccessGrid
import com.plantia.screens.home.components.UserHeaderCard
import com.plantia.screens.home.components.WeatherAlertCard
import com.plantia.screens.home.viewmodel.HomeViewModel
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.AmberLight
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToScan: () -> Unit = {},
    onNavigateToGarden: () -> Unit = {},
    onNavigateToCollection: () -> Unit = {},
    onNavigateToCatalogo: () -> Unit = {},
    onNavigateToPerfil: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    val bottomNavItems = listOf(
        BottomNavItem("Inicio", Icons.Default.Home),
        BottomNavItem("Escanear", Icons.Default.PhotoCamera),
        BottomNavItem("Jardín", Icons.Default.Yard),
        BottomNavItem("Colección", Icons.Default.Collections),
        BottomNavItem("Perfil", Icons.Default.Person)
    )
    var selectedTab by remember { mutableIntStateOf(0) }

    var confirmarSalida by remember { mutableStateOf(false) }

    // Cantidad real de plantas de la colección (mock fallback); se recarga cada vez que se vuelve al inicio.
    var totalColeccion by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(Unit) {
        totalColeccion = (CollectionRepository.listar() as? ColeccionResultado.Con)?.plantas?.size
    }

    val homeViewModel: HomeViewModel = viewModel()
    val state = homeViewModel.uiState

    val accesosRapidos = if (totalColeccion != null) {
        state.accesos.map { item ->
            if (item.id != "collection") {
                item
            } else {
                item.copy(
                    subtitle = when (val n = totalColeccion) {
                        null -> "Tus plantas"
                        0 -> "Aun sin plantas"
                        1 -> "1 planta descubierta"
                        else -> "$n plantas descubiertas"
                    }
                )
            }
        }
    } else {
        state.accesos
    }

    if (confirmarSalida) {
        AlertDialog(
            onDismissRequest = { confirmarSalida = false },
            containerColor = Color.White,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Volverás a la pantalla de bienvenida.") },
            confirmButton = {
                TextButton(onClick = { confirmarSalida = false; onCerrarSesion() }) {
                    Text("Cerrar sesión", color = GreenPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalida = false }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Inicio",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { confirmarSalida = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión",
                            tint = TextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AmberLight,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${state.usuario?.leaves ?: 0}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAlert
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = GreenPrimary,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "J",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        selected = selectedTab == index,
                        onClick = {
                            selectedTab = index
                            when (index) {
                                1 -> onNavigateToScan()
                                2 -> onNavigateToGarden()
                                3 -> onNavigateToCollection()
                                4 -> onNavigateToPerfil()
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GreenPrimary,
                            selectedTextColor = GreenPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = GreenPrimary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
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
                        color = Color(0xFFFEE2E2)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mensaje,
                                fontSize = 13.sp,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Reintentar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
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
                    UserHeaderCard(user = usuario)
                }

                state.alerta?.let { alerta ->
                    WeatherAlertCard(
                        alert = alerta,
                        onActionClick = {}
                    )
                }

                state.planta?.let { planta ->
                    PlantSummaryCard(
                        plant = planta,
                        onEnterGarden = onNavigateToGarden
                    )
                }

                if (accesosRapidos.isNotEmpty()) {
                    QuickAccessGrid(
                        items = accesosRapidos,
                        onItemClick = { item ->
                            when (item.id) {
                                "scan" -> onNavigateToScan()
                                "collection" -> onNavigateToCollection()
                                "encyclopedia" -> onNavigateToCatalogo()
                                "garden" -> onNavigateToGarden()
                                "jardin" -> onNavigateToGarden()
                                // "store" todavía no tiene endpoint en el backend.
                            }
                        }
                    )
                }

                state.mision?.let { mision ->
                    DailyMissionCard(mission = mision)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
