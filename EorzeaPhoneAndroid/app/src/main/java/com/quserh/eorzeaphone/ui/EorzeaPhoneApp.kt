package com.quserh.eorzeaphone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
