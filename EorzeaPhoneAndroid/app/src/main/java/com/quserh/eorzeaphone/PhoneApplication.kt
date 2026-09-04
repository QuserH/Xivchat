package com.quserh.eorzeaphone

import android.app.Application
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.WindowRecomposerPolicy
import androidx.compose.ui.platform.createLifecycleAwareWindowRecomposer
import com.quserh.eorzeaphone.ui.PhoneMotion
import com.quserh.eorzeaphone.ui.PhoneMotionDurationScale
import com.quserh.eorzeaphone.ui.PhoneState
import com.quserh.eorzeaphone.data.CacheMaintenance
import com.quserh.eorzeaphone.data.market.MarketRepository
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaAutoCheckIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PhoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Prefer IPv4 for outbound sockets. Some carriers/CDNs resolve
        // unreachable IPv6 addresses for the 石之家 image CDN, which manifests
        // as "API works but images never load" on those networks.
        try { System.setProperty("java.net.preferIPv4Stack", "true") } catch (_: Exception) {}
        // Must run before anything creates a window recomposer, so it lives here rather
        // than in MainActivity.onCreate.
        installMotionDurationScale()
        // These are application concerns, not screen concerns: schedule sign-in and cache
        // maintenance before constructing the (potentially large) PhoneState snapshot. That
        // way a cold start begins the daily check-in immediately instead of waiting for chat
        // preferences to be parsed on the main thread.
        ShizhijiaAutoCheckIn.schedule(this)
        CacheMaintenance.schedule(this, force = true)
        // 进程被系统回收后（如用户清理后台）由 START_STICKY 重启时，无界面也要立刻恢复连接。
        phoneState
        // Repair market history created by older builds even when the user does
        // not open the market screen this session.  The repository enforces both
        // the age and global row limits in one short IO transaction.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { MarketRepository.trimHistory(this@PhoneApplication) }
        }
        // Older releases copied gallery images at their original 5–20 MB resolution.
        // Compact those private avatar copies once in the background; failures leave the
        // originals intact and are retried on the next process start.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { CacheMaintenance.compactLegacyImages(this@PhoneApplication) }
        }
    }

    /**
     * Route every Compose animation through [PhoneMotionDurationScale].
     *
     * Compose otherwise reads Settings.Global.ANIMATOR_DURATION_SCALE straight from the
     * system, and HarmonyOS/Honor power saving pins that to 0 — which finishes every
     * animation instantly on device while emulators (nonzero scale) look fine.
     *
     * Failure is recorded rather than swallowed: this uses an internal Compose API, so if
     * an OEM ROM breaks it the settings screen has to be able to say so. A silent catch
     * here is indistinguishable from "the switch is off".
     */
    @OptIn(InternalComposeUiApi::class, ExperimentalComposeUiApi::class)
    private fun installMotionDurationScale() {
        try {
            val scale = PhoneMotionDurationScale(this)
            WindowRecomposerPolicy.setFactory { view ->
                view.createLifecycleAwareWindowRecomposer(coroutineContext = scale)
            }
            PhoneMotion.overrideInstalled = true
            PhoneMotion.overrideError = null
        } catch (t: Throwable) {
            PhoneMotion.overrideInstalled = false
            PhoneMotion.overrideError = t.javaClass.simpleName + ": " + (t.message ?: "")
        }
    }

    val phoneState: PhoneState by lazy {
        PhoneState(this, CoroutineScope(SupervisorJob() + Dispatchers.Main))
    }
}
