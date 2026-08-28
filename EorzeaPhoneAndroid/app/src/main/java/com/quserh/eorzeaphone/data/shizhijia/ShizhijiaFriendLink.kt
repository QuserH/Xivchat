package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context

/**
 * 把游戏里的好友对上石之家账号。
 *
 * 石之家的公开搜人接口（common/search type=6）返回的每条带
 * `character_name` + `area_name`（大区）+ `group_name`（服务器）。
 * 这里的 `group_name` 是**角色的原服**——角色数据绑在原服上，不会因为
 * 这个人跨界传送了就变。
 *
 * 所以匹配必须拿好友的**原服**（[PhoneFriend.homeWorld]）去比，
 * 不能拿当前所在服（[PhoneFriend.world]）：
 * 好友跨界跑到别的服时 world 是访问服，和石之家的 group_name 必然对不上，
 * 结果把明明注册了的人报成"没注册"。这是之前的 bug。
 *
 * 同名角色跨服存在，所以宁可说"没找到"也不猜：
 *
 * - 原服对上 → 就是他
 * - 原服读不到（离线好友可能没有 homeWorld），但当前服对上了 → 也算他
 *   （当前服 == 原服的情况占多数，聊胜于无）
 * - 两个服都读不到、同名只有一个 → 认为是他
 * - 两个服都读不到、同名有好几个 → 不猜，报 Ambiguous
 * - 服务器读到了但没有一条对得上 → 这人没注册（或没绑这个角色）
 *
 * 搜索接口不需要登录，所以没登录石之家也能用这个功能。
 */
object ShizhijiaFriendLink {

    /** 一次查询的结果。 */
    sealed interface Result {
        /** 找到了，可以跳主页。 */
        data class Found(val user: ShizhijiaSearchUser) : Result
        /** 搜过了，确实没有。 */
        data object NotFound : Result
        /**
         * 同名的有好几个但分不清是哪个（好友的服务器名读不到时会这样）。
         * 界面上按"没找到"处理，但文案可以说得更准。
         */
        data class Ambiguous(val candidates: List<ShizhijiaSearchUser>) : Result
        /** 请求没成功，和"没注册"不是一回事，别给用户看成没注册。 */
        data object Failed : Result
    }

    /**
     * 查过的结果缓存一下，同一个好友来回进出详情不重复打接口。
     * key 用"角色名@原服"——同一个人不管当前在哪个服，缓存都命中同一条。
     */
    private val cache = mutableMapOf<String, Result>()

    private fun key(name: String, homeWorld: String) = "$name@$homeWorld"

    /** 已经查过就直接给结果，用来避免界面闪一下"正在查"。 */
    fun peek(friend: PhoneFriendKey): Result? = cache[key(friend.name, friend.homeWorld)]

    /**
     * 匹配需要的那两个字段。不直接依赖 ui 层的 PhoneFriend，
     * data 层不该反向引用 ui。
     */
    data class PhoneFriendKey(
        val name: String,
        /** 原服。角色数据绑在原服上，跨界传送不改这个。 */
        val homeWorld: String,
        /** 当前所在服。只在原服读不到时当兜底。 */
        val currentWorld: String = "",
    )

    /**
     * 按角色名 + 原服找石之家账号。
     *
     * @param friend 角色名 + 原服（+ 当前服兜底）
     */
    suspend fun find(context: Context, friend: PhoneFriendKey): Result {
        val name = friend.name
        if (name.isBlank()) return Result.NotFound
        val k = key(name, friend.homeWorld)
        cache[k]?.let { return it }

        // 名册里有就不用搜（名册是登录后拉的"和我有关系的、注册了石之家的人"，
        // 直接给 uuid + 头像，比按名字搜准得多也快得多）。
        // 首次读名册要解析一次 JSON，这里是从 LaunchedEffect（主线程）调进来的，
        // 所以放到 IO 上。
        val fromRoster = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            ShizhijiaFriendRoster.match(context, name, friend.homeWorld)
        }
        if (fromRoster != null) {
            val hit = Result.Found(fromRoster)
            cache[k] = hit
            return hit
        }

        val hits = runCatching { ShizhijiaApi.searchUsers(context, name, limit = 30) }
            .getOrElse { return Result.Failed }

        // 搜索是模糊匹配（搜"猫"能出"会后空翻的猫"），所以先掐成完全同名。
        val exact = hits.filter { it.name == name }
        // 原服优先，当前服兜底。两个都空就只能靠"同名唯一"。
        val servers = listOf(friend.homeWorld, friend.currentWorld).filter { it.isNotBlank() }
        val result = when {
            exact.isEmpty() -> Result.NotFound
            servers.isNotEmpty() -> {
                // 按 servers 的顺序找第一个对得上的：原服命中优先于当前服命中。
                servers.firstNotNullOfOrNull { s -> exact.firstOrNull { it.groupName == s } }
                    ?.let { Result.Found(it) }
                    ?: Result.NotFound
            }
            exact.size == 1 -> Result.Found(exact.first())
            else -> Result.Ambiguous(exact)
        }
        cache[k] = result
        return result
    }

    /** 好友头像/资料变了时清掉，避免一直用旧结论。 */
    fun clear() = cache.clear()
}
