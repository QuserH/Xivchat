package com.quserh.eorzeaphone.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.FishingMapImageLoader
import com.quserh.eorzeaphone.data.wiki.DutyDb
import com.quserh.eorzeaphone.data.wiki.DutyDrop
import com.quserh.eorzeaphone.data.wiki.QuestDb
import com.quserh.eorzeaphone.data.wiki.QuestMap
import com.quserh.eorzeaphone.data.wiki.QuestNode
import com.quserh.eorzeaphone.data.wiki.WikiDb
import com.quserh.eorzeaphone.data.wiki.WikiDuty
import com.quserh.eorzeaphone.data.wiki.WikiInstance
import com.quserh.eorzeaphone.data.wiki.WikiItem
import com.quserh.eorzeaphone.data.wiki.WikiNode
import com.quserh.eorzeaphone.data.wiki.WikiQuest
import com.quserh.eorzeaphone.data.wiki.WikiRemote
import com.quserh.eorzeaphone.data.wiki.WikiShop
import com.quserh.eorzeaphone.ui.theme.CanvasLabelScrim
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import com.quserh.eorzeaphone.ui.theme.MapPin
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneInfo
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText
import com.quserh.eorzeaphone.ui.theme.PhoneWarn

// 物品检索的跳转目标屏：任务 / 副本 / 商店 / 采集点。
// 都只做一件事——把站点数据页渲染成能继续往下点的页面，
// 让「找东西顺着找到任务」这条链走得通。
//
// 加载/缺失两个占位屏放在这里给四个屏和物品路由共用。

