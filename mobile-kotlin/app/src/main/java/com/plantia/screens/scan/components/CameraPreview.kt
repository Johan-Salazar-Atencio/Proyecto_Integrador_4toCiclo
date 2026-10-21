package com.plantia.screens.scan.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onImageCaptured: ((File) -> Unit)? = null,
    onError: ((String) -> Unit)? = null,
    captureTrigger: Int = 0,
    lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    torchEnabled: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val configuration = LocalConfiguration.current

    // Se usa rememberUpdatedState para que los callbacks capturados dentro de
    // `takePicture` (creado con remember) siempre sean los mas recientes.
    val onErrorState = rememberUpdatedState(onError)

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
        }
    )

    val imageCapture = remember { mutableStateOf<ImageCapture?>(null) }
    val previewViewRef = remember { mutableStateOf<PreviewView?>(null) }
    val cameraProviderRef = remember { mutableStateOf<ProcessCameraProvider?>(null) }
    val cameraRef = remember { mutableStateOf<Camera?>(null) }
    val coroutineScope = remember { CoroutineScope(Dispatchers.IO) }

    // Estado para animación de flash de obturador
    var shutterFlashAlpha by remember { mutableStateOf(0f) }
    val triggerFlash = { shutterFlashAlpha = 1f }

    // Obtener la rotación actual de la pantalla
    val displayRotation = remember { mutableStateOf(getDisplayRotation(context)) }

    // Actualizar rotación cuando cambia la configuración (orientación)
    LaunchedEffect(configuration.orientation) {
        displayRotation.value = getDisplayRotation(context)
        Log.d("CameraPreview", "Rotación de pantalla actualizada: ${displayRotation.value}")
    }

    // Animación del flash: 0 -> 0.7 -> 0 en ~150ms
    val animatedFlashAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = shutterFlashAlpha,
        animationSpec = tween(75, delayMillis = 0, easing = FastOutSlowInEasing)
    )

    // Reset flash alpha después de la animación
    LaunchedEffect(animatedFlashAlpha) {
        if (animatedFlashAlpha > 0.6f) {
            shutterFlashAlpha = 0f
        }
    }

    // Función para corregir orientación EXIF del archivo guardado
    fun fixExifOrientation(photoFile: File) {
        try {
            val exif = ExifInterface(photoFile.absolutePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            var degrees = 0
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> degrees = 90
                ExifInterface.ORIENTATION_ROTATE_180 -> degrees = 180
                ExifInterface.ORIENTATION_ROTATE_270 -> degrees = 270
                else -> degrees = 0
            }

            if (degrees != 0) {
                Log.d("CameraPreview", "Corrigiendo orientación EXIF: $degrees grados")
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                if (bitmap != null) {
                    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

                    // Sobrescribir el archivo con la imagen rotada
                    FileOutputStream(photoFile).use { outputStream ->
                        rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
                    }

                    // Actualizar EXIF a orientación normal
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                    exif.saveAttributes()

                    bitmap.recycle()
                    rotatedBitmap.recycle()
                    Log.d("CameraPreview", "Archivo corregido y sobrescrito: ${photoFile.absolutePath}")
                }
            }
        } catch (e: Exception) {
            Log.e("CameraPreview", "Error corrigiendo EXIF: ${e.message}", e)
        }
    }

    // Función para tomar foto - expuesta para ser llamada desde LaunchedEffect
    val takePicture = remember {
        { callback: ((File) -> Unit)? ->
            // Disparar animación de flash inmediatamente
            triggerFlash()
            Log.d("CameraPreview", "Iniciando toma de foto real")
            imageCapture.value?.let { capture ->
                // Actualizar targetRotation dinámicamente antes de capturar
                val currentRotation = previewViewRef.value?.display?.rotation ?: getDisplayRotation(context)
                capture.targetRotation = currentRotation
                Log.d("CameraPreview", "targetRotation actualizado dinámicamente a: $currentRotation")

                coroutineScope.launch {
                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                    val fileName = "plantia_scan_$timestamp.jpg"
                    val outputFile = File(context.cacheDir, fileName)

                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

                    capture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exc: ImageCaptureException) {
                                Log.e("CameraPreview", "Error al guardar foto: ${exc.message}", exc)
                                // Sin esto la UI queda en "Analizando..." para siempre.
                                onErrorState.value?.invoke(
                                    "No se pudo tomar la foto: ${exc.message ?: "error de camara"}"
                                )
                            }

                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                Log.d("CameraPreview", "Foto guardada exitosamente: ${outputFile.absolutePath}")
                                // Corregir orientación EXIF antes de notificar
                                fixExifOrientation(outputFile)
                                callback?.invoke(outputFile)
                            }
                        }
                    )
                }
            } ?: run {
                Log.e("CameraPreview", "ImageCapture es null, no se puede tomar foto")
                onErrorState.value?.invoke("La camara aun no esta lista")
            }
        }
    }

    // Observar captureTrigger y disparar captura cuando cambie
    LaunchedEffect(captureTrigger) {
        if (captureTrigger > 0) {
            takePicture(onImageCaptured)
        }
    }

    // Solicitar permiso si no se tiene
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            Log.d("CameraPreview", "Solicitando permiso de cámara")
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT
                        )
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        previewViewRef.value = this

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                cameraProviderRef.value = cameraProviderFuture.get()
                                Log.d("CameraPreview", "ProcessCameraProvider listo")
                            } catch (e: Exception) {
                                Log.e("CameraPreview", "Error al obtener el proveedor de camara: ${e.message}", e)
                                onErrorState.value?.invoke(
                                    "No se pudo iniciar la camara: ${e.message ?: "error desconocido"}"
                                )
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Vincular la camara. Se reejecuta al cambiar de lente (frontal/trasera)
            // o cuando el proveedor aun no estaba listo.
            LaunchedEffect(lensFacing, cameraProviderRef.value) {
                val provider = cameraProviderRef.value ?: return@LaunchedEffect
                val previewView = previewViewRef.value ?: return@LaunchedEffect
                try {
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(displayRotation.value)
                        .build()
                    imageCapture.value = capture

                    val selector = CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build()

                    provider.unbindAll()
                    val camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, capture)
                    cameraRef.value = camera
                    camera.cameraControl.enableTorch(torchEnabled)
                    Log.d("CameraPreview", "Cámara vinculada (lensFacing=$lensFacing)")
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Error al vincular cámara: ${e.message}", e)
                    onErrorState.value?.invoke(
                        "No se pudo abrir la camara: ${e.message ?: "error desconocido"}"
                    )
                }
            }

            // Flash (linterna)
            LaunchedEffect(torchEnabled) {
                cameraRef.value?.cameraControl?.enableTorch(torchEnabled)
            }

            // Overlay de flash de obturador
            if (animatedFlashAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = animatedFlashAlpha * 0.7f))
                )
            }
        }
    } else {
        FallbackCameraPreview(modifier = modifier)
    }
}

private fun getDisplayRotation(context: Context): Int {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return windowManager.defaultDisplay.rotation
}

@Composable
fun FallbackCameraPreview(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Permiso de cámara requerido",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
