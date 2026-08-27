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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaApi
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaArea
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaBoundCharacter
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaComment
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaRecruit
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaRecruitDetail
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
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaImageLoader
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSession
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSignLog
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSignReward
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSearchUser
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaSearchGlamour
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaUserProfile
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaGlamourDetail
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
// 签名元素是 SzjShard——锥形水晶棱条，用在选中态和分区标记上，替代通用下划线。
// ---------------------------------------------------------------------------

// 深色：板岩夜。近黑但偏蓝，卡片逐层抬升。
private val SzjDarkBg = Color(0xFF12161B)          // 板岩底
private val SzjDarkCard = Color(0xFF1A1F26)        // 石板
private val SzjDarkCardRaised = Color(0xFF232932)  // 抬升层
private val SzjDarkAccent = Color(0xFF5FD2C8)      // 水晶青
private val SzjDarkAccentSoft = Color(0xFF14312F)  // 青光残留
private val SzjDarkOnAccentSoft = Color(0xFF8FE3DB)
private val SzjDarkText = Color(0xFFE8EDF2)
private val SzjDarkMuted = Color(0xFF8A94A2)
private val SzjDarkLine = Color(0xFF2A313A)
private val SzjDarkHairline = Color(0xFF39414C)
private val SzjDarkEdge = Color(0x14FFFFFF)        // 石板顶边高光

// 浅色：晨雾。冷调薄雾底 + 纯白卡片 + 深青。
private val SzjLightBg = Color(0xFFEEF1F4)         // 薄雾底
private val SzjLightCard = Color(0xFFFFFFFF)       // 白石板
private val SzjLightCardRaised = Color(0xFFE3E8ED) // 抬升层
private val SzjLightAccent = Color(0xFF10736C)     // 深水晶青（白底 5.2:1）
private val SzjLightAccentSoft = Color(0xFFD8EDEB)
private val SzjLightOnAccentSoft = Color(0xFF0B4F4A)
private val SzjLightText = Color(0xFF1B2129)
// #66707C 在薄雾底上只有 4.44:1，元信息用的是 11sp 小字，压到 5.0 才安全。
private val SzjLightMuted = Color(0xFF5E6874)
private val SzjLightLine = Color(0xFFDDE3E9)
private val SzjLightHairline = Color(0xFFC6CFD8)
private val SzjLightEdge = Color(0x0A000000)

private val szjLight: Boolean @Composable get() = MaterialTheme.colorScheme.background.luminance() > 0.5f

private val SzjBg: Color @Composable get() = if (szjLight) SzjLightBg else SzjDarkBg
private val SzjCard: Color @Composable get() = if (szjLight) SzjLightCard else SzjDarkCard
private val SzjCardRaised: Color @Composable get() = if (szjLight) SzjLightCardRaised else SzjDarkCardRaised
private val SzjAccent: Color @Composable get() = if (szjLight) SzjLightAccent else SzjDarkAccent
private val SzjAccentSoft: Color @Composable get() = if (szjLight) SzjLightAccentSoft else SzjDarkAccentSoft
private val SzjOnAccentSoft: Color @Composable get() = if (szjLight) SzjLightOnAccentSoft else SzjDarkOnAccentSoft
private val SzjText: Color @Composable get() = if (szjLight) SzjLightText else SzjDarkText
private val SzjMuted: Color @Composable get() = if (szjLight) SzjLightMuted else SzjDarkMuted
private val SzjLine: Color @Composable get() = if (szjLight) SzjLightLine else SzjDarkLine
private val SzjHairline: Color @Composable get() = if (szjLight) SzjLightHairline else SzjDarkHairline
private val SzjEdge: Color @Composable get() = if (szjLight) SzjLightEdge else SzjDarkEdge
private val SzjOnAccent: Color @Composable get() = if (szjLight) Color(0xFFFFFFFF) else Color(0xFF07211F)
private val SzjCommentBg: Color @Composable get() = if (szjLight) Color(0xFFE7ECF1) else Color(0xFF161B21)

// ---- 形状：卡片舒展，控件收紧。三档而不是一档，层级靠圆角区分。 ----
private val SzjCardShape = RoundedCornerShape(14.dp)
private val SzjInnerShape = RoundedCornerShape(10.dp)
private val SzjChipShape = RoundedCornerShape(9.dp)

// ---- 排版：没有可用的中文显示字体（项目内 AXIS 只有图标字形），
// 所以人格靠字号跨度、字重和字距，而不是字体家族。
// 元信息统一用宽字距小字，和正文形成"标签 vs 内容"的对照。
private val SzjMetaStyle = TextStyle(fontSize = 11.sp, letterSpacing = 0.4.sp, fontWeight = FontWeight.Medium)
private val SzjLabelStyle = TextStyle(fontSize = 12.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.SemiBold)

