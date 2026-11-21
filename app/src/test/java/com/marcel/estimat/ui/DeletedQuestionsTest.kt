package com.marcel.estimat.ui

import com.marcel.estimat.testing.InMemorySharedPreferences
import com.marcel.estimat.testing.MainDispatcherRule
import com.marcel.estimat.testing.TestBank
import com.marcel.estimat.testing.newViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeletedQuestionsTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = InMemorySharedPreferences()
    private var viewModel = newViewModel(preferences)
    private val state get() = viewModel.uiState.value

    @Test
    fun deletingAQuestionDrawsAnotherOneAndShrinksTheDeck() {
        viewModel.nextQuestion()
        val deleted = requireNotNull(state.current).id
        val total = state.available.size

        viewModel.deleteQuestion(deleted)

        assertNotEquals(deleted, state.current?.id)
        assertEquals(total - 1, state.available.size)
        assertFalse(state.available.any { it.id == deleted })
    }

    @Test
    fun deletedQuestionNeverAppearsInAnySession() {
        viewModel.deleteQuestion(TestBank.catEasy.id)

        viewModel.createSession("Other group")
        val seen = buildList {
            repeat(state.remainingCount) {
                viewModel.nextQuestion()
                add(requireNotNull(state.current).id)
            }
        }

        assertFalse(TestBank.catEasy.id in seen)
        assertEquals(3, seen.size)
    }

    @Test
    fun restartingTheDeckDoesNotBringDeletedQuestionsBack() {
        viewModel.deleteQuestion(TestBank.catEasy.id)
        repeat(state.remainingCount) { viewModel.nextQuestion() }

        viewModel.restartDeck()

        assertFalse(state.available.any { it.id == TestBank.catEasy.id })
    }

    @Test
    fun deletedQuestionsAreRememberedWhenTheAppIsReopened() {
        viewModel.deleteQuestion(TestBank.catEasy.id)

        viewModel = newViewModel(preferences)

        assertTrue(TestBank.catEasy.id in state.deletedIds)
        assertFalse(state.available.any { it.id == TestBank.catEasy.id })
        // Still part of the bundled bank, just hidden on this device.
        assertTrue(state.bank!!.questions.any { it.id == TestBank.catEasy.id })
    }
}
