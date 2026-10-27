package com.plantia.model

data class UserSummary(
    val name: String,
    val level: Int,
    val badge: String,
    val leaves: Int,
    val experience: Int,
    val experienceToNextLevel: Int,
    val profileImageUrl: String? = null
)
