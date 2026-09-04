package com.quserh.eorzeaphone.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import androidx.core.view.WindowCompat
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.CacheMaintenance
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaAutoCheckIn
import com.quserh.eorzeaphone.ui.theme.EorzeaPhoneTheme
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin
import com.quserh.eorzeaphone.ui.theme.PhoneBackground

private data class PhoneRoute(val screen: PhoneScreen, val appId: String, val friendId: Long)

/**
 * Position in the navigation hierarchy, which picks the transition direction.
 *
 * Contacts / Chat / App / Settings are siblings of each other and children of Home, so they
 * all sit at 1 and swap with a crossfade rather than a slide. The previous version collapsed
 * everything except Home and ContactDetail into "else -> 1" while treating equal levels as
 * forward, so every sibling swap replayed the push animation and back looked like forward.
 */
private fun PhoneRoute.depth(): Int = when (screen) {
    PhoneScreen.Home -> 0
    PhoneScreen.ContactDetail -> 2
    PhoneScreen.Settings, PhoneScreen.Contacts, PhoneScreen.Chat, PhoneScreen.App -> 1
}

@Composable
fun EorzeaPhoneApp(deepLink: MutableState<String?>) {
    val context = LocalContext.current
    val state = remember(context) { (context.applicationContext as com.quserh.eorzeaphone.PhoneApplication).phoneState }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    LaunchedEffect(Unit) {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= 33) {
                add(android.Manifest.permission.POST_NOTIFICATIONS)
                add(android.Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }.filter { ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
        val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as? android.os.PowerManager
        val prefs = context.getSharedPreferences("eorzea_phone_ui", android.content.Context.MODE_PRIVATE)
        if (pm != null && Build.VERSION.SDK_INT >= 23 && !prefs.getBoolean("batteryExemptRequested", false) && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
            prefs.edit().putBoolean("batteryExemptRequested", true).apply()
            runCatching {
                val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, android.net.Uri.parse("package:${context.packageName}"))
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }
    LaunchedEffect(deepLink.value) {
        val key = deepLink.value
        if (!key.isNullOrBlank()) {
            state.openDeepLink(key)
            deepLink.value = null
        }
    }
    val darkTheme = state.useDarkTheme(isSystemInDarkTheme())
    EorzeaPhoneTheme(darkTheme = darkTheme, accent = state.accent) {
        val view = LocalView.current
        val touchSlop = remember(view) { ViewConfiguration.get(view.context).scaledTouchSlop.toFloat() }
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(state, lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        state.ensureConnectedOnResume()
                        // Re-check the day when the process stayed alive across midnight and
                        // trim any cache growth accumulated while it was in the foreground.
                        ShizhijiaAutoCheckIn.schedule(context)
                        CacheMaintenance.schedule(context)
                    }
                    state.appInForeground = true
                }
                else if (event == Lifecycle.Event.ON_STOP) {
                    // The normal chat writer is debounced to keep packet bursts off the
                    // UI thread.  Flush once when the activity leaves the foreground so
                    // a process kill/background cleanup cannot lose the tail of a chat.
                    state.flushChatPersistence()
                    state.appInForeground = false
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }
        BackHandler(enabled = state.screen != PhoneScreen.Home) { state.back() }
        BackHandler(enabled = state.screen == PhoneScreen.Home && state.homeEditMode) { state.exitEditMode() }

        val route = remember(state.screen, state.selectedApp?.id, state.selectedFriend?.contentId) {
            PhoneRoute(state.screen, state.selectedApp?.id.orEmpty(), state.selectedFriend?.contentId ?: 0)
        }
        CompositionLocalProvider(LocalContentMargin provides state.contentMargin) {
        // Opaque floor under every transition. Without it the backdrop is the raw window,
        // which is black on this device -- so any moment where the outgoing screen has
        // faded past the incoming one, or where a scaled screen does not reach the edges,
        // flashed black. Screens paint their own background too; this only ever shows
        // during a transition.
        Box(
            Modifier.fillMaxSize()
                .background(PhoneBackground)
                .onSizeChanged { state.updateShellSize(it.width, it.height) },
        ) {
            val motionAllowed = phoneMotionEnabled()
            AnimatedContent(
            targetState = route,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                phoneNavTransition(
                    motionAllowed = motionAllowed,
                    targetDepth = targetState.depth(),
                    initialDepth = initialState.depth(),
                    // A feature -> desktop return must not keep the old feature alive
                    // for a fade; the user otherwise sees a ghost frame before Home.
                    instantPopToRoot = true,
                )
            },
            label = "phone-navigation",
            ) { target ->
                // [target] is the route snapshot for this animation layer.  Reading
                // selectedApp/selectedFriend directly here is racy: Back clears them before
                // the outgoing layer finishes, so it briefly renders a duplicate or the
                // wrong app. Resolve all route-specific inputs from the snapshot instead.
                val targetApp = target.appId.takeIf { it.isNotBlank() }?.let(state::appItem)
                val targetFriend = target.friendId.takeIf { it != 0L }
                    ?.let { id -> state.friends.firstOrNull { it.contentId == id } }
                Box(Modifier.fillMaxSize()) {
                when (target.screen) {
        PhoneScreen.Home -> HomeScreen(state)
        PhoneScreen.Settings -> SettingsSubScreen(state)
        PhoneScreen.Contacts -> AetherphoneMessagesScreen(state)
        PhoneScreen.ContactDetail -> AetherphoneContactDetailScreen(state, targetFriend)
        PhoneScreen.Chat -> AetherphoneMessagesScreen(state)
        PhoneScreen.App -> when (val appId = target.appId) {
            "inventory" -> androidx.compose.runtime.key(appId) { InventoryScreen(state) }
            "wallet" -> androidx.compose.runtime.key(appId) { WalletScreen(state) }
            "skywatcher" -> androidx.compose.runtime.key(appId) { SkywatcherScreen(state) }
            "character" -> androidx.compose.runtime.key(appId) { AetherphoneActivityScreen(state) }
            "jobs" -> androidx.compose.runtime.key(appId) { AetherphoneJobsScreen(state) }
            "collections" -> androidx.compose.runtime.key(appId) { CollectionsScreen(state) }
            "clock" -> androidx.compose.runtime.key(appId) { ClockScreen(state) }
            "calculator" -> androidx.compose.runtime.key(appId) { CalculatorScreen(state) }
            "notes" -> androidx.compose.runtime.key(appId) { AetherphoneNotesScreen(state) }
            "timers" -> androidx.compose.runtime.key(appId) { TimersScreen(state) }
            "calendar" -> androidx.compose.runtime.key(appId) { CalendarScreen(state) }
            "dailies" -> androidx.compose.runtime.key(appId) { DailiesScreen(state) }
            "submarine" -> androidx.compose.runtime.key(appId) { SubmarineScreen(state) }
            "housing" -> androidx.compose.runtime.key(appId) { HousingScreen(state) }
            "notifications" -> androidx.compose.runtime.key(appId) { NotificationsScreen(state) }
            "camera" -> androidx.compose.runtime.key(appId) { CameraScreen(state) }
            "photos" -> androidx.compose.runtime.key(appId) { PhotosScreen(state) }
            "shortcuts" -> androidx.compose.runtime.key(appId) { ShortcutsScreen(state) }
            "fishing" -> androidx.compose.runtime.key(appId) { FishingScreen(state) }
            "maps" -> androidx.compose.runtime.key(appId) { MapsScreen(state) }
            "health" -> androidx.compose.runtime.key(appId) { HealthScreen(state) }
            "shizhijia" -> androidx.compose.runtime.key(appId) { ShizhijiaScreen(state) }
            "wiki" -> androidx.compose.runtime.key(appId) { WikiScreen(state) }
            "gatherclock" -> androidx.compose.runtime.key(appId) { GatherClockScreen(state) }
            "market" -> androidx.compose.runtime.key(appId) { MarketScreen(state) }
            "appstore" -> androidx.compose.runtime.key(appId) { AppStoreScreen(state) }
            else -> androidx.compose.runtime.key(appId) { GenericAppScreen(state, targetApp) }
                }
                }
            }
            if (state.teleportStatus != TeleportStatus.Idle) {
                TeleportBanner(state)
            }
        }
        }
    }
}
}

