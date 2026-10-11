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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.verticalScroll
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
            // LANDSCAPE: Row con statusBarsPadding y navigationBarsPadding
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // LADO IZQUIERDO: 60% (weight 1.2f) - Cámara + marco verde centrado
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    CameraPreview(modifier = Modifier.fillMaxSize())
                    ScanOverlay(modifier = Modifier.align(Alignment.Center))

                    // Barra superior con botón volver
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
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

                // LADO DERECHO: 40% (weight 0.8f) - Controles + historial con scroll
                Column(
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.9f))
                        .padding(horizontal = 16.dp, vertical = 24.dp)
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
            // PORTRAIT: Column con camera+overlay usando weight(1f) y panel inferior fijo
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // ÁREA SUPERIOR: Cámara + marco verde (peso 1f = ocupa espacio disponible)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black)
                ) {
                    CameraPreview(modifier = Modifier.fillMaxSize())
                    ScanOverlay(modifier = Modifier.align(Alignment.Center))

                    // Barra superior con botón volver
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

                // ÁREA INFERIOR: Panel de controles e historial (altura fija, no invade cámara)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .background(Color.Black.copy(alpha = 0.9f))
                ) {
                    Text(
                        text = "Escaneos recientes",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    RecentScansRow()
                    Spacer(modifier = Modifier.height(16.dp))
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
}