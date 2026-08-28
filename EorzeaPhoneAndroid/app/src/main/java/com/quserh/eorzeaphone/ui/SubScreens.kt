package com.quserh.eorzeaphone.ui
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.ChatCategory
import com.quserh.eorzeaphone.data.GameChatMessage
import com.quserh.eorzeaphone.data.GameInventoryItem
import com.quserh.eorzeaphone.data.ItemIconLoader
import com.quserh.eorzeaphone.data.displayPlayerName
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneAccentContainer
import com.quserh.eorzeaphone.ui.theme.PhoneBackground
import com.quserh.eorzeaphone.ui.theme.PhoneDanger
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneOnAccentContainer
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.round
import kotlinx.coroutines.launch
@Composable
fun ScreenFrame(background: Color = PhoneBackground, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
        ) {
            content()
        }
        // soft fade-in right under the status bar so the top of the content
        // blends into the system bar instead of cutting off abruptly.
        Box(
            Modifier.fillMaxWidth().height(36.dp).background(
                Brush.verticalGradient(listOf(background.copy(alpha = 0.9f), Color.Transparent))
            )
        )
    }
}
@Composable
fun ScreenHeader(title: String, state: PhoneState, trailing: (@Composable () -> Unit)? = null, onBack: (() -> Unit)? = null, showBack: Boolean = true) {
    val margin = LocalContentMargin.current
    val sidePad = (margin.coerceAtLeast(2) - 2).dp
    Box(Modifier.fillMaxWidth().padding(horizontal = sidePad, vertical = 12.dp)) {
        Box(Modifier.align(Alignment.CenterStart)) {
            if (showBack) ImageGlyph(
                R.drawable.ic_back,
                PhoneAccent,
                Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = (onBack ?: state::back)).padding(horizontal = 2.dp, vertical = 6.dp),
            ) else Spacer(Modifier.width(12.dp))
        }
        Text(
            title,
            color = PhoneText,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 50.dp),
        )
        Row(Modifier.align(Alignment.CenterEnd), horizontalArrangement = Arrangement.End) {
            trailing?.invoke()
        }
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(state: PhoneState) {
    var characterMenu by remember { mutableStateOf(false) }
    val profileName = state.profile?.name?.takeIf { it.isNotBlank() }
    val profileSubtitle = when {
        profileName != null -> {
            listOf(
                listOf(state.profile?.homeWorld, state.profile?.currentWorld).filterNotNull().filter { it.isNotBlank() }.distinct().joinToString(" · "),
            ).filter { it.isNotBlank() }.joinToString(" · ")
        }
        state.connected -> "正在读取角色资料"
        else -> "连接游戏后显示角色资料"
    }
    val avatarKey = state.currentCharacterKey
    val avatarPath = state.characterAvatar(avatarKey)
    var avatarBmp by remember(avatarPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(avatarPath) { avatarBmp = if (avatarPath.isNotBlank()) runCatching { android.graphics.BitmapFactory.decodeFile(avatarPath) }.getOrNull() else null }
    var avatarMenu by remember { mutableStateOf(false) }
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && avatarKey.isNotBlank()) {
            val path = state.savePickedAvatar(avatarKey, uri)
            if (path != null) state.setCharacterAvatar(avatarKey, path)
        }
    }
    ScreenFrame {
        ScreenHeader("设置", state, showBack = false)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                // 头像 56dp（原来 68dp，比标题重太多）。在线状态改成头像右下角的
                // 小圆点，而不是名字后面挂一个 20dp 的图——名字那一行只放名字。
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PhoneSurface).padding(16.dp),
                ) {
                    Box {
                        Box(
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(PhoneAccent)
                                .combinedClickable(onClick = {}, onLongClick = { avatarMenu = true }),
                            contentAlignment = Alignment.Center,
                        ) {
                            val current = avatarBmp
                            if (current != null) {
                                Image(current.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Text(profileName?.take(1) ?: "人", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(15.dp).clip(CircleShape).background(PhoneSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.size(10.dp).clip(CircleShape)
                                    .background(if (state.activeCharacterOnline) PhoneGreen else PhoneMuted),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            profileName ?: if (state.connected) "已连接终端" else "未连接终端",
                            color = PhoneText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text(profileSubtitle, color = PhoneMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                    }
                    // 只有真的能切角色时才给入口。原来箭头一直在，只是变灰，
                    // 点了没反应。
                    if (state.knownCharacters.size > 1) {
                        Box {
                            Box(
                                Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).clickable { characterMenu = true },
                                contentAlignment = Alignment.Center,
                            ) { ImageGlyph(R.drawable.ic_person, PhoneAccent, Modifier.size(19.dp)) }
                            DropdownMenu(expanded = characterMenu, onDismissRequest = { characterMenu = false }) {
                                state.knownCharacters.forEach { character ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(character.name, fontWeight = FontWeight.SemiBold)
                                                Text(character.world, color = PhoneMuted, fontSize = 11.sp)
                                            }
                                        },
                                        onClick = { state.switchCharacter(character.key); characterMenu = false },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item {
                // 连接钮原来写死 #AE62DA——M3 模板紫的余党，全局品牌色换过一轮它漏了。
                // 断开是破坏性操作，改成描边 + PhoneDanger；连接才是实心主色。
                val connected = state.connected
                if (connected) {
                    OutlinedButton(
                        onClick = { state.disconnect() },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PhoneDanger),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PhoneDanger),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("断开游戏连接", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                } else {
                    Button(
                        onClick = { state.connect() },
                        colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                    ) { Text("连接游戏", fontSize = 15.sp, fontWeight = FontWeight.SemiBold) }
                }
                val connHint = buildString {
                    val online = state.onlineCharacterName
                    if (online.isNotBlank()) append("已连接 · ").append(online).append(" 在线")
                    else if (state.statusMessage.isNotBlank()) append(state.statusMessage)
                }
                if (connHint.isNotBlank()) {
                    Text(
                        connHint,
                        color = if (connected) PhoneGreen else PhoneMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                    )
                }
            }
            item {
                // IP / 端口原来是两个裸 OutlinedTextField 浮在列表里，
                // 和下面那些卡片组不是一个东西。收进卡片，标题也说清楚这是干什么的。
                SettingsGroup {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = SettingsRowPad, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ImageGlyph(R.drawable.ic_link, PhoneMuted, Modifier.size(SettingsIconSize))
                        Text("游戏电脑地址", color = PhoneText, fontSize = 15.sp, modifier = Modifier.padding(start = 14.dp))
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = SettingsRowPad, end = SettingsRowPad, bottom = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = state.host, onValueChange = { state.host = it },
                            label = { Text("IP") }, singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = state.port, onValueChange = { state.port = it.filter(Char::isDigit).take(5) },
                            label = { Text("端口") }, singleLine = true,
                            modifier = Modifier.width(104.dp),
                        )
                    }
                }
            }
            item {
                // 免打扰是"现在别吵我"，和下面四个分类设置不是一回事，单独一组。
                // 原来这一组还重复放了"锁定位置/待机滑动/集体动作"——
                // 通用页和外观页里各有一份，同一个开关三个地方能改。
                SettingsGroup {
                    ToggleRow("免打扰", state.doNotDisturb, R.drawable.ic_bell_off, "静音所有消息提示") { state.doNotDisturb = it }
                }
            }
            item {
                SettingsGroup {
                    LinkRow("通用", R.drawable.ic_tune) { state.settingsPage = SettingsPage.General }
                    SettingsDivider()
                    LinkRow("外观", R.drawable.ic_palette) { state.settingsPage = SettingsPage.Appearance }
                    SettingsDivider()
                    LinkRow("声音与触感", R.drawable.ic_volume) { state.settingsPage = SettingsPage.Sound }
                    SettingsDivider()
                    LinkRow(
                        "通知",
                        R.drawable.ic_bell_on,
                        value = if (state.doNotDisturb) "免打扰" else if (state.chatNotifications || state.tellNotifications) "已开启" else "已关闭",
                    ) { state.settingsPage = SettingsPage.Notifications }
                }
            }
            item {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    Text("数据来源：${state.serverLabel}", color = PhoneMuted, fontSize = 12.sp)
                    Text("长按上方头像可更换角色头像，每个角色单独保存。", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
    if (avatarMenu) {
        AlertDialog(
            onDismissRequest = { avatarMenu = false },
            title = { Text("更换头像") },
            text = {
                Column {
                    Text("从相册选择", color = PhoneText, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { avatarMenu = false; pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(vertical = 12.dp))
                    Text("恢复默认", color = PhoneDanger, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { state.setCharacterAvatar(avatarKey, ""); avatarMenu = false }.padding(vertical = 12.dp))
                }
            },
            confirmButton = {},
        )
    }
}
// ---------------------------------------------------------------------------
// 设置页的行骨架
//
// 三个尺寸常量是这一组行唯一的真相来源。以前 ToggleRow 和 LinkRow 各写一遍：
// 一个 padding(horizontal=18) 一个 padding(start=18)，图标一个不传尺寸一个只传
// padding，行之间没有分隔线，副值和箭头的间距也各算一次。
// ---------------------------------------------------------------------------

/** 设置行高。52dp 比原来的 60dp 紧一档，一屏能多放一项，仍高于 48dp 触控下限。 */
private val SettingsRowHeight = 52.dp
/** 图标尺寸。20dp 配 15sp 的标题，视觉重量差不多。 */
private val SettingsIconSize = 20.dp
/** 行左右内边距，也是分隔线的左缩进基准。 */
private val SettingsRowPad = 16.dp

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PhoneSurface),
        content = content,
    )
}

/** 组内分隔线。缩进到文字起点，不横穿图标那一列。 */
@Composable
private fun SettingsDivider() {
    Box(
        Modifier.fillMaxWidth()
            .padding(start = SettingsRowPad + SettingsIconSize + 14.dp)
            .height(1.dp).background(MaterialTheme.colorScheme.outlineVariant),
    )
}

/**
 * 设置行骨架。图标 → 标题（+ 说明）→ 尾部控件。
 * 尾部只放一个东西：开关、值 + 箭头、或者纯箭头。
 */
@Composable
private fun SettingsRow(
    label: String,
    icon: Int,
    hint: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .heightIn(min = SettingsRowHeight)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = SettingsRowPad, vertical = 8.dp),
    ) {
        ImageGlyph(icon, PhoneMuted, Modifier.size(SettingsIconSize))
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp)) {
            Text(label, color = PhoneText, fontSize = 15.sp)
            if (hint != null) {
                Text(hint, color = PhoneMuted, fontSize = 11.sp, lineHeight = 15.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        trailing?.invoke(this)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, icon: Int, hint: String? = null, onChange: (Boolean) -> Unit) {
    SettingsRow(label, icon, hint, onClick = { onChange(!checked) }) {
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            modifier = Modifier.scale(0.85f),
        )
    }
}

@Composable
private fun LinkRow(label: String, icon: Int, value: String? = null, hint: String? = null, onClick: (() -> Unit)? = null) {
    SettingsRow(label, icon, hint, onClick = onClick) {
        if (value != null) Text(value, color = PhoneMuted, fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp))
        ImageGlyph(
            R.drawable.ic_chevron_right,
            if (onClick != null) PhoneMuted else PhoneSurfaceRaised,
            Modifier.size(17.dp),
        )
    }
}

/**
 * 加减步进器。通用页的"保留消息上限"和外观页的"左右边距"原来各写一遍，
 * 连按钮圆角都不一样。
 */
@Composable
private fun SettingsStepper(
    label: String,
    icon: Int,
    value: String,
    hint: String? = null,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    SettingsRow(label, icon, hint) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(PhoneSurfaceRaised).clickable(onClick = onMinus),
            contentAlignment = Alignment.Center,
        ) { ImageGlyph(R.drawable.ic_remove, PhoneAccent, Modifier.size(16.dp)) }
        Text(
            value,
            color = PhoneText, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 54.dp).padding(horizontal = 4.dp),
        )
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(PhoneSurfaceRaised).clickable(onClick = onPlus),
            contentAlignment = Alignment.Center,
        ) { ImageGlyph(R.drawable.ic_add, PhoneAccent, Modifier.size(16.dp)) }
    }
}
@Composable
fun SettingsSubScreen(state: PhoneState) {
    when (state.settingsPage) {
        SettingsPage.General -> GeneralSettingsScreen(state)
        SettingsPage.Appearance -> AppearanceSettingsScreen(state)
        SettingsPage.Sound -> SoundSettingsScreen(state)
        SettingsPage.Notifications -> NotificationsSettingsScreen(state)
        null -> SettingsScreen(state)
    }
}
@Composable
private fun SettingsSubLayout(title: String, state: PhoneState, content: @Composable ColumnScope.() -> Unit) {
    ScreenFrame {
        ScreenHeader(title, state)
        // 加了说明文案之后内容会超出一屏（通用页 3 个开关各带一行说明就到底了），
        // 原来是定高 Column，多出来的部分直接被裁掉。
        Column(
            modifier = Modifier.fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = (10 + LocalContentMargin.current).dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) { content() }
    }
}
@Composable
private fun GeneralSettingsScreen(state: PhoneState) {
    SettingsSubLayout("通用", state) {
        SectionLabel("手机行为")
        SettingsGroup {
            ToggleRow("待机时滑动手机", state.screenSwipe, R.drawable.ic_swipe, "锁屏待机时手机会随呼吸轻微浮动") { state.screenSwipe = it }
            SettingsDivider()
            ToggleRow("集体动作时显示", state.showEmotes, R.drawable.ic_group, "做集体动作时不自动收起手机") { state.showEmotes = it }
            SettingsDivider()
            ToggleRow("锁定位置", state.lockPosition, R.drawable.ic_lock, "锁住手机在屏幕上的位置，避免误拖") { state.lockPosition = it }
        }
        SectionLabel("聊天记录")
        SettingsGroup {
            SettingsStepper(
                "保留消息上限",
                R.drawable.ic_history,
                if (state.chatRetentionLimit == 0) "不限" else state.chatRetentionLimit.toString(),
                hint = "每个角色保留最近 N 条，超出自动清理最旧的；0 = 永久保留",
                onMinus = { state.chatRetentionLimit = (state.chatRetentionLimit - 500).coerceAtLeast(0) },
                onPlus = { state.chatRetentionLimit = (state.chatRetentionLimit + 500).coerceAtMost(50000) },
            )
        }
    }
}
@Composable
private fun AppearanceSettingsScreen(state: PhoneState) {
    SettingsSubLayout("外观", state) {
        SectionLabel("主题")
        SettingsGroup {
            PhoneThemeMode.entries.forEachIndexed { index, mode ->
                if (index > 0) SettingsDivider()
                val on = state.themeMode == mode
                SettingsRow(mode.label, R.drawable.ic_contrast, onClick = { state.themeMode = mode }) {
                    ImageGlyph(
                        if (on) R.drawable.ic_radio_on else R.drawable.ic_radio_off,
                        if (on) PhoneAccent else PhoneMuted,
                        Modifier.size(20.dp),
                    )
                }
            }
        }
        SectionLabel("布局")
        SettingsGroup {
            SettingsStepper(
                "左右边距",
                R.drawable.ic_margins,
                "${state.contentMargin}",
                hint = "两侧同时向内收缩，数值越小越贴近屏幕边缘",
                onMinus = { state.contentMargin -= 2 },
                onPlus = { state.contentMargin += 2 },
            )
            SettingsDivider()
            ToggleRow("紧凑程序坞", state.compactDock, R.drawable.ic_grid, "底部程序坞排得更密，露出更多桌面") { state.compactDock = it }
        }
        SectionLabel("动效与屏幕")
        SettingsGroup {
            ToggleRow("减弱动态效果", state.reducedMotion, R.drawable.ic_motion, "关掉按压缩放、骨架微光等过渡动画") { state.reducedMotion = it }
            SettingsDivider()
            ToggleRow("保持屏幕常亮", state.keepScreenOn, R.drawable.ic_brightness, "看攻略或等窗口时屏幕不自动熄灭") { state.keepScreenOn = it }
        }
    }
}
@Composable
private fun SoundSettingsScreen(state: PhoneState) {
    SettingsSubLayout("声音与触感", state) {
        SettingsGroup {
            ToggleRow("消息提示音", state.chatNotifications, R.drawable.ic_volume, "收到消息时响一声") { state.chatNotifications = it }
            SettingsDivider()
            ToggleRow("触觉反馈", state.haptics, R.drawable.ic_vibrate, "点按钮和切换标签时轻震一下") { state.haptics = it }
        }
    }
}
@Composable
private fun NotificationsSettingsScreen(state: PhoneState) {
    SettingsSubLayout("通知", state) {
        SectionLabel("消息")
        SettingsGroup {
            ToggleRow("聊天消息", state.chatNotifications, R.drawable.ic_chat, "群聊和频道消息推送到系统通知栏") { state.chatNotifications = it; if (it) state.requestNotificationPermission() }
            SettingsDivider()
            ToggleRow("私聊消息", state.tellNotifications, R.drawable.ic_bell_on, "有人私聊时单独提醒") { state.tellNotifications = it; if (it) state.requestNotificationPermission() }
        }
        SectionLabel("游戏内提醒")
        SettingsGroup {
            ToggleRow("重置提醒", state.resetNotifications, R.drawable.ic_timer, "日常、周常和探险札记重置前提醒") { state.resetNotifications = it }
        }
        SectionLabel("免打扰")
        SettingsGroup {
            ToggleRow("全部静音", state.doNotDisturb, R.drawable.ic_bell_off, "盖过上面所有开关，一条都不推") { state.doNotDisturb = it }
        }
        Text(
            "推送需要系统通知权限，开启任一消息通知时会请求授权。",
            color = PhoneMuted, fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
    }
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContactsTab(state: PhoneState) {
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var friendsTab by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val filtered = state.friends.filter { it.name.contains(query, ignoreCase = true) }
    LaunchedEffect(searching) { if (searching) { focusRequester.requestFocus(); keyboard?.show() } }
    Column(Modifier.fillMaxSize()) {
        // header row: title on the left; a magnifier opens an inline search with the
        // keyboard up, so no tall persistent search box takes up space.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 14.dp, top = 8.dp, bottom = 2.dp),
        ) {
            Text("联系人", color = PhoneText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (searching) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索联系人", color = PhoneMuted, fontSize = 12.sp) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.width(150.dp).focusRequester(focusRequester),
                )
                ImageGlyph(R.drawable.ic_close, PhoneMuted, Modifier.clickable { searching = false; query = ""; keyboard?.hide() }.padding(start = 10.dp).size(19.dp))
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(38.dp).clip(CircleShape).clickable { searching = true },
                ) {
                    ImageGlyph(R.drawable.ic_search, PhoneAccent, Modifier.size(19.dp))
                }
            }
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item("segmented") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp).clip(CircleShape).background(Color(0xFF424148)), verticalAlignment = Alignment.CenterVertically) {
                    listOf("好友" to true, "所有人" to false).forEach { (label, value) ->
                        Text(label, color = if (friendsTab == value) Color.White else PhoneMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.weight(1f).clip(CircleShape).background(if (friendsTab == value) PhoneAccent else Color.Transparent).combinedClickable(onClick = { friendsTab = value }, onLongClick = { state.refreshFriends() }).padding(vertical = 6.dp))
                    }
                }
            }
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
    // 组标题跟着下面那张卡走：上间距大、下间距小，读起来才是"这一段的标题"。
    Text(
        text,
        color = PhoneMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.4.sp,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
    )
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
        ImageGlyph(R.drawable.ic_chevron_right, PhoneMuted, Modifier.size(17.dp))
    }
}
@Composable
private fun MessagesBottomNav(pager: androidx.compose.foundation.pager.PagerState) {
    val scope = rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth().height(62.dp).background(PhoneBackground).padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MessagesNavItem("聊天", R.drawable.app_messages, pager.currentPage == 0, Modifier.weight(1f)) { scope.launch { pager.animateScrollToPage(0) } }
        MessagesNavItem("联系人", R.drawable.app_contacts, pager.currentPage == 1, Modifier.weight(1f)) { scope.launch { pager.animateScrollToPage(1) } }
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
                ImageGlyph(R.drawable.ic_chevron_right, PhoneMuted, Modifier.size(18.dp))
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
    var selectedRetainerId by remember { mutableStateOf<Long?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    val groups = listOf("bags" to "背包", "armoury" to "兵装库", "saddle" to "陆行鸟鞍囊", "equipped" to "当前装备", "retainers" to "雇员", "company" to "部队仓库", "housing" to "房屋仓库")
    val selectedTypes = inventoryTypesForGroup(selectedGroup ?: "bags")
    val filtered = state.inventory.filter {
        (selectedGroup == null || it.container in selectedTypes) &&
            (selectedGroup != "retainers" || selectedRetainerId == null || it.retainerId == selectedRetainerId) &&
            (query.isBlank() || it.name.contains(query, true))
    }
    // system back inside a sub-stock collapses back to the inventory hub rather
    // than popping the whole inventory screen.
    BackHandler(enabled = showSearch || selectedGroup != null) {
        if (showSearch) { showSearch = false; query = "" } else { selectedGroup = null; selectedRetainerId = null; query = "" }
    }
    Box(Modifier.fillMaxSize().background(PhoneBackground)) {
    ScreenFrame(background = Color.Transparent) {
        val inventoryTitle = selectedRetainerId?.let { id -> state.retainers.firstOrNull { it.id == id }?.name } ?: groups.firstOrNull { it.first == selectedGroup }?.second ?: "物品栏"
        ScreenHeader(inventoryTitle, state,
            trailing = { Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${formatCount(state.inventory.size)} 件", color = PhoneMuted, fontSize = 12.sp)
                Box(Modifier.padding(start = 9.dp).size(34.dp).clip(RoundedCornerShape(7.dp)).background(PhoneSurfaceRaised).clickable {
                    showSearch = !showSearch
                    if (!showSearch) query = ""
                }, contentAlignment = Alignment.Center) { ImageGlyph(R.drawable.ic_search, PhoneAccent, Modifier.size(20.dp)) }
            } },
            onBack = if (selectedGroup == null) null else ({ selectedGroup = null; selectedRetainerId = null; query = "" }),
            showBack = selectedGroup != null)
        if (state.inventoryLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = PhoneAccent,
                trackColor = PhoneSurfaceRaised,
            )
        }
        if (state.inventory.isEmpty()) {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (state.connected) {
                    CircularProgressIndicator(color = PhoneAccent, modifier = Modifier.size(34.dp), strokeWidth = 3.dp)
                    Text("正在读取背包数据…", color = PhoneMuted, modifier = Modifier.padding(top = 14.dp))
                    Text("背包数据通过加密端口同步", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Text("请先连接游戏插件", color = PhoneMuted)
                    Text("背包数据通过加密端口同步", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        } else if (selectedGroup == null && query.isBlank()) {
            InventoryHub(state, open = { selectedGroup = it }, openRetainer = { id -> selectedRetainerId = id; selectedGroup = "retainers" })
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 12.dp)) {
                items(filtered.sortedWith(compareBy({ it.container }, { it.slot })), key = { "${it.container}-${it.slot}-${it.itemId}" }) { item -> InventorySearchRow(item) }
                item("inventory-end") { Spacer(Modifier.height(18.dp)) }
            }
        }
    }
    if (showSearch) {
        CompactInventorySearch(
            query,
            { query = it },
            Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.statusBars)
                .padding(top = 66.dp, start = 30.dp, end = 30.dp).zIndex(2f),
        )
    }
    }
}
@Composable
private fun CompactInventorySearch(value: String, change: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { focusRequester.requestFocus(); keyboard?.show() }
    BasicTextField(
        value = value,
        onValueChange = change,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = PhoneText, fontSize = 14.sp),
        modifier = modifier.fillMaxWidth().height(44.dp).focusRequester(focusRequester)
            .clip(RoundedCornerShape(11.dp)).background(PhoneSurfaceRaised),
        decorationBox = { field ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 13.dp)) {
                ImageGlyph(R.drawable.ic_search, PhoneMuted, Modifier.padding(end = 10.dp).size(19.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) Text("搜索物品", color = PhoneMuted, fontSize = 14.sp)
                    field()
                }
            }
        },
    )
}
@Composable
private fun InventoryHub(state: PhoneState, open: (String) -> Unit, openRetainer: (Long) -> Unit) {
    val localTypes = inventoryTypesForGroup("bags") + inventoryTypesForGroup("armoury") + inventoryTypesForGroup("saddle") + inventoryTypesForGroup("equipped")
    val total = state.inventory.filter { it.container in localTypes }.sumOf { it.quantity }
    val rows = listOf(
        Triple("bags", "兵装库与背包", R.drawable.app_inventory) to Color(0xFFC68731),
        Triple("armoury", "兵装库", R.drawable.app_muster) to Color(0xFF4F8DE8),
        Triple("equipped", "已装备", R.drawable.app_jobs) to Color(0xFF48B87D),
    )
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PhoneSurface).padding(vertical = 16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Text("Gil", color = Color(0xFFFFB74D), fontSize = 12.sp); Text(state.wallet?.gil?.let(::formatCount) ?: "--", color = PhoneText, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("金币", color = PhoneMuted, fontSize = 10.sp) }
                Divider(Modifier.height(54.dp).width(1.dp), color = PhoneMuted.copy(alpha = .25f))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) { Text("携带物品", color = PhoneText, fontSize = 12.sp); Text(if (state.inventory.isEmpty()) "--" else formatCount(total), color = PhoneText, fontSize = 21.sp, fontWeight = FontWeight.Bold); Text("总数量", color = PhoneMuted, fontSize = 10.sp) }
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
                        Text(formatCount(count), color = color, fontWeight = FontWeight.Bold, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(color.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 5.dp))
                        ImageGlyph(R.drawable.ic_chevron_right, PhoneMuted, Modifier.padding(start = 9.dp).size(17.dp))
                    }
                    if (index < rows.lastIndex) Divider(Modifier.padding(start = 78.dp), color = Color(0x22333333))
                }
            }
        }
        item { SectionLabel("存放于别处") }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneSurface)) {
                if (state.retainers.isEmpty()) {
                    Column(Modifier.fillMaxWidth().clickable { open("retainers") }.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Text("雇员", color = PhoneText, fontSize = 15.sp)
                        Text("在侍从铃处打开一次雇员，将其内容保存到这里", color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Divider(Modifier.padding(start = 16.dp), color = PhoneMuted.copy(alpha = .16f))
                } else {
                    state.retainers.forEach { retainer ->
                        val cachedCount = state.inventory.count { it.retainerId == retainer.id }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable(enabled = cachedCount > 0) { openRetainer(retainer.id) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFF8D6AC8)), contentAlignment = Alignment.Center) { Text(retainer.name.take(1), color = Color.White, fontWeight = FontWeight.Bold) }
                            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                                Text(retainer.name.ifBlank { "未命名雇员" }, color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                if (retainer.ventureId > 0) {
                                    val remaining = ((retainer.ventureCompleteUnix * 1000L - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
                                    Text(if (remaining == 0L) "探险已完成，可收取" else "探险中 · ${countdownLabel(remaining)}", color = if (remaining == 0L) Color(0xFF4CD487) else PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${formatCount(cachedCount)} 格", color = if (retainer.active) PhoneAccent else PhoneMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${formatCount(retainer.gil)} 金币", color = Color(0xFFFFB74D), fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }
                        Divider(Modifier.padding(start = 73.dp), color = PhoneMuted.copy(alpha = .16f))
                    }
                }
                listOf(
                    Triple("company", "部队仓库", "在游戏中打开部队仓库后同步"),
                    Triple("housing", "房屋仓库", "在房屋保管箱中打开一次后同步"),
                ).forEachIndexed { index, (id, title, subtitle) ->
                    Column(Modifier.fillMaxWidth().clickable { open(id) }.padding(horizontal = 16.dp, vertical = 14.dp)) { Text(title, color = PhoneText, fontSize = 15.sp); Text(subtitle, color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) }
                    if (index == 0) Divider(Modifier.padding(start = 16.dp), color = PhoneMuted.copy(alpha = .16f))
                }
            }
        }
    }
}
private fun inventoryTypesForGroup(group: String): List<Long> = when (group) {
    "bags" -> listOf(0, 1, 2, 3)
    "armoury" -> listOf(3500, 3200, 3201, 3202, 3203, 3204, 3205, 3206, 3207, 3208, 3209, 3300, 3400)
    "crystals" -> emptyList()
    "saddle" -> listOf(4000, 4001, 4100, 4101)
    "equipped" -> listOf(1000)
    "retainers" -> listOf(10000, 10001, 10002, 10003, 10004, 10005, 10006)
    "company" -> listOf(20000, 20001, 20002, 20003, 20004)
    "housing" -> listOf(27000, 27001, 27002, 27003, 27004, 27005, 27006, 27007, 27008, 27009, 27010, 27011, 27200)
    else -> emptyList()
}
private fun inventoryContainerName(type: Long): String = when (type) {
    0L -> "物品栏第 1 页"; 1L -> "物品栏第 2 页"; 2L -> "物品栏第 3 页"; 3L -> "物品栏第 4 页"
    1000L -> "当前装备"; 2001L -> "水晶"
    3500L -> "兵装库 · 主手"; 3200L -> "兵装库 · 副手"; 3201L -> "兵装库 · 头部"; 3202L -> "兵装库 · 身体"
    3203L -> "兵装库 · 手部"; 3204L -> "兵装库 · 腰部"; 3205L -> "兵装库 · 腿部"; 3206L -> "兵装库 · 脚部"
    3207L -> "兵装库 · 耳饰"; 3208L -> "兵装库 · 项链"; 3209L -> "兵装库 · 手镯"; 3300L -> "兵装库 · 戒指"; 3400L -> "兵装库 · 灵魂水晶"
    4000L -> "陆行鸟鞍囊第 1 页"; 4001L -> "陆行鸟鞍囊第 2 页"; 4100L -> "高级鞍囊第 1 页"; 4101L -> "高级鞍囊第 2 页"
    10000L -> "雇员背包第 1 页"; 10001L -> "雇员背包第 2 页"; 10002L -> "雇员背包第 3 页"; 10003L -> "雇员背包第 4 页"
    10004L -> "雇员背包第 5 页"; 10005L -> "雇员背包第 6 页"; 10006L -> "雇员背包第 7 页"; 11000L -> "雇员当前装备"; 12001L -> "雇员水晶"
    20000L -> "部队仓库第 1 页"; 20001L -> "部队仓库第 2 页"; 20002L -> "部队仓库第 3 页"; 20003L -> "部队仓库第 4 页"; 20004L -> "部队仓库第 5 页"; 22001L -> "部队水晶"
    27000L -> "屋外储物柜"; 27001L -> "屋内储物柜第 1 页"; 27002L -> "屋内储物柜第 2 页"; 27003L -> "屋内储物柜第 3 页"; 27004L -> "屋内储物柜第 4 页"
    27005L -> "屋内储物柜第 5 页"; 27006L -> "屋内储物柜第 6 页"; 27007L -> "屋内储物柜第 7 页"; 27008L -> "屋内储物柜第 8 页"; 27009L -> "屋内储物柜第 9 页"
    27010L -> "屋内储物柜第 10 页"; 27011L -> "屋内储物柜第 11 页"; 27200L -> "屋外储物柜第 2 页"
    else -> "容器 $type"
}
private fun defaultContainerSize(type: Long): Int = when (type) {
    in 0L..3L -> 35
    2001L -> 18
    1000L -> 14
    3300L, 3500L -> 50
    3400L -> 30
    4000L, 4001L, 4100L, 4101L -> 35
    in 10000L..10006L, in 20000L..20004L, in 27001L..27011L -> 50
    11000L, 12001L, 22001L -> 18
    27000L, 27200L -> 50
    else -> 35
}
@Composable
private fun InventorySlotCell(item: GameInventoryItem?, modifier: Modifier = Modifier) {
    Box(modifier.aspectRatio(1f).clip(RoundedCornerShape(5.dp)).background(if (item?.hq == true) Color(0xFF67522B) else PhoneSurface), contentAlignment = Alignment.Center) {
        if (item != null) {
            ItemIcon(item.iconId, Modifier.fillMaxSize(), fallback = item.name.take(3))
        }
    }
}
@Composable
internal fun ItemIcon(iconId: Int, modifier: Modifier = Modifier, fallback: String = "", tint: Color = Color.White) {
    var bitmap by remember(iconId) { mutableStateOf(ItemIconLoader.peek(iconId)) }
    val app = LocalContext.current.applicationContext
    LaunchedEffect(iconId) {
        if (iconId > 0) {
            val cached = ItemIconLoader.peek(iconId)
            if (cached != null) bitmap = cached else bitmap = ItemIconLoader.load(app, iconId)
        }
    }
    val bmp = bitmap
    if (bmp != null) {
        Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = modifier)
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            if (fallback.isNotBlank()) Text(fallback, color = tint, fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(4.dp))
        }
    }
}
@Composable
private fun InventorySearchRow(item: GameInventoryItem) {
    Row(Modifier.fillMaxWidth().background(PhoneSurface).padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        InventorySlotCell(item, Modifier.size(49.dp))
        Text(item.name, color = PhoneText, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 16.dp))
        if (item.hq) Text("HQ", color = Color(0xFFFFC071), fontSize = 9.sp, modifier = Modifier.clip(RoundedCornerShape(3.dp)).background(Color(0xFF9A4D13)).padding(horizontal = 5.dp, vertical = 3.dp))
        Text("×${"%,d".format(item.quantity)}", color = Color(0xFFC7681C), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
    }
    Divider(Modifier.padding(horizontal = 20.dp), color = Color(0x1AFFFFFF))
}
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(state: PhoneState) {
    val profile = state.profile
    val avatarKey = state.currentCharacterKey
    val avatarPath = state.characterAvatar(avatarKey)
    var avatarBmp by remember(avatarPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(avatarPath) { avatarBmp = if (avatarPath.isNotBlank()) runCatching { android.graphics.BitmapFactory.decodeFile(avatarPath) }.getOrNull() else null }
    var avatarMenu by remember { mutableStateOf(false) }
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && avatarKey.isNotBlank()) {
            val path = state.savePickedAvatar(avatarKey, uri)
            if (path != null) state.setCharacterAvatar(avatarKey, path)
        }
    }
    ScreenFrame {
        ScreenHeader("角色", state)
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(PhoneSurface).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(PhoneAccent)
                            .combinedClickable(onClick = {}, onLongClick = { avatarMenu = true }),
                        contentAlignment = Alignment.Center,
                    ) {
                        val current = avatarBmp
                        if (current != null) {
                            Image(current.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Text(profile?.name?.take(1) ?: "?", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(profile?.name ?: "等待角色资料", color = PhoneText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(profile?.currentWorld?.let { "${profile.homeWorld} · 当前世界 $it" } ?: "角色资料由游戏插件提供", color = PhoneMuted)
                        Text(profile?.location ?: "", color = PhoneAccent, fontSize = 13.sp)
                        if (profile != null && profile.jobName.isNotBlank()) Text("${profile.jobName} · Lv.${profile.level}", color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Text("长按头像可更换角色头像（不同角色独立保存）", color = PhoneMuted, fontSize = 12.sp)
            Text("职业与状态", color = PhoneText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(if (state.connected) "打开职业或角色页面后，插件会继续补充当前职业、部队和状态。" else "连接游戏后可查看当前角色。", color = PhoneMuted, fontSize = 14.sp)
        }
    }
    if (avatarMenu) {
        AlertDialog(
            onDismissRequest = { avatarMenu = false },
            title = { Text("更换头像") },
            text = {
                Column {
                    Text("从相册选择", color = PhoneText, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { avatarMenu = false; pickAvatar.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(vertical = 12.dp))
                    Text("恢复默认", color = PhoneDanger, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().clickable { state.setCharacterAvatar(avatarKey, ""); avatarMenu = false }.padding(vertical = 12.dp))
                }
            },
            confirmButton = {},
        )
    }
}
@Composable
fun SkywatcherScreen(state: PhoneState) {
    val weather = state.weather
    val bell = weather?.forecast?.firstOrNull()?.eorzeaBell ?: 12
    val visual = phoneWeatherVisual(weather?.current.orEmpty(), bell)
    Box(Modifier.fillMaxSize()) {
        WeatherBackdrop(weather?.current.orEmpty(), bell, Modifier.fillMaxSize())
        ScreenFrame(background = Color.Transparent) {
            ScreenHeader("天气预报", state)
            if (weather == null) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (state.connected) "等待区域天气…" else "连接游戏后显示天气", color = visual.ink)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Column(Modifier.fillMaxWidth().padding(vertical = 25.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            ImageGlyph(weatherIcon(weather.current), visual.ink, Modifier.size(72.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(weather.current, color = visual.ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
                            Text(weather.zone, color = visual.ink.copy(alpha = .74f), fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                            Text("艾欧泽亚时 ${String.format("%02d:00", bell)}", color = visual.ink.copy(alpha = .62f), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    item { Text("未来数小时", color = visual.ink.copy(alpha = .72f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    itemsIndexed(weather.forecast, key = { index, window -> "$index-${window.eorzeaBell}-${window.minutesFromNow}-${window.name}" }) { _, window ->
                        val rowVisual = phoneWeatherVisual(window.name, window.eorzeaBell)
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(rowVisual.ink.copy(alpha = .12f)).padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(43.dp).clip(CircleShape).background(Brush.verticalGradient(listOf(rowVisual.top, rowVisual.bottom))), contentAlignment = Alignment.Center) {
                                ImageGlyph(weatherIcon(window.name), rowVisual.ink, Modifier.size(21.dp))
                            }
                            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                                Text(window.name, color = visual.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text(if (window.minutesFromNow <= 0) "现在" else "${window.minutesFromNow} 分钟后", color = visual.ink.copy(alpha = .64f), fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                            Text(String.format("%02d:00", window.eorzeaBell), color = visual.ink.copy(alpha = .72f), fontSize = 12.sp)
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
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
private fun calculateSimple(input: String): String {
    val operatorIndex = input.indices.drop(1).lastOrNull { input[it] in charArrayOf('+', '−', '×', '÷') } ?: return input
    val left = input.substring(0, operatorIndex).toDoubleOrNull() ?: return "错误"
    val right = input.substring(operatorIndex + 1).toDoubleOrNull() ?: return "错误"
    val result = when (input[operatorIndex]) { '+' -> left + right; '−' -> left - right; '×' -> left * right; '÷' -> if (right == 0.0) return "错误" else left / right; else -> return "错误" }
    return if (result % 1.0 == 0.0) result.toLong().toString() else result.toString().take(14)
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
fun MessagesScreen(state: PhoneState) {
    val openConv = state.conversations.firstOrNull { it.key == state.openConversationKey }
    if (openConv != null) {
        ConversationDetailScreen(state, openConv)
        return
    }
    val pager = rememberPagerState(initialPage = if (state.messagesTab) 1 else 0, pageCount = { 2 })
    LaunchedEffect(pager.currentPage) { state.messagesTab = pager.currentPage == 1 }
    ScreenFrame {
        HorizontalPager(state = pager, modifier = Modifier.weight(1f), key = { it }, userScrollEnabled = true) { page ->
            if (page == 0) ConversationListTab(state)
            else ContactsTab(state)
        }
        MessagesBottomNav(pager)
    }
}
@Composable
private fun ConversationListTab(state: PhoneState) {
    var filterEditor by remember { mutableStateOf(false) }
    var filterName by remember { mutableStateOf("") }
    var filterCategories by remember { mutableStateOf(setOf<ChatCategory>()) }
    val activeFilter = state.chatFilters.firstOrNull { it.id == state.selectedChatFilterId } ?: state.chatFilters.first()
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("聊天", state, showBack = false)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            state.chatFilters.forEach { filter ->
                Text(filter.label, color = if (filter.id == state.selectedChatFilterId) Color.White else PhoneMuted, fontSize = 12.sp,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(if (filter.id == state.selectedChatFilterId) PhoneAccent else PhoneSurface).clickable { state.selectedChatFilterId = filter.id }.padding(horizontal = 10.dp, vertical = 7.dp))
            }
            ImageGlyph(R.drawable.ic_add, PhoneAccent, Modifier.clickable { filterEditor = true }.padding(horizontal = 4.dp).size(19.dp))
        }
        if (state.conversations.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (state.connected) "等待聊天消息…" else "请先连接游戏插件", color = PhoneMuted)
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp)) {
                val visibleConvs = state.conversations.filter { it.category in activeFilter.categories }
                if (visibleConvs.isEmpty()) {
                    item { Text("该标签暂无会话", color = PhoneMuted, modifier = Modifier.padding(20.dp)) }
                }
                items(visibleConvs, key = { it.key }) { conv -> Box(Modifier.animateItem()) { ConversationRow(conv, state) } }
            }
        }
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
                            TextButton(onClick = { state.removeChatFilter(custom) }) { Text("删除", color = PhoneDanger) }
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
private fun ConversationRow(conv: ChatConversation, state: PhoneState) {
    val last = conv.lastMessage
    val preview = when {
        last == null -> "暂无消息"
        last.isFrom(state.profile?.name) -> "我：${last.text}"
        else -> "${last.sender.displayPlayerName()}：${last.text}"
    }.replace('\n', ' ')
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { state.openConversation(conv) }.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Box(Modifier.size(50.dp).clip(CircleShape).background(convColor(conv.category)), contentAlignment = Alignment.Center) {
                Text(conv.title.take(1), color = Color.White, fontSize = 19.sp)
            }
            Column(Modifier.weight(1f).padding(start = 13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(conv.title, color = PhoneText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    conv.lastTimestamp?.let { Text(formatTalkTime(it), color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(start = 8.dp)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 5.dp)) {
                    Text(preview, color = PhoneMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (!conv.notify) ImageGlyph(R.drawable.ic_bell_off, PhoneMuted, Modifier.padding(start = 8.dp).size(13.dp))
                    if (conv.unread > 0) {
                        Box(Modifier.padding(start = 8.dp).size(width = 22.dp, height = 22.dp).clip(CircleShape).background(PhoneDanger), contentAlignment = Alignment.Center) {
                            Text(if (conv.unread > 99) "99+" else conv.unread.toString(), color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
            ImageGlyph(R.drawable.ic_chevron_right, PhoneMuted, Modifier.padding(start = 8.dp).size(17.dp))
        }
        Box(Modifier.fillMaxWidth().padding(start = 79.dp).height(1.dp).background(Color.White.copy(alpha = 0.06f)))
    }
}
@Composable
private fun ConversationDetailScreen(state: PhoneState, conv: ChatConversation) {
    var channelMenu by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val sendMessage = {
        if (state.connected && state.chatDraft.isNotBlank()) {
            state.sendToConversation(conv, state.chatDraft)
            focusManager.clearFocus()
        }
    }
    ScreenFrame {
        ScreenHeader(conv.title, state, trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 原来是 🔔/🔕 两个 emoji：各家系统画法不同，还是彩色的，
                // 和这一排纯色图标完全不搭。
                ImageGlyph(
                    if (conv.notify) R.drawable.ic_bell_on else R.drawable.ic_bell_off,
                    if (conv.notify) PhoneAccent else PhoneMuted,
                    Modifier.clickable { state.toggleConversationNotify(conv) }.padding(6.dp).size(19.dp),
                )
                if (conv.category != ChatCategory.Tell) {
                    Box {
                        TextButton(onClick = { channelMenu = true }, enabled = state.connected, contentPadding = ButtonDefaults.TextButtonContentPadding) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(state.currentChannelName, color = PhoneText, fontSize = 13.sp)
                                ImageGlyph(R.drawable.ic_chevron_down, PhoneText, Modifier.padding(start = 3.dp).size(14.dp))
                            }
                        }
                        DropdownMenu(expanded = channelMenu, onDismissRequest = { channelMenu = false }) {
                            outputChannels.forEach { channel ->
                                DropdownMenuItem(text = { Text(channel.label) }, onClick = { state.changeChannel(channel); channelMenu = false })
                            }
                        }
                    }
                }
            }
        })
        if (conv.messages.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    when {
                        !state.connected -> "请先连接游戏插件"
                        conv.category == ChatCategory.Tell -> "开始和 ${conv.title} 聊天吧"
                        else -> "该频道暂无消息"
                    },
                    color = PhoneMuted,
                )
            }
        } else {
            LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(conv.messages, key = { i, chat -> "$i-${chat.timestamp}-${chat.sender}-${chat.text}" }) { i, chat ->
                    val isSelf = chat.self || chat.isFrom(state.profile?.name)
                    val name = if (isSelf) (state.profile?.name?.takeIf { it.isNotBlank() } ?: "我") else chat.sender.ifBlank { conv.category.label }
                    val showSender = computeShowSender(conv.messages, i, state.profile?.name)
                    ChatBubble(name, chat.text, isSelf, chat.timestamp, state.chatWrapChars, showSender)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            OutlinedTextField(
                value = state.chatDraft,
                onValueChange = { state.chatDraft = it },
                placeholder = { Text(if (conv.category == ChatCategory.Tell) "发消息给 ${conv.title}" else "输入消息", color = PhoneMuted) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage() }),
            )
            TextButton(onClick = sendMessage, enabled = state.connected && state.chatDraft.isNotBlank()) { Text("发送", color = PhoneAccent) }
        }
    }
}
private fun ChatConversation.displayChannelHint(): String = if (tellRecipient.isNotBlank()) {
    tellRecipient.displayPlayerName()
} else {
    title
}
private fun convColor(category: ChatCategory): Color = when (category) {
    ChatCategory.Public -> Color(0xFF4C9F70)
ChatCategory.Party -> Color(0xFF5B8DEF)
    ChatCategory.Team -> Color(0xFF3FA9D9)
    ChatCategory.Tell -> Color(0xFFE08A3C)
    ChatCategory.Linkshell -> Color(0xFF9B6BC4)
    ChatCategory.FreeCompany -> Color(0xFFC0783F)
    ChatCategory.Emote -> Color(0xFFE26D8A)
    ChatCategory.System -> Color(0xFF6E7B8C)
}
private fun formatTalkTime(timestamp: Long): String {
    val time = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault())
    return if (time.toLocalDate() == LocalDate.now()) {
        time.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        time.format(DateTimeFormatter.ofPattern("M/d"))
    }
}
@Composable
private fun computeShowSender(messages: List<GameChatMessage>, i: Int, selfName: String?): Boolean {
    if (i <= 0) return true
    val cur = messages[i]
    val prev = messages[i - 1]
    val curSelf = cur.self || cur.isFrom(selfName)
    val prevSelf = prev.self || prev.isFrom(selfName)
    if (curSelf != prevSelf) return true
    // same sender: show the name only if the previous message was interrupted
    // (different sender in between) or more than 1 minute has passed since the
    // first message of the current run.
    if (i >= 2 && messages[i - 2].isFrom(selfName) != prevSelf) return true
    return (cur.timestamp - prev.timestamp) > 60_000L
}
@Composable
private fun ChatBubble(author: String, body: String, self: Boolean, timestamp: Long, wrapChars: Int, showSender: Boolean) {
    val timeLabel = timestampLabel(timestamp)
    val wrapped = wrapByChars(body, wrapChars)
    // a single unwrapped line shows the time right after the text on the same row;
    // two or more wrapped lines show the time on its own row, right-aligned.
    val multiLine = wrapped.contains('\n')
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (self) Arrangement.End else Arrangement.Start) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .wrapContentWidth(),
            horizontalAlignment = if (self) Alignment.End else Alignment.Start,
        ) {
            if (showSender) {
                Text(author, color = PhoneMuted, fontSize = 11.sp)
            }
            Box(Modifier.padding(top = 4.dp).clip(RoundedCornerShape(12.dp)).background(if (self) PhoneAccent else PhoneSurface)) {
                if (multiLine) {
                    Column(Modifier.width(IntrinsicSize.Max).padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 4.dp)) {
                        Text(wrapped, color = if (self) Color.White else PhoneText, fontSize = 14.sp, lineHeight = 17.sp)
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                            Text(timeLabel, color = if (self) Color.White.copy(alpha = 0.75f) else PhoneMuted, fontSize = 10.sp, lineHeight = 12.sp, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                } else {
                    Row(
                        Modifier.padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(wrapped, color = if (self) Color.White else PhoneText, fontSize = 14.sp, lineHeight = 17.sp)
                        Text(timeLabel, color = if (self) Color.White.copy(alpha = 0.75f) else PhoneMuted, fontSize = 10.sp, lineHeight = 12.sp)
                    }
                }
            }
        }
    }
}
private fun wrapByChars(text: String, charsPerLine: Int): String {
    return wrapChatTextByUnits(text, charsPerLine)
}
private fun timestampLabel(timestamp: Long): String {
    val time = java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault())
    return time.format(DateTimeFormatter.ofPattern("HH:mm"))
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
                        Text(formatCount(wallet.gil), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }
                wallet.entries.groupBy { it.section }.forEach { (section, entries) ->
                    item { SectionLabel(section) }
                    items(entries, key = { "${it.itemId}-${it.section}" }) { entry ->
                        Box(Modifier.animateItem()) {
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            ItemIcon(entry.iconId, Modifier.size(34.dp), fallback = entry.name.take(1))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(entry.name, color = PhoneText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    Text(formatCount(entry.amount), color = PhoneGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    if (entry.cap > 0) Text(" / ${formatCount(entry.cap)}", color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
                                }
                                if (entry.cap > 0) LinearProgressIndicator(
                                    progress = { (entry.amount.toFloat() / entry.cap.toFloat()).coerceIn(0f, 1f) },
                                    color = PhoneGreen,
                                    trackColor = PhoneGreen.copy(alpha = .12f),
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).clip(CircleShape),
                                )
                            }
                        }
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
private fun countdownLabel(seconds: Long): String {
    val s = seconds.coerceAtLeast(0L)
    val d = s / 86400
    val h = s % 86400 / 3600
    val m = s % 3600 / 60
    return when {
        d > 0 -> "${d}天${h}小时"
        h > 0 -> "${h}小时${m}分"
        else -> "${m}分钟"
    }
}
/**
 * 单色图标。
 *
 * [modifier] 里没给尺寸时兜底到 22dp。以前没有这个兜底，
 * 而 app_*.png 是 256×256 又放在无密度限定的 drawable/ 下（按 mdpi 解释），
 * 设置页的 ToggleRow/LinkRow 不传尺寸，图标就按 256dp 铺出来——
 * 行高把它裁成一条，看着就是"图标过大"。
 * 外层约束在 Compose 里优先于内层，所以已经自己 .size(...) 的调用点不受影响。
 */
@Composable
fun ImageGlyph(icon: Int, tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painterResource(icon),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(22.dp),
    )
}

/**
 * 骨架微光。首屏加载用它替代转圈——先把即将出现的内容形状占住，
 * 比一行"正在载入…"更少的跳变。实现和石之家的 SzjShimmerBox 同源。
 */
@Composable
fun PhoneShimmerBox(modifier: Modifier, shape: Shape = RoundedCornerShape(8.dp)) {
    val base = PhoneSurfaceRaised
    if (!phoneMotionEnabled()) {
        Box(modifier.clip(shape).background(base))
        return
    }
    val transition = rememberInfiniteTransition(label = "phoneShimmer")
    val x by transition.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1250, easing = LinearEasing), RepeatMode.Restart),
        label = "phoneShimmerX",
    )
    val light = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val glow = if (light) Color(0x40FFFFFF) else Color(0x18FFFFFF)
    Box(
        modifier.clip(shape).background(base).drawWithContent {
            drawContent()
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, glow, Color.Transparent),
                    start = Offset(x * size.width, 0f),
                    end = Offset((x + 0.7f) * size.width, size.height),
                )
            )
        }
    )
}

/**
 * 列表骨架屏：[rows] 行，每行一个图标位 + 两行文字位。
 * 形状照着真实列表行来，加载完不会突然跳版。
 */
@Composable
fun PhoneListSkeleton(rows: Int = 6, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        repeat(rows) {
            Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                PhoneShimmerBox(Modifier.size(34.dp), RoundedCornerShape(9.dp))
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    PhoneShimmerBox(Modifier.fillMaxWidth(0.42f).height(12.dp), RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(7.dp))
                    PhoneShimmerBox(Modifier.fillMaxWidth(0.78f).height(11.dp), RoundedCornerShape(4.dp))
                }
                Spacer(Modifier.width(10.dp))
                PhoneShimmerBox(Modifier.width(42.dp).height(11.dp), RoundedCornerShape(4.dp))
            }
        }
    }
}

/**
 * 全局空态版式：**图标 + 标题 + 一句引导 +（可选）动作按钮**。
 *
 * 版式是从石之家的 SzjEmpty 提上来的。规则：纯文字"暂无内容/正在载入…"
 * 只允许出现在行内小区域，不许占整屏——空屏是邀请动作的地方。
 * [iconRes] 传对应模块自己的图标；[tint] 默认压得很淡，图标是锚点不是主角。
 */
@Composable
fun PhoneEmpty(
    title: String,
    hint: String? = null,
    iconRes: Int = R.drawable.ic_empty_box,
    iconTint: Color = PhoneMuted.copy(alpha = 0.55f),
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ImageGlyph(iconRes, iconTint, Modifier.size(38.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        if (hint != null) {
            Spacer(Modifier.height(6.dp))
            Text(hint, color = PhoneMuted, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 17.sp)
        }
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

// ---------------------------------------------------------------------------
// 筛选面板
//
// 石之家的招募筛选（SzjRecruitFilterPanel）是全 App 里筛选做得最清楚的一处：
// 条件按语义分组、每组带标题、chip 换行不横滑（横滑看不到后面还有多少）、
// 改的是草稿、点"看结果"才发请求。这里把那套形态提成全局件，
// 别的模块（捕鱼、市场、图鉴…）直接用，不要再各写一套。
// ---------------------------------------------------------------------------

/** chip 圆角。 */
val PhoneChipShape = RoundedCornerShape(10.dp)

/**
 * 一组筛选 chip。[options] 是 (id, 显示名)，[multi] = true 时多选。
 * 每行 4 个后换行——横向滚动会藏住后面的选项。
 */
@Composable
fun PhoneChipGroup(
    label: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    onPick: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = PhoneText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            options.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { (id, name) ->
                        val on = id in selected
                        val bg by animateColorAsState(if (on) PhoneAccent else PhoneSurfaceRaised, tween(180), label = "chipBg")
                        val fg by animateColorAsState(if (on) Color.White else PhoneMuted, tween(180), label = "chipFg")
                        PhonePressable(onClick = { onPick(id) }, shape = PhoneChipShape) {
                            Text(
                                name,
                                fontSize = 12.sp, maxLines = 1,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                color = fg,
                                modifier = Modifier.clip(PhoneChipShape).background(bg)
                                    .padding(horizontal = 11.dp, vertical = 7.dp),
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f, fill = false)) }
                }
            }
        }
    }
}

/**
 * 从顶部滑下来的筛选面板。
 *
 * 高度封到 78%：条件多的时候面板会一直长到屏幕外，底部两个按钮点不到。
 * 现在选项区自己滚，按钮钉在面板底部。
 */
@Composable
fun PhoneFilterPanel(
    onClose: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    applyLabel: String = "看结果",
    content: @Composable ColumnScope.() -> Unit,
) {
    val noRipple = remember { MutableInteractionSource() }
    // 面板是覆盖层：返回键先关它，别一路穿透到退出这个 App。
    BackHandler { onClose() }
    Box(
        Modifier.fillMaxSize()
            .background(Color(0x73000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.TopCenter,
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { -it },
        ) {
            Column(
                Modifier.fillMaxWidth().fillMaxHeight(0.78f)
                    .clip(RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(PhoneSurface)
                    .clickable(interactionSource = noRipple, indication = null) { }
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(
                    Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content,
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(44.dp),
                    ) { Text("重置", fontSize = 14.sp) }
                    Button(
                        onClick = onApply,
                        colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.4f).height(44.dp),
                    ) { Text(applyLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}

/**
 * 筛选条 —— 列表页头那一行。
 *
 * 左边是当前生效的条件（点一下就摘掉这一条），右边是打开面板的入口。
 * 一条都没选时左边写一句"全部"，不留空白。
 * 这样"现在筛的是什么"永远看得见，不用打开面板才知道。
 */
@Composable
fun PhoneFilterBar(
    active: List<Pair<String, () -> Unit>>,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (active.isEmpty()) {
            Text("全部", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        } else {
            LazyRow(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(active.size) { index ->
                    val (label, clear) = active[index]
                    PhonePressable(onClick = clear, shape = PhoneChipShape) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clip(PhoneChipShape).background(PhoneAccentContainer)
                                .padding(start = 10.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        ) {
                            Text(label, color = PhoneOnAccentContainer, fontSize = 12.sp, maxLines = 1)
                            ImageGlyph(R.drawable.ic_close, PhoneOnAccentContainer, Modifier.padding(start = 3.dp).size(11.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        PhonePressable(onClick = onOpen, shape = PhoneChipShape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(PhoneChipShape)
                    .background(if (active.isEmpty()) PhoneSurfaceRaised else PhoneAccent)
                    .padding(horizontal = 11.dp, vertical = 7.dp),
            ) {
                ImageGlyph(
                    R.drawable.ic_filter,
                    if (active.isEmpty()) PhoneMuted else Color.White,
                    Modifier.size(13.dp),
                )
                Text(
                    if (active.isEmpty()) "筛选" else "筛选 ${active.size}",
                    color = if (active.isEmpty()) PhoneMuted else Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (active.isEmpty()) FontWeight.Normal else FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
    }
}

// ---- 全局按压反馈 ----
// 规则：**要么弹簧缩放，要么 ripple，不许裸奔**。
// 以前石之家有弹簧、桌面有弹簧，但聊天的会话行/筛选器行是裸 detectTapGestures
// 零反馈，捕鱼行是默认 ripple——同一台手机里三种手感。
// 这一套是从石之家的 SzjPressable 提上来的（那套手感是全模块标杆），
// 参数完全一致：控件 0.94、卡片 0.978、同一条弹簧。
// 对话框菜单项等系统控件保留 ripple，不要套这个。

/** 按压弹簧。阻尼 0.62 / 刚度 420——按下去有回弹但不晃。 */
val PhonePressSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 420f)

/** 系统关掉动画时（开发者选项 / 省电）不做缩放，只保留点击。 */
@Composable
fun phoneMotionEnabled(): Boolean {
    val ctx = LocalContext.current
    return remember {
        runCatching {
            android.provider.Settings.Global.getFloat(
                ctx.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0f
        }.getOrDefault(true)
    }
}

/**
 * 可按压容器：按下缩到 [pressedScale]，松手弹回。没有 ripple。
 *
 * 小控件（chip、图标钮）用默认 0.94；整行大卡片传 0.978——
 * 面积越大，同样的缩放比例看起来越夸张。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhonePressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    pressedScale: Float = 0.94f,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = phoneMotionEnabled()
    val scale by animateFloatAsState(
        if (pressed && motion && enabled) pressedScale else 1f,
        PhonePressSpring,
        label = "phonePressable",
    )
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        onLongClick = onLongClick,
                        onClick = onClick,
                    )
                } else {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    )
                }
            ),
        contentAlignment = contentAlignment,
    ) { content() }
}
