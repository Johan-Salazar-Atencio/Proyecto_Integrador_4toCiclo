package com.plantia.data.model

/** Rareza de un logro, de menor a mayor. Una clave desconocida cuenta como COMUN. */
enum class Rareza(val etiqueta: String) {
    COMUN("Común"),
    RARA("Rara"),
    EPICA("Épica"),
    LEGENDARIA("Legendaria");

    companion object {
        fun desde(clave: String?): Rareza =
            entries.firstOrNull { it.name.equals(clave?.trim(), ignoreCase = true) } ?: COMUN
    }
}

/** Un logro de la pantalla de Logros, con su avance. */
data class Logro(
    val idLogro: Int,
    val codigo: String,
    val titulo: String,
    val descripcion: String,
    val icono: String,
    val rareza: Rareza,
    val meta: Int,
    val progreso: Int,
    val desbloqueado: Boolean,
    val desbloqueadoEn: String? = null,
    val secreto: Boolean = false,
    val recompensaXp: Int = 0,
    val recompensaMonedas: Int = 0
) {
    /** Avance entre 0 y 1 (1 si ya esta desbloqueado). */
    val fraccion: Float
        get() = when {
            desbloqueado -> 1f
            meta <= 0 -> 0f
            else -> (progreso.toFloat() / meta).coerceIn(0f, 1f)
        }

    /** Secreto y todavia bloqueado: el servidor manda "???" y no hay avance que mostrar. */
    val oculto: Boolean get() = secreto && !desbloqueado
}

data class LogrosResumen(
    val total: Int,
    val desbloqueados: Int,
    val logros: List<Logro>
) {
    val fraccion: Float get() = if (total <= 0) 0f else (desbloqueados.toFloat() / total).coerceIn(0f, 1f)
}

/** Logro recien desbloqueado que falta celebrar. */
data class LogroNuevo(
    val idLogro: Int,
    val codigo: String,
    val titulo: String,
    val descripcion: String,
    val icono: String,
    val rareza: Rareza,
    val recompensaXp: Int = 0,
    val recompensaMonedas: Int = 0
)
