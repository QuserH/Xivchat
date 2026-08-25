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