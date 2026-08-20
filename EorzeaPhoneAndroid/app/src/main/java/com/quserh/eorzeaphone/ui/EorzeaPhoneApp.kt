package com.quserh.eorzeaphone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
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
import com.quserh.eorzeaphone.ui.theme.EorzeaPhoneTheme
import com.quserh.eorzeaphone.ui.theme.LocalContentMargin

private data class PhoneRoute(val screen: PhoneScreen, val appId: String, val friendId: Long)

private fun PhoneRoute.level(): Int = when (screen) {
    PhoneScreen.Home -> 0
    PhoneScreen.ContactDetail -> 2
    else -> 1
}

@Composable
fun EorzeaPhoneApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember(context, scope) { PhoneState(context, scope) }
    val darkTheme = state.useDarkTheme(isSystemInDarkTheme())
    EorzeaPhoneTheme(darkTheme = darkTheme) {
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
                if (event == Lifecycle.Event.ON_RESUME) state.ensureConnectedOnResume()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                state.disconnect()
            }
        }
        BackHandler(enabled = state.screen != PhoneScreen.Home) { state.back() }
        BackHandler(enabled = state.screen == PhoneScreen.Home && state.homeEditMode) { state.exitEditMode() }

        val route = PhoneRoute(state.screen, state.selectedApp?.id.orEmpty(), state.selectedFriend?.contentId ?: 0)
        CompositionLocalProvider(LocalContentMargin provides state.contentMargin) {
        Box(Modifier.fillMaxSize().onSizeChanged { state.updateShellSize(it.width, it.height) }) {
            AnimatedContent(
            targetState = route,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState.level() >= initialState.level()
                if (forward) {
                    // zoom-in from where the tile was tapped, plus a slide
                    val pivot = androidx.compose.ui.graphics.TransformOrigin(state.launchPivotX, state.launchPivotY)
                    (
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(230, easing = FastOutSlowInEasing))
                            + fadeIn(tween(180))
                            + scaleIn(initialScale = 0.86f, animationSpec = spring(dampingRatio = 0.72f, stiffness = 320f), transformOrigin = pivot)
                        ).togetherWith(
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(210, easing = FastOutSlowInEasing))
                            + fadeOut(tween(140))
                            + scaleOut(targetScale = 0.94f, animationSpec = tween(260), transformOrigin = pivot),
                    )
                } else {
                    (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(220, easing = FastOutSlowInEasing)) + fadeIn(tween(170, 40)))
                        .togetherWith(slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(210, easing = FastOutSlowInEasing)) + fadeOut(tween(140)))
                }
            },
            label = "phone-navigation",
            ) { target ->
                Box(Modifier.fillMaxSize()) {
                when (target.screen) {
        PhoneScreen.Home -> HomeScreen(state)
        PhoneScreen.Settings -> SettingsSubScreen(state)
        PhoneScreen.Contacts -> AetherphoneMessagesScreen(state)
        PhoneScreen.ContactDetail -> AetherphoneContactDetailScreen(state)
        PhoneScreen.Chat -> AetherphoneMessagesScreen(state)
        PhoneScreen.App -> when (state.selectedApp?.id) {
            "inventory" -> InventoryScreen(state)
            "wallet" -> WalletScreen(state)
            "skywatcher" -> SkywatcherScreen(state)
            "character" -> AetherphoneActivityScreen(state)
            "jobs" -> AetherphoneJobsScreen(state)
            "collections" -> CollectionsScreen(state)
            "clock" -> ClockScreen(state)
            "calculator" -> CalculatorScreen(state)
            "notes" -> AetherphoneNotesScreen(state)
            "timers" -> TimersScreen(state)
            "calendar" -> CalendarScreen(state)
            "dailies" -> DailiesScreen(state)
            "submarine" -> SubmarineScreen(state)
            "housing" -> HousingScreen(state)
            "notifications" -> NotificationsScreen(state)
            "camera" -> CameraScreen(state)
            "photos" -> PhotosScreen(state)
            "shortcuts" -> ShortcutsScreen(state)
            "fishing" -> FishingScreen(state)
            "maps" -> MapsScreen(state)
            "health" -> HealthScreen(state)
            "appstore" -> AppStoreScreen(state)
            else -> GenericAppScreen(state)
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
                .background(if (done) Color(0xEE23382A) else Color(0xEE20283A))
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
                    Text(if (done) "传送完毕" else "传送中", color = (if (done) Color(0xFF6FE39A) else Color(0xFF9CC8FF)), fontSize = 11.sp)
                }
                Text(if (done) "✓" else "…", color = if (done) Color(0xFF6FE39A) else Color(0xFF9CC8FF), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
