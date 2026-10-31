package io.github.egt17baa.morseprende.game.games

import io.github.egt17baa.morseprende.game.review.CharOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StormGameTest {

    private fun game(chars: List<Char> = listOf('S'), seed: Int = 1) = StormGame(chars, emptyMap(), Random(seed), startAt = 0)

    @Test fun first_signal_spawns_immediately() {
        val g = game()
        assertEquals(listOf(StormEvent.Spawned('S')), g.tick(0))
        assertEquals(1, g.signals.size)
        assertEquals(7000L, g.signals.single().fallMs)
    }

    @Test fun spawn_interval_and_speed_shrink() {
        val g = game()
        g.tick(0)
        assertEquals(emptyList<StormEvent>(), g.tick(2499))
        assertEquals(listOf(StormEvent.Spawned('S')), g.tick(2500))
        assertEquals(6790L, g.signals.last().fallMs)
        // siguiente intervalo: 2500 * 0.97 = 2425
        assertEquals(emptyList<StormEvent>(), g.tick(2500 + 2424))
        assertEquals(listOf(StormEvent.Spawned('S')), g.tick(2500 + 2425))
    }

    @Test fun input_hits_lowest_matching_signal() {
        val g = game()
        g.tick(0)
        g.tick(2500)
        val oldest = g.signals.first().id
        assertEquals(listOf(StormEvent.Hit('S')), g.input('S', 3000))
        assertEquals(1, g.score)
        assertFalse(g.signals.any { it.id == oldest })
        assertEquals(listOf(CharOutcome('S', true)), g.outcomes)
    }

    @Test fun wrong_input_does_nothing() {
        val g = game(listOf('S', 'O'), seed = 3)
        g.tick(0)
        val c = g.signals.single().char
        val other = if (c == 'S') 'O' else 'S'
        assertEquals(emptyList<StormEvent>(), g.input(other, 100))
        assertEquals(0, g.score)
        assertEquals(1, g.signals.size)
    }

    @Test fun double_input_scores_once() {
        val g = game()
        g.tick(0)
        g.input('S', 100)
        assertEquals(emptyList<StormEvent>(), g.input('S', 101))
        assertEquals(1, g.score)
    }

    @Test fun signal_reaching_sea_costs_a_life() {
        val g = game()
        g.tick(0)
        val events = g.tick(7000)
        assertTrue(StormEvent.Splash('S') in events)
        assertEquals(StormGame.LIVES - 1, g.lives)
        assertEquals(CharOutcome('S', false), g.outcomes.first())
    }

    @Test fun three_splashes_end_the_game() {
        val g = game()
        var t = 0L
        while (!g.isOver && t < 120_000) {
            g.tick(t)
            t += 50
        }
        assertTrue(g.isOver)
        assertEquals(0, g.lives)
        val score = g.score
        val count = g.signals.size
        assertEquals(emptyList<StormEvent>(), g.input('S', t))
        assertEquals(emptyList<StormEvent>(), g.tick(t + 10_000))
        assertEquals(score, g.score)
        assertEquals(count, g.signals.size)
    }

    @Test fun progress_is_fraction_of_fall() {
        val s = FallingSignal(1, 'S', spawnedAt = 1000, fallMs = 4000)
        assertEquals(0.5f, s.progress(3000), 1e-6f)
    }

    @Test fun few_chars_work() {
        val g = game(listOf('S', 'O'), seed = 5)
        var t = 0L
        val seen = mutableSetOf<Char>()
        while (t < 60_000 && !g.isOver) {
            g.tick(t).filterIsInstance<StormEvent.Spawned>().forEach { seen += it.char }
            g.signals.firstOrNull()?.let { g.input(it.char, t) }
            t += 100
        }
        assertEquals(setOf('S', 'O'), seen)
        assertFalse(g.isOver)
    }
}
