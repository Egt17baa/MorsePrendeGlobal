package io.github.egt17baa.morseprende.game.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class NightWatchGameTest {

    private val chars = ('A'..'Z').toList() + ('0'..'9').toList()
    private val words = listOf("FARO", "SOS", "MAR", "LUZ", "ECO")
    private fun game(vibration: Boolean = true, seed: Int = 1) = NightWatchGame(words, chars, startWpm = 12, random = Random(seed), vibrationAllowed = vibration)

    @Test fun night_hour_advances_every_5() {
        val g = game()
        assertEquals(0, g.hour)
        assertEquals("22:00", NightWatchGame.clock(g.hour))
        repeat(4) { g.answer(g.current.text) }
        assertEquals(0, g.hour)
        g.answer(g.current.text)
        assertEquals(1, g.hour)
        assertEquals("23:00", NightWatchGame.clock(1))
        assertEquals("00:00", NightWatchGame.clock(2))
        assertEquals("06:00", NightWatchGame.clock(NightWatchGame.DAWN_HOUR))
        assertEquals(14, g.wpm)
    }

    @Test fun night_speed_and_noise_capped() {
        val g = game()
        repeat(200) { g.answer(g.current.text) }
        assertEquals(30, g.wpm)
        assertEquals(0.35f, g.noise, 1e-6f)
        assertFalse(g.isOver)
    }

    @Test fun night_channels_pattern() {
        val g = game()
        val channels = (0 until 12).map { g.current.channel.also { _ -> g.answer(g.current.text) } }
        assertEquals(NightChannel.LIGHT, channels[3])
        assertEquals(NightChannel.VIBRATION, channels[5])
        assertEquals(NightChannel.LIGHT, channels[7])
        assertEquals(NightChannel.VIBRATION, channels[11])
        assertEquals(NightChannel.SOUND, channels[0])
        assertTrue(game(vibration = false).let { n -> (0 until 24).none { n.current.channel == NightChannel.VIBRATION.also { n.answer(n.current.text) } } })
    }

    @Test fun night_three_strikes_end() {
        val g = game()
        g.answer("???"); g.answer("???")
        assertFalse(g.isOver)
        g.answer("???")
        assertTrue(g.isOver)
        assertEquals(0, g.score)
        assertEquals(3, g.strikes)
    }

    @Test fun night_uses_only_learned_chars() {
        val g = NightWatchGame(listOf("FARO", "SOS", "OSO"), setOf('S', 'O'), 12, Random(3), true)
        repeat(40) {
            assertTrue(g.current.text, g.current.text.all { it in setOf('S', 'O') })
            g.answer(g.current.text)
        }
    }
}
