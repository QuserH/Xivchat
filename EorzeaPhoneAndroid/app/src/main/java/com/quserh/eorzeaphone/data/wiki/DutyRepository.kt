package com.quserh.eorzeaphone.data.wiki

import android.content.Context
import android.database.Cursor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 副本（迷宫挑战 / 讨伐歼灭战 / 大型任务 / 绝境战 …）的本地表。
 *
 * ## 为什么要有这个
 *
 * 用户反馈「搜歼灭战搜不出来，点了只能跳网页」。原因是本地库当初只建了
 * items/quests/nodes/maps/aetherytes 五类，副本一条没有，所以副本只能靠
 * [WikiSearch] 的线上全文检索兜底，命中的是 [WikiHit.Page]，而那个类型在 UI 里
 * 是硬编码 `Intent.ACTION_VIEW` 跳浏览器的。
 *
 * 现在 `Data:Instance` 目录的 json 的 427 页进了本地库（构建脚本
 * `开发/WIKI/build_duties.py`），副本变成和物品/任务同级的本地条目。
 *
 * 数据来自最终幻想XIV中文维基。
 */
data class WikiDuty(
    val id: Int,
    val name: String,
    val nameJa: String,
    val nameEn: String,
    /** 「讨伐歼灭战」/「迷宫挑战」/「大型任务」等，用户就是按这个词找的。 */
    val type: String,
    val typeId: Int,
    val version: Int,
    /** 副本横幅图。和物品 icon 同一套 icon-space 约定，喂 ItemIconLoader 即可。 */
    val imageId: Int,
    val levelMin: Int,
    val levelMax: Int,
    val ilvlMin: Int,
    val ilvlMax: Int,
    val timeLimit: Int,
    val place: String,
    val mapId: Int,
    val mapPlace: String,
    val tank: Int,
    val healer: Int,
    val melee: Int,
    val ranged: Int,
    val anyJob: Boolean,
    val partyCount: Int,
    val isAlliance: Boolean,
    val unrestricted: Boolean,
    val joinMidway: Boolean,
    val phoenixDown: Boolean,
    val echoKeep: Int,
    val echoStack: Int,
    val gil: Int,
    val exp: Int,
    val currencyA: Int,
    val currencyB: Int,
    val bosses: List<String>,
    val midBosses: List<String>,
    val description: String,
) {
    /** "Lv50" / "Lv71-80"（最高等级为 0 时只显示最低）。 */
    val levelText: String
        get() = when {
            levelMax > 0 && levelMax != levelMin -> "Lv$levelMin-$levelMax"
            levelMin > 0 -> "Lv$levelMin"
            else -> ""
        }

    /**
     * 人数构成。"防护2 治疗2 近战2 远程2" / "8人" / "不限职业"。
     *
     * 行会令那种 `任意职业` 的没有角色分配，只能报总人数。
     */
    val partyText: String
        get() {
            val roles = tank + healer + melee + ranged
            if (anyJob || roles == 0) {
                val n = totalPlayers
                return if (n > 0) "${n}人 不限职业" else "不限职业"
            }
            return buildList {
                if (tank > 0) add("防护$tank")
                if (healer > 0) add("治疗$healer")
                if (melee > 0) add("近战$melee")
                if (ranged > 0) add("远程$ranged")
            }.joinToString(" ")
        }

    /** 总人数。团队副本是 小队数量 × 每队人数（24 人本 = 3 × 8）。 */
    val totalPlayers: Int
        get() {
            val per = tank + healer + melee + ranged
            val n = if (partyCount > 1) per * partyCount else per
            return n
        }

    /** "8人" / "24人（3队）" */
    val sizeText: String
        get() {
            val n = totalPlayers
            if (n <= 0) return ""
            return if (partyCount > 1) "${n}人（${partyCount}队）" else "${n}人"
        }

    /** "60分钟" */
    val timeText: String get() = if (timeLimit > 0) "${timeLimit}分钟" else ""

    /** 列表副标题："讨伐歼灭战 · Lv50 · 炎帝陵" */
    val subtitle: String
        get() = buildList {
            type.takeIf { it.isNotBlank() }?.let(::add)
            levelText.takeIf { it.isNotBlank() }?.let(::add)
            (place.takeIf { it.isNotBlank() } ?: mapPlace).takeIf { it.isNotBlank() }
                ?.let(::add)
        }.joinToString(" · ")
}

