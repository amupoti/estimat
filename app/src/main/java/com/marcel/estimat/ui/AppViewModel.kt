package com.marcel.estimat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcel.estimat.data.QuestionSource
import com.marcel.estimat.data.SettingsRepository
import com.marcel.estimat.model.GameSession
import com.marcel.estimat.model.GameSessions
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
import java.util.UUID

data class AppUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val bank: QuestionBank? = null,
    val loadFailed: Boolean = false,
    val language: QuestionLanguage = QuestionLanguage.CA,
    val filter: QuestionFilter = QuestionFilter(),
    val sessions: List<GameSession> = emptyList(),
    val activeSessionId: String = "",
    val deletedIds: Set<String> = emptySet(),
    val current: Question? = null,
    val answerRevealed: Boolean = false,
) {
    val activeSession: GameSession? = sessions.firstOrNull { it.id == activeSessionId }

    /** Questions already shown in the active session. */
    val playedIds: Set<String> = activeSession?.playedIds.orEmpty()

    /** Questions that match the current filters, played or not; deleted ones never count. */
    val available: List<Question> = bank
        ?.let { filter.apply(it.questions) }
        ?.filterNot { it.id in deletedIds }
        ?: emptyList()

    val remainingCount: Int = QuestionDeck.remaining(available, playedIds).size
}

class AppViewModel(
    private val repository: SettingsRepository,
    private val questionRepository: QuestionSource,
) : ViewModel() {
    private var gameSessions: GameSessions = repository.gameSessions(::newSession)

    private val _uiState = MutableStateFlow(
        AppUiState(
            themePreference = repository.themePreference(),
            language = repository.questionLanguage(),
            filter = repository.questionFilter(),
            sessions = gameSessions.sessions,
            activeSessionId = gameSessions.activeId,
            deletedIds = repository.deletedQuestions(),
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
        if (question != null) {
            saveSessions(
                gameSessions.updateActive {
                    it.copy(
                        playedIds = it.playedIds + question.id,
                        lastPlayedAt = System.currentTimeMillis(),
                    )
                },
            )
        }
        _uiState.update { it.copy(current = question, answerRevealed = false) }
    }

    /**
     * Deletes [id] for good on this device: it stops appearing in every session,
     * but stays in the bundled question bank. The next question is drawn at once.
     */
    fun deleteQuestion(id: String) {
        val deleted = _uiState.value.deletedIds + id
        repository.saveDeletedQuestions(deleted)
        _uiState.update { it.copy(deletedIds = deleted) }
        nextQuestion()
    }

    fun revealAnswer() {
        _uiState.update { it.copy(answerRevealed = true) }
    }

    /** Makes every question matching the current filters available again. */
    fun restartDeck() {
        val availableIds = _uiState.value.available.map { it.id }.toSet()
        saveSessions(gameSessions.updateActive { it.copy(playedIds = it.playedIds - availableIds) })
        _uiState.update { it.copy(current = null, answerRevealed = false) }
    }

    /** Starts a new session with every question available and makes it active. */
    fun createSession(name: String) {
        saveSessions(gameSessions.add(newSession(name)), clearQuestion = true)
    }

    fun selectSession(id: String) {
        if (id == gameSessions.activeId) return
        saveSessions(gameSessions.select(id), clearQuestion = true)
    }

    fun renameSession(id: String, name: String) {
        saveSessions(gameSessions.rename(id, name))
    }

    /** Makes every question available again in the session [id]. */
    fun resetSession(id: String) {
        saveSessions(gameSessions.clearPlayed(id), clearQuestion = id == gameSessions.activeId)
    }

    /** Deletes a session; deleting the last one leaves a fresh, empty session. */
    fun deleteSession(id: String) {
        val wasActive = id == gameSessions.activeId
        saveSessions(gameSessions.remove(id) { newSession() }, clearQuestion = wasActive)
    }

    private fun saveSessions(sessions: GameSessions, clearQuestion: Boolean = false) {
        gameSessions = sessions
        repository.saveGameSessions(sessions)
        _uiState.update { state ->
            state.copy(
                sessions = sessions.sessions,
                activeSessionId = sessions.activeId,
                current = if (clearQuestion) null else state.current,
                answerRevealed = !clearQuestion && state.answerRevealed,
            )
        }
    }

    private fun newSession(name: String = ""): GameSession = GameSession(
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        createdAt = System.currentTimeMillis(),
    )

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
