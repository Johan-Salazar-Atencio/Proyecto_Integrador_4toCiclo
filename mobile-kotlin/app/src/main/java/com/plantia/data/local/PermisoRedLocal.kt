package com.plantia.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Android 17 pide permiso de "red local" para hablar con el PC que tiene el backend. Si se niega, todas las peticiones
 * fallan por tiempo agotado y la app solo decia "No se pudo conectar con el servidor" (QA-62). MainActivity actualiza
 * este estado al pedir el permiso y al volver a la app; AppNavigation muestra un aviso con acceso a Ajustes.
 */
object PermisoRedLocal {
    const val PERMISO = "android.permission.ACCESS_LOCAL_NETWORK"
    const val SDK_CON_PERMISO = 37

    private val _negado = MutableStateFlow(false)
    val negado: StateFlow<Boolean> = _negado

    fun actualizar(sdk: Int, concedido: Boolean) {
        _negado.value = sdk >= SDK_CON_PERMISO && !concedido
    }
}
