package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Application-level 石之家签到 coordinator.
 *
 * Signing used to live in the home-screen composable, which meant it never ran when the
 * user opened another feature first and could race with the login screen.  This object is
 * deliberately UI-independent: application start, foreground resume, and a fresh login all
 * call [schedule], while the mutex makes those triggers one request at a time.
 */
object ShizhijiaAutoCheckIn {
    enum class Status { Signed, AlreadySigned, NoSession, Failed }

    data class Result(
        val status: Status,
        /** False only when no usable session was available; network failures do not erase it. */
        val loggedIn: Boolean,
    ) {
        val signedToday: Boolean
            get() = status == Status.Signed || status == Status.AlreadySigned
    }

    private val gate = Mutex()
    private val worker = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lastScheduledAt = AtomicLong(0L)

    /** Fire-and-forget entry point for lifecycle callbacks. It never shows UI or a toast. */
    fun schedule(context: Context) {
        val app = context.applicationContext
        // Application start, ON_RESUME and login completion can all arrive within the same
        // frame.  Coalesce only that tiny burst; a later foreground resume must still be able
        // to retry a transient network failure.
        val now = System.currentTimeMillis()
        while (true) {
            val previous = lastScheduledAt.get()
            if (now - previous < SCHEDULE_COALESCE_MS) return
            if (lastScheduledAt.compareAndSet(previous, now)) break
        }
        worker.launch {
            try {
                var result = runCatching { ensure(app) }.getOrNull()
                // A cold start can race DNS/TLS or the first cookie refresh.  A short bounded
                // retry window makes "打开 App 自动签到" deterministic without keeping a process
                // alive forever or hammering the endpoint.  Login/no-session results stop here;
                // saving a new cookie schedules a fresh run.
                if (result?.status == Status.Failed && result.loggedIn) {
                    for (backoff in RETRY_BACKOFF_MS) {
                        delay(backoff)
                        result = runCatching { ensure(app) }.getOrNull()
                        if (result?.signedToday == true || result?.status == Status.NoSession) break
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "automatic check-in failed", t)
            }
        }
    }

    /**
     * Ensure today's sign-in, returning a result suitable for a manual button as well.
     * [force] retries the server even when the local date marker says today; this repairs a
     * stale marker without ever writing a successful date for an unconfirmed request.
     */
    suspend fun ensure(context: Context, force: Boolean = false): Result = gate.withLock {
        val app = context.applicationContext
        val today = today()

        // Check the session before trusting the local date marker.  A marker can
        // survive a WebView cookie eviction (or a manual logout), and returning
        // `AlreadySigned` in that state made the coordinator silently skip the
        // next login/sign-in cycle.  The marker is only a network-saving hint for
        // an account that still has a usable cookie.
        if (!ShizhijiaSession.hasSession(app)) {
            return@withLock Result(Status.NoSession, loggedIn = false)
        }
        if (!force && ShizhijiaSession.signDate(app) == today) {
            return@withLock Result(Status.AlreadySigned, loggedIn = true)
        }

        // A short probe retry absorbs the transient TLS/DNS failures common immediately
        // after process start. Do not clear the cookie on a false result: the endpoint also
        // returns false for a temporary network outage.
        var loggedIn = false
        for (attempt in 0 until LOGIN_ATTEMPTS) {
            loggedIn = runCatching { ShizhijiaApi.isLoggedIn(app) }.getOrDefault(false)
            if (loggedIn) break
            if (attempt + 1 < LOGIN_ATTEMPTS) delay(LOGIN_BACKOFF_MS[attempt])
        }

        if (loggedIn) {
            for (attempt in 0 until SIGN_ATTEMPTS) {
                if (runCatching { ShizhijiaApi.signIn(app) }.getOrDefault(false)) {
                    ShizhijiaSession.setSignDate(app, today)
                    Log.i(TAG, "automatic check-in accepted")
                    return@withLock Result(Status.Signed, loggedIn = true)
                }
                if (attempt + 1 < SIGN_ATTEMPTS) delay(SIGN_BACKOFF_MS[attempt])
            }
        }

        // Duplicate sign-ins are reported as a generic non-success code by the service. The
        // monthly log is the authoritative fallback and also handles a race with another
        // device or another lifecycle trigger.
        val already = runCatching { ShizhijiaApi.isSignedToday(app) }.getOrDefault(false)
        if (already) {
            ShizhijiaSession.setSignDate(app, today)
            Log.i(TAG, "automatic check-in already recorded by server")
            return@withLock Result(Status.AlreadySigned, loggedIn = loggedIn)
        }

        Result(Status.Failed, loggedIn = loggedIn)
    }

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private const val TAG = "ShizhijiaCheckIn"
    private const val LOGIN_ATTEMPTS = 3
    private const val SIGN_ATTEMPTS = 2
    private val LOGIN_BACKOFF_MS = longArrayOf(700L, 1_400L)
    private val SIGN_BACKOFF_MS = longArrayOf(800L)
    private val RETRY_BACKOFF_MS = longArrayOf(15_000L, 60_000L)
    private const val SCHEDULE_COALESCE_MS = 2_000L
}
