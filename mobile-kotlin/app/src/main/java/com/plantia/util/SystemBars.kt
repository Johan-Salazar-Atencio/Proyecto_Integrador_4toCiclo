package com.plantia.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Para pantallas de fondo claro: iconos oscuros en la barra de estado mientras la pantalla
 * esta visible (en tema oscuro del sistema serian blancos sobre blanco). Se restaura al salir.
 */
@Composable
fun IconosOscurosEnBarraDeEstado() {
    val vista = LocalView.current
    if (vista.isInEditMode) return
    DisposableEffect(Unit) {
        val ventana = (vista.context as? Activity)?.window
        val control = ventana?.let { WindowCompat.getInsetsController(it, vista) }
        val anterior = control?.isAppearanceLightStatusBars
        control?.isAppearanceLightStatusBars = true
        onDispose { anterior?.let { control.isAppearanceLightStatusBars = it } }
    }
}
