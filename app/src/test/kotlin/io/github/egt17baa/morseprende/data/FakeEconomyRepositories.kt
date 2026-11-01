package io.github.egt17baa.morseprende.data

import io.github.egt17baa.morseprende.data.achievements.AchievementsRepository
import io.github.egt17baa.morseprende.data.activity.ActivityRepository
import io.github.egt17baa.morseprende.data.economy.EconomyRepository
import io.github.egt17baa.morseprende.game.economy.BuyResult
import io.github.egt17baa.morseprende.game.economy.Shop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

class FakeEconomyRepository(balance: Int = 0, bought: Set<String> = emptySet()) : EconomyRepository {
    private val balanceState = MutableStateFlow(balance)
    private val ownedState = MutableStateFlow(bought + Shop.defaults())
    var buyCalls = 0
    private var walletExists = balance > 0 || bought.isNotEmpty()
    override suspend fun hasWallet(): Boolean = walletExists
    override val balance: StateFlow<Int> = balanceState
    override val owned: StateFlow<Set<String>> = ownedState
    override suspend fun earn(amount: Int) {
        if (amount > 0) {
            balanceState.value += amount
            walletExists = true
        }
    }
    override suspend fun buy(itemId: String): BuyResult {
        buyCalls++
        val r = Shop.check(itemId, balanceState.value, ownedState.value)
        if (r == BuyResult.Bought) {
            balanceState.value -= Shop.item(itemId)!!.price
            ownedState.value = ownedState.value + itemId
        }
        return r
    }
}

class FakeActivityRepository(days: Set<LocalDate> = emptySet()) : ActivityRepository {
    private val state = MutableStateFlow(days)
    override val playedDays: StateFlow<Set<LocalDate>> = state
    override suspend fun markPlayed(day: LocalDate): Boolean {
        if (day in state.value) return false
        state.value = state.value + day
        return true
    }
}

class FakeAchievementsRepository(initial: Map<String, Long> = emptyMap()) : AchievementsRepository {
    private val state = MutableStateFlow(initial)
    override val unlocked: StateFlow<Map<String, Long>> = state
    override suspend fun unlock(ids: Set<String>, now: Long): Set<String> {
        val new = ids - state.value.keys
        state.value = state.value + new.associateWith { now }
        return new
    }
}
