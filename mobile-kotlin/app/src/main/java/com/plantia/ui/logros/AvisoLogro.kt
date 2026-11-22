package com.plantia.ui.logros

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.plantia.data.model.Rareza
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.FuentePixel
import com.plantia.ui.theme.SuperficieElevada
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary

/** La misma curva con rebote del prototipo: `cubic-bezier(.2, 1.2, .35, 1)`. */
private val CURVA_BAJA = CubicBezierEasing(0.2f, 1.2f, 0.35f, 1f)

/**
 * Capa del aviso: un `Popup` arriba y al centro que NO roba el foco ni el boton Atras y deja tocar la app.
 * Se llama desde dentro del `Dialog` de la medalla (para quedar por encima de el) o desde el host cuando
 * la medalla ya se cerro.
 */
@Composable
fun CapaAvisoLogro(aviso: AvisoVisible, reducido: Boolean, control: ControlCelebracion) {
    val arriba = with(LocalDensity.current) {
        (WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding() + 12.dp).roundToPx()
    }
    Box(Modifier.fillMaxSize()) {
        Popup(
            alignment = Alignment.TopCenter,
            offset = IntOffset(0, arriba),
            properties = PopupProperties(focusable = false, dismissOnBackPress = false, dismissOnClickOutside = false)
        ) {
            AvisoLogro(
                aviso = aviso,
                reducido = reducido,
                onEntro = { control.marcarEntrada(aviso.serie) },
                onTocar = { control.quitarAviso(aviso.serie) }
            )
        }
    }
}

/** El aviso en si: icono pixelado, "¡Logro desbloqueado!", titulo y recompensa. Baja desde arriba con rebote. */
@Composable
fun AvisoLogro(aviso: AvisoVisible, reducido: Boolean, onEntro: () -> Unit, onTocar: () -> Unit) {
    val logro = aviso.logro
    val rz = logro.rareza.color()
    val bajada = remember(aviso.serie) { Animatable(if (aviso.entro || reducido) 0f else -1.6f) }
    LaunchedEffect(aviso.serie) {
        if (bajada.value != 0f) bajada.animateTo(0f, tween(450, easing = CURVA_BAJA))
        onEntro()
    }
    val recompensa = logro.textoRecompensa()
    val forma = RoundedCornerShape(22.dp)
    val legendaria = logro.rareza == Rareza.LEGENDARIA
    val borde = if (legendaria) rz else lerp(DividerColor, rz, 0.6f)
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .graphicsLayer { translationY = bajada.value * size.height }
            .shadow(
                if (legendaria) 16.dp else 10.dp, forma,
                ambientColor = if (legendaria) rz else Color.Black,
                spotColor = if (legendaria) rz else Color.Black
            )
            .clip(forma)
            .background(SurfaceWhite)
            .border(BorderStroke(1.dp, borde), forma)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTocar)
            .semantics(mergeDescendants = true) {
                contentDescription = "Logro desbloqueado: ${logro.titulo}. $recompensa"
                onClick(label = "Cerrar aviso") { onTocar(); true }
            }
            .padding(start = 8.dp, top = 8.dp, bottom = 8.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(SuperficieElevada).padding(3.dp)
        ) {
            Image(
                bitmap = rememberIconoPixel(logro.icono),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                filterQuality = FilterQuality.None
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "¡LOGRO DESBLOQUEADO!", fontFamily = FuentePixel, fontSize = 11.sp, letterSpacing = 1.5.sp,
                color = TextSecondary, maxLines = 1
            )
            Text(
                logro.titulo, fontFamily = FuentePixel, fontSize = 17.sp, lineHeight = 20.sp, color = TextPrimary,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            if (recompensa.isNotEmpty()) {
                Text(recompensa, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary, maxLines = 1)
            }
        }
    }
}
