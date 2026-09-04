package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.GameMarketListing
import com.quserh.eorzeaphone.data.GameMarketPurchase
import com.quserh.eorzeaphone.data.GameMarketPurchaseStatus
import com.quserh.eorzeaphone.data.GameMarketStatus
import com.quserh.eorzeaphone.data.market.MarketApi
import com.quserh.eorzeaphone.data.market.MarketRepository
import com.quserh.eorzeaphone.data.wiki.WikiDb
import com.quserh.eorzeaphone.data.wiki.WikiItem
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneInfo
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.PhoneType
import com.quserh.eorzeaphone.ui.theme.PhoneWarn
import kotlinx.coroutines.delay

/** Sort for the listing table. */
private enum class PriceSort(val label: String) {
    UnitAsc("单价 ↑"), UnitDesc("单价 ↓"), TotalAsc("总价 ↑"), World("按服务器")
}

/**
 * Which market to show. Replaces the per-DC strip, which offered five choices where
 * only three answer a real question: what can I buy here, what can I reach by
 * travelling inside my DC, and what does the whole game charge.
 */
private enum class MarketView { World, Dc, All }

/** Pick the best available sale-average block for the scope currently shown. */
private fun aggregateAverage(
    agg: MarketApi.ItemAgg?,
    view: MarketView,
    hqOnly: Boolean,
): Double? {
    if (agg == null) return null
    val blocks = if (hqOnly) {
        listOfNotNull(
            when (view) {
                MarketView.World -> agg.hqWorld
                MarketView.Dc -> agg.hqDc
                MarketView.All -> agg.hqRegion
            },
            agg.hqDc,
            agg.hqRegion,
            agg.hqWorld,
        )
    } else {
        listOfNotNull(
            when (view) {
                MarketView.World -> agg.nqWorld
                MarketView.Dc -> agg.nqDc
                MarketView.All -> agg.nqRegion
            },
            agg.nqDc,
            agg.nqRegion,
            agg.nqWorld,
        )
    }
    return blocks.asSequence().mapNotNull { it.avgPrice }.firstOrNull { it > 0 }
}

private fun weightedSaleAverage(sales: List<MarketApi.Sale>, hqOnly: Boolean): Double? {
    val rows = sales.filter { it.hq == hqOnly && it.pricePerUnit > 0 && it.quantity > 0 }
    val quantity = rows.sumOf { it.quantity.toLong() }
    if (quantity <= 0L) return null
    return rows.sumOf { it.pricePerUnit.toDouble() * it.quantity.toDouble() } / quantity
}

private fun weightedPointAverage(points: List<MarketRepository.Point>): Double? {
    // A sale row can contain a stack. Weight by quantity so a 99-item
    // transaction is not treated as one vote next to a single-item sale.
    // Older rows may have no quantity; fall back to their sale count.
    val weights = points.map {
        (if (it.quantity > 0) it.quantity else it.sales).coerceAtLeast(0).toLong()
    }
    val total = weights.sum()
    if (total <= 0L) return null
    return points.zip(weights).sumOf { (point, weight) -> point.avgPrice * weight } / total.toDouble()
}

private fun weightedListingAverage(
    listings: MarketApi.Listings?,
    hqOnly: Boolean,
): Double? {
    val rows = listings?.listings.orEmpty().filter {
        it.hq == hqOnly && it.pricePerUnit > 0 && it.quantity > 0
    }
    val quantity = rows.sumOf { it.quantity.toLong() }
    if (quantity <= 0L) return null
    return rows.sumOf { it.pricePerUnit.toDouble() * it.quantity.toDouble() } / quantity.toDouble()
}

/**
 * Item detail: aggregated summary, per-world listings (sortable + filterable),
 * and the 1w / 1m charts.
 *
 * Scope is a DC by default because that is what players can actually travel to;
 * region-wide numbers are shown in the summary only, as context.
 */
