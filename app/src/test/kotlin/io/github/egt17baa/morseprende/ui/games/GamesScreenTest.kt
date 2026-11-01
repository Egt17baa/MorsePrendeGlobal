package io.github.egt17baa.morseprende.ui.games

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GamesScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun storm_keyboard_shows_learned_letters() {
        val typed = mutableListOf<Char>()
        val state = StormUiState(phase = GamePhase.PLAYING, letters = listOf('A', 'E', 'S'))
        compose.setContent { StormContent(state, onStart = {}, onInput = { typed += it }, onBack = {}) }
        compose.onNodeWithText("E").performClick()
        compose.onNodeWithText("S").performClick()
        assertEquals(listOf('E', 'S'), typed)
    }

    @Test fun game_over_panel_shows_record() {
        var again = 0
        compose.setContent { GameOverPanel(score = 14, isNewRecord = true, onAgain = { again++ }, onBack = {}) }
        compose.onNodeWithText(context.getString(R.string.game_new_record)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.game_score, 14)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.game_again)).performClick()
        assertEquals(1, again)
    }

    @Test fun locked_game_explains() {
        compose.setContent { StormContent(StormUiState(phase = GamePhase.LOCKED), onStart = {}, onInput = {}, onBack = {}) }
        compose.onNodeWithText(context.getString(R.string.game_locked)).assertIsDisplayed()
    }

    @Test fun storm_keyboard_with_45_keys_fits_without_scrolling() {
        val typed = mutableListOf<Char>()
        val letters = (('A'..'Z').toList() + 'Ñ' + ('0'..'9').toList() + listOf('.', ',', '?', '/', '\'', '!', '=', '+')).sorted()
        compose.setContent { StormContent(StormUiState(phase = GamePhase.PLAYING, letters = letters), onStart = {}, onInput = { typed += it }, onBack = {}) }
        // Todas las teclas se ven a la vez: en plena partida no hay que desplazar nada.
        letters.forEach { compose.onNodeWithText(it.toString()).assertIsDisplayed() }
        compose.onNodeWithText("Z").performClick()
        compose.onNodeWithText("+").performClick()
        assertEquals(listOf('Z', '+'), typed)
    }

    @Test fun storm_keyboard_puts_letters_first() {
        assertEquals(listOf('A', 'Z', 'Ñ', '1', '9', '+', '?'), stormKeyOrder(listOf('?', '1', 'Z', '+', 'A', '9', 'Ñ')))
    }
}
