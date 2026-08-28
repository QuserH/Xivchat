package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 石之家账号名册：一次拉回"和我有关系的、注册了石之家的人"，
 * 之后好友匹配直接查表，不用按名字一个个去搜。
 *
 * 为什么需要它：
 * 按名字搜（common/search）每个好友一次请求，还得靠服务器名去猜是不是同一个人，
 * 同名跨服的时候只能放弃。名册这条路直接给 uuid + 头像，准确且一次拉完。
 *
 * 数据来自两个接口（都要登录）：
 *
 * - `userRelation/getUnFollowFriend`：官网"推荐关注"那一栏。它给的是**还没关注**
 *   的人，每条带 `recommend_type` 标明来路（游戏好友 / 同部队 之类）。
 * - `userRelation/followList`：已经关注的人。
 *
 * 两个都要，因为 getUnFollowFriend 只含"未关注"——关注了之后那个人就从
 * 推荐里掉出去了，光靠它会漏掉所有已关注的好友。
 *
 * **没验证的地方**（拿到真实响应后要回来确认）：
 * 1. getUnFollowFriend 的每条**是否带 `group_name`**。官网那个卡片只渲染了
 *    头像 + 角色名 + 简介 + recommend_type，没渲染服务器，所以字段可能没返回。
 *    没有 group_name 时下面的 [match] 只在"名册里同名唯一"时才敢认。
 * 2. `recommend_type` 的具体取值。现在的做法是全部收进名册但把这个值原样存下来，
 *    匹配时不按它过滤——名册里多几个不是游戏好友的人不影响正确性
 *    （匹配的前提是角色名 + 原服都对上），只是白占点空间。
 *
 * 名册存进 prefs，重启不用重拉。头像 URL 也在里面，联系人列表可以直接用。
 */
object ShizhijiaFriendRoster {

    private const val PREFS = "eorzea_phone_shizhijia"
    private const val KEY_ROSTER = "friend_roster"
    private const val KEY_ROSTER_TIME = "friend_roster_time"
    private const val KEY_ROSTER_TRY = "friend_roster_last_try"

    /** 名册有效期。好友注册石之家不是高频事件，一天拉一次够了。 */
    private const val TTL_MS = 24L * 60 * 60 * 1000

    /**
     * 失败后的退避。没登录石之家时这两个接口必然失败，
     * 没有退避的话每次打开联系人都会白打一次请求。
     */
    private const val RETRY_MS = 10L * 60 * 1000

    /** 名册里的一条。 */
    data class Entry(
        val uuid: String,
        val name: String,
        val avatar: String,
        val groupName: String,
        val areaName: String,
        /** 来路标签（游戏好友 / 同部队 …）。只用于展示和排查，不参与匹配。 */
        val recommendType: String = "",
    ) {
        fun toJson(): JSONObject = JSONObject().apply {
            put("uuid", uuid)
            put("name", name)
            put("avatar", avatar)
            put("group", groupName)
            put("area", areaName)
            put("rt", recommendType)
        }

        /** 转成搜索结果的形状，好让 UI 层复用同一条路径。 */
        fun toSearchUser(): ShizhijiaSearchUser = ShizhijiaSearchUser(
            uuid = uuid,
            name = name,
            avatar = avatar,
            areaName = areaName,
            groupName = groupName,
            profile = "",
            fansNum = 0,
        )

        companion object {
            fun fromJson(o: JSONObject) = Entry(
                uuid = o.optString("uuid"),
                name = o.optString("name"),
                avatar = o.optString("avatar"),
                groupName = o.optString("group"),
                areaName = o.optString("area"),
                recommendType = o.optString("rt"),
            )

            /** 从接口返回的一条解析。字段缺了就是空串，不抛。 */
            fun fromApi(o: JSONObject) = Entry(
                uuid = o.optString("uuid"),
                name = o.optString("character_name"),
                avatar = cleanAvatar(o.optString("avatar")),
                groupName = o.optString("group_name"),
                areaName = o.optString("area_name"),
                recommendType = o.optString("recommend_type"),
            )
        }
    }

