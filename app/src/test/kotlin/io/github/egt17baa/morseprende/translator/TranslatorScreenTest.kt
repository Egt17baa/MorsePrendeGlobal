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
}
