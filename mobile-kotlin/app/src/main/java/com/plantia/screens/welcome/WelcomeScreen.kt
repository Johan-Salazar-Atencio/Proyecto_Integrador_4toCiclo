package com.plantia.screens.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.plantia.ui.components.EscenaBrote
import com.plantia.ui.components.entradaEscalonada
import com.plantia.ui.components.escalaAlPresionar
import com.plantia.ui.theme.GreenDark
import com.plantia.ui.theme.GreenPrimary
import com.plantia.ui.theme.MobilekotlinTheme
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import com.plantia.util.IconosOscurosEnBarraDeEstado
import kotlinx.coroutines.delay

private val FondoBienvenida = Brush.verticalGradient(
    listOf(Color(0xFFE8F8F0), Color(0xFFF7FBF8), Color(0xFFFFF6EC))
)

/**
 * Primera pantalla de la app: presenta Plantia y lleva a crear la cuenta o a iniciar sesion.
 */
@Composable
fun WelcomeScreen(
    onCrearCuenta: () -> Unit = {},
    onIniciarSesion: () -> Unit = {}
) {
    IconosOscurosEnBarraDeEstado()

    // Al entrar, todo aparece en cascada (foto -> titular -> etiquetas -> botones).
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val config = LocalConfiguration.current
    val apaisado = config.screenWidthDp > config.screenHeightDp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FondoBienvenida)
            .safeDrawingPadding()
    ) {
        if (apaisado) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Escena(visible, Modifier.fillMaxHeight(0.86f))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Contenido(visible, onCrearCuenta, onIniciarSesion)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Marca(visible, Modifier.align(Alignment.Start))
                Spacer(Modifier.height(12.dp))
                Escena(visible, Modifier.fillMaxWidth(0.74f).widthIn(max = 320.dp))
                Spacer(Modifier.height(24.dp))
                Contenido(visible, onCrearCuenta, onIniciarSesion)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

/** Logo pequeno arriba a la izquierda. */
@Composable
private fun Marca(visible: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.entradaEscalonada(visible, retardoMs = 0, desplazamiento = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(shape = RoundedCornerShape(14.dp), color = SurfaceWhite, shadowElevation = 3.dp) {
            Icon(
                Icons.Filled.Eco,
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.padding(8.dp).size(24.dp)
            )
        }
        Text("Plantia", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GreenDark)
    }
}

/** Foto circular con el brote y el globo de saludo, que aparece con un rebote. */
@Composable
private fun Escena(visible: Boolean, modifier: Modifier = Modifier) {
    val globo = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(500)
        globo.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Box(modifier = modifier, contentAlignment = Alignment.TopCenter) {
        EscenaBrote(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
                .entradaEscalonada(visible, retardoMs = 100, desplazamiento = 0.dp, escalaInicial = 0.8f)
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = SurfaceWhite,
            shadowElevation = 6.dp,
            modifier = Modifier.graphicsLayer {
                scaleX = globo.value
                scaleY = globo.value
                alpha = globo.value.coerceIn(0f, 1f)
            }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("✨", fontSize = 16.sp)
                Text("¡Hola, jardinero!", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            }
        }
    }
}

/** Titular, descripcion, etiquetas y botones. */
@Composable
private fun Contenido(visible: Boolean, onCrearCuenta: () -> Unit, onIniciarSesion: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier.entradaEscalonada(visible, retardoMs = 300),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Tu jardín mágico",
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                "en el bolsillo",
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GreenPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Identifica plantas con IA y cuida a tu compañera botánica.",
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .entradaEscalonada(visible, retardoMs = 450),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Etiqueta(Icons.Filled.CameraAlt, "Escáner IA", Modifier.weight(1f))
            Etiqueta(Icons.Filled.Eco, "Mascota viva", Modifier.weight(1f))
            Etiqueta(Icons.Filled.WbSunny, "Alertas clima", Modifier.weight(1f))
        }

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier.entradaEscalonada(visible, retardoMs = 600),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val interaccion = remember { MutableInteractionSource() }
            Button(
                onClick = onCrearCuenta,
                interactionSource = interaccion,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .escalaAlPresionar(interaccion),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = Color.White),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Text("Crear mi Invernadero", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(16.dp))

            // Enlace de texto: no compite con el boton principal.
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onIniciarSesion)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿Ya tienes cuenta? ", fontSize = 15.sp, color = TextSecondary)
                Text("Iniciar sesión", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenDark)
            }
        }
    }
}

@Composable
private fun Etiqueta(icono: ImageVector, texto: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = SurfaceWhite.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, Color(0x1F10B981)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icono, contentDescription = null, tint = GreenDark, modifier = Modifier.size(15.dp))
            Spacer(Modifier.size(4.dp))
            Text(
                texto,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "Portrait", showBackground = true, widthDp = 393, heightDp = 851)
@Composable
private fun PreviewPortrait() {
    MobilekotlinTheme(dynamicColor = false) { WelcomeScreen() }
}

@Preview(name = "Landscape", showBackground = true, widthDp = 851, heightDp = 393)
@Composable
private fun PreviewLandscape() {
    MobilekotlinTheme(dynamicColor = false) { WelcomeScreen() }
}
