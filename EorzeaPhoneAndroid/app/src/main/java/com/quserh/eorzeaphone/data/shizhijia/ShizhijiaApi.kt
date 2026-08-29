package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

/**
 * Minimal HTTP client for the 石之家 (FF14 Rising Stones) JSON API.
 *
 * The web front end calls these endpoints with a browser cookie jar and appends
 * a random `tempsuid` to every request. We mirror exactly that: each call sends
 * the saved session cookie (see [ShizhijiaSession]) plus a fresh UUID, and POSTs
 * are url-encoded form bodies. Responses always look like
 * `{ "code": 10000, "msg": "...", "data": ... }`.
 *
 * Success codes observed: 10000 (ok) and 10002 (ok, alternate). 10403 means
 * "please log in" and 10502 means maintenance.
 */
object ShizhijiaApi {

    private const val MAIN = "https://apiff14risingstones.web.sdo.com/api/"
    private const val HOME_BASE = "https://apiff14risingstones.web.sdo.com/api/home/"
    private const val TIMEOUT_MS = 12_000
    // common/search type ids (mirror the web front end).
    const val SEARCH_TYPE_POST = 1
    const val SEARCH_TYPE_STRAT = 3
    const val SEARCH_TYPE_USER = 6
    const val SEARCH_TYPE_GLAMOUR = 7
    // The backend binds a session to the User-Agent that created it. WeGame login
    // runs in a PC-UA WebView, so the API must send the SAME desktop UA for the
    // session probe to succeed (a mobile UA yields 10105 "please re-login").
    private const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

