package com.plantia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val DarkColorScheme = darkColorScheme(
    primary = GreenSecondary,
    onPrimary = ColorDarkSurface,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenLightClaro,
    secondary = GreenPrimary,
    onSecondary = ColorDarkSurface,
    tertiary = AmberAlert,
    onTertiary = ColorDarkSurface,
    background = ColorDarkBackground,
    onBackground = ColorDarkText,
    surface = ColorDarkSurface,
    onSurface = ColorDarkText,
    surfaceVariant = ColorDarkSurfaceVariant,
    onSurfaceVariant = ColorDarkTextMuted,
    outline = ColorDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = SurfaceWhiteClaro,
    primaryContainer = GreenLightClaro,
    onPrimaryContainer = GreenDark,
    secondary = GreenSecondary,
    onSecondary = SurfaceWhiteClaro,
    tertiary = AmberAlert,
    onTertiary = SurfaceWhiteClaro,
    background = BackgroundLightClaro,
    onBackground = TextPrimaryClaro,
    surface = SurfaceWhiteClaro,
    onSurface = TextPrimaryClaro,
    surfaceVariant = BackgroundLightClaro,
    onSurfaceVariant = TextSecondaryClaro,
    outline = DividerColorClaro
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
