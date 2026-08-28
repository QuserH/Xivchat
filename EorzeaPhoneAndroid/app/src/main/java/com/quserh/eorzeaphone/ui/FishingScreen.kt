package com.quserh.eorzeaphone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.quserh.eorzeaphone.ui.theme.PhoneInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import com.quserh.eorzeaphone.data.FishingAlarmStore
import com.quserh.eorzeaphone.data.FishingCatalog
import com.quserh.eorzeaphone.data.FishingCatalogRepository
import com.quserh.eorzeaphone.data.FishingFish
import com.quserh.eorzeaphone.data.FishingItemRef
import com.quserh.eorzeaphone.data.FishingMapImageLoader
import com.quserh.eorzeaphone.data.FishingSpot
import com.quserh.eorzeaphone.data.FishingWindowCalculator
import com.quserh.eorzeaphone.R
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
import kotlin.math.roundToInt

/**
 * 捕鱼筛选。
 *
 * 原来是个单选枚举（全部/可捕获/鱼王/刺鱼/已捕获/未捕获），六个值混了三个维度，
 * 于是最常想问的那句话反而问不出来——"我还没钓到的鱼王，现在有哪条开着窗口"。
 * 现在拆成互不相干的四条，可以叠加。
 */
private data class FishFilter(
    /** 只看当前窗口开着的。 */
    val available: Boolean = false,
    /** "" 全部 / "big" 鱼王 / "spear" 刺鱼。 */
    val kind: String = "",
    /** "" 全部 / "caught" 已捕获 / "missing" 未捕获。 */
    val collected: String = "",
    /** 资料片版本；null = 全部。 */
    val version: Int? = null,
) {
    val isEmpty: Boolean get() = !available && kind.isEmpty() && collected.isEmpty() && version == null

    companion object {
        val KINDS = listOf("" to "全部", "big" to "鱼王", "spear" to "刺鱼")
        val COLLECTED = listOf("" to "全部", "missing" to "未捕获", "caught" to "已捕获")
    }
}

private data class ExpansionTab(val version: Int?, val label: String)

private val expansionTabs = listOf(
    ExpansionTab(null, "全部"),
    ExpansionTab(2, "重生之境"),
    ExpansionTab(3, "苍穹之禁城"),
    ExpansionTab(4, "红莲之狂潮"),
    ExpansionTab(5, "暗影之逆焰"),
    ExpansionTab(6, "晓月之终途"),
    ExpansionTab(7, "金曦之遗辉"),
)

