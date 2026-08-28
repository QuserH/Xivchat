package com.quserh.eorzeaphone.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

/** Adjustable global horizontal inset for feature screens (外观 -> 内容边距). */
val LocalContentMargin = staticCompositionLocalOf { 16 }

// ---- 品牌色：石之家的金 ----
//
// 取值不是挑的，是从石之家自己的样式表里读出来的
// （https://ff14risingstones.web.sdo.com/mob/static/css/app.*.css）：
// #c4a86a 在那份 CSS 里出现 177 次，是全站唯一的强调色。
//   .active{background-color:#c4a86a;color:#fff}
//   .is-selected{background-color:#fbf9f4;border-color:#c4a86a}
//   .van-tabs__line{background-color:#c4a86a}
// 配套的中性色：#fff 卡片、#f2f2f2 页底、#1f1f1f/#4b4b4b 文字、
// #a69162 按下态的深金、#fbf9f4/#f0e9da 金的浅色底、#b54545 危险色。
//
// 之前这里是三套色：全局"以太水晶蓝青"、聊天的 AetherPurple（Aetherphone 复刻）、
// 石之家自己的水晶青。理由是"每个 App 可以有自己的主题色"——但结果就是同一台
// 手机里三种强调色，看着不像一个东西。现在统一到石之家这一套。
//
// **金不能直接当文字色**：#c4a86a 在白底上只有 2.17:1。官网自己也只把它用作
// 填充（金底白字），个别地方拿它写小字是官网的无障碍问题，不该照抄。
// 所以分成两个角色：
//   AccentFill —— 填充（按钮、选中态的底），配白字，和官网完全一致
//   AccentInk  —— 文字/图标用的深金，白底 5.2:1
// 主题色现在是**可换的**（设置 → 外观 → 主题色，九套预设 + 自定义），
// 定义搬到 AccentPalette.kt。下面这些 getter 都读当前选中的那一套。
// 默认仍是石之家金。
//
// 中性色（背景/卡片/文字/分割线）不跟着换：那套是照石之家的中性色定的，
// 换的只是强调色。整套底色跟着变会让每个预设都得重新校一遍对比度。

/** 实心填充用的强调色（按钮底、选中态底），上面配 [BrandOnFill]。 */
val BrandFill: Color @Composable get() = CurrentAccent.fill

/** 落在 [BrandFill] 上的字色。 */
val BrandOnFill: Color @Composable get() = CurrentAccent.onFill

/** 按下态：把填充色压暗一档。 */
val BrandFillPressed: Color @Composable get() = CurrentAccent.fill.let {
    Color(it.red * 0.85f, it.green * 0.85f, it.blue * 0.85f, it.alpha)
}

/** 自己发的聊天气泡底色。够深，游戏的浅色频道文字要能落在上面。 */
val BrandBubble: Color @Composable get() = CurrentAccent.bubble

/** 落在 [BrandBubble] 上的字色。 */
val BrandOnBubble: Color @Composable get() = CurrentAccent.onBubble

// M3 colorScheme 需要的静态默认值（Provider 之外的兜底）。
private val BrandOnLight = Color.White
private val BrandContainerLight = Color(0xFFFBF9F4)
private val BrandOnContainerLight = Color(0xFF4A3A15)
private val BrandOnDark = Color(0xFF2A2110)
private val BrandContainerDark = Color(0xFF2E2921)
private val BrandOnContainerDark = Color(0xFFE8D5A8)

/**
 * **文字/图标**用的强调色（在当前背景上够对比度）。
 * 实心填充用 [BrandFill] —— 那个鲜艳但当字色不够对比。
 */
val PhoneAccent: Color @Composable get() =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) CurrentAccent.inkLight
    else CurrentAccent.inkDark

val PhoneAccentContainer: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
val PhoneOnAccentContainer: Color @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer

/**
 * Canvas / DrawScope 是非 composable 作用域，读不到上面那些 getter。
 * 在 Canvas 外面先 `val accent = phoneAccentFor(dark, accent)` 拿到颜色再传进去。
 */
fun phoneAccentFor(dark: Boolean, palette: AccentPalette = AccentPalette.default): Color =
    if (dark) palette.inkDark else palette.inkLight

// ---- 语义色 ----
// 规则：同一种语义只允许有一个色值。以前红色散落成三个（#E5485D 桌面角标、
// #E53935 移除钮、#D64555 聊天删除项），蓝橙也各有自己的硬编码，改都改不齐。
private val DangerLight = Color(0xFFC2323D)
private val DangerDark = Color(0xFFFF8A93)
private val WarnLight = Color(0xFF9A5600)
private val WarnDark = Color(0xFFFFB870)
private val InfoLight = Color(0xFF1D6FA5)
private val InfoDark = Color(0xFF7FC4EE)

/** 危险 / 破坏性操作、未读角标。红只有这一个。 */
val PhoneDanger: Color @Composable get() =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) DangerLight else DangerDark

/** 提醒、需要注意但不致命（天气窗口、ET 条件）。 */
val PhoneWarn: Color @Composable get() =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) WarnLight else WarnDark

/** 中性信息类标记（条件类徽章统一走这个，别一行四个色）。 */
val PhoneInfo: Color @Composable get() =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) InfoLight else InfoDark

