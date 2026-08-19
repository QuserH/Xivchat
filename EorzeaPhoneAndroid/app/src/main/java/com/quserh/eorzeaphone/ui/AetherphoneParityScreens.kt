package com.quserh.eorzeaphone.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.ChatCategory
import com.quserh.eorzeaphone.data.GameActivity
import com.quserh.eorzeaphone.data.GameChatMessage
import com.quserh.eorzeaphone.data.GameJob
import com.quserh.eorzeaphone.data.ItemIconLoader
import com.quserh.eorzeaphone.data.displayPlayerName
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlinx.coroutines.launch

private val AetherLightBackground: Color @Composable get() = MaterialTheme.colorScheme.background
private val AetherLightSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
private val AetherLightText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val AetherLightMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val AetherLightSeparator: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val AetherLightControl: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val AetherPurple = Color(0xFF8669F2)
private val AetherPink = Color(0xFFF46DAA)
private val AetherNavyTop = Color(0xFF12335E)
private val AetherNavyBottom = Color(0xFF061423)
private val ActivityRed = Color(0xFFE83454)
private val ActivityGreen = Color(0xFF72B419)
private val ActivityCyan = Color(0xFF4BAFC4)

private fun ChatFilter.matchesConversation(conversation: ChatConversation): Boolean =
    conversation.lastMessage?.let(::matches) ?: (conversation.category in categories)

@Composable
private fun LightSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = AetherLightText, fontSize = 14.sp),
        modifier = modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(11.dp))
            .background(AetherLightControl),
        decorationBox = { field ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 13.dp),
            ) {
                Text("⌕", color = AetherLightMuted, fontSize = 21.sp, modifier = Modifier.padding(end = 10.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text(placeholder, color = AetherLightMuted, fontSize = 14.sp)
                    field()
                }
            }
        },
    )
}

@Composable
private fun LightFrame(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(AetherLightBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) { content() }
}

@Composable
private fun LightHeader(
    title: String,
    onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 14.dp),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(46.dp)) {
            Text("‹", color = AetherPurple, fontSize = 40.sp, lineHeight = 32.sp, fontWeight = FontWeight.Light)
        }
        Text(
            title,
            color = AetherLightText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(modifier = Modifier.widthIn(min = 46.dp), horizontalArrangement = Arrangement.End, content = trailing)
    }
}

@Composable
fun AetherphoneMessagesScreen(state: PhoneState) {
    var editingTab by remember { mutableStateOf(false) }
    if (editingTab) {
        AetherphoneTabEditor(state) { editingTab = false }
        return
    }
    if (state.editChatTabs) {
        AetherphoneTabEditor(state) { state.editChatTabs = false }
        return
    }
    val conversation = state.conversations.firstOrNull { it.key == state.openConversationKey }
    if (conversation != null) {
        AetherphoneConversationScreen(state, conversation)
        return
    }
    val pager = rememberPagerState(initialPage = if (state.messagesTab) 1 else 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { state.messagesTab = pager.currentPage == 1 }
    LightFrame {
        Column(Modifier.fillMaxSize()) {
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                if (page == 0) AetherphoneConversationList(state) { editingTab = true } else AetherphoneContactsList(state)
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(64.dp).background(AetherLightBackground),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LightNavItem("聊天", R.drawable.app_messages, pager.currentPage == 0, Modifier.weight(1f)) {
                    scope.launch { pager.animateScrollToPage(0) }
                }
                LightNavItem("联系人", R.drawable.app_contacts, pager.currentPage == 1, Modifier.weight(1f)) {
                    scope.launch { pager.animateScrollToPage(1) }
                }
            }
        }
    }
}

@Composable
private fun LightNavItem(label: String, icon: Int, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) AetherPurple else AetherLightMuted, label = "nav-color")
    Column(
        modifier = modifier.fillMaxHeight().clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        ImageGlyph(icon, color, Modifier.size(24.dp))
        Text(label, color = color, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun AetherphoneConversationList(state: PhoneState, editTab: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var filterOpen by remember { mutableStateOf(false) }
    val active = state.chatFilters.firstOrNull { it.id == state.selectedChatFilterId } ?: state.chatFilters.first()
    Column(Modifier.fillMaxSize()) {
        LightHeader("聊天", state::back) {
            Box {
                Text("⋯", color = AetherLightMuted, fontSize = 25.sp, modifier = Modifier.clickable { overflowOpen = true }.padding(horizontal = 10.dp))
                DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                    DropdownMenuItem(text = { Text(if (searching) "关闭搜索" else "搜索消息") }, onClick = {
                        searching = !searching
                        if (!searching) query = ""
                        overflowOpen = false
                    })
                    DropdownMenuItem(text = { Text("新建标签页") }, onClick = { overflowOpen = false; editTab() })
                }
            }
        }
        LightSearchField(query, { query = it }, "搜索消息和联系人", Modifier.padding(horizontal = 42.dp))
        val rows = state.conversations.filter {
            active.matchesConversation(it) && (query.isBlank() || it.title.contains(query, true) || it.lastMessage?.text?.contains(query, true) == true)
        }
        LazyColumn(Modifier.fillMaxSize().padding(top = 18.dp)) {
            item("active-tab") {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { filterOpen = true }.padding(horizontal = 43.dp, vertical = 5.dp),
                    ) {
                        Box(Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFDDE5D3)), contentAlignment = Alignment.Center) {
                            Text(active.label.take(1), color = Color(0xFF82A951), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(active.label, color = AetherLightText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 13.dp))
                        Text("⌄", color = AetherLightMuted, fontSize = 17.sp, modifier = Modifier.padding(start = 7.dp))
                    }
                    DropdownMenu(expanded = filterOpen, onDismissRequest = { filterOpen = false }) {
                        state.chatFilters.forEach { filter ->
                            DropdownMenuItem(text = { Text(filter.label) }, onClick = {
                                state.selectedChatFilterId = filter.id
                                filterOpen = false
                            })
                        }
                    }
                }
            }
            item("new-tab") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = editTab).padding(horizontal = 51.dp, vertical = 10.dp),
                ) {
                    Text("＋", color = AetherPurple, fontSize = 22.sp)
                    Text("新建标签页", color = AetherPurple, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 18.dp))
                }
            }
            if (rows.isNotEmpty()) item("messages-label") {
                Text("消息", color = AetherLightMuted, fontSize = 12.sp, modifier = Modifier.padding(start = 22.dp, top = 10.dp, bottom = 4.dp))
            }
            items(rows, key = { it.key }) { conversation -> LightConversationRow(conversation, state) }
            if (rows.isEmpty()) item("empty") {
                Text(if (state.connected) "暂无消息" else "连接游戏后显示聊天消息", color = AetherLightMuted, fontSize = 14.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 50.dp))
            }
        }
    }
}

