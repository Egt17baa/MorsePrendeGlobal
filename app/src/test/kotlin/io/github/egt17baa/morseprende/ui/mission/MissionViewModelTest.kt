package io.github.egt17baa.morseprende.ui.mission

import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.game.mission.Exercise
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
import io.github.egt17baa.morseprende.settings.KeyerMode
import io.github.egt17baa.morseprende.translator.FakeOutputFactory
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
    ): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val clock = longArrayOf(0)
        val sidetones = FakeSidetoneFactory()
        val vm = MissionViewModel(
            missionId, content, progress, FakeSettingsRepository(settings), FakeOutputFactory(this), sidetones,
            random = Random(1), playerScope = backgroundScope, clock = { clock[0] },
        )
        return Env(vm, progress, sidetones, clock)
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
        assertEquals(2, e.sidetones.created.size)
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
}
