package com.marcel.estimat.testing

import com.marcel.estimat.data.SettingsRepository
import com.marcel.estimat.model.QuestionBank
import com.marcel.estimat.model.QuestionStatus
import com.marcel.estimat.model.TagGroup
import com.marcel.estimat.model.question
import com.marcel.estimat.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** Replaces Dispatchers.Main so `viewModelScope` runs synchronously in unit tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

/** A small, fixed bank covering every filter dimension. */
object TestBank {
    val catEasy = question("catalunya", "facil", "geografia", id = "cat-easy", status = QuestionStatus.VERIFIED)
    val catHard = question("catalunya", "dificil", "historia", id = "cat-hard")
    val spaEasy = question("espanya", "facil", "esport", "divertit", id = "spa-easy", status = QuestionStatus.VERIFIED)
    val intlKids = question("internacional", "facil", "nens", id = "intl-kids")
    val intlAdults = question("internacional", "mitja", "adults", id = "intl-adults")
    val rejected = question("catalunya", "facil", id = "rejected", status = QuestionStatus.REJECTED)

    val all = listOf(catEasy, catHard, spaEasy, intlKids, intlAdults, rejected)

    val bank = QuestionBank(questions = all, tagGroups = emptyList<TagGroup>())
}

/**
 * Builds a view model backed by [preferences]. Creating a second view model with
 * the same preferences simulates closing and reopening the app.
 */
fun newViewModel(
    preferences: InMemorySharedPreferences = InMemorySharedPreferences(),
    bank: QuestionBank = TestBank.bank,
): AppViewModel = AppViewModel(SettingsRepository(preferences)) { bank }
