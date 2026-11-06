package uz.carlcare.salesassistant.core.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import uz.carlcare.salesassistant.core.data.db.AppDatabase
import uz.carlcare.salesassistant.core.data.db.ProductEntity
import uz.carlcare.salesassistant.core.data.db.ProductWithSpecs
import uz.carlcare.salesassistant.core.data.db.SpecEntity

class CatalogRepository(
    private val database: AppDatabase,
    private val context: Context,
) {
    private val dao = database.catalogDao()

    fun observeAll(): Flow<List<ProductWithSpecs>> = dao.observeAll()

    fun observeFiltered(brand: String?, query: String): Flow<List<ProductWithSpecs>> =
        dao.observeAll().map { items ->
            items.filter { data ->
                (brand == null || data.product.brand == brand) &&
                    SearchText.matches(data.searchText(), query)
            }
        }

    fun observeProduct(id: String): Flow<ProductWithSpecs?> = dao.observeProduct(id)

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.count() > 0) return@withContext
        val json = context.assets.open("products.json").bufferedReader().use { it.readText() }
        val items = JSONObject(json).getJSONArray("items")
        val products = mutableListOf<ProductEntity>()
        val specs = mutableListOf<SpecEntity>()

        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val id = o.getString("id")
            val variantsJson = o.optJSONArray("variant_labels")
            val variants = buildList {
                if (variantsJson != null) {
                    for (v in 0 until variantsJson.length()) add(variantsJson.getString(v))
                }
            }
            val baseVariant = variants.firstOrNull() ?: "global"
            val image = o.optJSONArray("images")?.takeIf { it.length() > 0 }?.getString(0)

            products += ProductEntity(
                id = id,
                brand = o.getString("brand"),
                series = o.getString("series"),
                model = o.getString("model"),
                year = o.optInt("year"),
                status = o.getString("status"),
                gsmarenaUrl = o.optStringOrNull("gsmarena_url"),
                gsmarenaName = o.optStringOrNull("gsmarena_name"),
                matchNote = o.optStringOrNull("match_note"),
                fetchedAt = o.optStringOrNull("fetched_at"),
                imageUrl = image,
                variantLabels = if (variants.size > 1) variants.joinToString("|") else null,
            )

            o.optJSONObject("specs")?.let { collectSpecs(id, baseVariant, it, specs) }

            o.optJSONArray("regional_variants")?.let { arr ->
                for (v in 0 until arr.length()) {
                    val variant = arr.getJSONObject(v)
                    val label = variant.optString("label", "variant")
                    variant.optJSONObject("specs")?.let { collectSpecs(id, label, it, specs) }
                }
            }
        }

        database.withTransaction {
            dao.insertProducts(products)
            dao.insertSpecs(specs)
        }
    }

    private fun collectSpecs(
        productId: String,
        variant: String,
        group: JSONObject,
        out: MutableList<SpecEntity>,
    ) {
        val groups = group.keys()
        while (groups.hasNext()) {
            val groupName = groups.next()
            val fields = group.optJSONObject(groupName) ?: continue
            val names = fields.keys()
            while (names.hasNext()) {
                val fieldName = names.next()
                if (fields.isNull(fieldName)) continue
                val value = fields.optString(fieldName, "").trim()
                if (value.isEmpty() || value == "null") continue
                if (value.equals("Unspecified", ignoreCase = true)) continue
                out += SpecEntity(productId, variant, groupName, fieldName, value)
            }
        }
    }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() && it != "null" }

private fun ProductWithSpecs.searchText(): String = buildString {
    append(product.brand).append(' ')
    append(product.series).append(' ')
    append(product.model).append(' ')
    product.variantLabels?.let { append(it).append(' ') }
    specs.forEach { append(it.value).append(' ') }
}
