package com.quserh.eorzeaphone.craft.data

import com.quserh.eorzeaphone.data.GameCraftConsumable
import org.junit.Assert.*
import org.junit.Test

class CraftConsumablesTest {
    private fun bag(id: Long, count: Int, hq: Boolean = false, container: Long = 0) =
        InventoryItem(id, "食物", count, container, 0, hq, 0, 0)

    @Test fun keepsHqSeparateAndUsesBaseIdForInventory() {
        val candidates = listOf(GameCraftConsumable(100, "料理", 20), GameCraftConsumable(1_000_100, "料理", 20))
        val result = craftConsumablesInBag(candidates, listOf(bag(100, 3), bag(100, 2, true)))
        assertEquals(mapOf(100 to 3, 1_000_100 to 2), result.associate { it.id to it.quantity })
        assertEquals("料理 HQ", result.first { it.id == 1_000_100 }.displayName())
    }

    @Test fun acceptsAlreadyEncodedHqInventoryIdsWithoutDoubleOffset() {
        val result = craftConsumablesInBag(listOf(GameCraftConsumable(1_000_100, "料理", 9)), listOf(bag(1_000_100, 4, true)))
        assertEquals(4, result.single().quantity)
    }

    @Test fun excludesRetainersSaddlebagAndItemsNotApprovedByPlugin() {
        val candidates = listOf(GameCraftConsumable(100, "料理", 20))
        val inventory = listOf(bag(100, 8, container = 10000), bag(100, 4, container = 4000), bag(200, 9))
        assertTrue(craftConsumablesInBag(candidates, inventory).isEmpty())
    }

    @Test fun sumsBagSlotsButNeverInflatesNewerPluginQuantity() {
        val candidate = GameCraftConsumable(100, "料理", 4)
        val result = craftConsumablesInBag(listOf(candidate, candidate), listOf(bag(100, 3), bag(100, 3, container = 3)))
        assertEquals(listOf(candidate), result)
    }

    @Test fun emptyInventoryAndZeroQuantityRemoveStaleChoices() {
        assertTrue(craftConsumablesInBag(listOf(GameCraftConsumable(100, "料理", 8)), emptyList()).isEmpty())
        assertTrue(craftConsumablesInBag(listOf(GameCraftConsumable(100, "料理", 0)), listOf(bag(100, 2))).isEmpty())
    }
}
