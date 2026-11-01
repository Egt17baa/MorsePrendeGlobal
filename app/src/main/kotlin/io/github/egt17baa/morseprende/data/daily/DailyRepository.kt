package io.github.egt17baa.morseprende.data.daily

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Días en que se descifró el «Mensaje del día». */
interface DailyRepository {
    val completed: Flow<Set<LocalDate>>

    /** Marca el día como hecho. true si es la primera vez (solo entonces hay recompensa). */
    suspend fun markCompleted(day: LocalDate): Boolean

    /** Olvida todos los retos (al borrar el progreso). */
    suspend fun clear()
}

private val Context.dailyDataStore: DataStore<Preferences> by preferencesDataStore(name = "daily")

class DataStoreDailyRepository(context: Context) : DailyRepository {
    private val store = context.applicationContext.dailyDataStore

    override val completed: Flow<Set<LocalDate>> = store.data.map { prefs ->
        prefs[DONE].orEmpty().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
    }

    override suspend fun markCompleted(day: LocalDate): Boolean {
        var isNew = false
        // Dentro de la misma edición: dos toques rápidos nunca pagan dos veces.
        store.edit { prefs ->
            val done = prefs[DONE].orEmpty()
            isNew = day.toString() !in done
            if (isNew) prefs[DONE] = done + day.toString()
        }
        return isNew
    }

    override suspend fun clear() {
        store.edit { it.remove(DONE) }
    }

    private companion object {
        val DONE = stringSetPreferencesKey("done")
    }
}
