package com.plantia.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/*
 * Paleta Esmeralda + Lima (nuevo diseno). Cada color tiene su valor oscuro y su valor claro; los getters de
 * abajo eligen segun el tema. Los nombres antiguos (GreenPrimary, TextPrimary...) se conservan para que el
 * resto de pantallas no cambie: solo cambian sus valores.
 */

// Oscuro
val ColorDarkBackground = Color(0xFF0B1512)
val ColorDarkSurface = Color(0xFF121F1A)
val ColorDarkSurfaceVariant = Color(0xFF1A2B24)
val ColorDarkText = Color(0xFFE8F3EC)
val ColorDarkTextMuted = Color(0xFF8FA89B)
val ColorDarkOutline = Color(0xFF243830)
val PrimarioOscuro = Color(0xFFA3E635)
val SobrePrimarioOscuro = Color(0xFF12200A)
val AguaOscuro = Color(0xFF38BDF8)
val LuzOscuro = Color(0xFFFBBF24)
val AbonoOscuro = Color(0xFFC084FC)
val EventoOscuro = Color(0xFFFB923C)
val PeligroOscuro = Color(0xFFFB7185)

// Claro
val BackgroundLightClaro = Color(0xFFF6FAF7)
val SurfaceWhiteClaro = Color(0xFFFFFFFF)
val SuperficieElevadaClaro = Color(0xFFEAF3EC)
val TextPrimaryClaro = Color(0xFF10261D)
val TextSecondaryClaro = Color(0xFF5A7164)
val DividerColorClaro = Color(0xFFD5E4DA)
val PrimarioClaro = Color(0xFF15803D)
val SobrePrimarioClaro = Color(0xFFFFFFFF)
val AguaClaro = Color(0xFF0284C7)
val LuzClaro = Color(0xFFD97706)
val AbonoClaro = Color(0xFF9333EA)
val EventoClaro = Color(0xFFEA580C)
val PeligroClaro = Color(0xFFE11D48)

// Compatibilidad con codigo que usa los nombres de antes
val GreenLightClaro = Color(0x1F15803D)
val AmberLightClaro = Color(0x1FD97706)
val BlueLightClaro = Color(0x1F0284C7)
val GreenLightOscuro = Color(0x24A3E635)
val AmberLightOscuro = Color(0x24FBBF24)
val BlueLightOscuro = Color(0x2438BDF8)

/** Lo fija MobilekotlinTheme segun el tema que eligio el usuario. */
val LocalTemaOscuro = staticCompositionLocalOf { false }

@Composable
@ReadOnlyComposable
private fun elegir(oscuro: Color, claro: Color): Color = if (LocalTemaOscuro.current) oscuro else claro

// ---- Colores nuevos del diseno ----
/** Color de marca: lima en oscuro, verde bosque en claro. */
val Primario: Color @Composable @ReadOnlyComposable get() = elegir(PrimarioOscuro, PrimarioClaro)
/** Texto o icono sobre [Primario]. */
val SobrePrimario: Color @Composable @ReadOnlyComposable get() = elegir(SobrePrimarioOscuro, SobrePrimarioClaro)
/** Fondo tenue del primario (elemento activo, chips). */
val Tinte: Color @Composable @ReadOnlyComposable get() = elegir(PrimarioOscuro.copy(alpha = 0.14f), PrimarioClaro.copy(alpha = 0.12f))
/** Superficie elevada: filas dentro de tarjetas, botones, pildoras. */
val SuperficieElevada: Color @Composable @ReadOnlyComposable get() = elegir(ColorDarkSurfaceVariant, SuperficieElevadaClaro)
val Agua: Color @Composable @ReadOnlyComposable get() = elegir(AguaOscuro, AguaClaro)
val Luz: Color @Composable @ReadOnlyComposable get() = elegir(LuzOscuro, LuzClaro)
val Abono: Color @Composable @ReadOnlyComposable get() = elegir(AbonoOscuro, AbonoClaro)
val Evento: Color @Composable @ReadOnlyComposable get() = elegir(EventoOscuro, EventoClaro)
val Peligro: Color @Composable @ReadOnlyComposable get() = elegir(PeligroOscuro, PeligroClaro)
/** Fondo oscuro translucido detras de las ventanas flotantes. */
val Velo: Color @Composable @ReadOnlyComposable get() = elegir(Color(0xA8040A07), Color(0x7310261D))

// ---- Nombres de antes, ahora con la paleta nueva ----
val GreenPrimary: Color @Composable @ReadOnlyComposable get() = Primario
val GreenSecondary: Color @Composable @ReadOnlyComposable get() = elegir(Color(0xFF84CC16), Color(0xFF22A04D))
val GreenDark: Color @Composable @ReadOnlyComposable get() = elegir(Color(0xFFA3E635), Color(0xFF166534))
val AmberAlert: Color @Composable @ReadOnlyComposable get() = Luz
val AmberDark: Color @Composable @ReadOnlyComposable get() = Evento
val BlueInfo: Color @Composable @ReadOnlyComposable get() = Agua

val GreenLight: Color @Composable @ReadOnlyComposable get() = Tinte
val AmberLight: Color @Composable @ReadOnlyComposable get() = elegir(AmberLightOscuro, AmberLightClaro)
val BlueLight: Color @Composable @ReadOnlyComposable get() = elegir(BlueLightOscuro, BlueLightClaro)
val BackgroundLight: Color @Composable @ReadOnlyComposable get() = elegir(ColorDarkBackground, BackgroundLightClaro)
val SurfaceWhite: Color @Composable @ReadOnlyComposable get() = elegir(ColorDarkSurface, SurfaceWhiteClaro)
val TextPrimary: Color @Composable @ReadOnlyComposable get() = elegir(ColorDarkText, TextPrimaryClaro)
val TextSecondary: Color @Composable @ReadOnlyComposable get() = elegir(ColorDarkTextMuted, TextSecondaryClaro)
val DividerColor: Color @Composable @ReadOnlyComposable get() = elegir(ColorDarkOutline, DividerColorClaro)

/** Fondo de las pantallas de bienvenida y acceso: ahora plano, con un leve toque de marca arriba. */
val FondoDegradado: Brush
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) {
        Brush.verticalGradient(listOf(Color(0xFF10241B), ColorDarkBackground, ColorDarkBackground))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFE6F2EA), BackgroundLightClaro, BackgroundLightClaro))
    }

/** Fondo de los campos de texto. */
val CampoFondo: Color @Composable @ReadOnlyComposable get() = SuperficieElevada

/** Avisos de error: fondo y texto. */
val RojoClaro: Color
    @Composable @ReadOnlyComposable get() = elegir(PeligroOscuro.copy(alpha = 0.16f), PeligroClaro.copy(alpha = 0.10f))
val RojoTexto: Color @Composable @ReadOnlyComposable get() = Peligro

/** Verde muy suave para el final de degradados de tarjetas. */
val GreenLightSuave: Color @Composable @ReadOnlyComposable get() = elegir(Color(0xFF0F211A), Color(0xFFF1F8F3))
