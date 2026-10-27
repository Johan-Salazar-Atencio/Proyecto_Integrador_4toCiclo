package com.plantia.dto;

/** Coincide con DailyMission del movil. */
public record DailyMissionDto(
        String id,
        String title,
        String reward,
        float progress,
        int currentCount,
        int targetCount,
        boolean isCompleted
) {
}
