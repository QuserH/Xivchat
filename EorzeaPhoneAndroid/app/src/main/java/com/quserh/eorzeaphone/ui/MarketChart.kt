package com.quserh.eorzeaphone.ui

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.data.market.MarketApi
import com.quserh.eorzeaphone.data.market.MarketRepository
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneHairline
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.PhoneWarn
import java.util.Calendar

private enum class ChartRange(val days: Int, val label: String) {
    Week(7, "7 天"), Month(30, "30 天")
}

/** A day's aggregated price. */
internal data class Bar(
    val day: Long,
    val min: Int,
    val avg: Double,
    val max: Int,
    val sales: Int,
)

/**
 * A horizontal reference on the price chart and a split marker in the listing
 * tables: listings above it are worse than just paying this price elsewhere
 * (NPC vendor, average sale). Colours mirror BetterMarketBoard -- orange for the
 * vendor price, blue for the sale average.
 */
internal data class MarketBenchmarkLine(
    val label: String,
    val price: Int,
    val color: Color,
)

/**
 * Interleave benchmark markers into a price-sorted list, before the first row
 * that prices above them -- the DR "everything above the line sorts after it"
 * visual, without reordering rows. Benchmarks never reached stay at the end so
 * the marker is still visible when every row undercuts it.
 */
internal fun <T> interleaveBenchmarks(
    rows: List<T>,
    benchmarks: List<MarketBenchmarkLine>,
    priceOf: (T) -> Int,
): List<Any> {
    val pending = benchmarks.sortedBy { it.price }.toMutableList()
    val out = mutableListOf<Any>()
    rows.forEach { row ->
        val crossed = pending.filter { priceOf(row) > it.price }
        if (crossed.isNotEmpty()) {
            out.addAll(crossed)
            pending.removeAll(crossed)
        }
        out.add(row as Any)
    }
    out.addAll(pending)
    return out
}

/**
 * Price chart with axis labels and touch scrub.
 *
 * The 7-day range comes from the API. The 30-day range can only come from local
 * accumulation: Universalis retains ~7 days of sales, so the month view fills in
 * as the item gets opened over time and says so until it has.
 *
 * Scrubbing is the point of the whole widget -- a bare line cannot answer "what
 * was it on Tuesday", which is what people actually want from a price history.
 */
