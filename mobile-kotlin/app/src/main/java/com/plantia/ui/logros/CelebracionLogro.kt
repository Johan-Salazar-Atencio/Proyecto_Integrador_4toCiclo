package com.plantia.ui.logros

import android.Manifest
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.plantia.data.local.EstadoApp
import com.plantia.data.logros.AvisosLogros
import com.plantia.data.logros.Celebracion
import com.plantia.data.logros.GestorLogros
import com.plantia.data.model.LogroNuevo
import com.plantia.data.model.Rareza
import com.plantia.di.AppContainer
import com.plantia.ui.theme.DividerColor
import com.plantia.ui.theme.FuentePixel
import com.plantia.ui.theme.LocalTemaOscuro
import com.plantia.ui.theme.Primario
import com.plantia.ui.theme.SobrePrimario
import com.plantia.ui.theme.SuperficieElevada
import com.plantia.ui.theme.SurfaceWhite
import com.plantia.ui.theme.TextPrimary
import com.plantia.ui.theme.TextSecondary
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/*
 * Celebracion de logros desbloqueados: "D + aviso al terminar" (decision de Johan, 2026-11-22).
 *
 * Todos los logros salen con la medalla que brota (MedallaQueBrota, a pantalla completa). Cuando su animacion
 * TERMINA baja el aviso arriba (AvisoLogro), que se va solo a los 4 s (6 s con animaciones quitadas) o con un toque
 * y se queda sobre la app aunque se cierre la medalla. El logro se marca visto al CERRAR la medalla.
 * Desde el 4.º logro de una tanda se celebran los 2 mas raros y el resto sale en una tarjeta resumen.
 */

private const val TIC_NS = MotorMedalla.TIC_MS * 1_000_000L

/** `steps(n)` de CSS: avanza a saltos, como el pixel art. */
private fun pasos(n: Int) = Easing { if (it >= 1f) 1f else floor(it * n) / n }

/** Android no tiene "reducir movimiento": se usa la opcion de desarrollador "Quitar animaciones" (escala 0). */
@Composable
fun animacionesQuitadas(): Boolean {
    val contexto = LocalContext.current
    return remember {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Fondo de la celebracion: verde casi negro al ~88 %. */
@Composable
private fun veloCelebracion(): Color = if (LocalTemaOscuro.current) Color(0xE6040A07) else Color(0xE00A1A13)

/** Relleno y texto de la cinta de rareza (mismos tonos que la web). */
@Composable
private fun Rareza.coloresCinta(): Pair<Color, Color> = if (LocalTemaOscuro.current) {
    when (this) {
        Rareza.COMUN -> Color(0xFFA3E635) to Color(0xFF12200A)
        Rareza.RARA -> Color(0xFF38BDF8) to Color(0xFF04202E)
        Rareza.EPICA -> Color(0xFFC084FC) to Color(0xFF2A0A45)
        Rareza.LEGENDARIA -> Color(0xFFFBBF24) to Color(0xFF2B1A02)
    }
} else {
    when (this) {
        Rareza.COMUN -> Color(0xFF15803D) to Color.White
        Rareza.RARA -> Color(0xFF0369A1) to Color.White
        Rareza.EPICA -> Color(0xFF7E22CE) to Color.White
        Rareza.LEGENDARIA -> Color(0xFFD97706) to Color(0xFF2B1A02)
    }
}

/** Cinta con muescas en los lados (`clip-path` del prototipo). */
private class FormaCinta(private val muesca: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val m = with(density) { muesca.toPx() }
        val camino = Path().apply {
            moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width - m, size.height / 2)
            lineTo(size.width, size.height); lineTo(0f, size.height); lineTo(m, size.height / 2); close()
        }
        return Outline.Generic(camino)
    }
}

/** Donde quedo el hueco de la medalla, en celdas de la rejilla. */
private data class Geometria(val cx: Int, val bot: Int, val ancho: Int, val alto: Int)

private class Medidas {
    var contenedor: LayoutCoordinates? = null
    var hueco: LayoutCoordinates? = null
}

