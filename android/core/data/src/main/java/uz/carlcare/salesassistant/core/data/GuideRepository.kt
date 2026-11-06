package uz.carlcare.salesassistant.core.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class GuideContent(
    val title: String,
    val what: String,
    val watch: String,
    val explain: String,
    val advantage: String,
    val important: String,
    val nuance: String,
    val examples: String,
)

data class GuideCard(
    val id: String,
    val ru: GuideContent,
    val en: GuideContent,
    val uz: GuideContent,
) {
    fun content(language: String): GuideContent = when (language) {
        "ru" -> ru
        "uz" -> uz
        else -> en
    }
}

class GuideRepository(private val context: Context) {
    private val cache = MutableStateFlow<List<GuideCard>>(emptyList())

    fun observeCards(): StateFlow<List<GuideCard>> = cache

    suspend fun load() = withContext(Dispatchers.IO) {
        if (cache.value.isNotEmpty()) return@withContext
        val root = JSONObject(
            context.assets.open("guide_cards.json").bufferedReader().use { it.readText() },
        )
        val items = root.getJSONArray("cards")
        val cards = buildList {
            for (i in 0 until items.length()) {
                val o = items.getJSONObject(i)
                add(
                    GuideCard(
                        id = o.getString("id"),
                        ru = o.getContent("ru"),
                        en = o.getContent("en"),
                        uz = o.getContent("uz"),
                    ),
                )
            }
        }
        cache.value = cards
    }
}

private fun JSONObject.getContent(key: String): GuideContent {
    val o = getJSONObject(key)
    return GuideContent(
        title = o.getString("title"),
        what = o.getString("what"),
        watch = o.getString("watch"),
        explain = o.getString("explain"),
        advantage = o.getString("advantage"),
        important = o.getString("important"),
        nuance = o.getString("nuance"),
        examples = o.getString("examples"),
    )
}
