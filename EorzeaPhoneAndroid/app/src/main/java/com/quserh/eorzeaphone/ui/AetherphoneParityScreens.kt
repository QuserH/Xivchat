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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.ChatCategory
import com.quserh.eorzeaphone.data.GameActivity
import com.quserh.eorzeaphone.data.GameChatMessage
import com.quserh.eorzeaphone.data.GameChatChunk
import com.quserh.eorzeaphone.data.GameJob
import com.quserh.eorzeaphone.data.ItemIconLoader
import com.quserh.eorzeaphone.data.displayPlayerName
import com.quserh.eorzeaphone.data.normalizedPlayerName
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
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
    var showLocalScreen by remember { mutableStateOf(false) }
    val openFilter = state.chatFilters.firstOrNull { it.id == state.openChatFilterId }
    val conversation = state.conversations.firstOrNull { it.key == state.openConversationKey }
    val pager = rememberPagerState(initialPage = if (state.messagesTab) 1 else 0, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager.currentPage) { state.messagesTab = pager.currentPage == 1 }
    val route = when {
        showLocalScreen -> "local"
        editingTab -> "new-tab"
        state.editChatTabs -> "edit-tab"
        openFilter != null -> "filter:${openFilter.id}"
        conversation != null -> "chat:${conversation.key}"
        else -> "list"
    }
    AnimatedContent(
        targetState = route,
        transitionSpec = { (fadeIn(tween(200)) + scaleIn(tween(220), initialScale = .98f)).togetherWith(fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 1.01f)) },
        label = "chat-navigation",
    ) { target ->
        when {
            target == "local" -> AetherphoneLocalScreen(state) { showLocalScreen = false }
            target == "new-tab" -> AetherphoneTabEditor(state) { editingTab = false }
            target == "edit-tab" -> AetherphoneTabEditor(state) { state.editChatTabs = false }
            target.startsWith("filter:") && openFilter != null -> AetherphoneFilterConversationScreen(state, openFilter)
            target.startsWith("chat:") && conversation != null -> AetherphoneConversationScreen(state, conversation)
            else -> LightFrame {
                Column(Modifier.fillMaxSize()) {
                    HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                        if (page == 0) AetherphoneConversationList(state, { editingTab = true }, { showLocalScreen = true }) else AetherphoneContactsList(state)
                    }
                    Row(modifier = Modifier.fillMaxWidth().height(64.dp).background(AetherLightBackground), verticalAlignment = Alignment.CenterVertically) {
                        LightNavItem("聊天", R.drawable.app_messages, pager.currentPage == 0, Modifier.weight(1f)) { scope.launch { pager.animateScrollToPage(0) } }
                        LightNavItem("联系人", R.drawable.app_contacts, pager.currentPage == 1, Modifier.weight(1f)) { scope.launch { pager.animateScrollToPage(1) } }
                    }
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AetherphoneConversationList(state: PhoneState, editTab: () -> Unit, onOpenLocal: () -> Unit) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var longPressedTabId by remember { mutableStateOf<String?>(null) }
    var messagesExpanded by remember { mutableStateOf(false) }
    var tabsExpanded by remember { mutableStateOf(true) }
    Column(Modifier.fillMaxSize()) {
        LightHeader("聊天", state::back)
        AnimatedVisibility(visible = searching, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            LightSearchField(query, { query = it }, "搜索消息和联系人", Modifier.padding(horizontal = 42.dp))
        }
        val myName = state.profile?.name.orEmpty().normalizedPlayerName()
        val rows = state.conversations.filter { c ->
            (c.category == ChatCategory.Tell || c.category == ChatCategory.Linkshell || c.category == ChatCategory.FreeCompany) &&
                !state.isConversationHidden(c) &&
                (myName.isEmpty() || c.title.normalizedPlayerName() != myName) &&
                (query.isBlank() || c.title.contains(query, true) || c.lastMessage?.text?.contains(query, true) == true)
        }
        val sortedRows = rows.sortedWith(
            compareByDescending<ChatConversation> { state.isConversationPinned(it) }.thenByDescending { it.lastTimestamp ?: 0L },
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = LocalContentMargin.current.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChatGroupChip("标签", 0, notify = true, active = tabsExpanded) {
                tabsExpanded = true; messagesExpanded = false
            }
            ChatGroupChip("消息", sortedRows.sumOf { it.unread }, notify = true, active = messagesExpanded) {
                messagesExpanded = true; tabsExpanded = false
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(top = 6.dp).padding(horizontal = LocalContentMargin.current.dp)) {
            if (messagesExpanded) {
                item("local-entry") {
                    Row(Modifier.fillMaxWidth().clickable { onOpenLocal() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ImageGlyph(R.drawable.app_messages, AetherLightMuted, Modifier.size(20.dp))
                        Text("本地", color = AetherLightText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 12.dp))
                        Text("说话/喊话/呼喊/情感动作 ›", color = AetherLightMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                items(sortedRows, key = { "message-${it.key}" }) { conversation ->
                    Box(Modifier.animateItem()) { LightConversationRow(conversation, state) }
                }
                if (sortedRows.isEmpty() && !state.connected) item("empty") {
                    Text("连接游戏后显示聊天消息", color = AetherLightMuted, fontSize = 14.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 50.dp))
                }
            } else {
                if (state.chatFilters.isEmpty()) {
                    item("empty-tabs") {
                        Text("还没有标签页，点右上角新建", color = AetherLightMuted, fontSize = 14.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 50.dp))
                    }
                } else {
                    items(state.chatFilters, key = { "tab-${it.id}" }) { filter ->
                        val selected = filter.id == state.selectedChatFilterId
                        val last = state.chats.lastOrNull(filter::matches)
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().animateItem().combinedClickable(
                                onClick = { state.selectedChatFilterId = filter.id; state.openChatFilterId = filter.id },
                                onLongClick = { longPressedTabId = filter.id },
                            ).padding(vertical = 8.dp)) {
                                Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(if (selected) AetherPurple else AetherLightControl), contentAlignment = Alignment.Center) {
                                    Text(filter.label.take(1), color = if (selected) Color.White else AetherLightMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column(Modifier.weight(1f).padding(start = 13.dp)) {
                                    Text(filter.label, color = AetherLightText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    if (last != null) Text(last.text.replace('\n', ' '), color = AetherLightMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp).padding(start = 4.dp)) {
                                    last?.let { Text(lightTalkTime(it.timestamp), color = AetherLightMuted, fontSize = 10.sp, maxLines = 1, softWrap = false) }
                                    ChatTabNotificationIcon(filter.alertPolicy != ChatAlertPolicy.Off) {
                                        state.toggleChatFilterNotifications(filter)
                                    }
                                }
                            }
                            DropdownMenu(expanded = longPressedTabId == filter.id, onDismissRequest = { longPressedTabId = null }) {
                                DropdownMenuItem(text = { Text("置顶") }, onClick = { state.pinChatFilter(filter); longPressedTabId = null })
                                DropdownMenuItem(text = { Text("删除标签页", color = Color(0xFFD64555)) }, onClick = { state.removeChatFilter(filter); longPressedTabId = null })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatTabNotificationIcon(enabled: Boolean, modifier: Modifier = Modifier, toggle: () -> Unit) {
    val slashColor = AetherLightMuted
    Box(modifier.size(25.dp).clickable(onClick = toggle), contentAlignment = Alignment.Center) {
        ImageGlyph(R.drawable.app_notifications, if (enabled) AetherLightMuted else AetherLightMuted.copy(alpha = .55f), Modifier.size(16.dp))
        if (!enabled) Canvas(Modifier.size(18.dp)) {
            drawLine(slashColor, start = androidx.compose.ui.geometry.Offset(2f, size.height - 2f), end = androidx.compose.ui.geometry.Offset(size.width - 2f, 2f), strokeWidth = 2.2f)
        }
    }
}

@Composable
private fun AetherphoneTabEditor(state: PhoneState, close: () -> Unit) {
    val existing = state.chatFilters.firstOrNull { it.id == state.editingChatFilterId }
    var name by remember(existing?.id) { mutableStateOf(existing?.label ?: "新建标签页") }
    var tint by remember(existing?.id) { mutableStateOf(existing?.tintIndex ?: 0) }
    var saved by remember { mutableStateOf(false) }
    var sendChannel by remember(existing?.id) { mutableStateOf(existing?.sendChannel) }
    var layout by remember(existing?.id) { mutableStateOf(existing?.layout ?: ChatLayout.Bubbles) }
    var historyPolicy by remember(existing?.id) { mutableStateOf(existing?.historyPolicy ?: ChatHistoryPolicy.ThirtyDays) }
    var alertPolicy by remember(existing?.id) { mutableStateOf(existing?.alertPolicy ?: ChatAlertPolicy.Mentions) }
    var settingMenu by remember { mutableStateOf<String?>(null) }
    data class ChannelChoice(val key: String, val category: ChatCategory, val label: String, val channels: Set<Int> = emptySet())
    val groups = listOf(
        "社区" to listOf(
            ChannelChoice("fc", ChatCategory.FreeCompany, "部队", setOf(24, 85)),
            ChannelChoice("fcann", ChatCategory.FreeCompany, "部队公告 / 登录", setOf(69, 70)),
            ChannelChoice("novice", ChatCategory.Public, "新人频道", setOf(27, 94)),
            ChannelChoice("novicesys", ChatCategory.System, "新人频道系统消息", setOf(75)),
        ),
        "团体" to listOf(
            ChannelChoice("party", ChatCategory.Party, "小队", setOf(14, 84)),
            ChannelChoice("alliance", ChatCategory.Party, "团队", setOf(15)),
            ChannelChoice("crossparty", ChatCategory.Party, "跨服小队", setOf(32)),
            ChannelChoice("pvp", ChatCategory.Party, "PvP小队", setOf(36)),
            ChannelChoice("pvpann", ChatCategory.Party, "PvP公告 / 登录", setOf(77, 78)),
        ),
        "通讯贝" to (1..8).map { ChannelChoice("ls$it", ChatCategory.Linkshell, "通讯贝 $it", setOf(15 + it, 85 + it)) },
        "跨服通讯贝" to (1..8).map { ChannelChoice("cwls$it", ChatCategory.Linkshell, "跨服通讯贝 $it", setOf(if (it == 1) 37 else 99 + it)) },
        "本地" to listOf(
            ChannelChoice("tell", ChatCategory.Tell, "悄悄话", setOf(12, 13, 80)),
            ChannelChoice("say", ChatCategory.Public, "说话", setOf(10, 81)),
            ChannelChoice("shout", ChatCategory.Public, "喊话", setOf(11, 82)),
            ChannelChoice("yell", ChatCategory.Public, "呼喊", setOf(30, 83)),
            ChannelChoice("emote_std", ChatCategory.Emote, "标准情感动作", setOf(29)),
            ChannelChoice("emote_cus", ChatCategory.Emote, "自定义情感动作", setOf(28)),
        ),
        "战斗" to listOf(
            ChannelChoice("battle_all", ChatCategory.System, "战斗 / 受击 / 治疗", setOf(41, 42, 43, 44, 45)),
            ChannelChoice("battle_buff", ChatCategory.System, "增益效果", setOf(46, 47)),
            ChannelChoice("battle_debuff", ChatCategory.System, "减益效果", setOf(48, 49)),
            ChannelChoice("glamour", ChatCategory.System, "外观同步提醒", setOf(54)),
        ),
        "系统" to listOf(
            ChannelChoice("echo", ChatCategory.System, "默语", setOf(56)),
            ChannelChoice("system", ChatCategory.System, "系统消息", setOf(57, 58, 60)),
            ChannelChoice("alarm", ChatCategory.System, "警报", setOf(55)),
            ChannelChoice("gathering_sys", ChatCategory.System, "采集系统消息", setOf(59)),
            ChannelChoice("recruit", ChatCategory.System, "定时招募公告", setOf(72)),
            ChannelChoice("retainersale", ChatCategory.System, "雇员售出", setOf(71)),
            ChannelChoice("sign", ChatCategory.System, "标识", setOf(73)),
            ChannelChoice("random", ChatCategory.System, "随机数", setOf(74)),
            ChannelChoice("orchestrion", ChatCategory.System, "管弦乐", setOf(76)),
            ChannelChoice("messagebook", ChatCategory.System, "留言本", setOf(79)),
        ),
        "剧情 / 进度" to listOf(
            ChannelChoice("npc", ChatCategory.System, "NPC 对话", setOf(61, 68)),
            ChannelChoice("loot", ChatCategory.System, "战利品 / 掉落", setOf(62, 65)),
            ChannelChoice("progress", ChatCategory.System, "进度", setOf(64)),
            ChannelChoice("crafting", ChatCategory.System, "制作", setOf(66)),
            ChannelChoice("gathering", ChatCategory.System, "采集", setOf(67)),
        ),
    )
    val initialSelection: Set<String> = remember(existing?.id) {
        if (existing == null) {
            emptySet()
        } else {
            groups.flatMap { it.second }.filter { choice ->
                choice.channels.any { it in existing.channels } || (choice.channels.isEmpty() && choice.category in existing.categories)
            }.mapTo(mutableSetOf()) { it.key }.toSet()
        }
    }
    var selected by remember(existing?.id) { mutableStateOf(initialSelection) }
    fun finish() {
        if (!saved && existing != null) {
            val choices = groups.flatMap { it.second }.filter { it.key in selected }
            val exactChannels = choices.flatMap { it.channels }.toSet()
            val broadCategories = choices.filter { it.channels.isEmpty() }.map { it.category }.toSet()
            state.updateChatFilter(existing, name, broadCategories, exactChannels, tint, sendChannel, layout, historyPolicy, alertPolicy)
            saved = true
        } else if (!saved && selected.isNotEmpty()) {
            val choices = groups.flatMap { it.second }.filter { it.key in selected }
            val exactChannels = choices.flatMap { it.channels }.toSet()
            val broadCategories = choices.filter { it.channels.isEmpty() }.map { it.category }.toSet()
            state.addChatFilter(name, broadCategories, exactChannels, tint, sendChannel, layout, historyPolicy, alertPolicy)
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
                    val settings = listOf(
                        "回复发送到" to (sendChannel?.let { id -> outputChannels.firstOrNull { it.id == id }?.label } ?: "当前频道"),
                        "布局" to if (layout == ChatLayout.Bubbles) "气泡" else "紧凑",
                        "保存记录" to when (historyPolicy) { ChatHistoryPolicy.Off -> "关闭"; ChatHistoryPolicy.Session -> "本次会话"; ChatHistoryPolicy.ThirtyDays -> "30 天"; ChatHistoryPolicy.Forever -> "永久" },
                        "提醒" to when (alertPolicy) { ChatAlertPolicy.All -> "全部"; ChatAlertPolicy.Mentions -> "仅提及"; ChatAlertPolicy.Off -> "关闭" },
                    )
                    settings.forEach { (label, value) ->
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(58.dp).clickable { settingMenu = label }) {
                            Text(label, color = AetherLightText, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text(value, color = AetherPurple, fontSize = 14.sp)
                            Text("›", color = AetherLightMuted, fontSize = 28.sp, modifier = Modifier.padding(start = 7.dp))
                            }
                            DropdownMenu(expanded = settingMenu == label, onDismissRequest = { settingMenu = null }) {
                                when (label) {
                                    "回复发送到" -> outputChannels.forEach { channel -> DropdownMenuItem(text = { Text(channel.label) }, onClick = { sendChannel = channel.id; settingMenu = null }) }
                                    "布局" -> listOf(ChatLayout.Bubbles to "气泡", ChatLayout.Compact to "紧凑").forEach { (choice, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { layout = choice; settingMenu = null }) }
                                    "保存记录" -> listOf(ChatHistoryPolicy.Off to "关闭", ChatHistoryPolicy.Session to "本次会话", ChatHistoryPolicy.ThirtyDays to "30 天", ChatHistoryPolicy.Forever to "永久").forEach { (choice, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { historyPolicy = choice; settingMenu = null }) }
                                    "提醒" -> listOf(ChatAlertPolicy.All to "全部", ChatAlertPolicy.Mentions to "仅提及", ChatAlertPolicy.Off to "关闭").forEach { (choice, text) -> DropdownMenuItem(text = { Text(text) }, onClick = { alertPolicy = choice; settingMenu = null }) }
                                }
                            }
                        }
                    }
                    Text("记录仅保存在这台手机上。", color = AetherLightMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, bottom = 30.dp))
                }
                if (existing?.removable == true) item("delete-tab") {
                    Text("删除标签页", color = Color(0xFFD64555), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth().clickable {
                            state.removeChatFilter(existing)
                            state.openChatFilterId = null
                            state.editingChatFilterId = null
                            close()
                        }.padding(vertical = 18.dp))
                }
            }
        }
    }
}

@Composable
private fun ChatGroupChip(label: String, unread: Int, notify: Boolean, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) AetherPurple else AetherLightControl
    val fg = if (active) Color.White else AetherLightText
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 6.dp),
    ) {
        Text(label, color = fg, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
        if (unread > 0) {
            Text(
                if (unread > 99) "99+" else unread.toString(),
                color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp).clip(CircleShape)
                    .background(if (notify) Color(0xFFD93025) else Color(0xFF9AA0A6))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }
}

private fun channelTag(channel: Int): String = when (channel) {
    10, 81 -> "说话"
    11, 82 -> "喊话"
    30, 83 -> "呼喊"
    12, 13, 80 -> "私聊"
    14, 84, 32 -> "小队"
    15 -> "团队"
    24, 85 -> "部队"
    27, 94, 75 -> "新人"
    in 16..23, in 86..93 -> "通讯贝"
    in 37..44, in 101..107 -> "跨服通讯贝"
    29, 28 -> "情感动作"
    else -> ""
}

// Local / group chat lines carry a "[频道]<名字>" or "名字：" prefix baked into the
// text. The app already shows the author separately, so strip it before rendering.
private fun cleanChatText(raw: String, author: String): String {
    var t = raw.trim()
    t = t.replaceFirst(Regex("^\\[[^\\]]*\\]"), "").trim()
    val lt = t.indexOf('<')
    val gt = t.indexOf('>', lt.coerceAtLeast(0))
    if (gt >= 0) {
        t = t.substring(gt + 1).trim()
    } else {
        val colon = t.indexOfAny(charArrayOf('：', ':'))
        if (colon in 1..24) {
            t = t.substring(colon + 1).trim()
        } else if (author.isNotEmpty() && t.startsWith(author)) {
            t = t.removePrefix(author).trimStart('：', ':', ' ', '>', '<').trim()
        }
    }
    return t.ifBlank { raw.trim() }
}

@Composable
private fun AetherphoneLocalScreen(state: PhoneState, onBack: () -> Unit) {
    var filter by remember { mutableIntStateOf(0) }
    var sendChannel by remember { mutableStateOf(1) }
    var channelMenu by remember { mutableStateOf(false) }
    val labels = listOf("全部", "说话", "喊话", "呼喊")
    val msgs = state.chats.filter {
        when {
            filter == 1 -> it.channel == 10
            filter == 2 -> it.channel == 11
            filter == 3 -> it.channel == 30
            else -> it.category == ChatCategory.Public || it.category == ChatCategory.Emote
        }
    }.sortedBy { it.timestamp }
    LightFrame {
        Column(Modifier.fillMaxSize()) {
            LightHeader("本地", onBack)
            Row(Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEachIndexed { i, l ->
                    Text(l, color = if (filter == i) Color.White else AetherLightMuted, fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (filter == i) AetherPurple else AetherLightControl).clickable { filter = i }.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            val listState = rememberLazyListState()
            LaunchedEffect(filter, msgs.size) { if (msgs.isNotEmpty()) listState.scrollToItem(msgs.lastIndex) }
            if (msgs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Text("暂无本地消息", color = AetherLightMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 44.dp))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    itemsIndexed(msgs, key = { index, msg -> "$index-${msg.timestamp}-${msg.channel}-${msg.text}" }) { index, msg ->
                        val self = msg.self || msg.isFrom(state.profile?.name)
                        val author = if (self) {
                            state.profile?.name.orEmpty().ifBlank { "我" }
                        } else {
                            msg.sender.ifBlank { if (msg.category == ChatCategory.Emote) "情感动作" else "本地" }
                        }
                        LightChatBubble(author, msg, self, shouldShowLightSender(msgs, index, state.profile?.name), state.chatWrapChars, fontSizeSp = state.chatFontSize)
                    }
                }
            }
            val focus = LocalFocusManager.current
            val send = {
                val trimmed = state.chatDraft.trim()
                if (state.activeCharacterOnline && trimmed.isNotBlank()) {
                    state.changeChannel(outputChannels.first { it.id == sendChannel })
                    state.sendChat(trimmed)
                    state.chatDraft = ""
                    focus.clearFocus()
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Box {
                    Box(
                        Modifier.width(58.dp).height(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFE4F0))
                            .clickable { channelMenu = true }, contentAlignment = Alignment.Center,
                    ) { Text(outputChannels.firstOrNull { it.id == sendChannel }?.label ?: "说话", color = AetherPink, fontSize = 11.sp, maxLines = 1) }
                    DropdownMenu(expanded = channelMenu, onDismissRequest = { channelMenu = false }) {
                        listOf(1, 4, 5).forEach { id ->
                            DropdownMenuItem(text = { Text(outputChannels.first { it.id == id }.label) }, onClick = { sendChannel = id; channelMenu = false })
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
                        .background(if (state.activeCharacterOnline && state.chatDraft.isNotBlank()) AetherPurple else AetherLightControl)
                        .clickable(enabled = state.activeCharacterOnline && state.chatDraft.isNotBlank()) { send() },
                    contentAlignment = Alignment.Center,
                ) { Text("↑", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LightConversationRow(conversation: ChatConversation, state: PhoneState) {
    var menuOpen by remember { mutableStateOf(false) }
    val last = conversation.lastMessage
    val preview = when {
        last == null -> "暂无消息"
        last.self || last.isFrom(state.profile?.name) -> "${state.profile?.name ?: "我"}：${last.text}"
        else -> "${last.sender.displayPlayerName()}：${last.text}"
    }.replace('\n', ' ')
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = { state.openConversation(conversation) },
                onLongClick = { menuOpen = true },
            ).padding(vertical = 10.dp),
        ) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(lightConversationColor(conversation.category)), contentAlignment = Alignment.Center) {
                Text(conversation.title.take(1), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(conversation.title, color = AetherLightText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (state.isConversationPinned(conversation)) Text("置顶", color = AetherPurple, fontSize = 9.sp, modifier = Modifier.padding(end = 6.dp))
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
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text(if (state.isConversationPinned(conversation)) "取消置顶" else "置顶") }, onClick = {
                state.toggleConversationPin(conversation)
                menuOpen = false
            })
            DropdownMenuItem(text = { Text("移出列表") }, onClick = {
                if (state.isConversationHidden(conversation)) state.unhideConversation(conversation) else state.hideConversation(conversation)
                menuOpen = false
            })
        }
    }
    Spacer(Modifier.height(6.dp))
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
                item("online-label") { Text("在线 · ${formatCount(online.size)}", color = AetherLightMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp)) }
                item("online-card") { LightContactCard(online, state) }
                item("online-gap") { Spacer(Modifier.height(18.dp)) }
            }
            if (offline.isNotEmpty()) {
                item("offline-label") { Text("离线 · ${formatCount(offline.size)}", color = AetherLightMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp)) }
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
        if (state.activeCharacterOnline && state.chatDraft.isNotBlank()) {
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
                            state.editingChatFilterId = state.openChatFilterId
                            state.editChatTabs = true
                            state.closeConversation()
                            overflowOpen = false
                        })
                        DropdownMenuItem(text = { Text(if (state.isConversationPinned(conversation)) "取消置顶" else "置顶") }, onClick = {
                            state.toggleConversationPin(conversation)
                            overflowOpen = false
                        })
                        DropdownMenuItem(text = { Text(if (conversation.notify) "关闭消息提醒" else "开启消息提醒") }, onClick = {
                            state.toggleConversationNotify(conversation)
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
            if (!state.activeCharacterOnline) {
                Text("当前无法使用聊天：角色未登录游戏", color = AetherLightMuted, fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp))
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
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    itemsIndexed(visible, key = { index, message -> "$index-${message.timestamp}-${message.sender}-${message.text}" }) { index, message ->
                        val self = message.self || message.isFrom(state.profile?.name)
                        val tag = if (conversation.key.startsWith("tab:")) channelTag(message.channel) else ""
                        val author = if (self) {
                            val me = state.profile?.name.orEmpty().ifBlank { "我" }
                            if (tag.isNotEmpty()) "[$tag] $me" else me
                        } else if (message.category == ChatCategory.System) {
                            "系统"
                        } else {
                            val base = message.sender.ifBlank { conversation.title }
                            if (tag.isNotEmpty()) "[$tag] $base" else base
                        }
                        Column(Modifier.fillMaxWidth()) {
                            LightChatBubble(author, message, self, shouldShowLightSender(visible, index, state.profile?.name), state.chatWrapChars, conversation.title, state.chatFontSize)
                            if (message.sendState == 2 && conversation.category == ChatCategory.Tell) {
                                Text(
                                    "向${conversation.title.ifBlank { "对方" }}发送悄悄话失败",
                                    color = AetherLightText,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                                )
                            }
                        }
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
                        .background(if (state.activeCharacterOnline && state.chatDraft.isNotBlank()) AetherPurple else AetherLightControl)
                        .clickable(enabled = state.activeCharacterOnline && state.chatDraft.isNotBlank()) { send() },
                    contentAlignment = Alignment.Center,
                ) { Text("↑", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun AetherphoneFilterConversationScreen(state: PhoneState, filter: ChatFilter) {
    val conversation = remember(filter.id, state.chats.size) {
        val category = filter.categories.firstOrNull() ?: ChatCategory.Public
        ChatConversation("tab:${filter.id}", category, filter.label).also { chat ->
            state.chats.filter(filter::matches).forEach(chat::add)
        }
    }
    AetherphoneConversationScreen(state, conversation)
}

@Composable
private fun LightChatBubble(author: String, message: GameChatMessage, self: Boolean, showSender: Boolean, wrapChars: Int, recipientTitle: String = "", fontSizeSp: Int = 14) {
    val fontSp = fontSizeSp.coerceIn(10, 26)
    val fontUnit = fontSp.sp
    val lineUnit = (fontSp + 5).sp
    val timeUnit = (fontSp - 5).coerceAtLeast(9).sp
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (self) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (self) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 310.dp)) {
            if (showSender) Text(author, color = AetherLightMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 3.dp))
            Column(
                Modifier.clip(RoundedCornerShape(12.dp)).background(if (self) AetherPurple else AetherLightSurface)
                    .animateContentSize().padding(start = 11.dp, end = 11.dp, top = 8.dp, bottom = 5.dp),
            ) {
                val cleaned = cleanChatText(message.text, author)
                val renderMsg = if (cleaned != message.text) message.copy(text = cleaned, chunks = emptyList()) else message
                val body = wrapLightText(cleaned, wrapChars)
                val timeColor = if (self) Color.White.copy(alpha = .72f) else AetherLightMuted
                if (body.contains('\n')) {
                    ChatChunkText(renderMsg, body, if (self) Color.White else AetherLightText, fontSize = fontUnit, lineHeight = lineUnit)
                    Text(lightClock(message.timestamp), color = timeColor, fontSize = timeUnit, lineHeight = timeUnit,
                        modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
                } else {
                    Row(verticalAlignment = Alignment.Bottom) {
                        ChatChunkText(renderMsg, body, if (self) Color.White else AetherLightText, fontSize = fontUnit, lineHeight = lineUnit, modifier = Modifier.weight(1f, fill = false))
                        Text(lightClock(message.timestamp), color = timeColor, fontSize = timeUnit, lineHeight = timeUnit,
                            modifier = Modifier.padding(start = 8.dp, bottom = 1.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatChunkText(message: GameChatMessage, fallback: String, color: Color, fontSize: androidx.compose.ui.unit.TextUnit,
                          lineHeight: androidx.compose.ui.unit.TextUnit, modifier: Modifier = Modifier) {
    val chunks = message.chunks.ifEmpty { listOf(GameChatChunk(text = fallback)) }
    val inline = buildMap<String, InlineTextContent> {
        chunks.forEachIndexed { index, chunk ->
            if (chunk.icon != null) put("icon-$index", InlineTextContent(Placeholder(fontSize, lineHeight, PlaceholderVerticalAlign.Center)) {
                ChatInlineIcon(chunk.icon)
            })
        }
    }
    val annotated = buildAnnotatedString {
        chunks.forEachIndexed { index, chunk ->
            if (chunk.icon != null) appendInlineContent("icon-$index", "◆") else {
                val chunkColor = chunk.foreground?.let(::chatChunkColor) ?: color
                withStyle(SpanStyle(color = chunkColor, fontStyle = if (chunk.italic) FontStyle.Italic else null)) { append(chunk.text.orEmpty()) }
            }
        }
    }
    val style = androidx.compose.ui.text.TextStyle(
        color = color, fontSize = fontSize, lineHeight = lineHeight,
        fontStyle = if (message.category == ChatCategory.Emote) FontStyle.Italic else FontStyle.Normal,
        fontFamily = if (message.category == ChatCategory.System) FontFamily.Monospace else FontFamily.Default,
        fontWeight = if (message.category == ChatCategory.Tell) FontWeight.Medium else FontWeight.Normal,
    )
    Text(annotated, inlineContent = inline, style = style, modifier = modifier)
}

private fun chatChunkColor(value: Long): Color {
    val red = ((value shr 24) and 0xFF).toInt()
    val green = ((value shr 16) and 0xFF).toInt()
    val blue = ((value shr 8) and 0xFF).toInt()
    val alpha = (value and 0xFF).toInt()
    return Color(red, green, blue, alpha)
}

@Composable
private fun ChatInlineIcon(index: Int) {
    if (index >= 1000) {
        val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
        var bitmap by remember(index) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(index) { bitmap = ItemIconLoader.load(appContext, index)?.asImageBitmap() }
        val bmp = bitmap
        if (bmp != null) {
            Image(bmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        }
        return
    }
    val rect = fontIconRect(index) ?: return
    val bitmap = ImageBitmap.imageResource(R.drawable.fonticon_ps4)
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        drawImage(bitmap, srcOffset = androidx.compose.ui.unit.IntOffset(rect[0], rect[1]), srcSize = androidx.compose.ui.unit.IntSize(rect[2], rect[3]))
    }
}

private fun fontIconRect(index: Int): IntArray? = when (index) {
    in 1..5 -> intArrayOf((index - 1) * 40, 342, 40, 40)
    in 6..10 -> intArrayOf((index - 6) * 40, 382, 40, 40)
    in 11..15 -> intArrayOf((index - 11) * 40, 422, 40, 40)
    16 -> intArrayOf(120, 542, 40, 40); 17 -> intArrayOf(160, 542, 40, 40)
    18 -> intArrayOf(0, 462, 108, 40); 19 -> intArrayOf(108, 462, 108, 40)
    20 -> intArrayOf(120, 502, 40, 40); 21 -> intArrayOf(0, 502, 56, 40); 22 -> intArrayOf(56, 502, 64, 40); 23 -> intArrayOf(160, 502, 40, 40)
    24 -> intArrayOf(0, 542, 56, 40); 25 -> intArrayOf(56, 542, 64, 40)
    51 -> intArrayOf(248, 342, 40, 40); 52 -> intArrayOf(288, 342, 40, 40); 53 -> intArrayOf(328, 342, 40, 40)
    54 -> intArrayOf(200, 342, 24, 40); 55 -> intArrayOf(224, 342, 24, 40)
    in 56..61 -> intArrayOf(200 + (index - 56) * 40, 382, 40, 40)
    62 -> intArrayOf(320, 382, 40, 40); 63 -> intArrayOf(320, 422, 40, 40)
    in 64..66 -> intArrayOf(368 + (index - 64) * 40, 342, 40, 40)
    67 -> intArrayOf(360, 382, 40, 40); 68 -> intArrayOf(400, 382, 40, 40)
    70 -> intArrayOf(360, 422, 40, 40); 71 -> intArrayOf(400, 422, 40, 40); 72 -> intArrayOf(440, 422, 40, 40); 73 -> intArrayOf(440, 382, 40, 40)
    in 74..80 -> intArrayOf(216 + (index - 74) * 40, 462, 40, 40)
    in 81..87 -> intArrayOf(200 + (index - 81) * 40, 502, 40, 40)
    in 88..94 -> intArrayOf(200 + (index - 88) * 40, 542, 40, 40)
    in 95..100 -> intArrayOf((index - 95) * 40, 582, 40, 40)
    else -> null
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
    return wrapChatTextByUnits(value, limit)
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
                Text("保存", color = AetherPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { state.upsertNote(note?.id, body); close() }.padding(horizontal = 10.dp))
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

@Composable
private fun lightConversationColor(category: ChatCategory): Color {
    val darkTheme = MaterialTheme.colorScheme.background.luminance() < .5f
    val lightHue = when (category) {
        ChatCategory.Public -> Color(0xFF7ED9A7)
        ChatCategory.Party -> Color(0xFF8FB0FF)
        ChatCategory.Tell -> Color(0xFFFFB06E)
        ChatCategory.Linkshell -> Color(0xFFBE9BE8)
        ChatCategory.FreeCompany -> Color(0xFFE8A879)
        ChatCategory.Emote -> Color(0xFFFF9CC4)
        ChatCategory.System -> Color(0xFFAAB6C4)
    }
    val darkHue = when (category) {
        ChatCategory.Public -> Color(0xFF2E7D50)
        ChatCategory.Party -> Color(0xFF3E63C4)
        ChatCategory.Tell -> Color(0xFFC06A24)
        ChatCategory.Linkshell -> Color(0xFF6E4E9E)
        ChatCategory.FreeCompany -> Color(0xFF965C2C)
        ChatCategory.Emote -> Color(0xFFB4486E)
        ChatCategory.System -> Color(0xFF4E5A68)
    }
    return if (darkTheme) lightHue else darkHue
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
