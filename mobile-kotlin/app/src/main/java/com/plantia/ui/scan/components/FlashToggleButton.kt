package com.plantia.ui.scan.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.plantia.ui.theme.PrimarioOscuro

/**
 * Boton de flash (linterna) de la barra de la camara.
 * Se muestra solo en la camara trasera: la frontal no tiene linterna.
 */
@Composable
fun FlashToggleButton(
    torchEnabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    BotonCuadrado(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
            contentDescription = if (torchEnabled) "Apagar flash" else "Encender flash",
            tint = if (torchEnabled) PrimarioOscuro else Color.White,
            modifier = Modifier.size(22.dp)
        )
    }
}
