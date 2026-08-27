package com.quserh.eorzeaphone.data.shizhijia

import org.json.JSONArray
import org.json.JSONObject

/**
 * 专项数据（官网叫「数据中心」/statistics）的模型。
 *
 * 接口一共 43 个，都在 `api/home/dataCenter/` 下面，全部要登录 + 绑角色。
 * 后端把所有数值都返回成**字符串**（"1520"、"0.35"、"942994.0"），所以这里
 * 统一用 [num] / [int] 解析，不要直接 optInt——那样全是 0。
 *
 * 字段名不在官网前端代码里（压缩后只剩访问路径），是用 [ShizhijiaProbe] 打一次
 * 真实响应抓出来的。
 */

/**
 * 道具图标地址。
 *
 * 抄的是官网 Glamour 里的那个函数：按千位分桶，文件名补零到 6 位再加 `_hr1`。
 * 39905 -> `.../039000/039905_hr1.png`。少了 `_hr1` 会 404。
 */
object ShizhijiaIcons {
    private const val BASE = "https://ff14-eo.web.sdo.com/ffstones/item/icon/dcsvv4fowz2m"

    fun item(icon: Int): String {
        if (icon <= 0) return ""
        val bucket = ((icon / 1000) * 1000).toString().padStart(6, '0')
        val name = icon.toString().padStart(6, '0')
        return "$BASE/$bucket/${name}_hr1.png"
    }
}

/** 把后端的字符串数字解析成 Double；空串/null 给 0。 */
private fun JSONObject.num(key: String): Double =
    optString(key).takeIf { it.isNotBlank() }?.toDoubleOrNull() ?: 0.0

private fun JSONObject.int(key: String): Int = num(key).toInt()

private fun JSONObject.str(key: String): String = optString(key).takeIf { it != "null" }.orEmpty()

private fun <T> JSONArray?.map(f: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optJSONObject(it) }.map(f)
}

// ---------------------------------------------------------------------------
// 开放状态：哪几屏有数据。jue1..jue7 对应 7 个绝，"0" 表示后端还没开。
// ---------------------------------------------------------------------------

