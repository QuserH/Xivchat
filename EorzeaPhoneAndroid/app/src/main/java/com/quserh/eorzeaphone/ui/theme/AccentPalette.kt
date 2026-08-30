package com.quserh.eorzeaphone.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * 主题色。
 *
 * 一个强调色不够用，因为同一个颜色要干三件互相冲突的事：
 *
 *  - **填充**（按钮底、选中态的底）：要够鲜艳，配白字
 *  - **文字/图标**：要在背景上够对比度（4.5:1），所以往往得比填充深
 *  - **气泡底**：自己发的聊天气泡。上面要落**游戏自带的频道文字色**
 *    （情感动作是 #BEFFF1 这种很浅的青），所以气泡必须**深**——
 *    否则浅色的频道文字直接看不见。石之家的金做气泡底就栽在这里。
 *
 * 所以一套主题是四个值，而不是一个。预设里每个都是手挑的；
 * 自定义色则由 [fromSeed] 按同样的规则推出来。
 *
 * @param fill 实心填充色，配白字。
 * @param inkLight 浅色主题下的文字/图标色（白底 ≥4.5:1）。
 * @param inkDark 深色主题下的文字/图标色。
 * @param bubble 自己气泡的底色。**必须够深**，浅色频道文字要能落在上面。
 */
