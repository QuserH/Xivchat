package com.quserh.eorzeaphone.data.wiki

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 任务检索的本地部分。
 *
 * 库和物品检索共用一个连接（[WikiDb.open]），三张表由
 * 开发/WIKI/build_quests.py + quest_chain.py 产出：
 *
 *   quests        5360 个任务，含所在块与块内布局坐标
 *   quest_prereq  5374 条前置边，external=1 表示跨块（根 → 块）
 *   quest_chain   468 个块的元信息
 *
 * 布局（col/row/tier）是**构建期算好**的，不在手机上跑图算法。理由见
 * quest_chain.py 顶部注释：布局是纯函数，离线算完能断言层数/交叉数，
 * App 只剩画和缩放。
 */

/** 树上的一个任务节点。坐标是块内的网格位置，不是像素。 */
data class QuestNode(
    val id: Int,
    val name: String,
    val nameEn: String,
    val level: Int,
    val type: String,
    val category: String,
    val expansion: Int,
    val jobGroup: String,
    val iconId: Int,
    val repeatable: Boolean,
    val cnOnly: Boolean,
    val place: String,
    /** 1 = 前置全部满足，2 = 满足其一。画「或」和「且」不是一回事。 */
    val prereqRel: Int,
    val chainId: Int,
    val col: Int,
    val row: Int,
    /** 层号。宽层折成多行后 row 会变，tier 保住「第几级」。 */
    val tier: Int,
    /** 这个任务是别的块的入口（画在树顶）。 */
    val isRoot: Boolean,
    val startNpc: QuestNpc,
    val endNpc: QuestNpc,
) {
    val isOrPrereq: Boolean get() = prereqRel == 2

    /** "等级 45 · 支线任务：黑衣森林" */
    val subtitle: String
        get() = buildList {
            if (level > 0) add("等级 $level")
            type.takeIf { it.isNotBlank() }?.let(::add)
        }.joinToString(" · ")
}

/**
 * 接取 / 交付 NPC。坐标直接来自 `Data:Quest` 的 `开始NPC.位置`，
 * 不用另外请求 —— 这是这个功能能离线给出「在哪接」的原因。
 */
data class QuestNpc(
    val id: Int,
    val name: String,
    val title: String,
    val x: Float,
    val y: Float,
    val mapId: Int,
    val mapName: String,
    val zoneName: String,
) {
    val hasPlace: Boolean get() = mapName.isNotBlank()
    val hasCoord: Boolean get() = x > 0f || y > 0f

    /** "格里达尼亚旧街 (6.5, 7.8)" */
    val placeText: String
        get() = buildString {
            append(mapName)
            if (hasCoord) append(" (%.1f, %.1f)".format(x, y))
        }

    /** "弗弗茶 · 行会会长" */
    val nameText: String
        get() = if (title.isBlank()) name else "$name · $title"

    companion object {
        val NONE = QuestNpc(0, "", "", 0f, 0f, 0, "", "")
    }
}

/** 前置边。[external] = 根到块的那条，UI 画虚线。 */
data class QuestEdge(val questId: Int, val preId: Int, val external: Boolean)

/** 一个「块」——用户说的按块搜索、以主线为顶的那个单位。 */
data class QuestChainMeta(
    val id: Int,
    val title: String,
    val members: Int,
    val layers: Int,
    /** 最大列数，<= 10（宽层会折行，见 quest_chain.py 的 WRAP）。 */
    val width: Int,
    /** 折行后的总行数，画布高度按这个算。 */
    val rows: Int,
    val minLevel: Int,
    val maxLevel: Int,
    val expansion: Int,
    val rootIds: List<Int>,
    /**
     * 起点任务名。用来区分同名的块 —— 468 个块里 358 个和别的块同名
     * （「尤卡图拉尔支线任务」有 13 个，其中几个连规模都一样）。
     */
    val leadName: String = "",
)

