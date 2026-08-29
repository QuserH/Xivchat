package com.quserh.eorzeaphone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectTapGestures
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDressDicts
import com.quserh.eorzeaphone.data.wiki.WikiDb
import com.quserh.eorzeaphone.data.wiki.WikiDicts
import com.quserh.eorzeaphone.data.wiki.WikiFilter
import com.quserh.eorzeaphone.data.wiki.WikiItem

/*
 * 幻化装备选择器。
 *
 * 为什么必须有它：石之家发幻化时服务端要求**至少一件有效装备**
 * （真机实测回 `10003 至少需要上传一件有效装备`）。原来推断
 * `equipment_id: -1` 全空也能发，是错的。所以这不是加分项，
 * 没有选择器发幻化这个功能就发不出去。
 *
 * 数据映射全部查自 `assets/wiki/items.db`，依据写在
 * `开发/WIKI/GLAMOUR_SLOTS.md`，别照猜的改。要点：
 *
 *  - `items.id` 就是标准 FF14 物品 id，直接当 `equipment_id`
 *  - 装备和染剂都带 `unobtainable = 0`（染剂那一条我第一版写反了，
 *    见文档里的「更正」一节：旧染剂站点自己的描述写着停产）
 *  - `items.dye` 是**染色孔数** 0/1/2，不是能不能染的布尔
 *  - 主手不按 category 手工映射，按 `jobId` 筛（`jobNarrow = false`）
 */

/** 一个幻化槽的静态信息。[categoryId] 为 0 表示这个槽按职业筛（只有主手）。 */
private data class GlamourSlotMeta(
    /** 服务端的槽位标识，必须和 `ShizhijiaApi.SZJ_GLAMOUR_SLOTS` 里的字面量一致。 */
    val slot: String,
    val label: String,
    val categoryId: Int,
)

/**
 * 12 个槽，顺序照 `ShizhijiaApi.SZJ_GLAMOUR_SLOTS`（也就是官网的顺序）。
 *
 * `WAIST` / `SOUL_CRYSTAL` 不在这里 —— 6.0 移除了腰带槽，
 * 官网对这两个槽固定补空，`publishGlamour` 自己追加。
 *
 * `GLOVES` 是 37、`LEGS` 是 36，**不按 34→38 顺号排**。照名字对，别照序号猜。
 */
private val GLAMOUR_SLOTS = listOf(
    GlamourSlotMeta("MAIN_HAND", "主手", 0),
    GlamourSlotMeta("OFF_HAND", "副手", 11),
    GlamourSlotMeta("HEAD", "头部", 34),
    GlamourSlotMeta("BODY", "身体", 35),
    GlamourSlotMeta("GLOVES", "手部", 37),
    GlamourSlotMeta("LEGS", "腿部", 36),
    GlamourSlotMeta("FEET", "足部", 38),
    GlamourSlotMeta("EARS", "耳饰", 41),
    GlamourSlotMeta("NECK", "项饰", 40),
    GlamourSlotMeta("WRISTS", "手镯", 42),
    GlamourSlotMeta("FINGER_LEFT", "戒指(左)", 43),
    GlamourSlotMeta("FINGER_RIGHT", "戒指(右)", 43),
)

/** 染剂的 category，162 行。**不按 `unobtainable` 过滤**，理由见 [dyePalette]。 */
private const val DYE_CATEGORY = 55

/** 松节油：cat 55 里唯一的非颜色项，是卸妆水（去染色）。不混进色块列表。 */
private const val DYE_REMOVER_ID = 5728

/**
 * 调色板里的一格：一种颜色 + 拿哪件染剂代表它。
 *
 * [stainId] 只用来排序 —— `STAINS` 的 id 顺序就是色系顺序，见 [dyePalette]。
 */
private data class DyeSwatch(
    val item: WikiItem,
    val stainId: Int,
    val colorName: String,
    val rgb: Int,
    val metallic: Boolean,
)

/**
 * 染剂物品名 → 颜色名。去掉「染剂」后缀和「特制」前缀，
 * 剩下的正是 `ShizhijiaDressDicts.STAINS` 里的 `label`。
 *
 * 站点自己也做同一件事（`getDyesList` 的结果拿去 `name.replace("染剂","")` 再显示）。
 */
private fun dyeColorKey(nameCn: String): String =
    nameCn.replace("染剂", "").replace("特制", "")

