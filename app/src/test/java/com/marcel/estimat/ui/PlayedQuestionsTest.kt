package com.marcel.estimat.ui

import com.marcel.estimat.model.QuestionFilter
import com.marcel.estimat.testing.InMemorySharedPreferences
import com.marcel.estimat.testing.MainDispatcherRule
import com.marcel.estimat.testing.newViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PlayedQuestionsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val state get() = viewModel.uiState.value
    private val preferences = InMemorySharedPreferences()
    private lateinit var viewModel: AppViewModel

    @Before
    fun openApp() {
        viewModel = newViewModel(preferences)
    }

    private fun reopenApp() {
        viewModel = newViewModel(preferences)
    }

    @Test
    fun drawnQuestionIsMarkedAsPlayedImmediately() {
        assertTrue(state.playedIds.isEmpty())

        viewModel.nextQuestion()

        val current = requireNotNull(state.current)
        assertEquals(setOf(current.id), state.playedIds)
    }

    @Test
    fun remainingCountDecreasesWithEachQuestion() {
        val total = state.remainingCount
        assertEquals(4, total)

        viewModel.nextQuestion()
        assertEquals(total - 1, state.remainingCount)
        viewModel.nextQuestion()
        assertEquals(total - 2, state.remainingCount)
    }

    @Test
    fun playedQuestionsAreNeverRepeated() {
        val seen = mutableListOf<String>()
        repeat(state.remainingCount) {
            viewModel.nextQuestion()
            seen += requireNotNull(state.current).id
        }

        assertEquals("a question was repeated: $seen", seen.size, seen.toSet().size)
        assertEquals(seen.toSet(), state.playedIds)
        assertEquals(0, state.remainingCount)
    }

    @Test
    fun noQuestionIsShownOnceEverythingWasPlayed() {
        repeat(state.remainingCount) { viewModel.nextQuestion() }

        viewModel.nextQuestion()

        assertNull(state.current)
        assertEquals(4, state.playedIds.size)
    }

    @Test
    fun revealingTheAnswerDoesNotUnmarkTheQuestion() {
        viewModel.nextQuestion()
        val id = requireNotNull(state.current).id

        viewModel.revealAnswer()

        assertTrue(state.answerRevealed)
        assertTrue(id in state.playedIds)
    }

    @Test
    fun nextQuestionHidesThePreviousAnswer() {
        viewModel.nextQuestion()
        viewModel.revealAnswer()

        viewModel.nextQuestion()

        assertFalse(state.answerRevealed)
    }

    @Test
    fun playedQuestionsAreRememberedWhenTheAppIsReopened() {
        viewModel.nextQuestion()
        viewModel.nextQuestion()
        val played = state.playedIds

        reopenApp()

        assertEquals(played, state.playedIds)
        assertEquals(2, state.remainingCount)
        repeat(2) {
            viewModel.nextQuestion()
            assertFalse(requireNotNull(state.current).id in played)
        }
    }

    @Test
    fun changingFiltersKeepsQuestionsMarkedAsPlayed() {
        viewModel.setQuestionFilter(QuestionFilter(scopes = setOf("catalunya")))
        repeat(2) { viewModel.nextQuestion() }

        viewModel.setQuestionFilter(QuestionFilter())

        assertEquals(setOf("cat-easy", "cat-hard"), state.playedIds)
        assertEquals(2, state.remainingCount)
    }

    @Test
    fun startingOverOnlyResetsQuestionsOfTheCurrentFilter() {
        repeat(4) { viewModel.nextQuestion() }
        viewModel.setQuestionFilter(QuestionFilter(scopes = setOf("catalunya")))
        assertEquals(0, state.remainingCount)

        viewModel.restartDeck()

        assertEquals(setOf("spa-easy", "intl-kids"), state.playedIds)
        assertEquals(2, state.remainingCount)
        assertNull(state.current)
    }

    @Test
    fun startingOverIsRememberedWhenTheAppIsReopened() {
        repeat(4) { viewModel.nextQuestion() }
        viewModel.restartDeck()

        reopenApp()

        assertTrue(state.playedIds.isEmpty())
        assertEquals(4, state.remainingCount)
    }
}
