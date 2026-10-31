package io.github.egt17baa.morseprende.ui.mission

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.game.mission.Exercise
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
import io.github.egt17baa.morseprende.settings.KeyerMode
import io.github.egt17baa.morseprende.translator.NoOpOutputFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MissionScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun str(id: Int) = context.getString(id)

    private class Recorder : MissionActions {
        val calls = mutableListOf<String>()
        override fun start() { calls += "start" }
        override fun replay() { calls += "replay" }
        override fun continueListen() { calls += "continueListen" }
        override fun answer(text: String) { calls += "answer:$text" }
        override fun keyDown() { calls += "down" }
        override fun keyUp() { calls += "up" }
        override fun tick() = Unit
        override fun dot() { calls += "dot" }
        override fun dash() { calls += "dash" }
        override fun nextLetter() { calls += "nextLetter" }
        override fun deleteLast() { calls += "delete" }
        override fun submitSend() { calls += "submit" }
        override fun next() { calls += "next" }
        override fun exit() { calls += "exit" }
    }

    @Test fun briefing_shows_and_starts() {
        val r = Recorder()
        compose.setContent { MissionContent(MissionUiState(title = "La primera luz", briefing = "Aprende S y O"), r) }
        compose.onNodeWithText("Aprende S y O").assertIsDisplayed()
        compose.onNodeWithText(str(R.string.mission_start)).performScrollTo().performClick()
        assertEquals(listOf("start"), r.calls)
    }

    @Test fun choice_options_are_clickable() {
        val r = Recorder()
        val state = MissionUiState(phase = MissionPhase.EXERCISE, exercise = Exercise.ReceiveChoice("S", listOf("O", "S")), exerciseCount = 5)
        compose.setContent { MissionContent(state, r) }
        compose.onNodeWithText("S").performScrollTo().performClick()
        assertEquals(listOf("answer:S"), r.calls)
    }

    @Test fun two_button_pad_shown_when_mode_two() {
        val r = Recorder()
        val state = MissionUiState(phase = MissionPhase.EXERCISE, exercise = Exercise.Send("S"), keyerMode = KeyerMode.TWO_BUTTONS, exerciseCount = 5)
        compose.setContent { MissionContent(state, r) }
        compose.onNodeWithText(str(R.string.mission_dot)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.mission_dash)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.mission_next_letter)).performScrollTo().performClick()
        compose.onNodeWithText(str(R.string.mission_submit)).performScrollTo().performClick()
        assertEquals(listOf("dot", "dash", "nextLetter", "submit"), r.calls)
    }

    @Test fun error_state_shows_message() {
        val r = Recorder()
        compose.setContent { MissionContent(MissionUiState(phase = MissionPhase.ERROR), r) }
        compose.onNodeWithText(str(R.string.mission_error)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.mission_back_to_lighthouse)).performClick()
        assertEquals(listOf("exit"), r.calls)
    }

    @Test fun feedback_shows_expected_and_continue() {
        val r = Recorder()
        val state = MissionUiState(
            phase = MissionPhase.EXERCISE, exercise = Exercise.ReceiveTyped("SOS"), feedback = Feedback(false, "SOS"), exerciseCount = 5,
        )
        compose.setContent { MissionContent(state, r) }
        compose.onNodeWithText(context.getString(R.string.mission_wrong, "SOS")).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(str(R.string.mission_continue)).performScrollTo().performClick()
        assertEquals(listOf("next"), r.calls)
    }

    @Test fun leaving_screen_abandons() {
        val sidetones = FakeSidetoneFactory()
        val vm = MissionViewModel(
            "t1", FakeContentSource(listOf(MissionTestContent.chapter)), FakeProgressRepository(),
            FakeSettingsRepository(), NoOpOutputFactory, sidetones,
        )
        var show by mutableStateOf(true)
        compose.setContent { if (show) MissionScreen(vm, onExit = {}) }
        compose.onNodeWithText(str(R.string.mission_start)).performScrollTo().performClick()
        compose.waitForIdle()
        show = false
        compose.waitForIdle()
        assertTrue(sidetones.last.closed)
    }
}
