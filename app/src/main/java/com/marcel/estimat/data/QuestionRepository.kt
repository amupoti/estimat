package com.marcel.estimat.data

import android.content.Context
import com.marcel.estimat.model.QuestionBank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads the question bank bundled in `assets/questions/`. */
class QuestionRepository(context: Context) {
    private val assets = context.applicationContext.assets

    suspend fun load(): QuestionBank = withContext(Dispatchers.IO) {
        QuestionParser.parse(
            questionsJson = readAsset("questions/questions.json"),
            tagsJson = readAsset("questions/tags.json"),
        )
    }

    private fun readAsset(path: String): String =
        assets.open(path).bufferedReader().use { it.readText() }
}
