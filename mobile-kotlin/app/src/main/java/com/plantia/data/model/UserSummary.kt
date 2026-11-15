package com.plantia.data.model

data class UserSummary(
    val name: String,
    val level: Int,
    val badge: String,
    val leaves: Int,
    /** XP total acumulado. */
    val experience: Int,
    /** XP total que marca el inicio del siguiente nivel. */
    val experienceToNextLevel: Int,
    val profileImageUrl: String? = null,
    /** Dias seguidos con actividad. */
    val streakDays: Int = 0,
    /** XP total que marca el inicio del nivel actual (0 si el servidor no lo envia). */
    val experienceFromLevel: Int = 0
) {
    /** En el ultimo nivel no hay siguiente: el servidor iguala el limite al XP. */
    val esNivelMaximo: Boolean
        get() = experienceToNextLevel > 0 && experienceToNextLevel <= experience

    /** Cuanto del nivel actual lleva recorrido, de 0 a 1; completo en el ultimo nivel. */
    val progresoNivel: Float
        get() {
            val tramo = experienceToNextLevel - experienceFromLevel
            if (tramo <= 0) return 1f
            return ((experience - experienceFromLevel).toFloat() / tramo).coerceIn(0f, 1f)
        }
}
