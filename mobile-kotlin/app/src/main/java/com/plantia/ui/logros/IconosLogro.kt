package com.plantia.ui.logros

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Cottage
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Flood
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.Yard
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.plantia.data.model.Rareza
import com.plantia.ui.theme.Abono
import com.plantia.ui.theme.Agua
import com.plantia.ui.theme.Luz
import com.plantia.ui.theme.Primario

/** Icono por defecto cuando el admin crea una clave que la app todavia no conoce. */
private val ICONO_POR_DEFECTO: ImageVector get() = Icons.Filled.EmojiEvents

/**
 * Clave de texto que manda el servidor -> icono de Material. Una clave nueva o vacia usa [ICONO_POR_DEFECTO],
 * asi que el admin puede ampliar la lista sin romper la app.
 */
fun iconoDeLogro(clave: String?): ImageVector = when (clave?.trim()?.lowercase()) {
    "semilla" -> Icons.Filled.Grass
    "hoja" -> Icons.Filled.Eco
    "huayco" -> Icons.Filled.Flood
    "copo" -> Icons.Filled.AcUnit
    "sol" -> Icons.Filled.WbSunny
    "bicho" -> Icons.Filled.BugReport
    "escudo" -> Icons.Filled.Shield
    "refugio" -> Icons.Filled.Cottage
    "corazon" -> Icons.Filled.Favorite
    "gota" -> Icons.Filled.WaterDrop
    "abono" -> Icons.Filled.Science
    "luz" -> Icons.Filled.LightMode
    "racha" -> Icons.Filled.LocalFireDepartment
    "nivel" -> Icons.Filled.Star
    "tienda" -> Icons.Filled.Storefront
    "maceta" -> Icons.Filled.LocalFlorist
    "invernadero" -> Icons.Filled.Yard
    "fondo" -> Icons.Filled.Landscape
    "mision" -> Icons.Filled.Flag
    "amanecer" -> Icons.Filled.WbTwilight
    "luna" -> Icons.Filled.Bedtime
    "secreto" -> Icons.Filled.QuestionMark
    else -> ICONO_POR_DEFECTO
}

/** Color de cada rareza, tomado del tema (cambia solo entre modo claro y oscuro). */
@Composable
fun Rareza.color(): Color = when (this) {
    Rareza.COMUN -> Primario
    Rareza.RARA -> Agua
    Rareza.EPICA -> Abono
    Rareza.LEGENDARIA -> Luz
}
