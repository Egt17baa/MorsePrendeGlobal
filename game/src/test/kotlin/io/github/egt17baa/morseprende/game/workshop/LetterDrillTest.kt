package io.github.egt17baa.morseprende.game.workshop

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LetterDrillTest {

    @Test fun drill_never_repeats_consecutively() {
        val drill = LetterDrill(listOf('E', 'T'), Random(3))
        var previous = drill.current
        repeat(50) {
            drill.next()
            assertNotEquals(previous, drill.current)
            previous = drill.current
        }
    }

    @Test fun drill_uses_only_selected_letters() {
        val drill = LetterDrill(listOf('K', 'M', 'R'), Random(1))
        val seen = mutableSetOf(drill.current)
        repeat(60) { drill.next(); seen += drill.current }
        assertEquals(setOf('K', 'M', 'R'), seen)
    }

    @Test fun drill_needs_two_letters() {
        assertFalse(LetterDrill.canStart(setOf('E')))
        assertTrue(LetterDrill.canStart(setOf('E', 'T')))
    }

    @Test fun drill_counts_per_letter() {
        val drill = LetterDrill(listOf('E', 'T'), Random(2))
        val first = drill.current
        assertTrue(drill.answer(first))
        drill.next()
        val second = drill.current
        assertFalse(drill.answer(if (second == 'E') 'T' else 'E'))
        assertEquals(LetterDrill.Score(1, 1), drill.stats[first])
        assertEquals(LetterDrill.Score(0, 1), drill.stats[second])
    }

    @Test fun drill_defaults_last_learned() {
        assertEquals(setOf('A', 'N', 'I', 'M'), LetterDrill.defaultSelection(listOf('S', 'O', 'E', 'T', 'A', 'N', 'I', 'M')))
        assertEquals(setOf('S', 'O'), LetterDrill.defaultSelection(listOf('S', 'O')))
    }
}
