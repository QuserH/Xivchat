package com.quserh.eorzeaphone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.GameMarketCategory
import com.quserh.eorzeaphone.data.GameMarketItem
import com.quserh.eorzeaphone.data.GameMarketSubcategory
import com.quserh.eorzeaphone.data.market.MarketRepository
import com.quserh.eorzeaphone.data.wiki.WikiDb
import com.quserh.eorzeaphone.data.wiki.WikiFilter
import com.quserh.eorzeaphone.data.wiki.WikiItem
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneGreen
import com.quserh.eorzeaphone.ui.theme.PhoneLine
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.PhoneType
import com.quserh.eorzeaphone.ui.theme.PhoneWarn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Market board. Prices come from Universalis (same source as the
 * ffxiv-priceinsight plugin) via [com.quserh.eorzeaphone.data.market.MarketApi].
 *
 * Item search is local (items.db) -- all 16843 marketable ids exist there, so we
 * never round-trip just to resolve a name.
 */
internal sealed interface MarketDest {
    data class Item(val itemId: Int) : MarketDest
    data object Watchlist : MarketDest
    data object Categories : MarketDest
    data class Category(val categoryId: Int) : MarketDest
}

/**
 * Market 的导航状态。
 *
 * 必须活在 composable 外面：MarketScreen 挂在外层 AnimatedContent 里，
 * 跨应用跳转时这个 composable 会重建，`remember` 的东西全丢——
 * 所以从背包跳过来后详情页闪一下就消失。
 * 放到 object 里之后，导航栈不会因为重组而丢失。
 */
private object MarketNav {
    var stack by mutableStateOf(listOf<MarketDest>())
        private set

    val current: MarketDest? get() = stack.lastOrNull()

    fun push(dest: MarketDest) { stack = stack + dest }

    fun pop() { if (stack.isNotEmpty()) stack = stack.dropLast(1) }

    fun clear() { stack = emptyList() }

    val canGoBack: Boolean get() = stack.isNotEmpty()

    /** 统一的返回逻辑，供 BackHandler 和界面左上角返回按钮使用 */
    fun handleBack(state: PhoneState) {
        if (stack.size > 1) pop()
        else {
            val returnTo = state.marketReturnToApp
            if (returnTo != null) {
                state.marketReturnToApp = null
                state.openApp(returnTo)
            } else {
                state.back()
            }
        }
    }

    val searchState = MarketSearchState()
}

@Composable
fun MarketScreen(state: PhoneState) {
    BackHandler(enabled = MarketNav.canGoBack) {
        MarketNav.handleBack(state)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { state.pendingMarketItemId }.collect { pending ->
            if (pending != null) {
                MarketNav.clear()
                MarketNav.push(MarketDest.Item(pending))
                state.pendingMarketItemId = null
            }
        }
    }

    val motion = phoneMotionEnabled()
    val targetState = MarketNav.stack.size to MarketNav.current
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            phoneNavTransition(
                motionAllowed = motion,
                targetDepth = targetState.first,
                initialDepth = initialState.first,
            )
        },
        label = "market-navigation",
    ) { (_, top) ->
        when (top) {
            null -> MarketSearchScreen(state, MarketNav.searchState, onOpen = { MarketNav.push(it) })
            is MarketDest.Item -> MarketItemScreen(
                state, top.itemId,
                onBack = { MarketNav.handleBack(state) },
                onOpen = { MarketNav.push(it) },
            )
            MarketDest.Watchlist -> MarketWatchlistScreen(
                state,
                onBack = { MarketNav.handleBack(state) },
                onOpen = { MarketNav.push(it) },
            )
            MarketDest.Categories -> MarketCategoriesScreen(
                state,
                onBack = { MarketNav.handleBack(state) },
                onOpen = { MarketNav.push(it) },
            )
            is MarketDest.Category -> MarketCategoryScreen(
                state, top.categoryId,
                onBack = { MarketNav.handleBack(state) },
                onOpen = { MarketNav.push(it) },
            )
        }
    }
}

