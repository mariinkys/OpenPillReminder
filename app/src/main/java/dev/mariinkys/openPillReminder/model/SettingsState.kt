package dev.mariinkys.openPillReminder.model

import java.time.LocalDate
import java.time.LocalTime

data class SettingsState(
    val userName: String = "",
    val activePills: Int = 21,
    val breakDays: Int = 7,
    val placebo: Boolean = false,
    val firstPillDate: LocalDate = LocalDate.now(),
    val reminderTime: LocalTime = LocalTime.of(8, 0),
    val buyingReminder: Boolean = false,
    val buyingReminderSchedule: BuyingReminderSchedule = BuyingReminderSchedule.FirstPillDate,
    val buyingReminderTime: LocalTime = LocalTime.of(8, 0),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val seedColor: Int = 0xFF6750A4.toInt() // Default Material Purple
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class BuyingReminderSchedule {
    FirstPillDate, DayBefore, TwoDaysBefore, ThreeDaysBefore, FourDaysBefore, FiveDaysBefore, SixDaysBefore, SevenDaysBefore
}

fun BuyingReminderSchedule.toDisplayString(): String = when (this) {
    BuyingReminderSchedule.FirstPillDate -> "On the first pill day"
    BuyingReminderSchedule.DayBefore -> "1 day before"
    BuyingReminderSchedule.TwoDaysBefore -> "2 days before"
    BuyingReminderSchedule.ThreeDaysBefore -> "3 days before"
    BuyingReminderSchedule.FourDaysBefore -> "4 days before"
    BuyingReminderSchedule.FiveDaysBefore -> "5 days before"
    BuyingReminderSchedule.SixDaysBefore -> "6 days before"
    BuyingReminderSchedule.SevenDaysBefore -> "7 days before"
}