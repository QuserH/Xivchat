package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.wiki.AncestorCell
import com.quserh.eorzeaphone.data.wiki.AncestorTree
import com.quserh.eorzeaphone.data.wiki.QuestAncestry
import com.quserh.eorzeaphone.data.wiki.QuestChainMeta
import com.quserh.eorzeaphone.data.wiki.QuestDb
import com.quserh.eorzeaphone.data.wiki.QuestHit
import com.quserh.eorzeaphone.data.wiki.QuestNode
import com.quserh.eorzeaphone.data.wiki.QuestTree
import com.quserh.eorzeaphone.data.wiki.withLabels
import com.quserh.eorzeaphone.ui.theme.BrandFill
import com.quserh.eorzeaphone.ui.theme.BrandOnFill
import com.quserh.eorzeaphone.ui.theme.CanvasControlScrim
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import com.quserh.eorzeaphone.ui.theme.OnCanvasScrim
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneHairline
import com.quserh.eorzeaphone.ui.theme.PhoneInfo
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.PhoneWarn

/**
 * 任务流程树。
 *
 * 为什么整棵树画在一个 Canvas 里，而不是每个节点一个 composable：
 * 最大的块有 255 个任务（重生之境主线 215 个、159 层），
 * 几百个 composable 的测量/布局在缩放时每帧都要走一遍。
 * Canvas 一次画完，缩放只是改 scale，点击靠命中测试反算逻辑坐标。
 *
 * 布局坐标（col/row/tier）由 quest_chain.py 在构建期算好存库，
 * 这里只把网格坐标乘成像素。
 */

// 一个节点的格子尺寸与间距（逻辑 dp，缩放前）
private const val CELL_W = 104f
private const val CELL_H = 40f
private const val GAP_X = 10f
private const val GAP_Y = 20f
private const val PITCH_X = CELL_W + GAP_X
private const val PITCH_Y = CELL_H + GAP_Y

/** 左侧留给层号（「第 N 级」）的宽度。 */
private const val GUTTER = 34f

/**
 * 根（入口任务）一行最多几个 —— 上限，实际按块的宽度收窄，见 [placeTree]。
 * 和 quest_chain.py 的 WRAP 一致。
 */
private const val ROOT_WRAP = 10

/** 低于这个缩放就不画字了 —— 再小也读不出来，只留框和高亮。 */
private const val TEXT_MIN_SCALE = 0.42f

/** 一个已经摆好像素位置的节点。 */
private data class Placed(
    val node: QuestNode,
    val rect: Rect,
    val isRoot: Boolean,
)

/**
 * 把树摆成像素坐标。
 *
 * 根占最上面几行（多根时按 [ROOT_WRAP] 折行），块内节点接在下面，
 * 行号沿用库里的 row（宽层已经在构建期折过行了）。
 */
private fun placeTree(tree: QuestTree): Pair<List<Placed>, Size> {
    // 根按**块的宽度**折行，而不是固定 10 个一行。
    //
    // 真机上撞出来的：「大地使者职业任务」块内只有 3 列，但有 9 个入口。
    // 固定 10 一行时画布宽度被 9 个根撑到 9 列，按宽度铺满后主体那 3 列
    // 只占左边 1/3，右边 60% 全是空白，放大到 90% 更明显。
    // 收窄到块宽后 9 个根变成 3×3，画布宽度回到 3 列，主体铺满。
    // 收到块宽（下界 1，不是 2）：全 468 个块横向零浪费。下界取 2 时还有
    // 47 个块是「主体 1 列 / 画布 2 列」，白扔一半宽度。
    // 代价是主体 1 列又有多个入口时入口会叠成多行，但实测只有 4 个块的
    // 入口超过 3 行，最多的那个（11 行）本身只有 1 个任务 ——
    // 「这 11 个里做任一个都能解锁它」，叠着显示才是实情。
    val bodyCols = (tree.nodes.maxOfOrNull { it.col } ?: 0) + 1
    val rootWrap = bodyCols.coerceIn(1, ROOT_WRAP)

    val rootRows = if (tree.roots.isEmpty()) 0
    else (tree.roots.size + rootWrap - 1) / rootWrap
    // 根和块之间空一行，让「入口 → 这个块」看得出断开
    val bodyTop = if (rootRows > 0) rootRows + 1 else 0

    val out = mutableListOf<Placed>()
    tree.roots.forEachIndexed { i, n ->
        out += Placed(n, cellRect(i % rootWrap, i / rootWrap), isRoot = true)
    }
    tree.nodes.forEach { n ->
        out += Placed(n, cellRect(n.col, bodyTop + n.row), isRoot = false)
    }

    val maxCol = out.maxOfOrNull { (it.rect.left - GUTTER) / PITCH_X } ?: 0f
    val maxRow = out.maxOfOrNull { it.rect.top / PITCH_Y } ?: 0f
    val w = GUTTER + (maxCol + 1) * PITCH_X
    val h = (maxRow + 1) * PITCH_Y
    return out to Size(w, h)
}

private fun cellRect(col: Int, row: Int) = Rect(
    left = GUTTER + col * PITCH_X,
    top = row * PITCH_Y,
    right = GUTTER + col * PITCH_X + CELL_W,
    bottom = row * PITCH_Y + CELL_H,
)

/** 画树用到的一组颜色。Canvas lambda 不是 composable，得先在外面取好。 */
private data class TreeInk(
    val node: Color,
    val nodeRoot: Color,
    /** 高亮节点的实心填充。 */
    val nodeHit: Color,
    /** 落在 [nodeHit] 上的字色。 */
    val onHit: Color,
    val nodeSel: Color,
    val border: Color,
    val borderHit: Color,
    val text: Color,
    val textDim: Color,
    val edge: Color,
    val edgeExt: Color,
    val edgeOr: Color,
    val tier: Color,
)

/**
 * 固定窗口里的可缩放树。窗口高度由 [heightDp] 定，内容缩放/平移都在窗口内。
 *
 * [highlightId] 是搜索命中的任务，画成实心强调色。
 * [selectedId] 是当前点选的，画成描边强调色。
 */
