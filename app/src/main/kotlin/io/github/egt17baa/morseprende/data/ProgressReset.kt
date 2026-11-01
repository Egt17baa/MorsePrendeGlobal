package io.github.egt17baa.morseprende.data

import io.github.egt17baa.morseprende.data.progress.AppDatabase
import io.github.egt17baa.morseprende.data.save.MissionSaveStore
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** «Borrar progreso» de Ajustes. */
fun interface ProgressReset {
    suspend fun resetAll()
}

/**
 * Vacía todo lo jugado (estrellas, repaso, récords, Destellos, objetos, días, logros, fichas vistas
 * y partidas a medias) y devuelve los cosméticos a los de serie, porque los objetos comprados se
 * pierden. La velocidad, el sonido, el manipulador y la accesibilidad se mantienen.
 */
class AppProgressReset(
    private val db: AppDatabase,
    private val seen: SeenCardsRepository,
    private val saves: MissionSaveStore,
    private val settings: SettingsRepository,
) : ProgressReset {
    override suspend fun resetAll() {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        seen.clear()
        saves.clearAll()
        val d = AppSettings()
        settings.update { it.copy(lampColor = d.lampColor, toneTimbre = d.toneTimbre, keyerSkin = d.keyerSkin, decorations = d.decorations) }
    }
}
