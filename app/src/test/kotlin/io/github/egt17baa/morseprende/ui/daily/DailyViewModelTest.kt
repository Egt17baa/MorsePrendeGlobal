package io.github.egt17baa.morseprende.ui.daily

import io.github.egt17baa.morseprende.data.FakeAchievementsRepository
import io.github.egt17baa.morseprende.data.FakeActivityRepository
import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeEconomyRepository
import io.github.egt17baa.morseprende.data.FakeGameRecordsRepository
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.data.FakeSeenCardsRepository
import io.github.egt17baa.morseprende.data.daily.FakeDailyRepository
import io.github.egt17baa.morseprende.game.economy.Rewards
import io.github.egt17baa.morseprende.rewards.RewardsService
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
import io.github.egt17baa.morseprende.translator.FakeOutputFactory
import io.github.egt17baa.morseprende.ui.mission.MissionTestContent
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class DailyViewModelTest {

    @After fun tearDown() = Dispatchers.resetMain()

    private val day = LocalDate.of(2026, 11, 5)
    private val economy = FakeEconomyRepository()

    private fun TestScope.vm(
        daily: FakeDailyRepository,
        progress: FakeProgressRepository = FakeProgressRepository.completed("t1" to 3),
        today: LocalDate = day,
    ): DailyViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val content = FakeContentSource(listOf(MissionTestContent.chapter), daily = listOf("SOS", "OSO", "FARO"))
        val rewards = RewardsService(
            content, progress, FakeGameRecordsRepository(), FakeSeenCardsRepository(), economy,
            FakeActivityRepository(), FakeAchievementsRepository(), today = { today }, now = { 1L },
        )
        return DailyViewModel(content, progress, daily, FakeSettingsRepository(), FakeOutputFactory(this), rewards, { today }, backgroundScope)
    }

    @Test fun daily_uses_learned_letters_and_plays() = runTest {
        val v = vm(FakeDailyRepository())
        runCurrent()
        assertEquals(DailyPhase.READY, v.state.value.phase)
        assertTrue(v.state.value.message in setOf("SOS", "OSO"))
        v.play()
        runCurrent()
        assertTrue(v.state.value.isPlaying)
    }

    @Test fun daily_locked_without_letters() = runTest {
        val v = vm(FakeDailyRepository(), progress = FakeProgressRepository())
        runCurrent()
        assertEquals(DailyPhase.LOCKED, v.state.value.phase)
    }

    @Test fun daily_rewards_once_per_day() = runTest {
        val daily = FakeDailyRepository()
        val v = vm(daily)
        runCurrent()
        v.answer(v.state.value.message!!.lowercase())
        runCurrent()
        assertEquals(DailyPhase.DONE, v.state.value.phase)
        assertNotNull(v.state.value.reward)
        val first = economy.balance.value
        assertTrue(first >= Rewards.DAILY_CHALLENGE)
        // Otra vez el mismo día (pantalla reabierta): ya hecho, sin pagar.
        val again = vm(daily)
        runCurrent()
        assertEquals(DailyPhase.DONE, again.state.value.phase)
        again.answer(again.state.value.message!!)
        runCurrent()
        assertEquals(first, economy.balance.value)
        assertNull(again.state.value.reward)
        assertEquals(1, again.state.value.totalDone)
        // Al día siguiente hay reto nuevo.
        val tomorrow = vm(daily, today = day.plusDays(1))
        runCurrent()
        assertEquals(DailyPhase.READY, tomorrow.state.value.phase)
    }

    @Test fun daily_hint_after_failures() = runTest {
        val v = vm(FakeDailyRepository())
        runCurrent()
        v.answer("XYZ")
        assertTrue(v.state.value.lastWrong)
        assertNull(v.state.value.hint)
        v.answer("XYZ")
        assertNotNull(v.state.value.hint)
        assertEquals(DailyPhase.READY, v.state.value.phase)
        v.answer("XYZ"); v.answer("XYZ")
        assertEquals(io.github.egt17baa.morseprende.game.daily.DailyChallenge.hint(v.state.value.message!!, 2), v.state.value.hint)
    }
}
