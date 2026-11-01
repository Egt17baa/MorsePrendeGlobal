package io.github.egt17baa.morseprende.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Colores de acento de cada capítulo (spec §8): el Faro usa los del tema; Sombra, oliva y ámbar;
 * Estelar, violeta, blanco estelar y magenta. Con alto contraste no se cambia nada.
 */
fun chapterColors(base: ColorScheme, chapterId: Int, dark: Boolean, highContrast: Boolean): ColorScheme {
    if (highContrast) return base
    return when (chapterId) {
        2 -> if (dark) {
            base.copy(primary = Amber, onPrimary = SeaNight, secondary = SignalGrey, primaryContainer = Olive, onPrimaryContainer = Amber, outline = Olive, outlineVariant = Olive)
        } else {
            base.copy(primary = Olive, onPrimary = White, secondary = SignalGrey, primaryContainer = Amber, onPrimaryContainer = SeaNight, outline = Olive, outlineVariant = Olive.copy(alpha = 0.55f))
        }
        3 -> if (dark) {
            base.copy(primary = StarWhite, onPrimary = DeepViolet, secondary = Magenta, primaryContainer = DeepViolet, onPrimaryContainer = StarWhite, outline = DeepViolet, outlineVariant = Magenta.copy(alpha = 0.6f))
        } else {
            base.copy(primary = DeepViolet, onPrimary = White, secondary = Magenta, primaryContainer = StarWhite, onPrimaryContainer = DeepViolet, outline = DeepViolet, outlineVariant = DeepViolet.copy(alpha = 0.55f))
        }
        else -> base
    }
}

/** Aplica la paleta del capítulo al contenido (las misiones de ese capítulo). */
@Composable
fun ChapterTheme(chapterId: Int, content: @Composable () -> Unit) {
    val colors = chapterColors(MaterialTheme.colorScheme, chapterId, LocalDarkTheme.current, LocalHighContrast.current)
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography, content = content)
}
