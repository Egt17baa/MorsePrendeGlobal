package io.github.egt17baa.morseprende.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyerDecoderTest {

    private fun KeyerDecoder.key(pressAt: Long, releaseAt: Long): Char? {
        press(pressAt)
        return release(releaseAt)
    }

    @Test fun dot_and_dash_classification() {
        val k = KeyerDecoder(100)
        assertEquals('.', k.key(0, 80))
        assertEquals('-', k.key(200, 500))
        assertEquals(".-", k.pending)
    }

    @Test fun letter_ends_after_2_5_units() {
        val k = KeyerDecoder(100)
        k.key(0, 80)
        k.key(200, 500)
        assertNull(k.poll(700))
        assertEquals(KeyerEvent.LetterEnded(".-", 'A'), k.poll(760))
        assertEquals("", k.pending)
    }

    @Test fun unit_adapts() {
        val k = KeyerDecoder(100)
        k.key(0, 80)
        assertEquals(95L, k.unitMs)
        k.key(200, 500)
        assertEquals(96L, k.unitMs)
    }

    @Test fun word_ends_after_6_units() {
        val k = KeyerDecoder(100)
        k.key(0, 80)
        k.key(200, 500)
        k.poll(760)
        assertEquals(KeyerEvent.WordEnded, k.poll(1100))
        assertNull(k.poll(1200))
    }

    @Test fun no_word_event_before_any_letter() {
        val k = KeyerDecoder(100)
        assertNull(k.poll(10_000))
    }

    @Test fun bounce_ignored() {
        val k = KeyerDecoder(100)
        assertNull(k.key(0, 10))
        assertEquals("", k.pending)
        assertEquals(100L, k.unitMs)
    }

    @Test fun unit_is_clamped() {
        val k = KeyerDecoder(100)
        var t = 0L
        repeat(20) {
            k.key(t, t + 10_000)
            t += 20_000
            k.poll(t)
        }
        assertEquals(KeyerDecoder.MAX_UNIT_MS, k.unitMs)
        val fast = KeyerDecoder(100)
        t = 0L
        repeat(30) {
            fast.key(t, t + 16)
            t += 100
        }
        assertEquals(KeyerDecoder.MIN_UNIT_MS, fast.unitMs)
    }

    @Test fun slow_beginner_is_followed() {
        val k = KeyerDecoder(120)
        assertEquals('.', k.key(0, 160))
        assertEquals(130L, k.unitMs)
        assertEquals('-', k.key(400, 880))
        assertEquals(138L, k.unitMs)
        assertNull(k.poll(880 + 300))
        assertEquals(".-", k.pending)
    }

    @Test fun unknown_code_reports_null_char() {
        val k = KeyerDecoder(100)
        var t = 0L
        repeat(8) {
            k.key(t, t + 90)
            t += 180
        }
        assertEquals(KeyerEvent.LetterEnded("........", null), k.poll(t + 1000))
    }

    @Test fun two_button_mode() {
        val k = KeyerDecoder(100)
        k.appendElement('-')
        k.appendElement('-')
        assertEquals(KeyerEvent.LetterEnded("--", 'M'), k.commitLetter())
        assertNull(k.commitLetter())
    }

    @Test fun reset_clears() {
        val k = KeyerDecoder(100)
        k.key(0, 80)
        k.reset()
        assertEquals("", k.pending)
        assertNull(k.poll(5_000))
    }
}
