package com.plantia.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Tema que el usuario elige en el Perfil. SISTEMA sigue el modo del telefono. */
enum class ModoTema(val etiqueta: String) {
    SISTEMA("Sistema"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}

/** Guarda el tema elegido para que se conserve al cerrar la app. */
object TemaConfig {

    private const val PREFS = "plantia_tema"
    private const val KEY_MODO = "modo"

    private var prefs: SharedPreferences? = null

    private val _modo = MutableStateFlow(ModoTema.SISTEMA)
    val modo: StateFlow<ModoTema> = _modo

    fun init(context: Context) {
        if (prefs != null) return
        val storage = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = storage
        _modo.value = runCatching { ModoTema.valueOf(storage.getString(KEY_MODO, null).orEmpty()) }
            .getOrDefault(ModoTema.SISTEMA)
    }

    fun setModo(nuevo: ModoTema) {
        prefs?.edit()?.putString(KEY_MODO, nuevo.name)?.apply()
        _modo.value = nuevo
    }
}
