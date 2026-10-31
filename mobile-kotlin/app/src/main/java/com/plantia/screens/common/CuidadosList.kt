package com.plantia.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.model.Cuidado
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.ui.theme.SurfaceWhite

/** Lista de secciones de cuidado (1:N) de una planta. */
@Composable
fun CuidadosList(
    cuidados: List<Cuidado>,
    modifier: Modifier = Modifier
) {
    if (cuidados.isEmpty()) {
        Text(
            text = "Todavia no hay cuidados registrados para esta planta.",
            fontSize = 13.sp,
            color = TextSecondary,
            modifier = modifier.padding(16.dp)
        )
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Cuidados",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        cuidados.forEach { cuidado ->
            CuidadoCard(cuidado)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CuidadoCard(cuidado: Cuidado) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        color = SurfaceWhite,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(BackgroundLight.copy(alpha = 0.5f))
                .padding(14.dp)
        ) {
            Text(
                text = cuidado.titulo.ifBlank { cuidado.tipo },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = cuidado.contenido,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}
