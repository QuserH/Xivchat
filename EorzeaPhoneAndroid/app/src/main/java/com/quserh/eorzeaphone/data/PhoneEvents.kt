package com.quserh.eorzeaphone.data

import androidx.compose.runtime.Immutable

data class GameFriend(
    val name: String,
    val world: String,
    val homeWorld: String = "",
    val freeCompany: String,
    val location: String,
    val online: Boolean,
    val job: String = "",
    val contentId: Long = 0,
    val currentWorldId: Int = 0,
    val homeWorldId: Int = 0,
    val classJobId: Int = 0,
    val status: Long = 0,
)

@Immutable
data class GameChatChunk(val text: String? = null, val icon: Int? = null, val italic: Boolean = false, val foreground: Long? = null)

@Immutable
data class GameChatMessage(
    val timestamp: Long,
    val sender: String,
    val text: String,
    val channel: Int,
    val self: Boolean = false,
    val chunks: List<GameChatChunk> = emptyList(),
    val sendState: Int = 0,
    val senderName: String? = null,
    val senderWorld: String? = null,
    val senderStatusName: String? = null,
    val senderStatusIcon: Int? = null,
    val senderWorldIcon: Int? = null,
    val characterTag: String? = null,
    val targetName: String? = null,
    val targetWorld: String? = null,
    val selfFlag: Boolean = false,
) {
    val category: ChatCategory get() = ChatCategory.fromChannel(channel)

    // 插件明确下发“这是自己发的”标志（自用情感动作等场景 sender 可能匹配不上）
    fun isSelfMessage(playerName: String?): Boolean = selfFlag || isFrom(playerName)

    fun isFrom(playerName: String?): Boolean {
        if (channel == 12) return true
        if (playerName.isNullOrBlank()) return false
        return sender.normalizedPlayerName() == playerName.normalizedPlayerName()
    }

    fun conversationKey(): String = when {
        channel == 27 || channel == 75 || channel == 94 -> "novice"
        // 统一用 sendName@世界 构造 key，避免私聊与情感动作生成不同 key 导致重命名只对其中一处生效
        category == ChatCategory.Tell -> "tell:${tellTarget().normalizedPlayerName()}"
        category == ChatCategory.Linkshell -> "linkshell:$channel"
        else -> category.name
    }

    fun displaySender(): String {
        val n = senderName?.takeIf { it.isNotBlank() }?.stripPlayerDecorations()
        val w = senderWorld?.takeIf { it.isNotBlank() }?.stripPlayerDecorations()
        if (!n.isNullOrBlank()) {
            if (w.isNullOrBlank()) return n
            val marker = sender.firstOrNull { it.code in 0xE000..0xF8FF || it in PlayerNameFlowers } ?: '\uE05D'
            return "$n$marker$w"
        }
        return sender.displayPlayerName()
    }

    fun tellTarget(): String {
        val n = senderName?.takeIf { it.isNotBlank() }?.stripPlayerDecorations()
        val w = senderWorld?.takeIf { it.isNotBlank() }
        if (!n.isNullOrBlank()) return if (w.isNullOrBlank()) n else "$n@$w"
        return sender.stripPlayerDecorations()
    }

    fun conversationTitle(): String = when {
        category == ChatCategory.Tell -> displaySender()
        category == ChatCategory.Linkshell -> linkshellChannelName(channel)
        else -> category.label
    }

    fun tellRecipient(): String = if (category == ChatCategory.Tell) tellTarget() else ""
}

enum class ChatCategory(val label: String) {
    Public("周围"),
    Party("队伍"),
    Team("团队"),
    Tell("私聊"),
    Linkshell("通讯贝"),
    FreeCompany("部队"),
    Emote("情感动作"),
    System("系统");

    companion object {
        fun fromChannel(channel: Int): ChatCategory = when (channel) {
            10, 11, 30, 81, 82, 83 -> Public
            14, 32, 36, 84 -> Party
            15 -> Team
            12, 13, 80 -> Tell
            in 16..23, in 86..93, 37, in 101..107 -> Linkshell
            24, 85 -> FreeCompany
            27, 94, 75 -> Public
            28, 29 -> Emote
            else -> System
        }
    }
}

internal fun linkshellChannelName(channel: Int): String = when (channel) {
    in 16..23 -> "通讯贝 ${channel - 15}"
    in 86..93 -> "通讯贝 ${channel - 85}"
    37 -> "跨服贝 1"
    in 38..44 -> "跨服贝 ${channel - 36}"
    in 101..107 -> "跨服贝 ${channel - 99}"
    else -> "通讯贝"
}