data class ShizhijiaDataOpen(
    val pvp: Boolean,
    val fishing: Boolean,
    val vanity: Boolean,
    val mkd: Boolean,
    /** 零式：官网这里放的是一个数字（854），非 "0" 就算开。 */
    val lingshi: Boolean,
    /** 7 个绝各自是否开放，index 0 = jue1。 */
    val ultimates: List<Boolean>,
) {
    companion object {
        fun fromJson(d: JSONObject?): ShizhijiaDataOpen {
            fun on(k: String) = d?.optString(k).orEmpty().let { it.isNotBlank() && it != "0" }
            return ShizhijiaDataOpen(
                pvp = on("pvp"), fishing = on("fishing"), vanity = on("vanity"),
                mkd = on("mkd"), lingshi = on("lingshi"),
                ultimates = (1..7).map { on("jue$it") },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 纷争前线
// ---------------------------------------------------------------------------

/**
 * frontline1TotalNew 的一行。`data_time` 区分统计窗口：
 * "30days" / "total" / 还有一个赛季值。rank 字段是百分位（0-100）。
 */
data class ShizhijiaPvpTotal(
    val dataTime: String,
    val fightTimes: Int,
    val winTimes: Int,
    val winRate: Double,
    val killTimes: Int,
    val deadTimes: Int,
    val assistTimes: Int,
    val kda: Double,
    val avgKill: Double,
    val avgDead: Double,
    val avgAssist: Double,
    val avgDamage: Double,
    val avgDamaged: Double,
    val avgHeal: Double,
    val killRank: Double,
    val deadRank: Double,
    val assistRank: Double,
    val damageRank: Double,
    val damagedRank: Double,
    val healRank: Double,
    val seriesLevel: Int,
    val pvpRank: Int,
    val gcId: String,
    val occupyCount: Int,
    val clearTime: Int,
) {
    /** 30 天窗口。 */
    val is30Days: Boolean get() = dataTime == "30days"
    val isTotal: Boolean get() = dataTime == "total"

    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaPvpTotal> = a.map {
            ShizhijiaPvpTotal(
                dataTime = it.str("data_time"),
                fightTimes = it.int("fight_times"), winTimes = it.int("win_times"),
                winRate = it.num("win_rate"), killTimes = it.int("kill_times"),
                deadTimes = it.int("dead_times"), assistTimes = it.int("assist_times"),
                kda = it.num("kda"),
                avgKill = it.num("avg_kill"), avgDead = it.num("avg_dead"),
                avgAssist = it.num("avg_assist"), avgDamage = it.num("avg_damage"),
                avgDamaged = it.num("avg_damaged"), avgHeal = it.num("avg_heal"),
                killRank = it.num("kill_rank"), deadRank = it.num("dead_rank"),
                assistRank = it.num("assist_rank"), damageRank = it.num("damage_rank"),
                damagedRank = it.num("damaged_rank"), healRank = it.num("heal_rank"),
                seriesLevel = it.int("series_level"), pvpRank = it.int("pvp_rank"),
                gcId = it.str("gc_id"), occupyCount = it.int("occupy_count"),
                clearTime = it.int("clear_time"),
            )
        }
    }
}

/** frontline2WeekNew：本周每场记录。 */
data class ShizhijiaPvpWeek(
    val partDate: String,
    val territory: String,
    val fightTimes: Int,
    val winTimes: Int,
    val killTimes: Int,
    val deadTimes: Int,
    val assistTimes: Int,
    val kda: Double,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaPvpWeek> = a.map {
            ShizhijiaPvpWeek(
                partDate = it.str("part_date"), territory = it.str("territory_type"),
                fightTimes = it.int("fight_times"), winTimes = it.int("win_times"),
                killTimes = it.int("kill_times"), deadTimes = it.int("dead_times"),
                assistTimes = it.int("assist_times"), kda = it.num("kda"),
            )
        }
    }
}

/** frontline3JobNew：按职业。`use_rate` 是出场占比。 */
data class ShizhijiaPvpJob(
    val dataTime: String,
    val jobName: String,
    val career: String,
    val times: Int,
    val winTimes: Int,
    val winRate: Double,
    val useRate: Double,
    val kda: Double,
    val kdaRate: Double,
    val killTimes: Int,
    val deadTimes: Int,
    val lbTimes: Int,
    val avgKill: Double,
    val avgDead: Double,
    val avgAssist: Double,
    val avgDamage: Double,
    val avgDamaged: Double,
    val avgHeal: Double,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaPvpJob> = a.map {
            ShizhijiaPvpJob(
                dataTime = it.str("data_time"), jobName = it.str("job_name"),
                career = it.str("career"), times = it.int("times"),
                winTimes = it.int("win_times"), winRate = it.num("win_rate"),
                useRate = it.num("use_rate"), kda = it.num("kda"),
                kdaRate = it.num("kda_rate"), killTimes = it.int("kill_times"),
                deadTimes = it.int("dead_times"), lbTimes = it.int("lb_times"),
                avgKill = it.num("avg_kill"), avgDead = it.num("avg_dead"),
                avgAssist = it.num("avg_assist"), avgDamage = it.num("avg_damage"),
                avgDamaged = it.num("avg_damaged"), avgHeal = it.num("avg_heal"),
            )
        }
    }
}

/**
 * frontline4Best：各项最佳的那一场。`best_type` 说明这行是哪项的最佳
 * （kill/dead/assist/damage/damaged/heal），所以同一个数组里会有 6 行。
 */
data class ShizhijiaPvpBest(
    val bestType: String,
    val career: String,
    val territory: String,
    val logTime: String,
    val resultRank: Int,
    val killTimes: Int,
    val deadTimes: Int,
    val assist: Int,
    val totalDamage: Double,
    val totalDamaged: Double,
    val totalHeal: Double,
) {
    /** 这行最佳项的中文名。 */
    val label: String
        get() = when (bestType) {
            "kill" -> "击杀"
            "dead" -> "阵亡"
            "assist" -> "助攻"
            "damage" -> "总伤害"
            "damaged" -> "承受伤害"
            "heal" -> "总治疗"
            else -> bestType
        }

    /** 这行最佳项对应的数值，用来在卡片上当主数字。 */
    val value: Double
        get() = when (bestType) {
            "kill" -> killTimes.toDouble()
            "dead" -> deadTimes.toDouble()
            "assist" -> assist.toDouble()
            "damage" -> totalDamage
            "damaged" -> totalDamaged
            "heal" -> totalHeal
            else -> 0.0
        }

    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaPvpBest> = a.map {
            ShizhijiaPvpBest(
                bestType = it.str("best_type"), career = it.str("career"),
                territory = it.str("territory_type"), logTime = it.str("log_time"),
                resultRank = it.int("result_rank"), killTimes = it.int("kill_times"),
                deadTimes = it.int("dead_times"), assist = it.int("assist"),
                totalDamage = it.num("total_damage"), totalDamaged = it.num("total_damaged"),
                totalHeal = it.num("total_heal"),
            )
        }
    }
}