/**
 * Punto unico de enganche: pinta lo que toca de la [celebracion] (la medalla de un logro o la tarjeta resumen).
 * [onAnimacionTerminada] llega cuando termina la animacion (sale el aviso); [onTerminado] cuando el usuario la
 * cierra (solo entonces se marca visto).
 */
@Composable
fun CelebracionLogro(
    celebracion: Celebracion,
    reducido: Boolean,
    ocultarCuenta: Boolean,
    onAnimacionTerminada: (LogroNuevo) -> Unit,
    onTerminado: () -> Unit,
    onSaltarTodos: () -> Unit,
    onVerLogros: (() -> Unit)?,
    capaSuperior: @Composable () -> Unit
) {
    when (celebracion) {
        is Celebracion.Una -> key(celebracion.logro.idLogro) {
            MedallaQueBrota(
                celebracion = celebracion,
                reducido = reducido,
                ocultarCuenta = ocultarCuenta,
                onAnimacionTerminada = { onAnimacionTerminada(celebracion.logro) },
                onCerrar = onTerminado,
                onSaltarTodos = onSaltarTodos,
                capaSuperior = capaSuperior
            )
        }
        is Celebracion.Resumen -> ResumenLogros(
            logros = celebracion.logros,
            onCerrar = onTerminado,
            onVerLogros = onVerLogros,
            capaSuperior = capaSuperior
        )
    }
}

/**
 * Propuesta D: crece un tallo en la maceta, en su punta se forma la medalla, cae el sello (temblor, destellos y
 * hojas) y sube la tarjeta con la cinta de rareza. Un toque antes de 1 s salta al final; despues, cierra.
 * [capaSuperior] se dibuja encima de todo, dentro del dialogo (alli va el aviso mientras la medalla esta abierta).
 */
