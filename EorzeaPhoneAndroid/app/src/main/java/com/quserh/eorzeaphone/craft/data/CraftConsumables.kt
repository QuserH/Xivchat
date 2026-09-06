package com.quserh.eorzeaphone.craft.data

import com.quserh.eorzeaphone.data.GameCraftConsumable

/** Item actions encode HQ with +1,000,000; inventory snapshots also have an HQ flag. */
fun craftConsumablesInBag(
    candidates: List<GameCraftConsumable>,
    inventory: List<InventoryItem>,
): List<GameCraftConsumable> {
    val counts = inventory.asSequence()
        .filter { it.container in 0L..3L && it.quantity > 0 && it.itemId in 1L..1_999_999L }
        .groupBy { (it.itemId % 1_000_000).toInt() + if (it.hq || it.itemId >= 1_000_000) 1_000_000 else 0 }
        .mapValues { (_, rows) -> rows.sumOf { it.quantity } }
    return candidates.asSequence()
        .filter { it.id in 1..1_999_999 && it.name.isNotBlank() && it.quantity > 0 }
        .distinctBy { it.id }
        .mapNotNull { item -> counts[item.id]?.takeIf { it > 0 }?.let { item.copy(quantity = minOf(it, item.quantity)) } }
        .sortedWith(compareBy<GameCraftConsumable> { it.name }.thenByDescending { it.id })
        .toList()
}

fun GameCraftConsumable.displayName(): String = name + if (id >= 1_000_000) " HQ" else ""