    /** 内存里的一份，省得每次匹配都读 prefs + 解析 JSON。 */
    @Volatile
    private var loaded: List<Entry>? = null

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 当前名册（可能是空的，表示还没拉过）。 */
    fun entries(context: Context): List<Entry> {
        loaded?.let { return it }
        val raw = prefs(context).getString(KEY_ROSTER, null)
        val list = if (raw.isNullOrBlank()) emptyList() else runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { add(Entry.fromJson(it)) }
            }
        }.getOrDefault(emptyList())
        loaded = list
        return list
    }

    /**
     * 该不该拉一次。
     * 成功过且没过期 → 不用；刚失败过（10 分钟内）→ 也先别，免得每次进联系人
     * 都白打一次请求（没登录时必然失败）。
     */
    fun isStale(context: Context): Boolean {
        val p = prefs(context)
        val ok = p.getLong(KEY_ROSTER_TIME, 0L)
        val now = System.currentTimeMillis()
        if (ok != 0L && now - ok <= TTL_MS) return false
        val tried = p.getLong(KEY_ROSTER_TRY, 0L)
        return tried == 0L || now - tried > RETRY_MS
    }

    private fun save(context: Context, list: List<Entry>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs(context).edit()
            .putString(KEY_ROSTER, arr.toString())
            .putLong(KEY_ROSTER_TIME, System.currentTimeMillis())
            .apply()
        loaded = list
    }

    /**
     * 在名册里找这个角色。
     *
     * [homeWorld] 是好友的**原服**——石之家的 group_name 也是原服，
     * 所以跨界传送不影响匹配。
     *
     * 判定顺序：
     * 1. 名字 + 原服都对上 → 就是他
     * 2. 名册里这条没有 group_name（接口可能不返回），且同名只有一个 → 认为是他
     * 3. 其他情况 → null（交给调用方去搜，或者报没找到）
     */
    fun match(context: Context, name: String, homeWorld: String): ShizhijiaSearchUser? {
        if (name.isBlank()) return null
        val sameName = entries(context).filter { it.name == name }
        if (sameName.isEmpty()) return null

        if (homeWorld.isNotBlank()) {
            sameName.firstOrNull { it.groupName == homeWorld }?.let { return it.toSearchUser() }
            // 名册里同名的都没有服务器信息 → 退到"同名唯一"判定
            if (sameName.any { it.groupName.isNotBlank() }) return null
        }
        return if (sameName.size == 1) sameName.first().toSearchUser() else null
    }

    /** 名册里这个角色的头像 URL，给联系人列表当头像用。没有就 null。 */
    fun avatarOf(context: Context, name: String, homeWorld: String): String? =
        match(context, name, homeWorld)?.avatar?.takeIf { it.isNotBlank() }

    /**
     * 拉一次名册。需要登录；没登录返回 [ShizhijiaApi.Res.NeedLogin]，
     * 调用方据此决定要不要提示。
     *
     * 两个接口分别拉，其中一个失败不影响另一个——能拿到一半也比没有好。
     */
    suspend fun refresh(context: Context): ShizhijiaApi.Res<Int> {
        // 先记下"试过了"，失败时靠它退避。
        prefs(context).edit().putLong(KEY_ROSTER_TRY, System.currentTimeMillis()).apply()
        val merged = LinkedHashMap<String, Entry>()
        var sawAuthFailure: ShizhijiaApi.Res<Int>? = null
        var anyOk = false

        for (source in listOf(
            // 未关注的推荐（含游戏好友）
            "userRelation/getUnFollowFriend" to emptyMap<String, String>(),
            // 已关注的人。limit 给大一点，一次拉完；石之家的 limit 上限未验证，
            // 200 是按其他列表接口的习惯取的。
            "userRelation/followList" to mapOf("page" to "1", "limit" to "200"),
        )) {
            val res = ShizhijiaApi.friendRosterPage(context, source.first, source.second)
            when (res) {
                is ShizhijiaApi.Res.Ok -> {
                    anyOk = true
                    res.value.forEach { e ->
                        if (e.uuid.isNotBlank() && e.name.isNotBlank()) merged[e.uuid] = e
                    }
                }
                is ShizhijiaApi.Res.NeedLogin -> sawAuthFailure = ShizhijiaApi.Res.NeedLogin
                is ShizhijiaApi.Res.NeedCharacter -> sawAuthFailure = ShizhijiaApi.Res.NeedCharacter
                is ShizhijiaApi.Res.Failed ->
                    if (sawAuthFailure == null) sawAuthFailure = ShizhijiaApi.Res.Failed(res.code, res.msg)
            }
        }

        if (!anyOk) return sawAuthFailure ?: ShizhijiaApi.Res.Failed(null, "名册没读到")

        val list = merged.values.toList()
        save(context, list)
        // 名册变了，之前"没找到"的结论可能已经过期。
        ShizhijiaFriendLink.clear()
        android.util.Log.d(
            "ShizhijiaRoster",
            "roster=${list.size} withServer=${list.count { it.groupName.isNotBlank() }} " +
                "types=${list.map { it.recommendType }.filter { it.isNotBlank() }.distinct()}",
        )
        return ShizhijiaApi.Res.Ok(list.size)
    }

    /** 退出登录时清掉：名册是账号相关的数据。 */
    fun clear(context: Context) {
        prefs(context).edit()
            .remove(KEY_ROSTER).remove(KEY_ROSTER_TIME).remove(KEY_ROSTER_TRY)
            .apply()
        loaded = null
    }
}