/**
 * 把 162 行染剂物品折成一张按**颜色**去重的调色板。
 *
 * **为什么不按 `unobtainable = 0` 过滤**（我前后判过两次，这是最终的）：
 * 那 116 行停产染剂**停的是物品，颜色还在** —— 新染剂能自由选色，
 * 所以对一个「选颜色」的选择器，物品能不能买到无关。
 * 实测过滤会把 **125 种颜色砍到 20 种，丢掉 84%**，
 * 素雪白 `#E4DFD0`、煤烟黑 `#2B2923`、玫瑰粉 `#E69F96`
 * 这些最常用的全在被砍的那批里。
 * 探针 `开发/WIKI/wiki-feature/_probe_dyecolor4.py`。
 *
 * 同一种颜色常有两件物品（`特制无瑕白染剂` 和 `无瑕白染剂` 是同一个色），
 * 只画色块的话两格长得一模一样。所以**按颜色去重**，每种颜色留一件代表：
 * 先挑还能买到的，都能买就挑名字短的那个（不带「特制」——
 * 「特制」是市场板货，普通版是玩家平时说的那个名字）。
 *
 * `STAINS` 里对不上的一律**丢掉，不显示**。那 16 行虽然也在 cat 55，
 * 但没有一个是能涂的颜色：`蓝色色素`/`红色色素` 等是 1.0 时代的**制作材料**
 * （`version 1.23`、il 28），`生漆`/`黑漆` 是漆，`第二期重建用的上级色素`
 * 是重建活动材料，`通用染剂`/`追加染剂1`/`追加染剂2`/`色素` 是 7.5 那套
 * 新染色体系的元物品。**放进调色板只会让人以为「生漆」是一个颜色。**
 *
 * **按 `stain.id` 排序，不按物品的品级/版本。** `STAINS` 的 id 顺序就是色系
 * 顺序（1-6 白灰黑、7-17 粉红橙、18-30 棕、31-41 黄、42-56 绿、57-76 蓝、
 * 77-85 紫、86-100 活动色、101-125 新系列），和游戏自己的染色界面一致。
 * 走 `WikiRepository.ORDER`（`item_level DESC, version DESC`）的话，
 * 开头会是玉米黄→深林绿→天上蓝→靛青蓝→虚空蓝→蜂鸟粉这种彩虹跳色，
 * 而最常用的 1.23 基础色因为 `version` 最小被排到最底下。
 * 按色系排还顺带解决了深色难辨：五个灰挨在一起，相邻对比就看出来了。
 */
private fun dyePalette(rows: List<WikiItem>): List<DyeSwatch> {
    val stainByName = ShizhijiaDressDicts.STAINS
        .filter { it.id != 0 }   // 0 是「无染色」，那是清除染色，不进调色板
        .associateBy { it.label }
    val byColor = LinkedHashMap<String, DyeSwatch>()
    for (it in rows) {
        if (it.id == DYE_REMOVER_ID) continue
        val stain = stainByName[dyeColorKey(it.nameCn)] ?: continue
        val old = byColor[stain.label]
        val better = when {
            old == null -> true
            // 能买到的优先。
            old.item.unobtainable != it.unobtainable -> old.item.unobtainable
            // 同为能买/同为停产时，名字短的优先（普通版 > 特制版）。
            else -> it.nameCn.length < old.item.nameCn.length
        }
        if (better) {
            byColor[stain.label] =
                DyeSwatch(it, stain.id, stain.label, stain.rgb, stain.metallic)
        }
    }
    return byColor.values.sortedBy { it.stainId }
}

/**
 * 一个槽当前选了什么。[item] 留着是为了显示图标和名字，
 * 提交时只用 `ShizhijiaApi.GlamourSlotPick`。
 *
 * [dyes] **是按孔位定长 2 的数组**，不是「选了哪几个」的列表：
 * `dyes[0]` 是染色 1、`dyes[1]` 是染色 2，没选的那孔是 null。
 *
 * 定长是照站点的写法（`PublishGlamour` 里 `part_info.dye` 就是 `[null, null]`）：
 * ```js
 * let n = l.part_info.dye.map(e => e != null ? e.id : -1)
 * if (0 === dye_count) n = []
 * else if (1 === dye_count) n = n.slice(0, 1)
 * ```
 * **孔位是有意义的** —— 两个染色孔的先后顺序影响最终效果
 * （`ShizhijiaGlamourDye` 那边的注释也写了这一条）。
 * 用紧凑列表的话「只染第 2 孔」会被提交成「染第 1 孔」，染错位置。
 */
