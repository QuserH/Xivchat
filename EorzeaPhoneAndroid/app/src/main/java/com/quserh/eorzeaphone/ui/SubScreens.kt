package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.ChatCategory
import com.quserh.eorzeaphone.data.GameInventoryItem
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneBackground
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.round

@Composable
fun ScreenFrame(background: Color = PhoneBackground, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        PhoneStatusBar()
        content()
    }
}

@Composable
fun ScreenHeader(title: String, state: PhoneState, trailing: (@Composable () -> Unit)? = null, onBack: (() -> Unit)? = null, showBack: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        if (showBack) TextButton(onClick = (onBack ?: state::back), contentPadding = ButtonDefaults.TextButtonContentPadding) {
            Text("‹", color = PhoneAccent, fontSize = 38.sp, lineHeight = 30.sp)
        } else Spacer(Modifier.width(12.dp))
        Text(title, color = PhoneText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun SettingsScreen(state: PhoneState) {
    val profileName = state.profile?.name?.takeIf { state.connected && it.isNotBlank() }
    val profileSubtitle = if (profileName != null) {
        listOf(state.profile?.homeWorld, state.profile?.currentWorld).filterNotNull().filter { it.isNotBlank() }.distinct().joinToString(" · ")
    } else if (state.connected) {
        "正在读取角色资料"
    } else {
        "连接游戏后显示角色资料"
    }
    ScreenFrame {
        ScreenHeader("设置", state, showBack = false)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PhoneSurface).padding(18.dp),
                ) {
                    Box(Modifier.size(68.dp).clip(CircleShape).background(PhoneAccent), contentAlignment = Alignment.Center) {
                        Text(profileName?.take(1) ?: "人", color = Color.White, fontSize = 20.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(profileName ?: if (state.connected) "已连接终端" else "未连接终端", color = PhoneText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text(profileSubtitle, color = PhoneMuted, fontSize = 13.sp)
                    }
                    Text("›", color = PhoneMuted, fontSize = 32.sp)
                }
            }
            item {
                Button(
                    onClick = { if (state.connected) state.disconnect() else state.connect() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAE62DA)),
                    shape = RoundedCornerShape(17.dp),
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                ) { Text(if (state.connected) "断开游戏连接" else "连接 XIVChat / 艾欧泽亚终端", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = state.host, onValueChange = { state.host = it }, label = { Text("游戏电脑 IP") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = state.port, onValueChange = { state.port = it.filter(Char::isDigit).take(5) }, label = { Text("端口") }, singleLine = true, modifier = Modifier.width(110.dp))
                }
                if (state.statusMessage.isNotBlank()) Text(state.statusMessage, color = if (state.connected) PhoneGreen else PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
            }
            item {
                SettingsGroup {
                    ToggleRow("免打扰", false, R.drawable.app_notifications)
                    ToggleRow("锁定位置", false, R.drawable.app_settings)
                    ToggleRow("待机时滑动手机", true, R.drawable.app_shortcuts)
                    ToggleRow("集体动作时显示", true, R.drawable.app_camera)
                }
            }
            item {
                SettingsGroup {
                    LinkRow("通用", R.drawable.app_settings)
                    LinkRow("外观", R.drawable.app_photos)
                    LinkRow("声音", R.drawable.app_music)
                    LinkRow("通知", R.drawable.app_notifications)
                    LinkRow("语音通话", R.drawable.app_message, "关闭")
                }
            }
            item {
                Text("数据来源：${state.serverLabel}", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneSurface)) { content() }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, icon: Int) {
    var value by remember { mutableStateOf(checked) }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 18.dp)) {
        ImageGlyph(icon, PhoneText)
        Text(label, color = PhoneText, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 14.dp))
        Switch(checked = value, onCheckedChange = { value = it })
    }
}