@Composable
internal fun WikiLoadingScreen(what: String, state: PhoneState, onBack: () -> Unit) {
    ScreenFrame {
        ScreenHeader(what, state, onBack = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = PhoneAccent, strokeWidth = 2.dp, modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
internal fun WikiMissingScreen(
    what: String,
    state: PhoneState,
    onBack: () -> Unit,
    onRetry: (() -> Unit)? = null,
) {
    ScreenFrame {
        ScreenHeader(what, state, onBack = onBack)
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            PhoneEmpty(
                "读不到这一页",
                if (onRetry != null) "离线或站点限流时会这样，点下面重试"
                else "站点可能没有这一页的资料",
                R.drawable.ic2_warning,
                action = onRetry?.let {
                    {
                        Text(
                            "重试", color = PhoneAccent, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(9.dp))
                                .background(PhoneSurfaceRaised)
                                .clickable(onClick = it)
                                .padding(horizontal = 18.dp, vertical = 9.dp),
                        )
                    }
                },
            )
        }
    }
}

// ---- 任务 ----

@Composable
internal fun WikiQuestScreen(
    state: PhoneState,
    id: Int,
    onBack: () -> Unit,
    onOpen: (WikiDest) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var q by remember(id) { mutableStateOf<WikiQuest?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var retry by remember(id) { mutableStateOf(0) }
    // 相关物品那一串是裸 ID，名字要用本地库补
    var itemNames by remember(id) { mutableStateOf<Map<Int, WikiItem>>(emptyMap()) }
    // 本地那份：名字/等级/接取坐标/所在块。有了它就不必等网络才显示东西。
    var local by remember(id) { mutableStateOf<QuestNode?>(null) }
    var prev by remember(id) { mutableStateOf<List<QuestNode>>(emptyList()) }
    var next by remember(id) { mutableStateOf<List<QuestNode>>(emptyList()) }

    LaunchedEffect(id) {
        local = runCatching { QuestDb.byId(context, id) }.getOrNull()
        prev = runCatching { QuestDb.prevOf(context, id) }.getOrDefault(emptyList())
        next = runCatching { QuestDb.nextOf(context, id) }.getOrDefault(emptyList())
    }

    LaunchedEffect(id, retry) {
        loading = true
        val got = runCatching { WikiRemote.quest(context, id) }.getOrNull()
        q = got
        loading = false
        if (got != null) {
            val ids = (got.rewardItems + got.relatedItems).map { it.first }.distinct()
            if (ids.isNotEmpty()) {
                itemNames = runCatching {
                    buildMap { for (i in ids) WikiDb.byId(context, i)?.let { put(i, it) } }
                }.getOrDefault(emptyMap())
            }
        }
    }

    val quest = q
    val loc = local
    when {
        // 本地有这个任务就先画出来，奖励/剧情那些等网络到了再补上。
        // 原来是网络没回来整屏都是转圈，而名字和接取位置本来就在库里。
        quest == null && loc != null -> WikiQuestBody(
            state, null, loc, prev, next, itemNames, loading, onBack, onOpen,
        )
        loading -> WikiLoadingScreen("任务", state, onBack)
        quest == null -> WikiMissingScreen("任务 $id", state, onBack) { retry++ }
        else -> WikiQuestBody(
            state, quest, loc, prev, next, itemNames, false, onBack, onOpen,
        )
    }
}

/**
 * 任务详情。
 *
 * [quest] 是网络那份（奖励、剧情），可能还没到；[local] 是本地那份
 * （名字、等级、接取坐标、前后置）。两份都可能单独存在，所以每一段
 * 各自选可用的来源，而不是二选一整屏。
 */
@Composable
private fun WikiQuestBody(
    state: PhoneState,
    quest: WikiQuest?,
    local: QuestNode?,
    prev: List<QuestNode>,
    next: List<QuestNode>,
    itemNames: Map<Int, WikiItem>,
    stillLoading: Boolean,
    onBack: () -> Unit,
    onOpen: (WikiDest) -> Unit,
) {
    val margin = LocalContentMargin.current
    val id = quest?.id ?: local?.id ?: 0
    val name = quest?.name?.takeIf { it.isNotBlank() }
        ?: local?.name?.takeIf { it.isNotBlank() } ?: "任务 $id"
    val level = quest?.level?.takeIf { it > 0 } ?: local?.level ?: 0
    val jobGroup = quest?.jobGroup?.takeIf { it.isNotBlank() } ?: local?.jobGroup.orEmpty()
    val repeatable = quest?.repeatable ?: local?.repeatable ?: false
    val category = quest?.category?.takeIf { it.isNotBlank() } ?: local?.type.orEmpty()
    val place = quest?.place?.takeIf { it.isNotBlank() } ?: local?.place.orEmpty()

    ScreenFrame {
        ScreenHeader(
            name, state, onBack = onBack,
            trailing = if (stillLoading) {
                { Text("载入详情…", color = PhoneMuted, fontSize = 11.sp) }
            } else null,
        )
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = margin.dp, end = margin.dp, bottom = 20.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (level > 0) WikiChipBadge("Lv$level", PhoneInfo)
                    jobGroup.takeIf { it.isNotBlank() }
                        ?.let { WikiChipBadge(it, PhoneAccent) }
                    if (repeatable) WikiChipBadge("可重复", PhoneWarn)
                    if (local?.isOrPrereq == true) WikiChipBadge("前置满足其一", PhoneWarn)
                }
            }

            // 所在任务链 —— 从这里能跳回整块流程图
            if (local != null && local.chainId > 0) {
                item {
                    // PhoneCard：壳层唯一的卡片原语（HANDOFF.md §6），
                    // 自带按下缩放 + 阴影 + 浅色主题收边
                    PhoneCard(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        onClick = { onOpen(WikiDest.QuestTree(local.chainId, local.id)) },
                    ) {
                        Row(
                            Modifier.padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    local.category.ifBlank { "任务链" },
                                    color = PhoneText, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "在流程图里看它的位置",
                                    color = PhoneMuted, fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Text("流程图 ›", color = PhoneAccent, fontSize = 11.sp)
                        }
                    }
                }
                // 完整前置线：跨块一路上溯，和上面那个「块内流程图」不是一回事
                item {
                    PhoneCard(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        onClick = { onOpen(WikiDest.Ancestry(local.id)) },
                    ) {
                        Row(
                            Modifier.padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "完整前置线",
                                    color = PhoneText, fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "从头到这里要做的全部任务，跨任务链",
                                    color = PhoneMuted, fontSize = 10.sp,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Text("展开 ›", color = PhoneAccent, fontSize = 11.sp)
                        }
                    }
                }
            }

            item { WikiLinkSection("任务信息") }
            item {
                WikiLinkFacts(
                    buildList {
                        category.takeIf { it.isNotBlank() }?.let { add("类型" to it) }
                        place.takeIf { it.isNotBlank() }?.let { add("地点" to it) }
                        // 接取/交付优先用本地那份 —— 它带 NPC 称号和坐标
                        val sn = local?.startNpc
                        if (sn != null && sn.name.isNotBlank()) {
                            add("接取" to sn.nameText)
                            if (sn.hasPlace) add("接取位置" to sn.placeText)
                        } else {
                            quest?.startNpc?.takeIf { it.isNotBlank() }
                                ?.let { add("接取" to it) }
                        }
                        val en = local?.endNpc
                        if (en != null && en.name.isNotBlank() && en.name != sn?.name) {
                            add("交付" to en.name)
                            if (en.hasPlace) add("交付位置" to en.placeText)
                        } else {
                            quest?.endNpc?.takeIf { it.isNotBlank() }
                                ?.takeIf { it != quest.startNpc }?.let { add("交付" to it) }
                        }
                        add("任务 ID" to id.toString())
                    },
                )
            }

            // 接取地点的地图针
            if (local != null && local.startNpc.hasPlace && local.startNpc.hasCoord) {
                item { WikiLinkSection("接取位置") }
                item { QuestPinCard(local) }
            }

            // 前置：本地库有就用本地（能显示等级），否则用网络那份
            val prevRows = if (prev.isNotEmpty()) {
                prev.map { it.name to it.id }
            } else {
                quest?.prevQuests?.map { it.second to it.first }.orEmpty()
            }
            if (prevRows.isNotEmpty()) {
                item {
                    WikiLinkSection(
                        if (local?.isOrPrereq == true) "前置任务（满足其一）" else "前置任务",
                    )
                }
                item {
                    WikiLinkList(prevRows) { qid -> onOpen(WikiDest.Quest(qid)) }
                }
            }

            // 后续任务：本地库独有（站点的 Data:Quest 只给前置）
            if (next.isNotEmpty()) {
                item { WikiLinkSection("后续任务") }
                item {
                    WikiLinkList(next.map { it.name to it.id }) { qid ->
                        onOpen(WikiDest.Quest(qid))
                    }
                }
            }

            if (quest != null && quest.rewardItems.isNotEmpty()) {
                item { WikiLinkSection("任务奖励") }
                item {
                    WikiItemLinkList(quest.rewardItems, itemNames) { iid ->
                        onOpen(WikiDest.Item(iid))
                    }
                }
            }

            if (quest != null && quest.relatedItems.isNotEmpty()) {
                item { WikiLinkSection("相关物品") }
                item {
                    WikiItemLinkList(quest.relatedItems, itemNames) { iid ->
                        onOpen(WikiDest.Item(iid))
                    }
                }
            }
        }
    }
}

