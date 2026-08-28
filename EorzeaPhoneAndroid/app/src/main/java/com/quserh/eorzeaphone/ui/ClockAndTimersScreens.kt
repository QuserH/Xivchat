package com.quserh.eorzeaphone.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneDanger
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.data.AlarmScheduler
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

private enum class ClockTab(val label: String) {
    World("世界时钟"), Alarms("闹钟"), Stopwatch("秒表"), Timer("计时器")
}

private sealed interface ClockDetail {
    data class AlarmEditor(val alarm: LocalAlarm?) : ClockDetail
    data object CityPicker : ClockDetail
}

private data class LocalAlarm(
    val id: Long,
    val hour: Int,
    val minute: Int,
    val repeatMask: Int,
    val label: String,
    val enabled: Boolean,
)

private data class CityClock(val label: String, val zoneId: String)

private class ClockStore(context: Context) {
    private val prefs = context.getSharedPreferences("eorzea_phone_clock", Context.MODE_PRIVATE)
    private val appContext = context.applicationContext
    val alarms = mutableStateListOf<LocalAlarm>().apply { addAll(loadAlarms()) }
    val cities = mutableStateListOf<CityClock>().apply { addAll(loadCities()) }
    val laps = mutableStateListOf<Long>().apply { addAll(loadLaps()) }
    var stopwatchRunning by mutableStateOf(prefs.getBoolean("stopwatchRunning", false))
        private set
    private var stopwatchStartedAt = prefs.getLong("stopwatchStartedAt", 0L)
    private var stopwatchAccumulated = prefs.getLong("stopwatchAccumulated", 0L)
    var timerDuration by mutableLongStateOf(prefs.getLong("timerDuration", 300L).coerceAtLeast(1L))
        private set
    var timerEndsAt by mutableLongStateOf(prefs.getLong("timerEndsAt", 0L))
        private set
    var timerPausedRemaining by mutableLongStateOf(prefs.getLong("timerPausedRemaining", 0L))
        private set

    fun upsertAlarm(alarm: LocalAlarm) {
        val index = alarms.indexOfFirst { it.id == alarm.id }
        if (index < 0) alarms.add(alarm) else alarms[index] = alarm
        saveAlarms()
        if (alarm.enabled) {
            AlarmScheduler.schedule(appContext, alarm.id, alarm.hour, alarm.minute, alarm.repeatMask, alarm.label)
        } else {
            AlarmScheduler.cancel(appContext, alarm.id)
        }
    }

    fun deleteAlarm(id: Long) {
        AlarmScheduler.cancel(appContext, id)
        alarms.removeAll { it.id == id }
        saveAlarms()
    }

    fun toggleAlarm(alarm: LocalAlarm, enabled: Boolean) = upsertAlarm(alarm.copy(enabled = enabled))

    fun addCity(city: CityClock) {
        if (cities.none { it.zoneId == city.zoneId }) {
            cities.add(city)
            saveCities()
        }
    }

    fun removeCity(city: CityClock) {
        cities.remove(city)
        saveCities()
    }

    fun stopwatchElapsed(now: Long): Long = stopwatchAccumulated + if (stopwatchRunning) (now - stopwatchStartedAt).coerceAtLeast(0L) else 0L

    fun toggleStopwatch(now: Long) {
        if (stopwatchRunning) {
            stopwatchAccumulated = stopwatchElapsed(now)
            stopwatchRunning = false
        } else {
            stopwatchStartedAt = now
            stopwatchRunning = true
        }
        saveStopwatch()
    }

    fun lap(now: Long) {
        laps.add(stopwatchElapsed(now))
        saveLaps()
    }

    fun resetStopwatch() {
        stopwatchRunning = false
        stopwatchStartedAt = 0L
        stopwatchAccumulated = 0L
        laps.clear()
        saveStopwatch()
        saveLaps()
    }

    fun startTimer(seconds: Long, now: Long) {
        timerDuration = seconds.coerceAtLeast(1L)
        timerEndsAt = now + timerDuration * 1_000L
        timerPausedRemaining = 0L
        saveTimer()
    }

