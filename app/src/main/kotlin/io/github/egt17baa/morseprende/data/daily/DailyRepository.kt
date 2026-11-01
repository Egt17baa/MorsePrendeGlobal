package io.github.egt17baa.morseprende.data.daily

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
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

    /**
     * Frase de [day]: la guardada si ya se eligió hoy; si no, la que da [choose] (y se guarda). Así,
     * aprender letras a mitad del día no cambia el mensaje ni borra las pistas.
     */
    suspend fun phraseFor(day: LocalDate, choose: () -> String?): String?

    /** Olvida todos los retos (al borrar el progreso). */
    suspend fun clear()
}

private val Context.dailyDataStore: DataStore<Preferences> by preferencesDataStore(name = "daily")

class DataStoreDailyRepository(private val store: DataStore<Preferences>) : DailyRepository {
    constructor(context: Context) : this(context.applicationContext.dailyDataStore)

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

    override suspend fun phraseFor(day: LocalDate, choose: () -> String?): String? {
        var phrase: String? = null
        store.edit { prefs ->
            val saved = prefs[PHRASE]?.split('|', limit = 2)
            phrase = if (saved?.size == 2 && saved[0] == day.toString()) {
                saved[1]
            } else {
                choose()?.also { prefs[PHRASE] = "$day|$it" }
            }
        }
        return phrase
    }

    override suspend fun clear() {
        store.edit {
            it.remove(DONE)
            it.remove(PHRASE)
        }
    }

    private companion object {
        val DONE = stringSetPreferencesKey("done")
        /** «2026-11-01|HOLA FARO»: la frase elegida para ese día. */
        val PHRASE = stringPreferencesKey("phrase")
    }
}
