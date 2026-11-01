package io.github.egt17baa.morseprende.ui.stats

import io.github.egt17baa.morseprende.data.FakeActivityRepository
import io.github.egt17baa.morseprende.data.FakeCharStatsRepository
import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeGameRecordsRepository
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.data.daily.FakeDailyRepository
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.data.games.GameRecord
import io.github.egt17baa.morseprende.game.activity.Streak
import io.github.egt17baa.morseprende.game.review.Mastery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @After fun tearDown() = Dispatchers.resetMain()

    private val today = LocalDate.of(2026, 11, 1)
    private val days = setOf(today, today.minusDays(1), today.minusDays(5))

    private fun TestScope.vm(
        progress: FakeProgressRepository = FakeProgressRepository.completed("c1m1" to 3, "c1m2" to 2, "c1m3" to 1),
        boxes: Map<Char, Int> = emptyMap(),
        records: Map<GameId, GameRecord> = emptyMap(),
        daily: Set<LocalDate> = emptySet(),
        played: Set<LocalDate> = emptySet(),
    ): StatsViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return StatsViewModel(
            FakeContentSource(listOf(FakeContentSource.realChapter1)), progress, FakeCharStatsRepository(boxes),
            FakeGameRecordsRepository(records), FakeDailyRepository(daily), FakeActivityRepository(played), { today },
        )
    }

    @Test fun stats_summary_counts() = runTest {
        val storm = GameRecord(GameId.STORM, 21, 0, 4)
        val s = vm(records = mapOf(GameId.STORM to storm), daily = setOf(today, today.minusDays(2)), played = days).also { runCurrent() }.state.value
        assertTrue(s.loaded)
        assertEquals(3, s.missionsDone)
        assertEquals(FakeContentSource.realChapter1.missions.size, s.missionsTotal)
        assertEquals(6, s.stars)
        assertEquals(FakeContentSource.realChapter1.missions.size * 3, s.starsMax)
        assertEquals(2, s.dailyDone)
        assertEquals(3, s.daysPlayed)
        assertEquals(Streak.current(days, today), s.streak)
        assertEquals(listOf(storm), s.records)
    }

    @Test fun stats_letters_levels() = runTest {
        val s = vm(boxes = mapOf('S' to 5, 'O' to 1, 'E' to 3, 'T' to 2)).also { runCurrent() }.state.value
        assertEquals(listOf('S', 'O', 'E', 'T', 'A', 'N'), s.letters.map { it.char })
        assertEquals(
            listOf(Mastery.Level.MASTERED, Mastery.Level.LEARNING, Mastery.Level.GOOD, Mastery.Level.LEARNING, Mastery.Level.NO_DATA, Mastery.Level.NO_DATA),
            s.letters.map { it.level },
        )
        // Solo las que siguen «aprendiendo»; E ya va bien.
        assertEquals(listOf('O', 'T'), s.weakest)
    }

    @Test fun stats_content_error() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val v = StatsViewModel(
            FakeContentSource(emptyList(), fail = true), FakeProgressRepository(), FakeCharStatsRepository(),
            FakeGameRecordsRepository(), null, FakeActivityRepository(), { today },
        )
        runCurrent()
        assertTrue(v.state.value.error)
    }

    @Test fun stats_new_player() = runTest {
        val s = vm(progress = FakeProgressRepository()).also { runCurrent() }.state.value
        assertTrue(s.loaded)
        assertEquals(0, s.missionsDone)
        assertTrue(s.letters.isEmpty())
        assertTrue(s.weakest.isEmpty())
        assertTrue(s.records.isEmpty())
    }
}