@Composable
private fun LinkRow(label: String, icon: Int, value: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(60.dp).clickable { }) {
        ImageGlyph(icon, PhoneText, Modifier.padding(start = 18.dp))
        Text(label, color = PhoneText, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 14.dp))
        if (value != null) Text(value, color = PhoneMuted, fontSize = 13.sp)
        Text("›", color = PhoneMuted, fontSize = 28.sp, modifier = Modifier.padding(horizontal = 15.dp))
    }
}

@Composable
fun ContactsScreen(state: PhoneState) {
    var query by remember { mutableStateOf("") }
    var friendsTab by remember { mutableStateOf(true) }
    val filtered = state.friends.filter { it.name.contains(query, ignoreCase = true) }
    ScreenFrame {
        ScreenHeader("联系人", state, trailing = { Text("⟳", color = PhoneAccent, fontSize = 27.sp, modifier = Modifier.clickable { state.refreshFriends() }) }, showBack = false)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("搜索", color = PhoneMuted) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp),
            shape = RoundedCornerShape(12.dp),
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 44.dp, vertical = 14.dp).clip(CircleShape).background(Color(0xFF424148))) {
            listOf("好友" to true, "所有人" to false).forEach { (label, value) ->
                Text(label, color = if (friendsTab == value) Color.White else PhoneMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f).clip(CircleShape).background(if (friendsTab == value) PhoneAccent else Color.Transparent).clickable { friendsTab = value }.padding(vertical = 9.dp))
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val online = filtered.filter { it.online }
            val offline = filtered.filter { !it.online }
            if (online.isNotEmpty()) {
                item("online") { ContactSection("在线", online, state) }
            }
            if (offline.isNotEmpty()) {
                item("offline") { ContactSection("离线", offline, state) }
            }
            if (filtered.isEmpty()) item { Text(if (state.connected) "正在读取好友列表" else "尚未读取好友列表", color = PhoneMuted, modifier = Modifier.padding(20.dp)) }
        }
        MessagesBottomNav(contacts = true, state = state)
    }
}

@Composable
private fun ContactSection(title: String, friends: List<PhoneFriend>, state: PhoneState) {
    Column(Modifier.fillMaxWidth()) {
        SectionLabel("$title · ${friends.size}")
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneSurface)) {
            friends.forEachIndexed { index, friend ->
                ContactRow(friend) { state.openFriend(friend) }
                if (index < friends.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 73.dp).height(1.dp).background(Color.White.copy(alpha = 0.08f)))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = PhoneMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 7.dp))
}

@Composable
private fun ContactRow(friend: PhoneFriend, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 11.dp)) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(if (friend.online) PhoneAccent else Color(0xFF30303B)), contentAlignment = Alignment.Center) {
            Text(friend.name.take(1), color = Color.White, fontSize = 17.sp)
        }
        Column(Modifier.weight(1f).padding(start = 13.dp)) {
            Text(friend.name, color = if (friend.online) PhoneText else PhoneMuted, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(if (friend.location.isBlank()) friend.world else "${friend.world} · ${friend.location}", color = PhoneMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (friend.job.isNotBlank()) Text(friend.job, color = PhoneAccent, fontSize = 11.sp)
        }
        Text("›", color = PhoneMuted, fontSize = 25.sp)
    }
}

@Composable
private fun MessagesBottomNav(contacts: Boolean, state: PhoneState) {
    Row(
        Modifier.fillMaxWidth().height(62.dp).background(PhoneBackground).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessagesNavItem("聊天", R.drawable.app_messages, !contacts, Modifier.weight(1f)) { state.showMessagesTab(false) }
        MessagesNavItem("联系人", R.drawable.app_contacts, contacts, Modifier.weight(1f)) { state.showMessagesTab(true) }
    }
}

