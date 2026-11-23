package com.plantia.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/** Textos del aviso de "red local" negada (QA-62). */
object TextosRedLocal {
    const val TITULO = "Plantia no puede conectarse al servidor"
    const val EXPLICACION = "Negaste el permiso de red local, y el servidor está en tu red (PC del laboratorio o de casa). Actívalo en Ajustes > Permisos."
    const val BOTON = "Abrir ajustes"
}

/** Aviso fijo arriba mientras el permiso de red local este negado; al volver de Ajustes desaparece solo. */
@Composable
fun AvisoRedLocal() {
    val contexto = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.dp, DividerColor)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(TextosRedLocal.TITULO, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(TextosRedLocal.EXPLICACION, fontSize = 13.sp, color = TextSecondary)
            TextButton(onClick = {
                contexto.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", contexto.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }) { Text(TextosRedLocal.BOTON, color = GreenDark, fontWeight = FontWeight.Bold) }
        }
    }
}
