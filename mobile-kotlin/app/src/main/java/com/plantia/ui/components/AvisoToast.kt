package com.plantia.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/** Muestra [aviso] como mensaje breve y avisa con [alMostrar] para que el ViewModel lo limpie. */
@Composable
fun AvisoToast(aviso: String?, alMostrar: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(aviso) {
        if (aviso != null) {
            Toast.makeText(context, aviso, Toast.LENGTH_SHORT).show()
            alMostrar()
        }
    }
}
