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
    suspend fun getPostParts(context: Context): List<ShizhijiaPostPart> {
        val d = data(context, HOME_BASE, "posts/partList", mapOf("type" to "1")) ?: return emptyList()
        val arr = d.optJSONArray("rows") ?: d.optJSONArray("list") ?: return emptyList()
        return buildList(arr.length()) { for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(ShizhijiaPostPart.fromJson(it)) } }
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
    suspend fun searchGlamours(context: Context, keywords: String, limit: Int = 20): List<ShizhijiaSearchGlamour> {
        if (keywords.isBlank()) return emptyList()
        val d = data(context, MAIN, "common/search", mapOf("type" to SEARCH_TYPE_GLAMOUR.toString(), "keywords" to keywords, "limit" to limit.toString()))
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
        runCatching {
            val bindJson = request(context, HOME_BASE, "groupAndRole/getCharacterBindInfo", mapOf("platform" to "2"))
            android.util.Log.d(
                tag,
                "bindInfo code=${bindJson?.optLong("code")} msg=${bindJson?.optString("msg")} body=${bindJson.toString().take(500)}",
            )
            val bind = bindJson?.takeIf { it.isOk() }?.optJSONObject("data")
            bindAvatar = clean(bind?.optString("avatar"))
            detail = bind?.optJSONObject("characterDetail")
                ?: bind?.optJSONArray("rows")?.optJSONObject(0)?.optJSONObject("characterDetail")
            android.util.Log.d(tag, "characterDetail=$detail bindAvatarLen=${bindAvatar.length}")
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
        return ShizhijiaLoginUser(name, d.optString("area_name"), d.optString("group_name"), chosen)
    }

    private const val RACE_AVATAR_BASE =
        "https://static.web.sdo.com/jijiamobile/pic/ff14/2023ffstone/"
}

/** Basic identity of the logged-in character, for the top bar. */
data class ShizhijiaLoginUser(val name: String, val area: String, val group: String, val avatar: String = "") {
    fun toJson(): String = org.json.JSONObject().apply {
        put("name", name); put("area", area); put("group", group); put("avatar", avatar)
    }.toString()

    companion object {
        fun fromJson(o: org.json.JSONObject): ShizhijiaLoginUser = ShizhijiaLoginUser(
            name = o.optString("name"),
            area = o.optString("area"),
            group = o.optString("group"),
            avatar = o.optString("avatar"),
        )
    }
}