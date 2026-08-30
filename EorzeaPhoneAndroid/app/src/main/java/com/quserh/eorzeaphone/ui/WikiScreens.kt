package com.quserh.eorzeaphone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.ItemIconLoader
import com.quserh.eorzeaphone.data.wiki.DutyDb
import com.quserh.eorzeaphone.data.wiki.WikiDb
import com.quserh.eorzeaphone.data.wiki.WikiDuty
import com.quserh.eorzeaphone.data.wiki.WikiHit
import com.quserh.eorzeaphone.data.wiki.WikiSearch
import com.quserh.eorzeaphone.data.wiki.WikiSearchResult
import com.quserh.eorzeaphone.data.wiki.WikiDetail
import com.quserh.eorzeaphone.data.wiki.WikiDicts
import com.quserh.eorzeaphone.data.wiki.WikiFilter
import com.quserh.eorzeaphone.data.wiki.WikiIconCache
import com.quserh.eorzeaphone.data.wiki.WikiItem
import com.quserh.eorzeaphone.data.wiki.WikiLinkKind
import com.quserh.eorzeaphone.data.wiki.WikiNode
import com.quserh.eorzeaphone.data.wiki.WikiRemote
import com.quserh.eorzeaphone.data.wiki.WikiSourceEntry
import com.quserh.eorzeaphone.ui.theme.BrandFill
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneInfo
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.PhoneWarn
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 灰机 Wiki 物品检索。数据全部来自随包内置的 SQLite（51120 件），**离线可用**。
 *
 * 图标走 [ItemIconLoader]（xivapi），和物品栏/收藏馆同一条缓存链路。
 * 排序与站点 Module:Item/ItemSearch 对齐：品级↓ 版本↓ ID↑，每页 60 条。
 */
/**
 * 一个可跳转的目标。
 *
 * 用栈装起来就能像 wiki 那样任意链式点击：
 * 物品 → 它的获取任务 → 任务奖励里的另一件物品 → 那件的商店 → 商店里第三件物品……
 * 返回键逐层弹出，不会一步掉回搜索页。
 */
internal sealed interface WikiDest {
    data class Item(val id: Int) : WikiDest
    data class Quest(val id: Int) : WikiDest
    data class Instance(val id: Int) : WikiDest
    data class Shop(val id: Int) : WikiDest
    data class Node(val id: Int) : WikiDest

    /** 一个块的任务流程图。[highlightId] = 搜索命中的那个任务。 */
    data class QuestTree(val chainId: Int, val highlightId: Int) : WikiDest

    /** 一个任务的**完整前置线**（跨块、传递闭包）。 */
    data class Ancestry(val questId: Int) : WikiDest

    /** 物品检索（带九项筛选的那个列表）。 */
    data object ItemBrowse : WikiDest

    /** 任务检索（按任务链浏览）。 */
    data object QuestBrowse : WikiDest

    /** 副本检索（按类型浏览：讨伐歼灭战 / 迷宫挑战 / 大型任务 …）。 */
    data object DutyBrowse : WikiDest
}

/**
 * 一棵树的浏览状态（缩放、平移、点选的节点）。
 *
 * 为什么要提到 [WikiScreen] 这一层：导航栈是手写的 `mutableStateListOf`，
 * 进详情页时树的 composable 被整个丢掉，`remember` 里的缩放/平移跟着没了。
 * 真机上的表现是「捏到 87% → 看详情 → 返回 → 弹回 125% 初始视图」。
 * 主线 159 层，翻到一半去看个详情再回来被弹回原点，很难用。
 *
 * 按 chainId 存，所以同时逛几棵树各自记得自己的位置。
 */
internal class QuestTreeView {
    /** 0 = 还没定初值，交给画布按宽度铺满并对准高亮节点。 */
    var scale: Float = 0f
    var panX: Float = 0f
    var panY: Float = 0f
    var picked: Int = 0
}

/**
 * 检索页自己的状态：当前模式、两个模式各自的搜索词、物品筛选条件。
 *
 * 和 [QuestTreeView] 同一个原因 —— 栈上有目标页时检索页被丢掉，
 * 返回时 `remember` 全部重置。真机上的表现是
 * 「切到任务 → 搜 → 开流程图 → 返回 → 回到物品模式，搜索词也没了」，
 * 而这正是主要用法，每次都要重选模式重打字。
 *
 * 两个模式的搜索词分开存：「铜锭」和「讨伐孟菲斯」不是同一次查找，
 * 串在一起反而要重打。
 */
internal class WikiSearchState {
    /** 首页那个统一搜索框的内容。物品和任务一起搜，不再分模式。 */
    var input: String = ""

    /** 物品检索页自己的筛选条件（那一页有九项筛选，和首页搜索是两回事）。 */
    var browseInput: String = ""
    var filter: WikiFilter = WikiFilter()
}

@Composable
fun WikiScreen(state: PhoneState) {
    // 导航栈。空 = 在搜索页。
    val stack = remember { mutableStateListOf<WikiDest>() }
    // 每棵树的缩放/平移，跨导航保留。见 QuestTreeView。
    val treeViews = remember { mutableMapOf<Int, QuestTreeView>() }
    // 检索页的模式/搜索词/筛选，跨导航保留。见 WikiSearchState。
    val searchState = remember { WikiSearchState() }
    // 返回键逐层弹出，照 ClockAndTimersScreens 的写法（它和聊天处理了这件事）。
    BackHandler(enabled = stack.isNotEmpty()) { stack.removeLastOrNull() }

    val top = stack.lastOrNull()
    when (top) {
        null -> WikiSearchScreen(state, searchState) { stack.add(it) }
        is WikiDest.Item -> WikiItemDetailRoute(
            state, top.id,
            onBack = { stack.removeLastOrNull() },
            onOpen = { stack.add(it) },
        )
        is WikiDest.Quest -> WikiQuestScreen(
            state, top.id,
            onBack = { stack.removeLastOrNull() },
            onOpen = { stack.add(it) },
        )
        is WikiDest.Instance -> WikiInstanceScreen(
            state, top.id,
            onBack = { stack.removeLastOrNull() },
            onOpen = { stack.add(it) },
        )
        is WikiDest.Shop -> WikiShopScreen(
            state, top.id,
            onBack = { stack.removeLastOrNull() },
            onOpen = { stack.add(it) },
        )
        is WikiDest.Node -> WikiNodeScreen(
            state, top.id, onBack = { stack.removeLastOrNull() },
        )
        is WikiDest.QuestTree -> QuestTreeScreen(
            state, top.chainId, top.highlightId,
            view = treeViews.getOrPut(top.chainId) { QuestTreeView() },
            onBack = { stack.removeLastOrNull() },
            onOpen = { stack.add(it) },
        )
        is WikiDest.Ancestry -> QuestAncestryScreen(
            state, top.questId,
            view = treeViews.getOrPut(-top.questId) { QuestTreeView() },
            onBack = { stack.removeLastOrNull() },
            onOpen = { stack.add(it) },
        )
        WikiDest.ItemBrowse -> WikiItemSearchScreen(
            state = state,
            saved = searchState,
            onBack = { stack.removeLastOrNull() },
            onOpen = { stack.add(WikiDest.Item(it.id)) },
        )
        WikiDest.QuestBrowse -> WikiQuestBrowseScreen(
            state = state,
            onBack = { stack.removeLastOrNull() },
            onOpenTree = { cid, hit -> stack.add(WikiDest.QuestTree(cid, hit)) },
        )
        WikiDest.DutyBrowse -> WikiDutyBrowseScreen(
            state = state,
            onBack = { stack.removeLastOrNull() },
            onOpen = { stack.add(it) },
        )
    }
}

