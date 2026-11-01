package io.github.egt17baa.morseprende.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.data.achievements.RoomAchievementsRepository
import io.github.egt17baa.morseprende.data.activity.RoomActivityRepository
import io.github.egt17baa.morseprende.data.economy.RoomEconomyRepository
import io.github.egt17baa.morseprende.data.games.RoomGameRecordsRepository
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import io.github.egt17baa.morseprende.data.progress.RoomProgressRepository
import io.github.egt17baa.morseprende.data.review.RoomCharStatsRepository
import io.github.egt17baa.morseprende.data.save.FileMissionSaveStore
import io.github.egt17baa.morseprende.game.economy.Shop
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.game.review.CharOutcome
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ProgressResetTest {

    @get:Rule val tmp = TemporaryFolder()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()

    @After fun close() = db.close()

    @Test fun reset_clears_everything_but_settings() = runBlocking {
        val progress = RoomProgressRepository(db)
        val stats = RoomCharStatsRepository(db)
        val records = RoomGameRecordsRepository(db)
        val economy = RoomEconomyRepository(db)
        val activity = RoomActivityRepository(db)
        val achievements = RoomAchievementsRepository(db)
        val seen = InMemorySeen()
        val saves = FileMissionSaveStore(tmp.newFolder("partidas"))
        val settings = FakeSettingsRepository(AppSettings(charWpm = 22, lampColor = "lamp_red", keyerSkin = "key_brass", decorations = setOf("decor_gulls")))
        withContext(Dispatchers.IO) {
            progress.recordResult("c1m1", 3, 1L)
            stats.record(listOf(CharOutcome('S', true)))
            records.recordGame(GameId.STORM, 12, 0)
            economy.earn(80)
            activity.markPlayed(LocalDate.of(2026, 11, 1))
            achievements.unlock(setOf("first_sos"), 1L)
        }
        seen.markSeen("lib-sos")
        saves.save("c1m2", "{}")

        AppProgressReset(db, seen, saves, settings).resetAll()

        withTimeout(5_000) {
            assertTrue(progress.progress.first().isEmpty())
            assertTrue(stats.boxes.first().isEmpty())
            assertTrue(records.records.first().isEmpty())
            assertEquals(0, economy.balance.first())
            assertEquals(Shop.defaults(), economy.owned.first())
            assertTrue(activity.playedDays.first().isEmpty())
            assertTrue(achievements.unlocked.first().isEmpty())
        }
        assertTrue(seen.seen.first().isEmpty())
        assertNull(saves.load("c1m2"))
        val s = settings.settings.first()
        assertEquals(22, s.charWpm)
        assertEquals(AppSettings().lampColor, s.lampColor)
        assertEquals(AppSettings().keyerSkin, s.keyerSkin)
        assertEquals(emptySet<String>(), s.decorations)
    }

    @Test fun open_progress_flow_sees_the_reset() = runBlocking {
        // El Faro ya está abierto (flujo de Room activo): debe pasar a vacío sin reabrirlo.
        val progress = RoomProgressRepository(db)
        withContext(Dispatchers.IO) { progress.recordResult("c1m1", 3, 1L) }
        withTimeout(5_000) { progress.progress.first { it.isNotEmpty() } }
        val emptied = async(Dispatchers.IO) { withTimeout(5_000) { progress.progress.first { it.isEmpty() } } }
        AppProgressReset(db, InMemorySeen(), FileMissionSaveStore(tmp.newFolder("p")), FakeSettingsRepository()).resetAll()
        assertTrue(emptied.await().isEmpty())
    }

    private class InMemorySeen : SeenCardsRepository {
        private val state = kotlinx.coroutines.flow.MutableStateFlow(emptySet<String>())
        override val seen = state
        override suspend fun markSeen(cardId: String) { state.value = state.value + cardId }
        override suspend fun clear() { state.value = emptySet() }
    }
}