@Composable
internal fun MarketChartSection(
    context: Context,
    itemId: Int,
    scope: String,
    benchmarks: List<MarketBenchmarkLine> = emptyList(),
    hqOnly: Boolean = false,
) {
    val margin = LocalContentMargin.current
    var range by remember { mutableStateOf(ChartRange.Week) }
    var bars by remember(itemId, scope, hqOnly) { mutableStateOf<List<Bar>>(emptyList()) }
    var picked by remember(itemId, scope, range, hqOnly) { mutableStateOf<Int?>(null) }

    // Two states that used to be one. "Nothing to draw yet" wants the blocking spinner;
    // "drawing cached bars while a wider window downloads" only wants the inline hint.
    // Switching 7 -> 30 is the second case and the old code showed *neither*: both ranges
    // write the same table, so the cache read came back non-empty, loading went false, and
    // the ~300 KB / 3.7s fetch ran with no indicator at all. That read as a dead tab.
    var fetching by remember(itemId, scope, range, hqOnly) { mutableStateOf(true) }

    LaunchedEffect(itemId, scope, range, hqOnly) {
        if (scope.isBlank()) {
            fetching = false
            return@LaunchedEffect
        }
        fetching = true
        picked = null
        try {
            // Show whatever is already stored first so the chart is never blank while
            // the network call runs -- 30d on a busy item is ~300 KB / 3.7s.
            bars = runCatching {
                MarketRepository.points(context, itemId, scope, range.days, hq = hqOnly)
                    .map { Bar(it.day, it.minPrice, it.avgPrice, it.maxPrice, it.sales) }
            }.getOrDefault(emptyList())

            // 全部 has no single Universalis history scope; the region covers all CN.
            val histScope = if (scope == MarketApi.SCOPE_ALL) "中国" else scope
            val sales = runCatching { MarketApi.history(histScope, itemId, days = range.days) }
                .getOrDefault(emptyList())
            val qualitySales = sales.filter { it.hq == hqOnly }
            if (qualitySales.isNotEmpty()) {
                // Store both qualities in one pass; the repository keeps separate
                // hq buckets and the next toggle can reuse them without a refetch.
                MarketRepository.recordSales(context, itemId, scope, sales)
                bars = qualitySales.groupBy { it.atSec / 86400 }
                    .map { (d, g) ->
                        val quantity = g.sumOf { it.quantity.toLong() }
                        val average = if (quantity > 0L) {
                            g.sumOf { it.pricePerUnit.toDouble() * it.quantity.toDouble() } / quantity
                        } else {
                            g.map { it.pricePerUnit.toDouble() }.average()
                        }
                        Bar(
                            d, g.minOf { it.pricePerUnit }, average,
                            g.maxOf { it.pricePerUnit }, g.size,
                        )
                    }
                    .sortedBy { it.day }
            }
        } finally {
            // A failed network/JSON decode must not leave the inline spinner running
            // forever. Cached bars (if any) remain visible and can be retried by
            // changing the range or reopening the item.
            fetching = false
        }
    }

    MarketLinkSection("价格走势")
    Column(Modifier.fillMaxWidth().padding(horizontal = margin.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val ranges = ChartRange.entries.toList()
            MarketSegmented(
                labels = ranges.map { it.label },
                selectedIndex = ranges.indexOf(range),
                onSelect = { range = ranges[it] },
                modifier = Modifier.width(132.dp),
            )
            // Feedback sits next to the control that was tapped. The only other indicator
            // was 10sp text below the chart, which is far from the finger and easy to miss
            // on a 3.7s wait -- the tab read as doing nothing.
            if (fetching) {
                CircularProgressIndicator(
                    color = PhoneAccent, strokeWidth = 1.5.dp,
                    modifier = Modifier.padding(start = 9.dp).size(13.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            // Selected point read-out, or the latest value when nothing is held.
            val show = picked?.let { bars.getOrNull(it) } ?: bars.lastOrNull()
            if (show != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${gil(show.min)} gil",
                        color = PhoneText, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${dayLabel(show.day)}  均 ${gil(show.avg)}  ${show.sales} 笔",
                        color = PhoneMuted, fontSize = 10.sp,
                    )
                }
            }
        }

        // A single day still has a valid min/average point. Drawing it keeps the
        // average line/benchmark visible while a second day is being accumulated.
        val enough = bars.isNotEmpty()
        // A benchmark is still useful even when Universalis returned no history
        // (new item, sparse scope, or a transient history timeout).  Previously the
        // whole chart switched to the "no transactions" text in that case, which
        // made the average/NPC guide appear intermittent although the values were
        // already available from the aggregate/live response.  Keep the reference
        // price visible as a flat, explicitly labelled line; do not fabricate a
        // transaction point or pretend that a sale happened.
        val benchmarkOnly = !enough && benchmarks.isNotEmpty()
        Box(
            Modifier.fillMaxWidth().padding(top = 10.dp).height(168.dp)
                .clip(RoundedCornerShape(12.dp)).background(PhoneSurface),
        ) {
            when {
                enough -> PriceLine(bars, picked, benchmarks, onPick = { picked = it })
                benchmarkOnly -> BenchmarkOnlyChart(benchmarks)
                fetching -> Box(
                    Modifier.fillMaxWidth().height(168.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = PhoneAccent, strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )
                }
                else -> Text(
                    "这件道具近 ${range.days} 天没有成交记录",
                    color = PhoneMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }

        if (enough) {
            // Date range under the axis, so the x scale is not a mystery.
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(dayLabel(bars.first().day), color = PhoneMuted, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text(dayLabel(bars.last().day), color = PhoneMuted, fontSize = 10.sp)
            }
            // Reference-line legend: without it the two flat lines read as grid noise.
            if (benchmarks.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    benchmarks.forEach { b ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(width = 12.dp, height = 2.dp)
                                    .background(b.color),
                            )
                            Text(
                                "${b.label} ${gil(b.price)}",
                                color = b.color, fontSize = 10.sp,
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    "按住曲线可看每天的价格",
                    color = PhoneMuted, fontSize = 10.sp,
                )
                if (fetching) {
                    Text(
                        "  ·  正在补全 ${range.days} 天数据…",
                        color = PhoneMuted, fontSize = 10.sp,
                    )
                }
            }
        } else if (benchmarkOnly) {
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                benchmarks.forEach { b ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(width = 12.dp, height = 2.dp)
                                .background(b.color),
                        )
                        Text(
                            "${b.label} ${gil(b.price)}",
                            color = b.color, fontSize = 10.sp,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
            Text(
                "暂无可绘制的历史成交点 · 以上为当前参考价",
                color = PhoneMuted, fontSize = 10.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/**
 * Draw benchmark lines when there are no daily sale buckets yet.
 *
 * This intentionally has no touch/scrub affordance: there is no historical point
 * to inspect.  Keeping the y-axis local to the references makes one or several
 * lines readable instead of putting them at an arbitrary chart edge.
 */
@Composable
private fun BenchmarkOnlyChart(benchmarks: List<MarketBenchmarkLine>) {
    val grid = PhoneHairline
    val sorted = benchmarks.filter { it.price > 0 }.distinctBy { it.label to it.price }
        .sortedBy { it.price }
    if (sorted.isEmpty()) return
    val lo = sorted.minOf { it.price }.toDouble()
    val hi = sorted.maxOf { it.price }.toDouble()
    val span = (hi - lo).takeIf { it > 0.0 } ?: maxOf(1.0, hi * 0.05)

    Box(Modifier.fillMaxWidth().height(168.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 26.dp),
        ) {
            Text(gil((lo + span).toInt()), color = PhoneMuted, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text(gil((lo + span / 2.0).toInt()), color = PhoneMuted, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text(gil(lo.toInt()), color = PhoneMuted, fontSize = 9.sp)
        }
        Canvas(
            Modifier.fillMaxWidth().height(168.dp)
                .padding(start = 58.dp, end = 14.dp, top = 14.dp, bottom = 26.dp),
        ) {
            fun y(price: Int): Float =
                (size.height * (1f - ((price.toDouble() - lo) / span).toFloat()))
                    .coerceIn(0f, size.height)
            listOf(0f, 0.5f, 1f).forEach { f ->
                drawLine(
                    grid, Offset(0f, size.height * f),
                    Offset(size.width, size.height * f), 1f,
                )
            }
            sorted.forEach { b ->
                val py = y(b.price)
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        b.color.copy(alpha = 0.65f),
                        Offset(x, py), Offset(minOf(x + 5f, size.width), py), 2f,
                    )
                    x += 10f
                }
            }
        }
    }
}

/** "8-31" for the axis and the read-out. */
private fun dayLabel(epochDay: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = epochDay * 86400_000L }
    return "${c.get(Calendar.MONTH) + 1}-${c.get(Calendar.DAY_OF_MONTH)}"
}

/**
 * Min line + average line, with y-axis price labels and a draggable readout.
 *
 * Buckets by day rather than plotting raw sales: density varies by three orders of
 * magnitude between items (a gear piece had 1757 sales in a week, a crystal 14).
 *
 * [benchmarks] draw as flat reference lines (NPC vendor price, sale average). They
 * also stretch the y-range so a line outside the price band stays visible instead
 * of silently disappearing.
 */
@Composable
private fun PriceLine(
    bars: List<Bar>,
    picked: Int?,
    benchmarks: List<MarketBenchmarkLine>,
    onPick: (Int?) -> Unit,
) {
    val accent = PhoneAccent
    val muted = PhoneMuted
    val grid = PhoneHairline
    val textColor = PhoneMuted

    val lo = minOf(
        bars.minOf { minOf(it.min.toDouble(), it.avg) },
        benchmarks.minOfOrNull { it.price.toDouble() } ?: Double.MAX_VALUE,
    )
    val hi = maxOf(
        bars.maxOf { maxOf(it.min.toDouble(), it.avg) },
        benchmarks.maxOfOrNull { it.price.toDouble() } ?: 0.0,
    )

    Box(Modifier.fillMaxWidth().height(168.dp)) {
        // y labels sit outside the canvas so the plot never draws under them
        Column(
            Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 26.dp),
        ) {
            Text(gil(hi.toInt()), color = textColor, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text(gil(((hi + lo) / 2).toInt()), color = textColor, fontSize = 9.sp)
            Spacer(Modifier.weight(1f))
            Text(gil(lo.toInt()), color = textColor, fontSize = 9.sp)
        }

        Canvas(
            Modifier.fillMaxWidth().height(168.dp)
                .padding(start = 58.dp, end = 14.dp, top = 14.dp, bottom = 26.dp)
                .pointerInput(bars.size) {
                    detectDragGestures(
                        onDragEnd = { onPick(null) },
                        onDragCancel = { onPick(null) },
                    ) { change, _ ->
                        val i = ((change.position.x / size.width) * (bars.size - 1))
                            .toInt().coerceIn(0, bars.lastIndex)
                        onPick(i)
                    }
                }
                .pointerInput(bars.size) {
                    detectTapGestures { off ->
                        val i = ((off.x / size.width) * (bars.size - 1))
                            .toInt().coerceIn(0, bars.lastIndex)
                        onPick(i)
                    }
                },
        ) {
            val span = (hi - lo).takeIf { it > 0 } ?: 1.0
            val dx = size.width / (bars.size - 1).coerceAtLeast(1)
            fun y(v: Double) = (size.height * (1f - ((v - lo) / span).toFloat()))
                .coerceIn(0f, size.height)

            listOf(0f, 0.5f, 1f).forEach { f ->
                drawLine(
                    grid, Offset(0f, size.height * f),
                    Offset(size.width, size.height * f), 1f,
                )
            }

            fun path(sel: (Bar) -> Double) = Path().apply {
                bars.forEachIndexed { i, b ->
                    val px = dx * i
                    val py = y(sel(b))
                    if (i == 0) moveTo(px, py) else lineTo(px, py)
                }
            }

            drawPath(path { it.avg }, muted.copy(alpha = 0.5f), style = Stroke(width = 2f))
            drawPath(path { it.min.toDouble() }, accent, style = Stroke(width = 3f))

            // With one day there is no segment to make the average path visible.
            // Draw an explicit dot so the average series still exists visually
            // instead of looking like the line randomly disappeared.
            if (bars.size == 1) {
                drawCircle(
                    muted.copy(alpha = 0.8f), radius = 3.5f,
                    center = Offset(0f, y(bars[0].avg)),
                )
            }

            // Reference lines: flat, dashed, behind the data. Kept out of the scrub
            // readout on purpose -- the legend under the chart names them.
            benchmarks.forEach { b ->
                val py = y(b.price.toDouble())
                val step = 10f
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        b.color.copy(alpha = 0.55f),
                        Offset(x, py),
                        Offset(minOf(x + step / 2, size.width), py),
                        2f,
                    )
                    x += step
                }
            }

            // scrub marker
            val sel = picked ?: (bars.size - 1)
            val sx = dx * sel
            val sy = y(bars[sel].min.toDouble())
            if (picked != null) {
                drawLine(accent.copy(alpha = 0.35f), Offset(sx, 0f),
                    Offset(sx, size.height), 1.5f)
            }
            drawCircle(accent, radius = if (picked != null) 6f else 4f, center = Offset(sx, sy))
        }
    }
}
