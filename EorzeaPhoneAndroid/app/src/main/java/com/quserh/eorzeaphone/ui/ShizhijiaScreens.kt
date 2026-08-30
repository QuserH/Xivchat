package com.quserh.eorzeaphone.ui

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.draw
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.quserh.eorzeaphone.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
// 职业名/分组用 Wiki 的本地字典：石之家自己的 ShizhijiaJob 要现拉 recruit 接口，
// 而且 id 是另一套字符串。WikiDicts 的 id 空间和幻化 job_ids 对得上（拿 8 篇真帖验过）。
import com.quserh.eorzeaphone.data.wiki.WikiDicts
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaApi
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaCosUpload
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaArea
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaBoundCharacter
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaCareer
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaComment
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaFriendRoster
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaRecruit
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaRecruitDetail
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaRecruitForm
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaRecruitFilter
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaRecruitKind
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaFbConfig
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaJob
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSlot
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaGuildMember
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaGuildMembers
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaGuildPhoto
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaDynamic
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaLoginUser
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPostCard
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPostDetail
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaPostPart
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaProbe
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaImageLoader
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSession
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSignLog
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSignReward
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSearchUser
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSearchGlamour
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaUserProfile
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaGlamourDetail
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaGlamourDye
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaGlamourCard
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaRecentEvent
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// 石之家设计体系（只作用于石之家，不污染全局主题）
//
// 取材：石之家在摩杜纳——低饱和蓝灰岩、雾气、银泪湖水晶阵的青光。
// 所以基调是冷调板岩 + 矿物水晶青，而不是金、紫或米白。
//
// 结构上抛弃"细线描边的平面板"，改成有厚度的石板：柔和分层阴影 + 顶边高光，
// 描边只在浅色模式做极淡的一层，负责在白底上收边。
//
// SzjShard（锥形水晶棱条）只留在**选中态**上——那里它是在回答"你在哪一档"。
// 分区标题前面的那一枚已经全部去掉：每个标题前顶一枚同样的小图形不带任何信息，
// 只是把标题的重心拽偏。分区靠字重和间距分，不靠装饰。
// ---------------------------------------------------------------------------

// 深色：板岩夜。近黑但偏蓝，卡片逐层抬升。
// 卡片明度是调过的：原来 #1A1F26 和底 #12161B 只差 ~4%，而 3dp 阴影在近黑底上
// 根本看不见，结果深色模式糊成一整块灰板，"石板卡片"这个概念只在浅色模式成立。
// 现在拉到 ~9%，靠明度本身分层，不指望阴影。
// 深色偏暖：官网只有浅色，深色这套是按同一个金推的暖灰，
// 不再是冷调板岩——冷灰配金会显脏。
private val SzjDarkBg = Color(0xFF101613)          // 暖墨底
private val SzjDarkCard = Color(0xFF181F1A)        // 石板
private val SzjDarkCardRaised = Color(0xFF232D26)  // 抬升层
private val SzjDarkAccent = Color(0xFF7FC49A)      // 金（深底上的字色，8.6:1）
private val SzjDarkAccentSoft = Color(0xFF1E2E24)  // 金光残留
private val SzjDarkOnAccentSoft = Color(0xFFA8D8B4)
private val SzjDarkText = Color(0xFFE2EAE2)
private val SzjDarkMuted = Color(0xFF93A39A)
private val SzjDarkLine = Color(0xFF26312A)
private val SzjDarkHairline = Color(0xFF37453C)
// 顶边高光原来 0x14（8%），在提亮后的卡片上等于没有。石面受光要看得见才算受光。
private val SzjDarkEdge = Color(0x24FFFFFF)        // 石板顶边高光

// 浅色：直接照官网的中性色。#f2f2f2 页底、#fff 卡片、#1f1f1f/#4b4b4b 文字、
// #9c9c9c 次要、#fbf9f4 金的浅底——全部取自 mob 的 app.css。
private val SzjLightBg = Color(0xFFF0F3F7)         // 官网页底
private val SzjLightCard = Color(0xFFFFFFFF)       // 白卡
private val SzjLightCardRaised = Color(0xFFECF0F5) // 抬升层（官网 #f5f5f5）
// 官网把 #c4a86a 也拿来写小字（白底 2.17:1），那是它的无障碍问题，不照抄。
// 文字用的金压深到 5.2:1；实心填充仍用官网原值 SzjAccentFill。
private val SzjLightAccent = Color(0xFF2F6B40)     // 金字（白底 5.2:1）
private val SzjLightAccentSoft = Color(0xFFEDF4EA) // 官网 .is-selected 的底
private val SzjLightOnAccentSoft = Color(0xFF27553A)
private val SzjLightText = Color(0xFF1F2730)       // 官网正文
// 官网次要色 #9c9c9c 在 #f2f2f2 上只有 2.6:1，元信息是 11sp 小字，
// 用官网的另一档 #4b4b4b 系推到 5.0 以上。
private val SzjLightMuted = Color(0xFF5D6874)
private val SzjLightLine = Color(0xFFE2E7ED)       // 官网 #e5e5e5
private val SzjLightHairline = Color(0xFFC4CDD6)   // 官网 #c2c2c2
private val SzjLightEdge = Color(0x0A000000)

internal val szjLight: Boolean @Composable get() = MaterialTheme.colorScheme.background.luminance() > 0.5f

internal val SzjBg: Color @Composable get() = if (szjLight) SzjLightBg else SzjDarkBg
internal val SzjCard: Color @Composable get() = if (szjLight) SzjLightCard else SzjDarkCard
internal val SzjCardRaised: Color @Composable get() = if (szjLight) SzjLightCardRaised else SzjDarkCardRaised
// 强调色跟设置里选的主题色走（默认石之家金）。
// 中性色（底/卡/文字/线）不跟着换——那套是照石之家的中性色定的。
internal val SzjAccent: Color @Composable get() = com.quserh.eorzeaphone.ui.theme.PhoneAccent
internal val SzjAccentSoft: Color @Composable get() = if (szjLight) SzjLightAccentSoft else SzjDarkAccentSoft
internal val SzjOnAccentSoft: Color @Composable get() = if (szjLight) SzjLightOnAccentSoft else SzjDarkOnAccentSoft
internal val SzjText: Color @Composable get() = if (szjLight) SzjLightText else SzjDarkText
internal val SzjMuted: Color @Composable get() = if (szjLight) SzjLightMuted else SzjDarkMuted
internal val SzjLine: Color @Composable get() = if (szjLight) SzjLightLine else SzjDarkLine
internal val SzjHairline: Color @Composable get() = if (szjLight) SzjLightHairline else SzjDarkHairline
internal val SzjEdge: Color @Composable get() = if (szjLight) SzjLightEdge else SzjDarkEdge
/**
 * 实心填充用的强调色（按钮、选中态的底），上面配 [SzjOnAccent]。
 *
 * 和 [SzjAccent] 分工：那个是**文字/图标**用的（够对比度），这个是**底色**用的。
 * 默认那一套就是官网原值 #c4a86a，用法也和官网一样：
 * `.active{background-color:#c4a86a;color:#fff}`。
 * 设置里换主题色时这里跟着变。
 */
internal val SzjAccentFill: Color @Composable get() = com.quserh.eorzeaphone.ui.theme.BrandFill

/** 落在 [SzjAccentFill] 上的字色。 */
internal val SzjOnAccent: Color @Composable get() = com.quserh.eorzeaphone.ui.theme.BrandOnFill

// ---- 形状：卡片舒展，控件收紧。三档而不是一档，层级靠圆角区分。 ----
internal val SzjCardShape = RoundedCornerShape(18.dp)
internal val SzjInnerShape = RoundedCornerShape(12.dp)
internal val SzjChipShape = RoundedCornerShape(12.dp)

// ---- 排版：没有可用的中文显示字体（项目内 AXIS 只有图标字形），
// 所以人格靠字号跨度、字重和字距，而不是字体家族。
// 元信息统一用宽字距小字，和正文形成"标签 vs 内容"的对照。
internal val SzjMetaStyle = TextStyle(fontSize = 11.sp, letterSpacing = 0.4.sp, fontWeight = FontWeight.Medium)
internal val SzjLabelStyle = TextStyle(fontSize = 12.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold)

// ---- 动效 ----
/** 系统「减少动画」开着时把动效降到 0，无障碍设置优先于观感。 */
@Composable
internal fun szjMotionEnabled(): Boolean {
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

/** 按压回弹：低刚度弹簧，手指离开时"浮"回来而不是弹回来。 */
internal val SzjPressSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 420f)
private val SzjMorphSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 320f)
private const val SZJ_ENTER_MS = 260
private const val SZJ_STAGGER_MS = 26

/**
 * 锥形水晶棱条。两头收尖的细长六边形，取自银泪湖水晶阵的形状。
 *
 * **只用在选中态**——在那里它回答"你在哪一档"，是带信息的。
 * 不要拿它当分区标题的前缀装饰：每个标题前一枚同样的图形不说明任何事情。
 */
@Composable
internal fun SzjShard(
    widthDp: Int = 3,
    heightDp: Int = 18,
    color: Color = SzjAccent,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.Canvas(modifier.size(widthDp.dp, heightDp.dp)) {
        val w = size.width
        val h = size.height
        val taper = h * 0.22f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(w / 2f, 0f)
            lineTo(w, taper)
            lineTo(w, h - taper)
            lineTo(w / 2f, h)
            lineTo(0f, h - taper)
            lineTo(0f, taper)
            close()
        }
        drawPath(path, color)
    }
}

/**
 * 石板卡片：柔和阴影撑出厚度，顶边一道高光模拟打磨过的石面，
 * 按下时下沉（缩放 + 阴影收窄），松手弹回。整个石之家的卡片都走这里，
 * 所以"手感"在全模块是一致的。
 */
@Composable
internal fun SzjCardSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = SzjCardShape,
    onClick: (() -> Unit)? = null,
    raised: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = szjMotionEnabled()
    val target = if (pressed && motion && onClick != null) 0.978f else 1f
    val scale by animateFloatAsState(target, SzjPressSpring, label = "szjCardPress")
    val elevation by animateDpAsState(
        if (pressed && onClick != null) 1.dp else if (raised) 6.dp else 3.dp,
        tween(140),
        label = "szjCardElev",
    )
    val light = szjLight
    Column(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(elevation, shape, ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
            .clip(shape)
            .background(if (raised) SzjCardRaised else SzjCard)
            // 浅色模式白卡在薄雾底上需要一道极淡收边；深色靠阴影就够。
            .then(if (light) Modifier.border(1.dp, SzjLine, shape) else Modifier)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else Modifier
            ),
    ) {
        // 顶边高光：石面打磨出的那一线反光，只有 1dp，深色模式下最明显。
        Box(
            Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(
                    listOf(Color.Transparent, SzjEdge, SzjEdge, Color.Transparent)
                )
            )
        )
        content()
    }
}

/**
 * 列表项入场：淡入 + 小幅上浮，按序号错开。
 * 只在首屏那几项做（index < 10），翻页加载出来的不再逐个动，
 * 否则无限滚动会一直有东西在动，反而显得廉价。
 */
@Composable
internal fun SzjRise(index: Int, content: @Composable () -> Unit) {
    val motion = szjMotionEnabled()
    if (!motion || index >= 10) { content(); return }
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val p by animateFloatAsState(
        if (shown) 1f else 0f,
        tween(SZJ_ENTER_MS, delayMillis = index * SZJ_STAGGER_MS, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "szjRise",
    )
    Box(Modifier.graphicsLayer { alpha = p; translationY = (1f - p) * 22f }) { content() }
}

/**
 * 底栏显隐状态。
 *
 * 往下滑（看后面的内容）时收起，往上滑或回到顶部时放出来。用累积量而不是
 * 单帧 delta 判断方向：手指抖一下不该让底栏闪。累积量夹在 ±阈值附近，
 * 所以反向滑一小段就能立刻翻转，不需要先把之前攒的量抵消完。
 */
@androidx.compose.runtime.Stable
private class SzjBarVisibility {
    var hidden by mutableStateOf(false)
        private set

    private var acc = 0f

    fun onScroll(delta: Float) {
        when {
            // delta < 0：内容上移，也就是手指往上滑、在往下看。
            delta < -0.5f -> {
                acc = (acc + delta).coerceAtLeast(-HIDE_AFTER * 1.5f)
                if (acc <= -HIDE_AFTER) hidden = true
            }
            delta > 0.5f -> {
                acc = (acc + delta).coerceAtMost(SHOW_AFTER * 1.5f)
                if (acc >= SHOW_AFTER) hidden = false
            }
        }
    }

    /** 回到列表顶部、或切换分区时，底栏必须是可见的。 */
    fun reveal() {
        hidden = false
        acc = 0f
    }

    private companion object {
        // 往下滑约 60dp 才收起（避免轻碰就消失）；往上滑 20dp 就放出来，
        // 因为用户想点底栏时希望它立刻回来。
        const val HIDE_AFTER = 60f
        const val SHOW_AFTER = 20f
    }
}

/** 把列表的滚动量喂给底栏状态。挂在滚动容器外层。 */
private fun szjBarNestedScroll(bar: SzjBarVisibility): androidx.compose.ui.input.nestedscroll.NestedScrollConnection =
    object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
        override fun onPostScroll(
            consumed: androidx.compose.ui.geometry.Offset,
            available: androidx.compose.ui.geometry.Offset,
            source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
        ): androidx.compose.ui.geometry.Offset {
            // available.y > 0 且 consumed.y == 0 表示已经到顶还在下拉，
            // 这种情况直接放出底栏。
            if (consumed.y == 0f && available.y > 0f) bar.reveal() else bar.onScroll(consumed.y)
            return androidx.compose.ui.geometry.Offset.Zero
        }
    }

/**
 * 分区切换转场：淡入淡出叠一层小幅横移，方向跟着 tab 前后关系走。
 * 位移只有屏宽的 1/12——够表达"往右翻了一页"，又不会让人等动画结束。
 */
private fun szjTabTransition(forward: Boolean): androidx.compose.animation.ContentTransform {
    val dir = if (forward) 1 else -1
    val enter = fadeIn(tween(200, delayMillis = 40)) +
        slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { w -> dir * w / 12 }
    val exit = fadeOut(tween(140)) +
        slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { w -> -dir * w / 12 }
    // sizeTransform 关掉裁剪：两个分区高度不同时不要卡一下再对齐。
    return androidx.compose.animation.ContentTransform(enter, exit, 0f, SizeTransform(clip = false))
}

/** 骨架微光：首屏加载用它替代转圈，先把即将出现的卡片形状占住。 */
@Composable
private fun SzjShimmerBox(modifier: Modifier, shape: RoundedCornerShape = SzjInnerShape) {
    val motion = szjMotionEnabled()
    val base = SzjCardRaised
    if (!motion) {
        Box(modifier.clip(shape).background(base))
        return
    }
    val transition = rememberInfiniteTransition(label = "szjShimmer")
    val x by transition.animateFloat(
        initialValue = -1.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(tween(1250, easing = LinearEasing), RepeatMode.Restart),
        label = "szjShimmerX",
    )
    val glow = if (szjLight) Color(0x40FFFFFF) else Color(0x18FFFFFF)
    Box(
        modifier.clip(shape).background(base).drawWithContent {
            drawContent()
            drawRect(
                Brush.linearGradient(
                    colors = listOf(Color.Transparent, glow, Color.Transparent),
                    start = androidx.compose.ui.geometry.Offset(x * size.width, 0f),
                    end = androidx.compose.ui.geometry.Offset((x + 0.7f) * size.width, size.height),
                )
            )
        }
    )
}

/**
 * 按接口返回的状态码给出对应的空态。
 *
 * 这解决了一个真实的 bug：以前所有失败都被压成空列表，所以已经登录的人
 * 打开收藏/招募管理也会看到"请登录"。现在只有服务端真的回 10403 才说未登录。
 *
 * @param onLogin 未登录时的登录入口
 * @param emptyTitle / emptyHint 请求成功但列表为空时的说法
 */
@Composable
internal fun <T> SzjResState(
    res: ShizhijiaApi.Res<T>?,
    emptyTitle: String,
    emptyHint: String? = null,
    onLogin: (() -> Unit)? = null,
    inline: Boolean = false,
) {
    // action 用具名参数传：SzjEmpty/SzjEmptyInline 的第三个位置现在是 iconRes。
    val empty: @Composable (String, String?, (@Composable () -> Unit)?) -> Unit =
        if (inline) { t, h, a -> SzjEmptyInline(t, h, action = a) } else { t, h, a -> SzjEmpty(t, h, action = a) }
    when (res) {
        null -> empty("正在读取", null, null)
        is ShizhijiaApi.Res.NeedLogin -> empty(
            "需要登录石之家账号",
            "登录后这里会显示你的内容",
        ) { if (onLogin != null) SzjPrimaryButton("登录", onClick = onLogin) }
        is ShizhijiaApi.Res.NeedCharacter -> empty(
            "账号还没绑定角色",
            "石之家要求先绑定一个 FF14 角色，才会返回这部分数据",
            null,
        )
        is ShizhijiaApi.Res.Failed -> empty(
            "没读取到",
            res.msg.ifBlank { if (res.code == null) "网络没通，检查一下连接" else "服务端返回 ${res.code}" },
            null,
        )
        is ShizhijiaApi.Res.Ok -> empty(emptyTitle, emptyHint, null)
    }
}

/**
 * 空态：一句说明现状，一句给下一步。空屏是邀请动作的地方，
 * 不是只写"暂无内容"的地方。
 *
 * 锚点原来是一根棱条——棱条现在只留给一级分区标题和选中态，这里换成真图标
 * （[iconRes]，默认通用空箱）。版式和全局的 PhoneEmpty 一致，只是配色走石之家。
 */
@Composable
internal fun SzjEmpty(
    title: String,
    hint: String? = null,
    iconRes: Int = R.drawable.ic_empty_box,
    action: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.Center) {
        SzjEmptyBody(title, hint, iconRes, action)
    }
}

/** SzjEmpty / SzjEmptyInline 共用的那一列内容。 */
@Composable
private fun SzjEmptyBody(
    title: String,
    hint: String?,
    iconRes: Int,
    action: (@Composable () -> Unit)?,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
        ImageGlyph(iconRes, SzjMuted.copy(alpha = 0.55f), Modifier.size(38.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
        if (hint != null) {
            Spacer(Modifier.height(6.dp))
            Text(hint, color = SzjMuted, style = SzjMetaStyle, textAlign = TextAlign.Center, lineHeight = 17.sp)
        }
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

/** 主按钮：实心金 + 白字（官网 `.active` 就是这一对），按下缩一下。 */
@Composable
internal fun SzjPrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SzjPressable(onClick = onClick, modifier = modifier, shape = SzjInnerShape) {
        Text(
            label,
            color = SzjOnAccent,
            style = SzjLabelStyle,
            modifier = Modifier.clip(SzjInnerShape).background(SzjAccentFill).padding(horizontal = 22.dp, vertical = 10.dp),
        )
    }
}

/** 帖子流的骨架屏：三张卡片的轮廓，比转圈更能说明"马上出来什么"。 */
@Composable
private fun SzjFeedSkeleton() {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        repeat(3) {
            SzjCardSurface(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(14.dp)) {
                    SzjShimmerBox(Modifier.fillMaxWidth(0.68f).height(15.dp), RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        repeat(3) { SzjShimmerBox(Modifier.weight(1f).aspectRatio(1f)) }
                    }
                    Spacer(Modifier.height(12.dp))
                    SzjShimmerBox(Modifier.fillMaxWidth(0.34f).height(11.dp), RoundedCornerShape(4.dp))
                }
            }
        }
    }
}

/**
 * 子页页头：返回箭头 + 标题。
 *
 * 标题左边原来有一枚水晶棱条当"签名"。它不带任何信息——每一页都是同一枚，
 * 只是装饰，而且四字标题前面顶一个小图形，重心被拽偏。已经去掉。
 * 现在和工具屏的 ScreenHeader 完全一样：左返回 + 居中 20sp SemiBold 标题。
 */
@Composable
internal fun SzjHeader(title: String, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val backScale by animateFloatAsState(if (pressed) 0.86f else 1f, SzjPressSpring, label = "szjBack")
    // 三栏 Row，不是"Box + 三个 alignment 互相叠"。后者的标题靠写死的
    // horizontal padding 躲开两边的按钮，右边一多按钮就重合（聊天页头栽过）。
    Row(
        Modifier.fillMaxWidth().background(SzjBg).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(42.dp), contentAlignment = Alignment.CenterStart) {
            ImageGlyph(
                R.drawable.ic_back,
                SzjAccent,
                Modifier
                    .graphicsLayer { scaleX = backScale; scaleY = backScale }
                    .size(30.dp).clip(RoundedCornerShape(10.dp))
                    .clickable(interactionSource = interaction, indication = null, onClick = { onBack?.invoke() })
                    .padding(horizontal = 2.dp, vertical = 6.dp),
            )
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                title,
                color = SzjText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            Modifier.widthIn(min = 42.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            trailing?.invoke()
        }
    }
}

/**
 * 石之家 (FF14 Rising Stones official community) - the in-phone "app".
 *
 * Renders the forum through the public JSON API: a post feed with partitions,
 * post detail (HTML body), comments, search, and the login-gated dynamics feed.
 *
 * 自成一套设计语言：**冷调板岩 + 水晶青**（摩杜纳/银泪湖的水晶阵意象）——
 * 深色是板岩夜（近黑偏蓝的底 + 逐层抬升的石板卡片 + 顶边受光高光 + 屏顶一层
 * 极淡的青色环境光），浅色是晨雾（薄雾冷底 + 白石板 + 深水晶青）。
 * 水晶青棱条（`SzjShard`）只用在选中态，不再当分区标题的装饰前缀。
 * 页头骨架、字号、返回键和全局的 `ScreenHeader` 一致，配色是自己的。
 * Internal navigation uses a simple back stack so the system back button walks
 * out level-by-level.
 */

private sealed interface SzjRoute {
    data object Home : SzjRoute
    data class PostDetail(val postId: String) : SzjRoute
    data class DynamicDetail(val id: String) : SzjRoute
    data object Search : SzjRoute
    data object Login : SzjRoute
    data object SignCalendar : SzjRoute
    data class UserProfile(val uuid: String) : SzjRoute
    /** 关注列表 / 粉丝列表。[fans] = true 看粉丝。 */
    data class RelationList(val uuid: String, val fans: Boolean, val who: String = "") : SzjRoute
    data class GlamourDetail(val glamourId: String) : SzjRoute
    /** 我收藏的帖子（userInfo/myStarPosts，需登录）。 */
    data object Favorites : SzjRoute
    /** 我发布的招募 + 一键擦亮（需登录）。 */
    data object MyRecruits : SzjRoute
    /** 当前角色 + 换绑。 */
    data object Characters : SzjRoute
    /** 我的部队主页（部队 id = 当前角色的 fc_id）。 */
    data object MyGuild : SzjRoute
    /** 部队照片墙的单张详情 + 评论。 */
    data class GuildPhotoDetail(val photoId: String) : SzjRoute
    /** 招募详情（四类共用一个页面，接口按 kind 分）。 */
    data class RecruitDetail(val kind: ShizhijiaRecruitKind, val id: String) : SzjRoute
    /** 发布招募（副本 / 新人 / 其他）。 */
    data class PublishRecruit(val kind: ShizhijiaRecruitKind) : SzjRoute

    /** 发帖 / 发攻略。同一个界面，[strategy] 决定 type 和版块字典。 */
    data class PublishPost(val strategy: Boolean = false) : SzjRoute

    /** 发幻化。 */
    data object PublishGlamour : SzjRoute
    /** 专项数据（官网的数据中心，7 个分类）。 */
    data object Statistics : SzjRoute
}

/** App-wide full-screen image viewer state; any thumbnail sets its URL here. */
object SzjViewer {
    /**
     * 当前在看的那一张。设它就等于打开查看器（[urls] 会被当成只有这一张）。
     * 兼容原来那些只有一张图的调用点。
     */
    var url by mutableStateOf<String?>(null)

    /**
     * 这一组图的全部 url。**有多张时查看器可以左右滑动切换**——
     * 原来只有 [url] 一个字段，查看器压根拿不到列表，所以点开只能看那一张，
     * 想看下一张得退出去再点，这是做漏了不是设计。
     */
    var urls by mutableStateOf<List<String>>(emptyList())

    /** 打开查看器：给整组图 + 从第几张开始。 */
    fun open(all: List<String>, index: Int) {
        val clean = all.filter { it.isNotBlank() }
        if (clean.isEmpty()) return
        urls = clean
        url = clean[index.coerceIn(clean.indices)]
    }

    fun close() {
        url = null
        urls = emptyList()
    }

    /** 当前这张在组里的下标；组里没有它（单图调用）时给 0。 */
    val startIndex: Int get() = urls.indexOf(url).coerceAtLeast(0)

    /** 查看器实际要翻的那组图。urls 为空时退化成只有 url 这一张。 */
    val effective: List<String> get() = urls.ifEmpty { listOfNotNull(url) }
}

/**
 * 石之家的导航与分区状态。
 *
 * 必须活在 composable 外面：ShizhijiaScreen 挂在外层 AnimatedContent 里，
 * 回桌面再进来这个 composable 是重建的，`remember` 的东西全丢——
 * 原来退出去再进来总是回到社区首页，就是这个原因。
 * 放到 object 里之后，返回栈、四个分区的选中项和各列表的数据都留着。
 */
private object SzjNav {
    var stack by mutableStateOf(listOf<SzjRoute>(SzjRoute.Home))
        private set

    /**
     * 动态流的重拉信号。发完动态自增，动态 Tab 的 LaunchedEffect 盯着它。
     *
     * 放在 SzjNav 里而不是 composable 里：发动态那一层挂在最外面（和 Tab 不同层），
     * 拿不到 Tab 内部的状态；而 SzjNav 本来就是为"跨重建保留状态"存在的。
     */
    val dynamicsReloadKey = mutableStateOf(0)

    /** 四个分区（社区/招募/幻化/我）和社区的二级 Tab。 */
    val mainTab = mutableStateOf(MAIN_COMMUNITY)
    val subTab = mutableStateOf(SUB_POSTS)

    // 各分区的数据缓存也挂这儿，回桌面再进来不用重新拉一遍。
    val posts = SzjPostsState()
    val strategy = SzjStrategyState()
    val recruit = SzjRecruitState()
    val glamour = SzjGlamourState()
    val search = SzjSearchState()

    /**
     * 「借道」栈：别处（比如联系人里点好友的石之家主页）跳进来时走这条，
     * 不碰上面那个 [stack]。
     *
     * 这样两件事同时成立：从好友跳进来看完一按返回就回好友详情；石之家自己
     * 原来停在哪一页、滑到哪儿，下次从桌面点进来还是老样子。
     */
    var guestStack by mutableStateOf(listOf<SzjRoute>())
        private set

    /** 借道模式：栈非空就说明是别处跳进来的。 */
    val isGuest: Boolean get() = guestStack.isNotEmpty()

    /** 当前该显示哪一页。 */
    val current: SzjRoute get() = guestStack.lastOrNull() ?: stack.last()

    /** 开始借道，[route] 是要看的那一页。 */
    fun enterGuest(route: SzjRoute) { guestStack = listOf(route) }

    /** 退出借道，回到调用方那一屏。 */
    fun leaveGuest() { guestStack = emptyList() }

    fun push(r: SzjRoute) {
        if (isGuest) guestStack = guestStack + r else stack = stack + r
    }

    fun pop() {
        if (isGuest) {
            // 借道栈只剩一页时不在这里弹空——由 ShizhijiaScreen 决定退回哪一屏。
            if (guestStack.size > 1) guestStack = guestStack.dropLast(1)
        } else if (stack.size > 1) {
            stack = stack.dropLast(1)
        }
    }

    fun selectTab(tab: Int) { mainTab.value = tab }

    /**
     * 返回键只在栈里还有上一页时被石之家吃掉。
     * 已经回到底部导航那一层（上面也没有覆盖层）时就穿透出去关掉石之家——
     * 不在四个分区之间回溯，那样要按好几次才出得去。
     *
     * 借道模式下一直吃掉：借道栈见底时也要由石之家自己收尾（退出借道并把
     * 屏幕还给调用方），不能穿透到桌面。
     */
    val canGoBack: Boolean get() = isGuest || stack.size > 1

    fun back() { pop() }
}

/**
 * Player avatar with the official default-portrait chain: custom photo →
 * per-race portrait (fetched lazily by uuid for players without a photo) →
 * letter chip. Mirrors how the official site treats missing avatars.
 */
/**
 * 定位图标（昵称与服务器之间的符号）。
 *
 * 原来固定用官方移动端的金色 #c4a86a，在板岩+水晶青的体系里成了孤立的暖色，
 * 而且它出现在几乎每张卡片上。它承载的是"服务器"这类元信息，
 * 所以跟随 SzjMuted，和它旁边的服务器名同一层级；水晶青留给选中态。
 */
@Composable
private fun SzjLocPin(sizeDp: Int = 16) {
    val ctx = LocalContext.current
    val tint = SzjMuted
    val pin = remember { runCatching { android.graphics.BitmapFactory.decodeStream(ctx.assets.open("loc_pin.png")) }.getOrNull() }
    if (pin != null) {
        Image(
            bitmap = pin.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
            modifier = Modifier.size(sizeDp.dp),
        )
    } else {
        ImageGlyph(R.drawable.ic_pin, tint, Modifier.size(sizeDp.dp))
    }
}


@Composable
private fun SzjAvatar(name: String, avatar: String, uuid: String, sizeDp: Int) {
    val context = LocalContext.current
    var url by remember(uuid) { mutableStateOf(avatar) }
    LaunchedEffect(uuid, avatar) {
        if (url.isBlank() && uuid.isNotBlank()) {
            url = ShizhijiaApi.resolveAvatar(context, uuid)
        }
    }
    // 头像用阴影托起来而不是描边圈住，和石板卡片同一套物理。
    Box(Modifier.size(sizeDp.dp)
        .shadow(2.dp, CircleShape, ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
        .clip(CircleShape).background(SzjCardRaised), contentAlignment = Alignment.Center) {
        if (url.isNotBlank()) {
            ShizhijiaRemoteImage(
                url = url,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                showPlaceholder = false,
            )
        } else {
            Text(name.take(1).ifBlank { "?" }, color = SzjMuted, fontSize = (sizeDp * 0.38f).sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Post-feed state held above the internal back stack so that opening a post
 * detail and returning does NOT lose the scroll position or loaded pages.
 */
private class SzjPostsState {
    val parts = mutableStateOf(listOf<ShizhijiaPostPart>())
    val partId = mutableStateOf("")
    val posts = mutableStateOf(listOf<ShizhijiaPostCard>())
    val page = mutableStateOf(1)
    val pageTime = mutableStateOf("")
    val loading = mutableStateOf(true)
    val seenPartId = mutableStateOf<String?>(null)
    val listState = androidx.compose.foundation.lazy.LazyListState()
}

/** 攻略流状态。结构和 SzjPostsState 一样，只是走 type=2 的接口。 */
private class SzjStrategyState {
    val parts = mutableStateOf(listOf<ShizhijiaPostPart>())
    val partId = mutableStateOf("")
    val posts = mutableStateOf(listOf<ShizhijiaPostCard>())
    val page = mutableStateOf(1)
    val pageTime = mutableStateOf("")
    val loading = mutableStateOf(true)
    val seenPartId = mutableStateOf<String?>(null)
    val listState = androidx.compose.foundation.lazy.LazyListState()
}

/**
 * 招募流状态。五类招募各自缓存自己的列表和页码，
 * 这样在分类之间来回切不用重新拉。
 */
private class SzjRecruitState {
    val kind = mutableStateOf(ShizhijiaRecruitKind.Fb)
    val items = mutableStateOf(mapOf<ShizhijiaRecruitKind, List<ShizhijiaRecruit>>())
    /** 每类最后一次请求的结果状态，用来区分未登录 / 空 / 失败。 */
    val status = mutableStateOf(mapOf<ShizhijiaRecruitKind, ShizhijiaApi.Res<List<ShizhijiaRecruit>>>())
    val pages = mutableStateOf(mapOf<ShizhijiaRecruitKind, Int>())
    val ended = mutableStateOf(setOf<ShizhijiaRecruitKind>())
    val loading = mutableStateOf(false)
    val listState = androidx.compose.foundation.lazy.LazyListState()

    /** 各类各自的筛选条件，切回来时保留。 */
    val filters = mutableStateOf(mapOf<ShizhijiaRecruitKind, ShizhijiaRecruitFilter>())
    val filterOpen = mutableStateOf(false)

    // 筛选面板要用的字典，只拉一次
    val fbConfig = mutableStateOf(listOf<ShizhijiaFbConfig>())
    val styles = mutableStateOf(listOf<Pair<String, String>>())
    val categories = mutableStateOf(listOf<Pair<String, String>>())
    val fbLabels = mutableStateOf(listOf<Pair<String, String>>())
    /** 职业字典，招募卡的位置图标按 id 反查。 */
    val jobs = mutableStateOf(mapOf<String, ShizhijiaJob>())
    /** 大区字典，招募大区筛选用。 */
    val areas = mutableStateOf(listOf<ShizhijiaArea>())
    val dictLoaded = mutableStateOf(false)

    fun listFor(k: ShizhijiaRecruitKind): List<ShizhijiaRecruit> = items.value[k].orEmpty()
    fun pageFor(k: ShizhijiaRecruitKind): Int = pages.value[k] ?: 0
    fun filterFor(k: ShizhijiaRecruitKind): ShizhijiaRecruitFilter =
        filters.value[k] ?: ShizhijiaRecruitFilter()

    fun setFilter(k: ShizhijiaRecruitKind, f: ShizhijiaRecruitFilter) {
        filters.value = filters.value + (k to f)
        // 条件变了，缓存的列表和分页作废
        items.value = items.value - k
        pages.value = pages.value - k
        ended.value = ended.value - k
        status.value = status.value - k
    }
}

/** Hoisted glamour feed state so it survives detail push/pop. */
private class SzjGlamourState {
    val tab = mutableStateOf(0)        // 0=全部 1=关注
    val sort = mutableStateOf(0)       // 0=推荐 1=最新 2=热门
    val items = mutableStateOf(listOf<ShizhijiaGlamourCard>())
    val loading = mutableStateOf(false)
    val page = mutableStateOf(1)
    val ended = mutableStateOf(false)
    val loadedKey = mutableStateOf("")
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState()
    // 筛选（服务端）
    val raceId = mutableStateOf(-1)
    val genderId = mutableStateOf(-1)
    val createTimeIdx = mutableStateOf(0)
    val filterOpen = mutableStateOf(false)
    /**
     * 职业筛（**客户端**）。-1 = 不限。
     *
     * 和上面三个不是一类：服务端的职业筛已经拆掉了（七个参数名都试过，
     * 见 [ShizhijiaApi.getGlamours]），只能拿返回里的 `job_ids` 自己滤。
     * 所以它**不进 [loadedKey]** —— 改职业不该重新拉流，只是把已有的行藏一部分。
     */
    val jobId = mutableStateOf(-1)
    /** 职业面板里当前展开的定位（"坦克"…）。只影响面板长什么样，不影响结果。 */
    val jobRole = mutableStateOf("")
    /** 勾上＝只看专属这个职业的，把通用款也滤掉。默认不勾，理由见 [ShizhijiaGlamourCard.universalJob]。 */
    val jobExclusive = mutableStateOf(false)
    /** 职业筛开着时自动翻了几页。给 [SzjGlamourTab] 的自动加载兜底，别一路翻到服务端尽头。 */
    val jobAutoPages = mutableStateOf(0)
}

// 搜索状态提升到模块根部：进详情再返回时保留关键词/类型/结果与滚动位置
class SzjSearchState {
    val query = mutableStateOf("")
    val searchType = mutableStateOf(ShizhijiaApi.SEARCH_TYPE_POST)
    val hotWords = mutableStateOf(listOf<String>())
    val history = mutableStateOf(listOf<Pair<String, Int>>())
    val postResults = mutableStateOf<List<ShizhijiaPostCard>?>(null)
    val userResults = mutableStateOf<List<ShizhijiaSearchUser>?>(null)
    val glamourResults = mutableStateOf<List<ShizhijiaSearchGlamour>?>(null)
    val searching = mutableStateOf(false)
    val page = mutableStateOf(1)
    val ended = mutableStateOf(false)
    val loadingMore = mutableStateOf(false)
    val glamourGridState = androidx.compose.foundation.lazy.grid.LazyGridState()
    val postListState = androidx.compose.foundation.lazy.LazyListState()
    val userListState = androidx.compose.foundation.lazy.LazyListState()

    /**
     * 回到"还没搜"的状态。进搜索页时调一次——
     * 状态本身要留着（翻进详情再回来结果还在），但重新进入应该是干净的。
     * 频道（searchType）保留，那是偏好不是结果。
     */
    fun reset() {
        query.value = ""
        postResults.value = null
        userResults.value = null
        glamourResults.value = null
        searching.value = false
        page.value = 1
        ended.value = false
        loadingMore.value = false
    }
}

@Composable
fun ShizhijiaScreen(state: PhoneState) {
    val context = LocalContext.current
    // 状态全部来自 SzjNav（object，活得比这个 composable 长）：
    // 回桌面再进来还在原来那一页，列表也不用重新拉。
    val postsState = SzjNav.posts
    val strategyState = SzjNav.strategy
    val recruitState = SzjNav.recruit
    val glamourState = SzjNav.glamour
    val searchState = SzjNav.search
    val homeMainTab = SzjNav.mainTab
    val homeSubTab = SzjNav.subTab
    var barHeight by remember { mutableStateOf(56f) }
    var barBottom by remember { mutableStateOf(ShizhijiaSession.bottomBarBottom(context)) }
    LaunchedEffect(Unit) { barHeight = ShizhijiaSession.bottomBarHeight(context) }
    // 借道模式（从联系人里点好友的石之家主页进来）：借道栈见底时不是穿透到
    // 桌面，而是把屏幕还给来源那一屏。
    val leaveGuest: () -> Unit = {
        SzjNav.leaveGuest()
        state.leaveShizhijiaGuest()
    }
    // 有上一页、或者分区有来路时吃掉返回键；都没有才穿透到桌面。
    BackHandler(enabled = SzjNav.canGoBack) {
        if (SzjNav.isGuest && SzjNav.guestStack.size == 1) leaveGuest() else SzjNav.back()
    }
    val route = SzjNav.current
    // nav pushes a destination; pop returns to the previous one (login success uses pop).
    val nav: (SzjRoute) -> Unit = { SzjNav.push(it) }
    // 借道栈见底时，"返回"意味着离开石之家回到来源屏，而不是弹栈。
    val pop: () -> Unit = {
        if (SzjNav.isGuest && SzjNav.guestStack.size == 1) leaveGuest() else SzjNav.pop()
    }
    Box(Modifier.fillMaxSize()) {
        when (route) {
SzjRoute.Home -> ShizhijiaHomeScreen(state, nav, postsState, strategyState, recruitState, glamourState, homeMainTab, homeSubTab, barHeightDp = barHeight, barBottomDp = barBottom, onBarHeightChange = { barHeight = it }, onBarBottomChange = { barBottom = it })
            is SzjRoute.PostDetail -> ShizhijiaPostDetailScreen(state, route.postId, pop, nav)
            is SzjRoute.DynamicDetail -> ShizhijiaDynamicDetailScreen(state, route.id, pop, nav)
            SzjRoute.Search -> ShizhijiaSearchScreen(state, pop, nav, searchState)
            SzjRoute.Login -> ShizhijiaLoginScreen(state, pop)
            SzjRoute.SignCalendar -> ShizhijiaSignCalendarScreen(state, pop)
            is SzjRoute.UserProfile -> ShizhijiaUserProfileScreen(state, route.uuid, pop, nav)
            is SzjRoute.RelationList -> ShizhijiaRelationListScreen(route.uuid, route.fans, route.who, pop, nav)
            is SzjRoute.GlamourDetail -> ShizhijiaGlamourDetailScreen(state, route.glamourId, pop, nav)
            SzjRoute.Favorites -> ShizhijiaFavoritesScreen(pop, nav)
            SzjRoute.MyRecruits -> ShizhijiaMyRecruitsScreen(pop, nav)
            SzjRoute.Characters -> ShizhijiaCharactersScreen(pop)
            SzjRoute.MyGuild -> ShizhijiaMyGuildScreen(pop, nav)
            is SzjRoute.GuildPhotoDetail -> ShizhijiaGuildPhotoDetailScreen(route.photoId, pop, nav)
            is SzjRoute.RecruitDetail -> ShizhijiaRecruitDetailScreen(route.kind, route.id, pop, nav)
            is SzjRoute.PublishRecruit -> ShizhijiaPublishRecruitScreen(route.kind, recruitState, pop, nav)
            is SzjRoute.PublishPost -> ShizhijiaPublishPostScreen(route.strategy, pop, nav)
            is SzjRoute.PublishGlamour -> ShizhijiaPublishGlamourScreen(pop, nav)
            SzjRoute.Statistics -> ShizhijiaStatisticsScreen(pop, onLogin = { nav(SzjRoute.Login) })
        }
        // 屏顶一层极淡的水晶青环境光，只在深色模式出现。深色板岩底本身很死，
        // 这层青光从顶部中央散开，呼应"水晶阵青光"，也是成本最低的气质提升。
        // 叠在内容之上（各屏的 ScreenFrame 背景是实色，压在下面看不见），
        // alpha 峰值 6%，不吃触摸，也不影响任何文字的对比度。
        SzjAmbientGlow()
        if (SzjViewer.url != null) {
            // Full-screen overlay for viewing a tapped image at size.
            // 传整组图 + 起始下标，多张时能左右滑。
            SzjPhotoViewer(
                all = SzjViewer.effective,
                startIndex = SzjViewer.startIndex,
                onClose = { SzjViewer.close() },
            )
        }
    }
}

/**
 * 石之家屏顶的水晶青环境光。
 *
 * 一个从顶部中央散开的径向渐变，半径约等于屏宽，向下 40% 处衰减到 0。
 * 浅色模式不画：薄雾底本来就有层次，再加一层青只会显得脏。
 */
@Composable
private fun SzjAmbientGlow() {
    if (szjLight) return
    val accent = SzjAccent
    Box(
        Modifier.fillMaxWidth().fillMaxHeight(0.42f).drawBehind {
            drawRect(
                Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = 0.06f), Color.Transparent),
                    center = Offset(size.width * 0.5f, 0f),
                    radius = size.width * 1.05f,
                )
            )
        }
    )
}

/**
 * 从别处（联系人里的好友详情）跳进石之家看某人的主页。
 *
 * 走借道栈：看完一按返回就回到调用处那一屏，石之家自己原来停在哪一页不受影响。
 */
fun openShizhijiaProfile(state: PhoneState, uuid: String) {
    if (uuid.isBlank()) return
    state.openShizhijiaGuest { SzjNav.enterGuest(SzjRoute.UserProfile(uuid)) }
}

/**
 * 丢掉借道栈。
 *
 * 回桌面或者打开别的 App 时要调：否则借道栈还留着，下次从桌面点石之家会直接
 * 落在别人的主页上，而且那一页的返回键还指着好友详情。
 */
fun clearShizhijiaGuest() = SzjNav.leaveGuest()

/**
 * 全屏看图：暗底、整图适配、右下角保存和关闭、返回键退出。
 *
 * **一组多张时可以左右滑动切换**（[all]）。原来这个查看器只收一个 url，
 * 所以点开只能看那一张，想看下一张得退出去再点下一张——那是做漏了。
 *
 * 双指缩放和左右翻页会抢手势，处理办法：**放大之后就不翻页**
 * （`userScrollEnabled = scale <= 1f`）。放大了的时候横向拖动的意图是
 * 平移看细节，不是翻页；缩回 1 倍才恢复翻页。
 */
@Composable
private fun SzjPhotoViewer(all: List<String>, startIndex: Int, onClose: () -> Unit) {
    BackHandler { onClose() }
    val context = LocalContext.current
    val pager = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = startIndex.coerceIn(0, (all.size - 1).coerceAtLeast(0)),
        pageCount = { all.size },
    )
    val url = all.getOrNull(pager.currentPage) ?: all.firstOrNull().orEmpty()
    var bmp by remember(url) { mutableStateOf(if (url.startsWith("data:image")) decodeDataUri(url) else ShizhijiaImageLoader.peek(url)) }
    LaunchedEffect(url) { if (!url.startsWith("data:image")) bmp = ShizhijiaImageLoader.load(context, url) }
    // Pinch-zoom + pan. All gestures are consumed here so the list underneath
    // never scrolls while the viewer is open.
    // 换页时把缩放和位移复位：带着上一张的放大倍数翻到下一张很怪。
    var scale by remember(pager.currentPage) { mutableStateOf(1f) }
    var offset by remember(pager.currentPage) { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    // 保存到相册。长按和右下角的按钮走同一个动作——**长按是不可见的入口**，
    // 只做长按的话没人知道能存（用户就是这么报上来的），所以两个都给。
    val saveScope = rememberCoroutineScope()
    var saving by remember(url) { mutableStateOf(false) }
    val doSave: () -> Unit = {
        val b = bmp
        when {
            saving -> Unit
            b == null -> android.widget.Toast.makeText(context, "图片还没加载完", android.widget.Toast.LENGTH_SHORT).show()
            else -> {
                saving = true
                saveScope.launch {
                    // 压缩 + 写文件放到 IO 线程，别卡住手势和动画。
                    val err = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        SaveImage.toGallery(context, b, SaveImage.nameFromUrl(url))
                    }
                    android.widget.Toast.makeText(
                        context,
                        err ?: "已保存到相册",
                        android.widget.Toast.LENGTH_SHORT,
                    ).show()
                    saving = false
                }
            }
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xE6000000))
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(1f, 5f)
                offset = if (scale > 1f) offset + pan else androidx.compose.ui.geometry.Offset.Zero
            }
        }
        .pointerInput(url) {
            detectTapGestures(
                onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f },
                onLongPress = { doSave() },
            )
        }
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            // 放大之后不翻页：那时候横向拖动是"平移看细节"，不是"下一张"。
            userScrollEnabled = all.size > 1 && scale <= 1f,
            // 预加载左右各一张，滑过去不用等。
            beyondViewportPageCount = 1,
        ) { page ->
            val pageUrl = all.getOrNull(page).orEmpty()
            // 每一页各自加载。当前页那张同时也存进 bmp，供保存用。
            var pageBmp by remember(pageUrl) {
                mutableStateOf(
                    if (pageUrl.startsWith("data:image")) decodeDataUri(pageUrl)
                    else ShizhijiaImageLoader.peek(pageUrl)
                )
            }
            LaunchedEffect(pageUrl) {
                if (!pageUrl.startsWith("data:image")) pageBmp = ShizhijiaImageLoader.load(context, pageUrl)
            }
            val b = pageBmp
            if (b != null) {
                Image(
                    b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        // 缩放只作用在**当前页**，翻到别页时那页是 1 倍。
                        val on = page == pager.currentPage
                        scaleX = if (on) scale else 1f
                        scaleY = if (on) scale else 1f
                        translationX = if (on) offset.x else 0f
                        translationY = if (on) offset.y else 0f
                    },
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(30.dp))
                }
            }
        }
        // 页码。多于一张才显示——只有一张时它是废话。
        if (all.size > 1) {
            Text(
                "${pager.currentPage + 1} / ${all.size}",
                color = Color(0xFFE8EDF2), style = SzjMetaStyle,
                modifier = Modifier.align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 14.dp)
                    .clip(SzjChipShape).background(Color(0xB3232932))
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            )
        }
        // 右下角：保存 + 关闭。查看器始终是暗底，所以这里固定用亮色，
        // 不跟随浅色主题（和传送横幅同一个理由）。
        Row(
            Modifier.align(Alignment.BottomEnd).padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SzjPressable(onClick = doSave, shape = SzjChipShape) {
                Row(
                    Modifier.clip(SzjChipShape).background(Color(0xB3232932))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            color = Color(0xFFE8EDF2), strokeWidth = 2.dp,
                            modifier = Modifier.size(15.dp),
                        )
                    } else {
                        ImageGlyph(R.drawable.ic_download, Color(0xFFE8EDF2), Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(if (saving) "保存中" else "保存", color = Color(0xFFE8EDF2), style = SzjLabelStyle)
                }
            }
            SzjPressable(onClick = onClose, shape = CircleShape) {
                Box(
                    Modifier.clip(CircleShape).background(Color(0xB3232932))
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) { ImageGlyph(R.drawable.ic_close, Color(0xFFE8EDF2), Modifier.size(18.dp)) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Home: partition chips + post feed / dynamics feed tabs
// ---------------------------------------------------------------------------

private const val MAIN_COMMUNITY = 0
private const val MAIN_RECRUIT = 1
private const val MAIN_GLAMOUR = 2
private const val MAIN_ME = 3
private const val SUB_POSTS = 0
private const val SUB_DYNAMICS = 1
private const val SUB_GUIDE = 2
private const val SUB_RECRUIT = 3

@Composable
private fun ShizhijiaHomeScreen(
    state: PhoneState,
    nav: (SzjRoute) -> Unit,
    postsState: SzjPostsState,
    strategyState: SzjStrategyState,
    recruitState: SzjRecruitState,
    glamourState: SzjGlamourState,
    mainTabState: MutableState<Int>,
    subTabState: MutableState<Int>,
    barHeightDp: Float,
    onBarHeightChange: (Float) -> Unit,
    barBottomDp: Float,
    onBarBottomChange: (Float) -> Unit,
) {
    val context = LocalContext.current
    var mainTab by mainTabState
    var subTab by subTabState
    // Login state drives the top bar and the dynamics tab.
    var loggedIn by remember { mutableStateOf(ShizhijiaSession.hasSession(context)) }
    // Hydrate from the persisted profile first so the top bar shows the real
    // character immediately (no 已登录→昵称 flash while the network call runs).
    var loginUser by remember { mutableStateOf(ShizhijiaSession.cachedLoginUser(context)) }
    val scope = rememberCoroutineScope()
    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    var signedToday by remember { mutableStateOf(ShizhijiaSession.signDate(context) == todayStr) }
    // Auto check-in once per day when logged in, and refresh the top bar state.
    LaunchedEffect(Unit) {
        val logged = ShizhijiaApi.isLoggedIn(context)
        loggedIn = logged
        if (logged) {
            loginUser = ShizhijiaApi.getLoginUser(context)
            loginUser?.let { ShizhijiaSession.cacheLoginUser(context, it) }
            android.util.Log.d("ShizhijiaLogin", "loginUser=${loginUser?.name} ava=${(loginUser?.avatar ?: "").take(50)}")
            if (!signedToday) {
                val ok = ShizhijiaApi.signIn(context)
                if (ok) { signedToday = true; ShizhijiaSession.setSignDate(context, todayStr); android.widget.Toast.makeText(context, "签到成功", android.widget.Toast.LENGTH_SHORT).show() }
            }
        } else {
            ShizhijiaSession.clearCachedUser(context)
            loginUser = null
        }
    }
    // Manual check-in from the top bar button. A duplicate check-in is rejected
    // by the server with a non-10000 code, so on failure we cross-check the
    // monthly sign log - when today shows up there the state still flips to 已签到.
    val onSignIn: () -> Unit = {
        scope.launch {
            val ok = ShizhijiaApi.signIn(context) || ShizhijiaApi.isSignedToday(context)
            if (ok) {
                signedToday = true
                ShizhijiaSession.setSignDate(context, todayStr)
                android.widget.Toast.makeText(context, "今日已签到", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "签到失败，请稍后再试", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 品牌行 + 账号卡不再固定在顶部，而是作为各分区滚动容器里的第一项，
    // 所以往下滑时它们跟着内容一起被顶出屏幕（原来是钉住不动）。
    // 标题行去掉了：进了石之家就知道是石之家，它只占地方。
    // 各分区顶部改成留一点呼吸的空隙。
    val brandRow: @Composable () -> Unit = {
        Spacer(Modifier.height(8.dp))
    }
    // 社区分区的完整头部：品牌行 + 账号卡。其他分区只要品牌行。
    val communityHeader: @Composable () -> Unit = {
        Column {
            brandRow()
            ShizhijiaTopBar(state, nav, loggedIn, loginUser, onSignIn, signedToday)
        }
    }

    val bar = remember { SzjBarVisibility() }
    // 发动态那一层。做成 Dialog 所以不进导航栈——发完就地关掉，
    // 不像发帖那样跳到新页面（动态没有详情页可跳）。
    var dynamicComposerOpen by remember { mutableStateOf(false) }
    // 换分区时底栏必须回来，否则切过去看不到当前选中项。
    LaunchedEffect(mainTab, subTab) { bar.reveal() }

    ScreenFrame(background = SzjBg) {
        Box(Modifier.fillMaxSize()) {
            // 底部四个分区之间横向滑动交叉切换：方向跟着 tab 序号走，
            // 左右移动和拇指在底栏上的动作一致。
            AnimatedContent(
                targetState = mainTab,
                transitionSpec = { szjTabTransition(targetState > initialState) },
                label = "szjMainTab",
                modifier = Modifier.fillMaxSize().nestedScroll(szjBarNestedScroll(bar)),
            ) { tab ->
                Column(Modifier.fillMaxSize()) {
                    when (tab) {
                        MAIN_COMMUNITY -> {
                            AnimatedContent(
                                targetState = subTab,
                                transitionSpec = { szjTabTransition(targetState > initialState) },
                                label = "szjSubTab",
                                modifier = Modifier.weight(1f),
                            ) { sub ->
                                // 头部和二级 Tab 都交给各 tab 自己放进滚动容器：
                                // 头部随内容滑走，Tab 行用 stickyHeader 钉在顶部，
                                // 这样滑下去之后还能直接切帖子/动态/攻略。
                                val subTabs: @Composable () -> Unit = { SzjSubTabRow(subTab) { subTab = it } }
                                when (sub) {
                                    SUB_POSTS -> ShizhijiaPostsTab(state, nav, postsState, communityHeader, subTabs)
                                    SUB_DYNAMICS -> ShizhijiaDynamicsTab(nav, loggedIn, communityHeader, subTabs)
                                    SUB_RECRUIT -> ShizhijiaRecruitTab(nav, loggedIn, recruitState, communityHeader, bar)
                                    else -> ShizhijiaStrategyTab(state, nav, strategyState, communityHeader, subTabs)
                                }
                            }
                        }
                        // bar 传下去：发布按钮跟底栏一起收起/放出。
                        MAIN_RECRUIT -> ShizhijiaRecruitTab(nav, loggedIn, recruitState, brandRow, bar)
                        MAIN_GLAMOUR -> ShizhijiaGlamourTab(nav, loggedIn, glamourState, brandRow)
                        else -> ShizhijiaMeTab(state, nav, loggedIn, loginUser, barHeightDp, barBottomDp, onBarHeightChange, onBarBottomChange, brandRow)
                    }
                }
            }
            // 发布入口。社区三个二级 Tab 都有，但**发的东西跟着当前 Tab 变**：
            // 帖子页发帖、攻略页发攻略、**动态页发动态**。
            //
            // 原来动态页故意不给入口，注释写的是"那是别人的动态流，
            // 在那儿放发帖是答错了问题"——前半句对，后半句的结论错了：
            // 该给的不是"发帖"，是"发动态"。而且动态才是**唯一能"只给自己看"**
            // 的地方（版块帖子永远公开），少了这个入口等于少了那个能力。
            // 收起/放出跟悬浮底栏同一个信号源（和招募页的发布按钮一致）。
            if (mainTab == MAIN_COMMUNITY) {
                val motion = szjMotionEnabled()
                val hide = bar.hidden
                val p by animateFloatAsState(
                    if (hide) 1f else 0f,
                    if (motion) tween(if (hide) 260 else 190, easing = FastOutSlowInEasing) else tween(0),
                    label = "szjPostFabHide",
                )
                val strategy = subTab == SUB_GUIDE
                val isDynamic = subTab == SUB_DYNAMICS
                SzjPressable(
                    onClick = {
                        if (isDynamic) dynamicComposerOpen = true
                        else nav(SzjRoute.PublishPost(strategy))
                    },
                    modifier = Modifier.align(Alignment.BottomEnd)
                        .padding(end = 18.dp, bottom = 108.dp)
                        .graphicsLayer {
                            alpha = 1f - p
                            translationX = p * 40f
                            translationY = p * 24f
                            scaleX = 1f - p * 0.2f
                            scaleY = 1f - p * 0.2f
                        },
                    shape = CircleShape,
                ) {
                    Row(
                        Modifier.shadow(8.dp, SzjChipShape, ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
                            .clip(SzjChipShape).background(SzjAccentFill)
                            .padding(horizontal = 15.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ImageGlyph(R.drawable.ic_add, SzjOnAccent, Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            if (isDynamic) "发动态" else if (strategy) "发攻略" else "发帖",
                            color = SzjOnAccent, style = SzjLabelStyle,
                        )
                    }
                }
            }
            SzjBottomBar(
                mainTab,
                onSelect = { SzjNav.selectTab(it) },
                barHeightDp = barHeightDp,
                barBottomDp = barBottomDp,
                hidden = bar.hidden,
                modifier = Modifier.align(Alignment.BottomCenter),
                onSearch = { nav(SzjRoute.Search) },
            )
            // **必须排在底栏之后。** 它以前是 `Dialog`（独立窗口），天然盖住一切；
            // 改成窗口内浮层之后层级只由**组合顺序**决定 —— 排在底栏前面时，
            // 底栏会画在遮罩之上而且还能点，打字打一半能切到「招募」tab
            // （真机 dump 里 y=1510 那一行就压在遮罩上面）。
            if (dynamicComposerOpen) {
                SzjDynamicComposer(
                    onDismiss = { dynamicComposerOpen = false },
                    onPublished = {
                        dynamicComposerOpen = false
                        // 发完让动态流重拉一次，自己那条才会出现。
                        SzjNav.dynamicsReloadKey.value++
                    },
                    nav = nav,
                )
            }
        }
    }
}

/** Top bar with the account entry (avatar + login label) and a check-in button. */
@Composable
private fun ShizhijiaTopBar(state: PhoneState, nav: (SzjRoute) -> Unit, loggedIn: Boolean, loginUser: ShizhijiaLoginUser?, onSignIn: () -> Unit, signedToday: Boolean) {
    // 头像和昵称点进自己的主页；没登录就去登录页。
    // uuid 来自 GHome/isLogin，没有它主页接口取不到人。
    val myUuid = loginUser?.uuid.orEmpty()
    val openMe: () -> Unit = {
        if (!loggedIn) nav(SzjRoute.Login)
        else if (myUuid.isNotBlank()) nav(SzjRoute.UserProfile(myUuid))
    }
    // 账号行本身就是一张石板卡片，取代原来"一行内容 + 一条分割线"。
    SzjCardSurface(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp)
                .clip(CircleShape)
                .clickable(enabled = !loggedIn || myUuid.isNotBlank(), onClick = openMe)
                .shadow(2.dp, CircleShape, ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
                .clip(CircleShape).background(SzjCardRaised), contentAlignment = Alignment.Center) {
            val ava = loginUser?.avatar
            // Default portraits arrive as inline data:image URIs; decode them
            // here so we can fall back to the first character on any failure.
            val bmp = if (!ava.isNullOrBlank() && ava.startsWith("data:image")) remember(ava) { decodeDataUri(ava) } else null
            if (bmp != null) {
                Image(bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else if (!ava.isNullOrBlank() && !ava.startsWith("data:image")) {
                ShizhijiaRemoteImage(url = ava, modifier = Modifier.fillMaxSize().clip(CircleShape), showPlaceholder = false)
            } else {
                Text(loginUser?.name?.take(1) ?: if (loggedIn) "我" else "?", color = SzjMuted, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            }
            }
            Spacer(Modifier.width(10.dp))
            Column(
                Modifier.weight(1f)
                    .clip(SzjChipShape)
                    .clickable(enabled = !loggedIn || myUuid.isNotBlank(), onClick = openMe),
            ) {
                Text(loginUser?.name ?: if (loggedIn) "已登录" else "未登录", color = SzjText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                val server = listOfNotNull(loginUser?.area, loginUser?.group)
                if (server.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(server.joinToString(" "), color = SzjMuted, style = SzjMetaStyle) }
                } else {
                    Text("登录后同步收藏与关注", color = SzjMuted, style = SzjMetaStyle)
                }
            }
            // Check-in button flips to a greyed "已签到" once done today; clicking it
            // then opens the sign-in calendar (rewards + signed days) instead.
            SzjPressable(
                onClick = { if (signedToday) nav(SzjRoute.SignCalendar) else onSignIn() },
                shape = SzjChipShape,
            ) {
                Text(
                    if (signedToday) "已签到" else "签到",
                    color = if (signedToday) SzjMuted else SzjOnAccentSoft,
                    style = SzjLabelStyle,
                    modifier = Modifier.clip(SzjChipShape)
                        .background(if (signedToday) SzjCardRaised else SzjAccentSoft)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

/**
 * 按压反馈包装：给不值得做成整张卡片的小控件（按钮、图标、chip）
 * 套上同一条弹簧，保证全模块手感一致。缩放比卡片深一点，
 * 因为小控件位移小，不压狠一些看不出来。
 */
@Composable
internal fun SzjPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = SzjChipShape,
    // PhonePressable 本来就支持 enabled，这个包装以前没往外露，
    // 于是想做"禁用态"的地方只能自己在 onClick 里判断（点了没反应，也没有
    // 按压反馈上的区别）。补上，默认值不变所以现有调用点不受影响。
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    // 实现已经提升成全局的 PhonePressable（这套手感是全模块标杆，
    // 其他模块现在向它看齐）。这里保留名字，省得改几十个调用点。
    PhonePressable(onClick = onClick, modifier = modifier, shape = shape, enabled = enabled, content = content)
}

/** Second-level tab row inside the Community section: 帖子 / 动态 / 攻略. */
@Composable
private fun SzjSubTabRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SzjSubTab("帖子", selected == SUB_POSTS) { onSelect(SUB_POSTS) }
        SzjSubTab("动态", selected == SUB_DYNAMICS) { onSelect(SUB_DYNAMICS) }
        SzjSubTab("攻略", selected == SUB_GUIDE) { onSelect(SUB_GUIDE) }
        SzjSubTab("招募", selected == SUB_RECRUIT) { onSelect(SUB_RECRUIT) }
    }
}

/**
 * 二级 Tab：选中态是横躺的水晶棱条，宽度从 0 长出来。
 * 比整条下划线更轻，也把签名形状带到了这一层。
 */
@Composable
private fun SzjSubTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val motion = szjMotionEnabled()
    val grow by animateFloatAsState(
        if (selected) 1f else 0f,
        if (motion) SzjMorphSpring else spring(stiffness = 100000f),
        label = "szjSubTabGrow",
    )
    val color by animateColorAsState(if (selected) SzjAccent else SzjMuted, tween(200), label = "szjSubTabColor")
    SzjPressable(onClick = onClick, shape = SzjChipShape) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                label,
                color = color,
                fontSize = 15.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
            // 横躺的棱条：两头收尖，宽度随选中态生长。
            androidx.compose.foundation.Canvas(Modifier.width(26.dp).height(3.dp)) {
                if (grow <= 0.01f) return@Canvas
                val w = size.width * grow
                val h = size.height
                val left = (size.width - w) / 2f
                val taper = w * 0.22f
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(left, h / 2f)
                    lineTo(left + taper, 0f)
                    lineTo(left + w - taper, 0f)
                    lineTo(left + w, h / 2f)
                    lineTo(left + w - taper, h)
                    lineTo(left + taper, h)
                    close()
                }
                drawPath(path, color)
            }
        }
    }
}


/**
 * 「我」页那三个计数（关注/粉丝/获赞）的进程内缓存。
 *
 * 从玩家主页返回时「我」这个 composable 会被重建。结果挂在 remember 上的话
 * 每次回来都要重新请求一次，三个数字重新转一圈——而这三个数变化很慢。
 * 退出登录时由 ShizhijiaSession.clear 一起清掉，否则换账号会看到上一个人的数。
 */
internal object SzjMyCountsCache {
    private var uuid: String = ""
    private var value: ShizhijiaUserProfile? = null

    fun valueFor(forUuid: String): ShizhijiaUserProfile? =
        if (forUuid.isNotBlank() && forUuid == uuid) value else null

    fun put(forUuid: String, profile: ShizhijiaUserProfile) {
        uuid = forUuid
        value = profile
    }

    fun clear() {
        uuid = ""
        value = null
    }
}

@Composable
private fun ShizhijiaMeTab(
    state: PhoneState,
    nav: (SzjRoute) -> Unit,
    loggedIn: Boolean,
    loginUser: ShizhijiaLoginUser?,
    bottomBarHeightDp: Float,
    barBottomDp: Float,
    onBarHeightChange: (Float) -> Unit,
    onBarBottomChange: (Float) -> Unit,
    header: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var bottomBarHeightDp by remember { mutableStateOf(bottomBarHeightDp) }
    var barBottomDp by remember { mutableStateOf(barBottomDp) }
    LaunchedEffect(barBottomDp) { onBarBottomChange(barBottomDp) }
    LaunchedEffect(bottomBarHeightDp) { onBarHeightChange(bottomBarHeightDp) }
    var showSettings by remember { mutableStateOf(false) }
    // 设置页盖在「我」上面，返回键先退出设置。
    BackHandler(enabled = showSettings) { showSettings = false }
    val p = loginUser
    // 关注/粉丝/获赞原来是硬编码的三个 0——自己主页明明有真数，摆一排假 0
    // 是最显眼的半成品。这里按 uuid 拉一次真资料；没拉到就显示占位条。
    //
    // 结果进 SzjMyCountsCache：从玩家主页返回时这个 composable 会被重建，
    // 挂在 remember 上的话每次回来都重新打一次接口、三个数字重新转一圈。
    // 这三个数变化很慢，进程内缓存一次就够。
    var myCounts by remember {
        mutableStateOf(SzjMyCountsCache.valueFor(p?.uuid.orEmpty()))
    }
    LaunchedEffect(p?.uuid) {
        val uuid = p?.uuid.orEmpty()
        if (uuid.isBlank()) {
            myCounts = null
            return@LaunchedEffect
        }
        val cached = SzjMyCountsCache.valueFor(uuid)
        if (cached != null) {
            myCounts = cached
            return@LaunchedEffect
        }
        val fresh = ShizhijiaApi.getUserProfile(context, uuid)
        if (fresh != null) SzjMyCountsCache.put(uuid, fresh)
        myCounts = fresh
    }
    Column(Modifier.fillMaxSize().padding(bottom = 90.dp).verticalScroll(rememberScrollState())) {
      // 品牌行随内容滑走，和其他分区一致。它自带左右 16dp，所以放在内边距外面。
      header()
      Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        if (showSettings) {
            // ---- 设置页 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("设置", color = SzjText, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
            Spacer(Modifier.height(14.dp))
            SzjCardSurface(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(15.dp)) {
                    Text("悬浮底栏", color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("调整底栏的大小和离屏幕底边的距离", color = SzjMuted, style = SzjMetaStyle)
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("高度", color = SzjMuted, style = SzjMetaStyle, modifier = Modifier.width(38.dp))
                        Text("${bottomBarHeightDp.toInt()} dp", color = SzjAccent, style = SzjLabelStyle)
                    }
                    Slider(
                        value = bottomBarHeightDp,
                        onValueChange = {
                            bottomBarHeightDp = it
                            ShizhijiaSession.setBottomBarHeight(context, it)
                        },
                        valueRange = 48f..96f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = SzjAccent,
                            activeTrackColor = SzjAccent,
                            inactiveTrackColor = SzjCardRaised,
                        ),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("离底", color = SzjMuted, style = SzjMetaStyle, modifier = Modifier.width(38.dp))
                        Text("${barBottomDp.toInt()} dp", color = SzjAccent, style = SzjLabelStyle)
                    }
                    Slider(
                        value = barBottomDp,
                        onValueChange = {
                            barBottomDp = it
                            ShizhijiaSession.setBottomBarBottom(context, it)
                        },
                        valueRange = 0f..40f,
                        colors = androidx.compose.material3.SliderDefaults.colors(
                            thumbColor = SzjAccent,
                            activeTrackColor = SzjAccent,
                            inactiveTrackColor = SzjCardRaised,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            if (loggedIn) {
                // 退出登录是破坏性动作，用描边而不是实心，别和主操作抢。
                SzjPressable(onClick = {
                    ShizhijiaSession.clear(context)
                    showSettings = false
                    android.widget.Toast.makeText(context, "已退出登录", android.widget.Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.fillMaxWidth(), shape = SzjInnerShape) {
                    Text("退出登录", color = SzjMuted, style = SzjLabelStyle,
                        modifier = Modifier.fillMaxWidth().clip(SzjInnerShape)
                            .border(1.dp, SzjHairline, SzjInnerShape)
                            .padding(vertical = 12.dp),
                        textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(12.dp))
            // ---- 推荐过滤：勾掉的版块不进"推荐"流 ----
            SzjPostFilterCard()
            if (loggedIn) {
                Spacer(Modifier.height(12.dp))
                SzjDataCenterProbeCard()
            }
            Spacer(Modifier.height(12.dp))
            SzjPressable(onClick = { showSettings = false }, shape = SzjChipShape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                ) {
                    ImageGlyph(R.drawable.ic_back, SzjAccent, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("返回", color = SzjAccent, fontSize = 14.sp)
                }
            }
        } else if (loggedIn) {
            // ---- 资料头卡：头像 + 名字 + 服务器 + 三个计数 ----
            // 整张资料头卡可以点进自己的主页（uuid 来自 GHome/isLogin）。
            val myUuid = p?.uuid.orEmpty()
            SzjCardSurface(
                Modifier.fillMaxWidth(),
                onClick = if (myUuid.isNotBlank()) ({ nav(SzjRoute.UserProfile(myUuid)) }) else null,
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SzjAvatar(p?.name ?: "", p?.avatar ?: "", myUuid, 60)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p?.name ?: "已登录", color = SzjText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            val srv = listOfNotNull(p?.area, p?.group).joinToString(" ")
                            if (srv.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(14); Text(srv, color = SzjMuted, style = SzjMetaStyle) }
                            }
                        }
                        if (myUuid.isNotBlank()) ImageGlyph(R.drawable.ic_chevron_right, SzjMuted, Modifier.size(18.dp))
                    }
                    // 计数条：数字大、标签小，中间用竖棱条分隔。
                    //
                    // 这一行从第一帧就占满最终高度，数字没到之前用占位条。
                    // 原来是 myCounts?.let —— 真数没回来时整行高度为 0，
                    // 一两秒后接口返回，行突然长出来把下面的宫格整体顶下去，
                    // 每次进"我"都要看一次这个跳动。宁可先占位，也不要抽搐。
                    Spacer(Modifier.height(16.dp))
                    // 关注/粉丝可以点进名单，和玩家主页那张卡一致。自己的这两份
                    // 名单是登录后能读的，所以这里比看别人的更靠得住。
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val c = myCounts
                        val cells: List<Triple<String, Int?, (() -> Unit)?>> = listOf(
                            Triple("关注", c?.followNum) { nav(SzjRoute.RelationList(myUuid, fans = false, who = p?.name.orEmpty())) },
                            Triple("粉丝", c?.fansNum) { nav(SzjRoute.RelationList(myUuid, fans = true, who = p?.name.orEmpty())) },
                            Triple("获赞", c?.likedNum, null),
                        )
                        cells.forEachIndexed { i, (label, num, onClick) ->
                            if (i > 0) Box(Modifier.width(1.dp).height(22.dp).background(SzjLine))
                            Column(
                                Modifier.weight(1f)
                                    .let { if (onClick != null && c != null) it.clip(SzjChipShape).clickable(onClick = onClick) else it }
                                    .padding(vertical = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (num == null) {
                                    // 占位条的高度对齐 18sp 数字的行高，换成真数时行不动。
                                    SzjShimmerBox(Modifier.width(30.dp).height(21.dp), RoundedCornerShape(4.dp))
                                } else {
                                    Text("$num", color = SzjText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(label, color = SzjMuted, style = SzjMetaStyle)
                                    if (onClick != null) {
                                        ImageGlyph(R.drawable.ic_chevron_right, SzjMuted, Modifier.padding(start = 1.dp).size(11.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            // ---- 入口宫格：三列，每格一张小石板 ----
            // 六个入口都接通了，直接跳页。
            val entries = listOf("收藏", "招募管理", "我的角色", "我的部队", "专项数据", "设置")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                entries.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { label ->
                            SzjCardSurface(
                                Modifier.weight(1f),
                                shape = SzjInnerShape,
                                onClick = {
                                    when (label) {
                                        "设置" -> showSettings = true
                                        "收藏" -> nav(SzjRoute.Favorites)
                                        "招募管理" -> nav(SzjRoute.MyRecruits)
                                        "我的角色" -> nav(SzjRoute.Characters)
                                        "我的部队" -> nav(SzjRoute.MyGuild)
                                        "专项数据" -> nav(SzjRoute.Statistics)
                                        else -> android.widget.Toast.makeText(context, "$label 还没接", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                            ) {
                                // 只放文字。棱条是分区标记，不是"收藏""设置"的图标，
                                // 拿它当六个格子的通用图标会把签名元素用废。
                                Box(Modifier.fillMaxWidth().padding(vertical = 18.dp), contentAlignment = Alignment.Center) {
                                    Text(label, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                }
                            }
                        }
                        // 最后一行不足三格时补空位，别让卡片被拉宽。
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        } else {
            SzjEmptyInline(
                "还没有登录",
                "登录后可以看关注动态、收藏帖子，并自动签到",
            ) { SzjPrimaryButton("登录石之家", onClick = { nav(SzjRoute.Login) }) }
        }
      }
    }
}

/**
 * 临时开发卡：抓一次专项数据的接口形状。
 *
 * 专项数据那 43 个接口要登录态，字段名又不在官网前端代码里，所以只能用真实
 * 响应来定结构。跑在设备上是因为会话 cookie 按域存，从浏览器复制出来的那份
 * 属于页面域，对接口域一律 10105。接完 7 个分类之后这张卡就删掉。
 */
@Composable
private fun SzjDataCenterProbeCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    SzjCardSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text("抓取专项数据结构", color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "开发用：请求专项数据的全部接口，把字段名和类型写到文件。" +
                    "绝境战和朝圣交错路的字段是从官网代码里读的、还没拿真实数据验过，" +
                    "所以这张卡先留着。不记录任何登录凭证。",
                color = SzjMuted, style = SzjMetaStyle, lineHeight = 16.sp,
            )
            Spacer(Modifier.height(12.dp))
            SzjPressable(
                onClick = {
                    if (running) return@SzjPressable
                    running = true
                    result = ""
                    scope.launch {
                        val summary = ShizhijiaProbe.runDataCenterProbe(context) { done, total, path ->
                            progress = "$done/$total  $path"
                        }
                        result = summary
                        running = false
                        progress = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(), shape = SzjInnerShape,
            ) {
                Text(
                    if (running) "抓取中…" else "开始抓取",
                    color = if (running) SzjMuted else SzjAccent, style = SzjLabelStyle,
                    modifier = Modifier.fillMaxWidth().clip(SzjInnerShape)
                        .border(1.dp, SzjHairline, SzjInnerShape)
                        .padding(vertical = 12.dp),
                    textAlign = TextAlign.Center,
                )
            }
            if (progress.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(progress, color = SzjMuted, style = SzjMetaStyle)
            }
            if (result.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(result, color = SzjAccent, style = SzjMetaStyle)
            }
        }
    }
}

/**
 * 推荐过滤：勾掉的版块不出现在"推荐"流里。
 *
 * 帖子接口没有排除参数，所以这是本地过滤（拉回来之后筛掉）。
 * 只作用于"推荐"——主动点开某个版块时不筛，那是明确想看。
 */
@Composable
private fun SzjPostFilterCard() {
    val context = LocalContext.current
    var parts by remember { mutableStateOf(listOf<ShizhijiaPostPart>()) }
    var muted by remember { mutableStateOf(ShizhijiaSession.mutedParts(context)) }
    // null = 还在读；空表 = 读完了但没有内容（要能区分，否则失败时永远显示"正在读"）
    var loaded by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(reload) {
        loaded = false
        // 帖子和攻略两套版块都列上，两个流用的是同一份屏蔽名单。
        parts = ShizhijiaApi.getPostParts(context) + ShizhijiaApi.getStrategyParts(context)
        loaded = true
    }
    // 默认收起，只显示这一项本身；点开才铺版块。
    // 十几个版块常态铺开会把设置页顶得很长。
    var expanded by remember { mutableStateOf(false) }
    SzjCardSurface(Modifier.fillMaxWidth(), onClick = { expanded = !expanded }) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("推荐过滤", color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (muted.isEmpty()) "选中的版块不会出现在推荐里"
                        else "已屏蔽 ${muted.size} 个版块",
                        color = SzjMuted, style = SzjMetaStyle,
                    )
                }
                // 展开时箭头转成向上，收起时向下。
                val rot by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "szjFilterArrow")
                ImageGlyph(
                    R.drawable.ic_chevron_down, SzjMuted,
                    Modifier.graphicsLayer { rotationZ = rot }.padding(start = 8.dp).size(16.dp),
                )
            }
            if (expanded) {
            Spacer(Modifier.height(12.dp))
            if (parts.isEmpty()) {
                if (!loaded) {
                    Text("正在读版块列表", color = SzjMuted, style = SzjMetaStyle)
                } else {
                    // 读完了还是空：给个重试，别让人干等。
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("没读到版块列表", color = SzjMuted, style = SzjMetaStyle)
                        Spacer(Modifier.width(10.dp))
                        SzjPressable(onClick = { reload++ }, shape = SzjChipShape) {
                            Text("重试", color = SzjAccent, style = SzjLabelStyle,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }
            } else {
                // 选中 = 已屏蔽。用实心态表示"拦住了"，比灰掉更直观。
                val distinct = parts.distinctBy { it.id }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    distinct.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { p ->
                                val on = p.id in muted
                                SzjPressable(
                                    onClick = {
                                        muted = if (on) muted - p.id else muted + p.id
                                        ShizhijiaSession.setMutedParts(context, muted)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = SzjChipShape,
                                ) {
                                    Text(
                                        p.name,
                                        color = if (on) SzjOnAccent else SzjMuted,
                                        style = SzjLabelStyle,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().clip(SzjChipShape)
                                            .background(if (on) SzjAccentFill else SzjCardRaised)
                                            .padding(vertical = 9.dp, horizontal = 4.dp),
                                    )
                                }
                            }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                if (muted.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    SzjPressable(
                        onClick = { muted = emptySet(); ShizhijiaSession.setMutedParts(context, emptySet()) },
                        shape = SzjChipShape,
                    ) {
                        Text("全部恢复", color = SzjAccent, style = SzjLabelStyle,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            }
        }
    }
}

/**
 * 悬浮底栏：一块窄石板浮在内容上。选中指示块在四格之间滑动，
 * 而不是各格自己亮灭——滑动能带出"从社区走到幻化"的方向感。
 */
@Composable
private fun SzjBottomBar(
    selected: Int,
    onSelect: (Int) -> Unit,
    barHeightDp: Float,
    barBottomDp: Float,
    hidden: Boolean,
    modifier: Modifier = Modifier,
    onSearch: () -> Unit = {},
) {
    val motion = szjMotionEnabled()
    val pos by animateFloatAsState(
        selected.coerceIn(0, 2).toFloat(),
        if (motion) spring(dampingRatio = 0.7f, stiffness = 300f) else spring(stiffness = 100000f),
        label = "szjBarPos",
    )
    val collapse by animateFloatAsState(
        if (hidden && motion) 1f else 0f,
        tween(if (hidden) 280 else 200, easing = FastOutSlowInEasing),
        label = "szjBarCollapse",
    )
    val density = androidx.compose.ui.platform.LocalDensity.current
    val slidePx = with(density) { (barHeightDp + barBottomDp + 24f).dp.toPx() }
    Row(
        modifier
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp + barBottomDp.dp)
            .fillMaxWidth().height(barHeightDp.dp)
            .graphicsLayer {
                translationY = collapse * slidePx
                val sc = 1f - collapse * 0.12f
                scaleX = sc
                scaleY = sc
                alpha = 1f - collapse * 0.55f
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 胶囊组：社区 / 幻化 / 设置。
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
                .clip(RoundedCornerShape(20.dp))
                .background(if (szjLight) Color(0xFFEDF1F6) else Color(0xFF1A222B))
                .then(if (szjLight) Modifier.border(1.dp, SzjLine, RoundedCornerShape(20.dp)) else Modifier),
        ) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(
                Brush.horizontalGradient(listOf(Color.Transparent, SzjEdge, SzjEdge, Color.Transparent))
            ))
            Row(Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                SzjBottomTab("社区", selected == MAIN_COMMUNITY, Modifier.weight(1f)) { onSelect(MAIN_COMMUNITY) }
                SzjBottomTab("幻化", selected == MAIN_GLAMOUR, Modifier.weight(1f)) { onSelect(MAIN_GLAMOUR) }
                SzjBottomTab("设置", selected == MAIN_ME, Modifier.weight(1f)) { onSelect(MAIN_ME) }
            }
        }
        // 独立圆形搜索，iPadOS 式。
        SzjPressable(onClick = onSearch, shape = CircleShape) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .shadow(10.dp, CircleShape, ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
                    .clip(CircleShape)
                    .background(if (szjLight) Color.White else Color(0xFF1A222B))
                    .then(if (szjLight) Modifier.border(1.dp, SzjLine, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                ImageGlyph(R.drawable.ic2_search, SzjAccent, Modifier.size(21.dp))
            }
        }
    }
}

@Composable
private fun SzjBottomTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) SzjAccent else SzjMuted, tween(200), label = "szjBottomTabColor")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = szjMotionEnabled()
    val scale by animateFloatAsState(if (pressed && motion) 0.9f else 1f, SzjPressSpring, label = "szjBottomTabPress")
    Box(
        modifier.fillMaxHeight()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(SzjInnerShape)
            .background(if (selected) (if (szjLight) Color.White else Color(0xFF2A3542)) else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = color,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ---- Post feed -------------------------------------------------------------

@Composable
private fun ShizhijiaPostsTab(
    state: PhoneState,
    nav: (SzjRoute) -> Unit,
    ps: SzjPostsState,
    header: @Composable () -> Unit,
    subTabs: @Composable () -> Unit,
) {
    val context = LocalContext.current
    // All feed state lives on `ps`, which is held above the internal back stack,
    // so returning from a post detail keeps the scroll position and pages.
    val listState = ps.listState

    LaunchedEffect(Unit) { ps.parts.value = ShizhijiaApi.getPostParts(context) }
    // Reload the feed ONLY when the partition actually changes (first visit or a
    // real switch). Re-entering after a post detail keeps loaded pages + scroll.
    LaunchedEffect(ps.partId.value) {
        if (ps.seenPartId.value == ps.partId.value) return@LaunchedEffect
        ps.seenPartId.value = ps.partId.value
        ps.loading.value = true
        ps.posts.value = emptyList(); ps.page.value = 1; ps.pageTime.value = ""
        val result = ShizhijiaApi.getPostsList(context, partId = ps.partId.value)
        ps.posts.value = result.rows; ps.pageTime.value = result.pageTime
        ps.loading.value = false
    }

    // Infinite scroll: fetch the next page shortly before reaching the end.
    val nearEnd by remember { derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        last >= ps.posts.value.size - 3
    } }
    LaunchedEffect(nearEnd, ps.partId.value) {
        if (nearEnd && !ps.loading.value && ps.posts.value.isNotEmpty() && ps.pageTime.value.isNotBlank()) {
            ps.loading.value = true
            val next = ShizhijiaApi.getPostsList(context, partId = ps.partId.value, page = ps.page.value + 1, pageTime = ps.pageTime.value)
            // Stop paging when the server returns no more rows.
            if (next.rows.isEmpty()) ps.pageTime.value = "" else {
                ps.posts.value = ps.posts.value + next.rows
                ps.pageTime.value = next.pageTime
                ps.page.value += 1
            }
            ps.loading.value = false
        }
    }

    // 推荐过滤：接口没有"排除版块"的参数，所以在本地筛。
    // 只在"推荐"流里生效——你主动点开某个版块，说明就是想看它。
    val muted = ShizhijiaSession.mutedParts(context)
    val shown = remember(ps.posts.value, muted, ps.partId.value) {
        if (ps.partId.value.isNotBlank() || muted.isEmpty()) ps.posts.value
        else ps.posts.value.filter { it.partId !in muted }
    }
    // 屏蔽得多的时候整页可能被筛空，这时候要主动再拉一页，
    // 否则列表空着、滚不动，分页也就永远不会被触发。
    LaunchedEffect(shown.size, ps.posts.value.size, ps.loading.value) {
        if (shown.isEmpty() && ps.posts.value.isNotEmpty() &&
            !ps.loading.value && ps.pageTime.value.isNotBlank()
        ) {
            ps.loading.value = true
            val next = ShizhijiaApi.getPostsList(
                context, partId = ps.partId.value,
                page = ps.page.value + 1, pageTime = ps.pageTime.value,
            )
            if (next.rows.isEmpty()) ps.pageTime.value = "" else {
                ps.posts.value = ps.posts.value + next.rows
                ps.pageTime.value = next.pageTime
                ps.page.value += 1
            }
            ps.loading.value = false
        }
    }

    // 整个分区（头部 + Tab 行 + 版块 chips + 帖子流）都在同一个 LazyColumn 里，
    // 所以头部随内容滑走；Tab 行和 chips 用 stickyHeader 钉住，
    // 滑到深处也还能切换分区。
    SzjFeedScaffold(
        listState = listState,
        header = header,
        sticky = {
            Column(Modifier.fillMaxWidth().background(SzjBg)) {
                subTabs()
                LazyRow(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "all") { SzjPartChip("推荐", ps.partId.value == "") { ps.partId.value = "" } }
                    items(ps.parts.value, key = { it.id }) { p -> SzjPartChip(p.name, ps.partId.value == p.id) { ps.partId.value = p.id } }
                }
            }
        },
    ) {
        when {
            // 首屏用骨架屏：先把三张卡片的轮廓占住，比转圈更能说明马上出什么。
            ps.loading.value && ps.posts.value.isEmpty() -> item(key = "skeleton") { SzjFeedSkeleton() }
            shown.isEmpty() && ps.posts.value.isNotEmpty() -> item(key = "allmuted") {
                SzjEmptyInline("这一页都被你屏蔽了", "在 我 → 设置 → 推荐过滤 里改")
            }
            ps.posts.value.isEmpty() -> item(key = "empty") { SzjEmptyInline("这个分区还没有帖子", "换个分区，或下拉看看推荐") }
            else -> {
                itemsIndexed(shown, key = { _, it -> it.postsId }) { index, post ->
                    SzjRise(index) {
                        SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) })
                    }
                }
                item(key = "loading-footer") {
                    if (ps.loading.value) Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/**
 * 分区列表的骨架：头部当普通项（跟着滑走），Tab/筛选行当 sticky（钉在顶部）。
 * 五个 feed（帖子/动态/攻略/招募/收藏）共用，保证滚动行为一致。
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SzjFeedScaffold(
    listState: androidx.compose.foundation.lazy.LazyListState,
    header: @Composable () -> Unit,
    sticky: (@Composable () -> Unit)? = null,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        // 底部留出悬浮底栏的高度：底栏会收起，但内容不该被它压住。
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
    ) {
        item(key = "szj-header") { header() }
        if (sticky != null) stickyHeader(key = "szj-sticky") { sticky() }
        content()
    }
}

/**
 * 空态的列表内版本。SzjEmpty 用 fillMaxSize 居中，放进 LazyColumn 的 item
 * 里会塌成零高，所以这里给一个固定高度的版本。
 */
@Composable
internal fun SzjEmptyInline(
    title: String,
    hint: String? = null,
    iconRes: Int = R.drawable.ic_empty_box,
    action: (@Composable () -> Unit)? = null,
) {
    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        SzjEmptyBody(title, hint, iconRes, action)
    }
}

/** 分区 chip：选中时底色和文字色一起过渡，不做位移，避免和棱条抢戏。 */
@Composable
private fun SzjPartChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // 未选中态原来填 SzjCard 落在 SzjBg 上，深色下两个色差不到一档，chip 几乎隐形。
    // 改成透明底 + 描边：轮廓比色块更能在近黑底上立住，选中态才是唯一的色块。
    val bg by animateColorAsState(if (selected) SzjAccentSoft else Color.Transparent, tween(220), label = "szjChipBg")
    val fg by animateColorAsState(if (selected) SzjOnAccentSoft else SzjMuted, tween(220), label = "szjChipFg")
    val stroke by animateColorAsState(if (selected) Color.Transparent else SzjHairline, tween(220), label = "szjChipStroke")
    SzjPressable(onClick = onClick, shape = SzjChipShape) {
        Text(
            label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.clip(SzjChipShape).background(bg)
                .border(1.dp, stroke, SzjChipShape)
                .padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

/**
 * 图标 + 数字的元信息（评论/阅读/点赞/收藏数）。
 * 图标默认 12dp，跟 11sp 元信息字号配平；[fontSize] 大一档时图标跟着放大。
 * 石之家所有"图标 + 计数"都走这个，别再写 "♥ $n" 这种字符拼接。
 */
@Composable
private fun SzjCountMeta(iconRes: Int, count: Long, fontSize: androidx.compose.ui.unit.TextUnit = 11.sp) {
    val iconDp = (fontSize.value + 1f).dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material3.Icon(
            painterResource(iconRes), contentDescription = null,
            tint = SzjMuted, modifier = Modifier.size(iconDp),
        )
        Spacer(Modifier.width(3.dp))
        Text("$count", color = SzjMuted, fontSize = fontSize, letterSpacing = 0.4.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SzjPostRow(post: ShizhijiaPostCard, onClick: () -> Unit) {
    SzjCardSurface(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        onClick = onClick,
    ) {
        Column(Modifier.padding(14.dp)) {
        // Line 1: 分区标签独占一行做眉标，标题不再被标签挤成两段。
        // 眉标左侧带一根小棱条，和页头/品牌行的标记同源。
        if (post.partName.isNotBlank()) {
            // 眉标不带棱条：分区名本来就是 accent 色，已经够醒目；
            // 棱条只留给一级分区标题和选中态（否则满屏碎片）。
            Text(
                post.partName, color = SzjAccent, style = SzjMetaStyle,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Text(
            post.title,
            color = SzjText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = 23.sp, letterSpacing = 0.1.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        // Line 2: 配图。**不管几张，每格都是同一个固定 1/3 宽**。
        //
        // 0.7.225 改成过"1 张就铺满全宽 16:9、2 张各半宽"，理由是单张时那个
        // 小方块看着寒酸。但代价是列表里每张卡的高度取决于配了几张图：
        // 一张图的帖子比三张图的帖子高一倍，滑起来忽高忽低。
        // 列表要的是稳定的行高，不是每张卡都争最大版面。改回来了。
        // 失败的图直接塌掉，不留空框。
        if (post.coverPics.isNotEmpty()) {
            Spacer(Modifier.height(11.dp))
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cell = (maxWidth - 12.dp) / 3
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val covers = post.coverPics.distinct().take(3)
                    covers.forEachIndexed { ci, url ->
                        // 点开带上这一组，查看器里能左右滑。
                        ShizhijiaRemoteImage(url = url, modifier = Modifier.width(cell).height(cell).clip(SzjInnerShape), contentScale = ContentScale.Crop, showPlaceholder = false, collapseOnFail = true, onClick = { SzjViewer.open(covers, ci) })
                    }
                }
            }
        }
        // Line 3: author on the left; comment / read counts on the right.
        Spacer(Modifier.height(11.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(post.characterName.ifBlank { "匿名玩家" }, color = SzjMuted, style = SzjMetaStyle,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (post.groupName.isNotBlank()) {
                Spacer(Modifier.width(3.dp))
                SzjLocPin(13)
                Spacer(Modifier.width(2.dp))
                Text(post.groupName, color = SzjMuted, style = SzjMetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.weight(1f))
            // 计数带 12dp 小图标，比"N 评论 · N 阅读"这一串裸文字好扫。
            if (post.commentCount > 0) SzjCountMeta(R.drawable.ic_comment, post.commentCount)
            if (post.commentCount > 0 && post.readCount > 0) Spacer(Modifier.width(10.dp))
            if (post.readCount > 0) SzjCountMeta(R.drawable.ic_eye, post.readCount)
        }
        }
    }
}

// ---- Dynamics feed ----------------------------------------------------------

@Composable
private fun ShizhijiaDynamicsTab(
    nav: (SzjRoute) -> Unit,
    loggedIn: Boolean,
    header: @Composable () -> Unit,
    subTabs: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var dynamics by remember { mutableStateOf(listOf<ShizhijiaDynamic>()) }
    var loading by remember { mutableStateOf(loggedIn) }
    val listState = rememberLazyListState()
    // 加上 dynamicsReloadKey：发完动态要重拉，否则自己刚发的那条不出现。
    LaunchedEffect(loggedIn, SzjNav.dynamicsReloadKey.value) {
        if (loggedIn) { loading = true; dynamics = ShizhijiaApi.getFollowDynamicList(context).rows; loading = false }
    }
    SzjFeedScaffold(
        listState = listState,
        header = header,
        sticky = { Column(Modifier.fillMaxWidth().background(SzjBg).padding(bottom = 6.dp)) { subTabs() } },
    ) {
        when {
            !loggedIn -> item(key = "login") {
                SzjEmptyInline(
                    "登录后这里是你关注的人",
                    "用石之家账号登录，动态、收藏和签到会一起同步",
                ) { SzjPrimaryButton("登录", onClick = { nav(SzjRoute.Login) }) }
            }
            loading && dynamics.isEmpty() -> item(key = "skeleton") { SzjFeedSkeleton() }
            dynamics.isEmpty() -> item(key = "empty") { SzjEmptyInline("关注的人还没有发动态", "去社区找几个想追的光之战士") }
            else -> itemsIndexed(dynamics, key = { _, it -> it.id }) { index, d ->
                SzjRise(index) { SzjDynamicRow(d, onClick = { nav(SzjRoute.DynamicDetail(d.id)) }) }
            }
        }
    }
}

// ---- 攻略 -------------------------------------------------------------------

/**
 * 攻略分区。接口和帖子同构（posts 那套，type=2），所以直接复用 SzjPostRow，
 * 版块来自 partList?type=2（新手指引/副本攻略/战斗职业/生产采集…）。
 */
@Composable
private fun ShizhijiaStrategyTab(
    state: PhoneState,
    nav: (SzjRoute) -> Unit,
    ss: SzjStrategyState,
    header: @Composable () -> Unit,
    subTabs: @Composable () -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { if (ss.parts.value.isEmpty()) ss.parts.value = ShizhijiaApi.getStrategyParts(context) }
    LaunchedEffect(ss.partId.value) {
        if (ss.seenPartId.value == ss.partId.value) return@LaunchedEffect
        ss.seenPartId.value = ss.partId.value
        ss.loading.value = true
        ss.posts.value = emptyList(); ss.page.value = 1; ss.pageTime.value = ""
        val result = ShizhijiaApi.getStrategyList(context, partId = ss.partId.value)
        ss.posts.value = result.rows; ss.pageTime.value = result.pageTime
        ss.loading.value = false
    }
    val nearEnd by remember { derivedStateOf {
        val last = ss.listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        last >= ss.posts.value.size - 3
    } }
    LaunchedEffect(nearEnd, ss.partId.value) {
        if (nearEnd && !ss.loading.value && ss.posts.value.isNotEmpty() && ss.pageTime.value.isNotBlank()) {
            ss.loading.value = true
            val next = ShizhijiaApi.getStrategyList(context, partId = ss.partId.value, page = ss.page.value + 1, pageTime = ss.pageTime.value)
            if (next.rows.isEmpty()) ss.pageTime.value = "" else {
                ss.posts.value = ss.posts.value + next.rows
                ss.pageTime.value = next.pageTime
                ss.page.value += 1
            }
            ss.loading.value = false
        }
    }

    SzjFeedScaffold(
        listState = ss.listState,
        header = header,
        sticky = {
            Column(Modifier.fillMaxWidth().background(SzjBg)) {
                subTabs()
                LazyRow(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "all") { SzjPartChip("全部", ss.partId.value == "") { ss.partId.value = "" } }
                    items(ss.parts.value, key = { it.id }) { p -> SzjPartChip(p.name, ss.partId.value == p.id) { ss.partId.value = p.id } }
                }
            }
        },
    ) {
        when {
            ss.loading.value && ss.posts.value.isEmpty() -> item(key = "skeleton") { SzjFeedSkeleton() }
            ss.posts.value.isEmpty() -> item(key = "empty") { SzjEmptyInline("这个版块还没有攻略", "换个版块看看") }
            else -> {
                itemsIndexed(ss.posts.value, key = { _, it -> it.postsId }) { index, post ->
                    SzjRise(index) { SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) }) }
                }
                item(key = "loading-footer") {
                    if (ss.loading.value) Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SzjDynamicRow(d: ShizhijiaDynamic, onClick: () -> Unit) {
    SzjCardSurface(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        onClick = onClick,
    ) {
        Column(Modifier.padding(13.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SzjAvatar(d.characterName, d.avatar, d.uuid, 38)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(d.characterName.ifBlank { "光之战士" }, color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                val dserver = listOf(d.areaName, d.groupName).filter { it.isNotBlank() }.joinToString(" ")
                if (dserver.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(dserver, color = SzjMuted, style = SzjMetaStyle) }
                if (d.createdAt.isNotBlank()) Text(d.createdAt, color = SzjMuted, style = SzjMetaStyle)
            }
        }
        if (d.contentText.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            // **动态正文是 HTML，得走富文本渲染。**
            // 原来是纯 Text，于是 `<p>` 这些标签和 `[emoN]` 占位符都原样显示成
            // 字符——用户看到的"[emo34]"和"<p>"就是这么来的。
            // ShizhijiaRichContent 会去标签、把 [emoN] 换成真表情图。
            ShizhijiaRichContent(d.contentText)
        }
        d.images.firstOrNull()?.let { first ->
            Spacer(Modifier.height(10.dp))
            ShizhijiaRemoteImage(url = first, modifier = Modifier.fillMaxWidth().height(160.dp).clip(SzjInnerShape), contentScale = ContentScale.Crop)
        }
        if (d.likeCount > 0 || d.commentCount > 0) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (d.likeCount > 0) Text("${d.likeCount} 赞", color = SzjMuted, style = SzjMetaStyle)
                if (d.likeCount > 0 && d.commentCount > 0) Text(" · ", color = SzjMuted, style = SzjMetaStyle)
                if (d.commentCount > 0) Text("${d.commentCount} 评论", color = SzjMuted, style = SzjMetaStyle)
            }
        }
        }
    }
}

// ---- 招募 -------------------------------------------------------------------

/**
 * 招募分区。五类招募（副本组队/新人招待/部队/其他/RP）走各自的列表接口，
 * 行结构由 ShizhijiaRecruit 归一，所以这里只有一种卡片。
 *
 * 部队招募接口需登录（未登录 10403 → 空列表），此时显示登录引导而不是"暂无"。
 */
@Composable
private fun ShizhijiaRecruitTab(
    nav: (SzjRoute) -> Unit,
    loggedIn: Boolean,
    rs: SzjRecruitState,
    header: @Composable () -> Unit,
    /** 底栏的显隐状态：发布按钮跟着它一起走。 */
    bar: SzjBarVisibility,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kind = rs.kind.value
    val items = rs.listFor(kind)
    val filter = rs.filterFor(kind)

    // 筛选面板的字典一次性拉齐（都是公开接口）
    LaunchedEffect(Unit) {
        if (!rs.dictLoaded.value) {
            rs.dictLoaded.value = true
            rs.fbConfig.value = ShizhijiaApi.getFbConfig(context)
            rs.styles.value = ShizhijiaApi.getStyleConfig(context)
            rs.categories.value = ShizhijiaApi.getOtherCategories(context)
            rs.fbLabels.value = ShizhijiaApi.getFbLabels(context)
            rs.jobs.value = ShizhijiaApi.getJobConfig(context)
            rs.areas.value = ShizhijiaApi.getAreaList(context)
        }
    }

    fun load(reset: Boolean) {
        if (rs.loading.value) return
        rs.loading.value = true
        scope.launch {
            val page = if (reset) 1 else rs.pageFor(kind) + 1
            val res = ShizhijiaApi.getRecruitList(context, kind, page = page, filter = rs.filterFor(kind))
            rs.status.value = rs.status.value + (kind to res)
            val rows = (res as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
            val merged = if (reset) rows else rs.listFor(kind) + rows
            rs.items.value = rs.items.value + (kind to merged)
            rs.pages.value = rs.pages.value + (kind to page)
            if (rows.isEmpty()) rs.ended.value = rs.ended.value + kind
            rs.loading.value = false
        }
    }

    // 首次进入某个分类（或改了筛选）时拉第一页；已有数据就不动。
    LaunchedEffect(kind, filter, loggedIn) {
        if (rs.listFor(kind).isEmpty() && !rs.ended.value.contains(kind)) load(reset = true)
    }
    val nearEnd by remember { derivedStateOf {
        val last = rs.listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        items.isNotEmpty() && last >= items.size - 3
    } }
    LaunchedEffect(nearEnd, kind) {
        if (nearEnd && !rs.loading.value && !rs.ended.value.contains(kind)) load(reset = false)
    }

    Box(Modifier.fillMaxSize()) {
        SzjFeedScaffold(
            listState = rs.listState,
            header = header,
            sticky = {
                Column(Modifier.fillMaxWidth().background(SzjBg).padding(bottom = 6.dp)) {
                    // 分类 Tab 独占一行，横向滚动不被右边的按钮挤窄。
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(ShizhijiaRecruitKind.entries, key = { it.name }) { k ->
                            SzjPartChip(k.label, kind == k) { rs.kind.value = k }
                        }
                    }
                    // 筛选行：左边是"筛选"，右边跟着生效的条件。
                    // 原来挤在 Tab 行右端，既压窄了 Tab 也不知道它管的是哪一层。
                    // 部队招募列表没有公开筛选参数，那一类不给入口。
                    if (kind != ShizhijiaRecruitKind.Guild) {
                        val on = filter.activeFor(kind)
                        val chips = szjFilterSummary(kind, filter, rs)
                        Row(
                            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SzjPressable(onClick = { rs.filterOpen.value = true }, shape = SzjChipShape) {
                                Row(
                                    Modifier.clip(SzjChipShape)
                                        .background(if (on) SzjAccentSoft else SzjCardRaised)
                                        .padding(horizontal = 11.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // 三条渐短的横线：漏斗的意思，比纯文字更快认出来
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        listOf(11.dp, 7.dp, 3.dp).forEach { w ->
                                            Box(Modifier.width(w).height(1.5.dp)
                                                .background(if (on) SzjOnAccentSoft else SzjMuted))
                                        }
                                    }
                                    Spacer(Modifier.width(7.dp))
                                    Text(
                                        if (on) "已筛选" else "筛选",
                                        style = SzjLabelStyle,
                                        color = if (on) SzjOnAccentSoft else SzjMuted,
                                    )
                                }
                            }
                            if (chips.isEmpty()) {
                                Spacer(Modifier.width(10.dp))
                                Text("全部招募", color = SzjMuted, style = SzjMetaStyle)
                                Spacer(Modifier.weight(1f))
                            } else {
                                LazyRow(
                                    Modifier.weight(1f).padding(start = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    items(chips) { c ->
                                        Text(
                                            c, color = SzjOnAccentSoft, style = SzjMetaStyle,
                                            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(SzjAccentSoft)
                                                .padding(horizontal = 7.dp, vertical = 3.dp),
                                        )
                                    }
                                    item {
                                        SzjPressable(onClick = { rs.setFilter(kind, ShizhijiaRecruitFilter()) }, shape = SzjChipShape) {
                                            Text("清空", color = SzjMuted, style = SzjMetaStyle,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
        ) {
            val status = rs.status.value[kind]
            when {
                rs.loading.value && items.isEmpty() -> item(key = "skeleton") { SzjFeedSkeleton() }
                items.isEmpty() -> item(key = "empty") {
                    SzjResState(
                        res = status,
                        emptyTitle = if (filter.activeFor(kind)) "没有符合条件的招募" else "这个分类暂时没有招募",
                        emptyHint = if (filter.activeFor(kind)) "放宽筛选条件试试" else "换个分类，或者稍后再来看看",
                        onLogin = { nav(SzjRoute.Login) },
                        inline = true,
                    )
                }
                else -> {
                    itemsIndexed(items, key = { _, it -> it.kind.name + it.id }) { index, r ->
                        SzjRise(index) { SzjRecruitRow(r, nav, rs.jobs.value) }
                    }
                    item(key = "footer") {
                        if (rs.loading.value) {
                            Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            }
                        } else if (rs.ended.value.contains(kind)) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                                Text("到底了", color = SzjHairline, style = SzjMetaStyle)
                            }
                        }
                    }
                }
            }
        }
        // 发布按钮：浮在右下、底栏之上。RP 和部队招募的发布表单没做，
        // 那两类不给入口（点了也发不出去）。
        if (kind != ShizhijiaRecruitKind.Rp && kind != ShizhijiaRecruitKind.Guild) {
            // 往下滑收起、往上滑放出，和悬浮底栏同一个信号源，两者动作一致。
            // 收起方向朝右下（它在右下角），比单纯淡出更像"滑走了"。
            val motion = szjMotionEnabled()
            val hide = bar.hidden
            val p by animateFloatAsState(
                if (hide) 1f else 0f,
                if (motion) tween(if (hide) 260 else 190, easing = FastOutSlowInEasing)
                else tween(0),
                label = "szjFabHide",
            )
            SzjPressable(
                onClick = { nav(SzjRoute.PublishRecruit(kind)) },
                modifier = Modifier.align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 108.dp)
                    .graphicsLayer {
                        alpha = 1f - p
                        translationX = p * 40f
                        translationY = p * 24f
                        scaleX = 1f - p * 0.2f
                        scaleY = 1f - p * 0.2f
                    },
                shape = CircleShape,
            ) {
                Row(
                    Modifier.shadow(8.dp, SzjChipShape, ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
                        .clip(SzjChipShape).background(SzjAccentFill)
                        .padding(horizontal = 15.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ImageGlyph(R.drawable.ic_add, SzjOnAccent, Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("发布", color = SzjOnAccent, style = SzjLabelStyle)
                }
            }
        }
        if (rs.filterOpen.value) {
            SzjRecruitFilterPanel(
                kind = kind,
                rs = rs,
                onClose = { rs.filterOpen.value = false },
            )
        }
    }
}

/** 把生效的筛选条件转成一行短标签，显示在分类 Tab 下方。 */
@Composable
private fun szjFilterSummary(
    kind: ShizhijiaRecruitKind,
    f: ShizhijiaRecruitFilter,
    rs: SzjRecruitState,
): List<String> = buildList {
    fun nameOf(pairs: List<Pair<String, String>>, id: String) = pairs.firstOrNull { it.first == id }?.second ?: id
    when (kind) {
        ShizhijiaRecruitKind.Fb -> {
            if (f.fbType.isNotBlank()) add(f.fbType)
            if (f.fbName.isNotBlank()) add(f.fbName)
            if (f.teamComposition.isNotBlank()) add(f.teamComposition)
            f.positions.forEach { add(it) }
            f.labelIds.forEach { add(nameOf(rs.fbLabels.value, it)) }
        }
        ShizhijiaRecruitKind.Novice -> {
            ShizhijiaRecruitFilter.NOVICE_IDENTITY.firstOrNull { it.first == f.identity }?.let { add(it.second) }
            f.styleIds.forEach { add(nameOf(rs.styles.value, it)) }
        }
        ShizhijiaRecruitKind.Other -> f.categoryIds.forEach { add(nameOf(rs.categories.value, it)) }
        ShizhijiaRecruitKind.Rp -> {
            f.rpTypes.forEach { id -> ShizhijiaRecruitFilter.RP_TYPES.firstOrNull { it.first == id }?.let { add(it.second) } }
            f.actStatus.forEach { id -> ShizhijiaRecruitFilter.RP_ACT_STATUS.firstOrNull { it.first == id }?.let { add(it.second) } }
        }
        ShizhijiaRecruitKind.Guild -> Unit
    }
    // 大区是三类共用的条件，放最后一枚。
    if (f.targetAreaId.isNotBlank()) {
        add(
            if (f.targetAreaId == "-1") "国际服"
            else rs.areas.value.firstOrNull { it.areaId.toString() == f.targetAreaId }?.areaName ?: "大区 ${f.targetAreaId}"
        )
    }
}

/**
 * 招募筛选面板。四类各有自己的条件（部队招募没有公开筛选参数，不进这里）：
 *   副本组队  副本类型 → 具体副本 → 位置 → 标签
 *   新人招待  身份 → 玩法风格
 *   其他      分类
 *   RP        RP 元素浓度 → 活动状态
 * 从顶部滑下，改完点"看结果"才发请求——边选边刷会打很多次接口。
 */
@Composable
private fun SzjRecruitFilterPanel(
    kind: ShizhijiaRecruitKind,
    rs: SzjRecruitState,
    onClose: () -> Unit,
) {
    // 面板里改的是草稿，确认后才写回 state 触发重新加载
    var draft by remember(kind) { mutableStateOf(rs.filterFor(kind)) }
    val noRipple = remember { MutableInteractionSource() }
    // 面板是覆盖层：返回键先关它，别一路穿透到关掉石之家。
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
            // 高度封到 72%：条件多的时候（副本那类有类型+副本+规模+位置+标签+大区）
            // 面板会一直长到屏幕外，"看结果"被悬浮底栏盖住点不到。
            // 现在选项区自己滚，两个按钮钉在面板底部。
            Column(
                Modifier.fillMaxWidth().fillMaxHeight(0.72f)
                    .shadow(12.dp, RoundedCornerShape(bottomEnd = 18.dp, bottomStart = 18.dp), ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
                    .clip(RoundedCornerShape(bottomEnd = 18.dp, bottomStart = 18.dp))
                    .background(SzjBg)
                    .clickable(interactionSource = noRipple, indication = null) { }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
              Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                when (kind) {
                    ShizhijiaRecruitKind.Fb -> {
                        val types = rs.fbConfig.value.map { it.fbType }.distinct().filter { it.isNotBlank() }
                        SzjFilterChips(
                            "副本类型",
                            listOf("" to "全部") + types.map { it to it },
                            selected = setOf(draft.fbType),
                        ) { id ->
                            // 换类型时把具体副本清掉，否则会出现类型和副本不匹配
                            draft = draft.copy(fbType = id, fbName = "")
                        }
                        // 选了类型才列具体副本，全部类型下 84 个副本铺满屏没法用
                        if (draft.fbType.isNotBlank()) {
                            val names = rs.fbConfig.value.filter { it.fbType == draft.fbType }
                                .map { it.fbName }.distinct().filter { it.isNotBlank() }
                            if (names.isNotEmpty()) {
                                Spacer(Modifier.height(14.dp))
                                SzjFilterChips(
                                    "具体副本",
                                    listOf("" to "不限") + names.map { it to it },
                                    selected = setOf(draft.fbName),
                                ) { draft = draft.copy(fbName = it) }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        SzjFilterChips(
                            "队伍规模",
                            listOf("" to "不限") + ShizhijiaRecruitFilter.TEAM_COMPOSITIONS,
                            selected = setOf(draft.teamComposition),
                        ) { draft = draft.copy(teamComposition = it) }
                        Spacer(Modifier.height(14.dp))
                        SzjFilterChips(
                            "位置（可多选）",
                            ShizhijiaRecruitFilter.FB_POSITIONS.map { it to it },
                            selected = draft.positions.toSet(),
                            multi = true,
                        ) { id -> draft = draft.copy(positions = draft.positions.toggle(id)) }
                        if (rs.fbLabels.value.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            SzjFilterChips(
                                "标签（可多选）",
                                rs.fbLabels.value,
                                selected = draft.labelIds.toSet(),
                                multi = true,
                            ) { id -> draft = draft.copy(labelIds = draft.labelIds.toggle(id)) }
                        }
                    }
                    ShizhijiaRecruitKind.Novice -> {
                        SzjFilterChips(
                            "身份",
                            listOf("0" to "全部") + ShizhijiaRecruitFilter.NOVICE_IDENTITY.map { it.first.toString() to it.second },
                            selected = setOf(draft.identity.toString()),
                        ) { draft = draft.copy(identity = it.toIntOrNull() ?: 0) }
                        if (rs.styles.value.isNotEmpty()) {
                            Spacer(Modifier.height(14.dp))
                            SzjFilterChips(
                                "玩法风格（可多选）",
                                rs.styles.value,
                                selected = draft.styleIds.toSet(),
                                multi = true,
                            ) { id -> draft = draft.copy(styleIds = draft.styleIds.toggle(id)) }
                        }
                    }
                    ShizhijiaRecruitKind.Other -> {
                        SzjFilterChips(
                            "分类（可多选）",
                            rs.categories.value,
                            selected = draft.categoryIds.toSet(),
                            multi = true,
                        ) { id -> draft = draft.copy(categoryIds = draft.categoryIds.toggle(id)) }
                    }
                    ShizhijiaRecruitKind.Rp -> {
                        SzjFilterChips(
                            "RP 元素（可多选）",
                            ShizhijiaRecruitFilter.RP_TYPES,
                            selected = draft.rpTypes.toSet(),
                            multi = true,
                        ) { id -> draft = draft.copy(rpTypes = draft.rpTypes.toggle(id)) }
                        Spacer(Modifier.height(14.dp))
                        SzjFilterChips(
                            "活动状态（可多选）",
                            ShizhijiaRecruitFilter.RP_ACT_STATUS,
                            selected = draft.actStatus.toSet(),
                            multi = true,
                        ) { id -> draft = draft.copy(actStatus = draft.actStatus.toggle(id)) }
                    }
                    ShizhijiaRecruitKind.Guild -> Unit
                }
                // 招募大区：副本/新人/其他三类都吃 target_area_id，放在最后共用一段。
                // RP 和部队招募没有这个参数。
                if (kind != ShizhijiaRecruitKind.Rp && kind != ShizhijiaRecruitKind.Guild &&
                    rs.areas.value.isNotEmpty()
                ) {
                    Spacer(Modifier.height(14.dp))
                    SzjFilterChips(
                        "招募大区",
                        listOf("" to "不限大区") +
                            rs.areas.value.map { it.areaId.toString() to it.areaName } +
                            listOf("-1" to "国际服"),
                        selected = setOf(draft.targetAreaId),
                    ) { draft = draft.copy(targetAreaId = it) }
                }
              }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SzjPressable(
                        onClick = { draft = ShizhijiaRecruitFilter() },
                        modifier = Modifier.weight(1f),
                        shape = SzjInnerShape,
                    ) {
                        Text("重置", color = SzjMuted, style = SzjLabelStyle, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clip(SzjInnerShape)
                                .border(1.dp, SzjHairline, SzjInnerShape).padding(vertical = 11.dp))
                    }
                    SzjPressable(
                        onClick = { rs.setFilter(kind, draft); onClose() },
                        modifier = Modifier.weight(1f),
                        shape = SzjInnerShape,
                    ) {
                        Text("看结果", color = SzjOnAccent, style = SzjLabelStyle, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().clip(SzjInnerShape).background(SzjAccentFill).padding(vertical = 11.dp))
                    }
                }
            }
        }
    }
}

/** 多选列表里加/减一项。 */
private fun List<String>.toggle(id: String): List<String> =
    if (contains(id)) this - id else this + id

/**
 * 发布表单的文本输入。单行和多行共用一个，`lines` 大于 1 时是多行。
 * 外观跟卡片体系一致：石板底 + 内圆角，聚焦时描边换成主色。
 */
@Composable
private fun SzjFormField(
    label: String,
    value: String,
    placeholder: String = "",
    lines: Int = 1,
    required: Boolean = false,
    maxLen: Int = 0,
    onChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (focused) SzjAccent else SzjHairline, tween(180), label = "szjFieldBorder")
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = SzjText, style = SzjLabelStyle)
            if (required) Text(" *", color = SzjAccent, style = SzjLabelStyle)
            Spacer(Modifier.weight(1f))
            if (maxLen > 0) Text("${value.length}/$maxLen", color = SzjMuted, style = SzjMetaStyle)
        }
        Spacer(Modifier.height(7.dp))
        BasicTextField(
            value = value,
            onValueChange = { if (maxLen <= 0 || it.length <= maxLen) onChange(it) },
            singleLine = lines <= 1,
            minLines = lines,
            textStyle = TextStyle(color = SzjText, fontSize = 14.sp, lineHeight = 21.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(SzjAccent),
            decorationBox = { inner ->
                Box(
                    Modifier.fillMaxWidth().clip(SzjInnerShape).background(SzjCard)
                        .border(1.dp, border, SzjInnerShape)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                ) {
                    if (value.isEmpty() && placeholder.isNotBlank()) {
                        Text(placeholder, color = SzjMuted, fontSize = 14.sp)
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
                .onFocusChanged { focused = it.isFocused },
        )
    }
}

/** 发布表单里的一组选项（单选/多选），复用筛选那套 chip 外观。 */
@Composable
private fun SzjFormChips(
    label: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    multi: Boolean = false,
    required: Boolean = false,
    onPick: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = SzjText, style = SzjLabelStyle)
            if (required) Text(" *", color = SzjAccent, style = SzjLabelStyle)
            if (multi) Text("  可多选", color = SzjMuted, style = SzjMetaStyle)
        }
        Spacer(Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            options.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { (id, name) ->
                        val on = id in selected
                        SzjPressable(onClick = { onPick(id) }, modifier = Modifier.weight(1f), shape = SzjChipShape) {
                            Text(
                                name,
                                color = if (on) SzjOnAccent else SzjMuted,
                                style = SzjLabelStyle,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().clip(SzjChipShape)
                                    .background(if (on) SzjAccentFill else SzjCardRaised)
                                    .padding(vertical = 9.dp, horizontal = 4.dp),
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** 一组筛选 chip。multi=true 时是多选（选中项各自高亮），否则单选。 */
@Composable
private fun SzjFilterChips(
    label: String,
    options: List<Pair<String, String>>,
    selected: Set<String>,
    multi: Boolean = false,
    onPick: (String) -> Unit,
) {
    Column {
        // 筛选标题不带棱条：棱条只留给一级分区标题和选中态，
        // 撒在每个小标题上单个看不见、满屏又显得碎。
        Text(label, color = SzjText, style = SzjLabelStyle)
        Spacer(Modifier.height(9.dp))
        // 每行 4 个，多了换行——横向滚动的话看不到后面还有多少
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            options.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { (id, name) ->
                        val on = selected.contains(id)
                        val bg by animateColorAsState(if (on) SzjAccent else SzjCardRaised, tween(180), label = "fchipBg")
                        val fg by animateColorAsState(if (on) SzjOnAccent else SzjMuted, tween(180), label = "fchipFg")
                        SzjPressable(onClick = { onPick(id) }, shape = SzjChipShape) {
                            Text(
                                name, fontSize = 12.sp, maxLines = 1,
                                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                color = fg,
                                modifier = Modifier.clip(SzjChipShape).background(bg)
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
 * 招募卡片。五类共用：封面（有就显示）→ 标题 → 分类特有的几行 → 标签 → 作者行。
 * 点作者进主页；卡片本身暂不进详情——详情接口虽然公开，但报名/上下架这些
 * 写操作没做，只读详情的价值不大，先让列表信息尽量完整。
 */
@Composable
private fun SzjRecruitRow(r: ShizhijiaRecruit, nav: (SzjRoute) -> Unit, jobs: Map<String, ShizhijiaJob>) {
    // 整张卡进招募详情；底部那行的头像/昵称单独点是进发布者主页。
    SzjCardSurface(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
        onClick = if (r.id.isNotBlank()) ({ nav(SzjRoute.RecruitDetail(r.kind, r.id)) }) else null,
    ) {
        if (r.coverPic.isNotBlank()) {
            Box(Modifier.clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp))) {
                ShizhijiaRemoteImage(
                    url = r.coverPic,
                    modifier = Modifier.fillMaxWidth().height(130.dp),
                    contentScale = ContentScale.Crop,
                    collapseOnFail = true,
                )
            }
        }
        Column(Modifier.padding(14.dp)) {
            // 标题行：`[副本类型]副本名` + 右侧面向服务器的角标。
            // 移动端就是这个排法——类型用主色方括号贴在名字前面，不占独立一行。
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    buildAnnotatedString {
                        if (r.titlePrefix.isNotBlank()) {
                            withStyle(SpanStyle(color = SzjAccent, fontWeight = FontWeight.SemiBold)) {
                                append("[${r.titlePrefix}]")
                            }
                        }
                        append(r.title)
                    },
                    color = SzjText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    lineHeight = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (r.targetServer.isNotBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        r.targetServer, color = SzjOnAccentSoft, style = SzjMetaStyle, maxLines = 1,
                        modifier = Modifier.clip(SzjChipShape).background(SzjAccentSoft)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
            // 键值行：键用主色，值用正文色。进度/攻略/时间这种一眼要扫到的信息。
            if (r.infoRows.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                r.infoRows.forEach { (k, v) ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(k, color = SzjAccent, fontSize = 12.sp, modifier = Modifier.width(34.dp))
                        Text(
                            v, color = SzjText, fontSize = 12.sp, lineHeight = 18.sp,
                            maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            // 分类特有的信息行（在线时段、营业时间…）
            if (r.lines.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                r.lines.forEach { line ->
                    Text(line, color = SzjMuted, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (r.summary.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(r.summary, color = SzjMuted, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (r.tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                // 标签最多展示 4 个，多了在窄屏上会换行成一大片。
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    r.tags.take(4).forEach { t ->
                        Text(
                            t, color = SzjOnAccentSoft, fontSize = 10.sp, maxLines = 1,
                            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(SzjAccentSoft)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            // 位置槽：空位显示 MT/ST/H1… 的位置名，占了的显示职业图标。
            if (r.slots.isNotEmpty()) {
                Spacer(Modifier.height(11.dp))
                SzjSlotRow(r.slots, jobs)
            }
            // 团队（24 人）：A/B/C 三队各一行。
            r.alliances.forEach { (g, slots) ->
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(g, color = SzjMuted, style = SzjLabelStyle, modifier = Modifier.width(16.dp))
                    SzjSlotRow(slots, jobs, modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(11.dp))
            Row(
                Modifier.clip(SzjChipShape)
                    .clickable(enabled = r.uuid.isNotBlank()) { nav(SzjRoute.UserProfile(r.uuid)) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SzjAvatar(r.characterName, r.avatar, r.uuid, 22)
                Spacer(Modifier.width(7.dp))
                Text(r.characterName.ifBlank { "匿名玩家" }, color = SzjMuted, style = SzjMetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val own = listOf(r.areaName, r.groupName).filter { it.isNotBlank() }.joinToString(" ")
                if (own.isNotBlank()) {
                    Spacer(Modifier.width(3.dp))
                    SzjLocPin(13)
                    Text(own, color = SzjMuted, style = SzjMetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.weight(1f))
                if (r.responseNum >= 0) Text("${r.responseNum} 报名", color = SzjMuted, style = SzjMetaStyle)
            }
        }
    }
}

/**
 * 招募详情。四类共用这一页——卡片部分复用列表那张卡（位置槽、标签都在里面），
 * 下面接详情独有的正文段落和有效期。
 *
 * 四个接口都是公开的（`?id=`，不用登录），只有部队招募的详情要登录。
 */
@Composable
private fun ShizhijiaRecruitDetailScreen(
    kind: ShizhijiaRecruitKind,
    id: String,
    pop: () -> Unit,
    nav: (SzjRoute) -> Unit,
) {
    val context = LocalContext.current
    var res by remember(kind, id) { mutableStateOf<ShizhijiaApi.Res<ShizhijiaRecruitDetail?>?>(null) }
    var jobs by remember { mutableStateOf(mapOf<String, ShizhijiaJob>()) }
    // 响应招募：填自己的联系方式 → 换回发布者的联系方式。
    var respondOpen by remember(kind, id) { mutableStateOf(false) }
    var responding by remember(kind, id) { mutableStateOf(false) }
    /** 响应成功后接口回的发布者联系方式（未打码）。空串表示还没拿到。 */
    var revealedContact by remember(kind, id) { mutableStateOf("") }
    val respondScope = rememberCoroutineScope()
    LaunchedEffect(kind, id) {
        jobs = ShizhijiaApi.getJobConfig(context)
        res = ShizhijiaApi.getRecruitDetail(context, kind, id)
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader(kind.label, onBack = pop)
        val d = (res as? ShizhijiaApi.Res.Ok)?.value
        when {
            res == null -> Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                SzjShimmerBox(Modifier.fillMaxWidth().height(200.dp), SzjCardShape)
            }
            d == null -> {
                val r = res
                SzjEmpty(
                    "看不到这条招募",
                    when (r) {
                        is ShizhijiaApi.Res.NeedLogin -> "这一类的详情要登录才能看"
                        is ShizhijiaApi.Res.Failed ->
                            r.msg.ifBlank { if (r.code == null) "网络没通" else "服务端返回 ${r.code}" }
                        else -> "可能已经下架了"
                    },
                ) {
                    if (r is ShizhijiaApi.Res.NeedLogin) SzjPrimaryButton("登录", onClick = { nav(SzjRoute.Login) })
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 2.dp, bottom = 24.dp),
            ) {
                // 顶部就是列表里那张卡，不另画一套——同一条招募在两处长得一样。
                item(key = "card") { SzjRecruitRow(d.card, nav, jobs) }
                items(d.sections, key = { it.first }) { (label, body) ->
                    SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(label, color = SzjText, style = SzjLabelStyle)
                            }
                            Spacer(Modifier.height(9.dp))
                            Text(body, color = SzjText, fontSize = 13.sp, lineHeight = 21.sp)
                        }
                    }
                }
                item(key = "meta") {
                    val rows = buildList {
                        if (d.dueDay >= 0) add("有效期" to "还剩 ${d.dueDay} 天")
                        if (d.card.responseNum >= 0) add("报名" to "${d.card.responseNum} 人")
                        if (d.updatedAt.isNotBlank()) add("更新" to d.updatedAt)
                        // 和帖子/评论用同一个说法。之前这里叫"发布地"，
                        // 同一个字段两个名字，看起来像两种东西。
                        // 这里是键值表（有独立的标签列），所以用完整的"IP属地"；
                        // 帖子/评论那种挤在一行里的用"属地"。裁"中国"前缀共用同一个函数。
                        szjIpShort(d.ipLocation).takeIf { it.isNotBlank() }?.let { add("IP属地" to it) }
                    }
                    if (rows.isNotEmpty()) {
                        SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)) {
                            Column(Modifier.padding(14.dp)) {
                                rows.forEachIndexed { i, (k, v) ->
                                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
                                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                                        Text(k, color = SzjMuted, style = SzjMetaStyle, modifier = Modifier.width(60.dp))
                                        Text(v, color = SzjText, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
                item(key = "respond") {
                    // 响应招募。原来这里只有一句"报名要在石之家网页或官方 App 里做"。
                    //
                    // 响应的实质是**交换联系方式**：你填自己的联系方式发过去，
                    // 接口回你发布者的真实联系方式（响应之前只能看到打码的）。
                    // 所以这一块要同时管三件事：填、发、显示拿到的联系方式。
                    val already = d.isResponse
                    val shown = revealedContact.ifBlank { if (already) d.contactInfoMask else "" }
                    SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                if (kind == ShizhijiaRecruitKind.Rp) "情景剧招募" else "发布者联系方式",
                                color = SzjText, style = SzjLabelStyle,
                            )
                            Spacer(Modifier.height(9.dp))
                            when {
                                kind == ShizhijiaRecruitKind.Rp -> Text(
                                    "情景剧招募没有「响应」这个动作，去评论区联系发布者",
                                    color = SzjMuted, fontSize = 12.sp, lineHeight = 18.sp,
                                )
                                shown.isNotBlank() -> {
                                    Text(shown, color = SzjText, fontSize = 14.sp, lineHeight = 21.sp)
                                    if (revealedContact.isBlank() && already) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            "你已经响应过这条招募（这里是打码的，重新进详情能看到完整的）",
                                            color = SzjMuted, style = SzjMetaStyle, lineHeight = 17.sp,
                                        )
                                    }
                                }
                                else -> {
                                    Text(
                                        "响应之后才能看到对方的联系方式，同时把你填的联系方式发给他",
                                        color = SzjMuted, fontSize = 12.sp, lineHeight = 18.sp,
                                    )
                                    if (d.contactInfoMask.isNotBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(d.contactInfoMask, color = SzjMuted, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    SzjPrimaryButton(
                                        if (responding) "发送中…" else "响应招募",
                                        onClick = { if (!responding) respondOpen = true },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (respondOpen) {
        SzjRecruitRespondDialog(
            initial = ShizhijiaSession.recruitContact(context),
            sending = responding,
            onDismiss = { respondOpen = false },
            onSend = { contact ->
                responding = true
                respondScope.launch {
                    when (val r = ShizhijiaApi.respondRecruit(context, kind, id, contact)) {
                        is ShizhijiaApi.Res.Ok -> {
                            // 记住联系方式，下次不用重新敲。
                            ShizhijiaSession.setRecruitContact(context, contact)
                            revealedContact = r.value
                            respondOpen = false
                            android.widget.Toast.makeText(
                                context,
                                if (r.value.isBlank()) "响应成功" else "响应成功，已拿到对方联系方式",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        }
                        else -> szjToastWriteFail(context, r, nav)
                    }
                    responding = false
                }
            },
        )
    }
}

/**
 * 响应招募的对话框：填自己的联系方式。
 *
 * [initial] 是上次填过的，直接带出来（这东西一个人基本不变）。
 */
@Composable
private fun SzjRecruitRespondDialog(
    initial: String,
    sending: Boolean,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit,
) {
    var contact by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { if (!sending) onDismiss() },
        title = { Text("响应招募", color = SzjText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "填你自己的联系方式发给发布者，发完就能看到他的",
                    color = SzjMuted, fontSize = 12.sp, lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                SzjFormField(
                    label = "我的联系方式",
                    value = contact,
                    placeholder = "QQ / 群号 / 微信…",
                    lines = 2,
                    required = true,
                    maxLen = 120,
                    onChange = { contact = it },
                )
            }
        },
        confirmButton = {
            SzjPrimaryButton(
                if (sending) "发送中…" else "发送",
                onClick = { if (!sending && contact.isNotBlank()) onSend(contact.trim()) },
            )
        },
        dismissButton = {
            Text(
                "取消",
                color = SzjMuted, fontSize = 14.sp,
                modifier = Modifier.clip(SzjInnerShape).clickable(enabled = !sending) { onDismiss() }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        },
        containerColor = SzjCardRaised,
    )
}

/** 副本组队的发布字段。副本表按类型二级联动，位置槽可以点着选职业。 */
@Composable
private fun SzjPublishFbFields(
    form: ShizhijiaRecruitForm,
    rs: SzjRecruitState,
    /** 点了位置槽：("" 或 "A"/"B"/"C" 的小队, 位置名)。选择层由页面层画。 */
    onPickSlot: (String, String) -> Unit,
    onChange: (ShizhijiaRecruitForm) -> Unit,
) {
    val types = rs.fbConfig.value.map { it.fbType }.distinct().filter { it.isNotBlank() }
    SzjFormChips("副本类型", types.map { it to it }, setOf(form.fbType), required = true) { t ->
        // 换类型时清掉副本名，并把规模跟到那个类型的默认值上
        val comp = rs.fbConfig.value.firstOrNull { it.fbType == t }?.teamComposition.orEmpty()
        onChange(form.copy(fbType = t, fbName = "", teamComposition = comp.ifBlank { form.teamComposition }))
    }
    if (form.fbType.isNotBlank()) {
        val names = rs.fbConfig.value.filter { it.fbType == form.fbType }
            .map { it.fbName }.distinct().filter { it.isNotBlank() }
        if (names.isNotEmpty()) {
            SzjFormChips("具体副本", names.map { it to it }, setOf(form.fbName), required = true) { n ->
                val comp = rs.fbConfig.value.firstOrNull { it.fbType == form.fbType && it.fbName == n }?.teamComposition.orEmpty()
                onChange(form.copy(fbName = n, teamComposition = comp.ifBlank { form.teamComposition }))
            }
        }
    }
    SzjFormChips(
        "队伍规模",
        ShizhijiaRecruitFilter.TEAM_COMPOSITIONS,
        setOf(form.teamComposition),
        required = true,
    ) { onChange(form.copy(teamComposition = it, slots = emptyMap(), alliance = emptyMap())) }

    // 位置：点一格选职业。留空表示这个位置在招人。
    val slotKeys = when (form.teamComposition) {
        "满编小队" -> listOf("MT", "ST", "H1", "H2", "D1", "D2", "D3", "D4")
        "轻锐小队" -> listOf("T", "H", "D1", "D2")
        else -> emptyList()
    }
    if (slotKeys.isNotEmpty()) {
        SzjPublishSlotPicker(
            keys = slotKeys,
            slots = form.slots,
            jobs = rs.jobs.value,
            onSet = { k, v -> onChange(form.copy(slots = form.slots + (k to v))) },
            onPick = { k -> onPickSlot("", k) },
        )
    } else if (form.teamComposition == "团队") {
        // 24 人：三个小队各一组
        listOf("A", "B", "C").forEach { g ->
            Text("$g 队", color = SzjAccent, style = SzjLabelStyle, modifier = Modifier.padding(bottom = 6.dp))
            SzjPublishSlotPicker(
                keys = listOf("MT", "ST", "H1", "H2", "D1", "D2", "D3", "D4"),
                slots = form.alliance[g].orEmpty(),
                jobs = rs.jobs.value,
                onSet = { k, v ->
                    val cur = form.alliance[g].orEmpty()
                    onChange(form.copy(alliance = form.alliance + (g to (cur + (k to v)))))
                },
                onPick = { k -> onPickSlot(g, k) },
            )
        }
    }

    // 招募职能：官方就是那七个"职能分类"，不是具体职业
    val roles = rs.jobs.value.values.filter { it.type == "职能分类" }.sortedBy { it.id.toIntOrNull() ?: 0 }
    if (roles.isNotEmpty()) {
        SzjFormChips(
            "招募职能", roles.map { it.id to it.name },
            selected = form.needJobs.toSet(), multi = true,
        ) { onChange(form.copy(needJobs = form.needJobs.toggle(it))) }
    }
    SzjFormField("进度", form.progress, "例如 P3 开荒 P2 练", maxLen = 30) { onChange(form.copy(progress = it)) }
    SzjFormField("攻略", form.strategy, "例如 猪野一套", maxLen = 30) { onChange(form.copy(strategy = it)) }
    SzjFormField("时间", form.fbTime, "例如 235 晚 9-11", maxLen = 30) { onChange(form.copy(fbTime = it)) }
    if (rs.fbLabels.value.isNotEmpty()) {
        SzjFormChips("标签", rs.fbLabels.value, form.labelIds.toSet(), multi = true) {
            onChange(form.copy(labelIds = form.labelIds.toggle(it)))
        }
    }
    SzjFormField("招募要求", form.recruitRequire, "对队友的要求", lines = 3, maxLen = 500) {
        onChange(form.copy(recruitRequire = it))
    }
    SzjFormField("备注", form.teamDetail, "叠甲、约定、别的想说的", lines = 3, maxLen = 500) {
        onChange(form.copy(teamDetail = it))
    }
}

/**
 * 位置槽选择器：点一格弹出职业列表，选完那格显示职业图标。
 * 再点已选中的格子清空（回到"这个位置在招人"）。
 */
@Composable
private fun SzjPublishSlotPicker(
    keys: List<String>,
    slots: Map<String, String>,
    jobs: Map<String, ShizhijiaJob>,
    onSet: (String, String) -> Unit,
    /**
     * 请求打开职业选择层。选择层不能画在这里——这个 composable 在
     * LazyColumn 的 item 里，item 的高度是无界的，里面再放 fillMaxSize
     * 会直接抛 infinite constraint 崩掉（0.7.219 点位置闪退就是这个）。
     * 所以由页面层统一画，这里只上报"点了哪个位置"。
     */
    onPick: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("我的位置", color = SzjText, style = SzjLabelStyle)
            Text("  点一格选自己玩的职业，留空＝这个位置在招人", color = SzjMuted, style = SzjMetaStyle)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            keys.forEach { k ->
                val jobId = slots[k].orEmpty()
                val filled = jobId.isNotBlank() && jobId != "0"
                val job = jobs[jobId]
                SzjPressable(
                    onClick = { if (filled) onSet(k, "0") else onPick(k) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(6.dp))
                            .background(if (filled) SzjAccentSoft else SzjCardRaised),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (filled && job != null && job.iconUrl.isNotBlank()) {
                            ShizhijiaRemoteImage(
                                url = job.iconUrl,
                                modifier = Modifier.fillMaxSize().padding(2.dp),
                                contentScale = ContentScale.Fit,
                                showPlaceholder = false,
                            )
                        } else {
                            Text(k, color = SzjMuted, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 职业选择层。必须画在页面层（不能在 LazyColumn 的 item 里），
 * 见 SzjPublishSlotPicker.onPick 的注释。
 */
@Composable
private fun SzjJobPickerSheet(
    slotName: String,
    jobs: Map<String, ShizhijiaJob>,
    onClose: () -> Unit,
    onPicked: (String) -> Unit,
) {
    val byRole = jobs.values.filter { it.type != "职能分类" }.groupBy { it.type }
    SzjSheet(title = "$slotName 位置的职业", onClose = onClose) {
        byRole.forEach { (role, list) ->
            Text(role, color = SzjAccent, style = SzjLabelStyle, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                list.sortedBy { it.id.toIntOrNull() ?: 0 }.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { j ->
                            SzjPressable(
                                onClick = { onPicked(j.id) },
                                modifier = Modifier.weight(1f),
                                shape = SzjChipShape,
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().clip(SzjChipShape).background(SzjCardRaised)
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    if (j.iconUrl.isNotBlank()) {
                                        ShizhijiaRemoteImage(
                                            url = j.iconUrl,
                                            modifier = Modifier.size(26.dp),
                                            contentScale = ContentScale.Fit,
                                            showPlaceholder = false,
                                        )
                                        Spacer(Modifier.height(4.dp))
                                    }
                                    Text(j.name, color = SzjText, fontSize = 10.sp, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

/** 从底部升起的选择层。发布表单里的二级选择（职业等）用它。 */
@Composable
private fun SzjSheet(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler { onClose() }
    val noRipple = remember { MutableInteractionSource() }
    Box(
        // **这个 0x8C000000 和 theme 里的 CanvasLabelScrim 数值相同，但不是同一件事**
        // ——那个是压在地图底图上的文字底衬，这个是模态层的遮罩。
        // 换成同一个 token 会把两者绑在一起：以后调标签底衬的深浅，
        // 会连带把所有弹层的遮罩一起改掉。**同值不等于同义，不合并。**
        Modifier.fillMaxSize().background(Color(0x8C000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.72f)
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(SzjBg)
                .clickable(interactionSource = noRipple, indication = null) { }
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                Text(title, color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                SzjPressable(onClick = onClose, shape = CircleShape) {
                    ImageGlyph(R.drawable.ic_close, SzjMuted, Modifier.padding(6.dp).size(16.dp))
                }
            }
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) { content() }
        }
    }
}

/** 新人招待的发布字段。 */
@Composable
private fun SzjPublishNoviceFields(
    form: ShizhijiaRecruitForm,
    rs: SzjRecruitState,
    onChange: (ShizhijiaRecruitForm) -> Unit,
) {
    SzjFormField("标题", form.title, "一句话说清你在找什么", required = true, maxLen = 40) {
        onChange(form.copy(title = it))
    }
    SzjFormChips(
        "我的身份",
        ShizhijiaRecruitFilter.NOVICE_IDENTITY.map { it.first.toString() to it.second },
        setOf(form.identity.toString()),
        required = true,
    ) { onChange(form.copy(identity = it.toIntOrNull() ?: 1)) }
    if (rs.styles.value.isNotEmpty()) {
        SzjFormChips("玩法风格", rs.styles.value, form.styleIds.toSet(), multi = true) {
            onChange(form.copy(styleIds = form.styleIds.toggle(it)))
        }
    }
    SzjFormField("工作日在线", form.weekdayTime, "例如 20:00-24:00", maxLen = 24) {
        onChange(form.copy(weekdayTime = it))
    }
    SzjFormField("周末在线", form.weekendTime, "例如 14:00-02:00", maxLen = 24) {
        onChange(form.copy(weekendTime = it))
    }
    SzjFormField("正文", form.detail, "想带什么、想学什么，写清楚一点更容易配到人", lines = 5, required = true, maxLen = 1000) {
        onChange(form.copy(detail = it))
    }
}

/** 其他招募的发布字段。 */
@Composable
private fun SzjPublishOtherFields(
    form: ShizhijiaRecruitForm,
    rs: SzjRecruitState,
    onChange: (ShizhijiaRecruitForm) -> Unit,
) {
    SzjFormField("标题", form.title, "一句话说清你在找什么", required = true, maxLen = 40) {
        onChange(form.copy(title = it))
    }
    if (rs.categories.value.isNotEmpty()) {
        SzjFormChips("分类", rs.categories.value, setOf(form.categoryId), required = true) {
            onChange(form.copy(categoryId = it))
        }
    }
    SzjFormField("正文", form.detail, "详细说明", lines = 5, required = true, maxLen = 1000) {
        onChange(form.copy(detail = it))
    }
}

/**
 * 发布招募。三类共用这一页，字段按 kind 分支。
 *
 * body 字段名对齐官网发布页（RecruitPublishInstance / Beginner / Others）。
 * RP 俱乐部和部队招募没做：前者要传封面图和营业时间，后者要校验团长身份，
 * 都得先有图片上传通道。
 */
@Composable
private fun ShizhijiaPublishRecruitScreen(
    kind: ShizhijiaRecruitKind,
    rs: SzjRecruitState,
    pop: () -> Unit,
    nav: (SzjRoute) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var form by remember(kind) { mutableStateOf(ShizhijiaRecruitForm()) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    // 正在给哪个位置选职业：(小队, 位置名)。小队为 "" 表示非团队。
    var picking by remember { mutableStateOf<Pair<String, String>?>(null) }
    // 发布要登录：先确认，没登录就直接引导，别等填完一屏才报错。
    var loggedIn by remember { mutableStateOf<Boolean?>(null) }
    LaunchedEffect(Unit) {
        loggedIn = ShizhijiaApi.isLoggedIn(context)
        // 字典（副本表、标签、风格、分类、大区）都是公开接口，进来就补齐。
        if (!rs.dictLoaded.value) {
            rs.dictLoaded.value = true
            rs.fbConfig.value = ShizhijiaApi.getFbConfig(context)
            rs.styles.value = ShizhijiaApi.getStyleConfig(context)
            rs.categories.value = ShizhijiaApi.getOtherCategories(context)
            rs.fbLabels.value = ShizhijiaApi.getFbLabels(context)
            rs.jobs.value = ShizhijiaApi.getJobConfig(context)
        }
        if (rs.areas.value.isEmpty()) rs.areas.value = ShizhijiaApi.getAreaList(context)
    }

    // 必填校验：缺什么直接说缺什么，不做成灰按钮让人猜。
    fun missing(): String = when (kind) {
        ShizhijiaRecruitKind.Fb -> when {
            form.fbType.isBlank() -> "选一个副本类型"
            form.fbName.isBlank() -> "选一个具体副本"
            form.targetAreaId.isBlank() -> "选招募大区"
            form.contactInfo.isBlank() -> "填联系方式"
            else -> ""
        }
        ShizhijiaRecruitKind.Novice -> when {
            form.title.isBlank() -> "填标题"
            form.targetAreaId.isBlank() -> "选招募大区"
            form.contactInfo.isBlank() -> "填联系方式"
            form.detail.isBlank() -> "填正文"
            else -> ""
        }
        else -> when {
            form.title.isBlank() -> "填标题"
            form.categoryId.isBlank() -> "选一个分类"
            form.targetAreaId.isBlank() -> "选招募大区"
            form.contactInfo.isBlank() -> "填联系方式"
            form.detail.isBlank() -> "填正文"
            else -> ""
        }
    }

    val submit: () -> Unit = {
        val miss = missing()
        if (miss.isNotBlank()) error = miss
        else {
            error = ""
            submitting = true
            scope.launch {
                val res = ShizhijiaApi.publishRecruit(context, kind, form)
                submitting = false
                when (res) {
                    is ShizhijiaApi.Res.Ok -> {
                        android.widget.Toast.makeText(context, res.value, android.widget.Toast.LENGTH_SHORT).show()
                        // 发布成功后清掉那一类的缓存，回去能看到新发的。
                        rs.items.value = rs.items.value - kind
                        rs.pages.value = rs.pages.value - kind
                        rs.ended.value = rs.ended.value - kind
                        pop()
                    }
                    is ShizhijiaApi.Res.NeedLogin -> error = "登录状态过期了，重新登录一次"
                    is ShizhijiaApi.Res.NeedCharacter -> error = "账号还没绑定角色"
                    is ShizhijiaApi.Res.Failed ->
                        error = res.msg.ifBlank { if (res.code == null) "网络没通" else "服务端返回 ${res.code}" }
                }
            }
        }
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("发布${kind.label}", onBack = pop)
        if (loggedIn == false) {
            SzjEmpty("发布招募要先登录", "登录后才能以你的角色名义发布") {
                SzjPrimaryButton("登录", onClick = { nav(SzjRoute.Login) })
            }
            return@ScreenFrame
        }
        // 职业选择层要盖在整页上，所以列表和它同在一个 Box 里。
        Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 28.dp,
            ),
        ) {
            item(key = "fields") {
                Column {
                    when (kind) {
                        ShizhijiaRecruitKind.Fb -> SzjPublishFbFields(
                            form, rs,
                            onPickSlot = { g, k -> picking = g to k },
                        ) { form = it }
                        ShizhijiaRecruitKind.Novice -> SzjPublishNoviceFields(form, rs) { form = it }
                        else -> SzjPublishOtherFields(form, rs) { form = it }
                    }
                    // 招募大区：三类共用。"不限大区" 不是有效的发布目标，所以没有空选项。
                    if (rs.areas.value.isNotEmpty()) {
                        SzjFormChips(
                            "招募大区",
                            rs.areas.value.map { it.areaId.toString() to it.areaName },
                            selected = setOf(form.targetAreaId),
                            required = true,
                        ) { form = form.copy(targetAreaId = it, targetGroupId = "") }
                        // 选了大区再列服务器（可不选，代表整个大区）。
                        val groups = rs.areas.value.firstOrNull { it.areaId.toString() == form.targetAreaId }?.groups
                        if (!groups.isNullOrEmpty() && kind != ShizhijiaRecruitKind.Fb) {
                            SzjFormChips(
                                "服务器（不选＝整个大区）",
                                listOf("" to "整个大区") + groups.map { it.first.toString() to it.second },
                                selected = setOf(form.targetGroupId),
                            ) { form = form.copy(targetGroupId = it) }
                        }
                    }
                    SzjFormField(
                        "联系方式", form.contactInfo,
                        placeholder = "游戏 ID / QQ 群 / 别的能找到你的方式",
                        required = true, maxLen = 60,
                    ) { form = form.copy(contactInfo = it) }
                }
            }
            item(key = "submit") {
                Column {
                    if (error.isNotBlank()) {
                        Text(
                            error, color = SzjAccent, style = SzjMetaStyle,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                        )
                    }
                    SzjPressable(
                        onClick = { if (!submitting) submit() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = SzjInnerShape,
                    ) {
                        Box(
                            Modifier.fillMaxWidth().clip(SzjInnerShape)
                                .background(if (submitting) SzjCardRaised else SzjAccentFill)
                                .padding(vertical = 13.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (submitting) {
                                CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            } else {
                                Text("发布", color = SzjOnAccent, style = SzjLabelStyle)
                            }
                        }
                    }
                    Text(
                        "发布后可以在 我 → 招募管理 里擦亮或下架",
                        color = SzjMuted, style = SzjMetaStyle, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    )
                }
            }
        }
        picking?.let { (group, slot) ->
            SzjJobPickerSheet(
                slotName = slot,
                jobs = rs.jobs.value,
                onClose = { picking = null },
                onPicked = { jobId ->
                    form = if (group.isBlank()) {
                        form.copy(slots = form.slots + (slot to jobId))
                    } else {
                        val cur = form.alliance[group].orEmpty()
                        form.copy(alliance = form.alliance + (group to (cur + (slot to jobId))))
                    }
                    picking = null
                },
            )
        }
        }
    }
}

/**
 * 位置槽一行。空位是一个浅色方块 + 位置名（MT/ST/H1…），
 * 有人报名的位置换成那个人的职业图标——一眼能看出还缺什么。
 * 8 个格子平分宽度，窄屏上也不会挤出边。
 */
@Composable
private fun SzjSlotRow(
    slots: List<ShizhijiaSlot>,
    jobs: Map<String, ShizhijiaJob>,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        slots.forEach { s ->
            val job = jobs[s.jobId]
            Box(
                Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(6.dp))
                    .background(if (s.filled) SzjAccentSoft else SzjCardRaised),
                contentAlignment = Alignment.Center,
            ) {
                if (s.filled && job != null && job.iconUrl.isNotBlank()) {
                    ShizhijiaRemoteImage(
                        url = job.iconUrl,
                        modifier = Modifier.fillMaxSize().padding(2.dp),
                        contentScale = ContentScale.Fit,
                        showPlaceholder = false,
                        collapseOnFail = false,
                    )
                } else {
                    // 图标还没到、或者字典里没这个 id：退回位置名，别留白格。
                    Text(
                        s.name,
                        color = if (s.filled) SzjOnAccentSoft else SzjMuted,
                        fontSize = 11.sp,
                        fontWeight = if (s.filled) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Post detail
// ---------------------------------------------------------------------------

@Composable
private fun ShizhijiaPostDetailScreen(state: PhoneState, postId: String, pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    var detail by remember { mutableStateOf<ShizhijiaPostDetail?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Comments are rendered inline below the article body on the same screen.
    // Server order values: default (post time asc) / hot (likes) / time (newest).
    var commentOrder by remember { mutableStateOf("earliest") }
    var onlyAuthor by remember { mutableStateOf(false) } // 只看楼主
    var comments by remember { mutableStateOf(listOf<ShizhijiaComment>()) }
    var commentPage by remember { mutableStateOf(1) }
    var commentPageTime by remember { mutableStateOf("") }
    var commentLoading by remember { mutableStateOf(false) }
    // 正文和评论各自一条滚动：原来是一整条 LazyColumn（正文在上、评论接在下面），
    // 长帖要一直滑到底才见得到第一条评论。现在分成两页横向切换，
    // 两页各记自己的位置——看完评论切回正文，还在原来那一行。
    val articleState = rememberLazyListState()
    val commentState = rememberLazyListState()

    // ---- 点赞 / 收藏 / 评论 ----
    // 三个状态在本地跟着走，不整屏重拉：点赞收藏是切换接口，返回值告诉你
    // 现在是开还是关，按返回值定状态（不能"点了就当赞了"——可能是取消）。
    var liked by remember(postId) { mutableStateOf(false) }
    var starred by remember(postId) { mutableStateOf(false) }
    var likeNum by remember(postId) { mutableStateOf(0L) }
    var starNum by remember(postId) { mutableStateOf(0L) }
    var busy by remember(postId) { mutableStateOf(false) }
    var composerOpen by remember(postId) { mutableStateOf(false) }
    // 非 null 表示这次打开输入框是**回复某条评论**，而不是给帖子发新评论。
    // 发送时据此决定 parent_id / root_parent。
    var replyTo by remember(postId) { mutableStateOf<ShizhijiaComment?>(null) }
    val actionScope = rememberCoroutineScope()

    LaunchedEffect(postId) {
        loading = true
        val d = ShizhijiaApi.getPostDetail(context, postId)
        detail = d
        if (d != null) {
            liked = d.isLike
            starred = d.isStar
            likeNum = d.likeCount
            starNum = d.starCount
        }
        loading = false
    }
    // (Re)load comments whenever the ordering changes. The old list stays
    // visible while fetching and the scroll position is not touched.
    LaunchedEffect(postId, commentOrder, onlyAuthor) {
        commentLoading = true
        val result = ShizhijiaApi.getPostComments(context, postId, commentOrder, onlyLandlord = onlyAuthor)
        comments = result.rows; commentPageTime = result.pageTime; commentPage = 1
        commentLoading = false
    }
    // Infinite scroll for comments —— 挂在评论那一页的滚动上。
    val nearEnd by remember { derivedStateOf {
        val last = commentState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        last >= comments.size - 2
    } }
    LaunchedEffect(nearEnd, commentOrder, postId, onlyAuthor) {
        if (nearEnd && !commentLoading && comments.isNotEmpty() && commentPageTime.isNotBlank()) {
            commentLoading = true
            val next = ShizhijiaApi.getPostComments(context, postId, commentOrder, page = commentPage + 1, pageTime = commentPageTime, onlyLandlord = onlyAuthor)
            if (next.rows.isEmpty()) commentPageTime = "" else {
                comments = comments + next.rows
                commentPageTime = next.pageTime
                commentPage += 1
            }
            commentLoading = false
        }
    }

    // 删帖入口：**只在这是自己的帖子时出现**。判据是帖子的 uuid 和登录账号的
    // uuid 相同——官网那边是父组件决定要不要渲染删除项，同一个意思。
    // 拿不到登录 uuid（没登录）时一律不显示。
    val myUuid = remember(postId) {
        com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSession.cachedLoginUser(context)?.uuid.orEmpty()
    }
    var confirmDelete by remember(postId) { mutableStateOf(false) }
    var deleting by remember(postId) { mutableStateOf(false) }

    ScreenFrame(background = SzjBg) {
        val d0 = detail
        val isMine = d0 != null && myUuid.isNotBlank() && d0.uuid == myUuid
        SzjHeader(
            "帖子详情",
            onBack = { pop() },
            trailing = if (!isMine) null else ({
                SzjPressable(onClick = { confirmDelete = true }, shape = SzjChipShape) {
                    Row(
                        Modifier.clip(SzjChipShape).padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ImageGlyph(R.drawable.ic_trash, com.quserh.eorzeaphone.ui.theme.PhoneDanger, Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除", color = com.quserh.eorzeaphone.ui.theme.PhoneDanger, style = SzjMetaStyle)
                    }
                }
            }),
        )
        if (confirmDelete) {
            // 删帖不可撤销，所以要二次确认，而且确认按钮就叫"删除"不叫"确定"——
            // 按钮说清它做什么，别让人回去读标题才知道点下去会发生什么。
            AlertDialog(
                onDismissRequest = { if (!deleting) confirmDelete = false },
                title = { Text("删除这篇帖子？", color = SzjText) },
                text = { Text("删了就找不回来了，评论也会一起消失。", color = SzjMuted, fontSize = 13.sp) },
                confirmButton = {
                    TextButton(
                        enabled = !deleting,
                        onClick = {
                            deleting = true
                            actionScope.launch {
                                when (val r = ShizhijiaApi.deletePost(context, postId)) {
                                    is ShizhijiaApi.Res.Ok -> {
                                        android.widget.Toast.makeText(context, r.value, android.widget.Toast.LENGTH_SHORT).show()
                                        confirmDelete = false
                                        pop()
                                    }
                                    else -> szjToastWriteFail(context, r, nav)
                                }
                                deleting = false
                            }
                        },
                    ) { Text(if (deleting) "删除中" else "删除", color = com.quserh.eorzeaphone.ui.theme.PhoneDanger) }
                },
                dismissButton = {
                    TextButton(enabled = !deleting, onClick = { confirmDelete = false }) {
                        Text("再想想", color = SzjMuted)
                    }
                },
                containerColor = SzjCard,
            )
        }
        if (loading && detail == null) {
            // 详情骨架：标题两行 + 作者行 + 正文块，位置和真内容对齐。
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                SzjShimmerBox(Modifier.fillMaxWidth(0.85f).height(19.dp), RoundedCornerShape(4.dp))
                Spacer(Modifier.height(8.dp))
                SzjShimmerBox(Modifier.fillMaxWidth(0.5f).height(19.dp), RoundedCornerShape(4.dp))
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SzjShimmerBox(Modifier.size(36.dp), RoundedCornerShape(18.dp))
                    Spacer(Modifier.width(10.dp))
                    SzjShimmerBox(Modifier.width(120.dp).height(13.dp), RoundedCornerShape(4.dp))
                }
                Spacer(Modifier.height(20.dp))
                repeat(5) {
                    SzjShimmerBox(Modifier.fillMaxWidth().height(13.dp), RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(9.dp))
                }
            }
            return@ScreenFrame
        }
        val d = detail
        if (d == null) {
            SzjEmpty("这篇帖子没能打开", "可能已被删除，或者网络断了一下。返回再试一次")
            return@ScreenFrame
        }
        // 正文和评论还是**一条流**（评论接在正文后面），左滑只是"跳到评论那一行"。
        //
        // 上一版我把它切成了横滑两页——那样评论区变成独立的一页，跳过去之后
        // 往上滑看到的是评论区的顶，看不到帖子内容了。分页解决了"够不着"，
        // 却把"看完评论顺手往上翻正文"这件事弄没了。
        //
        // 现在：一条 LazyColumn，左滑 = animateScrollToItem(评论区那个 item)。
        // 跳过去之后正文就在上面，往上滑就能看，位置关系没有断。
        val jumpScope = rememberCoroutineScope()
        // 评论区第一个 item 在这条流里的下标 = 正文部分的 item 个数。
        // 写成这两个 key 的列表而不是字面量 2：正文再加一段的时候，
        // 忘了改这里就会滑到错的地方，而那种错很难看出来。
        // 下面 LazyColumn 里正文那些 item 用的就是这些 key，顺序一致。
        val articleKeys = listOf("post-header", "post-body")
        val commentAnchor = articleKeys.size
        val jumpToComments: () -> Unit = {
            jumpScope.launch { articleState.animateScrollToItem(commentAnchor) }
            Unit
        }
        LazyColumn(
            state = articleState,
            modifier = Modifier.fillMaxWidth().weight(1f)
                // 左滑跳评论。只认横向占优、且确实是往左的手势，
                // 竖向滚动照常（判定交给 detectHorizontalDragGestures 自己的
                // 方向锁：它只在横向位移先越过触摸阈值时才接管）。
                //
                // 方向定左不定右还有个实际好处：安卓手势导航下，从屏幕左缘
                // 往右划是系统返回手势，会被系统先吃掉。往左划没这个冲突。
                .pointerInput(commentAnchor) {
                    var dx = 0f
                    val threshold = 56.dp.toPx()
                    detectHorizontalDragGestures(
                        onDragStart = { dx = 0f },
                        onDragEnd = {
                            // 阈值取 56dp：比误触大，比"翻页"手势小。
                            // dx 往右为正，所以往左划是负的。
                            if (dx < -threshold) jumpToComments()
                        },
                        onHorizontalDrag = { _, amount -> dx += amount },
                    )
                },
        ) {
            item(key = articleKeys[0]) {
                // 标题和作者收进一张石板：正文是长内容，先给它一个明确的"头"。
                SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(15.dp)) {
                        Text(d.title, color = SzjText, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = 0.1.sp)
                        Spacer(Modifier.height(13.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(SzjInnerShape).clickable { nav(SzjRoute.UserProfile(d.uuid)) }) {
                            SzjAvatar(d.characterName, d.avatar, d.uuid, 36)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                // 属地挂在名字行右端：那一行右边本来空着，
                                // 放这儿既不占新行、也不再挤压下面的区服。
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        d.characterName, color = SzjText, fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    SzjIpTail(d.ipLocation)
                                }
                                SzjMetaLine(d.areaName, d.groupName, d.createdAt)
                            }
                        }
                        // 四个计数排成一行小标签，用点分隔而不是各自留白。
                        val stats = buildList {
                            if (d.readCount > 0) add("${d.readCount} 阅读")
                            if (d.likeCount > 0) add("${d.likeCount} 赞")
                            if (d.commentCount > 0) add("${d.commentCount} 评论")
                            if (d.starCount > 0) add("${d.starCount} 收藏")
                        }
                        if (stats.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                stats.forEachIndexed { i, t ->
                                    if (i > 0) Text(" · ", color = SzjMuted, style = SzjMetaStyle)
                                    Text(t, color = SzjMuted, style = SzjMetaStyle)
                                }
                            }
                        }
                    }
                }
            }
            item(key = articleKeys[1]) {
                // 正文直接落在底色上，**不**再套第二张石板。
                //
                // 0.7.22x 那版给正文也包了一张卡，于是"作者"一张卡、"正文"另一张卡，
                // 中间一道沟——同一篇帖子被切成两块，这就是割裂感的来源。
                // 上面那张卡是这篇文章的"头"（标题+署名+计数），正文是文章本身，
                // 它不需要自己的容器。
                Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                    ShizhijiaRichContent(d.contentHtml)
                }
            }
            // 评论区：接在正文后面，同一条流。这个 item 的下标就是 commentAnchor，
            // 左滑跳到的就是它。
            szjCommentSection(
                comments = comments,
                commentLoading = commentLoading,
                onlyAuthor = onlyAuthor,
                onToggleAuthor = { onlyAuthor = !onlyAuthor },
                commentOrder = commentOrder,
                onOrder = { commentOrder = it },
                nav = nav,
                onReply = { target ->
                    replyTo = target
                    composerOpen = true
                },
            )
        }
        // 底部动作条：赞 / 收藏 / 评论。三个都是真请求。
        SzjPostActionBar(
            liked = liked, likeNum = likeNum,
            starred = starred, starNum = starNum,
            commentNum = d.commentCount,
            busy = busy,
            onLike = {
                if (!busy) actionScope.launch {
                    busy = true
                    when (val r = ShizhijiaApi.likePost(context, postId)) {
                        is ShizhijiaApi.Res.Ok -> {
                            // 按返回值定状态：这是个切换接口，返回 Off 说明刚取消。
                            val on = r.value == ShizhijiaApi.Toggle.On
                            liked = on
                            likeNum = (likeNum + if (on) 1 else -1).coerceAtLeast(0)
                        }
                        else -> szjToastWriteFail(context, r, nav)
                    }
                    busy = false
                }
            },
            onStar = {
                if (!busy) actionScope.launch {
                    busy = true
                    when (val r = ShizhijiaApi.starPost(context, postId)) {
                        is ShizhijiaApi.Res.Ok -> {
                            val on = r.value == ShizhijiaApi.Toggle.On
                            starred = on
                            starNum = (starNum + if (on) 1 else -1).coerceAtLeast(0)
                            android.widget.Toast.makeText(
                                context,
                                if (on) "已收藏" else "已取消收藏",
                                android.widget.Toast.LENGTH_SHORT,
                            ).show()
                        }
                        else -> szjToastWriteFail(context, r, nav)
                    }
                    busy = false
                }
            },
            onComment = { composerOpen = true },
        )
    }
    if (composerOpen) {
        val target = replyTo
        SzjCommentComposer(
            // 回复谁交给上面那条引用说（placeholder 一打字就消失，靠不住）。
            hint = if (target != null) "写你的回复…" else "说点什么…",
            replyTo = target,
            onDismiss = { composerOpen = false; replyTo = null },
            onSend = { text, pics, done ->
                actionScope.launch {
                    // 回复：parent_id 是被回复的那一条，root_parent 是它所在那一楼。
                    // 给帖子发新评论时两个都是 "0"。
                    when (
                        val r = ShizhijiaApi.commentPost(
                            context, postId, text,
                            parentId = target?.id ?: "0",
                            rootParent = target?.rootParent ?: "0",
                            pics = pics,
                        )
                    ) {
                        is ShizhijiaApi.Res.Ok -> {
                            android.widget.Toast.makeText(context, r.value, android.widget.Toast.LENGTH_SHORT).show()
                            composerOpen = false
                            replyTo = null
                            // 重新拉第一页评论，让自己那条出现。
                            // 回复也要重拉：那一楼的 children_count 变了，
                            // 不重拉的话"N 条回复"还是旧数字。
                            commentLoading = true
                            val result = ShizhijiaApi.getPostComments(context, postId, commentOrder, onlyLandlord = onlyAuthor)
                            comments = result.rows
                            commentPageTime = result.pageTime
                            commentPage = 1
                            commentLoading = false
                        }
                        else -> szjToastWriteFail(context, r, nav)
                    }
                    done()
                }
            },
        )
    }
}

/**
 * 写操作失败时的统一提示。
 *
 * 未登录/没绑角色要说清是哪一种，并且能直接去登录——原来所有失败都压成
 * 一句"失败了"，已登录的人也会看到"请登录"。
 */
private fun szjToastWriteFail(
    context: android.content.Context,
    res: ShizhijiaApi.Res<*>,
    nav: (SzjRoute) -> Unit,
) {
    val msg = when (res) {
        is ShizhijiaApi.Res.NeedLogin -> "要先登录石之家"
        is ShizhijiaApi.Res.NeedCharacter -> "登录了，但还没绑定游戏角色"
        is ShizhijiaApi.Res.Failed -> res.msg.ifBlank { "没成功，稍后再试" }
        else -> "没成功，稍后再试"
    }
    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
    if (res is ShizhijiaApi.Res.NeedLogin) nav(SzjRoute.Login)
}

/**
 * 帖子详情底部的动作条。
 *
 * 赞和收藏是**切换**：图标在 filled / outline 之间换，不是只换个颜色——
 * 只靠颜色深浅表达"开/关"在浅色主题下几乎看不出来。
 */
@Composable
private fun SzjPostActionBar(
    liked: Boolean,
    likeNum: Long,
    starred: Boolean,
    starNum: Long,
    commentNum: Long,
    busy: Boolean,
    onLike: () -> Unit,
    onStar: () -> Unit,
    onComment: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
        Row(
            Modifier.fillMaxWidth().background(SzjCardRaised).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SzjActionCell(
                icon = if (liked) R.drawable.ic_heart else R.drawable.ic_heart_outline,
                label = if (likeNum > 0) formatCount(likeNum) else "赞",
                on = liked,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                onClick = onLike,
            )
            SzjActionCell(
                icon = if (starred) R.drawable.ic_star_filled else R.drawable.ic_star_outline,
                label = if (starNum > 0) formatCount(starNum) else "收藏",
                on = starred,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                onClick = onStar,
            )
            SzjActionCell(
                icon = R.drawable.ic_comment,
                label = if (commentNum > 0) formatCount(commentNum) else "回帖",
                on = false,
                enabled = !busy,
                modifier = Modifier.weight(1f),
                onClick = onComment,
            )
        }
    }
}

/**
 * 发评论 / 回复。
 *
 * 上一版是 AlertDialog 套一个 SzjFormField（带"内容"标签、外描边框、
 * 独立的取消/发送按钮行）。那是**表单**的样子，套在一句话的输入上就是
 * 一堆和内容无关的边框和标签：标签"内容"没有信息（这里只可能输内容）、
 * 系统对话框的内边距把输入框挤成一条窄缝、发送按钮离输入框很远。
 *
 * 现在是从底部升起的一层：正文区就是文本本身（无标签、无外框），
 * 发送是右下角一个圆形图标钮，跟着有没有内容亮/暗。字数只在接近上限时出现。
 *
 * @param onSend 发送回调。第二个参数是"我这边结束了"的通知，
 *   请求成功与否都要调，否则按钮一直停在"发送中"。
 */
@Composable
private fun SzjCommentComposer(
    hint: String,
    onDismiss: () -> Unit,
    /**
     * 正在回复的那条评论。非 null 时输入框顶上显示一条**引用**：
     * 谁说的 + 说了什么。
     *
     * 为什么必须有：输入框是从底部升起来的，一升起来就把被回复的那条评论
     * 盖住了。原来只把名字塞进 placeholder，而 placeholder **一开始打字就消失**
     * ——于是打到一半完全不知道自己在回谁、回的哪句话。
     */
    replyTo: ShizhijiaComment? = null,
    /** 第二个参数是已上传好的图片 URL（逗号分隔，没图时空串）。 */
    onSend: (String, String, () -> Unit) -> Unit,
) {
    // 用 TextFieldValue 而不是 String：插表情要往**光标处**插，
    // 只存 String 的话拿不到光标位置，只能往末尾拼——那样在中间打字时很别扭。
    var field by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    val text = field.text
    var sending by remember { mutableStateOf(false) }
    var emojiOpen by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val maxLen = 500
    val ctx = LocalContext.current
    val picScope = rememberCoroutineScope()
    // 已上传成功的图片：(本地缩略图, 远端 URL)。
    // 选完就立刻上传，不等到点发送——发送时才传的话，人点了发送要干等，
    // 而且传失败了内容还在手里不知道该怎么办。
    val pics = remember { mutableStateListOf<Pair<android.graphics.Bitmap?, String>>() }
    var uploading by remember { mutableStateOf(false) }
    val maxPics = 9
    val picker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        picScope.launch {
            when (val r = ShizhijiaCosUpload.upload(ctx, uri, channel = "posts")) {
                is ShizhijiaApi.Res.Ok -> {
                    val thumb = runCatching {
                        ctx.contentResolver.openInputStream(uri)?.use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        }
                    }.getOrNull()
                    pics.add(thumb to r.value)
                }
                is ShizhijiaApi.Res.NeedLogin ->
                    android.widget.Toast.makeText(ctx, "要先登录石之家", android.widget.Toast.LENGTH_SHORT).show()
                is ShizhijiaApi.Res.NeedCharacter ->
                    android.widget.Toast.makeText(ctx, "账号要先绑定角色才能传图", android.widget.Toast.LENGTH_LONG).show()
                is ShizhijiaApi.Res.Failed ->
                    android.widget.Toast.makeText(ctx, r.msg.ifBlank { "上传失败" }, android.widget.Toast.LENGTH_LONG).show()
            }
            uploading = false
        }
    }
    /** 往光标处插一段文本，并把光标移到插入内容之后。 */
    val insert: (String) -> Unit = { s ->
        val t = field.text
        val start = field.selection.start.coerceIn(0, t.length)
        val end = field.selection.end.coerceIn(start, t.length)
        val next = t.substring(0, start) + s + t.substring(end)
        if (next.length <= maxLen) {
            field = androidx.compose.ui.text.input.TextFieldValue(
                text = next,
                selection = androidx.compose.ui.text.TextRange(start + s.length),
            )
        }
    }
    // 一进来就聚焦并弹键盘：这一层唯一的用途就是打字，还要人再点一下才起键盘
    // 等于白占一步。
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!sending) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // 让这一层能收到键盘 inset，否则下面的 windowInsetsPadding(ime) 拿到 0。
        SzjDialogImeFix()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            // 点空白处收起。
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = .32f))
                    .clickable(enabled = !sending) { onDismiss() },
            )
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(SzjCard)
                    // union 取每边较大值：键盘起来时用 ime（它已包含导航栏那一段），
                    // 收起时用导航栏。两个分开叠会多让一次，键盘上方空一条。
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                // 回复谁：一条引用，打字过程中一直在。
                if (replyTo != null) {
                    SzjReplyQuote(replyTo)
                    Spacer(Modifier.height(10.dp))
                }
                BasicTextField(
                    value = field,
                    onValueChange = { if (it.text.length <= maxLen) field = it },
                    textStyle = TextStyle(color = SzjText, fontSize = 15.sp, lineHeight = 23.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(SzjAccent),
                    enabled = !sending,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth().heightIn(min = 76.dp)) {
                            // 占位符就是提示，不再另起一行"内容"标签。
                            if (text.isEmpty()) Text(hint, color = SzjMuted, fontSize = 15.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                // 效果预览。**只在插了表情时出现**——纯文字不需要预览，
                // 输入框里看到的就是发出去的样子。
                //
                // 原来这儿是一条"表情 + 一排小图"的独立条：那是把表情单独列出来，
                // 而人想知道的是"这句话连着表情长什么样"。现在直接用渲染帖子/评论
                // 的那个渲染器画一遍，所见即所得，不用再自己拼一套。
                SzjComposerPreview(text)
                // 已选的图。缩略图右上角一个叉可以去掉——传上去了但不想要，
                // 总得有办法撤。
                if (pics.isNotEmpty() || uploading) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(pics.size, key = { pics[it].second }) { i ->
                            Box {
                                val thumb = pics[i].first
                                if (thumb != null) {
                                    Image(
                                        thumb.asImageBitmap(), contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(62.dp).clip(SzjInnerShape),
                                    )
                                } else {
                                    Box(Modifier.size(62.dp).clip(SzjInnerShape).background(SzjCardRaised))
                                }
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(3.dp)
                                        .size(18.dp).clip(CircleShape)
                                        .background(Color.Black.copy(alpha = .55f))
                                        .clickable { pics.removeAt(i) },
                                    contentAlignment = Alignment.Center,
                                ) { ImageGlyph(R.drawable.ic_close, Color.White, Modifier.size(10.dp)) }
                            }
                        }
                        if (uploading) {
                            item(key = "uploading") {
                                Box(
                                    Modifier.size(62.dp).clip(SzjInnerShape).background(SzjCardRaised),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        color = SzjAccent, strokeWidth = 2.dp,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                // 工具行和上面的内容之间拉一道发丝线：原来四块（输入框、预览、
                // 图片、工具）等距堆着，谁也不领谁，看着散。一条线把它分成
                // "在写的东西"和"操作"两部分，工具行就成了页脚而不是第四块。
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // 加图。上限 9 张（和站点一致），到上限就禁用而不是消失——
                    // 消失了人会以为功能没了。
                    val canAddPic = pics.size < maxPics && !uploading && !sending
                    SzjPressable(
                        onClick = {
                            if (pics.size >= maxPics) {
                                android.widget.Toast.makeText(ctx, "最多 $maxPics 张", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                picker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    )
                                )
                            }
                        },
                        shape = CircleShape,
                        enabled = canAddPic,
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(SzjCardRaised),
                            contentAlignment = Alignment.Center,
                        ) {
                            ImageGlyph(
                                R.drawable.ic_add,
                                if (canAddPic) SzjMuted else SzjLine,
                                Modifier.size(15.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    // 表情入口。选中态高亮，再点一下收起。
                    // 原来这儿放的是一个"颜"字——字形和旁边的真图标不齐（基线、
                    // 粗细、视觉重量全不一样），一眼就看出是凑的。换成真图标。
                    SzjPressable(onClick = { emojiOpen = !emojiOpen }, shape = CircleShape) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(if (emojiOpen) SzjAccentSoft else SzjCardRaised),
                            contentAlignment = Alignment.Center,
                        ) {
                            ImageGlyph(
                                R.drawable.ic_emoji,
                                if (emojiOpen) SzjOnAccentSoft else SzjMuted,
                                Modifier.size(17.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    // 字数只在快满时才出现——一直显示 0/500 是噪音。
                    if (text.length > maxLen - 100) {
                        Text("${text.length}/$maxLen", color = SzjMuted, style = SzjMetaStyle)
                    }
                    Spacer(Modifier.weight(1f))
                    // 有图但没文字也不让发：接口要求 content 非空，
                    // 空内容发出去服务端会拒，不如按钮就别亮。
                    val canSend = text.isNotBlank() && !sending && !uploading
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(if (canSend) SzjAccentFill else SzjCardRaised)
                            .clickable(enabled = canSend) {
                                sending = true
                                // 图片 URL 逗号分隔，和站点的 comment_pic 一致。
                                onSend(text.trim(), pics.joinToString(",") { it.second }) { sending = false }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (sending) {
                            CircularProgressIndicator(
                                color = SzjOnAccent, strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp),
                            )
                        } else {
                            ImageGlyph(
                                R.drawable.ic_send_arrow,
                                if (canSend) SzjOnAccent else SzjMuted,
                                Modifier.size(17.dp),
                            )
                        }
                    }
                }
                // 表情面板展开在最下面：键盘之上、发送按钮之下，
                // 收起时完全不占位（不是隐藏一个占着高度的空盒子）。
                if (emojiOpen) {
                    Spacer(Modifier.height(8.dp))
                    SzjEmojiPanel(onPick = { insert(it) })
                }
            }
        }
    }
}

/**
 * 表情面板。石之家的表情就是 `[emoN]` 这种文本占位符，
 * 发出去之后服务端/客户端再渲染成图片（[EMO_BASE] 那一套）。
 *
 * **一共 46 个（emo1~emo46），不是我猜的**：来自移动站自己的全局配置
 * `https://ff14risingstones.web.sdo.com/mob/actConfig.js`，里面
 * `emoarr: [1..46]`，移动站的选择器就是遍历它。我另外实测过
 * emo1/emo23/emo46 都是 200、**emo47 是 404**，确认边界。
 */
private const val SZJ_EMOJI_COUNT = 46

@Composable
private fun SzjEmojiPanel(onPick: (String) -> Unit) {
    // 固定高度 + 自己滚动：面板不能顶走输入框，也不能长到把发送按钮挤出屏幕。
    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
        columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth().height(184.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(SZJ_EMOJI_COUNT, key = { it }) { idx ->
            val n = idx + 1
            SzjPressable(onClick = { onPick("[emo$n]") }, shape = SzjInnerShape) {
                Box(
                    Modifier.aspectRatio(1f).clip(SzjInnerShape),
                    contentAlignment = Alignment.Center,
                ) {
                    ShizhijiaRemoteImage(
                        url = "$SZJ_EMO_BASE$n.png",
                        modifier = Modifier.fillMaxSize().padding(3.dp),
                        contentScale = ContentScale.Fit,
                        showPlaceholder = false,
                        collapseOnFail = false,
                    )
                }
            }
        }
    }
}

/**
 * 输入框顶上的"正在回复"引用条。
 *
 * 左边一道竖条 + 名字 + 内容摘要，两行封顶。竖条是引用的通用记号，
 * 比给整块加底色轻——输入框本身已经是一块浮起的面，再叠个色块就是两层。
 *
 * 内容用纯文本摘要而不是 [ShizhijiaRichContent]：这里要的是"哪一句话"，
 * 把图片和表情也渲染出来会让这条引用比输入框还高。
 */
@Composable
private fun SzjReplyQuote(c: ShizhijiaComment) {
    // 去标签 + 把表情占位符换成"[表情]"，免得引用里出现一串 [emo12][emo3]。
    val summary = remember(c.contentHtml) {
        c.contentHtml
            .replace(Regex("<[^>]+>"), "")
            .replace(Regex("""\[emo\d+]"""), "[表情]")
            .replace(Regex("\\s+"), " ")
            .trim()
            .ifBlank { if (c.commentPic.isNotBlank()) "[图片]" else "" }
    }
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.width(2.dp).heightIn(min = 30.dp).fillMaxHeight().background(SzjAccent))
        Column(Modifier.padding(start = 9.dp)) {
            Text(
                "回复 ${c.characterName.ifBlank { "匿名玩家" }}",
                color = SzjAccent, style = SzjMetaStyle,
                fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (summary.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    summary, color = SzjMuted, style = SzjMetaStyle,
                    lineHeight = 16.sp,
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 输入框下面的效果预览：把正在写的内容按**发出去之后的样子**画一遍。
 *
 * 只在插了表情时出现。纯文字不需要预览——输入框里看到的就是结果，
 * 多一块空白的"预览"是噪音。
 *
 * 用的是渲染帖子/评论的同一个 [ShizhijiaRichContent]，所以预览和最终显示
 * 一定一致；自己另拼一套小图排版，迟早和真正的渲染跑偏。
 */
@Composable
private fun SzjComposerPreview(text: String) {
    val hasEmoji = remember(text) {
        Regex("""\[emo(\d+)]""").find(text)
            ?.groupValues?.get(1)?.toIntOrNull()
            ?.let { it in 1..SZJ_EMOJI_COUNT } == true
    }
    if (!hasEmoji) return
    Spacer(Modifier.height(10.dp))
    Column(
        Modifier.fillMaxWidth().clip(SzjInnerShape).background(SzjCardRaised)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text("发出去的样子", color = SzjMuted, style = SzjMetaStyle)
        Spacer(Modifier.height(4.dp))
        // 把纯文本换行变成段落，交给正文渲染器——表情会变成真图。
        ShizhijiaRichContent(remember(text) { text.replace("\n", "<br>") })
    }
}

@Composable
private fun SzjActionCell(
    icon: Int,
    label: String,
    on: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> SzjMuted.copy(alpha = .45f)
        on -> SzjAccent
        else -> SzjMuted
    }
    SzjPressable(onClick = onClick, shape = SzjInnerShape, modifier = modifier) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 11.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ImageGlyph(icon, tint, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = tint, fontSize = 13.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal)
        }
    }
}

/**
 * 评论区 —— 接在正文后面，**同一条 LazyColumn 里**。
 *
 * 写成 LazyListScope 的扩展而不是一个独立的屏：评论和正文必须在一条流上，
 * 左滑跳过去之后往上滑还要能看到帖子内容。拆成两页会把这个关系弄断。
 *
 * 第一个 item 的下标就是调用方的 commentAnchor（左滑的落点）。
 */
private fun LazyListScope.szjCommentSection(
    comments: List<ShizhijiaComment>,
    commentLoading: Boolean,
    onlyAuthor: Boolean,
    onToggleAuthor: () -> Unit,
    commentOrder: String,
    onOrder: (String) -> Unit,
    nav: (SzjRoute) -> Unit,
    /** 点某条评论的"回复"时调，参数是被回复的那一条。 */
    onReply: (ShizhijiaComment) -> Unit,
) {
    run {
        item(key = "comments-head") {
            // 评论区头部。
            // 原来这一整块（含下面的评论）铺一层灰底 CommentAreaBg，评论卡片再浮在
            // 灰底上——灰套白两层容器，很重。现在灰底去掉，靠标题和发丝线区隔。
            Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)) {
                // 正文和评论之间要有一道明确的界，否则一条流看下来分不清
                // 哪里结束、哪里开始。
                Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("全部评论", color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    // 只看楼主: client-side filter on the loaded comment list.
                    SzjPressable(onClick = onToggleAuthor, shape = SzjChipShape) {
                        Text("只看楼主", color = if (onlyAuthor) SzjOnAccentSoft else SzjMuted, style = SzjMetaStyle,
                            modifier = Modifier.clip(SzjChipShape)
                                .background(if (onlyAuthor) SzjAccentSoft else Color.Transparent)
                                .border(1.dp, if (onlyAuthor) Color.Transparent else SzjLine, SzjChipShape)
                                .padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
                Spacer(Modifier.height(9.dp))
                // 排序独占一行，三档并排——原来挤在标题右边，字小到点不准。
                // 底色改成描边：灰底去掉之后，实色块落在 SzjBg 上反而突兀。
                Row(Modifier.clip(SzjChipShape).border(1.dp, SzjLine, SzjChipShape)) {
                    SzjSmallOption("默认", commentOrder == "earliest") { onOrder("earliest") }
                    SzjSmallOption("热门", commentOrder == "hottest") { onOrder("hottest") }
                    SzjSmallOption("最新", commentOrder == "latest") { onOrder("latest") }
                }
            }
        }
        if (commentLoading && comments.isEmpty()) {
            item(key = "comments-loading") {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                    repeat(2) {
                        SzjCardSurface(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                SzjShimmerBox(Modifier.size(30.dp), RoundedCornerShape(15.dp))
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    SzjShimmerBox(Modifier.fillMaxWidth(0.4f).height(12.dp), RoundedCornerShape(4.dp))
                                    Spacer(Modifier.height(7.dp))
                                    SzjShimmerBox(Modifier.fillMaxWidth(0.9f).height(12.dp), RoundedCornerShape(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        } else if (comments.isEmpty()) {
            item(key = "comments-empty") {
                // 行内空态（固定高度），不是 SzjEmpty——那个是 fillMaxSize，
                // 放在 LazyColumn 的 item 里会把整页吃掉。
                Box(Modifier.fillMaxWidth().padding(vertical = 34.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (onlyAuthor) "楼主还没在这里回帖" else "还没有人评论", color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(6.dp))
                        Text(if (onlyAuthor) "关掉「只看楼主」看全部" else "点底下的「回帖」发第一条", color = SzjMuted, style = SzjMetaStyle)
                    }
                }
            }
        } else {
            itemsIndexed(comments, key = { _, it -> it.id }) { index, c ->
                SzjRise(index) { SzjCommentRow(c, nav, onReply) }
            }
            item(key = "comments-footer") {
                if (commentLoading) Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Comments (rendered inline inside the post detail screen)
// ---------------------------------------------------------------------------

@Composable
private fun SzjSmallOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) SzjAccentSoft else Color.Transparent, tween(200), label = "szjOptBg")
    val fg by animateColorAsState(if (selected) SzjOnAccentSoft else SzjMuted, tween(200), label = "szjOptFg")
    SzjPressable(onClick = onClick, shape = SzjChipShape) {
        Text(label, color = fg, style = SzjMetaStyle,
            modifier = Modifier.clip(SzjChipShape).background(bg).padding(horizontal = 13.dp, vertical = 7.dp))
    }
}

@Composable
private fun SzjCommentRow(
    c: ShizhijiaComment,
    nav: (SzjRoute) -> Unit,
    /**
     * 点"回复"时调。**为 null 表示这一处没有输入框**（比如幻化详情下面的评论列表），
     * 那就不显示回复按钮——显示一个点了没反应的按钮比没有按钮更糟。
     */
    onReply: ((ShizhijiaComment) -> Unit)? = null,
) {
    // 平铺，不是卡片：评论区去掉灰底之后，一条条白卡落在 SzjBg 上还是两层容器。
    // 现在评论之间用 1dp 发丝线分隔，和正文一样直接落在底色上。
    Column(Modifier.fillMaxWidth()) {
      Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(SzjInnerShape).clickable { nav(SzjRoute.UserProfile(c.uuid)) }) {
            SzjAvatar(c.characterName, c.avatar, c.uuid, 30)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.characterName.ifBlank { "匿名玩家" }, color = SzjText, fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    // 作者标记做成实心小标签，比灰字更容易在长评论列里认出楼主。
                    if (c.isPostsAuthor) {
                        Spacer(Modifier.width(6.dp))
                        Text("作者", color = SzjOnAccentSoft, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(SzjAccentSoft).padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                    // 属地挂在名字行右端，不进下面的元信息行（否则挤掉区服）。
                    SzjIpTail(c.ipLocation)
                }
                SzjMetaLine(c.areaName, c.groupName, c.createdAt)
            }
            // 点赞：评论走 posts/like 的 type=2（和帖子同一个端点）。
            // 状态和计数在本地跟着切换接口的返回值走，不重拉整页评论。
            var liked by remember(c.id) { mutableStateOf(c.isLike) }
            var likeNum by remember(c.id) { mutableStateOf(c.likeCount) }
            var busy by remember(c.id) { mutableStateOf(false) }
            val likeScope = rememberCoroutineScope()
            val ctx = LocalContext.current
            SzjPressable(
                shape = SzjChipShape,
                onClick = {
                    if (busy) return@SzjPressable
                    busy = true
                    likeScope.launch {
                        when (val r = ShizhijiaApi.likePost(ctx, c.id, isComment = true)) {
                            is ShizhijiaApi.Res.Ok -> {
                                val on = r.value == ShizhijiaApi.Toggle.On
                                liked = on
                                likeNum = (likeNum + if (on) 1 else -1).coerceAtLeast(0)
                            }
                            else -> szjToastWriteFail(ctx, r, nav)
                        }
                        busy = false
                    }
                },
            ) {
                Row(
                    Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ImageGlyph(
                        if (liked) R.drawable.ic_heart else R.drawable.ic_heart_outline,
                        if (liked) SzjAccent else SzjMuted,
                        Modifier.size(13.dp),
                    )
                    if (likeNum > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(formatCount(likeNum), color = if (liked) SzjAccent else SzjMuted, style = SzjMetaStyle)
                    }
                }
            }
        }
        if (c.contentHtml.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            // Backend emoji ([emoN]) is expanded into a small image by the renderer.
            ShizhijiaRichContent(c.contentHtml)
        }
        if (c.commentPic.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            // Comment pictures render small (capped 200px) at their own ratio,
            // never stretched full-bleed; a failed picture collapses away.
            ShizhijiaRemoteImage(
                url = c.commentPic,
                modifier = Modifier.widthIn(max = 200.dp).heightIn(max = 200.dp).clip(SzjInnerShape),
                contentScale = ContentScale.Fit,
                fitByAspect = true,
                collapseOnFail = true,
                onClick = { SzjViewer.url = it },
            )
        }
        // 回复 + 楼中楼。放在内容下面、分隔线上面：这两个动作都是"针对这条评论"的。
        SzjCommentReplies(c, nav, onReply)
      }
      // 评论之间的分隔线。左侧留出 16dp，和内容对齐而不是顶到屏幕边。
      Box(Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp).background(SzjLine))
    }
}

/**
 * 一条评论下面的"回复"入口和楼中楼。
 *
 * 官网的做法是点开一个对话框显示整楼；这里改成**就地展开**——
 * 手机上再叠一层对话框，看完还得关掉才能回到评论列表，
 * 而楼中楼通常只有几条，就地展开更顺。
 *
 * 只有 `childrenCount > 0` 才发请求（官网也是这么判的），而且**展开时才拉**，
 * 不是进页面就把每一楼的子评论都拉一遍。
 */
@Composable
private fun SzjCommentReplies(
    c: ShizhijiaComment,
    nav: (SzjRoute) -> Unit,
    onReply: ((ShizhijiaComment) -> Unit)?,
) {
    // 既不能回复、也没有子评论，这一行就整个不画。
    if (onReply == null && c.childrenCount <= 0) return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by remember(c.id) { mutableStateOf(false) }
    var subs by remember(c.id) { mutableStateOf<List<ShizhijiaComment>?>(null) }
    var loading by remember(c.id) { mutableStateOf(false) }
    var status by remember(c.id) { mutableStateOf<ShizhijiaApi.Res<List<ShizhijiaComment>>?>(null) }
    // 已经拉到第几页。接口 limit 是 10（照官网），超过 10 条回复的楼
    // 不给「加载更多」的话第 11 条起就永远看不到，界面上也没有任何提示。
    var page by remember(c.id) { mutableStateOf(1) }
    var appending by remember(c.id) { mutableStateOf(false) }
    // 续页失败要单独存：`status` 只在列表为空那个分支渲染，
    // 而续页失败时列表非空，写进 status 等于把错误咽掉了。
    var appendError by remember(c.id) { mutableStateOf("") }
    // 「真的没有了」和「这次没拉到」要分开：前者该把按钮收掉，
    // 后者必须留着按钮让人重试 —— 一次网络抖动不该让剩下的回复永久看不到。
    var exhausted by remember(c.id) { mutableStateOf(false) }

    Spacer(Modifier.height(6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 回复这一条。传的是这一楼的 rootParent 和被回复评论的 id。
        if (onReply != null) {
            SzjPressable(onClick = { onReply(c) }, shape = SzjChipShape) {
                Row(
                    Modifier.clip(SzjChipShape).padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ImageGlyph(R.drawable.ic_comment, SzjMuted, Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("回复", color = SzjMuted, style = SzjMetaStyle)
                }
            }
        }
        if (c.childrenCount > 0) {
            Spacer(Modifier.width(4.dp))
            SzjPressable(
                onClick = {
                    expanded = !expanded
                    if (expanded && subs == null && !loading) {
                        loading = true
                        scope.launch {
                            val r = ShizhijiaApi.getSubComments(context, c.rootParent)
                            status = r
                            subs = (r as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
                            // 重新从第一页拉了，页码跟着回到 1，
                            // 否则续页会从旧的页码接着走、跳过中间几页。
                            page = 1
                            appendError = ""
                            exhausted = false
                            loading = false
                        }
                    }
                },
                shape = SzjChipShape,
            ) {
                Row(
                    Modifier.clip(SzjChipShape).padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (expanded) "收起回复" else "${c.childrenCount} 条回复",
                        color = SzjAccent, style = SzjMetaStyle,
                    )
                    Spacer(Modifier.width(3.dp))
                    ImageGlyph(
                        if (expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down,
                        SzjAccent, Modifier.size(12.dp),
                    )
                }
            }
        }
    }
    if (!expanded) return
    // 楼中楼整块左缩进 + 一道竖线：靠缩进表达层级，不再给每条子评论套卡片
    // （评论本身已经是平铺的，再套一层就是三层容器）。
    Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Box(Modifier.padding(start = 4.dp).width(2.dp).fillMaxHeight().background(SzjLine))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            val list = subs
            when {
                loading && list == null -> Row(
                    Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("正在读取回复", color = SzjMuted, style = SzjMetaStyle)
                }
                list.isNullOrEmpty() -> Text(
                    // 计数说有回复但拉不到，多半是被删了或者要登录。
                    when (status) {
                        is ShizhijiaApi.Res.NeedLogin -> "登录后能看这些回复"
                        is ShizhijiaApi.Res.Failed -> (status as ShizhijiaApi.Res.Failed).msg.ifBlank { "没读取到回复" }
                        else -> "这些回复已经不在了"
                    },
                    color = SzjMuted, style = SzjMetaStyle,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                else -> {
                    list.forEachIndexed { i, s ->
                        if (i > 0) Spacer(Modifier.height(10.dp))
                        SzjSubCommentRow(s, nav, onReply)
                    }
                    // 还有没拉到的：判据用 childrenCount 而不是「上一页满 10 条」——
                    // 后者在恰好 10 条时会多发一次空请求，而且那次返回空之后
                    // 按钮该消失还是该留着没有依据。计数是服务端给的，直接用。
                    if (list.size < c.childrenCount && !exhausted) {
                        Spacer(Modifier.height(8.dp))
                        SzjPressable(
                            onClick = {
                                if (appending) return@SzjPressable
                                appending = true
                                appendError = ""
                                scope.launch {
                                    val next = page + 1
                                    val r = ShizhijiaApi.getSubComments(
                                        context, c.rootParent, page = next,
                                    )
                                    when (r) {
                                        is ShizhijiaApi.Res.Ok -> {
                                            // 去重：翻页期间有人删回复会让分页错位，
                                            // 同一条可能在两页里都出现。
                                            val have = list.mapTo(mutableSetOf()) { it.id }
                                            val add = r.value.filter { it.id !in have }
                                            subs = list + add
                                            page = next
                                            // 计数说还有、但这一页一条新的都没有：
                                            // 多半是回复被删了而计数没跟着降。
                                            // 不说一声的话按钮会一直亮着点不出东西。
                                            if (add.isEmpty()) {
                                                appendError = "没有更多了"
                                                exhausted = true
                                            }
                                        }
                                        is ShizhijiaApi.Res.NeedLogin -> appendError = "登录后能看更多回复"
                                        is ShizhijiaApi.Res.Failed -> appendError = r.msg.ifBlank { "没读取到更多回复" }
                                        else -> appendError = "没读取到更多回复"
                                    }
                                    appending = false
                                }
                            },
                            shape = SzjChipShape,
                        ) {
                            Row(
                                Modifier.clip(SzjChipShape).padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (appending) {
                                    CircularProgressIndicator(
                                        color = SzjAccent, strokeWidth = 1.5.dp,
                                        modifier = Modifier.size(11.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    // 说清还剩几条，比光写「加载更多」更有用。
                                    "还有 ${c.childrenCount - list.size} 条回复",
                                    color = SzjAccent, style = SzjMetaStyle,
                                )
                            }
                        }
                    }
                    if (appendError.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(appendError, color = SzjMuted, style = SzjMetaStyle)
                    }
                }
            }
        }
    }
}

/**
 * 楼中楼里的一条。比顶层评论轻：头像 22dp、没有点赞按钮、没有分隔线。
 *
 * "回复 @某人"只在 `parentId != rootParent` 时显示（[ShizhijiaComment.showReplyTo]），
 * 判据和官网一致——直接回复楼主那条不显示，因为紧挨着上面就是被回复的内容。
 */
@Composable
private fun SzjSubCommentRow(
    c: ShizhijiaComment,
    nav: (SzjRoute) -> Unit,
    onReply: ((ShizhijiaComment) -> Unit)?,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SzjAvatar(c.characterName, c.avatar, c.uuid, 22)
            Spacer(Modifier.width(7.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        c.characterName.ifBlank { "匿名玩家" }, color = SzjText, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                            .clip(SzjChipShape).clickable { nav(SzjRoute.UserProfile(c.uuid)) },
                    )
                    if (c.isPostsAuthor) {
                        Spacer(Modifier.width(5.dp))
                        Text(
                            "作者", color = SzjOnAccentSoft, fontSize = 9.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(SzjAccentSoft)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                    SzjIpTail(c.ipLocation)
                }
                if (c.showReplyTo) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("回复 ", color = SzjMuted, style = SzjMetaStyle)
                        Text(
                            "@${c.toCname}", color = SzjAccent, style = SzjMetaStyle,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clip(SzjChipShape)
                                .clickable(enabled = c.toUuid.isNotBlank()) { nav(SzjRoute.UserProfile(c.toUuid)) },
                        )
                    }
                }
            }
            if (onReply != null) {
                SzjPressable(onClick = { onReply(c) }, shape = SzjChipShape) {
                    Text(
                        "回复", color = SzjMuted, style = SzjMetaStyle,
                        modifier = Modifier.clip(SzjChipShape).padding(horizontal = 7.dp, vertical = 4.dp),
                    )
                }
            }
        }
        if (c.contentHtml.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            ShizhijiaRichContent(c.contentHtml, modifier = Modifier.padding(start = 29.dp))
        }
        if (c.commentPic.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            ShizhijiaRemoteImage(
                url = c.commentPic,
                modifier = Modifier.padding(start = 29.dp)
                    .widthIn(max = 160.dp).heightIn(max = 160.dp).clip(SzjInnerShape),
                contentScale = ContentScale.Fit,
                fitByAspect = true,
                collapseOnFail = true,
                onClick = { SzjViewer.url = it },
            )
        }
        Row(Modifier.padding(start = 29.dp, top = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            SzjMetaLine(c.areaName, c.groupName, c.createdAt)
        }
    }
}

// ---------------------------------------------------------------------------
// 发幻化
// ---------------------------------------------------------------------------

/**
 * 发幻化。
 *
 * **这一版不含"挑装备"**，说清楚为什么：`createGlamour` 的 `equipments` 里
 * `equipment_id: -1` 就是"这个槽空着"，所以一件不选也能发——那是
 * "只发外观图 + 标题 + 种族/职业标签"的幻化，本身是站点上最常见的形态。
 * 挑装备要一个 5 万件物品的选择器（wiki 那套本地库正好有），
 * 那一块单独做，做好了接进来就行，接口这边已经留好了 [ShizhijiaApi.GlamourSlotPick]。
 *
 * 先能发、再能发得细——半个装备选择器发出去一套错的搭配，比没有更糟。
 */
@Composable
private fun ShizhijiaPublishGlamourScreen(
    pop: () -> Unit,
    nav: (SzjRoute) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    // **封面和细节图分开两个槽**，照官网的表单：封面 1 张（`main_image`，必填），
    // 细节图最多 2 张（`images`），都进详情页的图片区。
    //
    // 原来是一个 9 格的多选、约定"第一张算封面"。那样封面是个隐式规则：
    // 想换封面得先删掉前面几张再按顺序重传，而且删掉第一张时封面会**静默改变**。
    // 官网把封面单独摆一个大格子就是为了避免这件事。
    val cover = remember { mutableStateOf<Pair<android.graphics.Bitmap?, String>?>(null) }
    val details = remember { mutableStateListOf<Pair<android.graphics.Bitmap?, String>>() }
    // 分享到动态。官网这个表单底部有这个开关，默认关。
    var shareToDynamic by remember { mutableStateOf(false) }
    // 正在传的是封面还是细节图 —— 一个 picker 两个用途，得记住回调该写哪儿。
    var pickingCover by remember { mutableStateOf(true) }
    var uploading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    val races = remember { mutableStateListOf<Int>() }
    val genders = remember { mutableStateListOf<Int>() }
    // slot → 选了什么。服务端要求至少一件（实测 10003），发布时按
    // SZJ_GLAMOUR_SLOTS 的顺序拼，没选的槽补 equipment_id = -1。
    val slotPicks = remember { mutableStateMapOf<String, GlamourPick>() }
    // 弹层的开关状态提到这一层，弹层本体画在 LazyColumn 之外（见 SzjGlamourPickerHost）。
    val pickerTarget = remember { GlamourPickerTarget() }
    // 细节图上限。官网是封面 1 + 细节 2。
    val maxDetails = 2

    // 种族/性别字典：站点的 race_ids 是 1..8，gender 1 男 2 女。
    val raceNames = remember {
        listOf(
            1 to "人族", 2 to "精灵族", 3 to "拉拉菲尔族", 4 to "猫魅族",
            5 to "鲁加族", 6 to "敖龙族", 7 to "硌狮族", 8 to "维埃拉族",
        )
    }

    val picker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            when (val r = ShizhijiaCosUpload.upload(context, uri, channel = "glamour")) {
                is ShizhijiaApi.Res.Ok -> {
                    val thumb = runCatching {
                        context.contentResolver.openInputStream(uri)?.use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        }
                    }.getOrNull()
                    if (pickingCover) cover.value = thumb to r.value
                    else if (details.size < maxDetails) details.add(thumb to r.value)
                }
                else -> szjToastWriteFail(context, r, nav)
            }
            uploading = false
        }
    }

    // 两个槽共用一个 picker：记下这次点的是哪个槽再拉起相册。
    val launchPick: (Boolean) -> Unit = { forCover ->
        pickingCover = forCover
        picker.launch(
            androidx.activity.result.PickVisualMediaRequest(
                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
            )
        )
    }

    // 外面这层 Box 是给选择器弹层用的：弹层必须和 ScreenFrame 平级，
    // 不能在 LazyColumn 的 item 里（那样 fillMaxSize() 量到的是 item 尺寸，
    // 弹层就长在滚动内容底部，要往下滑才看得见）。见 GlamourPickerTarget。
    Box(Modifier.fillMaxSize()) {
    ScreenFrame(background = SzjBg) {
        SzjHeader("发幻化", onBack = pop)
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
        ) {
            item(key = "pics") {
                Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("封面图", color = SzjText, style = SzjLabelStyle)
                        Text(" *", color = SzjAccent, style = SzjLabelStyle)
                        Spacer(Modifier.width(6.dp))
                        Text("详情页最上面那张", color = SzjMuted, style = SzjMetaStyle)
                    }
                    Spacer(Modifier.height(8.dp))
                    // 封面单独一个大格子。幻化图是竖的，给 3:4 而不是正方形，
                    // 缩略图的构图和详情页看到的一致。
                    SzjPressable(
                        onClick = { if (!uploading && !sending) launchPick(true) },
                        shape = SzjInnerShape,
                        enabled = !uploading && !sending,
                    ) {
                        Box(
                            Modifier.width(126.dp).height(168.dp).clip(SzjInnerShape)
                                .background(SzjCardRaised)
                                .border(1.dp, SzjLine, SzjInnerShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            val c = cover.value
                            val thumb = c?.first
                            if (thumb != null) {
                                Image(
                                    thumb.asImageBitmap(), contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().clip(SzjInnerShape),
                                )
                            } else if (uploading && pickingCover) {
                                CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    ImageGlyph(R.drawable.ic_add, SzjMuted, Modifier.size(22.dp))
                                    Spacer(Modifier.height(5.dp))
                                    Text("选封面", color = SzjMuted, style = SzjMetaStyle)
                                }
                            }
                            if (c != null) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(4.dp)
                                        .size(20.dp).clip(CircleShape)
                                        .background(Color.Black.copy(alpha = .55f))
                                        .clickable { cover.value = null },
                                    contentAlignment = Alignment.Center,
                                ) { ImageGlyph(R.drawable.ic_close, Color.White, Modifier.size(11.dp)) }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("细节图", color = SzjText, style = SzjLabelStyle)
                        Spacer(Modifier.width(6.dp))
                        Text("最多 $maxDetails 张，可留空", color = SzjMuted, style = SzjMetaStyle)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        details.forEachIndexed { i, p ->
                            Box {
                                val thumb = p.first
                                if (thumb != null) {
                                    Image(
                                        thumb.asImageBitmap(), contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(84.dp).clip(SzjInnerShape),
                                    )
                                } else {
                                    Box(Modifier.size(84.dp).clip(SzjInnerShape).background(SzjCardRaised))
                                }
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(3.dp)
                                        .size(18.dp).clip(CircleShape)
                                        .background(Color.Black.copy(alpha = .55f))
                                        .clickable { details.removeAt(i) },
                                    contentAlignment = Alignment.Center,
                                ) { ImageGlyph(R.drawable.ic_close, Color.White, Modifier.size(10.dp)) }
                            }
                        }
                        if (details.size < maxDetails) {
                            SzjPressable(
                                onClick = { if (!uploading && !sending) launchPick(false) },
                                shape = SzjInnerShape,
                                enabled = !uploading && !sending,
                            ) {
                                Box(
                                    Modifier.size(84.dp).clip(SzjInnerShape)
                                        .background(SzjCardRaised)
                                        .border(1.dp, SzjLine, SzjInnerShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (uploading && !pickingCover) {
                                        CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    } else {
                                        ImageGlyph(R.drawable.ic_add, SzjMuted, Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "title") {
                SzjFormField(
                    label = "标题", value = title, placeholder = "给这套搭配起个名字",
                    required = true, maxLen = 40, onChange = { title = it },
                )
            }
            item(key = "desc") {
                SzjFormField(
                    label = "说明", value = desc, placeholder = "灵感、场景、搭配思路（可留空）",
                    lines = 4, maxLen = 500, onChange = { desc = it },
                )
            }
            item(key = "race") {
                Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("适用种族", color = SzjText, style = SzjLabelStyle)
                        // 服务端必填（实测回 10003 种族必填）。
                        Text(" *", color = SzjAccent, style = SzjLabelStyle)
                    }
                    Spacer(Modifier.height(7.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        raceNames.chunked(4).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                row.forEach { (id, name) ->
                                    SzjPartChip(name, races.contains(id)) {
                                        if (races.contains(id)) races.remove(id) else races.add(id)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "gender") {
                Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("适用性别", color = SzjText, style = SzjLabelStyle)
                        // 服务端必填（实测不带就回 10003 性别必填）。
                        Text(" *", color = SzjAccent, style = SzjLabelStyle)
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf(1 to "男性", 2 to "女性").forEach { (id, name) ->
                            SzjPartChip(name, genders.contains(id)) {
                                if (genders.contains(id)) genders.remove(id) else genders.add(id)
                            }
                        }
                    }
                }
            }
            item(key = "gear") {
                SzjGlamourSlotSection(picks = slotPicks, target = pickerTarget) { slot, pick ->
                    if (pick == null) slotPicks.remove(slot) else slotPicks[slot] = pick
                }
            }
            item(key = "share") {
                // 官网这个表单底部有这个开关（`is_share`），默认关。
                // **和发帖那个不一样**：幻化本身就是公开的（`scope` 官网写死 "1"），
                // 所以这里不跟一组可见范围单选 —— 那三档只对动态有意义，
                // 而幻化的动态可见范围站点没给选。
                Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    SzjPressable(onClick = { shareToDynamic = !shareToDynamic }, shape = SzjInnerShape) {
                        Row(
                            Modifier.fillMaxWidth().clip(SzjInnerShape)
                                .background(if (shareToDynamic) SzjAccentSoft else SzjCardRaised)
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "同时分享到动态",
                                    color = if (shareToDynamic) SzjOnAccentSoft else SzjText,
                                    style = SzjLabelStyle,
                                )
                                Text(
                                    "在你的动态里也发一条，指向这套幻化",
                                    color = if (shareToDynamic) SzjOnAccentSoft.copy(alpha = .75f) else SzjMuted,
                                    style = SzjMetaStyle,
                                )
                            }
                            ImageGlyph(
                                if (shareToDynamic) R.drawable.ic_check_small else R.drawable.ic_radio_off,
                                if (shareToDynamic) SzjOnAccentSoft else SzjMuted,
                                Modifier.size(17.dp),
                            )
                        }
                    }
                }
            }
        }
        // 发布按钮钉在底部，不进滚动区（和发帖那屏同一个处理）。
        Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            // 四项都是服务端必填，缺一样就回 10003。按钮亮着、点了才失败
            // 是最糟的状态——人会以为自己填错了，反复改标题改标签试。
            // 所以条件不满足就常灰，下面写明还缺什么。
            //
            // `slotPicks.isNotEmpty()` 对应「至少需要上传一件有效装备」，
            // 这条是真机实测出来的，不是推断（原来以为全空槽也能发）。
            val canSend = title.isNotBlank() && cover.value != null &&
                races.isNotEmpty() && genders.isNotEmpty() &&
                slotPicks.isNotEmpty() && !sending && !uploading
            // 缺什么直接说，别让人自己对照星号找。
            val missing = buildList {
                if (cover.value == null) add("封面图")
                if (title.isBlank()) add("标题")
                if (races.isEmpty()) add("种族")
                if (genders.isEmpty()) add("性别")
                if (slotPicks.isEmpty()) add("至少一件装备")
            }
            if (missing.isNotEmpty()) {
                Text(
                    "还差：${missing.joinToString("、")}",
                    color = SzjMuted, style = SzjMetaStyle,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            SzjPressable(
                onClick = {
                    sending = true
                    scope.launch {
                        val r = ShizhijiaApi.publishGlamour(
                            context,
                            title = title.trim(),
                            desc = desc.trim(),
                            mainImage = cover.value?.second.orEmpty(),
                            images = details.joinToString(",") { it.second },
                            raceIds = races.toList(),
                            genderIds = genders.toList(),
                            share = shareToDynamic,
                            // 按 SZJ_GLAMOUR_SLOTS 的顺序拼，没选的槽补 -1。
                            // 顺序由 API 那边负责，这里只给选了的。
                            slots = slotPicks.map { (slot, pick) ->
                                ShizhijiaApi.GlamourSlotPick(
                                    slot = slot,
                                    equipmentId = pick.item.id.toLong(),
                                    // 按孔位、空孔补 -1、截到孔数 —— 照站点
                                    // PublishGlamour 的写法，见 GlamourPick.dyeIdsForSubmit。
                                    dyeIds = pick.dyeIdsForSubmit(),
                                )
                            },
                        )
                        when (r) {
                            is ShizhijiaApi.Res.Ok -> {
                                android.widget.Toast.makeText(context, "发布成功", android.widget.Toast.LENGTH_SHORT).show()
                                if (r.value.isNotBlank()) nav(SzjRoute.GlamourDetail(r.value)) else pop()
                            }
                            else -> szjToastWriteFail(context, r, nav)
                        }
                        sending = false
                    }
                },
                shape = SzjInnerShape,
                enabled = canSend,
            ) {
                Box(
                    Modifier.fillMaxWidth().clip(SzjInnerShape)
                        .background(if (canSend) SzjAccentFill else SzjCardRaised)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (sending) {
                        CircularProgressIndicator(color = SzjOnAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            "发布",
                            color = if (canSend) SzjOnAccent else SzjMuted,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
    // 屏幕层：从屏幕底部升起，盖住整屏（含头部和发布按钮）。
    SzjGlamourPickerHost(picks = slotPicks, target = pickerTarget) { slot, pick ->
        if (pick == null) slotPicks.remove(slot) else slotPicks[slot] = pick
    }
    }
}

// ---------------------------------------------------------------------------
// 发动态
// ---------------------------------------------------------------------------

/**
 * 发动态。**做成从底部升起的一层，不是整页**——动态就是一段话加几张图，
 * 和发帖（标题、版块、长正文）不是一个量级，给它一个整页会显得空。
 * 形态照评论输入框那一套（同一个手感）。
 *
 * **这是"只给自己看"唯一真正生效的地方。** 发到版块的帖子永远公开，
 * 动态的 `scope` 才是无条件生效的可见范围（见 [ShizhijiaApi.publishDynamic]）。
 * 所以这里的可见范围**不藏在开关后面**，直接摆出来，而且默认就显示当前选择。
 */
@Composable
private fun SzjDynamicComposer(
    onDismiss: () -> Unit,
    onPublished: () -> Unit,
    nav: (SzjRoute) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var field by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    val text = field.text
    var sending by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    var emojiOpen by remember { mutableStateOf(false) }
    var visibility by remember { mutableStateOf(ShizhijiaApi.PostScope.Public) }
    val pics = remember { mutableStateListOf<Pair<android.graphics.Bitmap?, String>>() }
    val maxPics = 9
    val maxLen = 1000
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val insert: (String) -> Unit = { s ->
        val t = field.text
        val start = field.selection.start.coerceIn(0, t.length)
        val end = field.selection.end.coerceIn(start, t.length)
        val next = t.substring(0, start) + s + t.substring(end)
        if (next.length <= maxLen) {
            field = androidx.compose.ui.text.input.TextFieldValue(
                next,
                androidx.compose.ui.text.TextRange(start + s.length),
            )
        }
    }

    val picker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            // channel 用 dynamic：站点各处按用途传，传错文件会落到别的目录，
            // 有可能过不了后端对图片 URL 的正则校验。
            when (val r = ShizhijiaCosUpload.upload(context, uri, channel = "dynamic")) {
                is ShizhijiaApi.Res.Ok -> {
                    val thumb = runCatching {
                        context.contentResolver.openInputStream(uri)?.use {
                            android.graphics.BitmapFactory.decodeStream(it)
                        }
                    }.getOrNull()
                    pics.add(thumb to r.value)
                }
                else -> szjToastWriteFail(context, r, nav)
            }
            uploading = false
        }
    }

    // **不用 Dialog。** 原来这一层是 `androidx.compose.ui.window.Dialog`，
    // 而 Dialog 是**独立的窗口**：MainActivity 那个窗口的 `adjustResize` +
    // `setDecorFitsSystemWindows(false)`（聊天会话靠它正常工作）对它一概无效。
    //
    // 我原来加了 `SzjDialogImeFix()` 去补：给 Dialog 的 window 同时设
    // `setDecorFitsSystemWindows(false)` 和 `SOFT_INPUT_ADJUST_RESIZE`。
    // **那两个是互相矛盾的一对** —— 前者是"我自己处理 inset，别 resize 我"，
    // 后者是"请 resize 我"。结果 Dialog 窗口既不 resize、`WindowInsets.ime`
    // 也拿不到有效值，下面那句 `windowInsetsPadding(navigationBars.union(ime))`
    // 等于加了 0。用户报的就是这个："导航栏被顶上去了（那是 Activity 窗口里的
    // 底栏），而输入框还是没有任何反应，完全被软键盘覆盖住了"。
    //
    // 现在改成 Activity 窗口内的一层浮层。调用点在社区屏的 `ScreenFrame` 里面，
    // 而 `ScreenFrame` 的内层 Column 已经带 `imePadding()` —— **那是全项目
    // 唯一确认能被键盘顶起来的形状**。所以这一层不再自己让 ime，
    // 交给外面统一让，它只负责贴在可用区底部。
    //
    // Dialog 顺带提供的两件事要自己接回来：返回键（BackHandler）、
    // 以及"盖住一切"（靠**组合顺序**，见调用点的注释：必须排在底栏之后）。
    BackHandler(enabled = !sending) { onDismiss() }
    run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = .32f))
                    .clickable(enabled = !sending) { onDismiss() },
            )
            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(SzjCard)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text("发动态", color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                BasicTextField(
                    value = field,
                    onValueChange = { if (it.text.length <= maxLen) field = it },
                    textStyle = TextStyle(color = SzjText, fontSize = 15.sp, lineHeight = 23.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(SzjAccent),
                    enabled = !sending,
                    decorationBox = { inner ->
                        Box(Modifier.fillMaxWidth().heightIn(min = 88.dp)) {
                            if (text.isEmpty()) {
                                Text("这一刻想说什么…", color = SzjMuted, fontSize = 15.sp)
                            }
                            inner()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
                SzjComposerPreview(text)
                if (pics.isNotEmpty() || uploading) {
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(pics.size, key = { pics[it].second }) { i ->
                            Box {
                                val thumb = pics[i].first
                                if (thumb != null) {
                                    Image(
                                        thumb.asImageBitmap(), contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(62.dp).clip(SzjInnerShape),
                                    )
                                } else {
                                    Box(Modifier.size(62.dp).clip(SzjInnerShape).background(SzjCardRaised))
                                }
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(3.dp)
                                        .size(18.dp).clip(CircleShape)
                                        .background(Color.Black.copy(alpha = .55f))
                                        .clickable { pics.removeAt(i) },
                                    contentAlignment = Alignment.Center,
                                ) { ImageGlyph(R.drawable.ic_close, Color.White, Modifier.size(10.dp)) }
                            }
                        }
                        if (uploading) {
                            item(key = "up") {
                                Box(
                                    Modifier.size(62.dp).clip(SzjInnerShape).background(SzjCardRaised),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
                // 可见范围。**摆在明面上不藏进开关**：这是全 App 唯一能"只给自己看"
                // 的地方，而发帖那边我曾经把它做错过（帖子其实永远公开）。
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImageGlyph(R.drawable.ic_eye, SzjMuted, Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShizhijiaApi.PostScope.entries.forEach { s ->
                            SzjPartChip(s.label, visibility == s) { visibility = s }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val canAddPic = pics.size < maxPics && !uploading && !sending
                    SzjPressable(
                        onClick = {
                            if (pics.size >= maxPics) {
                                android.widget.Toast.makeText(context, "最多 $maxPics 张", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                picker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    )
                                )
                            }
                        },
                        shape = CircleShape,
                        enabled = canAddPic,
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape).background(SzjCardRaised),
                            contentAlignment = Alignment.Center,
                        ) {
                            ImageGlyph(R.drawable.ic_add, if (canAddPic) SzjMuted else SzjLine, Modifier.size(15.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    SzjPressable(onClick = { emojiOpen = !emojiOpen }, shape = CircleShape) {
                        Box(
                            Modifier.size(34.dp).clip(CircleShape)
                                .background(if (emojiOpen) SzjAccentSoft else SzjCardRaised),
                            contentAlignment = Alignment.Center,
                        ) {
                            ImageGlyph(
                                R.drawable.ic_emoji,
                                if (emojiOpen) SzjOnAccentSoft else SzjMuted,
                                Modifier.size(17.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    if (text.length > maxLen - 100) {
                        Text("${text.length}/$maxLen", color = SzjMuted, style = SzjMetaStyle)
                    }
                    Spacer(Modifier.weight(1f))
                    // **内容或图片有一个就能发**（官网就是这个判据），
                    // 所以允许只发图——和评论要求内容非空不同。
                    val canSend = (text.isNotBlank() || pics.isNotEmpty()) && !sending && !uploading
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(if (canSend) SzjAccentFill else SzjCardRaised)
                            .clickable(enabled = canSend) {
                                sending = true
                                scope.launch {
                                    val r = ShizhijiaApi.publishDynamic(
                                        context,
                                        content = szjComposeHtml(text.trim(), emptyList()),
                                        pics = pics.joinToString(",") { it.second },
                                        scope = visibility,
                                    )
                                    when (r) {
                                        is ShizhijiaApi.Res.Ok -> {
                                            android.widget.Toast.makeText(context, r.value, android.widget.Toast.LENGTH_SHORT).show()
                                            onPublished()
                                        }
                                        else -> szjToastWriteFail(context, r, nav)
                                    }
                                    sending = false
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (sending) {
                            CircularProgressIndicator(color = SzjOnAccent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            ImageGlyph(
                                R.drawable.ic_send_arrow,
                                if (canSend) SzjOnAccent else SzjMuted,
                                Modifier.size(17.dp),
                            )
                        }
                    }
                }
                if (emojiOpen) {
                    Spacer(Modifier.height(8.dp))
                    SzjEmojiPanel(onPick = { insert(it) })
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 发帖
// ---------------------------------------------------------------------------

/**
 * 发帖 / 发攻略。两者接口同构，差别只是 `type`（1 帖子 / 2 攻略），
 * 版块字典也各自一份，所以用同一个界面。
 *
 * **正文是 HTML**（官网那边是富文本编辑器）。这里不做富文本工具栏——
 * 加粗斜体这些在手机上打字时用不到，而且要维护一套自己的编辑器状态。
 * 做的是：纯文本按空行分段包成 `<p>`，图片和表情按插入顺序嵌进去。
 * 这样发出去的正文在官网和我们自己的渲染器里都是正常的。
 */
@Composable
@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
private fun ShizhijiaPublishPostScreen(
    isStrategy: Boolean,
    pop: () -> Unit,
    nav: (SzjRoute) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue("")) }
    var parts by remember { mutableStateOf<List<ShizhijiaPostPart>>(emptyList()) }
    var partId by remember { mutableStateOf("") }
    var visibility by remember { mutableStateOf(ShizhijiaApi.PostScope.Public) }
    // 分享到动态。官网默认关（is_share 的初值是 0），保持一致。
    // scope 只在这个打开时才有意义——它管那条动态，不管帖子。
    var shareToDynamic by remember { mutableStateOf(false) }
    var emojiOpen by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var uploading by remember { mutableStateOf(false) }
    // 已上传的图片 URL，按插入顺序。发的时候拼到正文末尾。
    val pics = remember { mutableStateListOf<String>() }
    val maxPics = 9
    // 正文框的"滚进视野"请求器。**键盘弹起来时要手动触发一次** ——
    // 聚焦在弹键盘之前发生（点输入框才弹键盘），那一刻视口还是全高，
    // Compose 自带的 bringIntoView 已经跑完；等视口真的矮下来，
    // 没有任何事件再触发一次滚动。
    val bodyBring = remember { BringIntoViewRequester() }
    var bodyFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isStrategy) {
        parts = if (isStrategy) ShizhijiaApi.getStrategyParts(context) else ShizhijiaApi.getPostParts(context)
        // 只有叶子版块能发（父版块是分组）。默认选第一个能选的。
        val selectable = parts.filter { it.parentId.isNotBlank() && it.parentId != "0" }.ifEmpty { parts }
        if (partId.isBlank()) partId = selectable.firstOrNull()?.id.orEmpty()
    }

    val insert: (String) -> Unit = { s ->
        val t = body.text
        val start = body.selection.start.coerceIn(0, t.length)
        val end = body.selection.end.coerceIn(start, t.length)
        val next = t.substring(0, start) + s + t.substring(end)
        body = androidx.compose.ui.text.input.TextFieldValue(
            text = next,
            selection = androidx.compose.ui.text.TextRange(start + s.length),
        )
    }

    val picker = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        uploading = true
        scope.launch {
            when (val r = ShizhijiaCosUpload.upload(context, uri, channel = "posts")) {
                is ShizhijiaApi.Res.Ok -> pics.add(r.value)
                else -> szjToastWriteFail(context, r, nav)
            }
            uploading = false
        }
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader(if (isStrategy) "发攻略" else "发帖", onBack = pop)
        val selectable = parts.filter { it.parentId.isNotBlank() && it.parentId != "0" }.ifEmpty { parts }
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
        ) {
            item(key = "title") {
                SzjFormField(
                    label = "标题", value = title, placeholder = "一句话说清这帖讲什么",
                    required = true, maxLen = 60, onChange = { title = it },
                )
            }
            item(key = "part") {
                Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("版块", color = SzjText, style = SzjLabelStyle)
                        Text(" *", color = SzjAccent, style = SzjLabelStyle)
                    }
                    Spacer(Modifier.height(7.dp))
                    if (selectable.isEmpty()) {
                        Text("正在读取版块", color = SzjMuted, style = SzjMetaStyle)
                    } else {
                        // 换行不横滑：横滑会藏住后面还有多少个版块。
                        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            selectable.chunked(3).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                    row.forEach { p ->
                                        SzjPartChip(p.name, partId == p.id) { partId = p.id }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item(key = "body") {
                Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("正文", color = SzjText, style = SzjLabelStyle)
                        Text(" *", color = SzjAccent, style = SzjLabelStyle)
                    }
                    Spacer(Modifier.height(7.dp))
                    BasicTextField(
                        value = body,
                        onValueChange = { body = it },
                        minLines = 8,
                        textStyle = TextStyle(color = SzjText, fontSize = 14.sp, lineHeight = 22.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(SzjAccent),
                        decorationBox = { inner ->
                            Box(
                                Modifier.fillMaxWidth().clip(SzjInnerShape).background(SzjCard)
                                    .border(1.dp, SzjHairline, SzjInnerShape)
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                            ) {
                                if (body.text.isEmpty()) {
                                    Text("正文。空一行分段", color = SzjMuted, fontSize = 14.sp)
                                }
                                inner()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                            // 键盘弹起来时把这个框滚进视野。见下面 imeVisible 那段
                            // LaunchedEffect —— 光靠 imePadding 不够：
                            // LazyColumn 只是视口变矮，里面的内容不会自己滚。
                            .bringIntoViewRequester(bodyBring)
                            .onFocusChanged { bodyFocused = it.isFocused },
                    )
                    SzjComposerPreview(body.text)
                }
            }
            if (pics.isNotEmpty() || uploading) {
                item(key = "pics") {
                    Column(Modifier.fillMaxWidth().padding(bottom = 14.dp)) {
                        Text("图片（会按顺序放在正文后面）", color = SzjMuted, style = SzjMetaStyle)
                        Spacer(Modifier.height(7.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(pics.size, key = { pics[it] }) { i ->
                                Box {
                                    ShizhijiaRemoteImage(
                                        url = pics[i],
                                        modifier = Modifier.size(72.dp).clip(SzjInnerShape),
                                        contentScale = ContentScale.Crop,
                                        showPlaceholder = false,
                                        collapseOnFail = false,
                                    )
                                    Box(
                                        Modifier.align(Alignment.TopEnd).padding(3.dp)
                                            .size(18.dp).clip(CircleShape)
                                            .background(Color.Black.copy(alpha = .55f))
                                            .clickable { pics.removeAt(i) },
                                        contentAlignment = Alignment.Center,
                                    ) { ImageGlyph(R.drawable.ic_close, Color.White, Modifier.size(10.dp)) }
                                }
                            }
                            if (uploading) {
                                item(key = "up") {
                                    Box(
                                        Modifier.size(72.dp).clip(SzjInnerShape).background(SzjCardRaised),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // **工具行 / 可见范围 / 发布按钮固定在底部，不在滚动区里。**
        //
        // 原来它们是 LazyColumn 的 item：键盘一弹起来视口变矮，这几项就落到
        // 折叠线以下——而人正在打字，不会去滚动找按钮，于是「表情/图片/是否公开」
        // 全看不见，得先收键盘。这不是 padding 的问题，是**它们放错了层**。
        //
        // 放到滚动区外面之后，ScreenFrame 的 ime padding 一让位，
        // 这一条就自然贴在键盘上方，永远可见。
        //
        // ---- 但这么放会**饿死上面的滚动区**（0.7.258 修的就是这个）----
        //
        // 真机实测（900×1600）：这一整块底部区是 y=1300..1600，**约 300px**。
        // 键盘起来占掉约 700px 之后总高只剩 900：
        //     头部 110 + 底部 300  →  LazyColumn 只剩约 490px
        // 而正文输入框在 y=568 起 —— **整个落在视口外面**。
        // 用户报的就是这个："导航栏被顶上去了，而输入框还是没有任何反应，
        // 完全被软键盘覆盖住了"。底部栏被抬起是对的（它在 imePadding 里面），
        // 输入框没动是因为 LazyColumn 只是**视口变矮**，不会自动滚。
        //
        // 两个修法一起用：
        // 1. 键盘起来时**只留工具行**（表情/图片），把可见范围说明、分享开关、
        //    发布按钮收起来 —— 正在打字的人不需要「发布」，那是打完才点的。
        //    300px 收到约 60px，还给滚动区 240px。
        // 2. 正文框加 bringIntoViewRequester（见上面那个 modifier）。
        val imeVisible = WindowInsets.isImeVisible
        // 键盘可见性一变、且正文框正被聚焦，就把它滚进视野。
        // 照聊天会话那个确认能工作的形状（AetherphoneParityScreens 里也是
        // `WindowInsets.isImeVisible` + LaunchedEffect）。
        LaunchedEffect(imeVisible, bodyFocused) {
            if (imeVisible && bodyFocused) {
                // 等一帧：视口收缩和 inset 变化不在同一帧完成，
                // 立刻滚会按旧的视口高度算。
                withFrameNanos {}
                runCatching { bodyBring.bringIntoView() }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                SzjPressable(
                    onClick = {
                        if (pics.size >= maxPics) {
                            android.widget.Toast.makeText(context, "最多 $maxPics 张", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            picker.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly,
                                )
                            )
                        }
                    },
                    shape = SzjChipShape,
                    enabled = pics.size < maxPics && !uploading && !sending,
                ) {
                    Row(
                        Modifier.clip(SzjChipShape).background(SzjCardRaised).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ImageGlyph(R.drawable.ic_add, SzjMuted, Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("加图片", color = SzjMuted, style = SzjMetaStyle)
                    }
                }
                Spacer(Modifier.width(8.dp))
                SzjPressable(onClick = { emojiOpen = !emojiOpen }, shape = SzjChipShape) {
                    Text(
                        "表情",
                        color = if (emojiOpen) SzjOnAccentSoft else SzjMuted,
                        style = SzjMetaStyle,
                        modifier = Modifier.clip(SzjChipShape)
                            .background(if (emojiOpen) SzjAccentSoft else SzjCardRaised)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            if (emojiOpen) {
                Box(Modifier.padding(bottom = 14.dp)) { SzjEmojiPanel(onPick = { insert(it) }) }
            }
            // 键盘起来时这一段整体收起（说明 + 分享开关 + 发布按钮，约 240px）。
            // 打字的时候不需要它们，而它们占的高度正是输入框被挤出视口的原因。
            if (!imeVisible) {
            Column(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
                // **这里原来是一个叫"谁能看"的选择器，带"仅自己可见"——那是错的，
                // 而且是会误导人的错：帖子发到版块本身就是公开的，石之家没有
                // 私密帖。用户选了"仅自己可见"，帖子照样公开，别人能看到。**
                //
                // 真相（PublishPost.DWDKRiEh.js）：那三个单选**只在"分享到动态"
                // 打开时才渲染**（`1 == et.value ? 单选组 : 不渲染`），
                // 也就是说 scope 管的是**那条动态**的可见范围，不是帖子的。
                // 我把它当帖子的可见性摆出来，是把两件事搞混了。
                //
                // 现在：先说清帖子是公开的，再把 scope 挂到它真正属于的开关下面。
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ImageGlyph(R.drawable.ic_info, SzjMuted, Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(
                        "发到版块的帖子是公开的，谁都能看到",
                        color = SzjMuted, style = SzjMetaStyle,
                    )
                }
                Spacer(Modifier.height(12.dp))
                // 分享到动态：开关。关着的时候下面那三档不出现——
                // 照官网的条件渲染，因为那三档只对动态有意义。
                SzjPressable(onClick = { shareToDynamic = !shareToDynamic }, shape = SzjInnerShape) {
                    Row(
                        Modifier.fillMaxWidth().clip(SzjInnerShape)
                            .background(if (shareToDynamic) SzjAccentSoft else SzjCardRaised)
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "同时分享到动态",
                                color = if (shareToDynamic) SzjOnAccentSoft else SzjText,
                                style = SzjLabelStyle,
                            )
                            Text(
                                "在你的动态里也发一条，指向这篇帖子",
                                color = if (shareToDynamic) SzjOnAccentSoft.copy(alpha = .75f) else SzjMuted,
                                style = SzjMetaStyle,
                            )
                        }
                        ImageGlyph(
                            if (shareToDynamic) R.drawable.ic_check_small else R.drawable.ic_radio_off,
                            if (shareToDynamic) SzjOnAccentSoft else SzjMuted,
                            Modifier.size(17.dp),
                        )
                    }
                }
                if (shareToDynamic) {
                    Spacer(Modifier.height(10.dp))
                    Text("这条动态谁能看", color = SzjText, style = SzjLabelStyle)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ShizhijiaApi.PostScope.entries.forEach { s ->
                            SzjPartChip(s.label, visibility == s) { visibility = s }
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "只影响这条动态。帖子本身在版块里仍然公开。",
                        color = SzjMuted, style = SzjMetaStyle,
                    )
                }
            }
            val canSend = title.isNotBlank() && body.text.isNotBlank() &&
                partId.isNotBlank() && !sending && !uploading
            SzjPressable(
                onClick = {
                    sending = true
                    scope.launch {
                        val html = szjComposeHtml(body.text, pics)
                        when (
                            val r = ShizhijiaApi.publishPost(
                                context, title.trim(), html, partId,
                                // scope 只在分享到动态时才有意义；不分享就送默认值，
                                // 免得留一个看着像"设了可见范围"其实没用的值。
                                scope = if (shareToDynamic) visibility else ShizhijiaApi.PostScope.Public,
                                type = if (isStrategy) "2" else "1",
                                share = shareToDynamic,
                            )
                        ) {
                            is ShizhijiaApi.Res.Ok -> {
                                android.widget.Toast.makeText(context, "发布成功", android.widget.Toast.LENGTH_SHORT).show()
                                // 发完直接进新帖子（官网也是这个行为）。
                                if (r.value.isNotBlank()) nav(SzjRoute.PostDetail(r.value)) else pop()
                            }
                            else -> szjToastWriteFail(context, r, nav)
                        }
                        sending = false
                    }
                },
                shape = SzjInnerShape,
                enabled = canSend,
            ) {
                Box(
                    Modifier.fillMaxWidth().clip(SzjInnerShape)
                        .background(if (canSend) SzjAccentFill else SzjCardRaised)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (sending) {
                        CircularProgressIndicator(color = SzjOnAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text(
                            if (isStrategy) "发布攻略" else "发布",
                            color = if (canSend) SzjOnAccent else SzjMuted,
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            }
        }
    }
}

/**
 * 把输入框里的纯文本 + 图片拼成接口要的 HTML。
 *
 * 规则：空行分段（每段一个 `<p>`），段内换行成 `<br>`，图片按顺序接在正文后面。
 * `&<>` 要转义——正文里出现 `<` 不转义会被当成标签，轻则丢内容重则破坏结构。
 * 表情占位符 `[emoN]` **原样保留**：服务端和渲染端都认这个形式。
 */
private fun szjComposeHtml(text: String, pics: List<String>): String {
    fun esc(s: String) = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
    // 表情占位符要**在转义之后再包回标签**，顺序不能反：
    // 先包标签再转义会把 <span> 的尖括号也转掉，发出去就是一串
    // "&lt;span class=..." 的可见字符。
    //
    // 包成 `<span class="at-emo">[emoN]</span>` 是照官网发送前的处理
    // （PublishDynamic / CommentBox 里都是这个 replaceAll）。裸 [emoN] 我们
    // 自己的渲染器认，但官网客户端认的是这个形式——发出去的东西要让**两边**
    // 都能正确显示，不能只顾自己。
    fun wrapEmoji(s: String) = Regex("""\[(emo\d+)]""").replace(s) { m ->
        """<span class="at-emo">[${m.groupValues[1]}]</span>&nbsp;"""
    }
    val paragraphs = text.trim()
        .split(Regex("\n\\s*\n"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("") { p -> "<p>" + wrapEmoji(esc(p).replace("\n", "<br>")) + "</p>" }
    val imgs = pics.joinToString("") { """<p><img src="$it"></p>""" }
    return paragraphs + imgs
}

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

@Composable
private fun ShizhijiaSearchScreen(state: PhoneState, pop: () -> Unit, nav: (SzjRoute) -> Unit, s: SzjSearchState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var typeMenu by remember { mutableStateOf(false) }
    val query = s.query
    val searchType = s.searchType
    val hotWords = s.hotWords
    val history = s.history
    val postResults = s.postResults
    val userResults = s.userResults
    val glamourResults = s.glamourResults
    val searching = s.searching
    val page = s.page
    val ended = s.ended
    val loadingMore = s.loadingMore
    // Search channel: 帖子 / 攻略 / 用户 / 幻化 (common/search type ids).
    val typeLabel = when (searchType.value) {
        ShizhijiaApi.SEARCH_TYPE_STRAT -> "攻略"
        ShizhijiaApi.SEARCH_TYPE_USER -> "用户"
        ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> "幻化"
        else -> "帖子"
    }
    // 每次进搜索页都是一次新的搜索：清掉上次的关键词和结果。
    // 搜索状态挂在 SzjNav 上（这样翻到帖子详情再回来结果还在），
    // 所以必须在进页面时显式清一次，否则会看到上回搜的东西。
    LaunchedEffect(Unit) {
        s.reset()
        hotWords.value = ShizhijiaApi.getHotSearchList(context).map { it.text }.filter { it.isNotBlank() }.distinct()
    }

    fun doSearch() {
        val q = query.value.trim()
        if (q.isEmpty()) return
        ShizhijiaSession.addSearchHistory(context, q, searchType.value)
        history.value = ShizhijiaSession.searchHistory(context)
        scope.launch {
            searching.value = true
            loadingMore.value = false
            ended.value = false
            page.value = 1
            postResults.value = null; userResults.value = null; glamourResults.value = null
            when (searchType.value) {
                ShizhijiaApi.SEARCH_TYPE_USER -> userResults.value = ShizhijiaApi.searchUsers(context, q)
                ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> glamourResults.value = ShizhijiaApi.searchGlamours(context, q, page.value)
                else -> postResults.value = ShizhijiaApi.searchPosts(context, q, searchType.value, page.value)
            }
            searching.value = false
        }
    }

    fun loadMore() {
        val q = query.value.trim()
        if (q.isEmpty() || ended.value || loadingMore.value || searching.value) return
        loadingMore.value = true
        scope.launch {
            val nextPage = page.value + 1
            when (searchType.value) {
                ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> {
                    val next = ShizhijiaApi.searchGlamours(context, q, nextPage)
                    glamourResults.value = (glamourResults.value.orEmpty() + next)
                    if (next.isEmpty()) ended.value = true else page.value = nextPage
                }
                else -> {
                    val next = ShizhijiaApi.searchPosts(context, q, searchType.value, nextPage)
                    postResults.value = (postResults.value.orEmpty() + next)
                    if (next.isEmpty()) ended.value = true else page.value = nextPage
                }
            }
            loadingMore.value = false
        }
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("搜索", onBack = { pop() })
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // 搜索栏是一块抬起的石板，聚焦感来自阴影而不是描边。
            Row(Modifier.fillMaxWidth()
                .shadow(3.dp, SzjCardShape, ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
                .clip(SzjCardShape).background(SzjCard)
                .then(if (szjLight) Modifier.border(1.dp, SzjLine, SzjCardShape) else Modifier)
                .padding(horizontal = 7.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                // 类型切换与输入框融合在同一搜索栏内（左侧）
                Box {
                    SzjPressable(onClick = { typeMenu = true }, shape = SzjChipShape) {
                    Row(Modifier.clip(SzjChipShape).background(SzjCardRaised)
                        .padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(typeLabel, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        ImageGlyph(R.drawable.ic_chevron_down, SzjMuted, Modifier.padding(start = 4.dp).size(13.dp))
                    }
                    }
                    androidx.compose.material3.DropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        listOf(
                            "帖子" to ShizhijiaApi.SEARCH_TYPE_POST,
                            "攻略" to ShizhijiaApi.SEARCH_TYPE_STRAT,
                            "用户" to ShizhijiaApi.SEARCH_TYPE_USER,
                            "幻化" to ShizhijiaApi.SEARCH_TYPE_GLAMOUR,
                        ).forEach { (label, id) ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(label, color = if (id == searchType.value) SzjAccent else SzjText) },
                                onClick = { searchType.value = id; typeMenu = false },
                            )
                        }
                    }
                }
                BasicTextField(
                    value = query.value,
                    onValueChange = { query.value = it },
                    singleLine = true,
                    textStyle = TextStyle(color = SzjText, fontSize = 15.sp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { doSearch() }),
                    decorationBox = { inner ->
                        Box {
                            if (query.value.isEmpty()) Text("搜索$typeLabel", color = SzjMuted, fontSize = 15.sp)
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp),
                )
                // 放大镜按钮：有内容时才实心，空输入时保持安静。
                val ready = query.value.isNotBlank()
                val btnBg by animateColorAsState(if (ready) SzjAccent else SzjCardRaised, tween(220), label = "szjSearchBtn")
                SzjPressable(onClick = { doSearch() }, shape = SzjChipShape) {
                    Box(Modifier.size(36.dp).clip(SzjChipShape).background(btnBg), contentAlignment = Alignment.Center) {
                        ImageGlyph(R.drawable.ic_search, if (ready) SzjOnAccent else SzjMuted, Modifier.size(18.dp))
                    }
                }
            }
            if (postResults.value == null && userResults.value == null && glamourResults.value == null && !searching.value) {
                if (history.value.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("搜索记录", color = SzjText, style = SzjLabelStyle)
                        Spacer(Modifier.weight(1f))
                        SzjPressable(onClick = {
                            history.value.forEach { ShizhijiaSession.removeSearchHistory(context, it.first, it.second) }
                            history.value = emptyList()
                        }, shape = SzjChipShape) {
                            Text("清空", color = SzjMuted, style = SzjMetaStyle,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("长按一条可以单独删除", color = SzjMuted, fontSize = 10.sp, letterSpacing = 0.3.sp)
                    Spacer(Modifier.height(9.dp))
                    // History chips: tap = quick search, long-press = remove entry.
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        history.value.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { (q, t) ->
                                    val typeLabel2 = when (t) {
                                        ShizhijiaApi.SEARCH_TYPE_STRAT -> "攻略"
                                        ShizhijiaApi.SEARCH_TYPE_USER -> "用户"
                                        ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> "幻化"
                                        else -> "帖子"
                                    }
                                    Text("$typeLabel2 · $q", fontSize = 12.sp, color = SzjText,
                                        modifier = Modifier
                                            .clip(SzjChipShape)
                                            .background(SzjCardRaised)
                                            .pointerInput(q, t) {
                                                detectTapGestures(
                                                    onTap = { query.value = q; searchType.value = t; doSearch() },
                                                    onLongPress = {
                                                        ShizhijiaSession.removeSearchHistory(context, q, t)
                                                        history.value = ShizhijiaSession.searchHistory(context)
                                                    },
                                                )
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                if (hotWords.value.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text("热门搜索", color = SzjText, style = SzjLabelStyle)
                    Spacer(Modifier.height(10.dp))
                    // 热词换行排布，别在窄屏上被挤出屏幕外。
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        hotWords.value.take(9).chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { word ->
                                    SzjPartChip(word, selected = false) { query.value = word; doSearch() }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(Modifier.height(8.dp))
                when {
                    searching.value -> SzjFeedSkeleton()
                    searchType.value == ShizhijiaApi.SEARCH_TYPE_USER -> {
                        if (userResults.value.isNullOrEmpty()) SzjEmpty("没有叫「${query.value.trim()}」的角色", "试试只输入名字的一部分", R.drawable.ic_search)
                        else LazyColumn(state = s.userListState, modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)) {
                            val users = userResults.value.orEmpty()
                            itemsIndexed(users, key = { _, it -> it.uuid }) { index, u ->
                                SzjRise(index) {
                                SzjCardSurface(Modifier.fillMaxWidth().padding(vertical = 5.dp), onClick = { nav(SzjRoute.UserProfile(u.uuid)) }) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        SzjAvatar(u.name, u.avatar, u.uuid, 44)
                                        Spacer(Modifier.width(11.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(u.name, color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                            val line = listOf(u.areaName, u.groupName).filter { it.isNotBlank() }.joinToString(" ")
                                            if (line.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(13); Text(line, color = SzjMuted, style = SzjMetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                            if (u.profile.isNotBlank()) Text(u.profile, color = SzjMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        Text("${u.fansNum} 粉丝", color = SzjMuted, style = SzjMetaStyle)
                                    }
                                }
                                }
                            }
                        }
                    }
                    searchType.value == ShizhijiaApi.SEARCH_TYPE_GLAMOUR -> {
                        if (glamourResults.value.isNullOrEmpty()) SzjEmpty("没找到「${query.value.trim()}」的幻化", "换个部件名或职业名试试", R.drawable.ic_search)
                        else androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            state = s.glamourGridState,
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val results = glamourResults.value.orEmpty()
                            items(results.size, key = { results[it].id }) { idx ->
                                val g = results[idx]
                                // 一行三列，只显示头图，点击进入幻化详情（不是预览）
                                Box(Modifier.clip(SzjInnerShape).clickable { nav(SzjRoute.GlamourDetail(g.id)) }) {
                                    SzjGlamourImage(url = g.mainImage)
                                }
                            }
                        }
                    }
                    postResults.value.isNullOrEmpty() -> SzjEmpty("没有匹配「${query.value.trim()}」的$typeLabel", "换个说法，或者切到别的搜索类型", R.drawable.ic_search)
                    else -> LazyColumn(state = s.postListState, modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp)) {
                        // 搜索页自带左右 16dp 内边距，卡片这里不再重复加。
                        val posts = postResults.value.orEmpty()
                        itemsIndexed(posts, key = { _, it -> it.postsId }) { index, post ->
                            SzjRise(index) { SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) }) }
                        }
                    }
                }
                // 滚动接近底部自动加载下一页（幻化/帖子）
                val gridNearEnd = remember { derivedStateOf {
                    val last = s.glamourGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val total = glamourResults.value?.size ?: 0
                    searchType.value == ShizhijiaApi.SEARCH_TYPE_GLAMOUR && total > 0 && last >= total - 3
                } }
                val listNearEnd = remember { derivedStateOf {
                    val last = s.postListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val total = postResults.value?.size ?: 0
                    searchType.value != ShizhijiaApi.SEARCH_TYPE_GLAMOUR && searchType.value != ShizhijiaApi.SEARCH_TYPE_USER && total > 0 && last >= total - 3
                } }
                LaunchedEffect(gridNearEnd.value, listNearEnd.value, loadingMore.value, ended.value) {
                    if ((gridNearEnd.value || listNearEnd.value) && !loadingMore.value && !ended.value) loadMore()
                }
                if (loadingMore.value) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Dynamic detail (minimal)
// ---------------------------------------------------------------------------

@Composable
private fun ShizhijiaDynamicDetailScreen(
    state: PhoneState,
    id: String,
    pop: () -> Unit,
    nav: (SzjRoute) -> Unit,
) {
    val context = LocalContext.current
    val actionScope = rememberCoroutineScope()
    var d by remember { mutableStateOf<ShizhijiaDynamic?>(null) }
    // **评论。之前这一屏压根没接评论**，只画了作者卡和图片——
    // 所以"明明有评论，点进去什么都没有"。复用帖子那套 szjCommentSection，
    // 行结构同族（dynamic/dynamicCommentDetail 的参数形状和帖子评论一样）。
    var comments by remember(id) { mutableStateOf<List<ShizhijiaComment>>(emptyList()) }
    var commentLoading by remember(id) { mutableStateOf(true) }
    var commentOrder by remember(id) { mutableStateOf("earliest") }
    var onlyAuthor by remember(id) { mutableStateOf(false) }
    var composerOpen by remember(id) { mutableStateOf(false) }
    var replyTo by remember(id) { mutableStateOf<ShizhijiaComment?>(null) }
    // 拉完了没有：原来只有 `d == null` 一个状态，于是**失败和"正在加载"长得一样**，
    // 骨架屏会一直转下去。真实原因（字段名不对导致内容为空）就被这个骨架屏盖住了，
    // 看起来像"打不开"，其实是打开了但没东西。
    var loaded by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) {
        d = ShizhijiaApi.getDynamicDetail(context, id)
        loaded = true
    }
    // 评论单独拉一次，和详情并行——详情失败不该连评论一起没了。
    LaunchedEffect(id, commentOrder, onlyAuthor) {
        commentLoading = true
        comments = ShizhijiaApi.getDynamicComments(
            context, id, order = commentOrder, onlyLandlord = onlyAuthor,
        ).rows
        commentLoading = false
    }
    ScreenFrame(background = SzjBg) {
        SzjHeader("动态详情", onBack = { pop() })
        val item = d
        if (item == null && loaded) {
            // 拉完了但没有内容：说清楚，并给一条下一步。
            SzjEmpty(
                "这条动态没能打开",
                "可能已被删除，或者关注的人设了权限。返回再试一次",
            )
            return@ScreenFrame
        }
        if (item == null) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SzjShimmerBox(Modifier.size(40.dp), RoundedCornerShape(20.dp))
                    Spacer(Modifier.width(10.dp))
                    SzjShimmerBox(Modifier.width(110.dp).height(14.dp), RoundedCornerShape(4.dp))
                }
                Spacer(Modifier.height(16.dp))
                repeat(3) {
                    SzjShimmerBox(Modifier.fillMaxWidth().height(14.dp), RoundedCornerShape(4.dp))
                    Spacer(Modifier.height(9.dp))
                }
            }
            return@ScreenFrame
        }
        // **weight(1f) 不是 fillMaxSize()**：下面新加了固定的回帖入口，
        // 用 fillMaxSize 会把剩余高度全吃掉、那一条被压成 0 高看不见。
        // 这个坑我在这个项目里犯过六次以上，这次是加东西时当场想到的。
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            item {
                SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
                  Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SzjAvatar(item.characterName, item.avatar, item.uuid, 40)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(item.characterName, color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            if (item.createdAt.isNotBlank()) Text(item.createdAt, color = SzjMuted, style = SzjMetaStyle)
                        }
                    }
                    // 同上：动态正文是 HTML，纯 Text 会把标签和 [emoN] 原样显示。
                    if (item.contentText.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        ShizhijiaRichContent(item.contentText)
                    }
                  }
                }
            }
            itemsIndexed(item.images) { ii, img ->
                ShizhijiaRemoteImage(
                    url = img,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clip(SzjInnerShape),
                    onClick = { SzjViewer.open(item.images, ii) },
                )
            }
            // 评论接在图片后面，同一条流（和帖子详情一个形态）。
            szjCommentSection(
                comments = comments,
                commentLoading = commentLoading,
                onlyAuthor = onlyAuthor,
                onToggleAuthor = { onlyAuthor = !onlyAuthor },
                commentOrder = commentOrder,
                onOrder = { commentOrder = it },
                nav = nav,
                onReply = { target -> replyTo = target; composerOpen = true },
            )
        }
        // 底部回帖入口。动态没有点赞/收藏那一排，所以只给一个"回帖"。
        Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
        SzjPressable(
            onClick = { replyTo = null; composerOpen = true },
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.ui.graphics.RectangleShape,
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImageGlyph(R.drawable.ic_comment, SzjMuted, Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("说点什么…", color = SzjMuted, fontSize = 14.sp)
            }
        }
    }
    if (composerOpen) {
        val target = replyTo
        SzjCommentComposer(
            hint = if (target != null) "写你的回复…" else "说点什么…",
            replyTo = target,
            onDismiss = { composerOpen = false; replyTo = null },
            onSend = { text, pics, done ->
                actionScope.launch {
                    when (
                        val r = ShizhijiaApi.commentDynamic(
                            context, id, text,
                            parentId = target?.id ?: "0",
                            rootParent = target?.rootParent ?: "0",
                            pics = pics,
                        )
                    ) {
                        is ShizhijiaApi.Res.Ok -> {
                            android.widget.Toast.makeText(context, r.value, android.widget.Toast.LENGTH_SHORT).show()
                            composerOpen = false
                            replyTo = null
                            // 重拉：新评论要出现，楼中楼的计数也变了。
                            commentLoading = true
                            comments = ShizhijiaApi.getDynamicComments(
                                context, id, order = commentOrder, onlyLandlord = onlyAuthor,
                            ).rows
                            commentLoading = false
                        }
                        else -> szjToastWriteFail(context, r, nav)
                    }
                    done()
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Login via SDO pass WebView
// ---------------------------------------------------------------------------

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ShizhijiaLoginScreen(state: PhoneState, pop: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var done by remember { mutableStateOf(false) }
    // Show a friendly "loading" hint above the WebView while the QQ page loads,
    // which can take a while on a real device.
    var pageLoading by remember { mutableStateOf(false) }
    // null = probing, true = stored cookie works, false = no/invalid session.
    var verified by remember { mutableStateOf<Boolean?>(null) }

    // Validate whatever session cookie we already hold before trusting it.
    LaunchedEffect(Unit) {
        verified = if (ShizhijiaSession.hasSession(context)) ShizhijiaApi.isLoggedIn(context) else false
        android.util.Log.d("ShizhijiaLogin", "initial verified=$verified hasSession=${ShizhijiaSession.hasSession(context)}")
        if (verified == false) {
            // Discard a stale/invalid cookie so the app does not pretend to be logged in.
            ShizhijiaSession.clear(context)
            CookieManager.getInstance().removeAllCookies(null)
        }
    }

    // WeGame login: let the tgp authorize page build the OAuth state (it embeds a
    // QQ iframe that renders blank in WebView), then lift that iframe's URL to the
    // top-level page where the QQ QR code displays (0.7.164 confirmed top-level
    // QQ renders). The lifted URL keeps tgp's state, so the callback is complete.
    val loginUrl = ShizhijiaSession.loginUrl("https://ff14risingstones.web.sdo.com/pc/index.html#/me")
    val apiHost = "apiff14risingstones.web.sdo.com"

    // Only persists the session after a real isLogin probe succeeds using the
    // cookie just read from the WebView jar (it has not been persisted yet).
    fun tryFinalizeLogin() {
        if (done) return
        val cookie = ShizhijiaSession.cookieFromWebView()
        android.util.Log.d("ShizhijiaLogin", "cookie=(${cookie?.take(60) ?: "null"})")
        if (cookie.isNullOrBlank()) return
        scope.launch {
            val ok = ShizhijiaApi.isLoggedIn(context, cookie)
            android.util.Log.d("ShizhijiaLogin", "isLoggedIn(fullCookie)=$ok")
            if (ok) {
                ShizhijiaSession.save(context, cookie)
                done = true
                pop()
            }
        }
    }

    // Poll while the login form is visible: the WeGame/pass flow uses several
    // redirects and a final page finish may arrive before the session cookie
    // is usable, so a periodic probe is more reliable than one-shot detection.
    // Interval kept modest to avoid tripping server-side rate limits.
    LaunchedEffect(verified) {
        while (verified == false && !done) {
            tryFinalizeLogin()
            kotlinx.coroutines.delay(3_000)
        }
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("登录", onBack = { pop() })
        when (verified) {
            null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
            }
            true -> SzjEmpty(
                "已经登录了",
                "想换个账号就重新登录一次",
            ) {
                SzjPrimaryButton("重新登录", onClick = {
                    // Drop the stored cookie and clear the WebView jar so the
                    // next composition shows the SSO page again.
                    ShizhijiaSession.clear(context)
                    CookieManager.getInstance().removeAllCookies(null)
                    verified = false
                })
            }
            false -> Box(Modifier.fillMaxSize()) {
                AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        // The SSO dance jumps across domains (app -> pass.sdo.com
                        // -> app), so both first- and third-party cookies must be
                        // accepted for the session cookie to survive the redirects.
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        // A realistic phone Chrome UA avoids being mistaken for a bot.
                        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
                        // The WeGame/QQ OAuth page mixes http sub-resources into
                        // an https page; WebView blocks mixed content by default
                        // (unlike a desktop browser), which blanks the page.
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageLoading = newProgress in 1..99
                            }
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                android.util.Log.d("ShizhijiaLogin", "onPageFinished url=${url?.take(90)}")
                                val host = url?.let { runCatching { android.net.Uri.parse(it).host }.getOrNull() }
                                // Lift the QQ login iframe out of the tgp authorize
                                // page so the QR code renders top-level in WebView.
                                // Poll a few times: the iframe can appear a moment
                                // after onPageFinished, especially on real devices.
                                if (host == "api.rail.tgp.qq.com" && url.orEmpty().contains("/login/authorize")) {
                                    fun pollLift(attempt: Int) {
                                        if (attempt <= 0) return
                                        view?.evaluateJavascript("(function(){var f=document.querySelector('iframe');return f?f.src:''})()") { r ->
                                            val src = (r ?: "").trim().removeSurrounding("\"")
                                            if (src.isNotBlank()) {
                                                android.util.Log.d("ShizhijiaLogin", "liftQQ iframe src=${src.take(90)}")
                                                view.post { if (view.url != src) view.loadUrl(src) }
                                            } else {
                                                view.postDelayed({ pollLift(attempt - 1) }, 700)
                                            }
                                        }
                                    }
                                    pollLift(12)
                                }
                                view?.evaluateJavascript(
                                    "setInterval(function(){var f=document.querySelector('iframe');var h=f?(f.offsetHeight+'x'+f.offsetWidth):'-';var ft='-';try{ft=f?f.contentDocument.body.innerText.length:0}catch(e){ft='x'}var cn=document.querySelectorAll('canvas').length;var im=document.querySelectorAll('img').length;var app=document.getElementById('app');console.log('DBG vis='+(app?getComputedStyle(app).visibility:'na')+' ifh='+h+' ifText='+ft+' canvas='+cn+' img='+im);},2000);", null)
                                if (host == apiHost || host == "ff14risingstones.web.sdo.com") tryFinalizeLogin()
                            }
                        }
                        loadUrl(loginUrl)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            if (pageLoading) {
                // Friendly hint over the WebView while the QQ page loads (slow on
                // real devices), so it never looks like the app is stuck.
                Box(Modifier.fillMaxSize().background(SzjBg.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("正在打开盛趣登录页", color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(5.dp))
                        Text("这一步在真机上可能要十几秒", color = SzjMuted, style = SzjMetaStyle)
                    }
                }
            }
        }
    }
}
}

/** 签到日历页：本月签到记录 + 累计奖励表（满足天数可直接领取）。 */
@Composable
private fun ShizhijiaSignCalendarScreen(state: PhoneState, pop: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val monthFmt = remember { java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault()) }
    var month by remember { mutableStateOf(monthFmt.format(java.util.Date())) }
    var log by remember { mutableStateOf<ShizhijiaSignLog?>(null) }
    var rewards by remember { mutableStateOf(listOf<ShizhijiaSignReward>()) }
    var claimingId by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            log = ShizhijiaApi.getSignLog(context, month)
            rewards = ShizhijiaApi.getSignRewards(context, month)
        }
    }
    LaunchedEffect(month) { reload() }

    // Signed days as day-of-month numbers for the chip strip.
    val daysInMonth = remember(month) {
        runCatching {
            val cal = java.util.Calendar.getInstance()
            cal.time = monthFmt.parse(month)!!
            cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        }.getOrDefault(31)
    }
    val signedDays = remember(log) {
        log?.days
            ?.mapNotNull { s -> s.split('-').lastOrNull()?.takeWhile { it.isDigit() }?.toIntOrNull() }
            ?.toSortedSet() ?: sortedSetOf<Int>()
    }
    val todayDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)

    ScreenFrame(background = SzjBg) {
        SzjHeader("签到日历", onBack = pop)
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            item {
                // 计数卡：数字放大到 32sp 当主角，这页唯一一个大字号。
                SzjCardSurface(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 12.dp)) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (month == monthFmt.format(java.util.Date())) "本月已签到" else "$month 已签到",
                                fontSize = 13.sp, color = SzjMuted, letterSpacing = 0.4.sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${log?.count ?: 0}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = SzjAccent, lineHeight = 34.sp)
                                Text(" 天", fontSize = 13.sp, color = SzjMuted, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                        // 月份切换。接口本来就收 month 参数，但 month 这个状态
                        // 以前初始化成当月之后再没人改过——这一页只能看当月，
                        // 往前翻的能力白写了。不给往未来翻。
                        fun shift(delta: Int): String = runCatching {
                            val cal = java.util.Calendar.getInstance()
                            cal.time = monthFmt.parse(month)!!
                            cal.add(java.util.Calendar.MONTH, delta)
                            monthFmt.format(cal.time)
                        }.getOrDefault(month)
                        val atCurrent = month >= monthFmt.format(java.util.Date())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SzjPressable(onClick = { month = shift(-1) }, shape = CircleShape) {
                                ImageGlyph(R.drawable.ic_chevron_left, SzjAccent, Modifier.size(26.dp).padding(4.dp))
                            }
                            SzjPressable(onClick = { if (!atCurrent) month = shift(1) }, shape = CircleShape) {
                                ImageGlyph(
                                    R.drawable.ic_chevron_right,
                                    if (atCurrent) SzjMuted.copy(alpha = .4f) else SzjAccent,
                                    Modifier.size(26.dp).padding(4.dp),
                                )
                            }
                        }
                    }
                }
                // Day strip: 7 per row, signed days highlighted.
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..daysInMonth).chunked(7).forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            week.forEach { day ->
                                val signed = day in signedDays
                                val today = day == todayDay && month == monthFmt.format(java.util.Date())
                                // 今天用一根棱条标在数字下方，比描边更安静也更准。
                                Column(
                                    Modifier.size(36.dp).clip(SzjChipShape)
                                        .background(if (signed) SzjAccentSoft else SzjCardRaised),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        day.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = if (signed || today) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (signed) SzjOnAccentSoft else if (today) SzjAccent else SzjMuted,
                                        textAlign = TextAlign.Center,
                                    )
                                    if (today) {
                                        Spacer(Modifier.height(2.dp))
                                        Box(Modifier.size(width = 10.dp, height = 2.dp).background(SzjAccent))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("累计奖励", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = SzjText)
                }
                Spacer(Modifier.height(8.dp))
            }
            items(rewards.size) { i ->
                val r = rewards[i]
                val claimable = r.isGet != 1 && log.let { it != null && it.count >= r.rule } && r.isGet != -1
                // 可领取的那一档整卡抬起来，一眼能看出哪个能点。
                SzjCardSurface(
                    Modifier.fillMaxWidth().padding(vertical = 5.dp),
                    shape = SzjInnerShape,
                    raised = claimable,
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        ShizhijiaRemoteImage(url = r.itemPic, modifier = Modifier.size(44.dp).clip(SzjChipShape), showPlaceholder = true)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(r.itemName, fontSize = 14.sp, color = SzjText, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(2.dp))
                            // 差几天写清楚，比只说"未满足"有用。
                            val need = r.rule - (log?.count ?: 0)
                            Text(
                                when {
                                    r.isGet == 1 -> "累计 ${r.rule} 天 · 已到账"
                                    claimable -> "累计 ${r.rule} 天 · 可以领了"
                                    need > 0 -> "累计 ${r.rule} 天 · 还差 $need 天"
                                    else -> "累计 ${r.rule} 天"
                                },
                                color = if (claimable) SzjAccent else SzjMuted,
                                style = SzjMetaStyle,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        when {
                            r.isGet == 1 -> Text("已领取", style = SzjMetaStyle, color = SzjMuted,
                                modifier = Modifier.clip(SzjChipShape).background(SzjCardRaised).padding(horizontal = 12.dp, vertical = 6.dp))
                            !claimable -> Text("未满足", style = SzjMetaStyle, color = SzjMuted,
                                modifier = Modifier.clip(SzjChipShape).background(SzjCardRaised).padding(horizontal = 12.dp, vertical = 6.dp))
                            else -> SzjPressable(
                                onClick = {
                                    if (claimingId != null) return@SzjPressable
                                    scope.launch {
                                        claimingId = r.id
                                        val ok = ShizhijiaApi.claimSignReward(context, r.id, month)
                                        claimingId = null
                                        if (ok) android.widget.Toast.makeText(context, "已领取 ${r.itemName}", android.widget.Toast.LENGTH_SHORT).show()
                                        else android.widget.Toast.makeText(context, "领取失败，稍后再试", android.widget.Toast.LENGTH_SHORT).show()
                                        reload()
                                    }
                                },
                                shape = SzjChipShape,
                            ) {
                                Text(if (claimingId == r.id) "领取中" else "领取", style = SzjLabelStyle, color = SzjOnAccent,
                                    modifier = Modifier.clip(SzjChipShape).background(SzjAccentFill).padding(horizontal = 14.dp, vertical = 7.dp))
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// 我的：收藏 / 招募管理 / 角色
// ---------------------------------------------------------------------------

/**
 * 收藏页的四个分类。官网 MeCollections 就是这四个标签，
 * 但**四类各走各的接口**，参数和行结构都不一样：
 *
 * - 帖子 / 攻略：`userInfo/myStarPosts`，同一个接口靠 `type` 区分（1 / 2）
 * - RP：`recruit/homePageStarRecruitRp`，不带参数、没有分页
 * - 幻化：`glamour/myFavoriteItemsList`，要先有收藏夹 id
 *
 * 之前这一页只有帖子，而且漏了 `type` —— 服务端回 "Type不正确"，
 * 所以点进来永远是空的；幻化那一层压根没实现。
 */
private enum class SzjFavTab(val label: String) {
    Posts("帖子"), Strats("攻略"), Rp("RP"), Glamour("幻化")
}

/** 我的收藏。接口需登录，未登录时服务端返回 10403 → 这里显示登录引导。 */
@Composable
private fun ShizhijiaFavoritesScreen(pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(SzjFavTab.Posts) }

    // 每类各存一份，切回来不用重新拉。
    class Feed<T> {
        var items by mutableStateOf<List<T>?>(null)
        var status by mutableStateOf<ShizhijiaApi.Res<List<T>>?>(null)
        var page by mutableStateOf(1)
        var ended by mutableStateOf(false)
        var loading by mutableStateOf(false)
    }
    val posts = remember { Feed<ShizhijiaPostCard>() }
    val strats = remember { Feed<ShizhijiaPostCard>() }
    val rp = remember { Feed<ShizhijiaRecruit>() }
    val glam = remember { Feed<ShizhijiaGlamourCard>() }

    // RP 卡要职业字典画位置图标，和招募管理页一样拉一次。
    var jobs by remember { mutableStateOf(mapOf<String, ShizhijiaJob>()) }
    LaunchedEffect(Unit) { jobs = ShizhijiaApi.getJobConfig(context) }

    // 幻化收藏夹：多于一个才给切换条，只有一个就没必要占一行。
    var folders by remember { mutableStateOf<List<Triple<String, String, Boolean>>>(emptyList()) }
    var folderId by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val gridState = remember { androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState() }

    // 首次进入某个标签才拉；之后切回来用缓存。
    LaunchedEffect(tab, folderId) {
        when (tab) {
            SzjFavTab.Posts -> if (posts.items == null) {
                posts.loading = true
                val r = ShizhijiaApi.getMyStarPosts(context, type = "1", page = 1)
                posts.status = r
                posts.items = (r as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
                posts.loading = false
            }
            SzjFavTab.Strats -> if (strats.items == null) {
                strats.loading = true
                val r = ShizhijiaApi.getMyStarPosts(context, type = "2", page = 1)
                strats.status = r
                strats.items = (r as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
                strats.loading = false
            }
            SzjFavTab.Rp -> if (rp.items == null) {
                rp.loading = true
                val r = ShizhijiaApi.getMyStarRecruitRp(context)
                rp.status = r
                rp.items = (r as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
                // 这个接口不分页，一次给完。
                rp.ended = true
                rp.loading = false
            }
            SzjFavTab.Glamour -> {
                if (folders.isEmpty()) {
                    (ShizhijiaApi.glamourFavorites(context, 1, 50) as? ShizhijiaApi.Res.Ok)?.let { res ->
                        folders = res.value
                        if (folderId.isBlank()) {
                            folderId = res.value.firstOrNull { it.third }?.first
                                ?: res.value.firstOrNull()?.first.orEmpty()
                        }
                    }
                }
                if (glam.items == null) {
                    glam.loading = true
                    val r = ShizhijiaApi.getMyStarGlamours(context, favoriteId = folderId, page = 1)
                    glam.status = r
                    glam.items = (r as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
                    glam.loading = false
                }
            }
        }
    }

    // 翻页：RP 不分页，跳过。
    val nearEnd by remember { derivedStateOf {
        if (tab == SzjFavTab.Glamour) {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val n = glam.items?.size ?: 0
            n > 0 && last >= n - 3
        } else {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val n = when (tab) {
                SzjFavTab.Posts -> posts.items?.size
                SzjFavTab.Strats -> strats.items?.size
                else -> 0
            } ?: 0
            n > 0 && last >= n - 3
        }
    } }
    LaunchedEffect(nearEnd, tab) {
        if (!nearEnd) return@LaunchedEffect
        when (tab) {
            SzjFavTab.Posts, SzjFavTab.Strats -> {
                val f = if (tab == SzjFavTab.Posts) posts else strats
                val type = if (tab == SzjFavTab.Posts) "1" else "2"
                if (!f.loading && !f.ended) {
                    f.loading = true
                    val next = (ShizhijiaApi.getMyStarPosts(context, type, f.page + 1) as? ShizhijiaApi.Res.Ok)
                        ?.value.orEmpty()
                    if (next.isEmpty()) f.ended = true else { f.items = f.items.orEmpty() + next; f.page += 1 }
                    f.loading = false
                }
            }
            SzjFavTab.Glamour -> if (!glam.loading && !glam.ended) {
                glam.loading = true
                val next = (ShizhijiaApi.getMyStarGlamours(context, folderId, glam.page + 1) as? ShizhijiaApi.Res.Ok)
                    ?.value.orEmpty()
                if (next.isEmpty()) glam.ended = true else { glam.items = glam.items.orEmpty() + next; glam.page += 1 }
                glam.loading = false
            }
            SzjFavTab.Rp -> Unit
        }
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("我的收藏", onBack = pop)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SzjFavTab.values().forEach { t -> SzjSubTab(t.label, tab == t) { tab = t } }
        }
        Spacer(Modifier.height(4.dp))
        // 幻化有多个收藏夹时给一条切换；只有一个夹子不占这一行。
        if (tab == SzjFavTab.Glamour && folders.size > 1) {
            LazyRow(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(folders, key = { it.first }) { f ->
                    SzjPartChip(f.second.ifBlank { "收藏夹" }, folderId == f.first) {
                        if (folderId != f.first) {
                            folderId = f.first
                            // 换夹子就是换列表，清掉重拉。
                            glam.items = null
                            glam.page = 1
                            glam.ended = false
                        }
                    }
                }
            }
        }
        when (tab) {
            SzjFavTab.Posts, SzjFavTab.Strats -> {
                val f = if (tab == SzjFavTab.Posts) posts else strats
                val list = f.items
                when {
                    list == null && f.loading -> SzjFeedSkeleton()
                    list.isNullOrEmpty() -> SzjResState(
                        res = f.status,
                        emptyTitle = if (tab == SzjFavTab.Posts) "还没收藏过帖子" else "还没收藏过攻略",
                        emptyHint = "在详情页点收藏，之后就能在这里找回来",
                        onLogin = { nav(SzjRoute.Login) },
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = 20.dp),
                    ) {
                        itemsIndexed(list, key = { _, it -> it.postsId }) { index, post ->
                            SzjRise(index) { SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) }) }
                        }
                        item(key = "footer") { SzjFooterSpinner(f.loading) }
                    }
                }
            }
            SzjFavTab.Rp -> {
                val list = rp.items
                when {
                    list == null && rp.loading -> SzjFeedSkeleton()
                    list.isNullOrEmpty() -> SzjResState(
                        res = rp.status,
                        emptyTitle = "还没收藏过 RP 招募",
                        emptyHint = "在 RP 招募详情点收藏",
                        onLogin = { nav(SzjRoute.Login) },
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = 20.dp),
                    ) {
                        itemsIndexed(list, key = { _, it -> it.kind.name + it.id }) { index, r ->
                            SzjRise(index) { SzjRecruitRow(r, nav, jobs) }
                        }
                    }
                }
            }
            SzjFavTab.Glamour -> {
                val list = glam.items
                when {
                    list == null && glam.loading -> SzjFeedSkeleton()
                    list.isNullOrEmpty() -> SzjResState(
                        res = glam.status,
                        emptyTitle = "收藏夹里还没有幻化",
                        emptyHint = "在幻化详情点收藏，收藏会落在你的默认收藏夹里",
                        onLogin = { nav(SzjRoute.Login) },
                    )
                    // 幻化是竖图，和幻化频道一样走两列瀑布流，别塞进单列列表里。
                    else -> androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid(
                        columns = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Fixed(2),
                        state = gridState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 12.dp, end = 12.dp, top = 6.dp, bottom = 20.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 10.dp,
                    ) {
                        items(list.size, key = { list[it].id }) { idx ->
                            SzjRise(idx) { SzjGlamourCardItem(list[idx], nav) }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 让 Dialog 这一层能收到键盘的 inset。
 *
 * **这是"键盘挡住输入框底部按钮"的真正原因**：Compose 的 `Dialog` 会自己开一个
 * Window，而 `WindowInsets.ime` 只在**该 Window 自己**声明了
 * `decorFitsSystemWindows = false` 时才有值。我在 Activity 上设过、在
 * `ScreenFrame` 里也让过 ime，但那些对 Dialog 的窗口都不生效——所以在
 * 对话框里写 `windowInsetsPadding(ime)` 看着对、实际拿到的是 0，
 * 表情/图片/可见范围那一排就一直被键盘压在下面。
 *
 * 顺带把 softInputMode 设成 ADJUST_RESIZE：有些 ROM 的对话框默认是
 * ADJUST_PAN（整层往上推），那样底部按钮会被顶出屏幕外，同样看不见。
 *
 * 在每个带输入框的 Dialog 里调一次。
 */
@Composable
private fun SzjDialogImeFix() {
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        // 只认对话框自己的 window（DialogWindowProvider）。
        // **不要退回 Activity 的 window**：那是另一个窗口，在这儿改它既解决不了
        // 对话框的 inset，还会把副作用甩到不相干的地方（Activity 那份
        // MainActivity 里已经设好了）。拿不到就什么都不做。
        val w = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window ?: return@SideEffect
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, false)
        w.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }
}

/**
 * IP 属地的显示值。空就返回空（不少记录是空的，空就整个不显示）。
 *
 * 值是**省级地区名**，不是 IP 地址——实测形如"中国上海市"、"中国浙江省"。
 * 去掉开头的"中国"：一屏全是"中国"没有信息量，
 * 境外的会留着国名（"日本"），那时候这个前缀才有用。
 */
internal fun szjIpShort(ipLocation: String): String {
    val v = ipLocation.trim()
    if (v.isBlank()) return ""
    return v.removePrefix("中国").ifBlank { v }
}

/**
 * 元信息行里的时间。原来直接打整串 `2026-08-28 21:03:00`——
 * 19 个字符，把一行的横向空间吃光了，这也是 IP 属地挤不进同一行的原因。
 *
 * 列表里没人需要秒，也没人需要"今年"这个信息：
 * 今天的给 `21:03`，今年的给 `08-28 21:03`，更早的给日期。
 * 形状不对（不是 `yyyy-MM-dd HH:mm:ss`）就原样返回，不猜。
 */
internal fun szjShortTime(raw: String): String {
    val s = raw.trim()
    if (s.length < 16 || s[4] != '-' || s[10] != ' ') return s
    val date = s.substring(0, 10)
    val hm = s.substring(11, 16)
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
        .format(java.util.Date())
    return when {
        date == today -> hm
        date.take(4) == today.take(4) -> "${date.substring(5)} $hm"
        else -> date
    }
}

/**
 * 名字行右端的 IP 属地。空就不画。
 *
 * 为什么放在名字行而不是元信息行：见 [SzjMetaLine] 的注释——三段挤一行会把
 * 区服挤掉。名字通常只有几个字，那一行右边本来是空的，正好放这个次要信息，
 * 而且**不增加行数**（头像高度靠名字 + 元信息两行撑满，多一行就错位）。
 */
@Composable
private fun SzjIpTail(ipLocation: String) {
    val ip = szjIpShort(ipLocation)
    if (ip.isBlank()) return
    Text(
        "属地 $ip",
        color = SzjMuted,
        style = SzjMetaStyle,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(start = 8.dp),
    )
}

/**
 * 作者/评论的元信息行：`📍区服 · 时间`。
 *
 * 为什么不给 IP 属地单开一行（0.7.238 那版就是那么干的，难看在这儿）：
 * 头像是 30~36dp，名字 + 元信息刚好两行填满它；加第三行文字块就比头像高，
 * 头像顶在上面、右侧点赞按钮居中对齐整行，三者全错开。
 *
 * 为什么也不塞进这一行（0.7.239 那版，用户报"服务器名被省略了"）：
 * 见下面的注释——一行三段的时候唯一能被压缩的是区服，而那是最该看清的。
 * 现在属地挪到名字行右端（[SzjIpTail]），总行数不变。
 */
@Composable
private fun SzjMetaLine(
    areaName: String,
    groupName: String,
    createdAt: String,
) {
    val server = listOf(areaName, groupName).filter { it.isNotBlank() }.joinToString(" ")
    val time = szjShortTime(createdAt)
    if (server.isBlank() && time.isBlank()) return
    // 三段塞一行装不下，**区服被省略号吃掉了**——这是上一版的回归。
    //
    // 根因不是宽度不够，是我给区服加了 weight：Row 先按本身宽度量那些没有
    // weight 的兄弟（时间、属地），剩下多少才给区服。于是一行里唯一能被压缩的
    // 就是区服，而区服恰恰是最该看清的那个（"猫小胖 神意之地"被截成"猫小胖 神…"
    // 就没意义了）。
    //
    // 现在分两行，但**总行数不变**（还是名字 + 元信息两行，头像高度对得上）：
    //   第一行末尾  属地X        ← 名字那行右边本来是空的，把它用起来
    //   第二行      📍区服 · 时间  ← 区服拿回整行宽度
    // 属地是次要信息，放在名字行右端既不抢主体、又不再挤压区服。
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (server.isNotBlank()) {
            SzjLocPin()
            Text(
                server, color = SzjMuted, style = SzjMetaStyle,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (time.isNotBlank()) {
            if (server.isNotBlank()) Text(" · ", color = SzjLine, style = SzjMetaStyle)
            Text(time, color = SzjMuted, style = SzjMetaStyle, maxLines = 1)
        }
    }
}

/** 列表底部的翻页转圈。四个收藏标签共用，别再各写一遍。 */
@Composable
private fun SzjFooterSpinner(loading: Boolean) {
    if (loading) Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
    }
}

/**
 * 招募管理：我发布的招募 + 一键擦亮。
 *
 * 擦亮会刷新自己所有招募的排序时间（官方就是这个语义，一个接口全刷），
 * 所以按钮放在页头，不做逐条擦亮。
 */
@Composable
private fun ShizhijiaMyRecruitsScreen(pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // RP 没有"我发布的"接口，所以这一页只有四类。
    val kinds = listOf(
        ShizhijiaRecruitKind.Fb,
        ShizhijiaRecruitKind.Novice,
        ShizhijiaRecruitKind.Guild,
        ShizhijiaRecruitKind.Other,
    )
    var kind by remember { mutableStateOf(ShizhijiaRecruitKind.Fb) }
    var items by remember { mutableStateOf<List<ShizhijiaRecruit>?>(null) }
    var status by remember { mutableStateOf<ShizhijiaApi.Res<List<ShizhijiaRecruit>>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var polishing by remember { mutableStateOf(false) }
    // 招募卡的位置图标要用职业字典（公开接口，拉一次）。
    var jobs by remember { mutableStateOf(mapOf<String, ShizhijiaJob>()) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { jobs = ShizhijiaApi.getJobConfig(context) }

    LaunchedEffect(kind) {
        loading = true
        items = null
        val res = ShizhijiaApi.getMyRecruitList(context, kind)
        status = res
        items = (res as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        loading = false
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("招募管理", onBack = pop, trailing = {
            SzjPressable(
                onClick = {
                    if (polishing) return@SzjPressable
                    polishing = true
                    scope.launch {
                        val ok = ShizhijiaApi.oneKeyPolish(context)
                        polishing = false
                        android.widget.Toast.makeText(
                            context,
                            if (ok) "已擦亮，你的招募排序时间刷新了" else "擦亮失败，可能需要重新登录",
                            android.widget.Toast.LENGTH_SHORT,
                        ).show()
                        // 擦亮后排序变了，重新拉一次当前分类。
                        loading = true
                        val again = ShizhijiaApi.getMyRecruitList(context, kind)
                        status = again
                        items = (again as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
                        loading = false
                    }
                },
                shape = SzjChipShape,
            ) {
                Text(
                    if (polishing) "擦亮中" else "一键擦亮",
                    color = SzjOnAccent, style = SzjLabelStyle,
                    modifier = Modifier.clip(SzjChipShape).background(SzjAccentFill).padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        })
        // 发布入口跟着当前分类。部队招募的表单没做，那一类不给。
        if (kind != ShizhijiaRecruitKind.Guild) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.End) {
                SzjPressable(onClick = { nav(SzjRoute.PublishRecruit(kind)) }, shape = SzjChipShape) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(SzjChipShape).background(SzjAccentSoft)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        ImageGlyph(R.drawable.ic_add, SzjOnAccentSoft, Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("发布${kind.label}", color = SzjOnAccentSoft, style = SzjLabelStyle)
                    }
                }
            }
        }
        LazyRow(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(kinds, key = { it.name }) { k -> SzjPartChip(k.label, kind == k) { kind = k } }
        }
        val list = items
        when {
            loading && list == null -> SzjFeedSkeleton()
            list.isNullOrEmpty() -> SzjResState(
                res = status,
                emptyTitle = "这一类你还没发过招募",
                emptyHint = if (kind == ShizhijiaRecruitKind.Guild) "部队招募的发布要在网页里做" else null,
                onLogin = { nav(SzjRoute.Login) },
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp),
            ) {
                itemsIndexed(list, key = { _, it -> it.kind.name + it.id }) { index, r ->
                    SzjRise(index) { SzjRecruitRow(r, nav, jobs) }
                }
            }
        }
    }
}

/**
 * 我的角色：当前绑定的角色 + 按大区换绑。
 *
 * 接口链路取自官网前端（不是猜的）：
 *   getCharacterBindInfo?platform=2  → 当前角色（单个对象，不是数组）
 *   getAreaAndGroupList              → 大区字典（公开）
 *   getFF14Characters?AreaID=<n>     → 该大区下我的角色
 *   bindCharacterInfo {character_id, platform}  → 换绑
 */
@Composable
private fun ShizhijiaCharactersScreen(pop: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf<ShizhijiaBoundCharacter?>(null) }
    var currentStatus by remember { mutableStateOf<ShizhijiaApi.Res<ShizhijiaBoundCharacter?>?>(null) }
    var loadingCurrent by remember { mutableStateOf(true) }
    var areas by remember { mutableStateOf(listOf<ShizhijiaArea>()) }
    var areaId by remember { mutableStateOf(-1) }
    var candidates by remember { mutableStateOf<List<ShizhijiaBoundCharacter>?>(null) }
    var switching by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val res = ShizhijiaApi.getCurrentCharacter(context)
        currentStatus = res
        current = (res as? ShizhijiaApi.Res.Ok)?.value
        loadingCurrent = false
        areas = ShizhijiaApi.getAreaList(context)
    }
    LaunchedEffect(areaId) {
        if (areaId < 0) return@LaunchedEffect
        candidates = null
        candidates = ShizhijiaApi.getAreaCharacters(context, areaId)
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("我的角色", onBack = pop)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 24.dp),
        ) {
            item(key = "current") {
                Column(Modifier.padding(horizontal = 14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                        Text("当前角色", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    when {
                        loadingCurrent -> SzjShimmerBox(Modifier.fillMaxWidth().height(76.dp), SzjCardShape)
                        current == null -> SzjCardSurface(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(15.dp)) {
                                // 分清"没登录"和"登录了但没绑角色"，别一律说要登录
                                val s = currentStatus
                                val (t, h) = when (s) {
                                    is ShizhijiaApi.Res.NeedLogin -> "还没登录" to "登录后这里显示你当前绑定的角色"
                                    is ShizhijiaApi.Res.NeedCharacter -> "账号还没绑定角色" to "在石之家网页或官方 App 里绑一个角色"
                                    is ShizhijiaApi.Res.Failed -> "读不到角色" to s.msg.ifBlank { "服务端返回 ${s.code ?: "网络错误"}" }
                                    else -> "没有绑定角色" to "在石之家绑定一个 FF14 角色后再回来"
                                }
                                Text(t, color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(5.dp))
                                Text(h, color = SzjMuted, style = SzjMetaStyle, lineHeight = 17.sp)
                            }
                        }
                        else -> SzjCharacterCard(current!!, isCurrent = true, busy = false, onSwitch = null)
                    }
                }
            }
            item(key = "switch-title") {
                Column(Modifier.padding(horizontal = 14.dp).padding(top = 20.dp, bottom = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("换一个角色", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("先选大区，再选该区下你名下的角色", color = SzjMuted, style = SzjMetaStyle)
                }
            }
            item(key = "areas") {
                if (areas.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                        SzjShimmerBox(Modifier.fillMaxWidth().height(34.dp), SzjChipShape)
                    }
                } else {
                    LazyRow(
                        Modifier.fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(areas, key = { it.areaId }) { a ->
                            SzjPartChip(a.areaName, areaId == a.areaId) { areaId = a.areaId }
                        }
                    }
                }
            }
            val list = candidates
            when {
                areaId < 0 -> item(key = "pick-area") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 30.dp), contentAlignment = Alignment.Center) {
                        Text("选一个大区看看", color = SzjMuted, style = SzjMetaStyle)
                    }
                }
                list == null -> item(key = "cand-loading") {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        repeat(2) {
                            SzjShimmerBox(Modifier.fillMaxWidth().height(72.dp), SzjCardShape)
                            Spacer(Modifier.height(10.dp))
                        }
                    }
                }
                list.isEmpty() -> item(key = "cand-empty") {
                    SzjEmptyInline("这个大区下没有你的角色", "换个大区试试")
                }
                else -> itemsIndexed(list, key = { _, c -> c.characterId.ifBlank { c.name } }) { index, c ->
                    val isCur = c.characterId.isNotBlank() && c.characterId == current?.characterId
                    SzjRise(index) {
                        Box(Modifier.padding(horizontal = 14.dp, vertical = 5.dp)) {
                            SzjCharacterCard(
                                c,
                                isCurrent = isCur,
                                busy = switching == c.characterId,
                                onSwitch = if (isCur || c.characterId.isBlank()) null else ({
                                    switching = c.characterId
                                    scope.launch {
                                        val ok = ShizhijiaApi.bindCharacter(context, c.characterId)
                                        switching = ""
                                        android.widget.Toast.makeText(
                                            context,
                                            if (ok) "已切换到 ${c.name}" else "切换失败，可能需要重新登录",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                        if (ok) {
                                            // 换绑后当前角色变了，重新读一次；缓存的登录信息也要清，
                                            // 否则顶栏还显示旧角色。
                                            ShizhijiaSession.clearCachedUser(context)
                                            loadingCurrent = true
                                            val again = ShizhijiaApi.getCurrentCharacter(context)
                                            currentStatus = again
                                            current = (again as? ShizhijiaApi.Res.Ok)?.value
                                            loadingCurrent = false
                                        }
                                    }
                                })
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 我的部队。
 *
 * 部队 id 没有独立接口——它是当前角色的 `characterDetail.fc_id`
 * （官网 GuildMain 就是用 `characterDetail.fc_id === guild_id` 判断
 * "这是我自己的部队"）。所以这里先读当前角色拿 fc_id，再查部队主页。
 *
 * getGuildInfo 的字段名没有实测样本（要登录+有部队），所以取值时对
 * 几种常见命名都试一遍，取不到就不显示那一行，不会因为字段不符而空白一片。
 */
@Composable
private fun ShizhijiaMyGuildScreen(pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    var fcId by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<org.json.JSONObject?>(null) }
    // 角色/部队两步都可能因为未登录而失败，各自的状态要留着，
    // 否则"没登录"和"没部队"会显示成同一句话。
    var charRes by remember { mutableStateOf<ShizhijiaApi.Res<ShizhijiaBoundCharacter?>?>(null) }
    var infoRes by remember { mutableStateOf<ShizhijiaApi.Res<org.json.JSONObject>?>(null) }
    var loading by remember { mutableStateOf(true) }
    // 子页：资料 / 成员 / 动态 / 照片墙
    var tab by remember { mutableStateOf(GUILD_PROFILE) }
    // 三个子页各自的数据，切到才拉，切回来不重复请求。
    var members by remember { mutableStateOf<ShizhijiaApi.Res<ShizhijiaGuildMembers>?>(null) }
    var dynamics by remember { mutableStateOf<ShizhijiaApi.Res<List<ShizhijiaDynamic>>?>(null) }
    var photos by remember { mutableStateOf<ShizhijiaApi.Res<List<ShizhijiaGuildPhoto>>?>(null) }

    LaunchedEffect(Unit) {
        val cur = ShizhijiaApi.getCurrentCharacter(context)
        charRes = cur
        fcId = (cur as? ShizhijiaApi.Res.Ok)?.value?.fcId.orEmpty()
        if (!fcId.isNullOrBlank()) {
            val res = ShizhijiaApi.getGuildInfo(context, fcId!!)
            infoRes = res
            info = (res as? ShizhijiaApi.Res.Ok)?.value
        }
        loading = false
    }

    // 子页数据按需加载：切到那一页才发请求，已经有结果就不再发。
    LaunchedEffect(tab, fcId) {
        val id = fcId ?: return@LaunchedEffect
        if (id.isBlank()) return@LaunchedEffect
        when (tab) {
            GUILD_MEMBERS -> if (members == null) members = ShizhijiaApi.getGuildMembers(context, id)
            GUILD_DYNAMICS -> if (dynamics == null) dynamics = ShizhijiaApi.getGuildDynamics(context, id)
            GUILD_PHOTOS -> if (photos == null) photos = ShizhijiaApi.getGuildPhotos(context, id)
        }
    }

    /** 在返回的 JSON 里按多个候选键名取字符串。 */
    fun pick(vararg keys: String): String {
        val o = info ?: return ""
        for (k in keys) {
            val v = o.optString(k)
            if (v.isNotBlank() && v != "null") return v
        }
        return ""
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("我的部队", onBack = pop)
        when {
            loading -> Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
                SzjShimmerBox(Modifier.fillMaxWidth().height(96.dp), SzjCardShape)
                Spacer(Modifier.height(10.dp))
                SzjShimmerBox(Modifier.fillMaxWidth().height(140.dp), SzjCardShape)
            }
            charRes is ShizhijiaApi.Res.NeedLogin -> SzjEmpty(
                "需要登录石之家账号",
                "部队信息跟着当前绑定的角色走，登录后才能读到",
            ) { SzjPrimaryButton("登录", onClick = { nav(SzjRoute.Login) }) }
            fcId.isNullOrBlank() -> SzjEmpty(
                "你的角色没有加入部队",
                "部队信息跟着当前绑定的角色走。换个角色再看看",
            ) { SzjPrimaryButton("我的角色", onClick = { nav(SzjRoute.Characters) }) }
            info == null -> {
                val r = infoRes
                SzjEmpty(
                    "读不到部队信息",
                    when (r) {
                        is ShizhijiaApi.Res.NeedLogin -> "登录状态过期了，重新登录一次"
                        is ShizhijiaApi.Res.Failed ->
                            r.msg.ifBlank { if (r.code == null) "网络没通，检查一下连接" else "服务端返回 ${r.code}" }
                        else -> "接口返回了空"
                    },
                )
            }
            else -> {
                val name = pick("guild_name", "guildName", "name", "fc_name")
                val tag = pick("guild_tag", "guildTag", "tag")
                val slogan = pick("slogan", "guild_slogan", "introduction", "profile", "detail")
                val master = pick("master_name", "masterName", "leader_name")
                val area = pick("area_name", "areaName")
                val group = pick("group_name", "groupName")
                val logo = pick("guild_logo", "logo", "cover_pic", "avatar")
                val memberNum = pick("member_num", "memberNum", "member_count")
                val formed = pick("create_time", "created_at", "form_time")
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 24.dp),
                ) {
                    item(key = "head") {
                        SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                            Column(Modifier.padding(15.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (logo.isNotBlank()) {
                                        ShizhijiaRemoteImage(
                                            url = logo,
                                            modifier = Modifier.size(52.dp).clip(SzjInnerShape),
                                            contentScale = ContentScale.Crop,
                                            collapseOnFail = true,
                                        )
                                        Spacer(Modifier.width(12.dp))
                                    }
                                    Column(Modifier.weight(1f)) {
                                        // 部队简称跟在昵称后面（官网就是 名字 «TAG» 这个排法），
                                        // 不另起一行。名字长了先省略名字，简称始终留住。
                                        Row(verticalAlignment = Alignment.Bottom) {
                                            Text(
                                                name.ifBlank { "未命名部队" },
                                                color = SzjText, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (tag.isNotBlank()) {
                                                Spacer(Modifier.width(6.dp))
                                                Text(
                                                    "«$tag»", color = SzjAccent, style = SzjMetaStyle,
                                                    modifier = Modifier.padding(bottom = 2.dp),
                                                )
                                            }
                                        }
                                        val srv = listOf(area, group).filter { it.isNotBlank() }.joinToString(" ")
                                        if (srv.isNotBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(13); Text(srv, color = SzjMuted, style = SzjMetaStyle) }
                                        }
                                    }
                                }
                                if (slogan.isNotBlank()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(slogan, color = SzjMuted, fontSize = 12.sp, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                    item(key = "tabs") {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SzjPartChip("资料", tab == GUILD_PROFILE) { tab = GUILD_PROFILE }
                            SzjPartChip("成员", tab == GUILD_MEMBERS) { tab = GUILD_MEMBERS }
                            SzjPartChip("动态", tab == GUILD_DYNAMICS) { tab = GUILD_DYNAMICS }
                            SzjPartChip("照片墙", tab == GUILD_PHOTOS) { tab = GUILD_PHOTOS }
                        }
                    }
                    when (tab) {
                        GUILD_PROFILE -> {
                            val rows = listOf(
                                "部队 ID" to fcId.orEmpty(),
                                "团长" to master,
                                "成员" to memberNum,
                                "成立" to formed,
                            ).filter { it.second.isNotBlank() }
                            if (rows.isNotEmpty()) {
                                item(key = "rows") {
                                    SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                                        Column(Modifier.padding(14.dp)) {
                                            rows.forEachIndexed { i, (k, v) ->
                                                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
                                                Row(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                                                    Text(k, color = SzjMuted, style = SzjMetaStyle, modifier = Modifier.width(70.dp))
                                                    Text(v, color = SzjText, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        GUILD_MEMBERS -> szjGuildMemberItems(members, nav)
                        GUILD_DYNAMICS -> szjGuildDynamicItems(dynamics, nav)
                        else -> szjGuildPhotoItems(photos, nav)
                    }
                }
            }
        }
    }
}

private const val GUILD_PROFILE = 0
private const val GUILD_MEMBERS = 1
private const val GUILD_DYNAMICS = 2
private const val GUILD_PHOTOS = 3

/**
 * 子页的载入/失败/空态。三个子页共用，省得每个都写一遍 when。
 * 返回 true 表示已经画了占位，调用方不用再画列表。
 */
private fun LazyListScope.szjGuildPlaceholder(
    res: ShizhijiaApi.Res<*>?,
    isEmpty: Boolean,
    emptyTitle: String,
    key: String,
): Boolean {
    when {
        res == null -> item(key = "$key-load") { SzjFeedSkeleton() }
        res is ShizhijiaApi.Res.Ok && isEmpty -> item(key = "$key-empty") { SzjEmptyInline(emptyTitle) }
        res is ShizhijiaApi.Res.NeedLogin -> item(key = "$key-login") { SzjEmptyInline("登录状态过期了", "重新登录一次") }
        res is ShizhijiaApi.Res.Failed -> item(key = "$key-fail") {
            SzjEmptyInline("没读取到", res.msg.ifBlank { if (res.code == null) "网络没通" else "服务端返回 ${res.code}" })
        }
        else -> return false
    }
    return true
}

/** 成员子页：注册过石之家的能点进主页，未注册的只有名字。 */
private fun LazyListScope.szjGuildMemberItems(
    res: ShizhijiaApi.Res<ShizhijiaGuildMembers>?,
    nav: (SzjRoute) -> Unit,
) {
    val v = (res as? ShizhijiaApi.Res.Ok)?.value
    if (szjGuildPlaceholder(res, v == null || v.total == 0, "还没读到成员", "gm")) return
    val m = v ?: return
    if (m.registered.isNotEmpty()) {
        item(key = "gm-h1") { SzjGuildGroupLabel("石之家成员", m.registered.size) }
        itemsIndexed(m.registered, key = { _, it -> "r-${it.uuid.ifBlank { it.name }}" }) { i, mem ->
            SzjRise(i) { SzjGuildMemberRow(mem, onClick = { nav(SzjRoute.UserProfile(mem.uuid)) }.takeIf { mem.uuid.isNotBlank() }) }
        }
    }
    if (m.unregistered.isNotEmpty()) {
        item(key = "gm-h2") { SzjGuildGroupLabel("未注册石之家", m.unregistered.size) }
        items(m.unregistered, key = { "u-${it.name}" }) { mem -> SzjGuildMemberRow(mem, onClick = null) }
    }
}

/** 分组小标题：棱条 + 名字 + 人数。 */
@Composable
private fun SzjGuildGroupLabel(label: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = SzjText, style = SzjLabelStyle)
        Spacer(Modifier.width(6.dp))
        Text("$count", color = SzjMuted, style = SzjMetaStyle)
    }
}

@Composable
private fun SzjGuildMemberRow(m: ShizhijiaGuildMember, onClick: (() -> Unit)?) {
    SzjCardSurface(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
        onClick = onClick,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SzjAvatar(m.name, m.avatar, m.uuid, 36)
            Spacer(Modifier.width(11.dp))
            Text(m.name, color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            if (m.rank.isNotBlank()) {
                Text(
                    m.rank, color = SzjOnAccentSoft, style = SzjMetaStyle,
                    modifier = Modifier.clip(SzjChipShape).background(SzjAccentSoft).padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

/** 动态子页：直接复用社区那套动态卡。 */
private fun LazyListScope.szjGuildDynamicItems(
    res: ShizhijiaApi.Res<List<ShizhijiaDynamic>>?,
    nav: (SzjRoute) -> Unit,
) {
    val v = (res as? ShizhijiaApi.Res.Ok)?.value
    if (szjGuildPlaceholder(res, v.isNullOrEmpty(), "部队成员还没发动态", "gd")) return
    itemsIndexed(v ?: return, key = { _, it -> it.id }) { i, d ->
        SzjRise(i) { SzjDynamicRow(d, onClick = { nav(SzjRoute.DynamicDetail(d.id)) }) }
    }
}

/**
 * 照片墙子页。整张卡可点进详情看评论；卡里的图单独点是放大看原图。
 */
private fun LazyListScope.szjGuildPhotoItems(
    res: ShizhijiaApi.Res<List<ShizhijiaGuildPhoto>>?,
    nav: (SzjRoute) -> Unit,
) {
    val v = (res as? ShizhijiaApi.Res.Ok)?.value
    if (szjGuildPlaceholder(res, v.isNullOrEmpty(), "照片墙还是空的", "gp")) return
    itemsIndexed(v ?: return, key = { _, it -> it.id }) { i, p ->
        SzjRise(i) { SzjGuildPhotoCard(p, onOpen = { nav(SzjRoute.GuildPhotoDetail(p.id)) }) }
    }
}

@Composable
private fun SzjGuildPhotoCard(p: ShizhijiaGuildPhoto, onOpen: (() -> Unit)? = null) {
    SzjCardSurface(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp),
        onClick = onOpen.takeIf { p.id.isNotBlank() },
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SzjAvatar(p.uploaderName, p.uploaderAvatar, p.uploaderUuid, 32)
                Spacer(Modifier.width(9.dp))
                Text(
                    p.uploaderName.ifBlank { "光之战士" }, color = SzjText, fontSize = 13.sp,
                    fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f),
                )
                if (p.createdAt.isNotBlank()) Text(p.createdAt.take(10), color = SzjMuted, style = SzjMetaStyle)
            }
            if (p.desc.isNotBlank()) {
                Spacer(Modifier.height(9.dp))
                Text(p.desc, color = SzjMuted, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            // 一张时整宽，多张时三列方格——和帖子卡的缩略图同一套排法。
            if (p.urls.size == 1) {
                ShizhijiaRemoteImage(
                    url = p.urls[0],
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp).clip(SzjInnerShape),
                    contentScale = ContentScale.Crop,
                    collapseOnFail = true,
                    onClick = { SzjViewer.url = it },
                )
            } else {
                androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cell = (maxWidth - 12.dp) / 3
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        p.urls.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                row.forEach { url ->
                                    ShizhijiaRemoteImage(
                                        url = url,
                                        modifier = Modifier.width(cell).height(cell).clip(SzjInnerShape),
                                        contentScale = ContentScale.Crop,
                                        showPlaceholder = false,
                                        collapseOnFail = true,
                                        onClick = { SzjViewer.url = it },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (p.likeCount > 0 || p.commentCount > 0) {
                Spacer(Modifier.height(9.dp))
                Row {
                    if (p.likeCount > 0) Text("赞 ${p.likeCount}", color = SzjMuted, style = SzjMetaStyle)
                    if (p.likeCount > 0 && p.commentCount > 0) Text("   ", style = SzjMetaStyle)
                    if (p.commentCount > 0) Text("评论 ${p.commentCount}", color = SzjMuted, style = SzjMetaStyle)
                }
            }
        }
    }
}

/**
 * 照片墙的单张详情：大图 + 说明 + 评论。
 *
 * 评论接口的路径首字母是大写的（guild/GuildPhotoCommentDetail），
 * 服务端就这么写的，别顺手改成小写。
 */
@Composable
private fun ShizhijiaGuildPhotoDetailScreen(photoId: String, pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    var photo by remember(photoId) { mutableStateOf<ShizhijiaGuildPhoto?>(null) }
    var photoRes by remember(photoId) { mutableStateOf<ShizhijiaApi.Res<ShizhijiaGuildPhoto?>?>(null) }
    var comments by remember(photoId) { mutableStateOf<ShizhijiaApi.Res<List<ShizhijiaComment>>?>(null) }

    LaunchedEffect(photoId) {
        val d = ShizhijiaApi.getGuildPhotoDetail(context, photoId)
        photoRes = d
        photo = (d as? ShizhijiaApi.Res.Ok)?.value
        comments = ShizhijiaApi.getGuildPhotoComments(context, photoId)
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("照片", onBack = pop)
        val p = photo
        when {
            photoRes == null -> Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
                SzjShimmerBox(Modifier.fillMaxWidth().height(240.dp), SzjCardShape)
            }
            p == null -> {
                val r = photoRes
                SzjEmpty(
                    "看不到这张照片",
                    when (r) {
                        is ShizhijiaApi.Res.NeedLogin -> "登录后再看"
                        is ShizhijiaApi.Res.Failed ->
                            r.msg.ifBlank { if (r.code == null) "网络没通" else "服务端返回 ${r.code}" }
                        else -> "可能已经被删掉了"
                    },
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                item(key = "head") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SzjPressable(
                            onClick = { if (p.uploaderUuid.isNotBlank()) nav(SzjRoute.UserProfile(p.uploaderUuid)) },
                            shape = CircleShape,
                        ) { SzjAvatar(p.uploaderName, p.uploaderAvatar, p.uploaderUuid, 38) }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.uploaderName.ifBlank { "光之战士" }, color = SzjText,
                                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            )
                            if (p.createdAt.isNotBlank()) Text(p.createdAt, color = SzjMuted, style = SzjMetaStyle)
                        }
                    }
                }
                if (p.desc.isNotBlank()) {
                    item(key = "desc") {
                        Text(
                            p.desc, color = SzjText, fontSize = 14.sp, lineHeight = 21.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                }
                // 大图竖排铺满宽度，点开进全屏查看器。
                itemsIndexed(p.urls, key = { _, u -> u }) { pi, url ->
                    ShizhijiaRemoteImage(
                        url = url,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clip(SzjInnerShape),
                        contentScale = ContentScale.FillWidth,
                        collapseOnFail = true,
                        onClick = { SzjViewer.open(p.urls, pi) },
                    )
                }
                item(key = "counts") {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text("赞 ${p.likeCount}", color = SzjMuted, style = SzjMetaStyle)
                        Spacer(Modifier.width(14.dp))
                        Text("评论 ${p.commentCount}", color = SzjMuted, style = SzjMetaStyle)
                    }
                }
                item(key = "clabel") {
                    Row(
                        Modifier.fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("评论", color = SzjText, style = SzjLabelStyle)
                    }
                }
                val cs = (comments as? ShizhijiaApi.Res.Ok)?.value
                when {
                    comments == null -> item(key = "cload") {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        }
                    }
                    cs.isNullOrEmpty() -> item(key = "cempty") {
                        Box(Modifier.fillMaxWidth()) {
                            SzjEmptyInline("还没有评论", iconRes = R.drawable.ic_comment)
                        }
                    }
                    else -> items(cs, key = { it.id }) { c ->
                        SzjCommentRow(c, nav)
                    }
                }
            }
        }
    }
}

/** 角色卡：头像 + 名字 + 服务器 + 种族，右侧是「当前」标记或「切换」按钮。 */
@Composable
private fun SzjCharacterCard(
    c: ShizhijiaBoundCharacter,
    isCurrent: Boolean,
    busy: Boolean,
    onSwitch: (() -> Unit)?,
) {
    SzjCardSurface(Modifier.fillMaxWidth(), raised = isCurrent) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            SzjAvatar(c.name, c.avatar, "", 44)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                val srv = listOf(c.areaName, c.groupName).filter { it.isNotBlank() }.joinToString(" ")
                if (srv.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(13); Text(srv, color = SzjMuted, style = SzjMetaStyle) }
                }
                val rt = listOf(szjRaceName(c.race), szjTribeName(c.tribe))
                    .filter { it.isNotBlank() }.joinToString(" · ")
                if (rt.isNotBlank()) Text(rt, color = SzjMuted, style = SzjMetaStyle)
            }
            Spacer(Modifier.width(8.dp))
            when {
                isCurrent -> Text(
                    "当前", color = SzjOnAccentSoft, style = SzjMetaStyle,
                    modifier = Modifier.clip(SzjChipShape).background(SzjAccentSoft).padding(horizontal = 9.dp, vertical = 4.dp),
                )
                busy -> CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                onSwitch != null -> SzjPressable(onClick = onSwitch, shape = SzjChipShape) {
                    Text(
                        "切换", color = SzjOnAccent, style = SzjLabelStyle,
                        modifier = Modifier.clip(SzjChipShape).background(SzjAccentFill).padding(horizontal = 13.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

/** 种族/部族中文名（id 与官方一致）。 */
private fun szjRaceName(id: Int) = when (id) {
    1 -> "人族"; 2 -> "精灵族"; 3 -> "拉拉菲尔族"; 4 -> "猫魅族"
    5 -> "鲁加族"; 6 -> "敖龙族"; 7 -> "硌狮族"; 8 -> "维埃拉族"; else -> ""
}

private fun szjTribeName(id: Int) = when (id) {
    1 -> "中原之民"; 2 -> "高地之民"; 3 -> "森林之民"; 4 -> "黑影之民"
    5 -> "平原之民"; 6 -> "丘陵之民"; 7 -> "逐日之民"; 8 -> "追月之民"
    9 -> "海洋之民"; 10 -> "红血之民"; 11 -> "晨曦之民"; 12 -> "月影之民"
    13 -> "日耀之民"; 14 -> "流浪之民"; 15 -> "拉维之民"; 16 -> "维娜之民"; else -> ""
}

/** 能工巧匠/大地使者没有官方图标资源，用标准英文缩写徽章代替。 */
private fun szjCrafterAbbr(name: String) = when (name) {
    "刻木匠" -> "CRP"; "锻铁匠" -> "BSM"; "铸甲匠" -> "ARM"; "雕金匠" -> "GLD"
    "制革匠" -> "LTH"; "裁衣匠" -> "WVR"; "炼金术士" -> "ALC"; "烹调师" -> "CUL"
    "采矿工" -> "MIN"; "园艺工" -> "BTN"; "捕鱼人" -> "FSH"; else -> name.take(1)
}

/**
 * 关注 / 粉丝列表。
 *
 * 主页上那三个数字（关注/粉丝/获赞）以前是纯展示，点不动——想知道自己关注了谁
 * 只能去网页。现在关注和粉丝都能点进来（获赞不是名单，仍然不可点）。
 *
 * 粉丝接口没对过真实响应，所以空列表要分两种说法：
 * 请求成功但确实没人 → "还没有粉丝"；请求失败 → "读不到"，别让用户以为是 0。
 */
@Composable
private fun ShizhijiaRelationListScreen(
    uuid: String,
    fans: Boolean,
    who: String,
    pop: () -> Unit,
    nav: (SzjRoute) -> Unit,
) {
    val context = LocalContext.current
    var rows by remember(uuid, fans) { mutableStateOf<List<ShizhijiaFriendRoster.Entry>?>(null) }
    var failed by remember(uuid, fans) { mutableStateOf(false) }
    var needLogin by remember(uuid, fans) { mutableStateOf(false) }
    LaunchedEffect(uuid, fans) {
        when (val res = ShizhijiaApi.getRelationList(context, fans = fans, uuid = uuid)) {
            is ShizhijiaApi.Res.Ok -> rows = res.value
            is ShizhijiaApi.Res.NeedLogin -> { needLogin = true; rows = emptyList() }
            is ShizhijiaApi.Res.NeedCharacter -> { needLogin = true; rows = emptyList() }
            is ShizhijiaApi.Res.Failed -> { failed = true; rows = emptyList() }
        }
    }
    val title = (if (who.isNotBlank()) "$who · " else "") + if (fans) "粉丝" else "关注"
    ScreenFrame(background = SzjBg) {
        SzjHeader(title, onBack = pop)
        val list = rows
        when {
            list == null -> SzjFeedSkeleton()
            needLogin -> SzjEmpty("登录后才能看这份名单", "石之家只对本人开放关注和粉丝列表", R.drawable.ic_person)
            failed -> SzjEmpty("这份名单读不到", "接口没返回数据，不代表这里是空的", R.drawable.ic_warning)
            list.isEmpty() -> SzjEmpty(
                if (fans) "还没有粉丝" else "还没有关注任何人",
                if (fans) "发帖和发幻化会带来关注者" else "去社区找几个想追的光之战士",
                R.drawable.ic_person,
            )
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                itemsIndexed(list, key = { i, e -> "${e.uuid}-$i" }) { index, e ->
                    SzjRise(index) {
                        SzjCardSurface(
                            Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            onClick = if (e.uuid.isNotBlank()) ({ nav(SzjRoute.UserProfile(e.uuid)) }) else null,
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                SzjAvatar(e.name, e.avatar, e.uuid, 44)
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(e.name, color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    val line = listOf(e.areaName, e.groupName).filter { it.isNotBlank() }.joinToString(" ")
                                    if (line.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            SzjLocPin(13)
                                            Text(line, color = SzjMuted, style = SzjMetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                ImageGlyph(R.drawable.ic_chevron_right, SzjMuted, Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 职业按职能分组
//
// 接口给的 career_type 只有"战斗精英/魔法导师/能工巧匠/大地使者"四类，
// 没有坦克/治疗/近战/远敏/法系这一层——那是游戏里的职能划分，得自己列表。
// 官网移动端就是按职能一行一行摆的。
//
// 顺序按游戏内职业面板：坦克 → 治疗 → 近战 → 远敏 → 法系 → 巧匠 → 使者。
// ---------------------------------------------------------------------------

private val SzjRoleJobs: List<Pair<String, List<String>>> = listOf(
    "坦克" to listOf("骑士", "战士", "暗黑骑士", "绝枪战士"),
    "治疗" to listOf("白魔法师", "学者", "占星术士", "贤者"),
    "近战" to listOf("武僧", "龙骑士", "忍者", "武士", "钟情剑士", "蝰蛇剑士"),
    "远敏" to listOf("吟游诗人", "机工士", "舞者"),
    "法系" to listOf("黑魔法师", "召唤师", "赤魔法师", "绘灵法师", "青魔法师"),
    "能工巧匠" to listOf("刻木匠", "锻铁匠", "铸甲匠", "雕金匠", "制革匠", "裁衣匠", "炼金术士", "烹调师"),
    "大地使者" to listOf("采矿工", "园艺工", "捕鱼人"),
)

/**
 * 职能在 FFXIV 里是有颜色的，而且是全体玩家共识的那三个：
 * 坦克蓝、治疗绿、输出红——排本、招募、队列面板全用这一套。
 *
 * 这个色**直接染职能名那两个字**，不额外画杠也不加图形：不占一点多余空间，
 * 颜色这条信息照样在。生产/采集没有官定颜色，用静默灰。
 */
private val SzjRoleAccents: Map<String, Color> = mapOf(
    "坦克" to Color(0xFF4B7BE5),
    "治疗" to Color(0xFF4FA96A),
    "近战" to Color(0xFFC8574B),
    "远敏" to Color(0xFFC8574B),
    "法系" to Color(0xFFC8574B),
)

/**
 * 一个职能一行：左边职能名（染职能色），右边这一职能的全部职业，等级写在图标下面。
 *
 * 版式收紧过一轮。上一版是「职能名独占一行 + 下面一条色杠 + 右侧进度 + 8 列图标」，
 * 一个职能要占三层，七个职能铺下来比整张资料卡还高。现在压成一行：
 *   - 职能名和图标同一行（名字染色，不画杠）；
 *   - 格子尺寸按可用宽度算出来（BoxWithConstraints），始终按 8 列的宽度取，
 *     所以各职能的列位在竖直方向对齐，窄屏也不会挤出边界；
 *   - 等级回到图标正下方（原来压在图标右下角当角标，糊在图上不好读）；
 *   - "满级几个"去掉了——图标下面就是等级，数得出来的事不用再写一遍。
 *
 * 没练的职业照样列出来：图标压到 30% 透明，等级位写一个"—"。
 *
 * [known] 是石之家职业图标表的 key。用它过一遍，国服还没实装的职业
 * 不会凭空多出几个永远填不上的灰位。
 */
@Composable
private fun SzjCareerRoleRow(
    role: String,
    jobNames: List<String>,
    byName: Map<String, ShizhijiaCareer>,
    jobIcons: Map<String, String>,
    known: Set<String>,
    tipCareer: String?,
    onTip: (String?) -> Unit,
) {
    val shown = jobNames.filter { it in known || it in byName }
    if (shown.isEmpty()) return
    val roleColor = SzjRoleAccents[role] ?: SzjMuted
    // 职能名的槽：4 个字 × 11sp ≈ 44dp。给 46dp，"能工巧匠"也是一行。
    val labelWidth = 46.dp
    val gap = 4.dp
    val columns = 8
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        // 格子宽度始终按 8 列算——哪怕这个职能只有 3 个职业，格子也和别的职能一样大，
        // 列位才对得齐。窄屏上格子会自动变小，不会挤出边界。
        val cell = ((maxWidth - labelWidth - gap * (columns + 1)) / columns).coerceIn(20.dp, 34.dp)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(
                role,
                color = roleColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.width(labelWidth).padding(top = (cell - 11.dp) / 2),
            )
            Spacer(Modifier.width(gap))
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                shown.chunked(columns).forEach { chunk ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        chunk.forEach { name ->
                            val level = byName[name]?.level ?: 0
                            val has = level > 0
                            val icon = jobIcons[name].orEmpty()
                            val showTip = tipCareer == name
                            // 外层锁死 cell 宽：职业名那个气泡不能参与测量，
                            // 否则它比格子宽，会把这一行后面的图标全顶开。
                            Box(Modifier.width(cell)) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                        .clickable { onTip(if (showTip) null else name) },
                                ) {
                                    Box(
                                        Modifier.size(cell).clip(SzjChipShape)
                                            .background(if (has) SzjCardRaised else SzjCardRaised.copy(alpha = .45f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (icon.isNotBlank()) {
                                            ShizhijiaRemoteImage(
                                                url = icon,
                                                modifier = Modifier.fillMaxSize().padding(1.dp)
                                                    .graphicsLayer { alpha = if (has) 1f else 0.30f },
                                                contentScale = ContentScale.Fit,
                                                showPlaceholder = false,
                                            )
                                        } else {
                                            Text(
                                                szjCrafterAbbr(name),
                                                color = if (has) SzjText else SzjMuted.copy(alpha = .45f),
                                                fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                    // 等级在图标正下方。
                                    Text(
                                        if (has) "$level" else "—",
                                        fontSize = 9.sp, lineHeight = 11.sp,
                                        fontWeight = if (level >= 100) FontWeight.SemiBold else FontWeight.Normal,
                                        color = when {
                                            level >= 100 -> SzjAccent
                                            has -> SzjText
                                            else -> SzjMuted.copy(alpha = .5f)
                                        },
                                    )
                                }
                                if (showTip) {
                                    // 气泡装在一个 0 宽的容器里向两边溢出。
                                    // unbounded = true 允许子节点超出父约束，并且
                                    // **不**把子节点的宽度算回父节点——所以气泡再长
                                    // 也不会挤到旁边的职业图标。
                                    //
                                    // 之前只写 offset 是错的：offset 只挪绘制位置，
                                    // 测量阶段这个 Text 仍然按自己的完整宽度参与，
                                    // Box 于是被撑宽，兄弟节点被推走。
                                    // 和会话列表时间戳那个锯齿是同一类错误。
                                    Box(
                                        Modifier.align(Alignment.TopCenter)
                                            .width(0.dp)
                                            .wrapContentSize(unbounded = true),
                                    ) {
                                        Text(
                                            name, color = SzjOnAccentSoft, fontSize = 10.sp,
                                            maxLines = 1, softWrap = false,
                                            modifier = Modifier.offset(y = (-18).dp)
                                                .clip(SzjChipShape)
                                                .background(SzjAccentSoft)
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
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
}

/** 玩家主页：资料卡(头像/UID/粉丝获赞) + 信息(职业/种族/部队/游戏数据) + TA的帖子。 */
@Composable
private fun ShizhijiaUserProfileScreen(state: PhoneState, uuid: String, pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var profile by remember { mutableStateOf<ShizhijiaUserProfile?>(null) }
    var avatarUrl by remember { mutableStateOf("") }
    var tab by remember { mutableStateOf(0) } // 0=信息 1=帖子
    var posts by remember { mutableStateOf(listOf<ShizhijiaPostCard>()) }
    var postLoading by remember { mutableStateOf(false) }
    var postPage by remember { mutableStateOf(1) }
    var postEnded by remember { mutableStateOf(false) }

    var jobIcons by remember { mutableStateOf(mapOf<String, String>()) }
    var tipCareer by remember { mutableStateOf<String?>(null) }
    var recents by remember { mutableStateOf(listOf<ShizhijiaRecentEvent>()) }
    var recentsPrivate by remember { mutableStateOf(false) }
    LaunchedEffect(uuid) {
        jobIcons = ShizhijiaApi.jobIconByName(context)
        android.util.Log.d("ShizhijiaLogin", "profile jobIcons size=${jobIcons.size} crp=${jobIcons["裁衣匠"] ?: "MISS"}")
        profile = ShizhijiaApi.getUserProfile(context, uuid)
        scope.launch {
            val r = ShizhijiaApi.getRecentEvents(context, uuid)
            recents = r
            recentsPrivate = r.isEmpty()
        }
        val p = profile
        avatarUrl = when {
            p == null -> ""
            p.avatar.isNotBlank() -> p.avatar
            p.race > 0 && p.tribe > 0 && p.gender >= 0 ->
                "https://static.web.sdo.com/jijiamobile/pic/ff14/2023ffstone/${p.race}-${p.tribe}-${p.gender}.jpg"
            else -> ""
        }
    }
    LaunchedEffect(uuid, tab) {
        if (tab == 1 && posts.isEmpty() && !postEnded) {
            postLoading = true
            val next = ShizhijiaApi.getUserPosts(context, uuid, postPage)
            posts = posts + next.rows
            if (next.rows.isEmpty()) postEnded = true else postPage += 1
            postLoading = false
        }
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("玩家主页", onBack = pop)
        val p = profile
        if (p == null) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SzjShimmerBox(Modifier.size(64.dp), RoundedCornerShape(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        SzjShimmerBox(Modifier.width(130.dp).height(17.dp), RoundedCornerShape(4.dp))
                        Spacer(Modifier.height(7.dp))
                        SzjShimmerBox(Modifier.width(90.dp).height(12.dp), RoundedCornerShape(4.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                SzjShimmerBox(Modifier.fillMaxWidth().height(58.dp), SzjCardShape)
            }
            return@ScreenFrame
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item {
                // 资料卡：头像 + 名字 + 服务器 + UID + 签名 + 计数条，收成一张石板。
                SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
                  Column(Modifier.padding(15.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SzjAvatar(p.name, avatarUrl, p.uuid, 62)
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, color = SzjText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(14); Text(listOf(p.areaName, p.groupName).filter { it.isNotBlank() }.joinToString(" "), color = SzjMuted, style = SzjMetaStyle) }
                            Text("UID $uuid", color = SzjMuted, fontSize = 10.sp, letterSpacing = 0.5.sp)
                        }
                    }
                    if (p.profile.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(p.profile, color = SzjMuted, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    // 关注和粉丝可以点进名单（获赞不是名单，保持不可点）。
                    // 原来三个都是纯数字，想知道自己关注了谁只能去网页。
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        val cells: List<Triple<String, Int, (() -> Unit)?>> = listOf(
                            Triple("关注", p.followNum) { nav(SzjRoute.RelationList(uuid, fans = false, who = p.name)) },
                            Triple("粉丝", p.fansNum) { nav(SzjRoute.RelationList(uuid, fans = true, who = p.name)) },
                            Triple("获赞", p.likedNum, null),
                        )
                        cells.forEachIndexed { i, (label, num, onClick) ->
                            if (i > 0) Box(Modifier.width(1.dp).height(22.dp).background(SzjLine))
                            Column(
                                Modifier.weight(1f)
                                    .let { if (onClick != null) it.clip(SzjChipShape).clickable(onClick = onClick) else it }
                                    .padding(vertical = 3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("$num", color = SzjText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(label, color = SzjMuted, style = SzjMetaStyle)
                                    if (onClick != null) {
                                        ImageGlyph(R.drawable.ic_chevron_right, SzjMuted, Modifier.padding(start = 1.dp).size(11.dp))
                                    }
                                }
                            }
                        }
                    }
                  }
                }
                Spacer(Modifier.height(6.dp))
            }
            // Tabs: 信息 / 帖子 —— 复用社区那套棱条选中态
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SzjSubTab("信息", tab == 0) { tab = 0 }
                    SzjSubTab("游戏近况", tab == 2) { tab = 2 }
                    SzjSubTab("帖子", tab == 1) { tab = 1 }
                }
            }
            if (tab == 0) {
                // ---- 信息 tab ----
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        // 职业按职能分组（坦克/治疗/近战/远敏/法系 + 巧匠/使者），
                        // 没练的也列出来。原来是"战斗类全部按等级倒序铺成 5 列"，
                        // 读不出职能结构，也看不出哪些还没开。
                        val byName = remember(p.careers) { p.careers.associateBy { it.name } }
                        val known = remember(jobIcons) { jobIcons.keys.toSet() }
                        Column(Modifier.fillMaxWidth()) {
                            SzjRoleJobs.forEachIndexed { index, (role, names) ->
                                // 战斗职业和生产采集之间一道发丝线：这是两类完全不同的东西，
                                // 中间只留白的话读起来还是一长串。
                                if (role == "能工巧匠") {
                                    Spacer(Modifier.height(7.dp))
                                    Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
                                    Spacer(Modifier.height(7.dp))
                                }
                                SzjCareerRoleRow(
                                    role = role,
                                    jobNames = names,
                                    byName = byName,
                                    jobIcons = jobIcons,
                                    known = known,
                                    tipCareer = tipCareer,
                                    onTip = { tipCareer = it },
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Spacer(Modifier.height(6.dp))
                        val genderText = when (p.gender) { 0 -> "男"; 1 -> "女"; else -> "" }
                        // 资料明细收进一张卡：标签左对齐固定宽度，值右侧成一列，
                        // 原来"标签：值"混排在长内容下会各行错开。
                        val rows = listOf(
                            "种族性别" to listOfNotNull(szjRaceName(p.race).ifBlank { null }, szjTribeName(p.tribe).ifBlank { null }, genderText.takeIf { it.isNotBlank() }).joinToString(""),
                            "部队名称" to listOfNotNull(p.guildName.takeIf { it.isNotBlank() }, p.guildTag.takeIf { it.isNotBlank() }?.let { "<$it>" }).joinToString(" "),
                            "创作时间" to p.createTime,
                            "最近登录" to p.lastLoginTime,
                            "游戏时长" to p.playTime,
                            "房屋信息" to p.houseInfo,
                            // washing_num = 幻想药次数。"洗"是社区对幻想药的叫法
                            // （洗性别/洗种族），不是漂白染色。
                            "幻想药使用次数" to if (p.washingNum > 0) p.washingNum.toString() else "",
                            "水晶冲突段位" to p.crystalRank,
                            "钓鱼次数" to if (p.fishTimes > 0) p.fishTimes.toString() else "",
                            // kill_times / treasure_times 这两行删了。
                            // 原来写的"伪零击败数""宝物击败数"都是我编的中文名——
                            // characterDetail 里这两个字段官网没有对应栏目，
                            // 别的接口里的同名字段是 PvP 击杀，是另一回事。
                            // 编一个像真的名字比不显示更糟：你会当成真的去读。
                            // 字段仍在 ShizhijiaUserProfile 里，确认了真实说法再加回来。
                            "无人岛等级" to if (p.newrank > 0) p.newrank.toString() else "",
                        ).filter { it.second.isNotBlank() }
                        if (rows.isNotEmpty()) {
                            SzjCardSurface(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    rows.forEachIndexed { i, (label, value) ->
                                        if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
                                        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.Top) {
                                            Text(label, color = SzjMuted, style = SzjMetaStyle, modifier = Modifier.width(84.dp))
                                            Text(value, color = SzjText, fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                        // 特殊成就: medal icons + name/detail/time.
                        if (p.achievements.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("特殊成就", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(6.dp))
                                Text("${p.achievements.size}", color = SzjMuted, style = SzjMetaStyle)
                            }
                            Spacer(Modifier.height(8.dp))
                            p.achievements.take(20).forEach { a ->
                                SzjCardSurface(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = SzjInnerShape) {
                                    Row(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                        val medalUrl = if (a.medalId.isNotBlank())
                                            "https://static.web.sdo.com/jijiamobile/pic/ff14/ffstones/medal/medal${a.medalId}.png" else ""
                                        ShizhijiaRemoteImage(url = medalUrl, modifier = Modifier.size(36.dp), contentScale = ContentScale.Fit, showPlaceholder = false)
                                        Spacer(Modifier.width(11.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(a.name, color = SzjText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (a.detail.isNotBlank()) Text(a.detail, color = SzjMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            if (a.time.isNotBlank()) Text(a.time.take(10), color = SzjMuted, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                        }
                    }
                }
            } else if (tab == 2) {
                // ---- 游戏近况 tab ----
                // 原来这一段挂在 信息 tab 的最后面：资料明细 + 特殊成就 + 近况
                // 三块堆在一屏里，近况在最底下要滑很久才看到。
                // 它本来就是一份独立的时间线，给它自己的标签。
                if (recents.isEmpty()) {
                    // 用 Inline 版：SzjEmpty 是 fillMaxSize 的，塞进 LazyColumn 的
                    // item 里会去抢整个视口的高度。
                    item {
                        SzjEmptyInline(
                            if (recentsPrivate) "这位玩家把近况设为了私密" else "最近没有可展示的记录",
                            if (recentsPrivate) "他在石之家上关掉了近况展示" else "打本、钓鱼、采集这些记录会出现在这里",
                        )
                    }
                } else {
                    items(recents.take(15)) { r ->
                        SzjCardSurface(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            shape = SzjInnerShape,
                        ) {
                            Row(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                val recentUrl = if (r.typeId.isNotBlank())
                                    "https://static.web.sdo.com/jijiamobile/pic/ff14/ffstones/recent/r${r.typeId}.png" else ""
                                ShizhijiaRemoteImage(url = recentUrl, modifier = Modifier.size(30.dp), showPlaceholder = false)
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(r.eventType, color = SzjText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    if (r.detail.isNotBlank()) Text(r.detail, color = SzjMuted, fontSize = 11.sp, lineHeight = 16.sp)
                                    if (r.logTime.isNotBlank()) Text(r.logTime, color = SzjMuted, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(10.dp)) }
                }
            } else {
                // ---- 帖子 tab ----
                if (posts.isEmpty() && !postLoading) {
                    item { Text("暂无帖子", color = SzjMuted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) }
                } else {
                    items(posts, key = { it.postsId }) { post ->
                        SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) })
                    }
                    if (!postEnded) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(10.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            }
                            LaunchedEffect(posts.size) {
                                if (posts.isNotEmpty()) {
                                    postLoading = true
                                    val next = ShizhijiaApi.getUserPosts(context, uuid, postPage)
                                    posts = posts + next.rows
                                    if (next.rows.isEmpty()) postEnded = true else postPage += 1
                                    postLoading = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 幻化详情：作者条 + 图片轮播 + 标题/种族性别/日期 + 双列装备表（仿官方布局）。 */
@Composable
private fun ShizhijiaGlamourDetailScreen(state: PhoneState, glamourId: String, pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    var g by remember { mutableStateOf<ShizhijiaGlamourDetail?>(null) }
    // 赞/收藏跟着切换接口的返回值走，同帖子详情。
    var liked by remember(glamourId) { mutableStateOf(false) }
    var favorited by remember(glamourId) { mutableStateOf(false) }
    var likeNum by remember(glamourId) { mutableStateOf(0L) }
    var favNum by remember(glamourId) { mutableStateOf(0L) }
    var busy by remember(glamourId) { mutableStateOf(false) }
    // 收藏夹超过一个时要选一个：(id, 名字, 是否默认)。
    var folderPick by remember(glamourId) { mutableStateOf<List<Triple<String, String, Boolean>>?>(null) }
    // 分享卡预览。和移动端一样：先给人看一眼生成的图，再决定发不发。
    var shareCardOpen by remember(glamourId) { mutableStateOf(false) }
    val actionScope = rememberCoroutineScope()
    LaunchedEffect(glamourId) {
        val detail = ShizhijiaApi.getGlamourDetail(context, glamourId)
        g = detail
        if (detail != null) {
            liked = detail.isLike
            favorited = detail.isFavorite
            likeNum = detail.likes.toLong()
            favNum = detail.favorites.toLong()
        }
    }

    val slotLabels = SzjSlotLabels
    val leftSlots = SzjLeftSlots
    val rightSlots = SzjRightSlots

    ScreenFrame(background = SzjBg) {
        SzjHeader("幻化详情", onBack = pop)
        val d = g
        if (d == null) {
            Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SzjShimmerBox(Modifier.size(32.dp), RoundedCornerShape(16.dp))
                    Spacer(Modifier.width(9.dp))
                    SzjShimmerBox(Modifier.width(100.dp).height(13.dp), RoundedCornerShape(4.dp))
                }
                Spacer(Modifier.height(12.dp))
                SzjShimmerBox(Modifier.fillMaxWidth().aspectRatio(9f / 14f), SzjCardShape)
            }
            return@ScreenFrame
        }
        // weight(1f) 而不是 fillMaxSize()：这是在 ScreenFrame 的 Column 里，
        // fillMaxSize 会把剩余高度全吃掉，下面的动作条被挤成 0 高——
        // 上一版就是这样，赞/收藏做了但根本看不见。
        LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
            item {
                SzjCardSurface(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = SzjInnerShape,
                    onClick = if (d.authorUuid.isNotBlank()) ({ nav(SzjRoute.UserProfile(d.authorUuid)) }) else null,
                ) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        SzjAvatar(d.authorName, d.authorAvatar, d.authorUuid, 32)
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text(d.authorName, color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            val line = listOf(d.areaName, d.groupName).filter { it.isNotBlank() }.joinToString(" ")
                            if (line.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(13); Text(line, color = SzjMuted, style = SzjMetaStyle) }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            item {
                if (d.images.isNotEmpty()) {
                    // Swipeable full-aspect pager: each page shows the complete
                    // picture, no cropping and no need to tap for full view.
                    val pagerState = androidx.compose.foundation.pager.rememberPagerState { d.images.size }
                    androidx.compose.foundation.pager.HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                    ) { page ->
                        ShizhijiaRemoteImage(
                            url = d.images[page],
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth,
                            showPlaceholder = true,
                            // 传整组图和当前页：点开之后在查看器里还能左右滑，
                            // 不用退出去再点下一张。
                            onClick = { _ -> SzjViewer.open(d.images, page) },
                        )
                    }
                    if (d.images.size > 1) {
                        Spacer(Modifier.height(8.dp))
                        // 页码指示改成小点，当前页用水晶青实心。
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            repeat(d.images.size.coerceAtMost(12)) { i ->
                                val on = i == pagerState.currentPage
                                val w by animateFloatAsState(if (on) 14f else 5f, SzjMorphSpring, label = "szjDot")
                                Box(
                                    Modifier.padding(horizontal = 2.5.dp)
                                        .width(w.dp).height(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(if (on) SzjAccentFill else SzjHairline)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
            item {
                SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                  Column(Modifier.padding(14.dp)) {
                    Text(d.title, color = SzjText, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val genderText = when (d.gender) { 1 -> "男性"; 2 -> "女性"; else -> "" }
                        val rg = (d.races + listOfNotNull(genderText.takeIf { it.isNotBlank() })).joinToString(" / ")
                        if (rg.isNotBlank()) {
                            Text(rg, color = SzjOnAccentSoft, style = SzjMetaStyle,
                                modifier = Modifier.clip(SzjChipShape).background(SzjAccentSoft).padding(horizontal = 8.dp, vertical = 3.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(d.createdAt.take(10), color = SzjMuted, style = SzjMetaStyle)
                    }
                    if (d.desc.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(d.desc, color = SzjText, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                    if (d.jobs.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("适用职业", color = SzjMuted, style = SzjMetaStyle)
                        Spacer(Modifier.height(3.dp))
                        Text(d.jobs.joinToString("、"), color = SzjText, fontSize = 12.sp, lineHeight = 18.sp)
                    }
                  }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("装备与染色", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
            }
            val rowsCount = maxOf(leftSlots.size, rightSlots.size)
            items(rowsCount) { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf(leftSlots.getOrNull(row), rightSlots.getOrNull(row)).forEach { slot ->
                        Box(Modifier.weight(1f)) {
                            if (slot == null) {
                                Spacer(Modifier.height(1.dp))
                            } else {
                                val label = slotLabels[slot] ?: slot
                                val equip = d.equips.firstOrNull { it.slot == slot }
                                val extraName = when (slot) { "GLASSES" -> d.glassesName; "ORNAMENT" -> d.ornamentName; else -> "" }
                                val extraIcon = when (slot) { "GLASSES" -> d.glassesIconUrl; "ORNAMENT" -> d.ornamentIconUrl; else -> "" }
                                Column {
                                    Text(label, color = SzjMuted, style = SzjMetaStyle)
                                    Spacer(Modifier.height(5.dp))
                                    if (equip == null && extraName.isBlank()) {
                                        // 空槽位画一条虚化的浅底，别用实心块看着像有内容。
                                        Box(Modifier.fillMaxWidth().height(46.dp).clip(SzjInnerShape).background(SzjCardRaised.copy(alpha = 0.45f)))
                                    } else {
                                        val eName = equip?.name ?: extraName
                                        val eIcon = equip?.iconUrl ?: extraIcon
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (eIcon.isNotBlank()) {
                                                Box {
                                                    ShizhijiaRemoteImage(url = eIcon, modifier = Modifier.size(40.dp).clip(SzjChipShape))
                                                        if (equip?.isMallItem == true) {
                                                            // 商城角标：预渲染的整图（黄底圆+购物袋），直接 Image 显示，密度无关。
                                                            val ctx = LocalContext.current
                                                            val badge = remember {
                                                                runCatching {
                                                                    android.graphics.BitmapFactory.decodeStream(ctx.assets.open("mall_badge.png"))
                                                                }.getOrNull()
                                                            }
                                                            if (badge != null) {
                                                                Image(
                                                                    bitmap = badge.asImageBitmap(),
                                                                    contentDescription = null,
                                                                    contentScale = ContentScale.Fit,
                                                                    modifier = Modifier
                                                                        .align(Alignment.TopEnd)
                                                                        .offset(x = 5.dp, y = (-5).dp)
                                                                        .size(18.dp),
                                                                )
                                                            }
                                                        }
                                                }
                                                Spacer(Modifier.width(8.dp))
                                            }
                                            Column {
                                                Text(eName, color = SzjText, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                Spacer(Modifier.height(2.dp))
                                                val holeCount = maxOf(equip?.dyeHoleCount ?: 0, 0)
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    for (hi in 0 until holeCount) {
                                                        val dy = equip?.dyes?.getOrNull(hi)
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            if (dy != null) {
                                                                // 和分享卡共用 szjDyeColor（原来这儿内联写了一遍同样的解析）
                                                                val dyeColor = szjDyeColor(dy.color) ?: SzjCardRaised
                                                                Box(Modifier.size(10.dp).clip(CircleShape).background(dyeColor).border(0.5.dp, SzjMuted, CircleShape))
                                                                Spacer(Modifier.width(3.dp))
                                                                Text(dy.name.removeSuffix("染剂"), color = SzjMuted, fontSize = 10.sp, maxLines = 1)
                                                            } else {
                                                                // 「无」就够 —— 旁边已经有孔位图标，
                                                                // 「无染色」三个字在一行两个孔的布局里挤掉部件名。
                                                                ImageGlyph(R.drawable.ic_block, SzjMuted, Modifier.size(10.dp))
                                                                Spacer(Modifier.width(2.dp))
                                                                Text("无", color = SzjMuted, fontSize = 10.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                // 计数原来只是两个静态数字。现在真正的赞/收藏在底部动作条上，
                // 这里不再重复显示同一组数。
                Spacer(Modifier.height(20.dp))
            }
        }
        // 底部动作条：赞 / 收藏 / 分享。
        SzjGlamourActionBar(
            liked = liked, likeNum = likeNum,
            favorited = favorited, favNum = favNum,
            busy = busy,
            onLike = {
                if (!busy) actionScope.launch {
                    busy = true
                    when (val r = ShizhijiaApi.likeGlamour(context, glamourId)) {
                        is ShizhijiaApi.Res.Ok -> {
                            val on = r.value == ShizhijiaApi.Toggle.On
                            liked = on
                            likeNum = (likeNum + if (on) 1 else -1).coerceAtLeast(0)
                        }
                        else -> szjToastWriteFail(context, r, nav)
                    }
                    busy = false
                }
            },
            onFavorite = {
                if (!busy) actionScope.launch {
                    busy = true
                    if (favorited) {
                        // 已收藏 → 取消。取消不需要收藏夹 id。
                        when (val r = ShizhijiaApi.cancelFavoriteGlamour(context, glamourId)) {
                            is ShizhijiaApi.Res.Ok -> {
                                favorited = false
                                favNum = (favNum - 1).coerceAtLeast(0)
                                android.widget.Toast.makeText(context, r.value, android.widget.Toast.LENGTH_SHORT).show()
                            }
                            else -> szjToastWriteFail(context, r, nav)
                        }
                    } else {
                        // 未收藏 → 收藏要先有收藏夹。官网的做法：查一次收藏夹列表，
                        // 只有一个且是默认夹就直接用它，否则让人选。
                        when (val list = ShizhijiaApi.glamourFavorites(context, page = 1, limit = 50)) {
                            is ShizhijiaApi.Res.Ok -> {
                                val folders = list.value
                                when {
                                    folders.isEmpty() -> android.widget.Toast.makeText(
                                        context,
                                        "还没有收藏夹，先在石之家网页版建一个",
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                    folders.size == 1 -> {
                                        val r = ShizhijiaApi.favoriteGlamour(context, glamourId, folders[0].first)
                                        if (r is ShizhijiaApi.Res.Ok) {
                                            favorited = true
                                            favNum += 1
                                            android.widget.Toast.makeText(context, r.value, android.widget.Toast.LENGTH_SHORT).show()
                                        } else szjToastWriteFail(context, r, nav)
                                    }
                                    else -> folderPick = folders
                                }
                            }
                            else -> szjToastWriteFail(context, list, nav)
                        }
                    }
                    busy = false
                }
            },
            // 分享**生成图片**，和石之家移动端一致（它把详情里的预览节点交给
            // html2canvas 栅格化，没有服务端接口）。上一版我发的是一条链接文本，
            // 是我没去看移动端就下的结论。
            onShare = { shareCardOpen = true },
        )
    }
    g?.let { detail ->
        if (shareCardOpen) {
            SzjGlamourShareSheet(d = detail, onDismiss = { shareCardOpen = false })
        }
    }
    // 多个收藏夹时选一个。
    folderPick?.let { folders ->
        AlertDialog(
            onDismissRequest = { folderPick = null },
            title = { Text("收藏到", color = SzjText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    folders.forEach { (fid, fname, isDefault) ->
                        Row(
                            Modifier.fillMaxWidth().clip(SzjInnerShape).clickable {
                                folderPick = null
                                actionScope.launch {
                                    busy = true
                                    val r = ShizhijiaApi.favoriteGlamour(context, glamourId, fid)
                                    if (r is ShizhijiaApi.Res.Ok) {
                                        favorited = true
                                        favNum += 1
                                        android.widget.Toast.makeText(context, r.value, android.widget.Toast.LENGTH_SHORT).show()
                                    } else szjToastWriteFail(context, r, nav)
                                    busy = false
                                }
                            }.padding(horizontal = 4.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(fname.ifBlank { "未命名收藏夹" }, color = SzjText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            if (isDefault) Text("默认", color = SzjMuted, style = SzjMetaStyle)
                        }
                    }
                }
            },
            confirmButton = {
                Text(
                    "取消",
                    color = SzjMuted, fontSize = 14.sp,
                    modifier = Modifier.clip(SzjInnerShape).clickable { folderPick = null }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            },
            containerColor = SzjCardRaised,
        )
    }
}

// 装备槽位的字典和左右两列顺序。**详情页和分享卡共用这一份**——
// 原来它们是详情页里的局部变量，分享卡只好自己按 equips 的原始顺序列，
// 于是分享出去的图既没有槽位名也没有染色。
internal val SzjSlotLabels = mapOf(
    "MAIN_HAND" to "主手", "OFF_HAND" to "副手", "HEAD" to "头部", "EARS" to "耳坠",
    "BODY" to "上衣", "NECK" to "项链", "GLOVES" to "手部", "WRISTS" to "手镯",
    "LEGS" to "腿部", "FINGER_LEFT" to "戒指", "FEET" to "脚部", "FINGER_RIGHT" to "戒指",
    "GLASSES" to "面部配饰", "ORNAMENT" to "时尚配饰",
)
internal val SzjLeftSlots = listOf("MAIN_HAND", "HEAD", "BODY", "GLOVES", "LEGS", "FEET", "GLASSES")
internal val SzjRightSlots = listOf("OFF_HAND", "EARS", "NECK", "WRISTS", "FINGER_LEFT", "FINGER_RIGHT", "ORNAMENT")

/**
 * 分享卡的搭配清单**按类别分组**，不用上面那两列。
 *
 * 上面 `SzjLeftSlots`/`SzjRightSlots` 是详情页用的左右两列（对应人物立绘两侧
 * 的装备位），在**并排两列**的清单里逐行配对会配出
 * `头部 | 耳坠`、`上衣 | 项链`、`腿部 | 戒指` —— 防具和首饰交替出现，
 * 读的人得在两种类别之间来回跳。
 *
 * 分享卡的清单是给人照着抄搭配的，所以按**武器 / 防具 / 首饰**分段，
 * 和游戏自己的装备栏一致。段内仍是两列（横向空间只够两列）。
 */
private val SzjShareGroups: List<Pair<String, List<String>>> = listOf(
    "武器" to listOf("MAIN_HAND", "OFF_HAND"),
    "防具" to listOf("HEAD", "BODY", "GLOVES", "LEGS", "FEET"),
    "首饰" to listOf("EARS", "NECK", "WRISTS", "FINGER_LEFT", "FINGER_RIGHT"),
    "配饰" to listOf("GLASSES", "ORNAMENT"),
)

/** 染剂色字符串（"#RRGGBB"）转 Color，解不出来给 null。 */
private fun szjDyeColor(raw: String): Color? =
    raw.takeIf { it.startsWith("#") }
        ?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }

/**
 * 分享卡里一个槽位要画的东西：名字、图标、染色。
 *
 * 面部配饰和时尚配饰**不在 equips 里**（服务端放在 ortInfo），
 * 所以要单独取——原来的分享卡只遍历 equips，这两样必然漏掉。
 */
private data class SzjShareSlotItem(
    val name: String,
    val iconUrl: String,
    /** 按孔位定长，空孔是 null。见 [ShizhijiaGlamourEquip.dyes]。 */
    val dyes: List<ShizhijiaGlamourDye?>,
    val dyeHoles: Int,
)

/** 取某个槽位的内容；这个槽没东西就返回 null（空槽不进分享图）。 */
private fun szjShareSlot(d: ShizhijiaGlamourDetail, slot: String): SzjShareSlotItem? {
    d.equips.firstOrNull { it.slot == slot && it.name.isNotBlank() }?.let {
        return SzjShareSlotItem(it.name, it.iconUrl, it.dyes, maxOf(it.dyeHoleCount, 0))
    }
    return when (slot) {
        "GLASSES" -> d.glassesName.takeIf { it.isNotBlank() }
            ?.let { SzjShareSlotItem(it, d.glassesIconUrl, emptyList(), 0) }
        "ORNAMENT" -> d.ornamentName.takeIf { it.isNotBlank() }
            ?.let { SzjShareSlotItem(it, d.ornamentIconUrl, emptyList(), 0) }
        else -> null
    }
}

/**
 * 分享卡里的一个槽位格：槽位名 + 图标 + 部件名 + 染色点。
 *
 * 比详情页那版紧凑（图标 26dp、字 10sp）——分享卡要在一屏里装完十几个槽，
 * 详情页的 40dp 图标放这儿会撑爆。
 *
 * **空孔要画。** 原来这里 `mapNotNull` 把没染的孔滤掉了，理由写的是
 * "分享图里每一行都该是信息" —— 但**孔位本身就是信息**：
 * 两个孔只染第 2 孔时，只画一个色点等于告诉人"染的是第 1 孔"，是错的。
 * 别人照着分享图去染就染错位置，而这正是分享图存在的意义。
 */
@Composable
private fun SzjShareSlotCell(slot: String, item: SzjShareSlotItem) {
    Row(verticalAlignment = Alignment.Top) {
        if (item.iconUrl.isNotBlank()) {
            ShizhijiaRemoteImage(
                url = item.iconUrl,
                modifier = Modifier.size(26.dp).clip(RoundedCornerShape(5.dp)),
                showPlaceholder = false,
                collapseOnFail = false,
            )
            Spacer(Modifier.width(6.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(SzjSlotLabels[slot] ?: slot, color = SzjMuted, fontSize = 9.sp, maxLines = 1)
            Text(
                item.name, color = SzjText, fontSize = 10.sp,
                lineHeight = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            // 染色：有几个孔就画几个，**空孔画成空心圈 + 「无」**，
            // 这样孔位对得上（见上面注释）。
            if (item.dyeHoles > 0) {
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    for (hi in 0 until item.dyeHoles) {
                        val dy = item.dyes.getOrNull(hi)?.takeIf { it.name.isNotBlank() }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (dy != null) {
                                Box(
                                    Modifier.size(8.dp).clip(CircleShape)
                                        .background(szjDyeColor(dy.color) ?: SzjCardRaised)
                                        .border(0.5.dp, SzjMuted.copy(alpha = .6f), CircleShape),
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    dy.name.removeSuffix("染剂"), color = SzjMuted, fontSize = 9.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                // 空心圈：和实心色点一样大，一眼能看出这一孔是空的。
                                Box(
                                    Modifier.size(8.dp).clip(CircleShape)
                                        .border(0.5.dp, SzjMuted.copy(alpha = .6f), CircleShape),
                                )
                                Spacer(Modifier.width(2.dp))
                                Text("无", color = SzjMuted, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
    }
}


/**
 * 幻化分享卡的**版面本体**。
 *
 * 尺寸照移动站：整卡 720dp 宽（站点 750px），左边 340dp 放整张竖图
 * （ContentScale.Fit —— 对应 object-fit: contain，**不裁**），右边并排放
 * 标题、作者和搭配清单。所以是**横版**，竖图能完整显示、清单也装得下。
 *
 * 调用方负责把它按屏宽缩小显示（见 SzjGlamourShareSheet 里的 layout{}），
 * 这里只管按固定尺寸排好版。
 */
@Composable
private fun SzjGlamourShareCard(
    d: ShizhijiaGlamourDetail,
    layer: androidx.compose.ui.graphics.layer.GraphicsLayer,
) {
            Column(
                Modifier.fillMaxWidth()
                    .clip(SzjCardShape)
                    .drawWithContent {
                        layer.record { this@drawWithContent.drawContent() }
                        drawLayer(layer)
                    }
                    .background(SzjCard),
            ) {
                // **横版：左图右清单。** 照移动站的 750×778（图 375×670 contain）。
                //
                // 上一版我做成竖版 + 把头图裁到 200dp，两个后果：头图被切掉大半
                // （幻化图是竖的，裁成 16:9 只剩中间一条），清单也被挤。
                // 并排之后竖图能整张显示（Fit = contain，不裁），清单在右边有独立
                // 的一列高度可用。
                Row(Modifier.fillMaxWidth()) {
                    if (d.images.isNotEmpty()) {
                        // 340×610 ≈ 站点的 375×670 同比例。Fit 不裁，
                        // 图比框窄时两侧留底色，比切掉内容好。
                        ShizhijiaRemoteImage(
                            url = d.images.first(),
                            modifier = Modifier.width(340.dp).height(610.dp),
                            contentScale = ContentScale.Fit,
                            showPlaceholder = true,
                        )
                    }
                    // **不写死高度。** 原来是 `height(610.dp)`（照左边那张图的高度），
                    // 但右边要装标题 + 作者 + 种族条 + 分隔线 + 7 行槽位，
                    // 任何一个部件名折成两行就超出 610dp —— Column 不滚动，
                    // **超出的部分直接被裁掉**，最后一行（面部配饰）就没了。
                    // 改成让内容决定高度：整卡跟着变高。这张图是离屏栅格化的，
                    // 高一点没有代价，被裁掉信息才有代价。
                    Column(
                        Modifier.weight(1f)
                            .heightIn(min = 610.dp)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                    Text(
                        d.title,
                        color = SzjText, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        lineHeight = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(9.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SzjAvatar(d.authorName, d.authorAvatar, d.authorUuid, 26)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.authorName, color = SzjText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            val where = listOf(d.areaName, d.groupName).filter { it.isNotBlank() }.joinToString(" ")
                            if (where.isNotBlank()) Text(where, color = SzjMuted, style = SzjMetaStyle)
                        }
                    }
                    val genderText = when (d.gender) { 1 -> "男性"; 2 -> "女性"; else -> "" }
                    val rg = (d.races + listOfNotNull(genderText.takeIf { it.isNotBlank() })).joinToString(" / ")
                    if (rg.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            rg, color = SzjOnAccentSoft, style = SzjMetaStyle,
                            modifier = Modifier.clip(SzjChipShape).background(SzjAccentSoft)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    // 搭配清单：**这是这套幻化真正要传达的东西**，
                    // 只发一张外观图别人还得追问"用的什么部件、染的什么色"。
                    //
                    // 原来这儿是按 equips 原始顺序两列平铺、只有名字：没有槽位名
                    // （分不清哪件是上衣哪件是腿）、**没有染色**、也漏了面部/时尚配饰
                    // （那两个不在 equips 里，是 ortInfo 单独给的）。
                    // 现在和详情页共用槽位字典，按左右两列的固定顺序走，
                    // 只画有内容的槽——空槽在分享图里没有意义。
                    // **按类别分段**（武器/防具/首饰/配饰），见 SzjShareGroups。
                    // 原来是拿详情页的左右两列逐行配对，配出来是
                    // `头部 | 耳坠`、`上衣 | 项链` —— 防具和首饰交替，
                    // 照着抄搭配的人得在两种类别之间来回跳。
                    val groups = remember(d.id) {
                        SzjShareGroups.mapNotNull { (title, slots) ->
                            val items = slots.mapNotNull { s -> szjShareSlot(d, s)?.let { s to it } }
                            if (items.isEmpty()) null else title to items
                        }
                    }
                    if (groups.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
                        groups.forEach { (title, items) ->
                            Spacer(Modifier.height(9.dp))
                            Text(
                                title,
                                color = SzjMuted,
                                fontSize = 9.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(5.dp))
                            // 段内两列。**按有内容的槽两两成行**，不是按固定位置——
                            // 固定位置在段内会留空洞（比如只有戒指左没有戒指右）。
                            items.chunked(2).forEach { pair ->
                                Row(
                                    Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    pair.forEach { (slot, item) ->
                                        Box(Modifier.weight(1f)) {
                                            SzjShareSlotCell(slot, item)
                                        }
                                    }
                                    // 单数时补一个空位，否则那一个会被拉成整行宽
                                    if (pair.size == 1) Box(Modifier.weight(1f)) {}
                                }
                            }
                        }
                    }
                    }
                }
                // 出处放在整卡最底：图会脱离 App 传播，没有这行不知道哪来的。
                Text(
                    "石之家 · 艾欧泽亚终端",
                    color = SzjMuted, style = SzjMetaStyle,
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 10.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
}

/**
 * 幻化分享卡的预览 + 生成。
 *
 * 石之家移动端的做法：把详情页里排好的预览节点交给 html2canvas 栅格化成 PNG，
 * 再弹图片预览让人保存/分享——**没有服务端接口**。
 * 这里对应物是 GraphicsLayer：把这张专门排的卡录一层，`toImageBitmap()` 出图。
 *
 * 渲的是专门排的卡，不是整屏截图：屏幕上那一版有动作条、滚动位置、
 * 系统栏，截出来是"截图"；这张卡是为"发出去被人看"排的版。
 */
@Composable
private fun SzjGlamourShareSheet(
    d: ShizhijiaGlamourDetail,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    var sharing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!sharing) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = .55f))
                .clickable(enabled = !sharing) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // **卡片按固定尺寸排版，再缩小显示。**
                //
                // 移动站的分享卡是 750×778 —— **横的**：左边 375×670 放整张竖图
                // （object-fit: contain，不裁），右边并排放清单。所以竖图能完整
                // 显示，清单也装得下。我原来是竖版卡 + 裁到 200dp 的头图，
                // 于是头图被切、清单也挤。
                //
                // 手机屏只有 360dp 宽，直接按 720dp 排会超出屏幕。做法是：
                // 用 layout{} 按固定宽度**量**，对外**汇报缩放后的尺寸**，
                // 放置时套一个缩放图层。这样：
                //   · 屏幕上看到的是缩小版（放得下）
                //   · layer.record 录的是**全尺寸**那一份（出图清晰）
                // 站点也是这个路子：html2canvas 对一个 750px 的离屏节点渲染。
                val cardW = 720.dp
                val density = LocalDensity.current
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val avail = maxWidth
                    val k = (avail / cardW).coerceAtMost(1f)
                    Box(
                        Modifier.layout { measurable, _ ->
                            val wPx = with(density) { cardW.roundToPx() }
                            val placeable = measurable.measure(
                                androidx.compose.ui.unit.Constraints(minWidth = wPx, maxWidth = wPx),
                            )
                            // 对外只占缩放后的大小，否则父布局会按 720dp 算宽度、被屏幕裁掉。
                            layout(
                                (placeable.width * k).toInt(),
                                (placeable.height * k).toInt(),
                            ) {
                                placeable.placeWithLayer(0, 0) {
                                    scaleX = k
                                    scaleY = k
                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                                }
                            }
                        },
                    ) {
                        SzjGlamourShareCard(d, layer)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "取消",
                        color = Color.White.copy(alpha = .85f), fontSize = 14.sp,
                        modifier = Modifier.clip(SzjInnerShape).clickable(enabled = !sharing) { onDismiss() }
                            .padding(horizontal = 18.dp, vertical = 11.dp),
                    )
                    // 保存到相册。**之前只有"分享"**——分享是把图交给别的 App，
                    // 想自己留一张反而没有办法（走系统分享再挑"保存到相册"要绕，
                    // 而且不是每台机器都有那个选项）。
                    // 用的是公共的 SaveImage（图片长按保存那套），不另写一遍写相册。
                    SzjPressable(
                        onClick = {
                            if (sharing || saving) return@SzjPressable
                            saving = true
                            scope.launch {
                                val err = runCatching {
                                    val bmp = layer.toImageBitmap().asAndroidBitmap()
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                        SaveImage.toGallery(
                                            context, bmp,
                                            d.title.ifBlank { "glamour-${d.id}" },
                                        )
                                    }
                                }.getOrElse { it.message?.take(50) ?: "保存失败" }
                                saving = false
                                android.widget.Toast.makeText(
                                    context,
                                    err ?: "已保存到相册",
                                    android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                        shape = SzjInnerShape,
                    ) {
                        Row(
                            Modifier.clip(SzjInnerShape)
                                .border(1.dp, Color.White.copy(alpha = .45f), SzjInnerShape)
                                .padding(horizontal = 15.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (saving) {
                                CircularProgressIndicator(
                                    color = Color.White, strokeWidth = 2.dp,
                                    modifier = Modifier.size(14.dp),
                                )
                            } else {
                                ImageGlyph(R.drawable.ic_download, Color.White, Modifier.size(15.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(if (saving) "保存中" else "保存", color = Color.White, fontSize = 14.sp)
                        }
                    }
                    SzjPrimaryButton(
                        if (sharing) "生成中…" else "分享图片",
                        onClick = {
                            if (sharing) return@SzjPrimaryButton
                            sharing = true
                            scope.launch {
                                val r = SzjShareImage.shareLayer(
                                    context, layer,
                                    name = d.title.ifBlank { "glamour-${d.id}" },
                                    text = d.title,
                                )
                                sharing = false
                                if (r.isSuccess) onDismiss()
                                else android.widget.Toast.makeText(
                                    context, "图片没生成成功", android.widget.Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )
                }
            }
        }
    }
}

/** 幻化详情底部的动作条。分享把这套幻化渲成一张图（同石之家移动端）。 */
@Composable
private fun SzjGlamourActionBar(
    liked: Boolean,
    likeNum: Long,
    favorited: Boolean,
    favNum: Long,
    busy: Boolean,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onShare: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(SzjLine))
        Row(
            Modifier.fillMaxWidth().background(SzjCardRaised).padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SzjActionCell(
                icon = if (liked) R.drawable.ic_heart else R.drawable.ic_heart_outline,
                label = if (likeNum > 0) formatCount(likeNum) else "赞",
                on = liked, enabled = !busy, modifier = Modifier.weight(1f), onClick = onLike,
            )
            SzjActionCell(
                icon = if (favorited) R.drawable.ic_star_filled else R.drawable.ic_star_outline,
                label = if (favNum > 0) formatCount(favNum) else "收藏",
                on = favorited, enabled = !busy, modifier = Modifier.weight(1f), onClick = onFavorite,
            )
            SzjActionCell(
                icon = R.drawable.ic_send_arrow,
                label = "分享",
                on = false, enabled = !busy, modifier = Modifier.weight(1f), onClick = onShare,
            )
        }
    }
}


/** 底栏「幻化」：关注/全部 + 推荐/最新 + 双列卡片流（仿官方布局）。 */
@Composable
private fun ShizhijiaGlamourTab(
    nav: (SzjRoute) -> Unit,
    loggedIn: Boolean,
    gs: SzjGlamourState,
    header: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by gs.tab        // 0=全部 1=关注
    var sort by gs.sort      // 0=推荐 1=最新 2=热门
    var items by gs.items
    var loading by gs.loading
    var page by gs.page
    var ended by gs.ended
    val gridState = gs.gridState
    // 筛选: 种族 / 性别 / 发布时间（服务端）
    var raceId by gs.raceId
    var genderId by gs.genderId
    var createTimeIdx by gs.createTimeIdx
    var filterOpen by gs.filterOpen
    // 职业筛（客户端，见 SzjGlamourState.jobId）
    var jobId by gs.jobId
    var jobExclusive by gs.jobExclusive
    // 筛选面板盖在幻化流上面：返回键先收面板，不能一路退出石之家。
    BackHandler(enabled = filterOpen) { filterOpen = false }
    val createTimeValues = listOf("all", "last24H", "lastWeek", "lastMonth")
    // order 只认这三个，别的静默回退到 latest（详见 ShizhijiaApi.getGlamours）。
    val orderValues = listOf("", "latest", "hottest")

    fun load(reset: Boolean) {
        if (loading) return
        if (reset) { page = 1; ended = false; items = emptyList(); gs.jobAutoPages.value = 0 }
        loading = true
        scope.launch {
            val next = if (tab == 1) ShizhijiaApi.getFollowGlamours(context, page)
            else ShizhijiaApi.getGlamours(context, page, order = orderValues.getOrElse(sort) { "" }, raceId = raceId, genderId = genderId, createTime = createTimeValues[createTimeIdx])
            // 翻页可能撞上重复行（推荐流尤其）——按 id 去重，否则 items(key=id) 会崩。
            val seen = items.mapTo(mutableSetOf()) { it.id }
            items = items + next.filter { seen.add(it.id) }
            if (next.isEmpty()) ended = true else page += 1
            loading = false
        }
    }
    // Reload only when the channel/filters changed; returning from a detail
    // page keeps the loaded feed, scroll position and active tab.
    // jobId / jobExclusive 故意不在这里：它们是客户端筛，重拉一遍没有意义。
    LaunchedEffect(tab, sort, raceId, genderId, createTimeIdx) {
        val key = "$tab-$sort-$raceId-$genderId-$createTimeIdx"
        if (gs.loadedKey.value != key || items.isEmpty()) {
            gs.loadedKey.value = key
            load(reset = true)
        }
    }

    // 职业筛在本地过一遍。通用款默认留着 —— 最新流里约七成帖没有 job_ids，
    // 那是"作者没选主手"，不是"数据缺失"，把它们滤掉等于筛完只剩零星几条。
    val shown = remember(items, jobId, jobExclusive) {
        if (jobId < 0) items
        else items.filter { if (it.universalJob) !jobExclusive else jobId in it.jobIds }
    }
    val hiddenByJob = items.size - shown.size
    // 整页被职业筛滤空时主动再拉一页 —— 和推荐流的"这一页都被你屏蔽了"
    // 走同一个套路（见本文件 2200 行附近）：列表空着就滚不动，
    // 滚动触发的分页永远不会被叫到。
    //
    // 但这里**加了上限**，那边没有：屏蔽版块最多滤掉几个版块，
    // 而勾上「只看专属」之后实测最新流 22 个职业里有 9 个连一条都凑不出
    // （绝枪战士、学者、贤者…）。不封顶就会从头翻到尽头。
    LaunchedEffect(shown.size, items.size, loading, jobId) {
        if (shown.isNotEmpty() || items.isEmpty()) return@LaunchedEffect
        if (loading || ended || jobId == -1) return@LaunchedEffect
        if (gs.jobAutoPages.value < 5) {
            gs.jobAutoPages.value += 1
            load(reset = false)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // 瀑布流没有 stickyHeader，所以品牌行和控制行都作为整行项放进网格，
            // 一起随内容滑走——和社区分区的行为保持一致。
            val fullLine = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine
            androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid(
                columns = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp,
            ) {
                item(key = "szj-header", span = fullLine) { header() }
                item(key = "glamour-controls", span = fullLine) {
                    Column(Modifier.fillMaxWidth()) {
                        // 幻化顶栏：全部/关注走棱条 Tab，筛选和排序在右侧。
                        // 原来"关注"排在"全部"左边，和默认落在"全部"矛盾，这里换回自然顺序。
                        Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            SzjSubTab("全部", tab == 0) { tab = 0 }
                            Spacer(Modifier.width(4.dp))
                            SzjSubTab("关注", tab == 1) { tab = 1 }
                            Spacer(Modifier.weight(1f))
                            // 有筛选生效时按钮变实心，让"我筛过了"这件事有痕迹。
                            val filtered = raceId != -1 || genderId != -1 || createTimeIdx != 0 || jobId != -1
                            SzjPressable(onClick = { filterOpen = !filterOpen }, shape = SzjChipShape) {
                                Row(
                                    Modifier.clip(SzjChipShape)
                                        .background(if (filtered) SzjAccentFill else SzjCardRaised)
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("筛选", style = SzjLabelStyle, color = if (filtered) SzjOnAccent else SzjMuted)
                                }
                            }
                        }
                        if (tab == 0) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // 发布按钮放这一行**左端**，不用右下角那个悬浮位。
                                // 理由（用户指定的位置，也讲得通）：幻化是瀑布流，
                                // 右下角的悬浮钮会一直压在图片上；而这一行本来右边
                                // 只有排序、左边整段空着，正好安置。
                                SzjPressable(onClick = { nav(SzjRoute.PublishGlamour) }, shape = SzjChipShape) {
                                    Row(
                                        Modifier.clip(SzjChipShape).background(SzjAccentFill)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        ImageGlyph(R.drawable.ic_add, SzjOnAccent, Modifier.size(13.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("发幻化", color = SzjOnAccent, style = SzjLabelStyle)
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Row(Modifier.clip(SzjChipShape).background(SzjCardRaised)) {
                                    // 「热门」是官网有、我们一直没接的第三种序（order=hottest）。
                                    // 站点只有这三种，没有"按点赞排"——那些参数名全部回退到最新。
                                    listOf("推荐" to 0, "最新" to 1, "热门" to 2).forEach { (label, id) ->
                                        SzjSmallOption(label, sort == id) { if (sort != id) sort = id }
                                    }
                                }
                            }
                        }
                        // 职业筛是本地筛，用户看到的条数会比服务端给的少，这里交代一句，
                        // 否则"我明明滑了很久却只有几张"看起来像 bug。
                        if (tab == 0 && jobId != -1) {
                            Row(
                                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    buildString {
                                        append(WikiDicts.jobName(jobId))
                                        if (jobExclusive) append("专属")
                                        append(" ${shown.size} 条")
                                        if (hiddenByJob > 0) append("，隐去 $hiddenByJob 条")
                                    },
                                    color = SzjMuted, fontSize = 11.sp,
                                )
                                Spacer(Modifier.weight(1f))
                                SzjPressable(onClick = { jobId = -1; gs.jobRole.value = ""; jobExclusive = false }, shape = SzjChipShape) {
                                    Text(
                                        "清除", color = SzjMuted, fontSize = 11.sp,
                                        modifier = Modifier.clip(SzjChipShape).background(SzjCardRaised)
                                            .padding(horizontal = 9.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                if (tab == 1 && !loggedIn) {
                    item(key = "glamour-login", span = fullLine) {
                        SzjEmptyInline(
                            "登录后能看关注的人的幻化",
                            "先在「我」里登录石之家账号",
                        ) { SzjPrimaryButton("登录", onClick = { nav(SzjRoute.Login) }) }
                    }
                } else if (loading && items.isEmpty()) {
                    // 瀑布流骨架：两列 9:16 占位，和真卡片同比例。
                    item(key = "glamour-skeleton", span = fullLine) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            repeat(2) {
                                SzjShimmerBox(Modifier.weight(1f).aspectRatio(9f / 16f), SzjCardShape)
                            }
                        }
                    }
                } else {
                    if (tab == 0) {
                        // 「光之收藏家」标识占瀑布流左列第一格，和幻化卡片同宽，
                        // 不跨列——跨列会把两列的卡片一起压下去。
                        item(key = "glamour-banner") { SzjGlamourBannerCard() }
                    }
                    items(shown.size, key = { shown[it].id }) { idx ->
                        SzjRise(idx) { SzjGlamourCardItem(shown[idx], nav) }
                    }
                    // 职业筛把这一页全滤掉了，但服务端还有货：给一个手动的出口，
                    // 不自动无限翻（窄职业可能要翻几十页才凑够一屏）。
                    if (jobId != -1 && !ended && !loading && gs.jobAutoPages.value >= 5) {
                        item(key = "glamour-job-more", span = fullLine) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(
                                    if (shown.isEmpty()) "翻了 ${items.size} 条都不是这个职业的"
                                    else "这个职业的不多，继续往后找？",
                                    color = SzjMuted, fontSize = 12.sp,
                                )
                                // 新帖大多没标职业，老帖标得全（最新流里带 job_ids 的约三成，
                                // 热门流里九成以上）。所以筛不到东西时，指路去热门比干等有用。
                                if (sort != 2) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "「热门」里标了职业的帖子多得多",
                                        color = SzjMuted, fontSize = 10.sp,
                                    )
                                }
                                Spacer(Modifier.height(9.dp))
                                SzjPressable(
                                    onClick = { gs.jobAutoPages.value = 0; load(reset = false) },
                                    shape = SzjChipShape,
                                ) {
                                    Text(
                                        "再找 5 页", color = SzjOnAccent, style = SzjLabelStyle,
                                        modifier = Modifier.clip(SzjChipShape).background(SzjAccentFill)
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                    )
                                }
                                if (sort != 2) {
                                    Spacer(Modifier.height(7.dp))
                                    SzjPressable(onClick = { sort = 2 }, shape = SzjChipShape) {
                                        Text(
                                            "换成热门", color = SzjMuted, fontSize = 11.sp,
                                            modifier = Modifier.clip(SzjChipShape)
                                                .border(1.dp, SzjHairline, SzjChipShape)
                                                .padding(horizontal = 14.dp, vertical = 7.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            // 滚动到底自动加载下一页。
            // 判定用 shown（画出来的），不是 items —— 职业筛开着时 items 有一堆
            // 但屏幕上没几张，拿 items 判会永远不触发。
            //
            val nearEnd by remember { derivedStateOf {
                val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                shown.isNotEmpty() && last >= shown.size - 3
            } }
            LaunchedEffect(nearEnd, loading, ended, jobId) {
                if (!nearEnd || loading || ended) return@LaunchedEffect
                // 没开职业筛就照常翻。开着的话记个数：滑到底也算一轮，
                // 和上面"整页被滤空"共用同一个 5 轮上限，
                // 否则冷门职业会一路把整个流翻到尽头。
                if (jobId == -1) { load(reset = false); return@LaunchedEffect }
                if (gs.jobAutoPages.value < 5) {
                    gs.jobAutoPages.value += 1
                    load(reset = false)
                }
            }
        }
        // 筛选面板: 从顶部滑下, 点击面板外区域自动收起。
        androidx.compose.animation.AnimatedVisibility(
            visible = filterOpen,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(160)),
        ) {
            val noRipple = remember { MutableInteractionSource() }
            Column(
                Modifier.fillMaxSize()
                    .background(Color(0x73000000))
                    .pointerInput(Unit) { detectTapGestures { filterOpen = false } }
            ) {
                // 面板本体单独做位移，遮罩只淡入，这样是"抽屉拉下来"而不是整块闪现。
                androidx.compose.animation.AnimatedVisibility(
                    visible = filterOpen,
                    enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { -it },
                    exit = slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { -it },
                ) {
                Column(
                    Modifier.fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(bottomEnd = 18.dp, bottomStart = 18.dp), ambientColor = Color(0x0D3C5A46), spotColor = Color(0x0D3C5A46))
                        .clip(RoundedCornerShape(bottomEnd = 18.dp, bottomStart = 18.dp))
                        .background(SzjBg)
                        .clickable(interactionSource = noRipple, indication = null) { }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    SzjFilterSection("种族", listOf("全部种族" to -1, "人族" to 1, "精灵族" to 2, "拉拉菲尔族" to 3, "猫魅族" to 4, "鲁加族" to 5, "敖龙族" to 6, "硌狮族" to 7, "维埃拉族" to 8), raceId) { raceId = it }
                    Spacer(Modifier.height(14.dp))
                    SzjFilterSection("性别", listOf("全部" to -1, "男性" to 1, "女性" to 2), genderId) { genderId = it }
                    Spacer(Modifier.height(14.dp))
                    SzjFilterSection("发布时间", listOf("全部" to 0, "24小时内" to 1, "最近一周" to 2, "最近一个月" to 3), createTimeIdx) { createTimeIdx = it }
                    Spacer(Modifier.height(14.dp))
                    SzjGlamourJobFilter(
                        jobId = jobId,
                        role = gs.jobRole.value,
                        exclusive = jobExclusive,
                        onRole = { gs.jobRole.value = it },
                        onJob = { jobId = it },
                        onExclusive = { jobExclusive = it },
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SzjPressable(
                            onClick = {
                                raceId = -1; genderId = -1; createTimeIdx = 0
                                jobId = -1; gs.jobRole.value = ""; jobExclusive = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = SzjInnerShape,
                        ) {
                            Text("重置", color = SzjMuted, style = SzjLabelStyle, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().clip(SzjInnerShape)
                                    .border(1.dp, SzjHairline, SzjInnerShape).padding(vertical = 11.dp))
                        }
                        SzjPressable(
                            // 只收面板，不强制重拉：chip 是即时改状态的，
                            // 种族/性别/时间一改上面那个 LaunchedEffect 就已经重拉过了。
                            // 原来这里清 loadedKey 再 load(reset=true)，等于**又拉一遍**；
                            // 加了客户端职业筛之后更明显 —— 只动职业也会白拉一整趟。
                            onClick = { filterOpen = false },
                            modifier = Modifier.weight(1f),
                            shape = SzjInnerShape,
                        ) {
                            Text("看结果", color = SzjOnAccent, style = SzjLabelStyle, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().clip(SzjInnerShape).background(SzjAccentFill).padding(vertical = 11.dp))
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun SzjFilterSection(label: String, options: List<Pair<String, Int>>, selected: Int, onSelect: (Int) -> Unit) {
    Column {
        // 筛选标题不带棱条：棱条只留给一级分区标题和选中态，
        // 撒在每个小标题上单个看不见、满屏又显得碎。
        Text(label, color = SzjText, style = SzjLabelStyle)
        Spacer(Modifier.height(9.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            options.chunked(4).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { (label2, id) ->
                        val on = selected == id
                        val bg by animateColorAsState(if (on) SzjAccent else SzjCardRaised, tween(200), label = "szjFilterBg")
                        val fg by animateColorAsState(if (on) SzjOnAccent else SzjMuted, tween(200), label = "szjFilterFg")
                        SzjPressable(onClick = { onSelect(id) }, shape = SzjChipShape) {
                            Text(label2, fontSize = 12.sp, fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                                color = fg,
                                modifier = Modifier.clip(SzjChipShape).background(bg)
                                    .padding(horizontal = 12.dp, vertical = 7.dp))
                        }
                    }
                }
            }
        }
    }
}
/**
 * 幻化筛选面板里的职业那一节。**两级**：先点定位，再点定位下的职业。
 *
 * 为什么不平铺：可筛职业 33 个，[SzjFilterSection] 一行 4 个要铺 9 行，
 * 把面板里其余三组条件全挤出屏幕（Wiki 的筛选面板踩过同一个坑，
 * 结论写在 [WikiDicts.jobRoles] 的注释里，这里照用同一套分组）。
 *
 * 「只看专属」那个开关只在选了职业之后出现 —— 没选职业时它没有意义。
 */
@Composable
private fun SzjGlamourJobFilter(
    jobId: Int,
    role: String,
    exclusive: Boolean,
    onRole: (String) -> Unit,
    onJob: (Int) -> Unit,
    onExclusive: (Boolean) -> Unit,
) {
    // 从别处（顶栏"清除"）改了 jobId 时，面板要能把定位回填出来。
    val shownRole = role.ifEmpty { if (jobId >= 0) WikiDicts.roleOfJob(jobId) else "" }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("职业", color = SzjText, style = SzjLabelStyle)
            Spacer(Modifier.width(7.dp))
            // 这一条是为了不让人以为"筛出来的就是全站结果"。
            Text("本地筛", color = SzjMuted, fontSize = 10.sp)
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            SzjFilterChip("不限", jobId == -1 && shownRole.isEmpty()) { onRole(""); onJob(-1) }
            WikiDicts.jobRoles.take(3).forEach { (name, _) ->
                SzjFilterChip(name, shownRole == name) { onRole(name) }
            }
        }
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            WikiDicts.jobRoles.drop(3).take(4).forEach { (name, _) ->
                SzjFilterChip(name, shownRole == name) { onRole(name) }
            }
        }
        if (shownRole.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                WikiDicts.jobsOfRole(shownRole).chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        row.forEach { (id, name) ->
                            SzjFilterChip(name, jobId == id) { onJob(if (jobId == id) -1 else id) }
                        }
                    }
                }
            }
        }
        if (jobId >= 0) {
            Spacer(Modifier.height(9.dp))
            SzjPressable(onClick = { onExclusive(!exclusive) }, shape = SzjChipShape) {
                Row(
                    Modifier.clip(SzjChipShape)
                        .background(if (exclusive) SzjAccent else SzjCardRaised)
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "只看专属（不含通用款）",
                        fontSize = 12.sp,
                        fontWeight = if (exclusive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (exclusive) SzjOnAccent else SzjMuted,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            // 这句是实测结论，不是猜的：最新流 72 行里 50 行没有 job_ids，
            // 抽 5 篇查详情，5 篇的主手都是空的。
            Text(
                "没选主手的幻化算通用款，谁都能穿；最新的帖子里大多是这种。",
                color = SzjMuted, fontSize = 10.sp, lineHeight = 14.sp,
            )
        }
    }
}

/** 筛选面板里的一颗 chip。抽出来是因为职业那一节要两级复用同一个样子。 */
@Composable
private fun SzjFilterChip(label: String, on: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) SzjAccent else SzjCardRaised, tween(200), label = "szjFilterBg")
    val fg by animateColorAsState(if (on) SzjOnAccent else SzjMuted, tween(200), label = "szjFilterFg")
    SzjPressable(onClick = onClick, shape = SzjChipShape) {
        Text(
            label, fontSize = 12.sp,
            fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
            color = fg,
            modifier = Modifier.clip(SzjChipShape).background(bg)
                .padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

/**
 * 幻化封面：先按（缓存的）真实宽高比占位，图片再慢慢加载填充。
 * 这样瀑布流的高度一开始就是正确的，快速滑动也不会因为图片加载晚而重排跳动。
 */
@Composable
private fun SzjGlamourImage(url: String) {
    val context = LocalContext.current
    var bmp by remember(url) { mutableStateOf<android.graphics.Bitmap?>(ShizhijiaImageLoader.peek(url)) }
    var loaded by remember(url) { mutableStateOf(bmp != null) }
    LaunchedEffect(url) {
        if (!loaded) {
            bmp = ShizhijiaImageLoader.load(context, url)
            loaded = true
        }
    }
    // 所有卡片用统一的 9:16 比例（实测移动端封面统一 9:16），保证间距均匀。
    Box(
        Modifier.fillMaxWidth().aspectRatio(9f / 16f).background(SzjCardRaised),
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) {
            // 图片就位时淡入，避免瀑布流里一张张"啪"地跳出来。
            val motion = szjMotionEnabled()
            var shown by remember(url) { mutableStateOf(!motion) }
            LaunchedEffect(b) { shown = true }
            val a by animateFloatAsState(if (shown) 1f else 0f, tween(220), label = "szjGlamFade")
            Image(b.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().graphicsLayer { alpha = a })
        } else {
            // 未就位时用微光占位，和别处的加载态一致。
            SzjShimmerBox(Modifier.fillMaxSize(), RoundedCornerShape(0.dp))
        }
    }
}

/**
 * 幻化分区的「光之收藏家」标识卡。
 *
 * 原来这里放的是 glamour_banner.png——那其实是旧版紫色 Material3 界面的截图
 * （1982x1125，98% 都是 #A060C0 一类的紫），当初误当成官方 banner 存了进来。
 * 现在换成移动端真正用的那张标识，白底已扣成透明，按主题取两版墨色，
 * 黄星和暗红描边保留原色。
 */
@Composable
private fun SzjGlamourBannerCard() {
    val ctx = LocalContext.current
    val light = szjLight
    val logo = remember(light) {
        val name = if (light) "glamour_logo_light.png" else "glamour_logo_dark.png"
        runCatching { android.graphics.BitmapFactory.decodeStream(ctx.assets.open(name)) }
            .onFailure { android.util.Log.w("ShizhijiaImg", "glamour logo: ${it.message}") }
            .getOrNull()
    }
    SzjCardSurface(Modifier.fillMaxWidth()) {
        Box(
            // 标识本身 2.59:1，卡片给到 1.9:1 留出四周呼吸，别让字贴边。
            Modifier.fillMaxWidth().aspectRatio(1.9f).padding(horizontal = 14.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) {
                Image(
                    bitmap = logo.asImageBitmap(),
                    contentDescription = "光之收藏家",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                // 资源读不到时退回文字，别留一张空卡。
                Text("光之收藏家", color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}

/**
 * 幻化瀑布流卡片：封面限高裁切 + 标题 + 作者/服务器 + 收藏/点赞。
 *
 * 收藏夹里的行字段少（没有计数、没有作者），失效的还不能点，
 * 所以这几处都按"有就画、没有就不占位"处理，而不是画一堆 0。
 */
@Composable
private fun SzjGlamourCardItem(card: ShizhijiaGlamourCard, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    SzjCardSurface(
        onClick = {
            // 原作已删：官网这时直接不响应。这里给一句话，
            // 不然点了没反应会让人以为是卡了。
            if (!card.valid || card.id.isBlank()) {
                android.widget.Toast.makeText(context, "这套幻化已经不在了", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                nav(SzjRoute.GlamourDetail(card.id))
            }
        },
    ) {
        // 封面顶部跟着卡片圆角裁一下，不然图片方角会顶出卡片边缘。
        Box(Modifier.clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp))) {
            SzjGlamourImage(url = card.mainImage)
            if (!card.valid) {
                // 失效的压一层暗罩 + 角标：比只把文字变灰更容易一眼扫到。
                Box(
                    Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "已失效",
                        color = Color.White,
                        style = SzjLabelStyle,
                        modifier = Modifier.clip(SzjChipShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 9.dp, vertical = 9.dp)) {
            Text(
                card.title.ifBlank { "无题" },
                color = if (card.valid) SzjText else SzjMuted,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            // 收藏行不带作者，那一行整个省掉，别留个空行撑高卡片。
            if (card.characterName.isNotBlank() || card.groupName.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(card.characterName, color = SzjMuted, style = SzjMetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (card.groupName.isNotBlank()) {
                        Spacer(Modifier.width(2.dp))
                        SzjLocPin(12)
                        Text(card.groupName, color = SzjMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            // 计数为 -1 = 接口没给这个数（收藏行），不画 0。
            if (card.favorites >= 0 || card.likes >= 0) {
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (card.favorites >= 0) SzjCountMeta(R.drawable.ic_star_filled, card.favorites.toLong())
                    if (card.favorites >= 0 && card.likes >= 0) Spacer(Modifier.width(10.dp))
                    if (card.likes >= 0) SzjCountMeta(R.drawable.ic_heart, card.likes.toLong())
                }
            }
        }
    }
}
