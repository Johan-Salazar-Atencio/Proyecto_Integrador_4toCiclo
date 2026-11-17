package com.plantia.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.ui.theme.BackgroundLight
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.LocalTemaOscuro
import com.plantia.ui.theme.Primario
import com.plantia.ui.theme.SobrePrimario
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextSecondary
import com.plantia.ui.theme.Tinte

/** Pestanas de la barra inferior: cuatro destinos y, al centro, el boton de escanear. */
private data class Pestana(val ruta: String, val etiqueta: String, val icono: ImageVector, val rutasHijas: Set<String> = emptySet())

private val pestanasIzquierda = listOf(
    Pestana(Routes.HOME, "Inicio", Icons.Outlined.Home),
    Pestana(Routes.COLLECTION, "Colección", Icons.Outlined.GridView, setOf(Routes.COLLECTION_DIGITALES))
)
private val pestanasDerecha = listOf(
    Pestana(Routes.TIENDA, "Tienda", Icons.Outlined.ShoppingBag),
    Pestana(Routes.PERFIL, "Perfil", Icons.Outlined.Person)
)

/** Rutas donde se muestra la barra inferior. */
val rutasConBarra: Set<String> = setOf(
    Routes.HOME, Routes.COLLECTION, Routes.COLLECTION_DIGITALES, Routes.TIENDA, Routes.PERFIL
)

/**
 * Barra inferior flotante: una tarjeta redondeada separada de los bordes con Inicio, Coleccion, el boton
 * central de escanear (circular, lima, sobresale), Tienda y Perfil.
 * [rutaActual] marca la pestana activa; [onIr] navega a otro destino y [onEscanear] abre la camara.
 */
@Composable
fun BarraInferior(
    rutaActual: String?,
    onIr: (String) -> Unit,
    onEscanear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val forma = RoundedCornerShape(28.dp)
    val oscuro = LocalTemaOscuro.current
    // El boton central sale por arriba de la barra: por eso va en un Box aparte, que no recorta.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 10.dp, end = 10.dp, bottom = 10.dp, top = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                // Sombra suave solo en claro; en oscuro la tarjeta se separa con su borde
                .shadow(if (oscuro) 0.dp else 10.dp, forma, clip = false)
                .clip(forma)
                .background(SurfaceWhite)
                .border(1.dp, DividerColor, forma),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            pestanasIzquierda.forEach { p -> ItemPestana(p, rutaActual, onIr, Modifier.weight(1f)) }
            // Hueco del boton central: solo lleva su etiqueta debajo
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClick = onEscanear)
                    .padding(top = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Escanear", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Primario)
            }
            pestanasDerecha.forEach { p -> ItemPestana(p, rutaActual, onIr, Modifier.weight(1f)) }
        }
        BotonEscanear(onEscanear, Modifier.align(Alignment.TopCenter).offset(y = (-24).dp))
    }
}

@Composable
private fun RowScope.ItemPestana(
    pestana: Pestana,
    rutaActual: String?,
    onIr: (String) -> Unit,
    modifier: Modifier
) {
    val activa = rutaActual == pestana.ruta || rutaActual in pestana.rutasHijas
    val color = if (activa) Primario else TextSecondary
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Tab) { onIr(pestana.ruta) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (activa) Tinte else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(pestana.icono, contentDescription = pestana.etiqueta, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(pestana.etiqueta, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** El boton circular lima de la camara, levantado sobre la barra, con un aro del color del fondo. */
@Composable
private fun BotonEscanear(onEscanear: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(70.dp)
            .clip(CircleShape)
            .background(BackgroundLight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .shadow(8.dp, CircleShape, ambientColor = Primario, spotColor = Primario)
                .clip(CircleShape)
                .background(Primario)
                .clickable(role = Role.Button, onClick = onEscanear),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = "Escanear", tint = SobrePrimario, modifier = Modifier.size(26.dp))
        }
    }
}
