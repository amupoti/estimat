package com.marcel.estimat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcel.estimat.data.QuestionRepository
import com.marcel.estimat.data.SettingsRepository
import com.marcel.estimat.model.Question
import com.marcel.estimat.model.QuestionBank
import com.marcel.estimat.model.QuestionDeck
import com.marcel.estimat.model.QuestionFilter
import com.marcel.estimat.model.QuestionLanguage
import com.marcel.estimat.model.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val bank: QuestionBank? = null,
    val loadFailed: Boolean = false,
    val language: QuestionLanguage = QuestionLanguage.CA,
    val filter: QuestionFilter = QuestionFilter(),
    val playedIds: Set<String> = emptySet(),
    val current: Question? = null,
    val answerRevealed: Boolean = false,
) {
    /** Questions that match the current filters, played or not. */
    val available: List<Question> = bank?.let { filter.apply(it.questions) } ?: emptyList()

    val remainingCount: Int = QuestionDeck.remaining(available, playedIds).size
}

class AppViewModel(
    private val repository: SettingsRepository,
    private val questionRepository: QuestionRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AppUiState(
            themePreference = repository.themePreference(),
            language = repository.questionLanguage(),
            filter = repository.questionFilter(),
            playedIds = repository.playedQuestionIds(),
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState

    init {
        viewModelScope.launch {
            runCatching { questionRepository.load() }
                .onSuccess { bank -> _uiState.update { it.copy(bank = bank) } }
                .onFailure { _uiState.update { it.copy(loadFailed = true) } }
        }
    }

    fun nextQuestion() {
        val state = _uiState.value
        val question = QuestionDeck.next(state.available, state.playedIds)
        val playedIds = question?.let { state.playedIds + it.id } ?: state.playedIds
        repository.savePlayedQuestionIds(playedIds)
        _uiState.value = state.copy(
            current = question,
            answerRevealed = false,
            playedIds = playedIds,
        )
    }

    fun revealAnswer() {
        _uiState.update { it.copy(answerRevealed = true) }
    }

    /** Makes every question matching the current filters available again. */
    fun restartDeck() {
        val state = _uiState.value
        val playedIds = state.playedIds - state.available.map { it.id }.toSet()
        repository.savePlayedQuestionIds(playedIds)
        _uiState.value = state.copy(playedIds = playedIds, current = null, answerRevealed = false)
    }

    fun setQuestionLanguage(language: QuestionLanguage) {
        repository.saveQuestionLanguage(language)
        _uiState.update { it.copy(language = language) }
    }

    fun setQuestionFilter(filter: QuestionFilter) {
        repository.saveQuestionFilter(filter)
        _uiState.update { state ->
            val keepCurrent = state.current?.let(filter::matches) == true
            state.copy(
                filter = filter,
                current = if (keepCurrent) state.current else null,
                answerRevealed = keepCurrent && state.answerRevealed,
            )
        }
    }

    fun setThemePreference(themePreference: ThemePreference) {
        repository.saveThemePreference(themePreference)
        _uiState.update { it.copy(themePreference = themePreference) }
    }
}
