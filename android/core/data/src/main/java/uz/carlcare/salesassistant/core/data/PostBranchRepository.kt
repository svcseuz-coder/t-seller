package uz.carlcare.salesassistant.core.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class PostBranch(
    val id: Int,
    val name: String,
    val region: String,
    val city: String,
    val address: String,
    val phones: List<String>,
    val hours: String,
    val lat: Double,
    val lng: Double,
)

data class PostBranches(
    val checkedAt: String,
    val branches: List<PostBranch> = emptyList(),
)

class PostBranchRepository(private val context: Context) {
    private val cache = MutableStateFlow(PostBranches(checkedAt = ""))

    fun observeBranches(): StateFlow<PostBranches> = cache

    suspend fun load() = withContext(Dispatchers.IO) {
        if (cache.value.branches.isNotEmpty()) return@withContext
        val root = JSONObject(
            context.assets.open("post_branches.json").bufferedReader().use { it.readText() },
        )
        val items = root.getJSONArray("branches")
        val branches = buildList {
            for (i in 0 until items.length()) {
                val o = items.getJSONObject(i)
                val phones = o.getJSONArray("phones")
                add(
                    PostBranch(
                        id = o.getInt("id"),
                        name = o.getString("name"),
                        region = o.getString("region"),
                        city = o.getString("city"),
                        address = o.getString("address"),
                        phones = (0 until phones.length()).map { phones.getString(it) },
                        hours = o.getString("hours"),
                        lat = o.getDouble("lat"),
                        lng = o.getDouble("lng"),
                    ),
                )
            }
        }
        cache.value = PostBranches(checkedAt = root.getString("checkedAt"), branches = branches)
    }
}
