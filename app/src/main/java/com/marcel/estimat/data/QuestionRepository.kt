package com.marcel.estimat.data

import android.content.Context
import com.marcel.estimat.model.QuestionBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Source of the question bank; abstracted so the game logic can be tested without assets. */
fun interface QuestionSource {
    suspend fun load(): QuestionBank
}

/** Loads the question bank bundled in `assets/questions/`. */
class QuestionRepository(context: Context) : QuestionSource {
    private val assets = context.applicationContext.assets

    override suspend fun load(): QuestionBank = withContext(Dispatchers.IO) {
        QuestionParser.parse(
            questionsJson = readAsset("questions/questions.json"),
            tagsJson = readAsset("questions/tags.json"),
        )
    }

    private fun readAsset(path: String): String =
        assets.open(path).bufferedReader().use { it.readText() }
}