    fun pauseTimer(now: Long) {
        timerPausedRemaining = timerRemaining(now).coerceAtLeast(1L)
        timerEndsAt = 0L
        saveTimer()
    }

    fun resumeTimer(now: Long) {
        if (timerPausedRemaining <= 0L) return
        timerEndsAt = now + timerPausedRemaining * 1_000L
        timerPausedRemaining = 0L
        saveTimer()
    }

    fun cancelTimer() {
        timerEndsAt = 0L
        timerPausedRemaining = 0L
        saveTimer()
    }

    fun timerRemaining(now: Long): Long = when {
        timerEndsAt > 0L -> ceil((timerEndsAt - now) / 1_000.0).toLong().coerceAtLeast(0L)
        timerPausedRemaining > 0L -> timerPausedRemaining
        else -> 0L
    }

    private fun loadAlarms(): List<LocalAlarm> = runCatching {
        val array = JSONArray(prefs.getString("alarms", "[]"))
        buildList(array.length()) {
            repeat(array.length()) { index ->
                val item = array.getJSONObject(index)
                add(LocalAlarm(item.getLong("id"), item.getInt("hour"), item.getInt("minute"), item.optInt("repeat"), item.optString("label"), item.optBoolean("enabled", true)))
            }
        }
    }.getOrDefault(emptyList())

    private fun saveAlarms() {
        val array = JSONArray()
        alarms.forEach { alarm -> array.put(JSONObject().put("id", alarm.id).put("hour", alarm.hour).put("minute", alarm.minute).put("repeat", alarm.repeatMask).put("label", alarm.label).put("enabled", alarm.enabled)) }
        prefs.edit().putString("alarms", array.toString()).apply()
    }

    private fun loadCities(): List<CityClock> = runCatching {
        val array = JSONArray(prefs.getString("cities", "[]"))
        buildList(array.length()) { repeat(array.length()) { index -> array.getJSONObject(index).let { add(CityClock(it.getString("label"), it.getString("zone"))) } } }
    }.getOrDefault(emptyList())

    private fun saveCities() {
        val array = JSONArray()
        cities.forEach { array.put(JSONObject().put("label", it.label).put("zone", it.zoneId)) }
        prefs.edit().putString("cities", array.toString()).apply()
    }

    private fun loadLaps(): List<Long> = runCatching {
        val array = JSONArray(prefs.getString("stopwatchLaps", "[]"))
        List(array.length()) { array.getLong(it) }
    }.getOrDefault(emptyList())

    private fun saveLaps() {
        val array = JSONArray()
        laps.forEach(array::put)
        prefs.edit().putString("stopwatchLaps", array.toString()).apply()
    }

    private fun saveStopwatch() {
        prefs.edit().putBoolean("stopwatchRunning", stopwatchRunning).putLong("stopwatchStartedAt", stopwatchStartedAt).putLong("stopwatchAccumulated", stopwatchAccumulated).apply()
    }

    private fun saveTimer() {
        prefs.edit().putLong("timerDuration", timerDuration).putLong("timerEndsAt", timerEndsAt).putLong("timerPausedRemaining", timerPausedRemaining).apply()
    }
}

@Composable
fun ClockScreen(state: PhoneState) {
    val context = LocalContext.current
    val store = remember(context) { ClockStore(context) }
    var tab by remember { mutableStateOf(ClockTab.World) }
    var detail by remember { mutableStateOf<ClockDetail?>(null) }
    BackHandler(enabled = detail != null) { detail = null }

    ScreenFrame(background = Color(0xFF111117)) {
        AnimatedContent(
            targetState = detail,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val entering = targetState != null
                (slideIntoContainer(if (entering) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right, tween(250)) + fadeIn(tween(170)))
                    .togetherWith(slideOutOfContainer(if (entering) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right, tween(250)) + fadeOut(tween(150)))
            },
            label = "clock-detail",
        ) { target ->
            when (target) {
                is ClockDetail.AlarmEditor -> AlarmEditorScreen(store, target.alarm) { detail = null }
                ClockDetail.CityPicker -> CityPickerScreen(store) { detail = null }
                null -> ClockRoot(
                    state = state,
                    store = store,
                    tab = tab,
                    selectTab = { tab = it },
                    add = { detail = if (tab == ClockTab.Alarms) ClockDetail.AlarmEditor(null) else ClockDetail.CityPicker },
                    editAlarm = { detail = ClockDetail.AlarmEditor(it) },
                )
            }
        }
    }
}

