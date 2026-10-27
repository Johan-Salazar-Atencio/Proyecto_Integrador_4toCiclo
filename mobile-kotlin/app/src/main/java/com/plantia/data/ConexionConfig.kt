package com.plantia.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Configuracion de conexion al backend.
 *
 * Guarda un host personalizado (por ejemplo `http://192.168.1.70:8080/`) para
 * los casos en los que el descubrimiento automatico no llega, por ejemplo si
 * el PC y el movil estan en redes distintas o si el mDNS esta bloqueado por el
 * router.
 */
object ConexionConfig {

    private const val PREFS = "plantia_conexion"
    private const val KEY_HOST = "host_personalizado"

    private var prefs: SharedPreferences? = null

    private val _host = MutableStateFlow<String?>(null)

    /** Host configurado a mano, o null si no hay ninguno. */
    val host: StateFlow<String?> = _host

    fun init(context: Context) {
        if (prefs != null) return
        val storage = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = storage
        _host.value = storage.getString(KEY_HOST, null)?.takeIf { it.isNotBlank() }
    }

    /** Guarda (o borra, si es null/vacío) el host personalizado. */
    fun setHost(valor: String?) {
        val limpio = valor?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() }
        prefs?.edit()?.putString(KEY_HOST, limpio ?: "")?.apply()
        _host.value = limpio
    }
}
