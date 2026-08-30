package com.quserh.eorzeaphone.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText

// ---------------------------------------------------------------------------
// 商店的局部 token
//
// 只在这个文件里用。字号写成一小组具名值而不是随手撒 sp：
// 这个项目的字号阶梯已经有 39 个值了，再随手加只会更乱。
// ---------------------------------------------------------------------------

// 卡片圆角由 PhoneCard 决定（14dp），这里只留图标块自己的圆角。
// chip 用公共的 PhoneChipShape，不再自己定一个 9dp。
private val StoreTileShape = RoundedCornerShape(12.dp)

private const val StoreDisplaySp = 26      // 英雄区的数字
private const val StoreTitleSp = 15        // 应用名
private const val StoreBodySp = 12         // 介绍
private const val StoreMetaSp = 11         // 状态、分类提示

// 卡片用公共的 PhoneCard（SubScreens.kt）。这里原来有个私有的 StoreCard，
// 现在壳层有了正式的卡片原语就不留第二份——那正是"每个界面自己写一套"的开头。

/**
 * 英雄区：桌面的实况地图。
 *
 * 商店在这台手机上的实际用途是"决定桌面上摆什么"，不是买东西。
 * 所以顶上不放广告位/编辑推荐那种东西（那是照抄真商店的形，
 * 而这台手机上没有"推荐"这回事，编不出真内容）——放**你自己的桌面**：
 * 两页各占多少格、每格是哪个应用的颜色、下一个装的会落在哪一页。
 *
 * 最后一条是真的有用信息：installApp() 挑的是格子最少的那一页，
 * 以前这个规则藏在代码里，装完才发现应用跑到第二页去了。
 */
@Composable
private fun StoreShelfMap(state: PhoneState) {
    // 颜色查表：dock 也算进去，桌面上那三个常驻图标也是"占着位置"的。
    val colorById = remember {
        (AppCatalog.firstPage + AppCatalog.secondPage + AppCatalog.dock).associate { it.id to it.color }
    }
    val pages = state.homePageIds
    val installedCount = pages.sumOf { it.size }
    val total = remember { (AppCatalog.firstPage + AppCatalog.secondPage).size }
    // 下一个装的会落在这一页（和 installApp 里的规则一致：格子最少的那页）。
    val nextPage = pages.indices.minByOrNull { pages[it].size }

    PhoneCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "$installedCount",
                    color = PhoneText,
                    fontSize = StoreDisplaySp.sp,
                    fontWeight = FontWeight.Bold,
                    // 大数字收紧字距：默认字距在大字号下会显得散。
                    letterSpacing = (-0.5).sp,
                )
                Text(
                    " / $total 个在桌面上",
                    color = PhoneMuted,
                    fontSize = StoreBodySp.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            pages.forEachIndexed { index, ids ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "第 ${index + 1} 页",
                        color = PhoneMuted,
                        fontSize = StoreMetaSp.sp,
                        modifier = Modifier.width(44.dp),
                    )
                    // 每个应用一个小色块，颜色就是它图标的颜色——
                    // 于是这张图和真桌面是同一个东西，不是抽象示意。
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ids.take(18).forEach { id ->
                            Box(
                                Modifier.size(9.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(colorById[id] ?: PhoneMuted),
                            )
                        }
                        if (ids.size > 18) {
                            Text("+${ids.size - 18}", color = PhoneMuted, fontSize = StoreMetaSp.sp)
                        }
                    }
                    if (index == nextPage) {
                        Text(
                            "下一个装这里",
                            color = PhoneAccent,
                            fontSize = StoreMetaSp.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            val shelved = state.homeLibraryIds.size
            if (shelved > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "另有 $shelved 个收在资源库里，没占桌面位置",
                    color = PhoneMuted,
                    fontSize = StoreMetaSp.sp,
                )
            }
        }
    }
}

/** 筛选条。三个状态就够了，再多的分面在这个规模上是负担。 */
@Composable
private fun StoreFilterRow(selected: Int, counts: List<Int>, onSelect: (Int) -> Unit) {
    val labels = listOf("全部", "未安装", "已安装")
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        labels.forEachIndexed { i, label ->
            val on = selected == i
            val bg by animateColorAsState(
                if (on) PhoneAccent else PhoneSurfaceRaised, tween(180), label = "storeFilterBg",
            )
            val fg by animateColorAsState(
                if (on) Color.White else PhoneMuted, tween(180), label = "storeFilterFg",
            )
            PhonePressable(onClick = { onSelect(i) }, shape = PhoneChipShape, pressedScale = 0.96f) {
                Row(
                    Modifier.clip(PhoneChipShape).background(bg).padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(label, color = fg, fontSize = StoreMetaSp.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
                    Text(" ${counts[i]}", color = fg.copy(alpha = 0.75f), fontSize = StoreMetaSp.sp)
                }
            }
        }
    }
}

/**
 * 一个应用一张卡。
 *
 * 图标底衬用应用自己的颜色淡淡铺一层：商店里最有辨识度的东西就是这些
 * 图标的颜色，让它们成为版面的颜色来源，而不是另外发明一套装饰色。
 * 主色调（按钮、选中态）仍然走 Phone token，跟随用户挑的主题。
 */
