package io.github.egt17baa.morseprende.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

interface SettingsRepository {
    val settings: Flow<AppSettings>

    /** Guarda `transform(actual).clamped()`. */
    suspend fun update(transform: (AppSettings) -> AppSettings)
}

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository private constructor(
    private val dataStore: DataStore<Preferences>,
    private val ownedScope: CoroutineScope?,
) : SettingsRepository {

    constructor(context: Context) : this(context.applicationContext.settingsDataStore, null)

    override val settings: Flow<AppSettings> = dataStore.data.map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val next = transform(prefs.toSettings()).clamped()
            prefs[CHAR_WPM] = next.charWpm
            prefs[EFFECTIVE_WPM] = next.effectiveWpm
            prefs[TONE_HZ] = next.toneHz
            prefs[VOLUME] = next.volume
            prefs[LIMIT_FLASHES] = next.limitFlashes
            prefs[FLASH_WARNING_ACCEPTED] = next.flashWarningAccepted
            prefs[KEYER_MODE] = next.keyerMode.name
        }
    }

    /** Libera el fichero. Solo para repositorios creados con [create]. */
    fun close() {
        ownedScope?.cancel()
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            charWpm = this[CHAR_WPM] ?: defaults.charWpm,
            effectiveWpm = this[EFFECTIVE_WPM] ?: defaults.effectiveWpm,
            toneHz = this[TONE_HZ] ?: defaults.toneHz,
            volume = this[VOLUME] ?: defaults.volume,
            limitFlashes = this[LIMIT_FLASHES] ?: defaults.limitFlashes,
            flashWarningAccepted = this[FLASH_WARNING_ACCEPTED] ?: defaults.flashWarningAccepted,
            keyerMode = this[KEYER_MODE]?.let { name -> KeyerMode.entries.firstOrNull { it.name == name } } ?: defaults.keyerMode,
        ).clamped()
    }

    companion object {
        private val CHAR_WPM = intPreferencesKey("char_wpm")
        private val EFFECTIVE_WPM = intPreferencesKey("effective_wpm")
        private val TONE_HZ = intPreferencesKey("tone_hz")
        private val VOLUME = floatPreferencesKey("volume")
        private val LIMIT_FLASHES = booleanPreferencesKey("limit_flashes")
        private val FLASH_WARNING_ACCEPTED = booleanPreferencesKey("flash_warning_accepted")
        private val KEYER_MODE = stringPreferencesKey("keyer_mode")

        /** Repositorio sobre un fichero concreto (pruebas). */
        fun create(file: File, parentScope: CoroutineScope): DataStoreSettingsRepository {
            val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
            val store = PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
            return DataStoreSettingsRepository(store, scope)
        }
    }
}
