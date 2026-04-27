package dev.mariinkys.openPillReminder.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.mariinkys.openPillReminder.data.PillLogRepository
import dev.mariinkys.openPillReminder.model.PillLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PillLogRepository(application)

    val pillLogs: StateFlow<Map<LocalDate, PillLog>> = repository.pillLogsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    fun saveLog(log: PillLog) {
        viewModelScope.launch {
            repository.saveLog(log)
        }
    }
}