package io.github.egt17baa.morseprende.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.mission.Exercise
import io.github.egt17baa.morseprende.ui.games.GamePhase
import io.github.egt17baa.morseprende.ui.games.StormContent
import io.github.egt17baa.morseprende.ui.games.StormUiState
import io.github.egt17baa.morseprende.ui.mission.MissionActions
import io.github.egt17baa.morseprende.ui.mission.MissionContent
import io.github.egt17baa.morseprende.ui.mission.MissionPhase
import io.github.egt17baa.morseprende.ui.mission.MissionUiState
import io.github.egt17baa.morseprende.ui.theme.MorsePrendeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** «Muy grande» con la letra del sistema también al máximo, en un móvil pequeño. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp", fontScale = 1.3f)
class LargeTextTest {

    @get:Rule val compose = createComposeRule()
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private class Recorder : MissionActions {
        val calls = mutableListOf<String>()
        override fun start() = Unit; override fun replay() = Unit; override fun continueListen() = Unit
        override fun answer(text: String) { calls += text }
        override fun keyDown(atMs: Long) = Unit; override fun keyUp(atMs: Long) = Unit; override fun tick() = Unit
        override fun dot() = Unit; override fun dash() = Unit; override fun nextLetter() = Unit; override fun deleteLast() = Unit
        override fun submitSend() = Unit; override fun next() = Unit; override fun exit() = Unit
        override fun useSound() = Unit; override fun resume() = Unit
    }

    @Test fun huge_text_mission_and_storm_still_usable() {
        val r = Recorder()
        var screen by androidx.compose.runtime.mutableStateOf(0)
        compose.setContent {
            MorsePrendeTheme(textScale = 1.3f) {
                when (screen) {
                    0 -> MissionContent(MissionUiState(phase = MissionPhase.EXERCISE, exercise = Exercise.ReceiveChoice("S", listOf("E", "S", "O", "T")), exerciseCount = 14), r)
                    1 -> MissionContent(MissionUiState(phase = MissionPhase.EXERCISE, exercise = Exercise.Send("SOS"), exerciseCount = 14, sentText = "S"), r)
                    else -> {
                        val keys = (('A'..'Z').toList() + 'Ñ' + ('0'..'9').toList() + listOf('.', ',', '?', '/', '\'', '!', '=', '+'))
                        StormContent(StormUiState(phase = GamePhase.PLAYING, letters = keys), {}, {}, {})
                    }
                }
            }
        }
        listOf("E", "S", "O", "T").forEach { compose.onNodeWithText(it).performScrollTo().performClick() }
        assertEquals(listOf("E", "S", "O", "T"), r.calls)
        screen = 1
        compose.onNodeWithContentDescription(context.getString(R.string.mission_key)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.mission_submit)).performScrollTo().assertIsDisplayed()
        screen = 2
        compose.onNodeWithText("+").assertIsDisplayed()
        compose.onNodeWithText("A").assertIsDisplayed()
    }
}
