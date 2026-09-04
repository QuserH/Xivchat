package com.quserh.eorzeaphone.data.wiki

import android.content.Context
import com.quserh.eorzeaphone.data.CacheMaintenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 「如何获得」等详情字段的按需拉取。
 *
 * 本地库只存搜索用的字段（12.9 MiB）。`来源` 归一化后还要 +4 MiB，
 * 而且会随版本过期，所以点进详情时实时拉 —— 实测单件 **220-260 ms / 1.6-3.2 KB**。
 *
 * 拉到的结果落磁盘缓存（`cacheDir/wiki-detail`），再看同一件就不走网络。
 *
 * 网络层沿用项目里 [com.quserh.eorzeaphone.data.shizhijia.ShizhijiaApi] 的写法：
 * 裸 HttpURLConnection，不引入 HTTP 库。
 */

/** 一条来源可以跳到哪儿去。空 = 这条不可点。 */
enum class WikiLinkKind { NONE, ITEM, QUEST, INSTANCE, SHOP, NODE }

/** 一条获取途径。[kind] 是"商店"/"副本"/"采集"这类，[detail] 已拼成可直接显示的文本。 */
data class WikiSourceEntry(
    val kind: String,
    val name: String,
    val detail: String,
    /** 关联的 ID，0 表示没有。 */
    val refId: Int = 0,
    /**
     * 这条能跳到什么。像 wiki 一样互相点击靠它 ——
     * 商店→Shop 页、副本→Instance 页、任务→Quest 页、物品箱/寻宝→Item 页、
     * 采集点/钓鱼→本地 nodes 表。
     */
    val linkKind: WikiLinkKind = WikiLinkKind.NONE,
    /** 跳转用的 ID。商店用商店ID（不是 NPC 的 ID），其余同 [refId]。 */
    val linkId: Int = 0,
)

data class WikiDetail(
    val id: Int,
    /** 游戏内描述文本。可能为空。 */
    val description: String,
    val sources: List<WikiSourceEntry>,
    /** 装备属性，如 "力量 87"。非装备为空。 */
    val stats: List<Pair<String, String>>,
    /** 灰机图床的文件名哈希，用来兜 xivapi 的缺图。 */
    val iconHash: String,
    /** true = 站点标了 Unobtainable。 */
    val unobtainable: Boolean,
)

/**
 * 任务详情。站点 `Data:Quest/<id>.json`。
 */
data class WikiQuest(
    val id: Int,
    val name: String,
    val level: Int,
    val category: String,
    val jobGroup: String,
    val expansion: String,
    val startNpc: String,
    val endNpc: String,
    val place: String,
    val repeatable: Boolean,
    val prevQuests: List<Pair<Int, String>>,
    /** 任务奖励里的物品，可继续跳转 */
    val rewardItems: List<Pair<Int, String>>,
    val relatedItems: List<Pair<Int, String>>,
)

/**
 * 副本详情。站点 `Data:Instance/<id>.json`。
 */
data class WikiInstance(
    val id: Int,
    val name: String,
    val type: String,
    val levelMin: Int,
    val levelMax: Int,
    val ilvlMin: Int,
    val timeLimit: Int,
    val place: String,
    val partyTank: Int,
    val partyHealer: Int,
    val partyMelee: Int,
    val partyRanged: Int,
    val description: String,
    /** BOSS 名，纯展示 */
    val bosses: List<String>,
)

