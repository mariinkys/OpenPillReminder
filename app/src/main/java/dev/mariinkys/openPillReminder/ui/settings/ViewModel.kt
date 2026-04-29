package dev.mariinkys.openPillReminder.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    private val _uiState = MutableStateFlow(SettingsState())
    val uiState: StateFlow<SettingsState> = _uiState.asStateFlow()

    val showPermissionRequest = combine(isLoaded, _uiState) { loaded, state ->
        loaded && !state.hasRequestedPermissions
    }

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
}