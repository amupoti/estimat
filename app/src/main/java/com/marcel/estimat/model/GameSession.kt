package com.marcel.estimat.model

/**
 * A group of games played with the same people. Every session starts with the
 * whole bank available and remembers which questions have already been shown,
 * so a group can keep playing over several days without repeats.
 *
 * A blank [name] means the session has no custom name; the UI shows a default.
 */
data class GameSession(
    val id: String,
    val name: String,
    val createdAt: Long,
    val playedIds: Set<String> = emptySet(),
    /** When a question was last drawn in this session, or 0 if never. */
    val lastPlayedAt: Long = 0L,
)

/** All saved sessions, in creation order, plus the one being played. */
data class GameSessions(
    val sessions: List<GameSession>,
    val activeId: String,
) {
    init {
        require(sessions.any { it.id == activeId }) { "Active session $activeId does not exist" }
    }

    val active: GameSession get() = sessions.first { it.id == activeId }

    /** Adds [session] and makes it the active one. */
    fun add(session: GameSession): GameSessions =
        GameSessions(sessions = sessions + session, activeId = session.id)

    fun select(id: String): GameSessions =
        if (sessions.any { it.id == id }) copy(activeId = id) else this

    fun rename(id: String, name: String): GameSessions =
        update(id) { it.copy(name = name.trim()) }

    /** Makes every question available again in the session [id]. */
    fun clearPlayed(id: String): GameSessions =
        update(id) { it.copy(playedIds = emptySet()) }

    fun updateActive(transform: (GameSession) -> GameSession): GameSessions =
        update(activeId, transform)

    /**
     * Removes the session [id]. Removing the active one activates the most
     * recent remaining session, or [replacement] when no session is left.
     */
    fun remove(id: String, replacement: () -> GameSession): GameSessions {
        val remaining = sessions.filterNot { it.id == id }
        if (remaining.size == sessions.size) return this
        return when {
            remaining.isEmpty() -> replacement().let { GameSessions(listOf(it), it.id) }
            id == activeId -> GameSessions(remaining, remaining.last().id)
            else -> copy(sessions = remaining)
        }
    }

    private fun update(id: String, transform: (GameSession) -> GameSession): GameSessions =
        copy(sessions = sessions.map { if (it.id == id) transform(it) else it })
}
