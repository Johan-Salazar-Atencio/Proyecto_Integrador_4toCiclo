package com.plantia.screens.scan.components

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private val Verde = Color(0xFF10B981)
private val VerdeClaro = Color(0xFF6EE7B7)
private val Fondo = Color(0xFF05100B)

/** Mensajes por etapa: solo dan sensacion de avance (el servidor no informa progreso real). */
private val ETAPAS = listOf(
    "Buscando información…" to "Preparando tu foto",
    "Analizando la imagen…" to "La IA observa hojas, forma y color",
    "Identificando la especie…" to "Comparando con miles de plantas",
    "Casi listo…" to "Armando tu guía de cuidados"
)

/**
 * Pantalla de espera del escaneo: anillo animado alrededor de la foto, mensajes por etapas,
 * check de exito al terminar y boton para cancelar y volver a la camara.
 */
@Composable
fun ScanProgressOverlay(
    foto: File?,
    terminado: Boolean,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Mientras se espera, el boton atras cancela en vez de salir de la pantalla.
    BackHandler(enabled = !terminado, onBack = onCancelar)

    var etapa by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3_000); etapa = 1
        kotlinx.coroutines.delay(6_000); etapa = 2
        kotlinx.coroutines.delay(8_000); etapa = 3
    }

    val miniatura = remember(foto) {
        foto?.let {
            val opciones = BitmapFactory.Options().apply { inSampleSize = 8 }
            BitmapFactory.decodeFile(it.absolutePath, opciones)?.asImageBitmap()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Fondo)
            // Bloquea los toques hacia la camara que queda debajo.
            .pointerInput(Unit) { detectTapGestures { } }
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                if (terminado) {
                    CheckExito()
                } else {
                    AnilloGiratorio()
                    // Miniatura (o una hoja que "respira") dentro del anillo.
                    val pulso = rememberInfiniteTransition(label = "pulso")
                    val escala by pulso.animateFloat(
                        initialValue = 0.94f,
                        targetValue = 1.04f,
                        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
                        label = "escala"
                    )
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .scale(escala)
                            .clip(CircleShape)
                            .background(Color(0xFF0F2A1E)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (miniatura != null) {
                            Image(
                                bitmap = miniatura,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Filled.Eco, contentDescription = null, tint = Verde, modifier = Modifier.size(64.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            AnimatedContent(
                targetState = if (terminado) -1 else etapa,
                transitionSpec = {
                    (fadeIn(tween(350)) + slideInVertically(tween(350)) { it / 3 }) togetherWith
                        (fadeOut(tween(250)) + slideOutVertically(tween(250)) { -it / 3 })
                },
                label = "etapa"
            ) { e ->
                val (titulo, detalle) = if (e == -1) {
                    "¡Planta identificada!" to "Abriendo tu ficha"
                } else {
                    ETAPAS[e.coerceIn(0, ETAPAS.lastIndex)]
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = titulo,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = detalle,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            if (!terminado) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .width(180.dp)
                        .clip(RoundedCornerShape(50)),
                    color = Verde,
                    trackColor = Color(0xFF123524)
                )
            }
        }

        if (!terminado) {
            OutlinedButton(
                onClick = onCancelar,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 24.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Cancelar", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun AnilloGiratorio() {
    val transicion = rememberInfiniteTransition(label = "anillo")
    val giro by transicion.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing)),
        label = "giro"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        val grosor = 8.dp.toPx()
        val inset = grosor / 2
        val lado = size.minDimension - grosor
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        val tam = androidx.compose.ui.geometry.Size(lado, lado)
        // Pista tenue + arco brillante que gira.
        drawArc(
            color = Color(0xFF123524),
            startAngle = 0f, sweepAngle = 360f, useCenter = false,
            topLeft = topLeft, size = tam,
            style = Stroke(width = grosor)
        )
        drawArc(
            color = Verde,
            startAngle = giro, sweepAngle = 90f, useCenter = false,
            topLeft = topLeft, size = tam,
            style = Stroke(width = grosor, cap = StrokeCap.Round)
        )
        drawArc(
            color = VerdeClaro,
            startAngle = giro + 180f, sweepAngle = 40f, useCenter = false,
            topLeft = topLeft, size = tam,
            style = Stroke(width = grosor, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun CheckExito() {
    val escala = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        escala.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(
        modifier = Modifier
            .size(140.dp)
            .scale(escala.value)
            .clip(CircleShape)
            .background(Verde),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Check, contentDescription = "Listo", tint = Color.White, modifier = Modifier.size(84.dp))
    }
}
