package io.github.egt17baa.morseprende.game.economy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShopTest {
    @Test fun catalog_exact() = assertEquals(
        listOf(
            ShopItem("lamp_cream", ItemCategory.LAMP, 0), ShopItem("lamp_emerald", ItemCategory.LAMP, 40),
            ShopItem("lamp_harbor", ItemCategory.LAMP, 40), ShopItem("lamp_polar", ItemCategory.LAMP, 60),
            ShopItem("lamp_amber", ItemCategory.LAMP, 60), ShopItem("lamp_violet", ItemCategory.LAMP, 70),
            ShopItem("tone_sine", ItemCategory.TONE, 0), ShopItem("tone_soft", ItemCategory.TONE, 50),
            ShopItem("tone_classic", ItemCategory.TONE, 80),
            ShopItem("key_coral", ItemCategory.KEYER, 0), ShopItem("key_brass", ItemCategory.KEYER, 50),
            ShopItem("key_silver", ItemCategory.KEYER, 70), ShopItem("key_wood", ItemCategory.KEYER, 60),
            ShopItem("decor_gulls", ItemCategory.DECOR, 30), ShopItem("decor_boat", ItemCategory.DECOR, 60),
            ShopItem("decor_flags", ItemCategory.DECOR, 50), ShopItem("decor_lanterns", ItemCategory.DECOR, 80),
            ShopItem("decor_stars", ItemCategory.DECOR, 70), ShopItem("decor_house", ItemCategory.DECOR, 90),
            ShopItem("decor_whale", ItemCategory.DECOR, 120), ShopItem("decor_aurora", ItemCategory.DECOR, 150),
        ),
        Shop.items,
    )
    @Test fun defaults_free_and_owned() = assertEquals(setOf("lamp_cream", "tone_sine", "key_coral"), Shop.defaults())
    @Test fun not_enough() = assertEquals(BuyResult.NotEnough(1), Shop.check("lamp_polar", 59, Shop.defaults()))
    @Test fun can_buy() = assertEquals(BuyResult.Bought, Shop.check("lamp_polar", 60, Shop.defaults()))
    @Test fun already_owned() = assertEquals(BuyResult.AlreadyOwned, Shop.check("lamp_cream", 0, Shop.defaults()))
    @Test fun unknown() {
        assertEquals(BuyResult.Unknown, Shop.check("nope", 1000, emptySet()))
        assertNull(Shop.item("nope"))
    }

    @Test fun shop_new_items_priced() {
        mapOf("decor_house" to 90, "decor_whale" to 120, "decor_stars" to 70, "decor_aurora" to 150, "lamp_violet" to 70, "key_wood" to 60)
            .forEach { (id, price) -> org.junit.Assert.assertEquals(id, price, Shop.item(id)?.price) }
        org.junit.Assert.assertEquals(Shop.items.size, Shop.items.map { it.id }.toSet().size)
    }
}
