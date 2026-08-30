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
private val BrandOnDark = Color(0xFF2A2110)

/**
 * 把 [fg] 以 [alpha] 叠在 [bg] 上，返回不透明结果。
 * primaryContainer 这类 M3 槽位不接受透明色，但语义上要的是"强调色的浅底"，
 * 所以按"fill 半透明叠在当前底色上"现场算，而不是再养一套写死的色值。
 */
private fun blendOver(fg: Color, alpha: Float, bg: Color): Color = Color(
    fg.red * alpha + bg.red * (1f - alpha),
    fg.green * alpha + bg.green * (1f - alpha),
    fg.blue * alpha + bg.blue * (1f - alpha),
)

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
// v2 design system: cool sage ramp. Light is the master palette; dark swaps values only.
private val PhoneLineLight = Color(0xFFE3EAE0)
private val PhoneLineDark = Color(0xFF26312A)
private val PhoneHairlineLight = Color(0xFFC6CFC5)
private val PhoneHairlineDark = Color(0xFF37453C)
private val PhoneEdgeLight = Color(0x0A000000)
private val PhoneEdgeDark = Color(0x24FFFFFF)

/** 分割线：同层内容之间的分界（列表行之间、卡内分组之间）。 */
val PhoneLine: Color @Composable get() = if (phoneLight) PhoneLineLight else PhoneLineDark

/** 描边：比分割线重一档，用来给控件收边（chip、输入框）。 */
val PhoneHairline: Color @Composable get() = if (phoneLight) PhoneHairlineLight else PhoneHairlineDark

/** 卡片顶边高光。深色下是那一线反光，浅色下是一道极淡的压边。 */
val PhoneEdge: Color @Composable get() = if (phoneLight) PhoneEdgeLight else PhoneEdgeDark

// ---- 压在图上的浮层 ----
// 这一组**有意不跟主题**，理由和 §6「哪些颜色有意不跟主题」一致：它们压在
// 游戏地图底图或任务图画布上，底下那层的颜色我们控制不了（羊皮纸黄、植被绿、
// 深色海图都有），所以浮层必须自带固定深底 + 固定亮字，任何底图上都读得清。
// 跟着强调色变的话，青/绿主题下针会和地图植被混掉。
//
// 之所以抽成 token 而不是就地写：同一个值散在 5 个文件里
// （GatherClock / WikiLink / QuestTree / Fishing / Shizhijia），改一处漏四处。
// 都是定值 val 而非 @Composable getter —— Canvas / DrawScope 是非 composable
// 作用域，读不到 getter（和 [PhoneGreen]、[PhoneOutline] 一样的处理）。

/** 地图定位针。站点用的也是这个红，采集点和任务 NPC 共用一个值。 */
val MapPin = Color(0xFFE0453D)

/** 图上文字标签的底衬（地名、采集点名）。半透明，底图纹理还透得出来。 */
val CanvasLabelScrim = Color(0x8C000000)

/** 图上文字的描边阴影。标签底衬之外再兜一层，压在亮底图上也不糊。 */
val CanvasLabelShadow = Color(0xCC000000)

/** 画布上实心小控件的底（缩放钮、节点角标）。比标签底衬重一档，要能点。 */
val CanvasControlScrim = Color(0x99000000)

/** 浮层底衬上的字。纯白偏刺眼，压到 80%。 */
val OnCanvasScrim = Color(0xCCFFFFFF)

// ---- v2 palette: "晨露 Morning Dew" (locked with user) ----
// Light scheme is the single design source; dark derives by value swap on the same
// ramp (cooler sage blacks instead of the old warm browns).
// primaryContainer is derived from the live accent (see blendOver) so switching
// accent presets re-tints chips instead of leaving stale gold containers.
private fun lightPhoneColors(accent: AccentPalette) = lightColorScheme(
    primary = accent.inkLight,
    onPrimary = BrandOnLight,
    primaryContainer = blendOver(accent.fill, 0.14f, Color.White),
    onPrimaryContainer = accent.inkLight,
    secondary = Color(0xFF55616C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8EEF2),
    onSecondaryContainer = Color(0xFF253038),
    tertiary = Color(0xFF5C6B60),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2EAE3),
    onTertiaryContainer = Color(0xFF1F2B24),
    error = Color(0xFFC2323D),
    errorContainer = Color(0xFFFBE4E4),
    onErrorContainer = Color(0xFF4A1212),
    background = Color(0xFFF2F5EF),
    onBackground = Color(0xFF24312A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF24312A),
    surfaceVariant = Color(0xFFEDF2EA),
    onSurfaceVariant = Color(0xFF5F6E64),
    outline = Color(0xFF8A988E),
    outlineVariant = Color(0xFFE3EAE0),
    scrim = Color(0xFF000000),
)

private fun darkPhoneColors(accent: AccentPalette) = darkColorScheme(
    primary = accent.inkDark,
    onPrimary = BrandOnDark,
    primaryContainer = blendOver(accent.fill, 0.22f, Color(0xFF181F1A)),
    onPrimaryContainer = accent.inkDark,
    secondary = Color(0xFFB9C6BD),
    onSecondary = Color(0xFF1F2B24),
    secondaryContainer = Color(0xFF2A3830),
    onSecondaryContainer = Color(0xFFD7E4DA),
    tertiary = Color(0xFFA9B8AC),
    onTertiary = Color(0xFF1F2B24),
    tertiaryContainer = Color(0xFF2E3A32),
    onTertiaryContainer = Color(0xFFD7E4DA),
    error = Color(0xFFFF8A93),
    errorContainer = Color(0xFF6E2429),
    onErrorContainer = Color(0xFFFBE4E4),
    background = Color(0xFF101613),
    onBackground = Color(0xFFE2EAE2),
    surface = Color(0xFF181F1A),
    onSurface = Color(0xFFE2EAE2),
    surfaceVariant = Color(0xFF232D26),
    onSurfaceVariant = Color(0xFF93A39A),
    outline = Color(0xFF6E7C72),
    outlineVariant = Color(0xFF26312A),
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
