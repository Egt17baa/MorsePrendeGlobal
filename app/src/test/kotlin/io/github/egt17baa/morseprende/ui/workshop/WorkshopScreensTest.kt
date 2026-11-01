package io.github.egt17baa.morseprende.ui.workshop

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.workshop.LetterDrill
import io.github.egt17baa.morseprende.ui.mission.KeyerActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkshopScreensTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val noKeyer = object : KeyerActions {
        override fun keyDown(atMs: Long) = Unit
        override fun keyUp(atMs: Long) = Unit
        override fun dot() = Unit
        override fun dash() = Unit
        override fun nextLetter() = Unit
    }

    @Test fun free_send_shows_hint_then_text_and_speed() {
        var state by androidx.compose.runtime.mutableStateOf(FreeSendUiState())
        var cleared = 0
        compose.setContent { FreeSendContent(state, noKeyer, onDelete = {}, onClear = { cleared++ }, onBack = {}) }
        compose.onNodeWithText(context.getString(R.string.workshop_send_hint)).performScrollTo()
        state = FreeSendUiState(text = "SOS", wpm = 14)
        compose.onNodeWithText("SOS").performScrollTo()
        compose.onNodeWithText(context.getString(R.string.workshop_speed, 14)).performScrollTo()
        compose.onNodeWithText(context.getString(R.string.workshop_clear)).performScrollTo().performClick()
        assertEquals(1, cleared)
    }

    @Test fun drill_setup_needs_two_letters_and_playing_shows_keys() {
        val toggled = mutableListOf<Char>()
        val answered = mutableListOf<Char>()
        var state by androidx.compose.runtime.mutableStateOf(
            LetterDrillUiState(phase = DrillPhase.SETUP, available = listOf('E', 'T', 'A'), selected = setOf('E')),
        )
        compose.setContent { LetterDrillContent(state, { toggled += it }, {}, {}, { answered += it }, {}, {}) }
        compose.onNodeWithText(context.getString(R.string.workshop_letters_start)).performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("T").performScrollTo().performClick()
        assertEquals(listOf('T'), toggled)
        state = state.copy(selected = setOf('E', 'T'))
        compose.onNodeWithText(context.getString(R.string.workshop_letters_start)).performScrollTo().assertIsEnabled()
        state = state.copy(phase = DrillPhase.PLAYING, current = 'E', stats = mapOf('E' to LetterDrill.Score(2, 3)))
        compose.onNodeWithText(context.getString(R.string.workshop_letters_which)).performScrollTo()
        compose.onNodeWithText("T").performScrollTo().performClick()
        assertEquals(listOf('T'), answered)
        compose.onNodeWithText(context.getString(R.string.workshop_letters_score, "E", 2, 3)).performScrollTo()
    }
}
