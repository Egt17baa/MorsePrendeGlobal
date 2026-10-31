package io.github.egt17baa.morseprende.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun lowering_char_wpm_lowers_effective() = runTest {
        val repo = FakeSettingsRepository(AppSettings(charWpm = 15, effectiveWpm = 12))
        val vm = SettingsViewModel(repo)
        vm.setCharWpm(8)
        assertEquals(8, repo.settings.value.effectiveWpm)
        assertEquals(8, vm.state.value.charWpm)
    }

    @Test fun setters_update_repository() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        vm.setToneHz(820)
        vm.setVolume(0.3f)
        vm.setLimitFlashes(false)
        vm.setEffectiveWpm(7)
        val s = repo.settings.value
        assertEquals(820, s.toneHz)
        assertEquals(0.3f, s.volume)
        assertEquals(false, s.limitFlashes)
        assertEquals(7, s.effectiveWpm)
    }

    @Test fun set_keyer_mode_updates_repo() = runTest {
        val repo = FakeSettingsRepository()
        SettingsViewModel(repo).setKeyerMode(KeyerMode.TWO_BUTTONS)
        assertEquals(KeyerMode.TWO_BUTTONS, repo.settings.value.keyerMode)
    }
}
