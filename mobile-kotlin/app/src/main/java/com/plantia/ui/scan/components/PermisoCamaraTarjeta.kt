package com.plantia.ui.scan.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SobrePrimario
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/** Textos de la tarjeta segun si Android aun puede mostrar el dialogo del permiso (QA-55). */
object TextosPermisoCamara {
    const val TITULO = "Necesitamos la cámara"
    const val EXPLICACION = "Para tomar la foto de tu planta. También puedes elegir una foto de la galería (botón de abajo a la izquierda)."
    fun boton(negadoParaSiempre: Boolean) = if (negadoParaSiempre) "Abrir ajustes" else "Permitir cámara"
}

/** Tarjeta del escaner cuando no hay permiso de camara: explica, ofrece pedirlo de nuevo o ir a Ajustes. */
@Composable
fun PermisoCamaraTarjeta(negadoParaSiempre: Boolean, onPermitir: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(36.dp))
            Text(TextosPermisoCamara.TITULO, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(TextosPermisoCamara.EXPLICACION, fontSize = 13.sp, color = TextSecondary, textAlign = TextAlign.Center)
            Button(
                onClick = onPermitir,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = SobrePrimario)
            ) { Text(TextosPermisoCamara.boton(negadoParaSiempre)) }
        }
    }
}
