package uz.carlcare.salesassistant.core.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class QuizContent(
    val question: String,
    val options: List<String>,
    val answer: Int,
    val explanation: String,
) {
    fun permute(order: List<Int>): QuizContent = copy(
        options = order.map { options[it] },
        answer = order.indexOf(answer),
    )
}

data class QuizQuestion(
    val id: String,
    val category: String,
    val ru: QuizContent,
    val en: QuizContent,
    val uz: QuizContent,
) {
    fun content(language: String): QuizContent = when (language) {
        "ru" -> ru
        "uz" -> uz
        else -> en
    }

    fun permuted(order: List<Int>): QuizQuestion = copy(
        ru = ru.permute(order),
        en = en.permute(order),
        uz = uz.permute(order),
    )
}

class QuizRepository(private val context: Context) {
    private val cache = MutableStateFlow<List<QuizQuestion>>(emptyList())

    fun observeQuestions(): StateFlow<List<QuizQuestion>> = cache

    suspend fun load() = withContext(Dispatchers.IO) {
        if (cache.value.isNotEmpty()) return@withContext
        val root = JSONObject(
            context.assets.open("quiz_questions.json").bufferedReader().use { it.readText() },
        )
        val items = root.getJSONArray("questions")
        val questions = buildList {
            for (i in 0 until items.length()) {
                val o = items.getJSONObject(i)
                add(
                    QuizQuestion(
                        id = o.getString("id"),
                        category = o.getString("category"),
                        ru = o.getQuizContent("ru"),
                        en = o.getQuizContent("en"),
                        uz = o.getQuizContent("uz"),
                    ),
                )
            }
        }
        cache.value = questions
    }
}

private fun JSONObject.getQuizContent(key: String): QuizContent {
    val o = getJSONObject(key)
    val options = o.getJSONArray("options")
    return QuizContent(
        question = o.getString("question"),
        options = (0 until options.length()).map { options.getString(it) },
        answer = o.getInt("answer"),
        explanation = o.getString("explanation"),
    )
}
