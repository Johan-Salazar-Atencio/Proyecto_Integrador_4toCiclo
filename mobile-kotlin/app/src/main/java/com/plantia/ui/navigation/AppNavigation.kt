package com.plantia.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.plantia.ui.auth.AuthScreen
import com.plantia.ui.catalog.CatalogoScreen
import com.plantia.ui.catalog.DetallePlantaScreen
import com.plantia.ui.catalog.viewmodel.CatalogoViewModel
import com.plantia.ui.catalog.viewmodel.CuidadosCatalogoViewModel
import com.plantia.ui.collection.CollectionScreen
import com.plantia.ui.detail.CollectionDetailScreen
import com.plantia.ui.detail.PlantDetailScreen
import com.plantia.ui.garden.VirtualGardenScreen
import com.plantia.ui.logros.HostCelebracionLogros
import com.plantia.ui.logros.LogrosScreen
import com.plantia.ui.misiones.MisionesScreen
import com.plantia.ui.home.HomeScreen
import com.plantia.ui.legal.TermsScreen
import com.plantia.ui.profile.EditarDatosScreen
import com.plantia.ui.profile.PerfilScreen
import com.plantia.ui.profile.viewmodel.PerfilViewModel
import com.plantia.ui.scan.HistorialScreen
import com.plantia.ui.scan.ScanScreen
import com.plantia.ui.scan.viewmodel.HistorialViewModel
import com.plantia.ui.tienda.TiendaScreen
import com.plantia.ui.welcome.WelcomeScreen

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
    // QA-56: con sesion guardada se abre en el inicio; si el token ya no vale (401) se vuelve a la bienvenida
    val rutaInicial = androidx.compose.runtime.remember { Routes.inicial(com.plantia.data.local.Sesion.token.value) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (rutaInicial == Routes.HOME) {
            val codigo = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { com.plantia.data.remote.RetrofitClient.api.usuarioMe().code() }.getOrNull()
            }
            if (codigo == 401) {
                com.plantia.data.local.Sesion.setToken("")
                navController.navigate(Routes.WELCOME) { popUpTo(navController.graph.id) { inclusive = true } }
            }
        }
    }
    val redLocalNegada by com.plantia.data.local.PermisoRedLocal.negado.collectAsState()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route

    Scaffold(
        modifier = modifier,
        // QA-62: si se nego "red local", la app no llega al servidor del PC: se explica y se ofrece ir a Ajustes
        topBar = { if (redLocalNegada) com.plantia.ui.components.AvisoRedLocal() },
        bottomBar = {
            if (rutaActual in rutasConBarra) {
                BarraInferior(
                    rutaActual = rutaActual,
                    onIr = { ruta ->
                        if (ruta != rutaActual) {
                            navController.navigate(ruta) {
                                // Cada pestana parte del inicio: no se acumulan pantallas al ir de una a otra.
                                popUpTo(Routes.HOME) { inclusive = ruta == Routes.HOME }
                                launchSingleTop = true
                            }
                        }
                    },
                    onEscanear = { navController.navigate(Routes.SCAN) }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { relleno ->
    NavHost(
        navController = navController,
        startDestination = rutaInicial,
        modifier = Modifier.padding(relleno).consumeWindowInsets(relleno),
        enterTransition = entra,
        exitTransition = sale,
        popEnterTransition = reEntra,
        popExitTransition = reSale
    ) {
        // Entrada
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

        // App
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToGarden = { navController.navigate(Routes.VIRTUAL_GARDEN) },
                onNavigateToTienda = { navController.navigate(Routes.TIENDA) },
                onNavigateToMisiones = { navController.navigate(Routes.MISIONES) },
                onScan = { navController.navigate(Routes.SCAN) }
            )
        }

        // Misiones
        composable(Routes.MISIONES) {
            MisionesScreen(
                onBack = { navController.popBackStack() },
                onEscanear = { navController.navigate(Routes.SCAN) },
                onPlantasVirtuales = { navController.navigate(Routes.COLLECTION_DIGITALES) }
            )
        }

        // Logros
        composable(Routes.LOGROS) {
            LogrosScreen(onBack = { navController.popBackStack() })
        }

        // Escaneo
        composable(Routes.SCAN) {
            ScanScreen(
                onNavigateToHome = { navController.popBackStack() },
                onScanSuccess = { navController.navigate(Routes.plantDetail("ultimo")) },
                onNavigateToHistory = { navController.navigate(Routes.HISTORIAL) }
            )
        }

        composable(Routes.HISTORIAL) {
            val historialViewModel: HistorialViewModel = viewModel(factory = HistorialViewModel.Factory)
            HistorialScreen(
                escaneos = historialViewModel.escaneos,
                cargando = historialViewModel.cargando,
                onVolver = { navController.popBackStack() }
            )
        }

        // Ficha de la planta escaneada
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

        // Jardin
        composable(Routes.VIRTUAL_GARDEN) {
            VirtualGardenScreen(
                onBack = { navController.popBackStack() },
                onScan = { navController.navigate(Routes.SCAN) },
                onTienda = { navController.navigate(Routes.TIENDA) }
            )
        }

        // Tienda
        composable(Routes.TIENDA) {
            TiendaScreen(onBack = { navController.popBackStack() })
        }

        // Coleccion
        composable(Routes.COLLECTION) {
            CollectionScreen(
                onBack = { navController.popBackStack() },
                onPlantaEscaneada = { planta -> navController.navigate(Routes.collectionDetail(planta.id)) },
                // La pantalla ya dejo elegida la planta; aqui solo se abre el jardin.
                onPlantaDigital = { navController.navigate(Routes.VIRTUAL_GARDEN) },
                onScan = { navController.navigate(Routes.SCAN) }
            )
        }

        // Coleccion abierta en las plantas virtuales: ahi se entra a cada una y se previene el evento o se cuida
        composable(Routes.COLLECTION_DIGITALES) {
            CollectionScreen(
                onBack = { navController.popBackStack() },
                onPlantaEscaneada = { planta -> navController.navigate(Routes.collectionDetail(planta.id)) },
                onPlantaDigital = { navController.navigate(Routes.VIRTUAL_GARDEN) },
                onScan = { navController.navigate(Routes.SCAN) },
                abrirDigitales = true
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

        // Catalogo
        composable(Routes.CATALOGO) {
            val catalogoViewModel: CatalogoViewModel = viewModel(factory = CatalogoViewModel.Factory)
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
            val cuidadosViewModel: CuidadosCatalogoViewModel = viewModel(factory = CuidadosCatalogoViewModel.Factory)
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

        // Perfil
        composable(Routes.PERFIL) {
            val perfilViewModel: PerfilViewModel = viewModel(factory = PerfilViewModel.Factory)
            val baseUrl by perfilViewModel.baseUrl.collectAsState()
            val hostManual by perfilViewModel.hostManual.collectAsState()
            val modoTema by perfilViewModel.modoTema.collectAsState()
            PerfilScreen(
                usuario = perfilViewModel.usuario,
                cargando = perfilViewModel.cargando,
                error = perfilViewModel.error,
                digitales = perfilViewModel.digitales,
                modoTema = modoTema,
                regiones = perfilViewModel.regiones,
                cambiandoRegion = perfilViewModel.cambiandoRegion,
                errorRegion = perfilViewModel.errorRegion,
                onCambiarRegion = perfilViewModel::cambiarRegion,
                subiendoFoto = perfilViewModel.subiendoFoto,
                errorFoto = perfilViewModel.errorFoto,
                onCambiarFoto = perfilViewModel::cambiarFoto,
                baseUrlActual = baseUrl,
                hostManualActual = hostManual.orEmpty(),
                onCambiarTema = perfilViewModel::cambiarTema,
                onGuardarHost = perfilViewModel::guardarHost,
                onReintentar = { perfilViewModel.recargar() },
                onVerLogros = { navController.navigate(Routes.LOGROS) },
                onVerHistorial = { navController.navigate(Routes.HISTORIAL) },
                onVerTerminos = { navController.navigate(Routes.TERMINOS) },
                onEditarDatos = { navController.navigate(Routes.EDITAR_DATOS) },
                onCerrarSesion = {
                    perfilViewModel.cerrarSesion()
                    navController.navigate(Routes.WELCOME) { popUpTo(Routes.HOME) { inclusive = true } }
                }
            )
        }

        composable(Routes.EDITAR_DATOS) {
            EditarDatosScreen(onBack = { navController.popBackStack() })
        }
    }
    }

    // Celebra los logros nuevos uno tras otro, encima de cualquier pantalla
    HostCelebracionLogros(onVerLogros = { navController.navigate(Routes.LOGROS) { launchSingleTop = true } })
}
