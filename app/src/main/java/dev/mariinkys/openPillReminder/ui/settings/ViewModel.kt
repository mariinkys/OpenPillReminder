package dev.mariinkys.openPillReminder.ui.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.mariinkys.openPillReminder.data.BackupManager
import dev.mariinkys.openPillReminder.data.SettingsRepository
import dev.mariinkys.openPillReminder.model.SettingsState
import dev.mariinkys.openPillReminder.worker.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.FlowPreview::class)
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    // needed for showing the permissions only the first time the user opens the app
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    // ui state, pretty obvious
    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    // needed for showing the permissions only the first time the user opens the app
    val showPermissionRequest = combine(isLoaded, _uiState) { loaded, state ->
        loaded && !state.hasRequestedPermissions
    }

    // BACKUP & RESTORE FUNC
    sealed interface BackupUiState {
        data object Idle : BackupUiState
        data object Loading : BackupUiState
        data class Success(val message: String) : BackupUiState
        data class Error(val message: String) : BackupUiState
    }

    private val _backupState = MutableStateFlow<BackupUiState>(BackupUiState.Idle)
    val backupState: StateFlow<BackupUiState> = _backupState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load initial data once
            val initialSettings = repository.settingsFlow.first()
            _uiState.value = initialSettings
            _isLoaded.value = true

            // Save changes with debounce
            _uiState
                .debounce(500L)
                .collect { latestSettings ->
                    saveToDisk(latestSettings)
                }
        }
    }

    fun updateSettings(newSettings: SettingsState) {
        _uiState.value = newSettings
    }

    fun markPermissionsRequested() {
        _uiState.update { it.copy(hasRequestedPermissions = true) }
    }

    private suspend fun saveToDisk(settings: SettingsState) {
        repository.saveSettings(settings)
        ReminderScheduler.schedulePillReminder(getApplication(), settings.reminderTime)

        if (settings.buyingReminder) {
            ReminderScheduler.scheduleBuyingReminder(getApplication(), settings.buyingReminderTime)
        } else {
            ReminderScheduler.cancelBuyingAlarm(getApplication())
        }

    }

    fun createBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading
            BackupManager.createBackup(getApplication(), uri)
                .onSuccess { _backupState.value = BackupUiState.Success("Backup created successfully") }
                .onFailure { _backupState.value = BackupUiState.Error("Backup failed: ${it.message}") }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupUiState.Loading
            BackupManager.restoreBackup(getApplication(), uri)
                .onSuccess {
                    // reload settings from disk into UI state
                    _uiState.value = repository.settingsFlow.first()
                    _backupState.value = BackupUiState.Success("Backup restored successfully")
                }
                .onFailure { _backupState.value = BackupUiState.Error("Restore failed: ${it.message}") }
        }
    }

    fun clearBackupState() {
        _backupState.value = BackupUiState.Idle
    }
}