private val PlayerNameDecorations = charArrayOf(
    '★', '☆', '♡', '♥', '✿', '❀', '❁', '❃', '❈', '❉', '✽', '❊', '⚜', '＊', '*', ' ', '>', '<',
    '\ue090', '\ue091', '\ue092', '\ue093', '\ue094', '\ue095', '\ue096', '\ue097',
)

// 游戏里名字与服务器之间的分隔“小花”等符号，统一视为分隔符
private val PlayerNameSeparatorRegex = Regex("[\uE000-\uF8FF\u200B\u2060\u00AD\uFEFF\u273F-\u2741\u2743\u2744\u2764\u2765\u2766\u2767\u269C]+")

private val PlayerNamePuaRange = 0xE000..0xF8FF
private val PlayerNameFlowers = "❀✿❁❃❈❉✽❊❋🌸🌼🌺★☆♡♥⚜＊*"

internal fun String.stripPlayerDecorations(): String {
    var s = this.trim()
    // 去掉开头/结尾的装饰（含 PUA 符号与小花），避免出现 “❀角色名@服务器” 这类带前缀花的显示
    s = s.trimStart(*PlayerNameDecorations)
    s = s.trim()
    s = s.trimStart { it.code in PlayerNamePuaRange || it in PlayerNameFlowers }
    s = s.trimEnd { it.code in PlayerNamePuaRange || it in PlayerNameFlowers }
    s = s.replace(PlayerNameSeparatorRegex, "@")
    val at = s.indexOf('@')
    if (at > 0) {
        val name = s.substring(0, at).trimStart(*PlayerNameDecorations).trim()
        s = name + s.substring(at)
    }
    return s
}

internal fun String.normalizedPlayerName(): String = this
    .stripPlayerDecorations()
    .replace("@", "")
    .lowercase()

internal fun String.displayPlayerName(): String = stripPlayerDecorations().ifBlank { "对方" }

// 彻底移除玩家名里所有花/PUA 符号（不止首尾），避免出现 “❀角色名” 这种前置花；保留空格与普通字符
internal fun String.cleanPlayerName(): String {
    val src = this
    return buildString {
        for (ch in src) {
            if (ch.code in PlayerNamePuaRange || ch in PlayerNameFlowers) continue
            append(ch)
        }
    }.trim()
}
internal fun String.tellNamePart(): String = substringBefore('@').stripPlayerDecorations().trim().lowercase()
internal fun GameChatMessage.tellNamePart(): String =
    senderName?.takeIf { it.isNotBlank() }?.stripPlayerDecorations()?.lowercase()
        ?: sender.tellNamePart()

data class GameInventoryItem(
    val itemId: Long,
    val name: String,
    val quantity: Int,
    val container: Long,
    val slot: Long,
    val hq: Boolean,
    val iconId: Int = 0,
    val retainerId: Long = 0,
)

data class GameInventoryContainer(
    val type: Long,
    val size: Int,
)

data class GameRetainer(
    val id: Long,
    val name: String,
    val active: Boolean,
    val itemCount: Int,
    val quantity: Int,
    val gil: Long = 0,
    val ventureId: Long = 0,
    val ventureCompleteUnix: Long = 0,
)

data class GameInventorySnapshot(
    val items: List<GameInventoryItem>,
    val containers: List<GameInventoryContainer>,
    val retainers: List<GameRetainer> = emptyList(),
)

data class GameWalletEntry(
    val itemId: Long,
    val name: String,
    val amount: Long,
    val cap: Long,
    val section: String,
    val iconId: Int = 0,
)

data class GameWallet(
    val gil: Long,
    val entries: List<GameWalletEntry>,
)

data class PlayerProfile(
    val name: String,
    val homeWorld: String,
    val currentWorld: String,
    val location: String,
    val classJobId: Long = 0,
    val jobName: String = "",
    val level: Int = 0,
    val territoryId: Long = 0,
    val currentHp: Int = 0,
    val maxHp: Int = 0,
    val currentMp: Int = 0,
    val maxMp: Int = 0,
    val currentCp: Int = 0,
    val maxCp: Int = 0,
    val currentGp: Int = 0,
    val maxGp: Int = 0,
    val itemLevel: Int = 0,
)

data class GameWeatherWindow(val name: String, val minutesFromNow: Int, val eorzeaBell: Int)

data class GameWeather(val zone: String, val current: String, val forecast: List<GameWeatherWindow>)

data class GameJob(
    val jobId: Long,
    val name: String,
    val abbreviation: String,
    val category: String,
    val level: Int,
    val active: Boolean,
    val itemLevel: Int,
    val iconId: Int = 0,
    val gearsetId: Int = -1,
)

data class GameHousingLocation(
    val ward: Int?,
    val plot: Int?,
    val exterior: Boolean,
    val apartmentWing: Int?,
)

