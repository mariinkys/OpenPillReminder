package dev.mariinkys.openPillReminder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.mariinkys.openPillReminder.model.BuyingReminderSchedule
import dev.mariinkys.openPillReminder.model.SettingsState
import dev.mariinkys.openPillReminder.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val HAS_REQUESTED_PERMISSIONS = booleanPreferencesKey("has_requested_permissions")
    val USER_NAME = stringPreferencesKey("user_name")
    val ACTIVE_PILLS = intPreferencesKey("active_pills")
    val BREAK_DAYS = intPreferencesKey("break_days")
    val PLACEBO = booleanPreferencesKey("placebo")
    val FIRST_PILL_DATE = stringPreferencesKey("first_pill_date")
    val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
    val BUYING_REMINDER = booleanPreferencesKey("buying_reminder")
    val BUYING_REMINDER_SCHEDULE = stringPreferencesKey("buying_reminder_schedule")
    val BUYING_REMINDER_HOUR = intPreferencesKey("buying_reminder_hour")
    val BUYING_REMINDER_MINUTE = intPreferencesKey("buying_reminder_minute")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    val SEED_COLOR = intPreferencesKey("seed_color")
    val PREVENT_SCREENSHOTS = booleanPreferencesKey("prevent_screenshots")
}

class SettingsRepository(private val context: Context) {

    val settingsFlow: Flow<SettingsState> = context.dataStore.data.map { prefs ->
        SettingsState(
            hasRequestedPermissions = prefs[SettingsKeys.HAS_REQUESTED_PERMISSIONS] ?: false,
            userName = prefs[SettingsKeys.USER_NAME] ?: "",
            activePills = prefs[SettingsKeys.ACTIVE_PILLS] ?: 21,
            breakDays = prefs[SettingsKeys.BREAK_DAYS] ?: 7,
            placebo = prefs[SettingsKeys.PLACEBO] ?: false,
            firstPillDate = prefs[SettingsKeys.FIRST_PILL_DATE]
                ?.let { LocalDate.parse(it) }
                ?: LocalDate.now(),
            reminderTime = LocalTime.of(
                prefs[SettingsKeys.REMINDER_HOUR] ?: 8,
                prefs[SettingsKeys.REMINDER_MINUTE] ?: 0
            ),
            buyingReminder = prefs[SettingsKeys.BUYING_REMINDER] ?: false,
            buyingReminderSchedule = BuyingReminderSchedule.valueOf(prefs[SettingsKeys.BUYING_REMINDER_SCHEDULE] ?: BuyingReminderSchedule.FirstPillDate.name),
            buyingReminderTime = LocalTime.of(
                prefs[SettingsKeys.BUYING_REMINDER_HOUR] ?: 8,
                prefs[SettingsKeys.BUYING_REMINDER_MINUTE] ?: 0
            ),
            themeMode = ThemeMode.valueOf(prefs[SettingsKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            useDynamicColor = prefs[SettingsKeys.DYNAMIC_COLOR] ?: true,
            seedColor = prefs[SettingsKeys.SEED_COLOR] ?: 0xFF6750A4.toInt(),
            preventScreenshots = prefs[SettingsKeys.PREVENT_SCREENSHOTS] ?: false,
        )
    }

    suspend fun saveSettings(settings: SettingsState) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.HAS_REQUESTED_PERMISSIONS] = settings.hasRequestedPermissions
            prefs[SettingsKeys.USER_NAME] = settings.userName
            prefs[SettingsKeys.ACTIVE_PILLS] = settings.activePills
            prefs[SettingsKeys.BREAK_DAYS] = settings.breakDays
            prefs[SettingsKeys.PLACEBO] = settings.placebo
            prefs[SettingsKeys.FIRST_PILL_DATE] = settings.firstPillDate.toString()
            prefs[SettingsKeys.REMINDER_HOUR] = settings.reminderTime.hour
            prefs[SettingsKeys.REMINDER_MINUTE] = settings.reminderTime.minute
            prefs[SettingsKeys.BUYING_REMINDER] = settings.buyingReminder
            prefs[SettingsKeys.BUYING_REMINDER_SCHEDULE] = settings.buyingReminderSchedule.name
            prefs[SettingsKeys.BUYING_REMINDER_HOUR] = settings.buyingReminderTime.hour
            prefs[SettingsKeys.BUYING_REMINDER_MINUTE] = settings.buyingReminderTime.minute
            prefs[SettingsKeys.THEME_MODE] = settings.themeMode.name
            prefs[SettingsKeys.DYNAMIC_COLOR] = settings.useDynamicColor
            prefs[SettingsKeys.SEED_COLOR] = settings.seedColor
            prefs[SettingsKeys.PREVENT_SCREENSHOTS] = settings.preventScreenshots
        }
    }
}