/** 物品详情的路由层：按 ID 从本地库取物品，再交给详情屏。 */
@Composable
private fun WikiItemDetailRoute(
    state: PhoneState,
    id: Int,
    onBack: () -> Unit,
    onOpen: (WikiDest) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var item by remember(id) { mutableStateOf<WikiItem?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    LaunchedEffect(id) {
        item = runCatching { WikiDb.byId(context, id) }.getOrNull()
        loading = false
    }
    val it2 = item
    when {
        loading -> WikiLoadingScreen("物品", state, onBack)
        it2 == null -> WikiMissingScreen("物品 $id", state, onBack)
        else -> WikiDetailScreen(state, it2, onBack, onOpen)
    }
}

/**
 * WiKi 首页。
 *
 * 一进来**不**列 51120 件物品 —— 那是一屏噪声，而且首屏就要跑一次全表查询。
 * 改成：搜索框 + 几个功能入口，点了才进对应的列表。
 *
 * 搜索框是**统一**的：物品和任务一起搜，结果合并、标类型，不用先选模式。
 * 再加一路站点全文检索兜底（见 [WikiSearch]），黑话也能搜到。
 */
@Composable
private fun WikiSearchScreen(
    state: PhoneState,
    saved: WikiSearchState,
    onOpen: (WikiDest) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var input by remember { mutableStateOf(saved.input) }
    var query by remember { mutableStateOf(saved.input) }

    LaunchedEffect(Unit) {
        snapshotFlow { input }
            .debounce(300)
            .distinctUntilChanged()
            .collect { query = it; saved.input = it }
    }

    ScreenFrame {
        ScreenHeader("WiKi", state, showBack = true)
        WikiSearchField(
            input,
            onChange = { input = it },
            onSubmit = { keyboard?.hide() },
            hint = "搜物品、任务、副本、或任何 wiki 条目",
        )
        if (query.isBlank()) {
            WikiHome(onOpen = onOpen)
        } else {
            WikiUnifiedResults(query = query, onOpen = onOpen)
        }
    }
}

/**
 * 首页的功能入口。点了才进列表，不在首页就把库倒出来。
 *
 * 两个入口做成**并排两格**，不是上下两条通栏。通栏那样一屏只有两根横条 +
 * 大片空白，看着像没做完；并排之后宽度被用掉，两个入口也是并列关系
 * （物品 / 任务），横排比竖排更能说明这一点。
 *
 * 每格把**库里的数量当主角**（51,120 / 5,360）—— 那是这个模块真正的分量所在，
 * 而且是真数字不是装饰。原来数量混在一行 11sp 说明文字里，看不见。
 */
@Composable
private fun WikiHome(onOpen: (WikiDest) -> Unit) {
    val context = LocalContext.current.applicationContext
    val margin = LocalContentMargin.current

    // 副本数从 meta 表读，不写死。物品/任务那两个数字是稳定的，副本这张表是
    // 新加的，发版时抓到多少条要如实显示 —— 写死一个数就等于给自己埋个
    // 「界面说 427、库里其实 327」的坑。
    var dutyCount by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        dutyCount = runCatching {
            WikiDb.meta(context)["duty_count"]?.takeIf { it.isNotBlank() } ?: ""
        }.getOrDefault("")
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = margin.dp, end = margin.dp, top = 12.dp, bottom = 20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WikiEntryTile(
                icon = R.drawable.ic2_search,
                count = "51,120",
                title = "物品检索",
                hint = "九项筛选",
                modifier = Modifier.weight(1f),
                onClick = { onOpen(WikiDest.ItemBrowse) },
            )
            WikiEntryTile(
                // 没有 ic_list；ic_swords 在这个项目里表示战斗/任务，和站点的任务图标语义一致
                icon = R.drawable.ic2_swords,
                count = "5,360",
                title = "任务检索",
                hint = "468 条任务链",
                modifier = Modifier.weight(1f),
                onClick = { onOpen(WikiDest.QuestBrowse) },
            )
        }
        // 副本单独一行、占满宽度：它和上面两个是并列关系，但只有一格，
        // 硬塞进上面那行会把三格挤到装不下 22sp 的数字。
        if (dutyCount.isNotBlank()) {
            Row(
                Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WikiEntryTile(
                    // 盾 = 小队构成，和 swords（任务）区分开
                    icon = R.drawable.ic2_shield,
                    count = dutyCount,
                    title = "副本检索",
                    hint = "讨伐歼灭战、迷宫挑战、含掉落",
                    modifier = Modifier.weight(1f),
                    onClick = { onOpen(WikiDest.DutyBrowse) },
                )
            }
        }
        // 原来这儿是一段两行的说明，读起来像文档不像界面。
        // 搜索框就在上面、点了就能用，不需要一段话解释它存在。
        // 只留两句**看不出来**的行为：离线可用、以及搜不到会怎样。
        Text(
            "全库随包内置，离线可用。本地搜不到的说法，会再问一次 wiki 全站。",
            color = PhoneMuted,
            fontSize = 11.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 14.dp),
        )
    }
}