@Composable
private fun AetherphoneTabEditor(state: PhoneState, close: () -> Unit) {
    var name by remember { mutableStateOf("新建标签页") }
    var tint by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var saved by remember { mutableStateOf(false) }
    data class ChannelChoice(val key: String, val category: ChatCategory, val label: String, val channels: Set<Int> = emptySet())
    val groups = listOf(
        "社区" to listOf(
            ChannelChoice("fc", ChatCategory.FreeCompany, "部队", setOf(24)),
            ChannelChoice("novice", ChatCategory.Public, "新人频道", setOf(27)),
        ),
        "团体" to listOf(
            ChannelChoice("party", ChatCategory.Party, "小队", setOf(14)),
            ChannelChoice("alliance", ChatCategory.Party, "团队", setOf(15)),
            ChannelChoice("pvp", ChatCategory.Party, "PvP小队", setOf(32)),
        ),
        "通讯贝" to (1..8).map { ChannelChoice("ls$it", ChatCategory.Linkshell, "通讯贝 $it", setOf(15 + it)) },
        "跨服通讯贝" to (1..8).map { ChannelChoice("cwls$it", ChatCategory.Linkshell, "跨服通讯贝 $it", setOf(36 + it)) },
        "本地" to listOf(
            ChannelChoice("tell", ChatCategory.Tell, "悄悄话", setOf(12, 13)),
            ChannelChoice("say", ChatCategory.Public, "说话", setOf(10)),
            ChannelChoice("shout", ChatCategory.Public, "喊话", setOf(11)),
            ChannelChoice("yell", ChatCategory.Public, "呼喊", setOf(30)),
            ChannelChoice("emote", ChatCategory.Emote, "情感动作", setOf(28, 29)),
        ),
        "系统" to listOf(
            ChannelChoice("echo", ChatCategory.System, "默语"),
            ChannelChoice("system", ChatCategory.System, "系统消息"),
        ),
    )
    fun finish() {
        if (!saved && selected.isNotEmpty()) {
            val choices = groups.flatMap { it.second }.filter { it.key in selected }
            val exactChannels = choices.flatMap { it.channels }.toSet()
            val broadCategories = choices.filter { it.channels.isEmpty() }.map { it.category }.toSet()
            state.addChatFilter(name, broadCategories, exactChannels)
            saved = true
        }
        close()
    }
    BackHandler { finish() }
    val colors = listOf(Color(0xFF58ACA4), Color(0xFF86A844), Color(0xFF3E82B9), Color(0xFFB45B7C), Color(0xFFC37136), Color(0xFF796AA2), Color(0xFFBC9438), Color(0xFF45979A))
    LightFrame {
        Column(Modifier.fillMaxSize()) {
            LightHeader("编辑标签页", ::finish)
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 38.dp)) {
                item("name-label") { Text("名称", color = AetherLightText, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)) }
                item("name") {
                    BasicTextField(name, { name = it.take(24) }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = AetherLightText, fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(9.dp)).background(AetherLightSurface).padding(horizontal = 16.dp, vertical = 15.dp))
                }
                item("color-label") { Text("颜色", color = AetherLightText, fontSize = 12.sp, modifier = Modifier.padding(top = 17.dp, bottom = 12.dp)) }
                item("colors") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        colors.forEachIndexed { index, color ->
                            Box(Modifier.size(if (index == tint) 40.dp else 32.dp).clip(CircleShape)
                                .background(if (index == tint) AetherLightText else Color.Transparent).padding(if (index == tint) 4.dp else 0.dp)
                                .clip(CircleShape).background(color).clickable { tint = index })
                        }
                    }
                }
                item("channels") { Text("频道", color = AetherLightText, fontSize = 12.sp, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp)) }
                groups.forEachIndexed { groupIndex, (heading, channels) ->
                    item("group-$groupIndex") { Text(heading, color = AetherLightMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 3.dp)) }
                    itemsIndexed(channels, key = { _, row -> row.key }) { index, row ->
                        val chosen = row.key in selected
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().height(44.dp).clickable {
                                selected = if (chosen) selected - row.key else selected + row.key
                            }) {
                            Box(Modifier.size(9.dp).clip(CircleShape).background(colors[(groupIndex + index + 1) % colors.size]))
                            Text(row.label, color = AetherLightText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 12.dp))
                            if (chosen) Text("✓", color = AetherPurple, fontSize = 18.sp)
                        }
                    }
                }
                item("settings") {
                    Text("标签页设置", color = AetherLightMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                    listOf("回复发送到" to "无法在此频道发送", "布局" to "气泡", "保存记录" to "30 天", "提醒" to "仅提及").forEach { (label, value) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                            Text(label, color = AetherLightText, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(value, color = AetherLightMuted, fontSize = 14.sp)
                            Text("›", color = AetherLightMuted, fontSize = 28.sp, modifier = Modifier.padding(start = 7.dp))
                        }
                    }
                    Text("记录仅保存在这台手机上。", color = AetherLightMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, bottom = 30.dp))
                }
            }
        }
    }
}

