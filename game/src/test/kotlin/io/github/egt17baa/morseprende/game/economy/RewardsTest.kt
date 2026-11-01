package io.github.egt17baa.morseprende.game.economy

import io.github.egt17baa.morseprende.game.content.MissionType
import org.junit.Assert.assertEquals
import org.junit.Test

class RewardsTest {
    @Test fun first_completion_three_stars() = assertEquals(35, Rewards.forMission(MissionType.LESSON, previousBest = 0, stars = 3))
    @Test fun replay_same_stars_only_base() = assertEquals(5, Rewards.forMission(MissionType.LESSON, 2, 2))
    @Test fun replay_worse_only_base() = assertEquals(5, Rewards.forMission(MissionType.LESSON, 3, 1))
    @Test fun improvement_counts_new_stars() = assertEquals(25, Rewards.forMission(MissionType.LESSON, 1, 3))
    @Test fun review_bonus() = assertEquals(20, Rewards.forMission(MissionType.REVIEW, 0, 1))
    @Test fun finale_bonus() = assertEquals(20, Rewards.forMission(MissionType.FINALE, 0, 1))
    @Test fun game_points_capped() {
        assertEquals(7, Rewards.forGame(37, isRecord = false))
        assertEquals(10, Rewards.forGame(200, isRecord = false))
    }
    @Test fun record_bonus_needs_points() {
        assertEquals(17, Rewards.forGame(12, isRecord = true))
        assertEquals(0, Rewards.forGame(0, isRecord = true))
    }
}
