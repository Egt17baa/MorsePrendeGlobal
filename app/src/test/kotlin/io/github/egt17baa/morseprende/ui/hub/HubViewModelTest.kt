package io.github.egt17baa.morseprende.ui.hub

import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.data.FakeSeenCardsRepository
import io.github.egt17baa.morseprende.game.content.LibraryCard
import io.github.egt17baa.morseprende.game.content.MissionType
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
class HubViewModelTest {

    @After fun tearDown() = Dispatchers.resetMain()

    private val cards = listOf(
        LibraryCard("a", "A", "x", "y", null, "book"),
        LibraryCard("b", "B", "x", "y", "c1m1", "ship"),
        LibraryCard("c", "C", "x", "y", "c1m5", "ship"),
    )

    private fun TestScope.vm(
        progress: FakeProgressRepository = FakeProgressRepository(),
        seen: Set<String> = emptySet(),
        fail: Boolean = false,
    ): HubViewModel {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        return HubViewModel(FakeContentSource(listOf(FakeContentSource.realChapter1), cards, fail), progress, FakeSeenCardsRepository(seen))
    }

    @Test fun nodes_reflect_progress() = runTest {
        val s = vm(FakeProgressRepository.completed("c1m1" to 3)).state.value
        assertEquals("El Faro", s.chapterTitle)
        assertEquals(8, s.nodes.size)
        assertEquals(MissionNode("c1m1", "La primera luz", MissionType.LESSON, 3, unlocked = true, isNext = false), s.nodes[0])
        assertTrue(s.nodes[1].unlocked)
        assertTrue(s.nodes[1].isNext)
        assertFalse(s.nodes[2].unlocked)
        assertEquals(MissionType.REVIEW, s.nodes[3].type)
        assertEquals(MissionType.FINALE, s.nodes[7].type)
    }

    @Test fun new_player_next_is_first_mission() = runTest {
        val s = vm().state.value
        assertTrue(s.nodes[0].isNext)
        assertTrue(s.nodes[0].unlocked)
        assertFalse(s.nodes[1].unlocked)
    }

    @Test fun glow_is_chapter_progress() = runTest {
        assertEquals(0f, vm().state.value.glow, 1e-6f)
        assertEquals(0.25f, vm(FakeProgressRepository.completed("c1m1" to 1, "c1m2" to 2)).state.value.glow, 1e-6f)
    }

    @Test fun new_cards_counts_unlocked_unseen() = runTest {
        assertEquals(1, vm().state.value.newCards)
        assertEquals(1, vm(FakeProgressRepository.completed("c1m1" to 1), seen = setOf("a")).state.value.newCards)
        assertEquals(0, vm(FakeProgressRepository.completed("c1m1" to 1), seen = setOf("a", "b")).state.value.newCards)
    }

    @Test fun content_error_sets_error() = runTest {
        assertTrue(vm(fail = true).state.value.error)
    }
}
