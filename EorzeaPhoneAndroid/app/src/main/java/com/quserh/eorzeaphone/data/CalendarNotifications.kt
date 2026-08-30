package com.quserh.eorzeaphone.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

/** One watched calendar event, persisted so alarms survive reboot. */
data class EventNotice(val id: Long, val name: String, val color: Int, val startTime: Long, val endTime: Long)

/**
 * Schedules "event started" and "ends tomorrow" alarms for Shizhijia calendar
 * events. The watched list is persisted; alarms are re-armed on boot.
 */
object CalendarNotifications {
    const val CHANNEL_ID = "shizhijia_events"
    private const val PREF = "calendarNotices"
    const val ACTION_FIRE = "com.quserh.eorzeaphone.EVENT_NOTICE"
    const val ACTION_BOOT = "android.intent.action.BOOT_COMPLETED"
    private const val DAY = 86_400_000L

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ch = NotificationChannel(CHANNEL_ID, "活动提醒", NotificationManager.IMPORTANCE_DEFAULT)
        ch.description = "石之家官方活动开始与即将结束提醒"
        nm.createNotificationChannel(ch)
    }

    private fun prefs(context: Context) = context.getSharedPreferences("calendar_events", Context.MODE_PRIVATE)

    fun loadWatched(context: Context): List<EventNotice> {
        val arr = runCatching { JSONArray(prefs(context).getString(PREF, "[]")) }.getOrDefault(JSONArray())
        return buildList(arr.length()) {
            for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { jo ->
                add(EventNotice(jo.optLong("id"), jo.optString("name"), jo.optInt("color"), jo.optLong("start"), jo.optLong("end")))
            }
        }
    }

    fun saveWatched(context: Context, events: List<EventNotice>) {
        val arr = JSONArray()
        events.forEach { e ->
            arr.put(JSONObject().put("id", e.id).put("name", e.name).put("color", e.color).put("start", e.startTime).put("end", e.endTime))
        }
        prefs(context).edit().putString(PREF, arr.toString()).apply()
    }

    /** Replace all scheduled alarms with the given events (called when the calendar loads). */
    fun schedule(context: Context, events: List<EventNotice>) {
        ensureChannel(context)
        saveWatched(context, events)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        events.forEach { e ->
            cancel(context, e)
            if (e.startTime > now) fireAt(context, e, e.startTime, kind = 0)
            val warnAt = e.endTime - DAY
            if (warnAt > now && e.endTime > now) fireAt(context, e, warnAt, kind = 1)
        }
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        loadWatched(context).forEach { cancel(context, it) }
        saveWatched(context, emptyList())
    }

    private fun cancel(context: Context, e: EventNotice) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(fireIntent(context, e, 0))
        am.cancel(fireIntent(context, e, 1))
    }

    private fun fireAt(context: Context, e: EventNotice, at: Long, kind: Int) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = fireIntent(context, e, kind)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (se: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    private fun fireIntent(context: Context, e: EventNotice, kind: Int): PendingIntent {
        val intent = Intent(context, ShizhijiaEventAlarmReceiver::class.java).setAction(ACTION_FIRE)
            .putExtra("id", e.id).putExtra("name", e.name).putExtra("color", e.color)
            .putExtra("start", e.startTime).putExtra("end", e.endTime).putExtra("kind", kind)
        val code = ((e.id % 100000L) * 2L + kind).toInt()
        return PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun notifyEvent(context: Context, e: EventNotice, kind: Int) {
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, com.quserh.eorzeaphone.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (kind == 0) "活动开始" else "活动即将结束"
        val body = if (kind == 0) e.name else "${e.name} 明天结束，尽快完成！"
        val n = androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.quserh.eorzeaphone.R.drawable.ic2_calendar)
            .setContentTitle(title).setContentText(body)
            .setColor(e.color).setAutoCancel(true).setContentIntent(open)
            .build()
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(((e.id % 100000L) * 2L + kind).toInt(), n)
        // one-shot consumed: drop from the watched list
        saveWatched(context, loadWatched(context).filterNot { it.id == e.id && ((it.startTime > System.currentTimeMillis()) == (kind == 0)) })
    }

    fun rescheduleFromSaved(context: Context) {
        schedule(context, loadWatched(context))
    }
}

/** Fires the start / expiring-soon notification, and re-arms alarms after boot. */
class ShizhijiaEventAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            CalendarNotifications.ACTION_BOOT -> CalendarNotifications.rescheduleFromSaved(context)
            CalendarNotifications.ACTION_FIRE -> {
                val e = EventNotice(
                    id = intent.getLongExtra("id", 0L),
                    name = intent.getStringExtra("name") ?: return,
                    color = intent.getIntExtra("color", 0xFFEDCA7F.toInt()),
                    startTime = intent.getLongExtra("start", 0L),
                    endTime = intent.getLongExtra("end", 0L),
                )
                CalendarNotifications.notifyEvent(context, e, intent.getIntExtra("kind", 0))
            }
        }
    }
}
