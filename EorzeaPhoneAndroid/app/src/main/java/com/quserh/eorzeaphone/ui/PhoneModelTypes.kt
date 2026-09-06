package com.quserh.eorzeaphone.ui

import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.ChatCategory
import com.quserh.eorzeaphone.data.GameChatMessage
import java.text.NumberFormat
import java.util.Locale

// Shared phone UI models and immutable defaults. Keep these declarations independent from PhoneState storage.

enum class PhoneScreen {
    Home,
    Settings,
    Contacts,
    ContactDetail,
    Chat,
    App,
}

enum class SettingsPage {
    General,
    Appearance,
    Sound,
    Notifications,
}

enum class PhoneThemeMode(val label: String) {
    System("跟随系统"),
    Light("浅色"),
    Dark("深色"),
}

data class PhoneAppItem(
    val id: String,
    val label: String,
    @DrawableRes val icon: Int,
    val color: Color,
    val destination: PhoneScreen = PhoneScreen.App,
)

data class BuiltinConversationIcon(val id: String, val label: String, @DrawableRes val res: Int, val category: String = "")

val builtinConversationIcons = listOf(
    BuiltinConversationIcon("gpose_01", "贴纸1", R.drawable.gpose_01, "avatar"),
    BuiltinConversationIcon("gpose_02", "贴纸2", R.drawable.gpose_02, "avatar"),
    BuiltinConversationIcon("gpose_03", "贴纸3", R.drawable.gpose_03, "avatar"),
    BuiltinConversationIcon("gpose_04", "贴纸4", R.drawable.gpose_04, "avatar"),
    BuiltinConversationIcon("gpose_05", "贴纸5", R.drawable.gpose_05, "avatar"),
    BuiltinConversationIcon("gpose_06", "贴纸6", R.drawable.gpose_06, "avatar"),
    BuiltinConversationIcon("gpose_07", "贴纸7", R.drawable.gpose_07, "avatar"),
    BuiltinConversationIcon("gpose_08", "贴纸8", R.drawable.gpose_08, "avatar"),
    BuiltinConversationIcon("gpose_09", "贴纸9", R.drawable.gpose_09, "avatar"),
    BuiltinConversationIcon("gpose_10", "贴纸10", R.drawable.gpose_10, "avatar"),
    BuiltinConversationIcon("gpose_11", "贴纸11", R.drawable.gpose_11, "avatar"),
    BuiltinConversationIcon("gpose_12", "贴纸12", R.drawable.gpose_12, "avatar"),
    BuiltinConversationIcon("gpose_13", "贴纸13", R.drawable.gpose_13, "avatar"),
    BuiltinConversationIcon("gpose_14", "贴纸14", R.drawable.gpose_14, "avatar"),
    BuiltinConversationIcon("gpose_15", "贴纸15", R.drawable.gpose_15, "avatar"),
    BuiltinConversationIcon("gpose_16", "贴纸16", R.drawable.gpose_16, "avatar"),
    BuiltinConversationIcon("gpose_17", "贴纸17", R.drawable.gpose_17, "avatar"),
    BuiltinConversationIcon("gpose_18", "贴纸18", R.drawable.gpose_18, "avatar"),
    BuiltinConversationIcon("gpose_19", "贴纸19", R.drawable.gpose_19, "avatar"),
    BuiltinConversationIcon("gpose_20", "贴纸20", R.drawable.gpose_20, "avatar"),
    BuiltinConversationIcon("gpose_21", "贴纸21", R.drawable.gpose_21, "avatar"),
    BuiltinConversationIcon("gpose_22", "贴纸22", R.drawable.gpose_22, "avatar"),
    BuiltinConversationIcon("gpose_23", "贴纸23", R.drawable.gpose_23, "avatar"),
    BuiltinConversationIcon("gpose_24", "贴纸24", R.drawable.gpose_24, "avatar"),
    BuiltinConversationIcon("gpose_25", "贴纸25", R.drawable.gpose_25, "avatar"),
    BuiltinConversationIcon("gpose_26", "贴纸26", R.drawable.gpose_26, "avatar"),
    BuiltinConversationIcon("gpose_27", "贴纸27", R.drawable.gpose_27, "avatar"),
    BuiltinConversationIcon("gpose_28", "贴纸28", R.drawable.gpose_28, "avatar"),
    BuiltinConversationIcon("gpose_29", "贴纸29", R.drawable.gpose_29, "avatar"),
    BuiltinConversationIcon("gpose_30", "贴纸30", R.drawable.gpose_30, "avatar"),
    BuiltinConversationIcon("gpose_31", "贴纸31", R.drawable.gpose_31, "avatar"),
    BuiltinConversationIcon("status_01", "状态1", R.drawable.ic_status_01, "status"),
    BuiltinConversationIcon("status_02", "状态2", R.drawable.ic_status_02, "status"),
    BuiltinConversationIcon("status_03", "状态3", R.drawable.ic_status_03, "status"),
    BuiltinConversationIcon("status_04", "状态4", R.drawable.ic_status_04, "status"),
    BuiltinConversationIcon("status_05", "状态5", R.drawable.ic_status_05, "status"),
    BuiltinConversationIcon("status_06", "状态6", R.drawable.ic_status_06, "status"),
    BuiltinConversationIcon("status_07", "状态7", R.drawable.ic_status_07, "status"),
    BuiltinConversationIcon("status_08", "状态8", R.drawable.ic_status_08, "status"),
    BuiltinConversationIcon("status_09", "状态9", R.drawable.ic_status_09, "status"),
    BuiltinConversationIcon("status_10", "状态10", R.drawable.ic_status_10, "status"),
    BuiltinConversationIcon("status_11", "状态11", R.drawable.ic_status_11, "status"),
    BuiltinConversationIcon("status_12", "状态12", R.drawable.ic_status_12, "status"),
    BuiltinConversationIcon("status_13", "状态13", R.drawable.ic_status_13, "status"),
    BuiltinConversationIcon("status_14", "状态14", R.drawable.ic_status_14, "status"),
    BuiltinConversationIcon("status_15", "状态15", R.drawable.ic_status_15, "status"),
    BuiltinConversationIcon("status_16", "状态16", R.drawable.ic_status_16, "status"),
    BuiltinConversationIcon("status_17", "状态17", R.drawable.ic_status_17, "status"),
    BuiltinConversationIcon("status_18", "状态18", R.drawable.ic_status_18, "status"),
    BuiltinConversationIcon("status_19", "状态19", R.drawable.ic_status_19, "status"),
    BuiltinConversationIcon("status_20", "状态20", R.drawable.ic_status_20, "status"),
    BuiltinConversationIcon("status_21", "状态21", R.drawable.ic_status_21, "status"),
    BuiltinConversationIcon("status_22", "状态22", R.drawable.ic_status_22, "status"),
    BuiltinConversationIcon("status_23", "状态23", R.drawable.ic_status_23, "status"),
    BuiltinConversationIcon("status_24", "状态24", R.drawable.ic_status_24, "status"),
    BuiltinConversationIcon("status_25", "状态25", R.drawable.ic_status_25, "status"),
    BuiltinConversationIcon("status_26", "状态26", R.drawable.ic_status_26, "status"),
    BuiltinConversationIcon("status_27", "状态27", R.drawable.ic_status_27, "status"),
    BuiltinConversationIcon("status_28", "状态28", R.drawable.ic_status_28, "status"),
    BuiltinConversationIcon("status_29", "状态29", R.drawable.ic_status_29, "status"),
    BuiltinConversationIcon("status_30", "状态30", R.drawable.ic_status_30, "status"),
    BuiltinConversationIcon("status_31", "状态31", R.drawable.ic_status_31, "status"),
    BuiltinConversationIcon("status_32", "状态32", R.drawable.ic_status_32, "status"),
    BuiltinConversationIcon("status_33", "状态33", R.drawable.ic_status_33, "status"),
    BuiltinConversationIcon("status_34", "状态34", R.drawable.ic_status_34, "status"),
    BuiltinConversationIcon("status_35", "状态35", R.drawable.ic_status_35, "status"),
    BuiltinConversationIcon("status_36", "状态36", R.drawable.ic_status_36, "status"),
    BuiltinConversationIcon("status_37", "状态37", R.drawable.ic_status_37, "status"),
    BuiltinConversationIcon("status_38", "状态38", R.drawable.ic_status_38, "status"),
    BuiltinConversationIcon("status_39", "状态39", R.drawable.ic_status_39, "status"),
    BuiltinConversationIcon("status_40", "状态40", R.drawable.ic_status_40, "status"),
    BuiltinConversationIcon("status_41", "状态41", R.drawable.ic_status_41, "status"),
    BuiltinConversationIcon("status_42", "状态42", R.drawable.ic_status_42, "status"),
    BuiltinConversationIcon("status_43", "状态43", R.drawable.ic_status_43, "status"),
    BuiltinConversationIcon("status_44", "状态44", R.drawable.ic_status_44, "status"),
    BuiltinConversationIcon("status_45", "状态45", R.drawable.ic_status_45, "status"),
    BuiltinConversationIcon("status_46", "状态46", R.drawable.ic_status_46, "status"),
    BuiltinConversationIcon("status_47", "状态47", R.drawable.ic_status_47, "status"),
    BuiltinConversationIcon("status_48", "状态48", R.drawable.ic_status_48, "status"),
    BuiltinConversationIcon("status_49", "状态49", R.drawable.ic_status_49, "status"),
    BuiltinConversationIcon("status_50", "状态50", R.drawable.ic_status_50, "status"),
    BuiltinConversationIcon("status_51", "状态51", R.drawable.ic_status_51, "status"),
)