@Composable
private fun ClockRoot(
    state: PhoneState,
    store: ClockStore,
    tab: ClockTab,
    selectTab: (ClockTab) -> Unit,
    add: () -> Unit,
    editAlarm: (LocalAlarm) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("时钟", state, trailing = {
            if (tab == ClockTab.World || tab == ClockTab.Alarms) {
                TextButton(onClick = add, modifier = Modifier.size(44.dp)) { Text("+", color = PhoneAccent, fontSize = 27.sp) }
            }
        })
        ClockTabStrip(tab, selectTab)
        when (tab) {
            ClockTab.World -> WorldClockTab(store)
            ClockTab.Alarms -> AlarmListTab(store, editAlarm)
            ClockTab.Stopwatch -> StopwatchTab(store)
            ClockTab.Timer -> CountdownTab(store)
        }
    }
}

@Composable
private fun ClockTabStrip(selected: ClockTab, onSelected: (ClockTab) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(8.dp)).background(PhoneSurfaceRaised).padding(3.dp)) {
        ClockTab.entries.forEach { tab ->
            Text(tab.label, color = if (selected == tab) Color.White else PhoneMuted, fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).background(if (selected == tab) PhoneAccent else Color.Transparent).clickable { onSelected(tab) }.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun WorldClockTab(store: ClockStore) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); now = System.currentTimeMillis() } }
    val local = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault())
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                AnalogClock(local.hour, local.minute, local.second, Modifier.size(116.dp))
                Column(Modifier.weight(1f).padding(start = 18.dp)) {
                    Text(local.format(DateTimeFormatter.ofPattern("HH:mm")), color = PhoneText, fontSize = 34.sp, fontWeight = FontWeight.Light)
                    Text(local.format(DateTimeFormatter.ofPattern("M月d日 EEEE")), color = PhoneMuted, fontSize = 12.sp)
                    Text("本地 · ${local.offset.id}", color = PhoneAccent, fontSize = 11.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
        item { WorldClockRow("艾欧泽亚", "游戏内时间", eorzeaClock(now), null) }
        item { WorldClockRow("服务器", "UTC", Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm")), null) }
        items(store.cities, key = { it.zoneId }) { city ->
            val cityNow = Instant.ofEpochMilli(now).atZone(ZoneId.of(city.zoneId))
            WorldClockRow(city.label, cityNow.offset.id, cityNow.format(DateTimeFormatter.ofPattern("HH:mm"))) { store.removeCity(city) }
        }
    }
}

@Composable
private fun AnalogClock(hour: Int, minute: Int, second: Int, modifier: Modifier = Modifier) {
    // Canvas 的 lambda 不是 composable，读不到 PhoneAccent 那个 getter，先在外面取好。
    val accent = PhoneAccent
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f - 5.dp.toPx()
        drawCircle(Color(0xFF272731), radius, center)
        drawCircle(Color.White.copy(alpha = .12f), radius, center, style = Stroke(1.dp.toPx()))
        repeat(12) { index ->
            val angle = Math.toRadians(index * 30.0 - 90.0)
            val outer = Offset(center.x + cos(angle).toFloat() * radius * .88f, center.y + sin(angle).toFloat() * radius * .88f)
            val inner = Offset(center.x + cos(angle).toFloat() * radius * .78f, center.y + sin(angle).toFloat() * radius * .78f)
            drawLine(Color.White.copy(alpha = .55f), inner, outer, 1.5.dp.toPx(), StrokeCap.Round)
        }
        fun hand(value: Float, units: Float, length: Float, width: Float, color: Color) {
            val angle = Math.toRadians(value / units * 360.0 - 90.0)
            val end = Offset(center.x + cos(angle).toFloat() * radius * length, center.y + sin(angle).toFloat() * radius * length)
            drawLine(color, center, end, width.dp.toPx(), StrokeCap.Round)
        }
        hand((hour % 12) + minute / 60f, 12f, .48f, 4f, Color.White)
        hand(minute + second / 60f, 60f, .68f, 3f, Color.White)
        hand(second.toFloat(), 60f, .76f, 1.2f, accent)
        drawCircle(accent, 3.dp.toPx(), center)
    }
}

@Composable
private fun WorldClockRow(name: String, subtitle: String, time: String, remove: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().height(68.dp).clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(name, color = PhoneText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold); Text(subtitle, color = PhoneMuted, fontSize = 11.sp) }
        Text(time, color = PhoneText, fontSize = 25.sp, fontWeight = FontWeight.Light)
        if (remove != null) TextButton(onClick = remove, modifier = Modifier.width(36.dp)) { ImageGlyph(R.drawable.ic_close, PhoneDanger, Modifier.size(18.dp)) }
    }
}

