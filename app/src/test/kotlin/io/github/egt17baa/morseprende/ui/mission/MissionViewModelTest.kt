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
        assertEquals("SO ", e.s.sentText)
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
}