/**
 * Survives navigation so returning from a detail page keeps the query.
 *
 * Lives in MarketScreen's `remember`, which outlives MarketSearchScreen: pushing a
 * detail page swaps the `when` branch, so the search screen leaves composition and
 * every `remember` inside it is discarded.
 *
 * [results] and [marketable] are cached here too, not just [input]. Keeping only the
 * query meant coming back re-ran WikiDb.search from an empty list -- the field showed
 * the old text with nothing under it until the query finished, which reads as "my
 * search results vanished". Caching the rows makes the return paint immediately; the
 * effect still re-queries and replaces them.
 *
 * [marketable] is cached for a second reason: it loads async, so on every return it
 * started empty and the search ran once unfiltered, then a second time once the ids
 * arrived -- two DB queries per visit for one search.
 */
internal class MarketSearchState {
    var input: String by mutableStateOf("")
    var results: List<WikiItem> by mutableStateOf(emptyList())
    var marketable: Set<Int> by mutableStateOf(emptySet())
}

/**
 * Search screen.
 *
 * Requested behaviour, implemented literally:
 * - focusing the empty field shows search history first
 * - typing hides history and searches live, **no Enter needed** (260ms debounce)
 * - history is only in the way while it is useful
 */
@OptIn(FlowPreview::class)
@Composable
private fun MarketSearchScreen(
    state: PhoneState,
    saved: MarketSearchState,
    onOpen: (MarketDest) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val keyboard = LocalSoftwareKeyboardController.current
    val margin = LocalContentMargin.current

    var input by remember { mutableStateOf(saved.input) }
    var query by remember { mutableStateOf(saved.input) }
    var focused by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<String>>(emptyList()) }
    // Seeded from the caller-owned state so a return from a detail page paints the
    // previous hits immediately instead of flashing an empty list.
    var results by remember { mutableStateOf(saved.results) }
    var marketable by remember { mutableStateOf(saved.marketable) }
    var loading by remember { mutableStateOf(false) }
    var watchCount by remember { mutableStateOf(0) }
    // Deletes are applied to the list immediately, then flushed to disk here so
    // the row tap stays synchronous.
    var pendingRemove by remember { mutableStateOf<String?>(null) }
    var pendingClear by remember { mutableStateOf(false) }

    LaunchedEffect(pendingRemove) {
        pendingRemove?.let { MarketRepository.removeSearch(context, it); pendingRemove = null }
    }
    LaunchedEffect(pendingClear) {
        if (pendingClear) { MarketRepository.clearSearchHistory(context); pendingClear = false }
    }

    // Live search, no Enter. 260ms matches the item browser.
    LaunchedEffect(Unit) {
        snapshotFlow { input }
            .debounce(260)
            .distinctUntilChanged()
            .collect { query = it; saved.input = it }
    }

    LaunchedEffect(Unit) {
        history = MarketRepository.searchHistory(context)
        watchCount = MarketRepository.watchList(context).size
        // Skip the reload when we already carry the ids across a detail visit.
        if (marketable.isEmpty()) {
            marketable = runCatching {
                com.quserh.eorzeaphone.data.market.MarketApi.marketableIds(context)
            }.getOrDefault(emptySet())
            saved.marketable = marketable
        }
    }

    LaunchedEffect(query, marketable) {
        val q = query.trim()
        if (q.isBlank()) {
            results = emptyList()
            saved.results = emptyList()
            return@LaunchedEffect
        }
        // Only show the spinner when there is nothing to look at. With cached rows
        // on screen, flipping to a spinner would undo the point of caching them.
        loading = results.isEmpty()
        results = runCatching {
            WikiDb.search(context, WikiFilter(query = q), page = 0, pageSize = 60)
                // Only tradable items -- an untradable hit here is a dead end,
                // and one such id makes the whole aggregated batch return 400.
                .filter { marketable.isEmpty() || it.id in marketable }
        }.getOrDefault(emptyList())
        saved.results = results
        loading = false
    }

    // History is shown when the field has focus and nothing is typed.
    val showHistory = focused && input.isBlank() && history.isNotEmpty()

    ScreenFrame {
        ScreenHeader("市场", state, showBack = false)
        MarketSearchField(
            input,
            onChange = { input = it },
            onSubmit = {
                keyboard?.hide()
                if (input.isNotBlank()) {
                    // Only record on explicit submit; recording every keystroke
                    // would fill history with prefixes of one word.
                    saved.input = input
                }
            },
            onFocusChange = { focused = it },
        )

        when {
            showHistory -> MarketHistoryList(
                history,
                onPick = { input = it; query = it; focused = false; keyboard?.hide() },
                onRemove = { q -> history = history - q; pendingRemove = q },
                onClear = { history = emptyList(); pendingClear = true },
            )

            query.isBlank() -> MarketHome(watchCount, onOpen = onOpen)

            loading && results.isEmpty() -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = PhoneAccent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp),
                )
            }

            results.isEmpty() -> MarketEmpty("没有能上市场的「$query」")

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                item {
                    Text(
                        "共 ${results.size} 件",
                        color = PhoneMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(
                            start = margin.dp, end = margin.dp, bottom = 4.dp,
                        ),
                    )
                }
                lazyItems(results, key = { it.id }) { it2 ->
                    MarketItemRow(it2) {
                        keyboard?.hide()
                        onOpen(MarketDest.Item(it2.id))
                    }
                }
            }
        }
    }

    // Persist the query once it settles, so history holds words not prefixes.
    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length >= 2) {
            MarketRepository.addSearch(context, q)
            history = MarketRepository.searchHistory(context)
        }
    }
}