@Composable
private fun CityPickerScreen(store: ClockStore, close: () -> Unit) {
    val catalog = listOf(
        CityClock("东京", "Asia/Tokyo"), CityClock("上海", "Asia/Shanghai"), CityClock("新加坡", "Asia/Singapore"),
        CityClock("伦敦", "Europe/London"), CityClock("巴黎", "Europe/Paris"), CityClock("纽约", "America/New_York"),
        CityClock("洛杉矶", "America/Los_Angeles"), CityClock("悉尼", "Australia/Sydney"),
    )
    Column(Modifier.fillMaxSize()) {
        SimpleLocalHeader("选择城市", close)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            items(catalog, key = { it.zoneId }) { city ->
                val added = store.cities.any { it.zoneId == city.zoneId }
                Row(
                    Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(8.dp)).background(PhoneSurface)
                        .clickable(enabled = !added) { store.addCity(city); close() }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(city.label, color = if (added) PhoneMuted else PhoneText, modifier = Modifier.weight(1f))
                    Text(if (added) "已添加" else ZoneId.of(city.zoneId).id, color = if (added) PhoneMuted else PhoneAccent, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AlarmListTab(store: ClockStore, edit: (LocalAlarm) -> Unit) {
    if (store.alarms.isEmpty()) {
        EmptyFeature("还没有闹钟\n点右上角 + 新建闹钟")
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(store.alarms.sortedWith(compareBy({ it.hour }, { it.minute })), key = { it.id }) { alarm ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).clickable { edit(alarm) }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("%02d:%02d".format(alarm.hour, alarm.minute), color = if (alarm.enabled) PhoneText else PhoneMuted, fontSize = 34.sp, fontWeight = FontWeight.Light)
                    Text(listOf(alarm.label, repeatLabel(alarm.repeatMask)).filter { it.isNotBlank() }.joinToString(" · "), color = PhoneMuted, fontSize = 11.sp)
                }
                Switch(alarm.enabled, { store.toggleAlarm(alarm, it) })
            }
        }
    }
}

@Composable
private fun AlarmEditorScreen(store: ClockStore, alarm: LocalAlarm?, close: () -> Unit) {
    var hour by remember(alarm?.id) { mutableStateOf(alarm?.hour ?: 7) }
    var minute by remember(alarm?.id) { mutableStateOf(alarm?.minute ?: 0) }
    var repeat by remember(alarm?.id) { mutableStateOf(alarm?.repeatMask ?: 0) }
    var label by remember(alarm?.id) { mutableStateOf(alarm?.label.orEmpty()) }
    Column(Modifier.fillMaxSize()) {
        SimpleLocalHeader(if (alarm == null) "新建闹钟" else "编辑闹钟", close)
        Column(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                TimeStepper(hour, { hour = (hour + 23) % 24 }, { hour = (hour + 1) % 24 })
                Text(":", color = PhoneText, fontSize = 46.sp, modifier = Modifier.padding(horizontal = 9.dp))
                TimeStepper(minute, { minute = (minute + 59) % 60 }, { minute = (minute + 1) % 60 })
            }
            Text("重复", color = PhoneMuted, fontSize = 12.sp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEachIndexed { index, day ->
                    val selected = repeat and (1 shl index) != 0
                    Box(Modifier.size(38.dp).clip(CircleShape).background(if (selected) PhoneAccent else PhoneSurface).clickable { repeat = repeat xor (1 shl index) }, contentAlignment = Alignment.Center) {
                        Text(day, color = if (selected) Color.White else PhoneMuted, fontSize = 13.sp)
                    }
                }
            }
            OutlinedTextField(label, { label = it.take(32) }, label = { Text("标签") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.weight(1f))
            if (alarm != null) TextButton(onClick = { store.deleteAlarm(alarm.id); close() }, modifier = Modifier.fillMaxWidth()) { Text("删除闹钟", color = Color(0xFFE05858)) }
            Button(onClick = {
                store.upsertAlarm(LocalAlarm(alarm?.id ?: System.currentTimeMillis(), hour, minute, repeat, label.trim(), true))
                close()
            }, colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("保存") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TimeStepper(value: Int, down: () -> Unit, up: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = up, modifier = Modifier.size(42.dp)) { ImageGlyph(R.drawable.ic_chevron_up, PhoneAccent, Modifier.size(22.dp)) }
        Box(Modifier.size(88.dp, 68.dp).clip(RoundedCornerShape(8.dp)).background(PhoneSurface), contentAlignment = Alignment.Center) { Text("%02d".format(value), color = PhoneText, fontSize = 38.sp, fontWeight = FontWeight.Light) }
        TextButton(onClick = down, modifier = Modifier.size(42.dp)) { ImageGlyph(R.drawable.ic_chevron_down, PhoneAccent, Modifier.size(22.dp)) }
    }
}

@Composable
private fun SimpleLocalHeader(title: String, close: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = close, modifier = Modifier.size(50.dp)) { ImageGlyph(R.drawable.ic_back, PhoneAccent, Modifier.size(24.dp)) }
        Text(title, color = PhoneText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StopwatchTab(store: ClockStore) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(store.stopwatchRunning) {
        while (store.stopwatchRunning) { now = System.currentTimeMillis(); delay(31) }
    }
    val elapsed = store.stopwatchElapsed(now)
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(formatStopwatch(elapsed), color = PhoneText, fontSize = 48.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(top = 44.dp))
        Row(Modifier.fillMaxWidth().padding(top = 35.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            CircleAction(if (store.stopwatchRunning) "计次" else "复位", Color(0xFF66666F), store.stopwatchRunning || elapsed > 0L) {
                if (store.stopwatchRunning) store.lap(now) else store.resetStopwatch()
            }
            CircleAction(if (store.stopwatchRunning) "停止" else "开始", if (store.stopwatchRunning) Color(0xFFE04444) else PhoneGreen, true) { store.toggleStopwatch(now); now = System.currentTimeMillis() }
        }
        if (store.laps.isNotEmpty() || store.stopwatchRunning) {
            LazyColumn(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                if (store.stopwatchRunning) item { LapRow(store.laps.size + 1, elapsed - (store.laps.lastOrNull() ?: 0L), PhoneAccent) }
                items(store.laps.indices.reversed().toList()) { index -> LapRow(index + 1, store.laps[index] - if (index > 0) store.laps[index - 1] else 0L, PhoneMuted) }
            }
        }
    }
}

@Composable
private fun CircleAction(label: String, color: Color, enabled: Boolean, action: () -> Unit) {
    Box(Modifier.size(76.dp).clip(CircleShape).background(color.copy(alpha = if (enabled) .2f else .08f)).clickable(enabled = enabled, onClick = action), contentAlignment = Alignment.Center) {
        Text(label, color = color.copy(alpha = if (enabled) 1f else .35f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun LapRow(index: Int, duration: Long, color: Color) {
    Row(Modifier.fillMaxWidth().height(43.dp), verticalAlignment = Alignment.CenterVertically) { Text("计次 $index", color = PhoneMuted, modifier = Modifier.weight(1f)); Text(formatStopwatch(duration), color = color, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun CountdownTab(store: ClockStore) {
    var pickerHours by remember { mutableStateOf(0) }
    var pickerMinutes by remember { mutableStateOf(5) }
    var pickerSeconds by remember { mutableStateOf(0) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(store.timerEndsAt) { while (store.timerEndsAt > 0L) { now = System.currentTimeMillis(); delay(250) } }
    val remaining = store.timerRemaining(now)
    val active = store.timerEndsAt > 0L || store.timerPausedRemaining > 0L
    if (!active) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.padding(top = 44.dp), verticalAlignment = Alignment.CenterVertically) {
                TimeStepper(pickerHours, { pickerHours = (pickerHours + 23) % 24 }, { pickerHours = (pickerHours + 1) % 24 })
                Text(":", color = PhoneText, fontSize = 36.sp)
                TimeStepper(pickerMinutes, { pickerMinutes = (pickerMinutes + 59) % 60 }, { pickerMinutes = (pickerMinutes + 1) % 60 })
                Text(":", color = PhoneText, fontSize = 36.sp)
                TimeStepper(pickerSeconds, { pickerSeconds = (pickerSeconds + 59) % 60 }, { pickerSeconds = (pickerSeconds + 1) % 60 })
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 21.dp)) { Text("小时", color = PhoneMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)); Text("分钟", color = PhoneMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)); Text("秒", color = PhoneMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
            Spacer(Modifier.weight(1f))
            CircleAction("开始", PhoneGreen, pickerHours + pickerMinutes + pickerSeconds > 0) { store.startTimer((pickerHours * 3600 + pickerMinutes * 60 + pickerSeconds).toLong(), System.currentTimeMillis()); now = System.currentTimeMillis() }
            Spacer(Modifier.height(28.dp))
        }
    } else {
        val fraction = if (store.timerDuration <= 0L) 0f else (remaining.toFloat() / store.timerDuration).coerceIn(0f, 1f)
        val accent = PhoneAccent   // 同上：进度弧在 Canvas 里画，颜色得先提出来
        Column(Modifier.fillMaxSize().padding(horizontal = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(238.dp).padding(top = 30.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 8.dp.toPx()
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val top = Offset(stroke / 2f, stroke / 2f)
                    drawArc(Color.White.copy(alpha = .09f), -90f, 360f, false, top, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(accent, -90f, 360f * fraction, false, top, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(formatClockDuration(remaining), color = PhoneText, fontSize = 38.sp, fontWeight = FontWeight.Light); if (remaining == 0L) Text("计时结束", color = PhoneAccent) }
            }
            Row(Modifier.fillMaxWidth().padding(top = 34.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                CircleAction("取消", Color(0xFF777780), true) { store.cancelTimer() }
                if (remaining == 0L) CircleAction("复位", PhoneGreen, true) { store.cancelTimer() }
                else if (store.timerPausedRemaining > 0L) CircleAction("继续", PhoneGreen, true) { store.resumeTimer(System.currentTimeMillis()); now = System.currentTimeMillis() }
                else CircleAction("暂停", PhoneAccent, true) { store.pauseTimer(System.currentTimeMillis()); now = System.currentTimeMillis() }
            }
        }
    }
}

@Composable
fun TimersScreen(state: PhoneState) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("eorzea_phone_game_timers", Context.MODE_PRIVATE) }
    var dailyReminder by remember { mutableStateOf(prefs.getBoolean("daily", state.resetNotifications)) }
    var gcReminder by remember { mutableStateOf(prefs.getBoolean("grandCompany", false)) }
    var weeklyReminder by remember { mutableStateOf(prefs.getBoolean("weekly", state.resetNotifications)) }
    var ventureReminder by remember { mutableStateOf(prefs.getBoolean("ventures", false)) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1_000); nowMillis = System.currentTimeMillis() } }
    val now = Instant.ofEpochMilli(nowMillis).atZone(ZoneOffset.UTC)
    val daily = nextDaily(now, 15)
    val gc = nextDaily(now, 20)
    val weekly = nextWeekly(now, DayOfWeek.TUESDAY, 8)
    val fashion = fashionWindow(now)
    val cactpot = nextWeekly(now, DayOfWeek.SATURDAY, 8)
    val ocean = oceanVoyage(now)

    FeatureFrame("计时器", state) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { TimerHero("每日重置", Duration.between(now, daily)) }
            item { TimerSectionLabel("服务器重置") }
            item { TimerCard(listOf(
                TimerItem("每日重置", localClock(daily), relative(Duration.between(now, daily)), Color(0xFFF0A43C)),
                TimerItem("部队筹备重置", localClock(gc), relative(Duration.between(now, gc)), Color(0xFFE06A72)),
                TimerItem("每周重置", localDayClock(weekly), relative(Duration.between(now, weekly)), Color(0xFF6088E8)),
            )) }
            item { TimerSectionLabel("活动") }
            item { TimerCard(listOf(
                TimerItem("时尚品鉴", if (fashion.first) "开放中" else "未开放", relative(Duration.between(now, fashion.second)), Color(0xFFDB6EA5)),
                TimerItem("仙人彩", localDayClock(cactpot), relative(Duration.between(now, cactpot)), Color(0xFFF0B94D)),
                TimerItem("海钓航班", ocean.route, if (ocean.boarding) "正在登船" else relative(Duration.between(now, ocean.time)), Color(0xFF4EB7A5)),
            )) }
            item { TimerSectionLabel("雇员探险") }
            item {
                val activity = state.activity
                if (activity == null || activity.retainerCount == 0) Text(if (state.connected) "请在游戏内打开一次雇员铃以读取状态" else "连接游戏后读取雇员探险状态", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(4.dp, 5.dp, 4.dp, 10.dp))
                else TimerCard(listOf(TimerItem("${activity.retainerCount} 名雇员", "${activity.venturesActive} 个探险中", "${activity.venturesReady} 个已完成", if (activity.venturesReady > 0) PhoneGreen else Color(0xFF4EB7A5))))
            }
            item { TimerSectionLabel("提醒") }
            item {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface)) {
                    ReminderRow("每日重置", dailyReminder) { dailyReminder = it; prefs.edit().putBoolean("daily", it).apply(); state.updateResetNotifications(dailyReminder || weeklyReminder) }
                    ReminderRow("部队筹备重置", gcReminder) { gcReminder = it; prefs.edit().putBoolean("grandCompany", it).apply() }
                    ReminderRow("每周重置", weeklyReminder) { weeklyReminder = it; prefs.edit().putBoolean("weekly", it).apply(); state.updateResetNotifications(dailyReminder || weeklyReminder) }
                    ReminderRow("雇员探险", ventureReminder) { ventureReminder = it; prefs.edit().putBoolean("ventures", it).apply() }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

private data class TimerItem(val name: String, val subtitle: String, val value: String, val color: Color)

@Composable
private fun TimerHero(title: String, remaining: Duration) {
    val seconds = remaining.seconds.coerceAtLeast(0L)
    val fraction = (1f - seconds / 86_400f).coerceIn(0f, 1f)
    Row(Modifier.fillMaxWidth().height(154.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF183F43)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(112.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 7.dp.toPx(); val inset = stroke / 2f; val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(Color.White.copy(alpha = .12f), -90f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                drawArc(Color(0xFF62D0BF), -90f, 360f * fraction, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text(heroDuration(seconds), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.padding(start = 20.dp)) { Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text(relative(remaining), color = Color.White.copy(alpha = .68f), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) }
    }
}

@Composable
private fun TimerSectionLabel(text: String) { Text(text, color = PhoneMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, top = 7.dp)) }

@Composable
private fun TimerCard(rows: List<TimerItem>) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface)) {
        rows.forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(7.dp)).background(row.color.copy(alpha = .2f)), contentAlignment = Alignment.Center) { Box(Modifier.size(10.dp).clip(CircleShape).background(row.color)) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(row.name, color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(row.subtitle, color = PhoneMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Text(row.value, color = row.color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            if (index < rows.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 59.dp).height(1.dp).background(Color.White.copy(alpha = .07f)))
        }
    }
}

@Composable
private fun ReminderRow(label: String, checked: Boolean, changed: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = PhoneText, modifier = Modifier.weight(1f)); Switch(checked, changed) }
}

private data class OceanStatus(val time: ZonedDateTime, val boarding: Boolean, val route: String)

private fun nextDaily(now: ZonedDateTime, hour: Int): ZonedDateTime {
    val candidate = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
    return if (candidate.isAfter(now)) candidate else candidate.plusDays(1)
}

private fun nextWeekly(now: ZonedDateTime, day: DayOfWeek, hour: Int): ZonedDateTime {
    var candidate = now.withHour(hour).withMinute(0).withSecond(0).withNano(0)
    while (candidate.dayOfWeek != day || !candidate.isAfter(now)) candidate = candidate.plusDays(1)
    return candidate
}

private fun fashionWindow(now: ZonedDateTime): Pair<Boolean, ZonedDateTime> {
    val open = nextWeekly(now, DayOfWeek.FRIDAY, 8)
    val close = nextWeekly(now, DayOfWeek.TUESDAY, 8)
    return if (close.isBefore(open)) true to close else false to open
}

private fun oceanVoyage(now: ZonedDateTime): OceanStatus {
    val evenHour = now.hour - now.hour % 2
    val start = now.withHour(evenHour).withMinute(0).withSecond(0).withNano(0)
    val boarding = Duration.between(start, now).toMinutes() < 15
    val next = if (boarding) start else start.plusHours(2)
    val routes = listOf("梅尔托尔海峡南 · 白天 / 红玉海 · 白天", "罗塔诺海 · 黄昏 / 一之江 · 黄昏", "梅尔托尔海峡北 · 夜晚 / 萨维奈近海 · 夜晚", "血滨海 · 白天 / 红玉海 · 白天")
    val index = Math.floorMod(next.toEpochSecond() / 7_200L + 88L, routes.size.toLong()).toInt()
    return OceanStatus(next, boarding, routes[index])
}

private fun eorzeaClock(realMillis: Long): String {
    val seconds = realMillis / 1_000.0 * 144.0 / 7.0
    val day = ((seconds.toLong() % 86_400L) + 86_400L) % 86_400L
    return "%02d:%02d".format(day / 3_600L, day % 3_600L / 60L)
}

private fun formatStopwatch(milliseconds: Long): String {
    val centis = milliseconds / 10L % 100L
    val totalSeconds = milliseconds / 1_000L
    val seconds = totalSeconds % 60L
    val minutes = totalSeconds / 60L % 60L
    val hours = totalSeconds / 3_600L
    return if (hours > 0) "%d:%02d:%02d.%02d".format(hours, minutes, seconds, centis) else "%02d:%02d.%02d".format(minutes, seconds, centis)
}

private fun formatClockDuration(seconds: Long): String {
    val hours = seconds / 3_600L
    val minutes = seconds / 60L % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds % 60L) else "%02d:%02d".format(minutes, seconds % 60L)
}

private fun repeatLabel(mask: Int): String = when (mask) {
    0 -> "永不重复"
    0b1111111 -> "每天"
    0b0011111 -> "工作日"
    0b1100000 -> "周末"
    else -> listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日").filterIndexed { index, _ -> mask and (1 shl index) != 0 }.joinToString("、")
}

private fun relative(duration: Duration): String {
    val seconds = duration.seconds.coerceAtLeast(0L)
    val days = seconds / 86_400L
    val hours = seconds % 86_400L / 3_600L
    val minutes = seconds % 3_600L / 60L
    return when { days > 0 -> "$days 天 $hours 小时后"; hours > 0 -> "$hours 小时 $minutes 分后"; else -> "${minutes.coerceAtLeast(1L)} 分后" }
}

private fun heroDuration(seconds: Long): String {
    val minutes = seconds / 60L
    return if (minutes < 60L) "${minutes.coerceAtLeast(1L)}m" else if (minutes < 1_440L) "%d:%02d".format(minutes / 60L, minutes % 60L) else "${minutes / 1_440L}d"
}

private fun localClock(value: ZonedDateTime): String = value.withZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
private fun localDayClock(value: ZonedDateTime): String = value.withZoneSameInstant(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("E HH:mm"))