/** 一条副本掉落。[kind] 0 = 直接奖励，1 = 宝箱。 */
data class DutyDrop(
    val dutyId: Int,
    val itemId: Int,
    val kind: Int,
    val qty: Int,
    val chance: Boolean,
    val weekly: Boolean,
)

object DutyDb {
    private const val SEARCH_LIMIT = 40

    private const val COLS =
        "id, name, name_ja, name_en, type, type_id, version, image_id, " +
            "level_min, level_max, ilvl_min, ilvl_max, time_limit, place, " +
            "map_id, map_place, tank, healer, melee, ranged, any_job, " +
            "party_count, is_alliance, unrestricted, join_midway, phoenix_down, " +
            "echo_keep, echo_stack, gil, exp, currency_a, currency_b, " +
            "bosses, mid_bosses, description"

    /**
     * 库里没有 duties 表时（用户装的是旧包、assets 里还是老库）一律当"没有副本"处理，
     * 而不是让整个检索炸掉。[WikiSearch] 三路并行，一路挂了不该拖垮另两路。
     */
    private suspend fun hasTable(context: Context): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            WikiDb.open(context).rawQuery(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name='duties'", null,
            ).use { it.moveToFirst() }
        }.getOrDefault(false)
    }

    /**
     * 搜副本。名字（中/日/英）、类型、BOSS 名都吃。
     *
     * BOSS 名参与匹配是故意的：用户搜「泰坦」想找的是泰坦歼灭战，
     * 而副本名里未必含 BOSS 名（「究极神兵破坏作战」的 BOSS 是究极神兵）。
     *
     * 排序：完全同名最前，前缀匹配次之，然后按等级、类内排序。
     */
    suspend fun search(context: Context, query: String): List<WikiDuty> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isBlank() || !hasTable(context)) return@withContext emptyList()
            val like = "%$q%"
            val sql = """
                SELECT $COLS FROM duties
                WHERE name LIKE ? OR name_ja LIKE ? OR LOWER(name_en) LIKE ?
                   OR type LIKE ? OR bosses LIKE ?
                ORDER BY
                  CASE WHEN name = ? THEN 0
                       WHEN name LIKE ? THEN 1
                       WHEN bosses LIKE ? THEN 2 ELSE 3 END,
                  level_min DESC, type_id, sort
                LIMIT $SEARCH_LIMIT
            """.trimIndent()
            runCatching {
                WikiDb.open(context).rawQuery(
                    sql,
                    arrayOf(
                        like, like, "%${q.lowercase()}%", like, like,
                        q, "$q%", "$q%",
                    ),
                ).use { c -> buildList(c.count) { while (c.moveToNext()) add(readRow(c)) } }
            }.getOrDefault(emptyList())
        }

    suspend fun byId(context: Context, id: Int): WikiDuty? = withContext(Dispatchers.IO) {
        if (!hasTable(context)) return@withContext null
        runCatching {
            WikiDb.open(context)
                .rawQuery("SELECT $COLS FROM duties WHERE id = ?", arrayOf(id.toString()))
                .use { if (it.moveToFirst()) readRow(it) else null }
        }.getOrNull()
    }

    /** 按类型列副本，"讨伐歼灭战" 118 个那种浏览列表。 */
    suspend fun byType(context: Context, type: String): List<WikiDuty> =
        withContext(Dispatchers.IO) {
            if (!hasTable(context)) return@withContext emptyList()
            runCatching {
                WikiDb.open(context).rawQuery(
                    "SELECT $COLS FROM duties WHERE type = ? " +
                        "ORDER BY level_min DESC, sort",
                    arrayOf(type),
                ).use { c -> buildList(c.count) { while (c.moveToNext()) add(readRow(c)) } }
            }.getOrDefault(emptyList())
        }

    /** 有哪些副本类型（带计数），用来填浏览页的分组。 */
    suspend fun types(context: Context): List<Pair<String, Int>> =
        withContext(Dispatchers.IO) {
            if (!hasTable(context)) return@withContext emptyList()
            runCatching {
                WikiDb.open(context).rawQuery(
                    "SELECT type, COUNT(*) FROM duties WHERE type <> '' " +
                        "GROUP BY type ORDER BY COUNT(*) DESC", null,
                ).use { c ->
                    buildList { while (c.moveToNext()) add(c.getString(0) to c.getInt(1)) }
                }
            }.getOrDefault(emptyList())
        }

    /** 这个副本掉什么。 */
    suspend fun drops(context: Context, dutyId: Int): List<DutyDrop> =
        withContext(Dispatchers.IO) {
            if (!hasTable(context)) return@withContext emptyList()
            runCatching {
                WikiDb.open(context).rawQuery(
                    "SELECT duty_id, item_id, kind, qty, chance, weekly FROM duty_drops " +
                        "WHERE duty_id = ? ORDER BY kind, item_id",
                    arrayOf(dutyId.toString()),
                ).use { c ->
                    buildList(c.count) {
                        while (c.moveToNext()) {
                            add(
                                DutyDrop(
                                    dutyId = c.getInt(0), itemId = c.getInt(1),
                                    kind = c.getInt(2), qty = c.getInt(3),
                                    chance = c.getInt(4) != 0, weekly = c.getInt(5) != 0,
                                )
                            )
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }

    /**
     * 反查：这件装备哪些副本掉。
     *
     * 物品的 `来源` 字段建库时为了压体积被整块裁掉了，这里等于把"副本产出"
     * 那一部分补回来，且不必联网。
     */
    suspend fun dutiesDropping(context: Context, itemId: Int): List<WikiDuty> =
        withContext(Dispatchers.IO) {
            if (!hasTable(context)) return@withContext emptyList()
            runCatching {
                WikiDb.open(context).rawQuery(
                    "SELECT ${COLS.split(", ").joinToString(", ") { "d.$it" }} " +
                        "FROM duties d JOIN duty_drops p ON p.duty_id = d.id " +
                        "WHERE p.item_id = ? ORDER BY d.level_min DESC, d.sort",
                    arrayOf(itemId.toString()),
                ).use { c -> buildList(c.count) { while (c.moveToNext()) add(readRow(c)) } }
            }.getOrDefault(emptyList())
        }

    private fun splitList(s: String?): List<String> =
        (s ?: "").split(',').map { it.trim() }.filter { it.isNotEmpty() }

    private fun readRow(c: Cursor) = WikiDuty(
        id = c.getInt(0),
        name = c.getString(1) ?: "",
        nameJa = c.getString(2) ?: "",
        nameEn = c.getString(3) ?: "",
        type = c.getString(4) ?: "",
        typeId = c.getInt(5),
        version = c.getInt(6),
        imageId = c.getInt(7),
        levelMin = c.getInt(8),
        levelMax = c.getInt(9),
        ilvlMin = c.getInt(10),
        ilvlMax = c.getInt(11),
        timeLimit = c.getInt(12),
        place = c.getString(13) ?: "",
        mapId = c.getInt(14),
        mapPlace = c.getString(15) ?: "",
        tank = c.getInt(16),
        healer = c.getInt(17),
        melee = c.getInt(18),
        ranged = c.getInt(19),
        anyJob = c.getInt(20) != 0,
        partyCount = c.getInt(21),
        isAlliance = c.getInt(22) != 0,
        unrestricted = c.getInt(23) != 0,
        joinMidway = c.getInt(24) != 0,
        phoenixDown = c.getInt(25) != 0,
        echoKeep = c.getInt(26),
        echoStack = c.getInt(27),
        gil = c.getInt(28),
        exp = c.getInt(29),
        currencyA = c.getInt(30),
        currencyB = c.getInt(31),
        bosses = splitList(c.getString(32)),
        midBosses = splitList(c.getString(33)),
        description = c.getString(34) ?: "",
    )
}