val defaultConversationIcons = listOf(
    BuiltinConversationIcon("party", "小队", R.drawable.msg_party, ""),
    BuiltinConversationIcon("fc", "部队", R.drawable.msg_fc, ""),
)

data class PhoneFriend(
    val name: String,
    val world: String,
    val homeWorld: String = "",
    val location: String = "",
    val online: Boolean,
    val job: String = "",
    val freeCompany: String = "",
    val contentId: Long = 0,
    val currentWorldId: Int = 0,
    val homeWorldId: Int = 0,
    val classJobId: Int = 0,
    val status: Long = 0,
)

data class ChatFilter(
    val id: String,
    val label: String,
    val categories: Set<ChatCategory>,
    val removable: Boolean = false,
    val channels: Set<Int> = emptySet(),
    val tintIndex: Int = 0,
    val sendChannel: Int? = null,
    val layout: ChatLayout = ChatLayout.Bubbles,
    val historyPolicy: ChatHistoryPolicy = ChatHistoryPolicy.ThirtyDays,
    val alertPolicy: ChatAlertPolicy = ChatAlertPolicy.Mentions,
) {
    fun matches(message: GameChatMessage): Boolean = message.channel in channels || message.category in categories
}

enum class ChatLayout { Bubbles, Compact }
enum class ChatHistoryPolicy { Off, Session, ThirtyDays, Forever }
enum class ChatAlertPolicy { All, Mentions, Off }
enum class TeleportStatus { Idle, Teleporting, Done }

