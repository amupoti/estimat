package com.marcel.estimat.model

import kotlin.random.Random

/**
 * Picks questions without repeating any that have already been played.
 * [playedIds] is kept across games so the same question does not come back
 * until the player starts over.
 */
object QuestionDeck {
    fun remaining(available: List<Question>, playedIds: Set<String>): List<Question> =
        available.filter { it.id !in playedIds }

    /** Returns a random unplayed question, or null when all of them have been played. */
    fun next(
        available: List<Question>,
        playedIds: Set<String>,
        random: Random = Random.Default,
    ): Question? = remaining(available, playedIds).randomOrNull(random)
}
