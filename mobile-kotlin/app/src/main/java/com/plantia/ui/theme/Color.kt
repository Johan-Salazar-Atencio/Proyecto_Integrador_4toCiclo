package com.plantia.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Colores de marca: iguales en tema claro y oscuro. */
val GreenPrimary = Color(0xFF22C55E)
val GreenSecondary = Color(0xFF4ADE80)
val GreenDark = Color(0xFF16A34A)

val AmberAlert = Color(0xFFF59E0B)
val AmberDark = Color(0xFFD97706)

val BlueInfo = Color(0xFF3B82F6)

// --- Valores base del tema claro y oscuro (para Theme.kt y para los getters de abajo) ---
val GreenLightClaro = Color(0xFFE8F8F0)
val AmberLightClaro = Color(0xFFFFF3E0)
val BlueLightClaro = Color(0xFFE8F4FD)
val BackgroundLightClaro = Color(0xFFF9FAFB)
val SurfaceWhiteClaro = Color(0xFFFFFFFF)
val TextPrimaryClaro = Color(0xFF1F2937)
val TextSecondaryClaro = Color(0xFF6B7280)
val DividerColorClaro = Color(0xFFE5E7EB)

val ColorDarkBackground = Color(0xFF111827)
val ColorDarkSurface = Color(0xFF1F2937)
val ColorDarkSurfaceVariant = Color(0xFF374151)
val ColorDarkText = Color(0xFFF3F4F6)
val ColorDarkTextMuted = Color(0xFF9CA3AF)
val ColorDarkOutline = Color(0xFF4B5563)
val GreenLightOscuro = Color(0xFF123D27)
val AmberLightOscuro = Color(0xFF3A2A0E)
val BlueLightOscuro = Color(0xFF14263D)

/** Lo fija MobilekotlinTheme segun el tema que eligio el usuario. */
val LocalTemaOscuro = staticCompositionLocalOf { false }

/**
 * Colores que cambian con el tema. Se usan igual que antes (`TextPrimary`, `GreenLight`...), pero ahora
 * solo se pueden leer dentro de un @Composable.
 */
val GreenLight: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) GreenLightOscuro else GreenLightClaro
val AmberLight: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) AmberLightOscuro else AmberLightClaro
val BlueLight: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) BlueLightOscuro else BlueLightClaro
val BackgroundLight: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) ColorDarkBackground else BackgroundLightClaro
val SurfaceWhite: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) ColorDarkSurface else SurfaceWhiteClaro
val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) ColorDarkText else TextPrimaryClaro
val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) ColorDarkTextMuted else TextSecondaryClaro
val DividerColor: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) ColorDarkOutline else DividerColorClaro

/** Fondo con degradado de las pantallas de bienvenida y acceso. */
val FondoDegradado: Brush
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) {
        Brush.verticalGradient(listOf(Color(0xFF123D27), ColorDarkBackground, ColorDarkBackground))
    } else {
        Brush.verticalGradient(listOf(GreenLightClaro, Color(0xFFF7FBF8), Color(0xFFFFF6EC)))
    }

/** Fondo de los campos de texto. */
val CampoFondo: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) ColorDarkSurfaceVariant else Color(0xFFF1F5F9)

/** Avisos de error: fondo y texto. */
val RojoClaro: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) Color(0xFF3D1A1A) else Color(0xFFFEE2E2)
val RojoTexto: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) Color(0xFFFCA5A5) else Color(0xFF991B1B)

/** Verde muy suave para el final de degradados de tarjetas. */
val GreenLightSuave: Color
    @Composable @ReadOnlyComposable get() = if (LocalTemaOscuro.current) Color(0xFF0F2E1C) else Color(0xFFF0FDF4)
