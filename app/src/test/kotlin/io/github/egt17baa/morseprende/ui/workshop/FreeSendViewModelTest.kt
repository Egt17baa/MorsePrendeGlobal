package io.github.egt17baa.morseprende.ui.workshop

import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
import io.github.egt17baa.morseprende.ui.mission.FakeSidetoneFactory
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

@OptIn(ExperimentalCoroutinesApi::class)
class FreeSendViewModelTest {

    @After fun tearDown() = Dispatchers.resetMain()

    private var now = 0L
    private val tones = FakeSidetoneFactory()

    /** 20 PPM: unidad de 60 ms. */
    private fun TestScope.vm(): FreeSendViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return FreeSendViewModel(FakeSettingsRepository(AppSettings(charWpm = 20, effectiveWpm = 20)), tones) { now }.also { it.enter() }
    }

    private fun FreeSendViewModel.key(from: Long, to: Long) {
        keyDown(from)
        keyUp(to)
    }

    private fun FreeSendViewModel.tickAt(t: Long) {
        now = t
        tick()
    }

    @Test fun free_send_decodes_letters_and_words() = runTest {
        val v = vm()
        // S (···), pausa de palabra, T (—).
        v.key(0, 60); v.key(120, 180); v.key(240, 300)
        assertEquals("...", v.state.value.pending)
        v.tickAt(460)
        assertEquals("S", v.state.value.text)
        v.tickAt(700)
        v.key(760, 940)
        v.tickAt(1_120)
        assertEquals("S T", v.state.value.text)
        // Una letra que no existe (········) se marca, no se inventa.
        repeat(8) { i -> v.key(1_200L + i * 120, 1_260L + i * 120) }
        v.tickAt(2_400)
        assertTrue(v.state.value.text.endsWith(MorseCodec.UNKNOWN.toString()))
    }

    @Test fun free_send_shows_speed() = runTest {
        val v = vm()
        // Teclea más despacio (unidad de 100 ms): la velocidad mostrada baja hacia 12 PPM.
        repeat(12) { i -> v.key(i * 400L, i * 400L + 100) }
        assertTrue("ppm ${v.state.value.wpm}", v.state.value.wpm in 12..15)
    }

    @Test fun free_send_exit_stops_tone() = runTest {
        val v = vm()
        assertTrue(tones.last.warmed)
        v.keyDown(0)
        v.leave()
        assertEquals(listOf("down", "up", "close"), tones.last.calls)
    }

    @Test fun free_send_text_capped_and_editable() = runTest {
        val v = vm()
        repeat(250) { v.dot(); v.nextLetter() }
        assertEquals(FreeSendViewModel.MAX_CHARS, v.state.value.text.length)
        v.deleteLast()
        assertEquals(FreeSendViewModel.MAX_CHARS - 1, v.state.value.text.length)
        v.clear()
        assertEquals("", v.state.value.text)
    }

    /** Como DataStore: el primer valor llega un poco después de abrir la pantalla. */
    private fun lateSettings(prefs: AppSettings) = object : io.github.egt17baa.morseprende.settings.SettingsRepository {
        override val settings = kotlinx.coroutines.flow.flow {
            kotlinx.coroutines.delay(50)
            emit(prefs)
        }
        override suspend fun update(transform: (AppSettings) -> AppSettings) = Unit
    }

    @Test fun free_send_uses_user_settings_even_if_they_load_late() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val mine = AppSettings(toneHz = 800, charWpm = 10, effectiveWpm = 10)
        val v = FreeSendViewModel(lateSettings(mine), tones) { now }
        v.enter()
        advanceTimeBy(60)
        runCurrent()
        assertEquals(listOf(800), tones.settingsUsed.map { it.toneHz })
        assertEquals(10, v.state.value.wpm)
    }

    @Test fun free_send_leave_while_loading_opens_nothing() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val v = FreeSendViewModel(lateSettings(AppSettings()), tones) { now }
        v.enter()
        v.leave()
        advanceTimeBy(60)
        runCurrent()
        assertTrue(tones.created.isEmpty())
    }

    @Test fun free_send_speed_shown_only_after_keying() = runTest {
        val v = vm()
        assertEquals(false, v.state.value.keyed)
        v.key(0, 60)
        assertEquals(true, v.state.value.keyed)
    }

    @Test fun free_send_delete_in_gap_adds_no_space() = runTest {
        val v = vm()
        v.key(0, 60)
        v.tickAt(220)
        v.key(400, 580)
        v.tickAt(740)
        assertEquals("ET", v.state.value.text)
        // Se borra la T en plena pausa: no debe aparecer después un espacio suelto.
        v.deleteLast()
        v.tickAt(1_200)
        assertEquals("E", v.state.value.text)
    }
}
