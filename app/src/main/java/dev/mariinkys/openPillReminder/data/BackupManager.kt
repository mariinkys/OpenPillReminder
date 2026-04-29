package dev.mariinkys.openPillReminder.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first
import org.json.JSONObject

object BackupManager {

    private const val BACKUP_VERSION = 1

    suspend fun createBackup(context: Context, uri: Uri): Result<Unit> {
        return try {
            val prefs = context.dataStore.data.first()

            val settings = JSONObject().apply {
                prefs.asMap().forEach { (key, value) ->
                    put(key.name, value)
                }
            }

            val backup = JSONObject().apply {
                put("version", BACKUP_VERSION)
                put("timestamp", System.currentTimeMillis())
                put("data", settings)
            }

            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(backup.toString(2).toByteArray())
            } ?: return Result.failure(Exception("Could not open output stream"))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreBackup(context: Context, uri: Uri): Result<Unit> {
        return try {
            val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().readText()
            } ?: return Result.failure(Exception("Could not open input stream"))

            val backup = JSONObject(json)
            val data = backup.getJSONObject("data")

            context.dataStore.edit { prefs ->
                prefs.clear()

                SettingsKeys.USER_NAME.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getString(key.name)
                }
                SettingsKeys.ACTIVE_PILLS.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getInt(key.name)
                }
                SettingsKeys.BREAK_DAYS.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getInt(key.name)
                }
                SettingsKeys.PLACEBO.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getBoolean(key.name)
                }
                SettingsKeys.FIRST_PILL_DATE.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getString(key.name)
                }
                SettingsKeys.REMINDER_HOUR.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getInt(key.name)
                }
                SettingsKeys.REMINDER_MINUTE.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getInt(key.name)
                }
                SettingsKeys.BUYING_REMINDER.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getBoolean(key.name)
                }
                SettingsKeys.BUYING_REMINDER_SCHEDULE.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getString(key.name)
                }
                SettingsKeys.BUYING_REMINDER_HOUR.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getInt(key.name)
                }
                SettingsKeys.BUYING_REMINDER_MINUTE.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getInt(key.name)
                }
                SettingsKeys.THEME_MODE.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getString(key.name)
                }
                SettingsKeys.DYNAMIC_COLOR.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getBoolean(key.name)
                }
                SettingsKeys.SEED_COLOR.let { key ->
                    if (data.has(key.name)) prefs[key] = data.getInt(key.name)
                }

                // pill_logs is stored as a raw string
                val pillLogsKey = androidx.datastore.preferences.core.stringPreferencesKey("pill_logs")
                if (data.has("pill_logs")) {
                    prefs[pillLogsKey] = data.getString("pill_logs")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}