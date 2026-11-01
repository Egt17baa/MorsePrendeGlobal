package io.github.egt17baa.morseprende.settings

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun str(id: Int) = context.getString(id)

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun reset_needs_confirmation() {
        var resets = 0
        val vm = SettingsViewModel(FakeSettingsRepository(), reset = { resets++ })
        compose.setContent { SettingsScreen(vm, onBack = {}) }
        compose.onNodeWithText(str(R.string.settings_reset)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.settings_reset_title)).assertExists()
        assertEquals(0, resets)
        compose.onNodeWithText(str(R.string.settings_reset_cancel)).performClick()
        assertEquals(0, resets)
        compose.onNodeWithText(str(R.string.settings_reset)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.settings_reset_confirm)).performClick()
        compose.waitForIdle()
        assertEquals(1, resets)
    }

    @Test fun accessibility_controls_update_settings() {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        compose.setContent { SettingsScreen(vm, onBack = {}) }
        compose.onNodeWithText(str(R.string.settings_text_huge)).performScrollTo().performClick()
        compose.onNodeWithTag("settings_high_contrast").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1.3f, repo.settings.value.textScale, 1e-6f)
        assertEquals(true, repo.settings.value.highContrast)
    }
}
