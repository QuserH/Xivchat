package com.quserh.eorzeaphone.data.wiki

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 限时采集点 + 艾欧泽亚时间。
 *
 * 只收限时点（`nodes.et_hours` 非空，实测 226 个）—— 常驻点没有"时钟"的意义，
 * 它们的位置在物品检索的详情页已经能看到。
 */
data class GatherNode(
    val id: Int,
    val kindId: Int,
    val level: Int,
    val stars: Int,
    val mapName: String,
    val areaName: String,
    val region: String,
    val x: Float,
    val y: Float,
    /** ET 出现时刻，如 [2, 14] */
    val etHours: List<Int>,
    /** 持续时长，单位是 **ET 分钟**（见 [EorzeaTime] 的说明） */
    val durationEtMin: Int,
    val folkloreName: String,
    /** 该点产出的物品，列表用第一个当标题 */
    val items: List<GatherItem>,
    /** 地图贴图名，如 `d2f3/00`，喂给 App 现成的 FishingMapImageLoader。空 = 无图 */
    val mapFile: String = "",
    /** 地图缩放系数（站点 sizeFactor），坐标换算要用。实测只有 100 和 95 两种 */
    val sizeFactor: Int = 100,
    /** 这张图上的以太之光，用来一键传送 */
    val aetherytes: List<GatherAetheryte> = emptyList(),
) {
    /**
     * 游戏坐标 → 图上比例（0-1）。
     *
     * `FishingScreen` 的显示公式是 `游戏 = 41/scale * (像素/2048) + 1`，
     * 这里是它的逆运算。226 个限时点全部落在 [0,1]、往返误差 0
     * （见 开发/WIKI/_verify_pins.py）。
     */
    val mapFracX: Float get() = fracOf(x)
    val mapFracY: Float get() = fracOf(y)

    private fun fracOf(game: Float): Float {
        val scale = maxOf(sizeFactor, 100) / 100f
        return ((game - 1f) * scale / 41f).coerceIn(0f, 1f)
    }

    /** 1 采矿、2/3 园艺、4 钓鱼。站点 Node.类型ID。 */
    val jobName: String
        get() = when (kindId) {
            1 -> "采矿工"
            2, 3 -> "园艺工"
            4 -> "捕鱼人"
            else -> ""
        }

    val placeText: String
        get() = listOf(mapName, areaName)
            .filter { it.isNotBlank() }.distinct().joinToString(" · ")

    val coordText: String get() = if (x > 0 || y > 0) "X:%.1f Y:%.1f".format(x, y) else ""

    val etHoursText: String get() = etHours.joinToString("、") { "%d:00".format(it) }
}

/**
 * 一个以太之光。[x] / [y] 是**像素坐标**（0-2048），和 `FishingSpot.aetherytes`
 * 一致，除 2048 就是图上比例 —— 注意和采集点的游戏坐标不是一套。
 */
data class GatherAetheryte(val name: String, val x: Float, val y: Float) {
    val fracX: Float get() = (x / 2048f).coerceIn(0f, 1f)
    val fracY: Float get() = (y / 2048f).coerceIn(0f, 1f)
}

data class GatherItem(val id: Int, val name: String, val iconId: Int, val iconHash: String)

/**
 * 艾欧泽亚时间。
 *
 * 系数取站点 `Gadget:Y.time.js` 的 `eorzeanTimeFactor = 20.5714285714`
 * （艾一天 1440 分钟 = 现实 70 分钟，即 1 ET 小时 = 175 真实秒）。
 *
 * **最容易错的地方**：`durationEtMin` 是 **ET 分钟**，不是真实分钟。
 * 站点 `Gadget:Gahtering.js` 里是
 * `e_end = e_start - 24h + durability*60*1000`，`durability` 直接加在艾时上。
 * 所以 240 只有约 11.7 真实分钟，站点写的"4小时"是按艾时说的。
 */
object EorzeaTime {
    const val FACTOR = 20.5714285714
    private const val ET_DAY_MS = 24L * 3_600_000

    /** 当前艾时的 时:分 */
    fun nowHourMinute(nowMs: Long = System.currentTimeMillis()): Pair<Int, Int> {
        val ofDay = (nowMs * FACTOR).toLong() % ET_DAY_MS
        return (ofDay / 3_600_000).toInt() to ((ofDay % 3_600_000) / 60_000).toInt()
    }

    /** ET 分钟 → 真实毫秒 */
    fun etMinutesToRealMs(etMin: Int): Long = (etMin * 60_000L / FACTOR).toLong()

    /**
     * 距下一次出现的真实毫秒数。
     *
     * **返回负值表示正在窗口内**，绝对值是剩余时间。这样列表按这个值升序排，
     * "现在可采集"自然排到最前，其次是最快要开的。
     */
    fun nextWindowMs(
        etHours: List<Int>,
        durationEtMin: Int,
        nowMs: Long = System.currentTimeMillis(),
    ): Long {
        if (etHours.isEmpty() || durationEtMin <= 0) return Long.MAX_VALUE
        val etNow = (nowMs * FACTOR).toLong()
        val etDayStart = etNow - etNow % ET_DAY_MS
        val windowRealMs = etMinutesToRealMs(durationEtMin)
        var soonest = Long.MAX_VALUE
        // 昨天/今天/明天三档：跨艾日边界时，昨天开的窗口可能还没关，
        // 今天的又都已过去，只看今天会算错。
        for (dayOffset in -1..1) {
            for (h in etHours) {
                val etOpen = etDayStart + dayOffset * ET_DAY_MS + h * 3_600_000L
                val realOpen = (etOpen / FACTOR).toLong()
                val realClose = realOpen + windowRealMs
                if (nowMs in realOpen until realClose) return -(realClose - nowMs)
                if (realOpen > nowMs) soonest = minOf(soonest, realOpen - nowMs)
            }
        }
        return soonest
    }

