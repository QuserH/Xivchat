package com.quserh.eorzeaphone.ui

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.FishingMapImageLoader
import com.quserh.eorzeaphone.data.wiki.EorzeaTime
import com.quserh.eorzeaphone.data.wiki.GatherClockDb
import com.quserh.eorzeaphone.data.wiki.GatherNode
import com.quserh.eorzeaphone.ui.theme.BrandFill
import com.quserh.eorzeaphone.ui.theme.BrandOnFill
import com.quserh.eorzeaphone.ui.theme.CanvasLabelScrim
import com.quserh.eorzeaphone.ui.theme.CanvasLabelShadow
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import com.quserh.eorzeaphone.ui.theme.MapPin
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneInfo
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneOutline
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.PhoneWarn
import kotlinx.coroutines.delay

/**
 * 采集时钟 —— 限时采集点的倒计时。
 *
 * 形态照钓鱼笔记：一行一张卡（14dp 圆角 + surface 底 + 左色条），
 * 正在可采集的左边一条绿竖条，倒计时单独占右侧一列且是这一行最大的数字 ——
 * 这一屏最重要的信息是"现在能不能挖、还剩多久"，不能是 10sp 灰字。
 *
 * 只列限时点（226 个）。常驻点没有时钟意义，位置在物品检索详情页已有。
 * 数据全部来自内置库，**离线可用**；ET 换算见 [EorzeaTime]。
 */
@Composable
fun GatherClockScreen(state: PhoneState) {
    val context = LocalContext.current.applicationContext
    var all by remember { mutableStateOf<List<GatherNode>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failure by remember { mutableStateOf<String?>(null) }
    var onlyActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf<GatherNode?>(null) }

    BackHandler(enabled = detail != null) { detail = null }

    LaunchedEffect(Unit) {
        runCatching { GatherClockDb.timedNodes(context) }
            .onSuccess { all = it }
            .onFailure { failure = it.message ?: "读取采集点失败" }
        loading = false
    }

    // 一秒一跳。艾时一分钟只有 2.9 真实秒，跳慢了倒计时看着是卡的。
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(1000)
        }
    }

    val open = detail
    if (open != null) {
        GatherDetailScreen(state, open, nowMs) { detail = null }
        return
    }

    // 负值 = 正在开，升序排即"可采集"在最前，其次最快要开的
    val ranked = remember(all, nowMs / 1000) {
        all.asSequence()
            .map { it to EorzeaTime.nextWindowMs(it.etHours, it.durationEtMin, nowMs) }
            .filter { it.second != Long.MAX_VALUE }
            .sortedBy { it.second }
            .toList()
    }
    val q = query.trim()
    val shown = ranked.filter { (n, ms) ->
        (!onlyActive || ms < 0) &&
            (q.isEmpty() ||
                n.items.any { it.name.contains(q, true) } ||
                n.mapName.contains(q, true) ||
                n.areaName.contains(q, true) ||
                n.region.contains(q, true))
    }
    val activeCount = ranked.count { it.second < 0 }
    val (etH, etM) = EorzeaTime.nowHourMinute(nowMs)
    val margin = LocalContentMargin.current

    ScreenFrame {
        ScreenHeader(
            "采集时钟",
            state,
            trailing = {
                // 艾时钟放页头右上：整屏所有倒计时都以它为基准
                Text(
                    "艾 %02d:%02d".format(etH, etM),
                    color = PhoneAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                )
            },
        )

        GatherSearchField(query) { query = it }

        Row(
            Modifier.fillMaxWidth().padding(start = margin.dp, end = margin.dp, top = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (loading) "读取中…" else "$activeCount 处开放中 · 共 ${ranked.size} 处",
                color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.weight(1f),
            )
            PhonePressable(onClick = { onlyActive = !onlyActive }, shape = PhoneChipShape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    // 底用 BrandFill、字用 BrandOnFill，而不是 PhoneAccent + 白字。
                    // PhoneAccent 是**字色**（它自己的注释就这么写），当底用时
                    // 深色主题下取到 inkDark（浅色），白字压上去只有 1.81:1，
                    // 9 个强调色预设全部不达标。BrandOnFill 会按填充明度自己选
                    // 白字还是深墨。见 HANDOFF.md §5.3、wiki-feature/check_contrast.py。
                    modifier = Modifier.clip(PhoneChipShape)
                        .background(if (onlyActive) BrandFill else PhoneSurfaceRaised)
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                ) {
                    ImageGlyph(
                        R.drawable.ic2_filter,
                        if (onlyActive) BrandOnFill else PhoneMuted,
                        Modifier.size(13.dp),
                    )
                    Text(
                        "仅看开放中",
                        color = if (onlyActive) BrandOnFill else PhoneMuted,
                        fontSize = 12.sp,
                        fontWeight = if (onlyActive) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(start = 5.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        when {
            loading -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = PhoneAccent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp),
                )
            }
            failure != null -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                PhoneEmpty("读取采集点失败", failure, R.drawable.ic2_warning)
            }
            shown.isEmpty() -> Box(
                Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                PhoneEmpty(
                    if (onlyActive) "现在没有开放的采集点" else "没有符合条件的采集点",
                    if (onlyActive) "关掉「仅看开放中」看全部 226 处"
                    else "换个物品名或地区试试",
                    R.drawable.ic2_clock,
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(
                    start = margin.dp, end = margin.dp, bottom = 18.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                lazyItems(shown, key = { it.first.id }) { (node, ms) ->
                    GatherRow(node, ms) { detail = node }
                }
            }
        }
    }
}

@Composable
private fun GatherSearchField(value: String, onChange: (String) -> Unit) {
    val margin = LocalContentMargin.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = margin.dp)
            .height(42.dp).clip(RoundedCornerShape(10.dp))
            .background(PhoneSurfaceRaised).padding(horizontal = 12.dp),
    ) {
        ImageGlyph(R.drawable.ic2_search, PhoneMuted, Modifier.size(17.dp))
        BasicTextField(
            value, onChange, singleLine = true,
            textStyle = TextStyle(color = PhoneText, fontSize = 14.sp),
            modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text("搜索物品、地图或地区", color = PhoneMuted, fontSize = 13.sp)
                    }
                    field()
                }
            },
        )
        if (value.isNotEmpty()) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).clickable { onChange("") },
                contentAlignment = Alignment.Center,
            ) {
                ImageGlyph(R.drawable.ic2_close_circle, PhoneMuted, Modifier.size(16.dp))
            }
        }
    }
}

