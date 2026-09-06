package com.quserh.eorzeaphone.craft.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.Image
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quserh.eorzeaphone.craft.CraftAppState
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.ui.ImageGlyph
import com.quserh.eorzeaphone.craft.Page
import com.quserh.eorzeaphone.craft.Tab
import com.quserh.eorzeaphone.craft.data.AggregateRow
import com.quserh.eorzeaphone.craft.data.BomNode
import com.quserh.eorzeaphone.craft.data.CraftItem
import com.quserh.eorzeaphone.craft.data.CraftJobs
import com.quserh.eorzeaphone.craft.data.CraftQuality
import com.quserh.eorzeaphone.craft.data.CraftList
import com.quserh.eorzeaphone.craft.data.CraftSession
import com.quserh.eorzeaphone.craft.data.CraftSkills
import com.quserh.eorzeaphone.craft.data.CraftSimulation
import com.quserh.eorzeaphone.craft.data.CraftState
import com.quserh.eorzeaphone.craft.data.InventoryGroups
import com.quserh.eorzeaphone.craft.data.InventorySnapshot
import com.quserh.eorzeaphone.craft.data.InventoryItem
import com.quserh.eorzeaphone.craft.data.SkillDef
import com.quserh.eorzeaphone.craft.data.craftConsumablesInBag
import com.quserh.eorzeaphone.craft.data.displayName
import com.quserh.eorzeaphone.data.ItemIconLoader
import com.quserh.eorzeaphone.data.GameCraftConsumable
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.quserh.eorzeaphone.craft.ui.CraftAccent
import com.quserh.eorzeaphone.craft.ui.CraftBackground
import com.quserh.eorzeaphone.craft.ui.CraftDanger
import com.quserh.eorzeaphone.craft.ui.CraftFill
import com.quserh.eorzeaphone.craft.ui.CraftHq
import com.quserh.eorzeaphone.craft.ui.CraftLine
import com.quserh.eorzeaphone.craft.ui.CraftMuted
import com.quserh.eorzeaphone.craft.ui.CraftOk
import com.quserh.eorzeaphone.craft.ui.CraftSurface
import com.quserh.eorzeaphone.craft.ui.CraftText
import com.quserh.eorzeaphone.craft.ui.CraftType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// ---------------------------------------------------------------- 配方 Tab

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RecipesTab(state: CraftAppState) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<CraftItem>()) }
    var completedQuery by remember { mutableStateOf<String?>(null) }
    var repairing by remember { mutableStateOf(false) }
    var pickerFor by remember { mutableStateOf<Pair<CraftItem, Int>?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(query, state.dbReady) {
        results = emptyList()
        completedQuery = null
        if (state.dbReady && query.isNotBlank()) {
            val pendingQuery = query
            delay(180)
            results = withContext(Dispatchers.IO) { state.db.search(pendingQuery) }
            completedQuery = pendingQuery
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = "配方")
            InlineField(
                query,
                { query = it },
                "搜索道具",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (query.isBlank()) {
                if (state.cart.history.isNotEmpty()) {
                    SectionLabel("搜索历史")
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(state.cart.history, key = { it }, contentType = { "history" }) { h ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            query = h
                                        },
                                        onLongClick = { state.cart.removeHistory(h) },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(h, style = CraftType.Row, color = CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Box(
                                    Modifier.size(36.dp)
                                        .semantics { contentDescription = "删除搜索历史" }
                                        .clickable { state.cart.removeHistory(h) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ImageGlyph(R.drawable.ic2_close, CraftMuted, Modifier.size(18.dp))
                                }
                            }
                            Hairline()
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            } else if (results.isEmpty() && !state.dbReady) {
                Column(Modifier.padding(20.dp)) {
                    if (state.dbError != null) {
                        Text(
                            "配方库加载失败：${state.dbError}",
                            style = CraftType.Callout, color = CraftDanger,
                        )
                        if (state.dbProgress.isNotBlank()) {
                            Text(state.dbProgress, style = CraftType.Caption, color = CraftMuted, modifier = Modifier.padding(top = 4.dp))
                        }
                        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "从网络重建数据库",
                                style = CraftType.Callout, color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CraftFill)
                                    .clickable {
                                        if (!repairing) {
                                            repairing = true
                                            scope.launch { state.repairDbFromNetwork(); repairing = false }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                            Text(
                                "重试本地",
                                style = CraftType.Callout, color = CraftText,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CraftSurface)
                                    .clickable {
                                        if (!repairing) {
                                            repairing = true
                                            scope.launch { state.prepareDb(); repairing = false }
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        Text(
                            if (state.dbProgress.isNotBlank()) state.dbProgress else "配方库载入中…",
                            style = CraftType.Callout, color = CraftMuted,
                        )
                    }
                }
            } else if (completedQuery != query) {
                Text("搜索中…", style = CraftType.Callout, color = CraftMuted, modifier = Modifier.padding(20.dp))
            } else if (results.isEmpty()) {
                Text("没有匹配的道具", style = CraftType.Callout, color = CraftMuted, modifier = Modifier.padding(20.dp))
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(results, key = { _, it -> it.id }) { _, item ->
                        ResultRow(
                            state, item,
                            onOpen = {
                                state.cart.addHistory(query.trim())
                                if (state.db.canCraft(item.id)) state.push(Page.RecipeDetail(item.id))
                            },
                            onPickList = { qty -> pickerFor = item to qty },
                            onCart = { qty -> state.cart.add(item.id, qty) },
                        )
                        Hairline()
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
        CartFab(
            count = state.cart.items.size,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { state.push(Page.Cart) }
    }
    pickerFor?.let { (target, qty) ->
        ListPickerDialog(
            state = state,
            title = "加入清单 · ${target.nameCn} ×$qty",
            onDismiss = { pickerFor = null },
            onPick = { list ->
                state.lists.addEntry(list.id, target.id, qty)
                pickerFor = null
            },
        )
    }
}

/** 搜索结果行：名称 + 数量步进器 + 「＋」（选清单）+ 「购物车」。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResultRow(
    state: CraftAppState,
    item: CraftItem,
    onOpen: () -> Unit,
    onPickList: (Int) -> Unit,
    onCart: (Int) -> Unit,
) {
    var qty by remember(item.id) { mutableStateOf(1) }
    val craftable = state.db.canCraft(item.id)
    val recipe = remember(item.id, state.dbReady) { if (craftable) state.repo.defaultRecipe(item.id) else null }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pressable(onClick = onOpen) { ItemIcon(item, 44.dp) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                item.nameCn,
                style = CraftType.Row, color = CraftText,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clickable(enabled = craftable, onClick = onOpen),
            )
            FlowRow(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MetaChip(
                    if (recipe != null) "${CraftJobs.abbr(recipe.job)} Lv${recipe.craftLv}" else "不可制作",
                    if (recipe != null) CraftAccent else CraftMuted,
                )
                state.db.jobCatOf(item.jobs)?.let { RoleTag(it) }
            }
            if (craftable) {
                QtyStepper(qty, { qty = it }, Modifier.padding(top = 6.dp))
            }
        }
        if (craftable) {
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                // 右上角加号：弹窗选一个清单放入
                Pressable(onClick = { onPickList(qty) }) {
                    Box(
                        Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(0.5.dp, CraftAccent.copy(alpha = 0.55f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("＋", style = CraftType.Headline, color = CraftAccent)
                    }
                }
                Spacer(Modifier.height(5.dp))
                // 右下角购物车：进临时购物车
                Pressable(onClick = { onCart(qty) }) {
                    Text(
                        "购物车",
                        style = CraftType.Caption, color = Color.White,
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(CraftFill)
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 配方详情（BOM 树）

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeDetail(state: CraftAppState, itemId: Int) {
    val item = remember(itemId, state.dbReady) { state.db.item(itemId) }
    if (item == null) {
        ScreenHeader(title = "未找到", onBack = { state.pop() })
        return
    }
    val recipes = remember(itemId, state.dbReady) { state.db.recipesFor(itemId) }
    var recipeIndex by remember(itemId) { mutableStateOf(0) }
    var wantQty by remember(itemId) { mutableStateOf(1) }
    var picking by remember { mutableStateOf(false) }
    val recipe = recipes.getOrNull(recipeIndex) ?: recipes.firstOrNull()
    val bom = remember(recipe, wantQty, state.dbReady) { state.repo.buildBom(itemId, wantQty, recipe) }
    val materials = remember(bom) {
        buildList<Pair<BomNode, Int>> {
            fun flatten(nodes: List<BomNode>, depth: Int) {
                nodes.forEach { node ->
                    add(node to depth)
                    flatten(node.children, depth + 1)
                }
            }
            flatten(bom.firstOrNull()?.children.orEmpty(), 0)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = item.nameCn, onBack = { state.pop() })
            LazyColumn(Modifier.fillMaxSize()) {
                if (recipes.size > 1) {
                    item {
                        SectionLabel("制作职业")
                        Segmented(
                            recipes.map { CraftJobs.abbr(it.job) },
                            recipeIndex,
                            { recipeIndex = it },
                            Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                if (recipe != null) {
                    item {
                        FlowRow(
                            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            MetaChip(CraftJobs.name(recipe.job))
                            MetaChip("Lv${recipe.craftLv}")
                            state.db.jobCatOf(item.jobs)?.let { RoleTag(it) }
                            if (recipe.stars > 0) MetaChip("★".repeat(recipe.stars))
                            MetaChip("产量 ${recipe.yield}")
                            if (recipe.hq) MetaChip("HQ", CraftHq)
                            if (recipe.qs) MetaChip("快速制作")
                        }
                    }
                }
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("目标数量", style = CraftType.Row, color = CraftText, modifier = Modifier.weight(1f))
                        QtyStepper(wantQty, { wantQty = it })
                    }
                    SectionLabel("所需材料")
                }
                if (bom.isEmpty()) {
                    item {
                        Text("该道具没有可用配方", style = CraftType.Callout, color = CraftMuted, modifier = Modifier.padding(16.dp))
                    }
                }
                itemsIndexed(materials, key = { index, entry -> "$index:${entry.first.item.id}" }, contentType = { _, _ -> "material" }) { _, (node, depth) ->
                    BomRow(state, node, depth)
                    Hairline(startPadding = (52 + depth.coerceAtMost(3) * 12).dp)
                }
                item {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Pressable(onClick = { picking = true }, modifier = Modifier.weight(1f)) {
                            Text(
                                "加入清单",
                                style = CraftType.Row, color = Color.White,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CraftFill)
                                    .padding(vertical = 12.dp),
                            )
                        }
                        Pressable(onClick = { state.cart.add(itemId, wantQty) }, modifier = Modifier.weight(1f)) {
                            Text(
                                "加入购物车",
                                style = CraftType.Row, color = CraftText,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(CraftSurface)
                                    .border(0.5.dp, CraftAccent.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(88.dp)) }
            }
        }
        CartFab(
            count = state.cart.items.size,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { state.push(Page.Cart) }
    }
    if (picking) {
        ListPickerDialog(
            state = state,
            title = "加入清单 · ${item.nameCn} ×$wantQty",
            onDismiss = { picking = false },
            onPick = { list ->
                state.lists.addEntry(list.id, itemId, wantQty)
                picking = false
            },
        )
    }
}

@Composable
private fun BomRow(state: CraftAppState, node: BomNode, depth: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { state.openLocations(node.item.id.toLong()) }
            .padding(start = (16 + depth.coerceAtMost(3) * 12).dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(node.item, 36.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    node.item.nameCn,
                    style = CraftType.Row,
                    color = if (node.crystal) CraftAccent else CraftText,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            if (node.craftable && node.recipe != null) {
                Text(
                    "制作 ${node.craftCount} 次 · 产量 ${node.recipe.yield}",
                    style = CraftType.Caption, color = CraftMuted,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        Text(
            "×${node.totalNeed}",
            style = CraftType.Row,
            color = if (node.crystal) CraftAccent else CraftText,
        )
    }
}

// ---------------------------------------------------------------- 库存 Tab

@Composable
fun InventoryTab(state: CraftAppState) {
    var showCrystals by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf<String?>(null) }
    val crystalRows = remember(state.inventory) { state.inventory.crystals() }
    // Read the snapshot contents, not just their sizes: quantity edits must recalculate materials.
    val sourceEntries = if (source == null) state.cart.items.toList()
        else state.lists.listById(source.orEmpty())?.entries?.toList().orEmpty()
    val calc = remember(sourceEntries, state.inventory, state.dbReady) {
        state.repo.withHeld(state.repo.aggregate(sourceEntries), state.inventory)
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "库存",
            subtitle = if (state.inventory.items.isEmpty()) "等待同步"
                else "最近同步 ${timeFmt.format(Date(state.inventory.updatedMs))}",
        )
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                Row(
                    Modifier.fillMaxWidth().clickable { showCrystals = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("水晶库存", style = CraftType.Row, color = CraftText, modifier = Modifier.weight(1f))
                    Text("${crystalRows.size} 种", style = CraftType.Callout, color = CraftMuted)
                    ImageGlyph(R.drawable.ic2_chevron_right, CraftMuted, Modifier.padding(start = 12.dp).size(18.dp))
                }
                Hairline()
                SectionLabel("材料来源")
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SourceChip("购物车 · ${state.cart.items.size}", source == null) { source = null }
                    state.lists.lists.forEach { list ->
                        SourceChip("${list.name} · ${list.entries.size}", source == list.id) { source = list.id }
                    }
                }
            }
            if (calc.basics.isEmpty() && calc.intermediates.isEmpty()) {
                item {
                    Text(
                        if (source == null) "购物车为空" else "清单为空",
                        style = CraftType.Callout, color = CraftMuted,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            } else {
                item { SectionLabel("基础材料") }
                items(calc.basics, key = { "basic:${it.item.id}" }, contentType = { "material" }) { row ->
                    CalcRow(state, row, state.inventory)
                    Hairline()
                }
                if (calc.intermediates.isNotEmpty()) {
                    item { SectionLabel("中间制品") }
                    items(calc.intermediates, key = { "intermediate:${it.item.id}" }, contentType = { "material" }) { row ->
                        CalcRow(state, row, state.inventory)
                        Hairline()
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
    if (showCrystals) {
        AlertDialog(
            onDismissRequest = { showCrystals = false },
            title = { Text("水晶库存", style = CraftType.Headline) },
            text = {
                LazyColumn(Modifier.height(320.dp)) {
                    if (crystalRows.isEmpty()) {
                        item { Text("尚无水晶数据", style = CraftType.Callout, color = CraftMuted) }
                    }
                    itemsIndexed(crystalRows, key = { _, it -> it.first.itemId }) { _, (row, qty) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val item = state.db.item(row.itemId.toInt())
                            if (item != null) {
                                ItemIcon(item, 36.dp)
                                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                    Text(item.nameCn, style = CraftType.Row, color = CraftText)
                                }
                            } else {
                                Column(Modifier.weight(1f)) { Text("物品 ${row.itemId}", style = CraftType.Row, color = CraftText) }
                            }
                            Text("×$qty", style = CraftType.Headline, color = CraftAccent)
                        }
                        Hairline()
                    }
                }
            },
            confirmButton = {
                Text(
                    "关闭",
                    style = CraftType.Row, color = CraftMuted,
                    modifier = Modifier.clickable { showCrystals = false }.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            },
            containerColor = CraftSurface,
        )
    }
}

// Avoids importing the enum type into every call site above.

private fun groupTotals(snapshot: InventorySnapshot): List<Triple<String, Int, Int>> {
    val retainerNames = snapshot.retainers.associate { it.id to it.name }
    return snapshot.items
        .groupBy { InventoryGroups.labelOf(it, retainerNames).first }
        .map { (group, rows) ->
            Triple(
                group,
                rows.sumOf { it.quantity },
                rows.map { it.itemId }.distinct().size,
            )
        }
        .sortedByDescending { it.second }
}

private fun locKeyOf(item: InventoryItem, snapshot: InventorySnapshot): Pair<String, String> =
    InventoryGroups.labelOf(item, snapshot.retainers.associate { it.id to it.name })

@Composable
private fun Pill(text: String, filled: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        style = CraftType.Callout,
        color = if (filled) Color.White else CraftText,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (filled) CraftFill else CraftSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

// ---------------------------------------------------------------- 工作台 Tab

private class WorkbenchSearchBounds {
    var origin = Offset.Zero
    var area = Rect.Zero
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkbenchTab(state: CraftAppState) {
    var targetItemId by remember { mutableStateOf<Int?>(null) }
    var mode by remember { mutableStateOf(0) } // 0 模拟 1 远程
    var recipeIndex by remember { mutableStateOf(0) }
    var showFoodPicker by remember { mutableStateOf(false) }
    var statsJob by remember { mutableStateOf<Int?>(null) }
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<CraftItem>()) }
    var completedQuery by remember { mutableStateOf<String?>(null) }
    var importedListId by remember { mutableStateOf(state.lists.lists.firstOrNull()?.id) }
    val focusManager = LocalFocusManager.current
    val searchBounds = remember { WorkbenchSearchBounds() }

    fun closeSearch() {
        searchActive = false
        focusManager.clearFocus(force = true)
    }
    BackHandler(searchActive) { closeSearch() }
    LaunchedEffect(state.lists.lists) {
        if (state.lists.listById(importedListId.orEmpty()) == null) importedListId = state.lists.lists.firstOrNull()?.id
    }

    val session = state.session
    val targetItem = remember(targetItemId, state.dbReady) { targetItemId?.let { state.db.item(it) } }
    val targetRecipes = remember(targetItemId, state.dbReady) { targetItemId?.let { state.db.recipesFor(it) }.orEmpty() }
    LaunchedEffect(query, state.dbReady) {
        results = emptyList()
        completedQuery = null
        if (state.dbReady && query.isNotBlank()) {
            val pendingQuery = query
            delay(180)
            results = withContext(Dispatchers.IO) { state.db.search(pendingQuery, 30).filter { state.db.canCraft(it.id) } }
            completedQuery = pendingQuery
        }
    }

    val liveRemote = state.remoteCraft?.takeIf { !it.finished && it.step > 0 }
    Column(Modifier.fillMaxSize()
        .onGloballyPositioned { searchBounds.origin = it.positionInRoot() }
        .pointerInput(searchActive) {
            if (!searchActive) return@pointerInput
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    if (event.changes.any { it.pressed && !it.previousPressed && !searchBounds.area.contains(it.position + searchBounds.origin) }) {
                        closeSearch()
                    }
                }
            }
        },
    ) {
        ScreenHeader(
            title = "工作台",
            actions = {
                if (session == null && liveRemote != null) {
                    Pressable(
                        onClick = { state.adoptCurrentRemoteCraft() },
                        enabled = state.dbReady,
                    ) {
                        Text(
                            "正在制作",
                            style = CraftType.Micro,
                            color = if (state.dbReady) Color.White else CraftMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (state.dbReady) CraftFill else CraftSurface)
                                .padding(horizontal = 9.dp, vertical = 7.dp),
                        )
                    }
                }
                Pressable(onClick = { statsJob = targetRecipes.getOrNull(recipeIndex)?.job ?: state.currentCraftJob ?: 0 }) {
                    ImageGlyph(R.drawable.ic2_settings, CraftText,
                        Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp).size(22.dp).semantics { contentDescription = "制作属性设置" })
                }
            },
        )
        if (session == null) {
            Column(Modifier.fillMaxWidth().onGloballyPositioned { searchBounds.area = it.boundsInRoot() }) {
            BasicTextField(
                value = query,
                onValueChange = { query = it; searchActive = true },
                singleLine = true,
                textStyle = CraftType.Row.copy(color = CraftText),
                cursorBrush = SolidColor(CraftAccent),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    .fillMaxWidth().height(42.dp)
                    .clip(RoundedCornerShape(8.dp)).background(CraftSurface)
                    .onFocusChanged { if (it.isFocused) searchActive = true },
                decorationBox = { field ->
                    Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ImageGlyph(R.drawable.ic2_search, CraftMuted, Modifier.size(18.dp))
                        Box(Modifier.weight(1f).padding(start = 8.dp)) {
                            if (query.isEmpty()) Text("搜索可制作道具", style = CraftType.Callout, color = CraftMuted)
                            field()
                        }
                        if (searchActive) Text("收起", style = CraftType.Callout, color = CraftAccent, modifier = Modifier.clickable { closeSearch() }.padding(start = 8.dp, top = 8.dp, bottom = 8.dp))
                    }
                },
            )
            if (searchActive) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(CraftSurface)
                        .border(1.dp, CraftLine),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("搜索结果", style = CraftType.SectionLabel, color = CraftMuted, modifier = Modifier.weight(1f))
                    }
                    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 190.dp)) {
                        if (query.isBlank()) {
                            item { Text("输入道具名称", style = CraftType.Callout, color = CraftMuted, modifier = Modifier.padding(16.dp)) }
                        } else if (results.isEmpty()) {
                            item {
                                Text(
                                    when {
                                        !state.dbReady -> "配方库载入中…"
                                        completedQuery != query -> "搜索中…"
                                        else -> "没有匹配的可制作道具"
                                    },
                                    style = CraftType.Callout, color = CraftMuted, modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                        items(results, key = { "drawer:${it.id}" }, contentType = { "item" }) { item ->
                            val recipe = remember(item.id, state.dbReady) { state.repo.defaultRecipe(item.id) }
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    targetItemId = item.id
                                    recipeIndex = 0
                                    query = ""
                                    closeSearch()
                                    results = emptyList()
                                }.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ItemIcon(item, 36.dp)
                                Text(item.nameCn, style = CraftType.Row, color = CraftText, modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                recipe?.let { MetaChip(CraftJobs.abbr(it.job) + " Lv" + it.craftLv) }
                            }
                            Hairline()
                        }
                    }
                }
            }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
            ) {
                // 游戏内有人在制作（无论谁发起）且未开远程会话：实时观察卡
                state.remoteCraft?.takeIf { !it.finished && it.step > 0 }?.let { remote ->
                    item {
                        val recipe = remember(remote.recipeId, state.dbReady) { state.db.recipe(remote.recipeId) }
                        val progressMax = remote.progressMax.takeIf { it > 0 } ?: recipe?.pmax ?: 0
                        val qualityMax = remote.qualityMax.takeIf { it > 0 } ?: recipe?.qmax ?: 0
                        val hqChance = remote.hqChance.takeIf { it in 0..100 }
                            ?: recipe?.let { CraftQuality.hqChance(remote.quality, qualityMax, it.hq) } ?: -1
                        SectionLabel("游戏内制作中")
                        GroupCard {
                            Column(Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("第 ${remote.step} 步", style = CraftType.Headline, color = CraftText, modifier = Modifier.weight(1f))
                                    MetaChip(
                                        when (remote.conditionId) { 0 -> "稳定"; 1 -> "高品质"; 2 -> "最高品质"; 3 -> "低品质"; else -> "特殊" },
                                        if (remote.conditionId == 1) CraftFill else CraftMuted,
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                CraftMeter("进展", remote.progress, if (progressMax > 0) progressMax else maxOf(remote.progress, 1), CraftOk)
                                Spacer(Modifier.height(6.dp))
                                CraftMeter("品质", remote.quality, if (qualityMax > 0) qualityMax else maxOf(remote.quality, 1), CraftFill)
                                Spacer(Modifier.height(6.dp))
                                CraftMeter("耐久", remote.durability, if (remote.durabilityMax > 0) remote.durabilityMax else maxOf(remote.durability, 1), CraftDanger)
                                Spacer(Modifier.height(6.dp))
                                CraftMeter("CP", remote.cp, if (remote.cpMax > 0) remote.cpMax else maxOf(remote.cp, 1), CraftAccent)
                                if (recipe?.hq != false) {
                                    Spacer(Modifier.height(10.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        CraftHqChance(hqChance)
                                    }
                                }
                            }
                        }
                    }
                }
                    item {
                        SectionLabel("制作清单")
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (state.lists.lists.isEmpty()) {
                                Text("暂无清单", style = CraftType.Callout, color = CraftMuted)
                            }
                            state.lists.lists.forEach { list ->
                                SourceChip("${list.name} · ${list.entries.size}", importedListId == list.id) { importedListId = list.id }
                            }
                        }
                    }
                    val importedList = importedListId?.let { state.lists.listById(it) }
                    if (importedList != null && importedList.entries.isNotEmpty()) {
                        items(importedList.entries, key = { "list:${it.itemId}" }, contentType = { "item" }) { entry ->
                                val item = remember(entry.itemId, state.dbReady) { state.db.item(entry.itemId) }
                                if (item != null) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                targetItemId = entry.itemId
                                                recipeIndex = 0
                                            }
                                            .padding(horizontal = 16.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        ItemIcon(item, 40.dp)
                                        Text(
                                            item.nameCn,
                                            style = CraftType.Row, color = CraftText,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f).padding(start = 10.dp),
                                        )
                                        if (targetItemId == entry.itemId) MetaChip("已选", CraftOk)
                                        Text("×" + entry.qty, style = CraftType.Callout, color = CraftMuted, modifier = Modifier.padding(start = 8.dp))
                                    }
                                    Hairline()
                                }
                        }
                    }
                // ---- 制作配置（选中目标后出现） ----
                val item = targetItem
                val recipes = targetRecipes
                val recipe = recipes.getOrNull(recipeIndex) ?: recipes.firstOrNull()
                if (item != null && recipe != null) {
                    item {
                    SectionLabel("制作配置")
                    GroupCard {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ItemIcon(item, 48.dp)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(item.nameCn, style = CraftType.Headline, color = CraftText)
                                Text(
                                    CraftJobs.name(recipe.job) + " · Lv" + recipe.craftLv + "★".repeat(recipe.stars) + " · 产量" + recipe.yield,
                                    style = CraftType.Caption, color = CraftMuted,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        if (recipes.size > 1) {
                            Hairline()
                            SectionLabel("制作职业", Modifier.padding(vertical = 4.dp))
                            Segmented(
                                recipes.map { CraftJobs.abbr(it.job) },
                                recipeIndex.coerceAtMost(recipes.size - 1),
                                { recipeIndex = it },
                                Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        Hairline()
                        SectionLabel("单次制作材料", Modifier.padding(vertical = 4.dp))
                        state.db.materialsFor(recipe.id).forEach { line ->
                            val mat = state.db.item(line.itemId)
                            if (mat != null) {
                                val held = state.inventory.totalOf(mat.id)
                                Row(
                                    Modifier.fillMaxWidth().clickable { state.openLocations(mat.id.toLong()) }.padding(horizontal = 16.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ItemIcon(mat, 30.dp)
                                    Text(
                                        mat.nameCn + if (line.isCrystal) "（水晶）" else "",
                                        style = CraftType.Callout, color = CraftText,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                                    )
                                    Text(
                                        held.toString() + " / " + line.qty,
                                        style = CraftType.Callout,
                                        color = if (held >= line.qty) CraftOk else CraftDanger,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    SectionLabel("制作增益")
                    GroupCard {
                        ConsumableSelectionRow(
                            label = "食物",
                            selectedName = state.craftFoodName.takeIf { state.craftFoodId > 0 },
                            onChoose = { showFoodPicker = true },
                            onClear = { state.setCraftFood(0, "") },
                        )
                        Hairline()
                        ConsumableSelectionRow(
                            label = "药剂",
                            selectedName = state.craftPotionName.takeIf { state.craftPotionId > 0 },
                            onChoose = { showFoodPicker = true },
                            onClear = { state.setCraftPotion(0, "") },
                        )
                    }

                    if (mode == 0) {
                        SimulationStatsEntry(recipe.job) { statsJob = recipe.job }
                    }

                    SectionLabel("制作方式")
                    Segmented(
                        listOf("模拟制作", "远程制作"),
                        mode,
                        { mode = it },
                        Modifier.padding(horizontal = 16.dp),
                    )
                    if (mode == 1 && !state.craftConnected) {
                        Text(
                            "未连接游戏插件",
                            style = CraftType.Caption, color = CraftDanger,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        )
                    }
                    Pressable(
                        onClick = {
                            val id = targetItemId ?: return@Pressable
                            val itemNow = state.db.item(id) ?: return@Pressable
                            state.session =
                                if (mode == 0) state.startSimulation(recipe, itemNow.nameCn)
                                else state.startRemoteCraft(recipe, itemNow.nameCn)
                        },
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        enabled = (mode == 0 || state.craftConnected),
                    ) {
                        Text(
                            if (mode == 0) "开始模拟制作" else "开始远程制作",
                            style = CraftType.Headline,
                            color = if (mode == 1 && !state.craftConnected) CraftMuted else Color.White,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (mode == 1 && !state.craftConnected) CraftSurface else CraftFill)
                                .padding(vertical = 14.dp),
                        )
                    }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        } else {
            // 制作中：进度置顶固定，技能区独立滚动；游戏内结束自动退出制作模式。
            androidx.compose.runtime.LaunchedEffect(session) {
                var sawProcess = false
                session.state.collect { s ->
                    when {
                        s == null -> Unit
                        s.remote && s.step > 0 && !s.finished -> sawProcess = true
                        s.remote && s.finished && (sawProcess || s.cancelled) -> {
                            kotlinx.coroutines.delay(1500)
                            state.engine.stop()
                            state.session = null
                        }
                        else -> Unit
                    }
                }
            }
            WorkbenchProgress(session)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                WorkbenchControls(state, session)
            }
        }
    }
    if (showFoodPicker) {
        ConsumablePickerSheet(
            state,
            onDismiss = {
                showFoodPicker = false
                focusManager.clearFocus(force = true)
            },
        )
    }
    statsJob?.let { job ->
        CraftStatsSettingsSheet(state, initialJob = job, onDismiss = { statsJob = null })
    }
}

@Composable
private fun ConsumableSelectionRow(
    label: String,
    selectedName: String?,
    onChoose: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onChoose).padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = CraftType.Caption, color = CraftMuted)
            Text(
                selectedName ?: "未选择",
                style = CraftType.Row,
                color = if (selectedName == null) CraftMuted else CraftText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (selectedName != null) {
            Text(
                "清除",
                style = CraftType.Caption,
                color = CraftDanger,
                modifier = Modifier.clickable(onClick = onClear).padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
        Text("选择 ›", style = CraftType.Callout, color = CraftAccent)
    }
}

/**
 * 游戏端已经筛过制作有益的食物/药剂；这里再与手机当前背包求交集，
 * 避免显示已经被取走的条目；点击保存才同时持久化两种选择。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConsumablePickerSheet(state: CraftAppState, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) { if (state.craftConnected) state.refreshCraftSkills() }
    var foodId by remember { mutableStateOf(state.craftFoodId) }
    var foodName by remember { mutableStateOf(state.craftFoodName) }
    var potionId by remember { mutableStateOf(state.craftPotionId) }
    var potionName by remember { mutableStateOf(state.craftPotionName) }
    val foods = remember(state.inventory, state.craftFoods) {
        craftConsumablesInBag(state.craftFoods, state.inventory.items)
    }
    val potions = remember(state.inventory, state.craftPots) {
        craftConsumablesInBag(state.craftPots, state.inventory.items)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CraftSurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("选择制作增益", style = CraftType.Headline, color = CraftText, modifier = Modifier.weight(1f))
                Text("保存", style = CraftType.Row, color = CraftAccent, modifier = Modifier.clickable {
                    state.setCraftFood(foodId, foodName, potionId, potionName)
                    onDismiss()
                }.padding(8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("食物", Modifier.weight(1f))
                if (foodId > 0) Text("清除", style = CraftType.Caption, color = CraftDanger, modifier = Modifier.clickable { foodId = 0; foodName = "" }.padding(8.dp))
            }
            ConsumableList(state, foods, selectedId = foodId) { item ->
                foodId = item.id
                foodName = item.displayName()
            }
            Row(Modifier.fillMaxWidth().padding(end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("药剂", Modifier.weight(1f))
                if (potionId > 0) Text("清除", style = CraftType.Caption, color = CraftDanger, modifier = Modifier.clickable { potionId = 0; potionName = "" }.padding(8.dp))
            }
            ConsumableList(state, potions, selectedId = potionId) { item ->
                potionId = item.id
                potionName = item.displayName()
            }
        }
    }
}

@Composable
private fun ConsumableList(
    state: CraftAppState,
    items: List<GameCraftConsumable>,
    selectedId: Int,
    onSelect: (GameCraftConsumable) -> Unit,
) {
    if (items.isEmpty()) {
        Text("背包中没有可用项", style = CraftType.Caption, color = CraftMuted, modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp))
        return
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 210.dp)) {
        items(items, key = { it.id }, contentType = { "consumable" }) { item ->
            val dbItem = remember(item.id, state.dbReady) { state.db.item(item.id % 1_000_000) }
            Row(
                Modifier.fillMaxWidth().clickable { onSelect(item) }.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (dbItem != null) {
                    ItemIcon(dbItem, 36.dp)
                } else {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(CraftFill.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                        Text(item.name.take(1), style = CraftType.Row, color = CraftAccent)
                    }
                }
                Column(Modifier.weight(1f).padding(start = 10.dp)) {
                    Text(item.displayName(), style = CraftType.Row, color = CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("持有 ${item.quantity}", style = CraftType.Caption, color = CraftMuted)
                }
                if (item.id == selectedId) Text("已选", style = CraftType.Caption, color = CraftOk)
            }
            Hairline()
        }
    }
}

@Composable
private fun WorkbenchProgress(craft: CraftState) {
    SectionLabel("制作进度")
    GroupCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(craft.itemName, style = CraftType.Headline, color = CraftText, modifier = Modifier.weight(1f))
                if (craft.finished) {
                    MetaChip(
                        if (craft.progress >= craft.progressMax) "完成" else "已结束",
                        CraftOk,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            CraftMeter("进展", craft.progress, craft.progressMax, CraftOk)
            Spacer(Modifier.height(8.dp))
            CraftMeter("品质", craft.quality, craft.qualityMax, CraftFill)
            Spacer(Modifier.height(8.dp))
            CraftMeter("耐久", craft.durability, craft.durabilityMax, CraftDanger)
            Spacer(Modifier.height(8.dp))
            CraftMeter("CP", craft.cp, craft.cpMax, CraftAccent)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                MetaChip("第 ${craft.step} 步", CraftMuted)
                MetaChip(craft.condition, if (craft.condition == "高品质") CraftFill else CraftMuted)
                Spacer(Modifier.weight(1f))
                if (craft.recipe.hq) CraftHqChance(craft.hqChance)
            }
        }
    }
}

@Composable
private fun CraftHqChance(chance: Int) {
    Text(
        if (chance in 0..100) "优质率 $chance%" else "优质率 —",
        style = CraftType.Callout,
        color = if (chance == 100) CraftOk else CraftHq,
    )
}

@Composable
private fun WorkbenchProgress(session: CraftSession) {
    val craft = session.state.collectAsState().value ?: return
    WorkbenchProgress(craft)
}

@Composable
private fun WorkbenchControls(state: CraftAppState, session: CraftSession) {
    val context = LocalContext.current
    val craft = session.state.collectAsState().value ?: return
    val log by session.log.collectAsState()
    var infoSkill by remember { mutableStateOf<SkillDef?>(null) }
    var confirmCancel by remember(session) { mutableStateOf(false) }
    val gameSkills = state.craftSkills
    val skillRows = remember(craft.recipe.job, gameSkills) { CraftSkills.forJob(craft.recipe.job, gameSkills).chunked(2) }
    LaunchedEffect(craft.remote, craft.recipe.job) {
        if (craft.remote && state.craftConnected) state.refreshCraftSkills()
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item { SectionLabel("技能") }
        items(skillRows, key = { it.first().id }, contentType = { "skills" }) { rowSkills ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSkills.forEach { skill ->
                    SkillButton(
                        skill, craft,
                        modifier = Modifier.weight(1f),
                        onInfo = { infoSkill = skill },
                    ) { session.useSkill(skill) }
                }
                if (rowSkills.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        item {
            Text(
                when {
                    craft.cancelPending -> "正在取消…"
                    craft.finished -> "返回工作台"
                    craft.remote -> "退出制作"
                    else -> "结束模拟"
                },
                style = CraftType.Row, color = CraftDanger,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !craft.cancelPending) {
                        if (craft.remote && !craft.finished) confirmCancel = true
                        else { state.engine.stop(); state.session = null }
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
        craft.cancelError?.let { error ->
            item { Text(error, style = CraftType.Caption, color = CraftDanger, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) }
        }
        if (log.isNotEmpty()) {
            item { SectionLabel("制作记录") }
            item {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    log.takeLast(6).reversed().forEach { line ->
                        Text(line, style = CraftType.Caption, color = CraftMuted, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
    infoSkill?.let { skill ->
        SkillInfoDialog(skill) { infoSkill = null }
    }
    if (confirmCancel && !craft.finished) {
        AlertDialog(
            onDismissRequest = { confirmCancel = false },
            title = { Text("取消制作？", style = CraftType.Headline, color = CraftText) },
            text = { Text("将终止游戏中「${craft.itemName}」的本次制作，可能损失已投入的材料。是否继续？", style = CraftType.Row, color = CraftText) },
            confirmButton = {
                Text("是，取消制作", style = CraftType.Row, color = CraftDanger,
                    modifier = Modifier.clickable {
                        confirmCancel = false
                        if (state.craftConnected) session.cancelCraft()
                        else android.widget.Toast.makeText(context, "未连接游戏，无法取消制作", android.widget.Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 12.dp, vertical = 10.dp))
            },
            dismissButton = {
                Text("否，继续制作", style = CraftType.Row, color = CraftAccent,
                    modifier = Modifier.clickable { confirmCancel = false }.padding(horizontal = 12.dp, vertical = 10.dp))
            },
            containerColor = CraftSurface,
        )
    }
}

/** 长按技能弹出的说明卡：游戏同款图标 + 名称 + 消耗 + 效果全文。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillButton(
    skill: SkillDef,
    craft: CraftState,
    modifier: Modifier = Modifier,
    onInfo: () -> Unit = {},
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    var iconBmp by remember(skill.icon) { mutableStateOf(ItemIconLoader.peek(skill.icon)) }
    LaunchedEffect(skill.icon) {
        if (skill.icon > 0 && iconBmp == null) {
            iconBmp = ItemIconLoader.load(context, skill.icon)
        }
    }
    // 远程:亮暗完全跟插件推的 canAct(游戏动画锁/可用性);模拟:只按本地 CP/耐久/状态判断。
    val disabled = if (craft.remote) {
        !craft.canAct || craft.finished
    } else {
        craft.finished || !CraftSimulation.canUse(craft, skill)
    }
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CraftSurface)
            .border(0.5.dp, if (disabled) CraftLine else CraftAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .combinedClickable(onClick = { if (!disabled) onClick() }, onLongClick = onInfo)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(Modifier.heightIn(min = 44.dp), verticalAlignment = Alignment.CenterVertically) {
            val bmp = iconBmp
            if (bmp != null) {
                Image(bmp.asImageBitmap(), null, Modifier.size(26.dp).alpha(if (disabled) 0.3f else 1f))
            } else {
                Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    Text("·", style = CraftType.Row, color = CraftMuted)
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                skill.cn,
                style = CraftType.Callout, color = if (disabled) CraftMuted else CraftText,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            buildString {
                val cp = if (craft.remote) skill.cp else CraftSimulation.cpCost(craft, skill)
                val durability = if (craft.remote) skill.durability else CraftSimulation.durabilityCost(craft, skill)
                if (cp > 0) append("CP $cp ")
                if (durability > 0) append("耐久 -$durability")
                if (cp == 0 && durability == 0) append("辅助")
            },
            style = CraftType.Micro, color = CraftMuted,
        )
    }
}

@Composable
private fun SkillInfoDialog(skill: SkillDef, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var iconBmp by remember(skill.icon) { mutableStateOf(ItemIconLoader.peek(skill.icon)) }
    androidx.compose.runtime.LaunchedEffect(skill.icon) {
        if (skill.icon > 0 && iconBmp == null) {
            iconBmp = ItemIconLoader.load(context, skill.icon)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val bmp = iconBmp
                if (bmp != null) {
                    Image(bmp.asImageBitmap(), null, Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                }
                Column {
                    Text(skill.cn, style = CraftType.Headline, color = CraftText)
                    Text(skill.en, style = CraftType.Caption, color = CraftMuted)
                }
            }
        },
        text = {
            Column {
                Text(
                    buildString {
                        append("消耗：")
                        if (skill.cp > 0) append("CP ${skill.cp} ")
                        if (skill.durability > 0) append("耐久 ${skill.durability}")
                        if (skill.cp == 0 && skill.durability == 0) append("无")
                    },
                    style = CraftType.Caption, color = CraftMuted,
                )
                Spacer(Modifier.height(8.dp))
                Text(skill.desc, style = CraftType.Body, color = CraftText)
            }
        },
        confirmButton = {
            Text(
                "关闭",
                style = CraftType.Row, color = CraftAccent,
                modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        },
        containerColor = CraftSurface,
    )
}

@Composable
private fun CraftMeter(label: String, value: Int, max: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = CraftType.Caption, color = CraftMuted, modifier = Modifier.width(36.dp))
        MeterBar(value, max, color, Modifier.weight(1f))
        Text(
            "$value / $max",
            style = CraftType.Caption, color = CraftMuted,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun SourceChip(label: String, active: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = CraftType.Callout,
        color = if (active) Color.White else CraftText,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) CraftFill else CraftSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/**
 * 库存计算器一行：需要 / 背包 / 待取回 / 缺少。
 * 待取回 = 还需要但不在背包里的部分（雇员、部队仓库、房屋、鞍袋等）。
 */
@Composable
private fun CalcRow(state: CraftAppState, row: AggregateRow, snapshot: InventorySnapshot) {
    val held = snapshot.totalOf(row.item.id)
    val inBag = snapshot.bagOf(row.item.id)
    val retrieve = minOf((row.need - inBag).coerceAtLeast(0), (held - inBag).coerceAtLeast(0))
    val missing = (row.need - held).coerceAtLeast(0)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { state.openLocations(row.item.id.toLong()) }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(row.item, 38.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(
                row.item.nameCn,
                style = CraftType.Row, color = CraftText,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append("需 ${row.need} · 背包 $inBag")
                    if (retrieve > 0) append(" · 待取回 $retrieve")
                },
                style = CraftType.Caption,
                color = CraftMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (missing > 0) {
            Text(
                "缺 $missing",
                style = CraftType.Headline, color = CraftDanger,
            )
        } else {
            Text(
                "充足",
                style = CraftType.Callout, color = CraftOk,
            )
        }
    }
}

// ---------------------------------------------------------------- 位置弹窗

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPopup(state: CraftAppState, itemId: Long) {
    val item = state.db.item(itemId.toInt())
    val locations = remember(state.inventory, itemId) { state.inventory.locationsOf(itemId.toInt()) }
    ModalBottomSheet(
        onDismissRequest = { state.locationPopupItem = null },
        containerColor = CraftSurface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (item != null) {
                    ItemIcon(item, 44.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(item.nameCn, style = CraftType.Headline, color = CraftText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(
                            "共持有 ${state.inventory.totalOf(itemId.toInt())}",
                            style = CraftType.Caption, color = CraftMuted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                } else {
                    Text("物品 $itemId", style = CraftType.Headline, color = CraftText)
                }
            }
            Spacer(Modifier.height(8.dp))
            if (locations.isEmpty()) {
                Text(
                    if (state.inventory.items.isEmpty()) "库存尚未同步" else "未持有此道具",
                    style = CraftType.Callout, color = CraftMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth()) {
                    items(locations, key = { "${it.group}:${it.label}" }) { location ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(CraftFill.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(location.group.take(1), style = CraftType.Row, color = CraftAccent)
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(location.label, style = CraftType.Row, color = CraftText)
                            Text(
                                "${location.group} · ${location.rows.size} 格${if (location.rows.any { it.hq }) " · 含 HQ" else ""}",
                                style = CraftType.Caption, color = CraftMuted,
                            )
                        }
                        Text("×${location.quantity}", style = CraftType.Headline, color = CraftAccent)
                    }
                }
                }
            }
        }
    }
}


// ---------------------------------------------------------------- 购物车

/**
 * 购物车页：临时清单。普通模式可改数量/移除；长按进入多选，
 * 多选模式下 左上角删除、右上角＋放入指定清单、其左侧为全选。
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun CartPage(state: CraftAppState) {
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<Int>()) }
    var picking by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        if (!selecting) {
            ScreenHeader(
                title = "购物车",
                subtitle = "${state.cart.items.size} 种道具",
                onBack = { state.pop() },
            ) {
                if (state.cart.items.isNotEmpty()) {
                    Text(
                        "选择",
                        style = CraftType.Row, color = CraftAccent,
                        modifier = Modifier.clickable { selecting = true }.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        } else {
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "删除",
                    style = CraftType.Row, color = CraftDanger,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CraftSurface)
                        .clickable {
                            state.cart.removeIds(selected)
                            selected = emptySet()
                            selecting = false
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
                Text(
                    "全选",
                    style = CraftType.Callout, color = CraftAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CraftSurface)
                        .clickable {
                            selected = if (selected.size == state.cart.items.size) emptySet() else state.cart.items.map { it.itemId }.toSet()
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
                Text(
                    "＋ 放入清单",
                    style = CraftType.Callout, color = if (selected.isEmpty()) CraftMuted else CraftAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CraftSurface)
                        .clickable(enabled = selected.isNotEmpty()) { picking = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
                Text(
                    "完成",
                    style = CraftType.Callout, color = CraftMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CraftSurface)
                        .clickable {
                            selecting = false
                            selected = emptySet()
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.cart.items, key = { it.itemId }) { entry ->
                val item = remember(entry.itemId, state.dbReady) { state.db.item(entry.itemId) }
                if (item != null) {
                    val row: @Composable () -> Unit = {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (selecting) {
                                            selected = if (entry.itemId in selected) selected - entry.itemId else selected + entry.itemId
                                        }
                                    },
                                    onLongClick = {
                                        selecting = true
                                        selected = setOf(entry.itemId)
                                    },
                                )
                                .padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (selecting) {
                                Checkbox(
                                    checked = entry.itemId in selected,
                                    onCheckedChange = { checked ->
                                        selected = if (checked) selected + entry.itemId else selected - entry.itemId
                                    },
                                )
                            }
                            ItemIcon(item, 44.dp)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(item.nameCn, style = CraftType.Row, color = CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            if (!selecting) {
                                QtyStepper(entry.qty, { state.cart.setQty(entry.itemId, it) })
                            } else {
                                Text("×" + entry.qty, style = CraftType.Row, color = CraftAccent)
                            }
                        }
                    }
                    if (selecting) {
                        row()
                    } else {
                        // 左滑填充删除：防误触，填满后松手才移除
                        SwipeDeleteRow(onRemove = { state.cart.remove(entry.itemId) }) { row() }
                    }
                    Hairline()
                }
            }
            if (state.cart.items.isEmpty()) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("购物车为空", style = CraftType.Row, color = CraftText)
                        Text(
                            "添加道具",
                            style = CraftType.Row, color = CraftAccent,
                            modifier = Modifier.clickable { state.tab = Tab.RECIPES; state.pop() }.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
    if (picking) {
        ListPickerDialog(
            state = state,
            title = "加入清单 · ${selected.size} 种道具",
            onDismiss = { picking = false },
            onPick = { list ->
                state.moveCartToList(list.id, selected)
                picking = false
                selecting = false
                selected = emptySet()
            },
        )
    }
}

/**
 * 清单选择弹窗：固定大小，清单过多时弹窗内上下滑动。
 */
@Composable
private fun ListPickerDialog(
    state: CraftAppState,
    title: String,
    onDismiss: () -> Unit,
    onPick: (CraftList) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = CraftType.Headline, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            LazyColumn(Modifier.height(320.dp)) {
                if (state.lists.lists.isEmpty()) {
                    item {
                        Text(
                            "暂无制作清单",
                            style = CraftType.Callout, color = CraftMuted,
                        )
                    }
                }
                items(state.lists.lists, key = { it.id }) { list ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(list) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(list.name, style = CraftType.Row, color = CraftText, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(list.entries.size.toString() + " 项", style = CraftType.Caption, color = CraftMuted)
                    }
                    Hairline()
                }
            }
        },
        confirmButton = {
            Text(
                "取消",
                style = CraftType.Row, color = CraftMuted,
                modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 6.dp),
            )
        },
        containerColor = CraftSurface,
    )
}
