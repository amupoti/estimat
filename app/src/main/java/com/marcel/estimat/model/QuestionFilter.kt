package com.marcel.estimat.model

enum class Audience {
    /** Everything except +18 questions. */
    GENERAL,

    /** Only questions tagged for kids. */
    KIDS,

    /** Everything, including +18 questions. */
    ADULTS,
    ;

    companion object {
        fun fromStoredValue(value: String?): Audience =
            entries.find { it.name == value } ?: GENERAL
    }
}

/**
 * Selected filters. Inside a tag group the selection is an OR; groups are
 * combined with AND. An empty selection in a group means "any".
 */
data class QuestionFilter(
    val scopes: Set<String> = emptySet(),
    val difficulties: Set<String> = emptySet(),
    val themes: Set<String> = emptySet(),
    val tones: Set<String> = emptySet(),
    val audience: Audience = Audience.GENERAL,
    val verifiedOnly: Boolean = false,
) {
    fun matches(question: Question): Boolean {
        if (question.status == QuestionStatus.REJECTED) return false
        if (verifiedOnly && question.status != QuestionStatus.VERIFIED) return false
        val tags = question.tags
        val audienceMatches = when (audience) {
            Audience.GENERAL -> QuestionBank.TAG_ADULTS !in tags
            Audience.KIDS -> QuestionBank.TAG_KIDS in tags
            Audience.ADULTS -> true
        }
        return audienceMatches &&
            tags.matchesAny(scopes) &&
            tags.matchesAny(difficulties) &&
            tags.matchesAny(themes) &&
            tags.matchesAny(tones)
    }

    fun apply(questions: List<Question>): List<Question> = questions.filter(::matches)

    private fun Set<String>.matchesAny(selected: Set<String>): Boolean =
        selected.isEmpty() || selected.any { it in this }
}
