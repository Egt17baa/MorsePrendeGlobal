package io.github.egt17baa.morseprende.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.data.economy.EconomyRepository
import io.github.egt17baa.morseprende.game.economy.BuyResult
import io.github.egt17baa.morseprende.game.economy.ItemCategory
import io.github.egt17baa.morseprende.game.economy.Shop
import io.github.egt17baa.morseprende.game.economy.ShopItem
import io.github.egt17baa.morseprende.playback.OutputFactory
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.playback.SignalPlayer
import io.github.egt17baa.morseprende.rewards.RewardsService
import io.github.egt17baa.morseprende.settings.AppSettings
import io.github.egt17baa.morseprende.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ShopEntry(val item: ShopItem, val owned: Boolean, val equipped: Boolean, val busy: Boolean)

data class ShopUiState(
    val balance: Int = 0,
    val items: List<ShopEntry> = emptyList(),
    val lastResult: BuyResult? = null,
    val newAchievements: List<String> = emptyList(),
)

/** Tienda del faro: comprar con Destellos y equipar cosméticos. Sin compras reales. */
class ShopViewModel(
    private val economy: EconomyRepository,
    private val settingsRepo: SettingsRepository,
    private val rewards: RewardsService?,
    private val outputs: OutputFactory,
) : ViewModel() {

    private val extra = MutableStateFlow(ShopUiState())
    private val busy = MutableStateFlow<Set<String>>(emptySet())
    private val settings: StateFlow<AppSettings> = settingsRepo.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    private val player = SignalPlayer(viewModelScope)

    val state: StateFlow<ShopUiState> = combine(economy.balance, economy.owned, settings, busy, extra) { balance, owned, prefs, busyIds, ex ->
        ShopUiState(
            balance = balance,
            items = Shop.items.map { item ->
                ShopEntry(item, owned = item.id in owned, equipped = isEquipped(item, prefs), busy = item.id in busyIds)
            },
            lastResult = ex.lastResult,
            newAchievements = ex.newAchievements,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ShopUiState())

    fun buy(id: String) {
        if (id in busy.value) return
        busy.update { it + id }
        viewModelScope.launch {
            try {
                val result = economy.buy(id)
                val achievements = if (result == BuyResult.Bought) rewards?.refreshAchievements().orEmpty() else emptyList()
                extra.update { it.copy(lastResult = result, newAchievements = achievements.ifEmpty { it.newAchievements }) }
            } finally {
                busy.update { it - id }
            }
        }
    }

    fun equip(id: String) {
        val item = Shop.item(id) ?: return
        if (id !in ownedNow()) return
        viewModelScope.launch {
            settingsRepo.update {
                when (item.category) {
                    ItemCategory.LAMP -> it.copy(lampColor = id)
                    ItemCategory.TONE -> it.copy(toneTimbre = id)
                    ItemCategory.KEYER -> it.copy(keyerSkin = id)
                    ItemCategory.DECOR -> it.copy(decorations = it.decorations + id)
                }
            }
        }
    }

    fun toggleDecor(id: String) {
        val item = Shop.item(id) ?: return
        if (item.category != ItemCategory.DECOR || id !in ownedNow()) return
        viewModelScope.launch {
            settingsRepo.update { s -> s.copy(decorations = if (id in s.decorations) s.decorations - id else s.decorations + id) }
        }
    }

    /** Escuchar un timbre antes de comprarlo. */
    fun preview(id: String) {
        val item = Shop.item(id) ?: return
        if (item.category != ItemCategory.TONE) return
        val prefs = settings.value.copy(toneTimbre = id)
        val signals = MorseTiming(prefs.charWpm, prefs.effectiveWpm).signalsFor(MorseCodec.encode("SOS").morse)
        player.play(signals, listOf(outputs.create(OutputKind.SOUND, prefs)))
    }

    fun dismissMessage() = extra.update { it.copy(lastResult = null, newAchievements = emptyList()) }

    override fun onCleared() = player.stop()

    private fun ownedNow(): Set<String> = state.value.items.filter { it.owned }.map { it.item.id }.toSet()

    private fun isEquipped(item: ShopItem, prefs: AppSettings): Boolean = when (item.category) {
        ItemCategory.LAMP -> prefs.lampColor == item.id
        ItemCategory.TONE -> prefs.toneTimbre == item.id
        ItemCategory.KEYER -> prefs.keyerSkin == item.id
        ItemCategory.DECOR -> item.id in prefs.decorations
    }
}
