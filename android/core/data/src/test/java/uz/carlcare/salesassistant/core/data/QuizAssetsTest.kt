package uz.carlcare.salesassistant.core.data

import java.io.File
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizAssetsTest {

    private fun asset(name: String): JSONObject {
        val file = listOf("src/main/assets/$name", "core/data/src/main/assets/$name")
            .map(::File)
            .firstOrNull { it.exists() }
            ?: error("missing asset $name (cwd=${File(".").absolutePath})")
        return JSONObject(file.readText())
    }

    @Test
    fun `quiz bundle has 30 valid trilingual questions`() {
        val questions = asset("quiz_questions.json").getJSONArray("questions")
        assertEquals(30, questions.length())

        val ids = mutableSetOf<String>()
        val categories = mutableSetOf<String>()
        for (i in 0 until questions.length()) {
            val question = questions.getJSONObject(i)
            val id = question.getString("id")
            assertTrue("blank id", id.isNotBlank())
            assertTrue("duplicate id $id", ids.add(id))
            categories += question.getString("category")

            val answers = mutableSetOf<Int>()
            for (locale in listOf("ru", "en", "uz")) {
                val content = question.getJSONObject(locale)
                val label = "$id/$locale"
                assertTrue("$label: blank question", content.getString("question").isNotBlank())
                assertTrue("$label: blank explanation", content.getString("explanation").isNotBlank())
                val options = content.getJSONArray("options")
                assertEquals("$label: option count", 4, options.length())
                for (j in 0 until options.length()) {
                    assertTrue("$label: blank option $j", options.getString(j).isNotBlank())
                }
                val answer = content.getInt("answer")
                assertTrue("$label: answer out of range", answer in 0 until options.length())
                answers += answer
            }
            assertEquals("$id: locale answers differ", 1, answers.size)
        }

        assertEquals(30, ids.size)
        assertEquals(
            setOf("display", "platform", "memory", "camera", "battery", "connectivity", "durability", "service", "catalog"),
            categories,
        )
    }
}
