package com.plantia.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Segundos que quedan, contando hacia atras cada segundo a partir del valor que mando el servidor (null = sin cuenta). */
@Composable
fun rememberCuentaRegresiva(inicial: Long?): Long? {
    var restante by remember(inicial) { mutableStateOf(inicial) }
    LaunchedEffect(inicial) {
        while ((restante ?: 0L) > 0L) {
            delay(1_000)
            restante = restante?.minus(1)
        }
    }
    return restante
}
