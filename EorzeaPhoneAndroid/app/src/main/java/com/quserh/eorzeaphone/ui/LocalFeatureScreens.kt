package com.quserh.eorzeaphone.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.data.GameJob
import com.quserh.eorzeaphone.data.GameDailyEntry
import com.quserh.eorzeaphone.data.GameRetainer
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneBackground
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureFrame(title: String, state: PhoneState, trailing: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(title, color = PhoneText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton(onClick = state::back) { Text("‹", color = PhoneAccent, fontSize = 30.sp) } },
            actions = { trailing?.invoke() },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
        )
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            var appeared by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { appeared = true }
            AnimatedVisibility(
                visible = appeared,
                enter = fadeIn(tween(260)) + slideInVertically(tween(300), initialOffsetY = { it / 18 }),
            ) {
                Box(Modifier.fillMaxSize().padding(horizontal = LocalContentMargin.current.dp)) { content() }
            }
        }
    }
}

@Composable
fun JobsScreen(state: PhoneState) {
    val categories = listOf("坦克", "治疗", "近战", "远程物理", "远程魔法", "生产", "采集", "战斗")
    FeatureFrame("职业", state) {
        if (state.jobs.isEmpty()) {
            EmptyFeature(if (state.connected) "正在读取职业等级…" else "连接游戏后显示全部职业等级")
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val active = state.jobs.firstOrNull { it.active }
                if (active != null) item { ActiveJobCard(active) }
                categories.forEach { category ->
                    val jobs = state.jobs.filter { it.category == category }
                    if (jobs.isNotEmpty()) {
                        item { Text(category, color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, start = 4.dp)) }
                        item {
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).animateContentSize()) {
                                jobs.forEachIndexed { index, job -> JobRow(job, index != jobs.lastIndex) }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun ActiveJobCard(job: GameJob) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF263D57)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(PhoneAccent), contentAlignment = Alignment.Center) {
            Text(job.abbreviation.take(3), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text("当前职业", color = Color(0xFFBFD4EA), fontSize = 11.sp)
            Text(job.name, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            if (job.itemLevel > 0) Text("装等 ${job.itemLevel}", color = Color(0xFFBFD4EA), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Text("Lv.${job.level}", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun JobRow(job: GameJob, divider: Boolean) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(7.dp)).background(if (job.active) PhoneAccent else PhoneSurfaceRaised), contentAlignment = Alignment.Center) {
                Text(job.abbreviation.take(3), color = if (job.active) Color.White else PhoneText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(job.name, color = PhoneText, fontSize = 14.sp)
                if (job.itemLevel > 0) Text("装等 ${job.itemLevel}", color = PhoneMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
            }
            Text("Lv.${job.level}", color = if (job.level >= 100) PhoneAccent else PhoneText, fontWeight = FontWeight.Bold)
        }
        if (divider) Box(Modifier.fillMaxWidth().height(1.dp).padding(start = 64.dp).background(Color(0x22333333)))
    }
}

@Composable
fun ActivityScreen(state: PhoneState) {
    val activity = state.activity
    FeatureFrame("活跃度", state) {
        if (activity == null) {
            EmptyFeature(if (state.connected) "正在建立角色活跃度档案…" else "连接游戏后追踪经验、Gil、副本和收藏")
            return@FeatureFrame
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(18.dp)) {
                    Text(state.profile?.name ?: "未连接角色", color = PhoneText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(state.profile?.let { "${it.jobName} · Lv.${it.level} · ${it.location}" } ?: "连接后开始本次会话统计", color = PhoneMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
            item { MetricGrid(listOf("本次游戏时间" to formatDuration(Duration.ofSeconds(activity.sessionPlaySeconds)), "获得 Gil" to activity.sessionGilEarned.toString(), "获得经验" to activity.sessionExpGained.toString(), "完成副本" to activity.sessionDutiesCompleted.toString())) }
            item { Text("今日累计", color = PhoneMuted, fontSize = 12.sp) }
            item { MetricGrid(listOf("游戏时间" to formatDuration(Duration.ofSeconds(activity.todayPlaySeconds)), "获得 Gil" to activity.todayGilEarned.toString(), "获得经验" to activity.todayExpGained.toString(), "提升等级" to activity.todayLevelsGained.toString())) }
            item { Text("收藏与雇员", color = PhoneMuted, fontSize = 12.sp) }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusLine("坐骑", "${activity.mountsOwned} / ${activity.mountsTotal}")
                    StatusLine("宠物", "${activity.minionsOwned} / ${activity.minionsTotal}")
                    StatusLine("雇员", "${formatCount(activity.retainerCount)} 人")
                    StatusLine("探险状态", "${activity.venturesReady} 完成 · ${activity.venturesActive} 进行中")
                }
            }
        }
    }
}

@Composable
private fun MetricGrid(values: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        values.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(14.dp)) {
                        Text(label, color = PhoneMuted, fontSize = 11.sp)
                        Text(value, color = PhoneText, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DailiesScreen(state: PhoneState) {
    var now by remember { mutableLongStateOf(Instant.now().epochSecond) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1_000); now = Instant.now().epochSecond } }
    val data = state.dailies
    val daily = data?.let { Duration.ofSeconds((it.nextDailyResetUnix - now).coerceAtLeast(0)) }
    val weekly = data?.let { Duration.ofSeconds((it.nextWeeklyResetUnix - now).coerceAtLeast(0)) }
    FeatureFrame("日常与重置", state) {
        if (data == null) {
            EmptyFeature(if (state.connected) "正在读取日常状态…" else "连接游戏后读取实际完成状态")
            return@FeatureFrame
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { ResetHero("每日重置", daily ?: Duration.ZERO, Color(0xFF237D72)) }
            item { ResetHero("每周重置", weekly ?: Duration.ZERO, Color(0xFF4D62A8)) }
            item { Text("每日", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
            items(data.entries.filter { !it.weekly }, key = { it.id }) { Box(Modifier.animateItem()) { DailyDataRow(state, it) } }
            item { Text("每周", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
            items(data.entries.filter { it.weekly }, key = { it.id }) { Box(Modifier.animateItem()) { DailyDataRow(state, it) } }
            if (state.retainers.any { it.ventureId > 0 }) {
                item { Text("雇员探险", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                items(state.retainers.filter { it.ventureId > 0 }, key = { it.id }) { Box(Modifier.animateItem()) { VentureRow(it, now) } }
            }
        }
    }
}

@Composable
private fun VentureRow(retainer: GameRetainer, now: Long) {
    val remaining = ((retainer.ventureCompleteUnix - now)).coerceAtLeast(0L)
    val done = remaining == 0L
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (done) Color(0xFF1F3A2C) else PhoneSurface).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(retainer.name, color = PhoneText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(if (done) "已完成" else "探险中", color = if (done) Color(0xFF4CD487) else PhoneAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Text(if (done) "可以前往侍从铃收取派遣成果" else "返回剩余 ${countdownLabel(remaining)}", color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
fun SubmarineScreen(state: PhoneState) {
    var now by remember { mutableLongStateOf(Instant.now().epochSecond) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1_000); now = Instant.now().epochSecond } }
    val vessels = state.submarine?.vessels.orEmpty()
    FeatureFrame("潜水艇", state) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF205B6E)).padding(20.dp)) {
                    Text("潜水艇远征", color = Color.White.copy(alpha = .8f), fontSize = 12.sp)
                    Text("${vessels.count { (it.returnUnix - now) > 0 }} 艘航行中", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                    Text("数据在插件进入房屋工房后自动读取同步", color = Color.White.copy(alpha = .8f), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (vessels.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(16.dp)) {
                        Text("暂无潜水艇数据", color = PhoneText, fontWeight = FontWeight.SemiBold)
                        Text(if (state.connected) "进入一次房屋工房后即可同步。" else "连接游戏插件后读取潜水艇状态", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            } else {
                items(vessels, key = { it.name }) { v ->
                    Box(Modifier.animateItem()) {
                    val remaining = (v.returnUnix - now).coerceAtLeast(0L)
                    val done = remaining == 0L
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (done) Color(0xFF1F3A2C) else PhoneSurface).padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(v.name, color = PhoneText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(if (done) "已回港" else "航行中", color = if (done) Color(0xFF4CD487) else PhoneAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            if (done) "可以收取探险成果" else "返航剩余 ${countdownLabel(remaining)}",
                            color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp),
                        )
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetHero(title: String, duration: Duration, color: Color) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(color).padding(18.dp)) {
        Text(title, color = Color.White.copy(alpha = .78f), fontSize = 12.sp)
        AnimatedContent(formatDuration(duration), label = "reset") { value -> Text(value, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun DailyDataRow(state: PhoneState, item: GameDailyEntry) {
    val manualChecked = !item.automatic && state.isDailyChecked(item.id, item.weekly)
    val complete = if (item.automatic) item.complete else manualChecked
    val done = if (item.automatic && item.available) (item.goal - item.remaining).coerceIn(0, item.goal.coerceAtLeast(0)) else if (complete) item.goal else 0
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).clickable(enabled = !item.automatic) { state.toggleDaily(item.id, item.weekly) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(if (complete) PhoneGreen else PhoneSurfaceRaised), contentAlignment = Alignment.Center) { Text(if (complete) "✓" else "", color = Color.White, fontWeight = FontWeight.Bold) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.label, color = PhoneText, fontWeight = FontWeight.SemiBold)
            val note = if (item.id == "daily.levequests" && item.available && item.remaining >= 0) {
                "当前额度 ${item.remaining} / ${item.goal}"
            } else when {
                item.note.isNotBlank() -> item.note
                !item.available -> "当前不可读取"
                item.goal > 0 -> "$done / ${item.goal}"
                else -> "--"
            }
            Text(note, color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            if (item.automatic && item.available && item.goal > 0) LinearProgressIndicator(progress = { done / item.goal.toFloat() }, modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(4.dp), color = PhoneAccent, trackColor = PhoneSurfaceRaised)
        }
        Text(if (item.automatic) "自动" else "手动", color = if (item.automatic) PhoneAccent else PhoneMuted, fontSize = 10.sp)
    }
}

@Composable
fun HousingScreen(state: PhoneState) {
    val housing = state.housing
    FeatureFrame("房屋", state) {
        if (housing?.ward == null || housing.plot == null) {
            EmptyFeature(if (state.connected) "进入住宅区或房屋后会显示当前位置" else "连接游戏后读取住宅位置")
        } else {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF315D4D)).padding(22.dp)) {
                    Text(state.profile?.location ?: "住宅区", color = Color.White.copy(alpha = .75f), fontSize = 12.sp)
                    Text("第 ${housing.ward} 区 · ${housing.plot} 号", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
                    Text(if (housing.exterior) "庭院 / 室外" else housing.apartmentWing?.let { "公寓 · 第 $it 栋" } ?: "室内", color = Color.White.copy(alpha = .82f), modifier = Modifier.padding(top = 8.dp))
                }
                Text("位置详情", color = PhoneMuted, fontSize = 12.sp)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatusLine("区域", state.profile?.location ?: "未知")
                    StatusLine("分区", "第 ${housing.ward} 区")
                    StatusLine("地块", "${housing.plot} 号")
                    StatusLine("位置", if (housing.exterior) "室外" else "室内")
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(state: PhoneState) {
    FeatureFrame("通知", state) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("本地通知", color = PhoneMuted, fontSize = 12.sp)
            ToggleRow("聊天提醒", "应用在后台时提醒新消息", state.chatNotifications, state::updateChatNotifications)
            ToggleRow("私聊优先提醒", "私聊消息使用高优先级", state.tellNotifications, state::updateTellNotifications)
            ToggleRow("重置提醒", "每日与每周重置前提醒", state.resetNotifications, state::updateResetNotifications)
            Text("通知仅保存在本机，不会上传到服务器。", color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, color = PhoneText); Text(subtitle, color = PhoneMuted, fontSize = 11.sp) }
        Switch(checked, onChange)
    }
}

@Composable
private fun StatusLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) { Text(label, color = PhoneMuted, modifier = Modifier.weight(1f)); Text(value, color = PhoneText, fontWeight = FontWeight.SemiBold) }
}

private fun countdownLabel(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val d = s / 86400
    val h = s % 86400 / 3600
    val m = s % 3600 / 60
    return when {
        d > 0 -> "${d}天${h}小时"
        h > 0 -> "${h}小时${m}分"
        else -> "${m}分钟"
    }
}

@Composable
fun EmptyFeature(text: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(text, color = PhoneMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(28.dp)) }
}

private fun formatDuration(value: Duration?): String {
    if (value == null) return "--:--"
    val seconds = value.seconds.coerceAtLeast(0)
    val days = seconds / 86400
    val hours = seconds % 86400 / 3600
    val minutes = seconds % 3600 / 60
    return if (days > 0) "${days}天 ${hours}小时" else "%02d:%02d:%02d".format(hours, minutes, seconds % 60)
}

private fun signedNumber(value: Long): String = when { value > 0 -> "+$value"; else -> value.toString() }

private fun nextAt(now: ZonedDateTime, hour: Int, day: DayOfWeek?): Duration {
    var target = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
    if (day == null) {
        if (!target.isAfter(now)) target = target.plusDays(1)
    } else {
        while (target.dayOfWeek != day || !target.isAfter(now)) target = target.plusDays(1)
    }
    return Duration.between(now, target)
}
