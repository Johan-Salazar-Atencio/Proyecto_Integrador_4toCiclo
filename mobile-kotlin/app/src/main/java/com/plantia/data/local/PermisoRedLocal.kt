package com.plantia.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Android 17 pide permiso de "red local" para hablar con el PC que tiene el backend. Si se niega, todas las peticiones
 * fallan por tiempo agotado y la app solo decia "No se pudo conectar con el servidor" (QA-62). MainActivity actualiza
 * este estado al pedir el permiso y al volver a la app; AppNavigation muestra un aviso con acceso a Ajustes.
 *
 * QA-59: antes de pedirlo por primera vez se explica para que sirve, con una tarjeta propia ([explicar]).
 */
object PermisoRedLocal {
    const val PERMISO = "android.permission.ACCESS_LOCAL_NETWORK"
    const val SDK_CON_PERMISO = 37

    private const val PREFS = "plantia_permisos"
    private const val KEY_EXPLICADO = "red_local_explicada"

    /** Que hacer con el permiso al abrir la app. */
    enum class PasoAlIniciar { NINGUNO, EXPLICAR, PEDIR }

    private val _negado = MutableStateFlow(false)
    val negado: StateFlow<Boolean> = _negado

    private val _explicar = MutableStateFlow(false)
    /** true mientras se muestra la tarjeta que explica el permiso antes del dialogo del sistema. */
    val explicar: StateFlow<Boolean> = _explicar

    fun actualizar(sdk: Int, concedido: Boolean) {
        _negado.value = sdk >= SDK_CON_PERMISO && !concedido
    }

    /**
     * En versiones sin este permiso, o si ya esta concedido, no hace falta nada. La primera vez se explica antes de
     * pedirlo; las siguientes (si se nego una vez) se pide directamente y, si se vuelve a negar, queda el aviso fijo.
     */
    fun pasoAlIniciar(sdk: Int, concedido: Boolean, yaExplicado: Boolean): PasoAlIniciar = when {
        sdk < SDK_CON_PERMISO || concedido -> PasoAlIniciar.NINGUNO
        !yaExplicado -> PasoAlIniciar.EXPLICAR
        else -> PasoAlIniciar.PEDIR
    }

    /** El descubrimiento mDNS solo arranca con el permiso dado (o en versiones donde no existe). */
    fun puedeBuscarEnRed(sdk: Int, concedido: Boolean): Boolean = sdk < SDK_CON_PERMISO || concedido

    fun mostrarExplicacion(mostrar: Boolean) {
        _explicar.value = mostrar
    }

    fun yaExplicado(contexto: Context): Boolean =
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_EXPLICADO, false)

    fun marcarExplicado(contexto: Context) {
        contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_EXPLICADO, true).apply()
    }
}