/** 一整棵可画的树：块信息 + 块内节点 + 顶部的入口任务 + 边。 */
data class QuestTree(
    val meta: QuestChainMeta,
    val nodes: List<QuestNode>,
    /** 树顶的入口任务（属于别的块）。 */
    val roots: List<QuestNode>,
    val edges: List<QuestEdge>,
) {
    val byId: Map<Int, QuestNode> by lazy { (nodes + roots).associateBy { it.id } }

    /**
     * 树上显示的名字。同名的补上地点以示区分。
     *
     * 为什么需要：重生之境主线第一层有 **8 个都叫「冒险者入门」**
     * （三个初始城市各自的版本），全是一模一样的框，看不出差别。
     * 补上地点后成为「冒险者入门（格里达尼亚）」等 3 组。
     * 全库 468 个块里只有 12 个块、60 个节点有重名，所以这只是补丁不是常态。
     *
     * 补了地点仍然重名的（同城市的多个版本）就保持原样 ——
     * 再往下就只能显示 ID 了，那对找任务没有帮助。
     */
    val labels: Map<Int, String> by lazy {
        val all = nodes + roots
        val dup = all.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
        all.associate { n ->
            n.id to if (n.name in dup && n.place.isNotBlank()) {
                "${n.name}（${n.place}）"
            } else {
                n.name
            }
        }
    }

    fun labelOf(node: QuestNode): String =
        labels[node.id] ?: node.name.ifBlank { "任务 ${node.id}" }
}

/** 搜索命中的一条。[chainId] 用来打开对应的树。 */
data class QuestHit(
    val id: Int,
    val name: String,
    val level: Int,
    val type: String,
    val iconId: Int,
    val chainId: Int,
    val chainTitle: String,
    val chainMembers: Int,
    /** 任务地点。同名任务靠它区分（搜「冒险者入门」会出 8 条同名的）。 */
    val place: String,
)

/**
 * 给一批命中补上区分用的显示名。同名的加地点，不同名的原样。
 *
 * 和 [QuestTree.labels] 同一个规则，但作用在搜索结果上 ——
 * 搜「冒险者入门」返回 8 条一模一样的行，不加地点没法选。
 */
fun List<QuestHit>.withLabels(): List<Pair<QuestHit, String>> {
    val dup = groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
    return map { h ->
        h to if (h.name in dup && h.place.isNotBlank()) {
            "${h.name}（${h.place}）"
        } else {
            h.name
        }
    }
}

/** 地图底图参数，画接取针要用。 */
data class QuestMap(val mapFile: String, val sizeFactor: Int) {
    /**
     * 游戏坐标 → 图上比例。沿用 FishingScreen 的公式反推：
     *     比例 = (游戏坐标 - 1) * (sizeFactor / 100) / 41
     *
     * 越界的要丢掉不画：实测「时空狭缝」「万魔殿正门」「伊甸内核」这 3 个
     * 副本战斗区共 20 个任务，坐标不指向这张图，算出来能到 2.2。
     * 把针画在图外或强行 clamp 到边角都是错的信息，所以返回 null。
     */
    fun fracOf(gx: Float, gy: Float): Pair<Float, Float>? {
        val s = maxOf(sizeFactor, 100) / 100f
        val fx = (gx - 1f) * s / 41f
        val fy = (gy - 1f) * s / 41f
        return if (fx in 0f..1f && fy in 0f..1f) fx to fy else null
    }
}

object QuestDb {
    /** 搜索结果上限。块搜索一次给不了太多，够翻就行。 */
    private const val SEARCH_LIMIT = 80

    private const val COLS =
        "id, name, name_en, level, type, category, expansion, job_group, " +
            "icon_id, repeatable, cn_only, place, prereq_rel, chain_id, " +
            "col, row, tier, is_root, " +
            "s_npc_id, s_npc, s_title, s_x, s_y, s_map, s_map_name, s_zone, " +
            "e_npc_id, e_npc, e_x, e_y, e_map, e_map_name, e_zone"

    /** quest_chain 的列。三处查询共用，免得加列时漏改其中一处。 */
    private const val CHAIN_COLS =
        "id, title, members, layers, width, rows, " +
            "min_level, max_level, expansion, root_ids, lead_name"

    private fun readChain(c: android.database.Cursor) = QuestChainMeta(
        id = c.getInt(0),
        title = c.getString(1) ?: "",
        members = c.getInt(2),
        layers = c.getInt(3),
        width = c.getInt(4),
        rows = c.getInt(5),
        minLevel = c.getInt(6),
        maxLevel = c.getInt(7),
        expansion = c.getInt(8),
        rootIds = (c.getString(9) ?: "").split(',')
            .mapNotNull { it.trim().toIntOrNull() },
        leadName = c.getString(10) ?: "",
    )

