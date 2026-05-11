package dev.mariinkys.openPillReminder.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.time.LocalDate
import java.time.LocalTime
import dev.mariinkys.openPillReminder.R

data class SettingsState(
    val hasRequestedPermissions: Boolean = false,
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
    val seedColor: Int = 0xFF6750A4.toInt(), // Material Purple
    val preventScreenshots: Boolean = false,
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Composable
fun ThemeMode.toLocalizedDisplayString(): String {
    return when (this) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }
}

enum class BuyingReminderSchedule {
    FirstPillDate, DayBefore, TwoDaysBefore, ThreeDaysBefore, FourDaysBefore, FiveDaysBefore, SixDaysBefore, SevenDaysBefore
}

@Composable
fun BuyingReminderSchedule.toDisplayString(): String = when (this) {
    BuyingReminderSchedule.FirstPillDate -> stringResource(R.string.first_pill_day)
    BuyingReminderSchedule.DayBefore -> stringResource(R.string.day_before)
    BuyingReminderSchedule.TwoDaysBefore -> stringResource(R.string.two_days_before)
    BuyingReminderSchedule.ThreeDaysBefore -> stringResource(R.string.three_days_before)
    BuyingReminderSchedule.FourDaysBefore -> stringResource(R.string.four_days_before)
    BuyingReminderSchedule.FiveDaysBefore -> stringResource(R.string.five_days_before)
    BuyingReminderSchedule.SixDaysBefore -> stringResource(R.string.six_days_before)
    BuyingReminderSchedule.SevenDaysBefore -> stringResource(R.string.seven_days_before)
}