package io.github.egt17baa.morseprende.data

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
class SeenCardsRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun marks_and_persists() = runTest {
        val f = File(context.filesDir, "seen-test.preferences_pb")
        val first = DataStoreSeenCardsRepository.create(f, backgroundScope)
        assertEquals(emptySet<String>(), first.seen.first())
        first.markSeen("lib-sos")
        first.close()
        assertEquals(setOf("lib-sos"), DataStoreSeenCardsRepository.create(f, backgroundScope).seen.first())
    }
}
