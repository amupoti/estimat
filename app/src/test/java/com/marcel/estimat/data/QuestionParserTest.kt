package com.marcel.estimat.data

import com.marcel.estimat.model.QuestionBank
import com.marcel.estimat.model.QuestionLanguage
import com.marcel.estimat.model.QuestionStatus
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionParserTest {
    @Test
    fun parsesAQuestion() {
        val questions = QuestionParser.parseQuestions(
            """
            [{
              "id": "cat-001",
              "pregunta": { "ca": "Quants km?", "es": "¿Cuántos km?" },
              "resposta": 172.5,
              "unitat": { "ca": "km", "es": "km" },
              "tags": ["catalunya", "mitja", "geografia"],
              "explicacio": null,
              "font": "https://example.com",
              "any_referencia": 2023,
              "confianca": "alta",
              "estat": "verificat"
            }]
            """,
        )
        val question = questions.single()
        assertEquals("¿Cuántos km?", question.text[QuestionLanguage.ES])
        assertEquals(172.5, question.answer, 0.0)
        assertEquals(setOf("catalunya", "mitja", "geografia"), question.tags)
        assertNull(question.explanation)
        assertEquals(2023, question.referenceYear)
        assertEquals(QuestionStatus.VERIFIED, question.status)
    }

    @Test
    fun parsesTheBundledQuestionBank() {
        // Unit tests run from the app module directory.
        val dir = File("src/main/assets/questions")
        val bank = QuestionParser.parse(
            questionsJson = File(dir, "questions.json").readText(),
            tagsJson = File(dir, "tags.json").readText(),
        )
        assertTrue(bank.questions.size >= 100)
        assertEquals(bank.questions.size, bank.questions.map { it.id }.toSet().size)
        val knownTags = bank.tagGroups.flatMap { group -> group.tags.map { it.id } }.toSet()
        bank.questions.forEach { question ->
            assertTrue("${question.id} has unknown tags", knownTags.containsAll(question.tags))
        }
        listOf(
            QuestionBank.GROUP_SCOPE,
            QuestionBank.GROUP_DIFFICULTY,
            QuestionBank.GROUP_THEME,
            QuestionBank.GROUP_TONE,
        ).forEach { groupId -> assertTrue(groupId, bank.group(groupId) != null) }
    }
}