data class GameDailyEntry(
    val id: String,
    val label: String,
    val weekly: Boolean,
    val automatic: Boolean,
    val available: Boolean,
    val complete: Boolean,
    val remaining: Int,
    val goal: Int,
    val note: String,
)

data class GameDailies(
    val nextDailyResetUnix: Long,
    val nextWeeklyResetUnix: Long,
    val entries: List<GameDailyEntry>,
)

data class GameActivity(
    val sessionStartedUnix: Long,
    val sessionPlaySeconds: Long,
    val sessionExpGained: Long,
    val sessionLevelsGained: Int,
    val sessionGilEarned: Long,
    val sessionDutiesCompleted: Int,
    val todayPlaySeconds: Long,
    val todayExpGained: Long,
    val todayLevelsGained: Int,
    val todayGilEarned: Long,
    val todayDutiesCompleted: Int,
    val mountsOwned: Int,
    val mountsTotal: Int,
    val minionsOwned: Int,
    val minionsTotal: Int,
    val retainerCount: Int,
    val venturesReady: Int,
    val venturesActive: Int,
)

data class GameCollectionItem(
    val id: Long,
    val name: String,
    val iconId: Int = 0,
    val owned: Boolean = true,
)

data class GameCollectionCategory(
    val id: Int,
    val total: Int,
    val owned: Int,
    val items: List<GameCollectionItem>,
)

data class GameCollections(
    val categories: List<GameCollectionCategory>,
)

data class GameMapDestination(
    val rowId: Long,
    val name: String,
    val order: Int,
)

data class GameMapRegion(
    val name: String,
    val order: Int,
    val destinations: List<GameMapDestination>,
)

data class GameMapExpansion(
    val name: String,
    val order: Int,
    val regions: List<GameMapRegion>,
)

data class GameMaps(
    val currentZone: String,
    val currentRegion: String,
    val expansions: List<GameMapExpansion>,
)

data class GameFishingLog(
    val updatedUnix: Long,
    val fishBits: ByteArray,
    val spearfishBits: ByteArray,
)

data class GameSubmarineVessel(
    val name: String,
    val returnUnix: Long,
    val rankId: Int,
    val currentExp: Long,
    val nextLevelExp: Long,
)

data class GameSubmarine(
    val updatedUnix: Long,
    val vessels: List<GameSubmarineVessel>,
)

/**
 * One live listing from the game client.
 *
 * No world: the game's listing struct has no world field, so the whole result is
 * labelled with [GameMarket.currentWorldName] instead.
 */
data class GameMarketListing(
    val listingId: Long,
    val unitPrice: Int,
    val quantity: Int,
    val hq: Boolean,
    /**
     * Retainer city. Not the retainer *name* -- the game's listing struct has no such
     * field for ordinary listings, so there is nothing to show. Universalis rows do
     * carry names; only this local-board path cannot.
     */
    val townName: String,
    /** Tax in gil. Not included in unitPrice * quantity. */
    val tax: Int,
    /** Sold as a set: the quantity cannot be split. */
    val isSet: Boolean = false,
    val materiaCount: Int = 0,
) {
    val total: Int get() = unitPrice * quantity
    val totalWithTax: Int get() = total + tax
}

/** Mirrors the plugin's MarketStatus. */
enum class GameMarketStatus {
    Ok, NotLoggedIn, InDuty, BoardOpen, NotMarketable, Timeout, Unknown;

    companion object {
        fun of(code: Int): GameMarketStatus = entries.getOrElse(code) { Unknown }
    }
}

data class GameMarket(
    val updatedUnix: Long,
    val itemId: Int,
    val statusCode: Int,
    val listings: List<GameMarketListing>,
    val currentWorldName: String,
    /** What NPC shops charge; 0 = no shop sells it (older plugins always send 0). */
    val npcPrice: Int = 0,
) {
    val status: GameMarketStatus get() = GameMarketStatus.of(statusCode)
}

/**
 * Purchase statuses 0-5 line up with [GameMarketStatus] so the shared precondition
 * check can be reused. Everything from 6 up is purchase-specific.
 */
enum class GameMarketPurchaseStatus(val code: Int) {
    Ok(0), NotLoggedIn(1), InDuty(2), BoardOpen(3), NotMarketable(4),
    Timeout(5), ListingGone(6), Changed(7), NotEnoughGil(8), Refused(9),
    Disabled(10), Busy(11);

    companion object {
        fun of(code: Int) = entries.firstOrNull { it.code == code } ?: Timeout
    }
}

data class GameMarketPurchase(
    val updatedUnix: Long,
    val itemId: Int,
    val statusCode: Int,
    val listingId: Long,
    val unitPrice: Int,
    val quantity: Int,
    val tax: Int,
    val errorId: Int,
) {
    val status: GameMarketPurchaseStatus get() = GameMarketPurchaseStatus.of(statusCode)
}

