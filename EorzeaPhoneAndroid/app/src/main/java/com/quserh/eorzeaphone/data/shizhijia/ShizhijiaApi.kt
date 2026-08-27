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
            if (body.isNotEmpty()) {
                conn.doOutput = true
                conn.outputStream.bufferedWriter().use { it.write(encodeParams(body)) }
            }
            read(conn)
        }.getOrNull()
    }

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

    /** 我收藏的帖子。需登录——未登录返回 Res.NeedLogin，界面据此区分空列表和未登录。 */
    suspend fun getMyStarPosts(context: Context, page: Int = 1, limit: Int = 15): Res<List<ShizhijiaPostCard>> =
        rowsRes(
            context, HOME_BASE, "userInfo/myStarPosts",
            mapOf("page" to page.toString(), "limit" to limit.toString()),
        ) { ShizhijiaPostCard.fromArray(it) }

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