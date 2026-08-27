package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 一次性的接口形状探测器：专项数据（dataCenter/）那 43 个接口的字段名不在
 * 前端 bundle 里，官网只有压缩后的访问代码，所以只能拿一次真实响应来看。
 *
 * 为什么要跑在 App 里而不是在 PC 上用 curl：那些接口要登录态，而会话 cookie
 * 是**按域**存的（接口域 apiff14risingstones 和页面域 ff14risingstones 是两个
 * cookie jar），从浏览器地址栏那一侧复制出来的值对接口域一律 10105 请重新登录。
 * App 里的会话本来就是接口域的，直接用它最省事，也不用把 cookie 复制到任何
 * 别的地方去。
 *
 * 输出只有字段名、类型、和截断到 24 字符的示例值；[REDACT] 里的键连示例都不出。
 * 报告落在 getExternalFilesDir()，用 adb pull 取走。
 */
object ShizhijiaProbe {

    private const val TAG = "SzjProbe"
    private const val SAMPLE_MAX = 24
    private const val ROWS_KEPT = 2

    /** 43 个接口，路径来自 pc/static/js/dataCenter.DQhnWuhR.js。 */
    private val ENDPOINTS = listOf(
        "dataOpenStatus",
        // 纷争前线
        "frontline1TotalNew", "frontline2WeekNew", "frontline3JobNew",
        "frontline4Best", "frontline5Map", "frontline6MapJob", "frontlineActiveDetail",
        // 捕鱼人
        "fishTotal1", "fishNum2", "fishBait3", "fishBig4", "fishAchieve5", "fishSea6",
        // 绝境战（高难）
        "gaoNanFirst1", "gaoNanTeam2", "gaoNanJob3", "gaoNanFriend4",
        "gaoNanDeadPoint5", "gaoNanPhase6",
        // 朝圣交错路（深层迷宫）
        "getDDTerr1", "getDDGaoNan2", "getDDItem3", "getDDHistory4",
        "getDDAchieve5", "getDDDeadPoint6", "getDDFirstTeam7",
        // 投影外观
        "getDressRace1", "getDressColor2", "getDressOrnament3", "getDressVanity4",
        "getDressFullset5", "getDressTotal7",
        // 新月岛
        "getMKDTotal1", "getMKDSupportJob2", "getMKDItemUse3", "getMKDItemGet4",
        "getMKDItemBox5", "getMKDIHistory6", "getMKDAchieve7", "getMKDLight8",
        // 零式
        "getLingShi", "getLingShiTotal",
    )

    /**
     * 要带参数的那 13 个。第一轮全部 10003 参数必填，参数名在
     * dataCenter.DQhnWuhR.js 的包装函数里，取值在各自的 view chunk 里：
     *
     * - 绝境战 territory_type：7 个绝的 instanceId（Ultimate.js 的 te 枚举）
     * - 朝圣交错路 dd_type 是字符串 "dd4"，territory_type 是 "1311"
     *   （DeepDungeon.js 只有 dd4 一个，老的三个深宫不在数据中心里）
     * - getDDHistory4 还要 catalog_type，取 item / treasure
     * - getMKDIHistory6 的 catalog_type 用新月岛道具分类名（getMKDItemGet4
     *   里的 catalog_type，比如"半魂晶"）
     */
    private val ULTIMATE_IDS = listOf("733", "777", "887", "968", "1122", "1238", "1363")
    private const val DD_TYPE = "dd4"
    private const val DD_TERRITORY = "1311"

    private fun paramEndpoints(): List<Pair<String, Map<String, String>>> {
        val out = mutableListOf<Pair<String, Map<String, String>>>()
        // 绝境战：先只探第一个绝，形状一样就够了；全 7 个会打 35 次没必要。
        val t = ULTIMATE_IDS.first()
        listOf("gaoNanTeam2", "gaoNanJob3", "gaoNanFriend4", "gaoNanDeadPoint5", "gaoNanPhase6")
            .forEach { out += it to mapOf("territory_type" to t) }
        // 顺带把 7 个绝的通关记录各打一次，看哪几个有数据。
        ULTIMATE_IDS.drop(1).forEach { out += "gaoNanTeam2" to mapOf("territory_type" to it) }
        listOf("getDDTerr1", "getDDItem3", "getDDAchieve5", "getDDDeadPoint6")
            .forEach { out += it to mapOf("dd_type" to DD_TYPE) }
        out += "getDDHistory4" to mapOf("dd_type" to DD_TYPE, "catalog_type" to "item")
        out += "getDDHistory4" to mapOf("dd_type" to DD_TYPE, "catalog_type" to "treasure")
        out += "getDDGaoNan2" to mapOf("territory_type" to DD_TERRITORY)
        out += "getDDFirstTeam7" to mapOf("territory_type" to DD_TERRITORY)
        out += "getMKDIHistory6" to mapOf("catalog_type" to "半魂晶")
        return out
    }

