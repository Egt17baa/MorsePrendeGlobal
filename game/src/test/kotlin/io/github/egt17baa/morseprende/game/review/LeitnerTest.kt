package io.github.egt17baa.morseprende.game.review

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LeitnerTest {

    @Test fun next_rules() {
        assertEquals(2, Leitner.next(1, true))
        assertEquals(5, Leitner.next(5, true))
        assertEquals(1, Leitner.next(4, false))
    }

    @Test fun apply_accumulates() {
        val after = Leitner.apply(mapOf('S' to 2), listOf(CharOutcome('S', true), CharOutcome('S', false), CharOutcome('O', true)))
        assertEquals(1, after['S'])
        assertEquals(2, after['O'])
    }

    @Test fun weights() {
        assertEquals(5, Leitner.weight(1))
        assertEquals(1, Leitner.weight(5))
    }

    @Test fun pick_prefers_low_boxes() {
        val random = Random(1)
        val boxes = mapOf('S' to 1, 'O' to 5)
        val s = (1..6000).count { Leitner.pick(listOf('S', 'O'), boxes, random) == 'S' }
        assertTrue("s=$s", s in 4680..5280)
    }

    @Test fun pick_unknown_char_counts_as_box_one() {
        val random = Random(2)
        val e = (1..6000).count { Leitner.pick(listOf('E', 'O'), mapOf('O' to 5), random) == 'E' }
        assertTrue("e=$e", e in 4680..5280)
    }

    @Test fun pick_single_char() = assertEquals('S', Leitner.pick(listOf('S'), emptyMap(), Random(3)))

    @Test fun outcomes_by_position() {
        assertEquals(listOf(CharOutcome('S', true), CharOutcome('O', true), CharOutcome('S', true)), Leitner.outcomesFor("SOS", "sos"))
        assertEquals(listOf(CharOutcome('S', true), CharOutcome('O', true), CharOutcome('S', false)), Leitner.outcomesFor("SOS", "SOT"))
        assertEquals(listOf(CharOutcome('S', false), CharOutcome('O', false), CharOutcome('S', false)), Leitner.outcomesFor("SOS", ""))
        assertEquals(listOf(CharOutcome('E', true), CharOutcome('S', true)), Leitner.outcomesFor("E S", "e s"))
        assertEquals(listOf(CharOutcome('A', true)), Leitner.outcomesFor("Á", "a"))
    }
}
