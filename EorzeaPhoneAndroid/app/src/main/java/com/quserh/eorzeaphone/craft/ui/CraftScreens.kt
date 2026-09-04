package com.quserh.eorzeaphone.craft.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
    val aggregation = remember(list.entries.toList(), state.dbReady) {
        state.repo.withHeld(state.repo.aggregate(list.entries), state.inventory)
    }
    val missing = (aggregation.basics + aggregation.intermediates).count { it.held < it.need }

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(
            title = list.name,
            subtitle = if (missing > 0) "还缺 $missing 种材料" else if (list.entries.isEmpty()) "空清单" else "材料齐全，可以做",
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
                item { SectionLabel("基础材料（含水晶）") }
                item {
                    GroupCard {
                        if (aggregation.basics.isEmpty()) {
                            Text("加入道具后自动汇总", style = CraftType.Callout, color = CraftMuted, modifier = Modifier.padding(16.dp))
                        } else {
                            aggregation.basics.forEachIndexed { index, row ->
                                MaterialRow(state, row)
                                if (index < aggregation.basics.size - 1) Hairline()
                            }
                        }
                    }
                }
                if (aggregation.intermediates.isNotEmpty()) {
                    item { SectionLabel("中间制品（需要先做出来的）") }
                    item {
                        GroupCard {
                            aggregation.intermediates.forEachIndexed { index, row ->
                                MaterialRow(state, row, intermediate = true)
                                if (index < aggregation.intermediates.size - 1) Hairline()
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
        Text("×$qty", style = CraftType.Headline, color = CraftAccent)
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

@Composable
fun RecipesTab(state: CraftAppState) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<CraftItem>()) }
    var repairing by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // The db opens async (asset decompress on first launch); re-run the pending
    // query once it is ready so an early keystroke is not stuck on empty results.
    androidx.compose.runtime.LaunchedEffect(state.dbReady) {
        if (state.dbReady && query.isNotBlank()) results = state.db.search(query)
    }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "配方", subtitle = "离线查询任意道具的制作材料")
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
            Column(Modifier.padding(24.dp)) {
                Text("试试搜索：", style = CraftType.Callout, color = CraftMuted)
                listOf("青铜锭", "Hempen Yarn", "巨匠的锤").forEach { sample ->
                    Text(
                        sample,
                        style = CraftType.Row, color = CraftAccent,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                query = sample
                                results = state.db.search(sample)
                            },
                    )
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
                    val recipe = if (state.db.canCraft(item.id)) state.repo.defaultRecipe(item.id) else null
                    Pressable(onClick = { if (recipe != null) state.push(Page.RecipeDetail(item.id)) }) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ItemIcon(item, 44.dp)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(item.nameCn, style = CraftType.Row, color = CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "${item.nameJp} · ${item.nameEn}",
                                    style = CraftType.Caption, color = CraftMuted,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (recipe != null) {
                                MetaChip("${CraftJobs.abbr(recipe.job)} Lv${recipe.craftLv}")
                            } else {
                                MetaChip("不可制作", CraftMuted)
                            }
                        }
                    }
                    Hairline()
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
    val recipe = recipes.getOrNull(recipeIndex) ?: recipes.firstOrNull()
    val bom = remember(recipe, wantQty, state.dbReady) { state.repo.buildBom(itemId, wantQty, recipe) }

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
                listOf(1, 5, 10, 50).forEach { q ->
                    Text(
                        "$q",
                        style = CraftType.Callout,
                        color = if (wantQty == q) Color.White else CraftText,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (wantQty == q) CraftFill else CraftSurface)
                            .clickable { wantQty = q }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
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
            val lists = state.lists.lists
            if (recipe != null && lists.isNotEmpty()) {
                Pressable(
                    onClick = { state.lists.addEntry(lists.first().id, itemId, wantQty) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                ) {
                    Text(
                        "加入「${lists.first().name}」 ×$wantQty",
                        style = CraftType.Row, color = Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CraftFill)
                            .padding(vertical = 12.dp),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
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
        if (state.inventory.items.isEmpty()) {
            Text(
                "游戏在线时，这里会同步角色全部容器：背包、装备、军械库、鞍袋、雇员、部队仓库与房屋仓库。",
                style = CraftType.Callout, color = CraftMuted,
                modifier = Modifier.padding(20.dp),
            )
        } else {
            val snapshot = state.inventory
            val groups = remember(snapshot) { groupTotals(snapshot) }
            SectionLabel("持有概览")
            GroupCard {
                groups.forEachIndexed { index, (group, total, kinds) ->
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(group, style = CraftType.Row, color = CraftText, modifier = Modifier.weight(1f))
                        Text("$kinds 种 · 共 $total", style = CraftType.Callout, color = CraftMuted)
                    }
                    if (index < groups.size - 1) Hairline()
                }
            }
            SectionLabel("容器明细")
            GroupCard {
                snapshot.items
                    .groupBy { locKeyOf(it, snapshot) }
                    .toList()
                    .sortedBy { it.first.first }
                    .take(200)
                    .forEachIndexed { index, (key, rows) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { state.openLocations(rows.first().itemId) }.padding(horizontal = 16.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(key.second, style = CraftType.Row, color = CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(key.first, style = CraftType.Caption, color = CraftMuted)
                            }
                            Text("${rows.sumOf { it.quantity }}", style = CraftType.Callout, color = CraftMuted)
                        }
                        if (index < 199) Hairline()
                    }
            }
        }
        Spacer(Modifier.height(24.dp))
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

    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "手动工作台", subtitle = "材料够就开工；远程驱动待插件端对接")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SectionLabel("制作目标")
            GroupCard {
                val item = targetItemId?.let { state.db.item(it) }
                if (item == null) {
                    Row(
                        Modifier.fillMaxWidth().clickable { showPicker = true }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("选择要制作的道具", style = CraftType.Row, color = CraftAccent, modifier = Modifier.weight(1f))
                        Text("从清单或搜索 ›", style = CraftType.Caption, color = CraftMuted)
                    }
                } else {
                    val recipe = state.repo.defaultRecipe(item.id)
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
            val session = state.session
            if (session == null) {
                Pressable(
                    onClick = {
                        val id = targetItemId ?: return@Pressable
                        val recipe = state.repo.defaultRecipe(id) ?: return@Pressable
                        val item = state.db.item(id) ?: return@Pressable
                        state.session = state.engine.startMock(recipe, item.nameCn)
                    },
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    enabled = targetItemId != null,
                ) {
                    Text(
                        if (targetItemId == null) "先选择制作目标" else "开始制作（模拟）",
                        style = CraftType.Headline,
                        color = if (targetItemId == null) CraftMuted else Color.White,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (targetItemId == null) CraftSurface else CraftFill)
                            .padding(vertical = 14.dp),
                    )
                }
                Text(
                    "说明：本阶段为模拟会话，用于打磨远程操控的手感与界面。插件端实现 CraftStart / CraftSkill / CraftState 推送后，这里将驱动游戏内真实制作（方案见 docs/集成方案.md）。",
                    style = CraftType.Caption, color = CraftMuted,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            } else {
                WorkbenchSession(state, session)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (showPicker) {
        TargetPickerDialog(state, onPick = { targetItemId = it; showPicker = false }, onDismiss = { showPicker = false })
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
private fun WorkbenchSession(state: CraftAppState, session: CraftSession) {
    val craft = session.state.collectAsState().value ?: return
    val log by session.log.collectAsState()
    val cooldown by session.cooldown.collectAsState()

    SectionLabel("制作进度（模拟）")
    GroupCard {
        Column(Modifier.padding(16.dp)) {
            Text(craft.itemName, style = CraftType.Headline, color = CraftText)
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
                if (craft.finished) {
                    MetaChip(
                        if (craft.progress >= craft.progressMax) "完成 · HQ ${craft.hqChance}%" else "失败",
                        CraftDanger,
                    )
                }
            }
        }
    }
    SectionLabel("技能")
    GroupCard {
        val skills = CraftSkills.ALL
        skills.chunked(2).forEachIndexed { index, rowSkills ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowSkills.forEach { skill ->
                    SkillButton(skill, craft, cooldown > 0, Modifier.weight(1f)) { session.useSkill(skill) }
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
    SectionLabel("记录")
    GroupCard {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            log.takeLast(6).reversed().forEach { line ->
                Text(line, style = CraftType.Caption, color = CraftMuted, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
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
private fun SkillButton(skill: SkillDef, craft: CraftState, cooling: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val disabled = cooling || craft.finished || craft.cp < skill.cp
    Column(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CraftSurface)
            .border(0.5.dp, if (disabled) CraftLine else CraftAccent.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable(enabled = !disabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Text(skill.cn, style = CraftType.Callout, color = if (disabled) CraftMuted else CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