@Composable
private fun WikiEntryTile(
    icon: Int,
    count: String,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    PhoneCard(modifier = modifier, onClick = onClick) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).clip(RoundedCornerShape(9.dp))
                        .background(BrandFill.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    ImageGlyph(icon, PhoneAccent, Modifier.size(16.dp))
                }
                Spacer(Modifier.weight(1f))
                ImageGlyph(R.drawable.ic2_chevron_right, PhoneMuted, Modifier.size(14.dp))
            }
            Text(
                count,
                color = PhoneText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(top = 12.dp),
            )
            // 数字的单位就是下面这行标题（「51,120 / 物品检索」）。
            // 原来中间还有一行「件物品」，于是一格里「物品」出现两次。
            Text(
                title,
                color = PhoneText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
            Text(
                hint,
                color = PhoneMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * 统一检索结果：物品 + 任务 + 副本 + 站点条目，合并成一个列表，每条标类型。
 *
 * 顺序是副本 → 物品 → 任务 → 站点条目。前三类是本地库（快、结构化、能继续点），
 * 站点条目是线上兜底（覆盖黑话、怪物、地名、攻略页那些我没有表的）。
 *
 * 副本排在最前是故意的：用户搜「歼灭战」「零式」这种词时要的就是副本，
 * 而这些词也会命中一堆同名装备（「伊弗利特之角」之类），物品数量压过副本。
 */
@Composable
private fun WikiUnifiedResults(query: String, onOpen: (WikiDest) -> Unit) {
    val context = LocalContext.current.applicationContext
    var res by remember { mutableStateOf<WikiSearchResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    val margin = LocalContentMargin.current

    LaunchedEffect(query) {
        loading = true
        res = runCatching { WikiSearch.search(context, query) }.getOrNull()
        loading = false
    }

    val r = res
    when {
        loading && r == null -> Box(
            Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = PhoneAccent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp),
            )
        }
        r == null || r.isEmpty -> Column(
            Modifier.fillMaxSize().padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ImageGlyph(
                R.drawable.ic2_empty_box, PhoneMuted.copy(alpha = 0.5f), Modifier.size(34.dp),
            )
            Text(
                "没有找到「$query」", color = PhoneMuted, fontSize = 12.sp,
                modifier = Modifier.padding(top = 11.dp),
            )
            if (r != null && !r.onlineOk) {
                Text(
                    "站内全文检索没连上，只搜了本地",
                    color = PhoneMuted, fontSize = 10.sp,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
        }
        else -> LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 20.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(start = margin.dp, end = margin.dp, top = 8.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "共 ${r.total} 条" +
                            listOfNotNull(
                                r.duties.size.takeIf { it > 0 }?.let { "副本 $it" },
                                r.items.size.takeIf { it > 0 }?.let { "物品 $it" },
                                r.quests.size.takeIf { it > 0 }?.let { "任务 $it" },
                                r.pages.size.takeIf { it > 0 }?.let { "条目 $it" },
                            ).joinToString("、", prefix = "  "),
                        color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.weight(1f),
                    )
                    if (!r.onlineOk) {
                        Text("仅本地", color = PhoneWarn, fontSize = 10.sp)
                    }
                }
            }

            lazyItems(r.duties, key = { "d${it.duty.id}" }) { h ->
                WikiHitRow(
                    // 类型标签直接用副本自己的类型（讨伐歼灭战/迷宫挑战/…），
                    // 比统一写「副本」信息量大，用户找的就是这个词
                    kind = h.duty.type.ifBlank { "副本" }, kindTint = PhoneWarn,
                    iconId = h.duty.imageId, iconHash = "",
                    title = h.duty.name,
                    sub = buildList {
                        h.duty.levelText.takeIf { it.isNotBlank() }?.let(::add)
                        h.duty.sizeText.takeIf { it.isNotBlank() }?.let(::add)
                        (h.duty.place.takeIf { it.isNotBlank() } ?: h.duty.mapPlace)
                            .takeIf { it.isNotBlank() }?.let(::add)
                        // 经 BOSS 名命中时说明理由，否则用户不知道这条为什么出现
                        h.viaBoss?.let { add("BOSS: $it") }
                    }.joinToString(" · "),
                    onClick = { onOpen(WikiDest.Instance(h.duty.id)) },
                )
            }
            lazyItems(r.items, key = { "i${it.item.id}" }) { h ->
                WikiHitRow(
                    kind = "物品", kindTint = PhoneAccent,
                    iconId = h.item.iconId, iconHash = h.item.iconHash,
                    title = h.item.nameCn,
                    sub = buildItemSubtitle(h.item),
                    onClick = { onOpen(WikiDest.Item(h.item.id)) },
                )
            }
            lazyItems(r.quests, key = { "q${it.hit.id}" }) { h ->
                WikiHitRow(
                    kind = "任务", kindTint = PhoneInfo,
                    iconId = h.hit.iconId, iconHash = "",
                    title = h.label,
                    sub = buildList {
                        if (h.hit.level > 0) add("等级 ${h.hit.level}")
                        h.hit.type.takeIf { it.isNotBlank() }?.let(::add)
                        add(h.hit.chainTitle)
                    }.joinToString(" · "),
                    onClick = { onOpen(WikiDest.Quest(h.hit.id)) },
                )
            }
            if (r.pages.isNotEmpty()) {
                item {
                    Text(
                        "wiki 条目",
                        color = PhoneMuted, fontSize = 10.sp,
                        modifier = Modifier.padding(
                            start = margin.dp, end = margin.dp, top = 14.dp, bottom = 2.dp,
                        ),
                    )
                }
                lazyItems(r.pages, key = { "p${it.pageId}" }) { p ->
                    WikiPageRow(p)
                }
            }
        }
    }
}

