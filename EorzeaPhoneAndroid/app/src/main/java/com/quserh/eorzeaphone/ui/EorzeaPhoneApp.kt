package com.quserh.eorzeaphone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.core.view.WindowCompat
import com.quserh.eorzeaphone.ui.theme.EorzeaPhoneTheme

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
        DisposableEffect(state) { onDispose { state.disconnect() } }
        BackHandler(enabled = state.screen != PhoneScreen.Home) { state.back() }
        BackHandler(enabled = state.screen == PhoneScreen.Home && state.homeEditMode) { state.exitEditMode() }

        val route = PhoneRoute(state.screen, state.selectedApp?.id.orEmpty(), state.selectedFriend?.contentId ?: 0)
        Box(Modifier.fillMaxSize().pointerInput(state.haptics) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var moved = false
                var event = awaitPointerEvent(PointerEventPass.Final)
                while (event.changes.any { it.pressed }) {
                    if (event.changes.any { (it.position - down.position).getDistance() > touchSlop }) moved = true
                    event = awaitPointerEvent(PointerEventPass.Final)
                }
                if (state.haptics && !moved) performPhoneHaptic(context, view)
            }
        }.onSizeChanged { state.updateShellSize(it.width, it.height) }) {
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
            "housing" -> HousingScreen(state)
            "notifications" -> NotificationsScreen(state)
            "camera" -> CameraScreen(state)
            "photos" -> PhotosScreen(state)
            "shortcuts" -> ShortcutsScreen(state)
            "fishing" -> FishingScreen(state)
            "maps" -> MapsScreen(state)
            "health" -> HealthScreen(state)
            else -> GenericAppScreen(state)
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
