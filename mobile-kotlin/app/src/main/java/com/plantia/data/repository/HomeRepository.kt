package com.plantia.data.repository

import com.plantia.data.model.DailyMission
import com.plantia.data.model.PlantSummary
import com.plantia.data.model.QuickAccessItem
import com.plantia.data.model.Region
import com.plantia.data.model.UserSummary
import com.plantia.data.model.WeatherAlert

/**
 * Lo que muestra la pantalla de inicio. Cada dato es nulo si el servidor no lo tiene (204)
 * o si esa peticion fallo; `fallidas` cuenta las peticiones que no respondieron bien.
 */
data class HomeDatos(
    val usuario: UserSummary?,
    val alerta: WeatherAlert?,
    val planta: PlantSummary?,
    val accesos: List<QuickAccessItem>,
    val misiones: List<DailyMission>,
    val fallidas: Int,
    val sinConexion: Boolean
)

/** Resultado de consultar solo la alerta: "no hay desastre" y "no se pudo consultar" son cosas distintas. */
sealed interface AlertaResultado {
    data class Con(val alerta: WeatherAlert) : AlertaResultado
    /** El servidor respondio 204: no hay desastre activo. */
    data object Sin : AlertaResultado
    data object Fallo : AlertaResultado
}

interface HomeRepository {
    /** Pide todas las secciones de Home; una que falle no tumba al resto. */
    suspend fun cargar(): HomeDatos

    /** Solo la alerta de desastre, para refrescarla mientras el Home esta abierto. */
    suspend fun alerta(): AlertaResultado

    /** Solo el perfil (XP, nivel, hojas), o null si no se pudo consultar. */
    suspend fun usuario(): UserSummary?

    /** Solo las misiones de hoy, o null si no se pudieron consultar (asi no se borran las que ya se ven). */
    suspend fun misiones(): List<DailyMission>?
}

/** Datos del usuario para la pantalla de perfil. */
interface UsuarioRepository {
    /** El perfil, o null si el servidor no encuentra al usuario (404). Lanza si no hay conexion. */
    suspend fun perfil(): UserSummary?

    /** Regiones que se pueden elegir, o null si no se pudieron leer. */
    suspend fun regiones(): List<Region>?

    /** Cambia la region y devuelve el perfil actualizado, o null si el servidor la rechazo. */
    suspend fun cambiarRegion(codigo: String): UserSummary?

    /** Elige el invernadero de la coleccion (el predeterminado o uno comprado); null si el servidor lo rechazo. */
    suspend fun cambiarInvernadero(idItem: Int): UserSummary?

    /** Sube la foto de perfil y devuelve el perfil con la nueva URL; null si no se pudo. */
    suspend fun cambiarFoto(foto: java.io.File): UserSummary?
}
