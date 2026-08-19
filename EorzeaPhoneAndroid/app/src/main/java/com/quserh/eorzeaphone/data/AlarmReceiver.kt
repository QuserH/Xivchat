package com.quserh.eorzeaphone.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires a device notification for a local alarm and re-arms repeating alarms. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, 0L)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL).orEmpty()
        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
        val repeat = intent.getIntExtra(AlarmScheduler.EXTRA_REPEAT, 0)
        val title = label.ifBlank { "闹钟" }
        val triggerAt = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_AT, 0L)
        val text = if (triggerAt > 0L) intent.getStringExtra(AlarmScheduler.EXTRA_DETAIL).orEmpty().ifBlank { "捕鱼窗口即将开始" }
            else "%02d:%02d".format(hour, minute)
        PhoneNotifier(context.applicationContext).alarm(title, text)
        if (triggerAt == 0L && repeat != 0 && id != 0L) {
            AlarmScheduler.schedule(context, id, hour, minute, repeat, label)
        }
        val fishId = intent.getIntExtra(AlarmScheduler.EXTRA_FISH_ID, 0)
        if (triggerAt > 0L && fishId > 0) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val catalog = FishingCatalogRepository.load(context.applicationContext)
                    val fish = catalog.fish.firstOrNull { it.id == fishId }
                    if (fish != null && FishingAlarmStore.isEnabled(context, fishId)) {
                        val after = maxOf(System.currentTimeMillis(), intent.getLongExtra(AlarmScheduler.EXTRA_WINDOW_END, 0L) + 1_000L)
                        FishingAlarmStore.scheduleNext(context, fish, catalog, after)
                    }
                } finally {
                    pending.finish()
                }
            }
        }
    }
}