/** 结果行。左边类型标签，右边名字 + 副标题。 */
@Composable
private fun WikiHitRow(
    kind: String,
    kindTint: Color,
    iconId: Int,
    iconHash: String,
    title: String,
    sub: String,
    onClick: () -> Unit,
) {
    val margin = LocalContentMargin.current
    PhonePressable(onClick = onClick, shape = RoundedCornerShape(0.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick)
                .padding(horizontal = margin.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconId > 0) {
                WikiIcon(iconId, iconHash, title.take(1), Modifier.size(34.dp))
            } else {
                Box(Modifier.size(34.dp))
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        kind, color = kindTint, fontSize = 9.sp,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                            .background(kindTint.copy(alpha = 0.14f))
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                    Text(
                        title, color = PhoneText, fontSize = 14.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp).weight(1f),
                    )
                }
                if (sub.isNotBlank()) {
                    Text(
                        sub, color = PhoneMuted, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
    PhoneHairlineRow(18.dp)
}

/** 站点条目行。带匹配片段，点了在浏览器打开（我本地没有这类条目的数据）。 */
@Composable
private fun WikiPageRow(p: WikiHit.Page) {
    val context = LocalContext.current
    val margin = LocalContentMargin.current
    Column(
        Modifier.fillMaxWidth().clickable {
            runCatching {
                context.startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(WikiSearch.pageUrl(p.title)),
                    )
                )
            }
        }.padding(horizontal = margin.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "条目", color = PhoneMuted, fontSize = 9.sp,
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(PhoneMuted.copy(alpha = 0.14f))
                    .padding(horizontal = 5.dp, vertical = 1.dp),
            )
            Text(
                p.title, color = PhoneText, fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp).weight(1f),
            )
            Text("↗", color = PhoneMuted, fontSize = 12.sp)
        }
        // 「经『A12』命中」—— 让用户知道为什么这条会出现
        if (p.viaAlias != null) {
            Text(
                "经「${p.viaAlias}」命中",
                color = PhoneAccent, fontSize = 10.sp,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        if (p.snippet.isNotBlank()) {
            Text(
                p.snippet, color = PhoneMuted, fontSize = 11.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
    PhoneHairlineRow(18.dp)
}

/**
 * 任务检索页：只搜任务，按块分组。
 *
 * 首页那个搜索框是物品+任务+全文一起搜的；这一页是「我就想翻任务」时用的，
 * 结果按任务链整块给，和站点的分类浏览对应。
 */
@OptIn(FlowPreview::class)
@Composable
private fun WikiQuestBrowseScreen(
    state: PhoneState,
    onBack: () -> Unit,
    onOpenTree: (chainId: Int, highlightId: Int) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var input by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        snapshotFlow { input }
            .debounce(260)
            .distinctUntilChanged()
            .collect { query = it }
    }

    ScreenFrame {
        ScreenHeader("任务检索", state, onBack = onBack)
        WikiSearchField(
            input,
            onChange = { input = it },
            onSubmit = { keyboard?.hide() },
            hint = "任务名或任务链名",
        )
        QuestSearchResults(query = query, onOpenTree = onOpenTree)
    }
}

/**
 * 副本检索。搜索框 + 类型分组。
 *
 * 空词时列类型（讨伐歼灭战 118、迷宫挑战 104、…），点类型进该类型的全部副本；
 * 有词时直接出命中的副本。427 个副本全在本地库，离线可用。
 */
@OptIn(FlowPreview::class)
@Composable
private fun WikiDutyBrowseScreen(
    state: PhoneState,
    onBack: () -> Unit,
    onOpen: (WikiDest) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val keyboard = LocalSoftwareKeyboardController.current
    val margin = LocalContentMargin.current

    var input by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var types by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var picked by remember { mutableStateOf("") }
    var list by remember { mutableStateOf<List<WikiDuty>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        snapshotFlow { input }
            .debounce(260)
            .distinctUntilChanged()
            .collect { query = it }
    }

    LaunchedEffect(Unit) {
        types = runCatching { DutyDb.types(context) }.getOrDefault(emptyList())
        loading = false
    }

    // 搜索词优先；没词时看选中的类型
    LaunchedEffect(query, picked) {
        loading = true
        list = runCatching {
            when {
                query.isNotBlank() -> DutyDb.search(context, query)
                picked.isNotBlank() -> DutyDb.byType(context, picked)
                else -> emptyList()
            }
        }.getOrDefault(emptyList())
        loading = false
    }

    ScreenFrame {
        ScreenHeader(
            if (picked.isNotBlank() && query.isBlank()) picked else "副本检索",
            state,
            onBack = {
                // 在某个类型里时，返回先退回类型列表，再退出这一页
                if (picked.isNotBlank() && query.isBlank()) picked = "" else onBack()
            },
        )
        WikiSearchField(
            input,
            onChange = { input = it },
            onSubmit = { keyboard?.hide() },
            hint = "副本名、类型、或 BOSS 名",
        )
        when {
            loading && list.isEmpty() && types.isEmpty() -> Box(
                Modifier.fillMaxSize(), contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = PhoneAccent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp),
                )
            }
            // 类型列表
            query.isBlank() && picked.isBlank() -> {
                if (types.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ImageGlyph(
                            R.drawable.ic_empty_box, PhoneMuted.copy(alpha = 0.5f),
                            Modifier.size(34.dp),
                        )
                        Text(
                            "本地库里还没有副本数据",
                            color = PhoneMuted, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 11.dp),
                        )
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 20.dp),
                    ) {
                        lazyItems(types, key = { it.first }) { (t, n) ->
                            Row(
                                Modifier.fillMaxWidth().clickable { picked = t }
                                    .padding(horizontal = margin.dp, vertical = 11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    t, color = PhoneText, fontSize = 14.sp,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("$n", color = PhoneMuted, fontSize = 12.sp)
                                ImageGlyph(
                                    R.drawable.ic_chevron_right, PhoneMuted,
                                    Modifier.size(15.dp),
                                )
                            }
                            PhoneHairlineRow(margin.dp)
                        }
                    }
                }
            }
            list.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ImageGlyph(
                    R.drawable.ic_empty_box, PhoneMuted.copy(alpha = 0.5f), Modifier.size(34.dp),
                )
                Text(
                    if (query.isBlank()) "这个类型下没有副本" else "没有找到「$query」",
                    color = PhoneMuted, fontSize = 12.sp,
                    modifier = Modifier.padding(top = 11.dp),
                )
            }
            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                item {
                    Text(
                        "共 ${list.size} 个副本",
                        color = PhoneMuted, fontSize = 11.sp,
                        modifier = Modifier.padding(
                            start = margin.dp, end = margin.dp, top = 8.dp, bottom = 2.dp,
                        ),
                    )
                }
                lazyItems(list, key = { it.id }) { d ->
                    WikiHitRow(
                        kind = d.levelText.ifBlank { "副本" }, kindTint = PhoneWarn,
                        iconId = d.imageId, iconHash = "",
                        title = d.name,
                        sub = buildList {
                            // 在某个类型里时不必每行重复类型名
                            if (query.isNotBlank()) {
                                d.type.takeIf { it.isNotBlank() }?.let(::add)
                            }
                            d.sizeText.takeIf { it.isNotBlank() }?.let(::add)
                            (d.place.takeIf { it.isNotBlank() } ?: d.mapPlace)
                                .takeIf { it.isNotBlank() }?.let(::add)
                            d.bosses.firstOrNull()?.let { add("BOSS: $it") }
                        }.joinToString(" · "),
                        onClick = { onOpen(WikiDest.Instance(d.id)) },
                    )
                }
            }
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun WikiItemSearchScreen(
    state: PhoneState,
    saved: WikiSearchState,
    onBack: () -> Unit,
    onOpen: (WikiItem) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val keyboard = LocalSoftwareKeyboardController.current

    var input by remember { mutableStateOf(saved.browseInput) }
    var filter by remember { mutableStateOf(saved.filter) }
    // 面板改的是草稿，点「看结果」才落到 filter —— 和石之家招募筛选、
    // 钓鱼笔记同一套（PhoneFilterPanel 的既定用法），也对上站点的显式搜索按钮。
    var showFilters by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(saved.filter) }

    var items by remember { mutableStateOf<List<WikiItem>>(emptyList()) }
    var total by remember { mutableStateOf(-1) }
    var page by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var appending by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    // 输入防抖 260ms：51120 行的 LIKE 全表扫在 PC 上 9-18ms，
    // 真机慢几倍也还在一帧多一点，但每敲一个字都查一次没必要。
    LaunchedEffect(Unit) {
        snapshotFlow { input }
            .debounce(260)
            .distinctUntilChanged()
            .collect { q -> filter = filter.copy(query = q); saved.browseInput = q }
    }

    // 条件变了就同步给持有者，返回时能原样恢复
    LaunchedEffect(filter) { saved.filter = filter }

    // 条件一变就回到第 1 页重查
    LaunchedEffect(filter) {
        loading = true
        failure = null
        page = 0
        runCatching {
            total = WikiDb.count(context, filter)
            items = WikiDb.search(context, filter, page = 0)
        }.onFailure { failure = it.message ?: "本地数据读取失败" }
        loading = false
    }

    val listState = rememberLazyListState()
    val canLoadMore by remember {
        derivedStateOf { !loading && !appending && items.size < total }
    }

    // 滚到接近底部时追加下一页
    LaunchedEffect(listState, canLoadMore) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= items.size - 8
        }.distinctUntilChanged().collect { near ->
            if (near && canLoadMore) {
                appending = true
                val next = page + 1
                runCatching { WikiDb.search(context, filter, page = next) }
                    .onSuccess { more ->
                        if (more.isNotEmpty()) {
                            items = items + more
                            page = next
                        }
                    }
                appending = false
            }
        }
    }

    val margin = LocalContentMargin.current

    ScreenFrame {
        ScreenHeader("物品检索", state, onBack = onBack)

        WikiSearchField(input, onChange = { input = it }, onSubmit = { keyboard?.hide() })

        // 页头只留「现在筛的是什么」，每条带 × 可直接摘掉；条件本体在面板里。
        PhoneFilterBar(
            active = wikiActiveChips(filter) { filter = it },
            onOpen = { draft = filter; showFilters = true },
            modifier = Modifier.padding(start = margin.dp, end = margin.dp, top = 9.dp),
        )

        WikiResultBar(total, loading, filter)

        when {
            failure != null -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                PhoneEmpty(
                    "本地物品库读不出来",
                    failure.orEmpty().ifBlank { "数据文件可能没解压完，重进一次这个应用" },
                    iconRes = R.drawable.ic2_warning,
                    iconTint = PhoneWarn,
                )
            }
            loading -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PhoneAccent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
            }
            items.isEmpty() -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                PhoneEmpty(
                    "没有符合条件的物品",
                    if (filter.isEmpty) "换个词试试，中文日文英文名都能搜"
                    else "条件收得有点紧，摘掉一两个筛选再看",
                )
            }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 18.dp),
            ) {
                lazyItems(items, key = { it.id }) { item ->
                    WikiItemRow(item) { onOpen(item) }
                    PhoneHairlineRow(18.dp)
                }
                if (appending) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = PhoneAccent, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        WikiFilterSheet(
            draft = draft,
            onDraft = { draft = it },
            onClose = { showFilters = false },
            onReset = { draft = WikiFilter(query = draft.query) },
            onApply = { filter = draft; showFilters = false },
        )
    }
}

