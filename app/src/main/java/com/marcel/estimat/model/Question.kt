package com.marcel.estimat.model

import java.text.NumberFormat
import java.util.Locale

/** Language used for question text. Independent from the app UI language. */
enum class QuestionLanguage(val code: String) {
    CA("ca"),
    ES("es"),
    ;

    val locale: Locale get() = Locale.forLanguageTag(code)

    companion object {
        fun fromStoredValue(value: String?): QuestionLanguage? =
            entries.find { it.name == value }

        /** Spanish devices get Spanish questions; everything else defaults to Catalan. */
        fun defaultFor(locale: Locale): QuestionLanguage =
            if (locale.language == "es") ES else CA
    }
}

data class LocalizedText(
    val ca: String,
    val es: String,
) {
    operator fun get(language: QuestionLanguage): String = when (language) {
        QuestionLanguage.CA -> ca
        QuestionLanguage.ES -> es
    }
}

enum class QuestionStatus {
    PENDING,
    VERIFIED,
    REJECTED,
    ;

    companion object {
        fun fromJsonValue(value: String): QuestionStatus = when (value) {
            "pendent" -> PENDING
            "verificat" -> VERIFIED
            "rebutjat" -> REJECTED
            else -> throw IllegalArgumentException("Unknown status: $value")
        }
    }
}

data class Question(
    val id: String,
    val text: LocalizedText,
    val answer: Double,
    val unit: LocalizedText,
    val tags: Set<String>,
    val explanation: LocalizedText?,
    val source: String,
    val referenceYear: Int?,
    val status: QuestionStatus,
)

data class Tag(
    val id: String,
    val label: LocalizedText,
)

data class TagGroup(
    val id: String,
    val tags: List<Tag>,
)

data class QuestionBank(
    val questions: List<Question>,
    val tagGroups: List<TagGroup>,
) {
    private val labels = tagGroups.flatMap { it.tags }.associate { it.id to it.label }

    fun group(id: String): TagGroup? = tagGroups.find { it.id == id }

    fun label(tagId: String, language: QuestionLanguage): String =
        labels[tagId]?.get(language) ?: tagId

    companion object {
        const val GROUP_SCOPE = "ambit"
        const val GROUP_DIFFICULTY = "dificultat"
        const val GROUP_THEME = "tema"
        const val GROUP_TONE = "to"
        const val TAG_KIDS = "nens"
        const val TAG_ADULTS = "adults"
        const val TAG_FUNNY = "divertit"
    }
}

/** Formats an answer with the separators of the question language (27.215, 172,5). */
fun formatAnswer(value: Double, language: QuestionLanguage): String {
    val format = NumberFormat.getNumberInstance(language.locale).apply {
        maximumFractionDigits = 2
        isGroupingUsed = true
    }
    return format.format(value)
}
