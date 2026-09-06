package com.quserh.eorzeaphone.craft.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quserh.eorzeaphone.craft.data.JobCat
import com.quserh.eorzeaphone.data.ItemIconLoader
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.ui.ImageGlyph
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import com.quserh.eorzeaphone.craft.data.CraftItem
import com.quserh.eorzeaphone.craft.ui.CraftFill
import com.quserh.eorzeaphone.craft.ui.CraftLine
import com.quserh.eorzeaphone.craft.ui.CraftMuted
import com.quserh.eorzeaphone.craft.ui.CraftSurface
import com.quserh.eorzeaphone.craft.ui.CraftText
import com.quserh.eorzeaphone.craft.ui.CraftType

/** Icon tile for an item; falls back to a name-initial tile while loading/offline. */
@Composable
fun ItemIcon(item: CraftItem, size: Dp, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bitmap by remember(item.icon) { mutableStateOf(ItemIconLoader.peek(item.icon)) }
    LaunchedEffect(item.icon) {
        if (item.icon > 0 && bitmap == null) {
            bitmap = ItemIconLoader.load(context, item.icon)
        }
    }
    val bmp = bitmap
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size / 4))
            .background(if (item.hq) Color(0x2E9A6B1F) else CraftSurface)
            .border(0.5.dp, CraftLine, RoundedCornerShape(size / 4)),
        contentAlignment = Alignment.Center,
    ) {
        if (bmp != null) {
            Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize())
        } else {
            Text(
                item.nameCn.take(1).ifBlank { "?" },
                color = CraftMuted,
                style = CraftType.Caption,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Apple press feedback: highlight on press-down with a tiny scale, spring back. */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(stiffness = 600f), label = "press")
    Box(
        modifier
            .scale(scale)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
    ) {
        content()
    }
}

/** Full-width section surface, separate from the cards used for individual controls. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(CraftSurface),
    ) { content() }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = com.quserh.eorzeaphone.craft.ui.CraftType.SectionLabel,
        color = CraftMuted,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
fun Hairline(startPadding: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = startPadding)
            .padding(vertical = 0.dp)
            .size(0.dp, 0.5.dp)
            .background(CraftLine),
    )
}

/** Plain count text used for quantities. */
@Composable
fun QtyText(text: String, ok: Boolean, modifier: Modifier = Modifier) {
    Text(
        text,
        style = CraftType.Row,
        color = if (ok) CraftMuted else com.quserh.eorzeaphone.craft.ui.CraftDanger,
        modifier = modifier,
    )
}

/** Apple-style segmented control (two/three options). */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CraftSurface)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (active) CraftFill else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 7.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = CraftType.Callout,
                    color = if (active) Color.White else CraftText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Soft gradient bar used for progress/quality/durability meters. */
@Composable
fun MeterBar(value: Int, max: Int, color: Color, modifier: Modifier = Modifier) {
    val fraction = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(CraftLine),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceAtLeast(0.001f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(4.dp))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.75f), color))),
        )
    }
}

/** Bottom tab bar of the craft feature (self-contained inside the app screen). */
@Composable
fun CraftTabBar(current: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().background(CraftSurface)) {
        Box(Modifier.fillMaxWidth().height(0.5.dp).background(CraftMuted.copy(alpha = 0.25f)))
        Row(Modifier.fillMaxWidth().height(52.dp)) {
            labels.forEachIndexed { index, label ->
                val selected = index == current
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clickable { onSelect(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(4.dp)) {
                        if (selected) {
                            Box(Modifier.size(4.dp).background(CraftFill, androidx.compose.foundation.shape.CircleShape))
                        }
                    }
                    Text(
                        label,
                        style = CraftType.Callout,
                        color = if (selected) CraftText else CraftMuted.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 2.dp).alpha(if (selected) 1f else 0.75f),
                    )
                }
            }
        }
    }
}

private val labels = listOf("清单", "配方", "库存", "工作台")

/**
 * 数量步进器：左减右加、中间数字可直接输入。
 * The text field keeps its own buffer so clearing it to type a new number does
 * not fight the external value; parsing only commits valid digits.
 */