/** frontline5Map：按地图。 */
data class ShizhijiaPvpMap(
    val territory: String,
    val fightTimes: Int,
    val winTimes: Int,
    val winRate: Double,
    val killTimes: Double,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaPvpMap> = a.map {
            ShizhijiaPvpMap(
                territory = it.str("territory_type"), fightTimes = it.int("fight_times"),
                winTimes = it.int("win_times"), winRate = it.num("win_rate"),
                killTimes = it.num("kill_times"),
            )
        }
    }
}

/** frontline6MapJob：地图 × 职业。 */
data class ShizhijiaPvpMapJob(
    val territory: String,
    val jobName: String,
    val classJob: String,
    val jobNum: Int,
    val jobWinTimes: Int,
    val jobWinRate: Double,
    val jobKillTimes: Double,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaPvpMapJob> = a.map {
            ShizhijiaPvpMapJob(
                territory = it.str("territory_type"), jobName = it.str("job_name"),
                classJob = it.str("class_job"), jobNum = it.int("job_num"),
                jobWinTimes = it.int("job_win_times"), jobWinRate = it.num("job_win_rate"),
                jobKillTimes = it.num("job_kill_times"),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 捕鱼人
// ---------------------------------------------------------------------------

/** fishTotal1：总览。 */
data class ShizhijiaFishTotal(
    val totalTimes: Int,
    val succRate: Double,
    val seaTimes: Int,
    val maxSeaScore: Int,
) {
    companion object {
        fun fromArray(a: JSONArray?): ShizhijiaFishTotal? = a.map {
            ShizhijiaFishTotal(
                totalTimes = it.int("total_times"), succRate = it.num("succ_rate"),
                seaTimes = it.int("sea_times"), maxSeaScore = it.int("max_sea_score"),
            )
        }.firstOrNull()
    }
}

/** fishNum2 / fishBait3 共用形状：名字 + 数量 + 分类（`rn` 是排名）。 */
data class ShizhijiaFishCount(
    val name: String,
    val num: Int,
    val type: String,
    val rank: Int,
) {
    companion object {
        /** fishNum2 用 fish_num，fishBait3 用 bait_num。 */
        fun fromArray(a: JSONArray?, numKey: String): List<ShizhijiaFishCount> = a.map {
            ShizhijiaFishCount(
                name = it.str("catalog_name"), num = it.int(numKey),
                type = it.str("fish_type"), rank = it.int("rn"),
            )
        }
    }
}

/** fishBig4：大鱼首次钓获。 */
data class ShizhijiaFishBig(
    val name: String,
    val logTime: String,
    val version: String,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaFishBig> = a.map {
            ShizhijiaFishBig(
                name = it.str("catalog_name"), logTime = it.str("log_time"),
                version = it.str("version"),
            )
        }
    }
}

/** fishAchieve5：钓鱼成就。 */
data class ShizhijiaFishAchieve(
    val id: String,
    val name: String,
    val detail: String,
    val logTime: String,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaFishAchieve> = a.map {
            ShizhijiaFishAchieve(
                id = it.str("achieve_id"), name = it.str("achieve_name"),
                detail = it.str("achieve_detail"), logTime = it.str("log_time"),
            )
        }
    }
}

/**
 * fishSea6：出海垂钓成绩。`territory_type` 只有两个值，官网就是按这两个
 * id 分「近海」和「远洋」两块显示的（Fishing chunk 里写死 "900" / "1163"）。
 */
data class ShizhijiaFishSea(
    val territory: String,
    val seaTimes: Int,
    val maxSeaScore: Int,
) {
    val label: String
        get() = when (territory) {
            "900" -> "近海（出海垂钓）"
            "1163" -> "远洋垂钓"
            else -> "航线 $territory"
        }

    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaFishSea> = a.map {
            ShizhijiaFishSea(
                territory = it.str("territory_type"), seaTimes = it.int("sea_times"),
                maxSeaScore = it.int("max_sea_score"),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 投影外观（幻化）
// ---------------------------------------------------------------------------

/** getDressTotal7：总览。 */
data class ShizhijiaDressTotal(
    val vanityTimes: Int,
    val colorTimes: Int,
    val setNum: Int,
    val washingNum: Int,
) {
    companion object {
        fun fromArray(a: JSONArray?): ShizhijiaDressTotal? = a.map {
            ShizhijiaDressTotal(
                vanityTimes = it.int("vanity_times"), colorTimes = it.int("color_times"),
                setNum = it.int("set_num"), washingNum = it.int("washing_num"),
            )
        }.firstOrNull()
    }
}

/** getDressRace1：各种族/性别的使用天数。`continue_rate` 是占比。 */
data class ShizhijiaDressRace(
    val race: String,
    val gender: String,
    val beginDate: String,
    val continueDays: Int,
    val continueRate: Double,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaDressRace> = a.map {
            ShizhijiaDressRace(
                race = it.str("race"), gender = it.str("gender"),
                beginDate = it.str("begin_date"), continueDays = it.int("continue_days"),
                continueRate = it.num("continue_rate"),
            )
        }
    }
}

/**
 * getDressVanity4：最常用的投影部件。这个接口自带 Name/Icon/ItemUICategory，
 * 是少见的能直接出图的。`rank_type` 目前见到 "total"。
 */
data class ShizhijiaDressVanity(
    val itemId: Int,
    val name: String,
    val icon: Int,
    val category: Int,
    val dressType: String,
    val times: Int,
    val rank: Int,
    val rankType: String,
) {
    /** 道具图标。 */
    val iconUrl: String get() = ShizhijiaIcons.item(icon)

    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaDressVanity> = a.map {
            ShizhijiaDressVanity(
                itemId = it.optInt("ItemId"), name = it.str("Name"),
                icon = it.optInt("Icon"), category = it.optInt("ItemUICategory"),
                dressType = it.str("dress_type"), times = it.int("times"),
                rank = it.int("rn_times"), rankType = it.str("rank_type"),
            )
        }
    }
}

/** getDressFullset5：套装记录。`partitem` 是逗号分隔的部件 id。 */
data class ShizhijiaDressFullset(
    val setItem: String,
    val partItems: List<String>,
    val logTime: String,
    val index: String,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaDressFullset> = a.map {
            ShizhijiaDressFullset(
                setItem = it.str("setitem"),
                partItems = it.str("partitem").split(",").filter { p -> p.isNotBlank() },
                logTime = it.str("log_time"), index = it.str("index_index"),
            )
        }
    }
}

/** getDressColor2 / getDressOrnament3：染色和时尚配饰的使用次数。 */
data class ShizhijiaDressUse(
    val id: String,
    val times: Int,
    val rank: Int,
) {
    companion object {
        /** color2 用 catalog_id/color_times；ornament3 用 ornament/ornament_times。 */
        fun fromArray(a: JSONArray?, idKey: String, timesKey: String): List<ShizhijiaDressUse> = a.map {
            ShizhijiaDressUse(id = it.str(idKey), times = it.int(timesKey), rank = it.int("rn"))
        }
    }
}

// ---------------------------------------------------------------------------
// 新月岛（MKD = 南方博兹雅/新月岛的内部叫法）
// ---------------------------------------------------------------------------

/** getMKDTotal1：总览。银/金是幻卡币，白银/白金是"白"版本。 */
data class ShizhijiaMkdTotal(
    val nowLevel: Int,
    val ceTimes: Int,
    val fateTimes: Int,
    val silverNum: Int,
    val goldNum: Int,
    val whiteSilverNum: Int,
    val whiteGoldNum: Int,
) {
    companion object {
        fun fromArray(a: JSONArray?): ShizhijiaMkdTotal? = a.map {
            ShizhijiaMkdTotal(
                nowLevel = it.int("now_level"), ceTimes = it.int("ce_times"),
                fateTimes = it.int("fate_times"), silverNum = it.int("silver_num"),
                goldNum = it.int("gold_num"), whiteSilverNum = it.int("white_silver_num"),
                whiteGoldNum = it.int("white_gold_num"),
            )
        }.firstOrNull()
    }
}

/** getMKDSupportJob2：临时职业等级。support_job 是职业 id。 */
data class ShizhijiaMkdJob(
    val supportJob: String,
    val nowLevel: Int,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaMkdJob> = a.map {
            ShizhijiaMkdJob(supportJob = it.str("support_job"), nowLevel = it.int("now_level"))
        }
    }
}

