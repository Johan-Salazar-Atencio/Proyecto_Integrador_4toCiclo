package com.plantia.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.TextSecondary

/** Pestanas de la barra inferior (propuesta E): cuatro destinos y, al centro, el boton de escanear. */
private data class Pestana(val ruta: String, val etiqueta: String, val icono: ImageVector, val rutasHijas: Set<String> = emptySet())

private val pestanasIzquierda = listOf(
    Pestana(Routes.HOME, "Inicio", Icons.Default.Home),
    Pestana(Routes.COLLECTION, "Colección", Icons.Default.Collections, setOf(Routes.COLLECTION_DIGITALES))
)
private val pestanasDerecha = listOf(
    Pestana(Routes.TIENDA, "Tienda", Icons.Default.ShoppingBag),
    Pestana(Routes.PERFIL, "Perfil", Icons.Default.Person)
)

/** Rutas donde se muestra la barra inferior. */
val rutasConBarra: Set<String> = setOf(
    Routes.HOME, Routes.COLLECTION, Routes.COLLECTION_DIGITALES, Routes.TIENDA, Routes.PERFIL
)

/**
 * Barra inferior comun de la app: Inicio, Coleccion, boton central de escanear, Tienda y Perfil.
 * [rutaActual] marca la pestana activa; [onIr] navega a otro destino y [onEscanear] abre la camara.
 */
@Composable
fun BarraInferior(
    rutaActual: String?,
    onIr: (String) -> Unit,
    onEscanear: () -> Unit,
    modifier: Modifier = Modifier
) {
    // El boton central sale por arriba de la barra: por eso va en un Box aparte, que no recorta.
    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .height(62.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                pestanasIzquierda.forEach { p -> ItemPestana(p, rutaActual, onIr, Modifier.weight(1f)) }
                Spacer(modifier = Modifier.weight(1f))
                pestanasDerecha.forEach { p -> ItemPestana(p, rutaActual, onIr, Modifier.weight(1f)) }
            }
        }
        BotonEscanear(onEscanear, Modifier.align(Alignment.TopCenter).offset(y = (-20).dp))
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.ItemPestana(
    pestana: Pestana,
    rutaActual: String?,
    onIr: (String) -> Unit,
    modifier: Modifier
) {
    val activa = rutaActual == pestana.ruta || rutaActual in pestana.rutasHijas
    val color = if (activa) GreenPrimary else TextSecondary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onIr(pestana.ruta) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(pestana.icono, contentDescription = pestana.etiqueta, tint = color, modifier = Modifier.size(24.dp))
        Text(
            pestana.etiqueta,
            fontSize = 11.sp,
            color = color,
            fontWeight = if (activa) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/** El boton verde de la camara, levantado sobre la barra como en la propuesta E. */
@Composable
private fun BotonEscanear(onEscanear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onEscanear,
        modifier = modifier
            .size(52.dp)
            .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = GreenPrimary, spotColor = GreenPrimary),
        shape = RoundedCornerShape(18.dp),
        color = GreenPrimary,
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PhotoCamera, contentDescription = "Escanear", tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
}
