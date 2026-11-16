package com.plantia.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Coincide con DailyMission del movil. Los tres ultimos campos solo viajan en la mision de un evento
 * activo (la tarjeta roja de Misiones): kind es "EVENT", eventType el codigo del tipo de evento y
 * endsInSeconds lo que falta para que termine. type es el tipo de una mision diaria (RIEGO, ESCANEO, SOL,
 * ABONO, CUIDADO, DIGITALIZAR, COLECCION, REFUGIO): la app lo usa para llevar al usuario a donde se cumple.
 */
public record DailyMissionDto(
        String id,
        String title,
        String reward,
        float progress,
        int currentCount,
        int targetCount,
        boolean isCompleted,
        @JsonInclude(JsonInclude.Include.NON_NULL) String kind,
        @JsonInclude(JsonInclude.Include.NON_NULL) String eventType,
        @JsonInclude(JsonInclude.Include.NON_NULL) Long endsInSeconds,
        @JsonInclude(JsonInclude.Include.NON_NULL) String type
) {
    /** Mision diaria normal. */
    public DailyMissionDto(String id, String title, String reward, float progress,
                           int currentCount, int targetCount, boolean isCompleted) {
        this(id, title, reward, progress, currentCount, targetCount, isCompleted, null, null, null, null);
    }
}
