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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quserh.eorzeaphone.data.ItemIconLoader
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

/** iOS inset-group card. */
@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
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
            .clip(RoundedCornerShape(9.dp))
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
                ) {
                    Box(Modifier.padding(top = 9.dp).size(4.dp)) {
                        if (selected) {
                            Box(Modifier.size(4.dp).background(CraftFill, androidx.compose.foundation.shape.CircleShape))
                        }
                    }
                    Text(
                        label,
                        style = CraftType.Caption,
                        color = if (selected) CraftText else CraftMuted.copy(alpha = 0.9f),
                        modifier = Modifier.padding(top = 2.dp).alpha(if (selected) 1f else 0.75f),
                    )
                }
            }
        }
    }
}

private val labels = listOf("清单", "配方", "库存", "工作台")
