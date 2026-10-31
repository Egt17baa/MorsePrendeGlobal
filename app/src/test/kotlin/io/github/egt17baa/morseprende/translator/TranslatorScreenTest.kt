package io.github.egt17baa.morseprende.translator

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TranslatorScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun show(torchAvailable: Boolean = true) {
        val vm = TranslatorViewModel(FakeSettingsRepository(), NoOpOutputFactory, torchAvailable)
        compose.setContent { TranslatorScreen(vm, onBack = {}) }
    }

    @Test fun typing_shows_morse() {
        show()
        compose.onNode(hasSetTextAction()).performTextInput("sos")
        compose.onNodeWithText(MorseDisplay.pretty("... --- ...")).assertIsDisplayed()
    }

    @Test fun torch_chip_hidden_without_flash() {
        show(torchAvailable = false)
        compose.onNodeWithText(context.getString(R.string.output_torch)).assertDoesNotExist()
    }

    @Test fun torch_chip_shown_with_flash() {
        show(torchAvailable = true)
        compose.onNodeWithText(context.getString(R.string.output_torch)).performScrollTo().assertIsDisplayed()
    }

    @Test fun warning_dialog_appears() {
        show()
        compose.onNodeWithText(context.getString(R.string.output_screen)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.flash_warning_title)).assertIsDisplayed()
    }

    @Test fun unsupported_characters_are_reported() {
        show()
        compose.onNode(hasSetTextAction()).performTextInput("a#")
        compose.onNodeWithText(context.getString(R.string.translator_unsupported, "#")).performScrollTo().assertIsDisplayed()
    }

    @Test fun screen_stays_on_while_playing() {
        val vm = TranslatorViewModel(FakeSettingsRepository(), EndlessOutputFactory, true)
        var view: android.view.View? = null
        compose.setContent {
            view = LocalView.current
            TranslatorScreen(vm, onBack = {})
        }
        compose.onNode(hasSetTextAction()).performTextInput("sos")
        compose.onNodeWithText(context.getString(R.string.translator_play)).performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(view!!.keepScreenOn)
        compose.onNodeWithText(context.getString(R.string.translator_stop)).performScrollTo().performClick()
        compose.waitForIdle()
        assertFalse(view!!.keepScreenOn)
    }

    @Test fun leaving_the_app_stops_playback() {
        val vm = TranslatorViewModel(FakeSettingsRepository(), EndlessOutputFactory, true)
        val owner = TestLifecycleOwner(Lifecycle.State.RESUMED, Dispatchers.Main.immediate)
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) { TranslatorScreen(vm, onBack = {}) }
        }
        compose.onNode(hasSetTextAction()).performTextInput("sos")
        compose.onNodeWithText(context.getString(R.string.translator_play)).performScrollTo().performClick()
        compose.waitForIdle()
        assertTrue(vm.state.value.isPlaying)
        compose.runOnIdle { owner.handleLifecycleEvent(Lifecycle.Event.ON_STOP) }
        compose.waitForIdle()
        assertFalse(vm.state.value.isPlaying)
    }

    @Test fun speed_control_is_on_the_translator() {
        show()
        compose.onNodeWithText(context.getString(R.string.translator_speed, 18)).performScrollTo().assertIsDisplayed()
    }
}
