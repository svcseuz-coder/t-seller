package uz.carlcare.salesassistant.core.data

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

sealed class AssistantResult {
    data class Success(val answer: String, val sources: List<String>) : AssistantResult()
    data object NotConfigured : AssistantResult()
    data object Unavailable : AssistantResult()
}

class AssistantRepository(
    private val catalog: CatalogRepository,
    private val guide: GuideRepository,
    private val service: ServiceRepository,
    private val proxyUrl: String,
    private val appToken: String,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    val configured: Boolean get() = proxyUrl.isNotBlank()

    suspend fun ask(
        question: String,
        language: String,
        history: List<Pair<Boolean, String>>,
    ): AssistantResult = withContext(Dispatchers.IO) {
        if (proxyUrl.isBlank()) return@withContext AssistantResult.NotConfigured

        val products = runCatching { catalog.observeAll().first() }.getOrDefault(emptyList())
        val guides = guide.observeCards().value
        val support = runCatching { service.observeSupport().first() }.getOrNull()
        val centers = runCatching { service.observeCenters().first() }.getOrDefault(emptyList())
        val context = AssistantContextBuilder.build(
            query = question,
            language = language,
            products = products,
            guides = guides,
            support = support,
            centers = centers,
        )

        val payload = JSONObject()
            .put("question", question)
            .put("language", language)
            .put("context", context.text)
            .put(
                "history",
                JSONArray().apply {
                    history.takeLast(6).forEach { (fromUser, text) ->
                        put(
                            JSONObject()
                                .put("role", if (fromUser) "user" else "assistant")
                                .put("content", text),
                        )
                    }
                },
            )

        val request = Request.Builder()
            .url(proxyUrl)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .apply { if (appToken.isNotBlank()) header("X-App-Token", appToken) }
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext AssistantResult.Unavailable
                val body = response.body?.string().orEmpty()
                val answer = JSONObject(body).optString("answer").trim()
                if (answer.isEmpty()) {
                    AssistantResult.Unavailable
                } else {
                    AssistantResult.Success(answer, context.sources)
                }
            }
        } catch (e: IOException) {
            AssistantResult.Unavailable
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AssistantResult.Unavailable
        }
    }
}
