package com.marcel.estimat.ui

import com.marcel.estimat.model.Audience
import com.marcel.estimat.model.QuestionFilter
import com.marcel.estimat.testing.InMemorySharedPreferences
import com.marcel.estimat.testing.MainDispatcherRule
import com.marcel.estimat.testing.TestBank
import com.marcel.estimat.testing.newViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FiltersTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun AppViewModel.availableIds() = uiState.value.available.map { it.id }.toSet()

    /** Draws until the deck is empty and returns every question id shown. */
    private fun AppViewModel.drawAll(): Set<String> {
        val drawn = mutableSetOf<String>()
        while (true) {
            nextQuestion()
            val current = uiState.value.current ?: return drawn
            assertTrue("${current.id} was drawn twice", drawn.add(current.id))
        }
    }

    @Test
    fun defaultFilterHidesAdultAndRejectedQuestions() {
        val viewModel = newViewModel()

        assertEquals(setOf("cat-easy", "cat-hard", "spa-easy", "intl-kids"), viewModel.availableIds())
    }

    @Test
    fun scopeFilterKeepsOnlySelectedScopes() {
        val viewModel = newViewModel()

        viewModel.setQuestionFilter(QuestionFilter(scopes = setOf("catalunya", "espanya")))

        assertEquals(setOf("cat-easy", "cat-hard", "spa-easy"), viewModel.availableIds())
    }

    @Test
    fun differentGroupsMustAllMatch() {
        val viewModel = newViewModel()

        viewModel.setQuestionFilter(
            QuestionFilter(scopes = setOf("catalunya"), difficulties = setOf("facil")),
        )

        assertEquals(setOf("cat-easy"), viewModel.availableIds())
    }

    @Test
    fun themeAndToneFiltersAreApplied() {
        val viewModel = newViewModel()

        viewModel.setQuestionFilter(QuestionFilter(themes = setOf("historia", "esport")))
        assertEquals(setOf("cat-hard", "spa-easy"), viewModel.availableIds())

        viewModel.setQuestionFilter(QuestionFilter(tones = setOf("divertit")))
        assertEquals(setOf("spa-easy"), viewModel.availableIds())
    }

    @Test
    fun audienceSelectsKidsOrAdultQuestions() {
        val viewModel = newViewModel()

        viewModel.setQuestionFilter(QuestionFilter(audience = Audience.KIDS))
        assertEquals(setOf("intl-kids"), viewModel.availableIds())

        viewModel.setQuestionFilter(QuestionFilter(audience = Audience.ADULTS))
        assertEquals(
            setOf("cat-easy", "cat-hard", "spa-easy", "intl-kids", "intl-adults"),
            viewModel.availableIds(),
        )
    }

    @Test
    fun verifiedOnlyKeepsVerifiedQuestions() {
        val viewModel = newViewModel()

        viewModel.setQuestionFilter(QuestionFilter(verifiedOnly = true))

        assertEquals(setOf("cat-easy", "spa-easy"), viewModel.availableIds())
    }

    @Test
    fun filterWithNoMatchesLeavesNothingToDraw() {
        val viewModel = newViewModel()

        viewModel.setQuestionFilter(
            QuestionFilter(scopes = setOf("internacional"), difficulties = setOf("dificil")),
        )
        viewModel.nextQuestion()

        assertEquals(0, viewModel.uiState.value.remainingCount)
        assertNull(viewModel.uiState.value.current)
    }

    @Test
    fun drawnQuestionsAlwaysMatchTheActiveFilter() {
        val viewModel = newViewModel()
        viewModel.setQuestionFilter(QuestionFilter(scopes = setOf("catalunya")))

        assertEquals(setOf("cat-easy", "cat-hard"), viewModel.drawAll())
    }

    @Test
    fun changingFilterDropsCurrentQuestionThatNoLongerMatches() {
        val viewModel = newViewModel()
        viewModel.setQuestionFilter(QuestionFilter(scopes = setOf("espanya")))
        viewModel.nextQuestion()
        viewModel.revealAnswer()
        assertEquals("spa-easy", viewModel.uiState.value.current?.id)

        viewModel.setQuestionFilter(QuestionFilter(scopes = setOf("catalunya")))

        assertNull(viewModel.uiState.value.current)
        assertFalse(viewModel.uiState.value.answerRevealed)
    }

    @Test
    fun changingFilterKeepsCurrentQuestionThatStillMatches() {
        val viewModel = newViewModel()
        viewModel.setQuestionFilter(QuestionFilter(scopes = setOf("espanya")))
        viewModel.nextQuestion()
        viewModel.revealAnswer()

        viewModel.setQuestionFilter(QuestionFilter(scopes = setOf("espanya"), difficulties = setOf("facil")))

        assertEquals("spa-easy", viewModel.uiState.value.current?.id)
        assertTrue(viewModel.uiState.value.answerRevealed)
    }

    @Test
    fun filtersAreRestoredWhenTheAppIsReopened() {
        val preferences = InMemorySharedPreferences()
        val filter = QuestionFilter(
            scopes = setOf("catalunya", "espanya"),
            difficulties = setOf("facil"),
            themes = setOf("esport"),
            tones = setOf("divertit"),
            audience = Audience.ADULTS,
            verifiedOnly = true,
        )
        newViewModel(preferences).setQuestionFilter(filter)

        val reopened = newViewModel(preferences)

        assertEquals(filter, reopened.uiState.value.filter)
        assertEquals(setOf("spa-easy"), reopened.availableIds())
    }
}
