package io.github.egt17baa.morseprende.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Colores de acento de cada capítulo (spec §8): el Faro usa los del tema; Sombra, oliva y ámbar;
 * Estelar, violeta, blanco estelar y magenta. Con alto contraste no se cambia nada.
 */
fun chapterColors(base: ColorScheme, chapterId: Int, dark: Boolean, highContrast: Boolean): ColorScheme {
    if (highContrast) return base
    return when (chapterId) {
        2 -> if (dark) {
            base.copy(primary = Amber, onPrimary = SeaNight, secondary = SignalGreyLight, primaryContainer = Olive, onPrimaryContainer = Amber, outline = Amber, outlineVariant = Olive)
        } else {
            base.copy(primary = Olive, onPrimary = White, secondary = SignalGreyDark, primaryContainer = Amber, onPrimaryContainer = SeaNight, outline = Olive, outlineVariant = Olive.copy(alpha = 0.55f))
        }
        3 -> if (dark) {
            base.copy(primary = StarWhite, onPrimary = DeepViolet, secondary = MagentaLight, primaryContainer = DeepViolet, onPrimaryContainer = StarWhite, outline = VioletLight, outlineVariant = MagentaLight.copy(alpha = 0.6f))
        } else {
            base.copy(primary = DeepViolet, onPrimary = White, secondary = MagentaDark, primaryContainer = StarWhite, onPrimaryContainer = DeepViolet, outline = DeepViolet, outlineVariant = DeepViolet.copy(alpha = 0.55f))
        }
        else -> base
    }
}

/** Contraste WCAG entre dos colores (1 a 21). */
fun contrastRatio(a: Color, b: Color): Double {
    val la = a.luminance() + 0.05
    val lb = b.luminance() + 0.05
    return maxOf(la, lb) / minOf(la, lb)
}

/**
 * Aro de «siguiente misión»: el secundario si destaca sobre el círculo; si no, el color del texto;
 * y si tampoco, blanco o negro (el que más destaque).
 */
fun nextRingColor(colors: ColorScheme): Color = listOf(colors.secondary, colors.onBackground)
    .firstOrNull { contrastRatio(it, colors.primary) >= 2.0 }
    ?: listOf(Color.White, Color.Black).maxBy { contrastRatio(it, colors.primary) }

/** Aplica la paleta del capítulo al contenido (las misiones de ese capítulo). */
@Composable
fun ChapterTheme(chapterId: Int, content: @Composable () -> Unit) {
    val colors = chapterColors(MaterialTheme.colorScheme, chapterId, LocalDarkTheme.current, LocalHighContrast.current)
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography, content = content)
}
