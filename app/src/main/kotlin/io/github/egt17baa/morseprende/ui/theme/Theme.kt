package io.github.egt17baa.morseprende.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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

@Composable
fun MorsePrendeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
