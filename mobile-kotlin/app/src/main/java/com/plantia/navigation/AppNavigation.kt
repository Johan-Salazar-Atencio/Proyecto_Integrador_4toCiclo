package com.plantia.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.plantia.screens.auth.AuthScreen
import com.plantia.screens.collection.CollectionScreen
import com.plantia.data.MockGardenData
import com.plantia.screens.detail.CollectionDetailScreen
import com.plantia.screens.detail.PlantDetailScreen
import com.plantia.screens.garden.VirtualGardenScreen
import com.plantia.screens.home.HomeScreen
import com.plantia.screens.legal.TermsScreen
import com.plantia.screens.scan.ScanScreen
import com.plantia.screens.welcome.WelcomeScreen

private const val DURACION_MS = 350

private typealias Enter = AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition
private typealias Exit = AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition

// Avanzar: la nueva pantalla entra por la derecha, la actual sale parcialmente a la izquierda.
private val entra: Enter = {
    slideInHorizontally(tween(DURACION_MS)) { it } + fadeIn(tween(DURACION_MS))
}
private val sale: Exit = {
    slideOutHorizontally(tween(DURACION_MS)) { -it / 3 } + fadeOut(tween(DURACION_MS))
}

// Volver (pop): sentido inverso.
private val reEntra: Enter = {
    slideInHorizontally(tween(DURACION_MS)) { -it / 3 } + fadeIn(tween(DURACION_MS))
}
private val reSale: Exit = {
    slideOutHorizontally(tween(DURACION_MS)) { it } + fadeOut(tween(DURACION_MS))
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME,
        modifier = modifier,
        enterTransition = entra,
        exitTransition = sale,
        popEnterTransition = reEntra,
        popExitTransition = reSale
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onCrearCuenta = { navController.navigate(Routes.auth(registro = true)) },
                onIniciarSesion = { navController.navigate(Routes.auth(registro = false)) }
            )
        }
        composable(
            route = Routes.AUTH,
            arguments = listOf(navArgument(Routes.ARG_MODO) { type = NavType.StringType })
        ) { entry ->
            AuthScreen(
                modoInicial = entry.arguments?.getString(Routes.ARG_MODO),
                onBack = { navController.popBackStack() },
                onTerminos = { navController.navigate(Routes.TERMINOS) },
                // Con sesion iniciada se entra a la app y Welcome/Auth salen de la pila.
                onAutenticado = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.WELCOME) { inclusive = true } }
                }
            )
        }
        composable(Routes.TERMINOS) {
            TermsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToScan = { navController.navigate(Routes.SCAN) },
                onNavigateToGarden = { navController.navigate(Routes.VIRTUAL_GARDEN) },
                onNavigateToCollection = { navController.navigate(Routes.COLLECTION) }
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onNavigateToHome = { navController.popBackStack() },
                onScanSuccess = { navController.navigate(Routes.plantDetail("ultimo")) }
            )
        }
        composable(
            route = Routes.PLANT_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_PLANT_ID) { type = NavType.StringType })
        ) { entry ->
            PlantDetailScreen(
                plantId = entry.arguments?.getString(Routes.ARG_PLANT_ID),
                onBack = { navController.popBackStack() },
                // Digitalizar: la planta pasa al jardin virtual; atras desde alli vuelve al inicio.
                onDigitalize = {
                    navController.navigate(Routes.VIRTUAL_GARDEN) { popUpTo(Routes.HOME) }
                },
                // Guardar: a la coleccion de plantas (el backend ya registra cada escaneo).
                onSave = {
                    navController.navigate(Routes.COLLECTION) { popUpTo(Routes.HOME) }
                }
            )
        }
        composable(Routes.VIRTUAL_GARDEN) {
            VirtualGardenScreen(
                onBack = { navController.popBackStack() },
                onScan = { navController.navigate(Routes.SCAN) }
            )
        }
        composable(Routes.COLLECTION) {
            CollectionScreen(
                onBack = { navController.popBackStack() },
                onPlantaEscaneada = { planta -> navController.navigate(Routes.collectionDetail(planta.id)) },
                // Planta digital: se vuelve la activa y se abre el jardin con ella.
                onPlantaDigital = { planta ->
                    MockGardenData.activar(planta.name)
                    navController.navigate(Routes.VIRTUAL_GARDEN)
                },
                onScan = { navController.navigate(Routes.SCAN) }
            )
        }
        composable(
            route = Routes.COLLECTION_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_COLECCION_ID) { type = NavType.IntType })
        ) { entry ->
            CollectionDetailScreen(
                coleccionId = entry.arguments?.getInt(Routes.ARG_COLECCION_ID) ?: 0,
                onBack = { navController.popBackStack() },
                onDigitalize = {
                    navController.navigate(Routes.VIRTUAL_GARDEN) { popUpTo(Routes.HOME) }
                }
            )
        }
    }
}
