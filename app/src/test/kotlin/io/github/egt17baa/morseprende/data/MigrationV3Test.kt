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
class MigrationV3Test {

    @get:Rule val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test fun migration_2_to_3_keeps_everything() {
        helper.createDatabase(DB, 2).apply {
            execSQL("INSERT INTO mission_progress (missionId, bestStars, attempts, lastPlayedAt) VALUES ('c1m1', 3, 2, 1000)")
            execSQL("INSERT INTO char_stats (char, box, correct, wrong) VALUES ('S', 4, 9, 1)")
            execSQL("INSERT INTO game_records (game, bestScore, bestExtra, plays) VALUES ('STORM', 21, 0, 5)")
            close()
        }
        val db = helper.runMigrationsAndValidate(DB, 3, true)
        fun count(sql: String) = db.query(sql).use { it.moveToFirst(); it.getInt(0) }
        assertEquals(3, count("SELECT bestStars FROM mission_progress WHERE missionId = 'c1m1'"))
        assertEquals(4, count("SELECT box FROM char_stats WHERE char = 'S'"))
        assertEquals(21, count("SELECT bestScore FROM game_records WHERE game = 'STORM'"))
        assertEquals(0, count("SELECT COUNT(*) FROM wallet"))
        assertEquals(0, count("SELECT COUNT(*) FROM owned_items"))
        assertEquals(0, count("SELECT COUNT(*) FROM played_days"))
        assertEquals(0, count("SELECT COUNT(*) FROM achievements"))
    }

    private companion object { const val DB = "migration-v3-test.db" }
}
