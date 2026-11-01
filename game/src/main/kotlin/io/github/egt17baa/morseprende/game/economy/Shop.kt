package io.github.egt17baa.morseprende.game.economy

enum class ItemCategory { LAMP, TONE, KEYER, DECOR }

data class ShopItem(val id: String, val category: ItemCategory, val price: Int) {
    val isDefault: Boolean get() = price == 0
}

sealed interface BuyResult {
    data object Bought : BuyResult
    data object AlreadyOwned : BuyResult
    data class NotEnough(val missing: Int) : BuyResult
    data object Unknown : BuyResult
}

/** Tienda del faro: solo objetos estéticos. Los de precio 0 vienen de serie. */
object Shop {
    val items: List<ShopItem> = listOf(
        ShopItem("lamp_cream", ItemCategory.LAMP, 0),
        ShopItem("lamp_emerald", ItemCategory.LAMP, 40),
        ShopItem("lamp_harbor", ItemCategory.LAMP, 40),
        ShopItem("lamp_polar", ItemCategory.LAMP, 60),
        ShopItem("lamp_amber", ItemCategory.LAMP, 60),
        ShopItem("tone_sine", ItemCategory.TONE, 0),
        ShopItem("tone_soft", ItemCategory.TONE, 50),
        ShopItem("tone_classic", ItemCategory.TONE, 80),
        ShopItem("key_coral", ItemCategory.KEYER, 0),
        ShopItem("key_brass", ItemCategory.KEYER, 50),
        ShopItem("key_silver", ItemCategory.KEYER, 70),
        ShopItem("decor_gulls", ItemCategory.DECOR, 30),
        ShopItem("decor_boat", ItemCategory.DECOR, 60),
        ShopItem("decor_flags", ItemCategory.DECOR, 50),
        ShopItem("decor_lanterns", ItemCategory.DECOR, 80),
    )

    private val byId = items.associateBy { it.id }

    fun item(id: String): ShopItem? = byId[id]

    fun defaults(): Set<String> = items.filter { it.isDefault }.map { it.id }.toSet()

    fun check(id: String, balance: Int, owned: Set<String>): BuyResult {
        val item = byId[id] ?: return BuyResult.Unknown
        return when {
            id in owned || item.isDefault -> BuyResult.AlreadyOwned
            balance < item.price -> BuyResult.NotEnough(item.price - balance)
            else -> BuyResult.Bought
        }
    }
}
