package com.quserh.eorzeaphone.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.quserh.eorzeaphone.MainActivity
import com.quserh.eorzeaphone.data.ChatCategory
import com.quserh.eorzeaphone.R

class PhoneNotifier(private val context: Context) {
    private val manager = context.getSystemService(NotificationManager::class.java)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHAT_CHANNEL, "游戏聊天", NotificationManager.IMPORTANCE_DEFAULT))
            manager.createNotificationChannel(NotificationChannel(TELL_CHANNEL, "游戏私聊", NotificationManager.IMPORTANCE_HIGH))
            manager.createNotificationChannel(NotificationChannel(ALARM_CHANNEL, "闹钟", NotificationManager.IMPORTANCE_HIGH))
        }
    }

    fun alarm(title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, ALARM_CHANNEL)
        } else {
            @Suppress("DEPRECATION") android.app.Notification.Builder(context)
        }.setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify("alarm".hashCode(), notification)
        } catch (_: SecurityException) {
        }
    }

    fun chat(message: GameChatMessage, highPriority: Boolean, title: String? = null) {
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val convKey = when (message.category) {
            ChatCategory.Tell, ChatCategory.Linkshell, ChatCategory.FreeCompany, ChatCategory.Party -> message.conversationKey()
            else -> "local"
        }
        intent.putExtra(MainActivity.EXTRA_CONVERSATION_KEY, convKey)
        val notificationId = (message.timestamp xor message.sender.hashCode().toLong()).toInt()
        val pending = PendingIntent.getActivity(context, notificationId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val channel = if (highPriority) TELL_CHANNEL else CHAT_CHANNEL
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, channel)
        } else {
            @Suppress("DEPRECATION") android.app.Notification.Builder(context)
        }.setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title ?: message.sender.ifBlank { message.category.label })
            .setContentText(message.text)
            .setStyle(android.app.Notification.BigTextStyle().bigText(message.text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Android 13+ may not have notification permission yet.
        }
    }

    private companion object {
        const val CHAT_CHANNEL = "game-chat"
        const val TELL_CHANNEL = "game-tell"
        const val ALARM_CHANNEL = "game-alarm"
    }
}
