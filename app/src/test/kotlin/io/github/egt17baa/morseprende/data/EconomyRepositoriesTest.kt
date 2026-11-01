package io.github.egt17baa.morseprende.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.data.achievements.RoomAchievementsRepository
import io.github.egt17baa.morseprende.data.activity.RoomActivityRepository
import io.github.egt17baa.morseprende.data.economy.RoomEconomyRepository
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import io.github.egt17baa.morseprende.game.economy.BuyResult
import io.github.egt17baa.morseprende.game.economy.Shop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class EconomyRepositoriesTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()

    @After fun close() = db.close()

    @Test fun earn_and_buy() = runTest {
        val repo = RoomEconomyRepository(db)
        assertEquals(0, repo.balance.first())
        assertEquals(Shop.defaults(), repo.owned.first())
        repo.earn(45)
        assertEquals(BuyResult.Bought, repo.buy("lamp_emerald"))
        assertEquals(5, repo.balance.first())
        assertTrue("lamp_emerald" in repo.owned.first())
    }

    @Test fun buy_twice_charges_once() = runTest {
        val repo = RoomEconomyRepository(db)
        repo.earn(100)
        assertEquals(BuyResult.Bought, repo.buy("decor_gulls"))
        assertEquals(BuyResult.AlreadyOwned, repo.buy("decor_gulls"))
        assertEquals(70, repo.balance.first())
    }

    @Test fun buy_without_balance_keeps_balance() = runTest {
        val repo = RoomEconomyRepository(db)
        repo.earn(10)
        assertEquals(BuyResult.NotEnough(50), repo.buy("lamp_polar"))
        assertEquals(10, repo.balance.first())
        assertFalse("lamp_polar" in repo.owned.first())
    }

    @Test fun mark_played_first_of_day() = runTest {
        val repo = RoomActivityRepository(db)
        val day = LocalDate.of(2026, 11, 1)
        assertTrue(repo.markPlayed(day))
        assertFalse(repo.markPlayed(day))
        assertTrue(repo.markPlayed(day.plusDays(1)))
        assertEquals(setOf(day, day.plusDays(1)), repo.playedDays.first())
    }

    @Test fun unlock_returns_only_new() = runTest {
        val repo = RoomAchievementsRepository(db)
        assertEquals(setOf("first_sos"), repo.unlock(setOf("first_sos"), 100))
        assertEquals(setOf("keeper"), repo.unlock(setOf("first_sos", "keeper"), 200))
        assertEquals(mapOf("first_sos" to 100L, "keeper" to 200L), repo.unlocked.first())
    }
}
