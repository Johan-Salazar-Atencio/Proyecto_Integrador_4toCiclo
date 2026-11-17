package com.plantia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = PrimarioOscuro,
    onPrimary = SobrePrimarioOscuro,
    primaryContainer = PrimarioOscuro.copy(alpha = 0.18f),
    onPrimaryContainer = PrimarioOscuro,
    secondary = AguaOscuro,
    onSecondary = ColorDarkBackground,
    tertiary = EventoOscuro,
    onTertiary = ColorDarkBackground,
    background = ColorDarkBackground,
    onBackground = ColorDarkText,
    surface = ColorDarkSurface,
    onSurface = ColorDarkText,
    surfaceVariant = ColorDarkSurfaceVariant,
    onSurfaceVariant = ColorDarkTextMuted,
    outline = ColorDarkOutline,
    outlineVariant = ColorDarkOutline,
    error = PeligroOscuro,
    onError = ColorDarkBackground
)

private val LightColorScheme = lightColorScheme(
    primary = PrimarioClaro,
    onPrimary = SobrePrimarioClaro,
    primaryContainer = PrimarioClaro.copy(alpha = 0.12f),
    onPrimaryContainer = PrimarioClaro,
    secondary = AguaClaro,
    onSecondary = SurfaceWhiteClaro,
    tertiary = EventoClaro,
    onTertiary = SurfaceWhiteClaro,
    background = BackgroundLightClaro,
    onBackground = TextPrimaryClaro,
    surface = SurfaceWhiteClaro,
    onSurface = TextPrimaryClaro,
    surfaceVariant = SuperficieElevadaClaro,
    onSurfaceVariant = TextSecondaryClaro,
    outline = DividerColorClaro,
    outlineVariant = DividerColorClaro,
    error = PeligroClaro,
    onError = SurfaceWhiteClaro
)

/**
 * `darkTheme` lo decide MainActivity segun el tema que eligio el usuario (Sistema, Claro u Oscuro).
 * El color dinamico del sistema (Material You) anula la paleta verde de marca, por eso no se usa.
 */
@Composable
fun MobilekotlinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalTemaOscuro provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
