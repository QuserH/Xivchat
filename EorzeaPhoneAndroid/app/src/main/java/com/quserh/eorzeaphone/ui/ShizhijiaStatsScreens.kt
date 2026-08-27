package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaApi
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaImageLoader
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDataOpen
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDdProgress
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDeadPoint
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDeepDungeon
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaFriendTimes
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaHardClear
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaItemLog
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaJobTimes
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaTeamMember
import com.quserh.eorzeaphone.data.shizhijia.fmtElapsed
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDressDicts
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDressFullset
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaIcons
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDressRace
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDressTotal
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDressUse
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDressVanity
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaFishAchieve
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaFishBig
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaFishCount
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaFishSea
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaFishTotal
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaMkdBox
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaMkdItem
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaMkdJob
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaMkdTotal
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPvpBest
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPvpJob
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPvpMap
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPvpTotal
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPvpWeek
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSavageClear
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSavageTable
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSavageTotal
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaStatsApi
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaUltimate

/**
 * 专项数据（官网「数据中心」/statistics）。
 *
 * 七个分类和官网导航同序：纷争前线 / 绝境战 / 捕鱼人 / 零式 / 投影外观 /
 * 新月岛 / 朝圣交错路。数据全部要登录 + 绑角色，所以空态一律走
 * [SzjResState]，让"没登录"和"真的没打过"分开。
 *
 * 接口一共 43 个，字段名不在官网前端代码里，是用 ShizhijiaProbe 打一次真实
 * 响应抓出来的；后端所有数值都是字符串，解析见 ShizhijiaStats.kt。
 */

private const val STAT_PVP = 0
private const val STAT_ULTIMATE = 1
private const val STAT_FISH = 2
private const val STAT_SAVAGE = 3
private const val STAT_DRESS = 4
private const val STAT_MKD = 5
private const val STAT_DD = 6

private val STAT_TABS = listOf(
    "纷争前线", "绝境战", "捕鱼人", "零式", "投影外观", "新月岛", "朝圣交错路",
)

@Composable
internal fun ShizhijiaStatisticsScreen(pop: () -> Unit, onLogin: () -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(STAT_PVP) }
    var open by remember { mutableStateOf<ShizhijiaDataOpen?>(null) }

    // 开放状态决定绝境战那一屏画不画进度，先拉一次，失败也不挡别的分类。
    LaunchedEffect(Unit) {
        open = (ShizhijiaStatsApi.openStatus(context) as? ShizhijiaApi.Res.Ok)?.value
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("专项数据", onBack = pop)
        SzjStatTabRow(tab) { tab = it }
        when (tab) {
            STAT_PVP -> SzjPvpPane(onLogin)
            STAT_ULTIMATE -> SzjUltimatePane(open, onLogin)
            STAT_FISH -> SzjFishPane(onLogin)
            STAT_SAVAGE -> SzjSavagePane(onLogin)
            STAT_DRESS -> SzjDressPane(onLogin)
            STAT_MKD -> SzjMkdPane(onLogin)
            STAT_DD -> SzjDeepDungeonPane(onLogin)
        }
    }
}

/** 七个分类横向滚动。七个标签在手机上塞不进一行，硬塞会挤成两字一个。 */
@Composable
private fun SzjStatTabRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        STAT_TABS.forEachIndexed { i, label ->
            val on = i == selected
            SzjPressable(onClick = { onSelect(i) }, shape = SzjChipShape) {
                Row(
                    Modifier.clip(SzjChipShape)
                        .background(if (on) SzjAccentSoft else Color.Transparent)
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (on) {
                        SzjShard(widthDp = 3, heightDp = 12)
                        Spacer(Modifier.width(5.dp))
                    }
                    Text(
                        label,
                        color = if (on) SzjOnAccentSoft else SzjMuted,
                        style = SzjLabelStyle,
                        maxLines = 1,
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
}

// ---------------------------------------------------------------------------
// 通用小件
// ---------------------------------------------------------------------------

/** 分区小标题：棱条 + 文字。比分割线省地方，也把签名元素带进每一段。 */
@Composable
private fun SzjStatSection(title: String, hint: String? = null) {
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SzjShard(widthDp = 3, heightDp = 15)
            Spacer(Modifier.width(7.dp))
            Text(title, color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.4.sp)
        }
        if (hint != null) {
            Spacer(Modifier.height(4.dp))
            Text(hint, color = SzjMuted, style = SzjMetaStyle, lineHeight = 16.sp)
        }
    }
}

/** 一格数字：大数字压小标签。专项数据整屏都是这个节奏。 */
@Composable
private fun SzjStatCell(label: String, value: String, modifier: Modifier = Modifier, accent: Boolean = false) {
    Column(modifier.padding(vertical = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = if (accent) SzjAccent else SzjText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(label, color = SzjMuted, style = SzjMetaStyle, maxLines = 1)
    }
}

/** 一行数字网格，自动按 n 列均分。 */
@Composable
private fun SzjStatGrid(cells: List<Pair<String, String>>, columns: Int = 3) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cells.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                row.forEach { (l, v) -> SzjStatCell(l, v, Modifier.weight(1f)) }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * 百分位条：石之家的 rank 字段是 0-100 的百分位（越大越前）。
 * 用条形比光写数字直观，条长直接就是百分位。
 */
@Composable
private fun SzjRankBar(label: String, percent: Double) {
    val p = (percent / 100.0).coerceIn(0.0, 1.0)
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = SzjMuted, style = SzjMetaStyle, modifier = Modifier.weight(1f))
            Text(fmt1(percent) + "%", color = SzjAccent, style = SzjLabelStyle)
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(SzjCardRaised)) {
            Box(Modifier.fillMaxWidth(p.toFloat()).height(5.dp).clip(RoundedCornerShape(3.dp)).background(SzjAccent))
        }
    }
}

