package io.github.egt17baa.morseprende.ui.practice

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeGameRecordsRepository
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.data.games.GameRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PracticeTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After fun tearDown() = Dispatchers.resetMain()

    private fun vm(vararg completed: String, records: Map<GameId, GameRecord> = emptyMap()): PracticeViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        return PracticeViewModel(
            FakeContentSource(listOf(FakeContentSource.realChapter1)),
            FakeProgressRepository.completed(*completed.map { it to 1 }.toTypedArray()),
            FakeGameRecordsRepository(records),
        )
    }

    @Test fun locked_until_mission() = runTest {
        val games = vm("c1m1", "c1m2").state.value.games
        assertEquals(listOf(GameId.STORM, GameId.FLASH, GameId.INTERCEPT, GameId.NIGHT_WATCH), games.map { it.id })
        assertTrue(games[0].unlocked)
        assertFalse(games[1].unlocked)
        assertEquals("El pesquero Gaviota", games[1].unlockMissionTitle)
        assertFalse(games[2].unlocked)
        // La Guardia nocturna se abre al terminar la campaña.
        assertFalse(games[3].unlocked)
    }

    @Test fun shows_records() = runTest {
        val s = vm("c1m1", "c1m2", records = mapOf(GameId.STORM to GameRecord(GameId.STORM, 12, 0, 3))).state.value
        assertEquals(12, s.games[0].record?.bestScore)
        assertEquals(3, s.games[0].record?.plays)
    }

    @Test fun tapping_unlocked_opens_game() {
        val opened = mutableListOf<GameId>()
        val state = PracticeUiState(
            listOf(
                PracticeGame(GameId.STORM, unlocked = true, unlockMissionTitle = null, record = null),
                PracticeGame(GameId.FLASH, unlocked = false, unlockMissionTitle = "El pesquero Gaviota", record = null),
            ),
        )
        compose.setContent { PracticeContent(state, onGame = { opened += it }, onBack = {}) }
        compose.onNodeWithText(context.getString(R.string.game_storm)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.game_flash)).performScrollTo().performClick()
        assertEquals(listOf(GameId.STORM), opened)
    }
}
