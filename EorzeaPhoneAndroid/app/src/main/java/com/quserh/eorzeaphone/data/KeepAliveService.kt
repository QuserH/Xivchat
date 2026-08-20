package com.quserh.eorzeaphone.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Process
import androidx.core.app.NotificationCompat
import com.quserh.eorzeaphone.MainActivity
import com.quserh.eorzeaphone.R

/** Keeps the app process alive while its socket is connected in the background. */
class KeepAliveService : Service() {
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    override fun onCreate() {
        super.onCreate()
        createChannel()
        val pm = getSystemService(android.os.PowerManager::class.java)
        wakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "eorzeaphone:keepalive").apply { acquire() }
        startForeground(NOTIFICATION_ID, notification())
    }
    override fun onDestroy() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_EXIT) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            Process.killProcess(Process.myPid())
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "艾欧泽亚终端连接", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "保持游戏终端连接"
                    setShowBadge(false)
                },
            )
        }
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val exit = PendingIntent.getService(this, 1, Intent(this, KeepAliveService::class.java).setAction(ACTION_EXIT), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("艾欧泽亚终端正在运行")
            .setContentText("保持游戏数据连接")
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .addAction(0, "关闭软件", exit)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "eorzea-connection"
        private const val NOTIFICATION_ID = 4107
        private const val ACTION_EXIT = "com.quserh.eorzeaphone.EXIT"

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
        }
    }
}