/**
 * 一个普通 wiki 条目页的正文（怪物 / 地名 / NPC / 攻略那些本地没有表的）。
 *
 * ## 为什么要这个
 *
 * 用户的诉求是「所有条目都能在 App 里看，别跳网页」。物品/任务/副本都有
 * 本地表了，但怪物、地名、NPC、攻略页没有 —— 那些以前是 [WikiHit.Page]，
 * UI 上直接 `Intent.ACTION_VIEW` 踢到浏览器。
 *
 * 站点装了 TextExtracts 扩展，`prop=extracts&explaintext=1` 能拿到纯文本正文
 * （实测：伊弗利特 959 字、弗栗多 3226 字、萨维奈岛 4349 字）。
 * 比 `action=parse&prop=text` 的 149 KB HTML 现实得多 —— 那个在手机上没法渲染。
 *
 * ## 两个实测限制
 *
 * 1. **整篇正文一次只能取 1 个标题**（`exlimit` 会被静默降到 1 并给 warning）。
 *    所以这里没有批量版；详情页一次一个正好。
 * 2. **表格会被丢掉**。纯表格的攻略页（如「坐骑获取方式」）正文是**空的**。
 *    [isThin] 就是为这种页面留的 —— UI 得说明「这页主要是表格」并给出
 *    浏览器入口，而不是显示一片空白装作加载成功。
 *
 * 重定向自动跟随，所以黑话能用：`AF` → 校服（1819 字）、
 * `A12` → 亚历山大机神城 天动之章4。[redirectedFrom] 记下是从哪个词跳来的。
 */
data class WikiPage(
    /** 重定向跟随之后的真实标题。 */
    val title: String,
    /** 纯文本正文，小节标题保留成 `== 档案 ==` 形式，交给 UI 解析。 */
    val extract: String,
    /** 站点分类，已去掉 `Category:`/`分类:` 前缀。用来标类型（BOSS/NPC/地理）。 */
    val categories: List<String>,
    /** 缩略图 URL，没有就是空串。 */
    val thumbUrl: String,
    /** 非空表示是从这个词重定向过来的（`AF` → 校服）。 */
    val redirectedFrom: String?,
) {
    /**
     * 正文太少，说明这页的内容主要在表格/模板里，extracts 取不到。
     *
     * 阈值 40 字是按实测定的：真有内容的页面最少也有 154 字（版本:7.0），
     * 而「A12」跳到的副本页只有 11 字、「坐骑获取方式」是 0 字。
     */
    val isThin: Boolean get() = extract.length < 40

    /** 类型标签，取第一个分类。没有分类就叫「条目」。 */
    val kindLabel: String get() = categories.firstOrNull() ?: "条目"
}

/**
 * 商店详情。站点 `Data:Shop/<id>.json`。
 */
data class WikiShop(
    val id: Int,
    val name: String,
    val condition: String,
    val npcNames: List<String>,
    /** 商品：物品 ID + 名 + 价格描述，可继续跳转 */
    val goods: List<WikiShopGood>,
)

data class WikiShopGood(
    val itemId: Int,
    val name: String,
    val iconId: Int,
    val costText: String,
)

object WikiRemote {
    // gadget 实际用的 CDN，对非浏览器客户端比主站宽容
    private const val BASE = "https://cdn.huijiwiki.com/ff14/api.php"
    private const val TIMEOUT_MS = 15_000
    private const val CACHE_DIR = "wiki-detail"

    // 带 App 名和联系方式，出问题时站方能找到我们。
    // 实测 Dalvik/okhttp 默认 UA 也能过，但 Java/* 与空 UA 会被 CF 403。
    private const val UA = "EorzeaPhone/0.7.225 (FF14 item lookup; +https://ff14.huijiwiki.com/)"

    /** 站点隐含的"已停止获取"标记键。 */
    private const val KEY_UNOBTAINABLE = "Unobtainable"

    /**
     * 取一件物品的详情。优先磁盘缓存；[maxAgeMs] 之内的缓存直接用。
     * 失败返回 null（离线时详情页应退回只显示本地字段）。
     */
    suspend fun detail(
        context: Context,
        id: Int,
        maxAgeMs: Long = 7L * 24 * 3600 * 1000,
    ): WikiDetail? = withContext(Dispatchers.IO) {
        cached(context, id, maxAgeMs)?.let { return@withContext runCatching { parse(id, it) }.getOrNull() }
        val raw = fetch(id) ?: return@withContext null
        runCatching { writeCache(context, id, raw) }
        CacheMaintenance.schedule(context)
        runCatching { parse(id, JSONObject(raw)) }.getOrNull()
    }

