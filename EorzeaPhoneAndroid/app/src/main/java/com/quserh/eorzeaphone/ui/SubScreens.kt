package com.quserh.eorzeaphone.ui
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.toArgb
import com.quserh.eorzeaphone.ui.theme.AccentPalette
import com.quserh.eorzeaphone.ui.theme.PhoneOutline
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.union
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.GameInventoryItem
import com.quserh.eorzeaphone.data.ItemIconLoader
import com.quserh.eorzeaphone.ui.theme.BrandFill
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneAccentContainer
import com.quserh.eorzeaphone.ui.theme.PhoneBackground
import com.quserh.eorzeaphone.ui.theme.PhoneDanger
import com.quserh.eorzeaphone.ui.theme.PhoneWarn
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneOnAccentContainer
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.PhoneLine
import com.quserh.eorzeaphone.ui.theme.PhoneEdge
import com.quserh.eorzeaphone.ui.theme.phoneLight
import com.quserh.eorzeaphone.ui.theme.BrandOnFill
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch
@Composable
fun ScreenFrame(background: Color = PhoneBackground, content: @Composable ColumnScope.() -> Unit) {
    // 系统栏在**外层 Box** 上让位，键盘在**内层 Column** 上让位——
    // 两个分开的布局节点，不是叠在一条 modifier 链上。
    //
    // 这个形状是抄聊天会话的（AetherphoneParityScreens 的 LightFrame +
    // `Column(fillMaxSize().imePadding())`）——**那个是全项目唯一确认能正常
    // 被键盘顶起来的**，所以照它来，不再自己发明。
    //
    // 我上一版写的是一条链上 `statusBars` + `navigationBars.union(ime)`。
    // 理论上 union 取每边较大值也对，但实测发帖那屏就是顶不起来。
    // 与其继续论证我那版为什么应该对，不如照抄一个确认工作的形状。
    //
    // 为什么分开不会**多让一次**：`windowInsetsPadding` 会**消费**它用掉的
    // inset。外层 Box 消费了导航栏，内层的 imePadding() 拿到的是
    // "ime 减去已消费的导航栏"，所以键盘起来时不会在上面空出一条。
    Box(
        Modifier.fillMaxSize().background(background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Column(Modifier.fillMaxSize().imePadding()) {
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
fun ScreenHeader(
    title: String,
    state: PhoneState,
    trailing: (@Composable () -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    showBack: Boolean = true,
    /**
     * 标题色。默认 [PhoneText]。
     *
     * 给 wiki 物品详情用的：物品名带**稀有度色**（白/绿/蓝/紫/以太），
     * 那是这个领域里唯一有含义的彩色。原来 hero 里再写一遍名字来承载这个色，
     * 结果头部 20sp、hero 17sp 显示同一串字——外壳比主体还大。
     * 色交给头部之后 hero 就不用重复名字了。
     */
    titleColor: Color = Color.Unspecified,
) {
    val margin = LocalContentMargin.current
    val sidePad = (margin.coerceAtLeast(2) - 2).dp
    // 三栏 Row。原来是 Box + 三个 alignment 叠，标题靠写死的 horizontal 50dp
    // 躲开两边按钮——右边按钮一多就重合（聊天页头栽过这个）。
    Row(
        Modifier.fillMaxWidth().padding(horizontal = sidePad, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) {
            if (showBack) ImageGlyph(
                R.drawable.ic_back,
                PhoneAccent,
                // 48dp hit target (visual glyph stays 30dp).
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = (onBack ?: state::back)).padding(horizontal = 9.dp, vertical = 9.dp),
            )
        }
        Text(
            title,
            color = titleColor.takeOrElse { PhoneText },
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier.widthIn(min = 42.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(BrandFill)
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
                // 改用 PhoneButton：原来是 M3 的 Button/OutlinedButton，带 ripple，
                // 和这个 App 里其他所有可按的东西（PhonePressable 的按压回弹）手感不同。
                // 这是整个壳层最重要的一个按钮，不该是唯一手感不一样的那个。
                if (connected) {
                    PhoneButton(
                        "断开游戏连接",
                        onClick = { state.disconnect() },
                        kind = PhoneButtonKind.Secondary,
                        size = PhoneButtonSize.Wide,
                        danger = true,
                    )
                } else {
                    PhoneButton(
                        "连接游戏",
                        onClick = { state.connect() },
                        size = PhoneButtonSize.Wide,
                    )
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
    // 原来是就地 clip+background：同样是 14dp 圆角，但没有阴影、没有顶边高光、
    // 浅色下没有收边——所以设置页的"卡"比石之家的卡薄一层，两边观感不一致。
    // 换成 PhoneCard 之后这 13 处一起有了厚度，不用逐个改。
    PhoneCard(Modifier.fillMaxWidth(), content = content)
}

/** 组内分隔线。缩进到文字起点，不横穿图标那一列。 */
@Composable
private fun SettingsDivider() {
    Box(
        Modifier.fillMaxWidth()
            .padding(start = SettingsRowPad + SettingsIconSize + 14.dp)
            // 改用 PhoneLine：分割线全 App 一个值，别一处 outlineVariant、
            // 一处 PhoneMuted.copy(alpha)、一处随手一个灰。
            .height(1.dp).background(PhoneLine),
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
        SectionLabel("主题色")
        AccentPicker(state)
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
/**
 * 主题色选择。
 *
 * 版式上刻意**不用**设置行那一套（图标 + 标签 + 右侧单选圈）：颜色这件事
 * 只能看，不能读——一行"石之家金 ○"没有任何信息量。所以是色块网格，
 * 选中的那个套一圈描边 + 打勾。
 *
 * 上面压一条实时预览：一颗填充按钮、一段自己发的气泡（带情感动作那种浅青字）、
 * 一行强调文字。这三样正好是主题色的三个角色（填充 / 气泡 / 文字），
 * 换色时能立刻看出后果——特别是气泡：浅底会把情感动作的字吃掉，
 * 光看色块看不出来。
 */
@Composable
private fun AccentPicker(state: PhoneState) {
    var customOpen by remember { mutableStateOf(false) }
    val current = state.accent
    Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        // ---- 实时预览 ----
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PhoneSurface)
                .padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "按钮",
                    color = current.onFill, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(current.fill)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text("强调文字", color = PhoneAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            // 气泡预览：一行普通字 + 一行情感动作色（#BEFFF1）。
            // 后者是判断气泡底够不够深的标尺。
            Column(
                Modifier.clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 4.dp))
                    .background(current.bubble)
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Text("我发的消息", color = current.onBubble, fontSize = 13.sp)
                Spacer(Modifier.height(3.dp))
                Text("※玛琳 摆了摆手。", color = Color(0xFFBEFFF1), fontSize = 13.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "上面那行浅青是游戏里的情感动作颜色。它由游戏决定，改不了，" +
                    "所以气泡底必须够深——看不清就换一套。",
                color = PhoneMuted, fontSize = 11.sp, lineHeight = 16.sp,
            )
        }
        Spacer(Modifier.height(10.dp))
        // ---- 预设色块 ----
        val swatches = AccentPalette.presets
        swatches.chunked(5).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { p ->
                    AccentSwatch(
                        palette = p,
                        selected = state.accentId == p.id,
                        modifier = Modifier.weight(1f),
                        onClick = { state.accentId = p.id },
                    )
                }
                repeat(5 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        // ---- 自定义 ----
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(PhoneSurface)
                .clickable { customOpen = true }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val customSeed = Color(state.accentCustom)
            Box(
                Modifier.size(26.dp).clip(CircleShape).background(customSeed)
                    .border(
                        if (state.accentId == "custom") 2.dp else 1.dp,
                        if (state.accentId == "custom") PhoneAccent else PhoneOutline.copy(alpha = .5f),
                        CircleShape,
                    ),
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("自定义颜色", color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    "挑一个颜色，其余三个角色自动推算",
                    color = PhoneMuted, fontSize = 11.sp,
                )
            }
            ImageGlyph(R.drawable.ic_chevron_right, PhoneMuted, Modifier.size(17.dp))
        }
    }
    if (customOpen) {
        AccentCustomDialog(
            initial = Color(state.accentCustom),
            onDismiss = { customOpen = false },
            onPick = { c ->
                state.accentCustom = c.toArgb()
                state.accentId = "custom"
                customOpen = false
            },
        )
    }
}

@Composable
private fun AccentSwatch(
    palette: AccentPalette,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // 色块画成两半：上半是填充色，下半是气泡色。一个色块就能看出这套主题
        // 的两个关键角色，不用点进去才发现气泡是深的还是浅的。
        Box(
            Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
        ) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxWidth().weight(1f).background(palette.fill))
                Box(Modifier.fillMaxWidth().weight(1f).background(palette.bubble))
            }
            if (selected) {
                Box(
                    Modifier.fillMaxSize().border(2.5.dp, PhoneText, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(20.dp).clip(CircleShape).background(PhoneText),
                        contentAlignment = Alignment.Center,
                    ) {
                        ImageGlyph(R.drawable.ic_check_small, PhoneSurface, Modifier.size(13.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            palette.label,
            color = if (selected) PhoneText else PhoneMuted,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 自定义颜色。
 *
 * 色相一条横带（点/拖选），下面饱和度和明度各一条。三条都是实时的渐变条，
 * 不是数字输入框——挑颜色应该看着挑。
 */
@Composable
private fun AccentCustomDialog(
    initial: Color,
    onDismiss: () -> Unit,
    onPick: (Color) -> Unit,
) {
    val hsv = remember {
        val out = FloatArray(3)
        android.graphics.Color.colorToHSV(initial.toArgb(), out)
        out
    }
    var hue by remember { mutableStateOf(hsv[0]) }
    var sat by remember { mutableStateOf(hsv[1].coerceAtLeast(0.15f)) }
    var value by remember { mutableStateOf(hsv[2].coerceAtLeast(0.25f)) }
    val picked = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
    val derived = AccentPalette.fromSeed(picked)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("自定义主题色", color = PhoneText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                // 预览：填充 + 气泡（带情感动作色），和上一屏同一套标尺。
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(derived.fill))
                    Spacer(Modifier.width(8.dp))
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(derived.bubble)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    ) {
                        Text("我发的消息", color = derived.onBubble, fontSize = 12.sp)
                        Text("※摆了摆手。", color = Color(0xFFBEFFF1), fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                ColorSlider(
                    label = "色相",
                    fraction = hue / 360f,
                    colors = List(13) { i -> Color(android.graphics.Color.HSVToColor(floatArrayOf(i * 30f, 1f, 1f))) },
                    onChange = { hue = (it * 360f).coerceIn(0f, 359.99f) },
                )
                Spacer(Modifier.height(12.dp))
                ColorSlider(
                    label = "饱和度",
                    fraction = sat,
                    colors = listOf(
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value))),
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value))),
                    ),
                    onChange = { sat = it.coerceIn(0f, 1f) },
                )
                Spacer(Modifier.height(12.dp))
                ColorSlider(
                    label = "明度",
                    fraction = value,
                    colors = listOf(
                        Color.Black,
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, 1f))),
                    ),
                    onChange = { value = it.coerceIn(0.08f, 1f) },
                )
            }
        },
        confirmButton = {
            Text(
                "用这个颜色",
                color = PhoneAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable { onPick(picked) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        },
        dismissButton = {
            Text(
                "取消",
                color = PhoneMuted, fontSize = 14.sp,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).clickable(onClick = onDismiss)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        },
        containerColor = PhoneSurface,
    )
}

/** 一条渐变滑条。点或拖都能改，游标是一个描白边的小圆。 */
@Composable
private fun ColorSlider(
    label: String,
    fraction: Float,
    colors: List<Color>,
    onChange: (Float) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = PhoneMuted, fontSize = 11.sp)
        Spacer(Modifier.height(5.dp))
        BoxWithConstraints(Modifier.fillMaxWidth().height(28.dp)) {
            val w = maxWidth
            Box(
                Modifier.fillMaxWidth().height(18.dp).align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Brush.horizontalGradient(colors))
                    // 按下就跟手、拖着继续跟。
                    // 不用 detectTapGestures + detectHorizontalDragGestures 两个
                    // pointerInput 叠：那两个会互相抢手势（谁先消费谁赢），
                    // 表现就是有时点了没反应。这里自己收事件，按下和移动一视同仁。
                    .pointerInput(colors) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitPointerEvent()
                                val pos = down.changes.firstOrNull()?.position ?: continue
                                if (size.width > 0) {
                                    onChange((pos.x / size.width).coerceIn(0f, 1f))
                                }
                                down.changes.forEach { it.consume() }
                            }
                        }
                    },
            )
            Box(
                Modifier.align(Alignment.CenterStart)
                    .offset(x = (w - 20.dp) * fraction.coerceIn(0f, 1f))
                    .size(20.dp).clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, PhoneText.copy(alpha = .45f), CircleShape),
            )
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
        Text("×${"%,d".format(item.quantity)}", color = PhoneWarn, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
    }
    Divider(Modifier.padding(horizontal = 20.dp), color = PhoneLine)
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
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp)).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Gil", color = PhoneWarn, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text(formatCount(wallet.gil), color = PhoneText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
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
/**
 * 没有独立界面的应用落到这里。
 *
 * 原来这页说的是"应用已打开，等待游戏数据"——**那是假话**：这些应用没有
 * 界面，等下去也不会有东西出现。连不上游戏时还给一个"前往设置连接"，
 * 把人往一个解决不了问题的方向推。
 *
 * 现在如实说：这个应用只有图标，还没做界面。并给一个真的下一步——
 * 从桌面移除（它现在占着一格），或者返回。哪个应用有界面这件事
 * 由 AppStoreCatalog 记着，商店里标"占位"用的是同一份数据，
 * 两处不会各说一套。
 */
