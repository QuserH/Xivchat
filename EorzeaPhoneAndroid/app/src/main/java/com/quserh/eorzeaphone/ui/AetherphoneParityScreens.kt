package com.quserh.eorzeaphone.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.horizontalScroll
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private val AetherLightBackground: Color @Composable get() = MaterialTheme.colorScheme.background
private val AetherLightSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
private val AetherLightText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
private val AetherLightMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
private val AetherLightSeparator: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
private val AetherLightControl: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
private val AetherPurple = Color(0xFF8669F2)
private val EmoteChatColor = Color(0xFFBEFFF1) // 情感动作文字颜色（游戏内 190,255,241）
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
        textStyle = androidx.compose.ui.text.TextStyle(color = AetherLightText, fontSize = 14.sp, lineHeight = 20.sp),
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
    titleOffsetY: Dp = 0.dp,
    trailing: @Composable RowScope.() -> Unit = {},
    titleIcon: (@Composable () -> Unit)? = null,
) {
    val headerMargin = LocalContentMargin.current
    val sidePad = (headerMargin.coerceAtLeast(2) - 2).dp
    // 标题垂直位置与 ScreenHeader（捕鱼等窗口）保持一致：内容高度 + 上下 12dp，返回键尺寸一致
    Box(Modifier.fillMaxWidth().padding(horizontal = sidePad, vertical = 12.dp)) {
        Box(Modifier.align(Alignment.CenterStart).width(46.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                "‹",
                color = AetherPurple,
                fontSize = 38.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick = onBack).padding(horizontal = 4.dp, vertical = 6.dp),
            )
        }
        // 标题限定在返回键与右侧按钮之间，避免窄屏/大边距时与右侧按钮重叠
        Box(Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 54.dp), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = AetherLightText,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.offset(y = titleOffsetY),
                )
                if (titleIcon != null) {
                    Box(Modifier.padding(start = 6.dp)) { titleIcon() }
                }
            }
        }
        Row(Modifier.align(Alignment.CenterEnd).widthIn(min = 46.dp), horizontalArrangement = Arrangement.End, content = trailing)
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
    LaunchedEffect(state.openLocalRequest) {
        if (state.openLocalRequest > 0) showLocalScreen = true
    }
    LaunchedEffect(state.openConversationKey) {
        if (state.openConversationKey != null) showLocalScreen = false
    }
    LaunchedEffect(pager.currentPage) { state.messagesTab = pager.currentPage == 1 }
    val route = when {
        editingTab -> "new-tab"
        state.editChatTabs -> "edit-tab"
        conversation != null -> "chat:${conversation.key}"
        openFilter != null -> "filter:${openFilter.id}"
        showLocalScreen -> "local"
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
    var renameTarget by remember { mutableStateOf<ChatConversation?>(null) }
    var renameText by remember { mutableStateOf("") }
    var iconKeyTarget by remember { mutableStateOf<String?>(null) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val target = iconKeyTarget
        iconKeyTarget = null
        if (uri != null && target != null) {
            val path = state.savePickedIcon(target, uri)
            if (path != null) state.setConversationIcon(target, path)
        }
    }
    val messagesExpanded = state.chatListTab == "messages"
    val tabsExpanded = state.chatListTab != "messages"
    var showDefaultTabDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        LightHeader("聊天", state::back, trailing = {
            Box {
                Text("⋯", color = AetherLightMuted, fontSize = 25.sp, modifier = Modifier.padding(horizontal = 8.dp).clickable { overflowOpen = true })
                DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                    DropdownMenuItem(text = { Text("新建筛选器") }, onClick = { overflowOpen = false; editTab() })
                    DropdownMenuItem(text = { Text("默认打开的标签") }, onClick = { overflowOpen = false; showDefaultTabDialog = true })
                }
            }
        })
        AnimatedVisibility(visible = searching, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            LightSearchField(query, { query = it }, "搜索消息和联系人", Modifier.padding(horizontal = 42.dp))
        }
        val myName = state.profile?.name.orEmpty().normalizedPlayerName()
        val rows = state.conversations.filter { c ->
            (c.category == ChatCategory.Tell || c.category == ChatCategory.Linkshell || c.category == ChatCategory.FreeCompany || c.category == ChatCategory.Party || c.category == ChatCategory.Team || c.key == "novice") &&
                !state.isConversationHidden(c) &&
                (myName.isEmpty() || c.title.normalizedPlayerName() != myName) &&
                (query.isBlank() || c.title.contains(query, true) || c.lastMessage?.text?.contains(query, true) == true)
        }.distinctBy { it.key }
        val sortedRows = rows.sortedWith(
            compareByDescending<ChatConversation> { state.isConversationPinned(it) }.thenByDescending { it.lastTimestamp ?: 0L },
        )
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                .padding(horizontal = LocalContentMargin.current.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val messagesFirst = state.defaultChatListTab == "messages"
            val chips = if (messagesFirst) listOf("messages" to "消息", "tabs" to "筛选器") else listOf("tabs" to "筛选器", "messages" to "消息")
            chips.forEach { (key, label) ->
                val active = if (key == "messages") messagesExpanded else tabsExpanded
                val unread = if (key == "messages") state.badgeUnread() else 0
                ChatGroupChip(label, unread, notify = true, active = active) {
                    state.chatListTab = key
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(top = 6.dp).padding(horizontal = LocalContentMargin.current.dp)) {
            if (messagesExpanded && !state.localHidden) {
                item("local-entry") {
                    Row(Modifier.fillMaxWidth().clickable { onOpenLocal() }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        ImageGlyph(R.drawable.app_messages, AetherLightMuted, Modifier.size(20.dp))
                        Text("本地", color = AetherLightText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 12.dp))
                        if (state.localPinned()) Text("置顶", color = AetherPurple, fontSize = 9.sp, modifier = Modifier.padding(end = 6.dp))
                        Text("说话/喊话/呼喊/情感动作 ›", color = AetherLightMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                items(sortedRows, key = { "message-${it.key}" }) { conversation ->
                    Box(Modifier.animateItem()) { LightConversationRow(conversation, state, onRename = { renameTarget = it; renameText = it.title }, onChangeIcon = { iconKeyTarget = it.key }) }
                }
                if (sortedRows.isEmpty() && !state.connected) item("empty") {
                    Text("连接游戏后显示聊天消息", color = AetherLightMuted, fontSize = 14.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 50.dp))
                }
            } else {
                if (state.chatFilters.isEmpty()) {
                    item("empty-tabs") {
                        Text("还没有筛选器，点右上角新建", color = AetherLightMuted, fontSize = 14.sp,
                            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 50.dp))
                    }
                } else {
                    items(state.chatFilters, key = { "tab-${it.id}" }) { filter ->
                        val selected = filter.id == state.selectedChatFilterId
                        val last = state.chats.lastOrNull { filter.matches(it) && it.timestamp > state.clearedUntil(filter.id) }
                        var tabPress by remember(filter.id) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                        Box {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().animateItem()
                                .pointerInput(filter.id) {
                                    detectTapGestures(
                                        onTap = { state.selectedChatFilterId = filter.id; state.openChatFilterId = filter.id },
                                        onLongPress = { offset -> tabPress = offset; longPressedTabId = filter.id },
                                    )
                                }
                                .padding(vertical = 8.dp)) {
                                Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(if (selected) AetherPurple else AetherLightControl), contentAlignment = Alignment.Center) {
                                    SmallConversationIcon(state.conversationIcon(filter.id, filter.categories.firstOrNull()), filter.label, if (selected) Color.White else AetherLightMuted)
                                }
                                Column(Modifier.weight(1f).padding(start = 13.dp)) {
                                    Text(filter.label, color = AetherLightText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    if (last != null) LightMessagePreview(last, AetherLightMuted, 12.sp, Modifier.padding(top = 2.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(50.dp).padding(start = 4.dp)) {
                                    last?.let { Text(lightTalkTime(it.timestamp), color = AetherLightMuted, fontSize = 10.sp, maxLines = 1, softWrap = false) }
                                    if (filter.alertPolicy == ChatAlertPolicy.Off) {
                                        Box(Modifier.size(25.dp).clickable { state.toggleChatFilterNotifications(filter) }, contentAlignment = Alignment.Center) {
                                            ImageGlyph(R.drawable.ic_muted, AetherLightMuted.copy(alpha = .75f), Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            Box(Modifier.offset { IntOffset(tabPress.x.roundToInt(), tabPress.y.roundToInt()) }) {
                            DropdownMenu(expanded = longPressedTabId == filter.id, onDismissRequest = { longPressedTabId = null }) {
                                DropdownMenuItem(text = { Text("更换图标") }, onClick = { iconKeyTarget = filter.id; longPressedTabId = null })
                                DropdownMenuItem(text = { Text("置顶") }, onClick = { state.pinChatFilter(filter); longPressedTabId = null })
                                DropdownMenuItem(text = { Text(if (filter.alertPolicy == ChatAlertPolicy.Off) "取消消息免打扰" else "消息免打扰") }, onClick = { state.toggleChatFilterNotifications(filter); longPressedTabId = null })
                                DropdownMenuItem(text = { Text("删除筛选器", color = Color(0xFFD64555)) }, onClick = { state.removeChatFilter(filter); longPressedTabId = null })
                            }
                            }
                        }
                    }
                }
            }
        }
    }
    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名群聊") },
            text = {
                BasicTextField(
                    value = renameText,
                    onValueChange = { renameText = it.take(20) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = AetherLightText, fontSize = 15.sp),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(AetherLightSurface).padding(16.dp),
                )
            },
            confirmButton = { TextButton(onClick = { state.renameGroup(target.key, renameText); renameTarget = null }) { Text("确定") } },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } },
        )
    }
    iconKeyTarget?.let { target ->
        var showLibrary by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { iconKeyTarget = null },
            title = { Text(if (showLibrary) "图标库" else "更换图标") },
            text = {
                if (showLibrary) {
                    BuiltinIconLibrary(onSelect = { state.setConversationIcon(target, it); iconKeyTarget = null })
                } else {
                    Column {
                        Text("图标库", color = AetherLightText, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { showLibrary = true }.padding(vertical = 12.dp))
                        Text("从相册选择", color = AetherLightText, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { pickImage.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(vertical = 12.dp))
                        Text("恢复默认", color = Color(0xFFD64555), fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { state.setConversationIcon(target, ""); iconKeyTarget = null }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = {},
        )
    }
    if (showDefaultTabDialog) {
        AlertDialog(
            onDismissRequest = { showDefaultTabDialog = false },
            title = { Text("默认打开的标签") },
            text = {
                Column {
                    listOf("messages" to "消息", "tabs" to "筛选器").forEach { (key, label) ->
                        val selected = state.defaultChatListTab == key
                        Row(Modifier.fillMaxWidth().clickable { state.defaultChatListTab = key; state.chatListTab = key; showDefaultTabDialog = false }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(label, color = AetherLightText, modifier = Modifier.weight(1f))
                            Text(if (selected) "●" else "○", color = if (selected) AetherPurple else AetherLightMuted, fontSize = 20.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDefaultTabDialog = false }) { Text("关闭") } },
        )
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
    var name by remember(existing?.id) { mutableStateOf(existing?.label ?: "新建筛选器") }
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
            ChannelChoice("alliance", ChatCategory.Team, "团队", setOf(15)),
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
            LightHeader("编辑筛选器", ::finish)
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
                    Text("筛选器设置", color = AetherLightMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 20.dp, bottom = 8.dp))
                    val settings = listOf(
                        "回复发送到" to (sendChannel?.let { id -> outputChannels.firstOrNull { it.id == id }?.label } ?: "当前频道"),
                        "布局" to if (layout == ChatLayout.Bubbles) "气泡" else "紧凑",
                        "保存记录" to when (historyPolicy) { ChatHistoryPolicy.Off -> "关闭"; ChatHistoryPolicy.Session -> "本次会话"; ChatHistoryPolicy.ThirtyDays -> "30 天"; ChatHistoryPolicy.Forever -> "永久" },
                        "提醒" to when (alertPolicy) { ChatAlertPolicy.All -> "全部"; ChatAlertPolicy.Mentions -> "仅提及"; ChatAlertPolicy.Off -> "关闭" },
                    )
                    settings.forEach { (label, value) ->
                    }
                    Text("记录仅保存在这台手机上。", color = AetherLightMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp, bottom = 30.dp))
                }
                if (existing?.removable == true) item("delete-tab") {
                    Text("删除筛选器", color = Color(0xFFD64555), fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
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
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(bg).clickable(onClick = onClick)
                .padding(horizontal = 13.dp, vertical = 6.dp),
        ) {
            Text(label, color = fg, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium)
        }
        if (unread > 0) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-5).dp).clip(CircleShape)
                    .background(if (notify) Color(0xFFD93025) else Color(0xFF9AA0A6))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Text(if (unread > 99) "99+" else unread.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun chatDay(timestamp: Long): Int {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    return cal.get(java.util.Calendar.YEAR) * 10000 + (cal.get(java.util.Calendar.MONTH) + 1) * 100 + cal.get(java.util.Calendar.DAY_OF_MONTH)
}

private fun chatDayLabel(timestamp: Long): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
    return "${cal.get(java.util.Calendar.YEAR)}年${cal.get(java.util.Calendar.MONTH) + 1}月${cal.get(java.util.Calendar.DAY_OF_MONTH)}日"
}
@Composable
private fun BuiltinIconLibrary(onSelect: (String) -> Unit) {
    var libraryTab by remember { mutableStateOf("all") }
    val categories = listOf("all") + builtinConversationIcons.map { it.category }.distinct().filter { it.isNotBlank() }
    val shownIcons = if (libraryTab == "all") builtinConversationIcons else builtinConversationIcons.filter { it.category == libraryTab }
    Column {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            categories.forEach { cat ->
                val label = when (cat) {
                    "all" -> "全部"
                    "avatar" -> "头像"
                    "status" -> "状态"
                    else -> cat
                }
                Text(label, color = if (libraryTab == cat) Color.White else AetherLightMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(13.dp)).background(if (libraryTab == cat) AetherPurple else AetherLightControl).clickable { libraryTab = cat }.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
        Column(Modifier.fillMaxWidth().height(220.dp).verticalScroll(rememberScrollState()).padding(top = 12.dp)) {
            shownIcons.chunked(4).forEach { rowIcons ->
                Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowIcons.forEach { builtin ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f).clickable { onSelect(builtin.id) }) {
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(AetherLightControl), contentAlignment = Alignment.Center) {
                                Image(painterResource(builtin.res), contentDescription = null, modifier = Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit)
                            }
                            Text(builtin.label, color = AetherLightMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun SmallConversationIcon(icon: String, fallback: String, fallbackColor: Color = Color.White) {
    val builtin = (builtinConversationIcons + defaultConversationIcons).firstOrNull { it.id == icon }
    when {
        builtin != null -> Image(painterResource(builtin.res), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        icon.startsWith("/") -> {
            var bmp by remember(icon) { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(icon) { bmp = runCatching { android.graphics.BitmapFactory.decodeFile(icon) }.getOrNull() }
            val current = bmp
            if (current != null) {
                Image(current.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                Text(fallback.take(1), color = fallbackColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        else -> Text(fallback.take(1), color = fallbackColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}


@Composable
private fun ConversationRowIcon(conversation: ChatConversation, state: PhoneState, fallback: String) {
    val icon = state.conversationIcon(conversation.key, conversation.category)
    val builtin = (builtinConversationIcons + defaultConversationIcons).firstOrNull { it.id == icon }
    when {
        builtin != null -> Image(painterResource(builtin.res), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        icon.startsWith("/") -> {
            var bmp by remember(icon) { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(icon) { bmp = runCatching { android.graphics.BitmapFactory.decodeFile(icon) }.getOrNull() }
            val current = bmp
            if (current != null) {
                Image(current.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                Text(fallback.take(1), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        else -> Text(fallback.take(1), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
private fun channelDefaultColor(channel: Int): Color = when (channel) {
    10, 81 -> Color(0xFFF7F7F7)
    11, 82 -> Color(0xFFFFA666)
    30, 83 -> Color(0xFFFFFF00)
    12, 13, 80 -> Color(0xFFFFB8DE)
    14, 84, 32 -> Color(0xFF66E5FF)
    15 -> Color(0xFFFF7F00)
    24, 85 -> Color(0xFF8C7AFF)
    27, 94, 75 -> Color(0xFFD4FF7D)
    29, 28 -> Color(0xFFBAFFF0)
    56, 57, 58, 59, 60, 61, 62, 64, 65, 66, 67, 68, 71, 72, 73, 74, 75, 76, 79 -> Color(0xFFCCCCCC)
    in 16..23, in 86..93, in 37..44, in 101..107 -> Color(0xFFD4FF7D)
    else -> Color(0xFFCCCCCC)
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
    in 16..23 -> "通讯贝 ${channel - 15}"
    in 86..93 -> "通讯贝 ${channel - 85}"
    37 -> "跨服贝 1"
    in 38..44 -> "跨服贝 ${channel - 36}"
    in 101..107 -> "跨服贝 ${channel - 99}"
    29, 28 -> "情感动作"
    else -> ""
}

// Local / group chat lines carry a "[频道]<名字>" or "名字：" prefix baked into the
// text. The app already shows the author separately, so strip it before rendering.
private fun decodeChatEntities(value: String): String = value
    .replace("&#x20;", " ")
    .replace("&nbsp;", " ")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&amp;", "&")

private fun cleanChatText(raw: String, author: String): String {
    var t = decodeChatEntities(raw.trim())
    t = t.replaceFirst(Regex("^\\[[^\\]]*\\]"), "").trim()
    // 只删开头 "<名字> " 形式的名字前缀；<se.N> 音效标签是内容本身，删了会把前面的文字一起截掉
    if (t.startsWith('<')) {
        val gt = t.indexOf('>')
        if (gt >= 0) {
            val after = t.substring(gt + 1)
            if (after.startsWith(" ") && !t.substring(0, gt).trimStart().startsWith("se.", ignoreCase = true)) {
                t = after.trim()
            } else {
                val colon = t.indexOfAny(charArrayOf('：', ':'))
                if (colon in 1..24) {
                    t = t.substring(colon + 1).trim()
                } else if (author.isNotEmpty() && t.startsWith(author)) {
                    t = t.removePrefix(author).trimStart('：', ':', ' ', '>', '<').trim()
                }
            }
        }
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
@OptIn(ExperimentalLayoutApi::class)
private fun AetherphoneLocalScreen(state: PhoneState, onBack: () -> Unit) {
    var filter by remember { mutableIntStateOf(0) }
    var sendChannel by remember { mutableStateOf(1) }
    var channelMenu by remember { mutableStateOf(false) }
    var inputFocused by remember { mutableStateOf(false) }
    var inputHeightPx by remember { mutableIntStateOf(0) }
    var chatStack by remember { mutableStateOf(listOf<ChatSub>(ChatSub.Main)) }
    val pushChatSub: (ChatSub) -> Unit = { chatStack = chatStack + it }
    val popChatSub: () -> Unit = { if (chatStack.size > 1) chatStack = chatStack.dropLast(1) }
    BackHandler(enabled = chatStack.size > 1) { popChatSub() }
    BackHandler(enabled = chatStack.size == 1) { onBack() }
    val localConv = remember(state.chats.size, state.localClearedUntil) {
        ChatConversation("local", ChatCategory.Public, "本地").also { conv ->
            conv.messages.clear()
            conv.messages.addAll(
                state.chats.filter { (it.category == ChatCategory.Public || it.category == ChatCategory.Emote) && it.channel != 27 && it.channel != 75 && it.channel != 94 && it.timestamp > state.localClearedUntil }.sortedBy { it.timestamp }
            )
        }
    }
    val labels = listOf("全部", "说话", "喊话", "呼喊")
    val msgs = state.chats.filter {
        it.timestamp > state.localClearedUntil &&
        when {
            filter == 1 -> it.channel == 10 || it.channel == 81 || it.category == ChatCategory.Emote
            filter == 2 -> it.channel == 11 || it.channel == 82
            filter == 3 -> it.channel == 30 || it.channel == 83
            else -> (it.category == ChatCategory.Public || it.category == ChatCategory.Emote) && it.channel != 27 && it.channel != 75 && it.channel != 94
        }
    }.sortedBy { it.timestamp }
    LightFrame {
        if (chatStack.last() != ChatSub.Main) {
            ChatSubScreen(state, localConv, chatStack.last(), onPop = popChatSub, onPush = pushChatSub)
            return@LightFrame
        }
        Column(Modifier.fillMaxSize().imePadding()) {
            LightHeader("本地", onBack) {
                Text("⋯", color = AetherLightMuted, fontSize = 25.sp, modifier = Modifier.clickable { pushChatSub(ChatSub.LocalSettings) }.padding(horizontal = 10.dp))
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                labels.forEachIndexed { i, l ->
                    Text(l, color = if (filter == i) Color.White else AetherLightMuted, fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(if (filter == i) AetherPurple else AetherLightControl).clickable { filter = i; sendChannel = listOf(1, 1, 4, 5)[i] }.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            val listState = rememberLazyListState()
            val scrolledLocal = remember(filter) { mutableStateOf(false) }
            val listLaidOut = remember(filter) { mutableStateOf(false) }
            // 进入本地列：等列表首次布局(onGloballyPositioned)后贴底
            LaunchedEffect(listLaidOut.value, msgs.size) {
                if (listLaidOut.value && msgs.isNotEmpty() && !scrolledLocal.value) { listState.requestScrollToItem(msgs.lastIndex); scrolledLocal.value = true }
            }
            LaunchedEffect(filter, msgs.size, inputHeightPx) {
                if (msgs.isEmpty()) return@LaunchedEffect
                if (!scrolledLocal.value || nearBottomLazy(listState)) { listState.requestScrollToItem(msgs.lastIndex); scrolledLocal.value = true }
            }
            val imeVisible = WindowInsets.isImeVisible
            LaunchedEffect(imeVisible, msgs.size) {
                if (imeVisible && msgs.isNotEmpty()) { listState.requestScrollToItem(msgs.lastIndex) }
            }
            if (msgs.isEmpty()) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Text("暂无本地消息", color = AetherLightMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 44.dp))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp).onGloballyPositioned { if (!listLaidOut.value) listLaidOut.value = true },
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    itemsIndexed(msgs, key = { _, msg -> "${msg.timestamp}-${msg.channel}-${msg.sender}-${msg.text.hashCode()}" }) { index, msg ->
                        val showDate = index == 0 || chatDay(msg.timestamp) != chatDay(msgs[index - 1].timestamp)
                        if (showDate) {
                            Text(chatDayLabel(msg.timestamp), color = AetherLightMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
                        }
                        val self = msg.self || msg.isFrom(state.profile?.name)
                        val showTag = filter == 0 || msg.category == ChatCategory.Emote
                        val tag = if (showTag) channelTag(msg.channel) else ""
                        val baseName = if (self) {
                            state.profile?.name.orEmpty().ifBlank { "我" }
                        } else {
                            state.displayNameFor(msg)
                        }
                        val author = if (tag.isBlank()) baseName else "[$tag] $baseName"
                        LightChatBubble(author, msg, self, shouldShowLightSender(msgs, index, state.profile?.name), state.chatWrapChars, fontSizeSp = state.chatFontSize, neutral = true, authorFontSizeSp = state.chatAuthorFontSize, showTail = shouldShowLightSender(msgs, index, state.profile?.name), senderWorldIconId = if (state.isCrossWorld(msg)) msg.senderWorldIcon ?: msg.senderStatusIcon ?: 0 else 0)
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
                BasicTextField(
                    value = state.chatDraft,
                    onValueChange = { state.chatDraft = it },
                    enabled = state.activeCharacterOnline,
                    textStyle = androidx.compose.ui.text.TextStyle(color = AetherLightText, fontSize = 14.sp, lineHeight = 20.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.weight(1f).padding(start = 8.dp).heightIn(min = 42.dp, max = 120.dp).clip(RoundedCornerShape(11.dp)).onSizeChanged { inputHeightPx = it.height }.onFocusChanged { inputFocused = it.isFocused }
                        .background(if (state.activeCharacterOnline) AetherLightSurface else AetherLightControl),
                    decorationBox = { field ->
                        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                if (!state.activeCharacterOnline) {
                                    Text("当前无法使用聊天：角色未登录游戏", color = AetherLightMuted.copy(alpha = .72f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                } else {
                                    if (state.chatDraft.isBlank() && !inputFocused) Text("消息内容", color = AetherLightMuted, fontSize = 13.sp)
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
private fun FriendStatusIcon(state: PhoneState, name: String, size: Dp = 14.dp, modifier: Modifier = Modifier) {
    if (!state.activeCharacterOnline) return
    val online = state.friendOnlineFor(name) ?: return
    Image(
        painter = painterResource(if (online) R.drawable.status_online else R.drawable.status_offline),
        contentDescription = if (online) "在线" else "离线",
        modifier = modifier.size(size),
    )
}

@Composable
private fun LightMessagePreview(message: GameChatMessage?, color: Color, fontSize: TextUnit = 13.sp, modifier: Modifier = Modifier) {
    if (message == null) {
        Text("暂无消息", color = color, fontSize = fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
        return
    }
    val lineH = (fontSize.value + 4).sp
    val light = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val axisFont = remember { FontFamily(Font(R.font.ffxiv_axis)) }
    val fallback = cleanChatText(message.text, "").replace('\n', ' ').trim().ifBlank { " " }
    val inkChunks = remember(message) { if (message.category == ChatCategory.Emote) message.chunks.map { it.copy(italic = false) } else message.chunks }
    val ink = remember(message, color, fontSize, lineH) { chatBubbleInk(inkChunks, fallback, color, true, "", light, fontSize, lineH, axisFont) }
    val inline = chatBubbleInline(inkChunks, fallback, fontSize, lineH)
    Text(ink.annotated, color = color, fontSize = fontSize, lineHeight = lineH, inlineContent = if (inline.isEmpty()) emptyMap() else inline, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LightConversationRow(conversation: ChatConversation, state: PhoneState, onRename: (ChatConversation) -> Unit = {}, onChangeIcon: (ChatConversation) -> Unit = {}) {
    var menuOpen by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    val last = conversation.lastMessage
    val rowTitle = conversation.title
    val previewMsg = if (last != null && last.category == ChatCategory.Emote && last.isFrom(state.profile?.name)) {
        last.copy(text = state.displayNameFor(last) + last.text)
    } else last
    val preview = when {
        previewMsg == null -> "暂无消息"
        else -> previewMsg.text
    }.replace('\n', ' ')
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { state.openConversation(conversation) },
                        onLongPress = { offset -> pressOffset = offset; menuOpen = true },
                    )
                }
                .padding(vertical = 10.dp),
        ) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(AetherLightControl), contentAlignment = Alignment.Center) {
                ConversationRowIcon(conversation, state, rowTitle)
            }
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (conversation.category == ChatCategory.Tell || conversation.key.startsWith("tell:")) {
                        FriendStatusIcon(state, conversation.tellRecipient.ifBlank { rowTitle }, 14.dp, Modifier.padding(end = 6.dp))
                    }
                    Text(rowTitle, color = AetherLightText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (state.isConversationPinned(conversation)) Text("置顶", color = AetherPurple, fontSize = 9.sp, modifier = Modifier.padding(end = 6.dp))
                    conversation.lastTimestamp?.let { Text(lightTalkTime(it), color = AetherLightMuted, fontSize = 11.sp) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    LightMessagePreview(previewMsg, if (previewMsg?.category == ChatCategory.Emote) themeAdjustedChannelColor(EmoteChatColor) else AetherLightMuted, 13.sp, Modifier.weight(1f))
                    if (!conversation.notify) ImageGlyph(R.drawable.ic_muted, AetherLightMuted.copy(alpha = .75f), Modifier.size(15.dp).padding(start = 2.dp))
                    if (conversation.unread > 0) {
                        Box(
                            Modifier.padding(start = 7.dp).height(21.dp).widthIn(min = 21.dp).clip(CircleShape).background(if (conversation.notify) Color(0xFFE5485D) else Color(0xFF8A93A5)),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (conversation.unread > 99) "99+" else conversation.unread.toString(), color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 5.dp)) }
                    }
                }
            }
        }
        Box(Modifier.offset { IntOffset(pressOffset.x.roundToInt(), pressOffset.y.roundToInt()) }) {
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(text = { Text(if (state.isConversationPinned(conversation)) "取消置顶" else "置顶") }, onClick = {
                state.toggleConversationPin(conversation)
                menuOpen = false
            })
            DropdownMenuItem(text = { Text("更换图标") }, onClick = {
                onChangeIcon(conversation)
                menuOpen = false
            })
            DropdownMenuItem(text = { Text("重命名") }, onClick = {
                onRename(conversation)
                menuOpen = false
            })
            if (state.groupTitleOverride(conversation.key) != null) {
                DropdownMenuItem(text = { Text("恢复默认昵称") }, onClick = {
                    state.renameGroup(conversation.key, "")
                    menuOpen = false
                })
            }
            DropdownMenuItem(text = { Text(if (conversation.notify) "消息免打扰" else "取消消息免打扰") }, onClick = {
                state.toggleConversationNotify(conversation)
                menuOpen = false
            })
            DropdownMenuItem(text = { Text("移出列表") }, onClick = {
                if (state.isConversationHidden(conversation)) state.unhideConversation(conversation) else state.hideConversation(conversation)
                menuOpen = false
            })
        }
        }
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun LightRefreshIcon(color: Color, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_refresh_cycle),
        contentDescription = null,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color),
        modifier = modifier,
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AetherphoneContactsList(state: PhoneState) {
    var query by remember { mutableStateOf("") }
    var friendsOnly by remember { mutableStateOf(true) }
    val shown = (if (friendsOnly) state.friends else state.party).filter { it.name.contains(query, true) || it.world.contains(query, true) }
    LaunchedEffect(Unit) { state.refreshParty() }
    LaunchedEffect(friendsOnly) { if (!friendsOnly) state.refreshParty() }
    var avatarFriend by remember { mutableStateOf<PhoneFriend?>(null) }
    var avatarShowLibrary by remember { mutableStateOf(false) }
    val pickFriendAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val friend = avatarFriend
        avatarFriend = null
        if (uri != null && friend != null) {
            val path = state.savePickedFriendAvatar(friend, uri)
            if (path != null) state.setFriendAvatar(friend, path)
        }
    }
    Column(Modifier.fillMaxSize()) {
        LightHeader("联系人", state::back) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).clickable { state.refreshFriends(); state.refreshParty() }, contentAlignment = Alignment.Center) {
                LightRefreshIcon(AetherPurple, Modifier.size(17.dp))
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            item("search") {
                LightSearchField(query, { query = it }, "搜索")
                LightSegment(
                    first = "好友",
                    second = "小队",
                    firstSelected = friendsOnly,
                    onSelect = { friendsOnly = it },
                    modifier = Modifier.padding(top = 14.dp, bottom = 18.dp),
                )
            }
            val online = shown.filter { it.online }
            val offline = shown.filter { !it.online }
            if (online.isNotEmpty()) {
                item("online-label") { Text("在线 · ${formatCount(online.size)}", color = AetherLightMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp)) }
                item("online-card") { LightContactCard(online, state, onChangeAvatar = { avatarFriend = it }) }
                item("online-gap") { Spacer(Modifier.height(18.dp)) }
            }
            if (offline.isNotEmpty()) {
                item("offline-label") { Text("离线 · ${formatCount(offline.size)}", color = AetherLightMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp)) }
                item("offline-card") { LightContactCard(offline, state, onChangeAvatar = { avatarFriend = it }) }
            }
            if (shown.isEmpty()) {
                item("empty") { Text(if (!state.connected) "连接游戏后读取列表" else if (friendsOnly) "暂无联系人" else "暂无小队成员", color = AetherLightMuted, modifier = Modifier.padding(top = 50.dp).fillMaxWidth(), textAlign = TextAlign.Center) }
            }
            item("end") { Spacer(Modifier.height(16.dp)) }
        }
    }
    avatarFriend?.let { friend ->
        AlertDialog(
            onDismissRequest = { avatarFriend = null },
            title = { Text(if (avatarShowLibrary) "图标库" else "更换好友头像") },
            text = {
                if (avatarShowLibrary) {
                    BuiltinIconLibrary(onSelect = { state.setFriendAvatar(friend, it); avatarFriend = null })
                } else {
                    Column {
                        Text("图标库", color = AetherLightText, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { avatarShowLibrary = true }.padding(vertical = 12.dp))
                        Text("从相册选择", color = AetherLightText, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { pickFriendAvatar.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(vertical = 12.dp))
                        Text("恢复默认", color = Color(0xFFD64555), fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { state.setFriendAvatar(friend, ""); avatarFriend = null }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = {},
        )
    }
}

private fun senderStatusNameIcon(name: String?): Int? {
    val n = name ?: return null
    return when {
        n.contains("新人") || n.contains("新手") -> R.drawable.fst_new
        n.contains("回归") -> R.drawable.fst_returner
        n.contains("制作采集") || n.contains("制作") -> R.drawable.fst_tradementor
        n.contains("对战") -> R.drawable.fst_pvpmentor
        n.contains("战斗") -> R.drawable.fst_pvementor
        n.contains("指导者") || n.contains("导师") -> R.drawable.fst_mentor
        n.contains("组队") -> R.drawable.fst_lookingforparty
        n.contains("招募") -> R.drawable.fst_recruiting
        else -> null
    }
}

private fun friendStatusIcon(status: Long): Int? {
    if (status and (1L shl 47) == 0L) return null
    return when {
        (status and (1L shl 43)) != 0L -> R.drawable.fst_duty           // 任务中
        (status and ((1L shl 36) or (1L shl 37) or (1L shl 38) or (1L shl 39))) != 0L -> R.drawable.fst_party       // 组队中
        (status and (1L shl 12)) != 0L -> R.drawable.fst_busy           // 忙碌
        (status and (1L shl 17)) != 0L -> R.drawable.fst_afk            // 离开
        (status and (1L shl 23)) != 0L -> R.drawable.fst_lookingforparty // 希望组队
        (status and (1L shl 26)) != 0L -> R.drawable.fst_recruiting     // 队员招募中
        (status and (1L shl 30)) != 0L -> R.drawable.fst_pvpmentor      // 对战指导者
        (status and (1L shl 28)) != 0L -> R.drawable.fst_pvementor      // 战斗指导者
        (status and (1L shl 29)) != 0L -> R.drawable.fst_tradementor    // 制作采集指导者
        (status and (1L shl 27)) != 0L -> R.drawable.fst_mentor         // 指导者
        (status and (1L shl 31)) != 0L -> R.drawable.fst_returner       // 回归玩家
        (status and (1L shl 32)) != 0L -> R.drawable.fst_new            // 新人
        else -> null
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun LightContactCard(friends: List<PhoneFriend>, state: PhoneState, onChangeAvatar: (PhoneFriend) -> Unit = {}) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(AetherLightSurface)) {
        friends.forEachIndexed { index, friend ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { state.openFriend(friend) }, onLongClick = { onChangeAvatar(friend) }).padding(horizontal = 20.dp, vertical = 11.dp),
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(AetherLightControl), contentAlignment = Alignment.Center) {
                    SmallConversationIcon(state.friendAvatar(friend), friend.name.take(1), if (friend.online) AetherPurple else Color(0xFFA7A7AE))
                }
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(friend.name, color = if (friend.online) AetherLightText else AetherLightMuted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(friend.world.ifBlank { "未知服务器" }, color = AetherLightMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
                if (friend.online) {
                    val stIcon = friendStatusIcon(friend.status)
                    if (stIcon != null) Image(painterResource(stIcon), contentDescription = null, modifier = Modifier.size(20.dp)) else Image(painterResource(R.drawable.status_online), contentDescription = "在线", modifier = Modifier.size(16.dp))
                }
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
            LightHeader(
                title = friend?.name ?: "联系人",
                onBack = state::back,
                trailing = {
                    Text("⋯", color = AetherLightMuted, fontSize = 25.sp, modifier = Modifier.padding(horizontal = 8.dp))
                },
            )
            if (friend == null) return@Column
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
                Box(Modifier.size(94.dp).clip(CircleShape).background(AetherLightControl), contentAlignment = Alignment.Center) {
                    SmallConversationIcon(state.friendAvatar(friend), friend.name.take(1), AetherPurple)
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

private sealed interface ChatSub {
    data object Main : ChatSub
    data object Settings : ChatSub
    data object TabSettings : ChatSub
    data object LocalSettings : ChatSub
    data object Appearance : ChatSub
    data class SearchHistory(val showAvatar: Boolean = true) : ChatSub
    data class SearchInput(val showAvatar: Boolean = true) : ChatSub
    data object Calendar : ChatSub
    data class MessageView(val anchorTimestamp: Long) : ChatSub
}

@Composable
private fun ChatSubScreen(state: PhoneState, conversation: ChatConversation, sub: ChatSub, onPop: () -> Unit, onPush: (ChatSub) -> Unit) {
    when (sub) {
        ChatSub.Settings -> ChatSettingsScreen(
            state, conversation,
            onBack = onPop,
            onSearchHistory = { onPush(ChatSub.SearchHistory()) },
            onAppearance = { onPush(ChatSub.Appearance) },
            onDeleteConversation = {
                state.hideConversation(conversation)
                state.closeConversation()
            },
        )
        ChatSub.LocalSettings -> LocalSettingsScreen(
            state,
            onBack = onPop,
            onHistory = { onPush(ChatSub.SearchHistory(showAvatar = false)) },
            onAppearance = { onPush(ChatSub.Appearance) },
        )
        ChatSub.TabSettings -> ChatTabSettingsScreen(
            state, conversation,
            onBack = onPop,
            onHistory = { onPush(ChatSub.SearchHistory(showAvatar = false)) },
            onEditFilter = {
                state.editingChatFilterId = state.openChatFilterId
                state.editChatTabs = true
                state.closeConversation()
            },
            onAppearance = { onPush(ChatSub.Appearance) },
            onDeleteConversation = {
                state.hideConversation(conversation)
                state.closeConversation()
            },
        )
        ChatSub.Appearance -> ChatAppearanceScreen(state, onBack = onPop)
        is ChatSub.SearchHistory -> ChatSearchHistoryScreen(onBack = onPop, onOpenInput = { onPush(ChatSub.SearchInput(sub.showAvatar)) }, onOpenCalendar = { onPush(ChatSub.Calendar) })
        is ChatSub.SearchInput -> ChatSearchInputScreen(state, conversation, showAvatar = sub.showAvatar, onBack = onPop, onOpenMessage = { timestamp -> onPush(ChatSub.MessageView(timestamp)) })
        is ChatSub.MessageView -> ChatMessageViewScreen(state, conversation, sub.anchorTimestamp, onBack = onPop)
        ChatSub.Calendar -> ChatCalendarScreen(conversation, onBack = onPop, onOpenDay = { firstTs -> onPush(ChatSub.MessageView(firstTs)) })
        ChatSub.Main -> Unit
    }
}

@Composable
private fun ChatSettingRow(label: String, onClick: (() -> Unit)? = null, color: Color = AetherLightText, trailing: (@Composable () -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label, color = color, fontSize = 15.sp, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun ChatSettingDivider() {
    Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(AetherLightSeparator))
}

@Composable
private fun ChatSettingsScreen(state: PhoneState, conversation: ChatConversation, onBack: () -> Unit, onSearchHistory: () -> Unit, onAppearance: () -> Unit, onDeleteConversation: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        LightHeader("聊天设置", onBack) {}
        Column(
            Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp)).background(AetherLightSurface),
        ) {
            ChatSettingRow("查找聊天记录", onClick = onSearchHistory, trailing = { Text("›", color = AetherLightMuted, fontSize = 24.sp) })
            ChatSettingDivider()
            ChatSettingRow("外观设置", onClick = onAppearance, trailing = { Text("›", color = AetherLightMuted, fontSize = 24.sp) })
            ChatSettingDivider()
            ChatSettingRow("设为置顶", trailing = { Switch(checked = state.isConversationPinned(conversation), onCheckedChange = { state.toggleConversationPin(conversation) }) })
            ChatSettingDivider()
            ChatSettingRow("消息免打扰", trailing = { Switch(checked = !conversation.notify, onCheckedChange = { state.toggleConversationNotify(conversation) }) })
            ChatSettingDivider()
            ChatSettingRow("删除会话", color = Color(0xFFD64555), onClick = onDeleteConversation)
            ChatSettingDivider()
            ChatSettingRow("删除聊天记录", color = Color(0xFFD64555), onClick = { confirmClear = true })
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("删除聊天记录") },
            text = { Text("确定删除该会话的全部聊天记录吗？此操作不可恢复。") },
            confirmButton = { TextButton(onClick = { state.clearConversation(conversation); confirmClear = false }) { Text("删除", color = Color(0xFFD64555)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun LocalSettingsScreen(state: PhoneState, onBack: () -> Unit, onHistory: () -> Unit, onAppearance: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        LightHeader("本地设置", onBack) {}
        Column(
            Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp)).background(AetherLightSurface),
        ) {
            ChatSettingRow("查看历史记录", onClick = onHistory, trailing = { Text("›", color = AetherLightMuted, fontSize = 24.sp) })
            ChatSettingDivider()
            ChatSettingRow("外观设置", onClick = onAppearance, trailing = { Text("›", color = AetherLightMuted, fontSize = 24.sp) })
            ChatSettingDivider()
            ChatSettingRow("删除聊天记录", color = Color(0xFFD64555), onClick = { confirmClear = true })
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("删除聊天记录") },
            text = { Text("确定删除本地聊天记录吗？此操作不可恢复。") },
            confirmButton = { TextButton(onClick = { state.clearLocalMessages(); confirmClear = false }) { Text("删除", color = Color(0xFFD64555)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
}
@Composable
private fun ChatTabSettingsScreen(state: PhoneState, conversation: ChatConversation, onBack: () -> Unit, onHistory: () -> Unit, onEditFilter: () -> Unit, onAppearance: () -> Unit, onDeleteConversation: () -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        LightHeader("筛选器设置", onBack) {}
        Column(
            Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp)).background(AetherLightSurface),
        ) {
            ChatSettingRow("查看历史记录", onClick = onHistory, trailing = { Text("›", color = AetherLightMuted, fontSize = 24.sp) })
            ChatSettingDivider()
            ChatSettingRow("编辑筛选器", onClick = onEditFilter, trailing = { Text("›", color = AetherLightMuted, fontSize = 24.sp) })
            ChatSettingDivider()
            ChatSettingRow("外观设置", onClick = onAppearance, trailing = { Text("›", color = AetherLightMuted, fontSize = 24.sp) })
            ChatSettingDivider()
            ChatSettingRow("设为置顶", trailing = { Switch(checked = state.isConversationPinned(conversation), onCheckedChange = { state.toggleConversationPin(conversation) }) })
            ChatSettingDivider()
            ChatSettingRow("消息免打扰", trailing = { Switch(checked = !conversation.notify, onCheckedChange = { state.toggleConversationNotify(conversation) }) })
            ChatSettingDivider()
            ChatSettingRow("删除会话", color = Color(0xFFD64555), onClick = onDeleteConversation)
            ChatSettingDivider()
            ChatSettingRow("删除聊天记录", color = Color(0xFFD64555), onClick = { confirmClear = true })
        }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("删除聊天记录") },
            text = { Text("确定删除该筛选器的全部聊天记录吗？此操作不可恢复。") },
            confirmButton = { TextButton(onClick = { state.clearConversation(conversation); confirmClear = false }) { Text("删除", color = Color(0xFFD64555)) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ChatAdjustRow(label: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit, hint: String? = null) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = AetherLightText, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).background(AetherLightControl).clickable(onClick = onMinus), contentAlignment = Alignment.Center) {
                Text("−", color = AetherPurple, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Text("  $value  ", color = AetherLightText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(9.dp)).background(AetherLightControl).clickable(onClick = onPlus), contentAlignment = Alignment.Center) {
                Text("＋", color = AetherPurple, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (hint != null) {
            Text(hint, color = AetherLightMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp))
        }
    }
}

@Composable
private fun ChatAppearanceScreen(state: PhoneState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        LightHeader("外观设置", onBack) {}
        Column(
            Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp)).background(AetherLightSurface),
        ) {
            Text("全局设置 · 对所有页面生效", color = AetherLightMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp))
            ChatSettingDivider()
            ChatAdjustRow("左右边距", state.contentMargin,
                onMinus = { state.contentMargin = (state.contentMargin - 2).coerceAtLeast(0) },
                onPlus = { state.contentMargin = (state.contentMargin + 2).coerceAtMost(60) },
                hint = "左右都向内收缩·数值越小越贴近屏幕边缘",
            )
            ChatSettingDivider()
            ChatAdjustRow("聊天字号", state.chatFontSize,
                onMinus = { state.chatFontSize = (state.chatFontSize - 1).coerceAtLeast(10) },
                onPlus = { state.chatFontSize = (state.chatFontSize + 1).coerceAtMost(26) },
            )
            ChatSettingDivider()
            ChatAdjustRow("角色ID字号", state.chatAuthorFontSize,
                onMinus = { state.chatAuthorFontSize = (state.chatAuthorFontSize - 1).coerceAtLeast(9) },
                onPlus = { state.chatAuthorFontSize = (state.chatAuthorFontSize + 1).coerceAtMost(22) },
            )

        }
        Text("主题", color = AetherLightMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = LocalContentMargin.current.dp + 4.dp, top = 18.dp, bottom = 6.dp))
        Column(
            Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp)
                .clip(RoundedCornerShape(12.dp)).background(AetherLightSurface),
        ) {
            PhoneThemeMode.entries.forEachIndexed { index, mode ->
                Row(Modifier.fillMaxWidth().clickable { state.themeMode = mode }.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(mode.label, color = AetherLightText, modifier = Modifier.weight(1f))
                    Text(if (state.themeMode == mode) "●" else "○", color = if (state.themeMode == mode) AetherPurple else AetherLightMuted, fontSize = 20.sp)
                }
                if (index < PhoneThemeMode.entries.lastIndex) ChatSettingDivider()
            }
        }
    }
}

@Composable
private fun ChatSearchHistoryScreen(onBack: () -> Unit, onOpenInput: () -> Unit, onOpenCalendar: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            LightHeader("查找聊天记录", onBack) {}
            Column(Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(11.dp)).background(AetherLightControl)
                        .clickable(onClick = onOpenInput).padding(horizontal = 13.dp),
                ) {
                    Text("⌕", color = AetherLightMuted, fontSize = 21.sp, modifier = Modifier.padding(end = 10.dp))
                    Text("搜索聊天记录", color = AetherLightMuted, fontSize = 14.sp)
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 22.dp).size(54.dp).clip(CircleShape)
                .background(AetherPurple).clickable(onClick = onOpenCalendar),
            contentAlignment = Alignment.Center,
        ) {
            Text("📅", color = Color.White, fontSize = 22.sp)
        }
    }
}

@Composable
private fun ChatSearchInputScreen(state: PhoneState, conversation: ChatConversation, showAvatar: Boolean, onBack: () -> Unit, onOpenMessage: (Long) -> Unit) {
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    val results = remember(conversation.messages, query) {
        if (query.isBlank()) emptyList() else conversation.messages.filter { it.text.contains(query, true) || it.sender.contains(query, true) }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        LightHeader("查找聊天记录", onBack) {}
        Row(Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            LightSearchField(query, { query = it }, "搜索聊天记录", Modifier.weight(1f).focusRequester(focusRequester))
            if (query.isNotBlank()) Text(results.size.toString(), color = AetherPurple, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
        }
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(results, key = { index, message -> "${message.timestamp}-$index" }) { _, message ->
                SearchResultRow(state, conversation, message, query, showAvatar) { onOpenMessage(message.timestamp) }
            }
            if (query.isNotBlank() && results.isEmpty()) item {
                Text("未找到匹配的聊天记录", color = AetherLightMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(30.dp))
            }
        }
    }
}

@Composable
private fun SearchResultRow(state: PhoneState, conversation: ChatConversation, message: GameChatMessage, query: String, showAvatar: Boolean = true, onClick: () -> Unit) {
    val self = message.self || (state.profile?.name != null && message.isFrom(state.profile?.name))
    val author = if (self) state.profile?.name?.takeIf { it.isNotBlank() } ?: "我" else state.displayNameFor(message).takeIf { it != "对方" } ?: conversation.title
    val cleaned = cleanChatText(message.text, author)
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(AetherLightSurface).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        if (showAvatar) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(AetherLightControl), contentAlignment = Alignment.Center) {
                SmallConversationIcon(state.conversationIcon(conversation.key, conversation.category), author.take(1), AetherPurple)
            }
        }
        Column(Modifier.weight(1f).padding(start = if (showAvatar) 11.dp else 0.dp)) {
            Text(author.replace('\uE05D', ' '), color = AetherLightText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            HighlightText(cleaned, query, AetherLightMuted, 13.sp, 3, Modifier.padding(top = 3.dp))
        }
        Text(chatRecordTime(message.timestamp), color = AetherLightMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun HighlightText(text: String, query: String, color: Color, fontSize: androidx.compose.ui.unit.TextUnit, maxLines: Int, modifier: Modifier = Modifier) {
    val annotated = buildAnnotatedString {
        appendHighlighted(text, query, SpanStyle(color = color), Color(0x66FFEB3B))
    }
    Text(annotated, fontSize = fontSize, maxLines = maxLines, overflow = TextOverflow.Ellipsis, modifier = modifier)
}

private fun chatRecordTime(timestamp: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun dayKey(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    return String.format(Locale.US, "%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}

private fun nearBottomLazy(listState: LazyListState, itemMargin: Int = 0): Boolean {
    if (listState.isScrollInProgress) return false
    val info = listState.layoutInfo
    if (info.totalItemsCount <= 0) return true
    val last = info.visibleItemsInfo.lastOrNull() ?: return true
    // 距离底部的条目数（用户上滑超过 itemMargin 条就不再自动跳转）
    return (info.totalItemsCount - 1 - last.index) <= itemMargin
}

@Composable
private fun ChatMessagesLazyColumn(messages: List<GameChatMessage>, conversation: ChatConversation, state: PhoneState, highlight: String, listState: LazyListState, modifier: Modifier, followLatest: Boolean = false) {
    var selectionActive by remember { mutableStateOf(false) }
    var selectionEpoch by remember { mutableIntStateOf(0) }
    val defaultToolbar = LocalTextToolbar.current
    val toolbar = remember(defaultToolbar) {
        TrackingTextToolbar(
            delegate = defaultToolbar,
            onShown = { selectionActive = true },
            onHidden = { selectionActive = false; selectionEpoch++ },
        )
    }
    val listLaidOut = remember(conversation.key) { mutableStateOf(false) }
    var anchoredBottom by remember(conversation.key) { mutableStateOf(false) }
    // 进入会话：等列表完成首次布局(onGloballyPositioned)后贴底；只在首次贴底，新消息到达时不得拽回底部
    LaunchedEffect(listLaidOut.value) {
        if (listLaidOut.value && followLatest && messages.isNotEmpty() && !anchoredBottom) {
            listState.requestScrollToItem(messages.lastIndex)
            anchoredBottom = true
        }
    }
    // 新消息到达且仍贴近底部时自动跟到底部
    LaunchedEffect(messages.size, conversation.key) {
        if (!followLatest || messages.isEmpty()) return@LaunchedEffect
        if (nearBottomLazy(listState)) { listState.requestScrollToItem(messages.lastIndex) }
    }
    CompositionLocalProvider(LocalTextToolbar provides toolbar) {
        val dismissMod = if (selectionActive) Modifier.pointerInput(Unit) { detectTapGestures(onTap = { selectionActive = false; selectionEpoch++ }, onLongPress = {}) } else Modifier
        LazyColumn(state = listState, modifier = modifier.then(dismissMod).onGloballyPositioned { if (!listLaidOut.value) listLaidOut.value = true }, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        itemsIndexed(messages, key = { _, message -> "${message.timestamp}-${message.channel}-${message.sender}-${message.text.hashCode()}" }) { index, message ->
            val showDate = index == 0 || chatDay(message.timestamp) != chatDay(messages[index - 1].timestamp)
            if (showDate) {
                Text(chatDayLabel(message.timestamp), color = AetherLightMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
            }
            val self = message.self || message.isFrom(state.profile?.name)
            val tag = if (conversation.key.startsWith("tab:")) channelTag(message.channel) else ""
            val author = if (self) {
                val me = state.profile?.name.orEmpty().ifBlank { "我" }
                if (tag.isNotEmpty()) "[$tag] $me" else me
            } else if (message.category == ChatCategory.System || message.sender.isBlank() || message.channel == 75 || message.channel == 56) {
                "系统"
            } else {
                // 私聊会话里对方的 ID 优先显示会话重命名的昵称，未重命名则显示 角色名@服务器
                val base = if (conversation.category == ChatCategory.Tell) (state.groupTitleOverride(conversation.key) ?: state.displayNameFor(message)) else state.displayNameFor(message)
                if (tag.isNotEmpty()) "[$tag] $base" else base
            }
            Column(Modifier.fillMaxWidth()) {
                val senderStatus = if (conversation.category != ChatCategory.Tell && !self && message.category != ChatCategory.System) {
                    val senderKey = (message.senderName ?: message.sender).normalizedPlayerName()
                    state.friends.firstOrNull { it.online && it.name.normalizedPlayerName() == senderKey }?.status ?: 0L
                } else 0L
                // 私聊会话不显示消息上方的角色 ID（气泡区分自己/对方），但组首条仍有尾巴；其它频道照旧
                val groupStart = shouldShowLightSender(messages, index, state.profile?.name)
                // 只有真正的私聊窗口(tell:)按私聊渲染；筛选器(tab:)/群聊/本地列都按“非私聊窗口”合并——组首条显示作者
                val privateChat = conversation.key.startsWith("tell:")
                val showAuthor = !privateChat && groupStart
                val showTail = privateChat || groupStart
                LightChatBubble(author, message, self, showAuthor, state.chatWrapChars, conversation.title, state.chatFontSize, neutral = !conversation.key.startsWith("tab:"), jobIconId = if (conversation.category == ChatCategory.Party || conversation.category == ChatCategory.Team) state.jobIconIdFor(author) else 0, highlight = highlight, senderStatus = senderStatus, authorFontSizeSp = state.chatAuthorFontSize, selectionEpoch = selectionEpoch, showTail = showTail, senderWorldIconId = if (state.isCrossWorld(message)) message.senderWorldIcon ?: message.senderStatusIcon ?: 0 else 0)
                if (message.sendState == 2 && conversation.category == ChatCategory.Tell) {
                    Text(
                        "⚠ 向${conversation.title.ifBlank { "对方" }}发送悄悄话失败",
                        color = Color(0xFFE5484D),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ChatMessageViewScreen(state: PhoneState, conversation: ChatConversation, anchorTimestamp: Long, onBack: () -> Unit) {
    val messages = conversation.messages
    val listState = rememberLazyListState()
    var anchored by remember { mutableStateOf(false) }
    val anchorIndex = remember(messages.size, anchorTimestamp) { messages.indexOfFirst { it.timestamp == anchorTimestamp }.coerceAtLeast(0) }
    // 只在该屏首次进入（或换日期）时定位到当天第一条；后续新消息到达不再跳回，
    // 否则用户上滑查看历史时会被新消息拽回当天第一条
    LaunchedEffect(anchorIndex) {
        if (messages.isNotEmpty() && !anchored) {
            listState.requestScrollToItem(anchorIndex)
            anchored = true
        }
    }
    Column(Modifier.fillMaxSize()) {
        LightHeader(conversation.title, onBack) {}
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无消息", color = AetherLightMuted, fontSize = 14.sp) }
        } else {
            ChatMessagesLazyColumn(messages, conversation, state, "", listState, Modifier.fillMaxSize().padding(horizontal = LocalContentMargin.current.dp), followLatest = false)
        }
    }
}

@Composable
private fun ChatCalendarScreen(conversation: ChatConversation, onBack: () -> Unit, onOpenDay: (Long) -> Unit) {
    val messages = conversation.messages
    val daysWithMessages = remember(messages) { messages.map { dayKey(it.timestamp) }.toSet() }
    val dayFirstMessage = remember(messages) {
        val map = HashMap<String, Long>()
        for (m in messages) {
            val key = dayKey(m.timestamp)
            if (!map.containsKey(key)) map[key] = m.timestamp
        }
        map
    }
    val now = Calendar.getInstance()
    var year by remember { mutableStateOf(now.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(now.get(Calendar.MONTH)) }
    val monthHasMessages = remember(year, month, daysWithMessages) {
        val prefix = String.format(Locale.US, "%04d-%02d", year, month + 1)
        daysWithMessages.any { it.startsWith(prefix) }
    }
    val cal = remember(year, month) { Calendar.getInstance().apply { set(year, month, 1, 0, 0, 0); set(Calendar.MILLISECOND, 0) } }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    Column(Modifier.fillMaxSize()) {
        LightHeader("聊天记录日历", onBack) {}
        Row(Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = AetherPurple, fontSize = 30.sp, modifier = Modifier.clickable {
                month -= 1
                if (month < 0) { month = 11; year -= 1 }
            }.padding(8.dp))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${year}年${month + 1}月", color = AetherLightText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(if (monthHasMessages) "本月有聊天记录" else "本月无聊天记录", color = if (monthHasMessages) AetherPurple else AetherLightMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Text("›", color = AetherPurple, fontSize = 30.sp, modifier = Modifier.clickable {
                month += 1
                if (month > 11) { month = 0; year += 1 }
            }.padding(8.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { Text(it, color = AetherLightMuted, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = LocalContentMargin.current.dp, vertical = 4.dp)) {
            var day = 1
            var week = 0
            while (day <= daysInMonth) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    for (col in 0..6) {
                        val cellDay = if (week == 0 && col < firstDayOfWeek) 0 else if (day <= daysInMonth) day else 0
                        if (cellDay == 0) {
                            Spacer(Modifier.weight(1f).height(42.dp))
                        } else {
                            val key = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, cellDay)
                            val has = key in daysWithMessages
                            val firstTs = dayFirstMessage[key]
                            Box(
                                Modifier.weight(1f).height(42.dp).padding(3.dp).clip(CircleShape)
                                    .background(if (has) AetherPurple.copy(alpha = .22f) else Color.Transparent)
                                    .clickable(enabled = has && firstTs != null) { firstTs?.let(onOpenDay) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(cellDay.toString(), color = if (has) AetherLightText else AetherLightMuted, fontSize = 13.sp)
                            }
                            day += 1
                        }
                    }
                }
                week += 1
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AetherphoneConversationScreen(state: PhoneState, conversation: ChatConversation) {
    var channelMenu by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var chatStack by remember { mutableStateOf(listOf<ChatSub>(ChatSub.Main)) }
    val pushChatSub: (ChatSub) -> Unit = { chatStack = chatStack + it }
    val popChatSub: () -> Unit = { if (chatStack.size > 1) chatStack = chatStack.dropLast(1) }
    BackHandler(enabled = chatStack.size > 1) { popChatSub() }
    val focus = LocalFocusManager.current
    val send = {
        if (state.activeCharacterOnline && state.chatDraft.isNotBlank()) {
            state.sendToConversation(conversation, state.chatDraft)
            focus.clearFocus()
        }
    }
    var inputFocused by remember { mutableStateOf(false) }
    var inputHeightPx by remember { mutableIntStateOf(0) }
    LightFrame {
        if (chatStack.last() != ChatSub.Main) {
            ChatSubScreen(state, conversation, chatStack.last(), onPop = popChatSub, onPush = pushChatSub)
            return@LightFrame
        }
        Column(Modifier.fillMaxSize().imePadding()) {
            LightHeader(
                title = conversation.title,
                onBack = state::back,
                titleIcon = {
                    if (conversation.key.startsWith("tell:")) {
                        FriendStatusIcon(state, conversation.tellRecipient.ifBlank { conversation.title }, 16.dp, Modifier.padding(start = 6.dp))
                    }
                },
                trailing = {
                    Text("⋯", color = AetherLightMuted, fontSize = 25.sp, modifier = Modifier.clickable {
                        if (conversation.key.startsWith("tab:")) pushChatSub(ChatSub.TabSettings) else pushChatSub(ChatSub.Settings)
                    }.padding(horizontal = 10.dp))
                },
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val visible = if (search.isBlank()) conversation.messages else conversation.messages.filter { it.text.contains(search, true) || it.sender.contains(search, true) }
                val listState = rememberLazyListState()
                val matchIndices = remember(visible, search) {
                    if (search.isBlank()) emptyList() else visible.indices.filter { i -> visible[i].text.contains(search, true) || visible[i].sender.contains(search, true) }
                }
                var matchIndex by remember(matchIndices.size) { mutableIntStateOf(0) }
                val scrollScope = rememberCoroutineScope()
                val jumpToMatch: (Int) -> Unit = { delta ->
                    if (matchIndices.isNotEmpty()) {
                        matchIndex = ((matchIndex + delta) % matchIndices.size + matchIndices.size) % matchIndices.size
                        scrollScope.launch { listState.animateScrollToItem(matchIndices[matchIndex]) }
                    }
                }
                LaunchedEffect(conversation.key, visible.size, search, inputHeightPx) {
                    if (search.isBlank() && visible.isNotEmpty() && nearBottomLazy(listState)) { listState.requestScrollToItem(visible.lastIndex) }
                }
                val imeVisible = WindowInsets.isImeVisible
                LaunchedEffect(imeVisible, visible.size) {
                    if (imeVisible && search.isBlank() && visible.isNotEmpty()) { listState.requestScrollToItem(visible.lastIndex) }
                }
                val failureTick = visible.takeLast(3).joinToString("|") { "${it.timestamp}:${it.sendState}" }
                LaunchedEffect(failureTick) {
                    val failedIdx = visible.indexOfLast { it.sendState == 2 }
                    if (failedIdx >= 0 && visible.isNotEmpty()) {
                        listState.animateScrollToItem(failedIdx)
                        val info = listState.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == failedIdx } ?: info.visibleItemsInfo.lastOrNull()
                        if (item != null) {
                            val overflow = item.offset + item.size - info.viewportEndOffset
                            if (overflow > 0) listState.animateScrollBy(overflow.toFloat())
                        }
                    }
                }
                if (visible.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                        Text("暂无消息", color = AetherLightMuted, fontSize = 14.sp, modifier = Modifier.padding(top = 44.dp))
                    }
                } else {
                    ChatMessagesLazyColumn(visible, conversation, state, search, listState, Modifier.fillMaxSize().padding(horizontal = LocalContentMargin.current.dp), followLatest = search.isBlank())
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = searching,
                    modifier = Modifier.align(Alignment.TopCenter),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        LightSearchField(search, { search = it }, "搜索聊天内容", Modifier.weight(1f))
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(AetherLightControl).clickable { jumpToMatch(-1) }, contentAlignment = Alignment.Center) {
                            Text("⌃", color = AetherLightMuted, fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(6.dp))
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(AetherLightControl).clickable { jumpToMatch(1) }, contentAlignment = Alignment.Center) {
                            Text("⌄", color = AetherLightMuted, fontSize = 18.sp)
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                if (conversation.key.startsWith("tab:")) {
                    Box {
                        Box(
                            modifier = Modifier.width(58.dp).height(42.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFE4F0))
                                .clickable { channelMenu = true }, contentAlignment = Alignment.Center,
                        ) { Text(state.currentChannelName, color = AetherPink, fontSize = 11.sp, maxLines = 1) }
                        DropdownMenu(expanded = channelMenu, onDismissRequest = { channelMenu = false }) {
                            outputChannels.forEach { channel ->
                                DropdownMenuItem(text = { Text(channel.label) }, onClick = { state.changeChannel(channel); channelMenu = false })
                            }
                        }
                    }
                }
                BasicTextField(
                    value = state.chatDraft,
                    onValueChange = { state.chatDraft = it },
                    enabled = state.activeCharacterOnline,
                    textStyle = androidx.compose.ui.text.TextStyle(color = AetherLightText, fontSize = 14.sp, lineHeight = 20.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.weight(1f).padding(start = 8.dp).heightIn(min = 42.dp, max = 120.dp).clip(RoundedCornerShape(11.dp)).onSizeChanged { inputHeightPx = it.height }.onFocusChanged { inputFocused = it.isFocused }
                        .background(if (state.activeCharacterOnline) AetherLightSurface else AetherLightControl),
                    decorationBox = { field ->
                        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp), contentAlignment = Alignment.CenterStart) {
                                if (!state.activeCharacterOnline) {
                                    Text("当前无法使用聊天：角色未登录游戏", color = AetherLightMuted.copy(alpha = .72f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                } else {
                                    if (state.chatDraft.isBlank() && !inputFocused) Text("消息内容", color = AetherLightMuted, fontSize = 13.sp)
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
    val clearedUntil = state.clearedUntil(filter.id)
    val conversation = remember(filter.id, state.chats.size, clearedUntil) {
        val category = filter.categories.firstOrNull() ?: ChatCategory.Public
        ChatConversation("tab:${filter.id}", category, filter.label).also { chat ->
            state.chats.filter { filter.matches(it) && it.timestamp > clearedUntil }.forEach(chat::add)
        }
    }
    AetherphoneConversationScreen(state, conversation)
}

private class ChatInk(
    val annotated: AnnotatedString,
    val placeholders: List<AnnotatedString.Range<Placeholder>>,
)

private fun cleanItemLinkChunks(chunks: List<GameChatChunk>): List<GameChatChunk> {
    // 只重建“图标后连续含 PUA/� 的道具链接簇”：拿到完整名 + HQ，其余句子按原顺序保留，避免把长句误当道具名打乱顺序。
    val iconIdx = chunks.indexOfFirst { it.icon == 0xE0BB }
    if (iconIdx < 0) return chunks
    val head = chunks.take(iconIdx)
    val icon = chunks[iconIdx]
    val rest = chunks.drop(iconIdx + 1)
    fun isMarker(t: String) = t.any { it.code in 0xE000..0xF8FF || it.code == 0xFFFD || it.code == 0xE0BB } || t.isBlank()
    var clusterEnd = 0
    for (i in rest.indices) { val t = rest[i].text.orEmpty(); if (i == 0 || isMarker(t)) clusterEnd = i + 1 else break }
    val cluster = rest.take(clusterEnd)
    val tail = rest.drop(clusterEnd)
    fun clean(s: String?) = (s ?: "").filter { it.code !in 0xE000..0xF8FF && it.code != 0xFFFD }
    val itemName = cluster.map { clean(it.text) }.maxByOrNull { it.length } ?: ""
    val glyphBuilder = StringBuilder()
    for (c in cluster) glyphBuilder.append((c.text.orEmpty()).filter { it.code in 0xE000..0xF8FF && it.code != 0xE0BB })
    val out = ArrayList<GameChatChunk>()
    out.addAll(head)
    out.add(icon)
    if (itemName.isNotEmpty()) {
        val ref = cluster.firstOrNull { it.text != null }
        out.add(GameChatChunk(text = itemName, italic = ref?.italic ?: false, foreground = ref?.foreground))
    }
    if (glyphBuilder.isNotEmpty()) out.add(GameChatChunk(text = glyphBuilder.toString()))
    out.addAll(tail)
    return out
}

// 把“自动换行的非末尾行”的两端对齐字距烘焙进字符 span：整条消息只需一个文本节点即可两端对齐
private fun justifyText(original: androidx.compose.ui.text.AnnotatedString, layout: androidx.compose.ui.text.TextLayoutResult, bubbleWidePx: Float, density: androidx.compose.ui.unit.Density): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder()
    for (i in 0 until layout.lineCount) {
        val ls = layout.getLineStart(i)
        val le = layout.getLineEnd(i)
        if (le <= ls) continue
        var slice = original.subSequence(ls, le)
        if (slice.text.endsWith("\n") || slice.text.endsWith("\r")) slice = slice.subSequence(0, slice.length - 1)
        if (slice.isEmpty()) continue
        val isLast = i == layout.lineCount - 1
        val lineW = layout.getLineRight(i) - layout.getLineLeft(i)
        val chars = (le - ls).coerceAtLeast(1)
        val lastCh = if (le > ls) original.text[le - 1] else ' '
        val hardBreak = !isLast && (lastCh == '\n' || lastCh == '\r')
        val isAutoWrap = !isLast && !hardBreak && lineW >= bubbleWidePx * 0.85f
        val extraPx = if (!isAutoWrap) 0f else (bubbleWidePx - lineW).coerceAtLeast(0f)
        val lsSp = if (isAutoWrap && chars > 1 && extraPx > 0f) with(density) { (extraPx / chars / (density.density * density.fontScale)).sp } else 0.sp
        if (lsSp.value > 0f) {
            builder.withStyle(androidx.compose.ui.text.SpanStyle(letterSpacing = lsSp)) { builder.append(slice) }
        } else {
            builder.append(slice)
        }
    }
    return builder.toAnnotatedString()
}
private fun chatBubbleInk(
    chunks: List<GameChatChunk>, fallback: String, color: Color, forceColor: Boolean, highlight: String, light: Boolean,
    fontSize: TextUnit, lineHeight: TextUnit, axisFont: FontFamily,
): ChatInk {
    val useChunks = cleanItemLinkChunks(chunks).ifEmpty { listOf(GameChatChunk(text = fallback)) }
    val builder = AnnotatedString.Builder()
    val placeholders = mutableListOf<AnnotatedString.Range<Placeholder>>()
    var len = 0
    useChunks.forEachIndexed { index, chunk ->
        if (chunk.icon != null) {
            val alt = "◆"
            builder.appendInlineContent("icon-$index", alt)
            placeholders.add(AnnotatedString.Range(Placeholder(fontSize, lineHeight, PlaceholderVerticalAlign.Center), len, len + alt.length))
            len += alt.length
        } else {
                                    val text = decodeChatEntities(chunk.text.orEmpty()).trimEnd('\n', '\r', ' ', '\u00A0')
            if (text.isEmpty()) return@forEachIndexed
            val chunkColor = if (forceColor) color else (chunk.foreground?.let { val c = chatChunkColor(it); if (light) blendColor(c, Color.Black, 0.30f) else blendColor(c, Color.White, 0.28f) } ?: color)
            val spanStyle = SpanStyle(color = chunkColor, fontStyle = if (chunk.italic) FontStyle.Italic else null)
            if (text.all { it.code in 0xE000..0xF8FF }) {
                val glyphKey = "glyph-$index"
                builder.appendInlineContent(glyphKey, " ")
                placeholders.add(AnnotatedString.Range(Placeholder(fontSize, lineHeight, PlaceholderVerticalAlign.Center), len, len + 1))
                len += 1
            } else {
                builder.appendPuaAware(text, highlight, spanStyle, Color(0x66FFEB3B), axisFont)
                len += text.length
            }
        }
    }
        return ChatInk(builder.toAnnotatedString(), placeholders)
}

@Composable
private fun chatBubbleInline(chunks: List<GameChatChunk>, fallback: String, fontSize: TextUnit, lineHeight: TextUnit): Map<String, InlineTextContent> {
    val useChunks = cleanItemLinkChunks(chunks).ifEmpty { listOf(GameChatChunk(text = fallback)) }
    return buildMap {
        useChunks.forEachIndexed { index, chunk ->
            val icon = chunk.icon
            if (icon != null) {
                val linkColor = if (icon in 0xE000..0xF8FF) {
                    var prevFg: Long? = null
                    var p = index - 1
                    while (p >= 0 && useChunks[p].icon != null) p--
                    if (p >= 0) prevFg = useChunks[p].foreground
                    var nextFg: Long? = null
                    var q = index + 1
                    while (q < useChunks.size && useChunks[q].icon != null) q++
                    if (q < useChunks.size) nextFg = useChunks[q].foreground
                    (nextFg ?: prevFg)?.let { chatChunkColor(it) }
                } else null
                put("icon-$index", InlineTextContent(Placeholder(fontSize, lineHeight, PlaceholderVerticalAlign.Center)) {
                    ChatInlineIcon(icon, fontSize, linkColor)
                })
            } else {
                            val gt = chunk.text.orEmpty().trimEnd('\n', '\r', ' ', '\u00A0')
if (gt.isNotEmpty() && gt.all { it.code in 0xE000..0xF8FF }) {
                    put("glyph-$index", InlineTextContent(Placeholder(fontSize, lineHeight, PlaceholderVerticalAlign.Center)) {
                        ChatInlineIcon(gt.first().code, fontSize, null)
                    })
                }
            }
        }
    }
}

private fun chatBubbleStyle(color: Color, fontSize: TextUnit, lineHeight: TextUnit, category: ChatCategory, align: TextAlign): androidx.compose.ui.text.TextStyle =
    androidx.compose.ui.text.TextStyle(
        color = color, fontSize = fontSize, lineHeight = lineHeight, textAlign = align,
        fontStyle = FontStyle.Normal,
        fontFamily = if (category == ChatCategory.System) FontFamily.Monospace else FontFamily.Default,
        fontWeight = if (category == ChatCategory.Tell) FontWeight.Medium else FontWeight.Normal,
    )

private class TrackingTextToolbar(
    private val delegate: TextToolbar,
    private val onShown: () -> Unit,
    private val onHidden: () -> Unit,
) : TextToolbar {
    override val status: TextToolbarStatus
        get() = delegate.status
    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        onShown()
        delegate.showMenu(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
    }
    override fun hide() {
        onHidden()
        delegate.hide()
    }
}

// 与气泡一体的尾巴形状：尾巴在顶部（镜像），对方=左上、自己=右上；尾巴侧底角 8dp，其余角 14dp
private class BubbleTailShape(private val self: Boolean) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: androidx.compose.ui.unit.LayoutDirection,
        density: androidx.compose.ui.unit.Density,
    ): androidx.compose.ui.graphics.Outline {
        val r = with(density) { 14.dp.toPx() }
        val tb = with(density) { 8.dp.toPx() }
        val tailPad = with(density) { 8.dp.toPx() }
        val tail = with(density) { 6.dp.toPx() }
        val w = size.width
        val h = size.height
        val path = androidx.compose.ui.graphics.Path().apply {
            if (self) {
                val right = w - tailPad
                moveTo(r, h)
                quadraticBezierTo(0f, h, 0f, h - r)
                lineTo(0f, r)
                quadraticBezierTo(0f, 0f, r, 0f)
                lineTo((right - tail).coerceAtLeast(1f), 0f)
                quadraticBezierTo(w - tailPad * 0.6f, tail * 0.4f, w - tailPad * 0.3f, 0f)
                quadraticBezierTo(w - tailPad * 0.6f, tail * 0.7f, right, tail)
                lineTo(right, (h - tb).coerceAtLeast(1f))
                quadraticBezierTo(right, h, right - tb, h)
            } else {
                val left = tailPad
                moveTo(left + tb, h)
                quadraticBezierTo(left, h, left, h - tb)
                lineTo(left, tail)
                quadraticBezierTo(left - tail * 0.4f, tail * 0.7f, left * 0.3f, 0f)
                quadraticBezierTo(left * 0.6f, tail * 0.4f, left + tail, 0f)
                lineTo(w - r, 0f)
                quadraticBezierTo(w, 0f, w, r)
                lineTo(w, h - r)
                quadraticBezierTo(w, h, w - r, h)
            }
            close()
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}
// 发送者名：跨服时用真实图标渲染 名字<花>服务器，其余普通文本
@Composable
private fun LightSenderName(part: String, muted: Color, fontSizeSp: Int, worldIcon: Int, modifier: Modifier = Modifier) {
    val sep = '\uE05D'
    val idx = if (worldIcon > 0) part.indexOf(sep) else -1
    if (idx >= 0) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
            Text(part.substring(0, idx), color = muted, fontSize = fontSizeSp.sp)
            Box(Modifier.size((fontSizeSp + 3).dp).padding(horizontal = 1.dp)) { ChatInlineIcon(worldIcon, fontSizeSp.sp, null) }
            Text(part.substring(idx + 1).trim(), color = muted, fontSize = fontSizeSp.sp)
        }
    } else {
        Text(part, color = muted, fontSize = fontSizeSp.sp, modifier = modifier)
    }
}
@Composable
private fun LightChatBubble(author: String, message: GameChatMessage, self: Boolean, showSender: Boolean, wrapChars: Int, recipientTitle: String = "", fontSizeSp: Int = 14, neutral: Boolean = false, jobIconId: Int = 0, highlight: String = "", senderStatus: Long = 0, authorFontSizeSp: Int = 12, selectionEpoch: Int = 0, showTail: Boolean = true, senderWorldIconId: Int = 0) {
    val fontSp = fontSizeSp.coerceIn(10, 26)
    val fontUnit = fontSp.sp
    val lineUnit = (fontSp + 5).sp
    val timeUnit = (fontSp - 5).coerceAtLeast(9).sp
    val dens = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val light = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val selfEmoteFull = self && message.category == ChatCategory.Emote && author.isNotBlank() && !message.text.startsWith(author)
    val rawText = if (selfEmoteFull) author + message.text else message.text
    val cleaned = remember(rawText, author) { cleanChatText(rawText, if (selfEmoteFull) "" else author).ifBlank { " " } }
    val axisFont = remember { FontFamily(Font(R.font.ffxiv_axis)) }
    val timeText = lightClock(message.timestamp)
    val timeColor = if (self) Color.White.copy(alpha = .72f) else AetherLightMuted
    val baseColor = when {
        message.category == ChatCategory.Emote -> themeAdjustedChannelColor(EmoteChatColor)
        self -> Color.White
        else -> AetherLightText
    }
    val bubbleBg = if (self) AetherPurple else AetherLightSurface
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (self) Arrangement.End else Arrangement.Start) {
        Column(horizontalAlignment = if (self) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 310.dp)) {
            if (showSender) {
                val tagColor = (message.chunks.firstNotNullOfOrNull { it.foreground }?.let { themeAdjustedChannelColor(chatChunkColor(it)) } ?: themeAdjustedChannelColor(channelDefaultColor(message.channel)))
                val authorAnnotated = buildAnnotatedString {
                    val close = author.indexOf(']')
                    if (author.startsWith('[') && close >= 0) {
                        withStyle(SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold)) { append(author.substring(0, close + 1)) }
                        withStyle(SpanStyle(color = AetherLightMuted)) { append(" " + author.substring(close + 1).trimStart()) }
                    } else {
                        withStyle(SpanStyle(color = AetherLightMuted)) { append(author) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 3.dp)) {
                    val worldIcon = (message.senderWorldIcon ?: senderWorldIconId.takeIf { it > 0 })?.takeIf { it > 0 }
                    val statIconId = if (!self && message.senderStatusIcon != null && message.senderStatusIcon != worldIcon) message.senderStatusIcon?.takeIf { it > 0 } else null
                    val titleIcon = if (!self) (senderStatusNameIcon(message.senderStatusName) ?: senderStatus.takeIf { it != 0L }?.let { friendStatusIcon(it) }) else null
                    val tagEnd = if (author.startsWith('[')) author.indexOf(']') else -1
                    if (tagEnd >= 0) {
                        // [频道] 标签在前，状态图标放在标签与角色名之间（如 [新人频道][导芽]名字）
                        Text(author.substring(0, tagEnd + 1), color = tagColor, fontSize = authorFontSizeSp.sp, fontWeight = FontWeight.SemiBold)
                        if (statIconId != null) Box(Modifier.size((authorFontSizeSp + 3).dp).padding(horizontal = 3.dp)) { ChatInlineIcon(statIconId, authorFontSizeSp.sp, null) }
                        else if (titleIcon != null) Image(painterResource(titleIcon), contentDescription = null, modifier = Modifier.size((authorFontSizeSp + 3).dp).padding(horizontal = 3.dp))
                        LightSenderName(author.substring(tagEnd + 1).trimStart(), AetherLightMuted, authorFontSizeSp, worldIcon ?: 0, Modifier.padding(start = 2.dp))
                    } else {
                        if (statIconId != null) Box(Modifier.size((authorFontSizeSp + 3).dp).padding(end = 3.dp)) { ChatInlineIcon(statIconId, authorFontSizeSp.sp, null) }
                        else if (titleIcon != null) Image(painterResource(titleIcon), contentDescription = null, modifier = Modifier.size((authorFontSizeSp + 3).dp).padding(end = 3.dp))
                        LightSenderName(author, AetherLightMuted, authorFontSizeSp, worldIcon ?: 0, Modifier)
                    }
                    if (jobIconId > 0) RemoteGameIcon(jobIconId, "?", Modifier.size((authorFontSizeSp + 1).dp).padding(start = 3.dp))
                }
            }
            val bubbleShape = if (showTail) remember(self) { BubbleTailShape(self) } else if (self) RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp, topEnd = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 14.dp, bottomEnd = 14.dp)
            val horizPad = if (self) Modifier.padding(end = 10.dp) else Modifier.padding(start = 10.dp)
            BoxWithConstraints(
                // 有尾巴：背景覆盖尾巴+本体，padding 缩内容；无尾巴：padding 在外层，本体从第 10dp 处开始，与带尾巴的首条主体对齐
                Modifier.then(if (showTail) Modifier.clip(bubbleShape).then(Modifier.background(bubbleBg)).then(horizPad) else horizPad.then(Modifier.clip(bubbleShape)).then(Modifier.background(bubbleBg))),
            ) {
                val bubbleContent = (maxWidth - 22.dp).coerceAtLeast(40.dp)
                val contentPx = with(dens) { bubbleContent.toPx() }
                // 情感动作文字保持正体（不随消息的斜体标记走），颜色不变；缓存 chunks 避免滚动时反复分配
                val inkChunks = remember(message) { if (message.category == ChatCategory.Emote) message.chunks.map { it.copy(italic = false) } else message.chunks }
                val ink = remember(message, cleaned, baseColor, neutral, highlight, light, fontUnit, lineUnit) {
                    chatBubbleInk(inkChunks, cleaned, baseColor, neutral, highlight, light, fontUnit, lineUnit, axisFont)
                }
                val inline = chatBubbleInline(inkChunks, cleaned, fontUnit, lineUnit)
                val measureStyle = remember(message.category, baseColor, fontUnit, lineUnit) { chatBubbleStyle(baseColor, fontUnit, lineUnit, message.category, TextAlign.Start) }
                val layout = remember(ink, measureStyle, contentPx) {
                    textMeasurer.measure(ink.annotated, measureStyle, placeholders = ink.placeholders, constraints = Constraints(maxWidth = contentPx.roundToInt()))
                }
                // 先把两端对齐字距烘焙进文本，再按烘焙后的文本量取宽度，保证气泡尺寸与内容一致（避免右侧留空）
                val baseWidePx = (0 until layout.lineCount).map { layout.getLineRight(it) - layout.getLineLeft(it) }.maxOrNull()?.coerceAtLeast(1f) ?: contentPx
                val justified = remember(ink, layout, baseWidePx) { justifyText(ink.annotated, layout, baseWidePx, dens) }
                val jLayout = remember(ink, justified, measureStyle, contentPx) {
                    textMeasurer.measure(justified, measureStyle, placeholders = ink.placeholders, constraints = Constraints(maxWidth = contentPx.roundToInt()))
                }
                val lineCount = jLayout.lineCount
                val lineWidths = (0 until lineCount).map { jLayout.getLineRight(it) - jLayout.getLineLeft(it) }
                val widePx = lineWidths.maxOrNull()?.coerceAtLeast(1f) ?: contentPx
                val lastLinePx = if (lineCount > 0) lineWidths[lineCount - 1] else 0f
                val timePx = remember(timeText, timeUnit) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textSize = with(dens) { timeUnit.toPx() } }.measureText(timeText) }
                val gapPx = with(dens) { 8.dp.toPx() }
                // 时间在最后一行能容纳时内联在行尾；一行放不下才另起一行放气泡右下角
                val canInline = lastLinePx + gapPx + timePx <= contentPx
                val bubbleWidePx = if (canInline) maxOf(widePx, lastLinePx + gapPx + timePx) else widePx
                val bubbleWideDp = with(dens) { bubbleWidePx.toDp() }
                key(selectionEpoch) {
                SelectionContainer {
                Column(Modifier.padding(start = 11.dp, end = 11.dp, top = 8.dp, bottom = 3.dp).width(bubbleWideDp)) {
                    if (lineCount == 0) {
                        Text(timeText, color = timeColor, fontSize = timeUnit, lineHeight = timeUnit, maxLines = 1, softWrap = false, modifier = Modifier.align(Alignment.End))
                    } else {
                        // 单文本主体（两端对齐已烘焙）
                        Box(Modifier.fillMaxWidth()) {
                            Text(justified, style = measureStyle, inlineContent = if (inline.isEmpty()) emptyMap() else inline)
                            if (canInline) {
                                // 最后一行能容纳时，时间右对齐到气泡右下角（内联在末行）
                                Text(timeText, color = timeColor, fontSize = timeUnit, lineHeight = timeUnit, maxLines = 1, softWrap = false, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 1.dp))
                            }
                        }
                        // 塞不进时间&非空消息：时间另起一行右下
                        if (!canInline) {
                            Text(timeText, color = timeColor, fontSize = timeUnit, lineHeight = timeUnit, maxLines = 1, softWrap = false, modifier = Modifier.align(Alignment.End).padding(top = 3.dp))
                        }
                    }
                }
                }
                }
            }
        }
    }
}

private fun blendColor(c: Color, target: Color, fraction: Float): Color = Color(
    red = c.red + (target.red - c.red) * fraction,
    green = c.green + (target.green - c.green) * fraction,
    blue = c.blue + (target.blue - c.blue) * fraction,
    alpha = c.alpha,
)

@Composable
private fun themeAdjustedChannelColor(color: Color): Color {
    val light = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    return if (light) blendColor(color, Color.Black, 0.30f) else blendColor(color, Color.White, 0.28f)
}

private fun AnnotatedString.Builder.appendHighlighted(text: String, query: String, style: SpanStyle, highlight: Color) {
    if (query.isBlank()) {
        withStyle(style) { append(text) }
        return
    }
    val lower = text.lowercase()
    val q = query.lowercase()
    var from = 0
    while (true) {
        val index = lower.indexOf(q, from)
        if (index < 0) {
            withStyle(style) { append(text.substring(from)) }
            break
        }
        if (index > from) withStyle(style) { append(text.substring(from, index)) }
        withStyle(style.merge(SpanStyle(background = highlight))) { append(text.substring(index, index + q.length)) }
        from = index + q.length
    }
}

private fun AnnotatedString.Builder.appendPuaAware(text: String, query: String, style: SpanStyle, highlight: Color, axisFont: FontFamily) {
    // 把 0xE000..0xF8FF 的轴字形（如 HQ / e03c、链接符号）用 FFXIV 轴字体渲染，其余走默认字体 + 高亮。
    var i = 0
    while (i < text.length) {
        if (text[i].code in 0xE000..0xF8FF) {
            withStyle(SpanStyle(fontFamily = axisFont, color = style.color, fontStyle = style.fontStyle)) {
                append(text[i].toString())
            }
            i++
        } else {
            val start = i
            while (i < text.length && text[i].code !in 0xE000..0xF8FF) i++
            appendHighlighted(text.substring(start, i), query, style, highlight)
        }
    }
}

private fun chatChunkColor(value: Long): Color {
    val red = ((value shr 24) and 0xFF).toInt()
    val green = ((value shr 16) and 0xFF).toInt()
    val blue = ((value shr 8) and 0xFF).toInt()
    val alpha = (value and 0xFF).toInt()
    return Color(red, green, blue, alpha)
}

private fun statusIconDrawable(index: Int): Int? = when (index) {
    77 -> R.drawable.fst_new            // 新人
    78 -> R.drawable.fst_mentor         // 指导者
    79 -> R.drawable.fst_pvementor      // 战斗指导者
    80 -> R.drawable.fst_tradementor    // 制作采集指导者
    81 -> R.drawable.fst_pvpmentor      // 对战指导者
    95 -> R.drawable.fst_returner       // 回归者
    else -> null
}

@Composable
private fun ChatInlineIcon(index: Int, fontSize: TextUnit, linkColor: Color?) {
    if (index in 0xE000..0xF8FF) {
        val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
        val color = Color(0xFFFF7E1E)
        var bmp by remember(index, color) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(index, color) { bmp = renderAxisGlyph(appContext, index, color) }
        val img = bmp
        if (img != null) Image(img, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
        return
    }
    if (index >= 1000) {
        val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
        var bitmap by remember(index) { mutableStateOf<ImageBitmap?>(null) }
        LaunchedEffect(index) { bitmap = ItemIconLoader.load(appContext, index)?.asImageBitmap() }
        val bmp = bitmap
        if (bmp != null) {
            Image(bmp, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }
        return
    }
    statusIconDrawable(index)?.let { res ->
        Image(painterResource(res), contentDescription = null, modifier = Modifier.fillMaxSize().padding(1.dp), contentScale = ContentScale.Fit)
        return
    }
    val rect = fontIconRect(index) ?: return
    val bitmap = ImageBitmap.imageResource(R.drawable.fonticon_ps4)
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        drawImage(bitmap, srcOffset = androidx.compose.ui.unit.IntOffset(rect[0], rect[1]), srcSize = androidx.compose.ui.unit.IntSize(rect[2], rect[3]))
    }
}

private fun renderAxisGlyph(context: android.content.Context, code: Int, color: Color): ImageBitmap? {
    val typeface = androidx.core.content.res.ResourcesCompat.getFont(context, R.font.ffxiv_axis) ?: return null
    val glyph = code.toChar().toString()
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        this.color = color.toArgb()
        this.textSize = 72f
    }
    val bounds = android.graphics.Rect()
    paint.getTextBounds(glyph, 0, glyph.length, bounds)
    val pad = 8
    val w = (bounds.width() + pad * 2).coerceAtLeast(2)
    val h = (bounds.height() + pad * 2).coerceAtLeast(2)
    val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    canvas.drawText(glyph, (-bounds.left + pad).toFloat(), (-bounds.top + pad).toFloat(), paint)
    return bmp.asImageBitmap()
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
    // 连续同一个人（未被其它人打断）的消息合并为一组：只要上一条是不同发送者，本消息即为新组首条
    if (index == 0) return true
    fun key(message: GameChatMessage) = if (message.self || message.isFrom(selfName)) "self:${message.channel}" else "${message.channel}:${message.sender.trim()}"
    return key(messages[index]) != key(messages[index - 1])
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
        ChatCategory.Team -> Color(0xFF6FC3E8)
        ChatCategory.Tell -> Color(0xFFFFB06E)
        ChatCategory.Linkshell -> Color(0xFFBE9BE8)
        ChatCategory.FreeCompany -> Color(0xFFE8A879)
        ChatCategory.Emote -> Color(0xFFFF9CC4)
        ChatCategory.System -> Color(0xFFAAB6C4)
    }
    val darkHue = when (category) {
        ChatCategory.Public -> Color(0xFF2E7D50)
        ChatCategory.Party -> Color(0xFF3E63C4)
        ChatCategory.Team -> Color(0xFF2C6E9E)
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
