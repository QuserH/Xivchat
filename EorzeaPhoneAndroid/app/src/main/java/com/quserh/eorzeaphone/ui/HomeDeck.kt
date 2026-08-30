package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneLine
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.phoneLight
import kotlinx.coroutines.delay

// Deck page for the v2 home: hero ET clock + upcoming rail + resets + feed rows.
// Layout numbers follow 开发/UI-redesign-2026/variant-j.html.

private val DeckHeroLight = listOf(Color(0xFFE4F1DE), Color(0xFFCDE7C5), Color(0xFFC2E1BA))
private val DeckHeroDark = listOf(Color(0xFF182B1E), Color(0xFF132218), Color(0xFF0F1B13))
private val DeckHeroBorder = Color(0xFFB9D6B2)
private val DeckHeroBorderDark = Color(0xFF2A3C2E)
private val DeckDot = Color(0xFF9DC4A0)

private val DeckAccentInk: Color @Composable get() = if (phoneLight) Color(0xFF2F6B40) else Color(0xFF7FC49A)
// Light value is 0x4A6353, not the spec's 0x4E6858: the hero gradient's darkest
// stop (0xC2E1BA) leaves that only 4.29:1, under the 4.5 body threshold, and the
// zone label lands on that end of the ramp at every screen width.
private val DeckSubInk: Color @Composable get() = if (phoneLight) Color(0xFF4A6353) else Color(0xFF9FC0A8)
private val DeckChipBg: Color @Composable get() = if (phoneLight) Color(0xCCFFFFFF) else Color(0xB31E3326)

