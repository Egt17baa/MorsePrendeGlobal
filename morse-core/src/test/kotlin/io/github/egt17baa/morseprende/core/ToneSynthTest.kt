package io.github.egt17baa.morseprende.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ToneSynthTest {

    @Test fun samples_for_duration() = assertEquals(4800, ToneSynth.samplesFor(100, 48000))

    @Test fun length_matches_duration() =
        assertEquals(4800, ToneSynth.render(Signal(true, 100), 48000, 600, 1f).size)

    @Test fun off_is_silence() {
        val pcm = ToneSynth.render(Signal(false, 50), 48000, 600, 1f)
        assertEquals(2400, pcm.size)
        assertTrue(pcm.all { it.toInt() == 0 })
    }

    @Test fun starts_and_ends_near_zero() {
        val pcm = ToneSynth.render(Signal(true, 100), 48000, 600, 1f)
        assertTrue("first=${pcm.first()}", abs(pcm.first().toInt()) < 200)
        assertTrue("last=${pcm.last()}", abs(pcm.last().toInt()) < 200)
    }

    @Test fun peak_respects_volume() {
        val peak = ToneSynth.render(Signal(true, 100), 48000, 600, 0.5f).maxOf { abs(it.toInt()) }
        assertTrue("peak=$peak", peak <= 0.5 * 0.8 * Short.MAX_VALUE + 1)
        assertTrue("peak=$peak", peak >= 0.45 * 0.8 * Short.MAX_VALUE)
    }

    @Test fun frequency_is_correct() {
        val pcm = ToneSynth.render(Signal(true, 1000), 48000, 600, 1f)
        var risingCrossings = 0
        for (i in 1 until pcm.size) if (pcm[i - 1] < 0 && pcm[i] >= 0) risingCrossings++
        assertTrue("crossings=$risingCrossings", abs(risingCrossings - 600) <= 2)
    }

    @Test fun ramp_shorter_than_tone() =
        assertEquals(288, ToneSynth.render(Signal(true, 6), 48000, 600, 1f, rampMs = 5).size)

    @Test fun zero_volume_is_silent() =
        assertTrue(ToneSynth.render(Signal(true, 20), 48000, 600, 0f).all { it.toInt() == 0 })

    @Test fun soft_and_classic_differ_from_sine_but_keep_peak_and_ramps() {
        val sine = ToneSynth.render(Signal(true, 200), 48000, 600, 1f)
        for (timbre in listOf(Timbre.SOFT, Timbre.CLASSIC)) {
            val pcm = ToneSynth.render(Signal(true, 200), 48000, 600, 1f, timbre = timbre)
            assertEquals(sine.size, pcm.size)
            val peak = pcm.maxOf { abs(it.toInt()) }
            assertTrue("$timbre peak=$peak", peak <= 0.8 * Short.MAX_VALUE + 1 && peak >= 0.7 * Short.MAX_VALUE)
            assertTrue("$timbre first=${pcm.first()}", abs(pcm.first().toInt()) < 200)
            assertTrue("$timbre last=${pcm.last()}", abs(pcm.last().toInt()) < 200)
            val diff = pcm.indices.sumOf { abs(pcm[it] - sine[it]).toLong() }
            assertTrue("$timbre diff=$diff", diff > pcm.size * 300L)
        }
    }

    @Test fun timbre_samples_are_bounded() {
        for (timbre in Timbre.entries) {
            var p = 0.0
            repeat(10_000) {
                val v = timbre.sample(p)
                assertTrue("$timbre $v", v in -1.0001..1.0001)
                p += 0.01
            }
        }
    }
}
