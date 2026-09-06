package com.quserh.eorzeaphone.craft.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.craft.CraftAppState
import com.quserh.eorzeaphone.craft.Page
import com.quserh.eorzeaphone.craft.Tab
import com.quserh.eorzeaphone.craft.data.AggregateRow
import com.quserh.eorzeaphone.craft.data.BomNode
import com.quserh.eorzeaphone.craft.data.CraftItem
import com.quserh.eorzeaphone.craft.data.CraftJobs
import com.quserh.eorzeaphone.craft.data.CraftList
import com.quserh.eorzeaphone.ui.ImageGlyph
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val timeFmt = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

// ---------------------------------------------------------------- shared bits

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
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
                    Text(subtitle, style = CraftType.Caption, color = CraftMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 1.dp))
                }
            }
            actions()
        }
    }
}

@Composable
internal fun MetaChip(text: String, color: Color = CraftAccent) {
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
internal fun InlineField(value: String, onValue: (String) -> Unit, hint: String, modifier: Modifier = Modifier) {
    TextField(
        value = value,
        onValueChange = onValue,
        placeholder = { Text(hint, style = CraftType.Body, color = CraftMuted) },
        textStyle = CraftType.Row,
        singleLine = true,
        shape = RoundedCornerShape(8.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListTab(state: CraftAppState) {
    var showNew by remember { mutableStateOf(false) }
    var manageList by remember { mutableStateOf<CraftList?>(null) }
    var renameList by remember { mutableStateOf<CraftList?>(null) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader(title = "制作清单") {
            Pressable(onClick = { showNew = true }) {
                Row(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CraftFill)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ImageGlyph(R.drawable.ic2_plus, Color.White, Modifier.size(16.dp))
                    Text("新建", style = CraftType.Callout, color = Color.White)
                }
            }
        }
        if (state.lists.lists.isEmpty()) {
            Text(
                "暂无清单",
                style = CraftType.Callout, color = CraftMuted,
                modifier = Modifier.padding(20.dp),
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.lists.lists, key = { it.id }) { list ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(CraftSurface)
                        .combinedClickable(
                            onClick = { state.push(Page.ListDetail(list.id)) },
                            onLongClick = { manageList = list },
                        )
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(CraftFill.copy(alpha = .16f)),
                        contentAlignment = Alignment.Center,
                    ) { ImageGlyph(R.drawable.ic2_edit, CraftAccent, Modifier.size(20.dp)) }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(list.name, style = CraftType.Row, color = CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "${list.entries.size} 种道具 · ${timeFmt.format(Date(list.updatedMs))}",
                            style = CraftType.Caption, color = CraftMuted,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    ImageGlyph(R.drawable.ic2_chevron_right, CraftMuted, Modifier.padding(start = 12.dp).size(18.dp))
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
                InlineField(name, { name = it }, "清单名称")
            },
            confirmButton = {
                Text(
                    "创建",
                    style = CraftType.Row, color = if (name.isBlank()) CraftMuted else CraftAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = name.isNotBlank()) {
                            state.lists.addList(name.trim())
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
    manageList?.let { list ->
        AlertDialog(
            onDismissRequest = { manageList = null },
            title = { Text(list.name, style = CraftType.Headline) },
            confirmButton = {
                Text(
                    "重命名", style = CraftType.Row, color = CraftAccent,
                    modifier = Modifier.clickable { renameList = list; manageList = null }.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "删除", style = CraftType.Row, color = CraftDanger,
                        modifier = Modifier.clickable { state.lists.removeList(list.id); manageList = null }.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                    Text(
                        "取消", style = CraftType.Row, color = CraftMuted,
                        modifier = Modifier.clickable { manageList = null }.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            },
            containerColor = CraftSurface,
        )
    }
    renameList?.let { list ->
        var name by remember(list.id) { mutableStateOf(list.name) }
        AlertDialog(
            onDismissRequest = { renameList = null },
            title = { Text("重命名清单", style = CraftType.Headline) },
            text = { InlineField(name, { name = it }, "清单名称") },
            confirmButton = {
                Text(
                    "保存", style = CraftType.Row, color = if (name.isBlank()) CraftMuted else CraftAccent,
                    modifier = Modifier.clickable(enabled = name.isNotBlank()) {
                        state.lists.renameList(list.id, name.trim())
                        renameList = null
                    }.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            },
            dismissButton = {
                Text("取消", style = CraftType.Row, color = CraftMuted, modifier = Modifier.clickable { renameList = null }.padding(horizontal = 12.dp, vertical = 6.dp))
            },
            containerColor = CraftSurface,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListDetail(state: CraftAppState, listId: String) {
    val list = state.lists.listById(listId) ?: return
    var selecting by remember(list.id) { mutableStateOf(false) }
    var selected by remember(list.id) { mutableStateOf(emptySet<Int>()) }
    val entries = list.entries.toList()
    val bomRows = remember(entries, state.dbReady) {
        buildList<Pair<BomNode, Int>> {
            fun flatten(nodes: List<BomNode>, depth: Int) {
                nodes.forEach { node ->
                    add(node to depth)
                    flatten(node.children, depth + 1)
                }
            }
            entries.forEach { entry ->
                val recipe = state.repo.defaultRecipe(entry.itemId)
                flatten(state.repo.buildBom(entry.itemId, entry.qty, recipe).firstOrNull()?.children.orEmpty(), 0)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (selecting) {
            ScreenHeader(title = "已选 ${selected.size} 项", onBack = {
                selecting = false
                selected = emptySet()
            }) {
                Text(
                    "删除",
                    style = CraftType.Row,
                    color = if (selected.isEmpty()) CraftMuted else CraftDanger,
                    modifier = Modifier.clickable(enabled = selected.isNotEmpty()) {
                        state.lists.removeEntries(list.id, selected)
                        selected = emptySet()
                        selecting = false
                    }.padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Text(
                    "全选",
                    style = CraftType.Callout,
                    color = CraftAccent,
                    modifier = Modifier.clickable {
                        selected = if (selected.size == entries.size) emptySet() else entries.map { it.itemId }.toSet()
                    }.padding(horizontal = 8.dp, vertical = 6.dp),
                )
                Text(
                    "完成",
                    style = CraftType.Callout,
                    color = CraftMuted,
                    modifier = Modifier.clickable {
                        selecting = false
                        selected = emptySet()
                    }.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        } else {
            ScreenHeader(
                title = list.name,
                subtitle = "${entries.size} 种道具",
                onBack = { state.pop() },
                actions = {
                    if (entries.isNotEmpty()) {
                        Text(
                            "选择",
                            style = CraftType.Callout,
                            color = CraftAccent,
                            modifier = Modifier.clickable { selecting = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                        )
                    }
                },
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (entries.isNotEmpty()) {
                item { SectionLabel("目标道具") }
                items(entries, key = { it.itemId }, contentType = { "entry" }) { entry ->
                    val item = remember(entry.itemId, state.dbReady) { state.db.item(entry.itemId) }
                    if (item != null) {
                        EntryRow(
                            state = state,
                            list = list,
                            item = item,
                            qty = entry.qty,
                            selecting = selecting,
                            selected = entry.itemId in selected,
                            onLongClick = {
                                selecting = true
                                selected = selected + entry.itemId
                            },
                            onSelect = { checked ->
                                selected = if (checked) selected + entry.itemId else selected - entry.itemId
                            },
                        )
                        Hairline()
                    }
                }
                item {
                    SectionLabel("材料计算")
                    Text(
                        "直接材料在上，缩进项为下一级加工材料",
                        style = CraftType.Caption,
                        color = CraftMuted,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                }
                if (bomRows.isEmpty()) {
                    item {
                        Text("暂无可展开的配方材料", style = CraftType.Callout, color = CraftMuted, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    itemsIndexed(bomRows, key = { index, pair -> "bom:$index:${pair.first.item.id}" }) { _, (node, depth) ->
                        BomListRow(state, node, depth)
                        Hairline(startPadding = (52 + depth.coerceAtMost(4) * 16).dp)
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            } else {
                item {
                    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无道具", style = CraftType.Row, color = CraftText)
                        Text(
                            "添加道具",
                            style = CraftType.Row,
                            color = CraftAccent,
                            modifier = Modifier.clickable { state.tab = Tab.RECIPES; state.pop() }.padding(12.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun EntryRow(
    state: CraftAppState,
    list: CraftList,
    item: CraftItem,
    qty: Int,
    selecting: Boolean,
    selected: Boolean,
    onLongClick: () -> Unit,
    onSelect: (Boolean) -> Unit,
) {
    var edit by remember { mutableStateOf(false) }
    val recipe = remember(item.id, state.dbReady) { state.repo.defaultRecipe(item.id) }
    Row(
        Modifier.fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selecting) onSelect(!selected) else edit = true
                },
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecting) {
            Checkbox(checked = selected, onCheckedChange = onSelect)
        }
        ItemIcon(item, 44.dp)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.nameCn, style = CraftType.Row, color = CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (recipe != null) {
                Text("${CraftJobs.abbr(recipe.job)} · Lv${recipe.craftLv}", style = CraftType.Caption, color = CraftMuted)
            }
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
private fun BomListRow(state: CraftAppState, node: BomNode, depth: Int) {
    Row(
        Modifier.fillMaxWidth()
            .clickable { state.openLocations(node.item.id.toLong()) }
            .padding(start = (16 + depth.coerceAtMost(5) * 16).dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemIcon(node.item, 34.dp)
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(node.item.nameCn, style = CraftType.Row, color = CraftText, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (node.crystal) {
                    Spacer(Modifier.width(6.dp))
                    MetaChip("水晶", CraftAccent)
                } else if (node.craftable) {
                    Spacer(Modifier.width(6.dp))
                    MetaChip("可制作", CraftOk)
                }
            }
            Text(
                if (node.craftable) "第 ${depth + 1} 层材料 · ${node.craftCount} 次制作" else "基础材料",
                style = CraftType.Caption,
                color = CraftMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text("需 ${node.totalNeed}", style = CraftType.Callout, color = CraftText)
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
                if (row.held >= row.need) "持有充足" else "还缺 ${row.need - row.held}",
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
