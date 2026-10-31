package io.github.egt17baa.morseprende.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertFailsWith

class MorseTimingTest {

    @Test fun standard_20wpm() {
        val t = MorseTiming(20)
        assertEquals(60L, t.unitMs)
        assertEquals(180L, t.charGapMs)
        assertEquals(420L, t.wordGapMs)
    }

    @Test fun farnsworth_18_10() {
        val t = MorseTiming(18, 10)
        assertEquals(67L, t.unitMs)
        assertEquals(621L, t.charGapMs)
        assertEquals(1449L, t.wordGapMs)
    }

    @Test fun signals_E_space_T() = assertEquals(
        listOf(Signal(true, 60), Signal(false, 420), Signal(true, 180)),
        MorseTiming(20).signalsFor(". / -"),
    )

    @Test fun signals_A() = assertEquals(
        listOf(Signal(true, 60), Signal(false, 60), Signal(true, 180)),
        MorseTiming(20).signalsFor(".-"),
    )

    @Test fun signals_letters_gap() = assertEquals(180L, MorseTiming(20).signalsFor(". .")[1].durationMs)

    @Test fun signals_empty_or_garbage() = assertEquals(emptyList<Signal>(), MorseTiming(20).signalsFor("  x / "))

    @Test fun signals_skip_invalid_tokens_between_valid_ones() = assertEquals(
        listOf(Signal(true, 60), Signal(false, 180), Signal(true, 180)),
        MorseTiming(20).signalsFor(". x -"),
    )

    @Test fun rejects_out_of_range() {
        assertFailsWith<IllegalArgumentException> { MorseTiming(4) }
        assertFailsWith<IllegalArgumentException> { MorseTiming(31) }
        assertFailsWith<IllegalArgumentException> { MorseTiming(10, 12) }
    }

    @Test fun paris_is_one_minute_at_wpm() {
        val wpm = 12
        val t = MorseTiming(wpm)
        val total = MorseTiming.totalMs(t.signalsFor(MorseCodec.encode("PARIS PARIS").morse)) + t.wordGapMs
        val expected = 2 * 60000.0 / wpm
        assertTrue("total=$total expected=$expected", abs(total - expected) <= expected * 0.02)
    }

    @Test fun signals_with_non_breaking_space() =
        assertEquals(3, MorseTiming(20).signalsFor(".\u00A0-").size)
}
