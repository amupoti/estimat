package com.marcel.estimat.ui

import com.marcel.estimat.testing.InMemorySharedPreferences
import com.marcel.estimat.testing.MainDispatcherRule
import com.marcel.estimat.testing.newViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GameSessionsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val state get() = viewModel.uiState.value
    private val preferences = InMemorySharedPreferences()
    private lateinit var viewModel: AppViewModel

    // Created after MainDispatcherRule has installed the test dispatcher, so the bank loads synchronously.
    @Before
    fun setUp() {
        viewModel = newViewModel(preferences)
    }

    private fun reopenApp() {
        viewModel = newViewModel(preferences)
    }

    private fun drawIds(count: Int): Set<String> = (1..count).map {
        viewModel.nextQuestion()
        checkNotNull(state.current).id
    }.toSet()

    @Test
    fun firstLaunchStartsWithOneEmptySession() {
        assertEquals(1, state.sessions.size)
        assertEquals(state.sessions.single().id, state.activeSessionId)
        assertTrue(state.playedIds.isEmpty())
    }

    @Test
    fun questionsPlayedBeforeSessionsExistedMoveToTheFirstSession() {
        val legacy = InMemorySharedPreferences()
        legacy.edit().putStringSet("played_questions", setOf("cat-easy", "spa-easy")).apply()

        viewModel = newViewModel(legacy)

        assertEquals(setOf("cat-easy", "spa-easy"), state.playedIds)
        assertNull(legacy.getStringSet("played_questions", null))
    }

    @Test
    fun newSessionHasEveryQuestionAvailableAgain() {
        drawIds(3)

        viewModel.createSession("Divendres")

        assertEquals(2, state.sessions.size)
        assertEquals("Divendres", state.activeSession?.name)
        assertTrue(state.playedIds.isEmpty())
        assertEquals(state.available.size, state.remainingCount)
        assertNull(state.current)
    }

    @Test
    fun eachSessionKeepsItsOwnPlayedQuestions() {
        val firstId = state.activeSessionId
        val firstPlayed = drawIds(2)
        viewModel.createSession("Second")
        val secondPlayed = drawIds(1)

        viewModel.selectSession(firstId)
        assertEquals(firstPlayed, state.playedIds)

        val secondId = state.sessions.single { it.id != firstId }.id
        viewModel.selectSession(secondId)
        assertEquals(secondPlayed, state.playedIds)
    }

    @Test
    fun sessionsAndActiveSessionSurviveReopeningTheApp() {
        drawIds(1)
        viewModel.createSession("Group")
        val played = drawIds(2)
        val activeId = state.activeSessionId

        reopenApp()

        assertEquals(2, state.sessions.size)
        assertEquals(activeId, state.activeSessionId)
        assertEquals("Group", state.activeSession?.name)
        assertEquals(played, state.playedIds)
        while (true) {
            viewModel.nextQuestion()
            val current = state.current ?: break
            assertFalse("${current.id} was repeated", current.id in played)
        }
    }

    @Test
    fun renamingASessionIsPersisted() {
        val id = state.activeSessionId

        viewModel.renameSession(id, "  Family  ")
        reopenApp()

        assertEquals("Family", state.activeSession?.name)
    }

    @Test
    fun deletingTheActiveSessionSwitchesToAnotherOne() {
        val firstId = state.activeSessionId
        val firstPlayed = drawIds(2)
        viewModel.createSession("Temp")
        val tempId = state.activeSessionId

        viewModel.deleteSession(tempId)
        reopenApp()

        assertEquals(listOf(firstId), state.sessions.map { it.id })
        assertEquals(firstId, state.activeSessionId)
        assertEquals(firstPlayed, state.playedIds)
        assertNull(preferences.getStringSet("session_${tempId}_played", null))
    }

    @Test
    fun deletingTheLastSessionLeavesAFreshOne() {
        val id = state.activeSessionId
        drawIds(2)

        viewModel.deleteSession(id)

        assertEquals(1, state.sessions.size)
        assertNotEquals(id, state.activeSessionId)
        assertTrue(state.playedIds.isEmpty())
    }

    @Test
    fun startingOverOnlyAffectsTheActiveSession() {
        val firstId = state.activeSessionId
        val firstPlayed = drawIds(2)
        viewModel.createSession("Other")
        drawIds(1)

        viewModel.restartDeck()
        viewModel.selectSession(firstId)

        assertEquals(firstPlayed, state.playedIds)
    }
}
