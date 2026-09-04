package com.quserh.eorzeaphone.data.market

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.quserh.eorzeaphone.MainActivity
import com.quserh.eorzeaphone.R
import com.quserh.eorzeaphone.data.GameMarketMonitorEvent
import com.quserh.eorzeaphone.data.GameMonitorEventKind
import com.quserh.eorzeaphone.data.wiki.WikiDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Periodic price check for watched items -> phone notification.
 *
 * `setInexactRepeating` on a 30min cadence, matching how ResetReminderReceiver
 * schedules. Inexact is deliberate: market prices don't need minute precision and
 * exact alarms cost a special permission on S+.
 *
 * Dedupe lives in [MarketRepository.checkAlerts] (only renotify on a lower price
 * or after 12h), so this receiver just delivers whatever it is handed.
 */
class MarketAlertReceiver : BroadcastReceiver() {

    /**
     * `goAsync()` is required, not optional: a bare `CoroutineScope(...).launch`
     * gets torn down as soon as onReceive returns, so the network call never
     * completed and no notification ever fired. goAsync holds the broadcast alive
     * until finish() is called.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hits = runCatching { MarketRepository.checkAlerts(app) }
                    .onFailure { Log.e(TAG, "checkAlerts failed", it) }
                    .getOrDefault(emptyList())
                Log.i(TAG, "checkAlerts -> ${hits.size} hit(s)")
                if (hits.isNotEmpty()) {
                    ensureChannel(app)
                    hits.forEach { notify(app, it) }
                }
            } finally {
                pending.finish()
            }
        }
    }

    internal suspend fun notify(context: Context, hit: MarketRepository.AlertHit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        val name = runCatching { WikiDb.byId(context, hit.itemId)?.nameCn }
            .getOrNull() ?: "道具 ${hit.itemId}"
        val reason = when (hit.mode) {
            MarketRepository.AlertMode.Ratio -> hit.reference
                ?.let { "均价 ${group(it.toInt())}，现价约 %.2f 倍".format(hit.price / it) }
                ?: "低于设定倍率"
            MarketRepository.AlertMode.Absolute -> "已进入设定价格区间"
            MarketRepository.AlertMode.None -> ""
        }

        val pending = PendingIntent.getActivity(
            context, hit.itemId,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, CHANNEL)
        } else {
            @Suppress("DEPRECATION") Notification.Builder(context)
        }
        val n = builder
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$name ${group(hit.price)} gil")
            .setContentText(listOf(hit.scope, reason).filter { it.isNotBlank() }
                .joinToString("  ·  "))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching {
            context.getSystemService(NotificationManager::class.java)
                .notify(NOTIFY_BASE + hit.itemId, n)
        }
    }

    /**
     * Notification for a plugin-pushed monitor event. Unlike the alert poll these
     * arrive over the socket while the game is running, so they surface the moment
     * a threshold is crossed or an auto-buy happens. Reuses the market channel and
     * per-item notification id so a stream of events replaces, not stacks.
     */

    companion object {
        internal fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val mgr = context.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(
                CHANNEL, "市场降价", NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "关注的道具达到设定价格时提醒"
                enableVibration(true)
            }
            mgr.createNotificationChannel(ch)
        }

        private fun group(v: Int): String {
            val s = v.toString()
            return if (s.length <= 3) s else s.reversed().chunked(3)
                .joinToString(",").reversed()
        }

        suspend fun notifyMonitor(context: Context, event: GameMarketMonitorEvent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) return

            val title: String
            val body: String
            when (event.kind) {
                GameMonitorEventKind.Purchased -> {
                    title = "已自动购买 ${WikiDb.byId(context, event.itemId)?.nameCn ?: "道具 ${event.itemId}"}"
                    body = buildList {
                        add("${group(event.price)} gil × ${event.quantity}")
                        if (event.detail.isNotBlank()) add(event.detail)
                    }.joinToString("  ·  ")
                }
                GameMonitorEventKind.BuyFailed -> {
                    title = "自动购买失败 ${WikiDb.byId(context, event.itemId)?.nameCn ?: "道具 ${event.itemId}"}"
                    body = event.detail.ifBlank { "游戏端拒绝了这笔交易" }
                }
                GameMonitorEventKind.CapReached -> {
                    title = "已达购买上限 ${WikiDb.byId(context, event.itemId)?.nameCn ?: "道具 ${event.itemId}"}"
                    body = event.detail.ifBlank { "不再自动买入这条规则" }
                }
                else -> {
                    title = "${WikiDb.byId(context, event.itemId)?.nameCn ?: "道具 ${event.itemId}"} 降价了"
                    body = buildList {
                        add("${group(event.price)} gil")
                        if (event.detail.isNotBlank()) add(event.detail)
                    }.joinToString("  ·  ")
                }
            }

            ensureChannel(context)
            val pending = PendingIntent.getActivity(
                context, event.itemId,
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, CHANNEL)
            } else {
                @Suppress("DEPRECATION") Notification.Builder(context)
            }
            val n = builder
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(pending)
                .setAutoCancel(true)
                .build()
            runCatching {
                context.getSystemService(NotificationManager::class.java)
                    .notify(NOTIFY_BASE + event.itemId, n)
            }
        }

        private const val TAG = "MarketAlert"
        const val CHANNEL = "market-alert"
        private const val NOTIFY_BASE = 0x4D41_0000
        private const val INTERVAL_MS = 30 * 60 * 1000L
        private const val FIRST_DELAY_MS = 20 * 1000L

        /**
         * Start or stop the poll. Safe to call repeatedly.
         *
         * First check runs [FIRST_DELAY_MS] after configuring rather than a full
         * interval out: someone who just set an alert on an item that already
         * qualifies should hear about it now, not in half an hour.
         */
        fun configure(context: Context, enabled: Boolean) {
            val alarms = context.getSystemService(AlarmManager::class.java) ?: return
            val p = pending(context)
            alarms.cancel(p)
            if (!enabled) return
            alarms.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + FIRST_DELAY_MS,
                INTERVAL_MS,
                p,
            )
        }

        /** Run one check now, off the alarm schedule. Used right after saving a rule. */
        suspend fun checkNow(context: Context) {
            val app = context.applicationContext
            val hits = runCatching { MarketRepository.checkAlerts(app) }
                .onFailure { Log.e(TAG, "checkAlerts failed", it) }
                .getOrDefault(emptyList())
            Log.i(TAG, "checkNow -> ${hits.size} hit(s)")
            if (hits.isEmpty()) return
            val r = MarketAlertReceiver()
            ensureChannel(app)
            hits.forEach { r.notify(app, it) }
        }

        private fun pending(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context, 0x4D41,
            Intent(context, MarketAlertReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
