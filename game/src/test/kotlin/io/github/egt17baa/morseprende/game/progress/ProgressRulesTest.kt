package io.github.egt17baa.morseprende.game.progress

import io.github.egt17baa.morseprende.game.content.RealContent
import io.github.egt17baa.morseprende.game.content.TestContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressRulesTest {

    private val chapters = listOf(RealContent.chapter1)
    private val all = RealContent.chapter1.missions.map { it.id }.toSet()

    @Test fun first_mission_always_unlocked() = assertTrue(ProgressRules.isUnlocked("c1m1", chapters, emptySet()))

    @Test fun next_unlocks_after_previous_completed() {
        assertFalse(ProgressRules.isUnlocked("c1m2", chapters, emptySet()))
        assertTrue(ProgressRules.isUnlocked("c1m2", chapters, setOf("c1m1")))
    }

    @Test fun completing_out_of_order_does_not_skip() =
        assertFalse(ProgressRules.isUnlocked("c1m3", chapters, setOf("c1m2")))

    @Test fun completed_missions_stay_unlocked() =
        assertTrue(ProgressRules.isUnlocked("c1m1", chapters, setOf("c1m1", "c1m2")))

    @Test fun unknown_mission_is_locked() = assertFalse(ProgressRules.isUnlocked("zzz", chapters, all))

    @Test fun next_mission_is_first_incomplete() {
        assertEquals("c1m1", ProgressRules.nextMission(chapters, emptySet())?.id)
        assertEquals("c1r1", ProgressRules.nextMission(chapters, setOf("c1m1", "c1m2", "c1m3"))?.id)
        assertNull(ProgressRules.nextMission(chapters, all))
    }

    @Test fun cards_without_unlock_always_visible() =
        assertEquals(listOf("c1"), ProgressRules.unlockedCards(TestContent.cards, emptySet()).map { it.id })

    @Test fun cards_unlock_with_mission() =
        assertEquals(listOf("c1", "c2"), ProgressRules.unlockedCards(TestContent.cards, setOf("m2")).map { it.id })

    @Test fun chapter_progress_fraction() {
        assertEquals(0.25f, ProgressRules.chapterProgress(RealContent.chapter1, setOf("c1m1", "c1m2")), 1e-6f)
        assertEquals(0f, ProgressRules.chapterProgress(RealContent.chapter1, setOf("otra")), 1e-6f)
        assertEquals(1f, ProgressRules.chapterProgress(RealContent.chapter1, all), 1e-6f)
    }

    @Test fun learned_from_completed_only() {
        assertEquals(emptySet<Char>(), ProgressRules.learnedChars(chapters, emptySet()))
        assertEquals(setOf('S', 'O', 'E', 'T'), ProgressRules.learnedChars(chapters, setOf("c1m1", "c1m2")))
        assertEquals(setOf('S', 'O', 'E', 'T'), ProgressRules.learnedChars(chapters, setOf("c1m1", "c1m2", "c1r1")))
    }

    @Test fun game_unlocks() {
        assertEquals("c1m2", ProgressRules.gameUnlockMission("STORM"))
        assertEquals("c1m3", ProgressRules.gameUnlockMission("FLASH"))
        assertEquals("c1r1", ProgressRules.gameUnlockMission("INTERCEPT"))
        assertFalse(ProgressRules.isGameUnlocked("STORM", setOf("c1m1")))
        assertTrue(ProgressRules.isGameUnlocked("STORM", setOf("c1m1", "c1m2")))
        assertFalse(ProgressRules.isGameUnlocked("DESCONOCIDO", all))
    }

    private val withOptional = listOf(
        io.github.egt17baa.morseprende.game.content.Chapter(
            1, "T", "i", listOf("SOS"),
            listOf(
                io.github.egt17baa.morseprende.game.content.Mission("a", io.github.egt17baa.morseprende.game.content.MissionType.LESSON, "A", "b", listOf('S', 'O'), io.github.egt17baa.morseprende.game.content.StoryMessage("x", "SOS"), "d"),
                io.github.egt17baa.morseprende.game.content.Mission("x", io.github.egt17baa.morseprende.game.content.MissionType.LESSON, "X", "b", listOf('Ñ'), io.github.egt17baa.morseprende.game.content.StoryMessage("x", "Ñ"), "d", optional = true),
                io.github.egt17baa.morseprende.game.content.Mission("b", io.github.egt17baa.morseprende.game.content.MissionType.LESSON, "B", "b", listOf('E', 'T'), io.github.egt17baa.morseprende.game.content.StoryMessage("x", "SET"), "d"),
            ),
        ),
    )

    @Test fun optional_does_not_block_next() {
        assertTrue(ProgressRules.isUnlocked("b", withOptional, setOf("a")))
        assertTrue(ProgressRules.isUnlocked("x", withOptional, setOf("a")))
        assertEquals("b", ProgressRules.nextMission(withOptional, setOf("a"))?.id)
    }

    @Test fun progress_ignores_optional() {
        assertEquals(1f, ProgressRules.chapterProgress(withOptional.single(), setOf("a", "b")), 1e-6f)
        assertEquals(0.5f, ProgressRules.chapterProgress(withOptional.single(), setOf("a", "x")), 1e-6f)
    }

    @Test fun optional_counts_for_minigames_when_completed() {
        assertEquals(setOf('S', 'O'), ProgressRules.learnedChars(withOptional, setOf("a")))
        assertEquals(setOf('S', 'O', 'Ñ'), ProgressRules.learnedChars(withOptional, setOf("a", "x")))
    }
}
