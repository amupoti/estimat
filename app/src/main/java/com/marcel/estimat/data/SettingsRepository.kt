package com.marcel.estimat.data

import android.content.Context
import com.marcel.estimat.model.Audience
import com.marcel.estimat.model.QuestionFilter
import com.marcel.estimat.model.QuestionLanguage
import com.marcel.estimat.model.ThemePreference
import java.util.Locale

class SettingsRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun themePreference(): ThemePreference =
        ThemePreference.fromStoredValue(preferences.getString(THEME_PREFERENCE_KEY, null))

    fun saveThemePreference(themePreference: ThemePreference) {
        preferences.edit().putString(THEME_PREFERENCE_KEY, themePreference.name).apply()
    }

    fun questionLanguage(): QuestionLanguage =
        QuestionLanguage.fromStoredValue(preferences.getString(QUESTION_LANGUAGE_KEY, null))
            ?: QuestionLanguage.defaultFor(Locale.getDefault())

    fun saveQuestionLanguage(language: QuestionLanguage) {
        preferences.edit().putString(QUESTION_LANGUAGE_KEY, language.name).apply()
    }

    fun questionFilter(): QuestionFilter = QuestionFilter(
        scopes = stringSet(FILTER_SCOPES_KEY),
        difficulties = stringSet(FILTER_DIFFICULTIES_KEY),
        themes = stringSet(FILTER_THEMES_KEY),
        tones = stringSet(FILTER_TONES_KEY),
        audience = Audience.fromStoredValue(preferences.getString(FILTER_AUDIENCE_KEY, null)),
        verifiedOnly = preferences.getBoolean(FILTER_VERIFIED_ONLY_KEY, false),
    )

    fun saveQuestionFilter(filter: QuestionFilter) {
        preferences.edit()
            .putStringSet(FILTER_SCOPES_KEY, filter.scopes)
            .putStringSet(FILTER_DIFFICULTIES_KEY, filter.difficulties)
            .putStringSet(FILTER_THEMES_KEY, filter.themes)
            .putStringSet(FILTER_TONES_KEY, filter.tones)
            .putString(FILTER_AUDIENCE_KEY, filter.audience.name)
            .putBoolean(FILTER_VERIFIED_ONLY_KEY, filter.verifiedOnly)
            .apply()
    }

    /** Ids of questions already shown, kept across games so they are not repeated. */
    fun playedQuestionIds(): Set<String> = stringSet(PLAYED_QUESTIONS_KEY)

    fun savePlayedQuestionIds(ids: Set<String>) {
        preferences.edit().putStringSet(PLAYED_QUESTIONS_KEY, ids.toSet()).apply()
    }

    // SharedPreferences must not see the returned set mutated, so always copy it.
    private fun stringSet(key: String): Set<String> =
        preferences.getStringSet(key, null)?.toSet() ?: emptySet()

    private companion object {
        const val THEME_PREFERENCE_KEY = "theme_preference"
        const val QUESTION_LANGUAGE_KEY = "question_language"
        const val FILTER_SCOPES_KEY = "filter_scopes"
        const val FILTER_DIFFICULTIES_KEY = "filter_difficulties"
        const val FILTER_THEMES_KEY = "filter_themes"
        const val FILTER_TONES_KEY = "filter_tones"
        const val FILTER_AUDIENCE_KEY = "filter_audience"
        const val FILTER_VERIFIED_ONLY_KEY = "filter_verified_only"
        const val PLAYED_QUESTIONS_KEY = "played_questions"
    }
}
