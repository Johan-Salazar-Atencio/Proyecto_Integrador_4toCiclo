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
import com.plantia.data.MockGardenData
import com.plantia.data.Sesion
import com.plantia.data.remote.RetrofitClient
import com.plantia.screens.auth.AuthScreen
import com.plantia.screens.catalog.CatalogoScreen
import com.plantia.screens.catalog.viewmodel.CatalogoViewModel
import com.plantia.screens.catalog.viewmodel.CuidadosCatalogoViewModel
import com.plantia.screens.collection.ColeccionScreen
import com.plantia.screens.collection.CollectionScreen
import com.plantia.screens.collection.viewmodel.ColeccionDetalleViewModel
import com.plantia.screens.collection.viewmodel.ColeccionViewModel
import com.plantia.screens.common.DetallePlantaScreen
import com.plantia.screens.detail.CollectionDetailScreen
import com.plantia.screens.detail.PlantDetailScreen
import com.plantia.screens.garden.VirtualGardenScreen
import com.plantia.screens.home.HomeScreen
import com.plantia.screens.jardin.JardinScreen
import com.plantia.screens.jardin.viewmodel.JardinViewModel
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

// ─── Routes ───────────────────────────────────────────────────────────────────

object Routes {
    const val WELCOME = "welcome"
    const val AUTH = "auth"
    const val ARG_MODO = "modo"
    const val HOME = "home"
    const val SCAN = "scan"
    const val HISTORIAL = "historial"
    const val COLECCION = "coleccion"
    const val COLECCION_DETALLE = "coleccion/{id}"
    const val ARG_COLECCION_ID = "id"
    const val CATALOGO = "catalogo"
    const val CATALOGO_DETALLE = "catalogo/{id}"
    const val JARDIN = "jardin"
    const val PERFIL = "perfil"
    const val TERMINOS = "terminos"
    const val PLANT_DETAIL = "plantDetail"
    const val ARG_PLANT_ID = "plantId"
    const val COLLECTION = "collection"
    const val VIRTUAL_GARDEN = "virtualGarden"

    fun auth(registro: Boolean = false): String = "auth/$registro"
    fun plantDetail(plantId: String): String = "plantDetail/$plantId"
    fun collectionDetail(coleccionId: Int): String = "collection/$coleccionId"
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
        // ─── Auth flow (remoto/main) ───────────────────────────────────────────
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

        // ─── Main app ──────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToScan = { navController.navigate(Routes.SCAN) },
                onNavigateToGarden = { navController.navigate(Routes.VIRTUAL_GARDEN) },
                onNavigateToCollection = { navController.navigate(Routes.COLLECTION) },
                onNavigateToColeccion = { navController.navigate(Routes.COLECCION) },
                onNavigateToCatalogo = { navController.navigate(Routes.CATALOGO) },
                onNavigateToJardin = { navController.navigate(Routes.JARDIN) },
                onNavigateToPerfil = { navController.navigate(Routes.PERFIL) },
                // Aun no hay sesion guardada: cerrar sesion es volver al Welcome con la pila limpia.
                onCerrarSesion = {
                    navController.navigate(Routes.WELCOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
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

        composable(Routes.JARDIN) {
            val jardinViewModel: JardinViewModel = viewModel()
            JardinScreen(
                planta = jardinViewModel.planta,
                cargando = jardinViewModel.cargando,
                error = jardinViewModel.error,
                onVolver = { navController.popBackStack() }
            )
        }

        // ─── Collection (from scan flow) ───────────────────────────────────────
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

        // ─── Coleccion flow (from feat/mobile) ─────────────────────────────────
        composable(Routes.COLECCION) {
            val coleccionViewModel: ColeccionViewModel = viewModel()
            ColeccionScreen(
                items = coleccionViewModel.items,
                cargando = coleccionViewModel.cargando,
                error = coleccionViewModel.error,
                onVolver = { navController.popBackStack() },
                onItemClick = { navController.navigate(Routes.COLECCION_DETALLE.replace("{id}", it.id.toString())) }
            )
        }

        composable(
            route = Routes.COLECCION_DETALLE,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
            val detalleViewModel: ColeccionDetalleViewModel = viewModel()
            androidx.compose.runtime.LaunchedEffect(id) {
                detalleViewModel.cargar(id)
            }
            val item = detalleViewModel.item
            DetallePlantaScreen(
                titulo = "Mi coleccion",
                nombreComun = item?.nombreComun.orEmpty(),
                nombreCientifico = item?.nombreCientifico.orEmpty(),
                cuidados = detalleViewModel.cuidados,
                cargando = detalleViewModel.cargando,
                error = detalleViewModel.error,
                imagenUrl = item?.fotoUrl,
                onVolver = { navController.popBackStack() }
            )
        }

        // ─── Catalogo flow (from feat/mobile) ──────────────────────────────────
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
                onItemClick = { navController.navigate(Routes.CATALOGO_DETALLE.replace("{id}", it.id.toString())) }
            )
        }

        composable(
            route = Routes.CATALOGO_DETALLE,
            arguments = listOf(navArgument("id") { type = NavType.IntType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getInt("id") ?: 0
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

        // ─── Profile (from feat/mobile) ────────────────────────────────────────
        composable(Routes.PERFIL) {
            val perfilViewModel: PerfilViewModel = viewModel()
            val userId by Sesion.userId.collectAsState()
            val baseUrl by RetrofitClient.baseUrl.collectAsState()
            val hostManual by ConexionConfig.host.collectAsState()
            PerfilScreen(
                usuario = perfilViewModel.usuario,
                userIdActual = userId,
                cargando = perfilViewModel.cargando,
                error = perfilViewModel.error,
                baseUrlActual = baseUrl,
                hostManualActual = hostManual.orEmpty(),
                onGuardarHost = { host ->
                    ConexionConfig.setHost(host)
                    RetrofitClient.reconectar()
                    perfilViewModel.recargar()
                },
                onCambiarUsuario = { id ->
                    Sesion.setUserId(id)
                    perfilViewModel.recargar()
                },
                onVolver = { navController.popBackStack() }
            )
        }
    }
}
