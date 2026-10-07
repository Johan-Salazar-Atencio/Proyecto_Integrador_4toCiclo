package com.plantia.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.plantia.screens.home.HomeScreen
import com.plantia.screens.scan.ScanScreen

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
            ScanScreen(
                onNavigateToHome = { navController.popBackStack() }
            )
        }
    }
}
