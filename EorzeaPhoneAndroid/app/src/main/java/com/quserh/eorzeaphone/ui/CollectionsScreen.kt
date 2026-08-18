package com.quserh.eorzeaphone.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.data.GameCollectionCategory
import com.quserh.eorzeaphone.data.GameCollectionItem
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneText

private data class CollectionKind(val id: Int, val label: String, val tint: Color)

private val collectionKinds = listOf(
    CollectionKind(0, "坐骑", Color(0xFFE8863C)),
    CollectionKind(1, "宠物", Color(0xFF4CC77A)),
    CollectionKind(2, "情感动作", Color(0xFFF0C24D)),
    CollectionKind(3, "乐谱", Color(0xFF9E75F5)),
)

@Composable
fun CollectionsScreen(state: PhoneState) {
    var selectedCategory by remember { mutableStateOf<Int?>(null) }
    val categories = state.collections?.categories.orEmpty()
    val active = categories.firstOrNull { it.id == selectedCategory }

    if (active == null) {
        CollectionsRoot(state, categories) { selectedCategory = it }
    } else {
        CollectionsBrowse(state, active) { selectedCategory = null }
    }
}

@Composable
private fun CollectionsRoot(state: PhoneState, categories: List<GameCollectionCategory>, open: (Int) -> Unit) {
    ScreenFrame {
        ScreenHeader("收藏馆", state, showBack = false,
            trailing = { Text(categories.sumOf { it.owned }.let { "$it 已收藏" }, color = PhoneMuted, fontSize = 12.sp) })
        if (categories.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (state.connected) "等待收藏数据…" else "连接游戏后查看坐骑、宠物等收藏", color = PhoneMuted)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("本地收藏", color = PhoneText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
                }
                lazyItems(collectionKinds, key = { it.id }) { kind ->
                    CollectionCategoryCard(kind, categories.firstOrNull { it.id == kind.id }) { open(kind.id) }
                }
                item {
                    Text("收藏进度由游戏插件实时同步，目前支持坐骑、宠物、情感动作与乐谱。", color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun CollectionCategoryCard(kind: CollectionKind, category: GameCollectionCategory?, onClick: () -> Unit) {
    val owned = category?.owned ?: 0
    val total = category?.total ?: 0
    val ratio = if (total > 0) owned.toFloat() / total else 0f
    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "progress")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PhoneSurface).clickable(onClick = onClick).padding(16.dp),
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)).background(kind.tint), contentAlignment = Alignment.Center) {
            Text(kind.label.take(1), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(kind.label, color = PhoneText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            if (category != null) {
                Spacer(Modifier.height(7.dp))
                Box(Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))) {
                    Box(Modifier.fillMaxWidth(animatedRatio.coerceIn(0f, 1f)).height(7.dp).clip(CircleShape).background(kind.tint))
                }
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
            Text(if (category == null) "--" else "$owned / $total", color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("已收藏", color = PhoneMuted, fontSize = 10.sp)
        }
        Text("›", color = PhoneMuted, fontSize = 26.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CollectionsBrowse(state: PhoneState, category: GameCollectionCategory, onBack: () -> Unit) {
    val kind = collectionKinds.firstOrNull { it.id == category.id }
    val tint = kind?.tint ?: PhoneAccent
    var query by remember { mutableStateOf("") }
    var ownership by remember { mutableStateOf(0) } // 0 all, 1 owned, 2 missing
    var showGuide by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<GameCollectionItem?>(null) }
    val label = kind?.label ?: "收藏"

    val filtered = category.items.filter {
        val matchesQuery = query.isBlank() || it.name.contains(query, ignoreCase = true)
        val matchesOwnership = when (ownership) {
            1 -> it.owned
            2 -> !it.owned
            else -> true
        }
        matchesQuery && matchesOwnership
    }
    val missingCount = (category.total - category.owned).coerceAtLeast(0)

    ScreenFrame {
        ScreenHeader(label, state, onBack = onBack,
            trailing = { Text("${category.owned} / ${category.total}", color = PhoneMuted, fontSize = 12.sp) })
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索", color = PhoneMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            shape = RoundedCornerShape(12.dp),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "全部", 1 to "已收藏", 2 to "未收藏").forEach { (value, text) ->
                Text(
                    text,
                    color = if (ownership == value) Color.White else PhoneMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.clip(CircleShape).background(if (ownership == value) tint else PhoneSurface)
                        .clickable { ownership = value }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text("?", color = tint, fontSize = 18.sp, modifier = Modifier.clickable { showGuide = true }.padding(horizontal = 6.dp, vertical = 8.dp))
        }
        if (filtered.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (ownership == 2) "全部已收藏，没有未收藏条目" else "没有匹配的收藏", color = PhoneMuted)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                gridItems(filtered, key = { it.id }) { item ->
                    CollectionsItemCell(item, tint) { selected = item }
                }
            }
        }
    }

    selected?.let { item ->
        CollectionItemDetail(item, tint, label, onDismiss = { selected = null })
    }

    AnimatedVisibility(visible = showGuide) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showGuide = false },
            title = { Text("收藏说明") },
            text = { Text("这里显示你已在游戏内解锁的$label。点击任意藏品可查看详情；进度数字是已收藏/总数。未收藏条目需要完整的收藏名录，后续版本补充。", color = PhoneText) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { showGuide = false }) { Text("知道了") } },
        )
    }
}

@Composable
private fun CollectionItemDetail(item: GameCollectionItem, tint: Color, label: String, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.name, color = PhoneText, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                    ItemIcon(item.iconId, Modifier.size(66.dp), fallback = item.name.take(2), tint = Color.White)
                }
                Text(
                    if (item.owned) "已在游戏内解锁" else "尚未解锁",
                    color = if (item.owned) tint else PhoneMuted,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp),
                )
                Text("$label 收藏之一", color = PhoneMuted, fontSize = 12.sp)
                Text(
                    if (item.owned) "获取来源：该藏品已记录在游戏解锁名录中。更多来源说明将在后续数据版本补充。"
                    else "获取来源：目前未纳入完整名录，无法给出确切获取途径。",
                    color = PhoneMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("关闭", color = tint) } },
    )
}

@Composable
private fun CollectionsItemCell(item: GameCollectionItem, tint: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneSurface).clickable(onClick = onClick).padding(vertical = 8.dp),
    ) {
        Box(Modifier.fillMaxWidth(0.86f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.06f)), contentAlignment = Alignment.Center) {
            ItemIcon(item.iconId, Modifier.fillMaxSize().padding(4.dp), fallback = item.name.take(2))
        }
        Text(item.name, color = PhoneText, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp, start = 3.dp, end = 3.dp))
    }
}
