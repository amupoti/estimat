package com.marcel.estimat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marcel.estimat.data.QuestionRepository
import com.marcel.estimat.data.SettingsRepository

class AppViewModelFactory(
    private val repository: SettingsRepository,
    private val questionRepository: QuestionRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AppViewModel(repository, questionRepository) as T
}
