package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.market.MarketAlertReceiver
import com.quserh.eorzeaphone.data.market.MarketApi
import com.quserh.eorzeaphone.data.market.MarketRepository
import com.quserh.eorzeaphone.data.wiki.WikiDb
import com.quserh.eorzeaphone.data.wiki.WikiItem
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.PhoneWarn

private data class WatchRow(
    val watch: MarketRepository.Watch,
    val item: WikiItem?,
    val price: Int?,
    val avg: Double?,
) {
    /** How far below average, for the "已达提醒线" badge. */
    val ratio: Double? get() =
        if (price != null && avg != null && avg > 0) price / avg else null

    val triggered: Boolean get() = when (watch.mode) {
        MarketRepository.AlertMode.Ratio ->
            ratio != null && watch.ratio != null && ratio!! <= watch.ratio!!
        MarketRepository.AlertMode.Absolute -> {
            val p = price
            val lo = watch.minPrice
            val hi = watch.maxPrice
            p != null && when {
                lo != null && hi != null -> p in lo..hi
                hi != null -> p <= hi
                lo != null -> p >= lo
                else -> false
            }
        }
        MarketRepository.AlertMode.None -> false
    }
}

/**
 * Watchlist. Prices for every watched item are fetched in one aggregated call
 * per scope, so this stays a single round trip in the common case.
 */
@Composable
internal fun MarketWatchlistScreen(
    state: PhoneState,
    onBack: () -> Unit,
    onOpen: (MarketDest) -> Unit,
) {
    val context = LocalContext()
    val margin = LocalContentMargin.current
    var rows by remember { mutableStateOf<List<WatchRow>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh) {
        loading = true
        val watches = MarketRepository.watchList(context)
        // Only poll while at least one rule exists, so an empty watchlist costs nothing.
        MarketAlertReceiver.configure(context, watches.any { it.hasAlert })
        val marketable = runCatching { MarketApi.marketableIds(context) }
            .getOrDefault(emptySet())
        val out = mutableListOf<WatchRow>()
        for ((scope, group) in watches.groupBy { it.scope }) {
            val ids = group.map { it.itemId }
                .filter { marketable.isEmpty() || it in marketable }
            val aggs = if (ids.isEmpty()) emptyMap()
            else MarketApi.aggregated(scope, ids).associateBy { it.itemId }
            for (w in group) {
                val a = aggs[w.itemId]
                val blk = if (w.hqOnly) (a?.hqDc ?: a?.hqWorld) else (a?.nqDc ?: a?.nqWorld)
                out += WatchRow(
                    watch = w,
                    item = runCatching { WikiDb.byId(context, w.itemId) }.getOrNull(),
                    price = blk?.minPrice,
                    avg = blk?.avgPrice,
                )
            }
        }
        // Triggered first, then by how far below average.
        rows = out.sortedWith(
            compareByDescending<WatchRow> { it.triggered }
                .thenBy { it.ratio ?: Double.MAX_VALUE }
        )
        loading = false
    }

    ScreenFrame {
        ScreenHeader(
            "关注列表", state, onBack = onBack,
            trailing = {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(9.dp))
                        .clickable { refresh++ },
                    contentAlignment = Alignment.Center,
                ) {
                    ImageGlyph(R.drawable.ic2_refresh_cycle, PhoneMuted, Modifier.size(17.dp))
                }
            },
        )
        when {
            loading && rows.isEmpty() -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = PhoneAccent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp),
                )
            }
            rows.isEmpty() -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                item { MarketEmpty("还没有关注的道具") }
                // The category grid lives here too: this screen is the market's
                // browsing home, so discovery must not depend on having favourites.
                item { MarketCategoryGridSection(state, onOpen) }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                lazyItems(rows, key = { it.watch.itemId }) { r ->
                    WatchRowView(r) { onOpen(MarketDest.Item(r.watch.itemId)) }
                }
                item {
                    Text(
                        "价格每次进入本页刷新；后台提醒由系统定时检查。",
                        color = PhoneMuted, fontSize = 10.sp, lineHeight = 15.sp,
                        modifier = Modifier.padding(
                            start = margin.dp, end = margin.dp, top = 16.dp,
                        ),
                    )
                }
                // Browsing by category sits right under the favourites, as requested.
                item { MarketCategoryGridSection(state, onOpen) }
            }
        }
    }
}

@Composable
private fun WatchRowView(r: WatchRow, onClick: () -> Unit) {
    val margin = LocalContentMargin.current
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = margin.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(PhoneSurface),
            contentAlignment = Alignment.Center,
        ) {
            ItemIcon(r.item?.iconId ?: 0, Modifier.fillMaxSize(),
                (r.item?.nameCn ?: "?").take(2))
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.item?.nameCn ?: "道具 ${r.watch.itemId}",
                    color = PhoneText, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (r.triggered) {
                    Text(
                        "已达提醒线", color = PhoneGreen, fontSize = 9.sp,
                        modifier = Modifier.padding(start = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PhoneGreen.copy(alpha = 0.14f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
                // Game-side monitor state, so a rule pushed to the plugin is visible
                // where the rules live.
                if (r.watch.monitorOn) {
                    Text(
                        if (r.watch.autoBuy) "自动购买中" else "监控中",
                        color = PhoneWarn, fontSize = 9.sp,
                        modifier = Modifier.padding(start = 6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PhoneWarn.copy(alpha = 0.14f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
            }
            Text(
                buildList {
                    add(r.watch.scope)
                    if (r.watch.monitorOn && r.watch.monitorThreshold > 0) {
                        add("监控 ≤${gil(r.watch.monitorThreshold)}" +
                            if (r.watch.autoBuy) "·自动买" else "")
                    }
                    when (r.watch.mode) {
                        MarketRepository.AlertMode.Ratio ->
                            r.watch.ratio?.let { add("低于均价 %.1f 倍".format(it)) }
                        MarketRepository.AlertMode.Absolute -> {
                            val lo = r.watch.minPrice
                            val hi = r.watch.maxPrice
                            add(
                                when {
                                    lo != null && hi != null -> "${gil(lo)}–${gil(hi)}"
                                    hi != null -> "低于 ${gil(hi)}"
                                    lo != null -> "高于 ${gil(lo)}"
                                    else -> "未设阈值"
                                }
                            )
                        }
                        MarketRepository.AlertMode.None -> add("仅收藏")
                    }
                    r.ratio?.let { add("现价 %.2f 倍".format(it)) }
                }.joinToString(" · "),
                color = PhoneMuted, fontSize = 11.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            r.price?.let { gil(it) } ?: "—",
            color = if (r.triggered) PhoneGreen else PhoneText,
            fontSize = 15.sp,
            fontWeight = if (r.triggered) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
    PhoneHairlineRow(margin.dp + 48.dp)
}

/** Local shorthand so the screen body reads cleanly. */
@Composable
private fun LocalContext() =
    androidx.compose.ui.platform.LocalContext.current.applicationContext
