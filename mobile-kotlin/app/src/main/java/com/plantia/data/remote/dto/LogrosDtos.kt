package com.plantia.data.remote.dto

/** GET /api/v1/logros: todos los logros del usuario con su avance. */
data class LogrosRespuestaDto(
    val total: Int = 0,
    val desbloqueados: Int = 0,
    val logros: List<LogroDto> = emptyList()
)

data class LogroDto(
    val idLogro: Int = 0,
    val codigo: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    /** Clave de texto del icono (semilla, hoja, huayco...); el admin puede crear claves nuevas. */
    val icono: String = "",
    /** COMUN, RARA, EPICA o LEGENDARIA. */
    val rareza: String = "COMUN",
    val meta: Int = 0,
    val progreso: Int = 0,
    val desbloqueado: Boolean = false,
    /** Fecha ISO ("2026-11-20T10:15:30"); null mientras esta bloqueado. */
    val desbloqueadoEn: String? = null,
    val secreto: Boolean = false,
    val recompensaXp: Int = 0,
    val recompensaMonedas: Int = 0
)

/**
 * Logro recien desbloqueado: lo devuelven GET /api/v1/logros/pendientes y el campo `logrosNuevos`
 * del progreso de escaneo, digitalizar y cuidados.
 */
data class LogroNuevoDto(
    val idLogro: Int = 0,
    val codigo: String = "",
    val titulo: String = "",
    val descripcion: String = "",
    val icono: String = "",
    val rareza: String = "COMUN",
    val recompensaXp: Int = 0,
    val recompensaMonedas: Int = 0
)

/** POST /api/v1/logros/vistos (responde 204). */
data class LogrosVistosRequest(val ids: List<Int>)

/**
 * Lo que ganó el usuario con una accion (campo `progreso` de escaneo, digitalizar y cuidados). Solo se lee
 * `logrosNuevos`; el servidor viejo no lo manda, por eso es opcional.
 */
data class ProgresoDto(
    val xpGanado: Int = 0,
    val monedasGanadas: Int = 0,
    val nivel: Int = 0,
    val subioDeNivel: Boolean = false,
    val experienciaXp: Int = 0,
    val rachaDias: Int = 0,
    val logrosNuevos: List<LogroNuevoDto>? = null
)
