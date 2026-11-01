package io.github.egt17baa.morseprende.ui.mission

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        override fun keyDown(atMs: Long) { calls += "down@$atMs" }
        override fun keyUp(atMs: Long) { calls += "up@$atMs" }
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
        val state = MissionUiState(phase = MissionPhase.EXERCISE, exercise = Exercise.Send("S"), keyerMode = KeyerMode.TWO_BUTTONS, exerciseCount = 5, sentText = "S")
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

    @Test fun submit_disabled_when_nothing_keyed() {
        val state = MissionUiState(phase = MissionPhase.EXERCISE, exercise = Exercise.Send("S"), exerciseCount = 5)
        compose.setContent { MissionContent(state, Recorder()) }
        compose.onNodeWithText(str(R.string.mission_submit)).performScrollTo().assertIsNotEnabled()
    }

    @Test fun drifting_finger_keeps_the_press_and_does_not_scroll() {
        val r = Recorder()
        compose.setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                KeyerPad(KeyerMode.ONE_BUTTON, r)
                Spacer(Modifier.height(2000.dp))
            }
        }
        compose.onNodeWithContentDescription(str(R.string.mission_key)).performTouchInput {
            down(center)
            advanceEventTime(150)
            moveBy(Offset(0f, 300f))
            advanceEventTime(250)
            up()
        }
        compose.waitForIdle()
        assertEquals(2, r.calls.size)
        val down = r.calls[0].substringAfter("@").toLong()
        val up = r.calls[1].substringAfter("@").toLong()
        assertTrue(r.calls[0].startsWith("down") && r.calls[1].startsWith("up"))
        assertEquals(150L + 16L + 250L, up - down)   // moveBy añade un periodo de evento (16 ms)
    }

    @Test fun debriefing_panel_shows_reward_and_achievement() {
        val state = MissionUiState(
            phase = MissionPhase.DEBRIEFING, debriefing = "¡Bien!", stars = 3,
            reward = io.github.egt17baa.morseprende.rewards.RewardSummary(40, dailyBonus = true, newAchievements = listOf("first_sos")),
        )
        compose.setContent { MissionContent(state, Recorder()) }
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.reward_destellos, 40, 40)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.reward_daily)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.reward_achievement, context.getString(R.string.ach_first_sos))).performScrollTo().assertIsDisplayed()
    }

    @Test fun enye_notice_in_listen() {
        val state = MissionUiState(phase = MissionPhase.EXERCISE, exercise = Exercise.Listen(listOf('Ñ')), exerciseCount = 5)
        compose.setContent { MissionContent(state, Recorder()) }
        compose.onNodeWithText(str(R.string.mission_enye_notice)).performScrollTo().assertIsDisplayed()
    }

    @Test fun timer_and_noise_shown() {
        val state = MissionUiState(
            phase = MissionPhase.EXERCISE, exercise = Exercise.ReceiveTyped("SOS"), exerciseCount = 5, secondsLeft = 9, noisy = true,
        )
        compose.setContent { MissionContent(state, Recorder()) }
        compose.onNodeWithText(context.getString(R.string.mission_seconds_left, 9)).assertIsDisplayed()
        compose.onNodeWithText(str(R.string.mission_noise)).assertIsDisplayed()
    }
}