@Composable
private fun MessagesNavItem(label: String, icon: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color = if (selected) PhoneAccent else PhoneMuted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().clickable(onClick = onClick),
    ) {
        ImageGlyph(icon, color, Modifier.size(22.dp))
        Text(label, color = color, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
fun ContactDetailScreen(state: PhoneState) {
    val friend = state.selectedFriend
    ScreenFrame {
        ScreenHeader("联系人信息", state)
        if (friend == null) return@ScreenFrame
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(vertical = 24.dp, horizontal = 14.dp),
            ) {
                Box(Modifier.size(94.dp).clip(CircleShape).background(if (friend.online) PhoneAccent else Color(0xFF484650)), contentAlignment = Alignment.Center) {
                    Text(friend.name.take(1), color = Color.White, fontSize = 30.sp)
                }
                Text(friend.name, color = PhoneText, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                val status = listOf(friend.world, friend.freeCompany, if (friend.online) "在线" else "离线").filter { it.isNotBlank() }.joinToString(" · ")
                Text(status, color = PhoneMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                if (friend.location.isNotBlank()) Text(friend.location, color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                ContactAction("发消息", R.drawable.app_messages, Color(0xFF45C979), true) { state.startTell(friend) }
                ContactAction("铭牌", R.drawable.app_contacts, Color(0xFF6684ED), friend.contentId != 0L) { state.friendAction(friend, 1) }
                ContactAction("小队", R.drawable.app_muster, PhoneAccent, friend.online && friend.contentId != 0L) { state.friendAction(friend, 2) }
                ContactAction("参观", R.drawable.app_housing, Color(0xFFFFA228), friend.contentId != 0L) { state.friendAction(friend, 3) }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).clickable(enabled = friend.contentId != 0L) { state.friendAction(friend, 4) }.padding(16.dp),
            ) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFF77798A)), contentAlignment = Alignment.Center) { Text("i", color = Color.White, fontWeight = FontWeight.Bold) }
                Text("查看玩家信息", color = if (friend.contentId != 0L) PhoneText else PhoneMuted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 14.dp))
                Text("›", color = PhoneMuted, fontSize = 28.sp)
            }
            if (state.statusMessage.isNotBlank()) Text(state.statusMessage, color = PhoneMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ContactAction(label: String, icon: Int, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(enabled = enabled, onClick = onClick).padding(3.dp)) {
        Box(Modifier.size(58.dp).clip(CircleShape).background(if (enabled) tint else Color(0xFF34343A)), contentAlignment = Alignment.Center) {
            ImageGlyph(icon, if (enabled) Color.White else PhoneMuted, Modifier.size(28.dp))
        }
        Text(label, color = if (enabled) PhoneMuted else Color(0xFF55555D), fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
fun InventoryScreen(state: PhoneState) {
    var query by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var showTutorial by remember { mutableStateOf(false) }
    val groups = listOf("bags" to "背包", "armoury" to "兵装库", "crystals" to "水晶", "saddle" to "陆行鸟鞍囊", "equipped" to "当前装备")
    val selectedTypes = inventoryTypesForGroup(selectedGroup ?: "bags")
    val filtered = state.inventory.filter { (selectedGroup == null || it.container in selectedTypes) && (query.isBlank() || it.name.contains(query, true)) }
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF3B1607), Color(0xFF140805))))) {
    ScreenFrame(background = Color.Transparent) {
        ScreenHeader(if (selectedGroup == null) "物品栏" else groups.firstOrNull { it.first == selectedGroup }?.second ?: "物品栏", state,
            trailing = { Row(verticalAlignment = Alignment.CenterVertically) { Text("${state.inventory.size} 件", color = PhoneMuted, fontSize = 12.sp); Text("  ?", color = PhoneAccent, fontSize = 18.sp, modifier = Modifier.clickable { showTutorial = true }) } },
            onBack = if (selectedGroup == null) null else ({ selectedGroup = null; query = "" }),
            showBack = selectedGroup != null)
        OutlinedTextField(query, { query = it }, placeholder = { Text("搜索物品", color = PhoneMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), shape = RoundedCornerShape(12.dp))
        if (state.inventory.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (state.connected) "等待背包快照…" else "请先连接游戏插件", color = PhoneMuted)
                Text("背包数据通过加密端口同步", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
        } else if (selectedGroup == null && query.isBlank()) {
            InventoryHub(state) { selectedGroup = it }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                if (query.isNotBlank()) {
                    items(filtered, key = { "${it.container}-${it.slot}-${it.itemId}" }) { item -> InventorySearchRow(item) }
                } else {
                    selectedTypes.forEach { type ->
                        val itemMap = state.inventory.filter { it.container == type }.associateBy { it.slot.toInt() }
                        val reportedSize = state.inventoryContainers.firstOrNull { it.type == type }?.size ?: 0
                        val size = reportedSize.coerceAtLeast(defaultContainerSize(type)).coerceAtLeast((itemMap.keys.maxOrNull() ?: -1) + 1)
                        if (size > 0) {
                            item(key = "header-$type") { SectionLabel(inventoryContainerName(type)) }
                            items((0 until size).chunked(5), key = { row -> "$type-${row.first()}" }) { slots ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    slots.forEach { slot -> InventorySlotCell(itemMap[slot], Modifier.weight(1f)) }
                                    repeat(5 - slots.size) { Spacer(Modifier.weight(1f).aspectRatio(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
    if (showTutorial) {
        AlertDialog(onDismissRequest = { showTutorial = false }, title = { Text("物品栏教程") }, text = {
            Text("顶部显示当前 Gil 和携带物品总数。\n\n手上有：背包、兵装库、水晶和已装备；点击一项进入与游戏一致的固定格子。\n\n存放于别处：在游戏中打开一次雇员或部队仓库后，插件才能缓存它们。搜索会跨当前页面查找物品。", color = PhoneText)
        }, confirmButton = { TextButton(onClick = { showTutorial = false }) { Text("知道了") } })
    }
}

@Composable
private fun InventoryHub(state: PhoneState, open: (String) -> Unit) {
    val localTypes = inventoryTypesForGroup("bags") + inventoryTypesForGroup("armoury") + inventoryTypesForGroup("crystals") + inventoryTypesForGroup("saddle") + inventoryTypesForGroup("equipped")
    val total = state.inventory.filter { it.container in localTypes }.sumOf { it.quantity }
    val rows = listOf(
        Triple("bags", "兵装库与背包", R.drawable.app_inventory) to Color(0xFFC68731),
        Triple("armoury", "兵装库", R.drawable.app_muster) to Color(0xFF4F8DE8),
        Triple("crystals", "水晶", R.drawable.app_shortcuts) to Color(0xFF9963E2),
        Triple("equipped", "已装备", R.drawable.app_jobs) to Color(0xFF48B87D),
    )
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFF402313)).padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Text("Gil", color = Color(0xFFFFD36A), fontSize = 12.sp); Text(state.wallet?.gil?.toString() ?: "--", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("金币", color = PhoneMuted, fontSize = 10.sp) }
                Divider(Modifier.height(54.dp).width(1.dp), color = Color(0x554F321F))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Text("携带物品", color = Color.White, fontSize = 12.sp); Text(if (state.inventory.isEmpty()) "--" else total.toString(), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("总数量", color = PhoneMuted, fontSize = 10.sp) }
            }
        }
        item { SectionLabel("手上有") }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneSurface)) {
                rows.forEachIndexed { index, (row, color) ->
                    val (id, label, icon) = row
                    val count = state.inventory.count { it.container in inventoryTypesForGroup(id) }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { open(id) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Box(Modifier.size(48.dp).clip(RoundedCornerShape(9.dp)).background(color), contentAlignment = Alignment.Center) { ImageGlyph(icon, Color.White, Modifier.size(26.dp)) }
                        Text(label, color = PhoneText, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 14.dp))
                        Text(count.toString(), color = color, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 5.dp))
                        Text("›", color = PhoneMuted, fontSize = 26.sp, modifier = Modifier.padding(start = 9.dp))
                    }
                    if (index < rows.lastIndex) Divider(Modifier.padding(start = 78.dp), color = Color(0x22333333))
                }
            }
        }
        item { SectionLabel("存放于别处") }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneSurface)) {
                listOf("雇员" to "在侍从铃处打开一次雇员，将其内容保存到这里", "部队仓库" to "在游戏中打开部队仓库后同步").forEachIndexed { index, (title, subtitle) ->
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) { Text(title, color = PhoneText, fontSize = 15.sp); Text(subtitle, color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) }
                    if (index == 0) Divider(Modifier.padding(start = 16.dp), color = Color(0x22333333))
                }
            }
        }
    }
}

