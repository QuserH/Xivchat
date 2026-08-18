package com.quserh.eorzeaphone.ui

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.quserh.eorzeaphone.data.GameChatMessage
import com.quserh.eorzeaphone.data.ChatCategory
import com.quserh.eorzeaphone.data.GameInventoryContainer
import com.quserh.eorzeaphone.data.GameInventoryItem
import com.quserh.eorzeaphone.data.GameWallet
import com.quserh.eorzeaphone.data.GameWeather
import com.quserh.eorzeaphone.data.GameJob
import com.quserh.eorzeaphone.data.GameHousingLocation
import com.quserh.eorzeaphone.data.GameDailies
import com.quserh.eorzeaphone.data.GameActivity
import com.quserh.eorzeaphone.data.GameCollections
import com.quserh.eorzeaphone.data.PhoneEvent
import com.quserh.eorzeaphone.data.XivChatConnection
import com.quserh.eorzeaphone.data.PhoneNotifier
import com.quserh.eorzeaphone.data.ResetReminderReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

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

data class PhoneAppItem(
    val id: String,
    val label: String,
    @DrawableRes val icon: Int,
    val color: Color,
    val destination: PhoneScreen = PhoneScreen.App,
)

data class PhoneFriend(
    val name: String,
    val world: String,
    val location: String = "",
    val online: Boolean,
    val job: String = "",
    val freeCompany: String = "",
    val contentId: Long = 0,
    val currentWorldId: Int = 0,
    val homeWorldId: Int = 0,
)

data class ChatFilter(
    val id: String,
    val label: String,
    val categories: Set<ChatCategory>,
    val removable: Boolean = false,
) {
    fun matches(message: GameChatMessage): Boolean = message.category in categories
}

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

private val builtInChatFilters = listOf(
    ChatFilter("all", "全部", ChatCategory.entries.toSet()),
    ChatFilter("chat", "聊天", setOf(ChatCategory.Public, ChatCategory.Emote)),
    ChatFilter("party", "队伍", setOf(ChatCategory.Party)),
    ChatFilter("tell", "私聊", setOf(ChatCategory.Tell)),
    ChatFilter("social", "部队/通讯贝", setOf(ChatCategory.FreeCompany, ChatCategory.Linkshell)),
    ChatFilter("system", "系统", setOf(ChatCategory.System)),
)

data class CustomShortcut(val name: String, val command: String)

val defaultShortcuts = listOf(
    CustomShortcut("返回", "/return"),
    CustomShortcut("坐骑随机", "/mount \"随机坐骑\""),
    CustomShortcut("跟随目标", "/follow"),
    CustomShortcut("准备确认", "/readycheck"),
    CustomShortcut("倒计时 10 秒", "/countdown 10"),
    CustomShortcut("离开队伍", "/leave"),
)

class PhoneState(context: Context, scope: CoroutineScope) {
    private val prefs = context.getSharedPreferences("eorzea_phone_ui", Context.MODE_PRIVATE)
    private val appContext = context.applicationContext
    private val activityRef = context as? android.app.Activity
    var screen by mutableStateOf(PhoneScreen.Home)
    var selectedApp by mutableStateOf<PhoneAppItem?>(null)
    var selectedFriend by mutableStateOf<PhoneFriend?>(null)
    var launchPivotX by mutableStateOf(0.5f)
        private set
    var launchPivotY by mutableStateOf(0.5f)
        private set
    var launchScale by mutableStateOf(0.14f)
        private set
    private var shellWidth = 1f
    private var shellHeight = 1f

    // ---- Home layout (editable, persisted) ----
    private val allApps by lazy {
        (AppCatalog.firstPage + AppCatalog.secondPage + AppCatalog.dock).associateBy { it.id }
    }
    var homeEditMode by mutableStateOf(false)
    var homePageIds by mutableStateOf(loadHomeLayout())
        private set
    var homeLibraryIds by mutableStateOf(loadHomeLibrary())
        private set

    val homePageCount: Int get() = homePageIds.size

    fun appsForPage(page: Int): List<PhoneAppItem> =
        homePageIds.getOrElse(page) { emptyList() }.mapNotNull { allApps[it] }

    fun homeLibraryApps(): List<PhoneAppItem> =
        homeLibraryIds.mapNotNull { allApps[it] }