@Composable
fun QtyStepper(value: Int, onValue: (Int) -> Unit, modifier: Modifier = Modifier) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    Row(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CraftSurface)
            .border(0.5.dp, CraftLine, RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).semantics { contentDescription = "减少数量" }.clickable(enabled = value > 1) { onValue((value - 1).coerceAtLeast(1)) },
            contentAlignment = Alignment.Center,
        ) { ImageGlyph(R.drawable.ic2_remove, if (value > 1) CraftText else CraftMuted, Modifier.size(18.dp)) }
        Box(Modifier.width(52.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.text.BasicTextField(
                value = text,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() }.take(5)
                    text = filtered
                    filtered.toIntOrNull()?.let { onValue(it.coerceAtLeast(1)) }
                },
                textStyle = CraftType.Row.copy(textAlign = TextAlign.Center, color = CraftText),
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                ),
                modifier = Modifier.padding(vertical = 5.dp),
            )
        }
        Box(
            Modifier.size(32.dp).semantics { contentDescription = "增加数量" }.clickable { onValue(value + 1) },
            contentAlignment = Alignment.Center,
        ) { ImageGlyph(R.drawable.ic2_plus, CraftText, Modifier.size(18.dp)) }
    }
}

/**
 * 圆形购物车悬浮按钮（仅配方搜索页与道具详情页显示；推车图标用 Canvas 画，
 * 不引入 emoji 或图片资源）。
 */
@Composable
fun CartFab(count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    // 外层不裁切，角标探出圆形边缘也不会被裁掉。
    Box(modifier.size(58.dp), contentAlignment = Alignment.Center) {
    Box(
        Modifier
            .size(54.dp)
            .shadow(6.dp, androidx.compose.foundation.shape.CircleShape)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(CraftFill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(26.dp)) {
            val stroke = 2.dp.toPx()
            val W = size.width
            val H = size.height
            fun x(f: Float) = W * f
            fun y(f: Float) = H * f
            // 把手
            drawLine(Color.White, Offset(x(0.10f), y(0.20f)), Offset(x(0.26f), y(0.20f)), stroke, cap = StrokeCap.Round)
            // 把手斜杆
            drawLine(Color.White, Offset(x(0.26f), y(0.20f)), Offset(x(0.40f), y(0.62f)), stroke, cap = StrokeCap.Round)
            // 车斗上沿
            drawLine(Color.White, Offset(x(0.30f), y(0.32f)), Offset(x(0.94f), y(0.32f)), stroke, cap = StrokeCap.Round)
            // 车斗右斜边
            drawLine(Color.White, Offset(x(0.94f), y(0.32f)), Offset(x(0.80f), y(0.62f)), stroke, cap = StrokeCap.Round)
            // 车斗下沿
            drawLine(Color.White, Offset(x(0.40f), y(0.62f)), Offset(x(0.80f), y(0.62f)), stroke, cap = StrokeCap.Round)
            // 两轮
            drawCircle(Color.White, radius = stroke * 1.5f, center = Offset(x(0.46f), y(0.82f)))
            drawCircle(Color.White, radius = stroke * 1.5f, center = Offset(x(0.74f), y(0.82f)))
        }
    }
    if (count > 0) {
        Text(
            count.toString(),
            style = CraftType.Micro,
            color = CraftFill,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 3.dp, y = (-3).dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.White)
                .border(0.5.dp, CraftLine, androidx.compose.foundation.shape.CircleShape)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}
}

@Composable
fun RoleTag(cat: JobCat, modifier: Modifier = Modifier) {
    if (cat.label.isBlank()) return
    val bg = when (cat.role) {
        "tank" -> RoleTank
        "heal" -> RoleHeal
        "battle" -> RoleBattle
        else -> RoleNeutral
    }
    Text(
        cat.label,
        style = CraftType.Micro,
        color = Color.White,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

/**
 * 左滑删除行：右侧删除层随滑动进度从右往左"填满"整行，填满后松手即删除；
 * 未填满松手则弹回。滑动 1:1 跟手，可随时反向。
 */
@Composable
fun SwipeDeleteRow(
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    var rowWidthPx by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    val scope = rememberCoroutineScope()

    fun settle() {
        if (rowWidthPx > 0f && -offsetX >= rowWidthPx * 0.92f) {
            onRemove()
        } else {
            scope.launch {
                androidx.compose.animation.core.animate(
                    initialValue = offsetX,
                    targetValue = 0f,
                ) { v, _ -> offsetX = v }
            }
        }
    }

    Box(modifier.fillMaxWidth().onSizeChanged { rowWidthPx = it.width.toFloat().coerceAtLeast(1f) }) {
        val reveal = (-offsetX).coerceIn(0f, rowWidthPx)
        if (reveal > 0.5f) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(with(density) { reveal.toDp() })
                    .background(CraftDanger),
                contentAlignment = Alignment.Center,
            ) {
                if (reveal > rowWidthPx * 0.35f) {
                    Text(
                        if (reveal >= rowWidthPx * 0.92f) "松手删除" else "删除",
                        style = CraftType.Row, color = Color.White,
                    )
                }
            }
        }
        Row(
            Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(rowWidthPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = { settle() },
                        onDragCancel = { settle() },
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount).coerceIn(-rowWidthPx, 0f)
                    }
                },
        ) { content() }
    }
}
