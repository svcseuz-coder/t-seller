package uz.carlcare.salesassistant.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProductEntity::class,
        SpecEntity::class,
        ServiceCenterEntity::class,
        SupportInfoEntity::class,
        FavoriteEntity::class,
        RecentViewEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun catalogDao(): CatalogDao
    abstract fun serviceDao(): ServiceDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN source TEXT")
                db.execSQL("ALTER TABLE products ADD COLUMN checked_at TEXT")
            }
        }
    }
}