/** 搜索框。照钓鱼笔记的形态：42dp 高、圆角、surfaceRaised 底，不用 OutlinedTextField。 */
@Composable
private fun WikiSearchField(
    value: String,
    onChange: (String) -> Unit,
    onSubmit: () -> Unit,
    hint: String = "中文 / 日文 / 英文名",
) {
    val margin = LocalContentMargin.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        // 46dp + 主题色搜索图标。搜索是这一屏的主操作（两个入口是次要的），
        // 原来 42dp 灰图标的一条，比下面的入口卡还轻。
        modifier = Modifier.fillMaxWidth().padding(horizontal = margin.dp)
            .height(46.dp).clip(RoundedCornerShape(12.dp))
            .background(PhoneSurfaceRaised).padding(horizontal = 13.dp),
    ) {
        ImageGlyph(R.drawable.ic2_search, PhoneAccent, Modifier.size(18.dp))
        BasicTextField(
            value,
            onChange,
            singleLine = true,
            textStyle = TextStyle(color = PhoneText, fontSize = 15.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(hint, color = PhoneMuted, fontSize = 13.sp)
                    }
                    field()
                }
            },
        )
        if (value.isNotEmpty()) {
            Box(
                Modifier.size(24.dp).clip(CircleShape).clickable { onChange("") },
                contentAlignment = Alignment.Center,
            ) {
                ImageGlyph(R.drawable.ic2_close_circle, PhoneMuted, Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun WikiResultBar(total: Int, loading: Boolean, filter: WikiFilter) {
    val margin = LocalContentMargin.current
    val text = when {
        loading -> "检索中…"
        total < 0 -> ""
        // 0 条时不写在这里 —— 下面整屏的 PhoneEmpty 已经说了一遍，
        // 两处同一句话是噪声。站点也是只在结果区写一次。
        total == 0 -> ""
        filter.isEmpty -> "共 ${formatCount(total)} 件物品"
        else -> "共有 ${formatCount(total)} 个符合条件的物品"
    }
    if (text.isBlank()) return
    Row(
        Modifier.fillMaxWidth().padding(horizontal = margin.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = PhoneMuted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        if (filter.jobId > 0 && filter.jobNarrow) {
            Text("仅显示符合该职业属性的装备", color = PhoneMuted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun WikiItemRow(item: WikiItem, onClick: () -> Unit) {
    val margin = LocalContentMargin.current
    val rarityArgb = WikiDicts.rarityColorArgb(item.rarity)
    val nameColor = if (rarityArgb == 0L) PhoneText else Color(rarityArgb)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = margin.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(7.dp)).background(PhoneSurface),
            contentAlignment = Alignment.Center,
        ) {
            WikiIcon(
                iconId = item.iconId,
                iconHash = item.iconHash,
                fallbackText = item.nameCn.take(2),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                item.nameCn.ifBlank { item.nameEn },
                color = nameColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildItemSubtitle(item),
                color = PhoneMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        // 品级单独一列。上一版把它并进副标题的括号里（`Lv100 单手剑（品级 795）`），
        // 那样一列数字读不出来 —— 挑装备就是在比这个数，它得能上下对着扫。
        // 副标题里已经不带品级了（见 buildItemSubtitle），所以这里不是重复。
        //
        // **没有品级的行也要占住这一列**（材料、家具、停止流通的腰带）。
        // 不占的话名字能多伸 34dp，一屏里装备行和材料行的名字在不同位置截断，
        // 右边缘参差。搜「iron」这种混合结果就是这个情形。
        Box(Modifier.padding(start = 8.dp).width(34.dp), contentAlignment = Alignment.CenterEnd) {
            if (item.kindId in 1..4 && item.itemLevel > 0) {
                Text(
                    item.itemLevel.toString(),
                    color = PhoneMuted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

/**
 * 副标题：`Lv60 大斧`（装备）/ `套装`（其余）。
 *
 * **品级不在这里** —— 它在行尾自己一列（见 [WikiItemRow]）。
 * 上一版是照抄站点 `.item-category` 的 `Lv60 大斧（品级 235）`，
 * 站点那样写是因为它是网页卡片、横向空间富裕；手机上 60 行括号里的数字
 * 上下不对齐，扫不出来。挑装备就是在比品级，所以让它单独成列。
 */
private fun buildItemSubtitle(item: WikiItem): String {
    val name = item.categoryName
    if (item.kindId !in 1..4) return name
    return if (item.equipLevel > 0) "Lv${item.equipLevel} $name" else name
}

// WikiEmpty 删了：它只有图标 + 一行灰字，说了"没有"但没说"接下来做什么"，
// 而且用 fillMaxSize（在 ScreenFrame 的 Column 里会吃掉全部剩余高度）。
// 现在统一用全局的 PhoneEmpty（标题 + 说明 + 可选动作），外面套 weight(1f)。

// ---- 筛选面板 ----

/**
 * 筛选面板。条件与顺序**照抄站点 `Module:Item/ItemSearch` 的 FILTER_LIST**：
 * 名称 / 物品品级 / 装备等级 / 职业 / 物品类型 / 品质 / 版本 / 染色 / 可获得。
 *
 * 名称在页头的搜索框里，其余八项在这。物品类型和版本都是两级
 * （主类型→细类、资料片→小版本）—— 站点是两个联动 select，
 * chip 铺 112 个细类或 110 个版本都不现实。
 *
 * 用 [PhoneFilterPanel] + [PhoneChipGroup]，和石之家招募筛选、钓鱼笔记同一套件。
 * 我第一版自己做了横滑 chip 条，而 FishingScreen 的注释里写着那版正是被淘汰的形态。
 */
@Composable
private fun WikiFilterSheet(
    draft: WikiFilter,
    onDraft: (WikiFilter) -> Unit,
    onClose: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {
    // 版本和职业的一级选择只是 UI 状态，不进 WikiFilter ——
    // 真正的条件是具体小版本 / 具体职业。开面板时按已选值回填。
    var versionMajor by remember(draft.version) {
        mutableStateOf(if (draft.version > 0) draft.version.toInt() else 0)
    }
    var jobRole by remember(draft.jobId) {
        mutableStateOf(WikiDicts.roleOfJob(draft.jobId))
    }

    PhoneFilterPanel(onClose = onClose, onReset = onReset, onApply = onApply) {
        WikiRangeGroup(
            "物品品级",
            draft.itemLevelMin, draft.itemLevelMax,
        ) { lo, hi -> onDraft(draft.copy(itemLevelMin = lo, itemLevelMax = hi)) }

        WikiRangeGroup(
            "装备等级",
            draft.equipLevelMin, draft.equipLevelMax,
        ) { lo, hi -> onDraft(draft.copy(equipLevelMin = lo, equipLevelMax = hi)) }

        // 职业：定位 → 具体职业。33 个 chip 平铺要 9 行，会把后面七组条件
        // 全挤出第一屏（真机验过）。定位一级只有 7 个，一行半就够。
        PhoneChipGroup(
            label = "职业",
            options = WikiDicts.jobRoles.map { it.first to it.first },
            selected = setOfNotNull(jobRole.takeIf { it.isNotBlank() }),
            onPick = { role ->
                if (jobRole == role) {
                    // 收起定位时把已选职业一起清掉，否则留下看不见的条件
                    jobRole = ""
                    onDraft(draft.copy(jobId = 0))
                } else {
                    jobRole = role
                    // 换定位时清掉旧职业 —— 它不属于新定位
                    onDraft(draft.copy(jobId = 0))
                }
            },
        )

        if (jobRole.isNotBlank()) {
            PhoneChipGroup(
                label = "　$jobRole",
                options = WikiDicts.jobsOfRole(jobRole).map { it.first.toString() to it.second },
                selected = setOfNotNull(draft.jobId.takeIf { it > 0 }?.toString()),
                onPick = { id ->
                    val v = id.toIntOrNull() ?: 0
                    onDraft(draft.copy(jobId = if (draft.jobId == v) 0 else v))
                },
            )
        }

        if (draft.jobId > 0) {
            WikiToggleRow(
                on = draft.jobNarrow,
                title = "只看该职业该穿的",
                hint = if (draft.jobNarrow) "与网页检索器一致，按属性需求过滤"
                       else "列出所有能装备的，找幻化时更全",
            ) { onDraft(draft.copy(jobNarrow = !draft.jobNarrow)) }
        }

        // 物品类型：主类型 → 细类（站点 switch_kind 的两级联动）
        PhoneChipGroup(
            label = "物品类型",
            options = WikiDicts.kinds.map { it.first.toString() to it.second },
            selected = setOfNotNull(draft.kindId.takeIf { it > 0 }?.toString()),
            onPick = { id ->
                val v = id.toIntOrNull() ?: 0
                // 换主类型必须清细类，否则留下不属于新主类型的 categoryId，查出 0 条
                onDraft(
                    if (draft.kindId == v) draft.copy(kindId = 0, categoryId = 0)
                    else draft.copy(kindId = v, categoryId = 0),
                )
            },
        )

        if (draft.kindId > 0) {
            val cats = WikiDicts.categoriesOf(draft.kindId)
            if (cats.isNotEmpty()) {
                PhoneChipGroup(
                    label = "　细类",
                    options = cats.map { it.first.toString() to it.second },
                    selected = setOfNotNull(draft.categoryId.takeIf { it > 0 }?.toString()),
                    onPick = { id ->
                        val v = id.toIntOrNull() ?: 0
                        onDraft(draft.copy(categoryId = if (draft.categoryId == v) 0 else v))
                    },
                )
            }
        }

        PhoneChipGroup(
            label = "品质",
            options = WikiDicts.rarities.map { it.first.toString() to it.second },
            selected = setOfNotNull(draft.rarity.takeIf { it > 0 }?.toString()),
            onPick = { id ->
                val v = id.toIntOrNull() ?: 0
                onDraft(draft.copy(rarity = if (draft.rarity == v) 0 else v))
            },
        )

        // 版本：资料片 → 小版本
        PhoneChipGroup(
            label = "版本",
            options = WikiDicts.expansions.map { it.first.toString() to it.second },
            selected = setOfNotNull(versionMajor.takeIf { it > 0 }?.toString()),
            onPick = { id ->
                val v = id.toIntOrNull() ?: 0
                if (versionMajor == v) {
                    versionMajor = 0
                    onDraft(draft.copy(version = 0.0))
                } else {
                    versionMajor = v
                    onDraft(draft.copy(version = 0.0))
                }
            },
        )

        if (versionMajor > 0) {
            PhoneChipGroup(
                label = "　小版本",
                options = WikiDicts.versionsOf(versionMajor)
                    .map { it.first.toString() to it.second },
                selected = setOfNotNull(draft.version.takeIf { it > 0 }?.toString()),
                onPick = { id ->
                    val v = id.toDoubleOrNull() ?: 0.0
                    onDraft(draft.copy(version = if (draft.version == v) 0.0 else v))
                },
            )
        }

        PhoneChipGroup(
            label = "染色",
            // 库里存的是染色槽数（0/1/2）；站点 dye 参数是槽数+1，别照搬站点的值
            options = WikiDicts.dyeOptions.map { it.first.toString() to it.second },
            selected = setOfNotNull(draft.dye.takeIf { it >= 0 }?.toString()),
            onPick = { id ->
                val v = id.toIntOrNull() ?: -1
                onDraft(draft.copy(dye = if (draft.dye == v) -1 else v))
            },
        )

        PhoneChipGroup(
            label = "可获得",
            options = WikiDicts.obtainableOptions.map { it.first.toString() to it.second },
            selected = setOfNotNull(draft.obtainable.takeIf { it > 0 }?.toString()),
            onPick = { id ->
                val v = id.toIntOrNull() ?: 0
                onDraft(draft.copy(obtainable = if (draft.obtainable == v) 0 else v))
            },
        )
    }
}

/** 品级/等级的区间输入。站点是两个 maxlength=3 的数字框。 */
@Composable
private fun WikiRangeGroup(
    label: String,
    min: Int,
    max: Int,
    onChange: (Int, Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = PhoneText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            WikiNumberField(min, "最小") { onChange(it, max) }
            Text("–", color = PhoneMuted, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 10.dp))
            WikiNumberField(max, "最大") { onChange(min, it) }
        }
    }
}

@Composable
private fun WikiNumberField(value: Int, hint: String, onChange: (Int) -> Unit) {
    // 0 当"不限"，所以显示成空串而不是 "0"
    var text by remember(value) { mutableStateOf(if (value > 0) value.toString() else "") }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.width(104.dp).height(40.dp)
            .clip(RoundedCornerShape(10.dp)).background(PhoneSurfaceRaised)
            .padding(horizontal = 11.dp),
    ) {
        BasicTextField(
            text,
            { raw ->
                // 站点 maxlength=3，但品级已经到 795 且以后会更高，放到 4 位
                val digits = raw.filter(Char::isDigit).take(4)
                text = digits
                onChange(digits.toIntOrNull() ?: 0)
            },
            singleLine = true,
            textStyle = TextStyle(color = PhoneText, fontSize = 13.sp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (text.isBlank()) Text(hint, color = PhoneMuted, fontSize = 12.sp)
                    field()
                }
            },
        )
    }
}

@Composable
private fun WikiToggleRow(on: Boolean, title: String, hint: String, onToggle: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggle).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ImageGlyph(
            if (on) R.drawable.ic2_radio_on else R.drawable.ic2_radio_off,
            if (on) PhoneAccent else PhoneMuted,
            Modifier.size(17.dp),
        )
        Column(Modifier.padding(start = 9.dp)) {
            Text(title, color = PhoneText, fontSize = 13.sp)
            Text(hint, color = PhoneMuted, fontSize = 10.sp)
        }
    }
}

/**
 * 页头筛选条上那些"当前生效的条件"，点一下摘掉一条。
 * 顺序和面板一致，别让用户在两处看到不同的排列。
 */
private fun wikiActiveChips(
    filter: WikiFilter,
    onChange: (WikiFilter) -> Unit,
): List<Pair<String, () -> Unit>> = buildList {
    if (filter.itemLevelMin > 0 || filter.itemLevelMax > 0) {
        add(wikiRangeLabel("品级", filter.itemLevelMin, filter.itemLevelMax) to {
            onChange(filter.copy(itemLevelMin = 0, itemLevelMax = 0))
        })
    }
    if (filter.equipLevelMin > 0 || filter.equipLevelMax > 0) {
        add(wikiRangeLabel("Lv", filter.equipLevelMin, filter.equipLevelMax) to {
            onChange(filter.copy(equipLevelMin = 0, equipLevelMax = 0))
        })
    }
    if (filter.jobId > 0) {
        add(WikiDicts.jobName(filter.jobId) to { onChange(filter.copy(jobId = 0)) })
    }
    if (filter.kindId > 0) {
        add(WikiDicts.kindName(filter.kindId) to {
            onChange(filter.copy(kindId = 0, categoryId = 0))
        })
    }
    if (filter.categoryId > 0) {
        add(WikiDicts.categoryName(filter.categoryId) to {
            onChange(filter.copy(categoryId = 0))
        })
    }
    if (filter.rarity > 0) {
        add("品质${WikiDicts.rarityName(filter.rarity)}" to {
            onChange(filter.copy(rarity = 0))
        })
    }
    if (filter.version > 0) {
        add(WikiDicts.versionLabel(filter.version) to { onChange(filter.copy(version = 0.0)) })
    }
    if (filter.dye >= 0) {
        val label = WikiDicts.dyeOptions.firstOrNull { it.first == filter.dye }?.second.orEmpty()
        add(label to { onChange(filter.copy(dye = -1)) })
    }
    if (filter.obtainable > 0) {
        val label = WikiDicts.obtainableOptions
            .firstOrNull { it.first == filter.obtainable }?.second.orEmpty()
        add(label to { onChange(filter.copy(obtainable = 0)) })
    }
}

private fun wikiRangeLabel(prefix: String, lo: Int, hi: Int): String = when {
    lo > 0 && hi > 0 -> "$prefix$lo-$hi"
    lo > 0 -> "$prefix≥$lo"
    else -> "$prefix≤$hi"
}

// ---- 详情 ----

/**
 * 物品详情。基础字段来自本地库（离线可看）；
 * 「如何获得」与装备属性点进来时按需拉（实测 220-260 ms / 1.6-3.2 KB），
 * 结果落磁盘缓存，再看同一件不走网络。
 */
@Composable
private fun WikiDetailScreen(
    state: PhoneState,
    item: WikiItem,
    onBack: () -> Unit,
    onOpen: (WikiDest) -> Unit = {},
) {
    val margin = LocalContentMargin.current
    val context = LocalContext.current.applicationContext
    val rarityArgb = WikiDicts.rarityColorArgb(item.rarity)
    val nameColor = if (rarityArgb == 0L) PhoneText else Color(rarityArgb)

    var detail by remember(item.id) { mutableStateOf<WikiDetail?>(null) }
    var loadingDetail by remember(item.id) { mutableStateOf(true) }
    var retry by remember(item.id) { mutableStateOf(0) }
    // 采集点/钓场从本地 nodes 表翻名字，不用再联网
    var nodes by remember(item.id) { mutableStateOf<Map<Int, WikiNode>>(emptyMap()) }

    LaunchedEffect(item.id, retry) {
        loadingDetail = true
        val d = runCatching { WikiRemote.detail(context, item.id) }.getOrNull()
        detail = d
        loadingDetail = false
        if (d != null) {
            val ids = d.sources
                .filter { it.kind == "采集点" || it.kind == "钓鱼" }
                .map { it.refId }
                .filter { it > 0 }
            if (ids.isNotEmpty()) {
                nodes = runCatching { WikiDb.nodes(context, ids) }.getOrDefault(emptyMap())
            }
        }
    }

    ScreenFrame {
        // 名字只在头部出现一次，**带稀有度色**。
        // 原来头部 20sp 和 hero 17sp 各写一遍同一串字，外壳比主体还大。
        ScreenHeader(
            item.nameCn.ifBlank { item.nameEn },
            state,
            onBack = onBack,
            titleColor = nameColor,
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = margin.dp, end = margin.dp, bottom = 20.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(68.dp).clip(RoundedCornerShape(10.dp)).background(PhoneSurface),
                        contentAlignment = Alignment.Center,
                    ) {
                        WikiIcon(
                            iconId = item.iconId,
                            iconHash = item.iconHash.ifBlank { detail?.iconHash.orEmpty() },
                            fallbackText = item.nameCn.take(2),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(
                            listOf(item.kindName, item.categoryName)
                                .filter { it.isNotBlank() }.joinToString(" · "),
                            color = PhoneText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                        // 装备等级紧跟类型 —— 这两个一起决定"我能不能穿"。
                        if (item.equipLevel > 1) {
                            Text(
                                "需要等级 Lv${item.equipLevel}",
                                color = PhoneMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        if (item.rare || item.uniqueItem || item.dye > 0) {
                            Row(
                                Modifier.padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                if (item.rare) WikiBadge("珍稀", PhoneWarn)
                                if (item.uniqueItem) WikiBadge("独占", PhoneInfo)
                                if (item.dye > 0) {
                                    WikiBadge(if (item.dye >= 2) "双染" else "可染", PhoneAccent)
                                }
                            }
                        }
                    }
                    // 签名元素：品级数牌。只给装备（kindId 1-4）——
                    // 材料/家具没有品级，给它们摆个空牌就是装饰。
                    if (item.kindId in 1..4 && item.itemLevel > 0) {
                        WikiIlvlPlate(item.itemLevel)
                    }
                }
            }

            // ---- 如何获得（按需拉取）----
            // **放在第一位**：查一件东西的目的通常就是"它怎么拿"。
            // 原来这节在最底下，而 `物品 ID 51000` 占着第一行。
            item { WikiSectionTitle("如何获得") }
            item {
                val d = detail
                when {
                    loadingDetail -> Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                            .background(PhoneSurface).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            color = PhoneAccent, strokeWidth = 2.dp,
                            modifier = Modifier.size(15.dp),
                        )
                        Text("正在获取…", color = PhoneMuted, fontSize = 11.sp,
                            modifier = Modifier.padding(start = 9.dp))
                    }
                    d == null -> Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                            .background(PhoneSurface).clickable { retry++ }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ImageGlyph(R.drawable.ic2_refresh_cycle, PhoneAccent, Modifier.size(15.dp))
                        Column(Modifier.padding(start = 9.dp)) {
                            Text("获取失败，点击重试", color = PhoneText, fontSize = 12.sp)
                            Text("离线或站点限流时会这样", color = PhoneMuted, fontSize = 10.sp)
                        }
                    }
                    d.unobtainable && d.sources.size <= 1 -> WikiFactCard(
                        listOf("状态" to (d.sources.firstOrNull()?.detail
                            ?.takeIf { it.isNotBlank() } ?: "已停止获取")),
                    )
                    d.sources.isEmpty() -> WikiFactCard(listOf("来源" to "站点未记录"))
                    else -> WikiSourceCard(d.sources, nodes, onOpen)
                }
            }

            // ---- 装备属性（同一次请求带回来的）----
            detail?.stats?.takeIf { it.isNotEmpty() }?.let { stats ->
                item { WikiSectionTitle("装备属性") }
                item { WikiFactCard(stats) }
            }

            detail?.description?.takeIf { it.isNotBlank() }?.let { desc ->
                item { WikiSectionTitle("描述") }
                item {
                    Text(
                        // 站点描述里带 <br> 标签
                        desc.replace("<br>", "\n").replace("<br/>", "\n"),
                        color = PhoneText,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp)).background(PhoneSurface).padding(12.dp),
                    )
                }
            }

            if (item.jobIds.isNotEmpty()) {
                item { WikiSectionTitle("可使用职业") }
                item {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        item.jobIds.forEach { id ->
                            WikiBadge(WikiDicts.jobName(id).ifBlank { "职业$id" }, PhoneAccent)
                        }
                    }
                }
            }

            item { WikiSectionTitle("基础信息") }
            item {
                WikiFactCard(
                    buildList {
                        // 品级和装备等级已经在 hero 里（数牌 + "需要等级"），
                        // 这里不再重复 —— 重复的行是噪声，会把真正只有这里才有的
                        // 堆叠/价格/版本挤下去。
                        add("品质" to (item.rarityName.ifBlank { "—" }))
                        add("染色槽" to when (item.dye) {
                            0 -> "不可染色"
                            1 -> "1 个"
                            else -> "${item.dye} 个"
                        })
                        if (item.stack > 1) add("堆叠上限" to formatCount(item.stack))
                        if (item.priceBuy > 0) add("购买价格" to "${formatCount(item.priceBuy)} 金币")
                        if (item.priceSell > 0) add("出售价格" to "${formatCount(item.priceSell)} 金币")
                        add("加入版本" to formatVersion(item.version))
                        // ID 排最后：它是查数据时才用的，玩家几乎不看。
                        // 原来它在第一行，和"装备等级"一样重。
                        add("物品 ID" to item.id.toString())
                    },
                )
            }

            // 多语言名称降级成一个紧凑块 —— 它是同一件东西的另外两种写法，
            // 撑不起一个和"如何获得"平级的分区。
            item { WikiSectionTitle("其他语言") }
            item {
                WikiFactCard(
                    buildList {
                        if (item.nameJp.isNotBlank()) add("日文" to item.nameJp)
                        if (item.nameEn.isNotBlank()) add("英文" to item.nameEn)
                    },
                )
            }
        }
    }
}

/**
 * 品级数牌 —— 这个模块的签名元素。
 *
 * 品级是 FF14 里给装备排序的那个数（站点、游戏内 tooltip 都把它摆在显眼处）。
 * 它配得上一个专门的位置：等宽数字右对齐，上面一行小眉标。
 *
 * 只给装备用。材料和家具没有品级，给它们摆个空牌就成了纯装饰。
 */
@Composable
private fun WikiIlvlPlate(itemLevel: Int) {
    Column(horizontalAlignment = Alignment.End) {
        // 和 WikiSectionTitle 同一档（10sp / 1.2sp 字距）—— 两者都是眉标，
        // 没有理由不一样。
        Text(
            "品级",
            color = PhoneMuted,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
        )
        Text(
            itemLevel.toString(),
            color = PhoneText,
            fontSize = 21.sp,
            fontWeight = FontWeight.Medium,
            // 等宽数字：一列品级要能上下对齐着读
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

/**
 * 按途径分组显示的来源卡片。
 *
 * [nodes] 用来把「采集点 ID 391」换成实际地点 —— 光给 ID 对用户没用。
 */
@Composable
private fun WikiSourceCard(
    sources: List<WikiSourceEntry>,
    nodes: Map<Int, WikiNode> = emptyMap(),
    onOpen: (WikiDest) -> Unit = {},
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(PhoneSurface)) {
        sources.groupBy { it.kind }.entries.forEachIndexed { gi, (kind, entries) ->
            if (gi > 0) PhoneHairlineRow(12.dp)
            Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(kind, color = PhoneAccent, fontSize = 11.sp,
                        fontWeight = FontWeight.Medium)
                    if (entries.size > 1) {
                        Text(" ×${entries.size}", color = PhoneMuted, fontSize = 10.sp)
                    }
                }
                entries.forEach { e ->
                    val dest = e.destOrNull()
                    val node = nodes[e.refId]
                    // 可点的行加下划线 + 用主题色，和纯文字行区分开 ——
                    // 不然用户不知道哪些能点。
                    val clickable = dest != null
                    val rowMod = if (clickable) {
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpen(dest!!) }
                            .padding(vertical = 3.dp)
                    } else {
                        Modifier.fillMaxWidth().padding(top = 3.dp)
                    }
                    Column(rowMod) {
                        if (node != null) {
                            WikiLinkText(node.placeText, clickable)
                            val extras = buildList {
                                if (node.level > 0) {
                                    add("Lv${node.level}" +
                                        "★".repeat(node.stars.coerceIn(0, 3)))
                                }
                                node.windowText.takeIf { it.isNotBlank() }?.let(::add)
                                node.folkloreName.takeIf { it.isNotBlank() }
                                    ?.let { add("需 $it") }
                            }
                            if (extras.isNotEmpty()) {
                                Text(
                                    extras.joinToString(" · "),
                                    color = if (node.isTimed) PhoneWarn else PhoneMuted,
                                    fontSize = 10.sp,
                                )
                            }
                        } else {
                            val line = listOf(e.name, e.detail)
                                .filter { it.isNotBlank() }.joinToString(" — ")
                            if (line.isNotBlank()) WikiLinkText(line, clickable)
                        }
                    }
                }
            }
        }
    }
}

/** 把一条来源翻成导航目标。不可跳的返回 null。 */
private fun WikiSourceEntry.destOrNull(): WikiDest? = when (linkKind) {
    WikiLinkKind.ITEM -> WikiDest.Item(linkId)
    WikiLinkKind.QUEST -> WikiDest.Quest(linkId)
    WikiLinkKind.INSTANCE -> WikiDest.Instance(linkId)
    WikiLinkKind.SHOP -> WikiDest.Shop(linkId)
    WikiLinkKind.NODE -> WikiDest.Node(linkId)
    WikiLinkKind.NONE -> null
}.takeIf { linkId > 0 }

/** 可点行用主题色 + 下划线，不可点的用正文色。 */
@Composable
private fun WikiLinkText(text: String, clickable: Boolean, size: Int = 12) {
    Text(
        text,
        color = if (clickable) PhoneAccent else PhoneText,
        fontSize = size.sp,
        textDecoration = if (clickable) TextDecoration.Underline else null,
    )
}

/**
 * 物品图标：先走 xivapi（复用 App 现有的 [ItemIconLoader] 缓存链路），
 * 取不到再退到灰机图床。
 *
 * 需要兜底是因为 xivapi 缺一部分图标 —— 实测随机 120 个里 2 个稳定 404
 * （约 1.7%，全都落在 7.x），而灰机图床都有。见 开发/WIKI/_probe_icons3.py。
 */
@Composable
private fun WikiIcon(
    iconId: Int,
    iconHash: String,
    fallbackText: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current.applicationContext
    var bitmap by remember(iconId, iconHash) {
        mutableStateOf(
            if (iconId > 0) ItemIconLoader.peek(iconId) else WikiIconCache.peek(iconHash)
        )
    }

    LaunchedEffect(iconId, iconHash) {
        if (bitmap != null) return@LaunchedEffect
        // 一屏 60 行同时拉图，并发下会有零星失败。真机上撞到过：
        // 首次进入两行显示文字兜底，滚开再回来（行被回收重建）就好了。
        // 所以这里做有限重试，别让一次网络抖动把图标永久留空。
        repeat(3) { attempt ->
            if (attempt > 0) kotlinx.coroutines.delay(400L * attempt)
            if (iconId > 0) bitmap = ItemIconLoader.load(context, iconId)
            if (bitmap == null && iconHash.isNotBlank()) {
                bitmap = WikiIconCache.load(context, iconHash)
            }
            if (bitmap != null) return@LaunchedEffect
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        Box(modifier, contentAlignment = Alignment.Center) {
            if (fallbackText.isNotBlank()) {
                Text(fallbackText, color = PhoneMuted, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

/**
 * 分区眉标。字距拉开 + 全大写式的克制感，让它明确是**标签**而不是内容的一部分。
 *
 * 原来 10sp 无字距，和下面卡片里 12sp 的标签几乎一样重，
 * 分区读起来不像分区。
 */
@Composable
private fun WikiSectionTitle(text: String) {
    Text(
        text,
        color = PhoneMuted,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun WikiFactCard(rows: List<Pair<String, String>>) {
    if (rows.isEmpty()) return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(PhoneSurface)) {
        rows.forEachIndexed { index, (label, value) ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 问答关系靠**三个通道**同时表达：色（Muted/Text）、字重
                // （Normal/Medium）、字号（12/13）。原来两边都是 12sp 同色同重，
                // 分不出谁是问谁是答。
                // 单靠字号做不到——12 和 13 只差一档，真正拉开距离的是色和字重。
                Text(label, color = PhoneMuted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    value,
                    color = PhoneText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            if (index < rows.lastIndex) {
                PhoneHairlineRow(12.dp)
            }
        }
    }
}

@Composable
private fun WikiBadge(text: String, tint: Color) {
    Text(
        text,
        color = tint,
        fontSize = 9.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/**
 * 3.5 -> "3.5"、7.0 -> "7.0"、7.51 -> "7.51"。
 *
 * 别用 `BigDecimal(double)` —— 那个构造函数会把二进制浮点的完整展开吐出来，
 * 7.51 会变成 "7.5099999999999997868371792719699442386627197265625"（真出过）。
 * `valueOf` 走的是 `Double.toString()`，拿到的是最短往返表示。
 */
private fun formatVersion(v: Double): String =
    if (v <= 0) "—" else java.math.BigDecimal.valueOf(v).stripTrailingZeros().toPlainString()
        .let { if ('.' in it) it else "$it.0" }