/** getMKDItemGet4 / ItemUse3：道具获取/使用。 */
data class ShizhijiaMkdItem(
    val id: String,
    val name: String,
    val type: String,
    val num: Int,
    val firstTime: String,
) {
    companion object {
        fun fromArray(a: JSONArray?, numKey: String = "get_num"): List<ShizhijiaMkdItem> = a.map {
            ShizhijiaMkdItem(
                id = it.str("catalog_id"), name = it.str("catalog_name"),
                type = it.str("catalog_type"), num = it.int(numKey),
                firstTime = it.str("first_time"),
            )
        }
    }
}

/** getMKDItemBox5：宝箱。box_level 是 gold/silver/copper。 */
data class ShizhijiaMkdBox(
    val boxType: String,
    val boxLevel: String,
    val num: Int,
) {
    val levelLabel: String
        get() = when (boxLevel) {
            "gold" -> "金"
            "silver" -> "银"
            "copper" -> "铜"
            else -> boxLevel
        }

    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaMkdBox> = a.map {
            ShizhijiaMkdBox(
                boxType = it.str("box_type"), boxLevel = it.str("box_level"),
                num = it.int("num"),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 零式
// ---------------------------------------------------------------------------

/** getLingShiTotal：总览。elapsed_time 是官网的"用时"（单位未知，先原样显示）。 */
data class ShizhijiaSavageTotal(
    val enterNum: Int,
    val finishTimes: Int,
    val territoryNum: Int,
    val elapsedTime: Int,
) {
    companion object {
        fun fromArray(a: JSONArray?): ShizhijiaSavageTotal? = a.map {
            ShizhijiaSavageTotal(
                enterNum = it.int("enter_num"), finishTimes = it.int("finish_times"),
                territoryNum = it.int("territory_num"), elapsedTime = it.int("elapsed_time"),
            )
        }.firstOrNull()
    }
}

/**
 * getLingShi：单个副本的通关记录。`no_limit` = 1 表示解除限制（能力全开）通关，
 * 也就是不算"当期"击杀。`job_name` 经常是空串。
 */
data class ShizhijiaSavageClear(
    val territory: String,
    val jobName: String,
    val logTime: String,
    val noLimit: Boolean,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaSavageClear> = a.map {
            ShizhijiaSavageClear(
                territory = it.str("territory_type"), jobName = it.str("job_name"),
                logTime = it.str("log_time"), noLimit = it.str("no_limit") == "1",
            )
        }
    }
}

/**
 * 零式副本表：id -> 名字。getLingShi 只给 territory_type，名字要靠这张表。
 * 抄自官网 index.js 里的静态副本表（7 个资料片段）。
 */
object ShizhijiaSavageTable {

    data class Tier(val name: String, val raids: List<Pair<String, Int>>)
    data class Expansion(val name: String, val abbr: String, val tiers: List<Tier>)

    val EXPANSIONS: List<Expansion> = listOf(
        Expansion("阿卡狄亚零式登天斗技场", "THE ARCADION", listOf(
            Tier("轻量级", listOf("轻量级1" to 1226, "轻量级2" to 1228, "轻量级3" to 1230, "轻量级4" to 1232)),
            Tier("中量级", listOf("中量级1" to 1257, "中量级2" to 1259, "中量级3" to 1261, "中量级4" to 1263)),
            Tier("重量级", listOf("重量级1" to 1321, "重量级2" to 1323, "重量级3" to 1325, "重量级4" to 1327)),
        )),
        Expansion("零式万魔殿", "PANDAEMONIUM", listOf(
            Tier("边境之狱", listOf("边境之狱1" to 1003, "边境之狱2" to 1005, "边境之狱3" to 1007, "边境之狱4" to 1009)),
            Tier("炼净之狱", listOf("炼净之狱1" to 1082, "炼净之狱2" to 1084, "炼净之狱3" to 1086, "炼净之狱4" to 1088)),
            Tier("荒天之狱", listOf("荒天之狱1" to 1148, "荒天之狱2" to 1150, "荒天之狱3" to 1152, "荒天之狱4" to 1154)),
        )),
        Expansion("伊甸零式希望乐园", "Eden", listOf(
            Tier("觉醒之章", listOf("觉醒之章1" to 853, "觉醒之章2" to 854, "觉醒之章3" to 855, "觉醒之章4" to 856)),
            Tier("共鸣之章", listOf("共鸣之章1" to 906, "共鸣之章2" to 907, "共鸣之章3" to 908, "共鸣之章4" to 909)),
            Tier("再生之章", listOf("再生之章1" to 946, "再生之章2" to 947, "再生之章3" to 948, "再生之章4" to 949)),
        )),
        Expansion("欧米茄零式时空狭缝", "Omega", listOf(
            Tier("德尔塔幻境", listOf("德尔塔幻境1" to 695, "德尔塔幻境2" to 696, "德尔塔幻境3" to 697, "德尔塔幻境4" to 698)),
            Tier("西格玛幻境", listOf("西格玛幻境1" to 752, "西格玛幻境2" to 753, "西格玛幻境3" to 754, "西格玛幻境4" to 755)),
            Tier("阿尔法幻境", listOf("阿尔法幻境1" to 802, "阿尔法幻境2" to 803, "阿尔法幻境3" to 804, "阿尔法幻境4" to 805)),
        )),
        // 老资料片官网只收了每层的最终关，没有分 tier。
        Expansion("亚历山大零式机神城", "Alexander", listOf(
            Tier("最终关", listOf("启动之章4" to 452, "律动之章4" to 532, "天动之章4" to 587)),
        )),
        Expansion("巴哈姆特大迷宫", "Bahamut", listOf(
            Tier("最终关", listOf("邂逅之章5" to 245, "入侵之章4" to 358, "真源之章4" to 196)),
        )),
        Expansion("巴哈姆特零式大迷宫", "Bahamut (Savage)", listOf(
            Tier("入侵之章", listOf("入侵之章1" to 380, "入侵之章2" to 381, "入侵之章3" to 382, "入侵之章4" to 383)),
        )),
    )

    /** territory_type -> "资料片 关卡名"。 */
    val NAMES: Map<Int, String> = buildMap {
        EXPANSIONS.forEach { e ->
            e.tiers.forEach { t -> t.raids.forEach { (n, id) -> put(id, "${e.name} $n") } }
        }
    }

    fun name(territory: String): String =
        territory.toIntOrNull()?.let { NAMES[it] } ?: "副本 $territory"
}

// ---------------------------------------------------------------------------
// 朝圣交错路 + 绝境战
//
// 这两屏的字段名不是从响应里看出来的——用户没通绝、朝圣交错路也只进过一次，
// 两轮探测这些接口都回空数组。字段名是从官网 DeepDungeon / Ultimate chunk 的
// 视图代码里扒的（那里读的键名是明文），所以解析一律"取不到就给 0/空"，
// 别因为某个字段没对上就整屏崩掉。
// ---------------------------------------------------------------------------

/**
 * 官网把两个字段打包成了字符串，格式 `"id:次数,id:次数"`：
 *
 * - `job_clear_times` 的 id 是职业 id
 * - `annihilation_num` 的 id 是 TerritoryType（朝圣路段）
 *
 * 拆出来给界面用。
 */
internal fun parsePackedPairs(raw: String): List<Pair<String, Int>> =
    raw.split(",").mapNotNull { part ->
        val kv = part.split(":")
        if (kv.size < 2) return@mapNotNull null
        val k = kv[0].trim()
        val v = kv[1].trim().toIntOrNull() ?: return@mapNotNull null
        if (k.isEmpty()) null else k to v
    }

/** 秒 -> "1小时23分45秒"，和官网一致（0 小时不显示"0小时"）。 */
internal fun fmtElapsed(seconds: Int): String {
    if (seconds <= 0) return "-"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return buildString {
        if (h > 0) append("${h}小时")
        append("${m}分")
        append("${s}秒")
    }
}

/**
 * 朝圣交错路的十个路段。抄自官网 DeepDungeon chunk 的静态表。
 * `annihilation_num` 里的 id 就是这里的 TerritoryType。
 */
object ShizhijiaDeepDungeon {
    /** dataCenter 只做了这一个深宫。 */
    const val DD_TYPE = "dd4"
    const val TERRITORY = 1311
    const val LABEL = "朝圣交错路"

    /** TerritoryType -> 路段名（已去掉"朝圣交错路 "前缀，和官网显示一致）。 */
    val SEGMENTS: Map<Int, String> = mapOf(
        1281 to "第1～10朝圣路",
        1282 to "第11～20朝圣路",
        1283 to "第21～30朝圣路",
        1284 to "第31～40朝圣路",
        1285 to "第41～50朝圣路",
        1286 to "第51～60朝圣路",
        1287 to "第61～70朝圣路",
        1288 to "第71～80朝圣路",
        1289 to "第81～90朝圣路",
        1290 to "第91～100朝圣路",
    )

    fun segment(id: String): String =
        id.toIntOrNull()?.let { SEGMENTS[it] } ?: "路段 $id"
}

/**
 * getDDTerr1 的一行。官网按 `is_solo` 把结果分成单人/组队两块显示。
 *
 * `clear_elapsed_time` 是秒；`annihilation_num` 和 `job_clear_times` 是打包字符串。
 */
data class ShizhijiaDdProgress(
    val solo: Boolean,
    val totalClearTime: Int,
    val clearElapsedTime: Int,
    val failedTimes: Int,
    val totalDeadNum: Int,
    val armorLevel: Int,
    val weaponLevel: Int,
    /** 路段 -> 团灭次数。 */
    val annihilation: List<Pair<String, Int>>,
    /** 职业 id -> 通关次数。 */
    val jobClears: List<Pair<String, Int>>,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaDdProgress> = a.map {
            ShizhijiaDdProgress(
                solo = it.str("is_solo") == "1",
                totalClearTime = it.int("total_clear_time"),
                clearElapsedTime = it.int("clear_elapsed_time"),
                failedTimes = it.int("failed_times"),
                totalDeadNum = it.int("total_dead_num"),
                armorLevel = it.int("armor_level"),
                weaponLevel = it.int("weapon_level"),
                annihilation = parsePackedPairs(it.str("annihilation_num")),
                jobClears = parsePackedPairs(it.str("job_clear_times")),
            )
        }
    }
}

/**
 * getDDGaoNan2 / gaoNanFirst1 共用的"高难通关"形状。
 *
 * 官网对 getDDGaoNan2 只取 `data[0]`（单条），绝境战那边是 7 个位置一组。
 */
data class ShizhijiaHardClear(
    val logTime: String,
    val classJob: String,
    val clearTimes: Int,
    val deadTimes: Int,
    val elapsedTime: Int,
    val enterBeforeClear: Int,
    val jobClears: List<Pair<String, Int>>,
) {
    companion object {
        fun fromJson(o: JSONObject?): ShizhijiaHardClear? {
            if (o == null) return null
            return ShizhijiaHardClear(
                logTime = o.str("log_time"),
                classJob = o.str("class_job"),
                clearTimes = o.int("clear_times"),
                deadTimes = o.int("dead_times"),
                elapsedTime = o.int("elapsed_time"),
                enterBeforeClear = o.int("enter_before_clear"),
                jobClears = parsePackedPairs(o.str("job_clear_times")),
            )
        }

        fun fromArray(a: JSONArray?): List<ShizhijiaHardClear> = a.map { fromJson(it)!! }
    }
}

/** 队伍成员一行（gaoNanTeam2 / getDDFirstTeam7 同形）。 */
data class ShizhijiaTeamMember(
    /** 注意官网模板里写的是 `character_namee`（多一个 e，是他们的笔误），两个都收。 */
    val characterName: String,
    val groupName: String,
    val areaName: String,
    /**
     * 这个字段名叫 job_name，但**装的是职业 id**——官网是
     * `jobs.find(j => j.id == row.job_name)` 这么用的。
     */
    val jobId: String,
    val classJob: String,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaTeamMember> = a.map {
            ShizhijiaTeamMember(
                characterName = it.str("character_name").ifBlank { it.str("character_namee") },
                groupName = it.str("group_name"),
                areaName = it.str("area_name"),
                jobId = it.str("job_name"),
                classJob = it.str("class_job"),
            )
        }
    }
}

