package io.github.egt17baa.morseprende

import android.app.Application
import android.content.Context
import io.github.egt17baa.morseprende.data.AssetContentSource
import io.github.egt17baa.morseprende.data.ContentSource
import io.github.egt17baa.morseprende.data.DataStoreSeenCardsRepository
import io.github.egt17baa.morseprende.data.SeenCardsRepository
import io.github.egt17baa.morseprende.data.progress.AppDatabase
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
    val progress: ProgressRepository by lazy { RoomProgressRepository(AppDatabase.create(appContext)) }
}

class MorsePrendeApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
