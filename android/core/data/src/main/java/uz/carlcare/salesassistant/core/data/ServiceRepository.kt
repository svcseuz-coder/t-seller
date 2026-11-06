package uz.carlcare.salesassistant.core.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import uz.carlcare.salesassistant.core.data.db.AppDatabase
import uz.carlcare.salesassistant.core.data.db.ServiceCenterEntity
import uz.carlcare.salesassistant.core.data.db.SupportInfoEntity

class ServiceRepository(
    private val database: AppDatabase,
    private val context: Context,
) {
    private val dao = database.serviceDao()

    fun observeCenters(): Flow<List<ServiceCenterEntity>> = dao.observeCenters()

    fun observeSupport(): Flow<SupportInfoEntity?> = dao.observeSupport()

    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.countCenters() > 0) return@withContext
        val root = JSONObject(
            context.assets.open("service_centers.json").bufferedReader().use { it.readText() },
        )
        val support = root.getJSONObject("support")
        val items = root.getJSONArray("centers")
        val centers = buildList {
            for (i in 0 until items.length()) {
                val o = items.getJSONObject(i)
                add(
                    ServiceCenterEntity(
                        id = o.getInt("id"),
                        code = o.getString("code"),
                        name = o.getString("name"),
                        city = o.getString("city"),
                        address = o.getString("address"),
                        phones = o.getJSONArray("phones").toStringList().joinToString("|"),
                        brands = o.getJSONArray("brands").toStringList().joinToString(","),
                        lat = o.getDouble("lat"),
                        lng = o.getDouble("lng"),
                        weekdayHours = o.optString("weekday_hours"),
                        saturdayHours = o.optString("saturday_hours"),
                        sundayHours = o.optString("sunday_hours"),
                    ),
                )
            }
        }

        database.withTransaction {
            dao.insertCenters(centers)
            dao.insertSupport(
                SupportInfoEntity(
                    hotline = support.getString("hotline"),
                    hotlineDial = support.getString("hotline_dial"),
                    email = support.getString("email"),
                    sourceUrl = root.getString("source"),
                    checkedAt = root.getString("checked_at"),
                ),
            )
        }
    }
}

private fun org.json.JSONArray.toStringList(): List<String> = buildList {
    for (i in 0 until length()) add(getString(i))
}