@Composable
internal fun MarketItemScreen(
    state: PhoneState,
    itemId: Int,
    onBack: () -> Unit,
    onOpen: (MarketDest) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val margin = LocalContentMargin.current

    var item by remember(itemId) { mutableStateOf<WikiItem?>(null) }
    // Three views instead of one-per-DC. The DC strip listed all five, but four of
    // them are worlds the player cannot buy from without a transfer, so the prices
    // were not actionable. What is actionable: my world, my DC, everywhere.
    var view by remember { mutableStateOf(MarketView.World) }
    var myWorld by remember { mutableStateOf("") }
    var myDc by remember { mutableStateOf("") }
    // Nothing to show per-world until a character has been seen at least once, so the
    // tab is simply absent rather than present-and-empty. Universalis covers the DC
    // and nationwide views without the plugin, which is the whole offline story.
    val tabs = remember(myWorld) {
        if (myWorld.isBlank()) listOf(MarketView.Dc, MarketView.All)
        else MarketView.entries.toList()
    }
    // Set once the player taps a tab, so the clamp below can tell an involuntary
    // correction from a real choice and only undo its own.
    var viewChosen by remember { mutableStateOf(false) }
    // Keep the selection inside the offered set: a player who was online and then
    // disconnected would otherwise be left on a tab that no longer exists.
    if (view !in tabs) view = tabs.first()
    // The character's world arrives a frame or more after this screen opens, so the
    // clamp above has already pushed the default off 本服 by the time 本服 exists.
    // Put it back -- but never over a tab the player picked themselves.
    LaunchedEffect(myWorld) {
        if (myWorld.isNotBlank() && !viewChosen) view = MarketView.World
    }
    var dcs by remember { mutableStateOf<List<MarketApi.DataCenter>>(emptyList()) }
    var worlds by remember { mutableStateOf<List<MarketApi.World>>(emptyList()) }
    // Hand-picked overrides. Null means "follow the character", which is the default
    // and the common case; these only fill in once the user taps a tab twice.
    var pickedWorld by remember { mutableStateOf<String?>(null) }
    var pickedDc by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf<MarketView?>(null) }
    // The listing awaiting confirmation, plus the live-board query counter. Both sit
    // here rather than in MarketLiveSection because the purchase sheets are
    // fillMaxSize overlays: inside a LazyColumn item they get clipped to that one
    // row's bounds and never become visible, so tapping 买 looked like a no-op.
    var confirming by remember(itemId) { mutableStateOf<GameMarketListing?>(null) }
    var liveAttempt by remember(itemId) { mutableIntStateOf(0) }

    val dcName = pickedDc ?: myDc
    // Worlds of the DC being shown, for the picker and for the sanity check below.
    val dcWorlds = remember(dcName, worlds) {
        worlds.filter { it.dc == dcName }.map { it.name }.sorted()
    }
    // A picked DC invalidates the character's own world: showing 白银乡 prices under a
    // 莫古力 tab is a combination that does not exist. Fall back to a world that is
    // actually in the DC on screen, so the label never lies.
    val worldName = pickedWorld
        ?: myWorld.takeIf { it.isNotBlank() && (dcWorlds.isEmpty() || it in dcWorlds) }
        ?: dcWorlds.firstOrNull()
        ?: myWorld
    val scope = when (view) {
        MarketView.World -> worldName
        MarketView.Dc -> dcName
        MarketView.All -> MarketApi.SCOPE_ALL
    }
    var agg by remember(itemId, scope) { mutableStateOf<MarketApi.ItemAgg?>(null) }
    var listings by remember(itemId, scope) { mutableStateOf<MarketApi.Listings?>(null) }
    var loading by remember(itemId, scope) { mutableStateOf(true) }
    var retry by remember(itemId, scope) { mutableStateOf(0) }
    var sort by remember { mutableStateOf(PriceSort.UnitAsc) }
    var worldFilter by remember { mutableStateOf<String?>(null) }
    var hqOnly by remember { mutableStateOf(false) }
    var watched by remember(itemId) { mutableStateOf(false) }
    // Do not write the default `false` back before the async watch row has been
    // loaded.  Without this gate the first composition could clear an existing
    // favourite (the load and the persistence effects race on a fresh screen).
    var watchLoaded by remember(itemId) { mutableStateOf(false) }
    // Whether a notification rule exists on this item -- lights the bell. Bumped by
    // the alert sheet closing so a just-saved rule lights it up immediately.
    var hasAlert by remember(itemId) { mutableStateOf(false) }
    var watchVersion by remember { mutableIntStateOf(0) }
    var showAlert by remember { mutableStateOf(false) }
    var change24h by remember(itemId, scope) { mutableStateOf<Double?>(null) }
    // History is fetched once per scope and then filtered locally when the HQ switch
    // changes. This keeps the average benchmark present instead of making it depend on
    // whichever quality happened to finish loading first.
    var recentSales by remember(itemId, scope) { mutableStateOf<List<MarketApi.Sale>>(emptyList()) }
    var storedAverage by remember(itemId, scope, hqOnly) { mutableStateOf<Double?>(null) }
    // No scope persistence any more: the view is derived from the character's own
    // world, so there is no hand-picked DC to remember. `setScope` is still used by
    // the watch list, which stores the scope a watch was created under.

    LaunchedEffect(itemId, state.profile?.currentWorld) {
        item = runCatching { WikiDb.byId(context, itemId) }.getOrNull()
        // World table first: the DC has to be resolved through it, so reading the
        // world before this would always miss.
        val (d, w) = MarketApi.worlds(context)
        dcs = d
        worlds = w
        // currentWorld, not homeWorld -- while travelling, the board you can
        // actually buy from is the one you are standing on.
        val live = state.profile?.currentWorld?.takeIf { it.isNotBlank() }
        if (live != null) MarketRepository.setLastWorld(context, live)
        // Falls back to the remembered world so the 本服 tab is populated offline.
        val world = live ?: MarketRepository.lastWorld(context)
        myWorld = world ?: ""
        myDc = world?.let { n -> w.firstOrNull { it.name == n }?.dc }
            ?: MarketRepository.scope(context)
    }

    // NPC vendor metadata is independent of the selected world/DC and is also
    // useful when the live game-board query is unavailable.  Ask for the cached
    // category tree here as well as on the category screen so an item opened from
    // inventory/search can resolve its vendor price without first visiting another
    // market page.
    LaunchedEffect(itemId, state.connected) {
        if (state.connected) state.requestMarketCategories()
    }

    // Watch + alert state, re-read after the alert sheet closes (watchVersion) so a
    // freshly saved rule lights the bell without waiting for a screen rebuild.
    LaunchedEffect(itemId, watchVersion) {
        watchLoaded = false
        val w = MarketRepository.watch(context, itemId)
        // A row can exist solely for an alert/price monitor.  The heart reflects
        // the explicit favourite flag, not mere row existence.
        watched = w?.favorite == true
        hasAlert = w?.hasAlert == true
        watchLoaded = true
    }

    LaunchedEffect(itemId, scope, retry) {
        if (scope.isBlank()) return@LaunchedEffect
        loading = true
        try {
            val all = scope == MarketApi.SCOPE_ALL
            // 全部 fans out per DC: the region listings endpoint caps at 100 globally
            // and came back covering only 13 of 28 worlds. See MarketApi.listingsAllCn.
            agg = runCatching {
                if (all) MarketApi.aggregatedAllCn(itemId)
                else MarketApi.aggregated(scope, listOf(itemId)).firstOrNull()
            }.getOrNull()
            listings = runCatching {
                if (all) MarketApi.listingsAllCn(context, itemId)
                else MarketApi.listings(scope, itemId, limit = 100)
            }.getOrNull()
            // 3 days is enough: this call only feeds the 24h delta. The chart
            // fetches its own window, and a 30d pull on a busy item is ~300 KB.
            val histScope = if (all) "中国" else scope
            val sales = runCatching { MarketApi.history(histScope, itemId, days = 3) }
                .getOrDefault(emptyList())
            recentSales = sales
            if (sales.isNotEmpty()) {
                MarketRepository.recordSales(context, itemId, scope, sales)
                storedAverage = weightedPointAverage(
                    MarketRepository.points(context, itemId, scope, 30, hq = hqOnly),
                )
                // 24h change: compare today's cheapest sale against yesterday's.
                val today = System.currentTimeMillis() / 86400_000
                val byDay = sales.groupBy { it.atSec / 86400 }
                val now = byDay[today]?.minOfOrNull { it.pricePerUnit }
                    ?: byDay[today - 1]?.minOfOrNull { it.pricePerUnit }
                val prev = byDay[today - 1]?.minOfOrNull { it.pricePerUnit }
                    ?: byDay[today - 2]?.minOfOrNull { it.pricePerUnit }
                change24h = if (now != null && prev != null && prev > 0) {
                    (now - prev) * 100.0 / prev
                } else null
            } else {
                storedAverage = weightedPointAverage(
                    MarketRepository.points(context, itemId, scope, 30, hq = hqOnly),
                )
                change24h = null
            }
        } finally {
            loading = false
        }
    }

    // Quality changes should immediately re-use the local history for the selected series;
    // do not wait for a fresh network response (and do not lose the benchmark in the gap).
    LaunchedEffect(itemId, scope, hqOnly) {
        if (scope.isNotBlank()) {
            storedAverage = weightedPointAverage(
                MarketRepository.points(context, itemId, scope, 30, hq = hqOnly),
            )
        }
    }

    val it0 = item
    // Benchmarks shared by the tables and the chart.  NPC price comes from the
    // authoritative GilShopItem-gated metadata (or a live reply), so it remains
    // available while switching scope/offline.  A zero means no gil vendor and is
    // intentionally omitted rather than showing the item's resale value.
    val npcPrice = state.npcPriceFor(itemId)
    val saleAverage = aggregateAverage(agg, view, hqOnly)
        ?: storedAverage
        ?: weightedSaleAverage(recentSales, hqOnly)
    // Universalis' currentAveragePrice is a listing average, not a completed-sale
    // average. Keep it as a clearly labelled last-resort reference so a sparse item
    // still gets a stable guide line, without feeding the less trustworthy number
    // into ratio-based alerts.
    val hasQualityListings = listings?.listings?.any { it.hq == hqOnly && it.pricePerUnit > 0 } == true
    val listingAverage = weightedListingAverage(listings, hqOnly)
        ?: listings?.currentAvg?.takeIf { it > 0 && hasQualityListings }
    val displayAverage = saleAverage ?: listingAverage
    val averageLabel = if (saleAverage != null) "成交均价" else if (listingAverage != null) "在售均价" else null
    val benchmarks = buildList {
        if (npcPrice > 0) add(MarketBenchmarkLine("NPC 售价", npcPrice, PhoneWarn))
        displayAverage?.takeIf { it > 0 }?.let {
            add(MarketBenchmarkLine(averageLabel ?: "均价", it.toInt(), PhoneInfo))
        }
    }.sortedBy { it.price }
    ScreenFrame {
        ScreenHeader(
            it0?.nameCn ?: "道具 $itemId", state, onBack = onBack,
            trailing = {
                // Two independent actions, side by side: the heart just bookmarks the
                // item for the watchlist, the bell configures a price notification.
                // They used to be one button that forced the alert sheet open on every
                // favourite -- a favourite that wanted no notification had to dismiss
                // the sheet first, every single time.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(9.dp))
                            .clickable { showAlert = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        ImageGlyph(
                            if (hasAlert) R.drawable.ic2_bell else R.drawable.ic2_bell_off,
                            if (hasAlert) PhoneWarn else PhoneMuted,
                            Modifier.size(18.dp),
                        )
                    }
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(9.dp))
                            .clickable { watched = !watched },
                        contentAlignment = Alignment.Center,
                    ) {
                        ImageGlyph(
                            if (watched) R.drawable.ic2_heart_fill else R.drawable.ic2_heart,
                            if (watched) PhoneWarn else PhoneMuted,
                            Modifier.size(18.dp),
                        )
                    }
                }
            },
        )

        LaunchedEffect(watched, watchLoaded, scope) {
            if (!watchLoaded) return@LaunchedEffect
            if (watched) MarketRepository.addWatch(context, itemId, scope)
            else MarketRepository.removeWatch(context, itemId)
        }

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 28.dp),
        ) {
            // Three tabs, labelled with the actual world/DC so it reads as a place
            // rather than a setting. 本服 first: it is the only one you can buy from
            // right now, and the only one with live board data.
            item {
                MarketSegmented(
                    labels = tabs.map {
                        when (it) {
                            MarketView.World -> worldName.ifBlank { "本服" }
                            MarketView.Dc -> dcName.ifBlank { "本大区" }
                            MarketView.All -> "全区"
                        }
                    },
                    selectedIndex = tabs.indexOf(view).coerceAtLeast(0),
                    onSelect = { i ->
                        // Second tap on the current tab opens its picker: the default
                        // follows the character, so an override needs no permanent
                        // control of its own. 全区 has nothing to choose.
                        val target = tabs[i]
                        if (target == view && target != MarketView.All) picking = target
                        view = target
                        viewChosen = true
                        worldFilter = null
                    },
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = margin.dp, vertical = 12.dp),
                )
            }

            if (loading && agg == null) {
                item {
                    Box(
                        Modifier.fillMaxWidth().height(160.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = PhoneAccent, strokeWidth = 2.dp,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            } else {
                item {
                    // Only worth surfacing when travelling actually pays: a 3% gap is
                    // not worth a teleport. Suppressed on 全国, where the listing table
                    // already spans every DC and "elsewhere" means nothing.
                    //
                    // currentDc stays the DC even under the 本服 tab: the comparison is
                    // "is another DC cheaper", so passing a world name there would make
                    // every world in the player's own DC look like a destination.
                    val cheaper = if (view == MarketView.All) null else {
                        MarketApi.cheaperElsewhere(
                            agg = agg,
                            currentPrice = listings?.listings
                                ?.filter { !hqOnly || it.hq }
                                ?.minOfOrNull { it.pricePerUnit },
                            hqOnly = hqOnly,
                            worldNames = worlds.associate { it.id to it.name },
                            dcOfWorld = worlds.associate { it.id to it.dc },
                            currentDc = myDc,
                        )?.takeIf { it.savedPct >= 5.0 }
                    }
                    MarketSummaryCard(agg, listings, hqOnly, change24h, cheaper)
                }
                item { MarketStatsCard(listings, agg, scope, hqOnly) }

                // charts
                item {
                    MarketChartSection(
                        context = context, itemId = itemId, scope = scope,
                        benchmarks = benchmarks,
                        hqOnly = hqOnly,
                    )
                }

                // Live board only under 本服: it reads the board of the world the
                // character is standing on, so showing it beside DC-wide or nationwide
                // numbers would imply a coverage it does not have.
                // ...and only while the shown world IS the one being stood on. Hand-picking
                // another world makes the live read meaningless: the game can only report
                // the board in front of the character, so it would answer a different
                // question than the tab is asking.
                if (view == MarketView.World &&
                    worldName == state.profile?.currentWorld?.takeIf { it.isNotBlank() }
                ) {
                    item {
                        MarketLiveSection(
                            state, itemId, hqOnly,
                            attempt = liveAttempt,
                            benchmarks = benchmarks,
                            onRequery = { liveAttempt++ },
                            onBuy = { confirming = it },
                        )
                    }
                }

                // listing controls
                item {
                    MarketLinkSection(
                        when (view) {
                            MarketView.World -> "${worldName.ifBlank { "本服" }}挂单"
                            MarketView.Dc -> "各服务器在售"
                            MarketView.All -> "全区在售"
                        }
                    )
                }
                item {
                    val sorts = PriceSort.entries.toList()
                    MarketSegmented(
                        labels = sorts.map { it.label },
                        selectedIndex = sorts.indexOf(sort),
                        onSelect = { sort = sorts[it] },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = margin.dp),
                    )
                }
                item {
                    val names = listings?.listings?.map { it.worldName }?.distinct()?.sorted()
                        ?: emptyList()
                    // HQ sits outside the world-count check on purpose. It used to be
                    // inside, so under a single-world view the whole row vanished and
                    // took the HQ toggle with it -- filtering by quality is wanted
                    // regardless of how many worlds are in the table.
                    LazyRow(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        contentPadding = PaddingValues(horizontal = margin.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item {
                            MarketChip(if (hqOnly) "仅 HQ" else "含 NQ", hqOnly,
                                onClick = { hqOnly = !hqOnly })
                        }
                        if (names.size > 1) {
                            item {
                                // "所有服务器", not "全部": the tab strip above already
                                // uses 全国, and two different "all"s read as the same one.
                                MarketChip("所有服务器", worldFilter == null,
                                    onClick = { worldFilter = null })
                            }
                            lazyItems(names, key = { it }) { n ->
                                MarketChip(n, worldFilter == n,
                                    onClick = { worldFilter = if (worldFilter == n) null else n })
                            }
                        }
                    }
                }

                // Tax is per-world. Under 本服 the world is already known, so show it
                // without making the reader pick a chip that is not there.
                val taxWorld = worldFilter
                    ?: worldName.takeIf { view == MarketView.World && it.isNotBlank() }
                taxWorld?.let { w -> item { MarketTaxCard(w) } }

                val rows = (listings?.listings ?: emptyList())
                    .filter { worldFilter == null || it.worldName == worldFilter }
                    .filter { !hqOnly || it.hq }
                    .let { l ->
                        when (sort) {
                            PriceSort.UnitAsc -> l.sortedBy { it.pricePerUnit }
                            PriceSort.UnitDesc -> l.sortedByDescending { it.pricePerUnit }
                            // Sorts on the tax-inclusive total so the order matches the
                            // number rendered in each row.
                            PriceSort.TotalAsc -> l.sortedBy { it.totalWithTax }
                            PriceSort.World -> l.sortedWith(
                                compareBy({ it.worldName }, { it.pricePerUnit })
                            )
                        }
                    }

                if (rows.isEmpty()) {
                    item {
                        Text(
                            if (loading) "读取中…" else "这个范围内没有在售",
                            color = PhoneMuted, fontSize = 12.sp,
                            modifier = Modifier.padding(
                                horizontal = margin.dp, vertical = 20.dp,
                            ),
                        )
                    }
                } else {
                    item {
                        Text(
                            "${rows.size} 条在售" +
                                (listings?.uploadTimes?.values?.maxOrNull()
                                    ?.let { "  ·  最近更新 ${ago(it)}" } ?: ""),
                            color = PhoneMuted, fontSize = 11.sp,
                            modifier = Modifier.padding(
                                start = margin.dp, end = margin.dp,
                                top = 12.dp, bottom = 4.dp,
                            ),
                        )
                    }
                    // Hoisted out of the item lambda: it was an O(n) scan per row, running
                    // inside measure on the scroll path.
                    val cheapest = rows.minOf { it.pricePerUnit }
                    // Keyed on the server's listingID, which the API does provide --
                    // an earlier comment here claimed it did not. The old hand-built
                    // world+price+quantity+retainer key was genuinely not unique (815
                    // collisions across 3036 probed listings) and crashed the list
                    // mid-fling; MarketApi now guarantees listingId is present, unique
                    // and deduped, so identity survives sort and filter changes.
                    //
                    // Benchmark markers (NPC price / sale average) sit in the same list:
                    // each is drawn once, just before the first row priced above it, so
                    // "everything above the line sorts after it" reads at a glance.
                    val tableEntries = interleaveBenchmarks(rows, benchmarks) { it.pricePerUnit }
                    itemsIndexed(tableEntries, key = { _, e ->
                        when (e) {
                            is MarketApi.Listing -> "l-${e.listingId}"
                            is MarketBenchmarkLine -> "b-${e.label}-${e.price}"
                            else -> e.hashCode().toString()
                        }
                    }) { i, e ->
                        when (e) {
                            is MarketApi.Listing -> {
                                val prevSep = i == 0 || tableEntries[i - 1] is MarketBenchmarkLine
                                val nextSep =
                                    i == tableEntries.lastIndex ||
                                        tableEntries[i + 1] is MarketBenchmarkLine
                                MarketListingRow(
                                    e, cheapest = cheapest,
                                    first = prevSep, last = nextSep,
                                )
                            }
                            is MarketBenchmarkLine -> MarketBenchmarkRow(e)
                        }
                    }
                }
            }
        }
    }

    if (showAlert) {
        MarketAlertSheet(
            itemId = itemId,
            scope = scope,
            currentPrice = agg?.let { a ->
                (if (hqOnly) a.hqDc else a.nqDc)?.minPrice
                    ?: a.nqDc?.minPrice ?: a.nqWorld?.minPrice
            },
            average = saleAverage,
            onDismiss = {
                showAlert = false
                watchVersion++
                state.pushMonitorRules()
            },
        )
    }

    when (picking) {
        MarketView.World -> MarketScopePickerSheet(
            title = "选择服务器",
            options = dcWorlds,
            current = worldName,
            homeOption = myWorld,
            onPick = { pickedWorld = it.takeIf { n -> n != myWorld }; picking = null },
            onDismiss = { picking = null },
        )
        MarketView.Dc -> MarketScopePickerSheet(
            title = "选择大区",
            options = dcs.map { it.name },
            current = dcName,
            homeOption = myDc,
            onPick = { n ->
                pickedDc = n.takeIf { it != myDc }
                // A world from the old DC would not exist in the new one.
                pickedWorld = null
                picking = null
            },
            onDismiss = { picking = null },
        )
        else -> Unit
    }

    confirming?.let { l ->
        MarketPurchaseConfirmSheet(
            listing = l,
            onConfirm = {
                confirming = null
                state.purchaseLiveMarket(itemId, l)
            },
            onDismiss = { confirming = null },
        )
    }

    state.lastPurchaseResult?.let { result ->
        MarketPurchaseResultSheet(
            result = result,
            onDismiss = {
                state.clearPurchaseResult()
                // A successful buy cleared the cached board; re-query so the bought row
                // is gone rather than leaving the section blank.
                if (result.status == GameMarketPurchaseStatus.Ok) {
                    liveAttempt++
                    state.searchLiveMarket(itemId, hqOnly)
                }
            },
        )
    }
}

