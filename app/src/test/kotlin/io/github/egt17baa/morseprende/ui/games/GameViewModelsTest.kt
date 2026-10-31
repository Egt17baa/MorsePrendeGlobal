package io.github.egt17baa.morseprende.ui.games

import io.github.egt17baa.morseprende.data.FakeCharStatsRepository
import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeGameRecordsRepository
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
import io.github.egt17baa.morseprende.translator.FakeOutputFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelsTest {

    @After fun tearDown() = Dispatchers.resetMain()

    /** Completadas c1m1..c1r1: letras S O E T A N. */
    private val completed = arrayOf("c1m1" to 2, "c1m2" to 2, "c1m3" to 2, "c1r1" to 2)
    private val learned = setOf('S', 'O', 'E', 'T', 'A', 'N')

    private class Deps(
        val records: FakeGameRecordsRepository = FakeGameRecordsRepository(),
        val stats: FakeCharStatsRepository = FakeCharStatsRepository(),
        val clock: LongArray = longArrayOf(0),
    )

    private fun TestScope.deps(): Deps {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return Deps()
    }

    private fun TestScope.storm(d: Deps) = StormViewModel(
        GameDeps(
            FakeContentSource(listOf(FakeContentSource.realChapter1)), FakeProgressRepository.completed(*completed),
            d.stats, d.records, FakeSettingsRepository(AppSettings()), FakeOutputFactory(this),
            Random(1), backgroundScope, { d.clock[0] },
        ),
    )

    private fun TestScope.flash(d: Deps, settings: AppSettings = AppSettings()) = FlashViewModel(
        GameDeps(
            FakeContentSource(listOf(FakeContentSource.realChapter1)), FakeProgressRepository.completed(*completed),
            d.stats, d.records, FakeSettingsRepository(settings), FakeOutputFactory(this),
            Random(1), backgroundScope, { d.clock[0] },
        ),
    )

    private fun TestScope.intercept(d: Deps) = InterceptViewModel(
        GameDeps(
            FakeContentSource(listOf(FakeContentSource.realChapter1)), FakeProgressRepository.completed(*completed),
            d.stats, d.records, FakeSettingsRepository(AppSettings(charWpm = 12)), FakeOutputFactory(this),
            Random(1), backgroundScope, { d.clock[0] },
        ),
    )

    // ---- Tormenta ----

    @Test fun storm_uses_only_learned_chars() = runTest {
        val d = deps()
        val vm = storm(d)
        assertEquals(learned, vm.state.value.letters.toSet())
        vm.start()
        repeat(200) { d.clock[0] += 200; vm.frame() }
        assertTrue(vm.state.value.signals.all { it.char in learned })
    }

    @Test fun storm_game_over_saves_once() = runTest {
        val d = deps()
        val vm = storm(d)
        vm.start()
        while (vm.state.value.phase != GamePhase.OVER && d.clock[0] < 300_000) { d.clock[0] += 100; vm.frame() }
        runCurrent()
        assertEquals(GamePhase.OVER, vm.state.value.phase)
        repeat(10) { d.clock[0] += 100; vm.frame() }
        runCurrent()
        assertEquals(1, d.records.games.size)
        assertEquals(GameId.STORM, d.records.games.single().first)
        assertEquals(1, d.stats.recorded.size)
        assertTrue(vm.state.value.isNewRecord)
    }

    @Test fun storm_hit_scores() = runTest {
        val d = deps()
        val vm = storm(d)
        vm.start()
        vm.frame()
        val c = vm.state.value.signals.first().char
        vm.input(c)
        assertEquals(1, vm.state.value.score)
    }

    @Test fun storm_abandon_saves_nothing() = runTest {
        val d = deps()
        val vm = storm(d)
        vm.start()
        repeat(5) { d.clock[0] += 100; vm.frame() }
        vm.abandon()
        runCurrent()
        assertEquals(GamePhase.READY, vm.state.value.phase)
        assertEquals(emptyList<Any>(), d.records.games)
        assertEquals(emptyList<Any>(), d.stats.recorded)
    }

    // ---- Destello rápido ----

    @Test fun flash_round_then_options_and_over_saves_once() = runTest {
        val d = deps()
        val vm = flash(d)
        vm.start()
        runCurrent()
        assertEquals(GamePhase.PLAYING, vm.state.value.phase)
        var rounds = 0
        while (vm.state.value.phase == GamePhase.PLAYING && rounds < 20) {
            advanceTimeBy(30_000)
            runCurrent()
            val s = vm.state.value
            assertTrue(s.options.isNotEmpty())
            assertTrue(s.options.all { it in learned })
            d.clock[0] += 400
            vm.answer(s.answer!!)
            runCurrent()
            rounds++
        }
        assertEquals(10, rounds)
        assertEquals(GamePhase.OVER, vm.state.value.phase)
        runCurrent()
        assertEquals(1, d.records.games.size)
        assertEquals(GameId.FLASH, d.records.games.single().first)
        assertEquals(400, d.records.games.single().third)
    }

    @Test fun flash_lamp_capped_when_limiting() = runTest {
        val d = deps()
        val vm = flash(d, AppSettings(charWpm = 20, effectiveWpm = 20, limitFlashes = true))
        vm.start()
        runCurrent()
        assertEquals(7, vm.state.value.flashWpm)
        val free = flash(Deps(), AppSettings(charWpm = 20, effectiveWpm = 20, limitFlashes = false))
        assertEquals(20, free.state.value.flashWpm)
    }

    @Test fun flash_abandon_saves_nothing() = runTest {
        val d = deps()
        val vm = flash(d)
        vm.start()
        runCurrent()
        vm.abandon()
        runCurrent()
        assertEquals(false, vm.state.value.lampOn)
        assertEquals(emptyList<Any>(), d.records.games)
    }

    // ---- Interceptación ----

    @Test fun intercept_words_only_learned_and_over_saves_once() = runTest {
        val d = deps()
        val vm = intercept(d)
        vm.start()
        runCurrent()
        assertEquals(12, vm.state.value.wpm)
        val word = vm.state.value.word!!
        assertTrue(word.all { it in learned })
        vm.answer(word)
        assertEquals(true, vm.state.value.feedback?.correct)
        vm.next()
        repeat(3) { vm.answer("zzz"); vm.next() }
        runCurrent()
        assertEquals(GamePhase.OVER, vm.state.value.phase)
        assertEquals(listOf(Triple(GameId.INTERCEPT, 1, 12)), d.records.games)
        assertEquals(1, d.stats.recorded.size)
    }

    @Test fun intercept_repeat_once_per_word() = runTest {
        val d = deps()
        val vm = intercept(d)
        vm.start()
        assertTrue(vm.state.value.canRepeat)
        vm.repeat()
        assertEquals(false, vm.state.value.canRepeat)
    }

    @Test fun intercept_abandon_saves_nothing() = runTest {
        val d = deps()
        val vm = intercept(d)
        vm.start()
        vm.answer("zzz")
        vm.abandon()
        runCurrent()
        assertEquals(GamePhase.READY, vm.state.value.phase)
        assertEquals(emptyList<Any>(), d.records.games)
    }

    @Test fun locked_game_reports_locked() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val vm = InterceptViewModel(
            GameDeps(
                FakeContentSource(listOf(FakeContentSource.realChapter1)), FakeProgressRepository.completed("c1m1" to 1),
                FakeCharStatsRepository(), FakeGameRecordsRepository(), FakeSettingsRepository(AppSettings()), FakeOutputFactory(this),
                Random(1), backgroundScope, { 0L },
            ),
        )
        assertEquals(GamePhase.LOCKED, vm.state.value.phase)
        vm.start()
        assertEquals(GamePhase.LOCKED, vm.state.value.phase)
    }
}
