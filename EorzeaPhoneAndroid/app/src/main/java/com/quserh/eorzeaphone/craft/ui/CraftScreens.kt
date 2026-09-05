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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.Image
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quserh.eorzeaphone.craft.CraftAppState
import com.quserh.eorzeaphone.craft.Page
import com.quserh.eorzeaphone.craft.data.AggregateRow
import com.quserh.eorzeaphone.craft.data.BomNode
import com.quserh.eorzeaphone.craft.data.CraftItem
import com.quserh.eorzeaphone.craft.data.CraftJobs
import com.quserh.eorzeaphone.craft.data.CraftList
import com.quserh.eorzeaphone.craft.data.CraftSession
import com.quserh.eorzeaphone.craft.data.CraftSkills
import com.quserh.eorzeaphone.craft.data.CraftState
import com.quserh.eorzeaphone.craft.data.InventoryGroups
import com.quserh.eorzeaphone.craft.data.InventorySnapshot
import com.quserh.eorzeaphone.craft.data.InventoryItem
import com.quserh.eorzeaphone.craft.data.SkillDef
import com.quserh.eorzeaphone.data.ItemIconLoader
import kotlinx.coroutines.launch
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

private val timeFmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

// ---------------------------------------------------------------- shared bits

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().background(CraftBackground)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Pressable(onClick = onBack) {
                    Text("‹ 返回", style = CraftType.Row, color = CraftAccent, modifier = Modifier.padding(end = 8.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = CraftType.Header, color = CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, style = CraftType.Caption, color = CraftMuted, modifier = Modifier.padding(top = 1.dp))
                }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String, color: Color = CraftAccent) {
    Text(
        text,
        style = CraftType.Micro,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun InlineField(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = onValue,
        placeholder = { Text(hint, style = CraftType.Body, color = CraftMuted) },
        textStyle = CraftType.Row,
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = CraftSurface,
            unfocusedContainerColor = CraftSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------- 清单 Tab

@Composable
fun ListTab(state: CraftAppState) {
    var showNew by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "制作清单", subtitle = "自由添加道具与数量，自动汇总材料")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Pressable(onClick = { showNew = true }) {
                Text(
                    "＋ 新建清单",
                    style = CraftType.Callout,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CraftFill)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        if (state.lists.lists.isEmpty()) {
            Text(
                "还没有清单，先新建一个吧",
                style = CraftType.Callout, color = CraftMuted,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.lists.lists, key = { it.id }) { list ->
                Pressable(onClick = { state.push(Page.ListDetail(list.id)) }) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(list.name, style = CraftType.Row, color = CraftText)
                            Text(
                                "${list.entries.size} 种道具 · ${timeFmt.format(Date(list.updatedMs))}",
                                style = CraftType.Caption, color = CraftMuted,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CraftMuted.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) { Text("›", style = CraftType.Row, color = CraftMuted) }
                    }
                }
                Hairline()
            }
        }
    }
    if (showNew) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNew = false },
            title = { Text("新建清单", style = CraftType.Headline) },
            text = {
                InlineField(name, { name = it }, "清单名称，例如「7.4 主手」")
            },
            confirmButton = {
                Text(
                    "创建",
                    style = CraftType.Row, color = CraftAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (name.isNotBlank()) state.lists.addList(name.trim())
                            showNew = false
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            },
            dismissButton = {
                Text(
                    "取消",
                    style = CraftType.Row, color = CraftMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showNew = false }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            },
            containerColor = CraftSurface,
        )
    }
}

