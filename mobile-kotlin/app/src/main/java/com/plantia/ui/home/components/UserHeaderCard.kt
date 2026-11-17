package com.plantia.ui.home.components

import com.plantia.ui.components.BarraProgreso

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.data.model.UserSummary
import com.plantia.ui.components.FotoPerfil
import com.plantia.ui.theme.AmberDark
import com.plantia.ui.theme.FuentePixel
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/** Iniciales para el avatar cuando no hay foto: "Johan Demo" -> "JD", "Lucia" -> "L". */
internal fun iniciales(nombre: String): String =
    nombre.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }

/**
 * Encabezado del Inicio (propuesta E): avatar, saludo, rango y nivel, las hojas a la derecha y, debajo,
 * la barra de XP hacia el siguiente nivel. Tocar las hojas abre la tienda.
 */
@Composable
fun UserHeaderCard(
    user: UserSummary,
    onHojas: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(GreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                val textoIniciales: @Composable () -> Unit = {
                    Text(iniciales(user.name), color = Color.White, fontSize = 15.sp, fontFamily = FuentePixel, fontWeight = FontWeight.Medium)
                }
                if (user.profileImageUrl.isNullOrBlank()) textoIniciales()
                else FotoPerfil(user.profileImageUrl, "Foto de ${user.name}", respaldo = textoIniciales)
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("¡Hola, ${user.name}!", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, maxLines = 1)
                Text("${user.badge} · Nivel ${user.level}", fontSize = 12.sp, color = TextSecondary)
            }
            Surface(onClick = onHojas, shape = RoundedCornerShape(20.dp), color = SurfaceWhite, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Eco, contentDescription = "Hojas", tint = GreenDark, modifier = Modifier.size(16.dp))
                    Text("${user.leaves}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmberDark)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val progreso by animateFloatAsState(user.progresoNivel, tween(600), label = "xp-home")
        BarraProgreso(progreso = progreso, color = GreenDark, alto = 8.dp, pista = GreenPrimary.copy(alpha = 0.22f))
        Text(
            text = if (user.esNivelMaximo) "Nivel máximo · ${user.experience} XP"
            else "${user.experience} / ${user.experienceToNextLevel} XP para el nivel ${user.level + 1}",
            fontSize = 11.sp,
            color = TextSecondary,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