    /** 这些键名只报类型，不报示例值——凭证和账号标识不该出现在报告里。 */
    private val REDACT = listOf(
        "cookie", "token", "session", "ticket", "sign", "secret",
        "password", "passwd", "phone", "mobile", "mail", "idcard",
        "sndaid", "sdid", "guid", "uuid", "openid", "accesskey",
    )

    private fun redacted(key: String): Boolean {
        val k = key.lowercase()
        return REDACT.any { k.contains(it) }
    }

    /** 把标量折叠成 `类型(示例)`；数组只留前两行，深层结构照样展开。 */
    private fun shape(key: String, v: Any?, indent: Int, sb: StringBuilder) {
        val pad = "  ".repeat(indent)
        when (v) {
            null, JSONObject.NULL -> sb.append("$pad$key: null\n")
            is JSONObject -> {
                sb.append("$pad$key: {\n")
                v.keys().asSequence().sorted().forEach { shape(it, v.opt(it), indent + 1, sb) }
                sb.append("$pad}\n")
            }
            is JSONArray -> {
                sb.append("$pad$key: [${v.length()} x\n")
                if (v.length() == 0) sb.append("$pad  (empty)\n")
                for (i in 0 until minOf(v.length(), ROWS_KEPT)) {
                    shape("[$i]", v.opt(i), indent + 1, sb)
                }
                if (v.length() > ROWS_KEPT) sb.append("$pad  ... 其余 ${v.length() - ROWS_KEPT} 行同构\n")
                sb.append("$pad]\n")
            }
            is Boolean -> sb.append("$pad$key: bool($v)\n")
            is Int, is Long, is Double -> sb.append("$pad$key: number($v)\n")
            else -> {
                val s = v.toString()
                if (redacted(key)) {
                    sb.append("$pad$key: string(len=${s.length}, 已隐去)\n")
                } else {
                    val cut = if (s.length > SAMPLE_MAX) s.take(SAMPLE_MAX) + "…" else s
                    sb.append("$pad$key: string(\"${cut.replace("\n", "\\n")}\")\n")
                }
            }
        }
    }

    /**
     * 依次打 43 个接口，把形状写进 `外部文件目录/szj_datacenter_shape.txt`。
     * 返回给界面看的一行摘要。[onProgress] 每打完一个报一次，用来显示进度。
     */
    suspend fun runDataCenterProbe(
        context: Context,
        onProgress: (done: Int, total: Int, path: String) -> Unit = { _, _, _ -> },
    ): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.append("石之家 dataCenter 形状报告\n")
        sb.append("生成时间: ${java.util.Date()}\n")
        sb.append("说明: 只记字段名/类型/24 字符内示例；敏感键已隐去。\n")
        sb.append("=".repeat(60)).append("\n\n")

        var ok = 0
        var fail = 0
        val jobs = ENDPOINTS.map { it to emptyMap<String, String>() } + paramEndpoints()
        jobs.forEachIndexed { i, (ep, params) ->
            val json = ShizhijiaApi.rawGet(context, "dataCenter/$ep", params)
            val label = if (params.isEmpty()) ep
            else ep + params.entries.joinToString(",", "?", "") { "${it.key}=${it.value}" }
            sb.append("### $label\n")
            if (json == null) {
                fail++
                sb.append("请求失败（网络层，无响应体）\n\n")
            } else {
                val code = json.optLong("code")
                val msg = json.optString("msg")
                sb.append("code=$code msg=$msg\n")
                if (code == 10000L || code == 10002L) {
                    ok++
                    shape("data", json.opt("data"), 0, sb)
                } else {
                    fail++
                }
                sb.append("\n")
            }
            onProgress(i + 1, jobs.size, label)
            Log.i(TAG, "[${i + 1}/${jobs.size}] $label -> ${json?.optLong("code") ?: "no-response"}")
        }

        val summary = "ok=$ok fail=$fail / ${jobs.size}"
        sb.append("=".repeat(60)).append("\n").append(summary).append("\n")

        val dir = context.getExternalFilesDir(null)
        val out = File(dir, "szj_datacenter_shape.txt")
        runCatching { out.writeText(sb.toString()) }
            .onFailure { Log.e(TAG, "写报告失败", it) }
        Log.i(TAG, "报告已写入 ${out.absolutePath} ($summary)")
        "$summary\n${out.absolutePath}"
    }
}