@Composable
fun ListDetail(state: CraftAppState, listId: String) {
    val list = state.lists.listById(listId) ?: return

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = list.name,
            subtitle = list.entries.size.toString() + " 种道具 · 材料汇总去「库存」页选本清单计算",
            onBack = { state.pop() },
        )
        LazyColumn(Modifier.fillMaxSize()) {
            if (list.entries.isNotEmpty()) {
                item { SectionLabel("目标道具") }
                item {
                    GroupCard {
                        list.entries.forEachIndexed { index, entry ->
                            val item = state.db.item(entry.itemId)
                            if (item != null) {
                                EntryRow(state, list, item, entry.qty)
                                if (index < list.entries.size - 1) Hairline()
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            } else {
                item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("清单还是空的", style = CraftType.Row, color = CraftText)
                        Text("去「配方」页搜索道具并加入清单", style = CraftType.Callout, color = CraftMuted, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryRow(state: CraftAppState, list: CraftList, item: CraftItem, qty: Int) {
    var edit by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clickable { edit = true }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(item, 44.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.nameCn, style = CraftType.Row, color = CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                buildString {
                    append(CraftJobs.abbr(state.repo.defaultRecipe(item.id)?.job ?: 0))
                    append(" · Lv")
                    append(state.repo.defaultRecipe(item.id)?.craftLv ?: 0)
                },
                style = CraftType.Caption, color = CraftMuted,
            )
        }
        QtyStepper(qty, { state.lists.setQty(list.id, item.id, it) })
    }
    if (edit) {
        var qtyText by remember { mutableStateOf(qty.toString()) }
        AlertDialog(
            onDismissRequest = { edit = false },
            title = { Text(item.nameCn, style = CraftType.Headline) },
            text = { InlineField(qtyText, { qtyText = it.filter { c -> c.isDigit() } }, "目标数量") },
            confirmButton = {
                Text(
                    "保存",
                    style = CraftType.Row, color = CraftAccent,
                    modifier = Modifier.clickable {
                        state.lists.setQty(list.id, item.id, qtyText.toIntOrNull() ?: qty)
                        edit = false
                    }.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            },
            dismissButton = {
                Text(
                    "删除",
                    style = CraftType.Row, color = CraftDanger,
                    modifier = Modifier.clickable {
                        state.lists.removeEntry(list.id, item.id)
                        edit = false
                    }.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            },
            containerColor = CraftSurface,
        )
    }
}

@Composable
private fun MaterialRow(state: CraftAppState, row: AggregateRow, intermediate: Boolean = false) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { state.openLocations(row.item.id.toLong()) }
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(row.item, 38.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.item.nameCn,
                    style = CraftType.Row, color = CraftText,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (row.crystal) {
                    Spacer(Modifier.width(6.dp))
                    MetaChip("水晶", CraftAccent)
                } else if (intermediate) {
                    Spacer(Modifier.width(6.dp))
                    MetaChip("可制作", CraftOk)
                }
            }
            Text(
                if (row.held >= row.need) "持有充足 · 点击查看位置" else "还缺 ${row.need - row.held}",
                style = CraftType.Caption,
                color = if (row.held >= row.need) CraftMuted else CraftDanger,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            "需 ${row.need} · 有 ${row.held}",
            style = CraftType.Callout,
            color = if (row.held >= row.need) CraftMuted else CraftDanger,
        )
    }
}

// ---------------------------------------------------------------- 配方 Tab

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun RecipesTab(state: CraftAppState) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<CraftItem>()) }
    var repairing by remember { mutableStateOf(false) }
    var pickerFor by remember { mutableStateOf<Pair<CraftItem, Int>?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // The db opens async (asset decompress on first launch); re-run the pending
    // query once it is ready so an early keystroke is not stuck on empty results.
    androidx.compose.runtime.LaunchedEffect(state.dbReady) {
        if (state.dbReady && query.isNotBlank()) results = state.db.search(query)
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = "配方", subtitle = "离线查询 · 「＋」放入清单，「购物车」先攒着")
            InlineField(
                query,
                {
                    query = it
                    results = if (it.isBlank()) emptyList() else state.db.search(it)
                },
                "搜索道具（中/日/英名均可）",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            if (query.isBlank()) {
                if (state.cart.history.isEmpty()) {
                    Text(
                        "搜索过的道具会记在这里，长按可删除",
                        style = CraftType.Callout, color = CraftMuted,
                        modifier = Modifier.padding(20.dp),
                    )
                } else {
                    SectionLabel("搜索历史")
                    GroupCard {
                        state.cart.history.forEachIndexed { index, h ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            query = h
                                            results = state.db.search(h)
                                        },
                                        onLongClick = { state.cart.removeHistory(h) },
                                    )
                                    .padding(horizontal = 16.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(h, style = CraftType.Row, color = CraftText, modifier = Modifier.weight(1f))
                                Text("长按删除", style = CraftType.Caption, color = CraftMuted)
                            }
                            if (index < state.cart.history.size - 1) Hairline()
                        }
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
            title = "「${target.nameCn}」×$qty 放入指定清单",
            onDismiss = { pickerFor = null },
            onPick = { list ->
                state.lists.addEntry(list.id, target.id, qty)
                pickerFor = null
            },
        )
    }
}

/** 搜索结果行：名称 + 数量步进器 + 「＋」（选清单）+ 「购物车」。 */
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
    val recipe = if (craftable) state.repo.defaultRecipe(item.id) else null
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Pressable(onClick = onOpen) { ItemIcon(item, 44.dp) }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.nameCn,
                    style = CraftType.Row, color = CraftText,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                MetaChip(
                    if (recipe != null) "${CraftJobs.abbr(recipe.job)} Lv${recipe.craftLv}" else "不可制作",
                    if (recipe != null) CraftAccent else CraftMuted,
                )
            }
            state.db.jobCatOf(item.jobs)?.let { RoleTag(it) }
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

@Composable
fun RecipeDetail(state: CraftAppState, itemId: Int) {
    val item = remember(itemId) { state.db.item(itemId) }
    if (item == null) {
        ScreenHeader(title = "未找到", onBack = { state.pop() })
        return
    }
    val recipes = remember(itemId) { state.db.recipesFor(itemId) }
    var recipeIndex by remember(itemId) { mutableStateOf(0) }
    var wantQty by remember(itemId) { mutableStateOf(1) }
    var picking by remember { mutableStateOf(false) }
    val recipe = recipes.getOrNull(recipeIndex) ?: recipes.firstOrNull()
    val bom = remember(recipe, wantQty, state.dbReady) { state.repo.buildBom(itemId, wantQty, recipe) }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(title = item.nameCn, subtitle = "${item.nameJp} · ${item.nameEn}", onBack = { state.pop() })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (recipes.size > 1) {
                    SectionLabel("配方（多职业）")
                    Segmented(
                        recipes.map { CraftJobs.abbr(it.job) },
                        recipeIndex,
                        { recipeIndex = it },
                        Modifier.padding(horizontal = 16.dp),
                    )
                }
                if (recipe != null) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MetaChip("${CraftJobs.name(recipe.job)}")
                        MetaChip("Lv${recipe.craftLv}")
                        state.db.jobCatOf(item.jobs)?.let { RoleTag(it) }
                        if (recipe.stars > 0) MetaChip("★".repeat(recipe.stars))
                        MetaChip("产量 ${recipe.yield}")
                        if (recipe.hq) MetaChip("可 HQ", CraftHq)
                        if (recipe.qs) MetaChip("可快修")
                    }
                }
                SectionLabel("制作 ${wantQty} 个所需材料")
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("数量", style = CraftType.Callout, color = CraftMuted)
                    Spacer(Modifier.width(10.dp))
                    QtyStepper(wantQty, { wantQty = it })
                }
                GroupCard(Modifier.padding(top = 8.dp)) {
                    if (bom.isEmpty()) {
                        Text("该道具没有可用配方", style = CraftType.Callout, color = CraftMuted, modifier = Modifier.padding(16.dp))
                    } else {
                        val flat = mutableListOf<Pair<BomNode, Int>>()
                        fun flatten(nodes: List<BomNode>, depth: Int) {
                            nodes.forEach { node ->
                                flat.add(node to depth)
                                flatten(node.children, depth + 1)
                            }
                        }
                        flatten(bom.first().children, 0)
                        flat.forEachIndexed { index, (node, depth) ->
                            BomRow(state, node, depth)
                            if (index < flat.size - 1) Hairline(startPadding = (52 + depth * 20).dp)
                        }
                    }
                }
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
                                .clip(RoundedCornerShape(10.dp))
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
                                .clip(RoundedCornerShape(10.dp))
                                .background(CraftSurface)
                                .border(0.5.dp, CraftAccent.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                .padding(vertical = 12.dp),
                        )
                    }
                }
                Spacer(Modifier.height(72.dp))
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
            title = "「${item.nameCn}」×$wantQty 放入指定清单",
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
            .padding(start = (16 + depth * 20).dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
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
                if (node.craftable) {
                    Spacer(Modifier.width(6.dp))
                    MetaChip("可制作", CraftOk)
                }
            }
            if (node.craftable && node.recipe != null) {
                Text(
                    "需做 ${node.craftCount} 次（产量 ${node.recipe.yield}）",
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
    val crystalRows = remember(state.inventory) { state.inventory.crystals() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenHeader(title = "库存", subtitle = "由终端连接实时同步")
        SectionLabel("库存同步")
        GroupCard {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("由终端连接同步", style = CraftType.Row, color = CraftText)
                    Text(
                        if (state.inventory.items.isEmpty()) "等待库存快照（游戏在线后自动到达）"
                        else "最近同步 ${timeFmt.format(Date(state.inventory.updatedMs))} · ${state.inventory.items.size} 格",
                        style = CraftType.Caption, color = CraftMuted, modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Box(Modifier.size(8.dp).background(if (state.inventory.items.isNotEmpty()) CraftOk else CraftMuted.copy(alpha = 0.4f), CircleShape))
            }
        }
        SectionLabel("水晶")
        GroupCard {
            Row(
                Modifier.fillMaxWidth().clickable { showCrystals = true }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("水晶库存", style = CraftType.Row, color = CraftText)
                    Text(
                        if (crystalRows.isEmpty()) "尚无水晶数据" else "${crystalRows.size} 种水晶 · 点击查看全部",
                        style = CraftType.Caption, color = CraftMuted,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text("›", style = CraftType.Row, color = CraftMuted)
            }
        }
        var source by remember { mutableStateOf<String?>(null) }
        val sourceEntries = remember(source, state.cart.items.size, state.lists.lists.size) {
            if (source == null) state.cart.items.toList()
            else state.lists.listById(source ?: "")?.entries?.toList() ?: emptyList()
        }
        val calc = remember(sourceEntries, state.inventory, state.dbReady) {
            state.repo.withHeld(state.repo.aggregate(sourceEntries), state.inventory)
        }
        SectionLabel("计算对象")
        GroupCard {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SourceChip("购物车(${state.cart.items.size})", source == null) { source = null }
                state.lists.lists.forEach { list ->
                    SourceChip("${list.name}(${list.entries.size})", source == list.id) { source = list.id }
                }
            }
        }
        if (calc.basics.isEmpty() && calc.intermediates.isEmpty()) {
            Text(
                if (source == null) "购物车是空的：去「配方」页加几个道具，或选择一个清单。"
                else "这个清单还没有道具。",
                style = CraftType.Callout, color = CraftMuted,
                modifier = Modifier.padding(20.dp),
            )
        } else {
            SectionLabel("基础材料（含水晶）")
            GroupCard {
                calc.basics.forEachIndexed { index, row ->
                    CalcRow(state, row, state.inventory)
                    if (index < calc.basics.size - 1) Hairline()
                }
            }
            if (calc.intermediates.isNotEmpty()) {
                SectionLabel("中间制品")
                GroupCard {
                    calc.intermediates.forEachIndexed { index, row ->
                        CalcRow(state, row, state.inventory, intermediate = true)
                        if (index < calc.intermediates.size - 1) Hairline()
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
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
                                    Text(
                                        row.name.ifBlank { item.nameJp },
                                        style = CraftType.Caption, color = CraftMuted,
                                    )
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

@Composable
fun WorkbenchTab(state: CraftAppState) {
    var targetItemId by remember { mutableStateOf<Int?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var mode by remember { mutableStateOf(0) } // 0 模拟 1 远程
    var recipeIndex by remember { mutableStateOf(0) }

    val session = state.session
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "手动工作台", subtitle = "模拟练习 · 远程操控游戏内真实制作")
        if (session == null) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                SectionLabel("制作目标")
                GroupCard {
                    val item = targetItemId?.let { state.db.item(it) }
                    val recipes = targetItemId?.let { state.db.recipesFor(it) } ?: emptyList()
                    val recipe = recipes.getOrNull(recipeIndex) ?: recipes.firstOrNull()
                    if (item == null) {
                        Row(
                            Modifier.fillMaxWidth().clickable { showPicker = true }.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("选择要制作的道具", style = CraftType.Row, color = CraftAccent, modifier = Modifier.weight(1f))
                            Text("从清单或搜索 ›", style = CraftType.Caption, color = CraftMuted)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ItemIcon(item, 48.dp)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(item.nameCn, style = CraftType.Headline, color = CraftText)
                                if (recipe != null) {
                                    Text(
                                        "${CraftJobs.name(recipe.job)} · Lv${recipe.craftLv}${"★".repeat(recipe.stars)} · 产量${recipe.yield}",
                                        style = CraftType.Caption, color = CraftMuted,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                            Text("更换", style = CraftType.Callout, color = CraftAccent, modifier = Modifier.clickable { showPicker = true })
                        }
                        // 一个道具多个职业都能做（如修理材料）：手动选职业配方
                        if (recipes.size > 1) {
                            Hairline()
                            SectionLabel("用哪个职业的配方", Modifier.padding(vertical = 4.dp))
                            Segmented(
                                recipes.map { CraftJobs.abbr(it.job) },
                                recipeIndex.coerceAtMost(recipes.size - 1),
                                { recipeIndex = it },
                                Modifier.padding(horizontal = 16.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        if (recipe != null) {
                            Hairline()
                            SectionLabel("直接材料（做 1 个）", Modifier.padding(vertical = 4.dp))
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
                                            "$held / ${line.qty}",
                                            style = CraftType.Callout,
                                            color = if (held >= line.qty) CraftOk else CraftDanger,
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
                SectionLabel("制作方式")
                Segmented(
                    listOf("模拟制作（离线）", "远程操控（游戏）"),
                    mode,
                    { mode = it },
                    Modifier.padding(horizontal = 16.dp),
                )
                if (mode == 1 && !state.craftConnected) {
                    Text(
                        "远程操控需要终端已连接游戏插件且角色在线；当前不可用，可先用模拟模式。",
                        style = CraftType.Caption, color = CraftDanger,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                    )
                }
                Pressable(
                    onClick = {
                        val id = targetItemId ?: return@Pressable
                        val recipes = state.db.recipesFor(id)
                        val recipe = recipes.getOrNull(recipeIndex) ?: state.repo.defaultRecipe(id) ?: return@Pressable
                        val item = state.db.item(id) ?: return@Pressable
                        // 远程模式插件端会按配方的职业自动切换角色
                        state.session =
                            if (mode == 0) state.engine.startMock(recipe, item.nameCn)
                            else state.startRemoteCraft(recipe, item.nameCn)
                    },
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    enabled = targetItemId != null && (mode == 0 || state.craftConnected),
                ) {
                    Text(
                        when {
                            targetItemId == null -> "先选择制作目标"
                            mode == 0 -> "开始模拟制作"
                            else -> "开始远程制作（游戏内）"
                        },
                        style = CraftType.Headline,
                        color = if (targetItemId == null || (mode == 1 && !state.craftConnected)) CraftMuted else Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (targetItemId == null || (mode == 1 && !state.craftConnected)) CraftSurface else CraftFill,
                            )
                            .padding(vertical = 14.dp),
                    )
                }
                Text(
                    "远程模式：游戏端按配方自动切换职业、打开配方笔记并开始制作；进度/品质/耐久实时回传，技能按钮直接驱动游戏内角色施放。",
                    style = CraftType.Caption, color = CraftMuted,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(24.dp))
            }
        } else {
            // 制作中：进度置顶固定，技能区独立滚动，滑技能时进度始终可见
            // 游戏内制作结束（含手动完成/中断）后自动退出制作模式。
            androidx.compose.runtime.LaunchedEffect(session) {
                var sawProcess = false
                session.state.collect { s ->
                    when {
                        s == null -> Unit
                        // 远程模式:制作中途(有步进)才算真的开始过,结束时才退出;
                        // 否则(旁观玩家手动做、或起始帧就 finished)只记日志。
                        s.remote && s.step > 0 && !s.finished -> sawProcess = true
                        s.remote && s.finished && sawProcess -> {
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
    if (showPicker) {
        TargetPickerDialog(state, onPick = { targetItemId = it; recipeIndex = 0; showPicker = false }, onDismiss = { showPicker = false })
    }
}

@Composable
private fun TargetPickerDialog(state: CraftAppState, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val listEntries = state.lists.lists.firstOrNull()?.entries ?: emptyList()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择制作目标", style = CraftType.Headline) },
        text = {
            Column {
                InlineField(query, { query = it }, "搜索道具", Modifier.fillMaxWidth())
                LazyColumn(Modifier.height(280.dp)) {
                    if (query.isBlank()) {
                        items(listEntries, key = { it.itemId }) { entry ->
                            val item = state.db.item(entry.itemId)
                            if (item != null) {
                                Row(
                                    Modifier.fillMaxWidth().clickable { onPick(item.id) }.padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    ItemIcon(item, 34.dp)
                                    Text(
                                        "${item.nameCn} ×${entry.qty}",
                                        style = CraftType.Row, color = CraftText,
                                        modifier = Modifier.padding(start = 10.dp),
                                    )
                                }
                            }
                        }
                    }
                    val hits = if (query.isBlank()) emptyList() else state.db.search(query, 30).filter { state.db.canCraft(it.id) }
                    itemsIndexed(hits, key = { _, it -> it.id }) { _, item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPick(item.id) }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ItemIcon(item, 34.dp)
                            Text(item.nameCn, style = CraftType.Row, color = CraftText, modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Text("关闭", style = CraftType.Row, color = CraftMuted,
                modifier = Modifier.clickable { onDismiss() }.padding(horizontal = 12.dp, vertical = 6.dp))
        },
        containerColor = CraftSurface,
    )
}

@Composable
private fun WorkbenchProgress(session: CraftSession) {
    val craft = session.state.collectAsState().value ?: return
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                MetaChip("第 ${craft.step} 步", CraftMuted)
                MetaChip(craft.condition, if (craft.condition == "高品质") CraftFill else CraftMuted)
            }
        }
    }
}

@Composable
private fun WorkbenchControls(state: CraftAppState, session: CraftSession) {
    val craft = session.state.collectAsState().value ?: return
    val log by session.log.collectAsState()
    val cooldown by session.cooldown.collectAsState()
    var infoSkill by remember { mutableStateOf<SkillDef?>(null) }

    LazyColumn(Modifier.fillMaxSize()) {
        item { SectionLabel("技能") }
        item {
            GroupCard {
                val skills = CraftSkills.ALL
                skills.chunked(2).forEachIndexed { index, rowSkills ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowSkills.forEach { skill ->
                            SkillButton(
                                skill, craft,
                                cooling = !craft.remote && cooldown > 0,
                                modifier = Modifier.weight(1f),
                                onInfo = { infoSkill = skill },
                            ) { session.useSkill(skill) }
                        }
                        if (rowSkills.size == 1) Spacer(Modifier.weight(1f))
                    }
                    if (index < (skills.size + 1) / 2 - 1) Hairline()
                }
                Hairline()
                Text(
                    "停止制作",
                    style = CraftType.Row, color = CraftDanger,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { state.engine.stop(); state.session = null }
                        .padding(vertical = 12.dp),
                )
            }
        }
        item { SectionLabel("记录") }
        item {
            GroupCard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
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
}

/** 长按技能弹出的说明卡：游戏同款图标 + 名称 + 消耗 + 效果全文。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SkillButton(
    skill: SkillDef,
    craft: CraftState,
    cooling: Boolean,
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
    // 远程:亮暗完全跟插件推的 canAct(游戏动画锁/可用性);模拟:本地冷却+CP。
    val disabled = if (craft.remote) {
        !craft.canAct || craft.finished
    } else {
        cooling || craft.finished || craft.cp < skill.cp
    }
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CraftSurface)
            .border(0.5.dp, if (disabled) CraftLine else CraftAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .combinedClickable(enabled = !disabled, onClick = onClick, onLongClick = onInfo)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val bmp = iconBmp
            if (bmp != null) {
                Image(bmp.asImageBitmap(), null, Modifier.size(26.dp))
            } else {
                Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    Text("·", style = CraftType.Row, color = CraftMuted)
                }
            }
            Spacer(Modifier.width(6.dp))
            Text(
                skill.cn,
                style = CraftType.Callout, color = if (disabled) CraftMuted else CraftText,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Text(
            buildString {
                if (skill.cp > 0) append("CP ${skill.cp} ")
                if (skill.durability > 0) append("耐久 -${skill.durability}")
                if (skill.cp == 0 && skill.durability == 0) append("辅助")
            },
            style = CraftType.Micro, color = CraftMuted,
        )
    }
}

@Composable
private fun SkillInfoDialog(skill: SkillDef, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var iconBmp by remember(skill.id) { mutableStateOf(ItemIconLoader.peek(skill.icon)) }
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
            .clip(RoundedCornerShape(16.dp))
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
private fun CalcRow(state: CraftAppState, row: AggregateRow, snapshot: InventorySnapshot, intermediate: Boolean = false) {
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
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.item.nameCn,
                    style = CraftType.Row, color = CraftText,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(Modifier.width(6.dp))
                MetaChip("需 ${row.need}", CraftAccent)
                if (row.crystal) {
                    Spacer(Modifier.width(4.dp))
                    MetaChip("水晶", CraftMuted)
                } else if (intermediate) {
                    Spacer(Modifier.width(4.dp))
                    MetaChip("可制作", CraftOk)
                }
            }
            Text(
                buildString {
                    append("背包 $inBag")
                    append(" · 待取回 $retrieve")
                    append(" · 缺 $missing")
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
                "够",
                style = CraftType.Headline, color = CraftOk,
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
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(item.nameCn, style = CraftType.Headline, color = CraftText)
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
                    if (state.inventory.items.isEmpty()) "还没有库存数据：连接游戏插件或载入演示数据后再看。"
                    else "所有已同步的容器里都没有这个道具。",
                    style = CraftType.Callout, color = CraftMuted,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            } else {
                locations.forEach { location ->
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


// ---------------------------------------------------------------- 购物车

/**
 * 购物车页：临时清单。普通模式可改数量/移除；长按进入多选，
 * 多选模式下 左上角删除、右上角＋放入指定清单、其左侧为全选。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CartPage(state: CraftAppState) {
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(emptySet<Int>()) }
    var picking by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        if (!selecting) {
            ScreenHeader(
                title = "购物车",
                subtitle = "临时清单 · " + state.cart.items.size + " 种道具 · 长按道具可多选",
                onBack = { state.pop() },
            )
        } else {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
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
                Spacer(Modifier.weight(1f))
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
                Spacer(Modifier.width(8.dp))
                Text(
                    "＋ 放入清单",
                    style = CraftType.Callout, color = if (selected.isEmpty()) CraftMuted else CraftAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CraftSurface)
                        .clickable(enabled = selected.isNotEmpty()) { picking = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
                Spacer(Modifier.width(8.dp))
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
                val item = state.db.item(entry.itemId)
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
                                Text(
                                    item.nameJp + " · " + item.nameEn,
                                    style = CraftType.Caption, color = CraftMuted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
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
                        Text("购物车是空的", style = CraftType.Row, color = CraftText)
                        Text(
                            "去「配方」页搜索道具，点右侧「购物车」加入",
                            style = CraftType.Callout, color = CraftMuted,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
    if (picking) {
        ListPickerDialog(
            state = state,
            title = "把选中的 " + selected.size + " 种放入指定清单",
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
                            "还没有制作清单，请先到「清单」页新建一个。",
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