@Composable
fun MedallaQueBrota(
    celebracion: Celebracion.Una,
    reducido: Boolean,
    ocultarCuenta: Boolean,
    onAnimacionTerminada: () -> Unit,
    onCerrar: () -> Unit,
    onSaltarTodos: () -> Unit,
    capaSuperior: @Composable () -> Unit = {}
) {
    val logro = celebracion.logro
    val alTerminar by rememberUpdatedState(onAnimacionTerminada)
    val motor = remember(logro.idLogro) {
        MotorMedalla(logro.rareza.ordinal, reducido, alTerminar = { alTerminar() })
    }
    val icono = rememberIconoPixel(logro.icono)
    val tic = remember { mutableIntStateOf(0) }
    var tarjeta by remember { mutableStateOf(motor.tarjetaVisible) }
    val inicio = remember { SystemClock.uptimeMillis() }
    var geometria by remember { mutableStateOf<Geometria?>(null) }
    val medidas = remember { Medidas() }
    val haptico = LocalHapticFeedback.current

    // Haptico si, sonido no (decision de Johan)
    LaunchedEffect(logro.idLogro) { haptico.performHapticFeedback(HapticFeedbackType.LongPress) }

    // Bucle: acumula el tiempo de cada cuadro y avanza un tic cada 80 ms. El tic solo se lee al dibujar.
    val lista = geometria != null
    LaunchedEffect(motor, lista) {
        if (!lista) return@LaunchedEffect
        if (reducido) {
            motor.completarEstatico()
            tarjeta = true
            tic.intValue++
            return@LaunchedEffect
        }
        motor.tic()
        tic.intValue++
        var previo = withFrameNanos { it }
        var acumulado = 0L
        while (!motor.debeParar()) {
            val ahora = withFrameNanos { it }
            acumulado += minOf(400_000_000L, ahora - previo)
            previo = ahora
            var hubo = false
            while (acumulado >= TIC_NS) {
                acumulado -= TIC_NS
                motor.tic()
                hubo = true
            }
            if (hubo) {
                tic.intValue++
                if (motor.tarjetaVisible) tarjeta = true
            }
        }
    }

    fun antesDelSalto() = !reducido && !motor.terminada && SystemClock.uptimeMillis() - inicio < MotorMedalla.SALTO_MS
    fun adelantar() {
        motor.adelantar()
        tarjeta = true
        tic.intValue++
    }
    // Toque en cualquier parte: el primer segundo salta al final; despues continua
    val tocar = { if (antesDelSalto()) adelantar() else onCerrar() }
    val aceptar = {
        if (antesDelSalto()) adelantar()
        onCerrar()
    }

    val recompensa = logro.textoRecompensa()
    val anuncio = listOf(
        "Logro desbloqueado: ${logro.titulo}.", "Rareza ${logro.rareza.etiqueta}.", logro.descripcion, recompensa
    ).filter { it.isNotBlank() }.joinToString(" ")

    Dialog(
        onDismissRequest = onSaltarTodos, // Atras = "Saltar todos", como Escape en la web
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(veloCelebracion())
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = tocar)
        ) {
            val densidad = LocalDensity.current
            val p = with(densidad) { (if (maxWidth < 520.dp) 3.dp else 4.dp).toPx() }.roundToInt().coerceAtLeast(1).toFloat()
            val altoHueco = with(densidad) { (62 * p).toDp() }
            val horizontal = maxWidth > maxHeight && maxHeight < 560.dp
            val anchoTarjetaHorizontal = min(maxWidth * 0.52f, 420.dp)

            fun recalcular() {
                val c = medidas.contenedor ?: return
                val h = medidas.hueco ?: return
                if (!c.isAttached || !h.isAttached) return
                val pos = c.localPositionOf(h, Offset.Zero)
                val g = Geometria(
                    cx = MotorMedalla.redondear((pos.x + h.size.width / 2f) / p),
                    bot = MotorMedalla.redondear((pos.y + h.size.height) / p),
                    ancho = ceil(c.size.width / p).toInt(),
                    alto = ceil(c.size.height / p).toInt()
                )
                if (g != geometria) {
                    motor.colocar(g.cx, g.bot, g.ancho, g.alto)
                    geometria = g
                }
            }

            // Todo lo de dentro tiembla junto cuando cae el sello
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        tic.intValue
                        translationX = motor.shx * p
                        translationY = motor.shy * p
                    }
                    .onGloballyPositioned { medidas.contenedor = it; recalcular() }
            ) {
                Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }) {
                    tic.intValue
                    dibujarMedalla(motor, p, icono)
                }
                val hueco = Modifier.height(altoHueco).onGloballyPositioned { medidas.hueco = it; recalcular() }
                if (horizontal) {
                    Row(
                        Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(hueco.fillMaxWidth(0.38f))
                        TarjetaLogro(
                            celebracion, tarjeta, compacta = true, onAceptar = aceptar, onSaltarTodos = onSaltarTodos,
                            modifier = Modifier.widthIn(max = anchoTarjetaHorizontal)
                        )
                    }
                } else {
                    Column(
                        Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(hueco.fillMaxWidth())
                        TarjetaLogro(
                            celebracion, tarjeta, compacta = false, onAceptar = aceptar, onSaltarTodos = onSaltarTodos,
                            modifier = Modifier.widthIn(max = 360.dp)
                        )
                    }
                }
            }

            if (celebracion.total > 1) {
                Text(
                    "${celebracion.numero} de ${celebracion.total}",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .graphicsLayer { alpha = if (ocultarCuenta) 0f else 1f }
                        .clip(RoundedCornerShape(99.dp))
                        .background(SurfaceWhite)
                        .border(1.dp, DividerColor, RoundedCornerShape(99.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                )
            }
            // Lector de pantalla: anuncia el logro completo al abrir
            Box(
                Modifier.size(1.dp).semantics {
                    liveRegion = LiveRegionMode.Assertive
                    contentDescription = anuncio
                }
            )
            capaSuperior()
        }
    }
}

