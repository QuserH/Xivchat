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

// ---- 品牌色：以太水晶蓝青 ----
// 原来这里是 M3 模板原封不动的紫（#6750A4 / #D0BCFF），全套工具屏都跟着它，
// 结果整台"手机"一眼就是没设计过的模板 App。换成水晶蓝青：和石之家的水晶青
// (#5FD2C8) 同族但更深一档，白底上够 4.5:1，深色底上够亮。
// 聊天保留 AetherPurple（那是对 FFXIV-Aetherphone 的复刻，不是品牌色），
// 石之家保留自己的水晶青——每个"App"可以有自己的主题色，但全局工具必须是
// 刻意选的品牌色。
private val BrandLight = Color(0xFF0E7C86)          // 白底 4.9:1
private val BrandOnLight = Color.White
private val BrandContainerLight = Color(0xFFB6ECEF)
private val BrandOnContainerLight = Color(0xFF00363B)
private val BrandDark = Color(0xFF6FD8DE)           // 深底 9.7:1
private val BrandOnDark = Color(0xFF00363B)
private val BrandContainerDark = Color(0xFF004F55)
private val BrandOnContainerDark = Color(0xFF9CF1F7)

// 亮/暗两档都要用，所以做成 composable。深色模式下用亮青，浅色下用深青。
val PhoneAccent: Color @Composable get() =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) BrandLight else BrandDark
val PhoneAccentContainer: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
val PhoneOnAccentContainer: Color @Composable get() = MaterialTheme.colorScheme.onPrimaryContainer

/**
 * Canvas / DrawScope 是非 composable 作用域，读不到上面那些 getter。
 * 这两个是给画笔用的定值：在 Canvas 外面先 `val accent = PhoneAccentFor(dark)`
 * 拿到颜色，再传进去。
 */
fun phoneAccentFor(dark: Boolean): Color = if (dark) BrandDark else BrandLight

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

// 中性色也从紫灰拧成蓝灰（surfaceVariant / outline 原来带紫底），
// 不然品牌色换了、底色还是紫的，反而更脏。
private val LightPhoneColors = lightColorScheme(
    primary = BrandLight,
    onPrimary = BrandOnLight,
    primaryContainer = BrandContainerLight,
    onPrimaryContainer = BrandOnContainerLight,
    secondary = Color(0xFF4C6268),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFE7EC),
    onSecondaryContainer = Color(0xFF081F23),
    tertiary = Color(0xFF515D7D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDBE1FF),
    onTertiaryContainer = Color(0xFF0C1A38),
    error = DangerLight,
    errorContainer = Color(0xFFFFDAD9),
    onErrorContainer = Color(0xFF410006),
    background = Color(0xFFEFF2F4),
    onBackground = Color(0xFF181C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF181C1E),
    surfaceVariant = Color(0xFFE3E9EC),
    onSurfaceVariant = Color(0xFF444B4E),
    outline = Color(0xFF74797C),
    outlineVariant = Color(0xFFC5CBCE),
    scrim = Color(0xFF000000),
)

private val DarkPhoneColors = darkColorScheme(
    primary = BrandDark,
    onPrimary = BrandOnDark,
    primaryContainer = BrandContainerDark,
    onPrimaryContainer = BrandOnContainerDark,
    secondary = Color(0xFFB3CBD1),
    onSecondary = Color(0xFF1D3439),
    secondaryContainer = Color(0xFF344A50),
    onSecondaryContainer = Color(0xFFCFE7EC),
    tertiary = Color(0xFFB9C5EA),
    onTertiary = Color(0xFF232F4D),
    tertiaryContainer = Color(0xFF3A4664),
    onTertiaryContainer = Color(0xFFDBE1FF),
    error = DangerDark,
    errorContainer = Color(0xFF8C1521),
    onErrorContainer = Color(0xFFFFDAD9),
    background = Color(0xFF101416),
    onBackground = Color(0xFFDFE3E6),
    surface = Color(0xFF1D2225),
    onSurface = Color(0xFFDFE3E6),
    surfaceVariant = Color(0xFF3F484B),
    onSurfaceVariant = Color(0xFFBFC8CB),
    outline = Color(0xFF899295),
    outlineVariant = Color(0xFF3F484B),
    scrim = Color(0xFF000000),
)

private val Md3Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun EorzeaPhoneTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkPhoneColors else LightPhoneColors,
        typography = Typography(),
        shapes = Md3Shapes,
        content = content,
    )
}
