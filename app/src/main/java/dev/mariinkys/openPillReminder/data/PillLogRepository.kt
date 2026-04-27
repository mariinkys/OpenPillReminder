package dev.mariinkys.openPillReminder.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.mariinkys.openPillReminder.model.PillLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class PillLogRepository(private val context: Context) {

    private val pillLogsKey = stringPreferencesKey("pill_logs")

    val pillLogsFlow: Flow<Map<LocalDate, PillLog>> = context.dataStore.data.map { prefs ->
        val json = prefs[pillLogsKey] ?: return@map emptyMap()
        parseLogs(json)
    }

    suspend fun saveLog(log: PillLog) {
        context.dataStore.edit { prefs ->
            val current = parseLogs(prefs[pillLogsKey] ?: "[]").toMutableMap()
            current[log.date] = log
            prefs[pillLogsKey] = serializeLogs(current)
        }
    }

    private fun parseLogs(json: String): Map<LocalDate, PillLog> {
        return try {
            val array = JSONArray(json)
            val map = mutableMapOf<LocalDate, PillLog>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val date = LocalDate.parse(obj.getString("date"))
                map[date] = PillLog(
                    date = date,
                    taken = obj.getBoolean("taken"),
                    note = obj.getString("note")
                )
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun serializeLogs(logs: Map<LocalDate, PillLog>): String {
        val array = JSONArray()
        logs.values.forEach { log ->
            val obj = JSONObject()
            obj.put("date", log.date.toString())
            obj.put("taken", log.taken)
            obj.put("note", log.note)
            array.put(obj)
        }
        return array.toString()
    }
}