@Composable
private fun LightConversationRow(conversation: ChatConversation, state: PhoneState) {
    val last = conversation.lastMessage
    val preview = when {
        last == null -> "暂无消息"
        last.self || last.isFrom(state.profile?.name) -> "${state.profile?.name ?: "我"}：${last.text}"
        else -> "${last.sender.displayPlayerName()}：${last.text}"
    }.replace('\n', ' ')
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { state.openConversation(conversation) }.padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(lightConversationColor(conversation.category)), contentAlignment = Alignment.Center) {
            Text(conversation.title.take(1), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(conversation.title, color = AetherLightText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                conversation.lastTimestamp?.let { Text(lightTalkTime(it), color = AetherLightMuted, fontSize = 11.sp) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text(preview, color = AetherLightMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (!conversation.notify) ImageGlyph(R.drawable.app_notifications, AetherLightMuted.copy(alpha = .62f), Modifier.size(14.dp).padding(start = 2.dp))
                if (conversation.unread > 0) {
                    Box(
                        Modifier.padding(start = 7.dp).height(21.dp).widthIn(min = 21.dp).clip(CircleShape).background(Color(0xFFE5485D)),
                        contentAlignment = Alignment.Center,
                    ) { Text(if (conversation.unread > 99) "99+" else conversation.unread.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 5.dp)) }
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().padding(start = 82.dp).height(1.dp).background(AetherLightSeparator))
}

@Composable
private fun AetherphoneContactsList(state: PhoneState) {
    var query by remember { mutableStateOf("") }
    var friendsOnly by remember { mutableStateOf(true) }
    val shown = state.friends.filter { it.name.contains(query, true) || it.world.contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        LightHeader("联系人", state::back) {
            Text("⟳", color = AetherPurple, fontSize = 27.sp, modifier = Modifier.clickable { state.refreshFriends() }.padding(horizontal = 8.dp))
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item("search") {
                LightSearchField(query, { query = it }, "搜索")
                LightSegment(
                    first = "好友",
                    second = "所有人",
                    firstSelected = friendsOnly,
                    onSelect = { friendsOnly = it },
                    modifier = Modifier.padding(top = 14.dp, bottom = 18.dp),
                )
            }
            val online = shown.filter { it.online }
            val offline = shown.filter { !it.online }
            if (online.isNotEmpty()) {
                item("online-label") { Text("在线 · ${online.size}", color = AetherLightMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp)) }
                item("online-card") { LightContactCard(online, state) }
                item("online-gap") { Spacer(Modifier.height(18.dp)) }
            }
            if (offline.isNotEmpty()) {
                item("offline-label") { Text("离线 · ${offline.size}", color = AetherLightMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp)) }
                item("offline-card") { LightContactCard(offline, state) }
            }
            if (shown.isEmpty()) {
                item("empty") { Text(if (state.connected) "暂无联系人" else "连接游戏后读取好友列表", color = AetherLightMuted, modifier = Modifier.padding(top = 50.dp).fillMaxWidth(), textAlign = TextAlign.Center) }
            }
            item("end") { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LightContactCard(friends: List<PhoneFriend>, state: PhoneState) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(AetherLightSurface)) {
        friends.forEachIndexed { index, friend ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clickable { state.openFriend(friend) }.padding(horizontal = 20.dp, vertical = 11.dp),
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(AetherLightControl), contentAlignment = Alignment.Center) {
                    Text(friend.name.take(1), color = if (friend.online) AetherPurple else Color(0xFFA7A7AE), fontSize = 16.sp)
                }
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(friend.name, color = if (friend.online) AetherLightText else AetherLightMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(friend.world.ifBlank { "未知服务器" }, color = AetherLightMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
                if (friend.online) Box(Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF35B865)))
            }
            if (index < friends.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 20.dp).height(1.dp).background(AetherLightSeparator))
        }
    }
}

@Composable
private fun LightSegment(first: String, second: String, firstSelected: Boolean, onSelect: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().height(40.dp).clip(CircleShape).background(AetherLightControl)) {
        listOf(first to true, second to false).forEach { (label, value) ->
            val selected = firstSelected == value
            Text(
                label,
                color = if (selected) AetherLightText else AetherLightMuted,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(11.dp))
                    .background(if (selected) AetherPurple else Color.Transparent)
                    .clickable { onSelect(value) }.padding(top = 11.dp),
            )
        }
    }
}

