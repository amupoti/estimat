package com.marcel.estimat.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.random.Random

class QuestionDeckTest {
    private val questions = listOf(question(id = "a"), question(id = "b"), question(id = "c"))

    @Test
    fun neverReturnsAPlayedQuestion() {
        repeat(20) { seed ->
            assertEquals("c", QuestionDeck.next(questions, setOf("a", "b"), Random(seed))?.id)
        }
    }

    @Test
    fun returnsNullWhenEverythingWasPlayed() {
        assertNull(QuestionDeck.next(questions, setOf("a", "b", "c")))
    }

    @Test
    fun remainingIgnoresPlayedIdsOutsideTheList() {
        assertEquals(2, QuestionDeck.remaining(questions, setOf("a", "zzz")).size)
    }
}
