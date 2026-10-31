package io.github.egt17baa.morseprende.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.data.games.RoomGameRecordsRepository
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import io.github.egt17baa.morseprende.data.review.RoomCharStatsRepository
import io.github.egt17baa.morseprende.game.review.CharOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StatsRepositoriesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    @After fun close() = db.close()

    @Test fun char_stats_apply_leitner() = runTest {
        val repo = RoomCharStatsRepository(db)
        repo.record(listOf(CharOutcome('S', true), CharOutcome('S', true), CharOutcome('O', false)))
        assertEquals(mapOf('S' to 3, 'O' to 1), repo.boxes.first())
        repo.record(listOf(CharOutcome('S', false)))
        assertEquals(1, repo.boxes.first()['S'])
    }

    @Test fun game_record_keeps_best_and_counts_plays() = runTest {
        val repo = RoomGameRecordsRepository(db)
        assertTrue(repo.recordGame(GameId.STORM, 5, 0))
        assertFalse(repo.recordGame(GameId.STORM, 3, 0))
        assertTrue(repo.recordGame(GameId.STORM, 8, 0))
        val r = repo.records.first().getValue(GameId.STORM)
        assertEquals(8, r.bestScore)
        assertEquals(3, r.plays)
    }

    @Test fun flash_extra_lower_is_better() = runTest {
        val repo = RoomGameRecordsRepository(db)
        repo.recordGame(GameId.FLASH, 10, 900)
        repo.recordGame(GameId.FLASH, 5, 700)
        repo.recordGame(GameId.FLASH, 5, 0)
        assertEquals(700, repo.records.first().getValue(GameId.FLASH).bestExtra)
    }

    @Test fun intercept_extra_higher_is_better() = runTest {
        val repo = RoomGameRecordsRepository(db)
        repo.recordGame(GameId.INTERCEPT, 3, 14)
        repo.recordGame(GameId.INTERCEPT, 2, 12)
        assertEquals(14, repo.records.first().getValue(GameId.INTERCEPT).bestExtra)
    }
}
