package io.github.egt17baa.morseprende.game.achievements

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AchievementsTest {
    private val ids = listOf("c1m1", "c1m2", "c1f")
    private val base = AchievementInput(emptyMap(), ids, 0, 0, 0, 0, 0, 0, 0, 7)
    private fun earned(i: AchievementInput) = Achievements.earned(i)

    @Test fun ids_in_order() = assertEquals(
        listOf("first_sos", "three_flashes", "keeper", "perfectionist", "storm_rider", "quick_eye", "sharp_ear", "speed_20", "steady", "collector", "reader", "mole_hunter"),
        Achievements.ids,
    )
    @Test fun none_initially() = assertEquals(emptySet<String>(), earned(base))
    @Test fun first_sos() = assertTrue("first_sos" in earned(base.copy(missionStars = mapOf("c1m1" to 1))))
    @Test fun three_flashes() {
        assertTrue("three_flashes" in earned(base.copy(missionStars = mapOf("c1m2" to 3))))
        assertFalse("three_flashes" in earned(base.copy(missionStars = mapOf("c1m2" to 2))))
    }
    @Test fun keeper() = assertTrue("keeper" in earned(base.copy(missionStars = mapOf("c1f" to 1))))
    @Test fun perfectionist() {
        assertTrue("perfectionist" in earned(base.copy(missionStars = ids.associateWith { 3 })))
        assertFalse("perfectionist" in earned(base.copy(missionStars = mapOf("c1m1" to 3, "c1m2" to 3, "c1f" to 2))))
    }
    @Test fun storm_rider() {
        assertTrue("storm_rider" in earned(base.copy(stormBest = 20)))
        assertFalse("storm_rider" in earned(base.copy(stormBest = 19)))
    }
    @Test fun quick_eye() {
        assertTrue("quick_eye" in earned(base.copy(flashBestReactionMs = 800)))
        assertFalse("quick_eye" in earned(base.copy(flashBestReactionMs = 801)))
        assertFalse("quick_eye" in earned(base.copy(flashBestReactionMs = 0)))
    }
    @Test fun sharp_ear() = assertTrue("sharp_ear" in earned(base.copy(interceptBest = 10)))
    @Test fun speed_20() {
        assertTrue("speed_20" in earned(base.copy(interceptMaxWpm = 20)))
        assertFalse("speed_20" in earned(base.copy(interceptMaxWpm = 19)))
    }
    @Test fun steady() = assertTrue("steady" in earned(base.copy(streak = 7)))
    @Test fun collector() {
        assertTrue("collector" in earned(base.copy(boughtItems = 3)))
        assertFalse("collector" in earned(base.copy(boughtItems = 2)))
    }
    @Test fun reader_needs_all_cards() {
        assertTrue("reader" in earned(base.copy(seenCards = 7)))
        assertFalse("reader" in earned(base.copy(seenCards = 6)))
        assertFalse("reader" in earned(base.copy(seenCards = 0, totalCards = 0)))
    }

    @Test fun mole_hunter() {
        assertTrue("mole_hunter" in earned(base.copy(missionStars = mapOf("c2f" to 1))))
        assertFalse("mole_hunter" in earned(base.copy(missionStars = mapOf("c1f" to 3))))
    }
}
