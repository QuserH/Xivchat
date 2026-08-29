package com.quserh.eorzeaphone.data.wiki

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 一件物品的检索/列表所需字段。
 *
 * 详情页要的"来源/属性"不在库里（占全量 88% 体积），点进详情时按需拉网络。
 * 数据来自最终幻想XIV中文维基，构建脚本见 开发/WIKI/build_item_db.py。
 */
data class WikiItem(
    val id: Int,
    val nameCn: String,
    val nameJp: String,
    val nameEn: String,
    val kindId: Int,
    val categoryId: Int,
    val itemLevel: Int,
    val equipLevel: Int,
    val rarity: Int,
    val version: Double,
    val iconId: Int,
    val dye: Int,
    val stack: Int,
    val rare: Boolean,
    val uniqueItem: Boolean,
    val priceBuy: Int,
    val priceSell: Int,
    val jobs: String,
    /**
     * 灰机图床的文件名哈希，用来兜 xivapi 的缺图。
     * 只有 7.x 物品有值 —— xivapi 落后约两个补丁，老版本它都齐。
     */
    val iconHash: String = "",
    /**
     * 站点口径的「拿不到了」。建库时把 类型ID=39（6.0 移除的腰带）一并标进来。
     *
     * **对染剂要当心**：停产的是物品、颜色还在（新染剂能自由选色），
     * 所以做颜色选择器时不能拿它当过滤条件，只能用来在同色多件里挑代表。
     * 见 `开发/WIKI/GLAMOUR_SLOTS.md`。
     */
    val unobtainable: Boolean = false,
) {
    /** ",3,21," -> [3, 21] */
    val jobIds: List<Int> get() = jobs.trim(',').split(',').mapNotNull(String::toIntOrNull)

    val kindName: String get() = WikiDicts.kindName(kindId)
    val categoryName: String get() = WikiDicts.categoryName(categoryId)
    val rarityName: String get() = WikiDicts.rarityName(rarity)
}

/**
 * 检索条件。0 表示该项不参与过滤。
 *
 * [dye] 例外，用 -1 表示不过滤 —— 0 是"不可染色"这个有效值，不能拿 0 当哨兵。
 * 另注意：库里 dye 存的是**染色槽数**（0/1/2），而站点检索器的 dye 参数是槽数+1，
 * 别照搬站点的值。
 */
data class WikiFilter(
    val query: String = "",
    val kindId: Int = 0,
    val categoryId: Int = 0,
    val rarity: Int = 0,
    val itemLevelMin: Int = 0,
    val itemLevelMax: Int = 0,
    val equipLevelMin: Int = 0,
    val equipLevelMax: Int = 0,
    val dye: Int = -1,
    val jobId: Int = 0,
    /** 版本精确匹配。站点也是精确匹配（7.5 和 7.51 是两回事）。0 = 不限。 */
    val version: Double = 0.0,
    /** 1 = 可获得、2 = 不可获得、0 = 不限。站点用 来源.Unobtainable 的存在性判断。 */
    val obtainable: Int = 0,
    /**
     * true  = 复刻站点窄筛：叠加属性类型条件，只出"这个职业该穿的"。
     * false = 只看能否装备，找幻化时更有用。
     */
    val jobNarrow: Boolean = true,
) {
    val isEmpty: Boolean
        get() = query.isBlank() && kindId == 0 && categoryId == 0 && rarity == 0 &&
            itemLevelMin == 0 && itemLevelMax == 0 && equipLevelMin == 0 &&
            equipLevelMax == 0 && dye < 0 && jobId == 0 && version <= 0 &&
            obtainable == 0
}

/**
 * 一个采集点 / 钓场。
 *
 * [etHours] 空 = 常驻点；非空 = 限时点，在这些 ET 时刻出现，
 * 每次持续 [durationMin] 分钟。[folkloreName] 非空表示要先买传承录。
 */
