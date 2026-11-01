package io.github.egt17baa.morseprende.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val DarkColors = darkColorScheme(
    primary = BeamCream,
    onPrimary = SeaNight,
    secondary = Coral,
    onSecondary = SeaNight,
    background = SeaNight,
    onBackground = Sand,
    surface = SeaDeep,
    onSurface = Sand,
    surfaceVariant = SeaMist,
    onSurfaceVariant = Sand.copy(alpha = 0.8f),
    primaryContainer = SeaMist,
    onPrimaryContainer = BeamCream,
    secondaryContainer = SeaMist,
    onSecondaryContainer = BeamCream,
    surfaceContainerLowest = SeaNight,
    surfaceContainerLow = SeaDeep,
    surfaceContainer = SeaDeep,
    surfaceContainerHigh = SeaDeep,
    surfaceContainerHighest = SeaMist,
    outline = SeaMist,
    outlineVariant = SeaMist,
)

private val LightColors = lightColorScheme(
    primary = Lagoon,
    onPrimary = White,
    secondary = CoralDark,
    onSecondary = White,
    background = Sand,
    onBackground = SeaNight,
    surface = White,
    onSurface = SeaNight,
    surfaceVariant = Sand,
    onSurfaceVariant = SeaDeep,
    primaryContainer = BeamCream,
    onPrimaryContainer = SeaNight,
    secondaryContainer = BeamCream,
    onSecondaryContainer = SeaNight,
    surfaceContainerLowest = White,
    surfaceContainerLow = White,
    surfaceContainer = White,
    surfaceContainerHigh = White,
    surfaceContainerHighest = Sand,
    outline = Lagoon,
    outlineVariant = Sand,
)

/** Alto contraste: negro y blanco puros, con un primario muy visible. */
private val HighContrastDark = darkColorScheme(
    primary = Color(0xFFFFE14D), onPrimary = Color.Black,
    secondary = Color(0xFFFFFFFF), onSecondary = Color.Black,
    background = Color.Black, onBackground = Color.White,
    surface = Color.Black, onSurface = Color.White,
    surfaceVariant = Color(0xFF1A1A1A), onSurfaceVariant = Color.White,
    primaryContainer = Color(0xFF1A1A1A), onPrimaryContainer = Color(0xFFFFE14D),
    secondaryContainer = Color(0xFF1A1A1A), onSecondaryContainer = Color.White,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black, surfaceContainer = Color(0xFF111111),
    surfaceContainerHigh = Color(0xFF1A1A1A), surfaceContainerHighest = Color(0xFF222222),
    outline = Color.White, outlineVariant = Color.White,
    error = Color(0xFFFF8A80), onError = Color.Black,
)

private val HighContrastLight = lightColorScheme(
    primary = Color(0xFF00363D), onPrimary = Color.White,
    secondary = Color.Black, onSecondary = Color.White,
    background = Color.White, onBackground = Color.Black,
    surface = Color.White, onSurface = Color.Black,
    surfaceVariant = Color(0xFFF0F0F0), onSurfaceVariant = Color.Black,
    primaryContainer = Color(0xFFE6F0F1), onPrimaryContainer = Color.Black,
    secondaryContainer = Color(0xFFEDEDED), onSecondaryContainer = Color.Black,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color.White, surfaceContainer = Color(0xFFF7F7F7),
    surfaceContainerHigh = Color(0xFFF0F0F0), surfaceContainerHighest = Color(0xFFE8E8E8),
    outline = Color.Black, outlineVariant = Color.Black,
    error = Color(0xFFB00020), onError = Color.White,
)

fun colorSchemeFor(dark: Boolean, highContrast: Boolean): ColorScheme = when {
    highContrast && dark -> HighContrastDark
    highContrast -> HighContrastLight
    dark -> DarkColors
    else -> LightColors
}

/** Si el alto contraste está activo (las paletas por capítulo se desactivan). */
val LocalHighContrast = staticCompositionLocalOf { false }

/** Si el tema actual es oscuro (para elegir la variante de las paletas por capítulo). */
val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun MorsePrendeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    highContrast: Boolean = false,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale * textScale),
        LocalHighContrast provides highContrast,
        LocalDarkTheme provides darkTheme,
    ) {
        MaterialTheme(colorScheme = colorSchemeFor(darkTheme, highContrast), typography = AppTypography, content = content)
    }
}