/**
 * 接取位置的地图针。
 *
 * 底图和坐标换算复用采集时钟那套（[FishingMapImageLoader] + sizeFactor 公式）。
 * 换算落在图外就只显示坐标文字 —— 实测「时空狭缝」「万魔殿正门」「伊甸内核」
 * 共 20 个任务是这样，它们的坐标不指向所在地图，画上去是错的。
 */
@Composable
private fun QuestPinCard(node: QuestNode) {
    val context = LocalContext.current
    val npc = node.startNpc
    var map by remember(npc.mapName) { mutableStateOf<QuestMap?>(null) }
    var bitmap by remember(npc.mapName) { mutableStateOf<Bitmap?>(null) }
    var done by remember(npc.mapName) { mutableStateOf(false) }

    LaunchedEffect(npc.mapName) {
        val m = runCatching { QuestDb.mapOf(context.applicationContext, npc.mapName) }
            .getOrNull()
        map = m
        if (m != null && m.mapFile.isNotBlank()) {
            bitmap = FishingMapImageLoader.load(context.applicationContext, m.mapFile)
        }
        done = true
    }

    val m = map
    val bmp = bitmap
    val frac = m?.fracOf(npc.x, npc.y)

    if (bmp == null || frac == null) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(PhoneSurface).padding(14.dp),
        ) {
            Text(npc.placeText, color = PhoneText, fontSize = 13.sp)
            Text(
                when {
                    !done -> "正在载入地图…"
                    m == null || m.mapFile.isBlank() -> "该地点暂无地图资料"
                    frac == null -> "该地点的坐标不在这张地图上"
                    else -> "地图资料暂时无法加载"
                },
                color = PhoneMuted, fontSize = 11.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        return
    }

    // 针为什么不跟主题、为什么是 token 而不是就地写，见 MapPin 的注释。
    // 和采集时钟的针同一个值 —— 以前这里和那边各写一遍 0xFFE0453D。
    val pin = MapPin
    Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp))) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = "${npc.mapName}地图",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
        )
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width * frac.first
            val cy = size.height * frac.second
            val r = size.minDimension * 0.032f
            drawCircle(pin.copy(alpha = 0.22f), r, Offset(cx, cy))
            drawCircle(pin, r, Offset(cx, cy), style = Stroke(2.5.dp.toPx()))
            drawCircle(pin, r * 0.28f, Offset(cx, cy))
        }
        Text(
            npc.placeText,
            color = Color.White, fontSize = 10.sp,
            modifier = Modifier.align(Alignment.BottomStart).padding(7.dp)
                .clip(RoundedCornerShape(4.dp)).background(CanvasLabelScrim)
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

// ---- 副本 ----

/**
 * 副本详情。**本地库优先**，本地没有才联网。
 *
 * 原来只走 [WikiRemote.instance]，于是没网时这一页是「副本 xxx 找不到」，
 * 而且搜索结果根本到不了这里 —— 副本命中的是 [WikiHit.Page]，直接跳浏览器了。
 * 现在 427 个副本在本地库里（[DutyDb]），离线也能看，还能列掉落并跳到物品详情。
 *
 * 联网那一路留着当兜底：本地库是发版时构建的，新补丁加的副本本地还没有。
 */
@Composable
internal fun WikiInstanceScreen(
    state: PhoneState,
    id: Int,
    onBack: () -> Unit,
    onOpen: (WikiDest) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var duty by remember(id) { mutableStateOf<WikiDuty?>(null) }
    var drops by remember(id) { mutableStateOf<List<Pair<DutyDrop, WikiItem?>>>(emptyList()) }
    var remote by remember(id) { mutableStateOf<WikiInstance?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var retry by remember(id) { mutableStateOf(0) }

    LaunchedEffect(id, retry) {
        loading = true
        val local = runCatching { DutyDb.byId(context, id) }.getOrNull()
        duty = local
        if (local != null) {
            // 掉落物品名从本地 items 表补，拿不到的（库里裁掉的）只显示 ID
            drops = runCatching {
                DutyDb.drops(context, id).map { d ->
                    d to WikiDb.byId(context, d.itemId)
                }
            }.getOrDefault(emptyList())
        } else {
            remote = runCatching { WikiRemote.instance(context, id) }.getOrNull()
        }
        loading = false
    }

    val d = duty
    val margin = LocalContentMargin.current
    when {
        loading -> WikiLoadingScreen("副本", state, onBack)
        d != null -> ScreenFrame {
            ScreenHeader(d.name.ifBlank { "副本 ${d.id}" }, state, onBack = onBack)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = margin.dp, end = margin.dp, bottom = 20.dp,
                ),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        d.type.takeIf { it.isNotBlank() }
                            ?.let { WikiChipBadge(it, PhoneAccent) }
                        d.levelText.takeIf { it.isNotBlank() }
                            ?.let { WikiChipBadge(it, PhoneInfo) }
                        d.timeText.takeIf { it.isNotBlank() }
                            ?.let { WikiChipBadge(it, PhoneWarn) }
                    }
                }
                item { WikiLinkSection("副本信息") }
                item {
                    WikiLinkFacts(
                        buildList {
                            (d.place.takeIf { it.isNotBlank() } ?: d.mapPlace)
                                .takeIf { it.isNotBlank() }?.let { add("地点" to it) }
                            if (d.ilvlMin > 0) add("最低品级" to d.ilvlMin.toString())
                            if (d.ilvlMax > 0) add("品级上限" to d.ilvlMax.toString())
                            d.sizeText.takeIf { it.isNotBlank() }?.let { add("人数" to it) }
                            d.partyText.takeIf { it.isNotBlank() }
                                ?.let { add("小队构成" to it) }
                            if (d.echoStack > 0) {
                                add("超越之力" to "每次失败叠加 ${d.echoStack}%")
                            }
                            add("中途加入" to if (d.joinMidway) "可以" else "不可")
                            if (d.unrestricted) add("人数限制解除" to "支持")
                            if (d.nameEn.isNotBlank()) add("英文名" to d.nameEn)
                            add("副本 ID" to d.id.toString())
                        },
                    )
                }
                if (d.bosses.isNotEmpty()) {
                    item { WikiLinkSection("BOSS") }
                    item {
                        Text(
                            d.bosses.joinToString("、"),
                            color = PhoneText, fontSize = 12.sp, lineHeight = 19.sp,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(PhoneSurface).padding(12.dp),
                        )
                    }
                }
                if (drops.isNotEmpty()) {
                    item { WikiLinkSection("掉落 ${drops.size} 件") }
                    lazyItems(drops, key = { "${it.first.kind}-${it.first.itemId}" }) { (drop, it2) ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpen(WikiDest.Item(drop.itemId)) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(34.dp).clip(RoundedCornerShape(6.dp))
                                    .background(PhoneSurface),
                                contentAlignment = Alignment.Center,
                            ) {
                                ItemIcon(
                                    it2?.iconId ?: 0, Modifier.fillMaxSize(),
                                    (it2?.nameCn ?: "?").take(2),
                                )
                            }
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(
                                    it2?.nameCn ?: "物品 ${drop.itemId}",
                                    color = PhoneAccent, fontSize = 13.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                val tags = buildList {
                                    if (drop.kind == 1) add("宝箱") else add("直接奖励")
                                    if (drop.chance) add("几率")
                                    if (drop.weekly) add("每周限定")
                                    if (drop.qty > 1) add("×${drop.qty}")
                                }
                                Text(
                                    tags.joinToString(" · "),
                                    color = PhoneMuted, fontSize = 10.sp,
                                )
                            }
                            ImageGlyph(
                                R.drawable.ic_chevron_right, PhoneMuted, Modifier.size(14.dp),
                            )
                        }
                    }
                }
                if (d.description.isNotBlank()) {
                    item { WikiLinkSection("简介") }
                    item {
                        Text(
                            d.description,
                            color = PhoneText, fontSize = 12.sp, lineHeight = 19.sp,
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .background(PhoneSurface).padding(12.dp),
                        )
                    }
                }
            }
        }
        // 本地没有（新补丁的副本），退回联网那一份
        remote != null -> {
            val i = remote!!
            ScreenFrame {
                ScreenHeader(i.name.ifBlank { "副本 ${i.id}" }, state, onBack = onBack)
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = margin.dp, end = margin.dp, bottom = 20.dp,
                    ),
                ) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            i.type.takeIf { it.isNotBlank() }
                                ?.let { WikiChipBadge(it, PhoneAccent) }
                            if (i.levelMin > 0) {
                                val lv = if (i.levelMax > i.levelMin)
                                    "Lv${i.levelMin}-${i.levelMax}" else "Lv${i.levelMin}"
                                WikiChipBadge(lv, PhoneInfo)
                            }
                            if (i.timeLimit > 0) WikiChipBadge("${i.timeLimit} 分钟", PhoneWarn)
                        }
                    }
                    item { WikiLinkSection("副本信息") }
                    item {
                        WikiLinkFacts(
                            buildList {
                                i.place.takeIf { it.isNotBlank() }?.let { add("地点" to it) }
                                if (i.ilvlMin > 0) add("最低品级" to i.ilvlMin.toString())
                                val party = buildList {
                                    if (i.partyTank > 0) add("坦 ${i.partyTank}")
                                    if (i.partyHealer > 0) add("治 ${i.partyHealer}")
                                    if (i.partyMelee > 0) add("近 ${i.partyMelee}")
                                    if (i.partyRanged > 0) add("远 ${i.partyRanged}")
                                }
                                if (party.isNotEmpty()) {
                                    add("小队构成" to party.joinToString(" / "))
                                }
                                add("副本 ID" to i.id.toString())
                            },
                        )
                    }
                    if (i.bosses.isNotEmpty()) {
                        item { WikiLinkSection("BOSS") }
                        item {
                            Text(
                                i.bosses.joinToString("、"),
                                color = PhoneText, fontSize = 12.sp, lineHeight = 19.sp,
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(PhoneSurface).padding(12.dp),
                            )
                        }
                    }
                    if (i.description.isNotBlank()) {
                        item { WikiLinkSection("简介") }
                        item {
                            Text(
                                i.description.replace("<br>", "\n").replace("<br/>", "\n"),
                                color = PhoneText, fontSize = 12.sp, lineHeight = 19.sp,
                                modifier = Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(PhoneSurface).padding(12.dp),
                            )
                        }
                    }
                }
            }
        }
        else -> WikiMissingScreen("副本 $id", state, onBack) { retry++ }
    }
}