val PhoneGreen = Color(0xFF27C66A)

val PhoneBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val PhoneSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val PhoneSurfaceRaised: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val PhoneText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val PhoneMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val PhoneOutline = Color(0xFF79747E)

/** 当前是浅色模式。判据和石之家那边一致（看背景亮度，不看系统开关）。 */
val phoneLight: Boolean @Composable get() = MaterialTheme.colorScheme.background.luminance() > 0.5f

// ---- 线与边 ----
// 壳层以前**没有**这三个 token，所有分割线和描边都是就地写一个灰或者
// PhoneMuted.copy(alpha=…)，同一条线在不同界面粗细深浅都不一样。
// 取值和石之家同源（它是从官网 app.css 读的），这样两边的线看着是一套。
private val PhoneLineLight = Color(0xFFE5E5E5)
private val PhoneLineDark = Color(0xFF322D24)
private val PhoneHairlineLight = Color(0xFFC2C2C2)
private val PhoneHairlineDark = Color(0xFF443D31)
private val PhoneEdgeLight = Color(0x0A000000)
private val PhoneEdgeDark = Color(0x24FFFFFF)

/** 分割线：同层内容之间的分界（列表行之间、卡内分组之间）。 */
val PhoneLine: Color @Composable get() = if (phoneLight) PhoneLineLight else PhoneLineDark

/** 描边：比分割线重一档，用来给控件收边（chip、输入框）。 */
val PhoneHairline: Color @Composable get() = if (phoneLight) PhoneHairlineLight else PhoneHairlineDark

/** 卡片顶边高光。深色下是那一线反光，浅色下是一道极淡的压边。 */
val PhoneEdge: Color @Composable get() = if (phoneLight) PhoneEdgeLight else PhoneEdgeDark

// 中性色跟着石之家走：#f2f2f2 页底、#fff 卡片、#f5f5f5 抬升层、
// #1f1f1f 正文、#e5e5e5 分割线、#c2c2c2 描边——都是从它的 app.css 里读的。
// 原来是蓝灰一套（配水晶青），底色偏冷，配金会显脏。现在中性色也退到中性偏暖。
private fun lightPhoneColors(accent: AccentPalette) = lightColorScheme(
    primary = accent.inkLight,
    onPrimary = BrandOnLight,
    primaryContainer = BrandContainerLight,
    onPrimaryContainer = BrandOnContainerLight,
    secondary = Color(0xFF6B6252),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E9DA),   // 官网 #f0e9da
    onSecondaryContainer = Color(0xFF252014),
    tertiary = Color(0xFF5C5B57),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE8E6E1),
    onTertiaryContainer = Color(0xFF1F1E1B),
    error = Color(0xFFB54545),                 // 官网 #b54545
    errorContainer = Color(0xFFFBE4E4),
    onErrorContainer = Color(0xFF4A1212),
    background = Color(0xFFF2F2F2),            // 官网页底
    onBackground = Color(0xFF1F1F1F),          // 官网正文
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = Color(0xFFF5F5F5),        // 官网 #f5f5f5
    onSurfaceVariant = Color(0xFF4B4B4B),      // 官网次要文字
    outline = Color(0xFF9C9C9C),               // 官网 #9c9c9c
    outlineVariant = Color(0xFFE5E5E5),        // 官网 #e5e5e5
    scrim = Color(0xFF000000),
)

// 官网只有浅色一套，深色这边按同一个金推暖灰（冷灰配金显脏）。
private fun darkPhoneColors(accent: AccentPalette) = darkColorScheme(
    primary = accent.inkDark,
    onPrimary = BrandOnDark,
    primaryContainer = BrandContainerDark,
    onPrimaryContainer = BrandOnContainerDark,
    secondary = Color(0xFFD3C9B4),
    onSecondary = Color(0xFF352F22),
    secondaryContainer = Color(0xFF4A4132),
    onSecondaryContainer = Color(0xFFF0E9DA),
    tertiary = Color(0xFFCAC7BF),
    onTertiary = Color(0xFF32302B),
    tertiaryContainer = Color(0xFF494740),
    onTertiaryContainer = Color(0xFFE8E6E1),
    error = Color(0xFFFF9C9C),
    errorContainer = Color(0xFF7A2626),
    onErrorContainer = Color(0xFFFBE4E4),
    background = Color(0xFF14120D),
    onBackground = Color(0xFFECE7DD),
    surface = Color(0xFF201D16),
    onSurface = Color(0xFFECE7DD),
    surfaceVariant = Color(0xFF453F33),
    onSurfaceVariant = Color(0xFFC9C2B4),
    outline = Color(0xFF938C7E),
    outlineVariant = Color(0xFF3A352B),
    scrim = Color(0xFF000000),
)

private val Md3Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * @param accent 主题色。设置里选的那一套，透过 [LocalAccent] 发给全 App。
 */
@Composable
fun EorzeaPhoneTheme(
    darkTheme: Boolean,
    accent: AccentPalette = AccentPalette.default,
    content: @Composable () -> Unit,
) {
    androidx.compose.runtime.CompositionLocalProvider(LocalAccent provides accent) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkPhoneColors(accent) else lightPhoneColors(accent),
            typography = Typography(),
            shapes = Md3Shapes,
            content = content,
        )
    }
}
