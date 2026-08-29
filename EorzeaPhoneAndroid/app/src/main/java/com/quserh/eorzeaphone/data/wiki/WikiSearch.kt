package com.quserh.eorzeaphone.data.wiki

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 统一检索：物品 + 任务 + 站点全文，一次搜完，结果合并后标注类型。
 *
 * ## 为什么要接站点全文检索
 *
 * 用户反馈「搜黑话搜不出来，但百度直接搜能匹配到这个 wiki 的条目」。查证过
 * （`开发/WIKI/_probe_search.py` / `_probe_alias.py` / `_probe_fulltext.py`）：
 *
 * 1. 站点 **有** `list=search` 全文检索，平均 0.24s，还带匹配片段高亮和
 *    `redirecttitle`（告诉你是经哪个别名命中的）。百度索引的就是这些正文。
 * 2. 站点有 5731 个重定向页（黑话 → 正式名），但**只有 91 个（2%）**指向
 *    我库里的物品/任务。剩下 3908 个指向怪物、地名、副本、攻略页 ——
 *    比如「A12」→ 亚历山大机神城 天动之章4、「AF」→ 校服。
 *    那些我本地根本没有表，所以做本地别名表解决不了问题。
 *
 * 结论：本地搜（快、离线、能给结构化结果）+ 线上全文兜底（覆盖黑话和
 * 我没有的条目类型）。两者并列显示，标清类型。
 *
 * 线上那部分**只在有网时锦上添花**，失败就静默省略，不影响本地结果。
 */

/** 一条统一检索结果。 */
sealed interface WikiHit {
    val title: String

    data class Item(val item: WikiItem) : WikiHit {
        override val title: String get() = item.nameCn
    }

    data class Quest(val hit: QuestHit, val label: String) : WikiHit {
        override val title: String get() = label
    }

    /**
     * 站点条目（怪物/地名/副本/攻略等，本地没有表的那些）。
     *
     * [snippet] 是站点给的匹配片段，已去掉 HTML。
     * [viaAlias] 非空表示是经这个别名命中的（「A12」→ 亚历山大机神城 天动之章4）。
     */
    data class Page(
        override val title: String,
        val snippet: String,
        val viaAlias: String?,
        val pageId: Int,
    ) : WikiHit
}

data class WikiSearchResult(
    val items: List<WikiHit.Item>,
    val quests: List<WikiHit.Quest>,
    val pages: List<WikiHit.Page>,
    /** 线上那部分有没有跑成功。false = 没网或站点没回，UI 可以提示。 */
    val onlineOk: Boolean,
) {
    val isEmpty: Boolean get() = items.isEmpty() && quests.isEmpty() && pages.isEmpty()
    val total: Int get() = items.size + quests.size + pages.size
}

object WikiSearch {
    private const val LOCAL_ITEM_LIMIT = 24
    private const val LOCAL_QUEST_LIMIT = 24
    private const val PAGE_LIMIT = 12

    // gadget 用的 CDN，对非浏览器客户端宽容。和 WikiRemote 同源。
    private const val BASE = "https://cdn.huijiwiki.com/ff14/api.php"
    private const val UA =
        "EorzeaPhone/0.7.242 (FF14 item lookup; +https://ff14.huijiwiki.com/)"
    private const val TIMEOUT_MS = 8_000

    /**
     * 一次搜三路。本地两路并行，线上一路同时发，谁先回不影响谁。
     */
    suspend fun search(context: Context, query: String): WikiSearchResult =
        coroutineScope {
            val q = query.trim()
            if (q.isBlank()) {
                return@coroutineScope WikiSearchResult(
                    emptyList(), emptyList(), emptyList(), true,
                )
            }
            val itemsJob = async(Dispatchers.IO) {
                runCatching {
                    WikiDb.search(context, WikiFilter(query = q), page = 0)
                        .take(LOCAL_ITEM_LIMIT)
                        .map { WikiHit.Item(it) }
                }.getOrDefault(emptyList())
            }
            val questsJob = async(Dispatchers.IO) {
                runCatching {
                    QuestDb.search(context, q).withLabels()
                        .take(LOCAL_QUEST_LIMIT)
                        .map { (h, label) -> WikiHit.Quest(h, label) }
                }.getOrDefault(emptyList())
            }
            val pagesJob = async(Dispatchers.IO) { fullText(q) }

            val items = itemsJob.await()
            val quests = questsJob.await()
            val (pages, ok) = pagesJob.await()

            // 线上结果里去掉已经作为物品/任务出现过的标题，免得同一个东西列两遍
            val seen = (items.map { it.title } + quests.map { it.hit.name }).toHashSet()
            WikiSearchResult(
                items = items,
                quests = quests,
                pages = pages.filter { it.title !in seen },
                onlineOk = ok,
            )
        }

    /**
     * 站点全文检索。返回 (结果, 是否成功)。
     *
     * 失败一律返回空 + false，不抛 —— 这是锦上添花的一路，
     * 没网时本地结果照常出。
     */
    private fun fullText(q: String): Pair<List<WikiHit.Page>, Boolean> {
        var conn: HttpURLConnection? = null
        return try {
            val url = buildString {
                append(BASE)
                append("?action=query&list=search&format=json&formatversion=2")
                append("&srnamespace=0&srlimit=").append(PAGE_LIMIT)
                append("&srprop=snippet%7Credirecttitle")
                append("&srsearch=").append(URLEncoder.encode(q, "UTF-8"))
            }
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept-Encoding", "gzip")
            }
            if (conn.responseCode !in 200..299) return emptyList<WikiHit.Page>() to false
            val body = (if (conn.contentEncoding?.contains("gzip") == true) {
                java.util.zip.GZIPInputStream(conn.inputStream)
            } else {
                conn.inputStream
            }).use { it.readBytes().decodeToString() }

            val arr = JSONObject(body).optJSONObject("query")?.optJSONArray("search")
                ?: return emptyList<WikiHit.Page>() to true
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        WikiHit.Page(
                            title = o.optString("title"),
                            snippet = stripHtml(o.optString("snippet")),
                            viaAlias = o.optString("redirecttitle").takeIf { it.isNotBlank() },
                            pageId = o.optInt("pageid"),
                        )
                    )
                }
            } to true
        } catch (_: Exception) {
            emptyList<WikiHit.Page>() to false
        } finally {
            runCatching { conn?.disconnect() }
        }
    }

    /**
     * 站点的 snippet 带 `<span class="searchmatch">` 高亮标签和 HTML 实体。
     * 手机上直接显示会露出标签，所以剥干净。
     */
    private fun stripHtml(s: String): String =
        s.replace(Regex("<[^>]*>"), "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace(Regex("\\s+"), " ")
            .trim()

    /**
     * 站点条目的可读地址，点「在网页打开」用。
     *
     * **不能直接用 `URLEncoder.encode`**：它是给 query string 设计的，
     * 把空格编成 `+`。但这里是 URL 的**路径**段，路径里的 `+` 是字面加号，
     * 于是「构想土神 泰坦」变成了 `构想土神+泰坦` —— 一个不存在的词条，
     * 站点回「这个页面被吃掉了」。真机上撞出来的。
     *
     * MediaWiki 的标题里空格和下划线等价，所以先把空格换成 `_` 再编码，
     * 既避开 `+` 的问题，也和站点自己的链接形式一致。
     */
    fun pageUrl(title: String): String {
        val path = URLEncoder.encode(title.replace(' ', '_'), "UTF-8")
            .replace("+", "%20")     // 兜底：万一还有残留的空格
        return "https://ff14.huijiwiki.com/wiki/$path"
    }
}