/** Tarjeta del logro: cinta de rareza, titulo pixel, descripcion, recompensa y botones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarjetaLogro(
    celebracion: Celebracion.Una,
    visible: Boolean,
    compacta: Boolean,
    onAceptar: () -> Unit,
    onSaltarTodos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logro = celebracion.logro
    val rz = logro.rareza.color()
    val aparicion by animateFloatAsState(if (visible) 1f else 0f, tween(320, easing = pasos(4)), label = "tarjeta")
    val cinta by animateFloatAsState(
        if (visible) 1f else 0f, tween(400, delayMillis = 100, easing = pasos(5)), label = "cinta"
    )
    val (relleno, textoCinta) = logro.rareza.coloresCinta()
    val forma = RoundedCornerShape(24.dp)
    val borde = if (logro.rareza == Rareza.LEGENDARIA) rz else lerp(DividerColor, rz, 0.55f)
    val brillo = when (logro.rareza) {
        Rareza.EPICA -> 22.dp
        Rareza.LEGENDARIA -> 30.dp
        else -> 0.dp
    }
    val desplazamiento = with(LocalDensity.current) { 18.dp.toPx() }
    val quedan = celebracion.quedanDespues

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = aparicion
                translationY = (1f - aparicion) * desplazamiento
            }
            .shadow(brillo, forma, ambientColor = rz, spotColor = rz)
            .clip(forma)
            .background(SurfaceWhite)
            .border(BorderStroke(1.dp, borde), forma)
            .padding(start = 18.dp, end = 18.dp, top = if (compacta) 12.dp else 18.dp, bottom = if (compacta) 8.dp else 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (compacta) 4.dp else 8.dp)
    ) {
        Text(
            "¡LOGRO DESBLOQUEADO!", fontFamily = FuentePixel, fontSize = 13.sp, letterSpacing = 1.8.sp,
            color = TextSecondary, textAlign = TextAlign.Center
        )
        Text(
            logro.rareza.etiqueta,
            modifier = Modifier
                .graphicsLayer { scaleX = cinta }
                .clip(FormaCinta(9.dp))
                .background(relleno)
                .padding(horizontal = 22.dp, vertical = 4.dp),
            fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textoCinta
        )
        Text(
            logro.titulo, fontFamily = FuentePixel, fontSize = if (compacta) 20.sp else 24.sp, lineHeight = 28.sp,
            color = TextPrimary, textAlign = TextAlign.Center
        )
        if (logro.descripcion.isNotBlank()) {
            Text(logro.descripcion, fontSize = 14.5.sp, color = TextSecondary, textAlign = TextAlign.Center)
        }
        Recompensas(logro.recompensaXp, logro.recompensaMonedas)
        BotonPrincipal(if (quedan > 0) "Siguiente" else "¡Genial!", compacta, onAceptar)
        if (quedan > 0) {
            TextButton(onClick = onSaltarTodos, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(
                    "Saltar todos ($quedan más)", color = TextSecondary, fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline
                )
            }
        } else {
            Text("Toca en cualquier parte para continuar", fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Recompensas(xp: Int, monedas: Int) {
    val partes = listOfNotNull(
        xp.takeIf { it > 0 }?.let { "+$it XP" },
        monedas.takeIf { it > 0 }?.let { "+$it ${if (it == 1) "hoja" else "hojas"}" }
    )
    if (partes.isEmpty()) return
    FlowRow(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        partes.forEach {
            Text(
                it,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SuperficieElevada)
                    .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary
            )
        }
    }
}

@Composable
private fun BotonPrincipal(texto: String, compacta: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp).heightIn(min = if (compacta) 46.dp else 52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Primario, contentColor = SobrePrimario)
    ) { Text(texto, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
}

/** Tarjeta resumen: "¡N logros más!" con sus iconos y la recompensa sumada. Un toque en cualquier parte la cierra. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResumenLogros(
    logros: List<LogroNuevo>,
    onCerrar: () -> Unit,
    onVerLogros: (() -> Unit)?,
    capaSuperior: @Composable () -> Unit = {}
) {
    val xp = logros.sumOf { it.recompensaXp }
    val hojas = logros.sumOf { it.recompensaMonedas }
    val anuncio = "${logros.size} logros más desbloqueados." +
        (if (xp > 0) " $xp XP." else "") + (if (hojas > 0) " $hojas hojas." else "")
    val forma = RoundedCornerShape(24.dp)
    val haptico = LocalHapticFeedback.current
    LaunchedEffect(Unit) { haptico.performHapticFeedback(HapticFeedbackType.LongPress) }

    Dialog(
        onDismissRequest = onCerrar,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(veloCelebracion())
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCerrar)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth()
                    .clip(forma)
                    .background(SurfaceWhite)
                    .border(BorderStroke(1.dp, lerp(DividerColor, Primario, 0.55f)), forma)
                    .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Y ADEMÁS", fontFamily = FuentePixel, fontSize = 13.sp, letterSpacing = 1.8.sp, color = TextSecondary)
                Text(
                    "¡${logros.size} logros más!", fontFamily = FuentePixel, fontSize = 24.sp, color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Los desbloqueaste mientras no mirabas.", fontSize = 14.5.sp, color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                FlowRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    logros.forEach { l ->
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SuperficieElevada)
                                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                                .padding(4.dp)
                                .semantics { contentDescription = l.titulo }
                        ) {
                            Image(
                                bitmap = rememberIconoPixel(l.icono), contentDescription = null,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit,
                                filterQuality = FilterQuality.None
                            )
                        }
                    }
                }
                Recompensas(xp, hojas)
                BotonPrincipal(if (onVerLogros != null) "Ver mis logros" else "¡Genial!", compacta = false) {
                    onCerrar()
                    onVerLogros?.invoke()
                }
                Text("Toca en cualquier parte para cerrar", fontSize = 12.sp, color = TextSecondary)
            }
            Box(
                Modifier.size(1.dp).semantics {
                    liveRegion = LiveRegionMode.Assertive
                    contentDescription = anuncio
                }
            )
            capaSuperior()
        }
    }
}

/**
 * Muestra la celebracion que toca (medalla o resumen), uno tras otro, y revisa los pendientes del servidor cada
 * vez que la app pasa a primer plano. Va una sola vez, encima de toda la navegacion. Tambien es la capa global del
 * aviso de arriba: sigue visible aunque se cierre la medalla.
 *
 * Despues de la ultima celebracion pide (una sola vez) el permiso de notificaciones para avisar de los logros
 * que se desbloqueen con la app cerrada.
 */
