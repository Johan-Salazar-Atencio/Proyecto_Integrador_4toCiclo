package com.plantia.screens.scan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.res.Configuration
import com.plantia.screens.scan.components.CameraPreview
import com.plantia.screens.scan.components.RecentScansRow
import com.plantia.screens.scan.components.ScanControlsBar
import com.plantia.screens.scan.components.ScanOverlay

@Composable
fun ScanScreen(
    onNavigateToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLandscape) {
            // HORIZONTAL: Row con cámara a la izquierda, controles a la derecha
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // LADO IZQUIERDO: Vista previa de cámara (flexible, ocupa espacio disponible)
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.7f)
                        .background(Color.Black)
                ) {
                    CameraPreview(modifier = Modifier.fillMaxSize())
                    ScanOverlay(modifier = Modifier.align(Alignment.Center))
                    
                    // Barra superior
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onNavigateToHome() },
                            modifier = Modifier.background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = CircleShape
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Escanear Planta",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }

                // LADO DERECHO: Panel de controles e historial
                Column(
                    modifier = Modifier
                        .fillMaxSize(0.3f)
                        .background(Color.Black.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Escaneos recientes",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RecentScansRow()
                    Spacer(modifier = Modifier.height(24.dp))
                    ScanControlsBar(
                        onGalleryClick = { },
                        onCaptureClick = { },
                        onFlashClick = { },
                        onFlipCameraClick = { }
                    )
                }
            }
        } else {
            // VERTICAL: Diseño original (cámara arriba, controles abajo)
            // CAPA 1: Cámara de Fondo
            CameraPreview(modifier = Modifier.fillMaxSize())

            // CAPA 2: Marco Guía de Escaneo Centrado
            ScanOverlay(
                modifier = Modifier.align(Alignment.Center)
            )

            // CAPA 3: Barra Superior con Botón Atrás
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onNavigateToHome() },
                    modifier = Modifier.background(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = CircleShape
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Escanear Planta",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }

            // CAPA 4: Menú Inferior Fijo (Escaneos Recientes + Botones)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                RecentScansRow()
                ScanControlsBar(
                    onGalleryClick = { },
                    onCaptureClick = { },
                    onFlashClick = { },
                    onFlipCameraClick = { }
                )
            }
        }
    }
}
