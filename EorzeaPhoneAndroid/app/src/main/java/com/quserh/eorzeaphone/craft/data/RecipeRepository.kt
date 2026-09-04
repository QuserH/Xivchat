package com.quserh.eorzeaphone.craft.data

/**
 * BOM tree expansion and cross-list aggregation, mirroring the 5p calculator's
 * findRC: quantities are tracked in item units; a craftable material spawns a
 * subcraft instead of a raw-material row (its own materials recurse).
 */
class RecipeRepository(private val db: RecipeDb) {

    /** Default recipe for an item: prefer the given job, else first by job id. */
    fun defaultRecipe(itemId: Int, preferJob: Int = -1): CraftRecipe? {
        val recipes = db.recipesFor(itemId)
        if (recipes.isEmpty()) return null
        return recipes.firstOrNull { it.job == preferJob } ?: recipes.first()
    }

    /**
     * Build the BOM tree for making [wantUnits] of [itemId] with [recipe].
     * [depth] caps pathological self-referencing recipes.
     */
    fun buildBom(itemId: Int, wantUnits: Int, recipe: CraftRecipe?, depth: Int = 0): List<BomNode> {
        if (recipe == null || depth > 8) return emptyList()
        val item = db.item(itemId) ?: return emptyList()
        val crafts = if (recipe.yield > 1) {
            (wantUnits + recipe.yield - 1) / recipe.yield
        } else wantUnits
        val children = db.materialsFor(recipe.id).mapNotNull { line ->
            val mat = db.item(line.itemId) ?: return@mapNotNull null
            val need = line.qty * crafts
            val matRecipe = defaultRecipe(line.itemId)
            BomNode(
                item = mat,
                qtyPerCraft = line.qty,
                totalNeed = need,
                craftCount = crafts,
                crystal = line.isCrystal,
                recipe = matRecipe,
                children = buildBom(line.itemId, need, matRecipe, depth + 1),
            )
        }
        return listOf(BomNode(item, 1, wantUnits, crafts, false, recipe, children))
    }

    /** Per-craft single-batch tree for the recipe detail page (no wanted count). */
    fun bomForRecipe(recipe: CraftRecipe, crafts: Int = 1): List<BomNode> =
        buildBom(recipe.itemId, recipe.yield * crafts, recipe)

    /**
     * Aggregate every list entry into base materials and intermediate crafts.
     * Intermediate rows are ALSO broken down, so 基础材料 is the full shopping
     * list while 中间制品 shows what to craft first.
     */
    fun aggregate(entries: List<ListEntry>): Aggregation {
        val basics = LinkedHashMap<Int, Int>()
        val inter = LinkedHashMap<Int, Int>()
        val paths = HashSet<Int>()

        fun walk(itemId: Int, wantUnits: Int, depth: Int) {
            if (depth > 8) return
            val recipe = defaultRecipe(itemId) ?: return
            val crafts = if (recipe.yield > 1) (wantUnits + recipe.yield - 1) / recipe.yield else wantUnits
            for (line in db.materialsFor(recipe.id)) {
                val need = line.qty * crafts
                if (line.isCrystal) {
                    basics[line.itemId] = (basics[line.itemId] ?: 0) + need
                    continue
                }
                if (db.canCraft(line.itemId)) {
                    inter[line.itemId] = (inter[line.itemId] ?: 0) + need
                    if (paths.add(line.itemId)) {
                        walk(line.itemId, need, depth + 1)
                        paths.remove(line.itemId)
                    }
                } else {
                    basics[line.itemId] = (basics[line.itemId] ?: 0) + need
                }
            }
        }

        val itemIds = LinkedHashSet<Int>()
        for (entry in entries) {
            itemIds.add(entry.itemId)
            walk(entry.itemId, entry.qty, 0)
        }

        fun rows(map: Map<Int, Int>, intermediate: Boolean): List<AggregateRow> =
            map.mapNotNull { (id, need) ->
                val item = db.item(id) ?: return@mapNotNull null
                // Crystal/shard item ids are the fixed 2..13 block (1 is Gil).
                AggregateRow(item, need, 0, item.id in 2..13, intermediate)
            }.sortedWith(compareBy<AggregateRow> { it.crystal }.thenBy { it.item.id })

        return Aggregation(itemIds.toList(), rows(basics, false), rows(inter, true))
    }

    /** Attach live inventory totals to aggregation rows. */
    fun withHeld(aggregation: Aggregation, snapshot: InventorySnapshot): Aggregation {
        fun fill(rows: List<AggregateRow>) = rows.map {
            it.copy(held = snapshot.totalOf(it.item.id))
        }
        return aggregation.copy(basics = fill(aggregation.basics), intermediates = fill(aggregation.intermediates))
    }

    data class Aggregation(
        val targets: List<Int>,
        val basics: List<AggregateRow>,
        val intermediates: List<AggregateRow>,
    )
}