/**
 * Search field. 44dp min height for the touch target, rounded, muted fill --
 * the iOS search-bar shape rather than an outlined Material box.
 */
@Composable
private fun MarketSearchField(
    value: String,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val margin = LocalContentMargin.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = margin.dp)
            .height(44.dp).clip(RoundedCornerShape(12.dp))
            .background(PhoneSurfaceRaised).padding(horizontal = 12.dp),
    ) {
        ImageGlyph(R.drawable.ic2_search, PhoneAccent, Modifier.size(18.dp))
        BasicTextField(
            value,
            onChange,
            singleLine = true,
            textStyle = TextStyle(color = PhoneText, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                .onFocusChanged { onFocusChange(it.isFocused) },
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text("搜道具名，边打边搜", color = PhoneMuted, fontSize = 13.sp)
                    }
                    field()
                }
            },
        )
        if (value.isNotBlank()) {
            Box(
                Modifier.size(24.dp).clip(RoundedCornerShape(12.dp))
                    .clickable { onChange("") },
                contentAlignment = Alignment.Center,
            ) {
                ImageGlyph(R.drawable.ic2_close, PhoneMuted, Modifier.size(14.dp))
            }
        }
    }
}

/** Search history. Newest first, each removable, with a clear-all at the bottom. */
@Composable
private fun MarketHistoryList(
    history: List<String>,
    onPick: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    val margin = LocalContentMargin.current
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
    ) {
        item {
            Text(
                "最近搜索",
                color = PhoneMuted, style = PhoneType.SectionLabel,
                modifier = Modifier.padding(
                    start = margin.dp, end = margin.dp, bottom = 4.dp,
                ),
            )
        }
        lazyItems(history, key = { it }) { q ->
            Row(
                Modifier.fillMaxWidth().clickable { onPick(q) }
                    .padding(horizontal = margin.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ImageGlyph(R.drawable.ic2_history, PhoneMuted, Modifier.size(16.dp))
                Text(
                    q, color = PhoneText, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                )
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(14.dp))
                        .clickable { onRemove(q) },
                    contentAlignment = Alignment.Center,
                ) {
                    ImageGlyph(R.drawable.ic2_close, PhoneMuted, Modifier.size(13.dp))
                }
            }
            PhoneHairlineRow(margin.dp)
        }
        item {
            Text(
                "清空搜索记录",
                color = PhoneMuted, fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth()
                    .clickable { onClear() }
                    .padding(horizontal = margin.dp, vertical = 16.dp),
            )
        }
    }
}