/** 列表一行。ms 为负表示正在开放，绝对值是剩余时间。 */
@Composable
private fun GatherRow(node: GatherNode, ms: Long, onClick: () -> Unit) {
    val active = ms < 0
    val remain = if (active) -ms else ms
    PhonePressable(onClick = onClick, shape = RoundedCornerShape(14.dp), pressedScale = 0.978f) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PhoneSurface),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 左色条：开放中绿、未开放淡。和钓鱼笔记同一套语言。
            Box(
                Modifier.width(4.dp).height(66.dp)
                    .background(if (active) PhoneGreen else PhoneOutline.copy(alpha = 0.22f)),
            )
            Column(
                Modifier.weight(1f).padding(start = 11.dp, top = 9.dp, bottom = 9.dp, end = 8.dp),
            ) {
                Text(
                    node.items.firstOrNull()?.name?.takeIf { it.isNotBlank() }
                        ?: "采集点 ${node.id}",
                    color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(node.placeText, node.coordText).filter { it.isNotBlank() }
                        .joinToString("  "),
                    color = PhoneMuted, fontSize = 11.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp),
                )
                Row(
                    Modifier.padding(top = 5.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (node.level > 0) {
                        GatherBadge(
                            "Lv${node.level}" + "★".repeat(node.stars.coerceIn(0, 3)),
                            PhoneInfo,
                        )
                    }
                    GatherBadge("艾 ${node.etHoursText}", PhoneMuted)
                    if (node.folkloreName.isNotBlank()) GatherBadge("需传承录", PhoneWarn)
                }
            }
            // 倒计时是这一行最大的数字
            Column(
                Modifier.padding(end = 12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    EorzeaTime.formatCountdown(remain),
                    color = if (active) PhoneGreen else PhoneText,
                    fontSize = 17.sp, fontWeight = FontWeight.Bold,
                )
                Text(
                    if (active) "后消失" else "后出现",
                    color = PhoneMuted, fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun GatherBadge(text: String, tint: Color) {
    Text(
        text, color = tint, fontSize = 9.sp, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/** 采集点详情：产出物品全列 + 窗口信息。 */
@Composable
private fun GatherDetailScreen(
    state: PhoneState,
    node: GatherNode,
    nowMs: Long,
    onBack: () -> Unit,
) {
    val margin = LocalContentMargin.current
    val ms = EorzeaTime.nextWindowMs(node.etHours, node.durationEtMin, nowMs)
    val active = ms < 0
    val remain = if (active) -ms else ms

    ScreenFrame {
        ScreenHeader(
            node.items.firstOrNull()?.name?.takeIf { it.isNotBlank() } ?: "采集点 ${node.id}",
            state,
            onBack = onBack,
        )
        // 用 Column + verticalScroll 而不是 LazyColumn ——
        // 地图卡里的定位针要 fillMaxSize() 才能按比例定位，而 LazyColumn 的 item
        // 高度无界，两者一起会抛 "Nesting scrollable in the same direction"。
        // FishingScreen 的详情页也是这么写的（内容固定，不需要 lazy）。
        // 占满剩余高度用 weight(1f)，不用 fillMaxSize()：这里在 ScreenFrame 的
        // ColumnScope 里，fillMaxSize 会吃掉全部剩余，以后谁在它后面加一行
        // 就被压成 0 高度。见 HANDOFF.md §5.1。
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                .padding(start = margin.dp, end = margin.dp, bottom = 20.dp),
        ) {
            run {
                // 倒计时横幅：开放中绿底，未开放中性底
                Column(
                    Modifier.fillMaxWidth().padding(top = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (active) PhoneGreen.copy(alpha = 0.14f) else PhoneSurface,
                        )
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        EorzeaTime.formatCountdown(remain),
                        color = if (active) PhoneGreen else PhoneText,
                        fontSize = 30.sp, fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (active) "开放中 · 剩余" else "距下次出现",
                        color = if (active) PhoneGreen else PhoneMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }

            if (node.mapFile.isNotBlank()) {
                GatherSection("地图")
                GatherMapCard(node, state)
            }

            GatherSection("位置")
            run {
                GatherFacts(
                    buildList {
                        node.region.takeIf { it.isNotBlank() }?.let { add("地区" to it) }
                        node.mapName.takeIf { it.isNotBlank() }?.let { add("地图" to it) }
                        node.areaName.takeIf { it.isNotBlank() }?.let { add("区域" to it) }
                        node.coordText.takeIf { it.isNotBlank() }?.let { add("坐标" to it) }
                    },
                )
            }

            GatherSection("采集条件")
            run {
                GatherFacts(
                    buildList {
                        node.jobName.takeIf { it.isNotBlank() }?.let { add("职业" to it) }
                        if (node.level > 0) {
                            add("等级" to ("Lv${node.level}" +
                                "★".repeat(node.stars.coerceIn(0, 3))))
                        }
                        add("出现时刻" to "艾 ${node.etHoursText}")
                        // 站点按艾时说"4小时"，这里两个都给，免得对不上
                        add("持续" to buildString {
                            append("${node.durationEtMin} 艾分")
                            val real = EorzeaTime.etMinutesToRealMs(node.durationEtMin) / 60000.0
                            append("（现实约 %.0f 分钟）".format(real))
                        })
                        node.folkloreName.takeIf { it.isNotBlank() }
                            ?.let { add("传承录" to it) }
                        add("采集点 ID" to node.id.toString())
                    },
                )
            }

            if (node.items.isNotEmpty()) {
                GatherSection("产出物品")
                run {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                            .background(PhoneSurface),
                    ) {
                        node.items.forEach { it2 ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(34.dp).clip(RoundedCornerShape(6.dp))
                                        .background(PhoneSurfaceRaised),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ItemIcon(
                                        it2.iconId, Modifier.fillMaxSize(),
                                        fallback = it2.name.take(2),
                                    )
                                }
                                Text(
                                    it2.name, color = PhoneText, fontSize = 13.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 采集点地图。红针标采集点，蓝色晶石标以太之光，点晶石直接传送。
 *
 * 底图和缩放都复用 App 现成的件：
 *   - [FishingMapImageLoader] 收 `mapFile`（如 `d2f3/00`），本身与钓鱼无关，
 *     缓存在 `cacheDir/maps/`
 *   - 坐标换算沿用 `FishingScreen` 的公式，反推成图上比例（见 [GatherNode.mapFracX]）
 *
 * 注意两套坐标不同源：采集点存的是**游戏坐标**（要换算），
 * 以太之光存的是**像素坐标**（直接除 2048）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GatherMapCard(node: GatherNode, state: PhoneState) {
    val context = LocalContext.current
    var bitmap by remember(node.mapFile) { mutableStateOf<Bitmap?>(null) }
    var loadDone by remember(node.mapFile) { mutableStateOf(false) }
    var pendingTeleport by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(node.mapFile) {
        bitmap = FishingMapImageLoader.load(context.applicationContext, node.mapFile)
        loadDone = true
    }

    val bmp = bitmap
    if (bmp == null) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp)).background(PhoneSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (loadDone) "地图资料暂时无法加载" else "正在载入地图…",
                color = PhoneMuted, fontSize = 12.sp,
            )
        }
        return
    }

    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)),
    ) {
        val w = maxWidth
        val h = maxHeight
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "${node.mapName}地图",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )

        // 以太之光先画，让采集点的红针盖在上面
        node.aetherytes.forEach { a ->
            Column(
                Modifier.offset(x = w * a.fracX - 28.dp, y = h * a.fracY - 14.dp)
                    .clickable(enabled = state.connected && a.name.isNotBlank()) {
                        pendingTeleport = a.name
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ItemIcon(60453, Modifier.size(26.dp), "晶")
                if (a.name.isNotBlank()) {
                    // 半透明深底 + 阴影：浅色地图上纯白字读不出来（钓鱼屏同样处理）
                    Text(
                        a.name, color = Color.White, fontSize = 9.sp,
                        fontWeight = FontWeight.Medium, maxLines = 1,
                        modifier = Modifier.padding(top = 1.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(CanvasLabelScrim)
                            .padding(horizontal = 3.dp, vertical = 1.dp),
                        style = TextStyle(
                            shadow = Shadow(CanvasLabelShadow, Offset.Zero, 3f),
                        ),
                    )
                }
            }
        }

        // 采集点：红圈 + 中心点。和任务 NPC 地图的针同一个 token，见 MapPin。
        val pinColor = MapPin
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width * node.mapFracX
            val cy = size.height * node.mapFracY
            val r = size.minDimension * 0.032f
            drawCircle(pinColor.copy(alpha = 0.22f), r, Offset(cx, cy))
            drawCircle(pinColor, r, Offset(cx, cy), style = Stroke(2.5.dp.toPx()))
            drawCircle(pinColor, r * 0.28f, Offset(cx, cy))
        }
    }

    val target = pendingTeleport
    if (target != null) {
        // 传送是会真的动游戏的操作，给一次确认
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingTeleport = null },
            title = { Text("传送到 $target？") },
            text = { Text("将向游戏发送传送指令。", color = PhoneMuted, fontSize = 12.sp) },
            confirmButton = {
                Text(
                    "传送", color = PhoneAccent, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { state.teleportTo(target); pendingTeleport = null }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            },
            dismissButton = {
                Text(
                    "取消", color = PhoneMuted,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { pendingTeleport = null }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            },
        )
    }
}

@Composable
private fun GatherSection(title: String) {
    Text(
        title, color = PhoneMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 16.dp, bottom = 5.dp),
    )
}

@Composable
private fun GatherFacts(rows: List<Pair<String, String>>) {
    if (rows.isEmpty()) return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(PhoneSurface)) {
        rows.forEachIndexed { i, (k, v) ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(k, color = PhoneMuted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(v, color = PhoneText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            if (i < rows.lastIndex) PhoneHairlineRow(12.dp)
        }
    }
}
