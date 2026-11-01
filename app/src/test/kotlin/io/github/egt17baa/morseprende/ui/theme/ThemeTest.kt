package io.github.egt17baa.morseprende.ui.theme

import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class ThemeTest {

    @get:Rule val compose = createComposeRule()

    @Test fun high_contrast_schemes() {
        val dark = colorSchemeFor(dark = true, highContrast = true)
        assertEquals(Color.Black, dark.background)
        assertEquals(Color.White, dark.onBackground)
        assertEquals(Color(0xFFFFE14D), dark.primary)
        val light = colorSchemeFor(dark = false, highContrast = true)
        assertEquals(Color.White, light.background)
        assertEquals(Color.Black, light.onBackground)
    }

    @Test fun text_scale_enlarges_text() {
        var scale by androidx.compose.runtime.mutableStateOf(1f)
        val seen = mutableListOf<Pair<Float, Float>>()
        compose.setContent {
            MorsePrendeTheme(textScale = scale) {
                val d = androidx.compose.ui.platform.LocalDensity.current
                Text("A", modifier = Modifier.testTag("t"), onTextLayout = { seen += d.fontScale to it.size.height.toFloat() })
            }
        }
        compose.waitForIdle()
        scale = 1.3f
        compose.waitForIdle()
        val (s1, h1) = seen.first()
        val (s2, h2) = seen.last()
        assertEquals(s1 * 1.3f, s2, 1e-3f)
        assertTrue("h1=$h1 h2=$h2", h2 >= h1 * 1.15f)
    }

    @Test fun typography_uses_atkinson() {
        assertEquals(Atkinson, AppTypography.bodyLarge.fontFamily)
        assertEquals(Atkinson, AppTypography.titleLarge.fontFamily)
    }

    @Test fun chapter_palettes() {
        val dark = colorSchemeFor(true, false)
        val light = colorSchemeFor(false, false)
        assertEquals(Amber, chapterColors(dark, 2, dark = true, highContrast = false).primary)
        assertEquals(Olive, chapterColors(light, 2, dark = false, highContrast = false).primary)
        assertEquals(StarWhite, chapterColors(dark, 3, dark = true, highContrast = false).primary)
        assertEquals(MagentaLight, chapterColors(dark, 3, dark = true, highContrast = false).secondary)
        assertEquals(DeepViolet, chapterColors(light, 3, dark = false, highContrast = false).primary)
        assertEquals(dark, chapterColors(dark, 1, dark = true, highContrast = false))
    }

    @Test fun high_contrast_overrides_chapter_palette() {
        val hc = colorSchemeFor(true, true)
        assertEquals(hc, chapterColors(hc, 3, dark = true, highContrast = true))
    }

    @Test fun chapter_palettes_are_readable() {
        for (dark in listOf(true, false)) for (ch in listOf(2, 3)) {
            val c = chapterColors(colorSchemeFor(dark, false), ch, dark, highContrast = false)
            val tag = "cap $ch ${if (dark) "oscuro" else "claro"}"
            assertTrue("$tag secundario/fondo", contrastRatio(c.secondary, c.background) >= 4.5)
            assertTrue("$tag secundario/tarjeta", contrastRatio(c.secondary, c.surfaceContainerHighest) >= 3.0)
            assertTrue("$tag borde/fondo", contrastRatio(c.outline, c.background) >= 3.0)
            assertTrue("$tag texto sobre primario", contrastRatio(c.onPrimary, c.primary) >= 4.5)
            assertTrue("$tag primario/fondo", contrastRatio(c.primary, c.background) >= 3.0)
        }
    }

    @Test fun next_mission_ring_stands_out() {
        for (dark in listOf(true, false)) for (ch in 1..3) {
            val c = chapterColors(colorSchemeFor(dark, false), ch, dark, highContrast = false)
            assertTrue("cap $ch $dark", contrastRatio(nextRingColor(c), c.primary) >= 2.0)
        }
    }
}