@Composable
fun HostCelebracionLogros(
    gestor: GestorLogros = AppContainer.celebracionLogros,
    onVerLogros: (() -> Unit)? = null
) {
    val contexto = LocalContext.current
    val celebracion by gestor.celebracion.collectAsState()
    val alcance = rememberCoroutineScope()
    val control = remember(gestor) { ControlCelebracion(gestor, alcance) }
    val aviso by control.aviso.collectAsState()
    val reducido = animacionesQuitadas()

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        EstadoApp.enPrimerPlano = true
        gestor.revisar()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { EstadoApp.enPrimerPlano = false }

    // Si lo niega no se vuelve a pedir: los logros se siguen celebrando dentro de la app
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val despuesDeCerrar = {
        if (gestor.enCola == 0 && AvisosLogros.debePedirPermiso(contexto)) {
            AvisosLogros.marcarPermisoPedido(contexto)
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Si empieza otra celebracion, el aviso anterior se quita
    val clave = when (val c = celebracion) {
        is Celebracion.Una -> "logro-${c.logro.idLogro}"
        is Celebracion.Resumen -> "resumen"
        null -> null
    }
    LaunchedEffect(clave) { control.alEmpezar(clave) }

    val capaAviso: @Composable () -> Unit = { aviso?.let { CapaAvisoLogro(it, reducido, control) } }

    when (val c = celebracion) {
        null -> capaAviso() // Sin medalla: el aviso queda sobre la app hasta su tiempo o un toque
        else -> CelebracionLogro(
            celebracion = c,
            reducido = reducido,
            ocultarCuenta = aviso != null,
            onAnimacionTerminada = { control.alTerminarAnimacion(it, reducido) },
            onTerminado = {
                when (c) {
                    is Celebracion.Una -> control.cerrar(c.logro)
                    is Celebracion.Resumen -> control.cerrarResumen(c.logros)
                }
                despuesDeCerrar()
            },
            onSaltarTodos = {
                control.saltarTodos()
                despuesDeCerrar()
            },
            onVerLogros = onVerLogros,
            capaSuperior = capaAviso
        )
    }
}
