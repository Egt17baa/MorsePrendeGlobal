package io.github.egt17baa.morseprende.ui.stats

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.review.Mastery
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StatsScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun stats_practice_opens_drill() {
        val practiced = mutableListOf<List<Char>>()
        val state = StatsUiState(
            loaded = true, missionsDone = 3, missionsTotal = 8, stars = 6, starsMax = 24,
            letters = listOf(LetterStat('S', Mastery.Level.MASTERED), LetterStat('O', Mastery.Level.LEARNING), LetterStat('E', Mastery.Level.GOOD)),
            weakest = listOf('O', 'E'),
        )
        compose.setContent { StatsContent(state, onPractice = { practiced += it }, onBack = {}) }
        compose.onNodeWithText(context.getString(R.string.stats_missions, 3, 8)).performScrollTo()
        // Cada letra dice su nivel a TalkBack (no solo con color).
        compose.onNodeWithContentDescription(context.getString(R.string.stats_letter_desc, "S", context.getString(R.string.stats_level_mastered))).performScrollTo()
        compose.onNodeWithText(context.getString(R.string.stats_no_records)).performScrollTo()
        compose.onNodeWithText(context.getString(R.string.stats_practice)).performScrollTo().performClick()
        assertEquals(listOf(listOf('O', 'E')), practiced)
    }

    @Test fun stats_new_player_screen() {
        compose.setContent { StatsContent(StatsUiState(loaded = true), onPractice = {}, onBack = {}) }
        compose.onNodeWithText(context.getString(R.string.stats_letters_empty)).performScrollTo()
    }
}
