package io.github.egt17baa.morseprende

import android.app.Application
import android.content.Context
import io.github.egt17baa.morseprende.data.AssetContentSource
import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.DataStoreSeenCardsRepository
import io.github.egt17baa.morseprende.data.SeenCardsRepository
import io.github.egt17baa.morseprende.data.games.GameRecordsRepository
import io.github.egt17baa.morseprende.data.games.RoomGameRecordsRepository
import io.github.egt17baa.morseprende.data.progress.AppDatabase
import io.github.egt17baa.morseprende.data.review.CharStatsRepository
import io.github.egt17baa.morseprende.data.review.RoomCharStatsRepository
import io.github.egt17baa.morseprende.data.progress.ProgressRepository
import io.github.egt17baa.morseprende.data.progress.RoomProgressRepository
import io.github.egt17baa.morseprende.playback.AndroidOutputFactory
import io.github.egt17baa.morseprende.playback.AndroidSidetoneFactory
import io.github.egt17baa.morseprende.playback.SidetoneFactory
import io.github.egt17baa.morseprende.playback.OutputFactory
import io.github.egt17baa.morseprende.playback.TorchOutput
import io.github.egt17baa.morseprende.settings.DataStoreSettingsRepository
import io.github.egt17baa.morseprende.settings.SettingsRepository

/** Dependencias compartidas de la app (inyección manual). */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settings: SettingsRepository = DataStoreSettingsRepository(appContext)
    val outputs: OutputFactory = AndroidOutputFactory(appContext)
    val torchAvailable: Boolean by lazy { TorchOutput.isAvailable(appContext) }
    val content: ContentSource = AssetContentSource(appContext)
    val sidetone: SidetoneFactory = AndroidSidetoneFactory(appContext)
    val seenCards: SeenCardsRepository = DataStoreSeenCardsRepository(appContext)
    val missionSaves: io.github.egt17baa.morseprende.data.save.MissionSaveStore =
        io.github.egt17baa.morseprende.data.save.FileMissionSaveStore(java.io.File(appContext.filesDir, "partidas"))
    private val database: AppDatabase by lazy { AppDatabase.create(appContext) }
    val progress: ProgressRepository by lazy { RoomProgressRepository(database) }
    val charStats: CharStatsRepository by lazy { RoomCharStatsRepository(database) }
    val gameRecords: GameRecordsRepository by lazy { RoomGameRecordsRepository(database) }
    val economy: io.github.egt17baa.morseprende.data.economy.EconomyRepository by lazy { io.github.egt17baa.morseprende.data.economy.RoomEconomyRepository(database) }
    val activity: io.github.egt17baa.morseprende.data.activity.ActivityRepository by lazy { io.github.egt17baa.morseprende.data.activity.RoomActivityRepository(database) }
    val achievements: io.github.egt17baa.morseprende.data.achievements.AchievementsRepository by lazy { io.github.egt17baa.morseprende.data.achievements.RoomAchievementsRepository(database) }
    val progressReset: io.github.egt17baa.morseprende.data.ProgressReset by lazy {
        io.github.egt17baa.morseprende.data.AppProgressReset(database, seenCards, missionSaves, settings)
    }
    val rewards: io.github.egt17baa.morseprende.rewards.RewardsService by lazy {
        io.github.egt17baa.morseprende.rewards.RewardsService(content, progress, gameRecords, seenCards, economy, activity, achievements)
    }
}

class MorsePrendeApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
