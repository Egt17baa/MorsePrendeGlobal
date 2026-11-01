package io.github.egt17baa.morseprende.ui.mission

import io.github.egt17baa.morseprende.data.FakeCharStatsRepository
import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.game.content.Channel
import io.github.egt17baa.morseprende.game.content.Mission
import io.github.egt17baa.morseprende.game.mission.Exercise
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
import io.github.egt17baa.morseprende.settings.KeyerMode
import io.github.egt17baa.morseprende.translator.FakeOutputFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MissionViewModelTest {

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(
        val vm: MissionViewModel,
        val progress: FakeProgressRepository,
        val charStats: FakeCharStatsRepository,
        val sidetones: FakeSidetoneFactory,
        val clock: LongArray,
    ) {
        val s get() = vm.state.value
        fun at(t: Long) { clock[0] = t }
    }

    private fun TestScope.env(
        settings: AppSettings = AppSettings(charWpm = 12, effectiveWpm = 12),
        content: FakeContentSource = FakeContentSource(listOf(MissionTestContent.chapter)),
        missionId: String = "t1",
        progress: FakeProgressRepository = FakeProgressRepository(),
        charStats: FakeCharStatsRepository = FakeCharStatsRepository(),
        saves: io.github.egt17baa.morseprende.data.save.MissionSaveStore = io.github.egt17baa.morseprende.data.save.NoMissionSaves,
    ): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val clock = longArrayOf(0)
        val sidetones = FakeSidetoneFactory()
        val vm = MissionViewModel(
            missionId, content, progress, FakeSettingsRepository(settings), FakeOutputFactory(this), sidetones,
            charStats = charStats, random = Random(1), playerScope = backgroundScope, clock = { clock[0] },
            saves = saves,
        )
        return Env(vm, progress, charStats, sidetones, clock)
    }

    /** Responde bien a todo hasta llegar al primer ejercicio de enviar. */
    private fun Env.advanceToSend() {
        vm.start()
        while (s.exercise !is Exercise.Send) {
            when (val e = s.exercise) {
                is Exercise.Listen -> vm.continueListen()
                is Exercise.ReceiveChoice -> { vm.answer(e.answer); vm.next() }
                is Exercise.ReceiveTyped -> { vm.answer(e.answer); vm.next() }
                else -> error("inesperado: $e")
            }
        }
    }

    private fun Env.keyDot(start: Long) {
        at(start); vm.keyDown()
        at(start + 80); vm.keyUp()
    }

    private fun Env.keyCode(code: String, start: Long): Long {
        var t = start
        code.forEach { el ->
            at(t); vm.keyDown()
            t += if (el == '.') 80 else 300
            at(t); vm.keyUp()
            t += 100
        }
        return t
    }

    @Test fun starts_in_briefing_then_exercise() = runTest {
        val e = env()
        assertEquals(MissionPhase.BRIEFING, e.s.phase)
        assertEquals("La primera luz", e.s.title)
        assertEquals("Aprende S y O", e.s.briefing)
        e.vm.start()
        assertEquals(MissionPhase.EXERCISE, e.s.phase)
        assertEquals(Exercise.Listen(listOf('S', 'O')), e.s.exercise)
        assertEquals(setOf('S', 'O'), e.s.newChars)
        assertTrue(e.s.exerciseCount > 0)
    }

    @Test fun receive_answer_gives_feedback_then_next() = runTest {
        val e = env()
        e.vm.start()
        e.vm.continueListen()
        val first = e.s.exercise as Exercise.ReceiveChoice
        val index = e.s.exerciseIndex
        e.vm.answer(first.options.first { it != first.answer })
        assertEquals(Feedback(false, first.answer), e.s.feedback)
        e.vm.next()
        assertEquals(null, e.s.feedback)
        assertEquals(index + 1, e.s.exerciseIndex)
    }

    @Test fun one_button_keying_builds_text() = runTest {
        val e = env()
        e.advanceToSend()
        e.keyDot(0); e.keyDot(180); e.keyDot(360)
        assertEquals("...", e.s.keyerPending)
        e.at(440 + 300); e.vm.tick()
        assertEquals("S", e.s.sentText)
        assertEquals("", e.s.keyerPending)
        assertEquals(List(3) { listOf("down", "up") }.flatten(), e.sidetones.last.calls)
    }

    @Test fun two_button_keying_builds_text() = runTest {
        val e = env(settings = AppSettings(charWpm = 12, effectiveWpm = 12, keyerMode = KeyerMode.TWO_BUTTONS))
        e.advanceToSend()
        assertEquals(KeyerMode.TWO_BUTTONS, e.s.keyerMode)
        repeat(3) { e.vm.dot() }
        e.vm.nextLetter()
        repeat(3) { e.vm.dash() }
        e.vm.nextLetter()
        assertEquals("SO", e.s.sentText)
        e.vm.nextLetter()
        assertEquals("SO", e.s.sentText)   // objetivo de una sola palabra: no se añaden espacios
    }

    @Test fun delete_last_removes_char() = runTest {
        val e = env(settings = AppSettings(keyerMode = KeyerMode.TWO_BUTTONS))
        e.advanceToSend()
        repeat(3) { e.vm.dot() }; e.vm.nextLetter()
        e.vm.dash()
        e.vm.deleteLast()
        assertEquals("", e.s.keyerPending)
        assertEquals("S", e.s.sentText)
        e.vm.deleteLast()
        assertEquals("", e.s.sentText)
    }

    @Test fun submit_send_scores_and_retries() = runTest {
        val e = env(settings = AppSettings(keyerMode = KeyerMode.TWO_BUTTONS))
        e.advanceToSend()
        val target = (e.s.exercise as Exercise.Send).target
        e.vm.dash(); e.vm.dash(); e.vm.dash(); e.vm.dash(); e.vm.dash()   // "0", seguro que no es el objetivo
        e.vm.submitSend()
        assertEquals(Feedback(false, target), e.s.feedback)
        assertEquals(2, e.s.attemptsLeft)
        e.vm.next()
        assertEquals("", e.s.sentText)
        assertEquals(Exercise.Send(target), e.s.exercise)
        target.forEach { c ->
            io.github.egt17baa.morseprende.core.MorseAlphabet.codeFor(c)!!.forEach { if (it == '.') e.vm.dot() else e.vm.dash() }
            e.vm.nextLetter()
        }
        e.vm.submitSend()
        assertEquals(Feedback(true, target), e.s.feedback)
    }

    @Test fun final_message_hint_level_increases() = runTest {
        val e = env()
        e.vm.start()
        while (e.s.exercise !is Exercise.FinalMessage) {
            when (val x = e.s.exercise) {
                is Exercise.Listen -> e.vm.continueListen()
                is Exercise.ReceiveChoice -> { e.vm.answer(x.answer); e.vm.next() }
                is Exercise.ReceiveTyped -> { e.vm.answer(x.answer); e.vm.next() }
                is Exercise.Send -> { repeat(3) { e.vm.submitSend(); e.vm.next() } }
                else -> Unit
            }
        }
        e.vm.answer("X"); e.vm.next()
        e.vm.answer("X"); e.vm.next()
        assertEquals(1, e.s.hintLevel)
    }

    @Test fun completing_records_progress_once() = runTest {
        val e = env(progress = FakeProgressRepository.completed("t1" to 2))
        e.vm.start()
        while (e.s.phase == MissionPhase.EXERCISE) {
            when (val x = e.s.exercise) {
                is Exercise.Listen -> e.vm.continueListen()
                is Exercise.ReceiveChoice -> { e.vm.answer(x.answer); e.vm.next() }
                is Exercise.ReceiveTyped -> { e.vm.answer(x.answer); e.vm.next() }
                is Exercise.Send -> { e.vm.submitSend(); e.vm.next(); e.vm.submitSend(); e.vm.next(); e.vm.submitSend(); e.vm.next() }
                is Exercise.FinalMessage -> { e.vm.answer(x.message.text); e.vm.next() }
                null -> error("sin ejercicio")
            }
        }
        runCurrent()
        assertEquals(MissionPhase.DEBRIEFING, e.s.phase)
        assertEquals("¡Bien!", e.s.debriefing)
        assertEquals(listOf("t1" to e.s.stars), e.progress.recorded)
        assertEquals(2, e.s.previousBest)
        assertTrue(e.s.stars in 1..3)
        e.vm.next()
        assertEquals(1, e.progress.recorded.size)
    }

    @Test fun abandon_does_not_record_and_stops_audio() = runTest {
        val e = env()
        e.vm.start()
        e.vm.continueListen()
        runCurrent()
        e.vm.abandon()
        runCurrent()
        assertEquals(emptyList<Pair<String, Int>>(), e.progress.recorded)
        assertFalse(e.s.isPlaying)
        assertTrue(e.sidetones.last.closed)
    }

    @Test fun broken_content_goes_to_error() = runTest {
        val e = env(content = FakeContentSource(emptyList(), fail = true))
        assertEquals(MissionPhase.ERROR, e.s.phase)
        val unknown = env(missionId = "no-existe")
        assertEquals(MissionPhase.ERROR, unknown.s.phase)
    }

    @Test fun receive_exercise_plays_automatically() = runTest {
        val e = env()
        e.vm.start()
        e.vm.continueListen()
        runCurrent()
        assertTrue(e.s.isPlaying)
        assertNotNull(e.s.exercise as? Exercise.ReceiveChoice)
    }

    @Test fun stop_then_resume_restarts_and_tone_works() = runTest {
        val e = env()
        e.advanceToSend()
        e.vm.abandon()
        assertEquals(MissionPhase.BRIEFING, e.s.phase)
        assertTrue(e.sidetones.last.closed)
        e.advanceToSend()
        e.vm.keyDown()
        assertTrue("los tonos anteriores están cerrados", e.sidetones.created.dropLast(1).all { it.closed })
        assertEquals(listOf("down"), e.sidetones.last.calls)
        assertEquals(emptyList<Pair<String, Int>>(), e.progress.recorded)
    }

    @Test fun abandon_in_debriefing_keeps_debriefing() = runTest {
        val e = env()
        e.vm.start()
        while (e.s.phase == MissionPhase.EXERCISE) {
            when (val x = e.s.exercise) {
                is Exercise.Listen -> e.vm.continueListen()
                is Exercise.ReceiveChoice -> { e.vm.answer(x.answer); e.vm.next() }
                is Exercise.ReceiveTyped -> { e.vm.answer(x.answer); e.vm.next() }
                is Exercise.Send -> repeat(3) { e.vm.dot(); e.vm.submitSend(); e.vm.next() }
                is Exercise.FinalMessage -> { e.vm.answer(x.message.text); e.vm.next() }
                null -> error("sin ejercicio")
            }
        }
        e.vm.abandon()
        assertEquals(MissionPhase.DEBRIEFING, e.s.phase)
    }

    @Test fun slow_pause_between_letters_is_not_a_word_break() = runTest {
        val e = env()
        e.advanceToSend()
        // Fuerza un objetivo sin espacios: se transmite exactamente el objetivo actual, con pausas largas.
        val target = (e.s.exercise as Exercise.Send).target
        var t = 0L
        target.forEach { c ->
            t = e.keyCode(io.github.egt17baa.morseprende.core.MorseAlphabet.codeFor(c)!!, t)
            e.at(t + 300); e.vm.tick()        // cierra la letra
            t += 1_500
            e.at(t); e.vm.tick()              // pausa larga de principiante (> 6 unidades)
        }
        assertEquals(target, e.s.sentText)
        e.vm.submitSend()
        assertEquals(Feedback(true, target), e.s.feedback)
    }

    @Test fun empty_next_letter_adds_no_space_for_single_word_target() = runTest {
        val e = env(settings = AppSettings(keyerMode = KeyerMode.TWO_BUTTONS))
        e.advanceToSend()
        repeat(3) { e.vm.dot() }
        e.vm.nextLetter()
        e.vm.nextLetter()
        assertEquals("S", e.s.sentText)
    }

    @Test fun tone_never_stays_on_after_submit() = runTest {
        val e = env()
        e.advanceToSend()
        e.at(0); e.vm.keyDown()
        e.vm.submitSend()
        assertEquals("up", e.sidetones.last.calls.last())
        e.at(500); e.vm.keyUp()
        assertEquals("up", e.sidetones.last.calls.last())
    }

    @Test fun key_timestamps_from_events_are_used() = runTest {
        val e = env()
        e.advanceToSend()
        e.at(99_999)
        e.vm.keyDown(atMs = 1_000)
        e.vm.keyUp(atMs = 1_350)
        assertEquals("-", e.s.keyerPending)
    }

    private fun Env.finishMission() {
        vm.start()
        while (s.phase == MissionPhase.EXERCISE) {
            when (val x = s.exercise) {
                is Exercise.Listen -> vm.continueListen()
                is Exercise.ReceiveChoice -> { vm.answer(x.answer); vm.next() }
                is Exercise.ReceiveTyped -> { vm.answer(x.answer); vm.next() }
                is Exercise.Send -> repeat(3) { vm.dot(); vm.submitSend(); vm.next() }
                is Exercise.FinalMessage -> { vm.answer(x.message.text); vm.next() }
                null -> error("sin ejercicio")
            }
        }
    }

    @Test fun completing_records_char_outcomes() = runTest {
        val e = env()
        e.finishMission()
        runCurrent()
        assertEquals(1, e.charStats.recorded.size)
        assertTrue(e.charStats.recorded.single().isNotEmpty())
        assertTrue(e.charStats.recorded.single().all { it.char in setOf('S', 'O') })
    }

    @Test fun abandon_records_no_char_outcomes() = runTest {
        val e = env()
        e.advanceToSend()
        e.vm.abandon()
        runCurrent()
        assertEquals(emptyList<Any>(), e.charStats.recorded)
    }

    @Test fun review_uses_boxes() = runTest {
        val review = io.github.egt17baa.morseprende.game.content.Mission(
            "r", io.github.egt17baa.morseprende.game.content.MissionType.REVIEW, "Repaso", "b", emptyList(),
            io.github.egt17baa.morseprende.game.content.StoryMessage("R", "SOS"), "d",
        )
        val chapter = MissionTestContent.chapter.copy(missions = MissionTestContent.chapter.missions + review)
        val e = env(
            content = FakeContentSource(listOf(chapter)), missionId = "r",
            charStats = FakeCharStatsRepository(mapOf('S' to 1, 'O' to 5)),
        )
        e.vm.start()
        val letters = mutableListOf<String>()
        while (e.s.phase == MissionPhase.EXERCISE) {
            when (val x = e.s.exercise) {
                is Exercise.ReceiveChoice -> { if (x.answer.length == 1) letters += x.answer; e.vm.answer(x.answer); e.vm.next() }
                is Exercise.ReceiveTyped -> { if (x.answer.length == 1) letters += x.answer; e.vm.answer(x.answer); e.vm.next() }
                is Exercise.Send -> { if (x.target.length == 1) letters += x.target; repeat(3) { e.vm.dot(); e.vm.submitSend(); e.vm.next() } }
                is Exercise.FinalMessage -> { e.vm.answer(x.message.text); e.vm.next() }
                else -> e.vm.continueListen()
            }
        }
        assertTrue("letters=$letters", letters.count { it == "S" } > letters.size / 2)
    }

    private fun TestScope.rewardsFor(progress: FakeProgressRepository) = io.github.egt17baa.morseprende.rewards.RewardsService(
        FakeContentSource(listOf(MissionTestContent.chapter)), progress, io.github.egt17baa.morseprende.data.FakeGameRecordsRepository(),
        io.github.egt17baa.morseprende.data.FakeSeenCardsRepository(), io.github.egt17baa.morseprende.data.FakeEconomyRepository(),
        io.github.egt17baa.morseprende.data.FakeActivityRepository(), io.github.egt17baa.morseprende.data.FakeAchievementsRepository(),
        today = { java.time.LocalDate.of(2026, 11, 5) }, now = { 1L },
    )

    @Test fun debriefing_shows_reward() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val progress = FakeProgressRepository()
        val vm = MissionViewModel(
            "t1", FakeContentSource(listOf(MissionTestContent.chapter)), progress, FakeSettingsRepository(), FakeOutputFactory(this),
            FakeSidetoneFactory(), rewards = rewardsFor(progress), random = Random(1), playerScope = backgroundScope,
        )
        val e = Env(vm, progress, FakeCharStatsRepository(), FakeSidetoneFactory(), longArrayOf(0))
        e.finishMission()
        runCurrent()
        val reward = vm.state.value.reward
        assertTrue(reward != null && reward.destellos >= 5 + 5 && reward.dailyBonus)
    }

    @Test fun abandon_gives_nothing() = runTest {
        val e = env()
        e.advanceToSend()
        e.vm.abandon()
        runCurrent()
        assertEquals(null, e.s.reward)
    }

    private fun TestScope.timedEnv(noise: Float = 0.25f, limit: Int? = 5, wpm: Int = 12): Pair<Env, FakeOutputFactory> {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val m = MissionTestContent.chapter.missions.single().copy(noise = noise, timeLimitSeconds = limit)
        val chapter = MissionTestContent.chapter.copy(missions = listOf(m))
        val outputs = FakeOutputFactory(this)
        val progress = FakeProgressRepository()
        val vm = MissionViewModel(
            "t1", FakeContentSource(listOf(chapter)), progress, FakeSettingsRepository(AppSettings(charWpm = wpm, effectiveWpm = wpm)),
            outputs, FakeSidetoneFactory(), random = Random(1), playerScope = backgroundScope,
        )
        return Env(vm, progress, FakeCharStatsRepository(), FakeSidetoneFactory(), longArrayOf(0)) to outputs
    }

    @Test fun timer_counts_down_and_times_out_once() = runTest {
        val (e, _) = timedEnv()
        e.vm.start()
        e.vm.continueListen()
        runCurrent()
        assertTrue(e.s.isPlaying)
        advanceTimeBy(3_000)
        runCurrent()
        assertFalse(e.s.isPlaying)
        val left = e.s.secondsLeft!!
        assertTrue("left=$left", left in 1..5)
        advanceTimeBy(6_000)
        runCurrent()
        assertEquals(false, e.s.feedback?.correct)
        assertEquals(null, e.s.secondsLeft)
        val index = e.s.exerciseIndex
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(index, e.s.exerciseIndex)
    }

    @Test fun answering_stops_timer() = runTest {
        val (e, _) = timedEnv()
        e.vm.start()
        e.vm.continueListen()
        advanceTimeBy(3_000)
        runCurrent()
        val x = e.s.exercise as Exercise.ReceiveChoice
        e.vm.answer(x.answer)
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(true, e.s.feedback?.correct)
        assertEquals(null, e.s.secondsLeft)
    }

    @Test fun abandon_cancels_timer() = runTest {
        val (e, _) = timedEnv()
        e.vm.start()
        e.vm.continueListen()
        advanceTimeBy(3_000)
        runCurrent()
        e.vm.abandon()
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(MissionPhase.BRIEFING, e.s.phase)
        assertEquals(null, e.s.feedback)
        assertEquals(null, e.s.secondsLeft)
    }

    @Test fun no_timer_without_limit() = runTest {
        val (e, _) = timedEnv(limit = null)
        e.vm.start()
        e.vm.continueListen()
        advanceTimeBy(30_000)
        runCurrent()
        assertEquals(null, e.s.secondsLeft)
        assertEquals(null, e.s.feedback)
    }

    @Test fun noise_passed_to_audio() = runTest {
        val (e, outputs) = timedEnv(noise = 0.25f)
        assertTrue(e.s.noisy)
        e.vm.start()
        e.vm.continueListen()
        runCurrent()
        assertEquals(0.25f, outputs.noises.last(), 1e-6f)
    }

    private class ChannelEnv(val e: Env, val outputs: FakeOutputFactory, val settings: FakeSettingsRepository)

    private fun TestScope.channelEnv(
        channel: Channel = Channel.SOUND,
        wpm: Int? = null,
        settings: AppSettings = AppSettings(charWpm = 12, effectiveWpm = 12),
        failing: Set<OutputKind> = emptySet(),
        newChars: List<Char> = listOf('S', 'O'),
        words: List<String> = MissionTestContent.chapter.words,
    ): ChannelEnv {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val m: Mission = MissionTestContent.chapter.missions.single().copy(channel = channel, wpm = wpm, newChars = newChars)
        val chapter = MissionTestContent.chapter.copy(missions = listOf(m), words = words)
        val outputs = FakeOutputFactory(this, failing)
        val repo = FakeSettingsRepository(settings)
        val progress = FakeProgressRepository()
        val vm = MissionViewModel(
            "t1", FakeContentSource(listOf(chapter)), progress, repo, outputs, FakeSidetoneFactory(),
            random = Random(1), playerScope = backgroundScope, clock = { testScheduler.currentTime },
        )
        return ChannelEnv(Env(vm, progress, FakeCharStatsRepository(), FakeSidetoneFactory(), longArrayOf(0)), outputs, repo)
    }

    private fun currentAnswer(e: Env): String = when (val x = e.s.exercise) {
        is Exercise.ReceiveChoice -> x.answer
        is Exercise.ReceiveTyped -> x.answer
        else -> error("no es de recibir: $x")
    }

    @Test fun mission_wpm_overrides_settings_without_saving() = runTest {
        val c = channelEnv(wpm = 20)
        c.e.vm.start()
        c.e.vm.continueListen()
        runCurrent()
        val played = c.outputs.created.last().second.played.last()
        assertEquals(MorseTiming(20).signalsFor(MorseCodec.encode(currentAnswer(c.e)).morse), played)
        assertEquals(12, c.settings.settings.value.charWpm)
        assertEquals(12, c.settings.settings.value.effectiveWpm)
    }

    @Test fun light_channel_capped_when_limiting() = runTest {
        val c = channelEnv(channel = Channel.LIGHT, wpm = 20, settings = AppSettings(charWpm = 12, effectiveWpm = 12, limitFlashes = true))
        c.e.vm.start()
        c.e.vm.continueListen()
        runCurrent()
        assertEquals(Channel.LIGHT, c.e.s.channel)
        assertTrue("la luz no usa salidas de hardware", c.outputs.created.isEmpty())
        assertTrue(c.e.s.lampOn)
        val uncapped = MorseTiming.totalMs(MorseTiming(20).signalsFor(MorseCodec.encode(currentAnswer(c.e)).morse))
        advanceTimeBy(uncapped + 50)
        assertTrue("a 7 PPM sigue destellando", c.e.s.isPlaying)
        advanceTimeBy(60_000)
        assertFalse(c.e.s.lampOn)
        assertFalse(c.e.s.isPlaying)
    }

    @Test fun light_channel_full_speed_without_limit() = runTest {
        val c = channelEnv(channel = Channel.LIGHT, wpm = 20, settings = AppSettings(charWpm = 12, effectiveWpm = 12, limitFlashes = false))
        c.e.vm.start()
        c.e.vm.continueListen()
        runCurrent()
        val uncapped = MorseTiming.totalMs(MorseTiming(20).signalsFor(MorseCodec.encode(currentAnswer(c.e)).morse))
        advanceTimeBy(uncapped + 50)
        assertFalse(c.e.s.isPlaying)
    }

    @Test fun vibration_channel_uses_vibration_output() = runTest {
        val c = channelEnv(channel = Channel.VIBRATION)
        c.e.vm.start()
        c.e.vm.continueListen()
        runCurrent()
        assertEquals(OutputKind.VIBRATION, c.outputs.created.last().first)
        assertFalse(c.e.s.channelFallback)
    }

    @Test fun vibration_failure_falls_back_to_sound() = runTest {
        val c = channelEnv(channel = Channel.VIBRATION, failing = setOf(OutputKind.VIBRATION))
        c.e.vm.start()
        c.e.vm.continueListen()
        runCurrent()
        assertTrue(c.e.s.channelFallback)
        assertEquals(OutputKind.SOUND, c.outputs.created.last().first)
        assertEquals(1, c.outputs.created.count { it.first == OutputKind.SOUND })
        c.e.vm.replay()
        runCurrent()
        assertEquals(2, c.outputs.created.count { it.first == OutputKind.SOUND })
        // Se puede responder igual: nunca se queda atascada.
        c.e.vm.answer(currentAnswer(c.e))
        assertEquals(true, c.e.s.feedback?.correct)
    }

    @Test fun punctuation_answers_match() = runTest {
        val c = channelEnv(newChars = listOf('?', '\''), words = listOf("?'", "'?", "??"))
        c.e.vm.start()
        c.e.vm.continueListen()
        runCurrent()
        var checked = 0
        while (c.e.s.exercise is Exercise.ReceiveChoice || c.e.s.exercise is Exercise.ReceiveTyped) {
            // Teclado del móvil: ¿ inicial y apóstrofo tipográfico.
            val typed = "¿ " + currentAnswer(c.e).replace('\'', '\u2019').lowercase() + " "
            c.e.vm.answer(typed)
            assertEquals("respuesta: $typed", true, c.e.s.feedback?.correct)
            checked++
            c.e.vm.next()
            runCurrent()
        }
        assertTrue(checked > 0)
    }

    @Test fun invalid_code_never_matches_question_mark() = runTest {
        val c = channelEnv(newChars = listOf('?'), words = listOf("?", "??"))
        c.e.vm.start()
        var sends = 0
        while (c.e.s.phase == MissionPhase.EXERCISE && sends < 3) {
            when (val x = c.e.s.exercise) {
                is Exercise.Listen -> c.e.vm.continueListen()
                is Exercise.ReceiveChoice -> { c.e.vm.answer(x.answer); c.e.vm.next() }
                is Exercise.ReceiveTyped -> { c.e.vm.answer(x.answer); c.e.vm.next() }
                is Exercise.Send -> {
                    // Una letra mal formada por cada «?» del objetivo: nunca puede contar como «?».
                    repeat(x.target.length) {
                        repeat(6) { c.e.vm.dot() }
                        c.e.vm.nextLetter()
                    }
                    c.e.vm.submitSend()
                    assertEquals("enviado: ${c.e.s.sentText}", false, c.e.s.feedback?.correct)
                    sends++
                    c.e.vm.next()
                }
                else -> break
            }
            runCurrent()
        }
        assertTrue(sends > 0)
    }

    @Test fun timer_pauses_while_replaying() = runTest {
        // A 5 PPM cada letra dura más de un segundo: la repetición cruza varios ticks del reloj.
        val (e, _) = timedEnv(limit = 5, wpm = 5)
        e.vm.start()
        e.vm.continueListen()
        runCurrent()
        while (e.s.isPlaying) { advanceTimeBy(50); runCurrent() }
        advanceTimeBy(1_500)
        runCurrent()
        val before = e.s.secondsLeft!!
        e.vm.replay()
        runCurrent()
        assertTrue(e.s.isPlaying)
        while (e.s.isPlaying) {
            assertEquals("mientras suena no corre el tiempo", before, e.s.secondsLeft)
            advanceTimeBy(50)
            runCurrent()
        }
        // Al acabar la repetición quedan los mismos segundos que antes.
        advanceTimeBy((before - 1) * 1_000L)
        runCurrent()
        assertEquals(null, e.s.feedback)
        advanceTimeBy(2_500)
        runCurrent()
        assertEquals(false, e.s.feedback?.correct)
    }

    @Test fun replay_during_first_play_does_not_start_timer() = runTest {
        val (e, _) = timedEnv(limit = 5)
        e.vm.start()
        e.vm.continueListen()
        runCurrent()
        assertTrue(e.s.isPlaying)
        e.vm.replay()
        runCurrent()
        assertTrue(e.s.isPlaying)
        assertEquals(null, e.s.secondsLeft)
        while (e.s.isPlaying) { advanceTimeBy(50); runCurrent() }
        advanceTimeBy(4_200)
        runCurrent()
        assertEquals(null, e.s.feedback)
    }

    @Test fun cant_feel_vibration_switches_to_sound() = runTest {
        val c = channelEnv(channel = Channel.VIBRATION)
        c.e.vm.start()
        c.e.vm.continueListen()
        runCurrent()
        assertEquals(OutputKind.VIBRATION, c.outputs.created.last().first)
        c.e.vm.useSound()
        runCurrent()
        assertTrue(c.e.s.channelFallback)
        assertEquals(OutputKind.SOUND, c.outputs.created.last().first)
    }

    @Test fun sidetone_released_outside_send_exercises() = runTest {
        // En el móvil, el canal de baja latencia del tono en vivo sonando a la vez que la señal
        // de recibir provoca chasquidos y cortes: solo debe existir en los ejercicios de enviar.
        val e = env()
        e.advanceToSend()
        e.keyDot(0)
        e.at(200)
        assertTrue(e.sidetones.created.isNotEmpty())
        while (e.s.exercise is Exercise.Send) {
            e.vm.submitSend()
            e.vm.next()
        }
        assertEquals(MissionPhase.EXERCISE, e.s.phase)
        assertTrue("tono cerrado fuera de enviar", e.sidetones.created.all { it.closed })
        e.vm.replay()
        assertTrue(e.sidetones.created.all { it.closed })
    }

    /** Responde bien hasta llegar al ejercicio [target] (índice). */
    private fun Env.answerUntil(target: Int) {
        while (s.exerciseIndex < target) {
            when (val x = s.exercise) {
                is Exercise.Listen -> vm.continueListen()
                is Exercise.ReceiveChoice -> { vm.answer(x.answer); vm.next() }
                is Exercise.ReceiveTyped -> { vm.answer(x.answer); vm.next() }
                is Exercise.Send -> repeat(3) { vm.dot(); vm.submitSend(); vm.next() }
                else -> error("inesperado: $x")
            }
        }
    }

    @Test fun leaving_mid_mission_offers_resume_at_same_exercise() = runTest {
        val saves = io.github.egt17baa.morseprende.data.save.InMemoryMissionSaveStore()
        val e = env(saves = saves)
        e.vm.start()
        e.answerUntil(3)
        val exercise = e.s.exercise
        e.vm.abandon()
        assertEquals(MissionPhase.BRIEFING, e.s.phase)
        assertEquals(3, e.s.resumeAt)
        e.vm.resume()
        assertEquals(MissionPhase.EXERCISE, e.s.phase)
        assertEquals(3, e.s.exerciseIndex)
        assertEquals(exercise, e.s.exercise)
    }

    @Test fun reopening_the_mission_resumes_after_app_restart() = runTest {
        val saves = io.github.egt17baa.morseprende.data.save.InMemoryMissionSaveStore()
        val first = env(saves = saves)
        first.vm.start()
        first.answerUntil(4)
        val exercise = first.s.exercise
        // El sistema cierra la app sin pasar por abandon(): un ViewModel nuevo lo encuentra guardado.
        val second = env(saves = saves)
        runCurrent()
        assertEquals(4, second.s.resumeAt)
        second.vm.resume()
        assertEquals(4, second.s.exerciseIndex)
        assertEquals(exercise, second.s.exercise)
    }

    @Test fun start_over_discards_the_saved_game() = runTest {
        val saves = io.github.egt17baa.morseprende.data.save.InMemoryMissionSaveStore()
        val e = env(saves = saves)
        e.vm.start()
        e.answerUntil(3)
        e.vm.abandon()
        e.vm.start()
        assertEquals(0, e.s.exerciseIndex)
        assertEquals(null, e.s.resumeAt)
        assertTrue(saves.saved.isEmpty())
    }

    @Test fun finishing_clears_the_saved_game() = runTest {
        val saves = io.github.egt17baa.morseprende.data.save.InMemoryMissionSaveStore()
        val e = env(saves = saves)
        e.finishMission()
        assertEquals(MissionPhase.DEBRIEFING, e.s.phase)
        assertTrue(saves.saved.isEmpty())
        assertEquals(1, e.progress.recorded.size)
    }

    @Test fun nothing_to_resume_at_first_exercise() = runTest {
        val saves = io.github.egt17baa.morseprende.data.save.InMemoryMissionSaveStore()
        val e = env(saves = saves)
        e.vm.start()
        e.vm.abandon()
        assertEquals(null, e.s.resumeAt)
    }
}
