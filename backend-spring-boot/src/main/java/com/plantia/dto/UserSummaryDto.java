package com.plantia.dto;

/** Coincide con UserSummary del movil. */
public record UserSummaryDto(
        String name,
        int level,
        String badge,
        int leaves,
        int experience,
        int experienceToNextLevel,
        String profileImageUrl
) {
}