private fun inventoryTypesForGroup(group: String): List<Long> = when (group) {
    "bags" -> listOf(0, 1, 2, 3)
    "armoury" -> listOf(3500, 3200, 3201, 3202, 3203, 3204, 3205, 3206, 3207, 3208, 3209, 3300, 3400)
    "crystals" -> listOf(2001)
    "saddle" -> listOf(4000, 4001, 4100, 4101)
    "equipped" -> listOf(1000)
    else -> emptyList()
}

private fun inventoryContainerName(type: Long): String = when (type) {
    0L -> "物品栏第 1 页"; 1L -> "物品栏第 2 页"; 2L -> "物品栏第 3 页"; 3L -> "物品栏第 4 页"
    1000L -> "当前装备"; 2001L -> "水晶"
    3500L -> "兵装库 · 主手"; 3200L -> "兵装库 · 副手"; 3201L -> "兵装库 · 头部"; 3202L -> "兵装库 · 身体"
    3203L -> "兵装库 · 手部"; 3204L -> "兵装库 · 腰部"; 3205L -> "兵装库 · 腿部"; 3206L -> "兵装库 · 脚部"
    3207L -> "兵装库 · 耳饰"; 3208L -> "兵装库 · 项链"; 3209L -> "兵装库 · 手镯"; 3300L -> "兵装库 · 戒指"; 3400L -> "兵装库 · 灵魂水晶"
    4000L -> "陆行鸟鞍囊第 1 页"; 4001L -> "陆行鸟鞍囊第 2 页"; 4100L -> "高级鞍囊第 1 页"; 4101L -> "高级鞍囊第 2 页"
    else -> "容器 $type"
}

