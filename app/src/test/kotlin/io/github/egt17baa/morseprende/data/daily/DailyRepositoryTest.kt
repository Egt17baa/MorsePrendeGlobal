package io.github.egt17baa.morseprende.data.daily

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DailyRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val day = LocalDate.of(2026, 11, 1)

    private fun kotlinx.coroutines.test.TestScope.repo() = DataStoreDailyRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope) { File(context.filesDir, "daily-test.preferences_pb") },
    )

    @Test fun mark_completed_only_once() = runTest {
        val r = repo()
        assertTrue(r.markCompleted(day))
        assertFalse(r.markCompleted(day))
        assertTrue(r.markCompleted(day.plusDays(1)))
        assertEquals(setOf(day, day.plusDays(1)), r.completed.first())
    }

    @Test fun phrase_kept_for_the_day() = runTest {
        val r = repo()
        assertEquals("SOS", r.phraseFor(day) { "SOS" })
        // Más letras aprendidas a mitad del día: sigue la misma frase.
        assertEquals("SOS", r.phraseFor(day) { "HOLA FARO" })
        // Al día siguiente, frase nueva.
        assertEquals("HOLA FARO", r.phraseFor(day.plusDays(1)) { "HOLA FARO" })
        // Sin frase posible (sin letras) no se guarda nada.
        assertNull(r.phraseFor(day.plusDays(2)) { null })
        assertEquals("ECO", r.phraseFor(day.plusDays(2)) { "ECO" })
    }

    @Test fun clear_forgets_days_and_phrase() = runTest {
        val r = repo()
        r.markCompleted(day)
        r.phraseFor(day) { "SOS" }
        r.clear()
        assertEquals(emptySet<LocalDate>(), r.completed.first())
        assertEquals("OSO", r.phraseFor(day) { "OSO" })
    }
}
