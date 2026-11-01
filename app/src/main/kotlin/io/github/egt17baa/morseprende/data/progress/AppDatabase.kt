package io.github.egt17baa.morseprende.data.progress

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.egt17baa.morseprende.data.achievements.AchievementEntity
import io.github.egt17baa.morseprende.data.achievements.AchievementsDao
import io.github.egt17baa.morseprende.data.activity.ActivityDao
import io.github.egt17baa.morseprende.data.activity.PlayedDayEntity
import io.github.egt17baa.morseprende.data.economy.EconomyDao
import io.github.egt17baa.morseprende.data.economy.OwnedItemEntity
import io.github.egt17baa.morseprende.data.economy.WalletEntity
import io.github.egt17baa.morseprende.data.games.GameRecordDao
import io.github.egt17baa.morseprende.data.games.GameRecordEntity
import io.github.egt17baa.morseprende.data.review.CharStatsDao
import io.github.egt17baa.morseprende.data.review.CharStatsEntity

/**
 * v1: progreso de misiones. v2: + cajas de Leitner y récords de minijuegos. v3: + Destellos,
 * objetos comprados, días jugados y logros. Las migraciones automáticas solo añaden tablas.
 */
@Database(
    entities = [
        MissionProgressEntity::class, CharStatsEntity::class, GameRecordEntity::class,
        WalletEntity::class, OwnedItemEntity::class, PlayedDayEntity::class, AchievementEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun charStatsDao(): CharStatsDao
    abstract fun gameRecordDao(): GameRecordDao
    abstract fun economyDao(): EconomyDao
    abstract fun activityDao(): ActivityDao
    abstract fun achievementsDao(): AchievementsDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "morseprende.db").build()
    }
}