    /**
     * 按名字搜任务。中文名优先，英文名兜底。
     *
     * 返回的是「命中的任务 + 它所在的块」，UI 拿 chainId 去开树。
     */
    suspend fun search(context: Context, query: String): List<QuestHit> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isBlank()) return@withContext emptyList()
            val db = WikiDb.open(context)
            val like = "%$q%"
            val sql = """
                SELECT q.id, q.name, q.level, q.type, q.icon_id,
                       q.chain_id, c.title, c.members, q.place
                FROM quests q JOIN quest_chain c ON c.id = q.chain_id
                WHERE q.name LIKE ? OR q.name_en LIKE ?
                ORDER BY
                  CASE WHEN q.name = ? THEN 0
                       WHEN q.name LIKE ? THEN 1 ELSE 2 END,
                  q.level, q.id
                LIMIT $SEARCH_LIMIT
            """.trimIndent()
            db.rawQuery(sql, arrayOf(like, like, q, "$q%")).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(
                            QuestHit(
                                id = c.getInt(0), name = c.getString(1) ?: "",
                                level = c.getInt(2), type = c.getString(3) ?: "",
                                iconId = c.getInt(4), chainId = c.getInt(5),
                                chainTitle = c.getString(6) ?: "",
                                chainMembers = c.getInt(7),
                                place = c.getString(8) ?: "",
                            )
                        )
                    }
                }
            }
        }

    /**
     * 按块名搜块。
     *
     * 光搜任务名不够：搜「龙诗战争」一条都出不来，因为没有任务**叫**这个，
     * 但「龙诗战争终章主线任务」「龙诗战争尾声主线任务」两个块共 44 个任务
     * 都属于它。这两个块正是用户想找的东西。
     *
     * 所以块名单独搜一次，和任务名的结果并列显示。
     */
    suspend fun searchChains(context: Context, query: String): List<QuestChainMeta> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isBlank()) return@withContext emptyList()
            WikiDb.open(context).rawQuery(
                "SELECT $CHAIN_COLS FROM quest_chain " +
                    "WHERE title LIKE ? OR category LIKE ? " +
                    "ORDER BY expansion, min_level, id LIMIT 40",
                arrayOf("%$q%", "%$q%"),
            ).use { c -> buildList { while (c.moveToNext()) add(readChain(c)) } }
        }

    /** 取一整棵树。5360 个任务里最大的块 255 个，实测 1.9ms。 */
    suspend fun tree(context: Context, chainId: Int): QuestTree? =
        withContext(Dispatchers.IO) {
            val db = WikiDb.open(context)
            val meta = db.rawQuery(
                "SELECT $CHAIN_COLS FROM quest_chain WHERE id = ?",
                arrayOf(chainId.toString()),
            ).use { c ->
                if (!c.moveToNext()) return@withContext null
                readChain(c)
            }

            val nodes = db.rawQuery(
                "SELECT $COLS FROM quests WHERE chain_id = ? ORDER BY row, col",
                arrayOf(chainId.toString()),
            ).use { c -> buildList { while (c.moveToNext()) add(readNode(c)) } }

            val roots = if (meta.rootIds.isEmpty()) emptyList() else {
                val ph = meta.rootIds.joinToString(",") { "?" }
                db.rawQuery(
                    "SELECT $COLS FROM quests WHERE id IN ($ph) ORDER BY level, id",
                    meta.rootIds.map { it.toString() }.toTypedArray(),
                ).use { c -> buildList { while (c.moveToNext()) add(readNode(c)) } }
            }

            // 块内边 + 根指进来的边
            val edges = db.rawQuery(
                "SELECT p.quest_id, p.pre_id, p.external FROM quest_prereq p " +
                    "JOIN quests q ON q.id = p.quest_id WHERE q.chain_id = ?",
                arrayOf(chainId.toString()),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(QuestEdge(c.getInt(0), c.getInt(1), c.getInt(2) == 1))
                    }
                }
            }
            QuestTree(meta, nodes, roots, edges)
        }

    /** 单个任务。从别处（物品来源、跨块跳转）进任务时用。 */
    suspend fun byId(context: Context, id: Int): QuestNode? =
        withContext(Dispatchers.IO) {
            WikiDb.open(context).rawQuery(
                "SELECT $COLS FROM quests WHERE id = ?", arrayOf(id.toString()),
            ).use { c -> if (c.moveToNext()) readNode(c) else null }
        }

    /** 直接后继（「接下来能做什么」）。 */
    suspend fun nextOf(context: Context, id: Int): List<QuestNode> =
        withContext(Dispatchers.IO) {
            WikiDb.open(context).rawQuery(
                "SELECT $COLS FROM quests WHERE id IN " +
                    "(SELECT quest_id FROM quest_prereq WHERE pre_id = ?) " +
                    "ORDER BY level, id LIMIT 40",
                arrayOf(id.toString()),
            ).use { c -> buildList { while (c.moveToNext()) add(readNode(c)) } }
        }

    /** 直接前置。 */
    suspend fun prevOf(context: Context, id: Int): List<QuestNode> =
        withContext(Dispatchers.IO) {
            WikiDb.open(context).rawQuery(
                "SELECT $COLS FROM quests WHERE id IN " +
                    "(SELECT pre_id FROM quest_prereq WHERE quest_id = ?) " +
                    "ORDER BY level, id LIMIT 40",
                arrayOf(id.toString()),
            ).use { c -> buildList { while (c.moveToNext()) add(readNode(c)) } }
        }

    /** 地图底图参数。查不到就没有底图，UI 退化成只显示坐标文字。 */
    suspend fun mapOf(context: Context, placeName: String): QuestMap? =
        withContext(Dispatchers.IO) {
            if (placeName.isBlank()) return@withContext null
            WikiDb.open(context).rawQuery(
                "SELECT map_file, size_factor FROM maps WHERE place_name = ?",
                arrayOf(placeName),
            ).use { c ->
                if (c.moveToNext()) {
                    QuestMap(c.getString(0) ?: "", c.getInt(1).takeIf { it > 0 } ?: 100)
                } else null
            }
        }

    /** 按块列出（不带搜索词时的浏览入口）。 */
    suspend fun chains(context: Context, expansion: Int = -1): List<QuestChainMeta> =
        withContext(Dispatchers.IO) {
            val db = WikiDb.open(context)
            val where = if (expansion >= 0) "WHERE expansion = $expansion" else ""
            db.rawQuery(
                "SELECT $CHAIN_COLS FROM quest_chain $where " +
                    "ORDER BY expansion, min_level, id",
                null,
            ).use { c -> buildList { while (c.moveToNext()) add(readChain(c)) } }
        }

    /** 给 [QuestAncestry] 用：它要自己拼 IN (…) 批量查，需要同一份列清单。 */
    internal fun colsForAncestry(): String = COLS

    /** 同上，让它复用同一个游标读法，避免两处下标各写一遍。 */
    internal fun readNodePublic(c: android.database.Cursor) = readNode(c)

    private fun readNode(c: android.database.Cursor) = QuestNode(
        id = c.getInt(0),
        name = c.getString(1) ?: "",
        nameEn = c.getString(2) ?: "",
        level = c.getInt(3),
        type = c.getString(4) ?: "",
        category = c.getString(5) ?: "",
        expansion = c.getInt(6),
        jobGroup = c.getString(7) ?: "",
        iconId = c.getInt(8),
        repeatable = c.getInt(9) == 1,
        cnOnly = c.getInt(10) == 1,
        place = c.getString(11) ?: "",
        prereqRel = c.getInt(12),
        chainId = c.getInt(13),
        col = c.getInt(14),
        row = c.getInt(15),
        tier = c.getInt(16),
        isRoot = c.getInt(17) == 1,
        startNpc = QuestNpc(
            id = c.getInt(18), name = c.getString(19) ?: "",
            title = c.getString(20) ?: "",
            x = c.getFloat(21), y = c.getFloat(22), mapId = c.getInt(23),
            mapName = c.getString(24) ?: "", zoneName = c.getString(25) ?: "",
        ),
        endNpc = QuestNpc(
            id = c.getInt(26), name = c.getString(27) ?: "",
            title = "",
            x = c.getFloat(28), y = c.getFloat(29), mapId = c.getInt(30),
            mapName = c.getString(31) ?: "", zoneName = c.getString(32) ?: "",
        ),
    )
}
