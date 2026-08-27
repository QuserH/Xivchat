package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context

/**
 * 把游戏里的好友对上石之家账号。
 *
 * 石之家只有一个按关键词搜人的公开接口（common/search type=6），返回的每条带
 * `character_name` + `area_name`（大区）+ `group_name`（服务器）。游戏里的
 * 好友我们知道角色名和服务器名，所以匹配的依据就是这两样。
 *
 * 关键是**同名角色跨服存在**，所以宁可说"没找到"也不能猜：
 *
 * - 角色名和服务器都对上 → 就是他
 * - 服务器读不到（好友离线时 world 可能是空的）且同名只有一个 → 认为是他
 * - 服务器读不到但同名有好几个 → 不猜，当没找到
 * - 服务器读到了但没有一条同服 → 这人没注册（或没绑这个角色）
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

    /** 查过的结果缓存一下，同一个好友来回进出详情不重复打接口。 */
    private val cache = mutableMapOf<String, Result>()

    private fun key(name: String, world: String) = "$name@$world"

    /** 已经查过就直接给结果，用来避免界面闪一下"正在查"。 */
    fun peek(name: String, world: String): Result? = cache[key(name, world)]

    /**
     * 按角色名 + 服务器名找石之家账号。
     *
     * @param name 游戏里的角色名
     * @param world 服务器名（[PhoneFriend.world]，可能是空的）
     */
    suspend fun find(context: Context, name: String, world: String): Result {
        if (name.isBlank()) return Result.NotFound
        val k = key(name, world)
        cache[k]?.let { return it }

        val hits = runCatching { ShizhijiaApi.searchUsers(context, name, limit = 30) }
            .getOrElse { return Result.Failed }

        // 搜索是模糊匹配（搜"猫"能出"会后空翻的猫"），所以先掐成完全同名。
        val exact = hits.filter { it.name == name }
        val result = when {
            exact.isEmpty() -> Result.NotFound
            world.isNotBlank() -> {
                // 服务器名对得上才算。对不上说明这人没注册，或者注册的是别的角色。
                exact.firstOrNull { it.groupName == world }
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
