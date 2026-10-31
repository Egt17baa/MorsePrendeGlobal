package io.github.egt17baa.morseprende.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun migration_1_to_2_keeps_progress() {
        helper.createDatabase(DB, 1).apply {
            execSQL("INSERT INTO mission_progress (missionId, bestStars, attempts, lastPlayedAt) VALUES ('c1m1', 3, 2, 1000)")
            close()
        }
        val db = helper.runMigrationsAndValidate(DB, 2, true)
        db.query("SELECT missionId, bestStars, attempts FROM mission_progress").use { c ->
            assertEquals(1, c.count)
            c.moveToFirst()
            assertEquals("c1m1", c.getString(0))
            assertEquals(3, c.getInt(1))
            assertEquals(2, c.getInt(2))
        }
        db.query("SELECT COUNT(*) FROM char_stats").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
        db.query("SELECT COUNT(*) FROM game_records").use { c -> c.moveToFirst(); assertEquals(0, c.getInt(0)) }
    }

    private companion object { const val DB = "migration-test.db" }
}