    fun reorderHome(page: Int, fromId: String, toId: String) {
        if (page !in homePageIds.indices || fromId == toId) return
        val current = homePageIds[page].toMutableList()
        val fromIndex = current.indexOf(fromId)
        val toIndex = current.indexOf(toId)
        if (fromIndex < 0 || toIndex < 0) return
        current.removeAt(fromIndex)
        current.add(toIndex, fromId)
        homePageIds = homePageIds.toMutableList().also { it[page] = current }
        saveHomeLayout()
    }

    fun removeFromHome(page: Int, id: String) {
        if (page !in homePageIds.indices) return
        val current = homePageIds[page].toMutableList()
        if (!current.remove(id)) return
        homePageIds = homePageIds.toMutableList().also { it[page] = current }
        homeLibraryIds = (listOf(id) + homeLibraryIds.filterNot { it == id }).distinct()
        saveHomeLayout()
        saveHomeLibrary()
    }

    fun restoreToHome(page: Int, id: String) {
        if (page !in homePageIds.indices || id !in homeLibraryIds) return
        homeLibraryIds = homeLibraryIds.filterNot { it == id }
        homePageIds = homePageIds.toMutableList().also { it[page] = it[page] + id }
        saveHomeLayout()
        saveHomeLibrary()
    }

    fun exitEditMode() {
        homeEditMode = false
    }

    private fun loadHomeLayout(): List<List<String>> {
        val defaultValue = listOf(AppCatalog.firstPage.map { it.id }, AppCatalog.secondPage.map { it.id })
        val saved = runCatching {
            val root = JSONObject(prefs.getString("homeLayout", ""))
            val pages = root.optJSONArray("pages")
            if (pages == null || pages.length() == 0) null else {
                val result: MutableList<List<String>> = mutableListOf()
                for (pageIndex in 0 until pages.length()) {
                    val arr = pages.optJSONArray(pageIndex)
                    if (arr == null) continue
                    val ids: MutableList<String> = mutableListOf()
                    for (i in 0 until arr.length()) {
                        val id = arr.optString(i)
                        if (allApps.containsKey(id)) ids.add(id)
                    }
                    result.add(ids)
                }
                result
            }
        }.getOrNull() ?: return defaultValue
        if (saved.isEmpty()) return defaultValue
        return saved
    }

    private fun saveHomeLayout() {
        prefs.edit().putString("homeLayout", JSONObject().apply {
            put("pages", JSONArray(homePageIds.map { page -> JSONArray(page) }))
        }.toString()).apply()
    }

    private fun loadHomeLibrary(): List<String> = runCatching {
        val arr = JSONArray(prefs.getString("homeLibrary", "[]"))
        val result: MutableList<String> = mutableListOf()
        for (i in 0 until arr.length()) {
            val id = arr.optString(i)
            if (allApps.containsKey(id)) result.add(id)
        }
        result
    }.getOrDefault(emptyList())

    private fun saveHomeLibrary() {
        prefs.edit().putString("homeLibrary", JSONArray(homeLibraryIds).toString()).apply()
    }

