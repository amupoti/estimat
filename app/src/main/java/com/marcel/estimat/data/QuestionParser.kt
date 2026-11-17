package com.marcel.estimat.data

import com.marcel.estimat.model.LocalizedText
import com.marcel.estimat.model.Question
import com.marcel.estimat.model.QuestionBank
import com.marcel.estimat.model.QuestionStatus
import com.marcel.estimat.model.Tag
import com.marcel.estimat.model.TagGroup
import org.json.JSONArray
import org.json.JSONObject

/** Parses `questions.json` and `tags.json` (see questions/README.md for the format). */
object QuestionParser {
    fun parse(questionsJson: String, tagsJson: String): QuestionBank = QuestionBank(
        questions = parseQuestions(questionsJson),
        tagGroups = parseTagGroups(tagsJson),
    )

    fun parseQuestions(json: String): List<Question> {
        val array = JSONArray(json)
        return (0 until array.length()).map { index -> parseQuestion(array.getJSONObject(index)) }
    }

    fun parseTagGroups(json: String): List<TagGroup> {
        val groups = JSONObject(json).getJSONObject("grups")
        return groups.keys().asSequence().map { groupId ->
            val tags = groups.getJSONObject(groupId).getJSONObject("tags")
            TagGroup(
                id = groupId,
                tags = tags.keys().asSequence()
                    .map { tagId -> Tag(tagId, tags.getJSONObject(tagId).toLocalizedText()) }
                    .toList(),
            )
        }.toList()
    }

    private fun parseQuestion(json: JSONObject): Question {
        val tags = json.getJSONArray("tags")
        return Question(
            id = json.getString("id"),
            text = json.getJSONObject("pregunta").toLocalizedText(),
            answer = json.getDouble("resposta"),
            unit = json.getJSONObject("unitat").toLocalizedText(),
            tags = (0 until tags.length()).map(tags::getString).toSet(),
            explanation = json.optJSONObject("explicacio")?.toLocalizedText(),
            source = json.getString("font"),
            referenceYear = if (json.isNull("any_referencia")) null else json.getInt("any_referencia"),
            status = QuestionStatus.fromJsonValue(json.getString("estat")),
        )
    }

    private fun JSONObject.toLocalizedText(): LocalizedText =
        LocalizedText(ca = getString("ca"), es = getString("es"))
}