internal data class GlamourPick(
    val item: WikiItem,
    val dyes: List<WikiItem?> = listOf(null, null),
) {
    /** 有没有选过染色。 */
    val hasDye: Boolean get() = dyes.any { it != null }

    /** 按孔位显示用的名字，跳过空孔。 */
    val dyeLabel: String get() = dyes.filterNotNull().joinToString("·") { it.nameCn }

    /**
     * 提交用的 `dye_ids`：按孔位，空孔是 -1，再截到这件装备的孔数。
     * 0 孔的装备提交空数组（站点也是这么做的）。
     */
    fun dyeIdsForSubmit(): List<Long> =
        if (item.dye <= 0) emptyList()
        else dyes.take(item.dye).map { it?.id?.toLong() ?: -1L }
}

/**
 * 发幻化屏里的「装备」那一节。12 个槽，点一个开选择器。
 *
 * [picks] 是 slot → 选择，调用方持有（`mutableStateMapOf`），
 * 因为发布时要按 `ShizhijiaApi.SZJ_GLAMOUR_SLOTS` 的顺序拼。
 *
 * **这里不能用 LazyVerticalGrid** —— 这一节是发幻化屏那个 `LazyColumn`
 * 的一个 item，同方向嵌套会崩。12 个格子是定数，手动分行就够。
 */
@Composable
internal fun SzjGlamourSlotSection(
    picks: Map<String, GlamourPick>,
    onPick: (String, GlamourPick?) -> Unit,
) {
    var editing by remember { mutableStateOf<GlamourSlotMeta?>(null) }
    var dyeing by remember { mutableStateOf<GlamourSlotMeta?>(null) }

    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("装备", color = SzjText, style = SzjLabelStyle)
            // 服务端要求至少一件（实测 10003）。星号和种族/性别那两节一致。
            Text(" *", color = SzjAccent, style = SzjLabelStyle)
            Spacer(Modifier.width(6.dp))
            Text(
                if (picks.isEmpty()) "至少选一件" else "已选 ${picks.size} 件",
                color = SzjMuted, style = SzjMetaStyle,
            )
        }
        Spacer(Modifier.height(8.dp))
        // 3 列 × 4 行。
        GLAMOUR_SLOTS.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                row.forEach { meta ->
                    Box(Modifier.weight(1f)) {
                        SzjGlamourSlotCell(
                            meta = meta,
                            pick = picks[meta.slot],
                            onClick = { editing = meta },
                            onDye = { dyeing = meta },
                            onClear = { onPick(meta.slot, null) },
                        )
                    }
                }
                // 最后一行不满 3 个时补空位，否则剩下的格子会被拉宽。
                repeat(3 - row.size) { Box(Modifier.weight(1f)) {} }
            }
        }
    }

    editing?.let { meta ->
        SzjEquipPickerSheet(
            meta = meta,
            onClose = { editing = null },
            onPicked = { item ->
                // 换装备就把染色清掉：新装备的孔数可能不一样，
                // 留着旧染色会出现「2 个染色配 0 孔装备」这种发不出去的组合。
                onPick(meta.slot, GlamourPick(item))
                editing = null
            },
        )
    }
    dyeing?.let { meta ->
        val pick = picks[meta.slot]
        if (pick == null) {
            dyeing = null
        } else {
            SzjDyePickerSheet(
                slotLabel = meta.label,
                holes = pick.item.dye,
                dyes = pick.dyes,
                onClose = { dyeing = null },
                onChange = { onPick(meta.slot, pick.copy(dyes = it)) },
            )
        }
    }
}

