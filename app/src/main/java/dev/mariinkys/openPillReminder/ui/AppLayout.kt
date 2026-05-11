package dev.mariinkys.openPillReminder.ui

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mariinkys.openPillReminder.ui.home.HomeScreen
import dev.mariinkys.openPillReminder.ui.home.HomeViewModel
import dev.mariinkys.openPillReminder.ui.settings.SettingsScreen
import dev.mariinkys.openPillReminder.ui.settings.SettingsViewModel
import dev.mariinkys.openPillReminder.R
import android.content.Context
import android.view.WindowManager
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow


@Composable
private fun SecureWindow(enabled: Boolean) {
    val context = LocalContext.current
    DisposableEffect(enabled) {
        val window = (context as? Activity)?.window
        if (enabled) {
            window?.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose { }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLayout(
    settingsViewModel: SettingsViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(),
    notificationEvents: Flow<String>,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val settings by settingsViewModel.uiState.collectAsState()
    val isLoaded by settingsViewModel.isLoaded.collectAsState()
    val showPermissions by settingsViewModel.showPermissionRequest.collectAsState(initial = false)

    SecureWindow(enabled = settings.preventScreenshots)

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {}

    val exactAlarmPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}

    LaunchedEffect(showPermissions) {
        if (showPermissions) {
            settingsViewModel.markPermissionsRequested()

            // Request POST_NOTIFICATIONS (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                if (!granted) {
                    notificationPermissionLauncher.launch(
                        android.Manifest.permission.POST_NOTIFICATIONS
                    )
                }
            }

            // Request SCHEDULE_EXACT_ALARM (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager =
                    context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    ).apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                    exactAlarmPermissionLauncher.launch(intent)
                }
            }
        }
    }

    // if settings haven't loaded don't load the ui just yet
    if (!isLoaded) return

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Pair(stringResource(R.string.home), Icons.Default.Home),
        Pair(stringResource(R.string.settings), Icons.Default.Settings)
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val backupState by settingsViewModel.backupState.collectAsState()

    LaunchedEffect(backupState) {
        when (val state = backupState) {
            is SettingsViewModel.BackupUiState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                settingsViewModel.clearBackupState()
            }
            is SettingsViewModel.BackupUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
                settingsViewModel.clearBackupState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        val topBarTitle = if (selectedTab == 0) {
                            if (settings.userName.isNotBlank()) {
                                stringResource(R.string.welcome_user, settings.userName)
                            } else {
                                stringResource(R.string.welcome)
                            }
                        } else {
                            stringResource(R.string.settings)
                        }

                        Text(topBarTitle)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, (label, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(
                settings = settings,
                modifier = Modifier.padding(innerPadding),
                viewModel = homeViewModel,
                notificationEvents = notificationEvents,
            )
            1 -> SettingsScreen(
                settings = settings,
                onSettingsChange = { settingsViewModel.updateSettings(it) },
                backupState = backupState,
                onCreateBackup = { uri -> settingsViewModel.createBackup(uri) },
                onRestoreBackup = { uri -> settingsViewModel.restoreBackup(uri) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}