    var connected by mutableStateOf(false)
    var serverLabel by mutableStateOf("未连接游戏")
    var host by mutableStateOf("127.0.0.1")
    var port by mutableStateOf("14777")
    var statusMessage by mutableStateOf("")
    var sessionStartedAt by mutableStateOf<Long?>(null)
    var sessionGilBaseline by mutableStateOf<Long?>(null)
    var chatNotifications by mutableStateOf(prefs.getBoolean("chatNotifications", true))
    var tellNotifications by mutableStateOf(prefs.getBoolean("tellNotifications", true))
    var resetNotifications by mutableStateOf(prefs.getBoolean("resetNotifications", true))
    private val _doNotDisturb = boolPref("doNotDisturb", false)
    var doNotDisturb: Boolean
        get() = _doNotDisturb.value
        set(value) { _doNotDisturb.value = value }
    private val _lockPosition = boolPref("lockPosition", false)
    var lockPosition: Boolean
        get() = _lockPosition.value
        set(value) { _lockPosition.value = value }
    private val _screenSwipe = boolPref("screenSwipe", true)
    var screenSwipe: Boolean
        get() = _screenSwipe.value
        set(value) { _screenSwipe.value = value }
    private val _showEmotes = boolPref("showEmotes", true)
    var showEmotes: Boolean
        get() = _showEmotes.value
        set(value) { _showEmotes.value = value }
    private val _keepScreenOn = boolPref("keepScreenOn", false)
    var keepScreenOn: Boolean
        get() = _keepScreenOn.value
        set(value) { _keepScreenOn.value = value }
    private val _haptics = boolPref("haptics", true)
    var haptics: Boolean
        get() = _haptics.value
        set(value) { _haptics.value = value }
    private val _reducedMotion = boolPref("reducedMotion", false)
    var reducedMotion: Boolean
        get() = _reducedMotion.value
        set(value) { _reducedMotion.value = value }
    private val _compactDock = boolPref("compactDock", false)
    var compactDock: Boolean
        get() = _compactDock.value
        set(value) { _compactDock.value = value }
    var settingsPage by mutableStateOf<SettingsPage?>(null)
    val chats = mutableStateListOf<GameChatMessage>()
    val inventory = mutableStateListOf<GameInventoryItem>()
    val inventoryContainers = mutableStateListOf<GameInventoryContainer>()
    var wallet by mutableStateOf<GameWallet?>(null)
    var weather by mutableStateOf<GameWeather?>(null)
    val jobs = mutableStateListOf<GameJob>()
    var housing by mutableStateOf<GameHousingLocation?>(null)
    var dailies by mutableStateOf<GameDailies?>(null)
    var activity by mutableStateOf<GameActivity?>(null)
    var collections by mutableStateOf<GameCollections?>(null)
    var profile by mutableStateOf<com.quserh.eorzeaphone.data.PlayerProfile?>(null)
    var noteText by mutableStateOf(prefs.getString("noteText", "").orEmpty())
    var customShortcuts by mutableStateOf(loadCustomShortcuts())
    var chatDraft by mutableStateOf("")
    var currentChannel by mutableStateOf(1)
    var currentChannelName by mutableStateOf("说话")
    var selectedChatFilterId by mutableStateOf("all")
    val chatFilters = mutableStateListOf<ChatFilter>().apply {
        addAll(builtInChatFilters)
        addAll(loadCustomFilters())
    }
    private val connection = XivChatConnection(context, scope) { event ->
        scope.launch(Dispatchers.Main.immediate) { handle(event) }
    }
    private val notifier = PhoneNotifier(context.applicationContext)

    private fun boolPref(key: String, default: Boolean): MutableState<Boolean> {
        val backing = mutableStateOf(prefs.getBoolean(key, default))
        return object : MutableState<Boolean> by backing {
            override var value: Boolean
                get() = backing.value
                set(v) {
                    backing.value = v
                    prefs.edit().putBoolean(key, v).apply()
                }
        }
    }

    init { ResetReminderReceiver.configure(appContext, resetNotifications) }
    val friends = mutableStateListOf<PhoneFriend>().apply { addAll(loadFriends()) }

    fun updateShellSize(width: Int, height: Int) {
        shellWidth = width.coerceAtLeast(1).toFloat()
        shellHeight = height.coerceAtLeast(1).toFloat()
    }

    fun open(app: PhoneAppItem, origin: Rect? = null) {
        if (origin != null) {
            launchPivotX = (origin.center.x / shellWidth).coerceIn(0.05f, 0.95f)
            launchPivotY = (origin.center.y / shellHeight).coerceIn(0.05f, 0.95f)
            launchScale = maxOf(origin.width / shellWidth, origin.height / shellHeight).coerceIn(0.10f, 0.24f)
        }
        selectedApp = app
        screen = app.destination
    }

    fun showMessagesTab(contacts: Boolean) {
        selectedFriend = null
        selectedApp = AppCatalog.dock.firstOrNull { it.destination == if (contacts) PhoneScreen.Contacts else PhoneScreen.Chat }
        screen = if (contacts) PhoneScreen.Contacts else PhoneScreen.Chat
        if (contacts) refreshFriends()
    }

    fun home() {
        screen = PhoneScreen.Home
        selectedApp = null
    }

    fun back() {
        when {
            settingsPage != null -> settingsPage = null
            screen == PhoneScreen.ContactDetail -> {
                screen = PhoneScreen.Contacts
                selectedFriend = null
            }
            else -> home()
        }
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        val activity = activityRef ?: return
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
    }

    fun openFriend(friend: PhoneFriend) {
        selectedFriend = friend
        screen = PhoneScreen.ContactDetail
    }

    fun connect() {
        val parsedPort = port.toIntOrNull()
        if (parsedPort == null || parsedPort !in 1..65535) {
            statusMessage = "端口无效"
            return
        }
        statusMessage = "正在连接…"
        connection.connect(host, parsedPort)
    }

    fun disconnect() = connection.disconnect()

    fun sendChat(text: String) = connection.sendChat(text)

