package com.plantia.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.util.Log
import android.widget.Toast as AndroidToast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.plantia.ui.theme.PrimarioOscuro
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.plantia.ui.scan.components.CameraPreview
import com.plantia.ui.scan.components.FlashToggleButton
import com.plantia.ui.scan.components.PlantResultCard
import com.plantia.ui.scan.components.ScanControlsBar
import com.plantia.ui.scan.components.ScanOverlay
import com.plantia.ui.scan.components.ScanProgressOverlay
import com.plantia.ui.scan.viewmodel.ScanUiState
import com.plantia.ui.scan.viewmodel.ScanViewModel
import com.plantia.util.ImagenLocal
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanScreen(
    onNavigateToHome: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onScanSuccess: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    val scanViewModel: ScanViewModel = viewModel(factory = ScanViewModel.Factory)

    var lastCapturedFile by remember { mutableStateOf<File?>(null) }
    var showToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }
    var captureTrigger by remember { mutableStateOf(0) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    // Lente de la camara y flash (linterna)
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var torchEnabled by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            Log.d("ScanScreen", "Permiso de cámara: ${if (isGranted) "CONCEDIDO" else "DENEGADO"}")
        }
    )

    // Galería: elegir una foto existente y escanearla.
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val preparada = ImagenLocal.desdeUri(context, uri)
        if (preparada == null) {
            Log.w("ScanScreen", "No se pudo leer la imagen de la galeria")
            toastMessage = "No se pudo abrir la imagen"
            showToast = true
            return@rememberLauncherForActivityResult
        }
        Log.d("ScanScreen", "Imagen de galeria: ${preparada.archivo.absolutePath}")
        lastCapturedFile = preparada.archivo
        scanViewModel.escanear(preparada.archivo)
    }

    // Verificar permiso al iniciar
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = granted
        if (!granted) {
            Log.d("ScanScreen", "Solicitando permiso de cámara desde ScanScreen")
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val onImageCaptured = { file: File ->
        lastCapturedFile = file
        Log.d("ScanScreen", "Callback onImageCaptured: ${file.absolutePath}")
        // Enviar la foto al backend para identificar la planta
        scanViewModel.escanear(file)
        Unit
    }

    // Si la camara falla, se corta la espera: sin esto la UI queda en
    // "Analizando..." indefinidamente.
    val onCameraError = { mensaje: String ->
        Log.w("ScanScreen", "Error de camara: $mensaje")
        scanViewModel.fallar(mensaje)
        toastMessage = mensaje
        showToast = true
    }

    val onCaptureClick = onCaptureClick@ {
        Log.d("ScanScreen", "Botón de captura presionado")
        if (!hasCameraPermission) {
            Log.w("ScanScreen", "Intento de captura sin permiso de cámara")
            toastMessage = "Permiso de cámara requerido"
            showToast = true
            return@onCaptureClick
        }
        captureTrigger++
    }

    // Cambia entre camara trasera y frontal.
    val onFlipCamera = {
        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        // La camara frontal no tiene linterna.
        torchEnabled = false
        Log.d("ScanScreen", "Lente cambiada a $lensFacing")
        Unit
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camara a pantalla completa; todo lo demas flota encima (consejos, marco, texto y controles)
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onImageCaptured = onImageCaptured,
            onError = onCameraError,
            lensFacing = lensFacing,
            torchEnabled = torchEnabled,
            captureTrigger = captureTrigger
        )

        // Barra superior: volver, historial y girar la camara; debajo, los tres consejos
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BotonRedondoCamara(onClick = onNavigateToHome, descripcion = "Volver") {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                BotonRedondoCamara(onClick = onNavigateToHistory, descripcion = "Historial de escaneos") {
                    Icon(Icons.Filled.History, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(8.dp))
                BotonRedondoCamara(onClick = onFlipCamera, descripcion = "Girar cámara") {
                    Icon(Icons.Filled.Cameraswitch, contentDescription = null, tint = Color.White)
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ConsejoCamara(Icons.Filled.WbSunny, "Buena luz")
                ConsejoCamara(Icons.Filled.Eco, "Hojas a la vista")
                ConsejoCamara(Icons.Filled.CenterFocusStrong, "Una sola planta")
            }
        }

        // Marco y texto, centrados un poco por encima del medio como en la referencia
        Column(
            modifier = Modifier.align(Alignment.Center).offset(y = if (isLandscape) 0.dp else (-8).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScanOverlay(lado = if (isLandscape) 150.dp else 224.dp)
            Spacer(Modifier.height(if (isLandscape) 6.dp else 14.dp))
            Text("Toma o elige una foto", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text("Se identifica con IA", color = Color.White.copy(alpha = 0.75f), fontSize = 11.sp)
        }

        // Controles: galeria, obturador y flash
        ScanControlsBar(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (isLandscape) 12.dp else 32.dp),
            onGalleryClick = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onCaptureClick = onCaptureClick,
            derecha = {
                // La camara frontal no tiene linterna
                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    FlashToggleButton(torchEnabled = torchEnabled, onToggle = { torchEnabled = !torchEnabled })
                }
            }
        )

        // Resultado del escaneo (identificación + cuidados) sobre la cámara
        val scanState = scanViewModel.uiState

        // Con respuesta del servidor (incluso 'Planta desconocida'): se muestra el check de exito
        // un instante y luego se abre el detalle.
        LaunchedEffect(scanState) {
            if (scanState is ScanUiState.Success) {
                kotlinx.coroutines.delay(900)
                scanViewModel.reiniciar()
                onScanSuccess()
            }
        }

        val enProgreso = scanState is ScanUiState.Loading || scanState is ScanUiState.Success
        if (enProgreso) {
            ScanProgressOverlay(
                foto = lastCapturedFile,
                terminado = scanState is ScanUiState.Success,
                onCancelar = { scanViewModel.cancelar() }
            )
        } else if (scanState != ScanUiState.Idle) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
                PlantResultCard(
                    state = scanState,
                    foto = lastCapturedFile,
                    onCerrar = { scanViewModel.reiniciar() }
                )
            }
        }

        // Toast para mostrar confirmación de captura o errores
        if (showToast) {
            LaunchedEffect(showToast) {
                AndroidToast.makeText(context, toastMessage, AndroidToast.LENGTH_SHORT).show()
                delay(2000)
                showToast = false
            }
        }
    }
}
/** Consejo sobre la camara: pildora oscura translucida con icono lima. */
@Composable
private fun ConsejoCamara(icono: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icono, contentDescription = null, tint = PrimarioOscuro, modifier = Modifier.size(14.dp))
        Text(texto, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Boton redondo translucido de la barra superior de la camara. */
@Composable
private fun BotonRedondoCamara(onClick: () -> Unit, descripcion: String, icono: @Composable () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            .semantics { contentDescription = descripcion }
    ) { icono() }
}
