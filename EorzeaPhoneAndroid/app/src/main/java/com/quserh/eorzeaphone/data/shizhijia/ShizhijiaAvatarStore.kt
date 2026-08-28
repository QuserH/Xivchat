package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context
import org.json.JSONObject

/**
 * 好友的石之家头像。
 *
 * 为什么要有这一层：名册（[ShizhijiaFriendRoster]）只装两拨人——
 * "推荐关注里未关注的"和"我已关注的"。一个注册了石之家、但既不在推荐里
 * 也没被我关注的好友，名册里根本没有他，于是 findEntry 返回 null，
 * 头像退回随机贴纸。这就是"有些好友明明有石之家主页，头像却没应用"的原因。
 *
 * [ShizhijiaFriendLink.find] 早就有完整的两级查找（名册 → 按名字搜），
 * 但头像那条路只查了名册。这里把两者接上，并且把结论**落盘**：
 * 一个好友最多问一次，空结果也记下来（记成"查过，没有"），
 * 不然每次进联系人都要为同一批人重新搜一遍。
 *
 * 图片本身走 [ShizhijiaImageLoader] 的内存 + 磁盘缓存，这里只存 URL。
 */
object ShizhijiaAvatarStore {

    private const val PREFS = "szj_friend_avatar"
    private const val KEY_MAP = "avatars"
    /** 记成"查过、确实没有"的标记。空串没法区分"没查过"和"查了没有"。 */
    private const val NONE = "-"

    @Volatile
    private var loaded: MutableMap<String, String>? = null

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun key(name: String, homeWorld: String) = "$name@$homeWorld"

    private fun map(context: Context): MutableMap<String, String> {
        loaded?.let { return it }
        val m = mutableMapOf<String, String>()
        runCatching {
            val o = JSONObject(prefs(context).getString(KEY_MAP, "{}").orEmpty())
            val it = o.keys()
            while (it.hasNext()) {
                val k = it.next()
                m[k] = o.getString(k)
            }
        }
        loaded = m
        return m
    }

    private fun put(context: Context, name: String, homeWorld: String, value: String) {
        val m = map(context)
        m[key(name, homeWorld)] = value.ifBlank { NONE }
        runCatching { prefs(context).edit().putString(KEY_MAP, JSONObject(m as Map<*, *>).toString()).apply() }
    }

    /**
     * 已知结果。
     * - 非空字符串 → 头像 URL
     * - 空字符串   → 查过了，这人没有石之家（别再查）
     * - null       → 还没查过
     */
    fun peek(context: Context, name: String, homeWorld: String): String? =
        map(context)[key(name, homeWorld)]?.let { if (it == NONE) "" else it }

    /**
     * 查一个好友的石之家头像。已经查过就直接返回，不打接口。
     *
     * 顺序：名册（免费，本地表）→ 按名字搜（一次网络请求，[ShizhijiaFriendLink]
     * 内部还有一层内存缓存）。拿到 uuid 但没头像时，按种族/部族/性别拼官方立绘。
     *
     * 要在 IO 线程上调。
     */
    suspend fun resolve(
        context: Context,
        name: String,
        homeWorld: String,
        currentWorld: String = "",
    ): String {
        if (name.isBlank()) return ""
        peek(context, name, homeWorld)?.let { return it }

        // 1. 名册
        ShizhijiaFriendRoster.findEntry(context, name, homeWorld.ifBlank { currentWorld })?.let { e ->
            val url = ShizhijiaFriendRoster.resolveAvatar(context, e)
            put(context, name, homeWorld, url)
            return url
        }

        // 2. 按名字搜。搜不到 / 同名分不清 / 请求失败，三种情况区别对待：
        //    只有"确实没有"才记成 NONE，失败不记——下次还要再试。
        val res = ShizhijiaFriendLink.find(
            context,
            ShizhijiaFriendLink.PhoneFriendKey(name, homeWorld, currentWorld),
        )
        return when (res) {
            is ShizhijiaFriendLink.Result.Found -> {
                val user = res.user
                val url = user.avatar.ifBlank {
                    if (user.uuid.isNotBlank()) {
                        runCatching { ShizhijiaApi.resolveAvatar(context, user.uuid) }.getOrDefault("")
                    } else ""
                }
                put(context, name, homeWorld, url)
                url
            }
            is ShizhijiaFriendLink.Result.NotFound,
            is ShizhijiaFriendLink.Result.Ambiguous -> {
                put(context, name, homeWorld, "")
                ""
            }
            is ShizhijiaFriendLink.Result.Failed -> ""
        }
    }

    /** 退登 / 手动刷新时清掉。 */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_MAP).apply()
        loaded = null
    }
}