@Composable
fun FishingScreen(state: PhoneState) {
    val context = LocalContext.current
    var catalog by remember { mutableStateOf<FishingCatalog?>(null) }
    var selected by remember { mutableStateOf<FishingFish?>(null) }
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(FishFilter()) }
    var filterPanel by remember { mutableStateOf(false) }
    var alarmsOnly by remember { mutableStateOf(false) }
    var alarmVersion by remember { mutableStateOf(0) }
    var mapSpot by remember { mutableStateOf<FishingSpot?>(null) }

    // 列表状态与可捕获数据提升到界面顶层：进出详情/返回时不重置滑动位置、不重新计算
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            nowMillis = System.currentTimeMillis()
        }
    }
    var displayNow by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            displayNow = System.currentTimeMillis()
        }
    }
    var availabilityEnd by remember(catalog) { mutableStateOf<Map<Int, Long>>(emptyMap()) }
    // 不在窗口内的鱼：下一个窗口的开始时间。
    // 列表右列原来在这种情况下只写"暂不可钓"——一句废话占掉整列。
    // nextWindow 本来就算出来了（下面这个循环里），以前只是把不在窗口内的丢掉。
    var nextWindowStart by remember(catalog) { mutableStateOf<Map<Int, Long>>(emptyMap()) }
    var availabilityReady by remember(catalog) { mutableStateOf(false) }
    LaunchedEffect(catalog, nowMillis) {
        val data = catalog ?: return@LaunchedEffect
        val now = nowMillis
        if (availabilityEnd.isEmpty()) availabilityReady = false
        val computed = withContext(Dispatchers.Default) {
            val open = HashMap<Int, Long>(data.fish.size)
            val next = HashMap<Int, Long>(data.fish.size)
            for (f in data.fish) {
                val window = FishingWindowCalculator.nextWindow(f, data, now - 1_000L)
                if (window == null) continue
                if (window.startMillis <= now && window.endMillis > now) {
                    open[f.id] = window.endMillis
                } else if (window.startMillis > now) {
                    next[f.id] = window.startMillis
                }
            }
            open to next
        }
        availabilityEnd = computed.first
        nextWindowStart = computed.second
        availabilityReady = true
    }
    val listState = rememberLazyListState()
    var pinHeader by remember { mutableStateOf(false) }
    var lastScrollPos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                val headerNaturalVisible = index == 0 && offset == 0
                val pos = index * 100000L + offset
                if (pos > lastScrollPos) {
                    pinHeader = false
                } else if (pos < lastScrollPos) {
                    pinHeader = !headerNaturalVisible
                }
                lastScrollPos = pos
            }
    }
    LaunchedEffect(filter, alarmsOnly) { listState.scrollToItem(0) }

    LaunchedEffect(Unit) {
        catalog = FishingCatalogRepository.load(context).also { FishingAlarmStore.refresh(context, it) }
    }
    BackHandler(enabled = selected != null) { selected = null }
    BackHandler(enabled = mapSpot != null) { mapSpot = null }

    val currentMapSpot = mapSpot
    val fishingRoute = selected?.id to mapSpot?.id
    AnimatedContent(
        targetState = fishingRoute,
        transitionSpec = { (fadeIn(tween(220)) + scaleIn(tween(240), initialScale = .97f)).togetherWith(fadeOut(tween(150)) + scaleOut(tween(170), targetScale = 1.02f)) },
        label = "fishing-detail",
    ) { route ->
        val fish = catalog?.fish?.firstOrNull { it.id == route.first }
        if (route.second != null && currentMapSpot != null && catalog != null) {
            FishingMapScreen(currentMapSpot, state) { mapSpot = null }
        } else if (fish != null && catalog != null) {
            FishingDetail(state, fish, catalog!!, alarmVersion, onBack = { selected = null }, onSpot = { mapSpot = it }) { enabled ->
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
                    // 首屏用骨架而不是"正在载入…"一行字：形状对齐真实的鱼行，
                    // 数据到了不会整屏跳一下。
                    PhoneListSkeleton(rows = 8, modifier = Modifier.fillMaxSize())
                } else {
                    val alarmIds = remember(alarmVersion, alarmsOnly) { FishingAlarmStore.enabledIds(context) }
                    val caught = remember(state.fishingLog, data) { data.fish.count { state.isFishCaught(it.logId, it.method) } }
                    val baseFiltered = remember(data, query, filter, alarmsOnly, alarmVersion, state.fishingLog) {
                        val needle = query.trim()
                        data.fish.asSequence()
                            .filter { !alarmsOnly || it.id in alarmIds }
                            .filter { filter.version == null || it.version.toInt() == filter.version }
                            .filter { needle.isBlank() || it.name.contains(needle, true) || it.spots.any { spot -> spot.name.contains(needle, true) || spot.region.contains(needle, true) || spot.zone.contains(needle, true) } }
                            .filter {
                                when (filter.kind) {
                                    "big" -> it.isBigFish
                                    "spear" -> it.method == "spear"
                                    else -> true
                                }
                            }
                            .filter {
                                when (filter.collected) {
                                    "caught" -> state.isFishCaught(it.logId, it.method)
                                    "missing" -> !state.isFishCaught(it.logId, it.method)
                                    else -> true
                                }
                            }.toList()
                    }
                    val filtered = remember(baseFiltered, availabilityEnd, filter) {
                        val list = baseFiltered.filter { f -> !filter.available || availabilityEnd[f.id] != null }.toMutableList()
                        list.sortWith(compareBy({ availabilityEnd[it.id] == null }, { availabilityEnd[it.id] ?: Long.MAX_VALUE }, { it.name }))
                        list
                    }
                    Box(Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            item("fishing-header") {
                                FishingListHeader(query, { query = it }, filter, { filter = it }, { filterPanel = true }, caught, data.fish.size, state.fishingLog != null, showCounts = true)
                            }
                            if (filtered.isEmpty()) item {
                                // 空态给图标 + 一句下一步，不是干巴巴一行字。
                                Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                    when {
                                        filter.available && !availabilityReady ->
                                            PhoneEmpty("正在计算可捕获时间", "窗口要按艾欧泽亚时和天气逐条推算，稍等一下", R.drawable.ic_timer)
                                        alarmsOnly ->
                                            PhoneEmpty("还没有设置捕鱼闹钟", "在鱼的详情页点闹钟，窗口开始前会提醒你", R.drawable.ic_alarm_bell)
                                        else ->
                                            PhoneEmpty("没有符合条件的鱼", "放宽筛选或清掉搜索词再看看", R.drawable.ic_search)
                                    }
                                }
                            }
                            items(filtered, key = { "${it.method}-${it.id}" }) { fishRow ->
                                Box(Modifier.animateItem()) {
                                    FishingRow(
                                        fishRow,
                                        state.isFishCaught(fishRow.logId, fishRow.method),
                                        fishRow.id in alarmIds,
                                        availabilityEnd[fishRow.id]?.let { it - displayNow },
                                        nextWindowStart[fishRow.id],
                                    ) { selected = fishRow }
                                }
                            }
                        }
                        androidx.compose.animation.AnimatedVisibility(
                            visible = pinHeader,
                            modifier = Modifier.align(Alignment.TopCenter),
                            enter = fadeIn(tween(160)) + expandVertically(tween(160)),
                            exit = fadeOut(tween(120)) + shrinkVertically(tween(120)),
                        ) {
                            Column(Modifier.fillMaxWidth().background(PhoneBackground).padding(horizontal = 14.dp, vertical = 4.dp)) {
                                FishingListHeader(query, { query = it }, filter, { filter = it }, { filterPanel = true }, caught, data.fish.size, state.fishingLog != null, showCounts = false)
                            }
                        }
                        if (filterPanel) {
                            // 面板里改的是草稿，点"看结果"才写回——边选边过滤会让列表
                            // 在脚下一直跳。
                            var draft by remember { mutableStateOf(filter) }
                            PhoneFilterPanel(
                                onClose = { filterPanel = false },
                                onReset = { draft = FishFilter() },
                                onApply = { filter = draft; filterPanel = false },
                                applyLabel = "看结果",
                            ) {
                                PhoneChipGroup(
                                    "窗口",
                                    listOf("" to "全部", "now" to "现在可捕获"),
                                    selected = setOf(if (draft.available) "now" else ""),
                                ) { draft = draft.copy(available = it == "now") }
                                PhoneChipGroup(
                                    "种类",
                                    FishFilter.KINDS,
                                    selected = setOf(draft.kind),
                                ) { draft = draft.copy(kind = it) }
                                PhoneChipGroup(
                                    "钓鱼笔记",
                                    FishFilter.COLLECTED,
                                    selected = setOf(draft.collected),
                                ) { draft = draft.copy(collected = it) }
                                PhoneChipGroup(
                                    "资料片",
                                    expansionTabs.map { (it.version?.toString() ?: "") to it.label },
                                    selected = setOf(draft.version?.toString() ?: ""),
                                ) { id -> draft = draft.copy(version = id.toIntOrNull()) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 列表页头。
 *
 * 历次形态：三行堆叠（搜索 / 7 个版本平铺 / 6 个筛选平分）→ 搜索 + 一行横滑
 * chip + 版本下拉 → 现在的搜索 + 筛选条。
 *
 * chip 横滑那一版的问题是：六个 chip 一行放不下，滑动才能看到后面的，
 * 而且六个值混了三个维度（窗口/种类/笔记），只能选一个。
 * 现在条件收进面板（和石之家的招募筛选同一套件），页头只留"当前筛的是什么"，
 * 每个条件后面带 × 可以直接摘掉。
 */
@Composable
private fun FishingListHeader(
    query: String,
    onQuery: (String) -> Unit,
    filter: FishFilter,
    onFilter: (FishFilter) -> Unit,
    onOpenFilter: () -> Unit,
    caught: Int,
    total: Int,
    synced: Boolean,
    showCounts: Boolean = true,
) {
    // 横向边距由外层列表统一给（LazyColumn / 悬浮头各自 padding），这里不再自己加。
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(10.dp)).background(PhoneSurfaceRaised).padding(horizontal = 12.dp)) {
            ImageGlyph(R.drawable.ic_search, PhoneMuted, Modifier.size(17.dp))
            BasicTextField(query, onQuery, singleLine = true, textStyle = TextStyle(color = PhoneText, fontSize = 14.sp), modifier = Modifier.weight(1f).padding(horizontal = 9.dp), decorationBox = { field -> Box(contentAlignment = Alignment.CenterStart) { if (query.isBlank()) Text("搜索鱼类、钓场或地区", color = PhoneMuted, fontSize = 13.sp); field() } })
            if (query.isNotEmpty()) {
                Box(Modifier.size(24.dp).clip(CircleShape).clickable { onQuery("") }, contentAlignment = Alignment.Center) {
                    ImageGlyph(R.drawable.ic_close_circle, PhoneMuted, Modifier.size(16.dp))
                }
            }
        }
        PhoneFilterBar(
            active = buildList {
                if (filter.available) add("现在可捕获" to { onFilter(filter.copy(available = false)) })
                FishFilter.KINDS.firstOrNull { it.first == filter.kind && it.first.isNotEmpty() }
                    ?.let { (_, label) -> add(label to { onFilter(filter.copy(kind = "")) }) }
                FishFilter.COLLECTED.firstOrNull { it.first == filter.collected && it.first.isNotEmpty() }
                    ?.let { (_, label) -> add(label to { onFilter(filter.copy(collected = "")) }) }
                filter.version?.let { v ->
                    val label = expansionTabs.firstOrNull { it.version == v }?.label ?: "$v.0"
                    add(label to { onFilter(filter.copy(version = null)) })
                }
            },
            onOpen = onOpenFilter,
            modifier = Modifier.padding(top = 9.dp),
        )
        if (showCounts) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("钓鱼笔记", color = PhoneText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (synced) "$caught / $total" else "$total 条资料 · 等待游戏同步",
                        color = if (synced) PhoneText else PhoneMuted,
                        fontSize = 11.sp,
                        fontWeight = if (synced) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                if (synced && total > 0) {
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(PhoneSurfaceRaised)) {
                        Box(
                            Modifier.fillMaxWidth((caught.toFloat() / total).coerceIn(0f, 1f))
                                .height(4.dp).clip(RoundedCornerShape(2.dp)).background(PhoneGreen),
                        )
                    }
                }
            }
        }
    }
}

