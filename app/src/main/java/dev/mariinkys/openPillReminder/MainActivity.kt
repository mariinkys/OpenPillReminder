package dev.mariinkys.openPillReminder

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mariinkys.openPillReminder.ui.AppLayout
import dev.mariinkys.openPillReminder.ui.settings.SettingsViewModel
import dev.mariinkys.openPillReminder.ui.theme.OpenPillReminderTheme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationChannel = Channel<String>(Channel.BUFFERED)
    val notificationEvents = notificationChannel.receiveAsFlow()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel(this)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val settings by settingsViewModel.uiState.collectAsState()

            OpenPillReminderTheme(settings = settings) {
                AppLayout(notificationEvents = notificationEvents, settingsViewModel = settingsViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        intent.getStringExtra("OPEN_LOG_DATE")?.let { date ->
            lifecycleScope.launch {
                notificationChannel.send(date)
            }
        }
    }
}