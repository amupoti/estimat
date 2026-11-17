package com.marcel.estimat.ui

import com.marcel.estimat.model.LocalizedText
import com.marcel.estimat.model.QuestionBank
import com.marcel.estimat.model.QuestionLanguage
import com.marcel.estimat.model.Tag
import com.marcel.estimat.model.TagGroup
import com.marcel.estimat.testing.InMemorySharedPreferences
import com.marcel.estimat.testing.MainDispatcherRule
import com.marcel.estimat.testing.newViewModel
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class QuestionLanguageSelectionTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val originalLocale: Locale = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    private fun languageOnFirstLaunch(deviceLocale: String): QuestionLanguage {
        Locale.setDefault(Locale.forLanguageTag(deviceLocale))
        return newViewModel().uiState.value.language
    }

    @Test
    fun firstLaunchOnSpanishDeviceUsesSpanish() {
        assertEquals(QuestionLanguage.ES, languageOnFirstLaunch("es-ES"))
    }

    @Test
    fun firstLaunchOnCatalanDeviceUsesCatalan() {
        assertEquals(QuestionLanguage.CA, languageOnFirstLaunch("ca-ES"))
    }

    @Test
    fun firstLaunchOnOtherDevicesFallsBackToCatalan() {
        assertEquals(QuestionLanguage.CA, languageOnFirstLaunch("en-US"))
        assertEquals(QuestionLanguage.CA, languageOnFirstLaunch("fr-FR"))
    }

    @Test
    fun selectingALanguageUpdatesTheState() {
        Locale.setDefault(Locale.forLanguageTag("ca-ES"))
        val viewModel = newViewModel()

        viewModel.setQuestionLanguage(QuestionLanguage.ES)
        assertEquals(QuestionLanguage.ES, viewModel.uiState.value.language)

        viewModel.setQuestionLanguage(QuestionLanguage.CA)
        assertEquals(QuestionLanguage.CA, viewModel.uiState.value.language)
    }

    @Test
    fun selectedLanguageIsKeptWhenTheAppIsReopened() {
        Locale.setDefault(Locale.forLanguageTag("ca-ES"))
        val preferences = InMemorySharedPreferences()
        newViewModel(preferences).setQuestionLanguage(QuestionLanguage.ES)

        assertEquals(QuestionLanguage.ES, newViewModel(preferences).uiState.value.language)
    }

    @Test
    fun selectedLanguageWinsOverTheDeviceLanguage() {
        val preferences = InMemorySharedPreferences()
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
        newViewModel(preferences).setQuestionLanguage(QuestionLanguage.CA)

        assertEquals(QuestionLanguage.CA, newViewModel(preferences).uiState.value.language)
    }

    @Test
    fun unknownStoredLanguageFallsBackToTheDeviceLanguage() {
        Locale.setDefault(Locale.forLanguageTag("es-ES"))
        val preferences = InMemorySharedPreferences()
        preferences.edit().putString("question_language", "FR").apply()

        assertEquals(QuestionLanguage.ES, newViewModel(preferences).uiState.value.language)
    }

    @Test
    fun textAndTagLabelsFollowTheSelectedLanguage() {
        val unit = LocalizedText(ca = "habitants", es = "habitantes")
        val bank = QuestionBank(
            questions = emptyList(),
            tagGroups = listOf(
                TagGroup("ambit", listOf(Tag("catalunya", LocalizedText("Catalunya", "Cataluña")))),
            ),
        )

        assertEquals("habitants", unit[QuestionLanguage.CA])
        assertEquals("habitantes", unit[QuestionLanguage.ES])
        assertEquals("Catalunya", bank.label("catalunya", QuestionLanguage.CA))
        assertEquals("Cataluña", bank.label("catalunya", QuestionLanguage.ES))
        // Unknown tags fall back to their id instead of crashing.
        assertEquals("desconegut", bank.label("desconegut", QuestionLanguage.ES))
    }

    @Test
    fun changingLanguageKeepsTheCurrentQuestionAndProgress() {
        val viewModel = newViewModel()
        viewModel.nextQuestion()
        viewModel.revealAnswer()
        val before = viewModel.uiState.value

        viewModel.setQuestionLanguage(QuestionLanguage.ES)

        val after = viewModel.uiState.value
        assertEquals(before.current, after.current)
        assertEquals(before.playedIds, after.playedIds)
        assertEquals(before.filter, after.filter)
        assertTrue(after.answerRevealed)
    }
}
