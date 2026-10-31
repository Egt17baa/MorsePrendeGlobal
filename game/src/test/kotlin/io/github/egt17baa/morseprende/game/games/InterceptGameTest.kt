package io.github.egt17baa.morseprende.game.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random
import kotlin.test.assertFailsWith

class InterceptGameTest {

    private val words = listOf("SOS", "OSO", "ESTO", "NOTA", "MAR", "CASA")
    private val learned = listOf('S', 'O', 'E', 'T', 'N', 'A')

    private fun game(startWpm: Int = 10, ws: List<String> = words, chars: List<Char> = learned) =
        InterceptGame(ws, chars, startWpm, Random(2))

    @Test fun only_words_with_learned_letters() {
        val g = game()
        repeat(30) {
            assertTrue(g.currentWord, g.currentWord.all { it in learned })
            g.answer(g.currentWord)
        }
    }

    @Test fun speed_increases_every_two_correct() {
        val g = game(startWpm = 10)
        g.answer(g.currentWord)
        assertEquals(10, g.wpm)
        g.answer(g.currentWord)
        assertEquals(11, g.wpm)
        g.answer("X")
        g.answer(g.currentWord)
        assertEquals(11, g.wpm)
        g.answer(g.currentWord)
        assertEquals(12, g.wpm)
        assertEquals(12, g.maxWpm)
        assertEquals(4, g.score)
    }

    @Test fun speed_caps_at_30() {
        val g = game(startWpm = 29)
        repeat(10) { g.answer(g.currentWord) }
        assertEquals(30, g.wpm)
    }

    @Test fun three_strikes_over() {
        val g = game()
        repeat(InterceptGame.STRIKES) { g.answer("nada") }
        assertTrue(g.isOver)
        assertEquals(3, g.strikes)
    }

    @Test fun lenient_matching() {
        val g = game()
        val w = g.currentWord
        assertTrue(g.answer("  ${w.lowercase()} ").correct)
    }

    @Test fun no_immediate_repeat() {
        val g = game()
        repeat(50) {
            val before = g.currentWord
            g.answer(before)
            assertNotEquals(before, g.currentWord)
        }
    }

    @Test fun fallback_combos_without_words() {
        val g = game(ws = emptyList(), chars = listOf('S', 'O'))
        assertTrue(g.currentWord.length in 2..3)
        assertTrue(g.currentWord.all { it == 'S' || it == 'O' })
    }

    @Test fun answer_after_over_throws() {
        val g = game()
        repeat(3) { g.answer("x") }
        assertFailsWith<IllegalStateException> { g.answer("x") }
    }

    @Test fun outcomes_recorded() {
        val g = game()
        val w = g.currentWord
        g.answer(w)
        assertEquals(w.length, g.outcomes.size)
        assertTrue(g.outcomes.all { it.correct })
    }
}
