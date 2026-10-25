package com.plantia.screens.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.plantia.data.MockPlantData
import com.plantia.data.PlantDetail
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.MobilekotlinTheme

@Composable
fun PlantDetailScreen(
    plantId: String?,
    onBack: () -> Unit = {}
) {
    // Fase 1: datos simulados. Mas adelante vendran de un ViewModel/API.
    PlantDetailContent(detail = MockPlantData.detailById(plantId), onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantDetailContent(detail: PlantDetail, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail.commonName) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(detail.species)
            Text("Confianza IA: ${(detail.confidence * 100).toInt()}%")
            Spacer(Modifier.height(12.dp))
            Text(detail.diagnosis)
            Spacer(Modifier.height(16.dp))
            detail.careLevels.forEach { care ->
                Text("${care.label}: ${care.recommendation}")
                LinearProgressIndicator(
                    progress = { care.level },
                    color = GreenPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantDetailPreview() {
    MobilekotlinTheme(dynamicColor = false) {
        PlantDetailContent(detail = MockPlantData.plantDetail, onBack = {})
    }
}
