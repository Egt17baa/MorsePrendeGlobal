package io.github.egt17baa.morseprende.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import io.github.egt17baa.morseprende.data.progress.MissionProgress
import io.github.egt17baa.morseprende.data.progress.RoomProgressRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomProgressRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
    private val repo = RoomProgressRepository(db)

    @After fun close() = db.close()

    @Test fun empty_initially() = runTest { assertEquals(emptyMap<String, MissionProgress>(), repo.progress.first()) }

    @Test fun records_and_reads() = runTest {
        repo.recordResult("c1m1", 2, 1_000)
        assertEquals(mapOf("c1m1" to MissionProgress("c1m1", 2, 1, 1_000)), repo.progress.first())
    }

    @Test fun keeps_best_stars() = runTest {
        repo.recordResult("c1m1", 3, 1_000)
        repo.recordResult("c1m1", 1, 2_000)
        assertEquals(MissionProgress("c1m1", 3, 2, 2_000), repo.progress.first()["c1m1"])
    }
}