data class WikiNode(
    val id: Int,
    val kindId: Int,
    /** 站点给的类型串，限时点是 "限时"。 */
    val kindName: String,
    val level: Int,
    val stars: Int,
    val x: Float,
    val y: Float,
    val mapName: String,
    val areaName: String,
    val region: String,
    val etHours: List<Int>,
    val durationMin: Int,
    val folkloreId: Int,
    val folkloreName: String,
) {
    val isTimed: Boolean get() = etHours.isNotEmpty()

    /** "翻云雾海 · 招恶荒岛 (32.5, 31.8)" */
    val placeText: String
        get() = buildList {
            mapName.takeIf { it.isNotBlank() }?.let(::add)
            areaName.takeIf { it.isNotBlank() && it != mapName }?.let(::add)
        }.joinToString(" · ") + if (x > 0 || y > 0) " (${x}, ${y})" else ""

    /** "ET 20:00 起 4 小时" / "ET 4:00、16:00 起 4 小时" */
    val windowText: String
        get() {
            if (!isTimed) return ""
            val hours = etHours.joinToString("、") { "%d:00".format(it) }
            val h = durationMin / 60
            val m = durationMin % 60
            val dur = when {
                h > 0 && m > 0 -> "${h}小时${m}分"
                h > 0 -> "${h}小时"
                else -> "${durationMin}分"
            }
            return "ET $hours 起 $dur"
        }
}

object WikiDb {
    /** 与站点每页条数一致，本地分页手感和网页对齐。 */
    const val PAGE_SIZE = 60

    /** 停止流通道具。站点在 job>0 且未指定 category 时隐含排除它。 */
    private const val CATEGORY_DISCONTINUED = 39

    /**
     * 注意条目名是 `.db` 而不是 `.db.gz`。
     *
     * 仓库里放的是 `items.db.gz`，但 **AGP 会在 mergeAssets 阶段自动把 .gz 解开**，
     * 打进 APK 的条目变成 `assets/wiki/items.db`（APK 自己再 Deflate 压到约 3.9 MB）。
     * 所以运行时要开不带 .gz 的名字，也不需要 GZIPInputStream ——
     * `assets.open()` 拿到的已经是解压后的库。
     *
     * 这一条是真机上撞出来的：写成 `.db.gz` 时首屏报 FileNotFoundException。
     * 同理 `openFd()` 对这个条目也会失败（Deflate 存储的条目不能直接 mmap），
     * 版本戳因此改用 VERSION_CODE。
     */
    private const val ASSET = "wiki/items.db"

    /**
     * 库的构建时间戳，由 build_item_db.py 一起产出。
     *
     * 别拿 VERSION_CODE 当戳：换了库但没改 versionCode 时用户会一直用旧库。
     * 开发期尤其明显 —— 我加了 nodes 表、重建了库，App 却还在读旧的那份，
     * 详情页一直显示"采集点 ID 391"而不是地名，就是这么来的。
     */
    private const val VERSION_ASSET = "wiki/db_version.txt"
    private const val FILE = "wiki-items.db"

    @Volatile private var db: SQLiteDatabase? = null

    /**
     * 首次调用把 assets 里的库拷到 filesDir 再打开（约 13.5 MiB）。
     * 拷过就直接开；assets 里的库换了（构建时间戳变了）就重拷一次。
     */
    suspend fun open(context: Context): SQLiteDatabase = db ?: withContext(Dispatchers.IO) {
        db ?: synchronized(this) {
            db ?: run {
                val target = File(context.filesDir, FILE)
                val stamp = File(context.filesDir, "$FILE.stamp")
                val want = assetVersion(context)
                if (!target.exists() || runCatching { stamp.readText() }.getOrNull() != want) {
                    context.assets.open(ASSET).use { input ->
                        target.outputStream().use { out -> input.copyTo(out) }
                    }
                    runCatching { stamp.writeText(want) }
                }
                SQLiteDatabase.openDatabase(
                    target.absolutePath, null, SQLiteDatabase.OPEN_READONLY,
                ).also { db = it }
            }
        }
    }

    /** 读 assets 里那几十字节的构建时间戳。读不到就退回 VERSION_CODE。 */
    private fun assetVersion(context: Context): String = runCatching {
        context.assets.open(VERSION_ASSET).use { it.readBytes().decodeToString().trim() }
    }.getOrElse { com.quserh.eorzeaphone.BuildConfig.VERSION_CODE.toString() }