private fun defaultContainerSize(type: Long): Int = when (type) {
    in 0L..3L -> 35
    2001L -> 18
    1000L -> 14
    3300L, 3500L -> 50
    3400L -> 30
    4000L, 4001L, 4100L, 4101L -> 35
    else -> 35
}

@Composable
private fun InventorySlotCell(item: GameInventoryItem?, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f).clip(RoundedCornerShape(5.dp)).background(if (item?.hq == true) Color(0xFF67522B) else PhoneSurface), contentAlignment = Alignment.Center) {
        if (item != null) {
            Text(item.name.take(3), color = PhoneText, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(4.dp))
            if (item.quantity > 1) Text(item.quantity.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.BottomEnd).background(Color(0xB0000000)).padding(horizontal = 2.dp))
            if (item.hq) Text("HQ", color = Color(0xFFFFD36A), fontSize = 7.sp, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp))
        }
    }
}

@Composable
private fun InventorySearchRow(item: GameInventoryItem) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp)).background(PhoneSurface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        InventorySlotCell(item, Modifier.size(44.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(item.name, color = PhoneText, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${inventoryContainerName(item.container)} · 第 ${item.slot + 1} 格", color = PhoneMuted, fontSize = 11.sp)
        }
        Text("×${item.quantity}", color = PhoneText, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ProfileScreen(state: PhoneState) {
    ScreenFrame {
        ScreenHeader("角色", state)
        val profile = state.profile
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PhoneSurface).padding(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(profile?.name ?: "等待角色资料", color = PhoneText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text(profile?.currentWorld?.let { "${profile.homeWorld} · 当前世界 $it" } ?: "角色资料由游戏插件提供", color = PhoneMuted)
                    Text(profile?.location ?: "", color = PhoneAccent, fontSize = 13.sp)
                    if (profile != null && profile.jobName.isNotBlank()) Text("${profile.jobName} · Lv.${profile.level}", color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text("职业与状态", color = PhoneText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(if (state.connected) "打开职业或角色页面后，插件会继续补充当前职业、部队和状态。" else "连接游戏后可查看当前角色。", color = PhoneMuted, fontSize = 14.sp)
        }
    }
}

@Composable
fun SkywatcherScreen(state: PhoneState) {
    val weather = state.weather
    ScreenFrame {
        ScreenHeader("天气预报", state)
        if (weather == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (state.connected) "等待区域天气…" else "连接游戏后显示天气", color = PhoneMuted)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color(0xFF63758D)).padding(20.dp)) {
                        Text(weather.current, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text(weather.zone, color = Color(0xFFE0E8F1), fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        Text("当前区域 · 游戏内天气", color = Color(0xFFD4DDE7), fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
                item { SectionLabel("未来天气") }
                items(weather.forecast, key = { "${it.eorzeaBell}-${it.name}" }) { window ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(PhoneSurface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (window.minutesFromNow <= 0) "现在" else "${window.minutesFromNow} 分钟后", color = PhoneAccent, fontSize = 12.sp, modifier = Modifier.width(76.dp))
                        Text(window.name, color = PhoneText, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Text("艾欧泽亚时 ${window.eorzeaBell}:00", color = PhoneMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ClockScreen(state: PhoneState) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); now = LocalDateTime.now() } }
    ScreenFrame {
        ScreenHeader("时钟", state)
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")), color = PhoneText, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            Text(now.format(DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日 · EEEE")), color = PhoneMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp))
            Text("艾欧泽亚时间", color = PhoneAccent, fontSize = 16.sp, modifier = Modifier.padding(top = 44.dp))
            Text(eorzeaTime(System.currentTimeMillis()), color = PhoneText, fontSize = 30.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
fun NotesScreen(state: PhoneState) {
    ScreenFrame {
        ScreenHeader("备忘录", state)
        OutlinedTextField(state.noteText, state::saveNote, placeholder = { Text("记录一条备忘", color = PhoneMuted) }, modifier = Modifier.fillMaxSize().padding(18.dp), shape = RoundedCornerShape(12.dp))
    }
}

@Composable
fun TimersScreen(state: PhoneState) {
    var seconds by remember { mutableStateOf(60) }
    var running by remember { mutableStateOf(false) }
    LaunchedEffect(running) { while (running && seconds > 0) { kotlinx.coroutines.delay(1000); seconds-- }; if (seconds == 0) running = false }
    ScreenFrame {
        ScreenHeader("计时器", state)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("%02d:%02d".format(seconds / 60, seconds % 60), color = PhoneText, fontSize = 54.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 12.dp)) { TextButton(onClick = { if (!running) seconds = (seconds - 60).coerceAtLeast(0) }) { Text("−1 分", color = PhoneAccent) }; TextButton(onClick = { if (!running) seconds = (seconds + 60).coerceAtMost(3599) }) { Text("+1 分", color = PhoneAccent) } }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 24.dp)) { Button(onClick = { running = !running }, colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent)) { Text(if (running) "暂停" else "开始") }; TextButton(onClick = { running = false; seconds = 60 }) { Text("重置", color = PhoneAccent) } }
        }
    }
}

private fun calculateSimple(input: String): String {
    val operatorIndex = input.indices.drop(1).lastOrNull { input[it] in charArrayOf('+', '−', '×', '÷') } ?: return input
    val left = input.substring(0, operatorIndex).toDoubleOrNull() ?: return "错误"
    val right = input.substring(operatorIndex + 1).toDoubleOrNull() ?: return "错误"
    val result = when (input[operatorIndex]) { '+' -> left + right; '−' -> left - right; '×' -> left * right; '÷' -> if (right == 0.0) return "错误" else left / right; else -> return "错误" }
    return if (result % 1.0 == 0.0) result.toLong().toString() else result.toString().take(14)
}

private fun eorzeaTime(realMillis: Long): String {
    val eorzeaSeconds = realMillis / 1000L * 144L / 7L
    val daySeconds = ((eorzeaSeconds % 86400L) + 86400L) % 86400L
    return "%02d:%02d".format(daySeconds / 3600L, daySeconds % 3600L / 60L)
}

@Composable
fun CalendarScreen(state: PhoneState) {
    val date = remember { LocalDate.now() }
    ScreenFrame {
        ScreenHeader("日历", state)
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text(date.format(DateTimeFormatter.ofPattern("yyyy 年 MM 月")), color = PhoneText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")), color = PhoneAccent, modifier = Modifier.padding(top = 5.dp))
            Text("游戏日历", color = PhoneText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 28.dp))
            Text(if (state.connected) "可继续接收游戏活动与重置时间。" else "连接游戏后显示活动和日常重置。", color = PhoneMuted, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun ChatScreen(state: PhoneState) {
    var channelMenu by remember { mutableStateOf(false) }
    var filterEditor by remember { mutableStateOf(false) }
    var filterName by remember { mutableStateOf("") }
    var filterCategories by remember { mutableStateOf(setOf<ChatCategory>()) }
    val focusManager = LocalFocusManager.current
    val activeFilter = state.chatFilters.firstOrNull { it.id == state.selectedChatFilterId } ?: state.chatFilters.first()
    val visibleChats = state.chats.filter(activeFilter::matches)
    val sendMessage = {
        if (state.connected && state.chatDraft.isNotBlank()) {
            state.sendChat(state.chatDraft)
            state.chatDraft = ""
            focusManager.clearFocus()
        }
    }
    ScreenFrame {
        ScreenHeader("聊天", state, showBack = false)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            state.chatFilters.forEach { filter ->
                Text(filter.label, color = if (filter.id == state.selectedChatFilterId) Color.White else PhoneMuted, fontSize = 12.sp,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (filter.id == state.selectedChatFilterId) PhoneAccent else PhoneSurface).clickable { state.selectedChatFilterId = filter.id }.padding(horizontal = 10.dp, vertical = 7.dp))
            }
            Text("＋", color = PhoneAccent, fontSize = 20.sp, modifier = Modifier.clickable { filterEditor = true }.padding(horizontal = 4.dp))
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 3.dp)) {
            TextButton(onClick = { channelMenu = true }, enabled = state.connected) {
                Text("发送频道：${state.currentChannelName} ⌄", color = PhoneText, fontSize = 13.sp)
            }
            DropdownMenu(expanded = channelMenu, onDismissRequest = { channelMenu = false }) {
                outputChannels.forEach { channel ->
                    DropdownMenuItem(text = { Text(channel.label) }, onClick = { state.changeChannel(channel); channelMenu = false })
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.chats.isEmpty()) item { ChatBubble("艾欧泽亚终端", if (state.connected) "已连接游戏，等待聊天消息…" else "等待连接游戏插件…", false) }
            if (state.chats.isNotEmpty() && visibleChats.isEmpty()) item { Text("当前标签没有消息", color = PhoneMuted, modifier = Modifier.padding(12.dp)) }
            items(visibleChats, key = { "${it.timestamp}-${it.sender}-${it.text}" }) { chat -> ChatBubble(chat.sender.ifBlank { "游戏" }, chat.text, chat.isFrom(state.profile?.name)) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            OutlinedTextField(
                value = state.chatDraft,
                onValueChange = { state.chatDraft = it },
                placeholder = { Text("输入消息", color = PhoneMuted) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage() }),
            )
            TextButton(onClick = sendMessage, enabled = state.connected && state.chatDraft.isNotBlank()) { Text("发送", color = PhoneAccent) }
        }
        MessagesBottomNav(contacts = false, state = state)
    }
    if (filterEditor) {
        AlertDialog(
            onDismissRequest = { filterEditor = false },
            title = { Text("自定义聊天标签") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    OutlinedTextField(filterName, { filterName = it }, label = { Text("标签名称") }, singleLine = true)
                    ChatCategory.entries.forEach { category ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = category in filterCategories, onCheckedChange = { checked -> filterCategories = if (checked) filterCategories + category else filterCategories - category })
                            Text(category.label)
                        }
                    }
                    state.chatFilters.filter { it.removable }.forEach { custom ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text("已保存：${custom.label}", modifier = Modifier.weight(1f))
                            TextButton(onClick = { state.removeChatFilter(custom) }) { Text("删除", color = Color(0xFFE56B6F)) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { state.addChatFilter(filterName, filterCategories); filterName = ""; filterCategories = emptySet(); filterEditor = false }) { Text("添加") } },
            dismissButton = { TextButton(onClick = { filterEditor = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ChatBubble(author: String, body: String, self: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (self) Arrangement.End else Arrangement.Start) {
        Column(Modifier.fillMaxWidth(0.82f), horizontalAlignment = if (self) Alignment.End else Alignment.Start) {
            Text(if (self) "我" else author, color = PhoneMuted, fontSize = 11.sp)
            Text(body, color = PhoneText, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).background(if (self) PhoneAccent else PhoneSurface).padding(12.dp))
        }
    }
}

@Composable
fun WalletScreen(state: PhoneState) {
    val wallet = state.wallet
    ScreenFrame {
        ScreenHeader("钱包", state, trailing = { Text(wallet?.entries?.size?.let { "$it 项" } ?: "等待数据", color = PhoneMuted, fontSize = 12.sp) })
        if (wallet == null) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (state.connected) "等待钱包数据…" else "请先连接游戏插件", color = PhoneMuted)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFF5B4826)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Gil", color = Color(0xFFFFD36A), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(wallet.gil.toString().reversed().chunked(3).joinToString(",").reversed(), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                wallet.entries.groupBy { it.section }.forEach { (section, entries) ->
                    item { SectionLabel(section) }
                    items(entries, key = { "${it.itemId}-${it.section}" }) { entry ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(entry.name, color = PhoneText, fontSize = 14.sp); if (entry.cap > 0) Text("上限 ${entry.cap}", color = PhoneMuted, fontSize = 11.sp) }
                            Text(entry.amount.toString(), color = PhoneText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GenericAppScreen(state: PhoneState) {
    val app = state.selectedApp
    ScreenFrame {
        ScreenHeader(app?.label ?: "应用", state)
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (app != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(15.dp)).background(app.color), contentAlignment = Alignment.Center) { ImageGlyph(app.icon, Color.White) }
                    Text(app.label, color = PhoneText, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 14.dp))
                }
            }
            Text(appDescription(app?.id, state), color = PhoneMuted, fontSize = 15.sp)
            if (!state.connected) Button(onClick = { state.open(AppCatalog.dock.last()) }, colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent)) { Text("前往设置连接") }
        }
    }
}

private fun appDescription(id: String?, state: PhoneState): String = when (id) {
    "skywatcher" -> if (state.connected) "天气组件会在插件提供天气数据后更新。" else "连接游戏后显示当前区域天气。"
    "collections" -> "收藏馆：查看坐骑、宠物和成就收藏。"
    "wallet" -> "钱包：同步当前金币和货币余额。"
    "dailies" -> "日常：显示每日和每周重置项目。"
    "housing" -> "房屋：显示当前角色的房屋位置。"
    else -> if (state.connected) "应用已打开，等待游戏数据。" else "请先在设置中连接游戏插件。"
}

@Composable
private fun ImageGlyph(icon: Int, tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(painterResource(icon), contentDescription = null, colorFilter = ColorFilter.tint(tint), modifier = modifier.size(28.dp))
}