/** gaoNanJob3：各职业通关次数。这里的 job_name 是真名字。 */
data class ShizhijiaJobTimes(val jobName: String, val times: Int) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaJobTimes> = a.map {
            ShizhijiaJobTimes(it.str("job_name"), it.int("job_times"))
        }
    }
}

/** gaoNanFriend4：一起通关最多的队友。 */
data class ShizhijiaFriendTimes(
    val characterName: String,
    val groupName: String,
    val times: Int,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaFriendTimes> = a.map {
            ShizhijiaFriendTimes(
                characterName = it.str("character_name").ifBlank { it.str("team_chara_name") },
                groupName = it.str("group_name"),
                times = it.int("friend_times"),
            )
        }
    }
}

/**
 * 一条道具获取记录（getDDHistory4 / getMKDIHistory6）。
 *
 * 和 [ShizhijiaMkdItem] 的区别是这里没有 `get_num`——一行就是一次获取，
 * 只带 `log_time`。实测形状：catalog_id / catalog_name / catalog_type /
 * dd_type / log_time。
 */
data class ShizhijiaItemLog(
    val id: String,
    val name: String,
    val type: String,
    val logTime: String,
) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaItemLog> = a.map {
            ShizhijiaItemLog(
                id = it.str("catalog_id"), name = it.str("catalog_name"),
                type = it.str("catalog_type"), logTime = it.str("log_time"),
            )
        }
    }
}

