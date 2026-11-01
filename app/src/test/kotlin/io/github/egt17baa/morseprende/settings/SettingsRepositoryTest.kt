package io.github.egt17baa.morseprende.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private fun file() = File(context.filesDir, "settings-test.preferences_pb")

    @Test fun defaults() = runTest {
        val repo = DataStoreSettingsRepository.create(file(), backgroundScope)
        assertEquals(AppSettings(), repo.settings.first())
    }

    @Test fun persists() = runTest {
        val f = file()
        val first = DataStoreSettingsRepository.create(f, backgroundScope)
        first.update { it.copy(toneHz = 750, limitFlashes = false) }
        assertEquals(750, first.settings.first().toneHz)
        // Se cierra el primer DataStore antes de abrir otro sobre el mismo fichero.
        first.close()
        val second = DataStoreSettingsRepository.create(f, backgroundScope)
        val loaded = second.settings.first()
        assertEquals(750, loaded.toneHz)
        assertEquals(false, loaded.limitFlashes)
    }

    @Test fun clamps() = runTest {
        val repo = DataStoreSettingsRepository.create(file(), backgroundScope)
        repo.update { it.copy(charWpm = 99, effectiveWpm = 50, toneHz = 100) }
        val s = repo.settings.first()
        assertEquals(30, s.charWpm)
        assertEquals(30, s.effectiveWpm)
        assertEquals(400, s.toneHz)
    }

    @Test fun keyer_mode_persists() = runTest {
        val f = File(context.filesDir, "settings-keyer.preferences_pb")
        val first = DataStoreSettingsRepository.create(f, backgroundScope)
        first.update { it.copy(keyerMode = KeyerMode.TWO_BUTTONS) }
        first.close()
        val second = DataStoreSettingsRepository.create(f, backgroundScope)
        assertEquals(KeyerMode.TWO_BUTTONS, second.settings.first().keyerMode)
    }

    @Test fun settings_persist_cosmetics() = runTest {
        val f = File(context.filesDir, "settings-cosmetics.preferences_pb")
        val first = DataStoreSettingsRepository.create(f, backgroundScope)
        first.update { it.copy(lampColor = "lamp_polar", toneTimbre = "tone_soft", keyerSkin = "key_brass", decorations = setOf("decor_gulls", "decor_boat")) }
        first.close()
        val s = DataStoreSettingsRepository.create(f, backgroundScope).settings.first()
        assertEquals("lamp_polar", s.lampColor)
        assertEquals("tone_soft", s.toneTimbre)
        assertEquals("key_brass", s.keyerSkin)
        assertEquals(setOf("decor_gulls", "decor_boat"), s.decorations)
    }

    @Test fun defaults_are_house_items() {
        val s = AppSettings()
        assertEquals("lamp_cream", s.lampColor)
        assertEquals("tone_sine", s.toneTimbre)
        assertEquals("key_coral", s.keyerSkin)
        assertEquals(emptySet<String>(), s.decorations)
    }

    @Test fun new_settings_persist() = runTest {
        val f = File(context.filesDir, "settings-access.preferences_pb")
        val first = DataStoreSettingsRepository.create(f, backgroundScope)
        first.update { it.copy(textScale = 1.3f, highContrast = true, vibration = false) }
        first.close()
        val s = DataStoreSettingsRepository.create(f, backgroundScope).settings.first()
        assertEquals(1.3f, s.textScale, 1e-6f)
        assertEquals(true, s.highContrast)
        assertEquals(false, s.vibration)
    }

    @Test fun old_settings_load_with_new_defaults() = runTest {
        // Un fichero de la 0.6.x: no tiene las claves nuevas.
        val f = File(context.filesDir, "settings-old.preferences_pb")
        val store = androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { f })
        store.edit { it[androidx.datastore.preferences.core.intPreferencesKey("char_wpm")] = 20 }
        // Otro DataStore sobre el mismo fichero: se espera a que el primero termine de escribir.
        val bytes = f.readBytes()
        val copy = File(context.filesDir, "settings-old-copy.preferences_pb").apply { writeBytes(bytes) }
        val s = DataStoreSettingsRepository.create(copy, backgroundScope).settings.first()
        assertEquals(20, s.charWpm)
        assertEquals(1f, s.textScale, 1e-6f)
        assertEquals(false, s.highContrast)
        assertEquals(true, s.vibration)
    }

    @Test fun text_scale_clamped() {
        assertEquals(1.3f, AppSettings(textScale = 2f).clamped().textScale, 1e-6f)
        assertEquals(1.15f, AppSettings(textScale = 1.1f).clamped().textScale, 1e-6f)
        assertEquals(1f, AppSettings(textScale = 0.5f).clamped().textScale, 1e-6f)
    }
}
