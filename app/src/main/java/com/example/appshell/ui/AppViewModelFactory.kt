package com.example.appshell.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.appshell.ReminderScheduler
import com.example.appshell.data.SettingsRepository

class AppViewModelFactory(
    private val repository: SettingsRepository,
    private val reminderScheduler: ReminderScheduler,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AppViewModel(repository, reminderScheduler) as T
}
