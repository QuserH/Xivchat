package com.quserh.eorzeaphone.data

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.quserh.eorzeaphone.MainActivity
import com.quserh.eorzeaphone.R
import java.time.DayOfWeek
import java.time.ZoneOffset
import java.time.ZonedDateTime

class ResetReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val weekly = intent.getBooleanExtra("weekly", false)
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) manager.createNotificationChannel(NotificationChannel(CHANNEL, "游戏重置", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, CHANNEL)
        } else {
            @Suppress("DEPRECATION") android.app.Notification.Builder(context)
        }
        try {
            manager.notify(if (weekly) 202 else 201, builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(if (weekly) "每周重置即将到来" else "每日重置即将到来")
                .setContentText(if (weekly) "神典石与每周奖励将在约 30 分钟后重置" else "每日任务与筹备任务将在约 30 分钟后重置")
                .setContentIntent(open).setAutoCancel(true).build())
        } catch (_: SecurityException) { }
    }

    companion object {
        private const val CHANNEL = "game-reset"
        fun configure(context: Context, enabled: Boolean) {
            val alarms = context.getSystemService(AlarmManager::class.java)
            val daily = pending(context, false)
            val weekly = pending(context, true)
            alarms.cancel(daily); alarms.cancel(weekly)
            if (!enabled) return
            alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, next(false), AlarmManager.INTERVAL_DAY, daily)
            alarms.setInexactRepeating(AlarmManager.RTC_WAKEUP, next(true), AlarmManager.INTERVAL_DAY * 7, weekly)
        }

        private fun pending(context: Context, weekly: Boolean): PendingIntent = PendingIntent.getBroadcast(
            context, if (weekly) 202 else 201,
            Intent(context, ResetReminderReceiver::class.java).putExtra("weekly", weekly),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun next(weekly: Boolean): Long {
            var target = ZonedDateTime.now(ZoneOffset.UTC).withHour(if (weekly) 7 else 14).withMinute(30).withSecond(0).withNano(0)
            if (weekly) while (target.dayOfWeek != DayOfWeek.TUESDAY || !target.isAfter(ZonedDateTime.now(ZoneOffset.UTC))) target = target.plusDays(1)
            else if (!target.isAfter(ZonedDateTime.now(ZoneOffset.UTC))) target = target.plusDays(1)
            return target.toInstant().toEpochMilli()
        }
    }
}
