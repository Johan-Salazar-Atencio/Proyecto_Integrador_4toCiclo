package com.plantia.model

data class DailyMission(
    val id: String,
    val title: String,
    val reward: String,
    val progress: Float,
    val currentCount: Int,
    val targetCount: Int,
    val isCompleted: Boolean
)