    /**
     * Opens a tracked HttpURLConnection with browser-ish headers. Always closes
     * the stream. `cookieOverride` lets the login flow probe with a cookie that
     * has not been persisted yet (read straight from the WebView jar).
     */
    private fun connect(url: String, context: Context, method: String, cookieOverride: String? = null): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            requestMethod = method
            setRequestProperty("User-Agent", UA)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Cookie", cookieOverride ?: ShizhijiaSession.savedCookie(context).orEmpty())
            setRequestProperty("Origin", "https://ff14risingstones.web.sdo.com")
            setRequestProperty("Referer", "https://ff14risingstones.web.sdo.com/pc/index.html")
            setRequestProperty("X-Requested-With", "XMLHttpRequest")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }

    private fun read(conn: HttpURLConnection): JSONObject? = try {
        if (conn.responseCode !in 200..299) null
        else {
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            if (text.isBlank()) null else runCatching { JSONObject(text) }.getOrNull()
        }
    } finally {
        conn.disconnect()
    }

    private fun encodeParams(params: Map<String, String>): String =
        params.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}"
        }

    private fun tempsuid(): String = UUID.randomUUID().toString()

    private suspend fun request(
        context: Context,
        base: String,
        path: String,
        params: Map<String, String> = emptyMap(),
        body: Map<String, String> = emptyMap(),
        method: String = "GET",
        cookie: String? = null,
    ): JSONObject? = withContext(Dispatchers.IO) {
        val all = params + mapOf("tempsuid" to tempsuid())
        val query = if (all.isEmpty()) "" else "?" + encodeParams(all)
        runCatching {
            val url = base + path + query
            val conn = connect(url, context, method, cookie)
            // DELETE 也可能带 body（删帖就是 DELETE + {posts_id}）。
            // HttpURLConnection 对 DELETE 开 doOutput 是允许的，只要
            // requestMethod 已经设好——connect() 里先设的就是 method。
            if (body.isNotEmpty() || method == "POST" || method == "PUT") {
                // 站点的 axios 请求拦截器（index bundle 里）对 POST 做两件事：
                //   t.data = {...t.data, tempsuid: uuid()}   ← body 里也要有一个
                //   t.data = qs.stringify(t.data)            ← form-urlencoded，不是 JSON
                // 所以 body 的 tempsuid 和 query 的是两个不同的 uuid，两边都要带。
                val full = body + mapOf("tempsuid" to tempsuid())
                conn.doOutput = true
                conn.outputStream.bufferedWriter().use { it.write(encodeParams(full)) }
            }
            read(conn)
        }.getOrNull()
    }

    /**
     * 原样返回一次 GET 的整个响应体（含 code/msg），只给 [ShizhijiaProbe] 用。
     *
     * 专项数据那 40 多个接口的字段名不在前端 bundle 里，只能拿一次真实响应来看。
     * 这里不加任何解析，探测器自己去折叠形状。
     */
    internal suspend fun rawGet(
        context: Context,
        path: String,
        params: Map<String, String> = emptyMap(),
    ): JSONObject? = request(context, HOME_BASE, path, params)

    /** True when the payload signals a successful business response. */
    private fun JSONObject.isOk(): Boolean {
        val code = optLong("code")
        return code == 10000L || code == 10002L || has("Code") && optLong("Code") == 0L
    }

    // ---- 业务状态码 -------------------------------------------------------
    // 之前所有失败都被压成 null / 空列表，界面分不出"没登录"和"真的没内容"，
    // 于是已登录的人也会看到"请登录"。这里把状态码带出来。
    const val CODE_NEED_LOGIN = 10403L
    const val CODE_NEED_CHARACTER = 10103L
    private const val CODE_NEED_CHARACTER_ALT = 10104L

    /** 一次调用的结果：成功带 payload，失败带原因。 */
    sealed interface Res<out T> {
        data class Ok<T>(val value: T) : Res<T>
        /** 未登录（10403）。 */
        data object NeedLogin : Res<Nothing>
        /** 已登录但没绑角色（10103/10104）。 */
        data object NeedCharacter : Res<Nothing>
        /** 网络问题或其他业务码。code 为 null 表示请求根本没成功。 */
        data class Failed(val code: Long?, val msg: String = "") : Res<Nothing>
    }

    /** 已登录（含"登录了但没绑角色"）时为 true。 */
    val Res<*>.isAuthed: Boolean
        get() = this is Res.Ok || this is Res.NeedCharacter

    private fun <T> JSONObject.toRes(extract: (JSONObject) -> T): Res<T> {
        if (isOk()) return Res.Ok(extract(this))
        val code = optLong("code")
        return when (code) {
            CODE_NEED_LOGIN -> Res.NeedLogin
            CODE_NEED_CHARACTER, CODE_NEED_CHARACTER_ALT -> Res.NeedCharacter
            else -> Res.Failed(code, optString("msg"))
        }
    }

    /** 带状态码的 GET/POST。需要区分登录态的接口走这个。 */
    private suspend fun <T> dataRes(
        context: Context,
        base: String,
        path: String,
        params: Map<String, String> = emptyMap(),
        body: Map<String, String> = emptyMap(),
        method: String = "GET",
        extract: (JSONObject) -> T,
    ): Res<T> {
        val json = request(context, base, path, params, body, method)
            ?: return Res.Failed(null, "网络请求失败")
        return json.toRes(extract)
    }

    /**
     * 给同包的 [ShizhijiaCosUpload] 用的两个出口。
     *
     * 图片上传要打的是 `/api/common/getCOSTokenI`——**base 不是 HOME_BASE**
     * （那个是 `/api/home/`），而且返回体要自己挑字段，套不进现成的
     * dataRes/rowsRes。所以开两个薄口子，而不是把 request/toRes 整个放开：
     * 请求的组装（tempsuid、cookie、UA、Referer）还是只有这里知道。
     */
    // 名字不叫 rawGet：上面 108 行已经有一个 rawGet(context, path, params)
    // （给 ShizhijiaProbe 用，固定走 HOME_BASE）。两个只差一个参数的同名函数
    // 靠重载解析区分，读代码的人得数参数才知道打的是哪个 base，太容易看错。
    internal suspend fun rawGetOn(
        context: Context,
        base: String,
        path: String,
        params: Map<String, String> = emptyMap(),
    ): JSONObject? = request(context, base, path, params)

    /** 把业务码翻成 [Res]，[extract] 返回 null 视为"响应缺字段"。 */
    internal fun <T> resOf(json: JSONObject, extract: (JSONObject) -> T?): Res<T> =
        when (val r = json.toRes { extract(it) }) {
            is Res.Ok -> r.value?.let { Res.Ok(it) } ?: Res.Failed(null, "响应缺字段")
            is Res.NeedLogin -> Res.NeedLogin
            is Res.NeedCharacter -> Res.NeedCharacter
            is Res.Failed -> r
        }

    /** rows 数组的便捷版本；data 为 null 或没有 rows 时给空列表。 */
    private suspend fun <T> rowsRes(
        context: Context,
        base: String,
        path: String,
        params: Map<String, String> = emptyMap(),
        parse: (org.json.JSONArray) -> List<T>,
    ): Res<List<T>> = dataRes(context, base, path, params) { root ->
        val d = root.optJSONObject("data")
        val arr = d?.optJSONArray("rows") ?: d?.optJSONArray("list")
            ?: root.optJSONArray("data")
        if (arr == null) emptyList() else parse(arr)
    }

    /** Returns the `data` object of a successful call, or null on failure. */
    private suspend fun data(
        context: Context,
        base: String,
        path: String,
        params: Map<String, String> = emptyMap(),
        body: Map<String, String> = emptyMap(),
        method: String = "GET",
    ): JSONObject? {
        val json = request(context, base, path, params, body, method) ?: return null
        return json.takeIf { it.isOk() }?.optJSONObject("data")
    }

    /**
     * Returns the raw `data` payload of a successful call - object OR array -
     * for endpoints like sign/signRewardList whose data is a bare array.
     */
    private suspend fun dataAny(
        context: Context,
        base: String,
        path: String,
        params: Map<String, String> = emptyMap(),
        method: String = "GET",
    ): Any? {
        val json = request(context, base, path, params, method = method) ?: return null
        return json.takeIf { it.isOk() }?.opt("data")
    }

    // ---- Public, login-free feed endpoints --------------------------------

    /** Forum partitions list (冒险者行会 / 同人创作 / ...). */
    suspend fun getPostParts(context: Context): List<ShizhijiaPostPart> = partList(context, "1")

    /**
     * 版块字典。
     *
     * 注意 `data` 是**裸数组**，不是 `{rows:[...]}`——原来走 data() 取
     * optJSONObject("data") 直接拿到 null，所以版块 chips 和推荐过滤
     * 一直是空的（只剩"推荐"那一个）。
     */
    private suspend fun partList(context: Context, type: String): List<ShizhijiaPostPart> {
        val payload = dataAny(context, HOME_BASE, "posts/partList", mapOf("type" to type))
        val arr = payload as? org.json.JSONArray
            ?: (payload as? JSONObject)?.let { it.optJSONArray("rows") ?: it.optJSONArray("list") }
            ?: return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(ShizhijiaPostPart.fromJson(it)) }
        }
    }

    /** Hot search words for the search box placeholder row. */
    suspend fun getHotSearchList(context: Context): List<ShizhijiaHotWord> {
        val d = data(context, MAIN, "common/getHotSearchList") ?: return emptyList()
        // The payload may be { data: [...] }, { list: [...] }, or a JSON object map.
        val rows = d.optJSONArray("rows") ?: d.optJSONArray("list") ?: d.optJSONArray("data")
            ?: return emptyList()
        return buildList(rows.length()) {
            for (i in 0 until rows.length()) {
                val item = rows.optJSONObject(i)
                if (item != null) add(ShizhijiaHotWord(item.optString("name"), item.optString("value")))
                else rows.optString(i).takeIf { it.isNotBlank() }?.let { add(ShizhijiaHotWord(it)) }
            }
        }
    }

    /** Paged post feed. `pageTime` comes from the previous response and drives the cursor paging. */
    suspend fun getPostsList(
        context: Context,
        partId: String = "",
        page: Int = 1,
        pageTime: String = "",
        hotType: String = "postsHotNow",
    ): ShizhijiaPage<ShizhijiaPostCard> {
        val params = mutableMapOf(
            "type" to "1",
            "is_top" to "0",
            "is_refine" to "0",
            "part_id" to partId,
            "hotType" to hotType,
            "order" to "",
            "page" to page.toString(),
            "limit" to "15",
        )
        if (pageTime.isNotBlank()) params["pageTime"] = pageTime
        val d = data(context, HOME_BASE, "posts/postsList", params) ?: return ShizhijiaPage(emptyList(), "")
        val rows = d.optJSONArray("rows")
        return ShizhijiaPage(
            rows = rows?.let { ShizhijiaPostCard.fromArray(it) } ?: emptyList(),
            pageTime = d.optString("pageTime"),
        )
    }

    /** Full post detail including HTML article body. */
    suspend fun getPostDetail(context: Context, postId: String): ShizhijiaPostDetail? {
        val d = data(context, HOME_BASE, "posts/postsDetail", mapOf("id" to postId))
            ?: return null
        return ShizhijiaPostDetail.fromJson(d)
    }

    /**
     * Paged post comments. Server order values: "earliest" (post time asc,
     * the site's 默认), "hottest" (likes), "latest" (newest first).
     * onlyLandlord=1 keeps only the post author's own replies (只看楼主).
     */
    suspend fun getPostComments(
        context: Context,
        postId: String,
        order: String = "earliest",
        page: Int = 1,
        pageTime: String = "",
        onlyLandlord: Boolean = false,
    ): ShizhijiaPage<ShizhijiaComment> {
        val params = mutableMapOf("id" to postId, "order" to order, "page" to page.toString(), "onlyLandlord" to if (onlyLandlord) "1" else "0")
        if (pageTime.isNotBlank()) params["pageTime"] = pageTime
        val d = data(context, HOME_BASE, "posts/postsCommentDetail", params) ?: return ShizhijiaPage(emptyList(), "")
        val rows = d.optJSONArray("rows")
        return ShizhijiaPage(
            rows = rows?.let { ShizhijiaComment.fromArray(it) } ?: emptyList(),
            pageTime = d.optString("pageTime"),
        )
    }

    /** Keyword search over posts/guides/users/glamours - see the common/search section. */

    // ---- Global search (common/search) ------------------------------------
    // type ids mirror the web front end: 1 post title, 3 guide title,
    // 6 user, 7 glamour. Param name is `keywords` (NOT `keyword`).

    /** Post/guide search - rows reuse the feed post-card shape. */
    suspend fun searchPosts(
        context: Context,
        keywords: String,
        type: Int = SEARCH_TYPE_POST,
        page: Int = 1,
        limit: Int = 20,
    ): List<ShizhijiaPostCard> {
        if (keywords.isBlank()) return emptyList()
        val d = data(context, MAIN, "common/search", mapOf("type" to type.toString(), "keywords" to keywords, "page" to page.toString(), "limit" to limit.toString()))
            ?: return emptyList()
        val rows = d.optJSONArray("rows") ?: d.optJSONArray("list") ?: return emptyList()
        return ShizhijiaPostCard.fromArray(rows)
    }

    /** User search - `data` is a bare array of profile objects. */
    suspend fun searchUsers(context: Context, keywords: String, limit: Int = 20): List<ShizhijiaSearchUser> {
        if (keywords.isBlank()) return emptyList()
        val payload = dataAny(context, MAIN, "common/search", mapOf("type" to SEARCH_TYPE_USER.toString(), "keywords" to keywords, "limit" to limit.toString()))
        val arr = (payload as? org.json.JSONArray)
            ?: (payload as? JSONObject)?.optJSONArray("rows")
            ?: return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(ShizhijiaSearchUser.fromJson(it)) }
        }
    }

    /**
     * 关注列表 / 粉丝列表。
     *
     * `userRelation/followList` 是名册已经在用的那个接口（拉自己关注的人），
     * 确定可用。粉丝走 `userRelation/fansList`——同模块下的对称接口，
     * **未对过真实响应**；拿不到就是空列表，界面上要说成"读不到"而不是"没有粉丝"。
     *
     * [uuid] 传别人的 uuid 能不能看他的列表也没验证过（官网只暴露看自己的）。
     * 传了服务端忽略也不出错，真不行就是空。
     */
    suspend fun getRelationList(
        context: Context,
        fans: Boolean,
        uuid: String = "",
        page: Int = 1,
        limit: Int = 100,
    ): Res<List<ShizhijiaFriendRoster.Entry>> {
        val params = buildMap {
            put("page", page.toString())
            put("limit", limit.toString())
            if (uuid.isNotBlank()) put("uuid", uuid)
        }
        return friendRosterPage(context, if (fans) "userRelation/fansList" else "userRelation/followList", params)
    }

    /**
     * 名册的一页。给 [ShizhijiaFriendRoster] 用。
     *
     * 两个来源的 `data` 形状不一样（getUnFollowFriend 是裸数组、followList 大概是
     * `{rows:[...]}`），[rowsRes] 两种都认，所以这里不用分开写。
     */
    internal suspend fun friendRosterPage(
        context: Context,
        path: String,
        params: Map<String, String> = emptyMap(),
    ): Res<List<ShizhijiaFriendRoster.Entry>> =
        rowsRes(context, HOME_BASE, path, params) { arr ->
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    arr.optJSONObject(i)?.let { add(ShizhijiaFriendRoster.Entry.fromApi(it)) }
                }
            }
        }

    /** Glamour search - rows carry main_image + like/favorite counts. */
    suspend fun searchGlamours(context: Context, keywords: String, page: Int = 1, limit: Int = 20): List<ShizhijiaSearchGlamour> {
        if (keywords.isBlank()) return emptyList()
        val d = data(context, MAIN, "common/search", mapOf("type" to SEARCH_TYPE_GLAMOUR.toString(), "keywords" to keywords, "page" to page.toString(), "limit" to limit.toString()))
            ?: return emptyList()
        val rows = d.optJSONArray("rows") ?: return emptyList()
        return buildList(rows.length()) {
            for (i in 0 until rows.length()) rows.optJSONObject(i)?.let { add(ShizhijiaSearchGlamour.fromJson(it)) }
        }
    }

    // ---- Login-gated feed endpoints ---------------------------------------

    /** Another player's full profile (userInfo/getUserInfo?uuid=). */
    private val jobIconCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    // Crafter/gatherer jobs use the static sjobN series (sjob0=刻木匠 ... sjob10=捕鱼人).
    private val crafterIcons = mapOf(
        "刻木匠" to "job/sjob0.png",
        "锻铁匠" to "job/sjob1.png",
        "铸甲匠" to "job/sjob2.png",
        "雕金匠" to "job/sjob3.png",
        "制革匠" to "job/sjob4.png",
        "裁衣匠" to "job/sjob5.png",
        "炼金术士" to "job/sjob6.png",
        "烹调师" to "job/sjob7.png",
        "采矿工" to "job/sjob8.png",
        "园艺工" to "job/sjob9.png",
        "捕鱼人" to "job/sjob10.png",
    )

    /**
     * Career name -> job icon url, from the public recruit/getJobConfigList.
     * Cached in-memory; used to render real job icons on the profile page.
     */
    suspend fun jobIconByName(context: Context): Map<String, String> {
        if (jobIconCache.isNotEmpty()) return jobIconCache
        // Seed crafters first so they survive even if the config fetch fails.
        crafterIcons.forEach { (name, path) ->
            jobIconCache[name] = "https://static.web.sdo.com/jijiamobile/pic/ff14/ffstones/$path"
        }
        val d = data(context, HOME_BASE, "recruit/getJobConfigList") ?: return jobIconCache
        // data is an object keyed by Chinese category names ("职能分类",
        // "战斗精英", ...), each holding an array of {id,value,job_pic_url}.
        d.keys().forEach { key ->
            val arr = d.optJSONArray(key) ?: return@forEach
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("value")
                val pic = cleanStr(o.optString("job_pic_url"))
                if (name.isNotBlank() && pic.isNotBlank()) jobIconCache[name] = pic
            }
        }
        android.util.Log.d("ShizhijiaLogin", "jobIconByName size=${jobIconCache.size} sampleCrp=${jobIconCache["刻木匠"] ?: "MISS"}")
        return jobIconCache
    }

    suspend fun getUserProfile(context: Context, uuid: String): ShizhijiaUserProfile? {
        if (uuid.isBlank()) return null
        val d = data(context, HOME_BASE, "userInfo/getUserInfo", mapOf("uuid" to uuid)) ?: return null
        val p = ShizhijiaUserProfile.fromJson(d)
        if (p.name.isBlank()) return null
        return p
    }

    /** A player's posts (userInfo/getUserPosts?uuid=&type=1). */
    suspend fun getUserPosts(context: Context, uuid: String, page: Int = 1, limit: Int = 15): ShizhijiaPage<ShizhijiaPostCard> {
        if (uuid.isBlank()) return ShizhijiaPage(emptyList(), "")
        val d = data(context, HOME_BASE, "userInfo/getUserPosts", mapOf("uuid" to uuid, "type" to "1", "page" to page.toString(), "limit" to limit.toString()))
            ?: return ShizhijiaPage(emptyList(), "")
        val rows = d.optJSONArray("rows")
        return ShizhijiaPage(rows = rows?.let { ShizhijiaPostCard.fromArray(it) } ?: emptyList(), pageTime = "")
    }

    /**
     * 攻略帖列表。攻略在站点上是独立页面，但接口复用 `posts` 那一套，
     * 只是 type=2 且版块树来自 partList?type=2（见 08 文档第五节）。
     * 已实测：type=2 返回与帖子同构的 rows。
     *
     * 注意别在 KDoc 里写 `posts` + 斜杠星号：Kotlin 的块注释可嵌套，
     * 那个序列会开一层内嵌注释，把后面的代码整段吞掉。
     */
    suspend fun getStrategyParts(context: Context): List<ShizhijiaPostPart> = partList(context, "2")

    /** 攻略列表。和 getPostsList 同构，差别只是 type=2。 */
    suspend fun getStrategyList(
        context: Context,
        partId: String = "",
        page: Int = 1,
        pageTime: String = "",
    ): ShizhijiaPage<ShizhijiaPostCard> {
        val params = mutableMapOf(
            "type" to "2",
            "is_top" to "0",
            "is_refine" to "0",
            "part_id" to partId,
            "order" to "",
            "page" to page.toString(),
            "limit" to "15",
        )
        if (pageTime.isNotBlank()) params["pageTime"] = pageTime
        val d = data(context, HOME_BASE, "posts/postsList", params) ?: return ShizhijiaPage(emptyList(), "")
        val rows = d.optJSONArray("rows")
        return ShizhijiaPage(
            rows = rows?.let { ShizhijiaPostCard.fromArray(it) } ?: emptyList(),
            pageTime = d.optString("pageTime"),
        )
    }

    // ---- 招募 -------------------------------------------------------------

    /**
     * 招募列表。五类各自的接口路径不同，行结构也不同，
     * 由 ShizhijiaRecruit 归一（字段名以实测为准，见 03 文档）。
     *
     * 子分类筛选参数取自官网前端各列表组件，只在有值时才带上——
     * 官方对空串的处理不一致，少传比传空安全：
     *   副本组队 RecruitParty  fb_type / fb_name / position / team_composition / label
     *   新人招待 RecruitBeginner  identity / style / target_area_id / target_group_id
     *   其他     RecruitOthers   category / target_area_id / target_group_id
     *   RP       RecruitRp       rp_type / act_status / rp_name
     *
     * 除部队招募外都公开；部队招募未登录返回 NeedLogin。
     */
    suspend fun getRecruitList(
        context: Context,
        kind: ShizhijiaRecruitKind,
        page: Int = 1,
        limit: Int = 15,
        filter: ShizhijiaRecruitFilter = ShizhijiaRecruitFilter(),
    ): Res<List<ShizhijiaRecruit>> {
        val path = when (kind) {
            ShizhijiaRecruitKind.Fb -> "recruit/recruitFbList"
            ShizhijiaRecruitKind.Novice -> "recruit/recruitNeList"
            ShizhijiaRecruitKind.Guild -> "recruit/recruitGuildList"
            ShizhijiaRecruitKind.Other -> "recruit/recruitOtherList"
            ShizhijiaRecruitKind.Rp -> "recruit/recruitRpList"
        }
        val params = mutableMapOf("page" to page.toString(), "limit" to limit.toString())
        params += filter.toParams(kind)
        return rowsRes(context, HOME_BASE, path, params) { ShizhijiaRecruit.fromArray(it, kind) }
    }

    /**
     * 招募详情。四类各有接口，参数统一是 `?id=`，都不需要登录。
     * 部队招募的详情接口（getRecruitGuildDetail）要登录，所以那一类返回 NeedLogin
     * 时界面会提示登录。
     */
    suspend fun getRecruitDetail(
        context: Context,
        kind: ShizhijiaRecruitKind,
        id: String,
    ): Res<ShizhijiaRecruitDetail?> {
        if (id.isBlank()) return Res.Failed(null, "没有招募 id")
        val path = when (kind) {
            ShizhijiaRecruitKind.Fb -> "recruit/getRecruitFbDetail"
            ShizhijiaRecruitKind.Novice -> "recruit/getNeDetail"
            ShizhijiaRecruitKind.Other -> "recruit/getOtherDetail"
            ShizhijiaRecruitKind.Rp -> "recruit/getRpDetail"
            ShizhijiaRecruitKind.Guild -> "recruit/getRecruitGuildDetail"
        }
        return dataRes(context, HOME_BASE, path, mapOf("id" to id)) { root ->
            root.optJSONObject("data")?.let { ShizhijiaRecruitDetail.fromJson(it, kind) }
        }
    }

    // ---- 发布招募 ---------------------------------------------------------

    /**
     * 发布招募。需登录。
     *
     * 三类的 body 取自官网的发布页（RecruitPublishInstance / Beginner / Others），
     * 字段名和顺序都对齐了：
     *   副本 createRecruitFb    fb_type,fb_name,target_area_id,progress,strategy,
     *                          fb_time,need_job,contact_info,label,custom_label,
     *                          team_detail,recruit_require,strategy_desc,
     *                          team_composition + 位置字段
     *   新人 createNoviceEntertain title,identity,target_area_id,target_group_id,
     *                          weekday_time,weekend_time,contact_info,style,detail
     *   其他 createRecruitOther title,target_area_id,target_group_id,contact_info,
     *                          category,cover_pic,detail
     *
     * 位置字段的挂法跟着规模走：满编/轻锐是平铺（MT/ST/H1…），
     * 团队(24) 是 team_position 一个 JSON 字符串。
     */
    suspend fun publishRecruit(
        context: Context,
        kind: ShizhijiaRecruitKind,
        form: ShizhijiaRecruitForm,
    ): Res<String> {
        val path = when (kind) {
            ShizhijiaRecruitKind.Fb -> "recruit/createRecruitFb"
            ShizhijiaRecruitKind.Novice -> "recruit/createNoviceEntertain"
            ShizhijiaRecruitKind.Other -> "recruit/createRecruitOther"
            ShizhijiaRecruitKind.Rp -> "recruit/createRecruitRp"
            ShizhijiaRecruitKind.Guild -> "recruit/createRecruitGuild"
        }
        val json = request(context, HOME_BASE, path, body = form.toBody(kind), method = "POST")
            ?: return Res.Failed(null, "网络没通")
        val code = json.optLong("code")
        val msg = json.optString("msg")
        return when {
            json.isOk() -> Res.Ok(msg.ifBlank { "发布成功" })
            code == CODE_NEED_LOGIN -> Res.NeedLogin
            code == CODE_NEED_CHARACTER -> Res.NeedCharacter
            else -> Res.Failed(code, msg)
        }
    }

    // ---- 写操作：点赞 / 收藏 / 评论 / 招募响应 -----------------------------
    //
    // 形状不是猜的，是从站点自己的 JS 分包里读出来的（那是公开静态资源）。
    // 完整的接口表和取得过程写在同目录的 API_WRITE_ENDPOINTS.md。
    //
    // 注意：**点赞和收藏都是"切换"，不是幂等的 set**。同一个接口再打一次就取消。
    // 返回 data == 1 表示现在是已赞/已收，== -1 表示刚被取消。
    // 界面必须按返回值来定状态，不能自己假设"点了就是赞了"。

    /** 一次切换的结果：[liked] 是**调用之后**的状态。 */
    enum class Toggle { On, Off }

    private fun JSONObject.toggleRes(): Res<Toggle> = toRes {
        // data 是裸数字 1 / -1。
        when (it.optInt("data")) {
            1 -> Toggle.On
            else -> Toggle.Off
        }
    }

    private suspend fun toggle(
        context: Context,
        path: String,
        body: Map<String, String>,
    ): Res<Toggle> {
        val json = request(context, HOME_BASE, path, body = body, method = "POST")
            ?: return Res.Failed(null, "网络没通")
        return json.toggleRes()
    }

    /**
     * 给帖子或评论点赞（再点一次取消）。
     *
     * `type` 是 1=帖子 / 2=评论——同一个端点两用。
     */
    suspend fun likePost(context: Context, postId: String, isComment: Boolean = false): Res<Toggle> {
        if (postId.isBlank()) return Res.Failed(null, "没有帖子 id")
        return toggle(context, "posts/like", mapOf("id" to postId, "type" to if (isComment) "2" else "1"))
    }

    /** 收藏帖子（再调一次取消）。注意字段名是 `posts_id`，和点赞的 `id` 不一样。 */
    suspend fun starPost(context: Context, postId: String): Res<Toggle> {
        if (postId.isBlank()) return Res.Failed(null, "没有帖子 id")
        return toggle(context, "posts/star", mapOf("posts_id" to postId))
    }

    /**
     * 发一条评论或回复。
     *
     * @param parentId 回复哪一条评论。顶层评论传 "0"。
     * @param rootParent 楼层根评论 id。顶层评论传 "0"；回复顶层评论时和 [parentId] 相同。
     * @param pics 逗号拼接的图片 URL；App 里没有上传通道，一直是空串。
     */
    suspend fun commentPost(
        context: Context,
        postId: String,
        content: String,
        parentId: String = "0",
        rootParent: String = "0",
        pics: String = "",
    ): Res<String> {
        if (postId.isBlank()) return Res.Failed(null, "没有帖子 id")
        if (content.isBlank()) return Res.Failed(null, "评论不能为空")
        val json = request(
            context, HOME_BASE, "posts/comment",
            body = mapOf(
                "posts_id" to postId,
                "content" to content,
                "parent_id" to parentId,
                "root_parent" to rootParent,
                "comment_pic" to pics,
                // **这里以前有一行 "atInfo" to "[]"，那是发不出去的原因**
                // （服务端回"@信息格式不对"）。
                //
                // atInfo 是 @ 的人的**数组**（官网 CommentBox 里是 `atInfo: ye.value`），
                // 而 body 走的是 qs.stringify —— 它对空数组的处理是**整个键都不发**，
                // 不是发一个字面量 "[]"。我照字面量发过去，服务端拿它当数组解析就报格式错。
                //
                // 所以没有 @ 的时候就是不带这个键。真要支持 @ 的话，
                // qs 的形状是 atInfo[0][字段]=值，得先把 body 从
                // Map<String,String> 扩成支持嵌套的结构，那是另一件事。
            ),
            method = "POST",
        ) ?: return Res.Failed(null, "网络没通")
        return json.toRes { it.optString("msg").ifBlank { "已发布" } }
    }

    /**
     * 动态的评论。**参数形状和帖子评论一样**（官网 DynamicDetail 里是
     * `{id, order, page, limit, pageTime, onlyLandlord}`，键名是 `id` 不是
     * `dynamic_id`——`dynamic_id` 只在**发**评论时用）。
     * 行结构也同族，所以直接复用 [ShizhijiaComment]。
     *
     * 之前动态详情页只画了作者卡和图片，**评论压根没接**——
     * 所以"明明有评论，点进去什么都没有"。
     */
    suspend fun getDynamicComments(
        context: Context,
        dynamicId: String,
        order: String = "earliest",
        page: Int = 1,
        pageTime: String = "",
        onlyLandlord: Boolean = false,
    ): ShizhijiaPage<ShizhijiaComment> {
        if (dynamicId.isBlank()) return ShizhijiaPage(emptyList(), "")
        val params = mutableMapOf(
            "id" to dynamicId,
            "order" to order,
            "page" to page.toString(),
            "onlyLandlord" to if (onlyLandlord) "1" else "0",
        )
        if (pageTime.isNotBlank()) params["pageTime"] = pageTime
        val d = data(context, HOME_BASE, "dynamic/dynamicCommentDetail", params)
            ?: return ShizhijiaPage(emptyList(), "")
        return ShizhijiaPage(
            rows = d.optJSONArray("rows")?.let { ShizhijiaComment.fromArray(it) } ?: emptyList(),
            pageTime = d.optString("pageTime"),
        )
    }

    /**
     * 给动态发评论 / 回复。
     *
     * 官网 CommentBox 的 dynamic 分支：
     * `{atInfo, content, dynamic_id, parent_id, root_parent, comment_pic}`
     * ——**发的时候键名是 `dynamic_id`**，和读列表用 `id` 不一样，别混。
     */
    suspend fun commentDynamic(
        context: Context,
        dynamicId: String,
        content: String,
        parentId: String = "0",
        rootParent: String = "0",
        pics: String = "",
    ): Res<String> {
        if (dynamicId.isBlank()) return Res.Failed(null, "没有动态 id")
        if (content.isBlank()) return Res.Failed(null, "评论不能为空")
        val json = request(
            context, HOME_BASE, "dynamic/comment",
            body = mapOf(
                "dynamic_id" to dynamicId,
                "content" to content,
                "parent_id" to parentId,
                "root_parent" to rootParent,
                "comment_pic" to pics,
                // atInfo 不带，同 commentPost 那个坑。
            ),
            method = "POST",
        ) ?: return Res.Failed(null, "网络没通")
        return json.toRes { it.optString("msg").ifBlank { "已发布" } }
    }

    /**
     * 发动态。
     *
     * body（官网 `PublishDynamic.kSy1QIIC.js`）：
     * ```
     * {atInfo, content, scope: "1"|"2"|"3", pic_url: "a.jpg,b.jpg"}
     * ```
     *
     * **这里的 [scope] 和发帖那个不一样，它是真正生效的可见范围。**
     * 官网发动态的三个单选**没有外层条件渲染**（默认 `w("1")` = 公开），
     * 而发帖那三个只在"分享到动态"打开时才出现——那个约束的是动态、不是帖子。
     * 所以"只给自己看"这件事，**只有发动态能做到**，发到版块的帖子永远公开。
     * 见 [PostScope] 的说明和 API_WRITE_ENDPOINTS.md 里那一节。
     *
     * **内容和图片至少有一个**就能发（官网是
     * `if (!content && 0 == pics.length) 才拦`），所以允许只发图——
     * 和 [commentPost] 要求 content 非空不同。
     *
     * `scope` 官网是拼成字符串发的（`G.value + ""`），这里也送字符串。
     * `atInfo` 是数组，没有 @ 时**整个键不发**（同 [commentPost] 那个坑）。
     */
    suspend fun publishDynamic(
        context: Context,
        content: String,
        /** 图片 URL，逗号分隔。可以为空（那时 [content] 必须非空）。 */
        pics: String = "",
        scope: PostScope = PostScope.Public,
    ): Res<String> {
        if (content.isBlank() && pics.isBlank()) {
            return Res.Failed(null, "写点什么，或者加一张图")
        }
        val json = request(
            context, HOME_BASE, "dynamic/create",
            body = mapOf(
                "content" to content,
                "scope" to scope.code.toString(),
                "pic_url" to pics,
                // atInfo 故意不带，理由同 commentPost。
            ),
            method = "POST",
        ) ?: return Res.Failed(null, "网络没通")
        return json.toRes { it.optString("msg").ifBlank { "已发布" } }
    }

    /**
     * 删帖。**方法是 DELETE，body 是 `{posts_id}`**（官网 PostInfo 里就是
     * `await n({posts_id: item.posts_id})`，函数定义 `method:"delete"`）。
     *
     * 攻略共用这个端点（`posts/deletePosts`，攻略也是 posts）。
     */
    suspend fun deletePost(context: Context, postId: String): Res<String> {
        if (postId.isBlank()) return Res.Failed(null, "没有帖子 id")
        val json = request(
            context, HOME_BASE, "posts/deletePosts",
            body = mapOf("posts_id" to postId),
            method = "DELETE",
        ) ?: return Res.Failed(null, "网络没通")
        return json.toRes { it.optString("msg").ifBlank { "已删除" } }
    }

    /**
     * **"分享到动态"那条动态的可见范围，不是帖子的。**
     *
     * 这一点我一开始搞错了，而且错得会误导人：我把它当成帖子的可见性
     * 摆在发帖界面上（标题写"谁能看"、含"仅自己可见"），于是用户以为
     * 自己发了一条私密帖，实际帖子照常公开、别人能看到。
     *
     * 真相在官网 PublishPost.DWDKRiEh.js 里：那三个单选写成
     * `1 == et.value ? 单选组 : 不渲染`，`et` 就是 `is_share`
     * （分享到动态开关，默认 0）。也就是说**只有打开"分享到动态"时
     * 这三档才出现**，它约束的是那条动态。
     *
     * **发到版块的帖子本身是公开的，石之家没有"私密帖"。**
     */
    enum class PostScope(val code: Int, val label: String) {
        Public(1, "公开"),
        Mutual(2, "仅互关可见"),
        Private(3, "仅自己可见"),
    }

    /**
     * 发帖（也用于发攻略，差别只是 [type]）。
     *
     * body 取自官网 `PublishPost.DWDKRiEh.js`：
     * ```
     * {id, updated_at, atInfo, type, part_id, title, is_share, content, scope, cover_pic, has_vote}
     * ```
     * - `id` / `updated_at` 是**编辑**已有帖子时才用的，新发留空
     * - `type` 1 = 帖子，2 = 攻略（和 postsList 的 type 一致）
     * - `content` 是 HTML；图片以 `<img src>` 的形式**嵌在正文里**
     * - `cover_pic` 是从正文里提取出来的图片 URL（逗号分隔），
     *   官网的做法是正则扫 `<img src>` 并**排除 static.web.sdo.com 开头的**
     *   ——那些是表情，不能当封面
     * - `has_vote` 没有投票时是 "0"
     * - **`atInfo` 是数组，没有 @ 时整个键都不发**（和 commentPost 同一个坑，
     *   发字面量 "[]" 会被服务端拒："@信息格式不对"）
     */
    suspend fun publishPost(
        context: Context,
        title: String,
        /** HTML 正文。图片用 `<img src="…">` 嵌进去。 */
        contentHtml: String,
        partId: String,
        /**
         * 只在 [share] 为 true 时有意义 —— 它是**那条动态**的可见范围。
         * 见 [PostScope] 的说明：帖子本身在版块里永远是公开的。
         */
        scope: PostScope = PostScope.Public,
        type: String = "1",
        /** 同时在动态里发一条指向这篇帖子。官网默认关。 */
        share: Boolean = false,
    ): Res<String> {
        if (title.isBlank()) return Res.Failed(null, "标题不能为空")
        if (contentHtml.isBlank()) return Res.Failed(null, "正文不能为空")
        if (partId.isBlank()) return Res.Failed(null, "要先选一个版块")
        // 封面：正文里的图片，排除表情（static.web.sdo.com）。
        val cover = Regex("""<img[^>]+src=['"]([^'"]+)['"]""")
            .findAll(contentHtml)
            .map { it.groupValues[1] }
            .filterNot { it.startsWith("https://static.web.sdo.com") }
            .toList()
            .joinToString(",")
        val json = request(
            context, HOME_BASE, "posts/create",
            body = mapOf(
                "id" to "",
                "updated_at" to "",
                "type" to type,
                "part_id" to partId,
                "title" to title,
                "is_share" to if (share) "1" else "0",
                "content" to contentHtml,
                "scope" to scope.code.toString(),
                "cover_pic" to cover,
                "has_vote" to "0",
                // atInfo 故意不带，理由同 commentPost。
            ),
            method = "POST",
        ) ?: return Res.Failed(null, "网络没通")
        // 成功时 data.id 是新帖子的 id，界面可以直接跳过去。
        return json.toRes { it.optJSONObject("data")?.optString("id").orEmpty() }
    }

    /**
     * 楼中楼：一条顶层评论下面的子评论。
     *
     * 参数取自官网 `Comment.CMSwEVrf.js`：`{root_parent, order, page, limit}`，
     * limit 官网用 10。**只有 `children_count > 0` 时才该调**——
     * 官网也是这么判的，没有子评论的楼不发这个请求。
     *
     * [rootParent] 是那一楼的 id（顶层评论自己的 id）。
     */
    suspend fun getSubComments(
        context: Context,
        rootParent: String,
        order: String = "earliest",
        page: Int = 1,
        limit: Int = 10,
    ): Res<List<ShizhijiaComment>> {
        if (rootParent.isBlank() || rootParent == "0") return Res.Failed(null, "没有楼层 id")
        return rowsRes(
            context, HOME_BASE, "posts/postsSubCommentDetail",
            mapOf(
                "root_parent" to rootParent,
                "order" to order,
                "page" to page.toString(),
                "limit" to limit.toString(),
            ),
        ) { ShizhijiaComment.fromArray(it) }
    }

    /**
     * 幻化的装备槽位，顺序照官网。发布时没选的槽补 equipment_id = -1。
     * WAIST / SOUL_CRYSTAL 官网固定补空，所以不列在这里、由 publishGlamour 追加。
     */
    internal val SZJ_GLAMOUR_SLOTS = listOf(
        "MAIN_HAND", "OFF_HAND", "HEAD", "BODY", "GLOVES", "LEGS", "FEET",
        "EARS", "NECK", "WRISTS", "FINGER_LEFT", "FINGER_RIGHT",
    )

    /** 一个装备槽的选择。[equipmentId] 为 -1 表示这个槽空着。 */
    data class GlamourSlotPick(
        val slot: String,
        val equipmentId: Long = -1L,
        val dyeIds: List<Long> = emptyList(),
    )

    /**
     * 发幻化。
     *
     * body（官网 `PublishGlamour.Ce8u-l5I.js`）：
     * ```
     * {main_image, images, title, desc, race_ids, gender_ids, is_share, scope,
     *  job_ids, equipments, is_hidden, ornaments, fashion_coupon?}
     * ```
     * - `race_ids` / `gender_ids` / `job_ids` 都是**逗号分隔的字符串**
     * - `equipments` 是 **JSON 字符串**，元素 `{equipment_id, slot, dye_ids}`；
     *   官网还会额外补两个空槽 `WAIST` 和 `SOUL_CRYSTAL`
     * - `ornaments` 也是 JSON 字符串：`[{glasses_id}, {ornament_id}]`
     * - `scope` 官网写死 `"1"`，`is_hidden` 写死 `"0"`
     *
     * **`equipment_id: -1` 表示这个槽空着**，所以一件装备都不选也能发——
     * 那就是"只发外观图 + 标题"的幻化。装备清单是加分项不是必填项，
     * 这也是这一版能先上的原因（挑装备要 5 万件物品的选择器，见 wiki 那套库）。
     */
    suspend fun publishGlamour(
        context: Context,
        title: String,
        desc: String,
        /** 封面图 URL（必填，要先上传）。 */
        mainImage: String,
        /** 其余图片 URL，逗号分隔。 */
        images: String = "",
        raceIds: List<Int> = emptyList(),
        genderIds: List<Int> = emptyList(),
        jobIds: List<String> = emptyList(),
        slots: List<GlamourSlotPick> = emptyList(),
        glassesId: Long = -1L,
        ornamentId: Long = -1L,
        share: Boolean = false,
    ): Res<String> {
        if (title.isBlank()) return Res.Failed(null, "标题不能为空")
        if (mainImage.isBlank()) return Res.Failed(null, "要先选一张封面图")
        // 没传的槽一律补成空槽，并补上官网那两个固定的空槽。
        val allSlots = (SZJ_GLAMOUR_SLOTS + listOf("WAIST", "SOUL_CRYSTAL")).distinct()
        val picked = slots.associateBy { it.slot }
        val equipJson = allSlots.joinToString(",", "[", "]") { slot ->
            val p = picked[slot]
            val dyes = p?.dyeIds?.joinToString(",", "[", "]") ?: "[]"
            """{"equipment_id":${p?.equipmentId ?: -1},"slot":"$slot","dye_ids":$dyes}"""
        }
        val ornJson = """[{"glasses_id":"$glassesId"},{"ornament_id":"$ornamentId"}]"""
        val json = request(
            context, HOME_BASE, "glamour/createGlamour",
            body = mapOf(
                "main_image" to mainImage,
                "images" to images,
                "title" to title,
                "desc" to desc,
                "race_ids" to raceIds.joinToString(","),
                "gender_ids" to genderIds.joinToString(","),
                "job_ids" to jobIds.joinToString(","),
                "is_share" to if (share) "1" else "0",
                "scope" to "1",
                "is_hidden" to "0",
                "equipments" to equipJson,
                "ornaments" to ornJson,
            ),
            method = "POST",
        ) ?: return Res.Failed(null, "网络没通")
        return json.toRes { it.optJSONObject("data")?.optString("id").orEmpty() }
    }

    /** 幻化点赞（切换）。 */
    suspend fun likeGlamour(context: Context, id: String): Res<Toggle> {
        if (id.isBlank()) return Res.Failed(null, "没有幻化 id")
        return toggle(context, "glamour/like", mapOf("id" to id))
    }

    /**
     * 幻化收藏夹。收藏必须指定一个夹子，所以先要拿到列表。
     *
     * 返回 (id, 名字, 是否默认夹)。
     */
    suspend fun glamourFavorites(
        context: Context,
        page: Int = 1,
        limit: Int = 20,
    ): Res<List<Triple<String, String, Boolean>>> = dataRes(
        context, HOME_BASE, "glamour/myFavoritesList",
        mapOf("page" to page.toString(), "limit" to limit.toString()),
    ) { root ->
        val rows = root.optJSONObject("data")?.optJSONArray("rows")
        buildList {
            if (rows != null) for (i in 0 until rows.length()) {
                val o = rows.optJSONObject(i) ?: continue
                add(
                    Triple(
                        o.opt("id")?.toString().orEmpty(),
                        o.optString("name").ifBlank { o.optString("title") },
                        o.optInt("is_default") == 1,
                    ),
                )
            }
        }
    }

    /**
     * 收藏一套幻化到指定收藏夹。
     *
     * `favorite_id` 是**收藏夹** id，不是幻化 id——官网的流程是先查
     * myFavoritesList，只有一个夹且 is_default 时直接用它，否则让人选。
     */
    suspend fun favoriteGlamour(context: Context, id: String, favoriteId: String): Res<String> {
        if (id.isBlank() || favoriteId.isBlank()) return Res.Failed(null, "缺少 id")
        val json = request(
            context, HOME_BASE, "glamour/favorite",
            body = mapOf("id" to id, "favorite_id" to favoriteId), method = "POST",
        ) ?: return Res.Failed(null, "网络没通")
        return json.toRes { it.optString("msg").ifBlank { "已收藏" } }
    }

    /** 取消幻化收藏。 */
    suspend fun cancelFavoriteGlamour(context: Context, id: String): Res<String> {
        if (id.isBlank()) return Res.Failed(null, "没有幻化 id")
        val json = request(
            context, HOME_BASE, "glamour/cancelFavorite",
            body = mapOf("id" to id), method = "POST",
        ) ?: return Res.Failed(null, "网络没通")
        return json.toRes { it.optString("msg").ifBlank { "已取消收藏" } }
    }

    /**
     * 响应一条招募。
     *
     * 四类各一个端点，body 都是 `{id, contact_info}`。
     * [contactInfo] 是**你自己**留给发布者的联系方式，不能为空。
     *
     * 成功后返回**发布者的**联系方式（`data.recruit_contact_info`）——
     * 这就是"响应"的实际作用：交换联系方式。响应之前只能看到打码的
     * `contact_info_mask`。
     *
     * 情景剧（Rp）没有响应接口，那一类走的是评论和 starRp。
     */
    suspend fun respondRecruit(
        context: Context,
        kind: ShizhijiaRecruitKind,
        id: String,
        contactInfo: String,
    ): Res<String> {
        if (id.isBlank()) return Res.Failed(null, "没有招募 id")
        if (contactInfo.isBlank()) return Res.Failed(null, "请先填写你的联系方式")
        val path = when (kind) {
            ShizhijiaRecruitKind.Fb -> "recruit/responseRecruitFb"
            ShizhijiaRecruitKind.Novice -> "recruit/responseNoviceEntertain"
            ShizhijiaRecruitKind.Other -> "recruit/responseRecruitOther"
            ShizhijiaRecruitKind.Guild -> "recruit/responseRecruitGuild"
            ShizhijiaRecruitKind.Rp -> return Res.Failed(null, "情景剧招募没有响应功能")
        }
        val json = request(
            context, HOME_BASE, path,
            body = mapOf("id" to id, "contact_info" to contactInfo), method = "POST",
        ) ?: return Res.Failed(null, "网络没通")
        return json.toRes { root ->
            root.optJSONObject("data")?.optString("recruit_contact_info").orEmpty()
        }
    }

    // ---- 招募筛选用到的字典（全部公开） ----------------------------------

    /** 副本字典：{id, fb_type(绝境战/零式/多变迷宫/诛灭战), fb_name, team_composition}。 */
    suspend fun getFbConfig(context: Context): List<ShizhijiaFbConfig> {
        val payload = dataAny(context, HOME_BASE, "recruit/getFbConfigList")
        val arr = payload as? org.json.JSONArray ?: return emptyList()
        return ShizhijiaFbConfig.fromArray(arr)
    }

    /** 玩法风格字典（新人招待用）：{id, style}。 */
    suspend fun getStyleConfig(context: Context): List<Pair<String, String>> =
        idNamePairs(context, "recruit/styleConfigList", "style")

    /** 其他招募的分类字典：{id, name}。 */
    suspend fun getOtherCategories(context: Context): List<Pair<String, String>> =
        idNamePairs(context, "recruit/categoryConfigList", "name")

    /** 副本招募标签字典：{id, name}。 */
    suspend fun getFbLabels(context: Context): List<Pair<String, String>> =
        idNamePairs(context, "recruit/fbLabelList", "name")

    /**
     * 职业/职能字典，招募卡上的位置图标要用它按 id 反查。
     * 响应是按分组的对象（职能分类/防护职业/治疗职业/…），
     * 前端拍平成一个数组再 find(id)，这里返回 id→职业 的表。
     */
    suspend fun getJobConfig(context: Context): Map<String, ShizhijiaJob> {
        val json = request(context, HOME_BASE, "recruit/getJobConfigList") ?: return emptyMap()
        if (!json.isOk()) return emptyMap()
        return ShizhijiaJob.fromGrouped(json.optJSONObject("data")).associateBy { it.id }
    }

    private suspend fun idNamePairs(context: Context, path: String, nameKey: String): List<Pair<String, String>> {
        val payload = dataAny(context, HOME_BASE, path)
        val arr = payload as? org.json.JSONArray ?: return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val id = o.optString("id")
                val name = o.optString(nameKey)
                if (id.isNotBlank() && name.isNotBlank()) add(id to name)
            }
        }
    }

    /**
     * 当前绑定的角色。需登录。
     *
     * 形状来自官网前端（pc/static/js/index.js）：
     *     getCharacterBindInfo?platform=2 → data.character_name / data.uuid /
     *     data.characterDetail{race,tribe,gender,fc_id,character_id,...}
     * 也就是**单个角色**，不是列表——之前按数组解析所以一直读不出来。
     * platform: PC=2，移动端=1；这里跟 App 的其他调用保持 platform=2。
     */
    suspend fun getCurrentCharacter(context: Context): Res<ShizhijiaBoundCharacter?> =
        dataRes(context, HOME_BASE, "groupAndRole/getCharacterBindInfo", mapOf("platform" to "2")) { root ->
            val d = root.optJSONObject("data")
            if (d == null || d.optString("character_name").isBlank()) null
            else ShizhijiaBoundCharacter.fromJson(d)
        }

    /**
     * 某个大区下我名下的角色列表，用于切换角色。
     * 前端：`getFF14Characters?AreaID=<n>`，AreaID 来自 getAreaAndGroupList。
     */
    suspend fun getAreaCharacters(context: Context, areaId: Int): List<ShizhijiaBoundCharacter> {
        val payload = dataAny(context, HOME_BASE, "groupAndRole/getFF14Characters", mapOf("AreaID" to areaId.toString()))
        val arr = payload as? org.json.JSONArray
            ?: (payload as? JSONObject)?.let { it.optJSONArray("rows") ?: it.optJSONArray("list") }
            ?: return emptyList()
        return ShizhijiaBoundCharacter.fromArray(arr)
    }

    /** 大区/服务器字典（公开）。 */
    suspend fun getAreaList(context: Context): List<ShizhijiaArea> {
        val payload = dataAny(context, HOME_BASE, "groupAndRole/getAreaAndGroupList")
        val arr = payload as? org.json.JSONArray ?: return emptyList()
        return ShizhijiaArea.fromArray(arr)
    }

    /**
     * 切换（绑定）角色。
     * 前端 actBindCharacter 实际只发 `{character_id, platform}`——
     * store 里虽然解构了 device_id，但调用点没传，所以是 undefined。
     */
    suspend fun bindCharacter(context: Context, characterId: String, platform: Int = 2): Boolean {
        if (characterId.isBlank()) return false
        val json = request(
            context, HOME_BASE, "groupAndRole/bindCharacterInfo",
            body = mapOf("character_id" to characterId, "platform" to platform.toString()),
            method = "POST",
        ) ?: return false
        return json.isOk()
    }

    /**
     * 我的部队主页信息。
     * 部队 id 就是当前角色的 `characterDetail.fc_id`（前端 GuildMain 里
     * 用 `characterDetail.fc_id === guild_id` 判断"这是我自己的部队"）。
     */
    suspend fun getGuildInfo(context: Context, guildId: String): Res<JSONObject> {
        if (guildId.isBlank()) return Res.Failed(null, "没有部队 id")
        return dataRes(context, HOME_BASE, "guild/getGuildInfo", mapOf("guild_id" to guildId)) {
            it.optJSONObject("data") ?: JSONObject()
        }
    }

    /**
     * 部队成员。只要 guild_id，没有分页；响应分两组
     * （前端 GuildMember 解构的是 `{registered, unRegister}`）：
     * 注册过石之家的成员有 uuid 可以点进主页，未注册的只有名字。
     */
    suspend fun getGuildMembers(context: Context, guildId: String): Res<ShizhijiaGuildMembers> {
        if (guildId.isBlank()) return Res.Failed(null, "没有部队 id")
        return dataRes(context, HOME_BASE, "guild/getGuildMember", mapOf("guild_id" to guildId)) { root ->
            val d = root.optJSONObject("data")
            ShizhijiaGuildMembers(
                registered = ShizhijiaGuildMember.fromArray(d?.optJSONArray("registered")),
                unregistered = ShizhijiaGuildMember.fromArray(d?.optJSONArray("unRegister")),
            )
        }
    }

    /** 部队成员动态聚合。前端用 page + limit=10。 */
    suspend fun getGuildDynamics(context: Context, guildId: String, page: Int = 1): Res<List<ShizhijiaDynamic>> {
        if (guildId.isBlank()) return Res.Failed(null, "没有部队 id")
        return rowsRes(
            context, HOME_BASE, "guild/guildMemberDynamic",
            mapOf("guild_id" to guildId, "page" to page.toString(), "limit" to "10"),
        ) { ShizhijiaDynamic.fromArray(it) }
    }

    /**
     * 部队相册。前端用 page + limit=15，响应里除 rows 还有 is_guild_master
     * （决定能不能删照片，本 App 只读，用不到）。
     */
    suspend fun getGuildPhotos(context: Context, guildId: String, page: Int = 1): Res<List<ShizhijiaGuildPhoto>> {
        if (guildId.isBlank()) return Res.Failed(null, "没有部队 id")
        return dataRes(
            context, HOME_BASE, "guild/getGuildPhotos",
            mapOf("guild_id" to guildId, "page" to page.toString(), "limit" to "15"),
        ) { root ->
            val arr = root.optJSONObject("data")?.optJSONArray("rows")
            ShizhijiaGuildPhoto.fromArray(arr)
        }
    }

    /**
     * 单张照片详情。前端 GuildPhotoDetail 用 `{id}`，
     * 返回里 photo_url 还是逗号分隔的多图。
     */
    suspend fun getGuildPhotoDetail(context: Context, photoId: String): Res<ShizhijiaGuildPhoto?> {
        if (photoId.isBlank()) return Res.Failed(null, "没有照片 id")
        return dataRes(context, HOME_BASE, "guild/getGuildPhotoDetail", mapOf("id" to photoId)) { root ->
            root.optJSONObject("data")?.let { ShizhijiaGuildPhoto.fromJson(it) }
        }
    }

    /**
     * 照片的评论。注意路径首字母大写（`guild/GuildPhotoCommentDetail`），
     * 这是服务端的写法，改成小写会 404。
     */
    suspend fun getGuildPhotoComments(
        context: Context,
        photoId: String,
        page: Int = 1,
        limit: Int = 20,
    ): Res<List<ShizhijiaComment>> {
        if (photoId.isBlank()) return Res.Failed(null, "没有照片 id")
        return rowsRes(
            context, HOME_BASE, "guild/GuildPhotoCommentDetail",
            mapOf("photo_id" to photoId, "page" to page.toString(), "limit" to limit.toString()),
        ) { ShizhijiaComment.fromArray(it) }
    }

    /**
     * 我收藏的帖子/攻略。需登录——未登录返回 Res.NeedLogin，
     * 界面据此区分空列表和未登录。
     *
     * **`type` 是必填的**，漏了服务端直接回 "Type不正确"（我之前就漏了，
     * 所以收藏页一直是空的）。取值来自官网 MeCollections 页的两个分页函数：
     * `{type:1}` = 帖子，`{type:2}` = 攻略，和 postsList 的 type 一致。
     */
    suspend fun getMyStarPosts(
        context: Context,
        type: String = "1",
        page: Int = 1,
        limit: Int = 15,
    ): Res<List<ShizhijiaPostCard>> =
        rowsRes(
            context, HOME_BASE, "userInfo/myStarPosts",
            mapOf("type" to type, "page" to page.toString(), "limit" to limit.toString()),
        ) { ShizhijiaPostCard.fromArray(it) }

    /**
     * 我收藏的 RP 招募。和上面不是一套接口：官网 MeCollections 的 rp 标签
     * 走 `recruit/homePageStarRecruitRp`，**不带任何参数**（没有分页），
     * 一次把收藏的 RP 全给你。所以这里也不做分页。
     *
     * 官网拿到 rows 之后自己补了 `is_star: true`（收藏列表里的必然已收藏，
     * 接口不重复告诉你），这里同样不依赖行里的收藏字段。
     */
    suspend fun getMyStarRecruitRp(context: Context): Res<List<ShizhijiaRecruit>> =
        rowsRes(context, HOME_BASE, "recruit/homePageStarRecruitRp", emptyMap()) {
            ShizhijiaRecruit.fromArray(it, ShizhijiaRecruitKind.Rp)
        }

    /**
     * 我收藏的幻化。**两层**：收藏必须落在某个收藏夹里，所以先 myFavoritesList
     * 拿夹子，再 myFavoriteItemsList 拿夹子里的内容。之前我只实现了第一层
     * （而且只当作收藏时的选择器用），所以"收藏"页里一套幻化都看不到。
     *
     * [favoriteId] 为空时取默认夹（没有默认夹就取第一个）。
     * uuid 只在看别人的收藏时才需要，看自己的不带——官网也是 undefined。
     */
    suspend fun getMyStarGlamours(
        context: Context,
        favoriteId: String = "",
        page: Int = 1,
        limit: Int = 16,
    ): Res<List<ShizhijiaGlamourCard>> {
        val folder = favoriteId.ifBlank {
            when (val folders = glamourFavorites(context, 1, 50)) {
                is Res.Ok -> folders.value.firstOrNull { it.third }?.first
                    ?: folders.value.firstOrNull()?.first
                    // 没有收藏夹 = 没收藏过任何幻化，这是空而不是错。
                    ?: return Res.Ok(emptyList())
                is Res.NeedLogin -> return Res.NeedLogin
                is Res.NeedCharacter -> return Res.NeedCharacter
                is Res.Failed -> return Res.Failed(folders.code, folders.msg)
            }
        }
        return rowsRes(
            context, HOME_BASE, "glamour/myFavoriteItemsList",
            mapOf("favorite_id" to folder, "page" to page.toString(), "limit" to limit.toString()),
            // 收藏行的字段和幻化列表不一样（主键是 glamour_id），必须走专用解析。
        ) { ShizhijiaGlamourCard.fromFavoriteArray(it) }
    }

    /**
     * 我发布的招募。需登录。四类各有自己的"我的"接口，
     * 部队招募的路径与其他三类命名不同（myGuildRecruitList）。
     */
    suspend fun getMyRecruitList(
        context: Context,
        kind: ShizhijiaRecruitKind,
        page: Int = 1,
        limit: Int = 15,
    ): Res<List<ShizhijiaRecruit>> {
        val path = when (kind) {
            ShizhijiaRecruitKind.Fb -> "recruit/myFbRecruitList"
            ShizhijiaRecruitKind.Novice -> "recruit/myNeRecruitList"
            ShizhijiaRecruitKind.Guild -> "recruit/myGuildRecruitList"
            ShizhijiaRecruitKind.Other -> "recruit/myOtherRecruitList"
            // RP 没有 myRpRecruitList，主页收藏接口是另一套，这里不提供。
            ShizhijiaRecruitKind.Rp -> return Res.Ok(emptyList())
        }
        return rowsRes(
            context, HOME_BASE, path,
            mapOf("page" to page.toString(), "limit" to limit.toString()),
        ) { ShizhijiaRecruit.fromArray(it, kind) }
    }

    /**
     * 一键擦亮：把自己所有招募的排序时间刷新到当前。
     * 返回 true 表示服务端接受了这次请求。
     */
    suspend fun oneKeyPolish(context: Context): Boolean {
        val json = request(context, HOME_BASE, "recruit/oneKeyPolish") ?: return false
        return json.isOk()
    }

    /** Full glamour post (glamour/glamourDetail?id=). */
    suspend fun getGlamourDetail(context: Context, id: String): ShizhijiaGlamourDetail? {
        val d = data(context, HOME_BASE, "glamour/glamourDetail", mapOf("id" to id)) ?: return null
        return ShizhijiaGlamourDetail.fromJson(d)
    }

    /**
     * Glamour feed (glamour/glamoursList). order="" = 推荐 (hot_score),
     * "time" = 最新. Requires login.
     * Filters: raceId (1-8, -1/blank=全部), genderId (-1全部/1男/2女),
     * createTime (all/last24H/lastWeek/lastMonth).
     */
    suspend fun getGlamours(
        context: Context,
        page: Int = 1,
        limit: Int = 10,
        order: String = "",
        raceId: Int = -1,
        genderId: Int = -1,
        createTime: String = "all",
    ): List<ShizhijiaGlamourCard> {
        val params = mutableMapOf(
            "page" to page.toString(),
            "limit" to limit.toString(),
            "order" to order,
        )
        if (raceId > 0) params["race_id"] = raceId.toString()
        if (genderId > 0) params["gender_id"] = genderId.toString()
        if (createTime != "all") params["createTime"] = createTime
        val d = data(context, HOME_BASE, "glamour/glamoursList", params) ?: return emptyList()
        val rows = d.optJSONArray("rows") ?: return emptyList()
        return ShizhijiaGlamourCard.fromArray(rows)
    }

    /** Glamour feed of followed creators (glamour/glamoursFollowList). */
    suspend fun getFollowGlamours(context: Context, page: Int = 1, limit: Int = 10): List<ShizhijiaGlamourCard> {
        val d = data(context, HOME_BASE, "glamour/glamoursFollowList", mapOf("page" to page.toString(), "limit" to limit.toString()))
            ?: return emptyList()
        val rows = d.optJSONArray("rows") ?: return emptyList()
        return ShizhijiaGlamourCard.fromArray(rows)
    }

    /**
     * 游戏近况 (userInfo/getResently). Pass uuid for other players - returns
     * empty when they set the activity feed to private (code 10001).
     * Icons: ffstones/recent/r{typeId}.png
     */
    suspend fun getRecentEvents(context: Context, uuid: String? = null): List<ShizhijiaRecentEvent> {
        val params = if (uuid.isNullOrBlank()) emptyMap() else mapOf("uuid" to uuid)
        val json = request(context, HOME_BASE, "userInfo/getResently", params) ?: return emptyList()
        val arr = json.takeIf { it.isOk() }?.opt("data") as? org.json.JSONArray ?: return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    ShizhijiaRecentEvent(
                        typeId = o.optString("event_type_id"),
                        eventType = o.optString("event_type"),
                        detail = o.optString("detail"),
                        logTime = o.optString("log_time"),
                    ),
                )
            }
        }
    }

    /** Following dynamics feed; requires an authenticated session cookie. */
    suspend fun getFollowDynamicList(context: Context, page: Int = 1): ShizhijiaPage<ShizhijiaDynamic> {
        val d = data(context, HOME_BASE, "dynamic/getFollowDynamicList", mapOf("page" to page.toString())) ?: return ShizhijiaPage(emptyList(), "")
        val rows = d.optJSONArray("rows")
        return ShizhijiaPage(
            rows = rows?.let { ShizhijiaDynamic.fromArray(it) } ?: emptyList(),
            pageTime = d.optString("pageTime"),
        )
    }

    /** Single dynamic detail. */
    suspend fun getDynamicDetail(context: Context, id: String): ShizhijiaDynamic? {
        val d = data(context, HOME_BASE, "dynamic/dynamicDetail", mapOf("id" to id)) ?: return null
        return ShizhijiaDynamic.fromJson(d)
    }

    /**
     * True when an `isLogin` probe reports a valid session (code gate differs
     * from 10403). `cookieOverride` carries a not-yet-persisted cookie during
     * the WebView login flow; otherwise the saved session is used.
     */
    suspend fun isLoggedIn(context: Context, cookieOverride: String? = null): Boolean {
        val json = request(context, HOME_BASE, "GHome/isLogin", emptyMap(), cookie = cookieOverride) ?: return false
        return json.isOk()
    }

    /** Daily check-in (sign/signIn, POST). Returns true when the server accepts it. */
    suspend fun signIn(context: Context): Boolean {
        val json = request(context, HOME_BASE, "sign/signIn", method = "POST")
        return json != null && json.isOk()
    }

    /**
     * True when the server-side log shows a check-in for today. Used to recover
     * the button state when signIn is rejected because today was already signed
     * (the server answers a non-10000 code for a duplicate check-in).
     */
    suspend fun isSignedToday(context: Context): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        val month = today.substring(0, 7)
        return try {
            getSignLog(context, month)?.days?.any { it.contains(today) } == true
        } catch (e: Exception) { false }
    }

    /** Cumulative reward table for `month` ("YYYY-MM"): rule / claim state. */
    suspend fun getSignRewards(context: Context, month: String): List<ShizhijiaSignReward> {
        val payload = dataAny(context, HOME_BASE, "sign/signRewardList", mapOf("month" to month))
        val arr = when (payload) {
            is org.json.JSONArray -> payload
            is JSONObject -> payload.optJSONArray("rows")
                ?: payload.optJSONArray("list")
                ?: payload.optJSONArray("data")
            else -> null
        } ?: return emptyList()
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(ShizhijiaSignReward.fromJson(it)) }
        }
    }

    /** Month check-in log (sign/mySignLog): total count for the month + signed dates. */
    suspend fun getSignLog(context: Context, month: String): ShizhijiaSignLog? {
        val d = data(context, HOME_BASE, "sign/mySignLog", mapOf("month" to month)) ?: return null
        // data is { count: <total>, rows: [ { sign_time: "..."} ] }; some builds
        // wrap rows in {rows:{rows:[...]}} - handle both shapes defensively.
        var count = d.optInt("count")
        var arr = d.optJSONArray("rows")
        if (arr == null) {
            val inner = d.optJSONObject("rows")
            count = inner?.optInt("count") ?: count
            arr = inner?.optJSONArray("rows")
        }
        val days = buildList(arr?.length() ?: 0) {
            if (arr != null) for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i)
                val t = o?.optString("sign_time").orEmpty()
                if (t.isNotBlank()) add(t)
            }
        }
        return ShizhijiaSignLog(count = count, days = days)
    }

    /** Claim one cumulative reward (sign/getSignReward, POST id+month). */
    suspend fun claimSignReward(context: Context, id: String, month: String): Boolean {
        val json = request(context, HOME_BASE, "sign/getSignReward", body = mapOf("id" to id, "month" to month), method = "POST")
        return json != null && json.isOk()
    }

    /**
     * Resolve the avatar for ANY player by uuid. Players without a custom
     * photo get the official per-race default portrait built from their
     * character detail (race/tribe/gender), exactly like the web does.
     * Results are cached in-memory for the session to keep request volume low.
     */
    private val avatarResolveCache = java.util.concurrent.ConcurrentHashMap<String, String>()

    suspend fun resolveAvatar(context: Context, uuid: String): String {
        if (uuid.isBlank()) return ""
        avatarResolveCache[uuid]?.let { return it }
        val d = data(context, HOME_BASE, "userInfo/getUserInfo", mapOf("uuid" to uuid)) ?: return ""
        // characterDetail is an ARRAY ([{race,tribe,gender,...}]) - take the
        // first entry; fall back to flat fields if the server changes shape.
        val det = d.optJSONObject("characterDetail")
            ?: d.optJSONArray("characterDetail")?.optJSONObject(0)
            ?: d
        var avatar = cleanStr(d.optString("avatar"))
        if (avatar.isBlank()) {
            val race = det.optInt("race")
            val tribe = det.optInt("tribe")
            val gender = det.optInt("gender", -1)
            if (race > 0 && tribe > 0 && gender >= 0) {
                avatar = RACE_AVATAR_BASE + "$race-$tribe-$gender.jpg"
            }
        }
        android.util.Log.d("ShizhijiaLogin", "resolveAvatar uuid=$uuid -> ${avatar.take(90)}")
        if (avatar.isNotBlank()) avatarResolveCache[uuid] = avatar
        return avatar
    }

    private fun cleanStr(v: String?): String =
        v?.takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }?.trim().orEmpty()

    /**
     * Logged-in character info from GHome/isLogin (name / area / server + avatar).
     * When no custom avatar exists we fall back to the official per-race default
     * portrait built from groupAndRole/getCharacterBindInfo -> characterDetail:
     *   https://static.web.sdo.com/jijiamobile/pic/ff14/2023ffstone/{race}-{tribe}-{gender}.jpg
     * (gender: 0=male 1=female; race/tribe are global ids, all 32 combos exist).
     */
    suspend fun getLoginUser(context: Context): ShizhijiaLoginUser? {
        val d = data(context, HOME_BASE, "GHome/isLogin") ?: return null
        val name = d.optString("character_name")
        if (name.isBlank()) return null

        val tag = "ShizhijiaLogin"

        // Character detail carries race/tribe/gender; needed for the official
        // per-race default portrait. Raw request here so unexpected payload
        // shapes are visible in logcat while diagnosing.
        fun clean(v: String?): String =
            v?.takeUnless { it.isBlank() || it == "null" || it == "NULL" }?.trim().orEmpty()

        var detail: JSONObject? = null
        var bindAvatar = ""
        var bindUuid = ""
        runCatching {
            val bindJson = request(context, HOME_BASE, "groupAndRole/getCharacterBindInfo", mapOf("platform" to "2"))
            android.util.Log.d(
                tag,
                "bindInfo code=${bindJson?.optLong("code")} msg=${bindJson?.optString("msg")} body=${bindJson.toString().take(500)}",
            )
            val bind = bindJson?.takeIf { it.isOk() }?.optJSONObject("data")
            bindAvatar = clean(bind?.optString("avatar"))
            // uuid 在这里，不在 isLogin 里——官网 store 就是
            // `myuuid = parseInt(getCharacterBindInfo.data.uuid)`。
            bindUuid = clean(bind?.optString("uuid"))
            detail = bind?.optJSONObject("characterDetail")
                ?: bind?.optJSONArray("rows")?.optJSONObject(0)?.optJSONObject("characterDetail")
            android.util.Log.d(tag, "characterDetail=$detail bindAvatarLen=${bindAvatar.length} uuid=$bindUuid")
        }.onFailure { android.util.Log.d(tag, "bindInfo call failed: ${it.message}") }

        val infoAvatar = clean(runCatching { data(context, HOME_BASE, "userInfo/getUserInfo")?.optString("avatar").orEmpty() }.getOrDefault(""))
        val isLoginAvatar = clean(d.optString("avatar"))

        // Precedence: a REAL uploaded photo (http url) always wins; otherwise the
        // official per-race portrait beats the generic base64 placeholder; the
        // inline data-uri comes last before the letter fallback in the UI.
        fun pick(vararg candidates: String): String? =
            candidates.firstOrNull { it.startsWith("https://") || it.startsWith("http://") }

        val det = detail
        val racePortrait = if (det != null) {
            val race = det.optInt("race")
            val tribe = det.optInt("tribe")
            val gender = det.optInt("gender", -1)
            android.util.Log.d(tag, "race=$race tribe=$tribe gender=$gender")
            if (race > 0 && tribe > 0 && gender >= 0) RACE_AVATAR_BASE + "$race-$tribe-$gender.jpg" else ""
        } else ""

        val chosen = pick(bindAvatar, infoAvatar, isLoginAvatar)
            ?: racePortrait.takeIf { it.isNotBlank() }
            ?: listOf(bindAvatar, infoAvatar, isLoginAvatar).firstOrNull { it.startsWith("data:image") }
            ?: ""
        android.util.Log.d(tag, "avatar sources: bind=${bindAvatar.take(30)} info=${infoAvatar.take(30)} isLogin=${isLoginAvatar.take(30)} -> final=${chosen.take(90)}")
        // uuid 主要来自 getCharacterBindInfo；isLogin 和 characterDetail 只作兜底。
        val uuid = bindUuid
            .ifBlank { clean(d.optString("uuid")) }
            .ifBlank { clean(detail?.optString("uuid")) }
            .ifBlank { clean(runCatching { data(context, HOME_BASE, "userInfo/getUserInfo")?.optString("uuid").orEmpty() }.getOrDefault("")) }
        android.util.Log.d(tag, "resolved uuid=$uuid")
        return ShizhijiaLoginUser(name, d.optString("area_name"), d.optString("group_name"), chosen, uuid)
    }

    private const val RACE_AVATAR_BASE =
        "https://static.web.sdo.com/jijiamobile/pic/ff14/2023ffstone/"
}

/**
 * Basic identity of the logged-in character, for the top bar.
 *
 * uuid 是点自己头像进主页要用的——玩家主页接口按 uuid 取人，
 * 没有它就只能看别人的主页、看不了自己的。
 */
data class ShizhijiaLoginUser(
    val name: String,
    val area: String,
    val group: String,
    val avatar: String = "",
    val uuid: String = "",
) {
    fun toJson(): String = org.json.JSONObject().apply {
        put("name", name); put("area", area); put("group", group); put("avatar", avatar)
        put("uuid", uuid)
    }.toString()

    companion object {
        fun fromJson(o: org.json.JSONObject): ShizhijiaLoginUser = ShizhijiaLoginUser(
            name = o.optString("name"),
            area = o.optString("area"),
            group = o.optString("group"),
            avatar = o.optString("avatar"),
            uuid = o.optString("uuid"),
        )
    }
}