internal fun formatCount(value: Long): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)
internal fun formatCount(value: Int): String = formatCount(value.toLong())
internal val excludedPhoneInventoryContainers = setOf(11000L, 12001L, 22001L)
internal fun isPhoneInventoryContainer(type: Long): Boolean = type !in excludedPhoneInventoryContainers

data class OutputChannel(val id: Int, val label: String)

val outputChannels = listOf(
    OutputChannel(1, "说话"), OutputChannel(2, "小队"), OutputChannel(3, "团队"),
    OutputChannel(4, "喊话"), OutputChannel(5, "呼喊"), OutputChannel(6, "部队"),
    OutputChannel(8, "新人频道"), OutputChannel(9, "跨服通讯贝 1"),
    OutputChannel(10, "跨服通讯贝 2"), OutputChannel(11, "跨服通讯贝 3"),
    OutputChannel(12, "跨服通讯贝 4"), OutputChannel(13, "跨服通讯贝 5"),
    OutputChannel(14, "跨服通讯贝 6"), OutputChannel(15, "跨服通讯贝 7"),
    OutputChannel(16, "跨服通讯贝 8"), OutputChannel(19, "通讯贝 1"),
    OutputChannel(20, "通讯贝 2"), OutputChannel(21, "通讯贝 3"),
    OutputChannel(22, "通讯贝 4"), OutputChannel(23, "通讯贝 5"),
    OutputChannel(24, "通讯贝 6"), OutputChannel(25, "通讯贝 7"),
    OutputChannel(26, "通讯贝 8"),
)

class ChatConversation(
    val key: String,
    val category: ChatCategory,
    title: String,
    var tellRecipient: String = "",
) {
    var title by mutableStateOf(title)
    val messages = mutableStateListOf<GameChatMessage>()
    var unread by mutableStateOf(0)
    var notify by mutableStateOf(true)
    private var _lastMessage by mutableStateOf<GameChatMessage?>(null)
    val lastMessage: GameChatMessage? get() = _lastMessage
    val lastTimestamp: Long? get() = _lastMessage?.timestamp

    fun add(message: GameChatMessage) {
        messages.add(message)
        _lastMessage = message
    }

    fun addAll(incoming: Collection<GameChatMessage>) {
        if (incoming.isEmpty()) return
        messages.addAll(incoming)
        _lastMessage = incoming.last()
    }

    fun replaceMessages(incoming: Collection<GameChatMessage>) {
        messages.clear()
        if (incoming.isEmpty()) {
            _lastMessage = null
        } else {
            messages.addAll(incoming)
            _lastMessage = incoming.last()
        }
    }

    fun clear() {
        messages.clear()
        _lastMessage = null
        unread = 0
    }
}

data class CustomShortcut(val name: String, val command: String)

data class LocalNote(
    val id: Long,
    val body: String,
    val updatedAt: Long,
)

data class LocalReminder(
    val id: Long,
    val title: String,
    val dueAt: Long?,
    val done: Boolean,
)

data class SavedCharacter(val key: String, val name: String, val world: String)

val defaultShortcuts = listOf(
    CustomShortcut("返回", "/return"),
    CustomShortcut("坐骑随机", "/mount \"随机坐骑\""),
    CustomShortcut("跟随目标", "/follow"),
    CustomShortcut("准备确认", "/readycheck"),
    CustomShortcut("倒计时 10 秒", "/countdown 10"),
    CustomShortcut("离开队伍", "/leave"),
)
