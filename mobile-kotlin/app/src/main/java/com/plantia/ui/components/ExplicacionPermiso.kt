package com.plantia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.Primario
import com.plantia.ui.theme.SobrePrimario
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.ui.theme.Tinte

/** Textos que explican cada permiso antes del dialogo del sistema (QA-59). */
object TextosPermisos {
    const val RED_TITULO = "Conexión con tu red"
    const val RED_EXPLICACION =
        "Plantia se conecta al servidor de tu red (el PC del laboratorio o de casa) para identificar tus plantas. " +
            "Para buscarlo, Android te pedirá el permiso de «dispositivos cercanos»."
    const val NOTIF_TITULO = "Avisos de Plantia"
    const val NOTIF_EXPLICACION =
        "Te pediremos permiso de notificaciones para avisarte de logros y de tus plantas, aunque la app esté cerrada."
    const val CONTINUAR = "Continuar"
    const val AHORA_NO = "Ahora no"
}

/**
 * Tarjeta propia (24dp, borde de 1dp) que explica para que sirve un permiso; "Continuar" lanza el del sistema.
 * Sin [onAhoraNo] no se puede cerrar de otra forma: el permiso de red local hace falta para usar la app.
 */
@Composable
fun ExplicacionPermiso(
    titulo: String,
    texto: String,
    icono: ImageVector,
    onContinuar: () -> Unit,
    onAhoraNo: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = { onAhoraNo?.invoke() },
        properties = DialogProperties(dismissOnBackPress = onAhoraNo != null, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, DividerColor)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp).background(Tinte, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icono, contentDescription = null, tint = Primario, modifier = Modifier.size(28.dp))
                }
                Text(titulo, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary, textAlign = TextAlign.Center)
                Text(texto, fontSize = 14.sp, color = TextSecondary, textAlign = TextAlign.Center, lineHeight = 20.sp)
                Button(
                    onClick = onContinuar,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primario, contentColor = SobrePrimario)
                ) { Text(TextosPermisos.CONTINUAR, fontWeight = FontWeight.Bold) }
                if (onAhoraNo != null) {
                    TextButton(onClick = onAhoraNo) { Text(TextosPermisos.AHORA_NO, color = TextSecondary) }
                }
            }
        }
    }
}
