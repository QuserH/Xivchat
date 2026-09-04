package com.quserh.eorzeaphone.ui

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalContext

/**
 * App-wide motion policy.
 *
 * Compose takes animation duration from the system's
 * [Settings.Global.ANIMATOR_DURATION_SCALE]. At 0 every animation jumps straight to its
 * end value, so the app looks completely static. HarmonyOS / Honor set that to 0 silently
 * behind 省电模式 and 辅助功能「减少动画」 — which is why animations played in an emulator
 * (scale 1) but not on a real phone.
 *
 * Three inputs decide whether we animate, in priority order:
 *  1. [reducedMotion] — the user's own in-app switch, always wins.
 *  2. [forceMotion] — user opted to ignore the system setting.
 *  3. the system scale — respected by default, so accessibility still comes first.
 *
 * The flags are Compose state so composables recompose when they change, and are still
 * plain reads for [PhoneMotionDurationScale], which the recomposer calls from outside
 * composition. [PhoneState] owns the persisted prefs and mirrors them in here.
 */
object PhoneMotion {

    private val _forceMotion = mutableStateOf(true)
    private val _reducedMotion = mutableStateOf(false)

    var forceMotion: Boolean
        get() = _forceMotion.value
        set(value) { _forceMotion.value = value }

    var reducedMotion: Boolean
        get() = _reducedMotion.value
        set(value) { _reducedMotion.value = value }

    /**
     * Whether the recomposer override actually got installed, and why not if it didn't.
     *
     * Surfaced in the settings screen on purpose: the override uses an internal Compose
     * API, so on an OEM ROM it could fail where it works on an emulator. Without this the
     * failure is invisible and indistinguishable from "the switch is off" — which is
     * exactly the dead end this ran into once already.
     */
    @Volatile
    var overrideInstalled: Boolean = false

    @Volatile
    var overrideError: String? = null

    /** Raw system scale; 1f when unreadable, matching the platform default. */
    fun systemScale(context: Context): Float = runCatching {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }.getOrDefault(1f)

    /** The scale Compose should actually use. */
    fun effectiveScale(context: Context): Float = when {
        reducedMotion -> 0f
        forceMotion -> 1f
        else -> systemScale(context)
    }
}

/**
 * Feeds [PhoneMotion] back into Compose.
 *
 * Installed into the recomposer's coroutine context so it covers every animation in the
 * app, not just the handful of places that call [phoneMotionEnabled] themselves. The
 * getter is re-read per animation, so flipping a switch applies without a restart.
 */
class PhoneMotionDurationScale(context: Context) : MotionDurationScale {
    private val appContext = context.applicationContext
    override val scaleFactor: Float
        get() = PhoneMotion.effectiveScale(appContext)
}

/**
 * Live motion flag for composables that branch on it.
 *
 * Watches the system setting instead of sampling it once: the previous version used a
 * bare `remember {}`, so a screen already in memory kept its stale answer even after the
 * user turned animations back on (and missed 省电模式 flipping mid-session).
 */
@Composable
fun rememberMotionEnabled(): Boolean {
    val appContext = LocalContext.current.applicationContext
    var systemScale by remember { mutableStateOf(PhoneMotion.systemScale(appContext)) }

    DisposableEffect(appContext) {
        val resolver = appContext.contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                systemScale = PhoneMotion.systemScale(appContext)
            }
        }
        runCatching {
            resolver.registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
                false,
                observer,
            )
        }
        onDispose { runCatching { resolver.unregisterContentObserver(observer) } }
    }

    return when {
        PhoneMotion.reducedMotion -> false
        PhoneMotion.forceMotion -> true
        else -> systemScale > 0f
    }
}
