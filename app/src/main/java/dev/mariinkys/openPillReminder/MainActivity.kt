package dev.mariinkys.openPillReminder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mariinkys.openPillReminder.ui.AppLayout
import dev.mariinkys.openPillReminder.ui.settings.SettingsViewModel
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
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.uiState.collectAsState()

            OpenPillReminderTheme(settings = settings) {
                AppLayout(notificationDate = initialDate, settingsViewModel = settingsViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Check intent when app is already running in background
        notificationDate = intent.getStringExtra("OPEN_LOG_DATE")
    }
}