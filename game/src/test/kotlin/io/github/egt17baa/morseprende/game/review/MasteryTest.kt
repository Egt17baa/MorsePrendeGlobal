package io.github.egt17baa.morseprende.game.review

import org.junit.Assert.assertEquals
import org.junit.Test

class MasteryTest {

    @Test fun mastery_levels() {
        assertEquals(Mastery.Level.NO_DATA, Mastery.level(null))
        assertEquals(Mastery.Level.LEARNING, Mastery.level(1))
        assertEquals(Mastery.Level.LEARNING, Mastery.level(2))
        assertEquals(Mastery.Level.GOOD, Mastery.level(3))
        assertEquals(Mastery.Level.GOOD, Mastery.level(4))
        assertEquals(Mastery.Level.MASTERED, Mastery.level(5))
        assertEquals(listOf(0, 1, 2, 3), Mastery.Level.entries.map { it.marks })
    }

    @Test fun weakest_lowest_boxes_recent_first() {
        val learned = listOf('S', 'O', 'E', 'T', 'A', 'N')
        val boxes = mapOf('S' to 5, 'O' to 1, 'E' to 2, 'T' to 1, 'A' to 4, 'N' to 2)
        // Caja 1: T (más reciente) y O; caja 2: N y E. Las que no tienen datos no cuentan.
        assertEquals(listOf('T', 'O', 'N', 'E'), Mastery.weakest(learned, boxes))
        assertEquals(listOf('T', 'O'), Mastery.weakest(learned + 'I', mapOf('O' to 1, 'T' to 1)))
    }

    @Test fun weakest_needs_two() {
        assertEquals(emptyList<Char>(), Mastery.weakest(listOf('S', 'O'), mapOf('S' to 1)))
        assertEquals(emptyList<Char>(), Mastery.weakest(listOf('S', 'O'), emptyMap()))
        // Todas dominadas o «bien»: nada que señalar como difícil.
        assertEquals(emptyList<Char>(), Mastery.weakest(listOf('S', 'O'), mapOf('S' to 5, 'O' to 5)))
        assertEquals(emptyList<Char>(), Mastery.weakest(listOf('S', 'O', 'E'), mapOf('S' to 4, 'O' to 3, 'E' to 1)))
    }
}