/** 一行"名字 …… 数值"，右边可再挂一个次要值。 */
@Composable
private fun SzjStatRow(
    name: String,
    value: String,
    meta: String? = null,
    rank: Int? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (rank != null) {
            Text(
                "$rank",
                color = if (rank <= 3) SzjAccent else SzjMuted,
                style = SzjLabelStyle,
                modifier = Modifier.width(24.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(name, color = SzjText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (meta != null) {
                Spacer(Modifier.height(2.dp))
                Text(meta, color = SzjMuted, style = SzjMetaStyle, maxLines = 1)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(value, color = SzjAccent, style = SzjLabelStyle)
    }
}

/** 大数字格式化：14796 -> 1.48万，避免长数字把格子挤破。 */
private fun fmtNum(v: Double): String = when {
    v >= 100_000_000 -> fmt2(v / 100_000_000) + "亿"
    v >= 10_000 -> fmt2(v / 10_000) + "万"
    v == v.toLong().toDouble() -> v.toLong().toString()
    else -> fmt1(v)
}

private fun fmt1(v: Double): String = String.format("%.1f", v)
private fun fmt2(v: Double): String = String.format("%.2f", v)
private fun pct(v: Double): String = String.format("%.1f%%", v * 100)

/** 分类内部的一行状态：正在读 / 空 / 出错，全部走 SzjResState 的说法。 */
@Composable
private fun <T> SzjStatState(res: ShizhijiaApi.Res<T>?, emptyTitle: String, emptyHint: String?, onLogin: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
        SzjResState(res = res, emptyTitle = emptyTitle, emptyHint = emptyHint, onLogin = onLogin, inline = true)
    }
}

/** 读取中的一圈。 */
@Composable
private fun SzjStatLoading() {
    Box(Modifier.fillMaxWidth().padding(vertical = 34.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
    }
}

/** 卡片包一段内容，统一左右留白。各分类里反复用。 */
@Composable
private fun SzjStatCard(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        SzjCardSurface(Modifier.fillMaxWidth()) { Box(Modifier.padding(14.dp)) { content() } }
    }
}

/** 卡片包一列行（行自带内边距，所以这里不再加）。 */
@Composable
private fun SzjStatListCard(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        SzjCardSurface(Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 4.dp)) { content() } }
    }
}

// ---------------------------------------------------------------------------
// 纷争前线
// ---------------------------------------------------------------------------

/**
 * 纷争前线。frontline1TotalNew 一次返回多行，用 data_time 区分统计窗口
 * （30days / total / 赛季值），所以顶上给一个窗口切换而不是分别请求。
 */
