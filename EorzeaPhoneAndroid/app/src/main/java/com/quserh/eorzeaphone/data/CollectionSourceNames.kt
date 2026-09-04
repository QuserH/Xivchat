package com.quserh.eorzeaphone.data

/**
 * Chinese names for ffxivcollect's source categories.
 *
 * The collection cabinet reads ffxivcollect.com, which is English-only — that is why
 * 获取方式 stayed English while the rest of the app is Chinese. It is not the local
 * items.db `来源` column (that one was stripped at build time, a different gap).
 *
 * The `type` field is a closed vocabulary: 30 distinct values across all 7 categories
 * (3440 source rows sampled live — mounts/minions/emotes/orchestrions/hairstyles/
 * fashions/triad cards), so it maps completely. The accompanying `text` is free-form
 * (NPC names, place names, currency amounts) and is left as-is.
 */
object CollectionSourceNames {

    private val TYPES = mapOf(
        // Duty content
        "Dungeon" to "副本",
        "Raid" to "团队副本",
        "Chaotic Raid" to "混乱团本",
        "Trial" to "讨伐战",
        "Deep Dungeon" to "深层迷宫",
        "V&C Dungeon" to "变幻迷宫",
        "FATE" to "危命任务",
        "Hunts" to "狩猎笔记",
        // Exploratory zones
        "Eureka" to "尤雷卡",
        "Bozja" to "博兹雅",
        "Occult Crescent" to "神秘月镰",
        "Cosmic Exploration" to "宇宙探索",
        "Island Sanctuary" to "无人岛",
        "Skybuilders" to "云冠营造",
        // Progression / rewards
        "Quest" to "任务",
        "Achievement" to "成就",
        "Wondrous Tails" to "仙人微彩",
        "Gold Saucer" to "金碟游乐场",
        "PvP" to "PvP",
        "Tribal" to "友好部族",
        // Acquisition
        "NPC" to "NPC 兑换",
        "Purchase" to "商店购买",
        "Premium" to "付费/活动特典",
        "Crafting" to "制作",
        "Gathering" to "采集",
        "Treasure Hunt" to "寻宝",
        "Venture" to "雇员探险",
        "Voyages" to "潜水艇探索",
        "Event" to "限时活动",
        "Other" to "其他",
    )

    /** Chinese label for a source type; unknown values pass through unchanged. */
    fun type(raw: String): String {
        val key = raw.trim()
        if (key.isEmpty()) return "获取来源"
        return TYPES[key] ?: key
    }
}
