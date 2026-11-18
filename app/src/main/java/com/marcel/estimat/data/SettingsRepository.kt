package com.marcel.estimat.data

import android.content.Context
import android.content.SharedPreferences
import com.marcel.estimat.model.Audience
import com.marcel.estimat.model.GameSession
import com.marcel.estimat.model.GameSessions
import com.marcel.estimat.model.QuestionFilter
import com.marcel.estimat.model.QuestionLanguage
import com.marcel.estimat.model.ThemePreference
import java.util.Locale

class SettingsRepository(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE),
    )

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

    /**
     * Loads every saved session. The first time, the questions played before
     * sessions existed are moved into a first session so nothing is lost.
     */
    fun gameSessions(newSession: () -> GameSession): GameSessions {
        val ids = preferences.getString(SESSION_IDS_KEY, null)
            ?.split(SESSION_ID_SEPARATOR)
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val sessions = ids.map { id ->
            GameSession(
                id = id,
                name = preferences.getString(sessionKey(id, SESSION_NAME), null).orEmpty(),
                createdAt = preferences.getLong(sessionKey(id, SESSION_CREATED_AT), 0L),
                playedIds = stringSet(sessionKey(id, SESSION_PLAYED)),
                lastPlayedAt = preferences.getLong(sessionKey(id, SESSION_LAST_PLAYED_AT), 0L),
            )
        }
        if (sessions.isNotEmpty()) {
            val activeId = preferences.getString(ACTIVE_SESSION_KEY, null)
                ?.takeIf { id -> sessions.any { it.id == id } }
                ?: sessions.last().id
            return GameSessions(sessions, activeId)
        }

        val first = newSession().copy(playedIds = stringSet(LEGACY_PLAYED_QUESTIONS_KEY))
        val migrated = GameSessions(listOf(first), first.id)
        saveGameSessions(migrated)
        preferences.edit().remove(LEGACY_PLAYED_QUESTIONS_KEY).apply()
        return migrated
    }

    fun saveGameSessions(gameSessions: GameSessions) {
        val previousIds = preferences.getString(SESSION_IDS_KEY, null)
            ?.split(SESSION_ID_SEPARATOR)
            .orEmpty()
        val currentIds = gameSessions.sessions.map { it.id }
        val editor = preferences.edit()
        (previousIds - currentIds.toSet()).filter { it.isNotBlank() }.forEach { id ->
            editor.remove(sessionKey(id, SESSION_NAME))
                .remove(sessionKey(id, SESSION_CREATED_AT))
                .remove(sessionKey(id, SESSION_PLAYED))
                .remove(sessionKey(id, SESSION_LAST_PLAYED_AT))
        }
        gameSessions.sessions.forEach { session ->
            editor.putString(sessionKey(session.id, SESSION_NAME), session.name)
                .putLong(sessionKey(session.id, SESSION_CREATED_AT), session.createdAt)
                .putStringSet(sessionKey(session.id, SESSION_PLAYED), session.playedIds.toSet())
                .putLong(sessionKey(session.id, SESSION_LAST_PLAYED_AT), session.lastPlayedAt)
        }
        editor.putString(SESSION_IDS_KEY, currentIds.joinToString(SESSION_ID_SEPARATOR))
            .putString(ACTIVE_SESSION_KEY, gameSessions.activeId)
            .apply()
    }

    private fun sessionKey(id: String, field: String) = "session_${id}_$field"

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
        const val LEGACY_PLAYED_QUESTIONS_KEY = "played_questions"
        const val SESSION_IDS_KEY = "session_ids"
        const val ACTIVE_SESSION_KEY = "active_session_id"
        const val SESSION_ID_SEPARATOR = ","
        const val SESSION_NAME = "name"
        const val SESSION_CREATED_AT = "created_at"
        const val SESSION_LAST_PLAYED_AT = "last_played_at"
        const val SESSION_PLAYED = "played"
    }
}
