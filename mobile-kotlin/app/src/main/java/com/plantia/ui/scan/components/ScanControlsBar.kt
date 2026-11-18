package com.plantia.ui.scan.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.plantia.ui.theme.PrimarioOscuro

/**
 * Controles de la camara: galeria a la izquierda, obturador grande con centro lima al medio y, a la derecha,
 * el hueco del flash (lo pone quien llama: la camara frontal no tiene linterna). Van sobre la imagen de la camara,
 * por eso usan blanco y negro translucido en claro y en oscuro, como en la referencia.
 */
@Composable
fun ScanControlsBar(
    onGalleryClick: () -> Unit = {},
    onCaptureClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    derecha: @Composable () -> Unit = {}
) {
    // Estado para animación de escala del botón de captura
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "obturador"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Galería
        BotonCuadrado(onClick = onGalleryClick) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = "Galería", tint = Color.White, modifier = Modifier.size(22.dp))
        }

        // 2. Obturador con animación de escala
        Box(
            modifier = Modifier
                .size(70.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .border(5.dp, Color.White, CircleShape)
                .padding(5.dp)
                .semantics { contentDescription = "Capturar" }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onCaptureClick() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(Modifier.size(50.dp).clip(CircleShape).background(PrimarioOscuro))
        }

        // 3. Flash (u otro boton): mismo tamaño que la galeria para que el obturador quede centrado
        Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { derecha() }
    }
}

/** Boton cuadrado redondeado y translucido de la camara (galeria y flash). */
@Composable
fun BotonCuadrado(onClick: () -> Unit, modifier: Modifier = Modifier, contenido: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .border(2.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { contenido() }
}
