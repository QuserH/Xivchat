package com.quserh.eorzeaphone.craft.data

/** One row of the bundled craft.db items table. */
data class CraftItem(
    val id: Int,
    val nameCn: String,
    val nameJp: String,
    val nameEn: String,
    val icon: Int,
    val ilv: Int,
    val hq: Boolean,
    val uicat: Int,
    /** ClassJobCategory id of the equippable jobs (0 = 无职业限制/非装备). */
    val jobs: Int = 0,
) {
    fun name(preferCn: Boolean = true): String =
        if (preferCn && nameCn.isNotBlank()) nameCn else nameJp.ifBlank { nameEn }
}

/** jobcat 表一行：可穿职业类别的中文标签与角色定位（决定标签底色）。 */
data class JobCat(val label: String, val role: String)

/** One row of recipes: how a craft job turns materials into [itemId]. */
data class CraftRecipe(
    val id: Int,
    val job: Int,
    val itemId: Int,
    val yield: Int,
    val craftLv: Int,
    val stars: Int,
    val rlv: Int,
    val hq: Boolean,
    val qs: Boolean,
)

/** One material line of a recipe, per single craft. */
data class MaterialLine(val itemId: Int, val qty: Int, val isCrystal: Boolean)

/** Recipe ids are 0..7; the craft job tables in game start at 8. */
object CraftJobs {
    val NAMES = listOf("刻木匠", "锻铁匠", "铸甲匠", "雕金匠", "制革匠", "裁衣匠", "炼金术士", "烹调师")
    val ABBR = listOf("刻木", "锻铁", "铸甲", "雕金", "制革", "裁衣", "炼金", "烹调")

    fun name(job: Int): String = if (job in NAMES.indices) NAMES[job] else "未知职业"
    fun abbr(job: Int): String = if (job in ABBR.indices) ABBR[job] else "??"
}

/** A node of the recipe BOM tree (what materials make this item, recursively). */
data class BomNode(
    val item: CraftItem,
    /** Materials consumed per single craft of the parent. */
    val qtyPerCraft: Int,
    /** Total item units needed to satisfy the requested output count. */
    val totalNeed: Int,
    val craftCount: Int,
    val crystal: Boolean,
    val recipe: CraftRecipe?,
    val children: List<BomNode>,
) {
    val craftable: Boolean get() = recipe != null
}

/** Snapshot of one inventory scan, straight from the plugin (op 11). */
data class InventoryItem(
    val itemId: Long,
    val name: String,
    val quantity: Int,
    val container: Long,
    val slot: Long,
    val hq: Boolean,
    val iconId: Int,
    val retainerId: Long,
)

data class InventoryContainer(val id: Long, val size: Int)

data class RetainerEntry(
    val id: Long,
    val name: String,
    val active: Boolean,
    val itemCount: Int,
    val quantity: Int,
    val gil: Long,
    val ventureId: Long,
    val ventureCompleteUnix: Long,
)

data class InventorySnapshot(
    val updatedMs: Long = 0L,
    val items: List<InventoryItem> = emptyList(),
    val containers: List<InventoryContainer> = emptyList(),
    val retainers: List<RetainerEntry> = emptyList(),
) {
    fun totalOf(itemId: Int): Int =
        items.filter { it.itemId == itemId.toLong() }.sumOf { it.quantity }

    /** 在背包（容器 0-3）里的数量；其余容器都算"待取回"的来源。 */
    fun bagOf(itemId: Int): Int =
        items.filter { it.itemId == itemId.toLong() && it.container in 0L..3L }.sumOf { it.quantity }

    /**
     * Where a given item lives, grouped per container, only groups that actually
     * hold it. Retainer rows use the retainer's own name when known.
     */
    fun locationsOf(itemId: Int): List<Location> {
        val retainerNames = retainers.associate { it.id to it.name }
        return items
            .filter { it.itemId == itemId.toLong() && it.quantity > 0 }
            .groupBy { keyFor(it, retainerNames) }
            .map { (key, rows) ->
                Location(key.group, key.label, rows.sumOf { it.quantity }, rows)
            }
            .sortedByDescending { it.quantity }
    }

    private fun keyFor(item: InventoryItem, retainerNames: Map<Long, String>): LocationKey {
        val (group, label) = InventoryGroups.labelOf(item, retainerNames)
        return LocationKey(group, label)
    }

    data class LocationKey(val group: String, val label: String)
    data class Location(val group: String, val label: String, val quantity: Int, val rows: List<InventoryItem>)
}

/** Container id ranges match the plugin's GameInventoryType snapshot (Server.cs). */
object InventoryGroups {
    const val BAGS = "背包"
    const val EQUIPPED = "装备"
    const val ARMOURY = "军械库"
    const val SADDLE = "鞍袋"
    const val RETAINER = "雇员"
    const val COMPANY = "部队仓库"
    const val HOUSING = "房屋仓库"
    const val OTHER = "其他"

    fun labelOf(item: InventoryItem, retainerNames: Map<Long, String>): Pair<String, String> {
        val c = item.container
        return when {
            c in 0L..3L -> BAGS to "背包·格${c + 1}"
            c == 1000L -> EQUIPPED to "身上装备"
            c in 3200L..3400L || c == 3500L -> ARMOURY to "军械库"
            c in 4000L..4101L -> SADDLE to "陆行鸟鞍袋"
            c in 10000L..10006L -> {
                val name = retainerNames[item.retainerId]
                RETAINER to (if (name.isNullOrBlank()) "雇员(${item.retainerId})" else name)
            }
            c in 20000L..20004L -> COMPANY to "部队仓库·页${c - 19999}"
            c in 27000L..27011L -> HOUSING to "房屋仓库·页${c - 26999}"
            c == 27200L -> HOUSING to "房屋仓库·室外"
            else -> OTHER to "容器 $c"
        }
    }
}

/** One entry of a crafting list: "make N of item X". */
data class ListEntry(val itemId: Int, val qty: Int)

data class CraftList(
    val id: String,
    var name: String,
    val entries: MutableList<ListEntry> = mutableListOf(),
    var updatedMs: Long = System.currentTimeMillis(),
)

/** Aggregated material row shown in the list summary. */
data class AggregateRow(
    val item: CraftItem,
    /** Total units required. */
    val need: Int,
    /** Units currently held across synced containers. */
    val held: Int,
    val crystal: Boolean,
    /** True when this row is an intermediate the player crafts themselves. */
    val intermediate: Boolean,
)
