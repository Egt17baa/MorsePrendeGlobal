package io.github.egt17baa.morseprende.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import io.github.egt17baa.morseprende.R

/**
 * Atkinson Hyperlegible (Braille Institute, SIL Open Font License 1.1), incluida en el APK: está
 * pensada para leerse bien con baja visión. La licencia está en `assets/licenses/`.
 */
val Atkinson = FontFamily(
    Font(R.font.atkinson_regular, FontWeight.Normal),
    Font(R.font.atkinson_bold, FontWeight.Bold),
)

private fun TextStyle.atkinson() = copy(fontFamily = Atkinson)

/** Tipografía de Material 3 con la fuente propia en todos los estilos. */
val AppTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.atkinson(), displayMedium = displayMedium.atkinson(), displaySmall = displaySmall.atkinson(),
        headlineLarge = headlineLarge.atkinson(), headlineMedium = headlineMedium.atkinson(), headlineSmall = headlineSmall.atkinson(),
        titleLarge = titleLarge.atkinson(), titleMedium = titleMedium.atkinson(), titleSmall = titleSmall.atkinson(),
        bodyLarge = bodyLarge.atkinson(), bodyMedium = bodyMedium.atkinson(), bodySmall = bodySmall.atkinson(),
        labelLarge = labelLarge.atkinson(), labelMedium = labelMedium.atkinson(), labelSmall = labelSmall.atkinson(),
    )
}