@Composable
fun AetherphoneContactDetailScreen(state: PhoneState) {
    val friend = state.selectedFriend
    LightFrame {
        Column(Modifier.fillMaxSize()) {
            LightHeader(friend?.name ?: "联系人", state::back) {
                Text("⋯", color = AetherLightMuted, fontSize = 25.sp, modifier = Modifier.padding(horizontal = 8.dp))
            }
            if (friend == null) return@Column
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
                Box(Modifier.size(94.dp).clip(CircleShape).background(AetherLightControl), contentAlignment = Alignment.Center) {
                    Text(friend.name.take(1), color = AetherPurple, fontSize = 32.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(friend.name, color = AetherLightText, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
                Text(listOf(friend.world, if (friend.online) "在线" else "离线").filter { it.isNotBlank() }.joinToString(" · "), color = AetherLightMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                Row(Modifier.fillMaxWidth().padding(top = 28.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    LightContactAction("发消息", R.drawable.app_messages, Color(0xFF49C97A)) { state.startTell(friend) }
                    LightContactAction("铭牌", R.drawable.app_contacts, Color(0xFF6A83E9)) { state.friendAction(friend, 1) }
                    LightContactAction("小队", R.drawable.app_muster, AetherPurple) { state.friendAction(friend, 2) }
                    LightContactAction("参观", R.drawable.app_housing, Color(0xFFF2A142)) { state.friendAction(friend, 3) }
                }
                Column(Modifier.fillMaxWidth().padding(top = 28.dp).clip(RoundedCornerShape(10.dp)).background(AetherLightSurface)) {
                    LightInfoRow("服务器", friend.world)
                    LightInfoRow("部队", friend.freeCompany.ifBlank { "未读取" })
                    LightInfoRow("位置", friend.location.ifBlank { "离线" })
                    LightInfoRow("职业", friend.job.ifBlank { "未读取" }, last = true)
                }
            }
        }
    }
}

@Composable
private fun LightContactAction(label: String, icon: Int, color: Color, action: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = action).padding(3.dp)) {
        Box(Modifier.size(54.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
            ImageGlyph(icon, Color.White, Modifier.size(27.dp))
        }
        Text(label, color = AetherLightMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp))
    }
}

@Composable
private fun LightInfoRow(label: String, value: String, last: Boolean = false) {
    Row(Modifier.fillMaxWidth().height(53.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = AetherLightMuted, fontSize = 13.sp, modifier = Modifier.width(72.dp))
        Text(value, color = AetherLightText, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (!last) Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(AetherLightSeparator))
}

@Composable
private fun AetherphoneConversationScreen(state: PhoneState, conversation: ChatConversation) {
    var channelMenu by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    val focus = LocalFocusManager.current
    val send = {
        if (state.connected && state.chatDraft.isNotBlank()) {
            state.sendToConversation(conversation, state.chatDraft)
            focus.clearFocus()
        }
    }
    LightFrame {
        Column(Modifier.fillMaxSize()) {
            LightHeader(conversation.title, state::back) {
                Text("⌕", color = if (searching) AetherPurple else AetherLightMuted, fontSize = 25.sp, modifier = Modifier.clickable { searching = !searching }.padding(horizontal = 7.dp))
                Box {
                    Text("⋯", color = AetherLightMuted, fontSize = 25.sp, modifier = Modifier.clickable { overflowOpen = true }.padding(horizontal = 10.dp))
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(text = { Text("标记为已读") }, onClick = {
                            conversation.unread = 0
                            overflowOpen = false
                        })
                        DropdownMenuItem(text = { Text("编辑标签页") }, onClick = {
                            state.editChatTabs = true
                            state.closeConversation()
                            overflowOpen = false
                        })
                        DropdownMenuItem(text = { Text("置顶") }, onClick = {
                            state.toggleConversationPin(conversation)
                            overflowOpen = false
                        })
                        DropdownMenuItem(text = { Text("清除记录", color = Color(0xFFD64555)) }, onClick = {
                            state.clearConversation(conversation)
                            overflowOpen = false
                        })
                    }
                }
            }
            AnimatedVisibility(
                visible = searching,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    LightSearchField(search, { search = it }, "搜索", Modifier.weight(1f))
                    Text("⌃", color = AetherLightMuted, fontSize = 19.sp, modifier = Modifier.padding(start = 6.dp))
                    Text("⌄", color = AetherLightMuted, fontSize = 19.sp, modifier = Modifier.padding(start = 5.dp))
                }
            }
            val visible = if (search.isBlank()) conversation.messages else conversation.messages.filter { it.text.contains(search, true) || it.sender.contains(search, true) }
            if (visible.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Text("暂无消息", color = AetherLightMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 44.dp))
                }
            } else {
                val listState = rememberLazyListState()
                LaunchedEffect(conversation.key, visible.size, search) {
                    if (search.isBlank() && visible.isNotEmpty()) listState.scrollToItem(visible.lastIndex)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    itemsIndexed(visible, key = { index, message -> "$index-${message.timestamp}-${message.sender}-${message.text}" }) { index, message ->
                        val self = message.self || message.isFrom(state.profile?.name)
                        val author = if (self) state.profile?.name.orEmpty().ifBlank { "我" } else message.sender.ifBlank { conversation.title }
                        LightChatBubble(author, message, self, shouldShowLightSender(visible, index, state.profile?.name), state.chatWrapChars)
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Box {
                    Box(
                        modifier = Modifier.width(58.dp).height(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFE4F0))
                            .clickable(enabled = conversation.category != ChatCategory.Tell) { channelMenu = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (conversation.category == ChatCategory.Tell) "私语" else state.currentChannelName,
                            color = AetherPink,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 5.dp),
                        )
                    }
                    DropdownMenu(expanded = channelMenu, onDismissRequest = { channelMenu = false }) {
                        outputChannels.forEach { channel ->
                            DropdownMenuItem(text = { Text(channel.label) }, onClick = { state.changeChannel(channel); channelMenu = false })
                        }
                    }
                }
                BasicTextField(
                    value = state.chatDraft,
                    onValueChange = { state.chatDraft = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = AetherLightText, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.weight(1f).padding(start = 8.dp).height(42.dp).clip(RoundedCornerShape(11.dp)).background(AetherLightSurface),
                    decorationBox = { field ->
                        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                if (state.chatDraft.isBlank()) Text("消息内容", color = AetherLightMuted, fontSize = 13.sp)
                                field()
                            }
                        }
                    },
                )
                Box(
                    Modifier.padding(start = 7.dp).size(38.dp).clip(CircleShape)
                        .background(if (state.connected && state.chatDraft.isNotBlank()) AetherPurple else AetherLightControl)
                        .clickable(enabled = state.connected && state.chatDraft.isNotBlank()) { send() },
                    contentAlignment = Alignment.Center,
                ) { Text("↑", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun LightChatBubble(author: String, message: GameChatMessage, self: Boolean, showSender: Boolean, wrapChars: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (self) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (self) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 310.dp)) {
            if (showSender) Text(author, color = AetherLightMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 3.dp))
            Column(
                Modifier.clip(RoundedCornerShape(12.dp)).background(if (self) AetherPurple else AetherLightSurface)
                    .animateContentSize().padding(start = 11.dp, end = 11.dp, top = 8.dp, bottom = 5.dp),
            ) {
                val body = wrapLightText(message.text, wrapChars)
                val timeColor = if (self) Color.White.copy(alpha = .72f) else AetherLightMuted
                if (body.contains('\n')) {
                    Text(body, color = if (self) Color.White else AetherLightText, fontSize = 14.sp, lineHeight = 19.sp)
                    Text(lightClock(message.timestamp), color = timeColor, fontSize = 9.sp, lineHeight = 10.sp,
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(body, color = if (self) Color.White else AetherLightText, fontSize = 14.sp, lineHeight = 19.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        Text(lightClock(message.timestamp), color = timeColor, fontSize = 9.sp, lineHeight = 10.sp,
                            modifier = Modifier.padding(start = 8.dp, bottom = 1.dp))
                    }
                }
            }
        }
    }
}

private fun shouldShowLightSender(messages: List<GameChatMessage>, index: Int, selfName: String?): Boolean {
    if (index == 0) return true
    fun key(message: GameChatMessage) = if (message.self || message.isFrom(selfName)) "self" else message.sender.trim()
    val currentKey = key(messages[index])
    if (key(messages[index - 1]) != currentKey) return true
    var groupStart = index - 1
    while (groupStart > 0 && key(messages[groupStart - 1]) == currentKey && messages[groupStart].timestamp - messages[groupStart - 1].timestamp <= 60_000L) {
        groupStart--
    }
    return messages[index].timestamp - messages[groupStart].timestamp > 60_000L
}

private fun wrapLightText(value: String, limit: Int): String {
    if (limit <= 0) return value
    return value.lineSequence().flatMap { line -> line.chunked(limit).asSequence() }.joinToString("\n")
}

@Composable
fun AetherphoneNotesScreen(state: PhoneState) {
    var tab by remember { mutableStateOf(0) }
    var editingNote by remember { mutableStateOf<LocalNote?>(null) }
    var creatingNote by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<LocalReminder?>(null) }
    var creatingReminder by remember { mutableStateOf(false) }
    val editorOpen = editingNote != null || creatingNote || editingReminder != null || creatingReminder
    BackHandler(enabled = editorOpen) {
        editingNote = null; creatingNote = false; editingReminder = null; creatingReminder = false
    }
    when {
        editingNote != null || creatingNote -> NoteEditor(state, editingNote) { editingNote = null; creatingNote = false }
        editingReminder != null || creatingReminder -> ReminderEditor(state, editingReminder) { editingReminder = null; creatingReminder = false }
        else -> LightFrame {
            Column(Modifier.fillMaxSize()) {
                LightHeader("备忘录", state::back) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFE1E1E6))
                            .clickable { if (tab == 0) creatingNote = true else creatingReminder = true },
                        contentAlignment = Alignment.Center,
                    ) { Text("+", color = AetherLightText, fontSize = 23.sp) }
                }
                LightSegment("备忘录", "提醒事项", tab == 0, { tab = if (it) 0 else 1 }, Modifier.padding(horizontal = 42.dp, vertical = 4.dp))
                if (tab == 0) {
                    if (state.notes.isEmpty()) {
                        Text("还没有备忘录。点按 + 写一条。", color = AetherLightMuted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 94.dp))
                    } else {
                        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
                            items(state.notes, key = { it.id }) { note ->
                                val lines = note.body.lines()
                                Column(Modifier.fillMaxWidth().clickable { editingNote = note }.padding(vertical = 13.dp)) {
                                    Text(lines.firstOrNull().orEmpty().ifBlank { "无标题" }, color = AetherLightText, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${noteDate(note.updatedAt)}  ${lines.drop(1).joinToString(" ")}", color = AetherLightMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                                }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(AetherLightSeparator))
                            }
                        }
                    }
                } else {
                    if (state.reminders.isEmpty()) {
                        Text("还没有提醒事项。点按 + 添加一条。", color = AetherLightMuted, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 94.dp))
                    } else {
                        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
                            items(state.reminders.sortedBy { it.done }, key = { it.id }) { reminder ->
                                Row(Modifier.fillMaxWidth().clickable { editingReminder = reminder }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        Modifier.size(25.dp).clip(CircleShape).background(if (reminder.done) AetherPurple else Color.Transparent)
                                            .clickable { state.toggleReminder(reminder.id) },
                                        contentAlignment = Alignment.Center,
                                    ) { Text(if (reminder.done) "✓" else "○", color = if (reminder.done) Color.White else AetherLightMuted, fontSize = 18.sp) }
                                    Column(Modifier.weight(1f).padding(start = 13.dp)) {
                                        Text(reminder.title, color = if (reminder.done) AetherLightMuted else AetherLightText, fontSize = 15.sp)
                                        reminder.dueAt?.let { Text(reminderDate(it), color = if (it < System.currentTimeMillis() && !reminder.done) Color(0xFFD64A57) else AetherLightMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp)) }
                                    }
                                }
                                Box(Modifier.fillMaxWidth().height(1.dp).background(AetherLightSeparator))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteEditor(state: PhoneState, note: LocalNote?, close: () -> Unit) {
    var body by remember(note?.id) { mutableStateOf(note?.body.orEmpty()) }
    LightFrame {
        Column(Modifier.fillMaxSize()) {
            LightHeader("备忘录", onBack = { state.upsertNote(note?.id, body); close() }) {
                if (note != null) Text("删除", color = Color(0xFFD64A57), fontSize = 13.sp, modifier = Modifier.clickable { state.deleteNote(note.id); close() }.padding(10.dp))
            }
            OutlinedTextField(
                value = body,
                onValueChange = { body = it.take(8000) },
                placeholder = { Text("开始输入…", color = AetherLightMuted) },
                modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

@Composable
private fun ReminderEditor(state: PhoneState, reminder: LocalReminder?, close: () -> Unit) {
    val context = LocalContext.current
    var title by remember(reminder?.id) { mutableStateOf(reminder?.title.orEmpty()) }
    var dueAt by remember(reminder?.id) { mutableStateOf(reminder?.dueAt) }
    var remind by remember(reminder?.id) { mutableStateOf(reminder?.dueAt != null) }
    val calendar = remember(dueAt) { Calendar.getInstance().apply { dueAt?.let { timeInMillis = it } } }
    LightFrame {
        Column(Modifier.fillMaxSize()) {
            LightHeader(if (reminder == null) "新提醒事项" else "编辑提醒事项", onBack = close) {
                if (reminder != null) Text("删除", color = Color(0xFFD64A57), fontSize = 13.sp, modifier = Modifier.clickable { state.deleteReminder(reminder.id); close() }.padding(10.dp))
            }
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(title, { title = it.take(120) }, placeholder = { Text("提醒内容") }, singleLine = true, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth().height(58.dp).clip(RoundedCornerShape(10.dp)).background(AetherLightSurface).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("提醒我", color = AetherLightText, modifier = Modifier.weight(1f))
                    Switch(checked = remind, onCheckedChange = { remind = it; if (it && dueAt == null) dueAt = System.currentTimeMillis() + 3_600_000L })
                }
                if (remind) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TextButton(
                            onClick = {
                                DatePickerDialog(context, { _, year, month, day ->
                                    calendar.set(year, month, day); dueAt = calendar.timeInMillis
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f).background(AetherLightSurface, RoundedCornerShape(10.dp)),
                        ) { Text(dueAt?.let(::reminderDay) ?: "选择日期", color = AetherPurple) }
                        TextButton(
                            onClick = {
                                TimePickerDialog(context, { _, hour, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute); dueAt = calendar.timeInMillis
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                            },
                            modifier = Modifier.weight(1f).background(AetherLightSurface, RoundedCornerShape(10.dp)),
                        ) { Text(dueAt?.let(::lightClock) ?: "选择时间", color = AetherPurple) }
                    }
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "保存",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(CircleShape).background(if (title.isBlank()) Color(0x668669F2) else AetherPurple)
                        .clickable(enabled = title.isNotBlank()) { state.upsertReminder(reminder?.id, title, if (remind) dueAt else null); close() }.padding(vertical = 15.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AetherphoneJobsScreen(state: PhoneState) {
    val categories = listOf("坦克", "治疗", "近战", "远程物理", "远程魔法", "生产", "采集", "战斗")
    DarkDataFrame(AetherNavyTop, AetherNavyBottom) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(70.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = state::back) { Text("‹", color = Color(0xFF78A7FF), fontSize = 37.sp) }
                Text("职业", color = Color(0xFFE7EEF9), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = .1f)), contentAlignment = Alignment.Center) { Text("▣", color = Color.White, fontSize = 13.sp) }
                Spacer(Modifier.width(9.dp))
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color.White.copy(alpha = .1f)), contentAlignment = Alignment.Center) { Text("◉", color = Color.White, fontSize = 13.sp) }
            }
            if (state.jobs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (state.connected) "正在读取职业套装" else "连接游戏后显示职业套装", color = Color(0xFF8FA1BA)) }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
                    categories.forEach { category ->
                        val jobs = state.jobs.filter { it.category == category }
                        if (jobs.isNotEmpty()) {
                            item("category-$category") {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp, bottom = 9.dp)) {
                                    Box(Modifier.width(4.dp).height(20.dp).background(Color(0xFF68A0FF), RoundedCornerShape(2.dp)))
                                    Text(category, color = Color(0xFFE7EEF9), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
                                }
                            }
                            item("jobs-$category") {
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFF142844)).animateContentSize()) {
                                    jobs.forEachIndexed { index, job ->
                                        AetherphoneJobRow(job, state)
                                        if (index < jobs.lastIndex) Box(Modifier.fillMaxWidth().padding(start = 20.dp).height(1.dp).background(Color.White.copy(alpha = .07f)))
                                    }
                                }
                            }
                        }
                    }
                    item("bottom") { Spacer(Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun AetherphoneJobRow(job: GameJob, state: PhoneState) {
    var menu by remember { mutableStateOf(false) }
    val activeBackground by animateColorAsState(if (job.active) Color.White.copy(alpha = .09f) else Color.Transparent, label = "job-active")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().background(activeBackground)
            .clickable(enabled = job.gearsetId >= 0 && !job.active) { state.equipGearset(job) }
            .padding(horizontal = 20.dp, vertical = 11.dp),
    ) {
        RemoteGameIcon(job.iconId, job.abbreviation, Modifier.size(50.dp))
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(job.name, color = Color(0xFFE6EDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (job.active) Text("当前", color = Color(0xFF7FA8FF), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp))
            }
            Text(
                if (job.itemLevel >= 0) "${job.abbreviation} · Lv${job.level} · iLv${job.itemLevel}" else "${job.abbreviation} · Lv${job.level}",
                color = Color(0xFF91A2BB), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp),
            )
        }
        Box {
            Text("⋯", color = Color(0xFF99ABC2), fontSize = 20.sp, modifier = Modifier.clickable { menu = true }.padding(10.dp))
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = { Text(if (job.active) "当前已装备" else if (job.gearsetId >= 0) "装备套装" else "没有装备套装") },
                    enabled = job.gearsetId >= 0 && !job.active,
                    onClick = { state.equipGearset(job); menu = false },
                )
            }
        }
    }
}