@Composable
private fun StoreAppRow(
    app: PhoneAppItem,
    installed: Boolean,
    isSystem: Boolean,
    onToggle: () -> Unit,
) {
    val implemented = AppStoreCatalog.isImplemented(app.id)
    PhoneCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(46.dp).clip(StoreTileShape)
                    // 图标块用自己的颜色，压一层极淡的竖向渐变给它一点体积，
                    // 不用阴影——46dp 的小块上阴影只会脏。
                    .background(
                        Brush.verticalGradient(
                            listOf(app.color, app.color.copy(alpha = 0.82f)),
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                ImageGlyph(app.icon, Color.White, Modifier.size(46.dp).padding(10.dp))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        app.label,
                        color = PhoneText,
                        fontSize = StoreTitleSp.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // 没做界面的如实标出来。放在名字后面而不是塞进介绍里：
                    // 这是关于"能不能用"的判断，该和名字一起被看到。
                    if (!implemented) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "占位",
                            color = PhoneMuted,
                            fontSize = StoreMetaSp.sp,
                            modifier = Modifier.clip(PhoneChipShape)
                                .background(PhoneSurfaceRaised)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    AppStoreCatalog.blurbOf(app.id),
                    color = PhoneMuted,
                    fontSize = StoreBodySp.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            StoreActionButton(installed = installed, isSystem = isSystem, onClick = onToggle)
        }
    }
}

/**
 * 装 / 移除按钮。
 *
 * 不用 Material3 的 Button：这个项目里所有可按的东西都走 PhonePressable
 * （统一的按压回弹手感），只有商店这一处用 M3 Button 会手感不一样。
 * 已安装态用描边而不是实心——实心是"建议你做的事"，
 * 桌面上已经有了的时候，移除不该被强调成主操作。
 */
@Composable
private fun StoreActionButton(installed: Boolean, isSystem: Boolean, onClick: () -> Unit) {
    if (isSystem) {
        Text(
            "系统",
            color = PhoneMuted,
            fontSize = StoreMetaSp.sp,
            modifier = Modifier.clip(PhoneChipShape)
                .background(PhoneSurfaceRaised)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )
        return
    }
    val label = if (installed) "移除" else "安装"
    PhonePressable(onClick = onClick, shape = PhoneChipShape, pressedScale = 0.94f) {
        if (installed) {
            Text(
                label,
                color = PhoneMuted,
                fontSize = StoreMetaSp.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clip(PhoneChipShape)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PhoneChipShape)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        } else {
            Text(
                label,
                color = Color.White,
                fontSize = StoreMetaSp.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(PhoneChipShape)
                    .background(PhoneAccent)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }
    }
}

/** 分区标题。分类是真的分类（按"做什么"分），不是为了排版加的装饰行。 */
@Composable
private fun StoreSectionHeader(category: AppStoreCategory, count: Int) {
    Column(Modifier.padding(top = 6.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(category.label, color = PhoneText, fontSize = StoreTitleSp.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(7.dp))
            Text("$count", color = PhoneMuted, fontSize = StoreMetaSp.sp)
        }
        Text(category.hint, color = PhoneMuted, fontSize = StoreMetaSp.sp, lineHeight = 15.sp)
    }
}

@Composable
fun AppStoreScreen(state: PhoneState) {
    var filter by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()

    val all = state.storeApps()
    val installedIds = all.filter { state.isAppInstalled(it.id) }.map { it.id }.toSet()
    val counts = listOf(all.size, all.count { it.id !in installedIds }, installedIds.size)

    val shown = when (filter) {
        1 -> all.filter { it.id !in installedIds }
        2 -> all.filter { it.id in installedIds }
        else -> all
    }
    // 按分类分组。组内保持 storeApps() 的顺序，不再排序——
    // 那个顺序把 App Store 自己排在最前面，是有意的。
    val grouped = shown.groupBy { AppStoreCatalog.categoryOf(it.id) }
    val orderedCategories = AppStoreCategory.entries.filter { grouped[it]?.isNotEmpty() == true }

    ScreenFrame {
        ScreenHeader("App Store", state)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            item(key = "shelf") { StoreShelfMap(state) }
            item(key = "filter") {
                Box(Modifier.padding(top = 4.dp)) {
                    StoreFilterRow(filter, counts) { filter = it }
                }
            }
            if (shown.isEmpty()) {
                item(key = "empty") {
                    // 空态给方向，不只说"没有"。
                    Box(Modifier.fillMaxWidth().padding(top = 28.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (filter == 1) "桌面上已经装齐了，没有漏下的" else "桌面上还没有应用",
                            color = PhoneMuted,
                            fontSize = StoreBodySp.sp,
                        )
                    }
                }
            }
            orderedCategories.forEach { category ->
                val apps = grouped[category].orEmpty()
                item(key = "sec-${category.name}") { StoreSectionHeader(category, apps.size) }
                items(apps.size, key = { "app-${apps[it].id}" }) { i ->
                    val app = apps[i]
                    val installed = app.id in installedIds
                    val isSystem = app.id == "appstore"
                    StoreAppRow(
                        app = app,
                        installed = installed,
                        isSystem = isSystem,
                        onToggle = {
                            if (installed) state.uninstallApp(app.id) else state.installApp(app.id)
                        },
                    )
                }
            }
        }
    }
}
