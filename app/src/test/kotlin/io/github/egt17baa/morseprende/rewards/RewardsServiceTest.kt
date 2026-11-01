package io.github.egt17baa.morseprende.rewards

import io.github.egt17baa.morseprende.data.FakeAchievementsRepository
import io.github.egt17baa.morseprende.data.FakeActivityRepository
import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeEconomyRepository
import io.github.egt17baa.morseprende.data.FakeGameRecordsRepository
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.data.FakeSeenCardsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RewardsServiceTest {

    private val today = LocalDate.of(2026, 11, 5)
    private val chapter = FakeContentSource.realChapter1
    private val c1m1 = chapter.missions.first()

    private class Env(
        val service: RewardsService,
        val progress: FakeProgressRepository,
        val economy: FakeEconomyRepository,
        val activity: FakeActivityRepository,
        val achievements: FakeAchievementsRepository,
    )

    private fun env(days: Set<LocalDate> = emptySet()): Env {
        val progress = FakeProgressRepository()
        val economy = FakeEconomyRepository()
        val activity = FakeActivityRepository(days)
        val achievements = FakeAchievementsRepository()
        val service = RewardsService(
            FakeContentSource(listOf(chapter)), progress, FakeGameRecordsRepository(), FakeSeenCardsRepository(),
            economy, activity, achievements, today = { today }, now = { 1234L },
        )
        return Env(service, progress, economy, activity, achievements)
    }

    @Test fun mission_rewards_and_daily_bonus() = runTest {
        val e = env()
        e.progress.recordResult("c1m1", 3, 0)
        val first = e.service.onMissionCompleted(c1m1, previousBest = 0, stars = 3)
        assertEquals(35 + 5, first.destellos)
        assertTrue(first.dailyBonus)
        val second = e.service.onMissionCompleted(c1m1, previousBest = 3, stars = 3)
        assertEquals(5, second.destellos)
        assertFalse(second.dailyBonus)
        assertEquals(40 + 5, e.economy.balance.value)
    }

    @Test fun first_sos_achievement_once() = runTest {
        val e = env()
        e.progress.recordResult("c1m1", 2, 0)
        assertEquals(listOf("first_sos"), e.service.onMissionCompleted(c1m1, 0, 2).newAchievements)
        assertEquals(emptyList<String>(), e.service.onMissionCompleted(c1m1, 2, 2).newAchievements)
    }

    @Test fun game_rewards() = runTest {
        val e = env()
        val r = e.service.onGameFinished(score = 12, isRecord = true)
        assertEquals(2 + 15 + 5, r.destellos)
    }

    @Test fun streak_achievement() = runTest {
        val e = env(days = (1L..6L).map { today.minusDays(it) }.toSet())
        val r = e.service.onGameFinished(score = 0, isRecord = false)
        assertTrue("steady" in r.newAchievements)
    }

    @Test fun refresh_achievements_without_activity() = runTest {
        val e = env()
        e.economy.earn(200)
        e.economy.buy("decor_gulls"); e.economy.buy("decor_flags"); e.economy.buy("lamp_emerald")
        assertEquals(listOf("collector"), e.service.refreshAchievements())
        assertEquals(emptySet<LocalDate>(), e.activity.playedDays.value)
    }
}