/** 一个槽的格子。空着是虚线框 + 槽名；选了就是图标 + 名字 + 棱条标记。 */
@Composable
private fun SzjGlamourSlotCell(
    meta: GlamourSlotMeta,
    pick: GlamourPick?,
    onClick: () -> Unit,
    onDye: () -> Unit,
    onClear: () -> Unit,
) {
    SzjCardSurface(onClick = onClick, shape = SzjInnerShape) {
        Column(
            Modifier.fillMaxWidth().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // 选中标记用签名棱条，只做标记不当图标（选中才画，空槽不画）。
                if (pick != null) SzjShard(widthDp = 3, heightDp = 12)
                Spacer(Modifier.width(if (pick != null) 4.dp else 0.dp))
                Text(
                    meta.label, color = if (pick != null) SzjText else SzjMuted,
                    style = SzjMetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (pick != null) {
                    SzjPressable(onClick = onClear, shape = CircleShape) {
                        ImageGlyph(R.drawable.ic_close, SzjMuted, Modifier.padding(3.dp).size(11.dp))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            if (pick == null) {
                Box(
                    Modifier.size(48.dp).clip(SzjChipShape)
                        .border(1.dp, SzjHairline, SzjChipShape),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageGlyph(R.drawable.ic_add, SzjMuted, Modifier.size(16.dp))
                }
            } else {
                SzjItemIcon(
                    pick.item.iconId, pick.item.iconHash, pick.item.nameCn,
                    Modifier.size(48.dp).clip(SzjChipShape),
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                pick?.item?.nameCn ?: "点一下挑一件",
                color = if (pick != null) SzjText else SzjMuted,
                fontSize = 11.sp, lineHeight = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            // 能染色才给染色入口，孔数是 items.dye（0/1/2）。
            if (pick != null && pick.item.dye > 0) {
                Spacer(Modifier.height(5.dp))
                SzjPressable(onClick = onDye, shape = SzjChipShape) {
                    Text(
                        if (!pick.hasDye) "染色" else pick.dyeLabel,
                        color = SzjAccent, fontSize = 10.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clip(SzjChipShape).background(SzjAccentSoft)
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

/**
 * 装备选择器。3 列网格、图标 48dp、名字两行截断。
 *
 * 主手那一槽额外给职业筛：`kind_id = 1` 一把抓是 6516 行，
 * 不给筛就只剩搜索框一个入口，等于要玩家先知道武器叫什么。
 */
@Composable
private fun SzjEquipPickerSheet(
    meta: GlamourSlotMeta,
    onClose: () -> Unit,
    onPicked: (WikiItem) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    // 主手默认落在坦克第一个职业上，比一进来就是 6516 行强。
    var jobId by remember { mutableStateOf(if (meta.categoryId == 0) 19 else 0) }
    var role by remember { mutableStateOf(if (meta.categoryId == 0) "坦克" else "") }
    var items by remember { mutableStateOf<List<WikiItem>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var appending by remember { mutableStateOf(false) }

    val filter = remember(query, jobId, meta.categoryId) {
        WikiFilter(
            query = query.trim(),
            // 主手：kind_id = 1 + 职业。其余槽：category 直筛。
            kindId = if (meta.categoryId == 0) 1 else 0,
            categoryId = meta.categoryId,
            jobId = if (meta.categoryId == 0) jobId else 0,
            // **必须 false。** 窄筛叠的是属性类型条件（复刻站点「这个职业该穿的」），
            // 找幻化时会把跨职业能穿的外观筛掉。WikiFilter 的注释里写了这一条。
            jobNarrow = false,
            // 绝版和 6.0 移除的腰带都靠这个挡掉。
            obtainable = 1,
        )
    }

    // 筛选条件一变就回到第 0 页重新查。
    LaunchedEffect(filter) {
        loading = true
        page = 0
        total = WikiDb.count(context, filter)
        items = WikiDb.search(context, filter, page = 0)
        loading = false
    }

    SzjPickerSheet(
        title = "选择${meta.label}",
        onClose = onClose,
        header = {
            SzjPickerSearchBox(query, "搜名字") { query = it }
            if (meta.categoryId == 0) {
                Spacer(Modifier.height(8.dp))
                // 两级：先定位再职业。33 个职业平铺成 chip 要铺 9 行，
                // 会把网格整个挤出屏幕（wiki 检索面板那边验过同一件事）。
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WikiDicts.jobRoles.filter { it.first !in setOf("生产", "采集") }
                        .forEach { (r, jobs) ->
                            SzjPickerChip(r, role == r) {
                                role = r
                                jobId = jobs.first()
                            }
                        }
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WikiDicts.jobsOfRole(role).forEach { (id, name) ->
                        SzjPickerChip(name, jobId == id) { jobId = id }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (loading) "正在查" else "$total 件",
                color = SzjMuted, style = SzjMetaStyle,
            )
        },
    ) { modifier ->
        when {
            loading -> Box(modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
            items.isEmpty() -> Box(modifier, contentAlignment = Alignment.Center) {
                Text(
                    if (query.isBlank()) "这个位置没有可选的装备" else "没搜到「${query.trim()}」",
                    color = SzjMuted, style = SzjMetaStyle,
                )
            }
            else -> {
                val grid = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
                // 滚到末尾前两行就续下一页。3407 行的身体槽不可能一次全查。
                LaunchedEffect(grid, items.size, total) {
                    snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
                        .collect { last ->
                            if (!appending && items.size < total && last >= items.size - 7) {
                                appending = true
                                val next = page + 1
                                val more = WikiDb.search(context, filter, page = next)
                                if (more.isNotEmpty()) {
                                    items = items + more
                                    page = next
                                }
                                appending = false
                            }
                        }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = grid,
                    modifier = modifier,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    items(items, key = { it.id }) { item ->
                        SzjEquipCell(item) { onPicked(item) }
                    }
                }
            }
        }
    }
}

/** 网格里的一件装备：图标 48dp + 名字两行 + 染色孔标记。 */
@Composable
private fun SzjEquipCell(item: WikiItem, onClick: () -> Unit) {
    SzjCardSurface(onClick = onClick, shape = SzjInnerShape) {
        Column(
            Modifier.fillMaxWidth().padding(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SzjItemIcon(
                item.iconId, item.iconHash, item.nameCn,
                Modifier.size(48.dp).clip(SzjChipShape),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                item.nameCn, color = SzjText, fontSize = 11.sp, lineHeight = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("il${item.itemLevel}", color = SzjMuted, fontSize = 9.sp, modifier = Modifier.weight(1f))
                // 孔数直接标出来，省得选完才发现不能染。
                if (item.dye > 0) {
                    Text("染${item.dye}", color = SzjAccent, fontSize = 9.sp)
                }
            }
        }
    }
}

/**
 * 染剂选择器。46 个（`category_id = 55` 且 `unobtainable = 0`），一次取完。
 *
 * [slots] 是这件装备的染色孔数（`items.dye`，1 或 2）。选满了再点别的会顶掉最早那个，
 * 不弹「已达上限」——直接换比先让人取消再选少一步。
 *
 * **这里本来要做 6 列纯色块，但色值拿不到。** items.db 没有颜色列，
 * 站点权威数据 `Data:Item/<id>.json` 也没有，而图标是**按色系共用的**：
 * 46 个染剂只有 15 个不同 `icon_id`，柔彩蓝/黑暗蓝/金属蓝/金属靛全是 22813。
 * 所以只能图标 + 名字，靠名字区分。硬编一份猜的色值更坏 ——
 * 玩家按色块选，染上去发现不是那个颜色。详见 GLAMOUR_SLOTS.md。
 */
@Composable
private fun SzjDyePickerSheet(
    slotLabel: String,
    holes: Int,
    dyes: List<WikiItem?>,
    onClose: () -> Unit,
    onChange: (List<WikiItem?>) -> Unit,
) {
    val context = LocalContext.current
    var palette by remember { mutableStateOf<List<DyeSwatch>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    // 当前在给哪个孔选色。站点也是这个形态（「染色 1」「染色 2」两个页签）。
    var hole by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // 162 行，一页就够（PAGE_SIZE 远大于这个数）。**不带 obtainable 过滤**，
        // 理由见 dyePalette 的注释：停产的是物品，颜色还在。
        val rows = WikiDb.search(context, WikiFilter(categoryId = DYE_CATEGORY))
        palette = dyePalette(rows)
        loading = false
    }

    val shown = remember(palette, query) {
        val q = query.trim()
        if (q.isEmpty()) palette
        else palette.filter { it.colorName.contains(q) || it.item.nameCn.contains(q) }
    }

    SzjPickerSheet(
        title = "$slotLabel 的染色",
        onClose = onClose,
        header = {
            // 孔位页签。两个孔的先后影响效果，所以必须让人指定给哪个孔。
            if (holes > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(holes) { i ->
                        val label = dyes.getOrNull(i)?.nameCn ?: "染色 ${i + 1}"
                        SzjPickerChip(label, hole == i) { hole = i }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            SzjPickerSearchBox(query, "搜颜色名") { query = it }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (holes > 1) "正在选染色 ${hole + 1}（共 $holes 孔）" else "$holes 个染色孔",
                    color = SzjMuted, style = SzjMetaStyle, modifier = Modifier.weight(1f),
                )
                if (dyes.any { it != null }) {
                    SzjPressable(
                        onClick = { onChange(listOf(null, null)) },
                        shape = SzjChipShape,
                    ) {
                        Text(
                            // 松节油（id 5728）就是游戏里的卸妆水，语义一致。
                            "清除染色", color = SzjAccent, style = SzjMetaStyle,
                            modifier = Modifier.clip(SzjChipShape).background(SzjAccentSoft)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        },
    ) { modifier ->
        when {
            loading -> Box(modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
            shown.isEmpty() -> Box(modifier, contentAlignment = Alignment.Center) {
                Text("没搜到「${query.trim()}」", color = SzjMuted, style = SzjMetaStyle)
            }
            // 6 列：色块面积是 4 列套卡片那版的 2.4 倍，行数还少三分之一
            // （125 色 → 21 行 vs 32 行）。间距收到 5dp，让同色系挨得更近好比色。
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(shown, key = { it.item.id }) { sw ->
                    val on = dyes.getOrNull(hole)?.id == sw.item.id
                    SzjDyeCell(sw, on) {
                        // 写进当前孔位；再点一次同一个 = 清掉这一孔。
                        val next = dyes.toMutableList()
                        while (next.size < 2) next.add(null)
                        next[hole] = if (on) null else sw.item
                        onChange(next)
                        // 选完第 1 孔自动跳到第 2 孔，省一次点击。
                        if (!on && holes > 1 && hole == 0 && next[1] == null) hole = 1
                    }
                }
            }
        }
    }
}

/**
 * 调色板里的一格：色块 + 颜色名，选中加高光边和棱条。
 *
 * 色值来自 `ShizhijiaDressDicts.STAINS`（126 条带 RGB，站点自己那张表）。
 *
 * **故意不用 `SzjCardSurface`。** 这一格的主体就是颜色本身，套卡片的话
 * 40dp 色块落在 ~136dp 的格子里，颜色只占 9%，其余全是阴影和留白；
 * 而且卡片间距 + 阴影会把相邻两个色隔开，**比色必须相邻无间隔**。
 * 所以色块自己铺满整格宽、固定高 44dp。
 *
 * 名字常驻，不做长按气泡 —— 深色系里好几个色只差一档，
 * 光看色块分不出「木炭灰」`#484742` 和「煤烟黑」`#2B2923`。
 * 125 个名字里 122 个是 3 个字（最长 5 个字：金属宝石红/金属钴铁绿/金属黑暗蓝），
 * 6 列时每格约 88dp，10sp 的 3 字名约 30dp，放得下。
 */
@Composable
private fun SzjDyeCell(sw: DyeSwatch, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(
        if (selected) SzjAccent else SzjHairline, tween(180), label = "szjDyeBorder",
    )
    SzjPressable(onClick = onClick, shape = SzjChipShape) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    Modifier.fillMaxWidth().height(44.dp).clip(SzjChipShape)
                        .background(Color(0xFF000000.toInt() or sw.rgb))
                        // 极浅和极深的色块在同色底上会看不见边界；选中时这道边
                        // 变成强调色，兼作选中标记的一半。
                        .border(if (selected) 2.dp else 1.dp, border, SzjChipShape),
                )
                if (selected) SzjShard(widthDp = 3, heightDp = 12)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                sw.colorName,
                color = if (selected) SzjAccent else SzjText,
                fontSize = 10.sp, lineHeight = 12.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            // 金属色在色块上看不出来（就是一个普通色），标一下。
            if (sw.metallic) {
                Text("金属", color = SzjMuted, fontSize = 8.sp, lineHeight = 10.sp)
            }
        }
    }
}

/**
 * 选择器用的底部弹层。
 *
 * 和 `SzjSheet` 的区别：那个把 content 塞进 `verticalScroll`，
 * 装不了几千条的懒加载网格（会一次性组合全部子项）。
 * 这里把**剩余空间**交给调用方，让它自己放 `LazyVerticalGrid`。
 *
 * [content] 收到的 `Modifier` 已经带了 `weight(1f)` ——
 * 在 `Column` 里必须用 weight 而不是 `fillMaxSize()`，
 * 否则会吃光高度、把上面的搜索框和标题压成 0 高。
 */
@Composable
private fun SzjPickerSheet(
    title: String,
    onClose: () -> Unit,
    header: @Composable ColumnScope.() -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    BackHandler { onClose() }
    val noRipple = remember { MutableInteractionSource() }
    Box(
        // 和 SzjSheet 同一个遮罩值。**不抽成共享 token** ——
        // 那边注释里写清了理由：模态遮罩和地图文字底衬同值不同义。
        Modifier.fillMaxSize().background(Color(0x8C000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.82f)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(SzjBg)
                .clickable(interactionSource = noRipple, indication = null) { }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                Text(
                    title, color = SzjText, fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                )
                SzjPressable(onClick = onClose, shape = CircleShape) {
                    ImageGlyph(R.drawable.ic_close, SzjMuted, Modifier.padding(6.dp).size(16.dp))
                }
            }
            header()
            content(Modifier.fillMaxWidth().weight(1f))
        }
    }
}

/** 选择器里的搜索框。即时筛，没有确认按钮。 */
@Composable
private fun SzjPickerSearchBox(value: String, placeholder: String, onChange: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(SzjInnerShape).background(SzjCardRaised)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageGlyph(R.drawable.ic_search, SzjMuted, Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(placeholder, color = SzjMuted, fontSize = 13.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = SzjText, fontSize = 13.sp),
                cursorBrush = SolidColor(SzjAccent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (value.isNotEmpty()) {
            SzjPressable(onClick = { onChange("") }, shape = CircleShape) {
                ImageGlyph(R.drawable.ic_close, SzjMuted, Modifier.padding(3.dp).size(12.dp))
            }
        }
    }
}

/**
 * 物品图标：先走 xivapi（复用 App 的 [ItemIconLoader] 缓存链路），
 * 取不到再退到灰机图床。
 *
 * 兜底是必要的：xivapi 缺一部分图标（实测随机 120 个里 2 个稳定 404，
 * 全落在 7.x），灰机图床都有。有限重试是因为一屏几十个格子同时拉图时
 * 并发下会有零星失败 —— 真机上撞到过，首次进入几格显示文字兜底。
 *
 * **和 `WikiScreens.kt` 的 `WikiIcon` 是同一件事。** 没直接复用是因为那个是
 * `private`，而那个文件正被另一个会话改观感，我不去动它。
 * 等那批落地之后应该把两个合成一个 `internal`，删掉这一份。
 */
@Composable
private fun SzjItemIcon(
    iconId: Int,
    iconHash: String,
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    var bitmap by remember(iconId, iconHash) {
        mutableStateOf(
            if (iconId > 0) {
                com.quserh.eorzeaphone.data.ItemIconLoader.peek(iconId)
            } else {
                com.quserh.eorzeaphone.data.wiki.WikiIconCache.peek(iconHash)
            },
        )
    }

    LaunchedEffect(iconId, iconHash) {
        if (bitmap != null) return@LaunchedEffect
        repeat(3) { attempt ->
            if (attempt > 0) kotlinx.coroutines.delay(400L * attempt)
            if (iconId > 0) {
                bitmap = com.quserh.eorzeaphone.data.ItemIconLoader.load(context, iconId)
            }
            if (bitmap == null && iconHash.isNotBlank()) {
                bitmap = com.quserh.eorzeaphone.data.wiki.WikiIconCache.load(context, iconHash)
            }
            if (bitmap != null) return@LaunchedEffect
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        androidx.compose.foundation.Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        // 图没来之前给首字，比空白框更能认出是哪件。
        Box(modifier.background(SzjCardRaised), contentAlignment = Alignment.Center) {
            Text(
                fallbackText.take(1), color = SzjMuted,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** 选择器里的筛选 chip。比 `SzjPartChip` 小一号，因为一行要放 5 个定位。 */
@Composable
private fun SzjPickerChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) SzjAccentSoft else Color.Transparent, tween(200), label = "szjPickChipBg",
    )
    val fg by animateColorAsState(
        if (selected) SzjOnAccentSoft else SzjMuted, tween(200), label = "szjPickChipFg",
    )
    val stroke by animateColorAsState(
        if (selected) Color.Transparent else SzjHairline, tween(200), label = "szjPickChipStroke",
    )
    SzjPressable(onClick = onClick, shape = SzjChipShape) {
        Text(
            label, color = fg, fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.clip(SzjChipShape).background(bg)
                .border(1.dp, stroke, SzjChipShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