/** Next UTC 15:00 daily reset and next Tuesday 15:00 UTC weekly reset, from [now]. */
private fun resetDelaysMillis(now: Long): Pair<Long, Long> {
    val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
    fun nextAt(dayOfWeek: Int?): Long {
        cal.timeInMillis = now
        cal.set(java.util.Calendar.HOUR_OF_DAY, 15)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        if (dayOfWeek != null) cal.set(java.util.Calendar.DAY_OF_WEEK, dayOfWeek)
        if (cal.timeInMillis <= now) {
            if (dayOfWeek != null) cal.add(java.util.Calendar.DAY_OF_YEAR, 7) else cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }
    return (nextAt(null) - now) to (nextAt(java.util.Calendar.TUESDAY) - now)
}

private fun formatCountdown(ms: Long): String {
    val minutes = (ms + 59_999L) / 60_000L
    val d = minutes / 1440
    val h = minutes % 1440 / 60
    val m = minutes % 60
    return when {
        d > 0 -> "${d}天${h}时"
        h > 0 -> "$h:${m.toString().padStart(2, '0')}"
        else -> "${m}分"
    }
}

@Composable
fun HomeDeck(state: PhoneState, modifier: Modifier = Modifier) {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        DeckHero(state)
        DeckUpcoming(state, now)
        DeckResets(state, now)
        DeckFeed(state)
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun DeckHero(state: PhoneState) {
    var time by remember { mutableStateOf(eorzeaNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(5_000)
            time = eorzeaNow()
        }
    }
    val weather = state.weather
    val ink = DeckAccentInk
    val heroBorder = if (phoneLight) DeckHeroBorder else DeckHeroBorderDark
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(if (phoneLight) DeckHeroLight else DeckHeroDark))
            .border(1.dp, heroBorder, RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(time, color = ink, fontSize = 44.sp, fontWeight = FontWeight.Bold, lineHeight = 46.sp)
                val bell = time.substringBefore(':').toIntOrNull() ?: 0
                Text(
                    "EORZEA TIME · 游戏内 ${bell}时",
                    color = DeckSubInk,
                    fontSize = 10.sp,
                    letterSpacing = 2.5.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (phoneLight) Color(0xCCFFFFFF) else Color(0xFF223528)),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageGlyph(weatherIcon(weather?.current.orEmpty()), ink, Modifier.size(24.dp))
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text(weather?.current ?: "等待天气", color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(weather?.zone ?: "连接游戏后显示", color = DeckSubInk, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 78.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            val (dailyMs, weeklyMs) = resetDelaysMillis(System.currentTimeMillis())
            val nextWeather = weather?.forecast?.firstOrNull { it.minutesFromNow > 0 }
            DeckChip("天气", nextWeather?.name ?: "--", if (nextWeather == null) "--" else formatCountdown(nextWeather.minutesFromNow * 60_000L), Modifier.weight(1f))
            DeckChip("日常", "重置", formatCountdown(dailyMs), Modifier.weight(1f))
            DeckChip("周常", "重置", formatCountdown(weeklyMs), Modifier.weight(1f))
        }
    }
}

@Composable
private fun DeckChip(label: String, name: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(11.dp))
            .background(DeckChipBg)
            .border(1.dp, if (phoneLight) Color(0x142F6B40) else Color(0x337FC49A), RoundedCornerShape(11.dp))
            .padding(horizontal = 9.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = DeckSubInk, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(4.dp))
        Text(name, color = DeckSubInk, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.weight(1f))
        Text(value, color = DeckAccentInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
private fun DeckSectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(DeckDot))
        Spacer(Modifier.width(7.dp))
        Text(text, color = DeckSubInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

private data class DeckRailItem(val tag: String, val tagColor: Color, val title: String, val sub: String, val countdown: String, val openAppId: String?)

@Composable
private fun DeckUpcoming(state: PhoneState, now: Long) {
    val weather = state.weather
    val nextWeather = weather?.forecast?.firstOrNull { it.minutesFromNow > 0 }
    val (dailyMs, weeklyMs) = resetDelaysMillis(now)
    val items = listOf(
        DeckRailItem("天气", Color(0xFF7C8A94), nextWeather?.name ?: "等待数据", weather?.zone ?: "连接游戏后显示", if (nextWeather == null) "--" else formatCountdown(nextWeather.minutesFromNow * 60_000L), null),
        DeckRailItem("日常", Color(0xFF4E8D5B), "日常重置", "探险札记 · 蛮神", formatCountdown(dailyMs), null),
        // 0x5793CE, not spec info 0x5B9BD3: the 3dp bar encodes category, so it
        // carries meaning and owes 3:1 non-text contrast; the spec value is 2.97.
        DeckRailItem("周常", Color(0xFF5793CE), "周常重置", "周常清单", formatCountdown(weeklyMs), null),
        DeckRailItem("采集", Color(0xFFC08A3E), "采集时钟", "节点窗口与鱼汛", "打开", "gatherclock"),
    )
    Column {
        DeckSectionLabel("接下来")
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            items.forEach { item ->
                DeckRailCard(item, onClick = item.openAppId?.let { id -> { state.openApp(id) } })
            }
        }
    }
}

@Composable
private fun DeckRailCard(item: DeckRailItem, onClick: (() -> Unit)?) {
    Column(
        Modifier
            .width(126.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, PhoneLine, RoundedCornerShape(15.dp))
            .clickable(enabled = onClick != null, onClick = onClick ?: {}),
    ) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(item.tagColor))
        Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.tag, color = PhoneMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text(item.countdown, color = DeckAccentInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(item.title, color = PhoneText, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Text(item.sub, color = PhoneMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun DeckResets(state: PhoneState, now: Long) {
    val (dailyMs, weeklyMs) = resetDelaysMillis(now)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PhoneCard(Modifier.weight(1.25f).height(92.dp), flat = true, onClick = { state.openApp("gatherclock") }) {
            Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("下一档采集", color = PhoneText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text("打开采集时钟查看节点窗口", color = PhoneMuted, fontSize = 10.5.sp, lineHeight = 14.sp, modifier = Modifier.padding(top = 3.dp))
                }
                ImageGlyph(R.drawable.ic_chevron_right, PhoneAccent, Modifier.size(16.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            DeckResetCard("日常重置", formatCountdown(dailyMs))
            DeckResetCard("周常重置", formatCountdown(weeklyMs))
        }
    }
}

@Composable
private fun DeckResetCard(label: String, value: String) {
    PhoneCard(Modifier.fillMaxWidth().height(41.dp), flat = true) {
        Row(Modifier.fillMaxSize().padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = PhoneMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text(value, color = PhoneText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DeckFeed(state: PhoneState) {
    val chatApp = remember { AppCatalog.dock.firstOrNull { it.destination == PhoneScreen.Chat } }
    val feed = state.conversations.filter { it.lastMessage != null }.sortedByDescending { it.lastTimestamp ?: 0L }.take(2)
    Column {
        DeckSectionLabel("动态")
        Spacer(Modifier.height(8.dp))
        if (feed.isEmpty()) {
            PhoneCard(Modifier.fillMaxWidth(), flat = true, onClick = { chatApp?.let { state.open(it) } }) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("打开 Linkpearl 查看最新消息", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    ImageGlyph(R.drawable.ic_chevron_right, PhoneAccent, Modifier.size(16.dp))
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                feed.forEach { conv ->
                    PhoneCard(Modifier.fillMaxWidth(), flat = true, onClick = { chatApp?.let { state.open(it) } }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SoftAvatar(conv.title.take(1), PhoneAccent, size = 38.dp)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(conv.title, color = PhoneText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(conv.lastMessage?.text ?: "", color = PhoneMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                            }
                            if (conv.unread > 0) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (conv.unread > 99) "99+" else conv.unread.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clip(CircleShape).background(androidx.compose.ui.graphics.Color(0xFFE06A5A)).padding(horizontal = 5.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