// ---- 动效 ----
/** 系统「减少动画」开着时把动效降到 0，无障碍设置优先于观感。 */
@Composable
private fun szjMotionEnabled(): Boolean {
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
private val SzjPressSpring = spring<Float>(dampingRatio = 0.62f, stiffness = 420f)
private val SzjMorphSpring = spring<Float>(dampingRatio = 0.75f, stiffness = 320f)
private const val SZJ_ENTER_MS = 260
private const val SZJ_STAGGER_MS = 26

/**
 * 锥形水晶棱条——石之家的签名标记。
 *
 * 两头收尖的细长六边形，取自银泪湖水晶阵的形状。用在选中态和分区标记，
 * 比通用下划线更能标出"你在这里"，也是整套设计里唯一的装饰性笔画。
 */
@Composable
private fun SzjShard(
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
private fun SzjCardSurface(
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
            .shadow(elevation, shape, ambientColor = Color(0xFF0A1016), spotColor = Color(0xFF0A1016))
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
private fun SzjRise(index: Int, content: @Composable () -> Unit) {
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
private fun <T> SzjResState(
    res: ShizhijiaApi.Res<T>?,
    emptyTitle: String,
    emptyHint: String? = null,
    onLogin: (() -> Unit)? = null,
    inline: Boolean = false,
) {
    val empty: @Composable (String, String?, (@Composable () -> Unit)?) -> Unit =
        if (inline) { t, h, a -> SzjEmptyInline(t, h, a) } else { t, h, a -> SzjEmpty(t, h, a) }
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
 * 不是只写"暂无内容"的地方。棱条在这里当一个安静的锚点。
 */
@Composable
private fun SzjEmpty(title: String, hint: String? = null, action: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(bottom = 90.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
            SzjShard(widthDp = 4, heightDp = 26, color = SzjHairline)
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
}

/** 主按钮：实心水晶青，按下缩一下。石之家里所有确认动作都用它。 */
@Composable
private fun SzjPrimaryButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SzjPressable(onClick = onClick, modifier = modifier, shape = SzjInnerShape) {
        Text(
            label,
            color = SzjOnAccent,
            style = SzjLabelStyle,
            modifier = Modifier.clip(SzjInnerShape).background(SzjAccent).padding(horizontal = 22.dp, vertical = 10.dp),
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
 * 子页页头：返回箭头 + 水晶棱条 + 标题。棱条把标题和返回键分开，
 * 顺便把签名元素带进每一个子页面，不用再画分割线。
 */
@Composable
private fun SzjHeader(title: String, onBack: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val backScale by animateFloatAsState(if (pressed) 0.86f else 1f, SzjPressSpring, label = "szjBack")
    Row(
        Modifier.fillMaxWidth().background(SzjBg)
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "‹",
            color = SzjAccent,
            fontSize = 34.sp,
            lineHeight = 30.sp,
            modifier = Modifier
                .graphicsLayer { scaleX = backScale; scaleY = backScale }
                .clip(CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = { onBack?.invoke() })
                .padding(horizontal = 10.dp, vertical = 2.dp),
        )
        SzjShard(heightDp = 15)
        Text(
            title,
            color = SzjText,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.2.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 10.dp),
        )
        trailing?.invoke()
    }
}

/**
 * 石之家 (FF14 Rising Stones official community) - the in-phone "app".
 *
 * Renders the forum through the public JSON API: a post feed with partitions,
 * post detail (HTML body), comments, search, and the login-gated dynamics feed.
 * The whole feature uses its own minimal "Rising Stones" design language:
 * near-black base, warm off-white text, antique-gold accents and thin hairline
 * dividers instead of the global purple Material3 theme. Internal navigation
 * uses a simple back stack so the system back button walks out level-by-level.
 */

private sealed interface SzjRoute {
    data object Home : SzjRoute
    data class PostDetail(val postId: String) : SzjRoute
    data class DynamicDetail(val id: String) : SzjRoute
    data object Search : SzjRoute
    data object Login : SzjRoute
    data object SignCalendar : SzjRoute
    data class UserProfile(val uuid: String) : SzjRoute
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
}

/** App-wide full-screen image viewer state; any thumbnail sets its URL here. */
object SzjViewer {
    var url by mutableStateOf<String?>(null)
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

    /** 四个分区（社区/招募/幻化/我）和社区的二级 Tab。 */
    val mainTab = mutableStateOf(MAIN_COMMUNITY)
    val subTab = mutableStateOf(SUB_POSTS)
    /** 分区切换历史：在招募按返回应该回社区，而不是直接退到桌面。 */
    private var tabHistory by mutableStateOf(listOf<Int>())

    // 各分区的数据缓存也挂这儿，回桌面再进来不用重新拉一遍。
    val posts = SzjPostsState()
    val strategy = SzjStrategyState()
    val recruit = SzjRecruitState()
    val glamour = SzjGlamourState()
    val search = SzjSearchState()

    fun push(r: SzjRoute) { stack = stack + r }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }

    /** 切分区时记一笔来路，供返回键回溯。 */
    fun selectTab(tab: Int) {
        if (tab == mainTab.value) return
        tabHistory = (tabHistory + mainTab.value).takeLast(8)
        mainTab.value = tab
    }

    /**
     * 返回键能不能被石之家吃掉：栈里有上一页，或者分区有来路。
     * 都没有才让它穿透到桌面。
     */
    val canGoBack: Boolean get() = stack.size > 1 || tabHistory.isNotEmpty()

    fun back() {
        if (stack.size > 1) { stack = stack.dropLast(1); return }
        val prev = tabHistory.lastOrNull() ?: return
        tabHistory = tabHistory.dropLast(1)
        mainTab.value = prev
    }
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
        Text("📍", color = tint, fontSize = (sizeDp * 0.85f).sp)
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
        .shadow(2.dp, CircleShape, ambientColor = Color(0xFF0A1016), spotColor = Color(0xFF0A1016))
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
    val sort = mutableStateOf(0)       // 0=推荐 1=最新
    val items = mutableStateOf(listOf<ShizhijiaGlamourCard>())
    val loading = mutableStateOf(false)
    val page = mutableStateOf(1)
    val ended = mutableStateOf(false)
    val loadedKey = mutableStateOf("")
    val gridState = androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState()
    // 筛选
    val raceId = mutableStateOf(-1)
    val genderId = mutableStateOf(-1)
    val createTimeIdx = mutableStateOf(0)
    val filterOpen = mutableStateOf(false)
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
    // 有上一页、或者分区有来路时吃掉返回键；都没有才穿透到桌面。
    BackHandler(enabled = SzjNav.canGoBack) { SzjNav.back() }
    val route = SzjNav.stack.last()
    // nav pushes a destination; pop returns to the previous one (login success uses pop).
    val nav: (SzjRoute) -> Unit = { SzjNav.push(it) }
    val pop: () -> Unit = { SzjNav.pop() }
    Box(Modifier.fillMaxSize()) {
        when (route) {
SzjRoute.Home -> ShizhijiaHomeScreen(state, nav, postsState, strategyState, recruitState, glamourState, homeMainTab, homeSubTab, barHeightDp = barHeight, barBottomDp = barBottom, onBarHeightChange = { barHeight = it }, onBarBottomChange = { barBottom = it })
            is SzjRoute.PostDetail -> ShizhijiaPostDetailScreen(state, route.postId, pop, nav)
            is SzjRoute.DynamicDetail -> ShizhijiaDynamicDetailScreen(state, route.id, pop)
            SzjRoute.Search -> ShizhijiaSearchScreen(state, pop, nav, searchState)
            SzjRoute.Login -> ShizhijiaLoginScreen(state, pop)
            SzjRoute.SignCalendar -> ShizhijiaSignCalendarScreen(state, pop)
            is SzjRoute.UserProfile -> ShizhijiaUserProfileScreen(state, route.uuid, pop, nav)
            is SzjRoute.GlamourDetail -> ShizhijiaGlamourDetailScreen(state, route.glamourId, pop, nav)
            SzjRoute.Favorites -> ShizhijiaFavoritesScreen(pop, nav)
            SzjRoute.MyRecruits -> ShizhijiaMyRecruitsScreen(pop, nav)
            SzjRoute.Characters -> ShizhijiaCharactersScreen(pop)
            SzjRoute.MyGuild -> ShizhijiaMyGuildScreen(pop, nav)
            is SzjRoute.GuildPhotoDetail -> ShizhijiaGuildPhotoDetailScreen(route.photoId, pop, nav)
            is SzjRoute.RecruitDetail -> ShizhijiaRecruitDetailScreen(route.kind, route.id, pop, nav)
        }
        SzjViewer.url?.let { url ->
            // Full-screen overlay for viewing a tapped image at size.
            SzjPhotoViewer(url = url, onClose = { SzjViewer.url = null })
        }
    }
}

/** Full-screen image viewer: dark scrim, fitted image, X (top-left) or back closes. */
@Composable
private fun SzjPhotoViewer(url: String, onClose: () -> Unit) {
    BackHandler { onClose() }
    val context = LocalContext.current
    var bmp by remember(url) { mutableStateOf(if (url.startsWith("data:image")) decodeDataUri(url) else ShizhijiaImageLoader.peek(url)) }
    LaunchedEffect(url) { if (!url.startsWith("data:image")) bmp = ShizhijiaImageLoader.load(context, url) }
    // Pinch-zoom + pan. All gestures are consumed here so the list underneath
    // never scrolls while the viewer is open.
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    Box(Modifier.fillMaxSize().background(Color(0xE6000000))
        .pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                scale = (scale * zoom).coerceIn(1f, 5f)
                offset = if (scale > 1f) offset + pan else androidx.compose.ui.geometry.Offset.Zero
            }
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = { scale = if (scale > 1f) 1f else 2.5f },
            )
        }
    ) {
        val bmpV = bmp
        if (bmpV != null) {
            Image(bmpV.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y })
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(30.dp)) }
        }
        // Close button at the bottom-right corner. 查看器始终是暗底，
        // 所以这里固定用亮色，不跟随浅色主题。
        Box(Modifier.align(Alignment.BottomEnd).padding(18.dp)) {
            SzjPressable(onClick = onClose, shape = CircleShape) {
                Text("✕", color = Color(0xFFE8EDF2), fontSize = 20.sp,
                    modifier = Modifier.clip(CircleShape).background(Color(0xB3232932))
                        .padding(horizontal = 14.dp, vertical = 9.dp))
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

/** Slightly raised backdrop for the comment area, distinct from the article
 *  body so the two regions are obvious while scrolling. */
private val CommentAreaBg: Color @Composable get() = SzjCommentBg

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
    val brandRow: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            SzjShard(widthDp = 4, heightDp = 24)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("石之家", color = SzjText, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("摩杜纳 · 拂晓血盟", color = SzjMuted, style = SzjMetaStyle)
            }
        }
    }
    // 社区分区的完整头部：品牌行 + 账号卡。其他分区只要品牌行。
    val communityHeader: @Composable () -> Unit = {
        Column {
            brandRow()
            ShizhijiaTopBar(state, nav, loggedIn, loginUser, onSignIn, signedToday)
        }
    }

    val bar = remember { SzjBarVisibility() }
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
                                    else -> ShizhijiaStrategyTab(state, nav, strategyState, communityHeader, subTabs)
                                }
                            }
                        }
                        MAIN_RECRUIT -> ShizhijiaRecruitTab(nav, loggedIn, recruitState, brandRow)
                        MAIN_GLAMOUR -> ShizhijiaGlamourTab(nav, loggedIn, glamourState, brandRow)
                        else -> ShizhijiaMeTab(state, nav, loggedIn, loginUser, barHeightDp, barBottomDp, onBarHeightChange, onBarBottomChange, brandRow)
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
            )
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
            Box(Modifier.size(40.dp)
                .clip(CircleShape)
                .clickable(enabled = !loggedIn || myUuid.isNotBlank(), onClick = openMe)
                .shadow(2.dp, CircleShape, ambientColor = Color(0xFF0A1016), spotColor = Color(0xFF0A1016))
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
                Text(loginUser?.name ?: if (loggedIn) "已登录" else "未登录", color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
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
            Spacer(Modifier.width(6.dp))
            SzjPressable(onClick = { nav(SzjRoute.Search) }, shape = CircleShape) {
                Text("⌕", color = SzjAccent, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
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
private fun SzjPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = SzjChipShape,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = szjMotionEnabled()
    val scale by animateFloatAsState(if (pressed && motion) 0.94f else 1f, SzjPressSpring, label = "szjPressable")
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Second-level tab row inside the Community section: 帖子 / 动态 / 攻略. */
@Composable
private fun SzjSubTabRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SzjSubTab("帖子", selected == SUB_POSTS) { onSelect(SUB_POSTS) }
        SzjSubTab("动态", selected == SUB_DYNAMICS) { onSelect(SUB_DYNAMICS) }
        SzjSubTab("攻略", selected == SUB_GUIDE) { onSelect(SUB_GUIDE) }
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
    val p = loginUser
    Column(Modifier.fillMaxSize().padding(bottom = 90.dp).verticalScroll(rememberScrollState())) {
      // 品牌行随内容滑走，和其他分区一致。它自带左右 16dp，所以放在内边距外面。
      header()
      Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
        if (showSettings) {
            // ---- 设置页 ----
            Row(verticalAlignment = Alignment.CenterVertically) {
                SzjShard(widthDp = 3, heightDp = 18)
                Spacer(Modifier.width(8.dp))
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
            Spacer(Modifier.height(12.dp))
            SzjPressable(onClick = { showSettings = false }, shape = SzjChipShape) {
                Text("‹ 返回", color = SzjAccent, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp))
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
                        if (myUuid.isNotBlank()) Text("›", color = SzjMuted, fontSize = 22.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                    // 计数条：数字大、标签小，中间用竖棱条分隔。
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        listOf("关注" to 0, "粉丝" to 0, "获赞" to 0).forEachIndexed { i, (label, num) ->
                            if (i > 0) Box(Modifier.width(1.dp).height(22.dp).background(SzjLine))
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$num", color = SzjText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text(label, color = SzjMuted, style = SzjMetaStyle)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            // ---- 入口宫格：三列，每格一张小石板 ----
            // 已接通的入口直接跳页；「我的部队」和「专项数据」还没接（见下方注释）。
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
                                        // 专项数据是 40 多个需登录+绑角色的接口，
                                        // 字段名不在前端代码里，只能等实测。
                                        else -> android.widget.Toast.makeText(context, "$label 还要抓一次响应才能接", android.widget.Toast.LENGTH_SHORT).show()
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
    LaunchedEffect(Unit) {
        // 帖子和攻略两套版块都列上，两个流用的是同一份屏蔽名单。
        parts = ShizhijiaApi.getPostParts(context) + ShizhijiaApi.getStrategyParts(context)
    }
    SzjCardSurface(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text("推荐过滤", color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                if (muted.isEmpty()) "选中的版块不会出现在推荐里"
                else "已屏蔽 ${muted.size} 个版块",
                color = SzjMuted, style = SzjMetaStyle,
            )
            Spacer(Modifier.height(12.dp))
            if (parts.isEmpty()) {
                Text("正在读版块列表", color = SzjMuted, style = SzjMetaStyle)
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
                                            .background(if (on) SzjAccent else SzjCardRaised)
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
) {
    val motion = szjMotionEnabled()
    // 指示块位置按格数插值：0..3 → 0f..1f，用弹簧跟过去。
    val pos by animateFloatAsState(
        selected.toFloat(),
        if (motion) spring(dampingRatio = 0.7f, stiffness = 300f) else spring(stiffness = 100000f),
        label = "szjBarPos",
    )
    // 收起：往下沉出屏幕 + 略微缩小 + 淡出。三者一起做才像"缩回去"，
    // 只做位移会显得是被裁掉。收起比放出慢一点（280 vs 200），
    // 因为放出来时用户通常已经想点它了。
    val collapse by animateFloatAsState(
        if (hidden && motion) 1f else 0f,
        tween(if (hidden) 280 else 200, easing = FastOutSlowInEasing),
        label = "szjBarCollapse",
    )
    val density = androidx.compose.ui.platform.LocalDensity.current
    val slidePx = with(density) { (barHeightDp + barBottomDp + 24f).dp.toPx() }
    Box(
        modifier
            .padding(start = 18.dp, end = 18.dp, bottom = 10.dp + barBottomDp.dp)
            .fillMaxWidth().height(barHeightDp.dp)
            .graphicsLayer {
                translationY = collapse * slidePx
                val s = 1f - collapse * 0.12f
                scaleX = s
                scaleY = s
                alpha = 1f - collapse * 0.55f
                // 缩放锚点放在底边中央，视觉上是"往下缩回去"而不是整体缩小。
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
            }
            .shadow(10.dp, RoundedCornerShape(20.dp), ambientColor = Color(0xFF0A1016), spotColor = Color(0xFF0A1016))
            .clip(RoundedCornerShape(20.dp))
            .background(SzjCard)
            .then(if (szjLight) Modifier.border(1.dp, SzjLine, RoundedCornerShape(20.dp)) else Modifier),
    ) {
        // 顶边高光，和石板卡片同一处理。
        Box(Modifier.fillMaxWidth().height(1.dp).background(
            Brush.horizontalGradient(listOf(Color.Transparent, SzjEdge, SzjEdge, Color.Transparent))
        ))
        // 滑动的选中底块，垫在文字下面。
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 7.dp)) {
            val cell = maxWidth / 4
            Box(
                Modifier
                    .offset(x = cell * pos)
                    .width(cell).fillMaxHeight()
                    .padding(horizontal = 4.dp)
                    .clip(SzjInnerShape)
                    .background(SzjAccentSoft)
            )
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                SzjBottomTab("社区", selected == MAIN_COMMUNITY, Modifier.width(cell)) { onSelect(MAIN_COMMUNITY) }
                SzjBottomTab("招募", selected == MAIN_RECRUIT, Modifier.width(cell)) { onSelect(MAIN_RECRUIT) }
                SzjBottomTab("幻化", selected == MAIN_GLAMOUR, Modifier.width(cell)) { onSelect(MAIN_GLAMOUR) }
                SzjBottomTab("我", selected == MAIN_ME, Modifier.width(cell)) { onSelect(MAIN_ME) }
            }
        }
    }
}

@Composable
private fun SzjBottomTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val color by animateColorAsState(if (selected) SzjOnAccentSoft else SzjMuted, tween(200), label = "szjBottomTabColor")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = szjMotionEnabled()
    val scale by animateFloatAsState(if (pressed && motion) 0.9f else 1f, SzjPressSpring, label = "szjBottomTabPress")
    Box(
        modifier.fillMaxHeight()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(SzjInnerShape)
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
private fun SzjEmptyInline(title: String, hint: String? = null, action: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 40.dp)) {
            SzjShard(widthDp = 4, heightDp = 26, color = SzjHairline)
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
}

/** 分区 chip：选中时底色和文字色一起过渡，不做位移，避免和棱条抢戏。 */
@Composable
private fun SzjPartChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) SzjAccentSoft else SzjCard, tween(220), label = "szjChipBg")
    val fg by animateColorAsState(if (selected) SzjOnAccentSoft else SzjMuted, tween(220), label = "szjChipFg")
    SzjPressable(onClick = onClick, shape = SzjChipShape) {
        Text(
            label,
            color = fg,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.clip(SzjChipShape).background(bg).padding(horizontal = 14.dp, vertical = 7.dp),
        )
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                SzjShard(widthDp = 2, heightDp = 10)
                Spacer(Modifier.width(6.dp))
                Text(post.partName, color = SzjAccent, style = SzjMetaStyle)
            }
        }
        Text(
            post.title,
            color = SzjText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            lineHeight = 23.sp, letterSpacing = 0.1.sp,
            maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        // Line 2: main-image thumbnails, up to 3 (deduplicated). Every thumbnail keeps
        // the SAME fixed ~1/3 width whether 1, 2 or 3 are shown; failed images
        // collapse away (no blank frame before the healthy ones).
        if (post.coverPics.isNotEmpty()) {
            Spacer(Modifier.height(11.dp))
            androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cell = (maxWidth - 12.dp) / 3
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    post.coverPics.distinct().take(3).forEach { url ->
                        ShizhijiaRemoteImage(url = url, modifier = Modifier.width(cell).height(cell).clip(SzjInnerShape), contentScale = ContentScale.Crop, showPlaceholder = false, collapseOnFail = true, onClick = { SzjViewer.url = it })
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
            if (post.commentCount > 0) Text("${post.commentCount} 评论", color = SzjMuted, style = SzjMetaStyle)
            if (post.commentCount > 0 && post.readCount > 0) {
                // 计数之间用一个小点分隔，比空格更有结构。
                Text(" · ", color = SzjMuted, style = SzjMetaStyle)
            }
            if (post.readCount > 0) Text("${post.readCount} 阅读", color = SzjMuted, style = SzjMetaStyle)
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
    LaunchedEffect(loggedIn) {
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
            Text(d.contentText, color = SzjText, fontSize = 14.sp, lineHeight = 21.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        LazyRow(
                            Modifier.weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            items(ShizhijiaRecruitKind.entries, key = { it.name }) { k ->
                                SzjPartChip(k.label, kind == k) { rs.kind.value = k }
                            }
                        }
                        // 部队招募列表没有公开筛选参数，所以那一类不给筛选入口。
                        if (kind != ShizhijiaRecruitKind.Guild) {
                            val on = filter.activeFor(kind)
                            SzjPressable(onClick = { rs.filterOpen.value = true }, shape = SzjChipShape) {
                                Text(
                                    "筛选",
                                    style = SzjLabelStyle,
                                    color = if (on) SzjOnAccent else SzjMuted,
                                    modifier = Modifier.clip(SzjChipShape)
                                        .background(if (on) SzjAccent else SzjCardRaised)
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                        }
                    }
                    // 生效的条件在 Tab 下面列一行，不用打开面板也知道筛了什么
                    val chips = szjFilterSummary(kind, filter, rs)
                    if (chips.isNotEmpty()) {
                        LazyRow(
                            Modifier.fillMaxWidth().padding(top = 6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                    Text("清空", color = SzjMuted, style = SzjMetaStyle, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
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

    Column(
        Modifier.fillMaxSize()
            .background(Color(0x73000000))
            .pointerInput(Unit) { detectTapGestures { onClose() } }
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = true,
            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { -it },
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(bottomEnd = 18.dp, bottomStart = 18.dp), ambientColor = Color(0xFF0A1016), spotColor = Color(0xFF0A1016))
                    .clip(RoundedCornerShape(bottomEnd = 18.dp, bottomStart = 18.dp))
                    .background(SzjBg)
                    .clickable(interactionSource = noRipple, indication = null) { }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
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
                Spacer(Modifier.height(18.dp))
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
                            modifier = Modifier.fillMaxWidth().clip(SzjInnerShape).background(SzjAccent).padding(vertical = 11.dp))
                    }
                }
            }
        }
    }
}

/** 多选列表里加/减一项。 */
private fun List<String>.toggle(id: String): List<String> =
    if (contains(id)) this - id else this + id

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            SzjShard(widthDp = 2, heightDp = 11)
            Spacer(Modifier.width(7.dp))
            Text(label, color = SzjText, style = SzjLabelStyle)
        }
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
                                SzjShard(widthDp = 3, heightDp = 13)
                                Spacer(Modifier.width(7.dp))
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
                        if (d.ipLocation.isNotBlank()) add("发布地" to d.ipLocation)
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
                item(key = "note") {
                    Text(
                        "报名要在石之家网页或官方 App 里做",
                        color = SzjMuted, style = SzjMetaStyle, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }
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
    val listState = rememberLazyListState()

    LaunchedEffect(postId) {
        loading = true
        detail = ShizhijiaApi.getPostDetail(context, postId)
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
    // Infinite scroll for comments.
    val nearEnd by remember { derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
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

    ScreenFrame(background = SzjBg) {
        SzjHeader("帖子详情", onBack = { pop() })
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
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item {
                // 标题和作者收进一张石板：正文是长内容，先给它一个明确的"头"。
                SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(15.dp)) {
                        Text(d.title, color = SzjText, fontSize = 20.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp, letterSpacing = 0.1.sp)
                        Spacer(Modifier.height(13.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(SzjInnerShape).clickable { nav(SzjRoute.UserProfile(d.uuid)) }) {
                            SzjAvatar(d.characterName, d.avatar, d.uuid, 36)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.characterName, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(listOf(d.areaName, d.groupName).filter { it.isNotBlank() }.joinToString(" "), color = SzjMuted, style = SzjMetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis); if (d.createdAt.isNotBlank()) { Text(" " + d.createdAt, color = SzjMuted, style = SzjMetaStyle, maxLines = 1) } }
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
            item {
                // Rich HTML body: paragraphs, bold, inline images, links.
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    ShizhijiaRichContent(d.contentHtml)
                }
            }
            item {
                // Comments header with an inline ordering toggle. Its tinted
                // backdrop signals the switch from the article body into the
                // comment area, so the two never blur together while scrolling.
                Column(Modifier.fillMaxWidth().background(CommentAreaBg).padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SzjShard(heightDp = 14)
                        Spacer(Modifier.width(8.dp))
                        Text("全部评论", color = SzjText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        // 只看楼主: client-side filter on the loaded comment list.
                        SzjPressable(onClick = { onlyAuthor = !onlyAuthor }, shape = SzjChipShape) {
                            Text("只看楼主", color = if (onlyAuthor) SzjOnAccentSoft else SzjMuted, style = SzjMetaStyle,
                                modifier = Modifier.clip(SzjChipShape)
                                    .background(if (onlyAuthor) SzjAccentSoft else SzjCard)
                                    .padding(horizontal = 10.dp, vertical = 6.dp))
                        }
                    }
                    Spacer(Modifier.height(9.dp))
                    // 排序独占一行，三档并排——原来挤在标题右边，字小到点不准。
                    Row(Modifier.clip(SzjChipShape).background(SzjCard)) {
                        SzjSmallOption("默认", commentOrder == "earliest") { commentOrder = "earliest" }
                        SzjSmallOption("热门", commentOrder == "hottest") { commentOrder = "hottest" }
                        SzjSmallOption("最新", commentOrder == "latest") { commentOrder = "latest" }
                    }
                }
            }
            if (commentLoading && comments.isEmpty()) {
                item(key = "comments-loading") {
                    Column(Modifier.fillMaxWidth().background(CommentAreaBg).padding(horizontal = 14.dp, vertical = 8.dp)) {
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
                    Box(Modifier.fillMaxWidth().background(CommentAreaBg).padding(vertical = 34.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(if (onlyAuthor) "楼主还没在这里回帖" else "还没有人评论", color = SzjText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(6.dp))
                            Text(if (onlyAuthor) "关掉「只看楼主」看全部" else "在石之家网页版可以发第一条", color = SzjMuted, style = SzjMetaStyle)
                        }
                    }
                }
            } else {
                itemsIndexed(comments, key = { _, it -> it.id }) { index, c ->
                    SzjRise(index) { SzjCommentRow(c, nav) }
                }
                item(key = "comments-footer") {
                    if (commentLoading) Box(Modifier.fillMaxWidth().background(CommentAreaBg).padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    }
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
private fun SzjCommentRow(c: ShizhijiaComment, nav: (SzjRoute) -> Unit) {
    SzjCardSurface(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp)) {
      Column(Modifier.padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(SzjInnerShape).clickable { nav(SzjRoute.UserProfile(c.uuid)) }) {
            SzjAvatar(c.characterName, c.avatar, c.uuid, 30)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(c.characterName.ifBlank { "匿名玩家" }, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    // 作者标记做成实心小标签，比灰字更容易在长评论列里认出楼主。
                    if (c.isPostsAuthor) {
                        Spacer(Modifier.width(6.dp))
                        Text("作者", color = SzjOnAccentSoft, fontSize = 10.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(SzjAccentSoft).padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) { SzjLocPin(); Text(listOf(c.areaName, c.groupName).filter { it.isNotBlank() }.joinToString(" "), color = SzjMuted, style = SzjMetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis); if (c.createdAt.isNotBlank()) { Text(" " + c.createdAt, color = SzjMuted, style = SzjMetaStyle, maxLines = 1) } }
            }
            if (c.likeCount > 0) Text("${c.likeCount} 赞", color = SzjMuted, style = SzjMetaStyle)
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
      }
    }
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
    LaunchedEffect(Unit) { hotWords.value = ShizhijiaApi.getHotSearchList(context).map { it.text }.filter { it.isNotBlank() }.distinct() }

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
                .shadow(3.dp, SzjCardShape, ambientColor = Color(0xFF0A1016), spotColor = Color(0xFF0A1016))
                .clip(SzjCardShape).background(SzjCard)
                .then(if (szjLight) Modifier.border(1.dp, SzjLine, SzjCardShape) else Modifier)
                .padding(horizontal = 7.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                // 类型切换与输入框融合在同一搜索栏内（左侧）
                Box {
                    SzjPressable(onClick = { typeMenu = true }, shape = SzjChipShape) {
                    Row(Modifier.clip(SzjChipShape).background(SzjCardRaised)
                        .padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(typeLabel, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("▾", color = SzjMuted, fontSize = 10.sp, modifier = Modifier.padding(start = 4.dp))
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
                        Text("⌕", color = if (ready) SzjOnAccent else SzjMuted, fontSize = 18.sp)
                    }
                }
            }
            if (postResults.value == null && userResults.value == null && glamourResults.value == null && !searching.value) {
                if (history.value.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SzjShard(widthDp = 2, heightDp = 11)
                        Spacer(Modifier.width(7.dp))
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SzjShard(widthDp = 2, heightDp = 11)
                        Spacer(Modifier.width(7.dp))
                        Text("热门搜索", color = SzjText, style = SzjLabelStyle)
                    }
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
                        if (userResults.value.isNullOrEmpty()) SzjEmpty("没有叫「${query.value.trim()}」的角色", "试试只输入名字的一部分")
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
                        if (glamourResults.value.isNullOrEmpty()) SzjEmpty("没找到「${query.value.trim()}」的幻化", "换个部件名或职业名试试")
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
                    postResults.value.isNullOrEmpty() -> SzjEmpty("没有匹配「${query.value.trim()}」的$typeLabel", "换个说法，或者切到别的搜索类型")
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
private fun ShizhijiaDynamicDetailScreen(state: PhoneState, id: String, pop: () -> Unit) {
    val context = LocalContext.current
    var d by remember { mutableStateOf<ShizhijiaDynamic?>(null) }
    LaunchedEffect(id) { d = ShizhijiaApi.getDynamicDetail(context, id) }
    ScreenFrame(background = SzjBg) {
        SzjHeader("动态详情", onBack = { pop() })
        val item = d
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
        LazyColumn(Modifier.fillMaxSize()) {
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
                    if (item.contentText.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text(item.contentText, color = SzjText, fontSize = 15.sp, lineHeight = 23.sp) }
                  }
                }
            }
            items(item.images) { img ->
                ShizhijiaRemoteImage(url = img, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clip(SzjInnerShape))
            }
        }
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
                            Text("本月已签到", fontSize = 13.sp, color = SzjMuted, letterSpacing = 0.4.sp)
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${log?.count ?: 0}", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = SzjAccent, lineHeight = 34.sp)
                                Text(" 天", fontSize = 13.sp, color = SzjMuted, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                        SzjShard(widthDp = 5, heightDp = 30)
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
                    SzjShard(widthDp = 2, heightDp = 12)
                    Spacer(Modifier.width(7.dp))
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
                                    modifier = Modifier.clip(SzjChipShape).background(SzjAccent).padding(horizontal = 14.dp, vertical = 7.dp))
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

/** 我收藏的帖子。接口需登录，未登录时服务端返回 10403 → 这里显示登录引导。 */
@Composable
private fun ShizhijiaFavoritesScreen(pop: () -> Unit, nav: (SzjRoute) -> Unit) {
    val context = LocalContext.current
    var posts by remember { mutableStateOf<List<ShizhijiaPostCard>?>(null) }
    var status by remember { mutableStateOf<ShizhijiaApi.Res<List<ShizhijiaPostCard>>?>(null) }
    var page by remember { mutableStateOf(1) }
    var ended by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        loading = true
        val res = ShizhijiaApi.getMyStarPosts(context, 1)
        status = res
        posts = (res as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
        loading = false
    }
    val nearEnd by remember { derivedStateOf {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val n = posts?.size ?: 0
        n > 0 && last >= n - 3
    } }
    LaunchedEffect(nearEnd) {
        if (nearEnd && !loading && !ended) {
            loading = true
            val next = (ShizhijiaApi.getMyStarPosts(context, page + 1) as? ShizhijiaApi.Res.Ok)?.value.orEmpty()
            if (next.isEmpty()) ended = true else { posts = posts.orEmpty() + next; page += 1 }
            loading = false
        }
    }

    ScreenFrame(background = SzjBg) {
        SzjHeader("我的收藏", onBack = pop)
        val list = posts
        when {
            list == null && loading -> SzjFeedSkeleton()
            list.isNullOrEmpty() -> SzjResState(
                res = status,
                emptyTitle = "收藏夹是空的",
                emptyHint = "在帖子详情点收藏，之后就能在这里找回来",
                onLogin = { nav(SzjRoute.Login) },
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 6.dp, bottom = 20.dp),
            ) {
                itemsIndexed(list, key = { _, it -> it.postsId }) { index, post ->
                    SzjRise(index) { SzjPostRow(post, onClick = { nav(SzjRoute.PostDetail(post.postsId)) }) }
                }
                item(key = "footer") {
                    if (loading) Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
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
                    modifier = Modifier.clip(SzjChipShape).background(SzjAccent).padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        })
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
                emptyHint = "发布要在石之家网页或官方 App 里做，这里只看和擦亮",
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
                        SzjShard(widthDp = 2, heightDp = 12)
                        Spacer(Modifier.width(7.dp))
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
                        SzjShard(widthDp = 2, heightDp = 12)
                        Spacer(Modifier.width(7.dp))
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
        SzjShard(widthDp = 3, heightDp = 13)
        Spacer(Modifier.width(7.dp))
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
                items(p.urls, key = { it }) { url ->
                    ShizhijiaRemoteImage(
                        url = url,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 5.dp).clip(SzjInnerShape),
                        contentScale = ContentScale.FillWidth,
                        collapseOnFail = true,
                        onClick = { SzjViewer.url = it },
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
                        Modifier.fillMaxWidth().background(SzjCommentBg)
                            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SzjShard(widthDp = 3, heightDp = 13)
                        Spacer(Modifier.width(7.dp))
                        Text("评论", color = SzjText, style = SzjLabelStyle)
                    }
                }
                val cs = (comments as? ShizhijiaApi.Res.Ok)?.value
                when {
                    comments == null -> item(key = "cload") {
                        Box(Modifier.fillMaxWidth().background(SzjCommentBg).padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SzjAccent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        }
                    }
                    cs.isNullOrEmpty() -> item(key = "cempty") {
                        Box(Modifier.fillMaxWidth().background(SzjCommentBg)) {
                            SzjEmptyInline("还没有评论")
                        }
                    }
                    else -> items(cs, key = { it.id }) { c ->
                        Box(Modifier.fillMaxWidth().background(SzjCommentBg)) { SzjCommentRow(c, nav) }
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
                        modifier = Modifier.clip(SzjChipShape).background(SzjAccent).padding(horizontal = 13.dp, vertical = 7.dp),
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        listOf("关注" to p.followNum, "粉丝" to p.fansNum, "获赞" to p.likedNum).forEachIndexed { i, (label, num) ->
                            if (i > 0) Box(Modifier.width(1.dp).height(22.dp).background(SzjLine))
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$num", color = SzjText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(2.dp))
                                Text(label, color = SzjMuted, style = SzjMetaStyle)
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
                    SzjSubTab("帖子", tab == 1) { tab = 1 }
                }
            }
            if (tab == 0) {
                // ---- 信息 tab ----
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        val battle = p.careers.filter { it.type !in listOf("能工巧匠", "大地使者") }.sortedByDescending { it.level }
                        val craft = p.careers.filter { it.type in listOf("能工巧匠", "大地使者") }.sortedByDescending { it.level }
                        if (battle.isNotEmpty()) {
                            Text("战斗精英 & 魔法导师", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                battle.chunked(5).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        row.forEach { c ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                val icon = jobIcons[c.name].orEmpty()
                                                val abbr = szjCrafterAbbr(c.name)
                                                var showTip = tipCareer == c.name
                                                Box {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.clickable { tipCareer = if (showTip) null else c.name }) {
                                                        Box(Modifier.size(34.dp).clip(SzjChipShape).background(SzjCardRaised), contentAlignment = Alignment.Center) {
                                                            if (icon.isNotBlank()) ShizhijiaRemoteImage(url = icon, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, showPlaceholder = false)
                                                            else Text(abbr, color = SzjAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                        Text("${c.level}", fontSize = 10.sp, color = SzjText)
                                                    }
                                                    if (showTip) {
                                                        // Small bubble above the icon with the job name.
                                                        Box(Modifier.matchParentSize()) {
                                                            Text(c.name, color = SzjOnAccentSoft, fontSize = 10.sp,
                                                                modifier = Modifier.align(Alignment.TopCenter)
                                                                    .offset(y = (-22).dp)
                                                                    .clip(SzjChipShape)
                                                                    .background(SzjAccentSoft)
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                        if (craft.isNotEmpty()) {
                            Text("能工巧匠 & 大地使者", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(5.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                craft.chunked(5).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        row.forEach { c ->
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                val icon = jobIcons[c.name].orEmpty()
                                                val abbr = szjCrafterAbbr(c.name)
                                                var showTip = tipCareer == c.name
                                                Box {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                                                        modifier = Modifier.clickable { tipCareer = if (showTip) null else c.name }) {
                                                        Box(Modifier.size(34.dp).clip(SzjChipShape).background(SzjCardRaised), contentAlignment = Alignment.Center) {
                                                            if (icon.isNotBlank()) ShizhijiaRemoteImage(url = icon, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, showPlaceholder = false)
                                                            else Text(abbr, color = SzjAccent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                                        }
                                                        Text("${c.level}", fontSize = 10.sp, color = SzjText)
                                                    }
                                                    if (showTip) {
                                                        // Small bubble above the icon with the job name.
                                                        Box(Modifier.matchParentSize()) {
                                                            Text(c.name, color = SzjOnAccentSoft, fontSize = 10.sp,
                                                                modifier = Modifier.align(Alignment.TopCenter)
                                                                    .offset(y = (-22).dp)
                                                                    .clip(SzjChipShape)
                                                                    .background(SzjAccentSoft)
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
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
                            "幻理模板使用" to if (p.washingNum > 0) p.washingNum.toString() else "",
                            "伪零击败数" to if (p.killTimes > 0) p.killTimes.toString() else "",
                            "水晶沙段位" to p.crystalRank,
                            "钓鱼抛竿" to if (p.fishTimes > 0) p.fishTimes.toString() else "",
                            "宝物击败数" to if (p.treasureTimes > 0) p.treasureTimes.toString() else "",
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
                                SzjShard(widthDp = 2, heightDp = 12)
                                Spacer(Modifier.width(7.dp))
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
                        // 游戏近况: recent/r{typeId}.png + event text.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            SzjShard(widthDp = 2, heightDp = 12)
                            Spacer(Modifier.width(7.dp))
                            Text("游戏近况", color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (recents.isEmpty()) {
                            Text(
                                if (recentsPrivate) "这位玩家把近况设为了私密" else "最近没有可展示的记录",
                                color = SzjMuted, style = SzjMetaStyle,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        } else {
                            recents.take(15).forEach { r ->
                                SzjCardSurface(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = SzjInnerShape) {
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
                        }
                    }
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
    LaunchedEffect(glamourId) { g = ShizhijiaApi.getGlamourDetail(context, glamourId) }

    val slotLabels = mapOf(
        "MAIN_HAND" to "主手", "OFF_HAND" to "副手", "HEAD" to "头部", "EARS" to "耳坠",
        "BODY" to "上衣", "NECK" to "项链", "GLOVES" to "手部", "WRISTS" to "手镯",
        "LEGS" to "腿部", "FINGER_LEFT" to "戒指", "FEET" to "脚部", "FINGER_RIGHT" to "戒指",
        "GLASSES" to "面部配饰", "ORNAMENT" to "时尚配饰",
    )
    val leftSlots = listOf("MAIN_HAND", "HEAD", "BODY", "GLOVES", "LEGS", "FEET", "GLASSES")
    val rightSlots = listOf("OFF_HAND", "EARS", "NECK", "WRISTS", "FINGER_LEFT", "FINGER_RIGHT", "ORNAMENT")

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
        LazyColumn(Modifier.fillMaxSize()) {
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
                            onClick = { url -> SzjViewer.url = url },
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
                                        .background(if (on) SzjAccent else SzjHairline)
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
                    SzjShard(widthDp = 2, heightDp = 12)
                    Spacer(Modifier.width(7.dp))
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
                                                                val dyeColor = dy.color.takeIf { it.startsWith("#") }?.let { runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: SzjCardRaised
                                                                Box(Modifier.size(10.dp).clip(CircleShape).background(dyeColor).border(0.5.dp, SzjMuted, CircleShape))
                                                                Spacer(Modifier.width(3.dp))
                                                                Text(dy.name.removeSuffix("染剂"), color = SzjMuted, fontSize = 10.sp, maxLines = 1)
                                                            } else {
                                                                Text("⊘", color = SzjMuted, fontSize = 11.sp)
                                                                Spacer(Modifier.width(2.dp))
                                                                Text("无染色", color = SzjMuted, fontSize = 10.sp)
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
                Spacer(Modifier.height(8.dp))
                Row(Modifier.padding(horizontal = 16.dp)) {
                    Text("♥ ${d.likes}", color = SzjMuted, fontSize = 13.sp)
                    Spacer(Modifier.width(14.dp))
                    Text("★ ${d.favorites}", color = SzjMuted, fontSize = 13.sp)
                }
                Spacer(Modifier.height(20.dp))
            }
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
    var sort by gs.sort      // 0=推荐 1=最新
    var items by gs.items
    var loading by gs.loading
    var page by gs.page
    var ended by gs.ended
    val gridState = gs.gridState
    // 筛选: 种族 / 性别 / 发布时间
    var raceId by gs.raceId
    var genderId by gs.genderId
    var createTimeIdx by gs.createTimeIdx
    var filterOpen by gs.filterOpen
    val createTimeValues = listOf("all", "last24H", "lastWeek", "lastMonth")

    fun load(reset: Boolean) {
        if (loading) return
        if (reset) { page = 1; ended = false; items = emptyList() }
        loading = true
        scope.launch {
            val next = if (tab == 1) ShizhijiaApi.getFollowGlamours(context, page)
            else ShizhijiaApi.getGlamours(context, page, order = if (sort == 1) "time" else "", raceId = raceId, genderId = genderId, createTime = createTimeValues[createTimeIdx])
            items = items + next
            if (next.isEmpty()) ended = true else page += 1
            loading = false
        }
    }
    // Reload only when the channel/filters changed; returning from a detail
    // page keeps the loaded feed, scroll position and active tab.
    LaunchedEffect(tab, sort, raceId, genderId, createTimeIdx) {
        val key = "$tab-$sort-$raceId-$genderId-$createTimeIdx"
        if (gs.loadedKey.value != key || items.isEmpty()) {
            gs.loadedKey.value = key
            load(reset = true)
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
                            val filtered = raceId != -1 || genderId != -1 || createTimeIdx != 0
                            SzjPressable(onClick = { filterOpen = !filterOpen }, shape = SzjChipShape) {
                                Row(
                                    Modifier.clip(SzjChipShape)
                                        .background(if (filtered) SzjAccent else SzjCardRaised)
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("筛选", style = SzjLabelStyle, color = if (filtered) SzjOnAccent else SzjMuted)
                                }
                            }
                        }
                        if (tab == 0) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.End) {
                                Row(Modifier.clip(SzjChipShape).background(SzjCardRaised)) {
                                    listOf("推荐" to 0, "最新" to 1).forEach { (label, id) ->
                                        SzjSmallOption(label, sort == id) { if (sort != id) sort = id }
                                    }
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
                    items(items.size, key = { items[it].id }) { idx ->
                        SzjRise(idx) { SzjGlamourCardItem(items[idx], nav) }
                    }
                }
            }
            // 滚动到底自动加载下一页
            val nearEnd by remember { derivedStateOf {
                val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                items.isNotEmpty() && last >= items.size - 3
            } }
            LaunchedEffect(nearEnd, loading, ended) {
                if (nearEnd && !loading && !ended) load(reset = false)
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
                        .shadow(12.dp, RoundedCornerShape(bottomEnd = 18.dp, bottomStart = 18.dp), ambientColor = Color(0xFF0A1016), spotColor = Color(0xFF0A1016))
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
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SzjPressable(
                            onClick = { raceId = -1; genderId = -1; createTimeIdx = 0 },
                            modifier = Modifier.weight(1f),
                            shape = SzjInnerShape,
                        ) {
                            Text("重置", color = SzjMuted, style = SzjLabelStyle, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().clip(SzjInnerShape)
                                    .border(1.dp, SzjHairline, SzjInnerShape).padding(vertical = 11.dp))
                        }
                        SzjPressable(
                            onClick = {
                                filterOpen = false
                                gs.loadedKey.value = ""
                                load(reset = true)
                            },
                            modifier = Modifier.weight(1f),
                            shape = SzjInnerShape,
                        ) {
                            Text("看结果", color = SzjOnAccent, style = SzjLabelStyle, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().clip(SzjInnerShape).background(SzjAccent).padding(vertical = 11.dp))
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            SzjShard(widthDp = 2, heightDp = 11)
            Spacer(Modifier.width(7.dp))
            Text(label, color = SzjText, style = SzjLabelStyle)
        }
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

/** 幻化瀑布流卡片：封面限高裁切 + 标题 + 作者/服务器 + 收藏/点赞。 */
@Composable
private fun SzjGlamourCardItem(card: ShizhijiaGlamourCard, nav: (SzjRoute) -> Unit) {
    SzjCardSurface(onClick = { nav(SzjRoute.GlamourDetail(card.id)) }) {
        // 封面顶部跟着卡片圆角裁一下，不然图片方角会顶出卡片边缘。
        Box(Modifier.clip(RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp))) {
            SzjGlamourImage(url = card.mainImage)
        }
        Column(Modifier.padding(horizontal = 9.dp, vertical = 9.dp)) {
            Text(card.title, color = SzjText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(card.characterName, color = SzjMuted, style = SzjMetaStyle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (card.groupName.isNotBlank()) {
                    Spacer(Modifier.width(2.dp))
                    SzjLocPin(12)
                    Text(card.groupName, color = SzjMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("★ ${card.favorites}", color = SzjMuted, style = SzjMetaStyle)
                Text(" · ", color = SzjMuted, style = SzjMetaStyle)
                Text("♥ ${card.likes}", color = SzjMuted, style = SzjMetaStyle)
            }
        }
    }
}