    private fun cacheFile(context: Context, id: Int) =
        File(File(context.cacheDir, CACHE_DIR).apply { mkdirs() }, "$id.json")

    private fun cached(context: Context, id: Int, maxAgeMs: Long): JSONObject? {
        val f = cacheFile(context, id)
        if (!f.exists()) return null
        if (System.currentTimeMillis() - f.lastModified() > maxAgeMs) return null
        return runCatching { JSONObject(f.readText()) }.getOrNull()
    }

    private fun writeCache(context: Context, id: Int, raw: String) {
        val target = cacheFile(context, id)
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(raw)
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) tmp.delete()
    }

    /**
     * `action=query&prop=revisions&rvslots=main` 取 `Data:Item/<id>.json` 的原文。
     *
     * 不用 `action=raw` —— 实测只有约 60% 成功率（会被 CF 间歇拦截），
     * 而 api.php 是 20/20。
     */
    private fun fetch(id: Int): String? = fetchPage("Data:Item/$id.json")

    /** 取任意数据页的原文。物品/任务/副本/商店都走这条。 */
    private fun fetchPage(pageTitle: String): String? {
        val title = URLEncoder.encode(pageTitle, "UTF-8")
        val url = "$BASE?action=query&prop=revisions&rvprop=content&rvslots=main" +
            "&titles=$title&format=json&formatversion=2&smaxage=1000&maxage=1000"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Encoding", "gzip")
            }
            // 403 是概率性限流不是永久失败，但详情页不该卡着重试 ——
            // 直接返回 null 让 UI 显示"点击重试"，由用户决定。
            if (conn.responseCode !in 200..299) return null
            val stream = if (conn.contentEncoding?.contains("gzip", true) == true) {
                java.util.zip.GZIPInputStream(conn.inputStream)
            } else {
                conn.inputStream
            }
            val body = stream.bufferedReader().use { it.readText() }
            val page = JSONObject(body)
                .optJSONObject("query")
                ?.optJSONArray("pages")
                ?.optJSONObject(0)
                ?: return null
            if (page.optBoolean("missing")) return null
            page.optJSONArray("revisions")
                ?.optJSONObject(0)
                ?.optJSONObject("slots")
                ?.optJSONObject("main")
                ?.optString("content")
                ?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    // ---- 可跳转目标的按需拉取 ----
    // 都走 fetchPage + 磁盘缓存，缓存目录按类型分开。任务/副本/商店基本不变，
    // 缓存放 30 天（物品详情是 7 天，因为版本更新会改来源）。

    suspend fun quest(context: Context, id: Int): WikiQuest? =
        page(context, "Quest", id)?.let { j ->
            runCatching {
                WikiQuest(
                    id = id,
                    name = j.optString("中文名"),
                    level = j.optInt("等级"),
                    category = j.optString("类型"),
                    jobGroup = j.optString("职业组"),
                    expansion = j.optString("资料片"),
                    startNpc = j.optJSONObject("开始NPC")?.optString("中文名").orEmpty(),
                    endNpc = j.optJSONObject("结束NPC")?.optString("中文名").orEmpty(),
                    place = j.optString("任务地点"),
                    repeatable = j.optBoolean("可重复"),
                    prevQuests = idNamePairs(j.optJSONArray("前置任务")),
                    rewardItems = questRewardItems(j.optJSONObject("任务奖励")),
                    relatedItems = bareIdList(j.optJSONArray("相关物品")),
                )
            }.getOrNull()
        }

    suspend fun instance(context: Context, id: Int): WikiInstance? =
        page(context, "Instance", id)?.let { j ->
            runCatching {
                WikiInstance(
                    id = id,
                    name = j.optString("中文名"),
                    type = j.optString("类型"),
                    levelMin = j.optInt("最低等级"),
                    levelMax = j.optInt("最高等级"),
                    ilvlMin = j.optInt("最低装备品级"),
                    timeLimit = j.optInt("时间"),
                    place = j.optString("地点"),
                    partyTank = j.optInt("防护"),
                    partyHealer = j.optInt("治疗"),
                    partyMelee = j.optInt("近战"),
                    partyRanged = j.optInt("远程"),
                    description = j.optString("描述"),
                    bosses = emptyList(),
                )
            }.getOrNull()
        }

    suspend fun shop(context: Context, id: Int): WikiShop? =
        page(context, "Shop", id)?.let { j ->
            runCatching {
                val goods = mutableListOf<WikiShopGood>()
                j.optJSONArray("商品列表")?.let { arr ->
                    for (i in 0 until minOf(arr.length(), 200)) {
                        val g = arr.optJSONObject(i) ?: continue
                        val gid = g.optInt("ID")
                        if (gid > 0) {
                            goods += WikiShopGood(gid, g.optString("中文名"),
                                g.optInt("图标ID"), "")
                        }
                    }
                }
                WikiShop(
                    id = id,
                    name = j.optString("商店名称"),
                    condition = j.optJSONArray("商店条件")?.let { a ->
                        (0 until a.length()).joinToString("、") { a.optString(it) }
                    }.orEmpty(),
                    npcNames = emptyList(),
                    goods = goods,
                )
            }.getOrNull()
        }

    /** `[{ID, 中文名}, …]` → `[(id, name)]` */
    private fun idNamePairs(arr: JSONArray?): List<Pair<Int, String>> {
        if (arr == null) return emptyList()
        return (0 until minOf(arr.length(), 40)).mapNotNull { i ->
            arr.optJSONObject(i)?.let { it.optInt("ID") to it.optString("中文名") }
                ?.takeIf { it.first > 0 }
        }
    }

    /** `[5364, …]` 这种裸 ID 数组。名字留空，由 UI 用本地库补上。 */
    private fun bareIdList(arr: JSONArray?): List<Pair<Int, String>> {
        if (arr == null) return emptyList()
        return (0 until minOf(arr.length(), 40)).mapNotNull { i ->
            arr.optInt(i, 0).takeIf { it > 0 }?.let { it to "" }
        }
    }

    /** 任务奖励里的物品。水晶/道具等混在一个对象的多个子数组里。 */
    private fun questRewardItems(o: JSONObject?): List<Pair<Int, String>> {
        if (o == null) return emptyList()
        val out = mutableListOf<Pair<Int, String>>()
        val keys = o.keys()
        while (keys.hasNext()) {
            when (val v = o.opt(keys.next())) {
                is JSONArray -> for (i in 0 until minOf(v.length(), 20)) {
                    val e = v.optJSONObject(i) ?: continue
                    val id = e.optInt("ID")
                    if (id > 0) out += id to e.optString("中文名")
                }
                is JSONObject -> {
                    val id = v.optInt("ID")
                    if (id > 0) out += id to v.optString("中文名")
                }
            }
        }
        return out.distinctBy { it.first }
    }

    /**
     * 取一个普通条目页的正文，给「在 App 内看 wiki 条目」用。见 [WikiPage]。
     *
     * 一次请求把要显示的东西全要齐：正文 + 分类 + 缩略图，并跟随重定向。
     * 缓存 30 天 —— 这些页面（怪物档案、地名介绍）基本不变。
     *
     * 缓存键用标题的 MD5：标题里有 `/`、`:`、空格（「临危受命任务一览/萨维奈岛」
     * 「版本:7.0」），直接当文件名会炸。
     */
    suspend fun wikiPage(
        context: Context,
        title: String,
        maxAgeMs: Long = 30L * 24 * 3600 * 1000,
    ): WikiPage? = withContext(Dispatchers.IO) {
        val key = md5(title)
        val dir = File(File(context.cacheDir, CACHE_DIR), "page").apply { mkdirs() }
        val f = File(dir, "$key.json")
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < maxAgeMs) {
            runCatching { parseWikiPage(JSONObject(f.readText()), title) }
                .getOrNull()?.let { return@withContext it }
        }
        val raw = fetchExtract(title) ?: return@withContext null
        val parsed = runCatching { parseWikiPage(JSONObject(raw), title) }.getOrNull()
            ?: return@withContext null
            runCatching {
                val tmp = File(f.parentFile, f.name + ".tmp")
                tmp.writeText(raw)
                if (f.exists()) f.delete()
                if (!tmp.renameTo(f)) tmp.delete()
            }
            CacheMaintenance.schedule(context)
        parsed
    }

    /**
     * `prop=extracts|categories|pageimages` 一次要齐。
     *
     * `exsectionformat=wiki` 让小节标题留成 `== 档案 ==`，UI 靠它分节渲染；
     * `plain` 会把标题混成普通行，认不出来。
     *
     * 标题必须走 POST body（`--data-urlencode` 的等价物）—— 中文标题拼在
     * query string 里实测回 400。这里用 GET + URLEncoder 是可以的，因为
     * `URLEncoder` 会正确百分号编码；400 是我在命令行上没编码才出的。
     */
    private fun fetchExtract(title: String): String? {
        val t = URLEncoder.encode(title, "UTF-8")
        val url = "$BASE?action=query&format=json&formatversion=2" +
            "&prop=extracts%7Ccategories%7Cpageimages" +
            "&explaintext=1&exsectionformat=wiki&redirects=1" +
            "&cllimit=20&piprop=thumbnail&pithumbsize=320" +
            "&titles=$t"
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Encoding", "gzip")
            }
            if (conn.responseCode !in 200..299) return null
            val stream = if (conn.contentEncoding?.contains("gzip", true) == true) {
                java.util.zip.GZIPInputStream(conn.inputStream)
            } else {
                conn.inputStream
            }
            stream.bufferedReader().use { it.readText() }.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    private fun parseWikiPage(root: JSONObject, asked: String): WikiPage? {
        val query = root.optJSONObject("query") ?: return null
        val page = query.optJSONArray("pages")?.optJSONObject(0) ?: return null
        if (page.optBoolean("missing")) return null

        // 「AF」→「校服」：记下是从哪个词跳来的，UI 上说明理由
        var from: String? = null
        query.optJSONArray("redirects")?.let { arr ->
            for (i in 0 until arr.length()) {
                val r = arr.optJSONObject(i) ?: continue
                if (r.optString("from") == asked) from = asked
            }
        }

        val cats = buildList {
            page.optJSONArray("categories")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val c = arr.optJSONObject(i)?.optString("title") ?: continue
                    val name = c.removePrefix("Category:").removePrefix("分类:")
                    if (name.isNotBlank()) add(name)
                }
            }
        }
        return WikiPage(
            title = page.optString("title").ifBlank { asked },
            extract = page.optString("extract").orEmpty().trim(),
            categories = cats,
            thumbUrl = page.optJSONObject("thumbnail")?.optString("source").orEmpty(),
            redirectedFrom = from,
        )
    }

    /** 通用数据页拉取 + 缓存。[kind] 是 `Data:` 后面的前缀。 */
    private suspend fun page(
        context: Context,
        kind: String,
        id: Int,
        maxAgeMs: Long = 30L * 24 * 3600 * 1000,
    ): JSONObject? = withContext(Dispatchers.IO) {
        val dir = File(File(context.cacheDir, CACHE_DIR), kind).apply { mkdirs() }
        val f = File(dir, "$id.json")
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < maxAgeMs) {
            runCatching { JSONObject(f.readText()) }.getOrNull()?.let {
                return@withContext it
            }
        }
        val raw = fetchPage("Data:$kind/$id.json") ?: return@withContext null
        runCatching {
            val tmp = File(f.parentFile, f.name + ".tmp")
            tmp.writeText(raw)
            if (f.exists()) f.delete()
            if (!tmp.renameTo(f)) tmp.delete()
        }
        CacheMaintenance.schedule(context)
        runCatching { JSONObject(raw) }.getOrNull()
    }

    // ---- 解析 ----

    private fun parse(id: Int, j: JSONObject): WikiDetail {
        val src = j.optJSONObject("来源")
        return WikiDetail(
            id = id,
            description = j.optString("描述").orEmpty(),
            sources = if (src == null) emptyList() else flattenSources(src),
            stats = parseStats(j.optJSONObject("属性")),
            iconHash = j.optString("图标").orEmpty(),
            unobtainable = src?.has(KEY_UNOBTAINABLE) == true,
        )
    }

    /**
     * `来源` 的形状每种途径都不一样（实测 20+ 种键）：
     *   商店/特殊商店 -> [{中文名, 位置:{MapName,X,Y}, ...}]
     *   副本         -> [{中文名, LevelMin, LevelMax, ...}]
     *   采集         -> [{职业, 等级, 星级}]
     *   采集点/钓鱼   -> [391]            纯 ID 数组
     *   任务         -> {中文名, 等级}     单个对象而非数组
     *   Unobtainable -> "1.0版本遗留装备"  纯字符串
     * 所以这里逐类型摊平成统一的可显示条目，而不是硬套一个 schema。
     */
    private fun flattenSources(src: JSONObject): List<WikiSourceEntry> {
        val out = mutableListOf<WikiSourceEntry>()
        val keys = src.keys()
        while (keys.hasNext()) {
            val kind = keys.next()
            if (kind == KEY_UNOBTAINABLE) {
                out += WikiSourceEntry(kind = "已停止获取", name = "",
                    detail = src.optString(kind).orEmpty())
                continue
            }
            when (val v = src.opt(kind)) {
                is JSONArray -> {
                    for (i in 0 until minOf(v.length(), 12)) {
                        entry(kind, v.opt(i))?.let(out::add)
                    }
                    if (v.length() > 12) {
                        out += WikiSourceEntry(kind, "", "…等 ${v.length()} 处")
                    }
                }
                is JSONObject -> entry(kind, v)?.let(out::add)
                is String -> out += WikiSourceEntry(kind, "", v)
                is Int -> out += WikiSourceEntry(kind, "", "ID $v")
                else -> Unit
            }
        }
        // 常见途径排前面，冷门的靠后
        val order = listOf("制作", "采集", "钓鱼", "栽培", "商店", "特殊商店",
                           "副本", "任务", "物品箱", "寻宝", "雇员探险")
        return out.sortedBy { e ->
            order.indexOf(e.kind).let { if (it < 0) order.size else it }
        }
    }

    private fun entry(kind: String, any: Any?): WikiSourceEntry? = when (any) {
        // 裸 ID。只有「采集点」指向本地 nodes 表。
        //
        // 「钓鱼」的 ID 是**钓场**编号，不在 nodes 表里（实测 12 个钓鱼 ID
        // 只有 6 个能在 nodes 里查到，剩下的是死链）。钓场数据在 App 自己的
        // assets/fishing/catalog.json 里，是另一套 ID 空间 —— 要接得单独做映射，
        // 所以这里先不给链接，宁可不可点也别给死链。
        is Int -> WikiSourceEntry(
            kind = kind, name = "", detail = "ID $any", refId = any,
            linkKind = if (kind == "采集点") WikiLinkKind.NODE else WikiLinkKind.NONE,
            linkId = any,
        )
        is JSONObject -> {
            val name = any.optString("中文名").ifBlank { any.optString("名称") }
            val bits = mutableListOf<String>()
            any.optJSONObject("位置")?.optString("MapName")
                ?.takeIf { it.isNotBlank() }?.let(bits::add)
            any.optString("职业").takeIf { it.isNotBlank() }?.let(bits::add)
            any.optInt("等级", 0).takeIf { it > 0 }?.let { bits += "Lv$it" }
            any.optInt("星级", 0).takeIf { it > 0 }?.let { bits += "★".repeat(it) }
            any.optString("称号").takeIf { it.isNotBlank() }?.let(bits::add)
            val (lk, lid) = linkTargetOf(kind, any)
            WikiSourceEntry(
                kind = kind,
                name = name,
                detail = bits.joinToString(" · "),
                refId = any.optInt("ID", 0),
                linkKind = lk,
                linkId = lid,
            )
        }
        else -> null
    }

    /**
     * 一条来源该跳到哪儿。
     *
     * 关键点：**商店类的 `ID` 是 NPC 的 ID，站点没有 NPC 数据页**
     * （`Data:ENpc` 那一族实测不存在），能跳的是 `商店ID` → `Data:Shop/<id>.json`。
     * 注意注释里别写 `Data:ENpc` 后面跟星号 —— Kotlin 的块注释可嵌套，
     * 那个组合会开一个永不闭合的内层注释，整个文件从此报"Unclosed comment"。
     * 副本和任务的 `ID` 直接对应各自的数据页。
     * 物品箱/寻宝/道具分解这些，`ID` 本身就是物品 ID。
     */
    private fun linkTargetOf(kind: String, o: JSONObject): Pair<WikiLinkKind, Int> {
        val id = o.optInt("ID", 0)
        return when (kind) {
            "商店", "特殊商店", "军票商店", "道具商城" -> {
                val shopId = o.optInt("商店ID", 0)
                if (shopId > 0) WikiLinkKind.SHOP to shopId
                else WikiLinkKind.NONE to 0
            }
            "副本", "特殊场景探索" ->
                if (id > 0) WikiLinkKind.INSTANCE to id else WikiLinkKind.NONE to 0
            "任务" ->
                if (id > 0) WikiLinkKind.QUEST to id else WikiLinkKind.NONE to 0
            // 这些的 ID 就是物品 ID
            "物品箱", "寻宝", "道具分解", "鉴定", "宝物库", "种子", "栽培" ->
                if (id > 0) WikiLinkKind.ITEM to id else WikiLinkKind.NONE to 0
            else -> WikiLinkKind.NONE to 0
        }
    }

    /** `属性.属性` 按 `属性顺序` 展开，保持站点的显示顺序。 */
    private fun parseStats(attr: JSONObject?): List<Pair<String, String>> {
        if (attr == null) return emptyList()
        val out = mutableListOf<Pair<String, String>>()

        fun section(valuesKey: String, orderKey: String) {
            val values = attr.optJSONObject(valuesKey) ?: return
            val order = attr.optJSONArray(orderKey)
            val names = if (order != null) {
                (0 until order.length()).mapNotNull { order.optString(it).takeIf(String::isNotBlank) }
            } else {
                values.keys().asSequence().toList()
            }
            for (n in names) {
                // opt() 返回 Any?，直接 .toString() 会把 null 变成字面量 "null"
                val v = values.opt(n) ?: continue
                out += n to v.toString()
            }
        }

        section("基本性能", "基本性能顺序")
        section("属性", "属性顺序")
        return out
    }

    /**
     * 条目正文的缓存文件名。
     *
     * 不能拿标题直接当文件名：「临危受命任务一览/萨维奈岛」里有 `/`、
     * 「版本:7.0」里有 `:`，都是 Android 文件名的非法字符。
     */
    private fun md5(s: String): String =
        java.security.MessageDigest.getInstance("MD5")
            .digest(s.toByteArray()).joinToString("") { "%02x".format(it) }

    /**
     * 灰机图床 URL。**必须先把文件名首字母大写再算 MD5** ——
     * 站点 `case: first-letter`，`dbe7b….png` 实际存成 `Dbe7b….png`，
     * 不大写算出来的目录是 `8/8a`（404），大写后才是 `5/52`（200）。
     */
    fun huijiIconUrl(iconHash: String, size: Int = 80): String {
        if (iconHash.isBlank()) return ""
        val fn = (iconHash + ".png").let { it.first().uppercaseChar() + it.substring(1) }
        val md5 = java.security.MessageDigest.getInstance("MD5")
            .digest(fn.toByteArray()).joinToString("") { "%02x".format(it) }
        val dir = "${md5.first()}/${md5.take(2)}"
        return "https://huiji-thumb.huijistatic.com/ff14/uploads/thumb/$dir/$fn/${size}px-$fn"
    }
}
