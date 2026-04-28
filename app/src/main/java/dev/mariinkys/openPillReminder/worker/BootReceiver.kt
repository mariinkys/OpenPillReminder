package dev.mariinkys.openPillReminder.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.mariinkys.openPillReminder.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                val settings = SettingsRepository(context).settingsFlow.first()

                ReminderScheduler.schedulePillReminder(context, settings.reminderTime)

                if (settings.buyingReminder) {
                    ReminderScheduler.scheduleBuyingReminder(context, settings.buyingReminderTime)
                }

                pendingResult.finish()
            }
        }
    }
}