package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 专项数据的接口封装（对应官网 `/statistics` 那 7 屏）。
 *
 * 所有路径都在 `api/home/dataCenter/` 下，全部 GET，全部要登录+绑角色。
 * 大部分不带参数；带参数的那几个参数名见各方法注释（来自官网
 * dataCenter.DQhnWuhR.js 的包装函数）。
 *
 * 返回统一用 [ShizhijiaApi.Res]，这样界面能区分"没登录"、"没绑角色"和"真的没数据"。
 */
object ShizhijiaStatsApi {

    private const val BASE = "dataCenter/"

    /** 打一次接口，成功时把 `data` 当数组交给 [parse]。data 是裸数组。 */
    private suspend fun <T> arr(
        context: Context,
        path: String,
        params: Map<String, String> = emptyMap(),
        parse: (JSONArray?) -> T,
    ): ShizhijiaApi.Res<T> {
        val json = ShizhijiaApi.rawGet(context, BASE + path, params)
            ?: return ShizhijiaApi.Res.Failed(null, "网络请求失败")
        val code = json.optLong("code")
        if (code == 10000L || code == 10002L) {
            // data 可能是数组，也可能是 null（后端没数据时给 null 而不是 []）
            return ShizhijiaApi.Res.Ok(parse(json.optJSONArray("data")))
        }
        return when (code) {
            ShizhijiaApi.CODE_NEED_LOGIN -> ShizhijiaApi.Res.NeedLogin
            ShizhijiaApi.CODE_NEED_CHARACTER, 10104L -> ShizhijiaApi.Res.NeedCharacter
            else -> ShizhijiaApi.Res.Failed(code, json.optString("msg"))
        }
    }

    /** data 是对象的版本（只有 dataOpenStatus 是这样）。 */
    private suspend fun <T> obj(
        context: Context,
        path: String,
        parse: (JSONObject?) -> T,
    ): ShizhijiaApi.Res<T> {
        val json = ShizhijiaApi.rawGet(context, BASE + path)
            ?: return ShizhijiaApi.Res.Failed(null, "网络请求失败")
        val code = json.optLong("code")
        if (code == 10000L || code == 10002L) {
            return ShizhijiaApi.Res.Ok(parse(json.optJSONObject("data")))
        }
        return when (code) {
            ShizhijiaApi.CODE_NEED_LOGIN -> ShizhijiaApi.Res.NeedLogin
            ShizhijiaApi.CODE_NEED_CHARACTER, 10104L -> ShizhijiaApi.Res.NeedCharacter
            else -> ShizhijiaApi.Res.Failed(code, json.optString("msg"))
        }
    }

    // ---- 开放状态 ---------------------------------------------------------

    /** 哪几屏后端开了。7 个绝各自有开关，没开的别画进度。 */
    suspend fun openStatus(context: Context) =
        obj(context, "dataOpenStatus") { ShizhijiaDataOpen.fromJson(it) }

    // ---- 纷争前线 ---------------------------------------------------------

    /** 总览。一次返回多行，按 data_time 分 30days / total / 赛季。 */
    suspend fun pvpTotal(context: Context) =
        arr(context, "frontline1TotalNew") { ShizhijiaPvpTotal.fromArray(it) }

    suspend fun pvpWeek(context: Context) =
        arr(context, "frontline2WeekNew") { ShizhijiaPvpWeek.fromArray(it) }

    suspend fun pvpJobs(context: Context) =
        arr(context, "frontline3JobNew") { ShizhijiaPvpJob.fromArray(it) }

    suspend fun pvpBest(context: Context) =
        arr(context, "frontline4Best") { ShizhijiaPvpBest.fromArray(it) }

    suspend fun pvpMaps(context: Context) =
        arr(context, "frontline5Map") { ShizhijiaPvpMap.fromArray(it) }

    suspend fun pvpMapJobs(context: Context) =
        arr(context, "frontline6MapJob") { ShizhijiaPvpMapJob.fromArray(it) }

