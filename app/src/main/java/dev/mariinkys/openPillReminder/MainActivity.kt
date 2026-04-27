package dev.mariinkys.openPillReminder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.mariinkys.openPillReminder.ui.AppLayout
import dev.mariinkys.openPillReminder.ui.theme.OpenPillReminderTheme

class MainActivity : ComponentActivity() {
    // Hold the date from the notification
    private var notificationDate by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        enableEdgeToEdge()

        // Get the date from the intent if it exists
        val initialDate = intent.getStringExtra("OPEN_LOG_DATE")

        setContent {
            OpenPillReminderTheme {
                AppLayout(notificationDate = initialDate)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Check intent when app is already running in background
        notificationDate = intent.getStringExtra("OPEN_LOG_DATE")
    }
}