package com.plantia.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Sesion de la app.
 *
 * El backend todavia no tiene login: el usuario se identifica con el header
 * `X-User-Id`. Aqui se guarda ese id de forma persistente para que no viva
 * hardcodeado en cada pantalla, y para que mas adelante se pueda cambiar por
 * un token real sin tocar las llamadas.
 */
object Sesion {

    private const val PREFS = "plantia_sesion"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_TOKEN = "session_token"
    const val USUARIO_POR_DEFECTO = 1

    private var prefs: SharedPreferences? = null

    private val _userId = MutableStateFlow(USUARIO_POR_DEFECTO)
    private val _token = MutableStateFlow("")

    /** Id del usuario actual. Cambiarlo recarga las pantallassuscritas. */
    val userId: StateFlow<Int> = _userId

    /** Token de sesion del usuario actual (se envia en X-Session-Token). */
    val token: StateFlow<String> = _token

    /** Se inicializa una vez desde el contexto de la aplicacion. */
    fun init(context: Context) {
        if (prefs != null) return
        val storage = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = storage
        _userId.value = storage.getInt(KEY_USER_ID, USUARIO_POR_DEFECTO)
        // Sin token guardado no hay sesion: el backend responde 401 hasta que se inicie sesion
        _token.value = storage.getString(KEY_TOKEN, "") ?: ""
    }

    fun setUserId(id: Int) {
        if (id <= 0) return
        prefs?.edit()?.putInt(KEY_USER_ID, id)?.apply()
        _userId.value = id
    }

    fun setToken(token: String?) {
        val t = token ?: ""
        prefs?.edit()?.putString(KEY_TOKEN, t)?.apply()
        _token.value = t
    }

    fun setSesion(id: Int, token: String?) {
        if (id <= 0) return
        prefs?.edit()?.putInt(KEY_USER_ID, id)?.putString(KEY_TOKEN, token ?: "")?.apply()
        _userId.value = id
        _token.value = token ?: ""
    }
}
