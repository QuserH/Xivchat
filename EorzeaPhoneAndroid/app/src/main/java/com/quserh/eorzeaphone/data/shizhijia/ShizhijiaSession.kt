package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context
import android.webkit.CookieManager

/**
 * Session + cookie persistence for the 石之家 API.
 *
 * Authentication is a plain session cookie named `ff14risingstones` that the
 * SDO pass SDK sets after a successful WebView login. We keep that cookie in
 * local SharedPreferences and re-attach it to every API request via the
 * `Cookie` request header. Nothing is ever uploaded anywhere.
 */
object ShizhijiaSession {

    private const val PREFS = "eorzea_phone_shizhijia"
    private const val KEY_COOKIE = "session_cookie"
    private const val KEY_TIME = "login_time"
    private const val KEY_SIGN = "last_sign_date"
    private const val KEY_USER = "cached_login_user"
    private const val KEY_BAR_HEIGHT = "bottom_bar_height"
    private const val KEY_BAR_BOTTOM = "bottom_bar_bottom"
    private const val KEY_SEARCH_HISTORY = "search_history"

    /** Search history entries (keyword + channel type), newest first, capped at 10. */
    fun searchHistory(context: Context): List<Pair<String, Int>> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return runCatching {
            val arr = org.json.JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(o.optString("q") to o.optInt("t"))
                }
            }
        }.getOrDefault(emptyList()).filter { it.first.isNotBlank() }
    }

    fun addSearchHistory(context: Context, keyword: String, type: Int) {
        if (keyword.isBlank()) return
        val rest = searchHistory(context).filterNot { it.first == keyword && it.second == type }
        val next = (listOf(keyword to type) + rest).take(10)
        val arr = org.json.JSONArray()
        next.forEach { (q, t) -> arr.put(org.json.JSONObject().put("q", q).put("t", t)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply()
    }

    fun removeSearchHistory(context: Context, keyword: String, type: Int) {
        val next = searchHistory(context).filterNot { it.first == keyword && it.second == type }
        val arr = org.json.JSONArray()
        next.forEach { (q, t) -> arr.put(org.json.JSONObject().put("q", q).put("t", t)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply()
    }

    private const val COOKIE_HOST = "https://apiff14risingstones.web.sdo.com"
    const val LOGIN_COOKIE_NAME = "ff14risingstones"

    /** The SDO SSO entry url used by the in-app WebView login screen. */
    fun loginUrl(redirectUrl: String): String =
        "https://apiff14risingstones.web.sdo.com/api/home/GHome/login?redirectUrl=" +
            java.net.URLEncoder.encode(redirectUrl, "UTF-8")

    fun savedCookie(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_COOKIE, null)?.takeIf { it.isNotBlank() }
    }

    fun loginTime(context: Context): Long? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_TIME)) prefs.getLong(KEY_TIME, 0L) else null
    }

    /** Whether we hold a session cookie at all (even if it is stale server-side). */
    fun hasSession(context: Context): Boolean = savedCookie(context) != null

    fun save(context: Context, cookieString: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_COOKIE, cookieString)
            .putLong(KEY_TIME, System.currentTimeMillis())
            .apply()
    }

    fun clear(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_COOKIE).remove(KEY_TIME).apply()
    }

    /** Persist the resolved profile so the top bar renders instantly on next entry (no 已登录→昵称 flash). */
    fun cacheLoginUser(context: Context, user: ShizhijiaLoginUser) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER, user.toJson()).apply()
    }

    fun cachedLoginUser(context: Context): ShizhijiaLoginUser? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_USER, null)
            ?.takeIf { it.isNotBlank() }
            ?.let { raw -> runCatching { ShizhijiaLoginUser.fromJson(org.json.JSONObject(raw)) }.getOrNull() }

    fun clearCachedUser(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_USER).apply()
    }

    /** 悬浮底栏高度（dp），外观设置里可调。 */
    fun bottomBarHeight(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_BAR_HEIGHT, 56f)

    fun setBottomBarHeight(context: Context, heightDp: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_BAR_HEIGHT, heightDp).apply()
    }

    /** 悬浮底栏距离屏幕底部的间距（dp）。 */
    fun bottomBarBottom(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY_BAR_BOTTOM, 10f)

    fun setBottomBarBottom(context: Context, dp: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_BAR_BOTTOM, dp).apply()
    }

    /** yyyy-MM-dd of the last automatic check-in, so we only sign in once a day. */
    fun signDate(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SIGN, null)
    }

    fun setSignDate(context: Context, date: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SIGN, date).apply()
    }

    /**
     * Extract the **whole** cookie string for our host from the Android WebView
     * cookie jar after the SDO login flow finishes. The session is composed of
     * several cookies (ff14risingstones, web_guidid, CAS_LOGIN_STATE, ...), so
     * sending only the one session cookie is not enough - the server rejects an
     * incomplete set. Returning the full `; `-joined string mirrors a browser.
     */
    fun cookieFromWebView(): String? {
        val raw = runCatching { CookieManager.getInstance().getCookie(COOKIE_HOST) }.getOrNull()
        if (raw.isNullOrBlank()) return null
        return raw.trim().takeIf { it.isNotBlank() }
    }
}