package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(state: PhoneState) {
    val pager = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.wallpaper_dusk_dark),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color(0x35000020)))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 14.dp),
        ) {
            PhoneStatusBar()
            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
            ) { page ->
                if (page == 0) {
                    SocialPage(state)
                } else {
                    AppsGrid(AppCatalog.secondPage, state)
                }
            }

            PageIndicator(pager.currentPage)
            Dock(state)
            Spacer(Modifier.height(8.dp))
        }

        if (pager.currentPage > 0) PageArrow(left = true, Modifier.align(Alignment.CenterStart)) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }
        if (pager.currentPage < 1) PageArrow(left = false, Modifier.align(Alignment.CenterEnd)) { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }
    }
}

@Composable
fun PhoneStatusBar() {
    var time by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) } }
    Box(Modifier.fillMaxWidth().height(44.dp)) {
        Text(time, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.CenterStart).padding(start = 19.dp))
        Box(Modifier.align(Alignment.Center).size(width = 98.dp, height = 28.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black))
        Row(Modifier.align(Alignment.CenterEnd).padding(end = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("▂▄▆", color = Color.White, fontSize = 10.sp)
            Text("100%", color = Color.White, fontSize = 11.sp)
            Box(Modifier.size(width = 23.dp, height = 11.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = .92f)))
        }
    }
}

@Composable
private fun PageArrow(left: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.width(24.dp).height(62.dp).clip(if (left) RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)).background(Color(0x43000000)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(if (left) "‹" else "›", color = Color.White, fontSize = 30.sp)
    }
}

@Composable
private fun SocialPage(state: PhoneState) {
    Column(Modifier.fillMaxSize()) {
        WeatherWidget(state, Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
        AppsGrid(AppCatalog.firstPage, state)
    }
}

@Composable
private fun AppsGrid(apps: List<PhoneAppItem>, state: PhoneState) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
        userScrollEnabled = false,
    ) {
        items(apps, key = { it.id }) { app -> AppTile(app) { origin -> state.open(app, origin) } }
    }
}

@Composable
private fun AppTile(app: PhoneAppItem, onClick: (Rect) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = .62f, stiffness = 520f), label = "app-press")
    var bounds by remember { mutableStateOf(Rect.Zero) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(interactionSource = interaction, indication = null) { onClick(bounds) }
            .padding(vertical = 3.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(app.color),
        ) {
            Image(
                painter = painterResource(app.icon),
                contentDescription = app.label,
                colorFilter = ColorFilter.tint(Color.White),
                modifier = Modifier.size(34.dp),
            )
        }
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )
    }
}

@Composable
private fun WeatherWidget(state: PhoneState, modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) } }
    val weather = state.weather
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xB5728094))
            .padding(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                AnimatedContent(weather?.current ?: "等待天气", label = "weather-title") { title -> Text(title, color = Color(0xFF18243A), fontSize = 25.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                Text(weather?.zone ?: "游戏内天气", color = Color(0xFF273247), fontSize = 11.sp)
            }
            Text(time, color = Color(0xFF172239), fontSize = 29.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            (weather?.forecast?.take(5) ?: emptyList()).forEach { window ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (window.minutesFromNow <= 0) "现在" else "${window.minutesFromNow}分", color = Color(0xFF2B3547), fontSize = 10.sp)
                    Text(weatherGlyph(window.name), color = Color.White, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun PageIndicator(page: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
    ) {
        repeat(2) { index ->
            val width by animateDpAsState(if (index == page) 18.dp else 7.dp, label = "page-dot")
            Box(
                Modifier
                    .height(7.dp).width(width)
                    .clip(RoundedCornerShape(50))
                    .background(if (index == page) Color.White else Color(0x887D7890)),
            )
        }
    }
}

private fun weatherGlyph(name: String): String = when {
    name.contains("雷") -> "ϟ"
    name.contains("雪") || name.contains("冰") -> "✻"
    name.contains("雨") -> "☂"
    name.contains("雾") || name.contains("尘") || name.contains("霾") -> "≋"
    name.contains("晴") || name.contains("碧") -> "☀"
    else -> "☁"
}

@Composable
private fun Dock(state: PhoneState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(25.dp))
            .background(Color(0x7A323044))
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppCatalog.dock.forEach { app ->
            var bounds by remember(app.id) { mutableStateOf(Rect.Zero) }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(58.dp)
                    .onGloballyPositioned { bounds = it.boundsInRoot() }
                    .clip(RoundedCornerShape(14.dp))
                    .background(app.color)
                    .clickable { state.open(app, bounds) },
            ) {
                Image(
                    painter = painterResource(app.icon),
                    contentDescription = app.label,
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}
