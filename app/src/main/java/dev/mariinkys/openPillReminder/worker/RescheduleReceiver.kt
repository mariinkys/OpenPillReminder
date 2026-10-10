package dev.mariinkys.openPillReminder.worker

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.mariinkys.openPillReminder.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RescheduleReceiver : BroadcastReceiver() {
    @SuppressLint("InlinedApi")
    override fun onReceive(context: Context, intent: Intent) {
        val shouldReschedule = intent.action == Intent.ACTION_BOOT_COMPLETED ||
                intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
        if (!shouldReschedule) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settingsFlow.first()

                if (settings.pillReminderEnabled) {
                    ReminderScheduler.schedulePillReminder(
                        context,
                        settings.reminderTime,
                        settings.firstPillDate,
                    )

                    if (settings.buyingReminder) {
                        ReminderScheduler.scheduleBuyingReminder(context, settings.buyingReminderTime)
                    }
                } else {
                    ReminderScheduler.cancelPillAlarm(context)
                    ReminderScheduler.cancelBuyingAlarm(context)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