// ---- 商店 ----

@Composable
internal fun WikiShopScreen(
    state: PhoneState,
    id: Int,
    onBack: () -> Unit,
    onOpen: (WikiDest) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    var shop by remember(id) { mutableStateOf<WikiShop?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var retry by remember(id) { mutableStateOf(0) }
    LaunchedEffect(id, retry) {
        loading = true
        shop = runCatching { WikiRemote.shop(context, id) }.getOrNull()
        loading = false
    }
    val s = shop
    val margin = LocalContentMargin.current
    when {
        loading -> WikiLoadingScreen("商店", state, onBack)
        s == null -> WikiMissingScreen("商店 $id", state, onBack) { retry++ }
        else -> ScreenFrame {
            ScreenHeader(s.name.ifBlank { "商店 ${s.id}" }, state, onBack = onBack)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = margin.dp, end = margin.dp, bottom = 20.dp,
                ),
            ) {
                if (s.condition.isNotBlank()) {
                    item { WikiLinkSection("购买条件") }
                    item { WikiLinkFacts(listOf("条件" to s.condition)) }
                }
                item { WikiLinkSection("商品 ${s.goods.size} 件") }
                lazyItems(s.goods, key = { it.itemId }) { g ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpen(WikiDest.Item(g.itemId)) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier.size(34.dp).clip(RoundedCornerShape(6.dp))
                                .background(PhoneSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            ItemIcon(g.iconId, Modifier.fillMaxSize(), g.name.take(2))
                        }
                        Text(
                            g.name, color = PhoneAccent, fontSize = 13.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 10.dp),
                        )
                        ImageGlyph(R.drawable.ic2_chevron_right, PhoneMuted,
                            Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// ---- 采集点 ----

@Composable
internal fun WikiNodeScreen(state: PhoneState, id: Int, onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    var node by remember(id) { mutableStateOf<WikiNode?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    LaunchedEffect(id) {
        node = runCatching { WikiDb.node(context, id) }.getOrNull()
        loading = false
    }
    val n = node
    val margin = LocalContentMargin.current
    when {
        loading -> WikiLoadingScreen("采集点", state, onBack)
        n == null -> WikiMissingScreen("采集点 $id", state, onBack)
        else -> ScreenFrame {
            ScreenHeader(n.placeText.ifBlank { "采集点 $id" }, state, onBack = onBack)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = margin.dp, end = margin.dp, bottom = 20.dp,
                ),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        if (n.level > 0) {
                            WikiChipBadge(
                                "Lv${n.level}" + "★".repeat(n.stars.coerceIn(0, 3)),
                                PhoneInfo,
                            )
                        }
                        if (n.isTimed) WikiChipBadge(n.windowText, PhoneWarn)
                    }
                }
                item { WikiLinkSection("位置") }
                item {
                    WikiLinkFacts(
                        buildList {
                            n.region.takeIf { it.isNotBlank() }?.let { add("地区" to it) }
                            n.mapName.takeIf { it.isNotBlank() }?.let { add("地图" to it) }
                            n.areaName.takeIf { it.isNotBlank() }?.let { add("区域" to it) }
                            if (n.x > 0 || n.y > 0) {
                                add("坐标" to "X:%.1f Y:%.1f".format(n.x, n.y))
                            }
                            n.folkloreName.takeIf { it.isNotBlank() }
                                ?.let { add("传承录" to it) }
                            add("采集点 ID" to n.id.toString())
                        },
                    )
                }
                if (n.isTimed) {
                    item {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 14.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(PhoneSurface).padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ImageGlyph(R.drawable.ic2_clock, PhoneMuted, Modifier.size(15.dp))
                            Text(
                                "这是限时采集点，倒计时和地图在「采集时钟」里。",
                                color = PhoneMuted, fontSize = 10.sp,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---- 共用小件 ----

@Composable
private fun WikiChipBadge(text: String, tint: Color) {
    Text(
        text, color = tint, fontSize = 10.sp, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(4.dp))
            .background(tint.copy(alpha = 0.14f))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

// 下面两个和 WikiScreens.kt 的 WikiSectionTitle / WikiFactCard 是同一个东西的
// 第二份拷贝（这个文件是后来拆出去的）。**排版参数必须跟着一起改** ——
// 物品详情和任务/副本/商店详情在同一个模块里，眉标字距和标签字号不一样的话，
// 点进去就看出来是两套。
//
// 没有合并成一份是因为两边都是 private，合并要提到 internal 并挪文件，
// 那是另一件事。改的时候两处一起改。

@Composable
private fun WikiLinkSection(title: String) {
    Text(
        title, color = PhoneMuted, fontSize = 10.sp, fontWeight = FontWeight.Medium,
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@Composable
private fun WikiLinkFacts(rows: List<Pair<String, String>>) {
    if (rows.isEmpty()) return
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(PhoneSurface)) {
        rows.forEachIndexed { i, (k, v) ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 问答关系靠色 + 字重 + 字号三个通道，见 WikiFactCard 的注释。
                Text(k, color = PhoneMuted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    v,
                    color = PhoneText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                    textAlign = TextAlign.End,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            if (i < rows.lastIndex) PhoneHairlineRow(12.dp)
        }
    }
}

/** 纯名字的可点列表（前置任务等）。 */
@Composable
private fun WikiLinkList(rows: List<Pair<String, Int>>, onClick: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(PhoneSurface)) {
        rows.forEachIndexed { i, (name, id) ->
            Row(
                Modifier.fillMaxWidth().clickable { onClick(id) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    name.ifBlank { "ID $id" }, color = PhoneAccent, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ImageGlyph(R.drawable.ic2_chevron_right, PhoneMuted, Modifier.size(14.dp))
            }
            if (i < rows.lastIndex) PhoneHairlineRow(12.dp)
        }
    }
}

/**
 * 带图标的物品可点列表。
 * [pairs] 里的名字可能是空的（站点给的是裸 ID），用 [names] 从本地库补。
 */
@Composable
private fun WikiItemLinkList(
    pairs: List<Pair<Int, String>>,
    names: Map<Int, WikiItem>,
    onClick: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).background(PhoneSurface)) {
        pairs.forEachIndexed { i, (id, rawName) ->
            val local = names[id]
            val label = rawName.ifBlank { local?.nameCn.orEmpty() }.ifBlank { "物品 $id" }
            Row(
                Modifier.fillMaxWidth().clickable { onClick(id) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                        .background(PhoneSurfaceRaised),
                    contentAlignment = Alignment.Center,
                ) {
                    ItemIcon(local?.iconId ?: 0, Modifier.fillMaxSize(), label.take(2))
                }
                Text(
                    label, color = PhoneAccent, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                ImageGlyph(R.drawable.ic2_chevron_right, PhoneMuted, Modifier.size(14.dp))
            }
            if (i < pairs.lastIndex) PhoneHairlineRow(12.dp)
        }
    }
}
