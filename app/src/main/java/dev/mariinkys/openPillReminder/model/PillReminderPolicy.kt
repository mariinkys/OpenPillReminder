package dev.mariinkys.openPillReminder.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object PillReminderPolicy {
    fun shouldSend(
        settings: SettingsState,
        date: LocalDate,
        alreadyTaken: Boolean,
    ): Boolean {
        if (!settings.pillReminderEnabled || alreadyTaken) return false

        val daysSinceStart = ChronoUnit.DAYS.between(settings.firstPillDate, date)
        if (daysSinceStart < 0) return false

        val cycleLength = (settings.activePills + settings.breakDays).coerceAtLeast(1)
        val positionInCycle = (daysSinceStart % cycleLength).toInt()
        val isBreakDay = positionInCycle >= settings.activePills

        return !isBreakDay || settings.placebo
    }
}
