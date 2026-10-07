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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.screens.scan.components.CameraPreview
import com.plantia.screens.scan.components.RecentScansRow
import com.plantia.screens.scan.components.ScanControlsBar
import com.plantia.screens.scan.components.ScanOverlay

@Composable
fun ScanScreen(
    onNavigateToHome: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
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
            modifier = Modifier.align(Alignment.BottomCenter)
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
