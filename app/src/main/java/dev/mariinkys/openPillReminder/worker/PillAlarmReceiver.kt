package dev.mariinkys.openPillReminder.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.mariinkys.openPillReminder.data.SettingsRepository
import dev.mariinkys.openPillReminder.sendPillNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class PillAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settingsFlow.first()

                val firstDate = settings.firstPillDate
                val cycleLength = (settings.activePills + settings.breakDays).coerceAtLeast(1)
                val today = LocalDate.now()
                val daysSinceStart = java.time.temporal.ChronoUnit.DAYS.between(firstDate, today)

                if (daysSinceStart >= 0) {
                    val positionInCycle = (daysSinceStart % cycleLength).toInt()
                    val isBreakDay = positionInCycle >= settings.activePills

                    if (!isBreakDay || settings.placebo) {
                        sendPillNotification(context, settings.userName, isBreakDay, today)
                    }
                }

                // Schedule tomorrow's alarm
                ReminderScheduler.schedulePillReminder(context, settings.reminderTime)
            } finally {
                pendingResult.finish()
            }
        }
    }
}