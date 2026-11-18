package com.plantia.ui.home.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.plantia.data.model.DailyMission

/** Una mision diaria como fila del nuevo diseno (la usa la pantalla de Misiones). */
@Composable
fun DailyMissionCard(
    mission: DailyMission,
    modifier: Modifier = Modifier,
    /** Si se da, la fila se puede tocar (lleva a donde se cumple la mision). */
    onClick: (() -> Unit)? = null
) {
    FilaMision(mission, modifier = modifier.padding(horizontal = 12.dp), onClick = onClick)
}
