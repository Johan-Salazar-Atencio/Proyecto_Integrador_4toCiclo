package com.plantia.ui.detail

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.data.remote.dto.ScanResultDto
import com.plantia.ui.components.AvisoToast
import com.plantia.ui.detail.viewmodel.DetalleUiState
import com.plantia.ui.detail.viewmodel.PlantDetailViewModel
import com.plantia.ui.preview.DatosEjemplo
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.MobilekotlinTheme
import com.plantia.ui.theme.TextPrimary

@Composable
fun PlantDetailScreen(
    plantId: String?,
    onBack: () -> Unit = {},
    onDigitalize: () -> Unit = {},
    onSave: () -> Unit = {},
    viewModel: PlantDetailViewModel = viewModel(factory = PlantDetailViewModel.Factory)
) {
    LaunchedEffect(plantId) { viewModel.mostrarUltimoEscaneo() }
    AvisoToast(viewModel.aviso, viewModel::avisoMostrado)

    when (val estado = viewModel.uiState) {
        // Sin escaneo en memoria (p. ej. Android cerro la app y la reabrio aqui) se vuelve atras.
        DetalleUiState.SinResultado -> LaunchedEffect(Unit) { onBack() }
        is DetalleUiState.Listo -> PlantDetailContent(
            resultado = estado.resultado,
            onBack = onBack,
            onDigitalize = { viewModel.digitalizar(onDigitalize) },
            onSave = { viewModel.guardar(onSave) }
        )
        DetalleUiState.Cargando, is DetalleUiState.Fallo -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailContent(
    resultado: ScanResultDto,
    onBack: () -> Unit,
    onDigitalize: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    mostrarGuardar: Boolean = true
) {
    val fallida = resultado.identificacionFallida()

    // Esta pantalla es clara: iconos oscuros en la barra de estado (se restauran al salir).
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val ventana = (view.context as? Activity)?.window
            val control = ventana?.let { WindowCompat.getInsetsController(it, view) }
            val anterior = control?.isAppearanceLightStatusBars
            control?.isAppearanceLightStatusBars = true
            onDispose { anterior?.let { control.isAppearanceLightStatusBars = it } }
        }
    }
    val config = LocalConfiguration.current
    val apaisado = config.screenWidthDp > config.screenHeightDp

    Scaffold(
        modifier = modifier,
        containerColor = BackgroundLight,
        topBar = {
            TopAppBar(
                title = { Text("Detalle de planta", fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = TextPrimary,
                    navigationIconContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            if (fallida) {
                BarraReintento(onBack)
            } else {
                BarraAcciones(
                    puedeGuardar = true,
                    mostrarGuardar = mostrarGuardar,
                    onDigitalize = onDigitalize,
                    onSave = onSave
                )
            }
        }
    ) { padding ->
        if (apaisado) {
            // Landscape: foto/cabecera a la izquierda, informacion a la derecha.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    Cabecera(resultado, fallida)
                    Spacer(Modifier.height(16.dp))
                }
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Informacion(resultado, fallida)
                    Spacer(Modifier.height(4.dp))
                }
            }
        } else {
            // Portrait: todo apilado.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Cabecera(resultado, fallida)
                Informacion(resultado, fallida)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Preview(name = "Portrait", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewPortrait() {
    MobilekotlinTheme(dynamicColor = false) {
        PlantDetailContent(DatosEjemplo.scanResult, {}, {}, {})
    }
}

@Preview(name = "Sin advertencias", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewSinAdvertencias() {
    MobilekotlinTheme(dynamicColor = false) {
        PlantDetailContent(
            DatosEjemplo.scanResult.copy(
                cuidados = DatosEjemplo.scanResult.cuidados.orEmpty().filter { it.tipo != "TOXICIDAD" }
            ),
            {}, {}, {}
        )
    }
}

@Preview(name = "Fallo", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewFallo() {
    MobilekotlinTheme(dynamicColor = false) {
        PlantDetailContent(DatosEjemplo.scanResultFallo, {}, {}, {})
    }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 851, heightDp = 393)
@Composable
private fun PreviewLandscape() {
    MobilekotlinTheme(dynamicColor = false) {
        PlantDetailContent(DatosEjemplo.scanResult, {}, {}, {})
    }
}