    /** 1:23:45 / 12:05 / 45s —— 和站点 formatCountdown 的形态一致。 */
    fun formatCountdown(ms: Long): String {
        val total = (ms / 1000).coerceAtLeast(0)
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return when {
            h > 0 -> "%d:%02d:%02d".format(h, m, s)
            m > 0 -> "%d:%02d".format(m, s)
            else -> "%ds".format(s)
        }
    }
}

object GatherClockDb {
    private const val NODE_COLS =
        "id, kind_id, level, stars, map_name, area_name, region, x, y, " +
            "et_hours, duration, folklore_name, map_file, size_factor"

    private fun android.database.Cursor.toGatherNode() = GatherNode(
        id = getInt(0),
        kindId = getInt(1),
        level = getInt(2),
        stars = getInt(3),
        mapName = getString(4) ?: "",
        areaName = getString(5) ?: "",
        region = getString(6) ?: "",
        x = getFloat(7),
        y = getFloat(8),
        etHours = (getString(9) ?: "").trim(',').split(',').mapNotNull(String::toIntOrNull),
        durationEtMin = getInt(10),
        folkloreName = getString(11) ?: "",
        items = emptyList(),
        mapFile = getString(12) ?: "",
        sizeFactor = getInt(13).takeIf { it > 0 } ?: 100,
    )

    /**
     * One node by id, timed or not, with its items and aetherytes.
     *
     * The list only loads the 226 timed nodes, but the wiki links to all 642 -- a jump
     * from an item's source list has to be able to land on a permanent node too.
     */
    suspend fun node(context: Context, id: Int): GatherNode? = withContext(Dispatchers.IO) {
        val db = WikiDb.open(context)
        val node = db.rawQuery("SELECT $NODE_COLS FROM nodes WHERE id = ?", arrayOf("$id"))
            .use { c -> if (c.moveToNext()) c.toGatherNode() else null }
            ?: return@withContext null
        val items = db.rawQuery(
            "SELECT i.id, i.name_cn, i.icon_id, i.icon_hash FROM node_items ni " +
                "JOIN items i ON i.id = ni.item_id WHERE ni.node_id = ? " +
                "ORDER BY i.item_level DESC",
            arrayOf("$id"),
        ).use { c ->
            buildList(c.count) {
                while (c.moveToNext()) {
                    add(GatherItem(c.getInt(0), c.getString(1) ?: "", c.getInt(2), c.getString(3) ?: ""))
                }
            }
        }
        val aetherytes = db.rawQuery(
            "SELECT name, x, y FROM aetherytes WHERE map_name = ?", arrayOf(node.mapName),
        ).use { c ->
            buildList(c.count) {
                while (c.moveToNext()) {
                    add(GatherAetheryte(c.getString(0) ?: "", c.getFloat(1), c.getFloat(2)))
                }
            }
        }
        node.copy(items = items, aetherytes = aetherytes)
    }

    /** 只取限时点，连带产出物品。642 个点里 226 个限时。 */
    suspend fun timedNodes(context: Context): List<GatherNode> = withContext(Dispatchers.IO) {
        val db = WikiDb.open(context)
        val nodes = db.rawQuery("SELECT $NODE_COLS FROM nodes WHERE et_hours <> '' AND duration > 0", null)
            .use { c -> buildList(c.count) { while (c.moveToNext()) add(c.toGatherNode()) } }
        // 以太之光按地图名一次捞完（38 张图 / 68 个点，不值得按需查）
        val aethersByMap = db.rawQuery(
            "SELECT map_name, name, x, y FROM aetherytes", null,
        ).use { c ->
            buildMap<String, MutableList<GatherAetheryte>> {
                while (c.moveToNext()) {
                    getOrPut(c.getString(0) ?: "") { mutableListOf() }.add(
                        GatherAetheryte(
                            c.getString(1) ?: "", c.getFloat(2), c.getFloat(3),
                        ),
                    )
                }
            }
        }
        // 一次把所有产出物品捞出来，别对 226 个点各查一次
        val itemsByNode = db.rawQuery(
            "SELECT ni.node_id, i.id, i.name_cn, i.icon_id, i.icon_hash " +
                "FROM node_items ni " +
                "JOIN items i ON i.id = ni.item_id " +
                "JOIN nodes n ON n.id = ni.node_id " +
                "WHERE n.et_hours <> '' AND n.duration > 0 " +
                "ORDER BY i.item_level DESC",
            null,
        ).use { c ->
            buildMap<Int, MutableList<GatherItem>> {
                while (c.moveToNext()) {
                    getOrPut(c.getInt(0)) { mutableListOf() }.add(
                        GatherItem(
                            c.getInt(1), c.getString(2) ?: "",
                            c.getInt(3), c.getString(4) ?: "",
                        ),
                    )
                }
            }
        }
        nodes.map {
            it.copy(
                items = itemsByNode[it.id].orEmpty(),
                aetherytes = aethersByMap[it.mapName].orEmpty(),
            )
        }
    }
}