/**
 * One market-board search category. [iconId] is the game's own
 * ItemSearchCategory icon, so category tiles can use the exact art the in-game
 * board grid shows. The tree is two-level, mirroring the in-game board:
 * top-level groups (武器/防具/素材/...) hold search subcategories (剑/斧/头/...).
 */
data class GameMarketCategory(
    val id: Int,
    val name: String,
    val order: Int = 0,
    val iconId: Int = 0,
    val subcategories: List<GameMarketSubcategory>,
) {
    /** Every item in the group, subcategory order preserved (level-sorted within). */
    val items: List<GameMarketItem> get() = subcategories.flatMap { it.items }
}

data class GameMarketSubcategory(
    val id: Int,
    val name: String,
    val order: Int = 0,
    val iconId: Int = 0,
    val items: List<GameMarketItem>,
)

data class GameMarketItem(
    val id: Int,
    val name: String,
    val iconId: Int = 0,
    val levelItem: Int = 0,
    val canBeHq: Boolean = false,
    /** NPC gil-shop price; zero means no real gil vendor sells this item. */
    val npcPrice: Int = 0,
)

data class GameMarketCategories(
    val categories: List<GameMarketCategory>,
    val timestampMs: Long = 0L,
    val gameVersion: String = "",
)

/** What the plugin's price monitor did; mirrors the plugin's MarketMonitorEventKind. */
enum class GameMonitorEventKind {
    Found, Purchased, BuyFailed, CapReached, Sync;

    companion object {
        fun of(code: Int): GameMonitorEventKind = entries.getOrElse(code) { Sync }
    }
}

data class GameMarketMonitorEvent(
    val updatedUnix: Long,
    val itemId: Int,
    val kindCode: Int,
    val price: Int,
    val quantity: Int,
    val detail: String,
) {
    val kind: GameMonitorEventKind get() = GameMonitorEventKind.of(kindCode)
}

/**
 * One monitor rule the phone pushes to the plugin (opcode 16). The plugin polls
 * the board for these on its own timer and, with auto-buy on, buys matching
 * listings through the same verified path as a manual purchase.
 */
data class MarketMonitorRule(
    val itemId: Int,
    val threshold: Int,
    val hqOnly: Boolean,
    val autoBuy: Boolean,
    val buyCap: Int,
)

/**
 * One ingredient in a crafting recipe.
 */
data class GameRecipeIngredient(
    val itemId: Int,
    val name: String,
    val amount: Int,
    val iconId: Int = 0,
)

/**
 * Complete crafting recipe for one item, returned from the plugin via Lumina.
 */
data class GameRecipe(
    val recipeId: Int,
    val itemId: Int,
    val itemName: String,
    val jobId: Int,
    val jobName: String,
    val recipeLevel: Int,
    val ingredients: List<GameRecipeIngredient>,
)

sealed interface PhoneEvent {
    data object Connected : PhoneEvent
    data class Disconnected(val reason: String) : PhoneEvent
    data class GameAvailability(val available: Boolean) : PhoneEvent
    data class Error(val message: String) : PhoneEvent
    data class FriendList(val friends: List<GameFriend>) : PhoneEvent
    data class PartyList(val members: List<GameFriend>) : PhoneEvent
    data class Chat(val message: GameChatMessage) : PhoneEvent
    data class Inventory(val snapshot: GameInventorySnapshot) : PhoneEvent
    data class Wallet(val wallet: GameWallet) : PhoneEvent
    data class Profile(val profile: PlayerProfile) : PhoneEvent
    data class Channel(val channel: Int, val name: String) : PhoneEvent
    data class Weather(val weather: GameWeather) : PhoneEvent
    data class Jobs(val jobs: List<GameJob>) : PhoneEvent
    data class Housing(val location: GameHousingLocation) : PhoneEvent
    data class Dailies(val dailies: GameDailies) : PhoneEvent
    data class Activity(val activity: GameActivity) : PhoneEvent
    data class Collections(val collections: GameCollections) : PhoneEvent
    data class Maps(val maps: GameMaps) : PhoneEvent
    data class Fishing(val log: GameFishingLog) : PhoneEvent
    data class Submarine(val submarine: GameSubmarine) : PhoneEvent
    data class Market(val market: GameMarket) : PhoneEvent
    data class MarketPurchase(val result: GameMarketPurchase) : PhoneEvent
    data class MarketCategories(val tree: GameMarketCategories) : PhoneEvent
    data class MarketMonitor(val event: GameMarketMonitorEvent) : PhoneEvent
    data class Recipe(val recipe: GameRecipe) : PhoneEvent
}
