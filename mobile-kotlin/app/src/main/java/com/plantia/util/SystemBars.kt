package com.plantia.util

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.plantia.ui.theme.LocalTemaOscuro

/**
 * Para pantallas de fondo claro: iconos oscuros en la barra de estado mientras la pantalla
 * esta visible. Con el tema oscuro elegido se dejan claros. Se restaura al salir.
 */
@Composable
fun IconosOscurosEnBarraDeEstado() {
    val vista = LocalView.current
    val oscuro = LocalTemaOscuro.current
    if (vista.isInEditMode) return
    DisposableEffect(oscuro) {
        val ventana = (vista.context as? Activity)?.window
        val control = ventana?.let { WindowCompat.getInsetsController(it, vista) }
        val anterior = control?.isAppearanceLightStatusBars
        control?.isAppearanceLightStatusBars = !oscuro
        onDispose { anterior?.let { control.isAppearanceLightStatusBars = it } }
    }
}
