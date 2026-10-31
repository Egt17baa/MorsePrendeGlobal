package io.github.egt17baa.morseprende.data.progress

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.egt17baa.morseprende.data.games.GameRecordDao
import io.github.egt17baa.morseprende.data.games.GameRecordEntity
import io.github.egt17baa.morseprende.data.review.CharStatsDao
import io.github.egt17baa.morseprende.data.review.CharStatsEntity

/**
 * v1: progreso de misiones. v2: + cajas de Leitner y récords de minijuegos (migración automática:
 * solo añade tablas, el progreso se conserva).
 */
@Database(
    entities = [MissionProgressEntity::class, CharStatsEntity::class, GameRecordEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun charStatsDao(): CharStatsDao
    abstract fun gameRecordDao(): GameRecordDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "morseprende.db").build()
    }
}
