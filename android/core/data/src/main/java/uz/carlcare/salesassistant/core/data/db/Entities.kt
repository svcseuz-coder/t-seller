package uz.carlcare.salesassistant.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "products",
    indices = [Index("brand"), Index("series")],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val brand: String,
    val series: String,
    val model: String,
    val year: Int,
    val status: String,
    @ColumnInfo(name = "gsmarena_url") val gsmarenaUrl: String?,
    @ColumnInfo(name = "gsmarena_name") val gsmarenaName: String?,
    @ColumnInfo(name = "match_note") val matchNote: String?,
    @ColumnInfo(name = "fetched_at") val fetchedAt: String?,
    @ColumnInfo(name = "image_url") val imageUrl: String?,
    @ColumnInfo(name = "variant_labels") val variantLabels: String?,
)

@Entity(
    tableName = "specs",
    primaryKeys = ["productId", "variant", "groupName", "fieldName"],
    indices = [Index("productId"), Index("groupName", "fieldName")],
)
data class SpecEntity(
    val productId: String,
    val variant: String,
    val groupName: String,
    val fieldName: String,
    val value: String,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "added_at") val addedAt: Long,
)

@Entity(tableName = "recent_views")
data class RecentViewEntity(
    @PrimaryKey @ColumnInfo(name = "product_id") val productId: String,
    @ColumnInfo(name = "viewed_at") val viewedAt: Long,
)

@Entity(tableName = "service_centers")
data class ServiceCenterEntity(
    @PrimaryKey val id: Int,
    val code: String,
    val name: String,
    val city: String,
    val address: String,
    val phones: String,
    val brands: String,
    val lat: Double,
    val lng: Double,
    @ColumnInfo(name = "weekday_hours") val weekdayHours: String,
    @ColumnInfo(name = "saturday_hours") val saturdayHours: String,
    @ColumnInfo(name = "sunday_hours") val sundayHours: String,
)

@Entity(tableName = "support_info")
data class SupportInfoEntity(
    @PrimaryKey val id: Int = 1,
    val hotline: String,
    @ColumnInfo(name = "hotline_dial") val hotlineDial: String,
    val email: String,
    @ColumnInfo(name = "source_url") val sourceUrl: String,
    @ColumnInfo(name = "checked_at") val checkedAt: String,
)

data class ProductWithSpecs(
    @Embedded val product: ProductEntity,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val specs: List<SpecEntity>,
) {
    fun variants(): List<String> =
        product.variantLabels?.split('|')?.filter { it.isNotBlank() }?.ifEmpty { listOf("global") }
            ?: listOf("global")

    fun specsFor(variant: String): Map<String, Map<String, String>> {
        val chosen = if (variants().contains(variant)) variant else variants().first()
        return specs
            .filter { it.variant == chosen }
            .groupBy { it.groupName }
            .mapValues { (_, rows) -> rows.associate { it.fieldName to it.value } }
    }

    fun field(group: String, field: String, variant: String): String? {
        val chosen = if (variants().contains(variant)) variant else variants().first()
        return specs.firstOrNull {
            it.variant == chosen && it.groupName == group && it.fieldName == field
        }?.value
    }
}
