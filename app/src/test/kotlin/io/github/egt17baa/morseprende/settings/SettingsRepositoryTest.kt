package io.github.egt17baa.morseprende.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
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
}
