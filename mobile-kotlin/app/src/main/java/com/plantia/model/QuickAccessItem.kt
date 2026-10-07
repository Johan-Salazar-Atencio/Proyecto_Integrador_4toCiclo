package com.plantia.model

import androidx.compose.ui.graphics.Color

data class QuickAccessItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: String,
    val backgroundColor: Color,
    val iconColor: Color
)
