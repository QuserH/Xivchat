package com.quserh.eorzeaphone.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.ZonedDateTime

/** Schedules and cancels repeating device alarms that fire the bundled AlarmReceiver. */
object AlarmScheduler {
    const val EXTRA_ID = "alarm_id"
    const val EXTRA_LABEL = "alarm_label"
    const val EXTRA_HOUR = "alarm_hour"
    const val EXTRA_MINUTE = "alarm_minute"
    const val EXTRA_REPEAT = "alarm_repeat"
    const val EXTRA_TRIGGER_AT = "alarm_trigger_at"
    const val EXTRA_DETAIL = "alarm_detail"
    const val EXTRA_FISH_ID = "alarm_fish_id"
    const val EXTRA_WINDOW_END = "alarm_window_end"

    fun nextTrigger(hour: Int, minute: Int, repeatMask: Int): Long {
        val zone = java.time.ZoneId.systemDefault()
        val now = ZonedDateTime.now(zone)
        var candidate = now.withHour(hour.coerceIn(0, 23)).withMinute(minute.coerceIn(0, 59)).withSecond(0).withNano(0)
        if (repeatMask == 0) {
            if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
            return candidate.toInstant().toEpochMilli()
        }
        for (i in 0..7) {
            val bit = candidate.dayOfWeek.value - 1
            if (repeatMask and (1 shl bit) != 0 && candidate.isAfter(now)) {
                return candidate.toInstant().toEpochMilli()
            }
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    fun schedule(context: Context, id: Long, hour: Int, minute: Int, repeatMask: Int, label: String) {
        val pending = pending(context, id, hour, minute, repeatMask, label)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger(hour, minute, repeatMask), pending)
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextTrigger(hour, minute, repeatMask), pending)
        }
    }

    fun cancel(context: Context, id: Long) {
        val pending = pending(context, id, 0, 0, 0, "")
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }

    fun scheduleAt(
        context: Context,
        id: Long,
        triggerAt: Long,
        label: String,
        detail: String,
        fishId: Int = 0,
        windowEnd: Long = 0L,
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_TRIGGER_AT, triggerAt)
            putExtra(EXTRA_DETAIL, detail)
            putExtra(EXTRA_FISH_ID, fishId)
            putExtra(EXTRA_WINDOW_END, windowEnd)
        }
        val pending = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } catch (_: SecurityException) {
            manager.set(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun pending(context: Context, id: Long, hour: Int, minute: Int, repeatMask: Int, label: String): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_ID, id)
            putExtra(EXTRA_LABEL, label)
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
            putExtra(EXTRA_REPEAT, repeatMask)
        }
        return PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