private fun performPhoneHaptic(context: Context, view: android.view.View) {
    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    } ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) vibrator.vibrate(VibrationEffect.createOneShot(14L, 90))
    else @Suppress("DEPRECATION") vibrator.vibrate(14L)
}

// 传送横幅固定深底：它盖在任意界面上（包括浅色的桌面和石之家），
// 只有自带深底 + 白字才能保证任何背景下都读得清。所以这几个色**不跟主题**，
// 是有意为之，不是漏改。抽成常量，别再散在布局里。
private val TeleportDoneBg = Color(0xEE23382A)   // 完成：墨绿
private val TeleportBusyBg = Color(0xEE20283A)   // 进行中：墨蓝
private val TeleportDoneInk = Color(0xFF6FE39A)
private val TeleportBusyInk = Color(0xFF9CC8FF)

@Composable
private fun TeleportBanner(state: PhoneState) {
    val target = state.teleportTarget ?: return
    val done = state.teleportStatus == TeleportStatus.Done
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(
            Modifier
                .widthIn(max = 340.dp)
                .padding(top = 56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (done) TeleportDoneBg else TeleportBusyBg)
                .pointerInput(state) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, _ -> },
                        onDragEnd = { state.dismissTeleport() },
                    )
                }
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ItemIcon(60453, Modifier.size(30.dp), "晶")
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(target, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (done) "传送完毕" else "传送中", color = if (done) TeleportDoneInk else TeleportBusyInk, fontSize = 11.sp)
                }
                // 完成打勾，进行中转圈。原来是"✓"和"…"两个字符，
                // 省略号在不同字体里宽度差一倍，横幅右端会忽宽忽窄。
                if (done) {
                    ImageGlyph(R.drawable.ic_check_small, TeleportDoneInk, Modifier.size(19.dp))
                } else {
                    CircularProgressIndicator(
                        color = TeleportBusyInk,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(15.dp),
                    )
                }
            }
        }
    }
}