@Composable
private fun RemoteGameIcon(iconId: Int, fallback: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(iconId) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(iconId) { bitmap = ItemIconLoader.load(context.applicationContext, iconId) }
    Box(modifier.clip(RoundedCornerShape(11.dp)).background(Color(0xFF2D4058)), contentAlignment = Alignment.Center) {
        AnimatedContent(bitmap, label = "game-icon") { image ->
            if (image != null) Image(image.asImageBitmap(), contentDescription = fallback, modifier = Modifier.fillMaxSize())
            else Text(fallback.take(2), color = Color(0xFFD7E2F2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AetherphoneActivityScreen(state: PhoneState) {
    val activity = state.activity
    var history by remember { mutableStateOf(false) }
    DarkDataFrame(Color(0xFF063454), Color(0xFF03111D)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = state::back) { Text("‹", color = Color(0xFF66ADD6), fontSize = 37.sp) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("活跃度", color = Color(0xFFAFC0D2), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Text(listOf(state.profile?.jobName, state.profile?.level?.let { "Lv$it" }, state.profile?.currentWorld).filterNotNull().filter { it.isNotBlank() }.joinToString(" · "), color = Color(0xFF6D879C), fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Text("◉", color = Color(0xFF7897AA), fontSize = 18.sp, modifier = Modifier.width(46.dp), textAlign = TextAlign.Center)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(42.dp).clip(CircleShape).background(Color(0xFF0D3C5B))) {
                listOf("今天" to false, "历史" to true).forEach { (label, value) ->
                    Text(label, color = Color(0xFF91A9BA), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(9.dp)).background(if (history == value) Color(0xFF176BA2) else Color.Transparent).clickable { history = value }.padding(top = 12.dp))
                }
            }
            if (activity == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(if (state.connected) "正在读取活跃度" else "连接游戏后显示活跃度", color = Color(0xFF7891A5)) }
            } else if (history) {
                ActivityHistory(activity)
            } else {
                ActivityToday(activity)
            }
        }
    }
}

@Composable
private fun ActivityToday(activity: GameActivity) {
    val progress = (activity.todayExpGained / 1_000_000f).coerceIn(0f, 1f)
    val adventure = (activity.todayDutiesCompleted / 3f).coerceIn(0f, 1f)
    val fortune = (activity.todayGilEarned / 50_000f).coerceIn(0f, 1f)
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        item("rings") {
            ActivityRings(progress, adventure, fortune, Modifier.fillMaxWidth().height(260.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActivityLegend("进度", "${(progress * 100).toInt()}%", ActivityRed)
                ActivityLegend("冒险", "${activity.todayDutiesCompleted} / 3", ActivityGreen)
                ActivityLegend("财富", "${compactNumber(activity.todayGilEarned)} / 50K", ActivityCyan)
            }
            Text("今天", color = Color(0xFF88A2B7), fontSize = 12.sp, modifier = Modifier.padding(top = 24.dp, bottom = 10.dp))
        }
        item("card") {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xAA07131D)).padding(vertical = 6.dp)) {
                ActivityMetric("⚡", "经验值", "+${compactNumber(activity.todayExpGained)}", progress, ActivityRed, "目前的 ${(progress * 100).toInt()}%")
                ActivityMetric("▥", "副本", "${activity.todayDutiesCompleted} / 3", adventure, ActivityGreen)
                ActivityMetric("◉", "获得金币", "+${compactNumber(activity.todayGilEarned)}", fortune, ActivityCyan)
                ActivityMetric("◷", "游戏时长", activityDuration(activity.todayPlaySeconds), null, Color(0xFF6C98C7))
            }
        }
        item("bottom") { Spacer(Modifier.height(22.dp)) }
    }
}

