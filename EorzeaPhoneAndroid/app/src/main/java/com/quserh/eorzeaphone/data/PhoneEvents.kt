package com.quserh.eorzeaphone.data

data class GameFriend(
    val name: String,
    val world: String,
    val freeCompany: String,
    val location: String,
    val online: Boolean,
    val job: String = "",
    val contentId: Long = 0,
    val currentWorldId: Int = 0,
    val homeWorldId: Int = 0,
)

data class GameChatMessage(
    val timestamp: Long,
    val sender: String,
    val text: String,
    val channel: Int,
) {
    val category: ChatCategory get() = ChatCategory.fromChannel(channel)

    fun isFrom(playerName: String?): Boolean {
        if (channel == 12) return true // TellOutgoing does not use the local player as its sender.
        if (playerName.isNullOrBlank()) return false
        return sender.normalizedPlayerName() == playerName.normalizedPlayerName()
    }
}

enum class ChatCategory(val label: String) {
    Public("周围"),
    Party("队伍"),
    Tell("私聊"),
    Linkshell("通讯贝"),
    FreeCompany("部队"),
    Emote("情感动作"),
    System("系统");

    companion object {
        fun fromChannel(channel: Int): ChatCategory = when (channel) {
            10, 11, 30 -> Public
            14, 15, 32 -> Party
            12, 13 -> Tell
            in 16..23, in 37..44, in 101..107, 27, 36 -> Linkshell
            24 -> FreeCompany
            28, 29 -> Emote
            else -> System
        }
    }
}

private fun String.normalizedPlayerName(): String = this
    .trim()
    .trimStart('>', '<', '\ue090', '\ue091', '\ue092', '\ue093', '\ue094', '\ue095', '\ue096', '\ue097')
    .substringBefore('@')
    .trim()
    .lowercase()

data class GameInventoryItem(
    val itemId: Long,
    val name: String,
    val quantity: Int,
    val container: Long,
    val slot: Long,
    val hq: Boolean,
    val iconId: Int = 0,
)

data class GameInventoryContainer(
    val type: Long,
    val size: Int,
)

data class GameInventorySnapshot(
    val items: List<GameInventoryItem>,
    val containers: List<GameInventoryContainer>,
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

sealed interface PhoneEvent {
    data object Connected : PhoneEvent
    data class Disconnected(val reason: String) : PhoneEvent
    data class Error(val message: String) : PhoneEvent
    data class FriendList(val friends: List<GameFriend>) : PhoneEvent
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
}