/**
 * Live board listings read from the game client via the desktop plugin.
 *
 * Kept visually distinct from the Universalis table because the two are not the
 * same kind of fact: this is the player's own client reading the board right now,
 * the other is crowd-uploaded and minutes-to-hours old. Only the current world,
 * since the game's listing struct carries no world field.
 */
@Composable
private fun MarketLiveSection(
    state: PhoneState,
    itemId: Int,
    hqOnly: Boolean,
    attempt: Int,
    benchmarks: List<MarketBenchmarkLine>,
    onRequery: () -> Unit,
    onBuy: (GameMarketListing) -> Unit,
) {
    val margin = LocalContentMargin.current
    val live = state.liveMarket?.takeIf { it.itemId == itemId }
    // A reply is not guaranteed: an older plugin has no MarketSearch case and the
    // opcode switch has no default, so it drops opcode 13 silently -- no error, no
    // reply. Without a timeout the spinner would spin forever.
    var timedOut by remember(itemId) { mutableStateOf(false) }
    LaunchedEffect(itemId, attempt) {
        if (attempt == 0) return@LaunchedEffect
        timedOut = false
        delay(12_000)
        if (state.liveMarket?.itemId != itemId) timedOut = true
    }

    // Fire on entry once the character is online, so the local-world board is simply
    // there. Keyed on (itemId, hqOnly, canQuery) rather than firing unconditionally:
    // hqOnly changes the answer, and connecting after the screen is already open
    // should still fill it in. Re-keying also cancels an in-flight wait, so switching
    // items mid-query cannot leave the previous item's timeout running.
    LaunchedEffect(itemId, hqOnly, state.canQueryLiveMarket) {
        if (!state.canQueryLiveMarket) return@LaunchedEffect
        // Each query rewrites the PC's shared search slot, so let the screen settle
        // before spending one -- avoids a burst while scrolling through items.
        delay(250)
        onRequery()
        state.searchLiveMarket(itemId, hqOnly)
    }

    if (!state.canQueryLiveMarket) return

    MarketLinkSection("游戏内实时数据")
    Column(Modifier.fillMaxWidth().padding(horizontal = margin.dp)) {
        when {
            live == null && timedOut -> Column(
                Modifier.fillMaxWidth().clip(PhoneCardShape).background(PhoneSurface)
                    .padding(16.dp),
            ) {
                Text("插件没有回应", color = PhoneText, fontSize = 12.sp)
                Text(
                    "电脑上的插件可能是旧版本，不认识市场查询。重装一次插件即可。",
                    color = PhoneMuted, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "下面的各服价格来自 Universalis，不受影响",
                    color = PhoneMuted, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    "重试", color = PhoneAccent, fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 10.dp)
                        .clickable {
                            onRequery()
                            state.searchLiveMarket(itemId, hqOnly)
                        },
                )
            }

            live == null -> Row(
                Modifier.fillMaxWidth().clip(PhoneCardShape).background(PhoneSurface)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    color = PhoneAccent, strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    "正在让游戏客户端查询…",
                    color = PhoneMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }

            live.status != GameMarketStatus.Ok -> Column(
                Modifier.fillMaxWidth().clip(PhoneCardShape).background(PhoneSurface)
                    .padding(16.dp),
            ) {
                Text(liveMarketRefusal(live.status), color = PhoneText, fontSize = 12.sp)
                Text(
                    "下面的各服价格来自 Universalis，不受影响",
                    color = PhoneMuted, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "重试", color = PhoneAccent, fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 10.dp)
                        .clickable {
                            onRequery()
                            state.searchLiveMarket(itemId, hqOnly)
                        },
                )
            }

            live.listings.isEmpty() -> Column(
                Modifier.fillMaxWidth().clip(PhoneCardShape).background(PhoneSurface)
                    .padding(16.dp),
            ) {
                Text(
                    "${live.currentWorldName.ifBlank { "本服" }}当前没有人在卖",
                    color = PhoneText, fontSize = 12.sp,
                )
                Text(
                    "游戏客户端刚刚查过，不是缓存",
                    color = PhoneMuted, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            else -> {
                val rows = live.listings.sortedBy { it.unitPrice }
                val cheapest = rows.first().unitPrice
                Row(
                    Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${live.currentWorldName.ifBlank { "本服" }}  ·  ${rows.size} 条",
                        color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.weight(1f),
                    )
                    Text(
                        "刷新", color = PhoneAccent, fontSize = 11.sp,
                        modifier = Modifier.clickable {
                            onRequery()
                            state.searchLiveMarket(itemId, hqOnly)
                        },
                    )
                }
                val liveEntries = interleaveBenchmarks(rows, benchmarks) { it.unitPrice }
                liveEntries.forEachIndexed { i, e ->
                    when (e) {
                        is GameMarketListing -> {
                            val prevSep = i == 0 || liveEntries[i - 1] is MarketBenchmarkLine
                            val nextSep =
                                i == liveEntries.lastIndex ||
                                    liveEntries[i + 1] is MarketBenchmarkLine
                            LiveListingRow(
                                e, cheapest, first = prevSep, last = nextSep,
                                pending = state.pendingPurchaseListingId == e.listingId,
                                // Any purchase in flight disables the rest: the plugin runs one
                                // at a time, so a second tap could only come back Busy.
                                buyEnabled = state.pendingPurchaseListingId == null,
                                onBuy = { onBuy(e) },
                            )
                        }
                        is MarketBenchmarkLine -> MarketBenchmarkRow(e)
                    }
                }
                Text(
                    "由电脑插件实时读取，仅本服（跨服价格见下方）",
                    color = PhoneMuted, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

/**
 * Reference marker between listing rows: a tinted hairline with a labelled badge,
 * drawn just before the first row priced above the benchmark (DR's "above the line
 * sorts after it" split, without touching the row order).
 */
@Composable
private fun MarketBenchmarkRow(b: MarketBenchmarkLine) {
    val margin = LocalContentMargin.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = margin.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.weight(1f).height(1.dp)
                .background(b.color.copy(alpha = 0.35f)),
        )
        Text(
            "${b.label} ${gil(b.price)}",
            color = b.color, fontSize = 10.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(b.color.copy(alpha = 0.12f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Box(
            Modifier.weight(1f).height(1.dp)
                .background(b.color.copy(alpha = 0.35f)),
        )
    }
}

/** Why the game refused, in the player's terms rather than the enum's. */
private fun liveMarketRefusal(status: GameMarketStatus): String = when (status) {
    GameMarketStatus.BoardOpen -> "电脑上的市场布告板正开着，请先关掉再试"
    GameMarketStatus.InDuty -> "角色在副本里，游戏不允许查询市场"
    GameMarketStatus.NotLoggedIn -> "角色不在线"
    GameMarketStatus.NotMarketable -> "这件道具不能在市场板交易"
    GameMarketStatus.Timeout -> "游戏没有及时返回，可以再试一次"
    else -> "读取失败"
}

/**
 * Confirm sheet for a purchase.
 *
 * Spells out the total including tax, because that is the number that leaves the
 * wallet and the row above shows the unit price first. Deliberately a second tap:
 * the board is a live list and a mis-tap here costs real gil.
 */
@Composable
private fun MarketPurchaseConfirmSheet(
    listing: GameMarketListing,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(PhoneSurface)
                .clickable(enabled = false) {}
                .padding(20.dp),
        ) {
            Text("确认购买", color = PhoneText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${gil(listing.totalWithTax)} gil",
                color = PhoneAccent, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(
                buildList {
                    add("单价 ${gil(listing.unitPrice)}")
                    add("数量 ${listing.quantity}")
                    if (listing.tax > 0) add("税 ${gil(listing.tax)}")
                    if (listing.hq) add("HQ")
                    if (listing.isSet) add("整套")
                    if (listing.materiaCount > 0) add("魔晶石 ${listing.materiaCount}")
                }.joinToString("  ·  "),
                color = PhoneMuted, fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            listing.townName.takeIf { it.isNotBlank() }?.let {
                Text(
                    "雇员在 $it", color = PhoneMuted, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                "整条一次买完，不能只买一部分。买完不能撤销。",
                color = PhoneWarn, fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "取消", color = PhoneMuted, fontSize = 14.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                )
                Text(
                    "买下", color = PhoneText, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PhoneAccent)
                        .clickable { onConfirm() }
                        .padding(horizontal = 20.dp, vertical = 9.dp),
                )
            }
        }
    }
}

/**
 * Outcome sheet.
 *
 * Every failure says whether gil was spent, because that is the only thing the
 * player actually needs to know. Timeout is the one case where it is genuinely
 * unknown, and it says so instead of guessing.
 */
@Composable
private fun MarketPurchaseResultSheet(
    result: GameMarketPurchase,
    onDismiss: () -> Unit,
) {
    val ok = result.status == GameMarketPurchaseStatus.Ok
    Box(
        Modifier.fillMaxSize().background(Color(0x99000000)).clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                .background(PhoneSurface)
                .clickable(enabled = false) {}
                .padding(20.dp),
        ) {
            Text(
                if (ok) "买到了" else "没买成",
                color = if (ok) PhoneGreen else PhoneWarn,
                fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            )
            if (ok) {
                Text(
                    "${gil(result.unitPrice * result.quantity + result.tax)} gil",
                    color = PhoneText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "单价 ${gil(result.unitPrice)}  ·  数量 ${result.quantity}" +
                        if (result.tax > 0) "  ·  税 ${gil(result.tax)}" else "",
                    color = PhoneMuted, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    "东西已经进背包了",
                    color = PhoneMuted, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            } else {
                Text(
                    purchaseFailureText(result.status),
                    color = PhoneText, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    purchaseSpendText(result.status),
                    color = PhoneMuted, fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    "知道了", color = PhoneText, fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .background(PhoneAccent.copy(alpha = 0.18f))
                        .clickable { onDismiss() }
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                )
            }
        }
    }
}

/** Why it failed, in the player's terms. */
private fun purchaseFailureText(status: GameMarketPurchaseStatus): String = when (status) {
    GameMarketPurchaseStatus.ListingGone -> "这条已经被别人买走了"
    GameMarketPurchaseStatus.Changed -> "这条的价格或数量变了，没有按旧价格下单"
    GameMarketPurchaseStatus.NotEnoughGil -> "金币不够（含税）"
    GameMarketPurchaseStatus.BoardOpen -> "电脑上的市场布告板正开着，请先关掉再试"
    GameMarketPurchaseStatus.InDuty -> "角色在副本里，游戏不允许买东西"
    GameMarketPurchaseStatus.NotLoggedIn -> "角色不在线"
    GameMarketPurchaseStatus.NotMarketable -> "这件道具不能在市场板交易"
    GameMarketPurchaseStatus.Disabled -> "电脑上的插件把手机购买关掉了"
    GameMarketPurchaseStatus.Busy -> "上一笔还没处理完，稍后再试"
    GameMarketPurchaseStatus.Refused -> "游戏拒绝了这笔交易，常见原因是背包满了"
    GameMarketPurchaseStatus.Timeout -> "游戏没有回应"
    GameMarketPurchaseStatus.Ok -> ""
}

/**
 * Whether gil moved. Separate from the reason because it is the part that matters,
 * and because Timeout genuinely cannot answer it.
 */
private fun purchaseSpendText(status: GameMarketPurchaseStatus): String = when (status) {
    GameMarketPurchaseStatus.Timeout ->
        "无法确认这笔是否成交，请到游戏里核对背包和金币，不要直接重试"
    else -> "没有花钱"
}

/** One live row. Same shape as the Universalis rows so prices stay comparable. */
@Composable
private fun LiveListingRow(
    l: GameMarketListing,
    cheapest: Int,
    first: Boolean,
    last: Boolean,
    pending: Boolean = false,
    buyEnabled: Boolean = false,
    onBuy: () -> Unit = {},
) {
    val isCheapest = l.unitPrice == cheapest
    val r = 18.dp
    val shape = RoundedCornerShape(
        topStart = if (first) r else 0.dp, topEnd = if (first) r else 0.dp,
        bottomStart = if (last) r else 0.dp, bottomEnd = if (last) r else 0.dp,
    )
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(shape)
                // Same tint and alpha as MarketListingRow: the two tables sit one
                // above the other, so a different highlight reads as a different widget.
                .background(if (isCheapest) PhoneAccent.copy(alpha = 0.07f) else PhoneSurface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.width(84.dp)) {
                Text(
                    gil(l.unitPrice),
                    color = if (isCheapest) PhoneGreen else PhoneText,
                    fontSize = 16.sp,
                    fontWeight = if (isCheapest) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text("gil", color = PhoneMuted, fontSize = 9.sp)
            }
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                // Tax-inclusive, matching the Universalis rows above.
                Text(
                    "×${l.quantity} = ${gil(l.totalWithTax)} gil",
                    color = PhoneText, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildList {
                        if (l.tax > 0) add("含税 ${gil(l.tax)}")
                        if (l.isSet) add("整套")
                        if (l.materiaCount > 0) add("魔晶石 ${l.materiaCount}")
                    }.joinToString("  ·  ").ifBlank { "无附加信息" },
                    color = PhoneMuted, fontSize = 10.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            // Same slot the Universalis rows put the world badge in, so the two
            // tables line up instead of the live one looking like a different widget.
            Column(horizontalAlignment = Alignment.End) {
                l.townName.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it, color = PhoneAccent, fontSize = 10.sp,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(PhoneAccent.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                if (l.hq) {
                    Text(
                        "HQ", color = PhoneWarn, fontSize = 9.sp,
                        modifier = Modifier.padding(top = 3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(PhoneWarn.copy(alpha = 0.14f))
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
                // Buying is only offered on live rows: Universalis rows are a cache of
                // another world's board and cannot be acted on.
                if (pending) {
                    CircularProgressIndicator(
                        color = PhoneAccent, strokeWidth = 1.5.dp,
                        modifier = Modifier.padding(top = 5.dp).size(14.dp),
                    )
                } else {
                    Text(
                        "买", color = if (buyEnabled) PhoneAccent else PhoneMuted,
                        fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 5.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (buyEnabled) PhoneAccent.copy(alpha = 0.14f)
                                else PhoneMuted.copy(alpha = 0.08f),
                            )
                            .clickable(enabled = buyEnabled) { onBuy() }
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
            }
        }
        if (!last) {
            Box(
                Modifier.fillMaxWidth().background(PhoneSurface).padding(start = 14.dp),
            ) { PhoneHairlineRow(0.dp) }
        }
    }
}

/**
 * Retainer-city sales tax on one world, cheapest city first.
 *
 * World-scoped because rates are not uniform inside a DC -- probing all 28 CN
 * worlds found 3 of 4 DCs disagree internally (莫古力 had 7 distinct tables), so
 * there is no honest DC-wide figure to show.
 *
 * This is seller information: it tells you where to park a retainer. Buyers get
 * the tax they actually pay on each listing row instead.
 */
@Composable
private fun MarketTaxCard(world: String) {
    val margin = LocalContentMargin.current
    var rates by remember(world) { mutableStateOf<List<MarketApi.CityTax>>(emptyList()) }

    LaunchedEffect(world) {
        rates = runCatching { MarketApi.taxRates(world) }.getOrDefault(emptyList())
    }
    if (rates.isEmpty()) return

    val lowest = rates.first().rate
    val cheapCities = rates.filter { it.rate == lowest }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = margin.dp, vertical = 12.dp)
            .clip(PhoneCardShape).background(PhoneSurface).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$world 寄卖税", color = PhoneMuted, fontSize = 11.sp,
                modifier = Modifier.weight(1f))
            Text("每周重置变动", color = PhoneMuted, fontSize = 10.sp)
        }
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
            Text(
                "$lowest%", color = PhoneGreen, fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                cheapCities.joinToString("、") { it.city },
                color = PhoneText, fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 3.dp),
            )
        }
        Text(
            "最低税城市，挂售放这里最划算",
            color = PhoneMuted, fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
        // The rest only matters if you are choosing between cities, so keep it
        // compact rather than a row per city.
        rates.filter { it.rate != lowest }.takeIf { it.isNotEmpty() }?.let { rest ->
            Column(Modifier.padding(top = 10.dp)) {
                PhoneHairlineRow(0.dp)
                Text(
                    rest.joinToString("　") { "${it.city} ${it.rate}%" },
                    color = PhoneMuted, fontSize = 11.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

/**
 * Grouped-list section header: small, muted, letterspaced, sitting well above its
 * card. The generous top gap is what separates groups in iOS -- without it the
 * page reads as one undifferentiated stack.
 */
@Composable
internal fun MarketLinkSection(title: String) {
    val margin = LocalContentMargin.current
    Text(
        title, color = PhoneMuted, fontSize = 11.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp,
        modifier = Modifier.padding(
            start = margin.dp + 4.dp, end = margin.dp, top = 28.dp, bottom = 8.dp,
        ),
    )
}

/**
 * Top summary: cheapest now, recent sale, average, daily volume.
 *
 * Shows DC and region side by side because "cheap on another DC" is only
 * actionable if you know you have to travel for it.
 */
@Composable
private fun MarketSummaryCard(
    agg: MarketApi.ItemAgg?,
    listings: MarketApi.Listings?,
    hqOnly: Boolean,
    change24h: Double?,
    cheaper: MarketApi.CheaperElsewhere? = null,
) {
    val margin = LocalContentMargin.current
    // Region blocks are the only ones populated when scope is a region (全部).
    val dc = agg?.let { if (hqOnly) it.hqDc else it.nqDc }
        ?: agg?.nqDc ?: agg?.let { if (hqOnly) it.hqRegion else it.nqRegion } ?: agg?.nqRegion
    val region = agg?.let { if (hqOnly) it.hqRegion else it.nqRegion } ?: agg?.nqRegion
    // Prefer the merged listings for "cheapest": with 全部 they span every DC.
    val cheapest = listings?.listings?.minByOrNull { it.pricePerUnit }

    Column(
        Modifier.fillMaxWidth().padding(horizontal = margin.dp, vertical = 12.dp)
            .clip(PhoneCardShape).background(PhoneSurface).padding(16.dp),
    ) {
        Text("当前最低价", color = PhoneMuted, fontSize = 11.sp)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
            Text(
                (cheapest?.pricePerUnit ?: dc?.minPrice)?.let { gil(it) } ?: "—",
                color = PhoneText, fontSize = 30.sp, fontWeight = FontWeight.Bold,
            )
            Text(
                " gil", color = PhoneMuted, fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 5.dp),
            )
            // 24h direction. Down is good for a buyer, so green on a fall.
            change24h?.let { c ->
                val down = c < 0
                Text(
                    (if (down) "▼ " else "▲ ") + "%.1f%%".format(kotlin.math.abs(c)),
                    color = if (down) PhoneGreen else PhoneWarn,
                    fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 10.dp, bottom = 6.dp),
                )
                Text(
                    "24小时", color = PhoneMuted, fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                )
            }
        }
        if (cheapest != null) {
            Text(
                "在 ${cheapest.worldName}" + if (cheapest.hq) "  ·  HQ" else "",
                color = PhoneAccent, fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        listings?.uploadTimes?.values?.maxOrNull()?.let {
            Text(
                "${ago(it)}更新", color = PhoneMuted, fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Actionable before informational: travelling elsewhere is a decision, the
        // rows below are just context. A bare "跨区最低" number left the reader to do
        // the subtraction themselves.
        cheaper?.let { c ->
            Column(
                Modifier.fillMaxWidth().padding(top = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(PhoneGreen.copy(alpha = 0.10f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    "${c.worldName}更便宜  ${gil(c.price)} gil",
                    color = PhoneGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                )
                Text(
                    "省 %.0f%%".format(c.savedPct) +
                        if (c.crossDc) "  ·  需跨大区传送" else "  ·  同大区内",
                    color = PhoneMuted, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        PhoneHairlineRow(0.dp)
        Column(Modifier.padding(top = 12.dp)) {
            MarketFactRow("最近成交", dc?.recentPrice?.let {
                gil(it) + (dc.recentAtMs?.let { t -> "  ·  ${ago(t)}" } ?: "")
            })
            MarketFactRow("平均成交", dc?.avgPrice?.let { gil(it) })
            MarketFactRow("日均成交量", dc?.dailyQty?.let { "%.0f".format(it) })
            MarketFactRow("跨区最低", region?.minPrice?.let { gil(it) })
        }
    }
}

/**
 * Listing spread (min / median / max) plus the sale average, split NQ vs HQ.
 *
 * 平均 comes from *sale history*, not listings: players park items at 999,999,999
 * to keep them off the board, and across 6 sampled items that skewed the listing
 * mean by up to 294,000x. The middle column is therefore a median, and the max is
 * marked when it is clearly a park rather than a real offer.
 */
@Composable
private fun MarketStatsCard(
    listings: MarketApi.Listings?,
    agg: MarketApi.ItemAgg?,
    scope: String,
    hqOnly: Boolean,
) {
    val margin = LocalContentMargin.current
    val all = listings?.stats(null) ?: return
    val nq = listings.stats(false)
    val hq = listings.stats(true)
    val saleAvg = (agg?.let { if (hqOnly) it.hqRegion else it.nqRegion }
        ?: agg?.nqRegion ?: agg?.nqDc)?.avgPrice

    Column(
        Modifier.fillMaxWidth().padding(horizontal = margin.dp)
            .clip(PhoneCardShape).background(PhoneSurface).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("价格分布", color = PhoneMuted, fontSize = 11.sp,
                modifier = Modifier.weight(1f))
            Text("$scope  ·  ${all.count} 条在售", color = PhoneMuted, fontSize = 11.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            StatCell("最低价", gil(all.min), Modifier.weight(1f))
            StatCell(
                if (saleAvg != null) "成交均价" else "中位价",
                gil(saleAvg ?: all.median), Modifier.weight(1f),
            )
            StatCell(
                if (all.maxIsOutlier) "最高价*" else "最高价",
                gil(all.max), Modifier.weight(1f),
            )
        }
        if (all.maxIsOutlier) {
            Text(
                "* 最高价是有人挂的天价（占位用），不是真实行情",
                color = PhoneMuted, fontSize = 10.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        // Per-quality rows only when both exist; otherwise it is noise.
        if (nq != null && hq != null) {
            Column(Modifier.padding(top = 12.dp)) {
                MarketFactRow(
                    "NQ 最低 / 中位",
                    "${gil(nq.min)} / ${gil(nq.median)}  （${nq.count} 条）",
                )
                MarketFactRow(
                    "HQ 最低 / 中位",
                    "${gil(hq.min)} / ${gil(hq.median)}  （${hq.count} 条）",
                )
            }
        } else {
            Text(
                if (hq == null) "当前没有 HQ 在售" else "当前没有 NQ 在售",
                color = PhoneMuted, fontSize = 10.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value, color = PhoneText, fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "gil", color = PhoneMuted, fontSize = 9.sp,
                modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
            )
        }
        Text(label, color = PhoneMuted, fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun MarketFactRow(label: String, value: String?) {
    if (value == null) return
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(value, color = PhoneText, fontSize = 13.sp)
    }
}

/**
 * One listing inside the grouped inset card.
 *
 * [first]/[last] round only the outer corners so consecutive rows read as one
 * surface. Cheapest row gets a faint accent wash rather than coloured text alone.
 */
@Composable
private fun MarketListingRow(
    l: MarketApi.Listing,
    cheapest: Int,
    first: Boolean = false,
    last: Boolean = false,
) {
    val margin = LocalContentMargin.current
    val isCheapest = l.pricePerUnit == cheapest
    val r = 18.dp
    val shape = RoundedCornerShape(
        topStart = if (first) r else 0.dp, topEnd = if (first) r else 0.dp,
        bottomStart = if (last) r else 0.dp, bottomEnd = if (last) r else 0.dp,
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = margin.dp)) {
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(
                if (isCheapest) PhoneAccent.copy(alpha = 0.07f) else PhoneSurface
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // unit price leads: it is what people compare on
        Column(Modifier.width(84.dp)) {
            Text(
                gil(l.pricePerUnit),
                color = if (isCheapest) PhoneGreen else PhoneText,
                fontSize = 16.sp,
                fontWeight = if (isCheapest) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text("gil", color = PhoneMuted, fontSize = 9.sp)
        }
        Column(Modifier.weight(1f).padding(start = 8.dp)) {
            // Total shown tax-inclusive: that is the number the buyer is charged.
            // The API's `total` field excludes tax (27 x 300 = 8100, tax 405).
            Text(
                "×${l.quantity} = ${gil(l.totalWithTax)} gil",
                color = PhoneText, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildList {
                    if (l.tax > 0) add("含税 ${gil(l.tax)}")
                    // lastReviewTime is seconds; ago() takes ms
                    if (l.lastReviewSec > 0) add("上架 ${ago(l.lastReviewSec * 1000)}")
                    l.retainerName.takeIf { it.isNotBlank() }?.let { add("提供：$it") }
                }.joinToString("  ·  "),
                color = PhoneMuted, fontSize = 10.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                l.worldName, color = PhoneAccent, fontSize = 10.sp,
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(PhoneAccent.copy(alpha = 0.12f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
            if (l.hq) {
                Text(
                    "HQ", color = PhoneWarn, fontSize = 9.sp,
                    modifier = Modifier.padding(top = 3.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(PhoneWarn.copy(alpha = 0.14f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                )
            }
        }
    }
        // Separator lives inside the card and stops short of the leading edge,
        // so the group reads as one object. No line after the final row.
        if (!last) {
            Box(
                Modifier.fillMaxWidth().background(PhoneSurface)
                    .padding(start = 14.dp),
            ) { PhoneHairlineRow(0.dp) }
        }
    }
}
