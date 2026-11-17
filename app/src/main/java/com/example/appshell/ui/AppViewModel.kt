package com.example.appshell.ui

import androidx.lifecycle.ViewModel
import com.example.appshell.ReminderScheduler
import com.example.appshell.data.SettingsRepository
import com.example.appshell.model.ReminderSettings
import com.example.appshell.model.ThemePreference
import java.time.LocalTime

data class AppUiState(
    val reminderSettings: ReminderSettings = ReminderSettings(),
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
)

class AppViewModel(
    private val repository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(
        AppUiState(
            reminderSettings = repository.reminderSettings(),
            themePreference = repository.themePreference(),
        ),
    )
    val uiState: kotlinx.coroutines.flow.StateFlow<AppUiState> = _uiState

    init {
        reminderScheduler.sync(repository.reminderSettings())
    }

    fun setRemindersEnabled(enabled: Boolean) {
        updateReminderSettings(_uiState.value.reminderSettings.copy(enabled = enabled))
    }

    fun setReminderTime(index: Int, time: LocalTime) {
        val current = _uiState.value.reminderSettings
        updateReminderSettings(
            if (index == 0) {
                current.copy(firstTime = time)
            } else {
                current.copy(secondTime = time)
            },
        )
    }

    fun setThemePreference(themePreference: ThemePreference) {
        repository.saveThemePreference(themePreference)
        _uiState.value = _uiState.value.copy(themePreference = themePreference)
    }

    private fun updateReminderSettings(settings: ReminderSettings) {
        repository.saveReminderSettings(settings)
        reminderScheduler.sync(settings)
        _uiState.value = _uiState.value.copy(reminderSettings = settings)
    }
}
