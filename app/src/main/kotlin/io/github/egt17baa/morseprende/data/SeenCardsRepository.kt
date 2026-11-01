package io.github.egt17baa.morseprende.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

/** Fichas de la biblioteca que el jugador ya ha abierto (para la insignia de "nuevas"). */
interface SeenCardsRepository {
    val seen: Flow<Set<String>>
    suspend fun markSeen(cardId: String)

    /** Olvida todas las fichas vistas (al borrar el progreso). */
    suspend fun clear()
}

private val Context.libraryDataStore: DataStore<Preferences> by preferencesDataStore(name = "library")

class DataStoreSeenCardsRepository private constructor(
    private val dataStore: DataStore<Preferences>,
    private val ownedScope: CoroutineScope?,
) : SeenCardsRepository {

    constructor(context: Context) : this(context.applicationContext.libraryDataStore, null)

    override val seen: Flow<Set<String>> = dataStore.data.map { it[SEEN_CARDS] ?: emptySet() }

    override suspend fun markSeen(cardId: String) {
        dataStore.edit { it[SEEN_CARDS] = (it[SEEN_CARDS] ?: emptySet()) + cardId }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(SEEN_CARDS) }
    }

    fun close() {
        ownedScope?.cancel()
    }

    companion object {
        private val SEEN_CARDS = stringSetPreferencesKey("seen_cards")

        fun create(file: File, parentScope: CoroutineScope): DataStoreSeenCardsRepository {
            val scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]))
            return DataStoreSeenCardsRepository(PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }), scope)
        }
    }
}
