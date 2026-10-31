package io.github.egt17baa.morseprende.translator

import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TranslatorViewModelTest {

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(
        val vm: TranslatorViewModel,
        val repo: FakeSettingsRepository,
        val factory: FakeOutputFactory,
    )

    private fun TestScope.env(
        settings: AppSettings = AppSettings(),
        torchAvailable: Boolean = true,
    ): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val repo = FakeSettingsRepository(settings)
        val factory = FakeOutputFactory(this)
        val vm = TranslatorViewModel(repo, factory, torchAvailable, playerScope = backgroundScope)
        return Env(vm, repo, factory)
    }

    private val TranslatorViewModel.s get() = state.value

    @Test fun translates_text() = runTest {
        val e = env()
        e.vm.onInputChange("sos")
        assertEquals("... --- ...", e.vm.s.output)
    }

    @Test fun translates_morse() = runTest {
        val e = env()
        e.vm.swapDirection()
        e.vm.onInputChange("... --- ...")
        assertEquals("SOS", e.vm.s.output)
        assertEquals(Direction.MORSE_TO_TEXT, e.vm.s.direction)
    }

    @Test fun swap_moves_output_to_input() = runTest {
        val e = env()
        e.vm.onInputChange("sos")
        e.vm.swapDirection()
        assertEquals("... --- ...", e.vm.s.input)
        assertEquals("SOS", e.vm.s.output)
        assertEquals(Direction.MORSE_TO_TEXT, e.vm.s.direction)
    }

    @Test fun truncates_input() = runTest {
        val e = env()
        e.vm.onInputChange("a".repeat(5000))
        assertEquals(TranslatorViewModel.MAX_INPUT, e.vm.s.input.length)
    }

    @Test fun unsupported_listed() = runTest {
        val e = env()
        e.vm.onInputChange("a😀")
        assertEquals(setOf("😀"), e.vm.s.unsupported)
    }

    @Test fun can_play_needs_morse_and_output() = runTest {
        val e = env()
        assertFalse(e.vm.s.canPlay)
        e.vm.onInputChange("😀")
        assertFalse(e.vm.s.canPlay)
        e.vm.onInputChange("e")
        assertTrue(e.vm.s.canPlay)
        e.vm.toggleOutput(OutputKind.SOUND)
        assertFalse(e.vm.s.canPlay)
    }

    @Test fun torch_not_offered_when_unavailable() = runTest {
        val e = env(settings = AppSettings(flashWarningAccepted = true), torchAvailable = false)
        e.vm.toggleOutput(OutputKind.TORCH)
        assertEquals(setOf(OutputKind.SOUND), e.vm.s.outputs)
        assertFalse(e.vm.s.torchAvailable)
    }

    @Test fun first_light_output_asks_warning() = runTest {
        val e = env()
        e.vm.toggleOutput(OutputKind.SCREEN)
        assertTrue(e.vm.s.showFlashWarning)
        assertFalse(OutputKind.SCREEN in e.vm.s.outputs)
        e.vm.acceptFlashWarning()
        assertFalse(e.vm.s.showFlashWarning)
        assertTrue(OutputKind.SCREEN in e.vm.s.outputs)
        assertTrue(e.repo.settings.value.flashWarningAccepted)
    }

    @Test fun dismissing_warning_adds_nothing() = runTest {
        val e = env()
        e.vm.toggleOutput(OutputKind.TORCH)
        e.vm.dismissFlashWarning()
        assertFalse(e.vm.s.showFlashWarning)
        assertEquals(setOf(OutputKind.SOUND), e.vm.s.outputs)
        assertFalse(e.repo.settings.value.flashWarningAccepted)
    }

    @Test fun flash_limit_caps_speed() = runTest {
        val e = env(settings = AppSettings(charWpm = 20, limitFlashes = true, flashWarningAccepted = true))
        e.vm.onInputChange("e")
        e.vm.toggleOutput(OutputKind.SCREEN)
        e.vm.play()
        runCurrent()
        assertTrue(e.vm.s.speedLimitedForFlashes)
        val sound = e.factory.created.single { it.first == OutputKind.SOUND }.second
        assertEquals(171L, sound.played.single().first().durationMs)
    }

    @Test fun no_limit_when_disabled() = runTest {
        val e = env(settings = AppSettings(charWpm = 20, limitFlashes = false, flashWarningAccepted = true))
        e.vm.onInputChange("e")
        e.vm.toggleOutput(OutputKind.SCREEN)
        e.vm.play()
        runCurrent()
        assertFalse(e.vm.s.speedLimitedForFlashes)
        val sound = e.factory.created.single { it.first == OutputKind.SOUND }.second
        assertEquals(60L, sound.played.single().first().durationMs)
    }

    @Test fun no_limit_with_sound_only() = runTest {
        val e = env(settings = AppSettings(charWpm = 20, limitFlashes = true))
        e.vm.onInputChange("e")
        e.vm.play()
        runCurrent()
        assertFalse(e.vm.s.speedLimitedForFlashes)
        assertEquals(60L, e.factory.created.single().second.played.single().first().durationMs)
    }

    @Test fun plays_morse_input_when_decoding() = runTest {
        val e = env(settings = AppSettings(charWpm = 20, effectiveWpm = 20))
        e.vm.swapDirection()
        e.vm.onInputChange("·−")
        e.vm.play()
        runCurrent()
        val played = e.factory.created.single().second.played.single()
        assertEquals(listOf(60L, 60L, 180L), played.map { it.durationMs })
    }

    @Test fun play_twice_restarts() = runTest {
        val e = env()
        e.vm.onInputChange("sos")
        e.vm.play()
        runCurrent()
        advanceTimeBy(20)
        e.vm.play()
        runCurrent()
        val first = e.factory.created.first().second
        assertEquals(false, first.lastState)
        assertEquals(2, e.factory.created.size)
        assertTrue(e.vm.s.isPlaying)
    }

    @Test fun stop_turns_screen_off() = runTest {
        val e = env(settings = AppSettings(flashWarningAccepted = true))
        e.vm.onInputChange("t")
        e.vm.toggleOutput(OutputKind.SOUND)
        e.vm.toggleOutput(OutputKind.SCREEN)
        e.vm.play()
        runCurrent()
        advanceTimeBy(10)
        assertTrue(e.vm.s.screenLit)
        assertTrue(e.vm.s.isPlaying)
        e.vm.stop()
        runCurrent()
        assertFalse(e.vm.s.screenLit)
        assertFalse(e.vm.s.isPlaying)
    }

    @Test fun clear_resets() = runTest {
        val e = env()
        e.vm.onInputChange("sos")
        e.vm.play()
        runCurrent()
        e.vm.clear()
        runCurrent()
        assertEquals("", e.vm.s.input)
        assertEquals("", e.vm.s.output)
        assertFalse(e.vm.s.isPlaying)
        assertEquals(false, e.factory.created.single().second.lastState)
    }
}
