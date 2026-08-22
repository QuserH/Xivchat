package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private fun eorzeaNow(): String {
    val seconds = System.currentTimeMillis() / 1_000.0 * 144.0 / 7.0
    val day = ((seconds.toLong() % 86_400L) + 86_400L) % 86_400L
    return String.format("%02d:%02d", day / 3_600L, day % 3_600L / 60L)
}

@Composable
fun HomeScreen(state: PhoneState) {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < .5f
    val homeText = if (darkTheme) Color.White else MaterialTheme.colorScheme.onBackground
    val totalPages = state.homePageCount
    val pager = rememberPagerState(
        initialPage = state.homePage.coerceIn(0, (totalPages - 1).coerceAtLeast(0)),
        pageCount = { totalPages },
    )
    LaunchedEffect(pager.currentPage) { state.homePage = pager.currentPage }
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.wallpaper_dusk_dark),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = if (darkTheme) 1f else .07f },
        )
        Box(Modifier.fillMaxSize().background(if (darkTheme) Color(0x35000020) else MaterialTheme.colorScheme.background.copy(alpha = .90f)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = LocalContentMargin.current.dp),
        ) {
            HomeEditBar(state, homeText)
            HorizontalPager(
                state = pager,
                userScrollEnabled = !state.homeEditMode,
                modifier = Modifier.weight(1f),
            ) { page ->
                SocialPage(state, page)
            }

            PageIndicator(pager.currentPage, totalPages, homeText)
            Dock(state, darkTheme)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HomeEditBar(state: PhoneState, homeText: Color) {
    if (state.homeEditMode) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("编辑主屏幕", color = homeText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("长按拖拽排序 · 点 − 移除", color = homeText.copy(alpha = .8f), fontSize = 11.sp)
            Text(
                "完成",
                color = Color(0xFF5BC0EB),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x3366DDFF))
                    .clickable { state.exitEditMode() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun SocialPage(state: PhoneState, page: Int) {
    Column(Modifier.fillMaxSize()) {
        if (page == 0) {
            WeatherWidget(state, Modifier.padding(vertical = 8.dp))
        }
        AppsGrid(state.appsForPage(page), page, state)
    }
}

@Composable
private fun AppsGrid(apps: List<PhoneAppItem>, page: Int, state: PhoneState) {
    val hapticView = LocalView.current
    val bounds = remember { mutableStateMapOf<String, Rect>() }
    var dragId by remember(page) { mutableStateOf<String?>(null) }
    var dragOffset by remember(page) { mutableStateOf(Offset.Zero) }
    var originRect by remember(page) { mutableStateOf<Rect?>(null) }
    var originIndex by remember(page) { mutableStateOf(0) }
    // home grid layout: 4 columns, used to compute the drop slot from finger position
    val cols = 4

    val applyDrop = { fromApp: PhoneAppItem, fromIdx: Int, fromOrigin: Rect, dx: Float, dy: Float ->
        val cellW = fromOrigin.width.coerceAtLeast(1f)
        val cellH = fromOrigin.height.coerceAtLeast(1f)
        val col = fromIdx % cols + dx / cellW
        val row = fromIdx / cols + dy / cellH
        val index = (row * cols + col).roundToInt().coerceIn(0, apps.lastIndex)
        state.reorderHomeToIndex(page, fromApp.id, index)
    }

    Column(
        Modifier.fillMaxSize().padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        apps.chunked(cols).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { app ->
                    Box(Modifier.weight(1f)) {
                        HomeTile(
                            app = app,
                            editing = state.homeEditMode,
                            removable = app.id != "appstore",
                            dragging = dragId == app.id,
                            dragOffset = if (dragId == app.id) dragOffset else Offset.Zero,
                            dimmed = false,
                            onBounds = { bounds[app.id] = it },
                            onTap = {
                                if (state.haptics) hapticView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                if (state.homeEditMode) {
                                    state.removeFromHome(page, app.id)
                                } else {
                                    state.open(app, bounds[app.id])
                                }
                            },
                            onLongPress = {
                                state.homeEditMode = true
                            },
                            onDragStart = {
                                if (state.homeEditMode) {
                                    dragId = app.id
                                    originRect = bounds[app.id]
                                    originIndex = apps.indexOfFirst { it.id == app.id }.coerceAtLeast(0)
                                    dragOffset = Offset.Zero
                                }
                            },
                            onDrag = { delta ->
                                if (dragId == app.id) dragOffset += delta
                            },
                            onDragEnd = {
                                val fromApp = app
                                val fromOrigin = originRect ?: bounds[fromApp.id]
                                val fromIdx = originIndex
                                val dx = dragOffset.x
                                val dy = dragOffset.y
                                dragId = null
                                originRect = null
                                dragOffset = Offset.Zero
                                if (fromOrigin != null) applyDrop(fromApp, fromIdx, fromOrigin, dx, dy)
                            },
                        )
                    }
                }
                repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeTile(
    app: PhoneAppItem,
    editing: Boolean,
    removable: Boolean,
    dragging: Boolean,
    dragOffset: Offset,
    dimmed: Boolean,
    onBounds: (Rect) -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val homeText = if (MaterialTheme.colorScheme.background.luminance() < .5f) Color.White else MaterialTheme.colorScheme.onBackground
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(
                if (dragging) 1.12f else if (pressed) 0.88f else 1f,
                animationSpec = spring(dampingRatio = .62f, stiffness = 520f),
                label = "app-press",
            )
    val editDrag = editing
    val shake = rememberInfiniteTransition(label = "shake")
    val rotation by shake.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(120), RepeatMode.Reverse),
        label = "shake-rotation",
    )
    var bounds by remember { mutableStateOf(Rect.Zero) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .onGloballyPositioned {
                bounds = it.boundsInRoot()
                onBounds(it.boundsInRoot())
            }
            .graphicsLayer {
                scaleX = if (dragging) 1.12f else 1f
                scaleY = if (dragging) 1.12f else 1f
                alpha = if (dimmed) 0.45f else 1f
                translationX = if (dragging) dragOffset.x else 0f
                translationY = if (dragging) dragOffset.y else 0f
                shadowElevation = if (dragging) 18f else 0f
            }
            .then(
                if (editDrag) {
                    // In edit mode the tile is moved by drag; the remove/restore badge
                    // handles add/remove, so no clickable is attached here (avoids a
                    // long-press/drag gesture conflict that broke drag-reorder).
                    Modifier.pointerInput(app.id, true) {
                        detectDragGestures(
                            onDragStart = { onDragStart() },
                            onDrag = { change, delta -> change.consume(); onDrag(delta) },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() },
                        )
                    }
                } else {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = { onTap() },
                        onLongClick = { onLongPress() },
                    )
                }
            )
            .then(if (dragging) Modifier.zIndex(10f) else Modifier)
            .padding(vertical = 3.dp),
    ) {
            Box {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer {
                        scaleX = if (dragging) 1f else scale
                        scaleY = if (dragging) 1f else scale
                        rotationZ = if (editDrag && !dragging) rotation else 0f
                    }
                    .clip(MaterialTheme.shapes.large)
                    .background(app.color),
            ) {
                Image(
                    painter = painterResource(app.icon),
                    contentDescription = app.label,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(34.dp),
                )
            }
            if (editing && removable) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE53935))
                        .clickable { onTap() },
                ) {
                    Text("−", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Text(
            text = app.label,
            color = homeText,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
fun PhoneStatusBar() {
    val homeText = if (MaterialTheme.colorScheme.background.luminance() < .5f) Color.White else MaterialTheme.colorScheme.onBackground
    var eorzea by remember { mutableStateOf(eorzeaNow()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(5_000); eorzea = eorzeaNow() } }
    Box(Modifier.fillMaxWidth().height(30.dp)) {
        Text(eorzea, color = homeText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp))
    }
}

@Composable
private fun WeatherWidget(state: PhoneState, modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf(eorzeaNow()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(5_000); time = eorzeaNow() } }
    val weather = state.weather
    val bell = time.substringBefore(':').toIntOrNull() ?: 12
    val visual = phoneWeatherVisual(weather?.current.orEmpty(), bell)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { state.openApp("skywatcher") },
    ) {
        WeatherBackdrop(weather?.current.orEmpty(), bell, Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    AnimatedContent(weather?.current ?: "等待天气", label = "weather-title") { title -> Text(title, color = visual.ink, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Text(weather?.zone ?: "游戏内天气", color = visual.ink.copy(alpha = .72f), fontSize = 11.sp)
                }
                Text(time, color = visual.ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (weather?.forecast?.take(5) ?: emptyList()).forEach { window ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (window.minutesFromNow <= 0) "现在" else "${window.minutesFromNow}分", color = visual.ink.copy(alpha = .78f), fontSize = 10.sp)
                        Text(weatherGlyph(window.name), color = visual.ink, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(page: Int, count: Int, homeText: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
    ) {
        repeat(count.coerceAtLeast(1)) { index ->
            val width by animateDpAsState(if (index == page) 18.dp else 7.dp, label = "page-dot")
            Box(
                Modifier
                    .height(7.dp).width(width)
                    .clip(RoundedCornerShape(50))
                    .background(if (index == page) homeText else homeText.copy(alpha = .45f)),
            )
        }
    }
}

internal fun weatherGlyph(name: String): String = when {
    name.contains("雷") -> "ϟ"
    name.contains("雪") || name.contains("冰") -> "✻"
    name.contains("雨") -> "☂"
    name.contains("雾") || name.contains("尘") || name.contains("霾") -> "≋"
    name.contains("晴") || name.contains("碧") -> "☀"
    else -> "☁"
}

@Composable
private fun Dock(state: PhoneState, darkTheme: Boolean) {
    val hapticView = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .96f))
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCatalog.dock.forEach { app ->
            var bounds by remember(app.id) { mutableStateOf(Rect.Zero) }
            val interaction = remember(app.id, state) { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) 0.86f else 1f, spring(dampingRatio = .62f, stiffness = 520f), label = "dock-press")
            Box(Modifier.size(60.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(58.dp)
                        .onGloballyPositioned { bounds = it.boundsInRoot() }
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(MaterialTheme.shapes.large)
                        .background(app.color)
                        .clickable(interactionSource = interaction, indication = null) {
                            if (state.haptics) hapticView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            state.open(app, bounds)
                        },
                ) {
                    Image(
                        painter = painterResource(app.icon),
                        contentDescription = app.label,
                        colorFilter = ColorFilter.tint(Color.White),
                        modifier = Modifier.size(36.dp),
                    )
                }
                if (app.destination == PhoneScreen.Chat) {
                    val unread = state.conversations.filter { it.notify }.sumOf { it.unread }
                    if (unread > 0) {
                        Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-3).dp).height(18.dp).widthIn(min = 18.dp).clip(CircleShape).background(Color(0xFFE5485D)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(if (unread > 99) "99+" else unread.toString(), color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 3.dp))
                        }
                    }
                }
            }
        }
    }
}