@Composable
private fun ActivityHistory(activity: GameActivity) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("本次登录", color = Color(0xFF88A2B7), fontSize = 12.sp)
            Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xAA07131D)).padding(vertical = 6.dp)) {
                ActivityMetric("⚡", "经验值", "+${compactNumber(activity.sessionExpGained)}", null, ActivityRed)
                ActivityMetric("▥", "副本", activity.sessionDutiesCompleted.toString(), null, ActivityGreen)
                ActivityMetric("◉", "获得金币", "+${compactNumber(activity.sessionGilEarned)}", null, ActivityCyan)
                ActivityMetric("◷", "游戏时长", activityDuration(activity.sessionPlaySeconds), null, Color(0xFF6C98C7))
            }
        }
        item {
            Text("收藏进度", color = Color(0xFF88A2B7), fontSize = 12.sp)
            Column(Modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xAA07131D))) {
                ActivitySimpleRow("坐骑", "${activity.mountsOwned} / ${activity.mountsTotal}")
                ActivitySimpleRow("宠物", "${activity.minionsOwned} / ${activity.minionsTotal}")
                ActivitySimpleRow("雇员", activity.retainerCount.toString())
            }
        }
    }
}

@Composable
private fun ActivityRings(progress: Float, adventure: Float, fortune: Float, modifier: Modifier) {
    val p by animateFloatAsState(progress, spring(dampingRatio = .9f, stiffness = 75f), label = "ring-progress")
    val a by animateFloatAsState(adventure, spring(dampingRatio = .9f, stiffness = 75f), label = "ring-adventure")
    val f by animateFloatAsState(fortune, spring(dampingRatio = .9f, stiffness = 75f), label = "ring-fortune")
    Canvas(modifier) {
        val center = center
        val radii = listOf(size.minDimension * .30f, size.minDimension * .23f, size.minDimension * .16f)
        val values = listOf(p to ActivityRed, a to ActivityGreen, f to ActivityCyan)
        radii.zip(values).forEach { (radius, value) ->
            drawArc(Color(0xFF17394E), -90f, 360f, false, topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius), size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
            if (value.first > 0f) drawArc(value.second, -90f, value.first * 360f, false, topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius), size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2), style = Stroke(13.dp.toPx(), cap = StrokeCap.Round))
        }
    }
}

