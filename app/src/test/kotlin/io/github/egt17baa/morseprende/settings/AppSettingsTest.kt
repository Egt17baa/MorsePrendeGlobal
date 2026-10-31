package io.github.egt17baa.morseprende.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test fun clamps_out_of_range_values() {
        val s = AppSettings(charWpm = 99, effectiveWpm = 50, toneHz = 100, volume = 3f).clamped()
        assertEquals(30, s.charWpm)
        assertEquals(30, s.effectiveWpm)
        assertEquals(400, s.toneHz)
        assertEquals(1f, s.volume)
    }

    @Test fun clamps_low_values() {
        val s = AppSettings(charWpm = 1, effectiveWpm = 0, toneHz = 5000, volume = -1f).clamped()
        assertEquals(5, s.charWpm)
        assertEquals(5, s.effectiveWpm)
        assertEquals(1000, s.toneHz)
        assertEquals(0f, s.volume)
    }

    @Test fun effective_never_exceeds_char() =
        assertEquals(12, AppSettings(charWpm = 12, effectiveWpm = 20).clamped().effectiveWpm)

    @Test fun defaults_are_already_valid() = assertEquals(AppSettings(), AppSettings().clamped())
}
