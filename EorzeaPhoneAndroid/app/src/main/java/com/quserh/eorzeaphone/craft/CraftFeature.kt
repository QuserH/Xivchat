package com.quserh.eorzeaphone.craft

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quserh.eorzeaphone.craft.data.CartStore
import com.quserh.eorzeaphone.craft.data.CraftRecipe
import com.quserh.eorzeaphone.craft.data.CraftSession
import com.quserh.eorzeaphone.craft.data.CraftingListStore
import com.quserh.eorzeaphone.craft.data.InventoryItem
import com.quserh.eorzeaphone.craft.data.InventorySnapshot
import com.quserh.eorzeaphone.craft.data.MockCraftEngine
import com.quserh.eorzeaphone.craft.data.RecipeDb
import com.quserh.eorzeaphone.craft.data.RecipeRepository
import com.quserh.eorzeaphone.craft.data.RetainerEntry
import com.quserh.eorzeaphone.craft.ui.CraftBackground
import com.quserh.eorzeaphone.craft.ui.CraftFill
import com.quserh.eorzeaphone.craft.ui.CraftMuted
import com.quserh.eorzeaphone.craft.ui.CraftSurface
import com.quserh.eorzeaphone.craft.ui.CraftTabBar
import com.quserh.eorzeaphone.craft.ui.CraftText
import com.quserh.eorzeaphone.craft.ui.CraftTheme
import com.quserh.eorzeaphone.craft.ui.CraftType
import com.quserh.eorzeaphone.craft.ui.CartPage
import com.quserh.eorzeaphone.craft.ui.InventoryTab
import com.quserh.eorzeaphone.craft.ui.ListDetail
import com.quserh.eorzeaphone.craft.ui.ListTab
import com.quserh.eorzeaphone.craft.ui.LocationPopup
import com.quserh.eorzeaphone.craft.ui.RecipeDetail
import com.quserh.eorzeaphone.craft.ui.RecipesTab
import com.quserh.eorzeaphone.craft.ui.WorkbenchTab
import com.quserh.eorzeaphone.data.GameInventoryItem
import com.quserh.eorzeaphone.data.GameRetainer
import com.quserh.eorzeaphone.ui.PhoneState

/**
 * Crafting-list feature embedded in the terminal app, wired the same way as
 * Shizhijia: one app tile -> one screen. Inventory comes from the shared
 * connection (PhoneState.inventory), recipes come from the bundled craft.db.
 */
fun buildCraftSnapshot(items: List<GameInventoryItem>, retainers: List<GameRetainer>): InventorySnapshot =
    InventorySnapshot(
        updatedMs = System.currentTimeMillis(),
        items = items.map {
            InventoryItem(
                it.itemId, it.name, it.quantity, it.container, it.slot, it.hq, it.iconId, it.retainerId,
            )
        },
        retainers = retainers.map {
            RetainerEntry(it.id, it.name, it.active, it.itemCount, it.quantity, it.gil, it.ventureId, it.ventureCompleteUnix)
        },
    )

@Composable
fun CraftListAppScreen(phone: PhoneState) {
    val context = LocalContext.current
    val state = remember { CraftAppState(context, phone) }

    LaunchedEffect(Unit) {
        state.prepareDb()
    }
    // Recompute the craft-side snapshot whenever any live inventory row changes.
    // Uses the RAW snapshot (before the host drops crystal containers) so the
    // crafting module can see the crystal pouch.
    val snapshot by remember {
        derivedStateOf {
            val raw = phone.craftRawInventory
            if (raw == null) InventorySnapshot()
            else InventorySnapshot(
                updatedMs = System.currentTimeMillis(),
                items = raw.items.map {
                    InventoryItem(it.itemId, it.name, it.quantity, it.container, it.slot, it.hq, it.iconId, it.retainerId)
                },
                retainers = raw.retainers.map {
                    RetainerEntry(it.id, it.name, it.active, it.itemCount, it.quantity, it.gil, it.ventureId, it.ventureCompleteUnix)
                },
            )
        }
    }
    LaunchedEffect(snapshot) { if (snapshot.items.isNotEmpty()) state.inventory = snapshot }

    BackHandler(enabled = state.stack.size > 1) { state.pop() }

    CraftTheme(darkTheme = isSystemInDarkTheme()) {
        CraftAppScaffold(state)
    }
}

enum class Tab(val label: String) { LISTS("清单"), RECIPES("配方"), INVENTORY("库存"), WORKBENCH("工作台") }

sealed class Page {
    data object Root : Page()
    data class RecipeDetail(val itemId: Int) : Page()
    data class ListDetail(val listId: String) : Page()
    data object Cart : Page()
}

