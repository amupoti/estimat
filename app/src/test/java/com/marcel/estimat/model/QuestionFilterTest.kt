package com.marcel.estimat.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionFilterTest {
    @Test
    fun emptyFilterHidesOnlyAdultAndRejectedQuestions() {
        val filter = QuestionFilter()
        assertTrue(filter.matches(question("catalunya", "mitja", "geografia")))
        assertFalse(filter.matches(question("catalunya", "mitja", "adults")))
        assertFalse(filter.matches(question("catalunya", status = QuestionStatus.REJECTED)))
    }

    @Test
    fun tagsInsideAGroupAreCombinedWithOr() {
        val filter = QuestionFilter(scopes = setOf("catalunya", "espanya"))
        assertTrue(filter.matches(question("catalunya")))
        assertTrue(filter.matches(question("espanya")))
        assertFalse(filter.matches(question("internacional")))
    }

    @Test
    fun groupsAreCombinedWithAnd() {
        val filter = QuestionFilter(scopes = setOf("catalunya"), difficulties = setOf("facil"))
        assertTrue(filter.matches(question("catalunya", "facil")))
        assertFalse(filter.matches(question("catalunya", "dificil")))
        assertFalse(filter.matches(question("espanya", "facil")))
    }

    @Test
    fun audienceControlsKidsAndAdultQuestions() {
        val kids = question("nens")
        val adults = question("adults")
        val general = question()
        assertEquals(listOf(kids), QuestionFilter(audience = Audience.KIDS).apply(listOf(kids, adults, general)))
        assertEquals(
            listOf(kids, adults, general),
            QuestionFilter(audience = Audience.ADULTS).apply(listOf(kids, adults, general)),
        )
    }

    @Test
    fun verifiedOnlyHidesPendingQuestions() {
        val filter = QuestionFilter(verifiedOnly = true)
        assertTrue(filter.matches(question(status = QuestionStatus.VERIFIED)))
        assertFalse(filter.matches(question(status = QuestionStatus.PENDING)))
    }
}

internal fun question(
    vararg tags: String,
    id: String = tags.joinToString("-").ifEmpty { "q" },
    status: QuestionStatus = QuestionStatus.PENDING,
) = Question(
    id = id,
    text = LocalizedText("Pregunta?", "¿Pregunta?"),
    answer = 1.0,
    unit = LocalizedText("", ""),
    tags = tags.toSet(),
    explanation = null,
    source = "https://example.com",
    referenceYear = null,
    status = status,
)
