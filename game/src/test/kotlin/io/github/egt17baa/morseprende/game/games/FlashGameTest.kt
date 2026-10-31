package io.github.egt17baa.morseprende.game.games

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FlashGameTest {

    private val letters = listOf('S', 'O', 'E', 'T', 'A', 'N')
    private fun game(chars: List<Char> = letters) = FlashGame(chars, emptyMap(), Random(4))

    @Test fun round_flow() {
        val g = game()
        assertEquals(1, g.round)
        assertEquals(FlashPhase.SHOWING, g.phase)
        assertTrue(g.current in letters)
        assertTrue(g.options.isEmpty())
        g.flashFinished(1000)
        assertEquals(FlashPhase.ANSWERING, g.phase)
        assertEquals(4, g.options.size)
        assertTrue(g.current in g.options)
        assertEquals(4, g.options.toSet().size)
    }

    @Test fun fast_answer_scores_more() {
        val g = game()
        g.flashFinished(1000)
        assertTrue(g.answer(g.current!!, 1500))
        assertEquals(75, g.score)
        g.flashFinished(5000)
        assertTrue(g.answer(g.current!!, 7500))
        assertEquals(75, g.score)
        assertEquals((500 + 2500) / 2, g.averageReactionMs)
    }

    @Test fun wrong_answer_zero() {
        val g = game()
        g.flashFinished(0)
        val wrong = g.options.first { it != g.current }
        assertFalse(g.answer(wrong, 100))
        assertEquals(0, g.score)
        assertEquals(2, g.round)
        assertEquals(0, g.averageReactionMs)
    }

    @Test fun answer_during_showing_ignored() {
        val g = game()
        assertFalse(g.answer(g.current!!, 10))
        assertEquals(1, g.round)
        assertEquals(FlashPhase.SHOWING, g.phase)
    }

    @Test fun double_answer_counts_once() {
        val g = game()
        g.flashFinished(0)
        val c = g.current!!
        g.answer(c, 100)
        assertFalse(g.answer(c, 101))
        assertEquals(2, g.round)
        assertEquals(1, g.outcomes.size)
    }

    @Test fun ten_rounds_then_over() {
        val g = game()
        repeat(FlashGame.ROUNDS) { i ->
            g.flashFinished(i * 10_000L)
            g.answer(g.current!!, i * 10_000L + 300)
        }
        assertTrue(g.isOver)
        assertEquals(FlashPhase.OVER, g.phase)
        assertNull(g.current)
        assertEquals(10 * 85, g.score)
        assertEquals(FlashGame.ROUNDS, g.outcomes.size)
    }

    @Test fun options_valid_with_two_letters() {
        val g = game(listOf('S', 'O'))
        g.flashFinished(0)
        assertEquals(setOf('S', 'O'), g.options.toSet())
    }
}