class CraftAppState(private val context: Context, private val phone: PhoneState) {
    val db = RecipeDb(context)
    val repo = RecipeRepository(db)
    val lists = CraftingListStore(context)
    val cart = CartStore(context)
    val engine = MockCraftEngine(kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main))

    /** 远程模式是否可用：终端已连上游戏插件。 */
    val craftConnected: Boolean get() = phone.isConnected()

    /** 游戏内正在制作的实时状态（无论谁发起），无则 null。 */
    val remoteCraft: com.quserh.eorzeaphone.data.GameCraftState? get() = phone.craftRemote

    private val foodPrefs = context.getSharedPreferences("craft_food", Context.MODE_PRIVATE)

    /** 选中的食物（0=未选）；持久化，启动远程制作时下发给插件。 */
    var craftFoodId by mutableStateOf(foodPrefs.getInt("foodId", 0))
    var craftFoodName by mutableStateOf(foodPrefs.getString("foodName", "") ?: "")

    fun setCraftFood(itemId: Int, name: String) {
        craftFoodId = itemId
        craftFoodName = name
        foodPrefs.edit().putInt("foodId", itemId).putString("foodName", name).apply()
        if (craftConnected) phone.craftFood(itemId)
    }

    private val craftBridge = object : com.quserh.eorzeaphone.craft.data.CraftRemote {
        override val chat = phone.craftChatEvents
        override val lastState: com.quserh.eorzeaphone.data.GameCraftState? get() = phone.craftRemote
        override fun craftStart(recipeId: Int) = phone.craftStart(recipeId)
        override fun craftSkill(actionId: Long) = phone.craftSkill(actionId)
        override fun craftStop() = phone.craftStop()
        override fun craftFood(itemId: Int) = phone.craftFood(itemId)
    }

    fun startRemoteCraft(recipe: CraftRecipe, itemName: String): CraftSession {
        if (craftFoodId > 0) craftBridge.craftFood(craftFoodId)
        // 丢弃上一次制作残留的最后一帧，否则新会话会立刻把它当成
        // “已完成”而锁死技能、不再消费新帧。
        phone.craftRemote = null
        return engine.startRemote(recipe, itemName, craftBridge)
    }

    var inventory by mutableStateOf(InventorySnapshot())
    var dbReady by mutableStateOf(false)
    var dbError by mutableStateOf<String?>(null)
    var dbProgress by mutableStateOf("")
    var session by mutableStateOf<CraftSession?>(null)

    var tab by mutableStateOf(Tab.LISTS)
    var stack by mutableStateOf<List<Page>>(listOf(Page.Root))
    var locationPopupItem by mutableStateOf<Long?>(null)

    /** Open the bundled db (with asset self-repair); errors surface, never swallow. */
    suspend fun prepareDb() {
        db.prepare(
            onReady = {
                dbReady = true
                dbError = null
                dbProgress = ""
            },
            onError = { dbError = it },
        )
        if (dbReady) return
        // Bundled asset missing/corrupt: self-heal once from the live source
        // automatically — the user should not need to know the repair exists.
        runCatching { repairDbFromNetwork() }
    }

    /** One-tap rebuild from the live 5p statics pack when the local db is unusable. */
    suspend fun repairDbFromNetwork() {
        try {
            db.rebuildFromNetwork { dbProgress = it }
            dbReady = true
            dbError = null
            dbProgress = ""
        } catch (e: Throwable) {
            dbError = "网络重建失败：${e.message ?: "未知错误"}"
            dbProgress = ""
        }
    }

    fun push(page: Page) {
        stack = stack + page
    }

    fun pop() {
        if (stack.size > 1) stack = stack.dropLast(1)
    }

    fun openLocations(itemId: Long) {
        locationPopupItem = itemId
    }

    /** 把购物车里勾选的道具（连同数量）移入指定清单。 */
    fun moveCartToList(listId: String, itemIds: Set<Int>) {
        itemIds.forEach { id ->
            cart.items.firstOrNull { it.itemId == id }?.let { lists.addEntry(listId, it.itemId, it.qty) }
        }
        cart.removeIds(itemIds)
    }
}

@Composable
fun CraftAppScaffold(state: CraftAppState) {
    // The host runs edge-to-edge; every app screen pads its own system bars
    // (same pattern as InventoryScreen in ui/SubScreens.kt).
    Column(
        Modifier
            .fillMaxSize()
            .background(CraftBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            AnimatedContent(
                targetState = state.stack,
                transitionSpec = {
                    if (targetState.size > initialState.size) {
                        slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280)) togetherWith
                            slideOutHorizontally(tween(280)) { -it / 4 } + fadeOut(tween(200))
                    } else {
                        slideInHorizontally(tween(280)) { -it / 4 } + fadeIn(tween(280)) togetherWith
                            slideOutHorizontally(tween(280)) { it / 3 } + fadeOut(tween(200))
                    }
                },
                label = "craftNav",
            ) { stack ->
                when (val top = stack.last()) {
                    Page.Root -> when (state.tab) {
                        Tab.LISTS -> ListTab(state)
                        Tab.RECIPES -> RecipesTab(state)
                        Tab.INVENTORY -> InventoryTab(state)
                        Tab.WORKBENCH -> WorkbenchTab(state)
                    }
                    is Page.RecipeDetail -> RecipeDetail(state, top.itemId)
                    is Page.ListDetail -> ListDetail(state, top.listId)
                    Page.Cart -> CartPage(state)
                }
            }
            state.locationPopupItem?.let { itemId ->
                LocationPopup(state, itemId)
            }
        }
        if (state.stack.size == 1) {
            CraftTabBar(state.tab.ordinal) { state.tab = Tab.entries[it] }
        }
    }
}
