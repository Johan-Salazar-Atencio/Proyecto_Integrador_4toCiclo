package com.plantia.dto;

/**
 * Coincide con UserSummary del movil. Los XP son totales acumulados: la barra del nivel actual
 * va de experienceFromLevel a experienceToNextLevel; en el ultimo nivel ambos limites se igualan al XP.
 */
public record UserSummaryDto(
        String name,
        int level,
        String badge,
        int leaves,
        int experience,
        int experienceToNextLevel,
        String profileImageUrl,
        String email,
        int streakDays,
        int experienceFromLevel
) {
}