@Composable
private fun ActivityLegend(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(13.dp).clip(CircleShape).background(color))
            Text(label, color = Color(0xFF7F98AB), fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp))
        }
        Text(value, color = Color(0xFFAAB8C5), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun ActivityMetric(icon: String, label: String, value: String, fraction: Float?, color: Color, detail: String? = null) {
    Row(Modifier.fillMaxWidth().height(82.dp).padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(7.dp)).background(color.copy(alpha = .85f)), contentAlignment = Alignment.Center) { Text(icon, color = Color.White, fontSize = 19.sp) }
        Column(Modifier.weight(1f).padding(start = 15.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Text(label, color = Color(0xFFA9B8C5), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            if (detail != null) Text(detail, color = Color(0xFF70899B), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            if (fraction != null) LinearProgressIndicator(progress = { fraction }, color = color, trackColor = Color(0xFF152B38), modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(5.dp).clip(CircleShape))
        }
    }
}

@Composable
private fun ActivitySimpleRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 17.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color(0xFFA9B8C5), modifier = Modifier.weight(1f))
        Text(value, color = Color(0xFF7FA7C4), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DarkDataFrame(top: Color, bottom: Color, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(top, bottom)))
            .windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars),
    ) { content() }
}

private fun lightConversationColor(category: ChatCategory): Color = when (category) {
    ChatCategory.Public -> Color(0xFF52A877)
    ChatCategory.Party -> Color(0xFF6688E8)
    ChatCategory.Tell -> Color(0xFFE88B45)
    ChatCategory.Linkshell -> Color(0xFF9870CF)
    ChatCategory.FreeCompany -> Color(0xFFC6834D)
    ChatCategory.Emote -> Color(0xFFE6719A)
    ChatCategory.System -> Color(0xFF7A8493)
}

private fun lightTalkTime(timestamp: Long): String {
    val time = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
    return if (time.toLocalDate() == LocalDate.now()) time.format(DateTimeFormatter.ofPattern("HH:mm")) else time.format(DateTimeFormatter.ofPattern("M/d"))
}

private fun lightClock(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
private fun noteDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M月d日"))
private fun reminderDate(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
private fun reminderDay(timestamp: Long): String = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy年M月d日"))

private fun compactNumber(value: Long): String = when {
    value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000f).replace(".0M", "M")
    value >= 1_000 -> String.format("%.1fK", value / 1_000f).replace(".0K", "K")
    else -> value.toString()
}

private fun activityDuration(seconds: Long): String {
    val minutes = seconds / 60
    return if (minutes >= 60) "${minutes / 60}小时${minutes % 60}分" else "${minutes}分钟"
}
