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
     * 人数构成。"防护2 治疗2 近战2 远程2" / "不限职业" / 空。
     *
     * 三种情况分清楚，别互相冒充（实测 427 条里的分布）：
     * - 有角色分配（406 条）→ 报构成
     * - `任意职业` 为真（11 条：节日副本、多变迷宫、卓异的悲寂那两个）
     *   → "不限职业"，这是数据明确说的
     * - 既没角色也没那个标记（10 条宝物库）→ **返回空**
     *
     * 最后那一类以前会被说成"不限职业"。宝物库大概确实不限，但数据里
     * 没这么写，我不能替它下结论 —— 详情页少一行事实，比多一行猜测好。
     */
    val partyText: String
        get() {
            val roles = tank + healer + melee + ranged
            if (roles == 0) return if (anyJob) "不限职业" else ""
            if (anyJob) {
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

    /**
     * 图标种类，UI 拿它换成 `R.drawable.ic2_duty_*`。
     *
     * ## 为什么不用 [imageId]
     *
     * `imageId` 是站点的副本**横幅图**（112021 那种，实测 55-60 KB 的宽幅 PNG）。
     * 塞进列表里 34dp 的方框会被压得看不出是什么，而且每一行都要联网拉一张图。
     * 用户要的是「副本图片用通用的就好了，迷宫就用迷宫的图标」——
     * 按类型给一套矢量图标，离线、清晰、一眼分得出种类。
     *
     * ## 为什么按 [typeId] 分派而不是按 [type] 字符串
     *
     * `typeId` 是数据库里的稳定字段；`type` 是展示用的中文名，站点改字就断。
     * 而且有 16 个副本**没有** `type`（宝物库 10 个、节日副本 4 个、
     * 卓异的悲寂那 2 个），但它们的 `typeId` 是有值的（0 / 22 / 39），
     * 按 ID 分派这些也能拿到对的图标。
     *
     * 实测 type_id 取值（427 个副本，全覆盖）：
     * 0=宝物库10、2=迷宫挑战104、3=行会令14、4=讨伐歼灭战118、5=大型任务155、
     * 22=节日副本4、28=绝境战7、30=特殊迷宫探索12、37=诛灭战1、39=卓异的悲寂2。
     */
    val iconKind: String get() = DutyDb.kindForTypeId(typeId)

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
     * 排序分 6 档：同名 → 名字前缀 → **名字包含** → BOSS 前缀 →
     * BOSS 包含 → 只靠类型命中。
     *
     * 「名字包含」必须排在「只靠类型命中」前面，否则搜「歼灭战」头几条是
     * *幻巧战* —— 它们的 `类型` 正是「讨伐歼灭战」，于是和真正名字里带
     * 「歼灭战」的挤在同一档，再按等级降序一排就翻到前面去了。
     * 这是打包出 APK 后按真实数据核出来的，不是推测。
     */
    suspend fun search(context: Context, query: String): List<WikiDuty> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isBlank() || !hasTable(context)) return@withContext emptyList()
            val like = "%$q%"
            val prefix = "$q%"
            val sql = """
                SELECT $COLS FROM duties
                WHERE name LIKE ? OR name_ja LIKE ? OR LOWER(name_en) LIKE ?
                   OR type LIKE ? OR bosses LIKE ?
                ORDER BY
                  CASE WHEN name = ? THEN 0
                       WHEN name LIKE ? THEN 1
                       WHEN name LIKE ? THEN 2
                       WHEN bosses LIKE ? THEN 3
                       WHEN bosses LIKE ? THEN 4 ELSE 5 END,
                  level_min DESC, type_id, sort
                LIMIT $SEARCH_LIMIT
            """.trimIndent()
            runCatching {
                WikiDb.open(context).rawQuery(
                    sql,
                    arrayOf(
                        like, like, "%${q.lowercase()}%", like, like,
                        q, prefix, like, prefix, like,
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

    /**
     * 源数据里有 16 条副本没有 `类型` 字段（10 个宝物库、4 个节日副本、
     * 2 个 tyid=39 的，其中一个是「卓异的悲寂歼灭战」）。它们是真副本，
     * 不能因为缺一个字段就在浏览页里消失 —— 归到这一组。
     *
     * 没有替它们硬编类型名：站点的 tyid→名 映射本地拿不到
     * （39/22 在有类型的记录里一次都没出现过），编一个名字就是造数据。
     */
    const val TYPE_OTHER = "其他"

    /** 按类型列副本，"讨伐歼灭战" 118 个那种浏览列表。 */
    suspend fun byType(context: Context, type: String): List<WikiDuty> =
        withContext(Dispatchers.IO) {
            if (!hasTable(context)) return@withContext emptyList()
            runCatching {
                val sql = if (type == TYPE_OTHER) {
                    "SELECT $COLS FROM duties WHERE type IS NULL OR type = '' " +
                        "ORDER BY level_min DESC, sort"
                } else {
                    "SELECT $COLS FROM duties WHERE type = ? " +
                        "ORDER BY level_min DESC, sort"
                }
                val args = if (type == TYPE_OTHER) null else arrayOf(type)
                WikiDb.open(context).rawQuery(sql, args)
                    .use { c -> buildList(c.count) { while (c.moveToNext()) add(readRow(c)) } }
            }.getOrDefault(emptyList())
        }

    /**
     * 浏览页的一个类型分组。
     *
     * [iconKind] 是这一组该用的图标；组里混了多种 type_id 时是 [KIND_MIXED]。
     * 只有 [TYPE_OTHER] 会混 —— 它装的是源数据没给类型名的 16 个，
     * 内部实际有 3 种（0 宝物库 / 22 节日副本 / 39 卓异的悲寂），
     * 给它们同一个图标会有两种是错的，所以这一组标成混合。
     * 组**内部**每一行仍然用各自的 [WikiDuty.iconKind]，那是准的。
     */
    data class DutyTypeGroup(val name: String, val count: Int, val iconKind: String)

    /** 一组里混了多种 type_id 时的图标标记。 */
    const val KIND_MIXED = "mixed"

    /** 有哪些副本类型（带计数 + 图标），用来填浏览页的分组。缺类型的归 [TYPE_OTHER]。 */
    suspend fun types(context: Context): List<DutyTypeGroup> =
        withContext(Dispatchers.IO) {
            if (!hasTable(context)) return@withContext emptyList()
            runCatching {
                WikiDb.open(context).rawQuery(
                    "SELECT CASE WHEN type IS NULL OR type = '' THEN '$TYPE_OTHER' " +
                        "ELSE type END AS t, COUNT(*), " +
                        "COUNT(DISTINCT type_id), MIN(type_id) " +
                        "FROM duties GROUP BY t ORDER BY COUNT(*) DESC", null,
                ).use { c ->
                    buildList {
                        while (c.moveToNext()) {
                            val distinctIds = c.getInt(2)
                            val kind = if (distinctIds == 1) {
                                // 组内只有一种 type_id，复用 WikiDuty 那张表
                                kindForTypeId(c.getInt(3))
                            } else {
                                KIND_MIXED
                            }
                            add(DutyTypeGroup(c.getString(0), c.getInt(1), kind))
                        }
                    }
                }
            }.getOrDefault(emptyList())
        }

    /**
     * type_id → 图标种类。[WikiDuty.iconKind] 和 [types] 共用这一张表，
     * 免得两处各写一份、改一处漏一处。
     */
    internal fun kindForTypeId(typeId: Int): String = when (typeId) {
        2 -> "dungeon"      // 迷宫挑战
        3 -> "guildhest"    // 行会令
        4 -> "trial"        // 讨伐歼灭战
        5 -> "raid"         // 大型任务
        28, 37, 39 -> "ultimate"   // 绝境战 / 诛灭战 / 卓异的悲寂
        30 -> "deep"        // 特殊迷宫探索（深宫、多变迷宫）
        22 -> "seasonal"    // 节日副本
        0 -> "treasure"     // 宝物库
        // 新补丁加的类型会走到这里。宁可给个通用图标，也别崩或留空白。
        else -> "dungeon"
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