    /** 活动详情。实测返回空数组（当前没活动）。 */
    suspend fun pvpActive(context: Context) =
        arr(context, "frontlineActiveDetail") { a -> (0 until (a?.length() ?: 0)).map { a!!.opt(it).toString() } }

    // ---- 捕鱼人 -----------------------------------------------------------

    suspend fun fishTotal(context: Context) =
        arr(context, "fishTotal1") { ShizhijiaFishTotal.fromArray(it) }

    suspend fun fishNums(context: Context) =
        arr(context, "fishNum2") { ShizhijiaFishCount.fromArray(it, "fish_num") }

    suspend fun fishBaits(context: Context) =
        arr(context, "fishBait3") { ShizhijiaFishCount.fromArray(it, "bait_num") }

    suspend fun fishBigs(context: Context) =
        arr(context, "fishBig4") { ShizhijiaFishBig.fromArray(it) }

    suspend fun fishAchieves(context: Context) =
        arr(context, "fishAchieve5") { ShizhijiaFishAchieve.fromArray(it) }

    suspend fun fishSeas(context: Context) =
        arr(context, "fishSea6") { ShizhijiaFishSea.fromArray(it) }

    // ---- 投影外观 ---------------------------------------------------------

    suspend fun dressTotal(context: Context) =
        arr(context, "getDressTotal7") { ShizhijiaDressTotal.fromArray(it) }

    suspend fun dressRaces(context: Context) =
        arr(context, "getDressRace1") { ShizhijiaDressRace.fromArray(it) }

    suspend fun dressColors(context: Context) =
        arr(context, "getDressColor2") { ShizhijiaDressUse.fromArray(it, "catalog_id", "color_times") }

    suspend fun dressOrnaments(context: Context) =
        arr(context, "getDressOrnament3") { ShizhijiaDressUse.fromArray(it, "ornament", "ornament_times") }

    suspend fun dressVanities(context: Context) =
        arr(context, "getDressVanity4") { ShizhijiaDressVanity.fromArray(it) }

    suspend fun dressFullsets(context: Context) =
        arr(context, "getDressFullset5") { ShizhijiaDressFullset.fromArray(it) }

    // ---- 新月岛 -----------------------------------------------------------

    suspend fun mkdTotal(context: Context) =
        arr(context, "getMKDTotal1") { ShizhijiaMkdTotal.fromArray(it) }

    suspend fun mkdJobs(context: Context) =
        arr(context, "getMKDSupportJob2") { ShizhijiaMkdJob.fromArray(it) }

    suspend fun mkdItemUse(context: Context) =
        arr(context, "getMKDItemUse3") { ShizhijiaMkdItem.fromArray(it, "use_num") }

    suspend fun mkdItemGet(context: Context) =
        arr(context, "getMKDItemGet4") { ShizhijiaMkdItem.fromArray(it, "get_num") }

    suspend fun mkdBoxes(context: Context) =
        arr(context, "getMKDItemBox5") { ShizhijiaMkdBox.fromArray(it) }

    suspend fun mkdAchieves(context: Context) =
        arr(context, "getMKDAchieve7") { ShizhijiaFishAchieve.fromArray(it) }

    /** 光之加护。实测空数组。 */
    suspend fun mkdLights(context: Context) =
        arr(context, "getMKDLight8") { a -> (0 until (a?.length() ?: 0)).mapNotNull { a!!.optJSONObject(it) } }

    /**
     * 道具获取历史，要 `catalog_type`（分类名，比如"半魂晶"，取值来自
     * getMKDItemGet4 每行的 catalog_type）。一行一次，只带 log_time。
     */
    suspend fun mkdItemHistory(context: Context, catalogType: String) =
        arr(context, "getMKDIHistory6", mapOf("catalog_type" to catalogType)) {
            ShizhijiaItemLog.fromArray(it)
        }

    // ---- 零式 -------------------------------------------------------------