    // ---- 动态 WHERE ----
    // 不用 `? = 0 OR col = ?` 那种哨兵写法：rawQuery 的 selectionArgs 是
    // Array<String>，绑进去是 TEXT，而 SQLite 比较 TEXT '0' 与 INTEGER 0 为假
    // （两边都没有列亲和性可套），结果每个过滤器都会筛空。
    // 所以条件按需拼，参数只出现在真正参与比较的位置。顺带也快一点。
    private fun buildWhere(f: WikiFilter): Pair<String, MutableList<String>> {
        val cond = mutableListOf<String>()
        val args = mutableListOf<String>()

        val q = f.query.trim()
        if (q.isNotEmpty()) {
            cond += "(name_cn LIKE ? OR name_jp LIKE ? OR name_en_lower LIKE ?)"
            val like = "%$q%"
            args += like
            args += like
            args += "%${q.lowercase()}%"
        }
        if (f.kindId > 0) { cond += "kind_id = ?"; args += f.kindId.toString() }
        if (f.categoryId > 0) { cond += "category_id = ?"; args += f.categoryId.toString() }
        if (f.rarity > 0) { cond += "rarity = ?"; args += f.rarity.toString() }
        if (f.itemLevelMin > 0) { cond += "item_level >= ?"; args += f.itemLevelMin.toString() }
        if (f.itemLevelMax > 0) { cond += "item_level <= ?"; args += f.itemLevelMax.toString() }
        if (f.equipLevelMin > 0) { cond += "equip_level >= ?"; args += f.equipLevelMin.toString() }
        if (f.equipLevelMax > 0) { cond += "equip_level <= ?"; args += f.equipLevelMax.toString() }
        if (f.dye >= 0) { cond += "dye = ?"; args += f.dye.toString() }
        // 版本精确匹配。REAL 列绑字符串会走类型亲和性转换，这里能对上；
        // 但为稳妥用 CAST，别指望隐式转换。
        if (f.version > 0) {
            cond += "version = CAST(? AS REAL)"
            args += f.version.toString()
        }
        // 站点口径：1 可获得 = 无 Unobtainable 标记且 类型ID != 39；
        // 2 不可获得 = 有标记 或 类型ID = 39。建库时已把 39 一并标进 unobtainable。
        if (f.obtainable == 1) cond += "unobtainable = 0"
        if (f.obtainable == 2) cond += "unobtainable = 1"
        if (f.jobId > 0) {
            // 前后带逗号，避免 21 命中 121
            cond += "jobs LIKE ?"
            args += "%,${f.jobId},%"
            if (f.jobNarrow) {
                cond += "param_type IN (SELECT param_type FROM job_params WHERE job_id = ?)"
                args += f.jobId.toString()
            }
            // 站点的隐含条件：选了职业又没指定细类时排除停止流通道具。
            // 少这一条，骑士窄筛会多出 242 件（实测 4487 vs 站点 4245）。
            if (f.categoryId == 0) cond += "category_id <> $CATEGORY_DISCONTINUED"
        }
        val where = if (cond.isEmpty()) "" else "WHERE " + cond.joinToString(" AND ")
        return where to args
    }

    private const val COLS =
        "id, name_cn, name_jp, name_en, kind_id, category_id, item_level, " +
            "equip_level, rarity, version, icon_id, dye, stack, rare, unique_item, " +
            "price_buy, price_sell, jobs, icon_hash, unobtainable"

    /** 对齐站点 Module:Item/ItemSearch 的排序：品级↓ 版本↓ ID↑ */
    private const val ORDER = "ORDER BY item_level DESC, version DESC, id ASC"

