package com.quserh.eorzeaphone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.data.FishingAlarmStore
import com.quserh.eorzeaphone.data.FishingCatalog
import com.quserh.eorzeaphone.data.FishingCatalogRepository
import com.quserh.eorzeaphone.data.FishingFish
import com.quserh.eorzeaphone.data.FishingItemRef
import com.quserh.eorzeaphone.data.FishingWindowCalculator
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneBackground
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class FishingFilter(val label: String) { All("全部"), Available("可捕获"), Big("鱼王"), Spear("刺鱼"), Caught("已捕获"), Missing("未捕获") }

@Composable
fun FishingScreen(state: PhoneState) {
    val context = LocalContext.current
    var catalog by remember { mutableStateOf<FishingCatalog?>(null) }
    var selected by remember { mutableStateOf<FishingFish?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FishingFilter.All) }
    var alarmsOnly by remember { mutableStateOf(false) }
    var alarmVersion by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        catalog = FishingCatalogRepository.load(context).also { FishingAlarmStore.refresh(context, it) }
    }
    BackHandler(enabled = selected != null) { selected = null }

    AnimatedContent(targetState = selected, label = "fishing-detail") { fish ->
        if (fish != null && catalog != null) {
            FishingDetail(state, fish, catalog!!, alarmVersion) { enabled ->
                FishingAlarmStore.set(context, fish, catalog!!, enabled)
                if (enabled) state.requestNotificationPermission()
                alarmVersion++
            }
        } else {
            ScreenFrame(background = PhoneBackground) {
                ScreenHeader("捕鱼", state, trailing = {
                    Text(
                        if (alarmsOnly) "图鉴" else "闹钟",
                        color = PhoneAccent,
                        fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { alarmsOnly = !alarmsOnly }.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                })
                val data = catalog
                if (data == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("正在载入捕鱼资料…", color = PhoneMuted) }
                } else {
                    val alarmIds = remember(alarmVersion, alarmsOnly) { FishingAlarmStore.enabledIds(context) }
                    val caught = remember(state.fishingLog, data) { data.fish.count { state.isFishCaught(it.logId, it.method) } }
                    val filtered = remember(data, query, filter, alarmsOnly, alarmVersion, state.fishingLog) {
                        val needle = query.trim()
                        data.fish.asSequence()
                            .filter { !alarmsOnly || it.id in alarmIds }
                            .filter { needle.isBlank() || it.name.contains(needle, true) || it.spots.any { spot -> spot.name.contains(needle, true) || spot.region.contains(needle, true) || spot.zone.contains(needle, true) } }
                            .filter {
                                when (filter) {
                                    FishingFilter.All -> true
                                    FishingFilter.Available -> FishingWindowCalculator.availableNow(it, data)
                                    FishingFilter.Big -> it.isBigFish
                                    FishingFilter.Spear -> it.method == "spear"
                                    FishingFilter.Caught -> state.isFishCaught(it.logId, it.method)
                                    FishingFilter.Missing -> !state.isFishCaught(it.logId, it.method)
                                }
                            }.toList()
                    }
                    FishingListHeader(query, { query = it }, filter, { filter = it }, caught, data.fish.size, state.fishingLog != null)
                    LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        if (filtered.isEmpty()) item { Text(if (alarmsOnly) "还没有设置捕鱼闹钟" else "没有符合条件的鱼", color = PhoneMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(34.dp)) }
                        items(filtered, key = { "${it.method}-${it.id}" }) { fishRow ->
                            FishingRow(fishRow, state.isFishCaught(fishRow.logId, fishRow.method), fishRow.id in alarmIds) { selected = fishRow }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FishingListHeader(query: String, onQuery: (String) -> Unit, filter: FishingFilter, onFilter: (FishingFilter) -> Unit, caught: Int, total: Int, synced: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(10.dp)).background(PhoneSurfaceRaised).padding(horizontal = 12.dp)) {
            Text("⌕", color = PhoneMuted, fontSize = 20.sp)
            BasicTextField(query, onQuery, singleLine = true, textStyle = TextStyle(color = PhoneText, fontSize = 14.sp), modifier = Modifier.weight(1f).padding(horizontal = 9.dp), decorationBox = { field -> Box(contentAlignment = Alignment.CenterStart) { if (query.isBlank()) Text("搜索鱼类、钓场或地区", color = PhoneMuted, fontSize = 13.sp); field() } })
            if (query.isNotEmpty()) Text("×", color = PhoneMuted, fontSize = 20.sp, modifier = Modifier.clickable { onQuery("") })
        }
        Row(Modifier.fillMaxWidth().padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FishingFilter.entries.forEach { item ->
                Text(item.label, color = if (filter == item) Color.White else PhoneMuted, fontSize = 11.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(13.dp)).background(if (filter == item) PhoneAccent else PhoneSurface).clickable { onFilter(item) }.padding(vertical = 6.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("钓鱼笔记", color = PhoneText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(if (synced) "$caught / $total 已捕获" else "$total 条资料 · 等待游戏同步", color = PhoneMuted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FishingRow(fish: FishingFish, caught: Boolean, alarm: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(PhoneSurface).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)).background(PhoneSurfaceRaised), contentAlignment = Alignment.Center) {
            ItemIcon(fish.icon, Modifier.fillMaxSize(), fish.name.take(2))
            if (caught) Text("✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomEnd).size(18.dp).clip(CircleShape).background(PhoneGreen), textAlign = TextAlign.Center)
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fish.name, color = if (caught) PhoneText else PhoneText.copy(alpha = .82f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (alarm) Text("◉", color = PhoneAccent, fontSize = 13.sp)
            }
            val place = fish.spots.firstOrNull()?.let { listOf(it.region, it.name).filter(String::isNotBlank).distinct().joinToString(" · ") }.orEmpty()
            Text(place.ifBlank { if (fish.method == "spear") "刺鱼笔记" else "钓鱼笔记" }, color = PhoneMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 4.dp)) {
                MiniBadge(if (fish.method == "spear") "刺鱼" else tugLabel(fish.tug))
                if (fish.isBigFish) MiniBadge("鱼王", PhoneAccent)
                if (fish.startHour != 0.0 || fish.endHour != 24.0) MiniBadge("ET ${fish.startText}-${fish.endText}")
                if (fish.weather.isNotEmpty()) MiniBadge("天气")
            }
        }
        Text("›", color = PhoneMuted, fontSize = 25.sp, modifier = Modifier.padding(start = 7.dp))
    }
}

@Composable
private fun FishingDetail(state: PhoneState, fish: FishingFish, catalog: FishingCatalog, alarmVersion: Int, onAlarm: (Boolean) -> Unit) {
    val context = LocalContext.current
    val alarm = remember(fish.id, alarmVersion) { FishingAlarmStore.isEnabled(context, fish.id) }
    val caught = state.isFishCaught(fish.logId, fish.method)
    val window = remember(fish, catalog, System.currentTimeMillis() / 60_000L) { FishingWindowCalculator.nextWindow(fish, catalog) }
    ScreenFrame {
        ScreenHeader(fish.name, state)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneSurface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(PhoneSurfaceRaised), contentAlignment = Alignment.Center) { ItemIcon(fish.icon, Modifier.fillMaxSize(), fish.name.take(2)) }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(fish.name, color = PhoneText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("版本 ${formatPatch(fish.version)} · ${if (fish.method == "spear") "刺鱼" else "钓鱼"} · ${if (caught) "已捕获" else "未捕获"}", color = if (caught) PhoneGreen else PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Text(if (caught) "✓" else "○", color = if (caught) PhoneGreen else PhoneMuted, fontSize = 26.sp)
                }
            }
            item {
                DetailSection("下次捕获窗口") {
                    if (window == null) Text("暂未计算到可用窗口", color = PhoneMuted, fontSize = 13.sp) else {
                        Text(if (window.startMillis <= System.currentTimeMillis() + 1_000L) "现在可以捕获" else formatWindow(window.startMillis, window.endMillis), color = if (window.startMillis <= System.currentTimeMillis() + 1_000L) PhoneGreen else PhoneText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        window.spot?.let { Text("${it.region} · ${it.zone} · ${it.name}", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }
                    }
                    Button(onClick = { onAlarm(!alarm) }, colors = ButtonDefaults.buttonColors(containerColor = if (alarm) PhoneSurfaceRaised else PhoneAccent, contentColor = if (alarm) PhoneText else Color.White), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                        Text(if (alarm) "取消闹钟" else "提前 ${FishingAlarmStore.DEFAULT_LEAD_MINUTES} 分钟提醒")
                    }
                }
            }
            item {
                DetailSection("上钩条件") {
                    ConditionRow("时间", if (fish.startHour == 0.0 && fish.endHour == 24.0) "全天" else "ET ${fish.startText} - ${fish.endText}")
                    ConditionRow("天气", fish.weatherNames(catalog).ifBlank { "无要求" })
                    if (fish.previousWeather.isNotEmpty()) ConditionRow("前置天气", fish.previousWeather.mapNotNull { catalog.weather[it]?.name }.joinToString(" / "))
                    if (fish.method == "rod") ConditionRow("竿型", tugLabel(fish.tug))
                    if (fish.hook.isNotBlank() && fish.hook != "unknown") ConditionRow("提钩", hookLabel(fish.hook))
                    if (fish.snagging) ConditionRow("特殊要求", "需要钓组")
                    if (fish.lure.isNotBlank() && fish.lure != "none") ConditionRow("拟饵技能", lureLabel(fish.lure, fish.lureStacks))
                    if (fish.folkloreId > 0) ConditionRow("传承录", "需要对应地区传承录")
                    if (fish.gathering > 0) ConditionRow("获得力", fish.gathering.toString())
                    if (fish.perception > 0) ConditionRow("鉴别力", fish.perception.toString())
                }
            }
            if (fish.bait.isNotEmpty() || fish.path.isNotEmpty() || fish.mooch.isNotEmpty()) item {
                DetailSection("鱼饵与钓法") {
                    if (fish.path.isNotEmpty()) ItemPath("推荐钓法", fish.path)
                    else if (fish.bait.isNotEmpty()) ItemPath("可用鱼饵", fish.bait.take(12))
                    if (fish.mooch.isNotEmpty()) ItemPath("以小钓大", fish.mooch)
                }
            }
            if (fish.predators.isNotEmpty()) item {
                DetailSection("捕鱼人之识") {
                    fish.predators.forEach { predator ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                            ItemIcon(predator.icon, Modifier.size(34.dp), predator.name.take(1))
                            Text(predator.name, color = PhoneText, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(start = 9.dp))
                            Text("×${predator.count}", color = PhoneAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (fish.intuitionSeconds > 0) Text("触发后持续 ${fish.intuitionSeconds} 秒", color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
            item {
                DetailSection("钓场") {
                    fish.spots.forEach { spot ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            Text(spot.name, color = PhoneText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(listOf(spot.region, spot.zone).filter(String::isNotBlank).distinct().joinToString(" · "), color = PhoneMuted, fontSize = 11.sp)
                        }
                    }
                    if (fish.spots.isEmpty()) Text("该鱼暂无钓场记录", color = PhoneMuted, fontSize = 12.sp)
                }
            }
            if (fish.quest.isNotBlank() || fish.collectableInfo.isNotBlank()) item {
                DetailSection("补充资料") {
                    if (fish.quest.isNotBlank()) ConditionRow("任务", fish.quest)
                    if (fish.collectableInfo.isNotBlank()) ConditionRow("收藏品", fish.collectableInfo)
                }
            }
            item { Text("资料已内置于 APP，来源参考鱼糕与 GatherBuddyReborn。", color = PhoneMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) }
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(10.dp)).background(PhoneSurface).padding(14.dp)) {
        Text(title, color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 7.dp))
        content()
    }
}

@Composable
private fun ConditionRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.width(72.dp))
        Text(value, color = PhoneText, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ItemPath(label: String, items: List<FishingItemRef>) {
    Text(label, color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp, bottom = 5.dp))
    items.forEachIndexed { index, item ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
            ItemIcon(item.icon, Modifier.size(32.dp), item.name.take(1))
            Text(item.name, color = PhoneText, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            if (index < items.lastIndex) Text("  →", color = PhoneAccent, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MiniBadge(text: String, color: Color = PhoneMuted) {
    Text(text, color = color, fontSize = 9.sp, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = .10f)).padding(horizontal = 5.dp, vertical = 2.dp))
}

private fun FishingFish.weatherNames(catalog: FishingCatalog): String = weather.mapNotNull { catalog.weather[it]?.name }.joinToString(" / ")
private fun tugLabel(value: String): String = when (value) { "light" -> "轻竿 !"; "medium" -> "中竿 !!"; "heavy" -> "重竿 !!!"; else -> "竿型未知" }
private fun hookLabel(value: String): String = when (value) { "precision" -> "精准提钩"; "powerful" -> "强力提钩"; else -> value }
private fun lureLabel(value: String, stacks: Int): String = when (value) { "modest" -> "谦逊之饵${if (stacks > 0) " ×$stacks" else ""}"; "ambitious" -> "雄心之饵${if (stacks > 0) " ×$stacks" else ""}"; else -> value }
private fun formatPatch(value: Double): String = if (value == value.toInt().toDouble()) "${value.toInt()}.0" else String.format(Locale.US, "%.2f", value).trimEnd('0')
private fun formatWindow(start: Long, end: Long): String {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val endFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${formatter.format(Date(start))} - ${endFormatter.format(Date(end))}"
}
