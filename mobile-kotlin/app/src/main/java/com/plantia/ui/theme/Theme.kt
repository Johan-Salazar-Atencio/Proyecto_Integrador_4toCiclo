package com.plantia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema oscuro real. Antes reutilizaba los colores claros, por lo que el
 * modo oscuro era identico al claro.
 */
private val DarkColorScheme = darkColorScheme(
    primary = GreenSecondary,
    onPrimary = ColorDarkSurface,
    primaryContainer = GreenDark,
    onPrimaryContainer = GreenLight,
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
    onPrimary = SurfaceWhite,
    primaryContainer = GreenLight,
    onPrimaryContainer = GreenDark,
    secondary = GreenSecondary,
    onSecondary = SurfaceWhite,
    tertiary = AmberAlert,
    onTertiary = SurfaceWhite,
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = BackgroundLight,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor
)

@Composable
fun MobilekotlinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // El color dinamico del sistema (Material You) anula la paleta verde de
    // marca, por eso viene desactivado por defecto.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
