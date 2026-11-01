package io.github.egt17baa.morseprende.ui.games

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.games.NightChannel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NightWatchScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun vibration_message_offers_sound() {
        var used = 0
        var state by androidx.compose.runtime.mutableStateOf(
            NightWatchUiState(phase = GamePhase.PLAYING, text = "ECO", channel = NightChannel.VIBRATION, canRepeat = true),
        )
        compose.setContent { NightWatchContent(state, {}, {}, {}, {}, {}, onUseSound = { used++ }) }
        compose.onNodeWithText(context.getString(R.string.mission_vibration_use_sound)).performScrollTo().performClick()
        assertEquals(1, used)
        // Ya por sonido: el botón desaparece y el aviso lo dice.
        state = state.copy(vibrationFallback = true)
        compose.onNodeWithText(context.getString(R.string.mission_channel_vibration_fallback)).assertIsDisplayed()
        assertEquals(0, compose.onAllNodes(androidx.compose.ui.test.hasText(context.getString(R.string.mission_vibration_use_sound))).fetchSemanticsNodes().size)
        // El botón «Comprobar» se alcanza desplazando (con el teclado abierto no cabría).
        compose.onNodeWithText(context.getString(R.string.mission_check)).performScrollTo()
    }
}
