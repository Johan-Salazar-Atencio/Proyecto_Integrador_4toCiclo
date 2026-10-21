package com.plantia.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.screens.home.HomeScreen
import com.plantia.screens.scan.HistorialScreen
import com.plantia.screens.scan.ScanScreen
import com.plantia.screens.scan.viewmodel.HistorialViewModel
import com.plantia.screens.scan.viewmodel.ScanViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(
                onNavigateToScan = { navController.navigate("scan") }
            )
        }
        composable("scan") {
            val scanViewModel: ScanViewModel = viewModel()
            ScanScreen(
                onNavigateToHome = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate("historial") }
            )
        }
        composable("historial") {
            val historialViewModel: HistorialViewModel = viewModel()
            HistorialScreen(
                escaneos = historialViewModel.escaneos,
                cargando = historialViewModel.cargando,
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