@Composable
private fun QuestTreeCanvas(
    tree: QuestTree,
    highlightId: Int,
    selectedId: Int,
    heightDp: Int,
    view: QuestTreeView,
    onPick: (QuestNode) -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    val (placed, logical) = remember(tree) { placeTree(tree) }
    val byId = remember(tree) { placed.associateBy { it.node.id } }
    // 每层最上沿。drawTree 是每帧跑的，这个不能放在里面算 —— 见那边的注释。
    val tierTop: Map<Int, Float> = remember(tree) {
        placed.filter { !it.isRoot }
            .groupBy { it.node.tier }
            .mapValues { (_, v) -> v.minOf { it.rect.top } }
    }

    // 节点名只量一次。255 个节点在缩放时每帧重量会明显掉帧。
    val labels: Map<Int, TextLayoutResult> = remember(tree) {
        placed.associate { p ->
            p.node.id to measurer.measure(
                text = tree.labelOf(p.node),
                style = TextStyle(fontSize = 10.sp, lineHeight = 12.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(
                    maxWidth = with(density) { (CELL_W - 26f).dp.roundToPx() },
                ),
            )
        }
    }
    val tierLabels: Map<Int, TextLayoutResult> = remember(tree) {
        placed.filter { !it.isRoot }.map { it.node.tier }.distinct().associateWith { t ->
            measurer.measure(
                "${t + 1}",
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
    val levelLabels: Map<Int, TextLayoutResult> = remember(tree) {
        placed.map { it.node.level }.distinct().associateWith { lv ->
            measurer.measure(
                if (lv > 0) lv.toString() else "-",
                style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold),
            )
        }
    }

    // 全部从主题色推导，不写死 ARGB。
    //
    // 真机撞出来的：原来 border 用 0x24FFFFFF、edge 用 0x59FFFFFF（白色半透明），
    // 在**浅色主题**下等于隐形 —— 3421 条块内前置边一条都看不见，
    // 树上只剩根那几条蓝虚线，看着像没有父子关系。
    val ink = TreeInk(
        node = PhoneSurfaceRaised,
        nodeRoot = PhoneInfo.copy(alpha = 0.22f),
        // 高亮节点是**实心填充**，所以底用 BrandFill、字用 onHit(=BrandOnFill)。
        // 原来底是 PhoneAccent + 白字：PhoneAccent 是字色，深色主题下它是
        // inkDark（浅色），白字压上去 1.81:1，9 个预设全部不达标。见 §5.3。
        nodeHit = BrandFill,
        onHit = BrandOnFill,
        nodeSel = PhoneAccent.copy(alpha = 0.20f),
        // 节点框是「控件收边」那一档，用项目的 PhoneHairline
        border = PhoneHairline,
        borderHit = PhoneAccent,
        text = PhoneText,
        textDim = PhoneMuted,
        // 用户反馈「线颜色不明显」。原来内部边是 PhoneMuted 70%，在浅色主题的
        // 浅灰底上几乎看不出来。改成不透明的 PhoneMuted，跨块边和「或」边
        // 也各自提到全不透明，靠颜色+虚线区分而不是靠深浅。
        edge = PhoneMuted,
        edgeExt = PhoneInfo,
        edgeOr = PhoneWarn,
        tier = PhoneMuted.copy(alpha = 0.55f),
    )

    // 初值从 view 取回来（跨导航保留），改动同时写回去。
    // 0 = 还没定初值，下面的 LaunchedEffect 会按宽度铺满并对准高亮节点。
    var scale by remember(tree.meta.id) { mutableStateOf(view.scale) }
    var panX by remember(tree.meta.id) { mutableStateOf(view.panX) }
    var panY by remember(tree.meta.id) { mutableStateOf(view.panY) }
    // 每次重组都同步回持有者。比在十来处赋值点各写一次可靠。
    view.scale = scale
    view.panX = panX
    view.panY = panY

    BoxWithConstraints(
        Modifier.fillMaxWidth().height(heightDp.dp)
            .clip(RoundedCornerShape(10.dp)).background(PhoneSurface),
    ) {
        val viewW = with(density) { maxWidth.toPx() }
        val viewH = with(density) { maxHeight.toPx() }
        val logW = with(density) { logical.width.dp.toPx() }
        val logH = with(density) { logical.height.dp.toPx() }

        // 初始视图：按宽度铺满（不按整体铺满 —— 主线 159 层，
        // 整体缩到能看全时字只有零点几像素，等于一片糊）。
        // 然后把高亮的那个节点滚到视野中间。
        LaunchedEffect(tree.meta.id, viewW, logW) {
            if (scale != 0f) return@LaunchedEffect
            val fitW = (viewW / logW).coerceIn(0.35f, 1.25f)
            scale = fitW
            panX = (viewW - logW * fitW) / 2f
            val focus = byId[highlightId] ?: byId[selectedId]
            panY = if (focus != null) {
                val cy = with(density) { focus.rect.center.y.dp.toPx() } * fitW
                (viewH / 2f - cy).coerceAtMost(0f)
            } else 0f
            // 内容比窗口矮时顶部对齐，别悬在中间
            if (logH * fitW <= viewH) panY = 0f
        }

        val transform = rememberTransformableState { zoom, pan, _ ->
            val next = (scale * zoom).coerceIn(0.2f, 2.2f)
            // 以窗口中心为锚缩放，否则捏合会把内容甩走
            val cx = viewW / 2f
            val cy = viewH / 2f
            panX = cx - (cx - panX) * (next / scale)
            panY = cy - (cy - panY) * (next / scale)
            scale = next
            panX += pan.x
            panY += pan.y
            // 留半屏余量，允许拖过边一点，但不让内容整个飞出去
            val slackX = viewW * 0.5f
            val slackY = viewH * 0.5f
            panX = panX.coerceIn(
                minOf(viewW - logW * scale - slackX, 0f),
                maxOf(slackX, viewW - logW * scale),
            )
            panY = panY.coerceIn(
                minOf(viewH - logH * scale - slackY, 0f),
                maxOf(slackY, viewH - logH * scale),
            )
        }

        val s = scale.takeIf { it > 0f } ?: 1f
        Canvas(
            Modifier.fillMaxSize().transformable(transform)
                .pointerInput(tree.meta.id, s, panX, panY) {
                    detectTapGestures { tap ->
                        // 屏幕 → 逻辑：先去掉平移再去掉缩放
                        val lx = (tap.x - panX) / s
                        val ly = (tap.y - panY) / s
                        val dx = with(density) { lx.toDp().value }
                        val dy = with(density) { ly.toDp().value }
                        placed.firstOrNull { it.rect.contains(Offset(dx, dy)) }
                            ?.let { onPick(it.node) }
                    }
                },
        ) {
            withTransform({
                translate(panX, panY)
                scale(s, s, pivot = Offset.Zero)
            }) {
                drawTree(
                    tree, placed, byId, labels, tierLabels, levelLabels,
                    highlightId, selectedId, ink, s, tierTop,
                )
            }
        }

        // 缩放比例 + 复位。固定窗口里没有滚动条，给个能回到原位的出口。
        Row(
            Modifier.align(Alignment.BottomEnd).padding(7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 底衬是固定的深色遮罩，所以字要固定白色，不能用 PhoneText/PhoneMuted ——
            // 浅色主题下那两个是近黑色，压在深色遮罩上等于看不见（真机撞出来的）。
            Text(
                "${(s * 100).toInt()}%",
                color = OnCanvasScrim, fontSize = 9.sp,
                modifier = Modifier.clip(RoundedCornerShape(5.dp))
                    .background(CanvasControlScrim).padding(horizontal = 5.dp, vertical = 2.dp),
            )
            TreeZoomButton("−") {
                val next = (scale / 1.3f).coerceIn(0.2f, 2.2f)
                panX = viewW / 2f - (viewW / 2f - panX) * (next / s)
                panY = viewH / 2f - (viewH / 2f - panY) * (next / s)
                scale = next
            }
            TreeZoomButton("+") {
                val next = (scale * 1.3f).coerceIn(0.2f, 2.2f)
                panX = viewW / 2f - (viewW / 2f - panX) * (next / s)
                panY = viewH / 2f - (viewH / 2f - panY) * (next / s)
                scale = next
            }
            TreeZoomButton("⤢") {
                // 整体铺满，看全局用
                val fit = minOf(viewW / logW, viewH / logH).coerceIn(0.2f, 2.2f)
                scale = fit
                panX = (viewW - logW * fit) / 2f
                panY = 0f
            }
        }
    }
}

/**
 * 画整棵树：层带 → 边 → 节点。顺序要紧，边不能压在节点上面。
 *
 * 坐标都是 dp 数值（[placeTree] 的输出），在这里转 px ——
 * DrawScope 本身实现了 Density，所以 `x.dp.toPx()` 直接可用。
 */
private fun DrawScope.drawTree(
    tree: QuestTree,
    placed: List<Placed>,
    byId: Map<Int, Placed>,
    labels: Map<Int, TextLayoutResult>,
    tierLabels: Map<Int, TextLayoutResult>,
    levelLabels: Map<Int, TextLayoutResult>,
    highlightId: Int,
    selectedId: Int,
    ink: TreeInk,
    scale: Float,
    /**
     * 每层最上沿的 y（逻辑 dp），画层带用。
     *
     * **必须由外面 remember 好传进来**：这个函数是 DrawScope 扩展，
     * 缩放/平移时每帧都跑一次。原来在这里 `placed.filter{}.groupBy{}`，
     * 最大的块 255 个节点，等于捏合时每秒做 60 次 filter + groupBy。
     */
    tierTop: Map<Int, Float>,
) {
    val showText = scale >= TEXT_MIN_SCALE

    // --- 层带：每层一条淡线 + 左侧层号，保住「层级递进」的读法 ---
    tierTop.forEach { (tier, top) ->
        val y = (top - GAP_Y / 2f).dp.toPx()
        drawLine(
            ink.tier.copy(alpha = 0.16f),
            Offset(0f, y), Offset(size.width, y),
            strokeWidth = 1f,
        )
        if (showText) {
            tierLabels[tier]?.let { tl ->
                drawText(
                    tl, color = ink.tier,
                    topLeft = Offset(4.dp.toPx(), (top + 4f).dp.toPx()),
                )
            }
        }
    }

    // --- 边 ---
    // dash 和 path 都在循环外建、循环内复用：最大的块有 174 条边，
    // 每条 new 一个 Path 的话捏合时每秒要扔掉一万个对象。
    val dash = PathEffect.dashPathEffect(
        floatArrayOf(5.dp.toPx(), 4.dp.toPx()), 0f,
    )
    val edgePath = Path()
    tree.edges.forEach { e ->
        val child = byId[e.questId] ?: return@forEach
        val parent = byId[e.preId] ?: return@forEach
        val isOr = child.node.isOrPrereq
        val color = when {
            e.external -> ink.edgeExt
            isOr -> ink.edgeOr
            else -> ink.edge
        }
        val emph = e.questId == highlightId || e.preId == highlightId ||
            e.questId == selectedId || e.preId == selectedId
        // 线宽也加粗一档（1.4→2.0，强调 2.2→3.2）：细线在缩到 50% 以下时
        // 会被抗锯齿糊掉，这也是「看不明显」的一部分
        drawElbow(
            from = parent.rect, to = child.rect,
            color = color,
            width = if (emph) 3.2f else 2.0f,
            effect = if (e.external || isOr) dash else null,
            path = edgePath,
        )
    }

    // --- 节点 ---
    placed.forEach { p ->
        val hit = p.node.id == highlightId
        val sel = p.node.id == selectedId && !hit
        val r = Rect(
            p.rect.left.dp.toPx(), p.rect.top.dp.toPx(),
            p.rect.right.dp.toPx(), p.rect.bottom.dp.toPx(),
        )
        val radius = CornerRadius(6.dp.toPx())
        val fill = when {
            hit -> ink.nodeHit
            sel -> ink.nodeSel
            p.isRoot -> ink.nodeRoot
            else -> ink.node
        }
        drawRoundRect(fill, r.topLeft, r.size, radius)
        drawRoundRect(
            if (hit || sel) ink.borderHit else ink.border,
            r.topLeft, r.size, radius,
            style = Stroke((if (hit || sel) 2f else 1f).dp.toPx()),
        )

        // 左侧等级带。同样不写死白色 —— 浅色主题下白色半透明压在浅底上看不见。
        // 命中的那个底是强调色，压一层黑；其余压一层 textDim。
        val bandW = 22.dp.toPx()
        if (p.node.level > 0) {
            val bandColor =
                if (hit) Color(0x33000000) else ink.textDim.copy(alpha = 0.14f)
            drawRoundRect(bandColor, r.topLeft, Size(bandW, r.height), radius)
        }

        if (!showText) return@forEach
        levelLabels[p.node.level]?.let { ll ->
            drawText(
                ll,
                color = if (hit) ink.onHit else ink.textDim,
                topLeft = Offset(
                    r.left + (bandW - ll.size.width) / 2f,
                    r.top + (r.height - ll.size.height) / 2f,
                ),
            )
        }
        labels[p.node.id]?.let { tl ->
            drawText(
                tl,
                color = if (hit) ink.onHit else ink.text,
                topLeft = Offset(
                    r.left + bandW + 3.dp.toPx(),
                    r.top + (r.height - tl.size.height) / 2f,
                ),
            )
        }
    }
}

/**
 * 父 → 子的直角折线：从父底边中点向下、横移、再向下进子顶边中点。
 *
 * 用折线不用斜线：斜线在多层交错时看不出是谁连谁，
 * 直角折线是流程图的常规画法，层级关系更清楚。
 */
private fun DrawScope.drawElbow(
    from: Rect,
    to: Rect,
    color: Color,
    width: Float,
    effect: PathEffect?,
    /** 复用的 Path，调用方在循环外建一个。每条边 new 一个会造成明显 GC 压力。 */
    path: Path,
) {
    val x1 = from.center.x.dp.toPx()
    val y1 = from.bottom.dp.toPx()
    val x2 = to.center.x.dp.toPx()
    val y2 = to.top.dp.toPx()
    // 子在父上方（跨块边可能这样）时直接连直线，折线会绕回去更乱
    if (y2 <= y1) {
        drawLine(color, Offset(x1, y1), Offset(x2, y2),
            strokeWidth = width.dp.toPx(), pathEffect = effect)
        return
    }
    // 横线贴着子节点上方走，不走父/子正中：祖先图里顶层和子层之间
    // 隔着大片空白，正中横线会从空白区穿过（用户截图里的乱线）。
    // 长的竖线垂直穿过空白（平行可读），横线只在子卡片上方一小段。
    val midY = y2 - 12.dp.toPx()
    path.reset()
    path.moveTo(x1, y1)
    path.lineTo(x1, midY)
    path.lineTo(x2, midY)
    path.lineTo(x2, y2)
    drawPath(path, color, style = Stroke(width.dp.toPx(), pathEffect = effect))
}

@Composable
private fun TreeZoomButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(24.dp).clip(RoundedCornerShape(6.dp))
            .background(CanvasControlScrim).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // 同上：深色遮罩上的字固定白色
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------------------
// 树屏
// ---------------------------------------------------------------------------

/**
 * 一个块的完整任务流程图。
 *
 * [highlightId] = 搜索命中的任务，进来就高亮并滚到视野中间。
 */
@Composable
internal fun QuestTreeScreen(
    state: PhoneState,
    chainId: Int,
    highlightId: Int,
    /** 跨导航保留的浏览状态。进详情再返回时缩放/平移不丢。 */
    view: QuestTreeView,
    onBack: () -> Unit,
    onOpen: (WikiDest) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var tree by remember(chainId) { mutableStateOf<QuestTree?>(null) }
    var loading by remember(chainId) { mutableStateOf(true) }
    var picked by remember(chainId) { mutableStateOf(view.picked) }

    LaunchedEffect(chainId) {
        loading = true
        tree = runCatching { QuestDb.tree(context, chainId) }.getOrNull()
        loading = false
    }

    val t = tree
    when {
        loading -> WikiLoadingScreen("任务流程", state, onBack)
        t == null -> WikiMissingScreen("任务流程 $chainId", state, onBack)
        else -> {
            val margin = LocalContentMargin.current
            val sel = picked.takeIf { it != 0 } ?: highlightId
            val selNode = t.byId[sel]
            ScreenFrame {
                ScreenHeader(
                    t.meta.title.ifBlank { "任务流程" }, state, onBack = onBack,
                    trailing = {
                        Text("${t.meta.members} 个任务", color = PhoneMuted, fontSize = 11.sp)
                    },
                )
                Column(
                    Modifier.fillMaxSize().padding(horizontal = margin.dp),
                ) {
                    Text(
                        buildList {
                            // 起点名：同标题的块有 26 个的情况，标题本身不够定位
                            if (t.meta.leadName.isNotBlank() &&
                                t.meta.leadName != t.meta.title
                            ) {
                                add("起点「${t.meta.leadName}」")
                            }
                            add("${t.meta.layers} 级递进")
                            if (t.meta.minLevel > 0) {
                                add("Lv ${t.meta.minLevel}-${t.meta.maxLevel}")
                            }
                            if (t.roots.isNotEmpty()) add("${t.roots.size} 个入口")
                        }.joinToString(" · "),
                        color = PhoneMuted, fontSize = 11.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp, bottom = 7.dp),
                    )

                    QuestTreeCanvas(
                        tree = t,
                        highlightId = highlightId,
                        selectedId = sel,
                        heightDp = 340,
                        view = view,
                        onPick = { picked = it.id; view.picked = it.id },
                    )

                    QuestTreeLegend(hasRoot = t.roots.isNotEmpty())

                    if (selNode != null) {
                        QuestNodeCard(
                            node = selNode,
                            label = t.labelOf(selNode),
                            isRoot = t.roots.any { it.id == selNode.id },
                            onDetail = { onOpen(WikiDest.Quest(selNode.id)) },
                        )
                    } else {
                        Text(
                            "点树上的任务看详情",
                            color = PhoneMuted, fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/** 图例。虚线有两个意思（入口边和「或」前置），不写清楚会被当成同一种。 */
@Composable
private fun QuestTreeLegend(hasRoot: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(top = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasRoot) LegendDot(PhoneInfo, "入口（前置主线）")
        LegendDot(PhoneWarn, "满足其一即可")
        Spacer(Modifier.weight(1f))
        Text("双指缩放 · 拖动平移", color = PhoneMuted, fontSize = 9.sp)
    }
}

@Composable
private fun LegendDot(tint: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(tint))
        Text(text, color = PhoneMuted, fontSize = 9.sp,
            modifier = Modifier.padding(start = 4.dp))
    }
}

/** 选中节点的卡片：名字、等级、接取位置，以及进详情的入口。 */
@Composable
private fun QuestNodeCard(
    node: QuestNode,
    label: String,
    isRoot: Boolean,
    onDetail: () -> Unit,
) {
    // 用 PhoneCard 而不是自己 clip+background：它带按下缩放、阴影、
    // 浅色主题下的收边，和石之家/设置页/商店是同一套观感。
    // HANDOFF.md §6：壳层唯一的卡片原语就是它。
    PhoneCard(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        onClick = onDetail,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (node.iconId > 0) {
                    ItemIcon(node.iconId, Modifier.size(28.dp), node.name.take(1))
                    Spacer(Modifier.width(9.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        label.ifBlank { "任务 ${node.id}" },
                        color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        node.subtitle, color = PhoneMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                if (isRoot) {
                    Text(
                        "入口", color = PhoneInfo, fontSize = 9.sp,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(PhoneInfo.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
            }
            if (node.startNpc.hasPlace) {
                Text(
                    "接取　${node.startNpc.nameText}　${node.startNpc.placeText}",
                    color = PhoneMuted, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
            Text(
                "查看完整详情 ›", color = PhoneAccent, fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 完整前置线（跨块，传递闭包）
// ---------------------------------------------------------------------------

/**
 * 一个任务的**完整前置线**。
 *
 * 和 [QuestTreeScreen] 的区别：那个画一个块，往上只到一级入口；
 * 这个沿 `quest_prereq` 一路上溯到没有前置为止，跨块。长直链会折叠成
 * 「⋯ N 个 ⋯」，点开列出里面的任务。理由和实测数字见 [QuestAncestry]。
 */
@Composable
internal fun QuestAncestryScreen(
    state: PhoneState,
    questId: Int,
    view: QuestTreeView,
    onBack: () -> Unit,
    onOpen: (WikiDest) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var tree by remember(questId) { mutableStateOf<AncestorTree?>(null) }
    var loading by remember(questId) { mutableStateOf(true) }
    var pickedCell by remember(questId) { mutableStateOf(-1) }

    LaunchedEffect(questId) {
        if (pickedCell != -1) pickedCell = -1

        loading = true
        tree = runCatching { QuestAncestry.of(context, questId) }.getOrNull()
        loading = false
    }

    val t = tree
    when {
        loading -> WikiLoadingScreen("前置任务线", state, onBack)
        t == null -> WikiMissingScreen("任务 $questId", state, onBack)
        else -> {
            val margin = LocalContentMargin.current
            ScreenFrame {
                ScreenHeader(
                    t.target.name.ifBlank { "任务 $questId" }, state, onBack = onBack,
                    trailing = {
                        Text(
                            if (t.totalAncestors == 0) "无前置"
                            else "${t.totalAncestors} 个前置",
                            color = PhoneMuted, fontSize = 11.sp,
                        )
                    },
                )
                Column(Modifier.fillMaxSize().padding(horizontal = margin.dp)) {
                    Text(
                        buildList {
                            if (t.targetIsMsq) {
                                add("这是主线任务，只显示直接前置")
                            } else {
                                add("上溯到前置主线")
                            }
                            if (t.collapsedCount > 0) {
                                add("已折叠 ${t.collapsedCount} 个直链")
                            }
                            add("${t.rows} 层")
                        }.joinToString(" · "),
                        color = PhoneMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 7.dp),
                    )

                    // 点任意任务：亮「最短依赖树」——每条前置依赖一条线到顶部主线。
                    //
                    // Nothing picked yet (just opened): default the highlight source to the
                    // target quest, so the whole upward tree is lit immediately. Falling back
                    // to the edges' own onPath flag lit only ONE parent per node
                    // (QuestAncestry step 5 keeps maxBy depth), so a deep tree opened from
                    // search showed most upper branches dimmed until you tapped something.
                    val (highlightCells, highlightEdges) = remember(t, pickedCell) {
                        val from =
                            if (pickedCell >= 0) pickedCell
                            else t.cells.indexOfFirst { it is AncestorCell.Quest && it.isTarget }
                        if (from < 0) emptySet<Int>() to emptySet<Pair<Int, Int>>()
                        else QuestAncestry.pathFrom(t, from)
                    }
                    AncestorCanvas(
                        tree = t,
                        selectedCell = pickedCell,
                        highlightCells = highlightCells,
                        highlightEdges = highlightEdges,
                        heightDp = 360,
                        view = view,
                        onPick = { pickedCell = it },
                    )

                    Row(
                        Modifier.fillMaxWidth().padding(top = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        LegendDot(BrandFill, "这个任务")
                        if (t.msqTops.isNotEmpty()) LegendDot(PhoneWarn, "前置主线")
                        if (t.collapsedCount > 0) LegendDot(PhoneInfo, "折叠的直链")
                        Spacer(Modifier.weight(1f))
                        Text("双指缩放 · 拖动", color = PhoneMuted, fontSize = 9.sp)
                    }

                    val cell = t.cells.getOrNull(pickedCell)
                    when (cell) {
                        is AncestorCell.Quest -> {
                            QuestNodeCard(
                                node = cell.node,
                                label = cell.node.name,
                                isRoot = cell.isEntry || cell.isMsqTop,
                                onDetail = { onOpen(WikiDest.Quest(cell.node.id)) },
                            )
                            // 顶部主线：链没展开，给一个「自己展开」的入口。
                            // 用户明确要的：主线链让玩家自己展开，不自动铺出来。
                            if (cell.isMsqTop) {
                                PhoneCard(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    onClick = { onOpen(WikiDest.Ancestry(cell.node.id)) },
                                ) {
                                    Row(
                                        Modifier.padding(11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "这是前置主线",
                                                color = PhoneText, fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                "它自己的主线链没有展开",
                                                color = PhoneMuted, fontSize = 10.sp,
                                                modifier = Modifier.padding(top = 2.dp),
                                            )
                                        }
                                        Text(
                                            "往上看 ›", color = PhoneAccent, fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
                        }
                        is AncestorCell.Run -> AncestorRunCard(cell, onOpen)
                        null -> Text(
                            "点上面的任务看详情",
                            color = PhoneMuted, fontSize = 11.sp,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 祖先树画布。结构和 [QuestTreeCanvas] 一样（单 Canvas + 缩放 + 命中测试），
 * 但画的是 [AncestorTree]，而且**连线明显加粗加深**了 ——
 * 用户反馈原来的线「颜色也不明显」。
 */
@Composable
private fun AncestorCanvas(
    tree: AncestorTree,
    selectedCell: Int,
    highlightCells: Set<Int>,
    highlightEdges: Set<Pair<Int, Int>>,
    heightDp: Int,
    view: QuestTreeView,
    onPick: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()

    val rects = remember(tree) {
        tree.cells.map { cellRect(it.col, it.row) }
    }
    val labels = remember(tree) {
        tree.cells.map { c ->
            val text = when (c) {
                is AncestorCell.Quest -> c.node.name.ifBlank { "任务 ${c.node.id}" }
                is AncestorCell.Run -> "⋯ ${c.count} 个 ⋯"
            }
            measurer.measure(
                text = text,
                style = TextStyle(fontSize = 10.sp, lineHeight = 12.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                constraints = Constraints(
                    maxWidth = with(density) { (CELL_W - 26f).dp.roundToPx() },
                ),
            )
        }
    }
    val levels = remember(tree) {
        tree.cells.map { c ->
            val s = when (c) {
                is AncestorCell.Quest -> if (c.node.level > 0) c.node.level.toString() else "-"
                is AncestorCell.Run -> "⋯"
            }
            measurer.measure(s, TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Bold))
        }
    }

    // 连线用强调色，比原来的 PhoneMuted 70% 明显得多
    val cPath = PhoneAccent
    val cEdge = PhoneMuted
    val cRun = PhoneInfo
    val cMsq = PhoneWarn        // 顶部前置主线
    val cTarget = BrandFill
    val cOnTarget = BrandOnFill
    val cSurface = PhoneSurfaceRaised
    val cBorder = PhoneHairline
    val cText = PhoneText
    val cDim = PhoneMuted

    var scale by remember(tree.target.id) { mutableStateOf(view.scale) }
    var panX by remember(tree.target.id) { mutableStateOf(view.panX) }
    var panY by remember(tree.target.id) { mutableStateOf(view.panY) }
    view.scale = scale
    view.panX = panX
    view.panY = panY

    BoxWithConstraints(
        Modifier.fillMaxWidth().height(heightDp.dp)
            .clip(RoundedCornerShape(10.dp)).background(PhoneSurface),
    ) {
        val viewW = with(density) { maxWidth.toPx() }
        val viewH = with(density) { maxHeight.toPx() }
        val logW = with(density) { (GUTTER + tree.cols * PITCH_X).dp.toPx() }
        val logH = with(density) { (tree.rows * PITCH_Y).dp.toPx() }

        // 初始：按宽度铺满，并把「这个任务」滚到视野里（它在最底下一层）
        LaunchedEffect(tree.target.id, viewW, logW) {
            if (scale != 0f) return@LaunchedEffect
            val fit = (viewW / logW).coerceIn(0.3f, 1.25f)
            scale = fit
            panX = (viewW - logW * fit) / 2f
            val ti = tree.cells.indexOfFirst {
                it is AncestorCell.Quest && it.isTarget
            }
            panY = if (ti >= 0) {
                val cy = with(density) { rects[ti].center.y.dp.toPx() } * fit
                (viewH / 2f - cy).coerceAtMost(0f)
            } else 0f
            if (logH * fit <= viewH) panY = 0f
        }

        val transform = rememberTransformableState { zoom, pan, _ ->
            val next = (scale * zoom).coerceIn(0.2f, 2.2f)
            val cx = viewW / 2f
            val cy = viewH / 2f
            panX = cx - (cx - panX) * (next / scale)
            panY = cy - (cy - panY) * (next / scale)
            scale = next
            panX += pan.x
            panY += pan.y
            val slackX = viewW * 0.5f
            val slackY = viewH * 0.5f
            panX = panX.coerceIn(
                minOf(viewW - logW * scale - slackX, 0f),
                maxOf(slackX, viewW - logW * scale),
            )
            panY = panY.coerceIn(
                minOf(viewH - logH * scale - slackY, 0f),
                maxOf(slackY, viewH - logH * scale),
            )
        }

        val s = scale.takeIf { it > 0f } ?: 1f
        Canvas(
            Modifier.fillMaxSize().transformable(transform)
                .pointerInput(tree.target.id, s, panX, panY) {
                    detectTapGestures { tap ->
                        val dx = with(density) { ((tap.x - panX) / s).toDp().value }
                        val dy = with(density) { ((tap.y - panY) / s).toDp().value }
                        val i = rects.indexOfFirst { it.contains(Offset(dx, dy)) }
                        if (i >= 0) onPick(i)
                    }
                },
        ) {
            withTransform({
                translate(panX, panY)
                scale(s, s, pivot = Offset.Zero)
            }) {
                val showText = s >= TEXT_MIN_SCALE
                val elbow = Path()

                // --- 边：主线粗且用强调色，其余中性但比原来深 ---
                tree.edges.forEach { e ->
                    val a = rects.getOrNull(e.fromCell) ?: return@forEach
                    val b = rects.getOrNull(e.toCell) ?: return@forEach
                    // 用户点了任务 → 只亮「它到顶部主线」的那几条线，
                    // 其余的线压到几乎看不见（Emil: strongest move is delete）。
                    val lit = if (highlightEdges.isEmpty()) e.onPath
                              else (e.fromCell to e.toCell) in highlightEdges
                    drawElbow(
                        from = a, to = b,
                        color = if (lit) cPath else cEdge.copy(alpha = if (highlightCells.isEmpty()) 0.55f else 0.16f),
                        width = if (lit) 3.2f else 1.4f,
                        effect = null,
                        path = elbow,
                    )
                }

                // --- 格子 ---
                tree.cells.forEachIndexed { i, c ->
                    val lr = rects[i]
                    val r = Rect(
                        lr.left.dp.toPx(), lr.top.dp.toPx(),
                        lr.right.dp.toPx(), lr.bottom.dp.toPx(),
                    )
                    val radius = CornerRadius(6.dp.toPx())
                    val isTarget = c is AncestorCell.Quest && c.isTarget
                    val isRun = c is AncestorCell.Run
                    val sel = i == selectedCell && !isTarget
                    val litCell = i in highlightCells

                    val isMsqTop = c is AncestorCell.Quest && c.isMsqTop
                    // 有高亮路线时，非路线卡片整体退暗（含边框），让亮线独行。
                    val dim = if (highlightCells.isEmpty()) 1f else 0.32f
                    val fill = when {
                        isTarget -> cTarget
                        isMsqTop -> cMsq.copy(alpha = 0.20f * dim)
                        isRun -> cRun.copy(alpha = 0.16f * dim)
                        litCell -> cPath.copy(alpha = 0.13f)
                        else -> cSurface.copy(alpha = dim)
                    }
                    drawRoundRect(fill, r.topLeft, r.size, radius)
                    drawRoundRect(
                        when {
                            isTarget || sel -> cPath
                            isMsqTop -> cMsq
                            litCell -> cPath.copy(alpha = 0.55f)
                            else -> cBorder.copy(alpha = dim)
                        },
                        r.topLeft, r.size, radius,
                        style = Stroke((if (isTarget || sel) 2.4f else if (litCell) 1.6f else 1f).dp.toPx()),
                    )

                    if (!showText) return@forEachIndexed
                    val bandW = 22.dp.toPx()
                    levels[i].let { ll ->
                        drawText(
                            ll,
                            color = if (isTarget) cOnTarget else cDim,
                            topLeft = Offset(
                                r.left + (bandW - ll.size.width) / 2f,
                                r.top + (r.height - ll.size.height) / 2f,
                            ),
                        )
                    }
                    labels[i].let { tl ->
                        drawText(
                            tl,
                            color = if (isTarget) cOnTarget else cText,
                            topLeft = Offset(
                                r.left + bandW + 3.dp.toPx(),
                                r.top + (r.height - tl.size.height) / 2f,
                            ),
                        )
                    }
                }
            }
        }

        Row(
            Modifier.align(Alignment.BottomEnd).padding(7.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${(s * 100).toInt()}%",
                color = OnCanvasScrim, fontSize = 9.sp,
                modifier = Modifier.clip(RoundedCornerShape(5.dp))
                    .background(CanvasControlScrim)
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
            TreeZoomButton("−") {
                val next = (scale / 1.3f).coerceIn(0.2f, 2.2f)
                panX = viewW / 2f - (viewW / 2f - panX) * (next / s)
                panY = viewH / 2f - (viewH / 2f - panY) * (next / s)
                scale = next
            }
            TreeZoomButton("+") {
                val next = (scale * 1.3f).coerceIn(0.2f, 2.2f)
                panX = viewW / 2f - (viewW / 2f - panX) * (next / s)
                panY = viewH / 2f - (viewH / 2f - panY) * (next / s)
                scale = next
            }
            TreeZoomButton("⤢") {
                val fit = minOf(viewW / logW, viewH / logH).coerceIn(0.2f, 2.2f)
                scale = fit
                panX = (viewW - logW * fit) / 2f
                panY = 0f
            }
        }
    }
}

/** 折叠段的卡片：说明这段有多少个、头尾是什么，并给一个逐个看的入口。 */
@Composable
private fun AncestorRunCard(run: AncestorCell.Run, onOpen: (WikiDest) -> Unit) {
    var expanded by remember(run) { mutableStateOf(false) }
    PhoneCard(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        onClick = { expanded = !expanded },
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                "连续 ${run.count} 个任务",
                color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            )
            Text(
                "「${run.headName}」→ 「${run.tailName}」",
                color = PhoneMuted, fontSize = 11.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
            Text(
                if (expanded) "收起 ▴" else "逐个查看 ▾",
                color = PhoneAccent, fontSize = 11.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
    if (expanded) {
        // 这一段本来就是一条直链，按顺序列出来即可
        Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            run.ids.forEachIndexed { i, id ->
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { onOpen(WikiDest.Quest(id)) }
                        .padding(vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${i + 1}", color = PhoneMuted, fontSize = 10.sp,
                        modifier = Modifier.width(22.dp),
                    )
                    Text(
                        run.names.getOrNull(i)?.takeIf { it.isNotBlank() } ?: "任务 $id",
                        color = PhoneText, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    ImageGlyph(R.drawable.ic2_chevron_right, PhoneMuted, Modifier.size(14.dp))
                }
                PhoneHairlineRow()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 任务检索（按块）
// ---------------------------------------------------------------------------

/**
 * 任务检索结果。按块分组 —— 命中哪个块就整块列出来，
 * 点任意一条进那个块的流程图并高亮它。
 */
@Composable
internal fun QuestSearchResults(
    query: String,
    onOpenTree: (chainId: Int, highlightId: Int) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var hits by remember { mutableStateOf<List<QuestHit>>(emptyList()) }
    var chainHits by remember { mutableStateOf<List<QuestChainMeta>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var page by remember { mutableStateOf(0) }
    var appending by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            hits = emptyList(); chainHits = emptyList(); total = 0
            page = 0; done = false
            return@LaunchedEffect
        }
        loading = true
        page = 0
        hits = runCatching { QuestDb.search(context, query) }.getOrDefault(emptyList())
        chainHits = runCatching { QuestDb.searchChains(context, query) }
            .getOrDefault(emptyList())
        // 总数单独查：翻到底之前要让用户知道一共多少条，
        // 否则「列表到这儿没了」会被读成「库里没有了」
        //（用户报过「感觉缺少了很多任务」）
        total = runCatching { QuestDb.countSearch(context, query) }.getOrDefault(0)
        loading = false
        done = true
    }

    val listState = rememberLazyListState()
    val canLoadMore by remember {
        derivedStateOf { !loading && !appending && hits.size < total }
    }

    // 滚到接近底部就追加下一页。和物品检索同一套做法。
    LaunchedEffect(listState, canLoadMore) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= hits.size - 8
        }.distinctUntilChanged().collect { near ->
            if (near && canLoadMore) {
                appending = true
                val next = page + 1
                runCatching { QuestDb.search(context, query, page = next) }
                    .onSuccess { more ->
                        if (more.isNotEmpty()) {
                            hits = hits + more
                            page = next
                        }
                    }
                appending = false
            }
        }
    }

    val margin = LocalContentMargin.current
    // 块名命中里，去掉那些任务名也命中的 —— 否则同一个块出现两次
    val chainOnly = remember(hits, chainHits) {
        val withQuestHit = hits.map { it.chainId }.toSet()
        chainHits.filter { it.id !in withQuestHit }
    }
    when {
        query.isBlank() -> WikiEmptyHint(
            R.drawable.ic2_search, "输入任务名或任务链名，按块整体显示",
        )
        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = PhoneAccent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp),
            )
        }
        hits.isEmpty() && chainOnly.isEmpty() && done -> WikiEmptyHint(
            R.drawable.ic2_empty_box, "没有找到这个任务",
        )
        else -> {
            // 同一个块里的多个命中合并成一组，别让一次搜索出现十行同名块。
            // withLabels() 给同名的补地点 —— 搜「冒险者入门」会命中 8 条同名的。
            val groups = remember(hits) {
                hits.withLabels().groupBy { it.first.chainId }.entries
                    .sortedBy { g -> g.value.minOf { it.first.level } }
            }
            LazyColumn(
                Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(bottom = 18.dp),
            ) {
                // 命中总数。已加载 < 总数时把两个数都摆出来，
                // 不然「列表到这儿没了」会被读成「库里没有了」。
                if (total > 0) {
                    item {
                        Text(
                            if (hits.size < total) "共 $total 条，已加载 ${hits.size} 条"
                            else "共 $total 条",
                            color = PhoneMuted, fontSize = 11.sp,
                            modifier = Modifier.padding(
                                start = margin.dp, end = margin.dp,
                                top = 8.dp, bottom = 2.dp,
                            ),
                        )
                    }
                }
                // 只按块名命中的（没有具体任务匹配），整块给一个入口
                if (chainOnly.isNotEmpty()) {
                    item {
                        Text(
                            "任务链",
                            color = PhoneMuted, fontSize = 10.sp,
                            modifier = Modifier.padding(
                                start = margin.dp, end = margin.dp, top = 10.dp,
                            ),
                        )
                    }
                    lazyItems(chainOnly, key = { "c${it.id}" }) { m ->
                        Row(
                            Modifier.fillMaxWidth()
                                .padding(start = margin.dp, end = margin.dp, top = 7.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PhoneSurfaceRaised)
                                .clickable { onOpenTree(m.id, 0) }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    m.title, color = PhoneText, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                // 起点名只在它不等于标题时才有信息量
                                // （无 Category 的块标题本来就是起点名）
                                if (m.leadName.isNotBlank() && m.leadName != m.title) {
                                    Text(
                                        "起点「${m.leadName}」",
                                        color = PhoneMuted, fontSize = 10.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                                Text(
                                    buildList {
                                        add("${m.members} 个任务")
                                        add("${m.layers} 级递进")
                                        if (m.minLevel > 0) {
                                            add("Lv ${m.minLevel}-${m.maxLevel}")
                                        }
                                    }.joinToString(" · "),
                                    color = PhoneMuted, fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Text("流程图 ›", color = PhoneAccent, fontSize = 10.sp)
                        }
                    }
                }

                if (groups.isNotEmpty() && chainOnly.isNotEmpty()) {
                    item {
                        Text(
                            "任务",
                            color = PhoneMuted, fontSize = 10.sp,
                            modifier = Modifier.padding(
                                start = margin.dp, end = margin.dp, top = 14.dp,
                            ),
                        )
                    }
                }

                lazyItems(groups, key = { it.key }) { (chainId, rows) ->
                    val head = rows.first().first
                    Column(
                        Modifier.fillMaxWidth()
                            .padding(start = margin.dp, end = margin.dp, top = 10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                // 同标题的块可能同时命中（「萨纳兰支线任务」有 26 个），
                                // 规模跟在后面用来区分
                                head.chainTitle.ifBlank { "任务链" },
                                color = PhoneText, fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${head.chainMembers} 个任务",
                                color = PhoneMuted, fontSize = 10.sp,
                            )
                        }
                        rows.forEach { (h, label) ->
                            Row(
                                Modifier.fillMaxWidth().padding(top = 7.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PhoneSurfaceRaised)
                                    .clickable { onOpenTree(chainId, h.id) }
                                    .padding(9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                if (h.iconId > 0) {
                                    ItemIcon(h.iconId, Modifier.size(26.dp), h.name.take(1))
                                    Spacer(Modifier.width(8.dp))
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        label, color = PhoneText, fontSize = 13.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        buildList {
                                            if (h.level > 0) add("等级 ${h.level}")
                                            h.type.takeIf { it.isNotBlank() }?.let(::add)
                                        }.joinToString(" · "),
                                        color = PhoneMuted, fontSize = 10.sp,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 1.dp),
                                    )
                                }
                                Text("流程图 ›", color = PhoneAccent, fontSize = 10.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        PhoneHairlineRow()
                    }
                }
                // 追加下一页时的转圈。没有它的话滚到底像是卡住了。
                if (appending) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = PhoneAccent, strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 空态。和物品检索的 WikiEmpty 同形，但不占整屏高度的居中位置。 */
@Composable
private fun WikiEmptyHint(icon: Int, text: String) {
    Column(
        Modifier.fillMaxSize().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ImageGlyph(icon, PhoneMuted.copy(alpha = 0.5f), Modifier.size(34.dp))
        Text(
            text, color = PhoneMuted, fontSize = 12.sp,
            modifier = Modifier.padding(top = 11.dp),
            textAlign = TextAlign.Center,
        )
    }
}
