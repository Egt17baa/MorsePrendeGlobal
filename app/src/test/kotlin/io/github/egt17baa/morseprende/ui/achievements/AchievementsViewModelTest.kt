package io.github.egt17baa.morseprende.ui.achievements

import io.github.egt17baa.morseprende.data.FakeAchievementsRepository
import io.github.egt17baa.morseprende.game.achievements.Achievements
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AchievementsViewModelTest {
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun achievements_screen_lists_all() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val vm = AchievementsViewModel(FakeAchievementsRepository(mapOf("first_sos" to 1_700_000_000_000L)))
        val items = vm.state.value
        assertEquals(Achievements.ids, items.map { it.id })
        assertEquals(1_700_000_000_000L, items.first().unlockedAt)
        assertEquals(null, items[1].unlockedAt)
    }
}
