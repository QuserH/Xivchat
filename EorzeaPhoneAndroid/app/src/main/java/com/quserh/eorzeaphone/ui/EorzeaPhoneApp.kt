package com.quserh.eorzeaphone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onSizeChanged

private data class PhoneRoute(val screen: PhoneScreen, val appId: String, val friendId: Long)

@Composable
fun EorzeaPhoneApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember(context, scope) { PhoneState(context, scope) }
    DisposableEffect(state) { onDispose { state.disconnect() } }
    BackHandler(enabled = state.screen != PhoneScreen.Home) { state.back() }

    val route = PhoneRoute(state.screen, state.selectedApp?.id.orEmpty(), state.selectedFriend?.contentId ?: 0)
    val pivot = TransformOrigin(state.launchPivotX, state.launchPivotY)
    Box(Modifier.fillMaxSize().onSizeChanged { state.updateShellSize(it.width, it.height) }) {
        AnimatedContent(
            targetState = route,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                when {
                    initialState.screen == PhoneScreen.Home && targetState.screen != PhoneScreen.Home ->
                        (scaleIn(tween(360, easing = FastOutSlowInEasing), state.launchScale, pivot) + fadeIn(tween(190, 120)))
                            .togetherWith(scaleOut(tween(330, easing = FastOutSlowInEasing), 1.07f, pivot) + fadeOut(tween(170)))
                    targetState.screen == PhoneScreen.Home && initialState.screen != PhoneScreen.Home ->
                        (scaleIn(tween(330, easing = FastOutSlowInEasing), 1.07f, pivot) + fadeIn(tween(190, 80)))
                            .togetherWith(scaleOut(tween(340, easing = FastOutSlowInEasing), state.launchScale, pivot) + fadeOut(tween(180, 80)))
                    targetState.screen == PhoneScreen.Contacts && initialState.screen == PhoneScreen.ContactDetail ->
                        (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(250)) + fadeIn(tween(170)))
                            .togetherWith(slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(250)) + fadeOut(tween(150)))
                    else ->
                        (slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(250)) + fadeIn(tween(170)))
                            .togetherWith(slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(250)) + fadeOut(tween(150)))
                }
            },
            label = "phone-navigation",
        ) { target ->
    when (target.screen) {
        PhoneScreen.Home -> HomeScreen(state)
        PhoneScreen.Settings -> SettingsSubScreen(state)
        PhoneScreen.Contacts -> ContactsScreen(state)
        PhoneScreen.ContactDetail -> ContactDetailScreen(state)
        PhoneScreen.Chat -> ChatScreen(state)
        PhoneScreen.App -> when (state.selectedApp?.id) {
            "inventory" -> InventoryScreen(state)
            "wallet" -> WalletScreen(state)
            "skywatcher" -> SkywatcherScreen(state)
            "character" -> ActivityScreen(state)
            "jobs" -> JobsScreen(state)
            "collections" -> CollectionsScreen(state)
            "clock" -> ClockScreen(state)
            "calculator" -> CalculatorScreen(state)
            "notes" -> NotesScreen(state)
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