/** Landing state: watchlist entry + a hint about what this screen does. */
@Composable
private fun MarketHome(watchCount: Int, onOpen: (MarketDest) -> Unit) {
    val margin = LocalContentMargin.current
    Column(
        Modifier.fillMaxSize().padding(horizontal = margin.dp, vertical = 12.dp),
    ) {
        PhoneCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onOpen(MarketDest.Watchlist) },
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(PhoneWarn.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageGlyph(R.drawable.ic2_star, PhoneWarn, Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        "关注列表", color = PhoneText, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        if (watchCount > 0) "$watchCount 件在盯着，可设降价提醒"
                        else "收藏道具，设降价提醒",
                        color = PhoneMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                ImageGlyph(R.drawable.ic2_chevron_right, PhoneMuted, Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        PhoneCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = { onOpen(MarketDest.Categories) },
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                        .background(PhoneAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageGlyph(R.drawable.ic2_grid, PhoneAccent, Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        "浏览分类", color = PhoneText, fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "按游戏内类别浏览所有可交易物品",
                        color = PhoneMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                ImageGlyph(R.drawable.ic2_chevron_right, PhoneMuted, Modifier.size(16.dp))
            }
        }
        Text(
            "价格来自 Universalis，玩家上传汇总。国服 4 个大区都有数据。",
            color = PhoneMuted, fontSize = 11.sp, lineHeight = 17.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** One search result. Icon + name + level, price is fetched on the detail page. */
@Composable
private fun MarketItemRow(item: WikiItem, onClick: () -> Unit) {
    val margin = LocalContentMargin.current
    PhonePressable(onClick = onClick, shape = RoundedCornerShape(0.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick)
                .padding(horizontal = margin.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                    .background(PhoneSurface),
                contentAlignment = Alignment.Center,
            ) {
                ItemIcon(item.iconId, Modifier.fillMaxSize(), item.nameCn.take(2))
            }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    item.nameCn, color = PhoneText, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    buildList {
                        item.kindName.takeIf { it.isNotBlank() }?.let(::add)
                        if (item.itemLevel > 0) add("品级 ${item.itemLevel}")
                    }.joinToString(" · "),
                    color = PhoneMuted, fontSize = 11.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            ImageGlyph(R.drawable.ic2_chevron_right, PhoneMuted, Modifier.size(15.dp))
        }
    }
    PhoneHairlineRow(margin.dp + 48.dp)
}

@Composable
internal fun MarketEmpty(text: String) {
    Column(
        Modifier.fillMaxSize().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ImageGlyph(
            R.drawable.ic2_empty_box, PhoneMuted.copy(alpha = 0.5f), Modifier.size(34.dp),
        )
        Text(
            text, color = PhoneMuted, fontSize = 12.sp,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

/**
 * iOS-style segmented control: one recessed track, the active segment a raised
 * white pill. This is the native idiom for "pick exactly one of a few" -- a row of
 * separate outlined pills reads as multi-select and looks like Material chips.
 *
 * Use [MarketChip] only for genuinely independent toggles.
 */
@Composable
internal fun MarketSegmented(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(PhoneSurfaceRaised)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        labels.forEachIndexed { i, label ->
            val on = i == selectedIndex
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .then(if (on) Modifier.background(PhoneSurface) else Modifier)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) PhoneText else PhoneMuted,
                    fontSize = 12.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Independent toggle (HQ filter, world filter). Tinted when on. */
@Composable
internal fun MarketChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .height(30.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.background(PhoneAccent.copy(alpha = 0.14f))
                else Modifier.background(PhoneSurfaceRaised)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) PhoneAccent else PhoneMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

/** gil formatting: 1,234,567 -- market numbers get large and need grouping. */
internal fun gil(v: Int): String {
    val s = v.toString()
    if (s.length <= 3) return s
    return s.reversed().chunked(3).joinToString(",").reversed()
}

internal fun gil(v: Double): String = gil(v.toInt())

/** "3 分钟前" / "5 小时前" / "2 天前" -- staleness matters a lot here. */
internal fun ago(ms: Long): String {
    if (ms <= 0) return "未知"
    val d = System.currentTimeMillis() - ms
    return when {
        d < 60_000 -> "刚刚"
        d < 3600_000 -> "${d / 60_000} 分钟前"
        d < 86400_000 -> "${d / 3600_000} 小时前"
        else -> "${d / 86400_000} 天前"
    }
}

/**
 * Market categories screen - the game's own market board grid, carried over:
 * tiles use the exact ItemSearchCategory icon the in-game board shows, laid out
 * several columns wide the way the user asked for.
 *
 * Works offline too: the tree is cached on disk after the first successful sync,
 * and the connection effect below only tops it up.
 */
@Composable
private fun MarketCategoriesScreen(
    state: PhoneState,
    onBack: () -> Unit,
    onOpen: (MarketDest) -> Unit,
) {
    val margin = LocalContentMargin.current

    // Re-request whenever the connection state flips; rate-limited in PhoneState.
    LaunchedEffect(state.connected) {
        if (state.connected) state.requestMarketCategories()
    }
    // An old plugin silently drops opcode 15 -- say so instead of spinning forever.
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(state.connected) {
        timedOut = false
        if (state.connected) {
            kotlinx.coroutines.delay(12_000)
            if (state.marketCategories == null) timedOut = true
        }
    }

    ScreenFrame {
        ScreenHeader("市场分类", state, showBack = true, onBack = onBack)

        when (val categories = state.marketCategories) {
            null -> when {
                timedOut && state.connected -> Column(
                    Modifier.fillMaxSize().padding(margin.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "插件没有回应分类请求",
                        color = PhoneText, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        "电脑上的插件可能是旧版本，装一次新插件包即可。",
                        color = PhoneMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        "重试", color = PhoneAccent, fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PhoneSurfaceRaised)
                            .clickable {
                                timedOut = false
                                state.invalidateMarketCategories()
                                state.requestMarketCategories()
                            }
                            .padding(horizontal = 18.dp, vertical = 8.dp),
                    )
                }
                state.connected -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = PhoneAccent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(26.dp),
                    )
                }
                else -> MarketEmpty(
                    "连接电脑上的插件后，第一次打开会缓存分类\n之后离线也能浏览",
                )
            }
            else -> Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = margin.dp, vertical = 12.dp),
            ) {
                MarketCategoryGridContent(categories.categories) {
                    onOpen(MarketDest.Category(it))
                }
            }
        }
    }
}

/**
 * The category grid itself, reusable: it lives under the watchlist (its requested
 * home) and as the categories screen body. Requests the tree from the plugin on
 * first composition; tiles show the in-game board art.
 */
@Composable
internal fun MarketCategoryGridSection(
    state: PhoneState,
    onOpen: (MarketDest) -> Unit,
) {
    val margin = LocalContentMargin.current
    // Re-request whenever the connection state flips: the original version fired
    // exactly once on first composition, so opening this screen before the socket
    // was up meant an eternal spinner with no retry.
    LaunchedEffect(state.connected) {
        if (state.connected) state.requestMarketCategories()
    }
    // An old plugin silently drops opcode 15. Say so instead of spinning forever.
    var timedOut by remember { mutableStateOf(false) }
    LaunchedEffect(state.connected) {
        timedOut = false
        if (state.connected) {
            kotlinx.coroutines.delay(12_000)
            if (state.marketCategories == null) timedOut = true
        }
    }

    val categories = state.marketCategories?.categories
    val perRow = 3
    val rows = categories?.chunked(perRow) ?: emptyList()

    Text(
        "按分类浏览", color = PhoneMuted, fontSize = 11.sp,
        fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp,
        modifier = Modifier.padding(
            start = margin.dp + 4.dp, end = margin.dp, top = 24.dp, bottom = 8.dp,
        ),
    )
    when {
        categories == null && timedOut -> Row(
            Modifier.fillMaxWidth().padding(horizontal = margin.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "插件没有回应分类请求。电脑上的插件可能是旧版本，装一次新插件包即可。",
                color = PhoneMuted, fontSize = 11.sp, lineHeight = 16.sp,
                modifier = Modifier.weight(1f),
            )
            Text(
                "重试", color = PhoneAccent, fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp)
                    .clickable {
                        timedOut = false
                        state.invalidateMarketCategories()
                        state.requestMarketCategories()
                    },
            )
        }
        categories == null -> Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(
                color = PhoneAccent, strokeWidth = 2.dp, modifier = Modifier.size(22.dp),
            )
        }
        categories.isEmpty() -> Text(
            "连接游戏后可以按游戏内分类浏览道具",
            color = PhoneMuted, fontSize = 11.sp,
            modifier = Modifier.padding(start = margin.dp),
        )
        else -> MarketCategoryGridContent(categories) { onOpen(MarketDest.Category(it)) }
    }
}

/**
 * The category grid itself, grouped the way the game board groups them: a header
 * per top-level group (武器 / 主工具 / 防具 / ...), and the group's subcategories
 * as tiles under it. Non-scrolling: embed inside a scrollable container.
 */
@Composable
internal fun MarketCategoryGridContent(
    categories: List<GameMarketCategory>,
    onOpenSub: (Int) -> Unit,
) {
    categories.sortedBy { it.order }.forEach { group ->
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (group.iconId > 0) {
                ItemIcon(group.iconId, Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                group.name, color = PhoneAccent, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "  ${group.items.size} 件", color = PhoneMuted, fontSize = 10.sp,
            )
        }
        val subs = group.subcategories.sortedBy { it.order }
        subs.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { sub ->
                    CategoryTile(
                        sub, Modifier.weight(1f),
                        onClick = { onOpenSub(sub.id) },
                    )
                }
                // Keep tiles equal-width on the ragged last row.
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
internal fun CategoryTile(
    sub: GameMarketSubcategory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PhoneCard(modifier = modifier, onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
                    .background(PhoneSurface),
                contentAlignment = Alignment.Center,
            ) {
                // Same icon id the in-game board renders, via the game-icon CDN.
                ItemIcon(sub.iconId, Modifier.size(42.dp), sub.name.take(1))
            }
            Text(
                sub.name, color = PhoneText, fontSize = 11.sp,
                fontWeight = FontWeight.Medium, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
            )
            Text(
                "${sub.items.size} 件", color = PhoneMuted, fontSize = 9.sp,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

/**
 * Market category detail screen - every item in one search subcategory, with the
 * game's item icon, gear level and HQ badge, so it reads like the market board
 * list rather than bare text. Resolves the subcategory across all top-level
 * groups, since tiles navigate by subcategory id.
 */
@Composable
private fun MarketCategoryScreen(
    state: PhoneState,
    categoryId: Int,
    onBack: () -> Unit,
    onOpen: (MarketDest) -> Unit,
) {
    val margin = LocalContentMargin.current
    val ctx = LocalContext.current
    val found = state.marketCategories?.categories
        ?.firstOrNull { c -> c.subcategories.any { it.id == categoryId } }
        ?.let { group -> group to group.subcategories.first { it.id == categoryId } }
    val sub = found?.second

    // Get saved state for this category
    val categoryState = remember(categoryId) { state.getMarketCategoryState(categoryId) }

    // Equip level filter state - restored from PhoneState
    var showFilter by remember(categoryId) { mutableStateOf(categoryState.showFilter) }
    var minLevel by remember(categoryId) { mutableStateOf(categoryState.minLevel) }
    var maxLevel by remember(categoryId) { mutableStateOf(categoryState.maxLevel) }
    var sortDescending by remember(categoryId) { mutableStateOf(categoryState.sortDescending) }

    // Save state changes back to PhoneState
    LaunchedEffect(showFilter, minLevel, maxLevel, sortDescending) {
        categoryState.showFilter = showFilter
        categoryState.minLevel = minLevel
        categoryState.maxLevel = maxLevel
        categoryState.sortDescending = sortDescending
    }

    // Fetch equip levels and item levels from WikiDb asynchronously
    var itemEquipLevels by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    var itemLevels by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }

    LaunchedEffect(sub) {
        if (sub != null) {
            val equipLevels = mutableMapOf<Int, Int>()
            val levels = mutableMapOf<Int, Int>()
            for (item in sub.items) {
                val wikiItem = WikiDb.byId(ctx, item.id)
                if (wikiItem != null) {
                    if (wikiItem.equipLevel > 0) {
                        equipLevels[item.id] = wikiItem.equipLevel
                    }
                    if (wikiItem.itemLevel > 0) {
                        levels[item.id] = wikiItem.itemLevel
                    }
                }
            }
            itemEquipLevels = equipLevels
            itemLevels = levels
        }
    }

    // Check if any item has equip level > 0 (this category contains gear)
    val hasLevels = itemEquipLevels.isNotEmpty()
    val levelRange = if (hasLevels) {
        val levels = itemEquipLevels.values
        levels.minOrNull()!! to levels.maxOrNull()!!
    } else {
        1 to 100
    }

    // Apply filter and sort
    val filteredItems = sub?.items?.let { items ->
        items.filter { item ->
            if (!hasLevels || !showFilter) true
            else {
                val equipLevel = itemEquipLevels[item.id] ?: 0
                equipLevel in minLevel..maxLevel
            }
        }.sortedWith(
            if (sortDescending) {
                compareByDescending<GameMarketItem> { itemLevels[it.id] ?: it.levelItem }.thenBy { it.id }
            } else {
                compareBy<GameMarketItem> { itemLevels[it.id] ?: it.levelItem }.thenBy { it.id }
            }
        )
    } ?: emptyList()

    ScreenFrame {
        ScreenHeader(sub?.name ?: "分类", state, showBack = true, onBack = onBack)

        if (sub == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未找到分类", color = PhoneMuted, fontSize = 13.sp)
            }
        } else {
            // Remember scroll state for this category
            val listState = rememberLazyListState(
                initialFirstVisibleItemIndex = categoryState.scrollIndex,
                initialFirstVisibleItemScrollOffset = categoryState.scrollOffset,
            )

            // Save scroll position when it changes
            LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                categoryState.scrollIndex = listState.firstVisibleItemIndex
                categoryState.scrollOffset = listState.firstVisibleItemScrollOffset
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            ) {
                // Filter controls (only show for gear categories)
                if (hasLevels) {
                    item {
                        Column(Modifier.padding(horizontal = margin.dp, vertical = 8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                MarketChip(
                                    label = if (showFilter) "筛选：$minLevel-$maxLevel" else "等级筛选",
                                    selected = showFilter,
                                    onClick = { showFilter = !showFilter },
                                    modifier = Modifier.weight(1f),
                                )
                                MarketChip(
                                    label = if (sortDescending) "品级 ↓" else "品级 ↑",
                                    selected = false,
                                    onClick = { sortDescending = !sortDescending },
                                )
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = showFilter,
                                enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
                                exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut(),
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PhoneSurfaceRaised)
                                        .padding(16.dp),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "装备等级",
                                            color = PhoneText,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                        Text(
                                            "$minLevel - $maxLevel",
                                            color = PhoneAccent,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }

                                    androidx.compose.material3.RangeSlider(
                                        value = minLevel.toFloat()..maxLevel.toFloat(),
                                        onValueChange = { range ->
                                            minLevel = range.start.toInt()
                                            maxLevel = range.endInclusive.toInt()
                                        },
                                        valueRange = levelRange.first.toFloat()..levelRange.second.toFloat(),
                                        steps = (levelRange.second - levelRange.first - 1).coerceAtLeast(0),
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    )

                                    Row(
                                        Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            "${levelRange.first}",
                                            color = PhoneMuted,
                                            fontSize = 11.sp,
                                        )
                                        Text(
                                            "${levelRange.second}",
                                            color = PhoneMuted,
                                            fontSize = 11.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        "共 ${filteredItems.size} 件${if (showFilter && hasLevels) "（已筛选）" else ""}",
                        color = PhoneMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(
                            start = margin.dp,
                            end = margin.dp,
                            top = if (hasLevels) 0.dp else 0.dp,
                            bottom = 4.dp,
                        ),
                    )
                }
                lazyItems(filteredItems, key = { it.id }) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpen(MarketDest.Item(item.id)) }
                            .padding(horizontal = margin.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(7.dp))
                                .background(PhoneSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            ItemIcon(item.iconId, Modifier.fillMaxSize(), item.name.take(2))
                        }
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(
                                item.name, color = PhoneText, fontSize = 14.sp,
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                buildList {
                                    val correctItemLevel = itemLevels[item.id] ?: item.levelItem
                                    if (correctItemLevel > 0) add("品级 $correctItemLevel")
                                    if (item.canBeHq) add("可 HQ")
                                }.joinToString(" · "),
                                color = PhoneMuted, fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        ImageGlyph(R.drawable.ic2_chevron_right, PhoneMuted, Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}
