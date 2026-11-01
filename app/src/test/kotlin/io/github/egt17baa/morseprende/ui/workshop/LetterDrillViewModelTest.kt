package io.github.egt17baa.morseprende.ui.workshop

import io.github.egt17baa.morseprende.data.FakeCharStatsRepository
import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.game.review.CharOutcome
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class LetterDrillViewModelTest {

    @After fun tearDown() = Dispatchers.resetMain()

    private val stats = FakeCharStatsRepository()

    /** Completadas c1m1..c1r1: letras S O E T A N, en ese orden. */
    private fun TestScope.vm(
        progress: FakeProgressRepository = FakeProgressRepository.completed("c1m1" to 2, "c1m2" to 2, "c1m3" to 2, "c1r1" to 2),
        outputs: FakeOutputFactory = FakeOutputFactory(this),
    ): LetterDrillViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return LetterDrillViewModel(
            FakeContentSource(listOf(FakeContentSource.realChapter1)), progress, stats, FakeSettingsRepository(), outputs, Random(4), backgroundScope,
        )
    }

    @Test fun drill_defaults_last_learned() = runTest {
        val v = vm()
        runCurrent()
        assertEquals(DrillPhase.SETUP, v.state.value.phase)
        assertEquals(listOf('S', 'O', 'E', 'T', 'A', 'N'), v.state.value.available)
        assertEquals(setOf('E', 'T', 'A', 'N'), v.state.value.selected)
    }

    @Test fun drill_locked_with_too_few_letters() = runTest {
        val v = vm(progress = FakeProgressRepository())
        runCurrent()
        assertEquals(DrillPhase.LOCKED, v.state.value.phase)
    }

    @Test fun drill_needs_two_letters() = runTest {
        val v = vm()
        runCurrent()
        listOf('E', 'T', 'A').forEach(v::toggle)
        assertEquals(setOf('N'), v.state.value.selected)
        v.start()
        assertEquals(DrillPhase.SETUP, v.state.value.phase)
        v.toggle('S')
        v.start()
        assertEquals(DrillPhase.PLAYING, v.state.value.phase)
    }

    @Test fun drill_records_outcomes_and_moves_on() = runTest {
        val outputs = FakeOutputFactory(this)
        val v = vm(outputs = outputs)
        runCurrent()
        v.start()
        runCurrent()
        assertEquals(1, outputs.created.size)
        val first = v.state.value.current!!
        v.answer(first)
        runCurrent()
        assertTrue(v.state.value.feedback!!.correct)
        // Cada respuesta va al repaso inteligente, en modo suave.
        assertEquals(listOf(listOf(CharOutcome(first, true))), stats.recorded)
        assertEquals(listOf(true), stats.gentleFlags)
        // Mientras se ve el resultado, otra respuesta no cuenta.
        v.answer(first)
        assertEquals(1, stats.recorded.size)
        // Sola, pasa a la siguiente letra (distinta) y la hace sonar.
        advanceTimeBy(LetterDrillViewModel.FEEDBACK_MS + 1)
        runCurrent()
        assertNull(v.state.value.feedback)
        assertNotEquals(first, v.state.value.current)
        assertEquals(2, outputs.created.size)
        assertEquals(1, v.state.value.stats[first]!!.right)
    }

    @Test fun drill_stop_returns_to_setup() = runTest {
        val v = vm()
        runCurrent()
        v.start()
        runCurrent()
        v.stop()
        assertEquals(DrillPhase.SETUP, v.state.value.phase)
        assertEquals(false, v.state.value.isPlaying)
    }

    @Test fun drill_pause_keeps_stats_and_silences() = runTest {
        val v = vm()
        runCurrent()
        v.start()
        runCurrent()
        val first = v.state.value.current!!
        v.answer(first)
        // Pasar a segundo plano (o girar el móvil) con el resultado a la vista: se calla, pero no se pierde nada.
        v.pause()
        advanceTimeBy(LetterDrillViewModel.FEEDBACK_MS * 3)
        runCurrent()
        assertEquals(DrillPhase.PLAYING, v.state.value.phase)
        assertEquals(false, v.state.value.isPlaying)
        assertNull(v.state.value.feedback)
        assertNotEquals(first, v.state.value.current)
        assertEquals(1, v.state.value.stats[first]!!.total)
    }
}
