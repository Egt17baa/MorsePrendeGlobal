package io.github.egt17baa.morseprende.ui.library

import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.data.FakeSeenCardsRepository
import io.github.egt17baa.morseprende.game.content.LibraryCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @After fun tearDown() = Dispatchers.resetMain()

    private val cards = listOf(
        LibraryCard("lib-a", "Qué es", "cuerpo", "dato", null, "book"),
        LibraryCard("lib-b", "SOS", "cuerpo", "dato", "c1m5", "ship"),
    )

    private fun TestScope.vm(progress: FakeProgressRepository = FakeProgressRepository(), seen: FakeSeenCardsRepository = FakeSeenCardsRepository()): LibraryViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return LibraryViewModel(FakeContentSource(listOf(FakeContentSource.realChapter1), cards), progress, seen)
    }

    @Test fun locked_cards_show_unlock_mission_title() = runTest {
        val items = vm().state.value.items
        assertTrue(items[0].unlocked)
        assertFalse(items[1].unlocked)
        assertEquals("Rescate en el arrecife", items[1].unlockMissionTitle)
        assertTrue(items[0].isNew)
    }

    @Test fun opening_card_marks_seen() = runTest {
        val seen = FakeSeenCardsRepository()
        val v = vm(seen = seen)
        v.open("lib-a")
        assertEquals("lib-a", v.state.value.opened?.id)
        assertEquals(setOf("lib-a"), seen.seen.value)
        assertFalse(v.state.value.items[0].isNew)
        v.close()
        assertEquals(null, v.state.value.opened)
    }

    @Test fun locked_card_cannot_be_opened() = runTest {
        val v = vm()
        v.open("lib-b")
        assertEquals(null, v.state.value.opened)
    }

    @Test fun unlocked_after_mission() = runTest {
        assertTrue(vm(FakeProgressRepository.completed("c1m5" to 2)).state.value.items[1].unlocked)
    }
}