/** 团灭坐标（gaoNanDeadPoint5 / getDDDeadPoint6）。官网画成热点图。 */
data class ShizhijiaDeadPoint(val x: Double, val y: Double) {
    companion object {
        fun fromArray(a: JSONArray?): List<ShizhijiaDeadPoint> = a.map {
            ShizhijiaDeadPoint(it.num("point_x"), it.num("point_y"))
        }
    }
}

/**
 * 7 个绝境战。territory_type 来自官网 Ultimate.js 的枚举，
 * medal_id 是成就里的奖章序号（1 = 绝巴哈）。
 */
enum class ShizhijiaUltimate(
    val territory: Int,
    val label: String,
    val medalId: Int,
) {
    BAHAMUT(733, "巴哈姆特绝境战", 1),
    WEAPON(777, "究极神兵绝境战", 2),
    ALEXANDER(887, "亚历山大绝境战", 3),
    DRAGONSONG(968, "幻想龙诗绝境战", 4),
    OMEGA(1122, "欧米茄绝境验证战", 5),
    FUTURES(1238, "光暗未来绝境战", 6),
    DANCING_MAD(1363, "妖星乱舞绝境战", 7);

    companion object {
        fun byTerritory(id: Int): ShizhijiaUltimate? = entries.find { it.territory == id }
    }
}
