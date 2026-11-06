package uz.carlcare.salesassistant.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CatalogDao {
    @Transaction
    @Query("SELECT * FROM products ORDER BY brand, series, model")
    fun observeAll(): Flow<List<ProductWithSpecs>>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :id")
    fun observeProduct(id: String): Flow<ProductWithSpecs?>

    @Query("SELECT COUNT(*) FROM products")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpecs(specs: List<SpecEntity>)
}
