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
import com.quserh.eorzeaphone.data.GameDailyEntry
import com.quserh.eorzeaphone.data.GameInventoryContainer
import com.quserh.eorzeaphone.data.GameInventoryItem
import com.quserh.eorzeaphone.data.GameWallet
import com.quserh.eorzeaphone.data.GameWalletEntry
import com.quserh.eorzeaphone.data.GameWeather
import com.quserh.eorzeaphone.data.GameWeatherWindow
import com.quserh.eorzeaphone.data.GameJob
import com.quserh.eorzeaphone.data.GameHousingLocation
import com.quserh.eorzeaphone.data.GameDailies
import com.quserh.eorzeaphone.data.GameActivity
import com.quserh.eorzeaphone.data.GameCollections
import com.quserh.eorzeaphone.data.PhoneEvent
import com.quserh.eorzeaphone.data.XivChatConnection
import com.quserh.eorzeaphone.data.PhoneNotifier
import com.quserh.eorzeaphone.data.ResetReminderReceiver
import com.quserh.eorzeaphone.data.normalizedPlayerName
import com.quserh.eorzeaphone.data.displayPlayerName
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

class ChatConversation(
    val key: String,
    val category: ChatCategory,
    val title: String,
    val tellRecipient: String = "",
) {
    val messages = mutableStateListOf<GameChatMessage>()
    var unread by mutableStateOf(0)
    var notify by mutableStateOf(true)
    private var _lastMessage: GameChatMessage? = null
    val lastMessage: GameChatMessage? get() = _lastMessage
    val lastTimestamp: Long? get() = _lastMessage?.timestamp

    fun add(message: GameChatMessage) {
        messages.add(message)
        _lastMessage = message
    }
}

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
    var homePage by mutableStateOf(0)
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
    private val _host = mutableStateOf(prefs.getString("host", "127.0.0.1").orEmpty())
    var host: String
        get() = _host.value
        set(value) { _host.value = value; prefs.edit().putString("host", value).apply() }
    private val _port = mutableStateOf(prefs.getString("port", "14777").orEmpty())
    var port: String
        get() = _port.value
        set(value) { _port.value = value; prefs.edit().putString("port", value).apply() }
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
    var profile by mutableStateOf(loadProfileCache())
    var noteText by mutableStateOf(prefs.getString("noteText", "").orEmpty())
    var customShortcuts by mutableStateOf(loadCustomShortcuts())
    var chatDraft by mutableStateOf("")
    private val _chatWrapChars = mutableStateOf(prefs.getInt("chatWrapChars", 20))
    var chatWrapChars: Int
        get() = _chatWrapChars.value
        set(value) { _chatWrapChars.value = value; prefs.edit().putInt("chatWrapChars", value).apply() }
    var currentChannel by mutableStateOf(1)
    var currentChannelName by mutableStateOf("说话")
    var selectedChatFilterId by mutableStateOf("all")
    val chatFilters = mutableStateListOf<ChatFilter>().apply {
        addAll(builtInChatFilters)
        addAll(loadCustomFilters())
    }
    val conversations = mutableStateListOf<ChatConversation>()
    private val conversationByKey = mutableMapOf<String, ChatConversation>()
    var openConversationKey by mutableStateOf<String?>(null)
    private val mutedConversations: MutableSet<String> =
        (prefs.getStringSet("mutedChatConvs", emptySet()) ?: emptySet()).toMutableSet()
    private val pendingSelfTexts = mutableMapOf<String, String>()
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

    init {
        ResetReminderReceiver.configure(appContext, resetNotifications)
        loadSavedChats()
        loadSavedInventory()
        loadSavedExtras()
        if (prefs.getBoolean("autoConnect", true)) {
            connect()
        }
    }
    val friends = mutableStateListOf<PhoneFriend>().apply { addAll(loadFriends()) }

    private fun loadProfileCache(): com.quserh.eorzeaphone.data.PlayerProfile? = runCatching {
        val s = prefs.getString("profileCache", "")
        if (s.isNullOrBlank()) return@runCatching null
        val o = JSONObject(s)
        com.quserh.eorzeaphone.data.PlayerProfile(
            name = o.optString("name"),
            homeWorld = o.optString("homeWorld"),
            currentWorld = o.optString("currentWorld"),
            location = o.optString("location"),
            classJobId = o.optLong("classJobId"),
            jobName = o.optString("jobName"),
            level = o.optInt("level"),
            territoryId = o.optLong("territoryId"),
            itemLevel = o.optInt("itemLevel"),
        )
    }.getOrNull()

    private fun saveProfileCache(p: com.quserh.eorzeaphone.data.PlayerProfile?) {
        if (p == null) { prefs.edit().remove("profileCache").apply(); return }
        prefs.edit().putString("profileCache", JSONObject().apply {
            put("name", p.name)
            put("homeWorld", p.homeWorld)
            put("currentWorld", p.currentWorld)
            put("location", p.location)
            put("classJobId", p.classJobId)
            put("jobName", p.jobName)
            put("level", p.level)
            put("territoryId", p.territoryId)
            put("itemLevel", p.itemLevel)
        }.toString()).apply()
    }

    private fun loadSavedChats() {
        runCatching {
            val arr = JSONArray(prefs.getString("chatCache", "[]"))
            val msgs = mutableListOf<GameChatMessage>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                msgs += GameChatMessage(
                    timestamp = o.optLong("timestamp"),
                    sender = o.optString("sender"),
                    text = o.optString("text"),
                    channel = o.optInt("channel"),
                    self = o.optBoolean("self"),
                )
            }
            msgs.sortBy { it.timestamp }
            for (m in msgs) {
                if (chats.none { it.timestamp == m.timestamp && it.sender == m.sender && it.text == m.text }) {
                    chats.add(m)
                    getOrCreateConversation(m).add(m)
                }
            }
        }
    }

    private fun saveChats() {
        val arr = JSONArray()
        chats.forEach { m ->
            arr.put(JSONObject().apply {
                put("timestamp", m.timestamp)
                put("sender", m.sender)
                put("text", m.text)
                put("channel", m.channel)
                put("self", m.self)
            })
        }
        prefs.edit().putString("chatCache", arr.toString()).apply()
    }

    private fun loadSavedInventory() {
        runCatching {
            val items = JSONArray(prefs.getString("inventoryItemCache", "[]"))
            inventory.clear()
            for (i in 0 until items.length()) {
                val o = items.getJSONObject(i)
                inventory += GameInventoryItem(
                    itemId = o.optLong("itemId"),
                    name = o.optString("name"),
                    quantity = o.optInt("quantity"),
                    container = o.optLong("container"),
                    slot = o.optLong("slot"),
                    hq = o.optBoolean("hq"),
                    iconId = o.optInt("iconId"),
                )
            }
            val ctrs = JSONArray(prefs.getString("inventoryContainerCache", "[]"))
            inventoryContainers.clear()
            for (i in 0 until ctrs.length()) {
                val o = ctrs.getJSONObject(i)
                inventoryContainers += GameInventoryContainer(o.optLong("type"), o.optInt("size"))
            }
        }
    }

    private fun saveInventory() {
        val items = JSONArray()
        inventory.forEach { it ->
            items.put(JSONObject().apply {
                put("itemId", it.itemId)
                put("name", it.name)
                put("quantity", it.quantity)
                put("container", it.container)
                put("slot", it.slot)
                put("hq", it.hq)
                put("iconId", it.iconId)
            })
        }
        val ctrs = JSONArray()
        inventoryContainers.forEach { it ->
            ctrs.put(JSONObject().apply { put("type", it.type); put("size", it.size) })
        }
        prefs.edit()
            .putString("inventoryItemCache", items.toString())
            .putString("inventoryContainerCache", ctrs.toString())
            .apply()
    }

    private fun loadSavedExtras() {
        runCatching {
            val w = prefs.getString("walletCache", "")
            if (!w.isNullOrBlank()) {
                val o = JSONObject(w)
                val gil = o.optLong("gil")
                val arr = o.optJSONArray("entries") ?: JSONArray()
                val entries = buildList(arr.length()) {
                    for (i in 0 until arr.length()) {
                        val e = arr.getJSONObject(i)
                        add(GameWalletEntry(e.optLong("itemId"), e.optString("name", ""), e.optLong("amount"), e.optLong("cap"), e.optString("section", ""), e.optInt("iconId")))
                    }
                }
                wallet = GameWallet(gil, entries)
            }

            val j = prefs.getString("jobsCache", "")
            if (!j.isNullOrBlank()) {
                val arr = JSONArray(j)
                jobs.clear()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    jobs += GameJob(o.optLong("jobId"), o.optString("name", ""), o.optString("abbreviation", ""), o.optString("category", ""), o.optInt("level"), o.optBoolean("active"), o.optInt("itemLevel"))
                }
            }

            val d = prefs.getString("dailiesCache", "")
            if (!d.isNullOrBlank()) {
                val o = JSONObject(d)
                val arr = o.optJSONArray("entries") ?: JSONArray()
                val entries = buildList(arr.length()) {
                    for (i in 0 until arr.length()) {
                        val e = arr.getJSONObject(i)
                        add(GameDailyEntry(e.optString("id", ""), e.optString("label", ""), e.optBoolean("weekly"), e.optBoolean("automatic"), e.optBoolean("available"), e.optBoolean("complete"), e.optInt("remaining"), e.optInt("goal"), e.optString("note", "")))
                    }
                }
                dailies = GameDailies(o.optLong("nextDailyResetUnix"), o.optLong("nextWeeklyResetUnix"), entries)
            }

            val a = prefs.getString("activityCache", "")
            if (!a.isNullOrBlank()) {
                val o = JSONObject(a)
                activity = GameActivity(
                    o.optLong("sessionStartedUnix"), o.optLong("sessionPlaySeconds"), o.optLong("sessionExpGained"), o.optInt("sessionLevelsGained"), o.optLong("sessionGilEarned"), o.optInt("sessionDutiesCompleted"),
                    o.optLong("todayPlaySeconds"), o.optLong("todayExpGained"), o.optInt("todayLevelsGained"), o.optLong("todayGilEarned"), o.optInt("todayDutiesCompleted"),
                    o.optInt("mountsOwned"), o.optInt("mountsTotal"), o.optInt("minionsOwned"), o.optInt("minionsTotal"), o.optInt("retainerCount"), o.optInt("venturesReady"), o.optInt("venturesActive"),
                )
            }

            val w2 = prefs.getString("weatherCache", "")
            if (!w2.isNullOrBlank()) {
                val o = JSONObject(w2)
                val arr = o.optJSONArray("forecast") ?: JSONArray()
                val forecast = buildList(arr.length()) {
                    for (i in 0 until arr.length()) {
                        val e = arr.getJSONObject(i)
                        add(GameWeatherWindow(e.optString("name", ""), e.optInt("minutesFromNow"), e.optInt("eorzeaBell")))
                    }
                }
                weather = GameWeather(o.optString("zone", ""), o.optString("current", ""), forecast)
            }

            val h = prefs.getString("housingCache", "")
            if (!h.isNullOrBlank()) {
                val o = JSONObject(h)
                housing = GameHousingLocation(
                    if (o.isNull("ward")) null else o.optInt("ward"),
                    if (o.isNull("plot")) null else o.optInt("plot"),
                    o.optBoolean("exterior"),
                    if (o.isNull("apartmentWing")) null else o.optInt("apartmentWing"),
                )
            }
        }
    }

    private fun saveWallet() {
        val w = wallet ?: return
        val arr = JSONArray()
        w.entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("itemId", e.itemId); put("name", e.name); put("amount", e.amount); put("cap", e.cap); put("section", e.section); put("iconId", e.iconId)
            })
        }
        prefs.edit().putString("walletCache", JSONObject().apply { put("gil", w.gil); put("entries", arr) }.toString()).apply()
    }

    private fun saveJobs() {
        val arr = JSONArray()
        jobs.forEach { j ->
            arr.put(JSONObject().apply {
                put("jobId", j.jobId); put("name", j.name); put("abbreviation", j.abbreviation); put("category", j.category); put("level", j.level); put("active", j.active); put("itemLevel", j.itemLevel)
            })
        }
        prefs.edit().putString("jobsCache", arr.toString()).apply()
    }

    private fun saveDailies() {
        val d = dailies ?: return
        val arr = JSONArray()
        d.entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("label", e.label); put("weekly", e.weekly); put("automatic", e.automatic); put("available", e.available); put("complete", e.complete); put("remaining", e.remaining); put("goal", e.goal); put("note", e.note)
            })
        }
        prefs.edit().putString("dailiesCache", JSONObject().apply { put("nextDailyResetUnix", d.nextDailyResetUnix); put("nextWeeklyResetUnix", d.nextWeeklyResetUnix); put("entries", arr) }.toString()).apply()
    }

    private fun saveActivity() {
        val a = activity ?: return
        prefs.edit().putString("activityCache", JSONObject().apply {
            put("sessionStartedUnix", a.sessionStartedUnix); put("sessionPlaySeconds", a.sessionPlaySeconds); put("sessionExpGained", a.sessionExpGained); put("sessionLevelsGained", a.sessionLevelsGained); put("sessionGilEarned", a.sessionGilEarned); put("sessionDutiesCompleted", a.sessionDutiesCompleted)
            put("todayPlaySeconds", a.todayPlaySeconds); put("todayExpGained", a.todayExpGained); put("todayLevelsGained", a.todayLevelsGained); put("todayGilEarned", a.todayGilEarned); put("todayDutiesCompleted", a.todayDutiesCompleted)
            put("mountsOwned", a.mountsOwned); put("mountsTotal", a.mountsTotal); put("minionsOwned", a.minionsOwned); put("minionsTotal", a.minionsTotal); put("retainerCount", a.retainerCount); put("venturesReady", a.venturesReady); put("venturesActive", a.venturesActive)
        }.toString()).apply()
    }

    private fun saveWeather() {
        val w = weather ?: return
        val arr = JSONArray()
        w.forecast.forEach { f ->
            arr.put(JSONObject().apply { put("name", f.name); put("minutesFromNow", f.minutesFromNow); put("eorzeaBell", f.eorzeaBell) })
        }
        prefs.edit().putString("weatherCache", JSONObject().apply { put("zone", w.zone); put("current", w.current); put("forecast", arr) }.toString()).apply()
    }

    private fun saveHousing() {
        val h = housing ?: return
        prefs.edit().putString("housingCache", JSONObject().apply {
            if (h.ward != null) put("ward", h.ward) else put("ward", JSONObject.NULL)
            if (h.plot != null) put("plot", h.plot) else put("plot", JSONObject.NULL)
            put("exterior", h.exterior)
            if (h.apartmentWing != null) put("apartmentWing", h.apartmentWing) else put("apartmentWing", JSONObject.NULL)
        }.toString()).apply()
    }

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
            openConversationKey != null -> closeConversation()
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

    fun sendToConversation(conv: ChatConversation, text: String) {
        val trimmed = text.trim()
        if (!connected || trimmed.isBlank()) return
        val payload = if (conv.category == ChatCategory.Tell && conv.tellRecipient.isNotBlank()) {
            "/tell ${conv.tellRecipient} $trimmed"
        } else {
            trimmed
        }
        connection.sendChat(payload)
        val selfMsg = GameChatMessage(System.currentTimeMillis(), profile?.name ?: "我", trimmed, outChannelFor(conv.category), self = true)
        chats.add(selfMsg)
        conv.add(selfMsg)
        pendingSelfTexts["${conv.key}\u0000$trimmed"] = ""
        saveChats()
        chatDraft = ""
    }

    private fun outChannelFor(category: ChatCategory): Int = when (category) {
        ChatCategory.Tell -> 12
        ChatCategory.Party -> 2
        ChatCategory.FreeCompany -> 6
        ChatCategory.Linkshell -> 19
        ChatCategory.Public -> 1
        ChatCategory.Emote -> 28
        ChatCategory.System -> 30
    }

    fun openConversation(conv: ChatConversation) {
        openConversationKey = conv.key
        conv.unread = 0
    }

    fun closeConversation() {
        openConversationKey?.let { key -> conversationByKey[key]?.unread = 0 }
        openConversationKey = null
    }

    fun toggleConversationNotify(conv: ChatConversation) {
        conv.notify = !conv.notify
        if (conv.notify) mutedConversations.remove(conv.key) else mutedConversations.add(conv.key)
        prefs.edit().putStringSet("mutedChatConvs", mutedConversations).apply()
    }

    private fun getOrCreateConversation(message: GameChatMessage): ChatConversation {
        val key = message.conversationKey()
        return conversationByKey.getOrPut(key) {
            val conv = ChatConversation(key, message.category, message.conversationTitle(), message.tellRecipient())
            conv.notify = key !in mutedConversations
            conversations.add(0, conv)
            conv
        }
    }

    private fun ensureTellConversation(recipient: String, displayName: String? = null): ChatConversation {
        val key = "tell:${recipient.normalizedPlayerName()}"
        return conversationByKey.getOrPut(key) {
            val title = displayName?.takeIf { it.isNotBlank() } ?: recipient.displayPlayerName()
            val conv = ChatConversation(key, ChatCategory.Tell, title, recipient)
            conv.notify = key !in mutedConversations
            conversations.add(0, conv)
            conv
        }
    }

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
        val conv = ensureTellConversation(recipient, friend.name)
        openConversation(conv)
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
                val convKey = event.message.conversationKey()
                val selfEcho = pendingSelfTexts.remove("${convKey}\u0000${event.message.text}")
                if (selfEcho != null) {
                    // already added locally when sent; skip the game echo
                } else if (chats.none { it.timestamp == event.message.timestamp && it.sender == event.message.sender && it.text == event.message.text }) {
                    chats.add(event.message)
                    val conv = getOrCreateConversation(event.message)
                    conv.add(event.message)
                    val index = conversations.indexOf(conv)
                    if (index > 0) {
                        conversations.removeAt(index)
                        conversations.add(0, conv)
                    }
                    val isSelf = event.message.isFrom(profile?.name)
                    val isOpen = openConversationKey == conv.key
                    if (!isSelf && !isOpen) {
                        conv.unread = (conv.unread + 1).coerceAtMost(99)
                    } else {
                        conv.unread = 0
                    }
                    if (!isSelf && chatNotifications && conv.notify) {
                        notifier.chat(event.message, tellNotifications && event.message.category == ChatCategory.Tell)
                    }
                    saveChats()
                }
            }
            is PhoneEvent.Inventory -> {
                inventory.clear()
                inventory.addAll(event.snapshot.items)
                inventoryContainers.clear()
                inventoryContainers.addAll(event.snapshot.containers)
                saveInventory()
            }
            is PhoneEvent.Wallet -> {
                wallet = event.wallet
                if (sessionGilBaseline == null) sessionGilBaseline = event.wallet.gil
                saveWallet()
            }
            is PhoneEvent.Weather -> {
                weather = event.weather
                saveWeather()
            }
            is PhoneEvent.Jobs -> {
                jobs.clear()
                jobs.addAll(event.jobs)
                saveJobs()
            }
            is PhoneEvent.Housing -> {
                housing = event.location
                saveHousing()
            }
            is PhoneEvent.Dailies -> {
                dailies = event.dailies
                saveDailies()
            }
            is PhoneEvent.Activity -> {
                activity = event.activity
                saveActivity()
            }
            is PhoneEvent.Collections -> collections = event.collections
            is PhoneEvent.Profile -> {
                profile = event.profile
                saveProfileCache(event.profile)
            }
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