    suspend fun savageTotal(context: Context) =
        arr(context, "getLingShiTotal") { ShizhijiaSavageTotal.fromArray(it) }

    suspend fun savageClears(context: Context) =
        arr(context, "getLingShi") { ShizhijiaSavageClear.fromArray(it) }

    // ---- 绝境战 -----------------------------------------------------------

    /**
     * 7 个绝的首通情况：一次返回 7 个位置，没通的位置是 null。
     * 位置顺序和 [ShizhijiaUltimate] 的声明顺序一致（就是 medal_id 1..7）。
     */
    suspend fun ultimateFirsts(context: Context) =
        arr(context, "gaoNanFirst1") { a ->
            (0 until (a?.length() ?: 0)).map { ShizhijiaHardClear.fromJson(a?.optJSONObject(it)) }
        }

    /** 以下 5 个都要 territory_type，取 [ShizhijiaUltimate.territory]。 */
    private fun terr(territory: Int) = mapOf("territory_type" to territory.toString())

    suspend fun ultimateTeam(context: Context, territory: Int) =
        arr(context, "gaoNanTeam2", terr(territory)) { ShizhijiaTeamMember.fromArray(it) }

    suspend fun ultimateJob(context: Context, territory: Int) =
        arr(context, "gaoNanJob3", terr(territory)) { ShizhijiaJobTimes.fromArray(it) }

    suspend fun ultimateFriend(context: Context, territory: Int) =
        arr(context, "gaoNanFriend4", terr(territory)) { ShizhijiaFriendTimes.fromArray(it) }

    suspend fun ultimateDeadPoint(context: Context, territory: Int) =
        arr(context, "gaoNanDeadPoint5", terr(territory)) { ShizhijiaDeadPoint.fromArray(it) }

    /** 阶段推进。字段名只从视图代码看到 log_time，所以保留原始数组。 */
    suspend fun ultimatePhase(context: Context, territory: Int) =
        arr(context, "gaoNanPhase6", terr(territory)) { it }

    // ---- 朝圣交错路 -------------------------------------------------------
    // 官网只做了 dd4（朝圣交错路），老的三个深宫不在数据中心里。

    private val ddType = mapOf("dd_type" to ShizhijiaDeepDungeon.DD_TYPE)

    /** 层数进度，按 is_solo 分单人/组队。 */
    suspend fun ddProgress(context: Context) =
        arr(context, "getDDTerr1", ddType) { ShizhijiaDdProgress.fromArray(it) }

    suspend fun ddItems(context: Context) =
        arr(context, "getDDItem3", ddType) { ShizhijiaMkdItem.fromArray(it, "get_num") }

    suspend fun ddAchieves(context: Context) =
        arr(context, "getDDAchieve5", ddType) { ShizhijiaFishAchieve.fromArray(it) }

    suspend fun ddDeadPoints(context: Context) =
        arr(context, "getDDDeadPoint6", ddType) { ShizhijiaDeadPoint.fromArray(it) }

    /**
     * 道具获取历史。catalog_type 取 item / treasure（实测 item 有数据、
     * treasure 为空）。这个接口只有 log_time，没有 get_num——一行就是一次。
     */
    suspend fun ddHistory(context: Context, catalogType: String) =
        arr(
            context, "getDDHistory4",
            ddType + mapOf("catalog_type" to catalogType),
        ) { ShizhijiaItemLog.fromArray(it) }

    /** 高难（=朝圣交错路本体）通关信息。官网只取 data[0]。 */
    suspend fun ddHardClear(context: Context) =
        arr(context, "getDDGaoNan2", terr(ShizhijiaDeepDungeon.TERRITORY)) { a ->
            ShizhijiaHardClear.fromJson(a?.optJSONObject(0))
        }

    suspend fun ddFirstTeam(context: Context) =
        arr(context, "getDDFirstTeam7", terr(ShizhijiaDeepDungeon.TERRITORY)) {
            ShizhijiaTeamMember.fromArray(it)
        }
}