@Composable
fun GenericAppScreen(state: PhoneState) {
    val app = state.selectedApp
    ScreenFrame {
        ScreenHeader(app?.label ?: "应用", state)
        Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (app != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(58.dp).clip(RoundedCornerShape(15.dp)).background(app.color), contentAlignment = Alignment.Center) { ImageGlyph(app.icon, Color.White) }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(app.label, color = PhoneText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("还没做界面", color = PhoneMuted, fontSize = 12.sp)
                    }
                }
            }
            Text(
                AppStoreCatalog.blurbOf(app?.id.orEmpty()),
                color = PhoneMuted,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
            Text(
                "它现在占着桌面上的一格。不想留着可以移除，之后在 App Store 里随时装回来。",
                color = PhoneMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
            )
            if (app != null && app.id != "appstore") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PhonePressable(
                        onClick = { state.uninstallApp(app.id); state.back() },
                        shape = RoundedCornerShape(10.dp),
                        pressedScale = 0.95f,
                    ) {
                        Text(
                            "从桌面移除",
                            color = PhoneText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clip(RoundedCornerShape(10.dp))
                                .background(PhoneSurfaceRaised)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
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
 * Candy avatar: round disc, pastel-to-channel vertical gradient, initial or glyph.
 */
@Composable
fun SoftAvatar(
    initial: String,
    channelColor: Color,
    modifier: Modifier = Modifier,
    iconRes: Int? = null,
    size: androidx.compose.ui.unit.Dp = 48.dp,
) {
    val top = if (phoneLight) lerp(Color.White, channelColor, 0.38f) else lerp(channelColor, Color.White, 0.22f)
    val bottom = if (phoneLight) channelColor else lerp(channelColor, Color.Black, 0.15f)
    Box(
        modifier.size(size).clip(CircleShape).background(Brush.linearGradient(listOf(top, bottom))),
        contentAlignment = Alignment.Center,
    ) {
        if (iconRes != null) {
            ImageGlyph(iconRes, Color.White, Modifier.size(size * 0.42f))
        } else {
            Text(
                initial,
                color = Color.White,
                fontSize = (size.value * 0.34f).sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

/** Channel tag pill shown after a conversation name. */
@Composable
fun ChannelPill(label: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        label,
        color = Color.White,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(horizontal = 7.dp, vertical = 2.dp),
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
                        // v2: soft selected state (accent container + accent ink) instead of solid fill.
                        val bg by animateColorAsState(if (on) PhoneAccentContainer else PhoneSurfaceRaised, tween(180), label = "chipBg")
                        val fg by animateColorAsState(if (on) PhoneAccent else PhoneMuted, tween(180), label = "chipFg")
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
                // 同样换掉 M3 Button：Wide 传 weight 就能各占一份宽度。
                // "应用"占 1.4 份、"重置"占 1 份——主操作更宽是有意的。
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PhoneButton(
                        "重置",
                        onClick = onReset,
                        modifier = Modifier.weight(1f),
                        kind = PhoneButtonKind.Secondary,
                        size = PhoneButtonSize.Wide,
                    )
                    PhoneButton(
                        applyLabel,
                        onClick = onApply,
                        modifier = Modifier.weight(1.4f),
                        size = PhoneButtonSize.Wide,
                    )
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
                    .background(if (active.isEmpty()) PhoneSurfaceRaised else BrandFill)
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

// v2 shape ladder: rounder cards, softer inner controls.
/** 卡片圆角。 */
val PhoneCardShape = RoundedCornerShape(18.dp)

/** 卡内元素圆角（按钮、输入框、内嵌块）。 */
val PhoneInnerShape = RoundedCornerShape(12.dp)

/**
 * 一道发丝线。列表项之间、卡内分组之间用这个。
 *
 * 为什么不用 M3 的 `Divider`：它已经废弃（换成 HorizontalDivider），
 * 而且每个调用点都要自己写 `color = PhoneLine` 和缩进——项目里数出 11 处，
 * 缩进有 12dp / 18dp / 0 三种，同一条线在不同界面粗细起止都不一样。
 *
 * [indent] 是左右缩进：列表里通常缩到文字起点，让线不横穿图标那一列。
 */
@Composable
fun PhoneHairlineRow(indent: androidx.compose.ui.unit.Dp = 0.dp) {
    Box(
        Modifier.fillMaxWidth()
            .padding(horizontal = indent)
            .height(1.dp)
            .background(PhoneLine),
    )
}

/**
 * 卡片。**壳层以前没有这个**——石之家里所有卡都走 SzjCardSurface，
 * 出了石之家全是就地 `clip(RoundedCornerShape(x)).background(y)`
 * （壳层里数了 148 处），圆角、底色、有没有描边各写各的。
 * "石之家里像设计过、外面像默认长相"的根源就在这儿。
 *
 * 构造和 SzjCardSurface 一致，只是颜色走 Phone token：
 * 柔和阴影撑厚度 → 顶边 1dp 高光 → 浅色模式才加收边（白卡落在近白底上
 * 只靠阴影会糊）→ 按下时下沉（缩放 + 阴影收窄）。
 *
 * 卡片按压用 0.978 而不是控件的 0.94：面积越大，同样比例看着越夸张。
 */
@Composable
fun PhoneCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = PhoneCardShape,
    onClick: (() -> Unit)? = null,
    raised: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = phoneMotionEnabled()
    val scale by animateFloatAsState(
        if (pressed && motion && onClick != null) 0.978f else 1f,
        PhonePressSpring,
        label = "phoneCardPress",
    )
    val elevation by animateDpAsState(
        if (pressed && onClick != null) 1.dp else if (raised) 6.dp else 3.dp,
        tween(140),
        label = "phoneCardElev",
    )
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation, shape, ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
            .clip(shape)
            .background(if (raised) PhoneSurfaceRaised else PhoneSurface)
            .border(1.dp, PhoneLine, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else Modifier
            ),
    ) {
        content()
    }
}

/**
 * 按钮。壳层散着 5 个 M3 Button、5 个 TextButton、1 个 OutlinedButton，
 * 每处自己配 ButtonDefaults.buttonColors，手感和圆角都不统一，
 * 而石之家那边全是 SzjPressable。这里给三档，覆盖那些用法：
 *
 * - [PhoneButtonKind.Primary]  实心，一屏只该有一个（"建议你做的事"）
 * - [PhoneButtonKind.Secondary] 描边，同等重要的备选
 * - [PhoneButtonKind.Ghost]    无底，次要动作（"取消"这类）
 *
 * 破坏性动作传 `danger = true`：红只有 PhoneDanger 一个值。
 */
enum class PhoneButtonKind { Primary, Secondary, Ghost }

/**
 * 按钮有两种尺寸，因为这个 App 里确实存在两种用法，不是为了参数化而参数化：
 *
 * - [Compact]：卡片里、行尾的动作（商店的"安装"、对话框的"取消"）
 * - [Wide]：一屏的主操作，整宽 50dp（"连接游戏"这种）
 *
 * 别再加第三档。要更大更小说明那个位置的层级没想清楚。
 */
enum class PhoneButtonSize { Compact, Wide }

@Composable
fun PhoneButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: PhoneButtonKind = PhoneButtonKind.Primary,
    size: PhoneButtonSize = PhoneButtonSize.Compact,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    // accent 是文字/描边用的（够对比度），fill 是实心底用的（鲜艳）。
    // 这两个分工在 Theme.kt 里有说明，别混用。
    val accent = if (danger) PhoneDanger else PhoneAccent
    val fill = if (danger) PhoneDanger else BrandFill
    val wide = size == PhoneButtonSize.Wide
    // Wide 用卡片圆角（14dp）而不是内元素圆角：它本身就是一块，不是嵌在卡里的。
    val shape = if (wide) PhoneCardShape else PhoneInnerShape
    // Wide 的语义是"填满给它的宽度"，不是硬性 fillMaxWidth——
    // 这样单独放（在 Column 里）会占满整宽，放在 Row 里传 weight 也能各占一份。
    // 实现上靠里面那层 fillMaxWidth 去撑，外层不写死，否则 weight 会被覆盖。
    PhonePressable(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        enabled = enabled,
        // 整宽的块跟卡片一样，按压幅度要小；小控件才用默认的 0.94。
        pressedScale = if (wide) 0.985f else 0.94f,
    ) {
        val base = Modifier.clip(shape).then(if (wide) Modifier.fillMaxWidth() else Modifier)
        val boxed = when (kind) {
            PhoneButtonKind.Primary -> base.background(if (enabled) fill else PhoneSurfaceRaised)
            PhoneButtonKind.Secondary -> base.border(1.dp, if (enabled) accent.copy(alpha = 0.55f) else PhoneLine, shape)
            PhoneButtonKind.Ghost -> base
        }
        Text(
            label,
            color = when {
                !enabled -> PhoneMuted
                kind == PhoneButtonKind.Primary -> if (danger) Color.White else BrandOnFill
                else -> accent
            },
            fontSize = if (wide) 15.sp else 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = if (wide) TextAlign.Center else null,
            modifier = boxed.padding(
                horizontal = if (kind == PhoneButtonKind.Ghost) 10.dp else 16.dp,
                vertical = if (wide) 15.dp else 10.dp,
            ),
        )
    }
}

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