/**
 * 列表一行。
 *
 * 这一屏最重要的信息是"现在能不能钓、还剩多久"，原来却是 10sp 灰字，
 * 输给了 14sp 的鱼名。现在可捕获的行左边有一条绿色竖条，倒计时单独占右侧
 * 一列并且是这一行最大的数字——扫一眼就知道该去钓哪条。
 *
 * 徽章原来一行能挂五个（含"版本 x.y"）。版本在右上角的下拉里已经能筛、
 * 详情页也写着，行里再挂一遍纯属噪声，删掉；只留身份（鱼王/刺鱼）和
 * 条件提示（ET/天气）。
 */
@Composable
private fun FishingRow(
    fish: FishingFish,
    caught: Boolean,
    alarm: Boolean,
    remainingMillis: Long?,
    nextWindowMillis: Long?,
    onClick: () -> Unit,
) {
    val available = remainingMillis != null
    // 一行一张卡，和聊天/联系人列表同一套形态（14dp 圆角 + surface 底 + 左色条）。
    // 原来是贴满宽度的扁平行、靠 1dp 间隔分隔，跟别的列表不是一个东西。
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PhoneSurface)
            .clickable(onClick = onClick)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 可捕获的左侧竖条。不可捕获时留同宽的空位，图标才不会左右跳。
        Box(Modifier.width(3.dp).fillMaxHeight().background(if (available) PhoneGreen else Color.Transparent))
        Row(
            Modifier.weight(1f).padding(start = 11.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)).background(PhoneSurfaceRaised), contentAlignment = Alignment.Center) {
                ItemIcon(fish.icon, Modifier.fillMaxSize(), fish.name.take(2))
                if (caught) {
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(17.dp).clip(CircleShape).background(PhoneGreen),
                        contentAlignment = Alignment.Center,
                    ) { ImageGlyph(R.drawable.ic_check_small, Color.White, Modifier.size(11.dp)) }
                }
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fish.name,
                        color = if (caught) PhoneText else PhoneText.copy(alpha = .82f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (alarm) {
                        Spacer(Modifier.width(5.dp))
                        ImageGlyph(R.drawable.ic_alarm_bell, PhoneAccent, Modifier.size(12.dp))
                    }
                }
                val place = fish.spots.firstOrNull()?.let { listOf(it.region, it.name).filter(String::isNotBlank).distinct().joinToString(" · ") }.orEmpty()
                Text(
                    place.ifBlank { if (fish.method == "spear") "刺鱼笔记" else "钓鱼笔记" },
                    color = PhoneMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                // 徽章配色收敛成两类，不再一行四个色（原来蓝/accent/紫/橙各一个）：
                //   身份类（鱼王）→ accent，它是"这条鱼值不值得专门跑一趟"
                //   条件类（刺鱼/ET/天气）→ PhoneInfo，都是"能不能钓"的限定条件
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 5.dp)) {
                    if (fish.isBigFish) MiniBadge("鱼王", PhoneAccent)
                    if (fish.method == "spear") MiniBadge("刺鱼", PhoneInfo)
                    if (fish.startHour != 0.0 || fish.endHour != 24.0) MiniBadge("ET ${fish.startText}-${fish.endText}", PhoneInfo)
                    if (fish.weather.isNotEmpty()) MiniBadge("天气", PhoneInfo)
                }
            }
            // 倒计时列：可捕获时是这一行最显眼的东西；
            // 不可捕获时显示距下一个窗口还有多久，整列永远有信息。
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                if (available) {
                    Text("可捕获", color = PhoneGreen, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatRemainingSeconds(remainingMillis!!),
                        color = PhoneGreen, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    )
                } else if (nextWindowMillis != null) {
                    Text("下次", color = PhoneMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        formatNextWindow(nextWindowMillis),
                        color = PhoneMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
                    )
                } else {
                    // 连下一个窗口都算不出来（没有窗口条件 = 全天可钓，或者资料缺失）。
                    Text("全天", color = PhoneMuted.copy(alpha = .55f), fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun FishingDetail(state: PhoneState, fish: FishingFish, catalog: FishingCatalog, alarmVersion: Int, onBack: () -> Unit, onSpot: (FishingSpot) -> Unit, onAlarm: (Boolean) -> Unit) {
    val context = LocalContext.current
    val alarm = remember(fish.id, alarmVersion) { FishingAlarmStore.isEnabled(context, fish.id) }
    var leadMinutes by remember(alarmVersion) { mutableStateOf(FishingAlarmStore.leadMinutes(context)) }
    val caught = state.isFishCaught(fish.logId, fish.method)
    val window = remember(fish, catalog, System.currentTimeMillis() / 60_000L) { FishingWindowCalculator.nextWindow(fish, catalog) }
    ScreenFrame {
        ScreenHeader(fish.name, state, onBack = onBack)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneSurface).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(10.dp)).background(PhoneSurfaceRaised), contentAlignment = Alignment.Center) { ItemIcon(fish.icon, Modifier.fillMaxSize(), fish.name.take(2)) }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(fish.name, color = PhoneText, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Text("版本 ${formatPatch(fish.version)} · ${if (fish.method == "spear") "刺鱼" else "钓鱼"} · ${if (caught) "已捕获" else "未捕获"}", color = if (caught) PhoneGreen else PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (caught) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(PhoneGreen), contentAlignment = Alignment.Center) {
                            ImageGlyph(R.drawable.ic_check_small, Color.White, Modifier.size(16.dp))
                        }
                    } else {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape)
                                .border(1.5.dp, PhoneMuted.copy(alpha = .5f), CircleShape),
                        )
                    }
                }
            }
            // 窗口和提醒拆成两段：一段回答"什么时候能钓"，一段才是"要不要叫我"。
            // 原来滑杆和闹钟按钮塞在窗口那段里，倒计时被推到很上面，两件事都说不清。
            item {
                DetailSection("下次捕获窗口") {
                    var detailNow by remember(fish.id) { mutableLongStateOf(System.currentTimeMillis()) }
                    LaunchedEffect(fish.id) {
                        while (true) {
                            delay(1_000)
                            detailNow = System.currentTimeMillis()
                        }
                    }
                    if (window == null) Text("暂未计算到可用窗口", color = PhoneMuted, fontSize = 13.sp) else {
                        val openNow = window.startMillis <= detailNow && window.endMillis > detailNow
                        val startingNow = window.startMillis <= detailNow + 1_000L
                        if (openNow) {
                            // 正在窗口里：把剩余时间做成这一屏最大的数字。
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    formatRemainingSeconds(window.endMillis - detailNow),
                                    color = PhoneGreen, fontSize = 30.sp, fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "后关闭",
                                    color = PhoneGreen.copy(alpha = .75f), fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 6.dp, bottom = 5.dp),
                                )
                            }
                            Text("现在可以捕获", color = PhoneGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 2.dp))
                        } else {
                            Text(
                                if (startingNow) "现在可以捕获" else formatWindow(window.startMillis, window.endMillis),
                                color = if (startingNow) PhoneGreen else PhoneText,
                                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            )
                            if (!startingNow) {
                                Text(
                                    "还有 ${formatRemainingSeconds(window.startMillis - detailNow)}",
                                    color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                        window.spot?.let { Text("${it.region} · ${it.zone} · ${it.name}", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
                    }
                }
            }
            item {
                DetailSection("捕鱼提醒") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("提前提醒", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text(
                            if (leadMinutes == 0) "窗口出现时" else "提前 $leadMinutes 分钟",
                            color = PhoneAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Slider(
                        value = leadMinutes.toFloat(),
                        onValueChange = { leadMinutes = it.roundToInt().coerceIn(0, 10) },
                        onValueChangeFinished = { FishingAlarmStore.updateLeadMinutes(context, catalog, leadMinutes) },
                        valueRange = 0f..10f,
                        steps = 9,
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                    )
                    Button(
                        onClick = { onAlarm(!alarm) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (alarm) PhoneSurfaceRaised else PhoneAccent,
                            contentColor = if (alarm) PhoneText else Color.White,
                        ),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        ImageGlyph(
                            R.drawable.ic_alarm_bell,
                            if (alarm) PhoneText else Color.White,
                            Modifier.size(15.dp),
                        )
                        Text(if (alarm) "取消闹钟" else "设置捕鱼提醒", modifier = Modifier.padding(start = 7.dp))
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
                    if (fish.path.isNotEmpty()) FishingTechniquePath(fish, catalog)
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
                        Column(Modifier.fillMaxWidth().clickable { if (spot.mapFile.isNotBlank()) onSpot(spot) }.padding(vertical = 5.dp)) {
                            Text(spot.name, color = PhoneText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(listOf(spot.region, spot.zone).filter(String::isNotBlank).distinct().joinToString(" · "), color = PhoneMuted, fontSize = 11.sp)
                            if (spot.mapFile.isNotBlank()) Text("查看地图 · ${spot.displayPosition()}", color = PhoneAccent, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
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
            if (fish.guide.isNotBlank() || fish.guidePath.isNotBlank()) item {
                val guideAccent = PhoneAccent
                DetailSection("攻略") {
                    if (fish.guidePath.isNotBlank()) {
                        Text("推荐路线", color = PhoneMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        guideParagraphs(fish.guidePath, guideAccent).forEach { paragraph ->
                            Text(paragraph, color = PhoneText, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    if (fish.guide.isNotBlank()) {
                        Text("钓法说明", color = PhoneMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = if (fish.guidePath.isNotBlank()) 12.dp else 0.dp))
                        guideParagraphs(fish.guide, guideAccent).forEach { paragraph ->
                            Text(paragraph, color = PhoneText, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    if (fish.guideAuthor.isNotBlank()) Text("来源：${fish.guideAuthor}", color = PhoneMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            item { Text("资料已内置于 APP，来源参考鱼糕与 GatherBuddyReborn。", color = PhoneMuted, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) }
        }
    }
}

@Composable
private fun FishingMapScreen(spot: FishingSpot, state: PhoneState, onBack: () -> Unit) {
    val context = LocalContext.current
    var bitmap by remember(spot.mapFile) { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loadFinished by remember(spot.mapFile) { mutableStateOf(spot.mapFile.isBlank()) }
    var mapScale by remember(spot.mapFile) { mutableStateOf(1f) }
    var mapPanX by remember(spot.mapFile) { mutableStateOf(0f) }
    var mapPanY by remember(spot.mapFile) { mutableStateOf(0f) }
    var pendingTeleport by remember { mutableStateOf<String?>(null) }
    val transformState = rememberTransformableState { zoom, pan, _ ->
        mapScale = (mapScale * zoom).coerceIn(1f, 4f)
        mapPanX += pan.x
        mapPanY += pan.y
    }
    LaunchedEffect(spot.mapFile) {
        bitmap = FishingMapImageLoader.load(context.applicationContext, spot.mapFile)
        loadFinished = true
    }
    ScreenFrame {
        ScreenHeader(spot.name, state, onBack = onBack, trailing = { Text("地图", color = PhoneMuted, fontSize = 12.sp) })
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(listOf(spot.region, spot.zone).filter(String::isNotBlank).distinct().joinToString(" · "), color = PhoneMuted, fontSize = 12.sp)
                if (bitmap == null) {
                    Box(Modifier.fillMaxWidth().aspectRatio(1f), contentAlignment = Alignment.Center) {
                        Text(when {
                            spot.mapFile.isBlank() -> "该钓场暂无地图资料"
                            loadFinished -> "地图资料暂时无法加载"
                            else -> "正在载入地图…"
                        }, color = PhoneMuted)
                    }
                } else {
                    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).transformable(transformState)) {
                        val mapWidth = maxWidth
                        val mapHeight = maxHeight
                        Box(Modifier.fillMaxSize().graphicsLayer(scaleX = mapScale, scaleY = mapScale, translationX = mapPanX, translationY = mapPanY)) {
                        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "${spot.name}地图", contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
                        if (spot.x > 0 && spot.y > 0) {
                            val x = (spot.x / 2048f).coerceIn(0f, 1f)
                            val y = (spot.y / 2048f).coerceIn(0f, 1f)
                            if (spot.radius > 0) {
                                // Canvas lambda 非 composable，accent 先在外面取。
                                val accent = PhoneAccent
                                Canvas(Modifier.fillMaxSize()) {
                                    val radius = size.minDimension * (spot.radius / 6.25f / 2048f)
                                    val center = Offset(size.width * x, size.height * y)
                                    drawCircle(accent.copy(alpha = .17f), radius, center)
                                    drawCircle(accent.copy(alpha = .78f), radius, center, style = Stroke(2.dp.toPx()))
                                }
                            }
                        }
                        spot.aetherytes.forEach { crystal ->
                            val x = (crystal.x / 2048f).coerceIn(0f, 1f)
                            val y = (crystal.y / 2048f).coerceIn(0f, 1f)
                            Column(
                                Modifier.offset(x = mapWidth * x - 28.dp, y = mapHeight * y - 14.dp)
                                    .clickable(enabled = state.connected && crystal.name.isNotBlank()) { pendingTeleport = crystal.name },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                ItemIcon(60453, Modifier.size(28.dp), "晶")
                                if (crystal.name.isNotBlank()) {
                                    // 9sp 是这里的下限（地图上要塞好几个水晶名，
                                    // 再大就互相压）。原来 8sp + 2f 阴影在浅色地图上
                                    // 基本读不出来，所以给一块半透明深底 + 更实的阴影。
                                    Text(
                                        crystal.name,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        modifier = Modifier.padding(top = 1.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(Color(0x8C000000))
                                            .padding(horizontal = 3.dp, vertical = 1.dp),
                                        style = androidx.compose.ui.text.TextStyle(
                                            shadow = androidx.compose.ui.graphics.Shadow(Color(0xCC000000), Offset.Zero, 3f),
                                        ),
                                    )
                                }
                            }
                        }
                        }
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneSurface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("钓场位置", color = PhoneText, fontWeight = FontWeight.SemiBold)
                        Text("${spot.name} · ${spot.displayPosition()}", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Text("地图标记", color = PhoneAccent, fontSize = 11.sp)
                }
            }
        }
    }
    val target = pendingTeleport
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingTeleport = null },
            title = { Text("确认传送") },
            text = { Text("确定要传送到 ${target} 吗？") },
            confirmButton = { TextButton(onClick = { pendingTeleport = null; state.requestTeleport(target) }) { Text("是") } },
            dismissButton = { TextButton(onClick = { pendingTeleport = null }) { Text("否") } },
        )
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().animateContentSize().clip(RoundedCornerShape(12.dp)).background(PhoneSurface).padding(start = 16.dp, end = 14.dp, top = 14.dp, bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(PhoneAccent))
            Text(title, color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 9.dp))
        }
        Spacer(Modifier.height(10.dp))
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
private fun FishingTechniquePath(fish: FishingFish, catalog: FishingCatalog) {
    Text("推荐钓法", color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp, bottom = 5.dp))
    val nodes = fishingTechniqueChain(fish)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        nodes.forEachIndexed { index, node ->
            TechniqueNode(node, catalog.fish.firstOrNull { it.id == node.id })
            if (index < nodes.lastIndex) TechniqueArrow(if (index == 0) "上钩" else "以小钓大")
        }
    }
}

private fun fishingTechniqueChain(fish: FishingFish): List<FishingItemRef> {
    val target = FishingItemRef(fish.id, fish.name, fish.icon)
    // The generated path is already ordered bait -> small fish -> target fish.
    // Keep every entry: collapsing to the first mooch loses intermediate fish.
    return (fish.path.ifEmpty { fish.bait.take(1) } + target).distinctBy { it.id }
}

@Composable
private fun TechniqueNode(item: FishingItemRef, fish: FishingFish?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (fish != null) {
            Column(Modifier.width(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (fish.tug.isNotBlank()) MiniBadge(tugShort(fish.tug), Color(0xFFE8A83A)) else Spacer(Modifier.height(18.dp))
                HooksetIcon(fish.hook)
            }
        }
        TooltipIcon(item.icon, item.name, 46.dp, item.name.take(1))
    }
}

@Composable
private fun HooksetIcon(value: String) {
    val resource = when (value) { "precision" -> R.drawable.precision_hookset; "powerful" -> R.drawable.powerful_hookset; else -> 0 }
    if (resource != 0) {
        var show by remember { mutableStateOf(false) }
        Box {
            Image(painterResource(resource), contentDescription = hookLabel(value), modifier = Modifier.size(20.dp).clickable { show = !show })
            if (show) {
                Popup(
                    alignment = Alignment.TopCenter,
                    offset = IntOffset(0, with(LocalDensity.current) { 24.dp.roundToPx() }),
                    onDismissRequest = { show = false },
                    properties = PopupProperties(focusable = true),
                ) {
                    TooltipBubble(hookLabel(value))
                }
            }
        }
    } else {
        Spacer(Modifier.size(20.dp))
    }
}

@Composable
private fun TooltipIcon(icon: Int, name: String, size: Dp = 46.dp, fallback: String = name.take(1)) {
    var show by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    Box {
        Box(Modifier.size(size).clip(RoundedCornerShape(8.dp)).background(PhoneSurfaceRaised).clickable { show = !show }, contentAlignment = Alignment.Center) {
            ItemIcon(icon, Modifier.fillMaxSize(), fallback)
        }
        if (show) {
            Popup(
                alignment = Alignment.TopCenter,
                offset = IntOffset(0, with(density) { (size + 8.dp).roundToPx() }),
                onDismissRequest = { show = false },
                properties = PopupProperties(focusable = true),
            ) {
                TooltipBubble(name)
            }
        }
    }
}

@Composable
private fun TooltipBubble(text: String) {
    val container = MaterialTheme.colorScheme.surfaceVariant
    val content = MaterialTheme.colorScheme.onSurfaceVariant
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(container).padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(text, color = content, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TechniqueArrow(label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 1.dp)) {
        ImageGlyph(R.drawable.ic_chevron_right, PhoneMuted, Modifier.size(18.dp))
        Text(label, color = PhoneMuted, fontSize = 8.sp, maxLines = 1)
    }
}

@Composable
private fun ItemPath(label: String, items: List<FishingItemRef>) {
    Text(label, color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp, bottom = 5.dp))
    items.forEachIndexed { index, item ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
            TooltipIcon(item.icon, item.name, 32.dp, item.name.take(1))
            Text(item.name, color = PhoneText, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
            if (index < items.lastIndex) {
                ImageGlyph(R.drawable.ic_chevron_right, PhoneAccent, Modifier.size(18.dp))
                Spacer(Modifier.width(2.dp))
            }
        }
    }
}

@Composable
// 10sp 是全局字号下限。8sp 在高密度屏上已经开始糊，而这几个徽章
// （刺鱼 / 鱼王 / ET / 天气）恰恰是判断"这条鱼要不要现在去钓"的关键信息。
private fun MiniBadge(text: String, color: Color = PhoneMuted, modifier: Modifier = Modifier) {
    Text(text, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = modifier.clip(CircleShape).background(color).padding(horizontal = 6.dp, vertical = 2.dp))
}

private fun FishingFish.weatherNames(catalog: FishingCatalog): String = weather.mapNotNull { catalog.weather[it]?.name }.joinToString(" / ")
private fun tugLabel(value: String): String = when (value) { "light" -> "轻竿 !"; "medium" -> "中竿 !!"; "heavy" -> "重竿 !!!"; else -> "竿型未知" }
private fun hookLabel(value: String): String = when (value) { "precision" -> "精准提钩"; "powerful" -> "强力提钩"; else -> value }
private fun tugShort(value: String): String = when (value) { "light" -> "!"; "medium" -> "!!"; "heavy" -> "!!!"; else -> value }
private fun hookShort(value: String): String = when (value) { "precision" -> "精准"; "powerful" -> "强力"; else -> value }
private fun lureLabel(value: String, stacks: Int): String = when (value) { "modest" -> "谦逊之饵${if (stacks > 0) " ×$stacks" else ""}"; "ambitious" -> "雄心之饵${if (stacks > 0) " ×$stacks" else ""}"; else -> value }
private fun formatPatch(value: Double): String = if (value == value.toInt().toDouble()) "${value.toInt()}.0" else String.format(Locale.US, "%.2f", value).trimEnd('0')
private fun formatWindow(start: Long, end: Long): String {
    val formatter = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val endFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return "${formatter.format(Date(start))} - ${endFormatter.format(Date(end))}"
}

private fun formatRemaining(millis: Long): String {
    val totalMinutes = (millis / 60_000L).coerceAtLeast(1)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}小时${minutes}分"
        hours > 0 -> "${hours}小时"
        else -> "${minutes}分"
    }
}

/**
 * 下一个窗口的说法。近的说"还有多久"，远的直接说钟点——
 * "14小时后"没有"明天 14:00"好用。
 */
private fun formatNextWindow(startMillis: Long): String {
    val delta = startMillis - System.currentTimeMillis()
    if (delta <= 0) return "即将开始"
    val minutes = delta / 60_000L
    return when {
        minutes < 1 -> "不到 1 分钟"
        minutes < 60 -> "${minutes}分钟后"
        minutes < 12 * 60 -> {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0L) "${h}小时后" else "${h}小时${m}分后"
        }
        else -> {
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = startMillis }
            val clock = String.format("%02d:%02d", cal.get(java.util.Calendar.HOUR_OF_DAY), cal.get(java.util.Calendar.MINUTE))
            // 按"过了几个午夜"算天数差，不是按 24 小时——21:00 看"次日 09:00"
            // 差 12 小时但确实是明天。
            val midnight = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis
            when (((startMillis - midnight) / 86_400_000L)) {
                0L -> clock
                1L -> "明天 $clock"
                else -> String.format("%d/%d %s", cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH), clock)
            }
        }
    }
}

private fun formatRemainingSeconds(millis: Long): String {
    val totalSeconds = (millis / 1_000L).coerceAtLeast(1)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}小时${minutes}分${seconds}秒"
        minutes > 0 -> "${minutes}分${seconds}秒"
        else -> "${seconds}秒"
    }
}

private fun FishingSpot.displayPosition(): String {
    val scale = mapSizeFactor.coerceAtLeast(100) / 100f
    val xPos = 41f / scale * (x / 2048f) + 1f
    val yPos = 41f / scale * (y / 2048f) + 1f
    return "X ${"%.1f".format(Locale.US, xPos)}, Y ${"%.1f".format(Locale.US, yPos)}"
}

private fun stripGuideMarkup(value: String): String = value
    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("<[^>]+>"), "")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")

private fun String.decodeHtmlEntities(): String = this
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")

// accent 从调用方传进来：这两个函数不是 composable，而品牌色现在跟主题走。
private fun guideParagraphs(raw: String, accent: Color): List<AnnotatedString> = raw
    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    .decodeHtmlEntities()
    .split("\n")
    .map { it.trim() }
    .filter { it.isNotBlank() }
    .map { buildGuideAnnotated(it, accent) }

private fun buildGuideAnnotated(line: String, accent: Color): AnnotatedString {
    val result = AnnotatedString.Builder()
    val spanRegex = Regex("""<span\s+style="color:([^"]+)">([^<]*)</span>""")
    val skillRegex = Regex("<([^<>]{1,12})>")
    val itemRegex = Regex("【([^】]{1,40})】")
    val tagRegex = Regex("<[^>]+>")
    var from = 0
    while (from < line.length) {
        val candidates = listOfNotNull(
            spanRegex.find(line, from)?.let { it to "span" },
            skillRegex.find(line, from)?.let { it to "skill" },
            itemRegex.find(line, from)?.let { it to "item" },
            tagRegex.find(line, from)?.let { it to "tag" },
        )
        val match = candidates.minByOrNull { it.first.range.first }
        if (match == null) {
            result.append(line.substring(from))
            break
        }
        val (m, kind) = match
        if (m.range.first > from) result.append(line.substring(from, m.range.first))
        when (kind) {
            "span" -> {
                val color = runCatching { Color(android.graphics.Color.parseColor(m.groupValues[1])) }.getOrNull() ?: Color(0xFFD8D8DE)
                result.withStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold)) { append(m.groupValues[2]) }
            }
            "skill" -> result.withStyle(SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)) { append("〈${m.groupValues[1]}〉") }
            "item" -> result.withStyle(SpanStyle(color = Color(0xFFFFC071), fontWeight = FontWeight.SemiBold)) { append(m.groupValues[0]) }
            else -> Unit
        }
        from = m.range.last + 1
    }
    return result.toAnnotatedString()
}