    fun saveNote(text: String) {
        noteText = text
        prefs.edit().putString("noteText", text).apply()
    }

    fun addShortcut(name: String, command: String) {
        val cleanName = name.trim().take(16)
        val cleanCommand = command.trim().take(64)
        if (cleanName.isBlank() || cleanCommand.isBlank()) return
        customShortcuts = (customShortcuts + CustomShortcut(cleanName, cleanCommand)).distinctBy { it.command }
        saveCustomShortcuts()
    }

    fun removeShortcut(command: String) {
        customShortcuts = customShortcuts.filterNot { it.command == command }
        saveCustomShortcuts()
    }

    fun resetShortcuts() {
        customShortcuts = defaultShortcuts
        saveCustomShortcuts()
    }

    private fun loadCustomShortcuts(): List<CustomShortcut> {
        val restored = runCatching {
            val arr = JSONArray(prefs.getString("customShortcuts", "[]"))
            val result: MutableList<CustomShortcut> = mutableListOf()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val name = obj.optString("name").trim().take(16)
                val command = obj.optString("command").trim().take(64)
                if (name.isNotBlank() && command.isNotBlank()) result.add(CustomShortcut(name, command))
            }
            result
        }.getOrNull()
        if (restored == null || restored.isEmpty()) return defaultShortcuts
        return (defaultShortcuts + restored).distinctBy { it.command }
    }

    private fun saveCustomShortcuts() {
        prefs.edit().putString("customShortcuts", JSONArray(customShortcuts.map { JSONObject().put("name", it.name).put("command", it.command) }).toString()).apply()
    }

    fun isDailyChecked(id: String, weekly: Boolean): Boolean {
        val reset = if (weekly) dailies?.nextWeeklyResetUnix else dailies?.nextDailyResetUnix
        if (reset == null) return false
        return prefs.getStringSet("dailyChecks", emptySet()).orEmpty().contains("$id:$reset")
    }

    fun toggleDaily(id: String, weekly: Boolean) {
        val reset = (if (weekly) dailies?.nextWeeklyResetUnix else dailies?.nextDailyResetUnix) ?: return
        val key = "$id:$reset"
        val values = prefs.getStringSet("dailyChecks", emptySet()).orEmpty().toMutableSet()
        if (!values.add(key)) values.remove(key)
        prefs.edit().putStringSet("dailyChecks", values).apply()
        dailies = dailies?.copy(entries = dailies?.entries.orEmpty().toList())
    }

    fun updateChatNotifications(value: Boolean) { chatNotifications = value; prefs.edit().putBoolean("chatNotifications", value).apply() }
    fun updateTellNotifications(value: Boolean) { tellNotifications = value; prefs.edit().putBoolean("tellNotifications", value).apply() }
    fun updateResetNotifications(value: Boolean) { resetNotifications = value; prefs.edit().putBoolean("resetNotifications", value).apply(); ResetReminderReceiver.configure(appContext, value) }

    fun changeChannel(channel: OutputChannel) {
        connection.changeChannel(channel.id)
        currentChannel = channel.id
        currentChannelName = channel.label
    }

    fun startTell(friend: PhoneFriend) {
        val recipient = if (friend.world.isBlank()) friend.name else "${friend.name}@${friend.world}"
        chatDraft = "/tell $recipient "
        selectedChatFilterId = "tell"
        selectedApp = AppCatalog.dock.first()
        screen = PhoneScreen.Chat
    }

    fun friendAction(friend: PhoneFriend, action: Int) {
        if (friend.contentId == 0L) {
            statusMessage = "该好友缺少游戏角色标识，请刷新好友列表"
            return
        }
        connection.friendAction(action, friend.contentId, friend.currentWorldId)
        statusMessage = "操作已发送到游戏"
    }

    fun addChatFilter(label: String, categories: Set<ChatCategory>) {
        val cleanLabel = label.trim().replace("|", "").replace(";", "").take(12)
        if (cleanLabel.isBlank() || categories.isEmpty()) return
        chatFilters += ChatFilter("custom-${System.currentTimeMillis()}", cleanLabel, categories, true)
        saveCustomFilters()
    }

    fun removeChatFilter(filter: ChatFilter) {
        if (!filter.removable) return
        chatFilters.remove(filter)
        if (selectedChatFilterId == filter.id) selectedChatFilterId = "all"
        saveCustomFilters()
    }

    fun refreshFriends() = connection.requestFriends()

    fun clearTrustedServer() = connection.clearTrustedServer()

    private fun handle(event: PhoneEvent) {
        when (event) {
            PhoneEvent.Connected -> {
                connected = true
                sessionStartedAt = System.currentTimeMillis()
                sessionGilBaseline = wallet?.gil
                serverLabel = "已连接游戏"
                statusMessage = "连接成功"
            }
            is PhoneEvent.Disconnected -> {
                connected = false
                serverLabel = "未连接游戏"
                for (index in friends.indices) friends[index] = friends[index].copy(online = false, location = "", job = "")
                if (statusMessage.isBlank() || statusMessage == "连接成功") statusMessage = event.reason
            }
            is PhoneEvent.Error -> statusMessage = event.message
            is PhoneEvent.FriendList -> {
                friends.clear()
                friends.addAll(event.friends.map { PhoneFriend(it.name, it.world, it.location, it.online, it.job, it.freeCompany, it.contentId, it.currentWorldId, it.homeWorldId) })
                saveFriends()
            }
            is PhoneEvent.Chat -> {
                if (chats.none { it.timestamp == event.message.timestamp && it.sender == event.message.sender && it.text == event.message.text }) {
                    chats.add(event.message)
                    if (chatNotifications && !event.message.isFrom(profile?.name)) notifier.chat(event.message, tellNotifications && event.message.category == ChatCategory.Tell)
                }
            }
            is PhoneEvent.Inventory -> {
                inventory.clear()
                inventory.addAll(event.snapshot.items)
                inventoryContainers.clear()
                inventoryContainers.addAll(event.snapshot.containers)
            }
            is PhoneEvent.Wallet -> {
                wallet = event.wallet
                if (sessionGilBaseline == null) sessionGilBaseline = event.wallet.gil
            }
            is PhoneEvent.Weather -> weather = event.weather
            is PhoneEvent.Jobs -> {
                jobs.clear()
                jobs.addAll(event.jobs)
            }
            is PhoneEvent.Housing -> housing = event.location
            is PhoneEvent.Dailies -> dailies = event.dailies
            is PhoneEvent.Activity -> activity = event.activity
            is PhoneEvent.Collections -> collections = event.collections
            is PhoneEvent.Profile -> profile = event.profile
            is PhoneEvent.Channel -> {
                currentChannel = event.channel
                currentChannelName = event.name.ifBlank { outputChannels.firstOrNull { it.id == event.channel }?.label ?: "频道 ${event.channel}" }
                serverLabel = "$currentChannelName · 已连接"
            }
        }
    }

    private fun loadCustomFilters(): List<ChatFilter> = prefs.getString("chatFilters", "")
        .orEmpty()
        .split(';')
        .mapNotNull { encoded ->
            val fields = encoded.split('|', limit = 3)
            if (fields.size != 3) return@mapNotNull null
            val categories = fields[2].split(',').mapNotNull { value -> ChatCategory.entries.firstOrNull { it.name == value } }.toSet()
            if (fields[1].isBlank() || categories.isEmpty()) null else ChatFilter(fields[0], fields[1], categories, true)
        }

    private fun saveCustomFilters() {
        val encoded = chatFilters.filter { it.removable }.joinToString(";") { filter ->
            "${filter.id}|${filter.label}|${filter.categories.joinToString(",") { it.name }}"
        }
        prefs.edit().putString("chatFilters", encoded).apply()
    }

    private fun loadFriends(): List<PhoneFriend> = runCatching {
        val array = JSONArray(prefs.getString("friendCache", "[]"))
        buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val name = item.optString("name").trim()
                if (name.isBlank()) continue
                add(PhoneFriend(
                    name = name,
                    world = item.optString("world"),
                    online = false,
                    freeCompany = item.optString("freeCompany"),
                    contentId = item.optLong("contentId"),
                    currentWorldId = item.optInt("currentWorldId"),
                    homeWorldId = item.optInt("homeWorldId"),
                ))
            }
        }
    }.getOrDefault(emptyList())

    private fun saveFriends() {
        val array = JSONArray()
        friends.forEach { friend ->
            array.put(JSONObject().apply {
                put("name", friend.name)
                put("world", friend.world)
                put("freeCompany", friend.freeCompany)
                put("contentId", friend.contentId)
                put("currentWorldId", friend.currentWorldId)
                put("homeWorldId", friend.homeWorldId)
            })
        }
        prefs.edit().putString("friendCache", array.toString()).apply()
    }
}
