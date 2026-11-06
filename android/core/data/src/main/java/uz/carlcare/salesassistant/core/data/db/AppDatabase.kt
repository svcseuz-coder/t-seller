package uz.carlcare.salesassistant.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProductEntity::class,
        SpecEntity::class,
        ServiceCenterEntity::class,
        SupportInfoEntity::class,
        FavoriteEntity::class,
        RecentViewEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun serviceDao(): ServiceDao
}
