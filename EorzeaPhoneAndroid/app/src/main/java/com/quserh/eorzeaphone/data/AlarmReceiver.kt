package com.quserh.eorzeaphone.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Fires a device notification for a local alarm and re-arms repeating alarms. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(AlarmScheduler.EXTRA_ID, 0L)
        val label = intent.getStringExtra(AlarmScheduler.EXTRA_LABEL).orEmpty()
        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 0)
        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)
        val repeat = intent.getIntExtra(AlarmScheduler.EXTRA_REPEAT, 0)
        val title = label.ifBlank { "闹钟" }
        val text = "%02d:%02d".format(hour, minute)
        PhoneNotifier(context.applicationContext).alarm(title, text)
        if (repeat != 0 && id != 0L) {
            AlarmScheduler.schedule(context, id, hour, minute, repeat, label)
        }
    }
}