    suspend fun count(context: Context, f: WikiFilter): Int = withContext(Dispatchers.IO) {
        val (where, args) = buildWhere(f)
        open(context).rawQuery("SELECT COUNT(*) FROM items $where", args.toTypedArray())
            .use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    suspend fun search(
        context: Context,
        f: WikiFilter,
        page: Int = 0,
        pageSize: Int = PAGE_SIZE,
    ): List<WikiItem> = withContext(Dispatchers.IO) {
        val (where, args) = buildWhere(f)
        args += pageSize.toString()
        args += (page * pageSize).toString()
        open(context)
            .rawQuery("SELECT $COLS FROM items $where $ORDER LIMIT ? OFFSET ?", args.toTypedArray())
            .use { c -> buildList(c.count) { while (c.moveToNext()) add(readRow(c)) } }
    }

    suspend fun byId(context: Context, id: Int): WikiItem? = withContext(Dispatchers.IO) {
        open(context)
            .rawQuery("SELECT $COLS FROM items WHERE id = ?", arrayOf(id.toString()))
            .use { if (it.moveToFirst()) readRow(it) else null }
    }

    private fun readRow(c: Cursor) = WikiItem(
        id = c.getInt(0),
        nameCn = c.getString(1) ?: "",
        nameJp = c.getString(2) ?: "",
        nameEn = c.getString(3) ?: "",
        kindId = c.getInt(4),
        categoryId = c.getInt(5),
        itemLevel = c.getInt(6),
        equipLevel = c.getInt(7),
        rarity = c.getInt(8),
        version = c.getDouble(9),
        iconId = c.getInt(10),
        dye = c.getInt(11),
        stack = c.getInt(12),
        rare = c.getInt(13) != 0,
        uniqueItem = c.getInt(14) != 0,
        priceBuy = c.getInt(15),
        priceSell = c.getInt(16),
        jobs = c.getString(17) ?: "",
        iconHash = c.getString(18) ?: "",
        unobtainable = c.getInt(19) != 0,
    )

    /** meta 表：built_at / item_count / last_sync / data_version */
    suspend fun meta(context: Context): Map<String, String> = withContext(Dispatchers.IO) {
        open(context).rawQuery("SELECT k, v FROM meta", null).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getString(1)) }
        }
    }

    /**
     * 采集点 / 钓场。物品 JSON 里只有裸 ID（"采集点: [391]"），
     * 靠这张表翻成"翻云雾海 · 招恶荒岛 (32.5, 31.8)"。
     *
     * 642 个节点只占 0.18 MiB，所以随包内置而不是联网查。
     * [etHours] / [durationMin] 是限时采集点的 ET 窗口，采集时钟会用到。
     */
    suspend fun node(context: Context, id: Int): WikiNode? = withContext(Dispatchers.IO) {
        open(context).rawQuery(
            "SELECT id, kind_id, kind_name, level, stars, x, y, map_name, " +
                "area_name, region, et_hours, duration, folklore, folklore_name " +
                "FROM nodes WHERE id = ?",
            arrayOf(id.toString()),
        ).use { c ->
            if (!c.moveToFirst()) return@use null
            WikiNode(
                id = c.getInt(0),
                kindId = c.getInt(1),
                kindName = c.getString(2) ?: "",
                level = c.getInt(3),
                stars = c.getInt(4),
                x = c.getFloat(5),
                y = c.getFloat(6),
                mapName = c.getString(7) ?: "",
                areaName = c.getString(8) ?: "",
                region = c.getString(9) ?: "",
                etHours = (c.getString(10) ?: "").trim(',').split(',')
                    .mapNotNull(String::toIntOrNull),
                durationMin = c.getInt(11),
                folkloreId = c.getInt(12),
                folkloreName = c.getString(13) ?: "",
            )
        }
    }

    /** 批量版，详情页一次可能要翻好几个采集点。 */
    suspend fun nodes(context: Context, ids: List<Int>): Map<Int, WikiNode> =
        withContext(Dispatchers.IO) {
            if (ids.isEmpty()) return@withContext emptyMap()
            buildMap { for (i in ids.distinct()) node(context, i)?.let { put(i, it) } }
        }

    /** 某主类型下有哪些细类（带计数），用来填"物品类型"下拉。kindId=0 表示全部。 */
    suspend fun categoriesFor(context: Context, kindId: Int): List<Pair<Int, Int>> =
        withContext(Dispatchers.IO) {
            val sql = if (kindId > 0) {
                "SELECT category_id, COUNT(*) FROM items WHERE kind_id = ? " +
                    "GROUP BY category_id ORDER BY COUNT(*) DESC"
            } else {
                "SELECT category_id, COUNT(*) FROM items " +
                    "GROUP BY category_id ORDER BY COUNT(*) DESC"
            }
            val args = if (kindId > 0) arrayOf(kindId.toString()) else null
            open(context).rawQuery(sql, args).use { c ->
                buildList { while (c.moveToNext()) add(c.getInt(0) to c.getInt(1)) }
            }
        }
}
