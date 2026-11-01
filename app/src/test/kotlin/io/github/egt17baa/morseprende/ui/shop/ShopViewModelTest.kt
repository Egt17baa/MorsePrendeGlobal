package io.github.egt17baa.morseprende.ui.shop

import io.github.egt17baa.morseprende.data.FakeAchievementsRepository
import io.github.egt17baa.morseprende.data.FakeActivityRepository
import io.github.egt17baa.morseprende.data.FakeContentSource
import io.github.egt17baa.morseprende.data.FakeEconomyRepository
import io.github.egt17baa.morseprende.data.FakeGameRecordsRepository
import io.github.egt17baa.morseprende.data.FakeProgressRepository
import io.github.egt17baa.morseprende.data.FakeSeenCardsRepository
import io.github.egt17baa.morseprende.game.economy.BuyResult
import io.github.egt17baa.morseprende.rewards.RewardsService
import io.github.egt17baa.morseprende.settings.FakeSettingsRepository
import io.github.egt17baa.morseprende.translator.NoOpOutputFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShopViewModelTest {

    @After fun tearDown() = Dispatchers.resetMain()

    private class Env(val vm: ShopViewModel, val economy: FakeEconomyRepository, val settings: FakeSettingsRepository)

    private fun TestScope.env(balance: Int): Env {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val economy = FakeEconomyRepository(balance)
        val settings = FakeSettingsRepository()
        val rewards = RewardsService(
            FakeContentSource(listOf(FakeContentSource.realChapter1)), FakeProgressRepository(), FakeGameRecordsRepository(),
            FakeSeenCardsRepository(), economy, FakeActivityRepository(), FakeAchievementsRepository(),
            today = { java.time.LocalDate.of(2026, 11, 5) }, now = { 1L },
        )
        return Env(ShopViewModel(economy, settings, rewards, NoOpOutputFactory), economy, settings)
    }

    private fun ShopViewModel.item(id: String) = state.value.items.single { it.item.id == id }

    @Test fun lists_catalog_with_balance() = runTest {
        val e = env(70)
        assertEquals(70, e.vm.state.value.balance)
        assertEquals(io.github.egt17baa.morseprende.game.economy.Shop.items.size, e.vm.state.value.items.size)
        assertTrue(e.vm.item("lamp_cream").owned)
        assertTrue(e.vm.item("lamp_cream").equipped)
    }

    @Test fun buy_then_equip() = runTest {
        val e = env(70)
        e.vm.buy("lamp_polar")
        assertEquals(10, e.vm.state.value.balance)
        assertTrue(e.vm.item("lamp_polar").owned)
        e.vm.equip("lamp_polar")
        assertEquals("lamp_polar", e.settings.settings.value.lampColor)
        assertTrue(e.vm.item("lamp_polar").equipped)
    }

    @Test fun cannot_buy_without_balance_shows_missing() = runTest {
        val e = env(20)
        e.vm.buy("lamp_polar")
        assertEquals(BuyResult.NotEnough(40), e.vm.state.value.lastResult)
        assertEquals(20, e.vm.state.value.balance)
    }

    @Test fun double_tap_buy_charges_once() = runTest {
        val e = env(100)
        e.vm.buy("decor_gulls")
        e.vm.buy("decor_gulls")
        assertEquals(70, e.economy.balance.value)
    }

    @Test fun cannot_equip_unowned() = runTest {
        val e = env(0)
        e.vm.equip("lamp_polar")
        assertEquals("lamp_cream", e.settings.settings.value.lampColor)
    }

    @Test fun decor_toggle() = runTest {
        val e = env(100)
        e.vm.buy("decor_gulls")
        e.vm.toggleDecor("decor_gulls")
        assertEquals(setOf("decor_gulls"), e.settings.settings.value.decorations)
        e.vm.toggleDecor("decor_gulls")
        assertEquals(emptySet<String>(), e.settings.settings.value.decorations)
    }

    @Test fun buying_third_item_unlocks_collector() = runTest {
        val e = env(200)
        e.vm.buy("decor_gulls")
        e.vm.buy("decor_flags")
        e.vm.buy("lamp_emerald")
        assertEquals(listOf("collector"), e.vm.state.value.newAchievements)
    }
}