@Composable
private fun SzjPvpPane(onLogin: () -> Unit) {
    val context = LocalContext.current
    var totals by remember { mutableStateOf<ShizhijiaApi.Res<List<ShizhijiaPvpTotal>>?>(null) }
    var week by remember { mutableStateOf<List<ShizhijiaPvpWeek>>(emptyList()) }
    var jobs by remember { mutableStateOf<List<ShizhijiaPvpJob>>(emptyList()) }
    var best by remember { mutableStateOf<List<ShizhijiaPvpBest>>(emptyList()) }
    var maps by remember { mutableStateOf<List<ShizhijiaPvpMap>>(emptyList()) }
    var window by remember { mutableStateOf("total") }

    LaunchedEffect(Unit) {
        totals = ShizhijiaStatsApi.pvpTotal(context)
        week = (ShizhijiaStatsApi.pvpWeek(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        jobs = (ShizhijiaStatsApi.pvpJobs(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        best = (ShizhijiaStatsApi.pvpBest(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        maps = (ShizhijiaStatsApi.pvpMaps(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
    }

    val all = (totals as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
    if (all.isEmpty()) {
        if (totals == null) SzjStatLoading()
        else SzjStatState(totals, "还没有纷争前线记录", "打过一场之后，这里会有胜率、KDA 和百分位排名", onLogin)
        return
    }
    val cur = all.find { it.dataTime == window } ?: all.first()
    // 窗口选项按后端实际返回的来，别硬编码——赛季那一行的值是变的。
    val windows = all.map { it.dataTime }.distinct()

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item("window") {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                windows.forEach { w ->
                    val on = w == window
                    SzjPressable(onClick = { window = w }, shape = SzjChipShape) {
                        Text(
                            pvpWindowLabel(w),
                            color = if (on) SzjOnAccentSoft else SzjMuted,
                            style = SzjLabelStyle,
                            modifier = Modifier.clip(SzjChipShape)
                                .background(if (on) SzjAccentSoft else SzjCardRaised)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
        }
        item("overall") {
            SzjStatSection("总览", "军队 ${cur.gcId.ifBlank { "未知" }} · 系列 ${cur.seriesLevel} 级 · 阶级 ${cur.pvpRank}")
            SzjStatCard {
                Column {
                    SzjStatGrid(listOf(
                        "场次" to cur.fightTimes.toString(),
                        "胜场" to cur.winTimes.toString(),
                        "胜率" to pct(cur.winRate),
                        "击杀" to cur.killTimes.toString(),
                        "阵亡" to cur.deadTimes.toString(),
                        "助攻" to cur.assistTimes.toString(),
                    ))
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SzjStatCell("KDA", fmt2(cur.kda), Modifier.weight(1f), accent = true)
                        SzjStatCell("场均击杀", fmt1(cur.avgKill), Modifier.weight(1f))
                        SzjStatCell("场均阵亡", fmt1(cur.avgDead), Modifier.weight(1f))
                        SzjStatCell("场均助攻", fmt1(cur.avgAssist), Modifier.weight(1f))
                    }
                    if (cur.occupyCount > 0 || cur.clearTime > 0) {
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth()) {
                            SzjStatCell("据点占领", cur.occupyCount.toString(), Modifier.weight(1f))
                            SzjStatCell("通关次数", cur.clearTime.toString(), Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        item("avg") {
            SzjStatSection("场均输出")
            SzjStatCard {
                SzjStatGrid(listOf(
                    "伤害" to fmtNum(cur.avgDamage),
                    "承伤" to fmtNum(cur.avgDamaged),
                    "治疗" to fmtNum(cur.avgHeal),
                ))
            }
        }
        item("rank") {
            SzjStatSection("百分位", "在同服玩家里的位置，越靠右越前")
            SzjStatCard {
                Column {
                    SzjRankBar("击杀", cur.killRank)
                    SzjRankBar("助攻", cur.assistRank)
                    SzjRankBar("伤害", cur.damageRank)
                    SzjRankBar("承伤", cur.damagedRank)
                    SzjRankBar("治疗", cur.healRank)
                    SzjRankBar("生存", cur.deadRank)
                }
            }
        }
        if (best.isNotEmpty()) {
            item("best_h") { SzjStatSection("单场最佳", "每一项各取历史最好的那一场") }
            items(best, key = { it.bestType }) { b ->
                Box(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    SzjCardSurface(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                                Text(b.label, color = SzjMuted, style = SzjLabelStyle, modifier = Modifier.weight(1f))
                                Text(fmtNum(b.value), color = SzjAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${b.territory} · ${b.career} · 第 ${b.resultRank} 名",
                                color = SzjText, fontSize = 12.sp,
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(b.logTime, color = SzjMuted, style = SzjMetaStyle)
                        }
                    }
                }
            }
        }
        if (maps.isNotEmpty()) {
            item("maps_h") { SzjStatSection("按地图") }
            item("maps") {
                SzjStatListCard {
                    maps.sortedByDescending { it.fightTimes }.forEach { m ->
                        SzjStatRow(
                            m.territory,
                            pct(m.winRate),
                            meta = "${m.fightTimes} 场 · 胜 ${m.winTimes} · 击杀 ${fmt1(m.killTimes)}",
                        )
                    }
                }
            }
        }
        val jobsForWindow = jobs.filter { it.dataTime == window }.sortedByDescending { it.times }
        if (jobsForWindow.isNotEmpty()) {
            item("jobs_h") { SzjStatSection("按职业", "出场占比 · 胜率 · KDA") }
            item("jobs") {
                SzjStatListCard {
                    jobsForWindow.forEach { j ->
                        SzjStatRow(
                            j.jobName.ifBlank { "职业 ${j.career}" },
                            pct(j.winRate),
                            meta = "${j.times} 场 · 占比 ${pct(j.useRate)} · KDA ${fmt2(j.kda)} · LB ${j.lbTimes}",
                        )
                    }
                }
            }
        }
        if (week.isNotEmpty()) {
            item("week_h") { SzjStatSection("本周") }
            item("week") {
                SzjStatListCard {
                    week.forEach { w ->
                        SzjStatRow(
                            w.territory,
                            "${w.winTimes}/${w.fightTimes}",
                            meta = "${w.partDate} · K${w.killTimes} D${w.deadTimes} A${w.assistTimes} · KDA ${fmt2(w.kda)}",
                        )
                    }
                }
            }
        }
    }
}

private fun pvpWindowLabel(w: String): String = when (w) {
    "30days" -> "近 30 天"
    "total" -> "总计"
    else -> w
}

// ---------------------------------------------------------------------------
// 捕鱼人
// ---------------------------------------------------------------------------

/**
 * 捕鱼人。鱼种和鱼饵各有一百多条，全平铺会滑不到底，所以默认只出前 20，
 * 点"全部"再展开。
 */
@Composable
private fun SzjFishPane(onLogin: () -> Unit) {
    val context = LocalContext.current
    var total by remember { mutableStateOf<ShizhijiaApi.Res<ShizhijiaFishTotal?>?>(null) }
    var nums by remember { mutableStateOf<List<ShizhijiaFishCount>>(emptyList()) }
    var baits by remember { mutableStateOf<List<ShizhijiaFishCount>>(emptyList()) }
    var bigs by remember { mutableStateOf<List<ShizhijiaFishBig>>(emptyList()) }
    var achieves by remember { mutableStateOf<List<ShizhijiaFishAchieve>>(emptyList()) }
    var seas by remember { mutableStateOf<List<ShizhijiaFishSea>>(emptyList()) }
    var allNums by remember { mutableStateOf(false) }
    var allBaits by remember { mutableStateOf(false) }
    var allBigs by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        total = ShizhijiaStatsApi.fishTotal(context)
        nums = (ShizhijiaStatsApi.fishNums(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        baits = (ShizhijiaStatsApi.fishBaits(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        bigs = (ShizhijiaStatsApi.fishBigs(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        achieves = (ShizhijiaStatsApi.fishAchieves(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        seas = (ShizhijiaStatsApi.fishSeas(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
    }

    val t = (total as? ShizhijiaApi.Res.Ok)?.value
    if (t == null) {
        if (total == null) SzjStatLoading()
        else SzjStatState(total, "还没有捕鱼记录", "钓过鱼之后，这里会有鱼种、鱼饵和出海成绩", onLogin)
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item("total") {
            SzjStatSection("总览")
            SzjStatCard {
                SzjStatGrid(listOf(
                    "钓获总数" to fmtNum(t.totalTimes.toDouble()),
                    "成功率" to pct(t.succRate),
                    "出海次数" to t.seaTimes.toString(),
                    "出海最高分" to fmtNum(t.maxSeaScore.toDouble()),
                ), columns = 2)
            }
        }
        if (seas.isNotEmpty()) {
            item("sea_h") { SzjStatSection("出海垂钓", "按航线") }
            item("sea") {
                SzjStatListCard {
                    seas.sortedByDescending { it.maxSeaScore }.forEach { s ->
                        SzjStatRow(s.label, fmtNum(s.maxSeaScore.toDouble()), meta = "${s.seaTimes} 次 · 最高分")
                    }
                }
            }
        }
        if (nums.isNotEmpty()) {
            item("nums_h") {
                SzjStatSection("钓获最多的鱼", "共 ${nums.size} 种")
            }
            item("nums") {
                SzjStatListCard {
                    val sorted = nums.sortedByDescending { it.num }
                    (if (allNums) sorted else sorted.take(20)).forEachIndexed { i, f ->
                        SzjStatRow(f.name, "${f.num}", meta = f.type, rank = i + 1)
                    }
                    if (nums.size > 20) SzjStatMoreRow(allNums, nums.size) { allNums = !allNums }
                }
            }
        }
        if (baits.isNotEmpty()) {
            item("baits_h") { SzjStatSection("用得最多的鱼饵", "共 ${baits.size} 种") }
            item("baits") {
                SzjStatListCard {
                    val sorted = baits.sortedByDescending { it.num }
                    (if (allBaits) sorted else sorted.take(20)).forEachIndexed { i, f ->
                        SzjStatRow(f.name, "${f.num}", meta = f.type, rank = i + 1)
                    }
                    if (baits.size > 20) SzjStatMoreRow(allBaits, baits.size) { allBaits = !allBaits }
                }
            }
        }
        if (bigs.isNotEmpty()) {
            item("bigs_h") { SzjStatSection("大鱼首次钓获", "共 ${bigs.size} 条") }
            item("bigs") {
                SzjStatListCard {
                    val sorted = bigs.sortedByDescending { it.logTime }
                    (if (allBigs) sorted else sorted.take(20)).forEach { b ->
                        SzjStatRow(b.name, b.version, meta = b.logTime)
                    }
                    if (bigs.size > 20) SzjStatMoreRow(allBigs, bigs.size) { allBigs = !allBigs }
                }
            }
        }
        if (achieves.isNotEmpty()) {
            item("ach_h") { SzjStatSection("钓鱼成就") }
            items(achieves, key = { it.id }) { a ->
                Box(Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                    SzjCardSurface(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(a.name, color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Text(a.logTime.take(10), color = SzjMuted, style = SzjMetaStyle)
                            }
                            if (a.detail.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(a.detail, color = SzjMuted, style = SzjMetaStyle, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 列表底部的"展开全部 / 收起"。长表默认截断，别让人滑一百多行。 */
@Composable
private fun SzjStatMoreRow(expanded: Boolean, total: Int, onToggle: () -> Unit) {
    SzjPressable(onClick = onToggle, modifier = Modifier.fillMaxWidth(), shape = SzjInnerShape) {
        Text(
            if (expanded) "收起" else "展开全部 $total 条",
            color = SzjAccent,
            style = SzjLabelStyle,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// 零式
// ---------------------------------------------------------------------------

/**
 * 零式。getLingShi 只给 territory_type（副本 id），名字要靠
 * [ShizhijiaSavageTable]（抄自官网静态副本表）翻。
 *
 * `no_limit` = 1 是"解除限制"通关，也就是超出当期的碾压，和当期击杀分开标。
 */
@Composable
private fun SzjSavagePane(onLogin: () -> Unit) {
    val context = LocalContext.current
    var total by remember { mutableStateOf<ShizhijiaApi.Res<ShizhijiaSavageTotal?>?>(null) }
    var clears by remember { mutableStateOf<List<ShizhijiaSavageClear>>(emptyList()) }

    LaunchedEffect(Unit) {
        total = ShizhijiaStatsApi.savageTotal(context)
        clears = (ShizhijiaStatsApi.savageClears(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
    }

    val t = (total as? ShizhijiaApi.Res.Ok)?.value
    if (t == null) {
        if (total == null) SzjStatLoading()
        else SzjStatState(total, "还没有零式记录", "通关任一零式副本之后，这里会显示进度", onLogin)
        return
    }
    // id -> 通关记录，用来在副本表上打勾。
    val byId = clears.associateBy { it.territory.toIntOrNull() ?: -1 }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item("total") {
            SzjStatSection("总览")
            SzjStatCard {
                SzjStatGrid(listOf(
                    "进入次数" to t.enterNum.toString(),
                    "通关次数" to t.finishTimes.toString(),
                    "通关副本" to t.territoryNum.toString(),
                    "用时" to t.elapsedTime.toString(),
                ), columns = 2)
            }
        }
        ShizhijiaSavageTable.EXPANSIONS.forEach { exp ->
            val ids = exp.tiers.flatMap { it.raids.map { r -> r.second } }
            val done = ids.count { byId.containsKey(it) }
            if (done == 0) return@forEach
            item("exp_${exp.name}") {
                SzjStatSection(exp.name, "$done / ${ids.size} 关")
            }
            item("tiers_${exp.name}") {
                SzjStatListCard {
                    exp.tiers.forEach { tier ->
                        SzjSavageTierRow(tier.name, tier.raids, byId)
                    }
                }
            }
        }
        // 副本表里没有的 id（新副本上线但表还没更新）也要显示，不能悄悄丢。
        val known = ShizhijiaSavageTable.NAMES.keys
        val unknown = clears.filter { (it.territory.toIntOrNull() ?: -1) !in known }
        if (unknown.isNotEmpty()) {
            item("unknown_h") { SzjStatSection("其他通关记录", "副本表里还没有这几个 id") }
            item("unknown") {
                SzjStatListCard {
                    unknown.forEach { c ->
                        SzjStatRow(
                            ShizhijiaSavageTable.name(c.territory),
                            if (c.noLimit) "解限" else "通关",
                            meta = c.logTime,
                        )
                    }
                }
            }
        }
    }
}

/** 一个 tier 一行：tier 名 + 四个关卡的小方块，通了的填色。 */
@Composable
private fun SzjSavageTierRow(
    tier: String,
    raids: List<Pair<String, Int>>,
    byId: Map<Int, ShizhijiaSavageClear>,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 9.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(tier, color = SzjText, fontSize = 13.sp, modifier = Modifier.weight(1f))
            val done = raids.count { byId.containsKey(it.second) }
            Text("$done/${raids.size}", color = if (done == raids.size) SzjAccent else SzjMuted, style = SzjLabelStyle)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            raids.forEach { (name, id) ->
                val c = byId[id]
                val on = c != null
                Column(
                    Modifier.weight(1f).clip(SzjChipShape)
                        .background(if (on) SzjAccentSoft else SzjCardRaised)
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // 关卡名末尾的数字就是层数，取它当方块标签。
                    Text(
                        name.takeLast(1),
                        color = if (on) SzjOnAccentSoft else SzjMuted,
                        style = SzjLabelStyle,
                    )
                    if (c != null && c.noLimit) {
                        Spacer(Modifier.height(2.dp))
                        Text("解限", color = SzjOnAccentSoft, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 投影外观
// ---------------------------------------------------------------------------

/**
 * 投影外观。getDressVanity4 是少见的自带 Name/Icon 的接口，所以这一屏能出图；
 * 染色和配饰只给 id，官网自己也只显示色块/编号，这里保持一致显示编号。
 */
@Composable
private fun SzjDressPane(onLogin: () -> Unit) {
    val context = LocalContext.current
    var total by remember { mutableStateOf<ShizhijiaApi.Res<ShizhijiaDressTotal?>?>(null) }
    var races by remember { mutableStateOf<List<ShizhijiaDressRace>>(emptyList()) }
    var vanities by remember { mutableStateOf<List<ShizhijiaDressVanity>>(emptyList()) }
    var colors by remember { mutableStateOf<List<ShizhijiaDressUse>>(emptyList()) }
    var ornaments by remember { mutableStateOf<List<ShizhijiaDressUse>>(emptyList()) }
    var fullsets by remember { mutableStateOf<List<ShizhijiaDressFullset>>(emptyList()) }
    var allVanity by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        total = ShizhijiaStatsApi.dressTotal(context)
        races = (ShizhijiaStatsApi.dressRaces(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        vanities = (ShizhijiaStatsApi.dressVanities(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        colors = (ShizhijiaStatsApi.dressColors(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        ornaments = (ShizhijiaStatsApi.dressOrnaments(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        fullsets = (ShizhijiaStatsApi.dressFullsets(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
    }

    val t = (total as? ShizhijiaApi.Res.Ok)?.value
    if (t == null) {
        if (total == null) SzjStatLoading()
        else SzjStatState(total, "还没有投影记录", "换过投影之后，这里会有部件、染色和套装统计", onLogin)
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item("total") {
            SzjStatSection("总览")
            SzjStatCard {
                SzjStatGrid(listOf(
                    "投影次数" to fmtNum(t.vanityTimes.toDouble()),
                    "染色次数" to fmtNum(t.colorTimes.toDouble()),
                    "套装" to t.setNum.toString(),
                    "漂白" to t.washingNum.toString(),
                ), columns = 2)
            }
        }
        if (races.isNotEmpty()) {
            item("race_h") { SzjStatSection("种族外观", "按连续使用天数") }
            item("race") {
                SzjStatListCard {
                    races.sortedByDescending { it.continueDays }.forEach { r ->
                        SzjStatRow(
                            "${r.race} · ${r.gender}",
                            "${r.continueDays} 天",
                            meta = "自 ${r.beginDate} · 占比 ${pct(r.continueRate)}",
                        )
                    }
                }
            }
        }
        if (vanities.isNotEmpty()) {
            item("vanity_h") { SzjStatSection("最常用的部件", "共 ${vanities.size} 件") }
            item("vanity") {
                SzjStatListCard {
                    val sorted = vanities.sortedByDescending { it.times }
                    (if (allVanity) sorted else sorted.take(20)).forEachIndexed { i, v ->
                        SzjDressVanityRow(v, i + 1)
                    }
                    if (vanities.size > 20) SzjStatMoreRow(allVanity, vanities.size) { allVanity = !allVanity }
                }
            }
        }
        if (colors.isNotEmpty()) {
            item("color_h") { SzjStatSection("常用染色") }
            item("color") {
                SzjStatListCard {
                    colors.sortedByDescending { it.times }.forEach { c ->
                        SzjDressColorRow(c)
                    }
                }
            }
        }
        if (ornaments.isNotEmpty()) {
            item("orn_h") { SzjStatSection("常用时尚配饰") }
            item("orn") {
                SzjStatListCard {
                    ornaments.sortedByDescending { it.times }.forEach { o ->
                        SzjDressOrnamentRow(o)
                    }
                }
            }
        }
        if (fullsets.isNotEmpty()) {
            item("set_h") { SzjStatSection("套装记录", "共 ${fullsets.size} 套") }
            item("set") {
                SzjStatListCard {
                    fullsets.sortedByDescending { it.logTime }.take(20).forEach { s ->
                        SzjStatRow(
                            "套装 ${s.setItem}",
                            "${s.partItems.size} 件",
                            meta = s.logTime,
                        )
                    }
                }
            }
        }
    }
}

/** 部件一行：图标 + 名字 + 次数。这个接口带 Icon，所以能出真图。 */
@Composable
private fun SzjDressVanityRow(v: ShizhijiaDressVanity, rank: Int) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$rank",
            color = if (rank <= 3) SzjAccent else SzjMuted,
            style = SzjLabelStyle,
            modifier = Modifier.width(24.dp),
        )
        SzjItemIcon(v.iconUrl, v.name)
        Spacer(Modifier.width(10.dp))
        Text(
            v.name.ifBlank { "部件 ${v.itemId}" },
            color = SzjText, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text("${v.times} 次", color = SzjAccent, style = SzjLabelStyle)
    }
}

/**
 * 染色一行：真实色块 + 染色名。
 *
 * 接口只给 catalog_id，名字和 RGB 来自 [ShizhijiaDressDicts]（抄的官网前端
 * 那张 stain 表）。金属色标一下，因为同名的金属/非金属看起来一样。
 */
@Composable
private fun SzjDressColorRow(c: ShizhijiaDressUse) {
    val stain = ShizhijiaDressDicts.stain(c.id)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${c.rank}",
            color = if (c.rank <= 3) SzjAccent else SzjMuted,
            style = SzjLabelStyle,
            modifier = Modifier.width(24.dp),
        )
        // rgb 是 0xRRGGBB，补上不透明 alpha。
        val swatch = Color(0xFF000000L.or((stain?.rgb ?: 0).toLong()).toInt())
        Box(
            Modifier.size(18.dp).clip(RoundedCornerShape(5.dp))
                .background(swatch)
                .border(1.dp, SzjHairline, RoundedCornerShape(5.dp)),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stain?.label?.ifBlank { "染色 ${c.id}" } ?: "染色 ${c.id}",
                color = SzjText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (stain?.metallic == true) {
                Spacer(Modifier.height(2.dp))
                Text("金属色", color = SzjMuted, style = SzjMetaStyle)
            }
        }
        Spacer(Modifier.width(8.dp))
        Text("${c.times} 次", color = SzjAccent, style = SzjLabelStyle)
    }
}

/** 配饰一行：图标 + 名字。id 到名字同样来自官网前端那张 ornament 表。 */
@Composable
private fun SzjDressOrnamentRow(o: ShizhijiaDressUse) {
    val hit = ShizhijiaDressDicts.ornament(o.id)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "${o.rank}",
            color = if (o.rank <= 3) SzjAccent else SzjMuted,
            style = SzjLabelStyle,
            modifier = Modifier.width(24.dp),
        )
        if (hit != null && hit.second > 0) SzjItemIcon(ShizhijiaIcons.item(hit.second), hit.first)
        else Box(Modifier.size(34.dp).clip(SzjChipShape).background(SzjCardRaised))
        Spacer(Modifier.width(10.dp))
        Text(
            hit?.first?.ifBlank { "配饰 ${o.id}" } ?: "配饰 ${o.id}",
            color = SzjText, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text("${o.times} 次", color = SzjAccent, style = SzjLabelStyle)
    }
}

/**
 * 道具图标：34dp 小方块。项目里没有 Coil，图片走
 * [ShizhijiaImageLoader]（带内存缓存），和幻化封面同一条路径。
 */
@Composable
private fun SzjItemIcon(url: String, desc: String) {
    val context = LocalContext.current
    var bmp by remember(url) { mutableStateOf<android.graphics.Bitmap?>(ShizhijiaImageLoader.peek(url)) }
    LaunchedEffect(url) { if (bmp == null) bmp = ShizhijiaImageLoader.load(context, url) }
    Box(Modifier.size(34.dp).clip(SzjChipShape).background(SzjCardRaised)) {
        val b = bmp
        if (b != null) {
            Image(
                b.asImageBitmap(),
                contentDescription = desc,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 新月岛
// ---------------------------------------------------------------------------

/**
 * 新月岛（后端叫 MKD）。临时职业只给 support_job 的数字 id，用招募那边的
 * 职业字典翻名字（getJobConfigList 是公开接口，招募页本来就要拉）。
 */
@Composable
private fun SzjMkdPane(onLogin: () -> Unit) {
    val context = LocalContext.current
    var total by remember { mutableStateOf<ShizhijiaApi.Res<ShizhijiaMkdTotal?>?>(null) }
    var jobs by remember { mutableStateOf<List<ShizhijiaMkdJob>>(emptyList()) }
    var gets by remember { mutableStateOf<List<ShizhijiaMkdItem>>(emptyList()) }
    var uses by remember { mutableStateOf<List<ShizhijiaMkdItem>>(emptyList()) }
    var boxes by remember { mutableStateOf<List<ShizhijiaMkdBox>>(emptyList()) }
    var jobNames by remember { mutableStateOf(mapOf<String, String>()) }

    LaunchedEffect(Unit) {
        total = ShizhijiaStatsApi.mkdTotal(context)
        jobs = (ShizhijiaStatsApi.mkdJobs(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        gets = (ShizhijiaStatsApi.mkdItemGet(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        uses = (ShizhijiaStatsApi.mkdItemUse(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        boxes = (ShizhijiaStatsApi.mkdBoxes(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        jobNames = ShizhijiaApi.getJobConfig(context).mapValues { it.value.name }
    }

    val t = (total as? ShizhijiaApi.Res.Ok)?.value
    if (t == null) {
        if (total == null) SzjStatLoading()
        else SzjStatState(total, "还没有新月岛记录", "上过新月岛之后，这里会有等级、幻卡币和道具统计", onLogin)
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item("total") {
            SzjStatSection("总览", "当前等级 ${t.nowLevel}")
            SzjStatCard {
                Column {
                    SzjStatGrid(listOf(
                        "危命任务" to t.ceTimes.toString(),
                        "同盟标记" to t.fateTimes.toString(),
                    ), columns = 2)
                    Spacer(Modifier.height(14.dp))
                    SzjStatGrid(listOf(
                        "银币" to fmtNum(t.silverNum.toDouble()),
                        "金币" to fmtNum(t.goldNum.toDouble()),
                        "白银币" to fmtNum(t.whiteSilverNum.toDouble()),
                        "白金币" to fmtNum(t.whiteGoldNum.toDouble()),
                    ), columns = 2)
                }
            }
        }
        if (jobs.isNotEmpty()) {
            item("job_h") { SzjStatSection("临时职业等级") }
            item("job") {
                SzjStatListCard {
                    jobs.sortedByDescending { it.nowLevel }.forEach { j ->
                        SzjStatRow(
                            jobNames[j.supportJob] ?: "职业 ${j.supportJob}",
                            "Lv ${j.nowLevel}",
                        )
                    }
                }
            }
        }
        if (boxes.isNotEmpty()) {
            item("box_h") { SzjStatSection("宝箱") }
            item("box") {
                SzjStatListCard {
                    boxes.sortedByDescending { it.num }.forEach { b ->
                        SzjStatRow("${b.boxType} · ${b.levelLabel}", "${b.num} 个")
                    }
                }
            }
        }
        if (gets.isNotEmpty()) {
            // 按 catalog_type 分组，点分类可以展开该分类的获取记录
            // （getMKDIHistory6 就是按 catalog_type 查的）。
            val byType = gets.groupBy { it.type }
            item("get_h") { SzjStatSection("道具获取", "共 ${gets.size} 种，点分类看获取记录") }
            byType.forEach { (type, list) ->
                item("get_$type") {
                    SzjMkdCategoryCard(type, list)
                }
            }
        }
        if (uses.isNotEmpty()) {
            item("use_h") { SzjStatSection("道具使用") }
            item("use") {
                SzjStatListCard {
                    uses.sortedByDescending { it.num }.forEach { m ->
                        SzjStatRow(m.name, "${m.num}", meta = m.type)
                    }
                }
            }
        }
    }
}

/**
 * 新月岛的一个道具分类。点开拉该分类的获取记录（getMKDIHistory6 的
 * catalog_type 就是这个分类名，实测"半魂晶"能返回 16 条）。
 */
@Composable
private fun SzjMkdCategoryCard(type: String, list: List<ShizhijiaMkdItem>) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<ShizhijiaItemLog>?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(open) {
        if (open && history == null && !loading) {
            loading = true
            history = (ShizhijiaStatsApi.mkdItemHistory(context, type) as? ShizhijiaApi.Res.Ok)
                ?.value.orEmpty()
            loading = false
        }
    }

    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
        SzjCardSurface(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 4.dp)) {
                SzjPressable(onClick = { open = !open }, modifier = Modifier.fillMaxWidth(), shape = SzjInnerShape) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SzjShard(widthDp = 3, heightDp = 13)
                        Spacer(Modifier.width(7.dp))
                        Text(
                            type.ifBlank { "未分类" },
                            color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        Text("${list.size} 种", color = SzjMuted, style = SzjMetaStyle)
                        Spacer(Modifier.width(8.dp))
                        Text(if (open) "收起" else "记录 ›", color = SzjAccent, style = SzjLabelStyle)
                    }
                }
                list.sortedByDescending { it.num }.forEach { m ->
                    SzjStatRow(m.name, "${m.num}", meta = "首次 ${m.firstTime.take(10)}")
                }
                if (open) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 6.dp)) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
                    }
                    when {
                        loading -> SzjStatLoading()
                        history.isNullOrEmpty() -> Text(
                            "这个分类没有获取记录",
                            color = SzjMuted, style = SzjMetaStyle,
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                        )
                        else -> history!!.sortedByDescending { it.logTime }.take(30).forEach { h ->
                            SzjStatRow(h.name, h.logTime.take(10), meta = h.logTime.drop(11).ifBlank { null })
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 绝境战
// ---------------------------------------------------------------------------

/**
 * 绝境战。gaoNanFirst1 返回 7 个位置（对应 7 个绝），没打过的位置是 null，
 * 所以这里按位置对齐到 [ShizhijiaUltimate]，而不是按返回条数。
 *
 * 明细（队伍/职业/队友/团灭点/阶段）都要 territory_type，且只有通了才有内容，
 * 所以先只在通了的绝上拉明细。
 */
@Composable
private fun SzjUltimatePane(open: ShizhijiaDataOpen?, onLogin: () -> Unit) {
    val context = LocalContext.current
    var firsts by remember { mutableStateOf<ShizhijiaApi.Res<List<ShizhijiaHardClear?>>?>(null) }
    // 展开哪一个绝的明细。明细要 territory_type，一次只拉一个。
    var picked by remember { mutableStateOf<ShizhijiaUltimate?>(null) }

    LaunchedEffect(Unit) { firsts = ShizhijiaStatsApi.ultimateFirsts(context) }

    val rows = (firsts as? ShizhijiaApi.Res.Ok)?.value
    if (rows == null) {
        if (firsts == null) SzjStatLoading()
        else SzjStatState(firsts, "还没有绝境战记录", "通关任一绝境战之后，这里会显示首通信息", onLogin)
        return
    }

    val ults = ShizhijiaUltimate.entries
    val clearedCount = ults.indices.count { rows.getOrNull(it) != null }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item("head") {
            SzjStatSection("首通进度", "$clearedCount / ${ults.size} 个绝")
        }
        item("list") {
            SzjStatListCard {
                ults.forEachIndexed { i, u ->
                    val row = rows.getOrNull(i)
                    val opened = open?.ultimates?.getOrNull(i) ?: true
                    SzjUltimateRow(
                        u, row, opened,
                        expanded = picked == u,
                        onClick = if (row != null) ({ picked = if (picked == u) null else u }) else null,
                    )
                }
            }
        }
        picked?.let { u ->
            item("detail_${u.territory}") { SzjUltimateDetail(u) }
        }
        if (clearedCount == 0) {
            item("empty") {
                Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    SzjStatCard {
                        Text(
                            "还没有通关记录。队伍构成、团灭点和阶段推进都要通关之后才有数据。" +
                                "另外官网自己也说明：巴哈姆特绝境战因为原始数据缺失，不展示进度记录。",
                            color = SzjMuted, style = SzjMetaStyle, lineHeight = 17.sp,
                        )
                    }
                }
            }
        }
    }
}

/** 一个绝一行。通了的可以点开看明细。 */
@Composable
private fun SzjUltimateRow(
    u: ShizhijiaUltimate,
    row: ShizhijiaHardClear?,
    opened: Boolean,
    expanded: Boolean,
    onClick: (() -> Unit)?,
) {
    val done = row != null
    val content: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(8.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (done) SzjAccent else SzjCardRaised),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(u.label, color = SzjText, fontSize = 13.sp)
                val meta = when {
                    done -> row?.logTime?.takeIf { it.isNotBlank() }?.let { "首通 ${it.take(10)}" } ?: "已通关"
                    !opened -> "后端还没开放这个绝的数据"
                    else -> "未通关"
                }
                Spacer(Modifier.height(2.dp))
                Text(meta, color = SzjMuted, style = SzjMetaStyle)
            }
            Text(
                if (done) (if (expanded) "收起" else "明细 ›") else "—",
                color = if (done) SzjAccent else SzjMuted,
                style = SzjLabelStyle,
            )
        }
    }
    if (onClick != null) SzjPressable(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = SzjInnerShape) { content() }
    else content()
}

/**
 * 单个绝的明细：队伍 / 职业 / 队友 / 团灭点。
 *
 * 五个明细接口都要 territory_type，所以只在展开时拉。字段名来自官网
 * Ultimate chunk 的视图代码。
 */
@Composable
private fun SzjUltimateDetail(u: ShizhijiaUltimate) {
    val context = LocalContext.current
    var team by remember(u) { mutableStateOf<List<ShizhijiaTeamMember>>(emptyList()) }
    var jobs by remember(u) { mutableStateOf<List<ShizhijiaJobTimes>>(emptyList()) }
    var friends by remember(u) { mutableStateOf<List<ShizhijiaFriendTimes>>(emptyList()) }
    var deaths by remember(u) { mutableStateOf<List<ShizhijiaDeadPoint>>(emptyList()) }
    var loading by remember(u) { mutableStateOf(true) }
    var jobNames by remember { mutableStateOf(mapOf<String, String>()) }

    LaunchedEffect(u) {
        loading = true
        team = (ShizhijiaStatsApi.ultimateTeam(context, u.territory) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        jobs = (ShizhijiaStatsApi.ultimateJob(context, u.territory) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        friends = (ShizhijiaStatsApi.ultimateFriend(context, u.territory) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        deaths = (ShizhijiaStatsApi.ultimateDeadPoint(context, u.territory) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        if (jobNames.isEmpty()) jobNames = ShizhijiaApi.getJobConfig(context).mapValues { it.value.name }
        loading = false
    }

    Column(Modifier.fillMaxWidth()) {
        if (loading) {
            SzjStatLoading()
            return@Column
        }
        if (team.isEmpty() && jobs.isEmpty() && friends.isEmpty() && deaths.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                SzjStatCard {
                    Text(
                        "${u.label}没有明细数据。石之家对这一项返回了空结果——" +
                            "多数情况是后端还没开放（数据中心的开关里这个绝是关的）。",
                        color = SzjMuted, style = SzjMetaStyle, lineHeight = 17.sp,
                    )
                }
            }
            return@Column
        }
        if (team.isNotEmpty()) {
            SzjStatSection("${u.label} · 首通队伍")
            SzjStatListCard {
                team.forEach { m ->
                    SzjStatRow(
                        m.characterName.ifBlank { "队员" },
                        jobNames[m.jobId] ?: "职业 ${m.jobId}",
                        meta = listOf(m.groupName, m.areaName).filter { it.isNotBlank() }
                            .joinToString(" · ").ifBlank { null },
                    )
                }
            }
        }
        if (jobs.isNotEmpty()) {
            SzjStatSection("${u.label} · 职业通关")
            SzjStatListCard {
                jobs.sortedByDescending { it.times }.forEach { j ->
                    SzjStatRow(j.jobName.ifBlank { "未知职业" }, "${j.times} 次")
                }
            }
        }
        if (friends.isNotEmpty()) {
            SzjStatSection("${u.label} · 常见队友")
            SzjStatListCard {
                friends.sortedByDescending { it.times }.forEach { f ->
                    SzjStatRow(
                        f.characterName.ifBlank { "队友" },
                        "${f.times} 次",
                        meta = f.groupName.ifBlank { null },
                    )
                }
            }
        }
        if (deaths.isNotEmpty()) {
            SzjStatSection("${u.label} · 团灭点", "共 ${deaths.size} 次，官网画成场地热点图")
            SzjStatCard {
                Text(
                    "记录了 ${deaths.size} 个团灭坐标。热点图要用副本地图底图，" +
                        "这里先只给次数。",
                    color = SzjMuted, style = SzjMetaStyle, lineHeight = 17.sp,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 朝圣交错路
// ---------------------------------------------------------------------------

/**
 * 朝圣交错路。官网数据中心只做了这一个深宫（dd_type = "dd4"），
 * 老的三个（死者宫殿/天动之城/常暗之厅）没有接口，所以不列。
 *
 * 字段名是从官网 DeepDungeon chunk 的视图代码扒的，不是从响应里看的——
 * 用户只进过一次，多数接口回空数组。所以解析全部容错，拿不到就显示 0。
 */
@Composable
private fun SzjDeepDungeonPane(onLogin: () -> Unit) {
    val context = LocalContext.current
    var prog by remember { mutableStateOf<ShizhijiaApi.Res<List<ShizhijiaDdProgress>>?>(null) }
    var hard by remember { mutableStateOf<ShizhijiaHardClear?>(null) }
    var items by remember { mutableStateOf<List<ShizhijiaMkdItem>>(emptyList()) }
    var history by remember { mutableStateOf<List<ShizhijiaItemLog>>(emptyList()) }
    var achieves by remember { mutableStateOf<List<ShizhijiaFishAchieve>>(emptyList()) }
    var team by remember { mutableStateOf<List<ShizhijiaTeamMember>>(emptyList()) }
    var jobNames by remember { mutableStateOf(mapOf<String, String>()) }

    LaunchedEffect(Unit) {
        prog = ShizhijiaStatsApi.ddProgress(context)
        hard = (ShizhijiaStatsApi.ddHardClear(context) as? ShizhijiaApi.Res.Ok)?.value
        items = (ShizhijiaStatsApi.ddItems(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        history = (ShizhijiaStatsApi.ddHistory(context, "item") as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        achieves = (ShizhijiaStatsApi.ddAchieves(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        team = (ShizhijiaStatsApi.ddFirstTeam(context) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        jobNames = ShizhijiaApi.getJobConfig(context).mapValues { it.value.name }
    }

    val rows = (prog as? ShizhijiaApi.Res.Ok)?.value
    // 进度空但道具有记录也要显示——这正是用户当前的情况（进过但没通关）。
    if (rows == null && items.isEmpty() && history.isEmpty()) {
        if (prog == null) SzjStatLoading()
        else SzjStatState(prog, "还没有朝圣交错路记录", "进过朝圣交错路之后，这里会显示朝圣路进度、团灭点和道具", onLogin)
        return
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        val solo = rows?.find { it.solo }
        val party = rows?.find { !it.solo }
        listOfNotNull(
            solo?.let { "单人" to it },
            party?.let { "组队" to it },
        ).forEach { (label, p) ->
            item("prog_$label") {
                SzjStatSection(label, "装备等级 武器 ${p.weaponLevel} · 防具 ${p.armorLevel}")
                SzjStatCard {
                    Column {
                        SzjStatGrid(listOf(
                            "通关次数" to p.totalClearTime.toString(),
                            "失败次数" to p.failedTimes.toString(),
                            "阵亡" to p.totalDeadNum.toString(),
                        ))
                        if (p.clearElapsedTime > 0) {
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("最快通关", color = SzjMuted, style = SzjMetaStyle, modifier = Modifier.weight(1f))
                                Text(fmtElapsed(p.clearElapsedTime), color = SzjAccent, style = SzjLabelStyle)
                            }
                        }
                    }
                }
            }
            if (p.jobClears.isNotEmpty()) {
                item("job_$label") {
                    SzjStatSection("$label · 职业通关")
                    SzjStatListCard {
                        p.jobClears.sortedByDescending { it.second }.forEach { (jid, n) ->
                            SzjStatRow(jobNames[jid] ?: "职业 $jid", "$n 次")
                        }
                    }
                }
            }
            if (p.annihilation.isNotEmpty()) {
                item("anni_$label") {
                    SzjStatSection("$label · 团灭路段", "哪一段栽得最多")
                    SzjStatListCard {
                        p.annihilation.sortedByDescending { it.second }.forEach { (seg, n) ->
                            SzjStatRow(ShizhijiaDeepDungeon.segment(seg), "$n 次")
                        }
                    }
                }
            }
        }
        hard?.let { h ->
            item("hard") {
                SzjStatSection("朝圣交错路本体", if (h.logTime.isNotBlank()) "首通 ${h.logTime.take(10)}" else null)
                SzjStatCard {
                    Column {
                        SzjStatGrid(listOf(
                            "通关" to h.clearTimes.toString(),
                            "阵亡" to h.deadTimes.toString(),
                            "通关前进入" to h.enterBeforeClear.toString(),
                        ))
                        if (h.elapsedTime > 0) {
                            Spacer(Modifier.height(12.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("用时", color = SzjMuted, style = SzjMetaStyle, modifier = Modifier.weight(1f))
                                Text(fmtElapsed(h.elapsedTime), color = SzjAccent, style = SzjLabelStyle)
                            }
                        }
                    }
                }
            }
        }
        if (team.isNotEmpty()) {
            item("team_h") { SzjStatSection("首通队伍") }
            item("team") {
                SzjStatListCard {
                    team.forEach { m ->
                        SzjStatRow(
                            m.characterName.ifBlank { "队员" },
                            jobNames[m.jobId] ?: "职业 ${m.jobId}",
                            meta = listOf(m.groupName, m.areaName).filter { it.isNotBlank() }.joinToString(" · ")
                                .ifBlank { null },
                        )
                    }
                }
            }
        }
        if (items.isNotEmpty()) {
            item("item_h") { SzjStatSection("道具获取", "共 ${items.size} 种") }
            item("item") {
                SzjStatListCard {
                    items.sortedByDescending { it.num }.forEach { m ->
                        SzjStatRow(m.name, "${m.num}", meta = "首次 ${m.firstTime.take(10)}")
                    }
                }
            }
        }
        if (history.isNotEmpty()) {
            item("hist_h") { SzjStatSection("获取记录", "共 ${history.size} 条") }
            item("hist") {
                SzjStatListCard {
                    history.sortedByDescending { it.logTime }.take(30).forEach { h ->
                        SzjStatRow(h.name, h.logTime.take(10), meta = h.logTime.drop(11).ifBlank { null })
                    }
                }
            }
        }
        if (achieves.isNotEmpty()) {
            item("ach_h") { SzjStatSection("成就") }
            item("ach") {
                SzjStatListCard {
                    achieves.forEach { a ->
                        SzjStatRow(a.name, a.logTime.take(10), meta = a.detail.ifBlank { null })
                    }
                }
            }
        }
    }
}
