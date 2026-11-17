package com.marcel.estimat.model

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class QuestionTest {
    @Test
    fun formatsAnswersWithCatalanAndSpanishSeparators() {
        assertEquals("27.215", formatAnswer(27215.0, QuestionLanguage.CA))
        assertEquals("172,5", formatAnswer(172.5, QuestionLanguage.CA))
        assertEquals("9,58", formatAnswer(9.58, QuestionLanguage.ES))
        assertEquals("915.103.765", formatAnswer(915_103_765.0, QuestionLanguage.ES))
        assertEquals("580", formatAnswer(580.0, QuestionLanguage.ES))
    }

    @Test
    fun defaultsToSpanishOnlyOnSpanishDevices() {
        assertEquals(QuestionLanguage.ES, QuestionLanguage.defaultFor(Locale.forLanguageTag("es-ES")))
        assertEquals(QuestionLanguage.CA, QuestionLanguage.defaultFor(Locale.forLanguageTag("ca-ES")))
        assertEquals(QuestionLanguage.CA, QuestionLanguage.defaultFor(Locale.ENGLISH))
    }
}
