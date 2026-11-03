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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.plantia.data.ConexionConfig
import com.plantia.data.TemaConfig
import com.plantia.data.MockGardenData
import com.plantia.data.ScanResultStore
import com.plantia.data.Sesion
import com.plantia.data.remote.RetrofitClient
import com.plantia.screens.auth.AuthScreen
import com.plantia.screens.catalog.CatalogoScreen
import com.plantia.screens.catalog.viewmodel.CatalogoViewModel
import com.plantia.screens.catalog.viewmodel.CuidadosCatalogoViewModel
import com.plantia.screens.collection.CollectionScreen
import com.plantia.screens.common.DetallePlantaScreen
import com.plantia.screens.detail.CollectionDetailScreen
import com.plantia.screens.detail.PlantDetailScreen
import com.plantia.screens.garden.VirtualGardenScreen
import com.plantia.screens.home.HomeScreen
import com.plantia.screens.legal.TermsScreen
import com.plantia.screens.profile.PerfilScreen
import com.plantia.screens.profile.viewmodel.PerfilViewModel
import com.plantia.screens.scan.HistorialScreen
import com.plantia.screens.scan.ScanScreen
import com.plantia.screens.scan.viewmodel.HistorialViewModel
import com.plantia.screens.scan.viewmodel.ScanViewModel
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
        // ─── Entrada ───────────────────────────────────────────────────────────
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

        // ─── App ───────────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToScan = { navController.navigate(Routes.SCAN) },
                onNavigateToGarden = { navController.navigate(Routes.VIRTUAL_GARDEN) },
                onNavigateToCollection = { navController.navigate(Routes.COLLECTION) },
                onNavigateToCatalogo = { navController.navigate(Routes.CATALOGO) },
                onNavigateToPerfil = { navController.navigate(Routes.PERFIL) }
            )
        }

        // ─── Scan flow ─────────────────────────────────────────────────────────
        composable(Routes.SCAN) {
            ScanScreen(
                onNavigateToHome = { navController.popBackStack() },
                onScanSuccess = { navController.navigate(Routes.plantDetail("ultimo")) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORIAL) }
            )
        }

        composable(Routes.HISTORIAL) {
            val historialViewModel: HistorialViewModel = viewModel()
            HistorialScreen(
                escaneos = historialViewModel.escaneos,
                cargando = historialViewModel.cargando,
                onVolver = { navController.popBackStack() }
            )
        }

        // ─── Plant details (from scan) ─────────────────────────────────────────
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

        // ─── Garden ────────────────────────────────────────────────────────────
        composable(Routes.VIRTUAL_GARDEN) {
            VirtualGardenScreen(
                onBack = { navController.popBackStack() },
                onScan = { navController.navigate(Routes.SCAN) }
            )
        }

        // ─── Coleccion ─────────────────────────────────────────────────────────
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

        // ─── Catalogo ──────────────────────────────────────────────────────────
        composable(Routes.CATALOGO) {
            val catalogoViewModel: CatalogoViewModel = viewModel()
            CatalogoScreen(
                items = catalogoViewModel.items,
                consulta = catalogoViewModel.consulta,
                soloToxicas = catalogoViewModel.soloToxicas,
                cargando = catalogoViewModel.cargando,
                error = catalogoViewModel.error,
                onConsultaCambia = { catalogoViewModel.onConsultaCambia(it) },
                onToxicasCambia = { catalogoViewModel.onToxicasCambia(it) },
                onVolver = { navController.popBackStack() },
                onItemClick = { navController.navigate(Routes.catalogoDetalle(it.id)) }
            )
        }

        composable(
            route = Routes.CATALOGO_DETALLE,
            arguments = listOf(navArgument(Routes.ARG_CATALOGO_ID) { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt(Routes.ARG_CATALOGO_ID) ?: 0
            val cuidadosViewModel: CuidadosCatalogoViewModel = viewModel()
            androidx.compose.runtime.LaunchedEffect(id) {
                cuidadosViewModel.cargar(id)
            }
            val planta = cuidadosViewModel.planta
            DetallePlantaScreen(
                titulo = "Ficha de la planta",
                nombreComun = planta?.nombreComun.orEmpty(),
                nombreCientifico = planta?.nombreCientifico.orEmpty(),
                cuidados = cuidadosViewModel.cuidados,
                cargando = cuidadosViewModel.cargando,
                error = cuidadosViewModel.error,
                onVolver = { navController.popBackStack() }
            )
        }

        // ─── Perfil ────────────────────────────────────────────────────────────
        composable(Routes.PERFIL) {
            val perfilViewModel: PerfilViewModel = viewModel()
            val baseUrl by RetrofitClient.baseUrl.collectAsState()
            val hostManual by ConexionConfig.host.collectAsState()
            val modoTema by TemaConfig.modo.collectAsState()
            PerfilScreen(
                usuario = perfilViewModel.usuario,
                cargando = perfilViewModel.cargando,
                error = perfilViewModel.error,
                escaneadas = perfilViewModel.escaneadas,
                digitales = MockGardenData.plantasDigitales.size,
                modoTema = modoTema,
                baseUrlActual = baseUrl,
                hostManualActual = hostManual.orEmpty(),
                onCambiarTema = { TemaConfig.setModo(it) },
                onGuardarHost = { host ->
                    ConexionConfig.setHost(host)
                    RetrofitClient.reconectar()
                    perfilViewModel.recargar()
                },
                onReintentar = { perfilViewModel.recargar() },
                onVerHistorial = { navController.navigate(Routes.HISTORIAL) },
                onVerTerminos = { navController.navigate(Routes.TERMINOS) },
                onCerrarSesion = {
                    // Vuelve al usuario por defecto y borra lo simulado en memoria de esta sesion.
                    Sesion.setUserId(Sesion.USUARIO_POR_DEFECTO)
                    MockGardenData.reiniciar()
                    ScanResultStore.ultimo = null
                    navController.navigate(Routes.WELCOME) { popUpTo(Routes.HOME) { inclusive = true } }
                },
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
