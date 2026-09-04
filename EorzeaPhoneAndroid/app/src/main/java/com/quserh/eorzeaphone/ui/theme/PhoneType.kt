package com.quserh.eorzeaphone.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 全 App 的字阶。
 *
 * 起因：全库 684 处直接写 `fontSize = N.sp`，用了 21 个不同字号，8~15sp 之间几乎是
 * 随手挑的；`letterSpacing` 只有 23 处设过，而且**全是正值**；`lineHeight` 只有 85 处
 * 设过。所以标题和正文的区别只剩「字大一点」，读起来就散。
 *
 * Apple 的排版规则（*The Details of UI Typography*, WWDC 2020）里，这三件事是**一组**，
 * 不能各自随手填：
 *
 * - **字距随字号变，不能一个值通用。** 字越大，字母间的空隙看起来越宽，所以大字要
 *   **负字距**收紧；小字反而要**微正字距**才清楚。全库一个负值都没有，是最明显的问题。
 * - **行高与字号成反比。** 大标题行高要紧（1.05~1.2 倍），正文要松（1.4~1.5 倍）。
 * - **层级由「字号 + 字重 + 行高」一起决定**，不是只靠字号。字重能在不占更多空间的
 *   前提下加重存在感。
 *
 * 中文的处理：负字距对汉字要比拉丁字母**保守**得多。汉字是等宽方块，收紧过头会直接
 * 糊在一起，所以这里大字最多只收到 -0.4sp（拉丁字体常见的 -0.02em 在 34sp 上约等于
 * -0.7sp，对中文太狠）。行高反过来要比纯拉丁**松**一点，汉字没有 x-height 的视觉缓冲。
 *
 * 用法：`Text(x, style = PhoneType.Body)`，别再写裸 `fontSize`。
 * 需要改颜色就 `style = PhoneType.Body, color = PhoneMuted`（color 是 Text 的参数，
 * 不必复制整个 style）。
 */
object PhoneType {

    /** 大数字/大标题。仅用于「就该占满一眼」的地方（时钟、金币数）。 */
    val Display = TextStyle(
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
        lineHeight = 38.sp,
    )

    /** 页面主标题（大号）。 */
    val Title = TextStyle(
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp,
        lineHeight = 27.sp,
    )

    /** 页头标题。ScreenHeader 用这一档。 */
    val Header = TextStyle(
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp,
        lineHeight = 24.sp,
    )

    /** 卡片/分区标题。 */
    val Headline = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
        lineHeight = 21.sp,
    )

    /** 列表行主标题。iOS 设置行是 17pt，这里 15sp 配 240dpi 观感接近。 */
    val Row = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 20.sp,
    )

    /** 正文。字距 0 —— 正文既不该收紧也不该撑开。 */
    val Body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
        lineHeight = 21.sp,
    )

    /** 次要说明文字（行内 hint、卡片副标题）。开始给微正字距。 */
    val Callout = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp,
        lineHeight = 17.sp,
    )

    /** 元信息（时间、楼层、计数）。 */
    val Caption = TextStyle(
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.2.sp,
        lineHeight = 15.sp,
    )

    /** 最小一档：角标、徽标数字。再小就别放中文了。 */
    val Micro = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
        lineHeight = 13.sp,
    )

    /** 分区小标题（全大写/短标签）。字距明显放开，短标签才立得住。 */
    val SectionLabel = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.5.sp,
        lineHeight = 16.sp,
    )
}
