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
)

@Composable
fun MorsePrendeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
