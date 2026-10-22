package com.plantia.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.plantia.data.ConexionConfig
import com.plantia.data.Sesion
import com.plantia.data.remote.RetrofitClient
import com.plantia.screens.catalog.CatalogoScreen
import com.plantia.screens.catalog.viewmodel.CatalogoViewModel
import com.plantia.screens.catalog.viewmodel.CuidadosCatalogoViewModel
import com.plantia.screens.collection.ColeccionScreen
import com.plantia.screens.collection.viewmodel.ColeccionDetalleViewModel
import com.plantia.screens.collection.viewmodel.ColeccionViewModel
import com.plantia.screens.common.DetallePlantaScreen
import com.plantia.screens.home.HomeScreen
import com.plantia.screens.jardin.JardinScreen
import com.plantia.screens.jardin.viewmodel.JardinViewModel
import com.plantia.screens.profile.PerfilScreen
import com.plantia.screens.profile.viewmodel.PerfilViewModel
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
                onNavigateToScan = { navController.navigate("scan") },
                onNavigateToColeccion = { navController.navigate("coleccion") },
                onNavigateToCatalogo = { navController.navigate("catalogo") },
                onNavigateToJardin = { navController.navigate("jardin") },
                onNavigateToPerfil = { navController.navigate("perfil") }
            )
        }

        composable("perfil") {
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

        composable("jardin") {
            val jardinViewModel: JardinViewModel = viewModel()
            JardinScreen(
                planta = jardinViewModel.planta,
                cargando = jardinViewModel.cargando,
                error = jardinViewModel.error,
                onVolver = { navController.popBackStack() }
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

        composable("coleccion") {
            val coleccionViewModel: ColeccionViewModel = viewModel()
            ColeccionScreen(
                items = coleccionViewModel.items,
                cargando = coleccionViewModel.cargando,
                error = coleccionViewModel.error,
                onVolver = { navController.popBackStack() },
                onItemClick = { navController.navigate("coleccion/${it.id}") }
            )
        }

        composable(
            route = "coleccion/{id}",
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

        composable("catalogo") {
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
                onItemClick = { navController.navigate("catalogo/${it.id}") }
            )
        }

        composable(
            route = "catalogo/{id}",
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
    }
}
