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
        assertEquals(10, vm.state.value.wpm)   // empieza a la velocidad efectiva, como las misiones
        val word = vm.state.value.word!!
        assertTrue(word.all { it in learned })
        vm.answer(word)
        assertEquals(true, vm.state.value.feedback?.correct)
        vm.next()
        repeat(3) { vm.answer("zzz"); vm.next() }
        runCurrent()
        assertEquals(GamePhase.OVER, vm.state.value.phase)
        assertEquals(listOf(Triple(GameId.INTERCEPT, 1, 10)), d.records.games)
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

    /** Como Room: el progreso llega un poco después de crear el ViewModel. */
    private fun delayedProgress(vararg completed: Pair<String, Int>) = object : io.github.egt17baa.morseprende.data.progress.ProgressRepository {
        override val progress = kotlinx.coroutines.flow.flow {
            kotlinx.coroutines.delay(50)
            emit(completed.associate { (id, st) -> id to io.github.egt17baa.morseprende.data.progress.MissionProgress(id, st, 1, 0) })
        }
        override suspend fun recordResult(missionId: String, stars: Int, nowMs: Long) = Unit
    }

    private fun TestScope.lateDeps(d: Deps) = GameDeps(
        FakeContentSource(listOf(FakeContentSource.realChapter1)), delayedProgress(*completed),
        d.stats, d.records, FakeSettingsRepository(AppSettings()), FakeOutputFactory(this),
        Random(1), backgroundScope, { d.clock[0] },
    )

    @Test fun games_unlock_when_progress_loads_late() = runTest {
        val d = deps()
        val stormVm = StormViewModel(lateDeps(d))
        val flashVm = FlashViewModel(lateDeps(d))
        val interceptVm = InterceptViewModel(lateDeps(d))
        assertEquals(GamePhase.LOADING, stormVm.state.value.phase)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(GamePhase.READY, stormVm.state.value.phase)
        assertEquals(learned, stormVm.state.value.letters.toSet())
        assertEquals(GamePhase.READY, flashVm.state.value.phase)
        assertEquals(GamePhase.READY, interceptVm.state.value.phase)
    }

    @Test fun arcade_records_gentle_outcomes() = runTest {
        val d = deps()
        val vm = storm(d)
        vm.start()
        while (vm.state.value.phase != GamePhase.OVER && d.clock[0] < 300_000) { d.clock[0] += 100; vm.frame() }
        runCurrent()
        assertEquals(listOf(true), d.stats.gentleFlags)
    }

    @Test fun game_over_shows_reward() = runTest {
        val d = deps()
        val economy = io.github.egt17baa.morseprende.data.FakeEconomyRepository()
        val progress = FakeProgressRepository.completed(*completed)
        val rewards = io.github.egt17baa.morseprende.rewards.RewardsService(
            FakeContentSource(listOf(FakeContentSource.realChapter1)), progress, d.records,
            io.github.egt17baa.morseprende.data.FakeSeenCardsRepository(), economy,
            io.github.egt17baa.morseprende.data.FakeActivityRepository(), io.github.egt17baa.morseprende.data.FakeAchievementsRepository(),
            today = { java.time.LocalDate.of(2026, 11, 5) }, now = { 1L },
        )
        val vm = StormViewModel(
            GameDeps(
                FakeContentSource(listOf(FakeContentSource.realChapter1)), progress, d.stats, d.records,
                FakeSettingsRepository(AppSettings()), FakeOutputFactory(this), Random(1), backgroundScope, { d.clock[0] }, rewards,
            ),
        )
        vm.start()
        while (vm.state.value.phase != GamePhase.OVER && d.clock[0] < 300_000) { d.clock[0] += 100; vm.frame() }
        runCurrent()
        assertTrue(vm.state.value.reward?.dailyBonus == true)
        assertEquals(vm.state.value.reward?.destellos, economy.balance.value)
    }

    @Test fun intercept_without_correct_words_saves_no_speed() = runTest {
        val d = deps()
        val vm = intercept(d)
        vm.start()
        repeat(3) { vm.answer("zzz"); vm.next() }
        runCurrent()
        assertEquals(listOf(Triple(GameId.INTERCEPT, 0, 0)), d.records.games)
    }

    // ---- Guardia nocturna ----

    private fun TestScope.night(d: Deps, settings: AppSettings, outputs: FakeOutputFactory, finished: Boolean = true): NightWatchViewModel {
        val ch3 = io.github.egt17baa.morseprende.game.content.Chapter(
            id = 3, title = "Señal Estelar", intro = "", words = listOf("ECO"),
            missions = listOf(
                io.github.egt17baa.morseprende.game.content.Mission(
                    "c3f", io.github.egt17baa.morseprende.game.content.MissionType.FINALE, "Final", "", emptyList(),
                    io.github.egt17baa.morseprende.game.content.StoryMessage("Eco", "SOS"), "",
                ),
            ),
        )
        val progress = if (finished) FakeProgressRepository.completed(*completed, "c3f" to 3) else FakeProgressRepository.completed(*completed)
        return NightWatchViewModel(
            GameDeps(
                FakeContentSource(listOf(FakeContentSource.realChapter1, ch3)), progress,
                d.stats, d.records, FakeSettingsRepository(settings), outputs,
                Random(1), backgroundScope, { testScheduler.currentTime },
            ),
        )
    }

    /** Copia bien todos los mensajes hasta pasar por [count] de ellos, guardando el canal de cada uno. */
    private fun TestScope.playNight(vm: NightWatchViewModel, count: Int): List<io.github.egt17baa.morseprende.game.games.NightChannel> {
        val seen = mutableListOf<io.github.egt17baa.morseprende.game.games.NightChannel>()
        vm.start()
        runCurrent()
        repeat(count) {
            seen += vm.state.value.channel
            advanceTimeBy(60_000)
            vm.answer(vm.state.value.text!!)
            vm.next()
            runCurrent()
        }
        return seen
    }

    @Test fun night_watch_channels_respect_settings() = runTest {
        val d = deps()
        // Vibración desactivada: nunca se crea una salida de vibración.
        val noVib = FakeOutputFactory(this)
        val seen = playNight(night(d, AppSettings(vibration = false), noVib), 13)
        assertTrue(seen.none { it == io.github.egt17baa.morseprende.game.games.NightChannel.VIBRATION })
        assertTrue(noVib.created.none { it.first == io.github.egt17baa.morseprende.playback.OutputKind.VIBRATION })
        // Con vibración sí hay mensajes por vibración.
        val withVib = FakeOutputFactory(this)
        playNight(night(d, AppSettings(vibration = true), withVib), 7)
        assertTrue(withVib.created.any { it.first == io.github.egt17baa.morseprende.playback.OutputKind.VIBRATION })
    }

    @Test fun night_light_capped_when_limiting_flashes() = runTest {
        val d = deps()
        val vm = night(d, AppSettings(charWpm = 25, effectiveWpm = 25, limitFlashes = true), FakeOutputFactory(this))
        vm.start()
        runCurrent()
        // Hasta el 4.º mensaje (luz).
        repeat(3) { advanceTimeBy(60_000); vm.answer(vm.state.value.text!!); vm.next(); runCurrent() }
        assertEquals(io.github.egt17baa.morseprende.game.games.NightChannel.LIGHT, vm.state.value.channel)
        val text = vm.state.value.text!!
        val fast = io.github.egt17baa.morseprende.core.MorseTiming.totalMs(
            io.github.egt17baa.morseprende.core.MorseTiming(25).signalsFor(io.github.egt17baa.morseprende.core.MorseCodec.encode(text).morse),
        )
        assertTrue(vm.state.value.lampOn)
        val slow = io.github.egt17baa.morseprende.core.MorseTiming.totalMs(
            io.github.egt17baa.morseprende.core.MorseTiming(7).signalsFor(io.github.egt17baa.morseprende.core.MorseCodec.encode(text).morse),
        )
        // Entre el final que tendría a 25 PPM y el de 7 PPM, la luz sigue encendiéndose.
        advanceTimeBy(fast + 20)
        var litLater = false
        var t = fast + 20
        while (t < slow - 20) {
            if (vm.state.value.lampOn) litLater = true
            advanceTimeBy(10); t += 10
        }
        assertTrue("la luz sigue a 7 PPM (fast=$fast slow=$slow)", litLater)
    }

    @Test fun night_locked_until_c3f() = runTest {
        val d = deps()
        val vm = night(d, AppSettings(), FakeOutputFactory(this), finished = false)
        runCurrent()
        assertEquals(GamePhase.LOCKED, vm.state.value.phase)
        val open = night(d, AppSettings(), FakeOutputFactory(this), finished = true)
        runCurrent()
        assertEquals(GamePhase.READY, open.state.value.phase)
    }

    @Test fun night_saves_score_and_hour() = runTest {
        val d = deps()
        val vm = night(d, AppSettings(vibration = false), FakeOutputFactory(this))
        playNight(vm, 6)
        repeat(3) { vm.answer("???"); vm.next() }
        runCurrent()
        assertEquals(GamePhase.OVER, vm.state.value.phase)
        val rec = d.records.records.value[GameId.NIGHT_WATCH]!!
        assertEquals(6, rec.bestScore)
        assertEquals(1, rec.bestExtra)
    }

    @Test fun night_vibration_slow_and_can_switch_to_sound() = runTest {
        val d = deps()
        val outputs = FakeOutputFactory(this)
        val vm = night(d, AppSettings(charWpm = 25, effectiveWpm = 25, vibration = true), outputs)
        vm.start()
        runCurrent()
        // Hasta el 6.º mensaje (vibración).
        repeat(5) { advanceTimeBy(60_000); vm.answer(vm.state.value.text!!); vm.next(); runCurrent() }
        assertEquals(io.github.egt17baa.morseprende.game.games.NightChannel.VIBRATION, vm.state.value.channel)
        advanceTimeBy(60_000)
        val vib = outputs.created.last { it.first == io.github.egt17baa.morseprende.playback.OutputKind.VIBRATION }.second
        // A mano no se leen pulsos de 48 ms: la vibración nunca pasa de 15 PPM (punto de 80 ms).
        val shortest = vib.played.flatten().minOf { it.durationMs }
        assertTrue("pulso más corto $shortest ms", shortest >= 80)
        // «No noto la vibración»: el mismo mensaje se repite por sonido.
        val sounds = outputs.created.count { it.first == io.github.egt17baa.morseprende.playback.OutputKind.SOUND }
        vm.useSound()
        runCurrent()
        assertTrue(vm.state.value.vibrationFallback)
        assertEquals(sounds + 1, outputs.created.count { it.first == io.github.egt17baa.morseprende.playback.OutputKind.SOUND })
    }

    @Test fun night_abandon_keeps_the_run() = runTest {
        val d = deps()
        val vm = night(d, AppSettings(vibration = false), FakeOutputFactory(this))
        playNight(vm, 6)
        // Salir (atrás, llamada…) a mitad de guardia guarda lo conseguido.
        vm.abandon()
        runCurrent()
        assertEquals(GamePhase.OVER, vm.state.value.phase)
        val rec = d.records.records.value[GameId.NIGHT_WATCH]!!
        assertEquals(6, rec.bestScore)
        assertEquals(1, rec.bestExtra)
        // Sin aciertos no se guarda nada.
        val empty = night(d, AppSettings(vibration = false), FakeOutputFactory(this))
        empty.start(); runCurrent(); empty.abandon(); runCurrent()
        assertEquals(GamePhase.READY, empty.state.value.phase)
        assertEquals(1, d.records.games.count { it.first == GameId.NIGHT_WATCH })
    }
}
