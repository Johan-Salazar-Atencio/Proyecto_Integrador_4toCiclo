package com.plantia.screens.garden

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plantia.data.MockPlantData
import com.plantia.data.PetStatus
import com.plantia.ui.theme.AmberAlert
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.BlueInfo
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.MobilekotlinTheme

@Composable
fun VirtualGardenScreen(onBack: () -> Unit = {}) {
    VirtualGardenContent(status = MockPlantData.petStatus, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VirtualGardenContent(status: PetStatus, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jardin virtual") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(status.nickname)
            StatBar("Salud", status.healthPercent, GreenPrimary)
            StatBar("Agua", status.waterPercent, BlueInfo)
            StatBar("Luz", status.lightPercent, AmberAlert)
        }
    }
}

@Composable
private fun StatBar(label: String, percent: Int, color: Color) {
    Text("$label: $percent%", modifier = Modifier.padding(top = 12.dp))
    LinearProgressIndicator(
        progress = { percent / 100f },
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun VirtualGardenPreview() {
    MobilekotlinTheme(dynamicColor = false) {
        VirtualGardenContent(status = MockPlantData.petStatus, onBack = {})
    }
}