data class AccentPalette(
    val id: String,
    val label: String,
    val fill: Color,
    val inkLight: Color,
    val inkDark: Color,
    val bubble: Color,
) {
    /** 填充/气泡上的字色。底够深就用白。 */
    val onFill: Color get() = if (fill.luminance() > 0.55f) Color(0xFF241E10) else Color.White
    val onBubble: Color get() = if (bubble.luminance() > 0.55f) Color(0xFF241E10) else Color.White

    companion object {
        /**
         * 从一个种子色推出整套。自定义颜色走这条路。
         *
         * 规则和手挑的预设一致：
         *  - 填充 = 种子本身
         *  - 浅色墨 = 压暗直到白底 4.5:1
         *  - 深色墨 = 提亮直到深底（按 #14120D 算）4.5:1
         *  - 气泡 = 压到亮度约 0.10–0.16，保证 #BEFFF1 那类浅字能看清
         */
        fun fromSeed(seed: Color, id: String = "custom", label: String = "自定义"): AccentPalette =
            AccentPalette(
                id = id,
                label = label,
                fill = seed,
                inkLight = darkenUntilContrast(seed, Color.White, 4.5f),
                inkDark = lightenUntilContrast(seed, Color(0xFF14120D), 4.5f),
                bubble = scaleToLuminance(seed, 0.13f),
            )

        /** 相对亮度对比度（WCAG）。 */
        fun contrast(a: Color, b: Color): Float {
            val la = a.luminance()
            val lb = b.luminance()
            return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
        }

        private fun darkenUntilContrast(c: Color, against: Color, target: Float): Color {
            var f = 1f
            var out = c
            // 最多压到 15%，再深就不像原来那个颜色了。
            while (contrast(out, against) < target && f > 0.15f) {
                f -= 0.05f
                out = Color(c.red * f, c.green * f, c.blue * f, c.alpha)
            }
            return out
        }

        private fun lightenUntilContrast(c: Color, against: Color, target: Float): Color {
            var t = 0f
            var out = c
            while (contrast(out, against) < target && t < 0.85f) {
                t += 0.05f
                out = Color(
                    c.red + (1f - c.red) * t,
                    c.green + (1f - c.green) * t,
                    c.blue + (1f - c.blue) * t,
                    c.alpha,
                )
            }
            return out
        }

        /** 保持色相、把亮度压/提到目标值附近。 */
        private fun scaleToLuminance(c: Color, target: Float): Color {
            var lo = 0f
            var hi = 2f
            var out = c
            repeat(18) {
                val mid = (lo + hi) / 2f
                out = Color(
                    (c.red * mid).coerceIn(0f, 1f),
                    (c.green * mid).coerceIn(0f, 1f),
                    (c.blue * mid).coerceIn(0f, 1f),
                    c.alpha,
                )
                if (out.luminance() > target) hi = mid else lo = mid
            }
            return out
        }

        /**
         * 预设。
         *
         * 第一个是石之家自己的金（取自它的 app.css，#c4a86a 在那份样式表里出现
         * 177 次）。其余是常见的几支，色相分开，不是同一个颜色的深浅。
         *
         * 每套的 bubble 都单独挑过：够深，游戏的浅色频道文字（情感动作
         * #BEFFF1、密语的粉等）落上去要看得清。
         */
        // 每套的 bubble 都用情感动作色 #BEFFF1 验过对比度（见注释里的数字）。
        // 除了以太紫（3.53:1，那是原样保留的历史值），其余都在 8:1 以上。
        val presets: List<AccentPalette> = listOf(
            AccentPalette(
                id = "dew_green", label = "晨露绿",
                // 0x3F8150 (not 0x4E8D5B): white-on-fill passes WCAG at button sizes.
                fill = Color(0xFF3F8150),
                inkLight = Color(0xFF2F6B40),
                inkDark = Color(0xFF7FC49A),
                bubble = Color(0xFF1E4A2A),
            ),
            AccentPalette(
                id = "stone_gold", label = "石之家金",
                fill = Color(0xFFC4A86A),
                inkLight = Color(0xFF7D6229),
                inkDark = Color(0xFFD8BE85),
                // 深棕金。**不能**直接用 fill 那个 #c4a86a 当气泡底：
                // 情感动作色落在它上面只有 2.05:1，等于看不见（0.7.235 的 bug）。
                // 这个值是 9.84:1。
                bubble = Color(0xFF4A3A15),
            ),
            AccentPalette(
                id = "aether_violet", label = "以太紫",
                fill = Color(0xFF8669F2),
                inkLight = Color(0xFF5B41C4),
                inkDark = Color(0xFFC0B0FA),
                // 就是 0.7.234 之前自己气泡用的那个紫，原样保留。
                // 实测：情感动作色 #BEFFF1 落在它上面 3.53:1 —— 能看见，但不宽裕
                // （我拿石之家金做气泡时是 2.05:1，那是真看不见）。
                // 想更清楚就挑别的预设，其余几套的气泡都在 8:1 以上。
                bubble = Color(0xFF8669F2),
            ),
            AccentPalette(
                id = "crystal_teal", label = "水晶青",
                fill = Color(0xFF3FBFB4),
                inkLight = Color(0xFF10736C),
                inkDark = Color(0xFF6FD8DE),
                bubble = Color(0xFF0E4F4A),
            ),
            AccentPalette(
                id = "garlean_steel", label = "帝国钢蓝",
                fill = Color(0xFF5E82D6),
                inkLight = Color(0xFF3A5AA8),
                inkDark = Color(0xFFA8C0F0),
                bubble = Color(0xFF23386E),
            ),
            AccentPalette(
                id = "ruby_red", label = "红宝石",
                fill = Color(0xFFD9534F),
                inkLight = Color(0xFF9E3330),
                inkDark = Color(0xFFF2A19E),
                bubble = Color(0xFF5E1E1C),
            ),
            AccentPalette(
                id = "botanist_green", label = "园艺绿",
                fill = Color(0xFF4F9D4A),
                inkLight = Color(0xFF2E6B2B),
                inkDark = Color(0xFF9AD495),
                bubble = Color(0xFF1E4A1C),
            ),
            AccentPalette(
                id = "sunset_orange", label = "落日橙",
                fill = Color(0xFFE08A3C),
                inkLight = Color(0xFF95531A),
                inkDark = Color(0xFFF4BE86),
                bubble = Color(0xFF5C3410),
            ),
            AccentPalette(
                id = "lalafell_pink", label = "拉拉粉",
                fill = Color(0xFFE573A8),
                inkLight = Color(0xFFA83E70),
                inkDark = Color(0xFFF6AFCB),
                bubble = Color(0xFF632341),
            ),
            AccentPalette(
                id = "ink_slate", label = "墨石灰",
                fill = Color(0xFF6E7681),
                inkLight = Color(0xFF474E57),
                inkDark = Color(0xFFB6BDC6),
                bubble = Color(0xFF2C3138),
            ),
        )

        /**
         * 默认是**水晶青**，不是列表第一个的石之家金。
         *
         * 石之家金放在第一位是因为它是站点自己的品牌色（想和网页一致就选它），
         * 但它不该是默认：这个模块的设计体系定的是板岩 + 水晶青，
         * 而金色和紫色都是明确说过不喜欢的两支。默认值等于"没选之前给什么"，
         * 给一支被否过的颜色不合适——摆在预设里让人主动选是另一回事。
         *
         * 0.7.236 我把默认写成了 presets.first()（金），是个错。
         */
        // v2 default: the locked "Morning Dew" green. Existing users keep whatever
        // accent they picked (accentId is persisted); this only decides first-run.
        val default: AccentPalette = byIdOrFirst("dew_green")

        private fun byIdOrFirst(id: String): AccentPalette =
            presets.firstOrNull { it.id == id } ?: presets.first()

        fun byId(id: String): AccentPalette? = presets.firstOrNull { it.id == id }
    }
}

/**
 * 当前主题色。由 [EorzeaPhoneTheme] 提供，全 App 的 accent getter 都读它。
 * 默认给石之家金，这样即使某处漏了 Provider 也不会崩。
 */
val LocalAccent = staticCompositionLocalOf { AccentPalette.default }

/** 当前主题色，取用方便。 */
val CurrentAccent: AccentPalette
    @Composable get() = LocalAccent.current
