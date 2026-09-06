package com.quserh.eorzeaphone.craft.data

/** RecipeLevelTable contains base values; Recipe.sp1 holds percentage factors. */
internal fun recipeCap(base: Int, factor: Int): Int =
    (base.coerceAtLeast(0).toLong() * factor.coerceAtLeast(0) / 100).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

/** The game's non-linear quality-to-HQ table, indexed by whole quality percent. */
object CraftQuality {
    private val chances = intArrayOf(
        1, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5,
        5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 9, 9, 9, 10, 10, 10, 11, 11,
        11, 12, 12, 12, 13, 13, 13, 14, 14, 14, 15, 15, 15, 16, 16, 17, 17, 17, 18, 18,
        18, 19, 19, 20, 20, 21, 22, 23, 24, 26, 28, 31, 34, 38, 42, 47, 52, 58, 64, 68,
        71, 74, 76, 78, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 94, 96, 98, 100,
    )

    /** -1 means unknown. Non-HQ recipes (including collectables) have no HQ chance. */
    fun hqChance(quality: Int, maximum: Int, canHq: Boolean): Int {
        if (!canHq) return 0
        if (maximum <= 0) return -1
        val percent = (quality.coerceAtLeast(0).toLong() * 100 / maximum).coerceIn(0, 100).toInt()
        return chances[percent]
    }
}
