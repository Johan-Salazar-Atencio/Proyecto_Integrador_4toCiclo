package com.plantia.data

import com.plantia.model.DailyMission
import com.plantia.model.PlantSummary
import com.plantia.model.QuickAccessItem
import com.plantia.model.UserSummary
import com.plantia.model.WeatherAlert

/**
 * Lo que muestra la pantalla de inicio. Cada dato es nulo si el servidor no lo tiene (204)
 * o si esa peticion fallo; `fallidas` cuenta las peticiones que no respondieron bien.
 */
data class HomeDatos(
    val usuario: UserSummary?,
    val alerta: WeatherAlert?,
    val planta: PlantSummary?,
    val accesos: List<QuickAccessItem>,
    val mision: DailyMission?,
    val fallidas: Int,
    val sinConexion: Boolean
)

interface HomeRepository {
    /** Pide todas las secciones de Home; una que falle no tumba al resto. */
    suspend fun cargar(): HomeDatos
}

/** Datos del usuario para la pantalla de perfil. */
interface UsuarioRepository {
    /** El perfil, o null si el servidor no encuentra al usuario (404). Lanza si no hay conexion. */
    suspend fun perfil(): UserSummary?
}
